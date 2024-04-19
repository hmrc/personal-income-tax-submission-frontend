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
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import models.dividends.{DecodedDividendsSubmissionPayload, DividendsCheckYourAnswersModel, DividendsPriorSubmission, StockDividendsCheckYourAnswersModel}
import models.{APIErrorBodyModel, APIErrorModel, User}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services._
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.dividends.DividendsCYAView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DividendsCYAController @Inject()(
                                        dividendsCyaView: DividendsCYAView,
                                        dividendsSubmissionService: DividendsSubmissionService,
                                        session: DividendsSessionService,
                                        stockDividendsSession: StockDividendsSessionService,
                                        auditService: AuditService,
                                        errorHandler: ErrorHandler,
                                        excludeJourneyService: ExcludeJourneyService
                                      )
                                      (
                                        implicit appConfig: AppConfig,
                                        authorisedAction: AuthorisedAction,
                                        implicit val mcc: MessagesControllerComponents
                                      ) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  lazy val logger: Logger = Logger(this.getClass.getName)
  implicit val executionContext: ExecutionContext = mcc.executionContext

  //noinspection ScalaStyle
  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, DIVIDENDS).async { implicit user =>
    if (appConfig.isJourneyAvailable(STOCK_DIVIDENDS)) {
      stockDividendsSession.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>
        StockDividendsCheckYourAnswersModel.getCyaModel(cya.flatMap(_.stockDividends), prior) match {
          case Some(cyaData) =>
            if(cya.isDefined){
              stockDividendsSession.updateSessionData(cyaData, taxYear)(errorHandler.internalServerError())(
                Redirect(routes.DividendsSummaryController.show(taxYear))
              )
            } else {
              stockDividendsSession.createSessionData(cyaData, taxYear)(errorHandler.internalServerError())(
                Redirect(routes.DividendsSummaryController.show(taxYear)))
            }
          case _ =>
            logger.info("[DividendsCYAController][show] No CYA data in session. Redirecting to the overview page.")
            Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
        }
      }
    } else {
      session.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>
        DividendsCheckYourAnswersModel.getCyaModel(cya, prior) match {
          case Some(cyaData) if !cyaData.isFinished => Future.successful(handleUnfinishedRedirect(cyaData, taxYear))
          case Some(cyaData) =>
            session.updateSessionData(cyaData, taxYear, cya.isEmpty)(errorHandler.internalServerError())(
              Ok(dividendsCyaView(cyaData, prior, taxYear))
            )
          case _ =>
            logger.info("[DividendsCYAController][show] No CYA data in session. Redirecting to the overview page.")
            Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
        }
      }
    }
  }


  def submit(taxYear: Int): Action[AnyContent] = (authorisedAction andThen journeyFilterAction(taxYear, DIVIDENDS)).async { implicit user =>
    session.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cyaData, priorData) =>
      if (appConfig.dividendsTailoringEnabled && cyaData.flatMap(_.gateway).contains(false)) {
        auditTailorRemoveIncomeSources(TailorRemoveIncomeSourcesAuditDetail(
          nino = user.nino,
          mtditid = user.mtditid,
          userType = user.affinityGroup.toLowerCase,
          taxYear = taxYear,
          body = TailorRemoveIncomeSourcesBody(Seq(DIVIDENDS.stringify))
        ))
        excludeJourneyService.excludeJourney(DIVIDENDS.stringify, taxYear, user.nino).flatMap {
          case Right(_) => performSubmission(taxYear, cyaData, priorData)
          case Left(_) => errorHandler.futureInternalServerError()
        }
      } else {
        performSubmission(taxYear, cyaData, priorData)
      }
    }
  }

  private[controllers] def performSubmission(taxYear: Int, cya: Option[DividendsCheckYourAnswersModel], priorData: Option[DividendsPriorSubmission])
                                            (implicit user: User[_], hc: HeaderCarrier): Future[Result] = {
    (cya match {
      case Some(cyaData) =>
        dividendsSubmissionService.submitDividends(cya, user.nino, user.mtditid, taxYear).map {
          case response@Right(_) =>
            val model = CreateOrAmendDividendsAuditDetail.createFromCyaData(
              cyaData, priorData, None,
              priorData.isDefined, user.nino, user.mtditid, user.affinityGroup.toLowerCase, taxYear
            )
            auditSubmission(model)
            response
          case response => response
        }
      case _ =>
        logger.info("[DividendsCYAController][submit] CYA data or NINO missing from session.")
        Future.successful(Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("MISSING_DATA", "CYA data or NINO missing from session."))))
    }).flatMap {
      case Right(_) =>
        session.clear(taxYear)(errorHandler.internalServerError())(
          Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        )
      case Left(error) => Future.successful(errorHandler.handleError(error.status))
    }
  }


  private def auditSubmission(details: CreateOrAmendDividendsAuditDetail)
                             (implicit hc: HeaderCarrier,
                              executionContext: ExecutionContext): Future[AuditResult] = {
    val event = AuditModel("CreateOrAmendDividendsUpdate", "create-or-amend-dividends-update", details)
    auditService.auditModel(event)
  }

  private def auditTailorRemoveIncomeSources(details: TailorRemoveIncomeSourcesAuditDetail)
                                            (implicit hc: HeaderCarrier,
                                             executionContext: ExecutionContext): Future[AuditResult] = {
    val event = AuditModel("TailorRemoveIncomeSources", "tailorRemoveIncomeSources", details)
    auditService.auditModel(event)
  }

  private[dividends] def handleUnfinishedRedirect(cya: DividendsCheckYourAnswersModel, taxYear: Int): Result = {
    DividendsCheckYourAnswersModel.unapply(cya).getOrElse((None, None, None, None, None)) match {
      case (None, _, _, _, _) if appConfig.dividendsTailoringEnabled => Redirect(controllers.dividends.routes.DividendsGatewayController.show(taxYear))
      case (_, Some(true), None, None, None) => Redirect(controllers.dividends.routes.UkDividendsAmountController.show(taxYear))
      case (_, Some(false), None, None, None) => Redirect(controllers.dividends.routes.ReceiveOtherUkDividendsController.show(taxYear))
      case (_, Some(true), Some(_), None, None) => Redirect(controllers.dividends.routes.ReceiveOtherUkDividendsController.show(taxYear))
      case _ => Redirect(controllers.dividends.routes.OtherUkDividendsAmountController.show(taxYear))
    }
  }
}
