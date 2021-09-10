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

import common.OverseasCharityTaxTypes._
import config.{AppConfig, ErrorHandler, GIFT_AID}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import forms.YesNoForm
import models.charity.{CharityNameModel, GiftAidCYAModel}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.GiftAidSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.charity.RemoveOverseasCharityView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveOverseasCharityController @Inject()(
                                                 implicit val cc: MessagesControllerComponents,
                                                 overseasGiftAidSummaryController: OverseasGiftAidSummaryController,
                                                 overseasSharesLandSummaryController: OverseasSharesLandSummaryController,
                                                 authAction: AuthorisedAction,
                                                 removeOverseasCharityView: RemoveOverseasCharityView,
                                                 giftAidSessionService: GiftAidSessionService,
                                                 errorHandler: ErrorHandler,
                                                 ec: ExecutionContext,
                                                 implicit val appConfig: AppConfig
                                               ) extends FrontendController(cc) with I18nSupport with SessionHelper with Logging {

  val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm("charity.remove-overseas-charity.noChoice")

  def show(taxYear: Int, charityType: String, charityNameId: String): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>

    giftAidSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>

      cya match {
        case Some(cyaData) =>
          val charityNames = if (charityType == GIFTAID) cyaData.overseasCharityNames else cyaData.overseasDonatedSharesSecuritiesLandOrPropertyCharityNames
          val redirectLocation = if (charityType == GIFTAID) {
            overseasGiftAidSummaryController.handleRedirect(taxYear, cyaData, prior)
          } else {
            overseasSharesLandSummaryController.handleRedirect(taxYear, cyaData, prior)
          }

          if (charityNames.map(_.id).contains(charityNameId)) {
            val isLast = charityNames.map(_.id).forall(_ == charityNameId)
            val charityNameModel = charityNames.find(charityNameModel => charityNameModel.id == charityNameId).get
            Ok(removeOverseasCharityView(yesNoForm, taxYear, charityType, charityNameModel, isLast = isLast))
          } else {
            redirectLocation
          }
        case _ => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }
  }


  def submit(taxYear: Int, charityType: String, charityNameId: String): Action[AnyContent] =
    (authAction andThen journeyFilterAction(taxYear, GIFT_AID)).async { implicit user =>
      giftAidSessionService.getSessionData(taxYear).map(_.flatMap(_.giftAid)).map {
        case Some(cyaModel) =>
          val charityNames = if (charityType == GIFTAID) cyaModel.overseasCharityNames else cyaModel.overseasDonatedSharesSecuritiesLandOrPropertyCharityNames

          yesNoForm.bindFromRequest().fold({
            formWithErrors =>
              val charityNameModel = charityNames.find(charityNameModel => charityNameModel.id == charityNameId).get
              Future.successful(BadRequest(
                removeOverseasCharityView(formWithErrors, taxYear, charityType, charityNameModel, charityNames.map(_.id).forall(_ == charityNameId))
              ))
          }, {
            yesNoForm =>
              val updatedModel = updatedCya(charityNames, charityNameId, charityType, cyaModel)
              val redirectLocation = redirect(yesNoForm, charityType, taxYear, charityNames.map(_.name))

              if (yesNoForm) {
                giftAidSessionService.updateSessionData(updatedModel, taxYear)(
                  InternalServerError(errorHandler.internalServerErrorTemplate)
                )(redirectLocation)
              } else {
                Future.successful(redirectLocation)
              }
          })
        case _ =>
          logger.info("[RemoveOverseasCharityController][submit] No CYA data in session. Redirecting to overview page.")
          Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }.flatten
    }

  def updatedCya(names: Seq[CharityNameModel], charityNameId: String, charityType: String, cyaModel: GiftAidCYAModel): GiftAidCYAModel = {
    charityType match {
      case GIFTAID =>
        cyaModel.copy(overseasCharityNames = names.filterNot(_.id == charityNameId),
          overseasDonationsViaGiftAid = if (names.length == 1) Some(false) else cyaModel.overseasDonationsViaGiftAid,
          overseasDonationsViaGiftAidAmount = if (names.length == 1) None else cyaModel.overseasDonationsViaGiftAidAmount
        )
      case SHARES =>
        cyaModel.copy(overseasDonatedSharesSecuritiesLandOrPropertyCharityNames = names.filterNot(_.id == charityNameId),
          overseasDonatedSharesSecuritiesLandOrProperty = if (names.length == 1) Some(false) else cyaModel.overseasDonatedSharesSecuritiesLandOrProperty,
          overseasDonatedSharesSecuritiesLandOrPropertyAmount = if (names.length == 1) None else cyaModel.overseasDonatedSharesSecuritiesLandOrPropertyAmount
        )
    }
  }

  def redirect(result: Boolean, charityType: String, taxYear: Int, names: Seq[String]): Result = {
    (result, charityType) match {
      case (true, GIFTAID) if names.length == 1 =>
        Redirect(controllers.charity.routes.GiftAidLastTaxYearController.show(taxYear))
      case (true, SHARES) if names.length == 1 =>
        Redirect(controllers.charity.routes.GiftAidCYAController.show(taxYear))
      case (_, GIFTAID) => Redirect(controllers.charity.routes.OverseasGiftAidSummaryController.show(taxYear))
      case _ => Redirect(controllers.charity.routes.OverseasSharesLandSummaryController.show(taxYear))
    }
  }
}
