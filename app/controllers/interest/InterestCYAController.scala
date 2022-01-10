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

package controllers.interest

import audit.{AuditModel, AuditService, CreateOrAmendInterestAuditDetail}
import config.{AppConfig, ErrorHandler, INTEREST}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import models.interest.{DecodedInterestSubmissionPayload, InterestCYAModel, InterestPriorSubmission}
import models.{APIErrorBodyModel, APIErrorModel, User}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{InterestSessionService, InterestSubmissionService, NrsService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.interest.InterestCYAView

import common.InterestTaxTypes.{TAXED, UNTAXED}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class InterestCYAController @Inject()(
                                       interestCyaView: InterestCYAView,
                                       interestSubmissionService: InterestSubmissionService,
                                       auditService: AuditService,
                                       errorHandler: ErrorHandler,
                                       interestSessionService: InterestSessionService,
                                       nrsService: NrsService
                                     )
                                     (
                                       implicit appConfig: AppConfig,
                                       authorisedAction: AuthorisedAction,
                                       val mcc: MessagesControllerComponents
                                     ) extends FrontendController(mcc) with I18nSupport with SessionHelper with Logging {


  implicit val executionContext: ExecutionContext = mcc.executionContext

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, INTEREST).async { implicit user =>
    interestSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>
      getCyaModel(cya, prior) match {
        case Some(cyaData) if !cyaData.isFinished => handleUnfinishedRedirect(cyaData, taxYear)
        case Some(cyaData) =>
          interestSessionService.updateSessionData(cyaData, taxYear, cya.isEmpty)(errorHandler.internalServerError())(
            Ok(interestCyaView(cyaData, taxYear, prior))
          )
        case _ =>
          logger.info("[InterestCYAController][show] No CYA data in session. Redirecting to the overview page.")
          Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = (authorisedAction andThen journeyFilterAction(taxYear, INTEREST)).async { implicit user =>
    interestSessionService.getAndHandle(taxYear)(errorHandler.futureInternalServerError()) { (cya, prior) =>
      Future((cya match {
        case Some(cyaData) => interestSubmissionService.submit(cyaData, user.nino, taxYear, user.mtditid).map {
          case response@Right(_) =>
            val model = CreateOrAmendInterestAuditDetail(
              Some(cyaData), prior, prior.isDefined, user.nino, user.mtditid, user.affinityGroup.toLowerCase, taxYear
            )
            auditSubmission(model)

            if (appConfig.nrsEnabled) {
              nrsService.submit(user.nino, new DecodedInterestSubmissionPayload(Some(cyaData), prior), user.mtditid)
            }

            response
          case response => response
        }
        case _ =>
          logger.info("[InterestCYAController][submit] CYA data or NINO missing from session.")
          Future.successful(Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("MISSING_DATA", "CYA data or NINO missing from session."))))
      }).flatMap {
        case Right(_) =>
          interestSessionService.clear(taxYear)(errorHandler.internalServerError())(
            Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
          )
        case Left(error) => Future.successful(errorHandler.handleError(error.status))
      })
    }.flatten
  }

  private[interest] def getCyaModel(cya: Option[InterestCYAModel], prior: Option[InterestPriorSubmission]): Option[InterestCYAModel] = {
    (cya, prior) match {
      case (None, Some(priorData)) =>
        Some(InterestCYAModel(
          Some(priorData.hasUntaxed),
          Some(priorData.hasTaxed),
          Some(priorData.submissions.filter(x => x.hasTaxed || x.hasUntaxed))
        ))
      case (Some(cyaData), _) => Some(cyaData)
      case _ => None
    }
  }

  private def auditSubmission(details: CreateOrAmendInterestAuditDetail)
                             (implicit hc: HeaderCarrier,
                              executionContext: ExecutionContext): Future[AuditResult] = {
    val event = AuditModel("CreateOrAmendInterestUpdate", "createOrAmendInterestUpdate", details)
    auditService.auditModel(event)
  }

  private def handleUnfinishedRedirect(cya: InterestCYAModel, taxYear: Int): Future[Result] = {
    Future(
      cya match {
        case InterestCYAModel(Some(true), None, None) => Redirect(controllers.interest.routes.ChooseAccountController.show(taxYear, UNTAXED))
        case InterestCYAModel(Some(false), None, None) => Redirect(controllers.interest.routes.TaxedInterestController.show(taxYear))
        case InterestCYAModel(Some(true), None, Some(accounts)) if accounts.exists(_.hasUntaxed) =>
          Redirect(controllers.interest.routes.TaxedInterestController.show(taxYear))
        case _ => Redirect(controllers.interest.routes.ChooseAccountController.show(taxYear, TAXED))
      }
    )
  }
}
