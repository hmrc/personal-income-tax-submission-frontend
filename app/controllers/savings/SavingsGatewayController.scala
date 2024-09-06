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

package controllers.savings


import config.{AppConfig, ErrorHandler, SAVINGS}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import forms.YesNoForm
import models.savings.SavingsIncomeCYAModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.SavingsSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.savings.SavingsGatewayView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SavingsGatewayController @Inject()(
                                          view: SavingsGatewayView,
                                          savingsSessionService: SavingsSessionService,
                                          errorHandler: ErrorHandler
                                        )
                                        (
                                          implicit appConfig: AppConfig,
                                          mcc: MessagesControllerComponents,
                                          ec: ExecutionContext,
                                          authorisedAction: AuthorisedAction
                                        ) extends FrontendController(mcc) with I18nSupport {

  def form(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    s"savings.gateway.error.${if (isAgent) "agent" else "individual"}")


  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, SAVINGS).async { implicit user =>
    savingsSessionService.getSessionData(taxYear).flatMap {
      case Left(_) => Future.successful(errorHandler.internalServerError())
      case Right(cya) =>
        Future.successful(cya.fold(Ok(view(form(user.isAgent), taxYear))) {
          cyaData =>
            cyaData.savingsIncome.fold(Ok(view(form(user.isAgent), taxYear))) {
              data =>
                data.gateway match {
                  case None => Ok(view(form(user.isAgent), taxYear))
                  case Some(value) => Ok(view(form(user.isAgent).fill(value), taxYear))
                }
            }
        })
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, SAVINGS).async { implicit user =>
    form(user.isAgent).bindFromRequest().fold(
      formWithErrors => {
        Future.successful(BadRequest(view(formWithErrors, taxYear)))
      }, {
      yesNoValue =>
        savingsSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>
          (cya, prior) match {
            case (cya, prior) =>
              val savingsCya = cya.getOrElse(SavingsIncomeCYAModel()).copy(gateway = Some(yesNoValue))

              if (cya.nonEmpty) {
                savingsSessionService.updateSessionData(savingsCya, taxYear)(errorHandler.internalServerError()) {
                  if(savingsCya.isFinished) {
                    if (!appConfig.interestSavingsEnabled) {
                      Redirect(controllers.savings.routes.InterestSecuritiesCYAController.show(taxYear))
                    } else {
                      val hasNonZeroData = Seq(savingsCya.grossAmount, savingsCya.taxTakenOffAmount).exists(_.exists(_ != 0))

                      if (!yesNoValue && hasNonZeroData) {
                        Redirect(controllers.routes.ZeroingWarningController.show(taxYear, SAVINGS.stringify))
                      } else {
                        Redirect(controllers.savings.routes.InterestSecuritiesCYAController.show(taxYear))
                      }
                    }
                  } else {
                    redirectToNext(yesNoValue, taxYear, savingsCya.isFinished)
                  }
                }
              } else {
                savingsSessionService.createSessionData(savingsCya, taxYear)(errorHandler.internalServerError()) {
                  redirectToNext(yesNoValue, taxYear, savingsCya.isFinished)
                }
              }
            case _ =>
              Future.successful(Redirect(controllers.savings.routes.InterestSecuritiesCYAController.show(taxYear)))
          }
        }
      }
    )
  }

  def redirectToNext(yesNoValue: Boolean, taxYear: Int, isFinished: Boolean): Result = {
    if (yesNoValue && !isFinished) {
      Redirect(controllers.savingsBase.routes.SavingsInterestAmountBaseController.show(taxYear))
    } else {
      Redirect(controllers.savings.routes.InterestSecuritiesCYAController.show(taxYear))
    }
  }

}
