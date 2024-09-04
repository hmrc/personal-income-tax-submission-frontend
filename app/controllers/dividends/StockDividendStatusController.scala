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

package controllers.dividends

import config.{AppConfig, DIVIDENDS, ErrorHandler, STOCK_DIVIDENDS}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import forms.YesNoForm
import models.dividends.StockDividendsCheckYourAnswersModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.StockDividendsSessionServiceProvider
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.dividends.StockDividendStatusView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StockDividendStatusController @Inject()(
                                               implicit val cc: MessagesControllerComponents,
                                               authAction: AuthorisedAction,
                                               view: StockDividendStatusView,
                                               implicit val appConfig: AppConfig,
                                               ec: ExecutionContext,
                                               errorHandler: ErrorHandler,
                                               session: StockDividendsSessionServiceProvider
                                             ) extends FrontendController(cc) with I18nSupport {


  def form(implicit isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    s"dividends.stock-dividend-status.errors.noChoice.${if (isAgent) "agent" else "individual"}")

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, STOCK_DIVIDENDS).async { implicit user =>
    session.getSessionData(taxYear).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(sessionData) =>
        val valueCheck: Option[Boolean] = sessionData.flatMap(_.stockDividends.flatMap(_.stockDividends))

        valueCheck match {
          case None => Ok(view(form(user.isAgent), taxYear))
          case Some(value) => Ok(view(form(user.isAgent).fill(value), taxYear))
        }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, DIVIDENDS).async { implicit user =>
    form(user.isAgent).bindFromRequest().fold(formWithErrors => {
      Future.successful(BadRequest(view(formWithErrors, taxYear)))
    }, {
      yesNoValue =>
        session.getSessionData(taxYear).flatMap {
          case Left(_) => Future.successful(errorHandler.internalServerError())
          case Right(sessionData) =>
            val dividendsCya = if (yesNoValue) {
              sessionData.flatMap(_.stockDividends).getOrElse(StockDividendsCheckYourAnswersModel()).copy(stockDividends = Some(yesNoValue))
            } else {
              sessionData.flatMap(_.stockDividends).getOrElse(StockDividendsCheckYourAnswersModel())
                .copy(stockDividends = Some(yesNoValue), stockDividendsAmount = None)
            }
            val needsCreating = sessionData.forall(_.stockDividends.isEmpty)

            session.createOrUpdateSessionData(dividendsCya, taxYear, needsCreating)(errorHandler.internalServerError())(
              if (dividendsCya.isFinished) {
                Redirect(controllers.dividends.routes.DividendsSummaryController.show(taxYear))
              } else {
                if (yesNoValue) {
                  Redirect(controllers.dividendsBase.routes.StockDividendAmountBaseController.show(taxYear))
                } else {
                  Redirect(controllers.dividends.routes.RedeemableSharesStatusController.show(taxYear))
                }
              }
            )
        }
    })

  }
}
