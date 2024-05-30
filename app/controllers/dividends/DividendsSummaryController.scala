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

import audit._
import config.{AppConfig, DIVIDENDS, ErrorHandler}
import controllers.predicates.AuthorisedAction
import models.dividends.{DividendsPriorSubmission, StockDividendModel, StockDividendsCheckYourAnswersModel, StockDividendsPriorSubmission}
import models.mongo.StockDividendsUserDataModel
import models.priorDataModels.StockDividendsPriorDataModel
import models.{APIErrorBodyModel, APIErrorModel, User}
import play.api.i18n.I18nSupport
import play.api.i18n.Lang.logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{DividendsSessionService, ExcludeJourneyService, StockDividendsSessionService, StockDividendsSubmissionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.dividends.DividendsSummaryView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class DividendsSummaryController @Inject()(authorisedAction: AuthorisedAction,
                                           view: DividendsSummaryView,
                                           errorHandler: ErrorHandler,
                                           dividendsSession: DividendsSessionService,
                                           stockDividendsSession: StockDividendsSessionService,
                                           auditService: AuditService,
                                           submissionService: StockDividendsSubmissionService,
                                           excludeJourneyService: ExcludeJourneyService)
                                          (implicit appConfig: AppConfig, user: User[_],
                                           hc: HeaderCarrier, mcc: MessagesControllerComponents, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport {


  def show(taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit request =>
    dividendsSession.getPriorData(taxYear)(user, hc).flatMap {
      case Left(_) => Future.successful(errorHandler.internalServerError())
      case Right(dividendsPrior) => dividendsPrior.dividends match {
            case Some(dividendsPriorData) =>
              stockDividendsSession.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, stockDividendsPrior) =>
                val mergedDividends = stockDividendsPrior match {
                  case Some(stockDividendsPriorData) => stockDividendsPriorData.copy(
                    ukDividendsAmount = dividendsPriorData.ukDividends,
                    otherUkDividendsAmount = dividendsPriorData.otherUkDividends
                  )
                  case None => StockDividendsPriorDataModel()
                }
                getStockDividendsCya(taxYear, cya, Some(mergedDividends))
              }
            case None =>
              getStockDividends(taxYear)
          }
    }
  }

  private def getStockDividends(taxYear: Int) = {
    stockDividendsSession.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, stockDividendsPrior) =>
      getStockDividendsCya(taxYear, cya, stockDividendsPrior)
    }
  }

  private def getStockDividendsCya(taxYear: Int, cya: Option[StockDividendsUserDataModel], prior: Option[StockDividendsPriorDataModel]) = {
    StockDividendsCheckYourAnswersModel.getCyaModel(cya.flatMap(_.stockDividends), prior) match {
      case Some(cyaData) if cyaData.gateway.contains(false) => handleSession(cya, cyaData, taxYear)
      case Some(cyaData) if !cyaData.isFinished => Future.successful(handleUnfinishedRedirect(cyaData, taxYear))
      case Some(cyaData) => handleSession(cya, cyaData, taxYear)
      case _ =>
        logger.info("[DividendsSummaryController][show] No CYA data in session. Redirecting to the overview page.")
        Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit request =>
    stockDividendsSession.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cyaData, priorData) =>
      if (appConfig.dividendsTailoringEnabled && cyaData.flatMap(_.stockDividends).flatMap(_.gateway).contains(false)) {
        auditTailorRemoveIncomeSources(TailorRemoveIncomeSourcesAuditDetail(
          nino = request.nino,
          mtditid = request.mtditid,
          userType = request.affinityGroup.toLowerCase,
          taxYear = taxYear,
          body = TailorRemoveIncomeSourcesBody(Seq(DIVIDENDS.stringify))
        ))
        excludeJourneyService.excludeJourney(DIVIDENDS.stringify, taxYear, request.nino).flatMap {
          case Right(_) => performSubmission(taxYear, cyaData, priorData)
          case Left(_) => errorHandler.futureInternalServerError()
        }
      } else {
        performSubmission(taxYear, cyaData, priorData)
      }
    }
  }

  private[controllers] def performSubmission(taxYear: Int, data: Option[StockDividendsUserDataModel], priorData: Option[StockDividendsPriorDataModel])
                                            (implicit hc: HeaderCarrier, request: User[AnyContent]): Future[Result] = {
    (data match {
      case Some(data) =>
        val cya = data.stockDividends.getOrElse(StockDividendsCheckYourAnswersModel())
        val stockDividendsSubmission = priorData.getOrElse(StockDividendsPriorDataModel())
        print("********cya: "+cya)
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
        logger.info("[DividendsSummaryController][submit] CYA data or NINO missing from session.")
        Future.successful(Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("MISSING_DATA", "CYA data or NINO missing from session."))))
    }).flatMap {
      case Right(_) =>
        stockDividendsSession.clear(taxYear)(errorHandler.internalServerError())(
          Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        )
      case Left(error) => Future.successful(errorHandler.handleError(error.status))
    }
  }

  private def auditSubmission(details: CreateOrAmendDividendsAuditDetail)
                             (implicit hc: HeaderCarrier): Future[AuditResult] = {
    val event = AuditModel("CreateOrAmendDividendsUpdate", "create-or-amend-dividends-update", details)
    auditService.auditModel(event)
  }

  private def auditTailorRemoveIncomeSources(details: TailorRemoveIncomeSourcesAuditDetail)
                                            (implicit hc: HeaderCarrier): Future[AuditResult] = {
    val event = AuditModel("TailorRemoveIncomeSources", "tailorRemoveIncomeSources", details)
    auditService.auditModel(event)
  }

  private def handleSession(sessionData: Option[StockDividendsUserDataModel], cyaData: StockDividendsCheckYourAnswersModel, taxYear: Int)
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