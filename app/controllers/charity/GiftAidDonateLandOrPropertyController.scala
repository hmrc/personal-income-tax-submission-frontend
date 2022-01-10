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
import views.html.charity.GiftAidDonateLandOrPropertyView
import javax.inject.Inject
import models.charity.GiftAidCYAModel.resetDonatedSharesSecuritiesLandOrProperty

import scala.concurrent.{ExecutionContext, Future}

class GiftAidDonateLandOrPropertyController @Inject()(
                                                       implicit val cc: MessagesControllerComponents,
                                                       authAction: AuthorisedAction,
                                                       giftAidDonateLandOrPropertyView: GiftAidDonateLandOrPropertyView,
                                                       giftAidQualifyingSharesSecuritiesController: GiftAidQualifyingSharesSecuritiesController,
                                                       giftAidTotalShareSecurityAmountController: GiftAidTotalShareSecurityAmountController,
                                                       giftAidSessionService: GiftAidSessionService,
                                                       errorHandler: ErrorHandler,
                                                       implicit val appConfig: AppConfig
                                                     ) extends FrontendController(cc) with I18nSupport with SessionHelper with CharityJourney {

  implicit val executionContext: ExecutionContext = cc.executionContext

  override def handleRedirect(taxYear: Int, cya: GiftAidCYAModel, prior: Option[GiftAidSubmissionModel], fromShow: Boolean)
                             (implicit user: User[AnyContent]): Result = {

    val prefillForm = cya.donatedLandOrProperty.fold(yesNoForm(user))(yesNoForm(user).fill)

    (prior, cya.donatedSharesOrSecurities, cya.donatedSharesOrSecuritiesAmount) match {
      case (Some(priorData), _, _) if priorData.gifts.map(_.landAndBuildings).isDefined =>
        redirectToCya(taxYear)
      case (_, Some(true), None) =>
        giftAidTotalShareSecurityAmountController.handleRedirect(taxYear, cya, prior)
      case (_, None, _) =>
        giftAidQualifyingSharesSecuritiesController.handleRedirect(taxYear, cya, prior)
      case _ => determineResult(
        Ok(giftAidDonateLandOrPropertyView(prefillForm, taxYear)),
        Redirect(controllers.charity.routes.GiftAidDonateLandOrPropertyController.show(taxYear)),
        fromShow)
    }
  }

  val yesNoForm: User[AnyContent] => Form[Boolean] = user => {
    val missingInputError = s"charity.donated-land-or-property.errors.noChoice.${if (user.isAgent) "agent" else "individual"}"
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
        case (_, Some(priorData)) if priorData.gifts.map(_.landAndBuildings).isDefined =>
          Future.successful(Redirect(controllers.charity.routes.GiftAidCYAController.show(taxYear)))
        case (Some(cyaData), _) => yesNoForm(user).bindFromRequest().fold({
          formWithErrors => Future.successful(BadRequest(giftAidDonateLandOrPropertyView(formWithErrors, taxYear)))
        }, {
          yesOrNoResponse =>
            val updatedModel = updatedCya(yesOrNoResponse, cyaData)

            val redirectLocation = (yesOrNoResponse, cyaData.donatedSharesOrSecurities, cyaData.isFinished) match {
              case (true, _, _) => Redirect(controllers.charity.routes.GiftAidLandOrPropertyAmountController.show(taxYear))
              case (false, Some(false), _) =>
                redirectToCya(taxYear)
              case (_,_ ,true) => redirectToCya(taxYear)
              case _ => Redirect(controllers.charity.routes.GiftAidSharesSecuritiesLandPropertyOverseasController.show(taxYear))
            }

            giftAidSessionService.updateSessionData(updatedModel, taxYear)(
              InternalServerError(errorHandler.internalServerErrorTemplate)
            )(
              redirectLocation
            )
        })
        case _ =>
          logger.info("[GiftAidLandOrPropertyController][submit] No CYA data in session. Redirecting to overview page.")
          Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }.flatten
  }

  private def updatedCya(yesOrNoResult: Boolean, cyaData: GiftAidCYAModel): GiftAidCYAModel = {
    if (!yesOrNoResult & cyaData.donatedSharesOrSecurities.contains(false)) {
      resetDonatedSharesSecuritiesLandOrProperty(cyaData)
    } else {
      cyaData.copy(
        donatedLandOrProperty = Some(yesOrNoResult),
        donatedLandOrPropertyAmount = if (yesOrNoResult) cyaData.donatedLandOrPropertyAmount else None
      )
    }
  }
}

