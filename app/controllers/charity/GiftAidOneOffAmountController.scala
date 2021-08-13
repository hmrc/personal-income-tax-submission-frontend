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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.charity.GiftAidOneOffAmountView
import javax.inject.Inject
import models.User
import models.charity.GiftAidCYAModel
import models.charity.prior.GiftAidSubmissionModel
import play.api.Logging
import services.GiftAidSessionService

import scala.concurrent.{ExecutionContext, Future}

class GiftAidOneOffAmountController @Inject()(
                                                implicit cc: MessagesControllerComponents,
                                                giftAidOneOffController: GiftAidOneOffController,
                                                authAction: AuthorisedAction,
                                                appConfig: AppConfig,
                                                view: GiftAidOneOffAmountView,
                                                giftAidSessionService: GiftAidSessionService,
                                                errorHandler: ErrorHandler,
                                                ec: ExecutionContext
                                              ) extends FrontendController(cc) with I18nSupport with CharityJourney with Logging {

  override def handleRedirect(taxYear: Int, cya: GiftAidCYAModel, prior: Option[GiftAidSubmissionModel], fromShow: Boolean)
                             (implicit user: User[AnyContent]): Result = {

    val priorAmount: Option[BigDecimal] = prior.flatMap(_.giftAidPayments.flatMap(_.oneOffCurrentYear))
    val cyaAmount: Option[BigDecimal] = cya.oneOffDonationsViaGiftAidAmount

    val amountForm = (priorAmount, cyaAmount) match {
      case (priorValueOpt, Some(cyaValue)) if !priorValueOpt.contains(cyaValue) => form(user.isAgent, taxYear).fill(cyaValue)
      case _ => form(user.isAgent, taxYear)
    }

    cya.oneOffDonationsViaGiftAid match {
      case Some(true) => determineResult(
        Ok(view(taxYear, amountForm, None)),
        Redirect(controllers.charity.routes.GiftAidOneOffAmountController.show(taxYear)),
        fromShow)
      case _ => giftAidOneOffController.handleRedirect(taxYear, cya, prior)
    }
  }

  def agentOrIndividual(implicit isAgent: Boolean): String = if (isAgent) "agent" else "individual"

  def form(implicit isAgent: Boolean, taxYear: Int): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "charity.gift-aid-one-off-amount.error.empty." + agentOrIndividual,
    wrongFormatKey = "charity.gift-aid-one-off-amount.error.incorrect-format." + agentOrIndividual,
    exceedsMaxAmountKey = "charity.gift-aid-one-off-amount.error.too-high." + agentOrIndividual,
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

    form(user.isAgent, taxYear).bindFromRequest().fold({
      formWithErrors => Future.successful(BadRequest(view(taxYear, formWithErrors, None)))
    }, {
      formAmount => giftAidSessionService.getSessionData(taxYear).map(_.flatMap(_.giftAid)).map {
        case Some(cyaModel) =>
          val updatedCya = cyaModel.copy(oneOffDonationsViaGiftAidAmount = Some(formAmount))
          val redirectLocation = if(updatedCya.isFinished) {
            redirectToCya(taxYear)
          } else {
            Redirect(controllers.charity.routes.OverseasGiftAidDonationsController.show(taxYear))
          }

          giftAidSessionService.updateSessionData(updatedCya, taxYear)(
            InternalServerError(errorHandler.internalServerErrorTemplate)
          )(redirectLocation)
        case _ =>
          logger.info("[GiftAidOneOffAmountController][submit] No CYA data in session. Redirecting to overview page.")
          Future.successful(redirectToOverview(taxYear))
      }.flatten
    })
  }
}
