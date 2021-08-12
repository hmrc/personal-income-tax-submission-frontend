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
import forms.YesNoForm

import javax.inject.Inject
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
import views.html.charity.GiftAidQualifyingSharesSecuritiesView

import scala.concurrent.{ExecutionContext, Future}

class GiftAidQualifyingSharesSecuritiesController @Inject()(
                                                             implicit val cc: MessagesControllerComponents,
                                                             authAction: AuthorisedAction,
                                                             giftAidQualifyingSharesSecuritiesView: GiftAidQualifyingSharesSecuritiesView,
                                                             giftAidSharesSecuritiesLandPropertyDonationController: GiftAidSharesSecuritiesLandPropertyDonationController,
                                                             errorHandler: ErrorHandler,
                                                             giftAidSessionService: GiftAidSessionService,
                                                             implicit val appConfig: AppConfig
                                                           ) extends FrontendController(cc) with I18nSupport with CharityJourney {

  implicit val executionContext: ExecutionContext = cc.executionContext

  override def handleRedirect(taxYear: Int, cya: GiftAidCYAModel, prior: Option[GiftAidSubmissionModel], fromShow: Boolean)
                             (implicit user: User[AnyContent]): Result = {
    (prior, cya.donatedSharesSecuritiesLandOrProperty) match {
      case (Some(priorData), _) if priorData.gifts.map(_.sharesOrSecurities).isDefined =>
        Redirect(controllers.charity.routes.GiftAidCYAController.show(taxYear))
      case (_, Some(true)) => determineResult(
        Ok(giftAidQualifyingSharesSecuritiesView(yesNoForm(user), taxYear)),
        Redirect(controllers.charity.routes.GiftAidQualifyingSharesSecuritiesController.show(taxYear)),
        fromShow)
      case (_, Some(false)) =>
        Redirect(controllers.charity.routes.GiftAidCYAController.show(taxYear))
      case _ => giftAidSharesSecuritiesLandPropertyDonationController.handleRedirect(taxYear, cya, prior)
    }
  }

  val yesNoForm: User[AnyContent] => Form[Boolean] = user => {
    val missingInputError = s"charity.qualifying-shares-or-securities.errors.noChoice.${if (user.isAgent) "agent" else "individual"}"
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


  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, GIFT_AID)).async { implicit user =>

    giftAidSessionService.getAndHandle(taxYear)(errorHandler.futureInternalServerError()) { (cya, prior) =>
      (cya, prior) match {
        case (_, Some(priorData)) if priorData.gifts.map(_.sharesOrSecurities).isDefined => Future.successful(redirectToCya(taxYear))
        case (Some(cyaData), _) =>
          yesNoForm(user).bindFromRequest().fold({
            formWithErrors => Future.successful(BadRequest(giftAidQualifyingSharesSecuritiesView(formWithErrors, taxYear)))
          }, {
            success =>
              val updatedCya = cyaData.copy(
                donatedSharesOrSecurities = Some(success),
                donatedSharesOrSecuritiesAmount = if(success) cyaData.donatedSharesOrSecuritiesAmount else None
              )

              val redirectLocation = (success, updatedCya.isFinished) match {
                case (true, _) => Redirect(controllers.charity.routes.GiftAidTotalShareSecurityAmountController.show(taxYear))
                case (_, true) => redirectToCya(taxYear)
                case _ => Redirect(controllers.charity.routes.GiftAidDonateLandOrPropertyController.show(taxYear))
              }

              giftAidSessionService.updateSessionData(updatedCya, taxYear)(
                InternalServerError(errorHandler.internalServerErrorTemplate)
              )(
                redirectLocation
              )
          }
          )
        case _ =>
          logger.info("[GiftAidQualifyingSharesSecuritiesController][submit] No CYA data in session. Redirecting to overview page.")
          Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }.flatten
  }


}
