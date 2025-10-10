/*
 * Copyright 2023 HM Revenue & Customs
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

import config.{AppConfig, GIFT_AID, ErrorHandler}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import forms.AmountForm
import models.User
import models.charity.GiftAidCYAModel
import models.charity.prior.GiftAidSubmissionModel

import javax.inject.Inject
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.i18n.Lang.logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.GiftAidSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.charity.OverseasSharesSecuritiesLandPropertyAmountView

import scala.concurrent.{ExecutionContext, Future}

class OverseasSharesSecuritiesLandPropertyAmountController @Inject()(
                                                           implicit cc: MessagesControllerComponents,
                                                           appConfig: AppConfig,
                                                           view: OverseasSharesSecuritiesLandPropertyAmountView,
                                                           giftAidQualifyingSharesSecuritiesController: GiftAidQualifyingSharesSecuritiesController,
                                                           giftAidSharesSecuritiesLandPropertyOverseasController:
                                                           GiftAidSharesSecuritiesLandPropertyOverseasController,
                                                           giftAidSessionService: GiftAidSessionService,
                                                           errorHandler: ErrorHandler,
                                                           authorisedAction: AuthorisedAction
                                                         ) extends FrontendController(cc) with I18nSupport with CharityJourney {

  implicit val executionContext: ExecutionContext = cc.executionContext

  override def handleRedirect(taxYear: Int, cya: GiftAidCYAModel, prior: Option[GiftAidSubmissionModel], fromShow: Boolean)
                             (implicit user: User[AnyContent]): Result = {

    val cyaAmount: Option[BigDecimal] = cya.overseasDonatedSharesSecuritiesLandOrPropertyAmount

    val donatedSSLP: BigDecimal =
      cya.donatedLandOrPropertyAmount.getOrElse(BigDecimal(0)) +
        cya.donatedSharesOrSecuritiesAmount.getOrElse(BigDecimal(0))

    val amountForm = cyaAmount match {
      case Some(cyaValue) => form(user.isAgent, donatedSSLP).fill(cyaValue)
      case _ => form(user.isAgent, donatedSSLP)
    }

    cya.overseasDonatedSharesSecuritiesLandOrProperty match {
      case Some(true) if donatedSSLP == BigDecimal(0) => giftAidQualifyingSharesSecuritiesController.handleRedirect(taxYear, cya, prior)
      case Some(true) => determineResult(Ok(view(taxYear, amountForm)),
        Redirect(controllers.charity.routes.OverseasSharesSecuritiesLandPropertyAmountController.show(taxYear)),
        fromShow)
      case _ => giftAidSharesSecuritiesLandPropertyOverseasController.handleRedirect(taxYear, cya, prior)
    }
  }

  def agentOrIndividual(implicit isAgent: Boolean): String = if (isAgent) "agent" else "individual"

  def form(implicit isAgent: Boolean, cannotExceed: BigDecimal): Form[BigDecimal] = AmountForm.amountExceedForm(
    emptyFieldKey = "charity.overseas-shares-securities-land-property-amount.error.empty-field." + agentOrIndividual,
    wrongFormatKey = "charity.overseas-shares-securities-land-property-amount.error.wrong-format." + agentOrIndividual,
    exceedsMaxAmountKey = "charity.overseas-shares-securities-land-property-amount.error.max-amount." + agentOrIndividual,
    exceedAmountKey = "charity.overseas-shares-securities-land-property-amount.error.exceed",
    exceedAmount = cannotExceed
  )

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>
    giftAidSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>

      cya match {
        case Some(cyaData) => handleRedirect(taxYear, cyaData, prior, fromShow = true)
        case _ => redirectToOverview(taxYear)
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>
    giftAidSessionService.getSessionData(taxYear).map {
      case Left(_) => Future.successful(errorHandler.internalServerError())
      case Right(data) =>
        data.flatMap(_.giftAid) match {
          case Some(cyaData) =>
            val landPropAmt: BigDecimal = cyaData.donatedLandOrPropertyAmount.getOrElse(0)
            val shareSecAmt: BigDecimal = cyaData.donatedSharesOrSecuritiesAmount.getOrElse(0)
            val zero = BigDecimal(0)

            landPropAmt + shareSecAmt match {
              case `zero` =>
                logger.warn("[OverseasSharesSecuritiesLandPropertyAmountController][submit] " +
                  "No donated land/property or shares/securities in mongo database. Redirecting to the overview page.")
                Future.successful(redirectToOverview(taxYear))
              case total =>
                form(user.isAgent, total).bindFromRequest().fold({
                  formWithErrors => Future.successful(BadRequest(view(taxYear, formWithErrors)))
                }, {
                  amount =>
                    val updatedCya = cyaData.copy(overseasDonatedSharesSecuritiesLandOrPropertyAmount = Some(amount))
                    val redirectLocation = if (updatedCya.isFinished) {
                      redirectToCya(taxYear)
                    } else {
                      Redirect(controllers.charity.routes.GiftAidOverseasSharesNameController.show(taxYear, None))
                    }
                    giftAidSessionService.updateSessionData(cyaData.copy(overseasDonatedSharesSecuritiesLandOrPropertyAmount = Some(amount)), taxYear)(
                      errorHandler.internalServerError()
                    )(redirectLocation)
                })
            }
          case _ =>
            logger.info("[OverseasSharesSecuritiesLandPropertyAmountController][submit] No CYA data in session. Redirecting to overview page.")
            Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
        }
    }.flatten
  }

}
