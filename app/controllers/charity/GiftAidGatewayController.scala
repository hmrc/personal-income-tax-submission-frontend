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
import forms.YesNoForm
import models.User
import models.charity.GiftAidCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.GiftAidSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.charity.GiftAidGatewayView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GiftAidGatewayController @Inject()(
                                          view: GiftAidGatewayView,
                                          session: GiftAidSessionService,
                                          errorHandler: ErrorHandler
                                        )
                                        (
                                          implicit appConfig: AppConfig,
                                          mcc: MessagesControllerComponents,
                                          ec: ExecutionContext,
                                          authorisedAction: AuthorisedAction
                                        ) extends FrontendController(mcc) with I18nSupport {

  private[charity] val form: Boolean => Form[Boolean] = isAgent => YesNoForm.yesNoForm("charity.gateway.error." + (if (isAgent) "agent" else "individual"))

  private def internalError(implicit user: User[_]): Result = errorHandler.internalServerError()

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>
    if (appConfig.charityTailoringEnabled) {
      session.getSessionData(taxYear).map {
        case Left(_) => errorHandler.internalServerError()
        case Right(sessionData) =>

          val gatewayCheck: Option[Boolean] = sessionData.flatMap(_.giftAid.flatMap(_.gateway))

          gatewayCheck match {
            case None => Ok(view(form(user.isAgent), taxYear))
            case Some(checkValue) => Ok(view(form(user.isAgent).fill(checkValue), taxYear))
          }
      }
    } else {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }

  def createOrUpdateGiftAidData(
                                 giftAidCyaModel: GiftAidCYAModel,
                                 taxYear: Int,
                                 isUpdate: Boolean
                               )(redirect: Result)(implicit user: User[_]): Future[Result] = {
    if (isUpdate) {
      session.updateSessionData(giftAidCyaModel, taxYear)(internalError)(redirect)
    } else {
      session.createSessionData(giftAidCyaModel, taxYear)(internalError)(redirect)
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).async { implicit user =>
    if (appConfig.charityTailoringEnabled) {
      form(user.isAgent).bindFromRequest().fold(formWithErrors => {
        Future.successful(BadRequest(view(formWithErrors, taxYear)))
      }, {
        yesNoValue =>
          session.getAndHandle(taxYear)(Future.successful(errorHandler.internalServerError())) {
            case (sessionData, prior) =>
              val update = sessionData.nonEmpty
              val giftAidCya = { if (prior.isEmpty && !yesNoValue) {GiftAidCYAModel().copy(gateway = Some(yesNoValue))}
                else {sessionData.getOrElse(GiftAidCYAModel()).copy(gateway = Some(yesNoValue))}
              }
              createOrUpdateGiftAidData(giftAidCya, taxYear, update)(
                if (giftAidCya.isFinished) {
                  if (!appConfig.charityTailoringEnabled || (appConfig.charityTailoringEnabled && sessionData.isEmpty)) {
                    Redirect(controllers.charity.routes.GiftAidCYAController.show(taxYear))
                  }
                  else {
                    if (giftAidCya == giftAidCya.zeroData) {
                      Redirect(controllers.charity.routes.GiftAidCYAController.show(taxYear))
                    }
                    else {
                      Redirect(controllers.routes.ZeroingWarningController.show(taxYear, GIFT_AID.stringify))
                    }
                  }
                } else {
                  Redirect(controllers.charity.routes.GiftAidDonationsController.show(taxYear))
                }
              )
          }.flatten
      })
    } else {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }

  }
}

