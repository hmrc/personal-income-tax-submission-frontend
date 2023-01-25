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
import forms.YesNoForm
import models.User
import models.charity.GiftAidCYAModel
import models.charity.prior.GiftAidSubmissionModel
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.GiftAidSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.charity.OverseasSharesLandSummaryView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OverseasSharesLandSummaryController @Inject()(overseasSharesLandSummaryView: OverseasSharesLandSummaryView)(
  implicit cc: MessagesControllerComponents,
  authAction: AuthorisedAction,
  giftAidOverseasSharesNameController: GiftAidOverseasSharesNameController,
  giftAidSessionService: GiftAidSessionService,
  errorHandler: ErrorHandler,
  ec: ExecutionContext,
  appConfig: AppConfig
) extends FrontendController(cc) with I18nSupport with CharityJourney with Logging {

  override def handleRedirect(taxYear: Int, cya: GiftAidCYAModel, prior: Option[GiftAidSubmissionModel], fromShow: Boolean)
                             (implicit user: User[AnyContent]): Result = {

    if (cya.overseasDonatedSharesSecuritiesLandOrPropertyCharityNames.nonEmpty) {
      determineResult(
        Ok(overseasSharesLandSummaryView(yesNoForm, taxYear, cya.overseasDonatedSharesSecuritiesLandOrPropertyCharityNames)),
        Redirect(controllers.charity.routes.OverseasSharesLandSummaryController.show(taxYear)),
        fromShow)
    } else {
      giftAidOverseasSharesNameController.handleRedirect(taxYear, cya, prior)
    }
  }

  val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm("charity.overseas-gift-aid-summary.noChoice")

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
      case Left(_) => errorHandler.internalServerError()
      case Right(data) =>
        data.flatMap(_.giftAid) match {
      case Some(cyaData) =>
        yesNoForm.bindFromRequest().fold({
          formWithErrors =>
            BadRequest(overseasSharesLandSummaryView(formWithErrors, taxYear, cyaData.overseasDonatedSharesSecuritiesLandOrPropertyCharityNames))
        }, {
          success =>
            val redirectLocation = if (success) {
              controllers.charity.routes.GiftAidOverseasSharesNameController.show(taxYear, None)
            } else {
              controllers.charity.routes.GiftAidCYAController.show(taxYear)
            }
            Redirect(redirectLocation)
        })
      case _ =>
        logger.info("[OverseasSharesLandSummaryController][submit] No CYA data in session. Redirecting to overview page.")
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }
    }
  }
}
