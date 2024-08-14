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
import config.{AppConfig, DIVIDENDS, ErrorHandler, STOCK_DIVIDENDS}
import controllers.predicates.AuthorisedAction
import models.dividends.{DividendsPriorSubmission, StockDividendModel, StockDividendsCheckYourAnswersModel, StockDividendsPriorSubmission}
import models.mongo.StockDividendsUserDataModel
import models.priorDataModels.{IncomeSourcesModel, StockDividendsPriorDataModel}
import models.{APIErrorBodyModel, APIErrorModel, User}
import play.api.i18n.I18nSupport
import play.api.i18n.Lang.logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{DividendsSessionService, ExcludeJourneyService, StockDividendsSessionService, StockDividendsSessionServiceProvider, StockDividendsSubmissionService}
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
                                           stockDividendsSession: StockDividendsSessionServiceProvider,
                                           auditService: AuditService,
                                           submissionService: StockDividendsSubmissionService,
                                           excludeJourneyService: ExcludeJourneyService)
                                          (implicit appConfig: AppConfig, mcc: MessagesControllerComponents, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit request =>
    dividendsSession.getPriorData(taxYear).flatMap {
      case Left(_) => Future.successful(errorHandler.internalServerError())
      case Right(dividendsPrior: IncomeSourcesModel) =>
        //in case of no data `dividendsPrior.dividends` will be None
        getStockDividends(taxYear, dividendsPrior.dividends)
    }
  }

  private def getStockDividends(taxYear: Int, dividendsPriorData: Option[DividendsPriorSubmission])
                               (implicit request: User[AnyContent]): Future[Result] = {
    stockDividendsSession.getAndHandle(taxYear)(Future.successful(errorHandler.internalServerError())) { (cya, stockDividendsUserDataModel) =>
      val stockDividends = stockDividendsUserDataModel
        .flatMap(_.stockDividends)
        .map(_.toStockDividendsPriorDataModel)
        .getOrElse(StockDividendsPriorDataModel())

      val mergedDividends = stockDividends.copy(
        ukDividendsAmount = dividendsPriorData.flatMap(_.ukDividends),
        otherUkDividendsAmount = dividendsPriorData.flatMap(_.otherUkDividends)
      )

      if (mergedDividends.isDefined) {
        getStockDividendsCya(taxYear, cya, Some(mergedDividends))
      } else {
        getStockDividendsCya(taxYear, cya, None)
      }
    }.flatten
  }

  private def getStockDividendsCya(taxYear: Int,
                                   stockDividendsCheckYourAnswersModel: Option[StockDividendsCheckYourAnswersModel],
                                   prior: Option[StockDividendsPriorDataModel])
                                  (implicit request: User[AnyContent]): Future[Result] = {
    StockDividendsCheckYourAnswersModel.getCyaModel(stockDividendsCheckYourAnswersModel, prior) match {
      case Some(cyaData) if cyaData.gateway.contains(false) => handleSession(cyaData, taxYear, needsCreating = stockDividendsCheckYourAnswersModel.isEmpty)
      case Some(cyaData) if !cyaData.isFinished => Future.successful(handleUnfinishedRedirect(cyaData, taxYear))
      case Some(cyaData) => handleSession(cyaData, taxYear, needsCreating = stockDividendsCheckYourAnswersModel.isEmpty)
      case _ =>
        logger.info("[DividendsSummaryController][show] No CYA data in session. Redirecting to the overview page.")
        Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit request =>
    stockDividendsSession.getAndHandle(taxYear)(Future.successful(errorHandler.internalServerError())) { (cyaData, priorData) =>
      if (appConfig.dividendsTailoringEnabled && cyaData.flatMap(_.gateway).contains(false)) {
        auditTailorRemoveIncomeSources(
          TailorRemoveIncomeSourcesAuditDetail(
            nino = request.nino,
            mtditid = request.mtditid,
            userType = request.affinityGroup.toLowerCase,
            taxYear = taxYear,
            body = TailorRemoveIncomeSourcesBody(Seq(DIVIDENDS.stringify))
          )
        )
        excludeJourneyService.excludeJourney(DIVIDENDS.stringify, taxYear, request.nino).flatMap {
          case Right(_) => performSubmission(taxYear, cyaData, priorData.flatMap(_.stockDividends))
          case Left(_) => errorHandler.futureInternalServerError()
        }
      } else {
        if (hasValuesToBeZeroed(cyaData, priorData.flatMap(_.stockDividends))) {
          Future.successful(Redirect(controllers.routes.ZeroingWarningController.show(taxYear, STOCK_DIVIDENDS.stringify)))
        } else {
          performSubmission(taxYear, cyaData, priorData.flatMap(_.stockDividends))
        }
      }
    }.flatten
  }

  private[controllers] def performSubmission(taxYear: Int, data: Option[StockDividendsCheckYourAnswersModel], priorData: Option[StockDividendsCheckYourAnswersModel])
                                            (implicit hc: HeaderCarrier, request: User[AnyContent]): Future[Result] = {
    val cya = data.getOrElse(StockDividendsCheckYourAnswersModel())
    val stockDividendsSubmission = priorData.getOrElse(StockDividendsCheckYourAnswersModel())
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
    }.flatMap {
      case Right(_) =>
        for {
          dividends <- dividendsSession.clear(taxYear)(errorHandler.internalServerError())(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
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

  private def auditTailorRemoveIncomeSources(details: TailorRemoveIncomeSourcesAuditDetail)
                                            (implicit hc: HeaderCarrier): Future[AuditResult] = {
    val event = AuditModel("TailorRemoveIncomeSources", "tailorRemoveIncomeSources", details)
    auditService.auditModel(event)
  }

  private def handleSession(cyaData: StockDividendsCheckYourAnswersModel, taxYear: Int, needsCreating: Boolean)
                           (implicit request: User[AnyContent]): Future[Result] = {
    stockDividendsSession.createOrUpdateSessionData(cyaData, taxYear, needsCreating)(errorHandler.internalServerError())(
      Ok(view(cyaData, taxYear))
    )
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

  private def hasValuesToBeZeroed(cyaData: Option[StockDividendsCheckYourAnswersModel], priorData: Option[StockDividendsCheckYourAnswersModel]): Boolean = {
    (priorData.exists(_.ukDividendsAmount.isDefined) && !cyaData.exists(_.ukDividendsAmount.isDefined)) ||
      (priorData.exists(_.otherUkDividendsAmount.isDefined) && !cyaData.exists(_.otherUkDividendsAmount.isDefined)) ||
      (priorData.exists(_.stockDividendsAmount.isDefined) && !cyaData.exists(_.stockDividendsAmount.isDefined)) ||
      (priorData.exists(_.redeemableSharesAmount.isDefined) && !cyaData.exists(_.redeemableSharesAmount.isDefined)) ||
      (priorData.exists(_.closeCompanyLoansWrittenOffAmount.isDefined) && !cyaData.exists(_.closeCompanyLoansWrittenOffAmount.isDefined))
  }

}
