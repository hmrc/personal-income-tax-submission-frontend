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

import config.{AppConfig, ErrorHandler, GIFT_AID}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import forms.AmountForm
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.charity.GiftAidOverseasAmountView
import javax.inject.Inject
import models.User
import models.charity.GiftAidCYAModel
import models.charity.prior.GiftAidSubmissionModel
import play.api.Logging
import services.GiftAidSessionService

import scala.concurrent.{ExecutionContext, Future}

class GiftAidOverseasAmountController @Inject()(
                                               implicit cc: MessagesControllerComponents,
                                               overseasGiftAidDonationsController: OverseasGiftAidDonationsController,
                                               authAction: AuthorisedAction,
                                               appConfig: AppConfig,
                                               view: GiftAidOverseasAmountView,
                                               giftAidSessionService: GiftAidSessionService,
                                               errorHandler: ErrorHandler,
                                               ec: ExecutionContext
                                             ) extends FrontendController(cc) with I18nSupport with CharityJourney with Logging {

  override def handleRedirect(taxYear: Int, cya: GiftAidCYAModel, prior: Option[GiftAidSubmissionModel], fromShow: Boolean)
                             (implicit user: User[AnyContent]): Result = {

    val cyaAmount: Option[BigDecimal] = cya.overseasDonationsViaGiftAidAmount

    val giftAidAmount: Option[BigDecimal] = cya.donationsViaGiftAidAmount

    (cya.overseasDonationsViaGiftAid, giftAidAmount) match {
      case (Some(true), Some(totalDonation)) =>
        val amountForm = cyaAmount match {
          case Some(cyaValue) => form(user.isAgent, taxYear, totalDonation).fill(cyaValue)
          case _ => form(user.isAgent, taxYear, totalDonation)
        }
        determineResult(
          Ok(view(taxYear, amountForm)),
          Redirect(controllers.charity.routes.GiftAidOverseasAmountController.show(taxYear)),
          fromShow
        )
          case _ => overseasGiftAidDonationsController.handleRedirect(taxYear, cya, prior)
    }
  }

  def agentOrIndividual(implicit isAgent: Boolean): String = if (isAgent) "agent" else "individual"

  def form(implicit isAgent: Boolean, taxYear: Int, cannotExceed: BigDecimal): Form[BigDecimal] = AmountForm.amountExceedForm(
    emptyFieldKey = "charity.amount-overseas-gift-aid.error.empty." + agentOrIndividual,
    wrongFormatKey = "charity.amount-overseas-gift-aid.error.incorrect-format." + agentOrIndividual,
    exceedsMaxAmountKey = "charity.amount-overseas-gift-aid.error.too-high." + agentOrIndividual,
    exceedAmountKey = "charity.amount-overseas-gift-aid.error.exceed." + agentOrIndividual,
    exceedAmount = cannotExceed,
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
      case Left(_) => Future.successful(errorHandler.internalServerError())
      case Right(data) =>
        data.flatMap(_.giftAid) match {
      case Some(cyaModel) =>
        cyaModel.donationsViaGiftAidAmount match {
          case Some(totalDonatedAmount) =>
            form(user.isAgent, taxYear, totalDonatedAmount).bindFromRequest().fold({
              formWithErrors =>
                Future.successful(BadRequest(view(taxYear, formWithErrors)))
            }, {
              formAmount =>
                val updatedCya = cyaModel.copy(overseasDonationsViaGiftAidAmount = Some(formAmount))
                val redirectLocation = if(updatedCya.isFinished) {
                  redirectToCya(taxYear)
                } else {
                  Redirect(controllers.charity.routes.GiftAidOverseasNameController.show(taxYear, None))
                }

                giftAidSessionService.updateSessionData(updatedCya, taxYear)(
                  InternalServerError(errorHandler.internalServerErrorTemplate)
                )(redirectLocation)
            })
          case _ =>
            logger.warn("[GiftAidOverseasAmountController][submit] No 'donationsViaGiftAidAmount' in mongo database. Redirecting to the overview page.")
            Future.successful(redirectToOverview(taxYear))
        }
      case _ =>
        logger.info("[GiftAidOverseasAmountController][submit] No CYA data in session. Redirecting to overview page.")
        Future.successful(redirectToOverview(taxYear))
    }
    }.flatten
  }

}
