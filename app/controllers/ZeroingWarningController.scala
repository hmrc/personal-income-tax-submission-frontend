/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers

import config.{AppConfig, DIVIDENDS, ErrorHandler, GIFT_AID, INTEREST}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import models.User
import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents, Result}
import services.{DividendsSessionService, GiftAidSessionService, InterestSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.ZeroingWarningView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ZeroingWarningController @Inject()(
                                          view: ZeroingWarningView,
                                          dividendsSession: DividendsSessionService,
                                          interestSession: InterestSessionService,
                                          giftAidSession: GiftAidSessionService,
                                          errorHandler: ErrorHandler
                                        )(
                                          implicit appConfig: AppConfig,
                                          auth: AuthorisedAction,
                                          mcc: MessagesControllerComponents,
                                          ec: ExecutionContext
                                        ) extends FrontendController(mcc) with I18nSupport {

  private def page(taxYear: Int, journeyKey: String)(implicit user: User[_]) = {
    val (continueCall, cancelHref): (Call, String) = {
      journeyKey match {
        case "dividends" => (Call("GET", "#"), controllers.dividends.routes.DividendsGatewayController.show(taxYear).url)
        case "interest" => (Call("GET", "#"), controllers.interest.routes.InterestGatewayController.show(taxYear).url)
        case "charity" => (Call("GET", "#"), controllers.charity.routes.GiftAidGatewayController.show(taxYear).url)
      }
    }

    Ok(view(taxYear, journeyKey, continueCall, cancelHref))
  }

  private def zeroingPredicates(taxYear: Int, journeyKey: String) = {
    commonPredicates(taxYear, {
      journeyKey match {
        case "dividends" => DIVIDENDS
        case "interest" => INTEREST
        case "charity" => GIFT_AID
      }
    })
  }

  def show(taxYear: Int, journeyKey: String): Action[AnyContent] = zeroingPredicates(taxYear, journeyKey).apply { implicit user =>
    if (appConfig.tailoringEnabled) {
      page(taxYear, journeyKey)
    } else {
      Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }
  }

  def submit(taxYear: Int, journeyKey: String): Action[AnyContent] = zeroingPredicates(taxYear, journeyKey).async { implicit user =>
    if(appConfig.tailoringEnabled) {
      def onSuccess(key: String): Result = {
        Redirect(s"/$key") //TODO Update once the endpoint on income tax submission frontend is complete
      }

      journeyKey match {
        case key@"dividends" =>
          dividendsSession.clear(taxYear)(errorHandler.internalServerError())(onSuccess(key))
        case key@"interest" =>
          interestSession.clear(taxYear)(errorHandler.internalServerError())(onSuccess(key))
        case key@"charity" =>
          dividendsSession.clear(taxYear)(errorHandler.internalServerError())(onSuccess(key))
      }
    } else {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }

}
