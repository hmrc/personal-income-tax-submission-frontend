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
    
    lazy val page = Ok(view(taxYear, form(user.isAgent, taxYear), cya.addDonationToLastYearAmount.map(_.toString())))

    cya.addDonationToLastYear match {
      case Some(true) => if (fromShow) page else Redirect(controllers.charity.routes.LastTaxYearAmountController.show(taxYear))
      case Some(false) => Redirect(controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear))
      case _ => previousPage.handleRedirect(taxYear, cya, prior)
    }
  }

  def agentOrIndividual(implicit isAgent: Boolean): String = if (isAgent) "agent" else "individual"

  def form(implicit isAgent: Boolean, taxYear: Int): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "charity.last-tax-year-donation-amount.error.no-entry." + agentOrIndividual,
    wrongFormatKey = "charity.last-tax-year-donation-amount.error.invalid",
    exceedsMaxAmountKey = "charity.last-tax-year-donation-amount.error.maximum." + agentOrIndividual,
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
    form(user.isAgent, taxYear).bindFromRequest().fold(
      {
        formWithErrors =>
          Future.successful(BadRequest(
            view(taxYear, formWithErrors, None)
          ))
      },
      {
        submittedAmount =>
          giftAidSessionService.getSessionData(taxYear).map(_.flatMap(_.giftAid)).map {
            case Some(cyaData) =>
              //noinspection DuplicatedCode
              val updatedCyaData: GiftAidCYAModel = cyaData.copy(addDonationToLastYearAmount = Some(submittedAmount))

              val redirectLocation = if(updatedCyaData.isFinished){
                Redirect(controllers.charity.routes.GiftAidCYAController.show(taxYear))
              } else {
                Redirect(controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear))
              }

              giftAidSessionService.updateSessionData(updatedCyaData, taxYear)(
                errorHandler.internalServerError()
              )(redirectLocation)
            case _ =>
              logger.warn("[LastTaxYearAmountController][submit] No CYA data retrieved from the mongo database. Redirecting to the overview page.")
              Future.successful(redirectToOverview(taxYear))
          }.flatten
      }
    )
  }

}
