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
import forms.YesNoForm
import models.User
import models.charity.GiftAidCYAModel
import models.charity.prior.GiftAidSubmissionModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.i18n.Lang.logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.GiftAidSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.charity.GiftAidSharesSecuritiesLandPropertyOverseasView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GiftAidSharesSecuritiesLandPropertyOverseasController @Inject()(
                                                                       implicit val cc: MessagesControllerComponents,
                                                                       authAction: AuthorisedAction,
                                                                       view: GiftAidSharesSecuritiesLandPropertyOverseasView,
                                                                       giftAidDonateLandOrPropertyController: GiftAidDonateLandOrPropertyController,
                                                                       giftAidLandOrPropertyAmountController: GiftAidLandOrPropertyAmountController,
                                                                       giftAidSessionService: GiftAidSessionService,
                                                                       errorHandler: ErrorHandler,
                                                                       implicit val appConfig: AppConfig
                                                                     ) extends FrontendController(cc) with I18nSupport with SessionHelper with CharityJourney {

  implicit val executionContext: ExecutionContext = cc.executionContext

  override def handleRedirect(taxYear: Int, cya: GiftAidCYAModel, prior: Option[GiftAidSubmissionModel], fromShow: Boolean)
                             (implicit user: User[AnyContent]): Result = {
    (prior, cya.donatedLandOrProperty, cya.donatedLandOrPropertyAmount) match {
      case (Some(priorData), _, _) if priorData.gifts.map(_.investmentsNonUkCharities).isDefined =>
        Redirect(controllers.charity.routes.GiftAidCYAController.show(taxYear))
      case (_, None, _) => giftAidDonateLandOrPropertyController.handleRedirect(taxYear, cya, prior)
      case (_, Some(true), None) => giftAidLandOrPropertyAmountController.handleRedirect(taxYear, cya, prior)
      case _ => determineResult(
        Ok(view(yesNoForm(user), taxYear)),
        Redirect(controllers.charity.routes.GiftAidSharesSecuritiesLandPropertyOverseasController.show(taxYear)),
        fromShow)
    }
  }

  val yesNoForm: User[AnyContent] => Form[Boolean] = user => {
    val missingInputError = s"charity.shares-securities-land-property-overseas.errors.noChoice.${if (user.isAgent) "agent" else "individual"}"
    YesNoForm.yesNoForm(missingInputError)
  }

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>
    giftAidSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>
      cya match {
        case Some(cyaData) => handleRedirect(taxYear, cyaData, prior, fromShow = true)
        case _ => redirectToOverview(taxYear)
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>

    giftAidSessionService.getAndHandle(taxYear)(errorHandler.futureInternalServerError()) { (cya, prior) =>
      (cya, prior) match {
        case (_, Some(priorData)) if priorData.gifts.map(_.investmentsNonUkCharities).isDefined =>
          Future.successful(Redirect(controllers.charity.routes.GiftAidCYAController.show(taxYear)))
        case (Some(cyaData), _) =>
          yesNoForm(user).bindFromRequest().fold({
            formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear)))
          }, {
            success =>
              val redirectLocation = if (success) {
                controllers.charity.routes.OverseasSharesSecuritiesLandPropertyAmountController.show(taxYear)
              } else {
                controllers.charity.routes.GiftAidCYAController.show(taxYear)
              }
              giftAidSessionService.updateSessionData(
                cyaData.copy(
                  overseasDonatedSharesSecuritiesLandOrProperty = Some(success),
                  overseasDonatedSharesSecuritiesLandOrPropertyAmount =
                    if (success) cyaData.overseasDonatedSharesSecuritiesLandOrPropertyAmount else None,
                  overseasDonatedSharesSecuritiesLandOrPropertyCharityNames =
                    if (success) cyaData.overseasDonatedSharesSecuritiesLandOrPropertyCharityNames else None)
                , taxYear)(
                InternalServerError(errorHandler.internalServerErrorTemplate)
              )(
                Redirect(redirectLocation)
              )
          }
          )
        case _ =>
          logger.info("[GiftAidSharesSecuritiesLandPropertyOverseasController][submit] No CYA data in session. Redirecting to overview page.")
          Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }.flatten
  }

}
