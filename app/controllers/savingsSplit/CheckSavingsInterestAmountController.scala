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

package controllers.savingsSplit

import audit.{AuditModel, AuditService, CreateOrAmendDividendsAuditDetail, CreateOrAmendSavingsAuditDetail}
import common.IncomeSources
import config.{AppConfig, ErrorHandler}
import connectors.IncomeSourceConnector
import controllers.predicates.AuthorisedAction
import models.dividends.{DividendsPriorSubmission, StockDividendModel, StockDividendsCheckYourAnswersModel, StockDividendsPriorSubmission}
import models.priorDataModels.StockDividendsPriorDataModel
import models.savings.{SavingsIncomeCYAModel, SavingsIncomeDataModel}
import models.{APIErrorBodyModel, APIErrorModel, User}
import play.api.i18n.I18nSupport
import play.api.i18n.Lang.logger
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{DividendsSessionService, SavingsSessionService, SavingsSubmissionService, StockDividendsSessionServiceProvider, StockDividendsSubmissionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.dividends.CheckOtherUkDividendsAmountView
import views.html.savings.InterestSecuritiesCYAView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CheckSavingsInterestAmountController @Inject()(authorisedAction: AuthorisedAction,
                                                     view: InterestSecuritiesCYAView,
                                                     errorHandler: ErrorHandler,
                                                     savingsSessionService: SavingsSessionService,
                                                     auditService: AuditService,
                                                     savingsSubmissionService: SavingsSubmissionService,
                                                     incomeSourceConnector: IncomeSourceConnector)
                                                    (implicit appConfig: AppConfig, mcc: MessagesControllerComponents, ec: ExecutionContext)
  extends FrontendController(mcc) with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit request =>
    getSavings(taxYear)
  }

  private def getSavings(taxYear: Int)(implicit request: User[AnyContent]): Future[Result] = {
    incomeSourceConnector.put(taxYear, request.nino, IncomeSources.INTEREST_SAVINGS)(hc.withExtraHeaders("mtditid" -> request.mtditid)).flatMap {
      case Left(_) =>
        Future.successful(errorHandler.internalServerError())
      case Right(_) =>
        savingsSessionService.getAndHandle(taxYear)(errorHandler.futureInternalServerError()) { (cya, prior) =>
          if (prior.isDefined) {
            getSavingsCya(taxYear, cya, prior)
          } else {
            getSavingsCya(taxYear, cya, None)
          }
        }
    }
  }

  private def getSavingsCya(taxYear: Int, cya: Option[SavingsIncomeCYAModel], prior: Option[SavingsIncomeDataModel])
                           (implicit request: User[AnyContent]): Future[Result] = {

    cya match {
      case Some(cyaData) => handleSession(cya, cyaData, taxYear)
      case _ =>
        logger.info("[CheckSavingInterestAmountController][show] No CYA data in session. Redirecting to the task list.")
        Future.successful(Redirect(s"${appConfig.incomeTaxSubmissionBaseUrl}/$taxYear/tasklist"))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit request =>
    savingsSessionService.getAndHandle(taxYear)(errorHandler.futureInternalServerError()) { (cya, prior) =>
      performSubmission(taxYear, cya, prior)
    }
  }

  private[controllers] def performSubmission(taxYear: Int, cyaData: Option[SavingsIncomeCYAModel], prior: Option[SavingsIncomeDataModel])
                                            (implicit user: User[_], hc: HeaderCarrier, request: User[AnyContent]): Future[Result] = {
    (cyaData match {
      case Some(cya) =>
//        val stockDividendsSubmission = prior.getOrElse(StockDividendsPriorDataModel())
        savingsSubmissionService.submitSavings(Some(cya), prior, user.nino, user.mtditid, taxYear).map {
          case response@Right(_) =>
            val model = CreateOrAmendSavingsAuditDetail(
              cyaData.flatMap(_.gateway), cyaData.flatMap(_.grossAmount),
              cyaData.flatMap(_.taxTakenOff), cyaData.flatMap(_.taxTakenOffAmount),
              prior.flatMap(_.securities), prior.isDefined, user.nino, user.mtditid, user.affinityGroup.toLowerCase, taxYear
            )
            auditSubmission(model)
            response
          case response => response
        }
      case _ =>
        logger.info("[CheckSavingInterestAmountController][submit] CYA data or NINO missing from session.")
        Future.successful(Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("MISSING_DATA", "CYA data or NINO missing from session."))))
    }).flatMap {
      case Right(_) =>
        savingsSessionService.clear(taxYear)(errorHandler.internalServerError())(Redirect(s"${appConfig.incomeTaxSubmissionBaseUrl}/$taxYear/tasklist"))
      case Left(error) =>
        Future.successful(errorHandler.handleError(error.status))
    }
  }

  private def auditSubmission(details: CreateOrAmendSavingsAuditDetail)
                             (implicit hc: HeaderCarrier): Future[AuditResult] = {
    val event = AuditModel("CreateOrAmendSavingsUpdate", "create-or-amend-savings-update", details)
    auditService.auditModel(event)
  }

  private def handleSession(sessionData: Option[SavingsIncomeCYAModel], cyaData: SavingsIncomeCYAModel, taxYear: Int)
                           (implicit request: User[AnyContent]): Future[Result] = {
    if (sessionData.isDefined) {
      savingsSessionService.updateSessionData(cyaData, taxYear)(errorHandler.internalServerError())(
        Ok(view(cyaData, taxYear))
      )
    } else {
      savingsSessionService.createSessionData(cyaData, taxYear)(errorHandler.internalServerError())(
        Ok(view(cyaData, taxYear))
      )
    }
  }
}
