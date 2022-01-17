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
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import forms.AmountForm
import models.User
import models.charity.GiftAidCYAModel
import models.charity.prior.GiftAidSubmissionModel
import org.slf4j
import play.api.Logger
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.GiftAidSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.charity.LastTaxYearAmountView

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class LastTaxYearAmountController @Inject()(
                                             view: LastTaxYearAmountView
                                           )(
                                             implicit cc: MessagesControllerComponents,
                                             authAction: AuthorisedAction,
                                             errorHandler: ErrorHandler,
                                             giftAidSessionService: GiftAidSessionService,
                                             appConfig: AppConfig,
                                             previousPage: GiftAidLastTaxYearController
                                           ) extends FrontendController(cc) with I18nSupport with CharityJourney {

  lazy val logger: slf4j.Logger = Logger(this.getClass).logger

  override def handleRedirect(
                               taxYear: Int,
                               cya: GiftAidCYAModel,
                               prior: Option[GiftAidSubmissionModel],
                               fromShow: Boolean = false
                             )(implicit user: User[AnyContent]): Result = {

    val priorAmount: Option[BigDecimal] = prior.flatMap(_.giftAidPayments.flatMap(_.currentYearTreatedAsPreviousYear))
    val cyaAmount: Option[BigDecimal] = cya.addDonationToLastYearAmount

    (cya.addDonationToLastYear, cya.donationsViaGiftAidAmount) match {
      case (Some(true), Some(totalDonation)) =>
        val amountForm = (priorAmount, cyaAmount) match {
          case (priorAmountOpt, Some(cyaValue)) if !priorAmountOpt.contains(cyaValue) => form(user.isAgent, taxYear, totalDonation).fill(cyaValue)
          case _ => form(user.isAgent, taxYear, totalDonation)
        }
        val page = Ok(view(taxYear, amountForm, cyaAmount.map(_.toString()), priorAmount))
        if (fromShow){
          page
        } else {
          Redirect(controllers.charity.routes.LastTaxYearAmountController.show(taxYear))
        }
      case (Some(false), _) => Redirect(controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear))
      case _ => previousPage.handleRedirect(taxYear, cya, prior)
    }
  }

  def agentOrIndividual(implicit isAgent: Boolean): String = if (isAgent) "agent" else "individual"

  def form(implicit isAgent: Boolean, taxYear: Int, cannotExceed: BigDecimal): Form[BigDecimal] = AmountForm.amountExceedForm(
    emptyFieldKey = "charity.last-tax-year-donation-amount.error.no-entry." + agentOrIndividual,
    wrongFormatKey = "charity.last-tax-year-donation-amount.error.invalid",
    exceedsMaxAmountKey = "charity.last-tax-year-donation-amount.error.maximum." + agentOrIndividual,
    exceedAmountKey = "charity.last-tax-year-donation-amount.error.exceeds." + agentOrIndividual,
    exceedAmount = cannotExceed,
    emptyFieldArguments = Seq(taxYear.toString)
  )

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>
    giftAidSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { case (cya, prior) =>
      cya match {
        case Some(cyaData) => handleRedirect(taxYear, cyaData, prior, fromShow = true)
        case _ =>
          logger.warn("[LastTaxYearAmountController][show] No CYA data retrieved from the mongo database. Redirecting to the overview page.")
          redirectToOverview(taxYear)
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, GIFT_AID)).async { implicit user =>

    giftAidSessionService.getSessionData(taxYear).map {
      case Left(_) => Future.successful(errorHandler.internalServerError())
      case Right(data) =>
        data.flatMap(_.giftAid) match {
      case Some(cyaData) =>
        cyaData.donationsViaGiftAidAmount match {
          case Some(totalDonatedAmount) =>
            form(user.isAgent, taxYear, totalDonatedAmount).bindFromRequest().fold(
              {
                formWithErrors =>
                  Future.successful(BadRequest(view(taxYear, formWithErrors, None, None)))
              }, {
                submittedAmount =>
                  val updatedCyaData: GiftAidCYAModel = cyaData.copy(addDonationToLastYearAmount = Some(submittedAmount))

                  val redirectLocation = if(updatedCyaData.isFinished){
                    Redirect(controllers.charity.routes.GiftAidCYAController.show(taxYear))
                  } else {
                    Redirect(controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear))
                  }

                  giftAidSessionService.updateSessionData(updatedCyaData, taxYear)(
                    errorHandler.internalServerError()
                  )(redirectLocation)
              })
          case _ =>
            logger.warn("[LastTaxYearAmountController][submit] No 'donationsViaGiftAidAmount' in mongo database. Redirecting to the overview page.")
            Future.successful(redirectToOverview(taxYear))
        }
      case _ =>
        logger.warn("[LastTaxYearAmountController][submit] No CYA data retrieved from the mongo database. Redirecting to the overview page.")
        Future.successful(redirectToOverview(taxYear))
    }
    }.flatten
  }
}
