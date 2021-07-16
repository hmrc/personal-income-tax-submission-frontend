/*
 * Copyright 2021 HM Revenue & Customs
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
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import forms.AmountForm
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.charity.GiftAidDonatedAmountView
import javax.inject.Inject
import models.User
import play.api.Logging
import models.charity.GiftAidCYAModel
import models.charity.prior.GiftAidSubmissionModel
import services.GiftAidSessionService

import scala.concurrent.{ExecutionContext, Future}

class GiftAidDonatedAmountController @Inject()(
                                                implicit cc: MessagesControllerComponents,
                                                donationsToPreviousTaxYearController: DonationsToPreviousTaxYearController,
                                                authAction: AuthorisedAction,
                                                appConfig: AppConfig,
                                                view: GiftAidDonatedAmountView,
                                                giftAidSessionService: GiftAidSessionService,
                                                errorHandler: ErrorHandler,
                                                ec: ExecutionContext
                                              ) extends FrontendController(cc) with I18nSupport with CharityJourney with Logging {

  override def handleRedirect(taxYear: Int, cya: GiftAidCYAModel, prior: Option[GiftAidSubmissionModel], fromShow:Boolean)
                             (implicit user: User[AnyContent]): Result = {

    val priorDonatedAmount: Option[BigDecimal] = prior.flatMap(_.giftAidPayments.flatMap(_.currentYear))
    val cyaDonatedAmount: Option[BigDecimal] = cya.donationsViaGiftAidAmount

    val amountForm = (priorDonatedAmount, cyaDonatedAmount) match {
      case (priorValueOpt, Some(cyaValue)) if !priorValueOpt.contains(cyaValue) => form(user.isAgent, taxYear).fill(cyaValue)
      case _ => form(user.isAgent, taxYear)
    }

    cya.donationsViaGiftAid match {
      case Some(true) => determineResult(
        Ok(view(taxYear, amountForm, None)),
        Redirect(controllers.charity.routes.GiftAidDonatedAmountController.show(taxYear)),
        fromShow)
      case _ => Redirect(controllers.charity.routes.GiftAidDonationsController.show(taxYear))
    }
  }

  def agentOrIndividual(implicit isAgent: Boolean): String = if (isAgent) "agent" else "individual"

  def form(implicit isAgent: Boolean, taxYear: Int): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "charity.amount-via-gift-aid.error.no-input." + agentOrIndividual,
    wrongFormatKey = "charity.amount-via-gift-aid.error.incorrect-format." + agentOrIndividual,
    exceedsMaxAmountKey = "charity.amount-via-gift-aid.error.too-high." + agentOrIndividual,
    emptyFieldArguments = Seq(taxYear.toString)
  )

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>

    giftAidSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>

      cya match {
        case Some(cyaData) => handleRedirect(taxYear, cyaData, prior, fromShow = true)
        case _ => redirectToOverview(taxYear)
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, GIFT_AID)).async { implicit user =>

    giftAidSessionService.getSessionData(taxYear).map {

      case Some(cyaData) =>
        form(user.isAgent, taxYear).bindFromRequest().fold(
          {
            formWithErrors => Future.successful(BadRequest(view(taxYear, formWithErrors, None)))
          },
          {
            success =>
              cyaData.giftAid.fold {
                Future.successful(redirectToOverview(taxYear))
              } {
                cyaModel =>
                  giftAidSessionService.updateSessionData(cyaModel.copy(donationsViaGiftAidAmount = Some(success)), taxYear)(
                    InternalServerError(errorHandler.internalServerErrorTemplate)
                  )(
                    Redirect(controllers.charity.routes.GiftAidOneOffController.show(taxYear))
                  )
              }
          }
        )
      case _ =>
        logger.info("[GiftAidDonatedAmountController][submit] No CYA data in session. Redirecting to overview page.")
        Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }.flatten
  }

}
