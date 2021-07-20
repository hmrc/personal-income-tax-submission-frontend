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
import javax.inject.{Inject, Singleton}
import models.User
import models.charity.GiftAidCYAModel
import models.charity.prior.GiftAidSubmissionModel
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.GiftAidSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.charity.OverseasGiftAidSummaryView

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OverseasGiftAidSummaryController @Inject()(overseasGiftAidSummaryView: OverseasGiftAidSummaryView)(
                                                 implicit cc: MessagesControllerComponents,
                                                 giftAidOverseasNameController: GiftAidOverseasNameController,
                                                 giftAidSessionService: GiftAidSessionService,
                                                 errorHandler: ErrorHandler,
                                                 ec: ExecutionContext,
                                                 authAction: AuthorisedAction,
                                                 appConfig: AppConfig
                                                ) extends FrontendController(cc) with I18nSupport with CharityJourney with Logging {

  override def handleRedirect(taxYear: Int, cya: GiftAidCYAModel, prior: Option[GiftAidSubmissionModel], fromShow: Boolean)
                             (implicit user: User[AnyContent]): Result = {

    cya.overseasCharityNames match {
      case Some(nameList) if nameList.nonEmpty => determineResult(
        Ok(overseasGiftAidSummaryView(yesNoForm, taxYear, nameList.toList)),
        Redirect(controllers.charity.routes.OverseasGiftAidSummaryController.show(taxYear)),
        fromShow)
      case _ => giftAidOverseasNameController.handleRedirect(taxYear, cya, prior)
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

    giftAidSessionService.getSessionData(taxYear).map(_.flatMap(_.giftAid)).map {
      case Some(cyaModel) =>
        yesNoForm.bindFromRequest().fold({
          formWithErrors =>
            cyaModel.overseasCharityNames match {
              case Some(namesList) => BadRequest(overseasGiftAidSummaryView(formWithErrors, taxYear, namesList.toList))
              case _ => redirectToOverview(taxYear)
            }
        }, {
          formAnswer =>
            val redirectLocation = if(formAnswer){
              controllers.charity.routes.GiftAidOverseasNameController.show(taxYear, None)
            } else {
              controllers.charity.routes.GiftAidLastTaxYearController.show(taxYear)
            }
            Redirect(redirectLocation)
        })
      case _ =>
        logger.info("[OverseasGiftAidDonationsController][submit] No CYA data in session. Redirecting to overview page.")
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }
  }

}
