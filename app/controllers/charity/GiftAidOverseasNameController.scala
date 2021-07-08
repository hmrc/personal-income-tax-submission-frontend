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
import forms.charity.GiftAidOverseasNameForm
import models.User
import models.charity.prior.GiftAidSubmissionModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.libs.json.{Json, Reads}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.charity.GiftAidOverseasNameView
import javax.inject.Inject
import models.charity.GiftAidCYAModel
import play.api.Logging
import services.GiftAidSessionService

import scala.concurrent.{ExecutionContext, Future}

class GiftAidOverseasNameController @Inject()(
                                                implicit cc: MessagesControllerComponents,
                                                giftAidOverseasAmountController: GiftAidOverseasAmountController,
                                                authAction: AuthorisedAction,
                                                appConfig: AppConfig,
                                                view: GiftAidOverseasNameView,
                                                giftAidSessionService: GiftAidSessionService,
                                                errorHandler: ErrorHandler,
                                                ec: ExecutionContext
                                              ) extends FrontendController(cc) with I18nSupport with CharityJourney with Logging {

  override def handleRedirect(taxYear: Int, cya: GiftAidCYAModel, prior: Option[GiftAidSubmissionModel], fromShow: Boolean)
                             (implicit user: User[AnyContent]): Result = {

    cya.overseasDonationsViaGiftAidAmount match {
      case Some(_) => determineResult(
        Ok(view(taxYear, form(user.isAgent, cya))),
        Redirect(controllers.charity.routes.GiftAidOverseasNameController.show(taxYear)),
        fromShow)
      case _ => giftAidOverseasAmountController.handleRedirect(taxYear, cya, prior)
    }
  }

  def form(isAgent: Boolean, cya: GiftAidCYAModel): Form[String] = {
    val previousNames = cya.overseasCharityNames.getOrElse(Seq("")).toList
    GiftAidOverseasNameForm.giftAidOverseasNameForm(previousNames, isAgent)
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

    giftAidSessionService.getSessionData(taxYear).map {

      case Some(cyaData) =>
        cyaData.giftAid match {
          case Some(cyaModel) =>
            form(user.isAgent, cyaModel).bindFromRequest().fold({
              formWithErrors =>
                Future.successful(BadRequest(view(taxYear, formWithErrors)))
            }, {
              success =>
                giftAidSessionService.updateSessionData(cyaModel.copy(overseasCharityNames = Some(Seq(success))), taxYear)(
                  InternalServerError(errorHandler.internalServerErrorTemplate)
                )(
                  Redirect(controllers.charity.routes.OverseasGiftAidSummaryController.show(taxYear))
                )
            })
          case _ => Future.successful(redirectToOverview(taxYear))
        }
      case _ =>
        logger.info("[GiftAidOverseasNameController][submit] No CYA data in session. Redirecting to overview page.")
        Future.successful(redirectToOverview(taxYear))
    }.flatten
  }

  private[charity] def getSessionData[T](key: String)(implicit user: User[_], reads: Reads[T]): Option[T] = {
    user.session.get(key).flatMap { stringValue =>
      Json.parse(stringValue).asOpt[T]
    }
  }

}
