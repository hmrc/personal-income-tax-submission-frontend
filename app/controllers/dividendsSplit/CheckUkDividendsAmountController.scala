/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.dividendsSplit

import audit.{AuditModel, AuditService, CreateOrAmendDividendsAuditDetail}
import common.IncomeSources
import config.{AppConfig, ErrorHandler}
import connectors.IncomeSourceConnector
import controllers.predicates.AuthorisedAction
import controllers.routes
import models.dividends.{DividendsPriorSubmission, StockDividendModel, StockDividendsCheckYourAnswersModel, StockDividendsPriorSubmission}
import models.priorDataModels.StockDividendsPriorDataModel
import models.{APIErrorBodyModel, APIErrorModel, Journey, User}
import play.api.i18n.I18nSupport
import play.api.i18n.Lang.logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{DividendsSessionService, StockDividendsSessionServiceProvider, StockDividendsSubmissionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.dividends.CheckUkDividendsAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckUkDividendsAmountController @Inject()(authorisedAction: AuthorisedAction,
                                                 view: CheckUkDividendsAmountView,
                                                 errorHandler: ErrorHandler,
                                                 dividendsSession: DividendsSessionService,
                                                 stockDividendsSession: StockDividendsSessionServiceProvider,
                                                 auditService: AuditService,
                                                 submissionService: StockDividendsSubmissionService,
                                                 incomeSourceConnector: IncomeSourceConnector)
                                                (implicit appConfig: AppConfig, mcc: MessagesControllerComponents, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport {

  private def submissionUrl(taxYear: Int): String = {
    if (appConfig.sectionCompletedQuestionEnabled) {
      routes.SectionCompletedStateController.show(taxYear, Journey.CashDividends.entryName).url
    } else {
      s"${appConfig.incomeTaxSubmissionBaseUrl}/$taxYear/tasklist"
    }
  }

  def show(taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit request =>
    getStockDividends(taxYear)
  }

  private def getStockDividends(taxYear: Int)
                               (implicit request: User[AnyContent]): Future[Result] = {
    incomeSourceConnector.put(taxYear, request.nino, IncomeSources.DIVIDENDS)(hc.withExtraHeaders("mtditid" -> request.mtditid)).flatMap {
      case Left(_) => Future.successful(errorHandler.internalServerError())
      case Right(_) =>
        stockDividendsSession.getAndHandle(taxYear)(errorHandler.futureInternalServerError()) { (cya, stockDividendsPrior) =>
          if (stockDividendsPrior.isDefined) {
            getStockDividendsCya(taxYear, cya, stockDividendsPrior)
          } else {
            getStockDividendsCya(taxYear, cya, None)
          }
        }
    }
  }

  private def getStockDividendsCya(taxYear: Int,
                                   cya: Option[StockDividendsCheckYourAnswersModel],
                                   prior: Option[StockDividendsPriorDataModel])
                                  (implicit request: User[AnyContent]): Future[Result] = {
    StockDividendsCheckYourAnswersModel.getCyaModel(cya, prior) match {
      case Some(cyaData) => handleSession(cya, cyaData, taxYear)
      case _ =>
        logger.info("[CheckUkDividendsAmountController][show] No CYA data in session. Redirecting to the task list.")
        Future.successful(Redirect(s"${appConfig.incomeTaxSubmissionBaseUrl}/$taxYear/tasklist"))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit request =>
    stockDividendsSession.getAndHandle(taxYear)(errorHandler.futureInternalServerError()) { (cyaData, priorData) =>
      performSubmission(taxYear, cyaData, priorData)
    }
  }

  private[controllers] def performSubmission(taxYear: Int,
                                             cyaData: Option[StockDividendsCheckYourAnswersModel],
                                             priorData: Option[StockDividendsPriorDataModel])
                                            (implicit hc: HeaderCarrier, request: User[AnyContent]): Future[Result] = {
    (cyaData match {
      case Some(cya) =>
        val stockDividendsSubmission = priorData.getOrElse(StockDividendsPriorDataModel())
        submissionService.submitDividends(cya, request.nino, taxYear).map {
          case response@Right(_) =>
            val model = CreateOrAmendDividendsAuditDetail.createFromStockCyaData(
              cya, Some(DividendsPriorSubmission(stockDividendsSubmission.ukDividendsAmount, stockDividendsSubmission.otherUkDividendsAmount)),
              Some(StockDividendsPriorSubmission(None, None, None, Some(StockDividendModel(None, stockDividendsSubmission.stockDividendsAmount.getOrElse(0))),
                Some(StockDividendModel(None, stockDividendsSubmission.redeemableSharesAmount.getOrElse(0))), None,
                Some(StockDividendModel(None, stockDividendsSubmission.closeCompanyLoansWrittenOffAmount.getOrElse(0))))),
              priorData.isDefined, request.nino, request.mtditid, request.affinityGroup.toLowerCase, taxYear
            )
            auditSubmission(model)
            response
          case response => response
        }
      case _ =>
        logger.info("[CheckUkDividendsAmountController][submit] CYA data or NINO missing from session.")
        Future.successful(Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("MISSING_DATA", "CYA data or NINO missing from session."))))
    }).flatMap {
      case Right(_) =>
        for {
          dividends <-
            dividendsSession.clear(taxYear)(errorHandler.internalServerError())(Redirect(submissionUrl(taxYear)))
          stockDividends <- stockDividendsSession.clear(taxYear)(errorHandler.internalServerError())(dividends)
        } yield {
          stockDividends
        }
      case Left(error) => Future.successful(errorHandler.handleError(error.status))
    }
  }

  private def auditSubmission(details: CreateOrAmendDividendsAuditDetail)
                             (implicit hc: HeaderCarrier): Future[AuditResult] = {
    val event = AuditModel("CreateOrAmendDividendsUpdate", "create-or-amend-dividends-update", details)
    auditService.auditModel(event)
  }

  private def handleSession(sessionData: Option[StockDividendsCheckYourAnswersModel], cyaData: StockDividendsCheckYourAnswersModel, taxYear: Int)
                           (implicit request: User[AnyContent]): Future[Result] = {
    if (sessionData.isDefined) {
      stockDividendsSession.updateSessionData(cyaData, taxYear)(errorHandler.internalServerError())(
        Ok(view(cyaData, taxYear))
      )
    } else {
      stockDividendsSession.createSessionData(cyaData, taxYear)(errorHandler.internalServerError())(
        Ok(view(cyaData, taxYear))
      )
    }
  }

}
