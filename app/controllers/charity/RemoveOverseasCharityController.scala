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
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.charity.RemoveOverseasCharityView
import javax.inject.Inject
import play.api.Logging
import services.GiftAidSessionService

import scala.concurrent.{ExecutionContext, Future}

class RemoveOverseasCharityController @Inject()(
                                                 implicit val cc: MessagesControllerComponents,
                                                 overseasGiftAidSummaryController: OverseasGiftAidSummaryController,
                                                 authAction: AuthorisedAction,
                                                 removeOverseasCharityView: RemoveOverseasCharityView,
                                                 giftAidSessionService: GiftAidSessionService,
                                                 errorHandler: ErrorHandler,
                                                 ec: ExecutionContext,
                                                 implicit val appConfig: AppConfig
                                               ) extends FrontendController(cc) with I18nSupport with SessionHelper with Logging {

  val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm("charity.remove-overseas-charity.noChoice")

  def show(taxYear: Int, charityType: String, charityName: String): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>

    giftAidSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>

      cya match {

        case Some(cyaData) => cyaData.overseasCharityNames match {

          case Some(namesList) if namesList.contains(charityName) =>
            val isLast = namesList.forall(_ == charityName)
            Ok(removeOverseasCharityView(yesNoForm, taxYear, charityType, charityName, isLast = isLast))

          case _ =>
            overseasGiftAidSummaryController.handleRedirect(taxYear, cyaData, prior)
        }
        case _ => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }
  }


  def submit(taxYear: Int, charityType: String, charityName: String): Action[AnyContent] =

    (authAction andThen journeyFilterAction(taxYear, GIFT_AID)).async { implicit user =>

      giftAidSessionService.getSessionData(taxYear).map {

        case Some(model) =>
          yesNoForm.bindFromRequest().fold({
            formWithErrors =>
              model.giftAid.flatMap(_.overseasCharityNames) match {
                case Some(names) =>
                  BadRequest(
                    removeOverseasCharityView(formWithErrors, taxYear, charityType, charityName, names.forall(_ == charityName))
                  )
                case _ => errorHandler.internalServerError()
              }
          }, {
            yesNoForm =>
              model.giftAid match {
                case Some(cya) =>
                  if(yesNoForm && cya.overseasCharityNames.exists(_.forall(_ == charityName))) {
                    Redirect(controllers.charity.routes.GiftAidLastTaxYearController.show(taxYear))
                  } else {
                    Redirect(controllers.charity.routes.OverseasGiftAidSummaryController.show(taxYear))
                  }
                case _ => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
              }
          }
          )
        case _ =>
          logger.info("[GiftAidOneOffAmountController][submit] No CYA data in session. Redirecting to overview page.")
          Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
  }
}
