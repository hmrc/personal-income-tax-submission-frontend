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

import config.{AppConfig, ErrorHandler}
import controllers.predicates.AuthorisedAction
import models.dividends.StockDividendsCheckYourAnswersModel
import play.api.i18n.I18nSupport
import play.api.i18n.Lang.logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{StockDividendsSessionService, StockDividendsSubmissionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.dividends.DividendsSummaryView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DividendsSummaryController @Inject()(authorisedAction: AuthorisedAction,
                                           view: DividendsSummaryView,
                                           errorHandler: ErrorHandler,
                                           session: StockDividendsSessionService,
                                           submissionService: StockDividendsSubmissionService)
                                          (implicit appConfig: AppConfig, mcc: MessagesControllerComponents, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport {


  def show(taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit request =>
    session.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>
      StockDividendsCheckYourAnswersModel.getCyaModel(cya.flatMap(_.stockDividends), prior) match {
        case Some(cyaData) if !cyaData.isFinished => Future.successful(handleUnfinishedRedirect(cyaData, taxYear))
        case Some(cyaData) => Future.successful(Ok(view(cyaData, taxYear)))
        case _ =>
          logger.info("[DividendsSummaryController][show] No CYA data in session. Redirecting to the overview page.")
          Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit request =>
    session.getSessionData(taxYear).flatMap {
      case Left(_) => Future(errorHandler.internalServerError())
      case Right(data) =>
        val cya = data.flatMap(_.stockDividends).getOrElse(StockDividendsCheckYourAnswersModel())

        submissionService.submitDividends(cya, request.nino, taxYear).map {
          case Left(error) => errorHandler.handleError(error.status)
          case Right(_) => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        }
    }
  }

  private[dividends] def handleUnfinishedRedirect(cya: StockDividendsCheckYourAnswersModel, taxYear: Int): Result = {
    StockDividendsCheckYourAnswersModel.unapply(cya).getOrElse((None, None, None, None, None, None, None, None, None, None, None)) match {
      case (_, Some(true), None, _, _, _, _, _, _, _, _) => Redirect(controllers.dividends.routes.UkDividendsAmountController.show(taxYear))
      case (_, _, _, Some(true), None, _, _, _, _, _, _) => Redirect(controllers.dividends.routes.OtherUkDividendsAmountController.show(taxYear))
      case (_, _, _, _, _, Some(true), None, _, _, _, _) => Redirect(controllers.dividends.routes.StockDividendAmountController.show(taxYear))
      case (_, _, _, _, _, _, _, Some(true), None, _, _) => Redirect(controllers.dividends.routes.RedeemableSharesAmountController.show(taxYear))
      case (_, _, _, _, _, _, _, _, _, Some(true), None) => Redirect(controllers.dividends.routes.CloseCompanyLoanAmountController.show(taxYear))
      case _ => Redirect(controllers.dividends.routes.DividendsGatewayController.show(taxYear))
    }
  }
}