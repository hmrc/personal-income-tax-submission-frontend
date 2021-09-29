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
import models.User
import models.charity.GiftAidCYAModel
import models.charity.GiftAidCYAModel.resetDonatedSharesSecuritiesLandOrProperty
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.i18n.Lang.logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.GiftAidSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.charity.GiftAidSharesSecuritiesLandPropertyConfirmationView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GiftAidSharesSecuritiesLandPropertyConfirmationController @Inject()(
                                                             implicit val cc: MessagesControllerComponents,
                                                             authAction: AuthorisedAction,
                                                             giftAidSharesSecuritiesLandPropertyConfirmationView: GiftAidSharesSecuritiesLandPropertyConfirmationView,
                                                             errorHandler: ErrorHandler,
                                                             giftAidSessionService: GiftAidSessionService,
                                                             implicit val appConfig: AppConfig
                                                           ) extends FrontendController(cc) with I18nSupport{

  implicit val executionContext: ExecutionContext = cc.executionContext

  def handleRedirect(taxYear: Int, cya: GiftAidCYAModel, charityType: String)
                             (implicit user: User[AnyContent]): Result = {

    (cya.donatedSharesOrSecurities, cya.donatedLandOrProperty) match {
      case (Some(_), _) => Ok(giftAidSharesSecuritiesLandPropertyConfirmationView(yesNoForm(user), taxYear, charityType))
      case _ => Redirect(controllers.charity.routes.GiftAidCYAController.show(taxYear))
    }
  }

  val yesNoForm: User[AnyContent] => Form[Boolean] = user => {
    val missingInputError = "charity.sharesSecuritiesLandPropertyConfirmation.errors.noChoice"
    YesNoForm.yesNoForm(missingInputError)
  }

  def show(taxYear: Int, charityType: String): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>
    giftAidSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>


      cya match {
        case Some(cyaData) => handleRedirect(taxYear, cyaData, charityType)
        case _ => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }
  }


  def submit(taxYear: Int, charityType: String): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, GIFT_AID)).async { implicit user =>


    giftAidSessionService.getAndHandle(taxYear)(errorHandler.futureInternalServerError()) { (cya, prior) =>
      (cya, prior) match {
        case (Some(cyaData), _) =>
          yesNoForm(user).bindFromRequest().fold({
            formWithErrors => Future.successful(BadRequest(giftAidSharesSecuritiesLandPropertyConfirmationView(formWithErrors, taxYear, charityType)))
          }, {
            yesOrNoResponse =>
              val updatedCya = getUpdatedCya(cyaData, yesOrNoResponse)

              val redirectLocation = if (yesOrNoResponse) {
                Redirect(controllers.charity.routes.GiftAidCYAController.show(taxYear))
              } else {
                if (cyaData.donatedLandOrProperty.isEmpty) {
                  Redirect(controllers.charity.routes.GiftAidQualifyingSharesSecuritiesController.show(taxYear))
                }else{
                  Redirect(controllers.charity.routes.GiftAidCYAController.show(taxYear))
                }
              }
              giftAidSessionService.updateSessionData(updatedCya, taxYear)(
                InternalServerError(errorHandler.internalServerErrorTemplate)
              )(
                redirectLocation
              )
          }
          )
        case _ =>
          logger.info("[GiftAidSharesSecuritiesLandPropertyConfirmationController][submit] No CYA data in session. Redirecting to overview page.")
          Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }.flatten
  }

  private def getUpdatedCya(cyaData: GiftAidCYAModel, yesOrNoResponse: Boolean) = {
    if (yesOrNoResponse) {
      resetDonatedSharesSecuritiesLandOrProperty(cyaData)
    } else {
      if (cyaData.donatedLandOrProperty.isEmpty){
        cyaData.copy(
          donatedSharesOrSecurities = None,
          donatedSharesOrSecuritiesAmount = None,
          donatedLandOrProperty = None,
          donatedLandOrPropertyAmount = None,
          overseasDonatedSharesSecuritiesLandOrProperty = None,
          overseasDonatedSharesSecuritiesLandOrPropertyAmount = None,
          overseasDonatedSharesSecuritiesLandOrPropertyCharityNames = Seq.empty
        )
      }
      else {
        cyaData
      }
    }
  }
}
