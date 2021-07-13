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

import common.SessionValues
import config.{AppConfig, ErrorHandler, GIFT_AID}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import forms.charity.GiftAidOverseasSharesNameForm

import javax.inject.Inject
import models.User
import models.charity.GiftAidCYAModel
import models.charity.prior.GiftAidSubmissionModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.i18n.Lang.logger
import play.api.libs.json.{Json, Reads}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.GiftAidSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.charity.GiftAidOverseasSharesNameView

import scala.concurrent.{ExecutionContext, Future}

class GiftAidOverseasSharesNameController @Inject()(
                                                implicit cc: MessagesControllerComponents,
                                                authAction: AuthorisedAction,
                                                appConfig: AppConfig,
                                                view: GiftAidOverseasSharesNameView,
                                                giftAidSharesSecuritiesLandPropertyOverseasController: GiftAidSharesSecuritiesLandPropertyOverseasController,
                                                giftAidSessionService: GiftAidSessionService,
                                                errorHandler: ErrorHandler,
                                              ) extends FrontendController(cc) with I18nSupport with CharityJourney {

  implicit val executionContext: ExecutionContext = cc.executionContext

  override def handleRedirect(taxYear: Int, cya: GiftAidCYAModel, prior: Option[GiftAidSubmissionModel], fromShow: Boolean)
                             (implicit user: User[AnyContent]): Result = {
    (prior, cya.overseasDonatedSharesSecuritiesLandOrPropertyAmount) match {
      case (_, Some(amount)) => cya.overseasCharityNames.fold {
          Ok(view(taxYear, GiftAidOverseasSharesNameForm.giftAidOverseasSharesNameForm(List(), user.isAgent)))
        } {
          names => Ok(view(taxYear, GiftAidOverseasSharesNameForm.giftAidOverseasSharesNameForm(names.toList, user.isAgent)))
        }
      case (Some(priorData), None) => Redirect(controllers.charity.routes.GiftAidCYAController.show(taxYear))
      case _ => giftAidSharesSecuritiesLandPropertyOverseasController.handleRedirect(taxYear, cya, prior)
    }
  }


  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>
    giftAidSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>

      cya match {
        case Some(cyaData) => handleRedirect(taxYear, cyaData, prior, true)
        case _ => redirectToOverview(taxYear)
      }
    }

  }

  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, GIFT_AID)).async { implicit user =>
    giftAidSessionService.getSessionData(taxYear).map {

      case Some(cyaData) =>
        cyaData.giftAid match {
          case Some(cyaModel) =>
            lazy val form: Form[String] = GiftAidOverseasSharesNameForm.giftAidOverseasSharesNameForm(
              cyaModel.overseasDonatedSharesSecuritiesLandOrPropertyCharityNames.getOrElse(List()).toList, user.isAgent)
            form.bindFromRequest().fold({
              formWithErrors =>
                Future.successful(BadRequest(view(taxYear, formWithErrors)))
            }, {
              success =>
                giftAidSessionService.updateSessionData(cyaModel.copy(
                  overseasDonatedSharesSecuritiesLandOrPropertyCharityNames =
                    Some(cyaModel.overseasDonatedSharesSecuritiesLandOrPropertyCharityNames.getOrElse(Seq("")):+ success)), taxYear)(
                  InternalServerError(errorHandler.internalServerErrorTemplate)
                )(
                  Redirect(controllers.charity.routes.OverseasSharesLandSummaryController.show(taxYear))
                )
            })
          case _ => Future.successful(redirectToOverview(taxYear))
        }
      case _ =>
        logger.info("[GiftAidOverseasSharesNameController][submit] No CYA data in session. Redirecting to overview page.")
        Future.successful(redirectToOverview(taxYear))
    }.flatten
  }


}
