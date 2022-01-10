/*
 * Copyright 2022 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package controllers.charity

import config.{AppConfig, ErrorHandler, GIFT_AID}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import forms.AmountForm
import models.User
import models.charity.GiftAidCYAModel
import models.charity.prior.GiftAidSubmissionModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.GiftAidSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.charity.GiftAidAppendNextYearTaxAmountView

import javax.inject.Inject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class GiftAidAppendNextYearTaxAmountController @Inject()(
                                                          implicit cc: MessagesControllerComponents,
                                                          view: GiftAidAppendNextYearTaxAmountView,
                                                          authorisedAction: AuthorisedAction,
                                                          appConfig: AppConfig,
                                                          giftAidSessionService: GiftAidSessionService,
                                                          errorHandler: ErrorHandler,
                                                          previousPage: DonationsToPreviousTaxYearController
                                                        ) extends FrontendController(cc) with I18nSupport with CharityJourney {

  override def handleRedirect(
                               taxYear: Int,
                               cya: GiftAidCYAModel,
                               prior: Option[GiftAidSubmissionModel],
                               fromShow: Boolean = false
                             )(implicit user: User[AnyContent]): Result = {

    val priorAmount: Option[BigDecimal] = prior.flatMap(_.giftAidPayments.flatMap(_.nextYearTreatedAsCurrentYear))
    val cyaAmount: Option[BigDecimal] = cya.addDonationToThisYearAmount

    val amountForm = (priorAmount, cyaAmount) match {
      case (priorValueOpt, Some(cyaValue)) if !priorValueOpt.contains(cyaValue) => form(user.isAgent, taxYear).fill(cyaValue)
      case _ => form(user.isAgent, taxYear)
    }

    cya.addDonationToThisYear match {
      case Some(true) => if (fromShow) {
        Ok(view(taxYear, amountForm, cyaAmount.map(_.toString()), priorAmount))
      } else {
        Redirect(controllers.charity.routes.GiftAidAppendNextYearTaxAmountController.show(taxYear, taxYear))
      }
      case Some(false) => Redirect(controllers.charity.routes.GiftAidQualifyingSharesSecuritiesController.show(taxYear))
      case _ => previousPage.handleRedirect(taxYear, cya, prior)
    }
  }

  def agentOrIndividual(implicit isAgent: Boolean): String = if (isAgent) "agent" else "individual"

  def form(implicit isAgent: Boolean, taxYear: Int): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "charity.add-to-current-tax-year-amount.errors.no-entry." + agentOrIndividual,
    wrongFormatKey = "charity.add-to-current-tax-year-amount.errors.wrong-format",
    exceedsMaxAmountKey = "charity.add-to-current-tax-year-amount.errors.max-amount." + agentOrIndividual,
    emptyFieldArguments = Seq(taxYear.toString)
  )

  def show(taxYear: Int, someTaxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>
    if (taxYear == someTaxYear) {
      giftAidSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { case (cya, prior) =>
        cya match {
          case Some(cyaData) => handleRedirect(taxYear, cyaData, prior, fromShow = true)
          case None => redirectToOverview(taxYear)
        }
      }
    } else {
      Future.successful(Redirect(controllers.charity.routes.GiftAidAppendNextYearTaxAmountController.show(taxYear, taxYear)))
    }

  }

  def submit(taxYear: Int, someTaxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>
    if (taxYear == someTaxYear) {
      form(user.isAgent, taxYear).bindFromRequest.fold(
        {
          formWithErrors => Future.successful(BadRequest(view(taxYear, formWithErrors, None, None)))
        },
        {
          formAmount =>
            giftAidSessionService.getSessionData(taxYear).map {
              case Left(_) => Future.successful(errorHandler.internalServerError())
              case Right(data) =>
                data.flatMap(_.giftAid) match {
                  case Some(cyaData) =>
                    //noinspection DuplicatedCode
                    val updatedCya: GiftAidCYAModel = cyaData.copy(addDonationToThisYearAmount = Some(formAmount))

                    val redirectLocation = if (updatedCya.isFinished) {
                      Redirect(controllers.charity.routes.GiftAidCYAController.show(taxYear))
                    } else {
                      Redirect(controllers.charity.routes.GiftAidQualifyingSharesSecuritiesController.show(taxYear))
                    }

                    giftAidSessionService.updateSessionData(updatedCya, taxYear)(errorHandler.internalServerError())(
                      redirectLocation
                    )
                  case None => Future.successful(redirectToOverview(taxYear))
                }
            }.flatten
        }
      )
    }
      else
      {
        Future.successful(Redirect(controllers.charity.routes.GiftAidAppendNextYearTaxAmountController.show(taxYear, taxYear)))
      }
    }

  }
