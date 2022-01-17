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
import play.api.i18n.Lang.logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.GiftAidSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.charity.GiftAidTotalShareSecurityAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GiftAidTotalShareSecurityAmountController @Inject()(
                                                           implicit cc: MessagesControllerComponents,
                                                           appConfig: AppConfig,
                                                           view: GiftAidTotalShareSecurityAmountView,
                                                           giftAidQualifyingSharesSecuritiesController: GiftAidQualifyingSharesSecuritiesController,
                                                           errorHandler: ErrorHandler,
                                                           giftAidSessionService: GiftAidSessionService,
                                                           executionContext: ExecutionContext,
                                                           authorisedAction: AuthorisedAction
                                                         ) extends FrontendController(cc) with I18nSupport with CharityJourney {

  override def handleRedirect(taxYear: Int, cya: GiftAidCYAModel, prior: Option[GiftAidSubmissionModel], fromShow: Boolean)
                             (implicit user: User[AnyContent]): Result = {

    val priorAmount: Option[BigDecimal] = prior.flatMap(_.gifts.flatMap(_.sharesOrSecurities))
    val cyaAmount: Option[BigDecimal] = cya.donatedSharesOrSecuritiesAmount

    val amountForm = (priorAmount, cyaAmount) match {
      case (priorValueOpt, Some(cyaValue)) if !priorValueOpt.contains(cyaValue) => form(user.isAgent).fill(cyaValue)
      case _ => form(user.isAgent)
    }

    cya.donatedSharesOrSecurities match {
      case Some(true) => determineResult(Ok(view(taxYear, amountForm, cyaAmount.map(_.toString()), priorAmount)),
        Redirect(controllers.charity.routes.GiftAidTotalShareSecurityAmountController.show(taxYear)),
        fromShow)
      case _ => giftAidQualifyingSharesSecuritiesController.handleRedirect(taxYear, cya, prior)
    }
  }

  def agentOrIndividual(implicit isAgent: Boolean): String = if (isAgent) "agent" else "individual"

  def form(implicit isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "charity.shares-or-securities.error.empty-field." + agentOrIndividual,
    wrongFormatKey = "charity.shares-or-securities.error.wrong-format." + agentOrIndividual,
    exceedsMaxAmountKey = "charity.shares-or-securities.error.max-amount." + agentOrIndividual
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
        data match {
      case Some(cyaData) =>
        form(user.isAgent).bindFromRequest().fold({
          formWithErrors => Future.successful(BadRequest(view(taxYear, formWithErrors, None, None)))
        }, {
          amount =>
            cyaData.giftAid.fold{
              Future.successful(redirectToOverview(taxYear))
            } {
              cyaModel =>
                val updatedCya = cyaModel.copy(donatedSharesOrSecuritiesAmount = Some(amount))
                val redirectLocation = if(updatedCya.isFinished) {
                  redirectToCya(taxYear)
                } else {
                  Redirect(controllers.charity.routes.GiftAidDonateLandOrPropertyController.show(taxYear))
                }

                giftAidSessionService.updateSessionData(updatedCya, taxYear)(
                  InternalServerError(errorHandler.internalServerErrorTemplate)
                )(
                  redirectLocation
                )
            }
        }

        )
      case _ =>
        logger.info("[GiftAidTotalShareSecurityAmountController][submit] No CYA data in session. Redirecting to overview page.")
        Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
    }.flatten
  }

}
