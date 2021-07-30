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
                                                 overseasSharesLandSummaryController: OverseasSharesLandSummaryController,
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
        case Some(cyaData) =>
          val charityNames = if(charityType == GIFTAID) cyaData.overseasCharityNames else cyaData.overseasDonatedSharesSecuritiesLandOrPropertyCharityNames
          val redirectLocation = if(charityType == GIFTAID){
            overseasGiftAidSummaryController.handleRedirect(taxYear, cyaData, prior)
          } else {
            overseasSharesLandSummaryController.handleRedirect(taxYear, cyaData, prior)
          }

          charityNames match {
            case Some(namesList) if namesList.contains(charityName) =>
              val isLast = namesList.forall(_ == charityName)
              Ok(removeOverseasCharityView(yesNoForm, taxYear, charityType, charityName, isLast = isLast))

            case _ => redirectLocation
          }
        case _ => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }
  }


  def submit(taxYear: Int, charityType: String, charityName: String): Action[AnyContent] =
    (authAction andThen journeyFilterAction(taxYear, GIFT_AID)).async { implicit user =>
      giftAidSessionService.getSessionData(taxYear).map(_.flatMap(_.giftAid)).map {
        case Some(cyaModel) =>
          val charityNames = if(charityType == GIFTAID) cyaModel.overseasCharityNames else cyaModel.overseasDonatedSharesSecuritiesLandOrPropertyCharityNames

          yesNoForm.bindFromRequest().fold({
            formWithErrors =>
              charityNames match {
                case Some(names) =>
                  Future.successful(BadRequest(
                    removeOverseasCharityView(formWithErrors, taxYear, charityType, charityName, names.forall(_ == charityName))
                  ))
                case _ => Future.successful(errorHandler.internalServerError())
              }
          }, {
            yesNoForm =>
              charityNames.fold(
                Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
              ){
                seqNames =>
                  val updatedCya = if(charityType == GIFTAID){
                    cyaModel.copy(overseasCharityNames = Some(seqNames.filterNot(_ == charityName)))
                  } else {
                    cyaModel.copy(overseasDonatedSharesSecuritiesLandOrPropertyCharityNames = Some(seqNames.filterNot(_ == charityName)))
                  }

                  val redirectLocation = (yesNoForm, charityType) match {
                    case (true, GIFTAID) if seqNames.length == 1 =>
                      Redirect(controllers.charity.routes.GiftAidLastTaxYearController.show(taxYear))
                    case (true, SHARES) if seqNames.length == 1 =>
                      Redirect(controllers.charity.routes.GiftAidCYAController.show(taxYear))
                    case (_, GIFTAID) => Redirect(controllers.charity.routes.OverseasGiftAidSummaryController.show(taxYear))
                    case _ => Redirect(controllers.charity.routes.OverseasSharesLandSummaryController.show(taxYear))
                  }

                  if(yesNoForm){
                    giftAidSessionService.updateSessionData(updatedCya, taxYear)(
                      InternalServerError(errorHandler.internalServerErrorTemplate)
                    )(redirectLocation)
                  } else {
                    Future.successful(redirectLocation)
                  }
              }
          })
        case _ =>
          logger.info("[GiftAidOneOffAmountController][submit] No CYA data in session. Redirecting to overview page.")
          Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }.flatten
  }
}
