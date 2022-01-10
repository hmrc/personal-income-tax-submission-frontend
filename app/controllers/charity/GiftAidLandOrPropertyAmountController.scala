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
import views.html.charity.GiftAidLandOrPropertyAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GiftAidLandOrPropertyAmountController @Inject()(
                                                       implicit cc: MessagesControllerComponents,
                                                       view: GiftAidLandOrPropertyAmountView,
                                                       giftAidDonateLandOrPropertyController: GiftAidDonateLandOrPropertyController,
                                                       giftAidSessionService: GiftAidSessionService,
                                                       errorHandler: ErrorHandler,
                                                       authorisedAction: AuthorisedAction,
                                                       appConfig: AppConfig
                                                     ) extends FrontendController(cc) with I18nSupport with CharityJourney {

  implicit val executionContext: ExecutionContext = cc.executionContext

  override def handleRedirect(taxYear: Int, cya: GiftAidCYAModel, prior: Option[GiftAidSubmissionModel], fromShow: Boolean)
                             (implicit user: User[AnyContent]): Result = {

    val priorAmount: Option[BigDecimal] = prior.flatMap(_.gifts.flatMap(_.landAndBuildings))
    val cyaAmount: Option[BigDecimal] = cya.donatedLandOrPropertyAmount

    val amountForm = (priorAmount, cyaAmount) match {
      case (priorValueOpt, Some(cyaValue)) if !priorValueOpt.contains(cyaValue) => form(user.isAgent).fill(cyaValue)
      case _ => form(user.isAgent)
    }

    cya.donatedLandOrProperty match {
      case Some(true) => determineResult(
        Ok(view(taxYear, amountForm, cyaAmount.map(_.toString()), priorAmount)),
        Redirect(controllers.charity.routes.GiftAidLandOrPropertyAmountController.show(taxYear)),
        fromShow)
      case _ => giftAidDonateLandOrPropertyController.handleRedirect(taxYear, cya, prior)
    }
  }

  def agentOrIndividual(implicit isAgent: Boolean): String = if (isAgent) "agent" else "individual"

  def form(implicit isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "charity.land-or-property.errors.no-entry." + agentOrIndividual,
    wrongFormatKey = "charity.land-or-property.errors.wrong-format." + agentOrIndividual,
    exceedsMaxAmountKey = "charity.land-or-property.errors.max-amount." + agentOrIndividual
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
                cyaData.giftAid.fold {
                  Future.successful(redirectToOverview(taxYear))
                } {
                  cyaModel =>
                    val updatedCya = cyaModel.copy(donatedLandOrPropertyAmount = Some(amount))
                    val redirectLocation = if (updatedCya.isFinished) {
                      redirectToCya(taxYear)
                    } else {
                      Redirect(controllers.charity.routes.GiftAidSharesSecuritiesLandPropertyOverseasController.show(taxYear))
                    }

                    giftAidSessionService.updateSessionData(cyaModel.copy(donatedLandOrPropertyAmount = Some(amount)), taxYear)(
                      InternalServerError(errorHandler.internalServerErrorTemplate)
                    )(
                      redirectLocation
                    )
                }
            })
          case _ =>
            logger.info("[GiftAidLandOrPropertyAmountController][submit] No CYA data in session. Redirecting to overview page.")
            Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
        }
    }.flatten
  }

}
