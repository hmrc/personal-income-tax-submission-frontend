/*
 * Copyright 2021 HM Revenue & Customs
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
import common.{InterestTaxTypes, SessionValues}
import config.{AppConfig, ErrorHandler, INTEREST}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import models.interest.{InterestCYAModel, InterestPriorSubmission}
import models.{APIErrorBodyModel, APIErrorModel, User}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.InterestSubmissionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.InterestSessionHelper
import views.html.interest.InterestCYAView

import java.util.UUID.randomUUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class InterestCYAController @Inject()(
                                       interestCyaView: InterestCYAView,
                                       interestSubmissionService: InterestSubmissionService,
                                       auditService: AuditService,
                                       errorHandler: ErrorHandler
                                     )
                                     (
                                       implicit appConfig: AppConfig,
                                       authorisedAction: AuthorisedAction,
                                       implicit val mcc: MessagesControllerComponents
                                     ) extends FrontendController(mcc) with I18nSupport with InterestSessionHelper {

  private val logger = Logger.logger
  implicit val executionContext: ExecutionContext = mcc.executionContext

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, INTEREST).async { implicit user =>
    val priorSubmission = getModelFromSession[InterestPriorSubmission](SessionValues.INTEREST_PRIOR_SUB)
    val cyaModel = getCyaModel()

    cyaModel match {
      case Some(cyaData) if !cyaData.isFinished => handleUnfinishedRedirect(cyaData, taxYear)
      case Some(cyaData) =>
        Future.successful(
          Ok(interestCyaView(cyaData, taxYear, priorSubmission))
            .addingToSession(
              SessionValues.INTEREST_CYA -> cyaData.asJsonString
            )
        )
      case _ =>
        logger.info("[InterestCYAController][show] No CYA data in session. Redirecting to the overview page.")
        Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }

  }

  def submit(taxYear: Int): Action[AnyContent] = (authorisedAction andThen journeyFilterAction(taxYear, INTEREST)).async { implicit user =>
    val cyaDataOptional = getCyaModel()
    val priorSubmission: Option[InterestPriorSubmission] = getModelFromSession[InterestPriorSubmission](SessionValues.INTEREST_PRIOR_SUB)

    (cyaDataOptional match {
      case Some(cyaData) => interestSubmissionService.submit(cyaData, user.nino, taxYear, user.mtditid).map {
        case response@Right(_) =>
          val model = CreateOrAmendInterestAuditDetail(Some(cyaData), priorSubmission, user.nino, user.mtditid, user.affinityGroup.toLowerCase, taxYear)
          auditSubmission(model)
          response
        case response => response
      }
      case _ =>
        logger.info("[InterestCYAController][submit] CYA data or NINO missing from session.")
        Future.successful(Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("MISSING_DATA", "CYA data or NINO missing from session."))))
    }).map {
      case Right(_) => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)).clearSessionData()
      case Left(error) => errorHandler.handleError(error.status)
    }
  }

  private[interest] def getCyaModel()(implicit user: User[_]): Option[InterestCYAModel] = {
    (getModelFromSession[InterestCYAModel](SessionValues.INTEREST_CYA), getModelFromSession[InterestPriorSubmission](SessionValues.INTEREST_PRIOR_SUB)) match {
      case (None, Some(priorData)) =>
        Some(InterestCYAModel(
          Some(priorData.hasUntaxed),
          priorData.submissions.map(_.filter(_.priorType.contains(InterestTaxTypes.UNTAXED))),
          Some(priorData.hasTaxed),
          priorData.submissions.map(_.filter(_.priorType.contains(InterestTaxTypes.TAXED)))
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
    Future.successful(
      InterestCYAModel.unapply(cya).getOrElse(None, None, None, None) match {
        case (Some(true), None, None, None) => Redirect(controllers.interest.routes.UntaxedInterestAmountController.show(taxYear, randomUUID().toString))
        case (Some(false), None, None, None) => Redirect(controllers.interest.routes.TaxedInterestController.show(taxYear))
        case (Some(true), Some(_), None, None) => Redirect(controllers.interest.routes.TaxedInterestController.show(taxYear))
        case _ => Redirect(controllers.interest.routes.TaxedInterestAmountController.show(taxYear, randomUUID().toString))
      }
    )
  }
}
