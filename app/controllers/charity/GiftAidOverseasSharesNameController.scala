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
                                                     overseasSharesSecuritiesLandPropertyAmountController: OverseasSharesSecuritiesLandPropertyAmountController,
                                                     giftAidSessionService: GiftAidSessionService,
                                                     errorHandler: ErrorHandler,
                                                     ec: ExecutionContext
                                                   ) extends FrontendController(cc) with I18nSupport with CharityJourney {

  override def handleRedirect(taxYear: Int, cya: GiftAidCYAModel, prior: Option[GiftAidSubmissionModel], fromShow: Boolean)
                             (implicit user: User[AnyContent]): Result = {

    cya.overseasDonatedSharesSecuritiesLandOrPropertyAmount match {
      case Some(_) => determineResult(
        Ok(view(taxYear, form(user.isAgent, cya))),
        Redirect(controllers.charity.routes.GiftAidOverseasSharesNameController.show(taxYear, None)),
        fromShow)
      case _ => overseasSharesSecuritiesLandPropertyAmountController.handleRedirect(taxYear, cya, prior)
    }
  }

  def form(isAgent: Boolean, cya: GiftAidCYAModel): Form[String] = {
    val previousNames = cya.overseasDonatedSharesSecuritiesLandOrPropertyCharityNames.getOrElse(Seq("")).toList
    GiftAidOverseasSharesNameForm.giftAidOverseasSharesNameForm(previousNames, isAgent)
  }

  def show(taxYear: Int, changeCharity: Option[String]): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>

    giftAidSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>

      (cya, changeCharity) match {
        case (Some(cyaData), Some(name)) if cyaData.overseasDonatedSharesSecuritiesLandOrPropertyAmount.isDefined =>
          Ok(view(taxYear, form(user.isAgent, cyaData).fill(name), Some(name)))
        case (Some(cyaData), _) => handleRedirect(taxYear, cyaData, prior, fromShow = true)
        case _ => redirectToOverview(taxYear)
      }
    }
  }

  def submit(taxYear: Int, changeCharity: Option[String]): Action[AnyContent] =
    (authAction andThen journeyFilterAction(taxYear, GIFT_AID)).async { implicit user =>

      giftAidSessionService.getSessionData(taxYear).map(_.flatMap(_.giftAid)).map {
        case Some(cyaModel) =>
          form(user.isAgent, cyaModel).bindFromRequest().fold({
            formWithErrors =>
              Future.successful(BadRequest(view(taxYear, formWithErrors, changeCharity)))
          }, {
            formName =>
              val updatedCya = {
                (changeCharity, cyaModel.overseasDonatedSharesSecuritiesLandOrPropertyCharityNames) match {
                  case (Some(name), Some(namesList)) =>
                    cyaModel.copy(overseasDonatedSharesSecuritiesLandOrPropertyCharityNames = Some(namesList.filterNot(_ == name) :+ formName))
                  case (_, Some(namesList)) =>
                    cyaModel.copy(overseasDonatedSharesSecuritiesLandOrPropertyCharityNames = Some(namesList :+ formName))
                  case _ =>
                    cyaModel.copy(overseasDonatedSharesSecuritiesLandOrPropertyCharityNames = Some(Seq(formName)))
                }
              }

              giftAidSessionService.updateSessionData(updatedCya, taxYear)(
                InternalServerError(errorHandler.internalServerErrorTemplate)
              )(
                Redirect(controllers.charity.routes.OverseasSharesLandSummaryController.show(taxYear))
              )
          })
        case _ =>
          logger.info("[GiftAidOverseasSharesNameController][submit] No CYA data in session. Redirecting to overview page.")
          Future.successful(redirectToOverview(taxYear))
      }.flatten
    }


}
