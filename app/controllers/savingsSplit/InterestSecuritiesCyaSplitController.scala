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

import audit.{AuditModel, AuditService, CreateOrAmendSavingsAuditDetail}
import common.IncomeSources
import config.{AppConfig, ErrorHandler, SAVINGS}
import connectors.IncomeSourceConnector
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import models.User
import models.savings.{SavingsIncomeCYAModel, SavingsIncomeDataModel}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{SavingsSessionService, SavingsSubmissionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.savings.InterestSecuritiesCYAView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class InterestSecuritiesCyaSplitController @Inject()(interestSecuritiesCYAView: InterestSecuritiesCYAView,
                                                     savingsSessionService: SavingsSessionService,
                                                     savingsSubmissionService: SavingsSubmissionService,
                                                     incomeSourceConnector: IncomeSourceConnector,
                                                     auditService: AuditService,
                                                     errorHandler: ErrorHandler
                                                    )(implicit appConfig: AppConfig,
                                                      authorisedAction: AuthorisedAction,
                                                      val mcc: MessagesControllerComponents
                                                    ) extends FrontendController(mcc) with I18nSupport with Logging {

  implicit val executionContext: ExecutionContext = mcc.executionContext

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, SAVINGS).async { implicit user =>
    incomeSourceConnector.put(taxYear, user.nino, IncomeSources.INTEREST_SAVINGS)(hc.withExtraHeaders("mtditid" -> user.mtditid)).flatMap {
      case Left(_) =>
        errorHandler.futureInternalServerError()
      case Right(_) =>
        savingsSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>
          (cya, prior) match {
            case (Some(cya), Some(_)) =>
              if (cya.isFinished) {
                Future.successful(Ok(interestSecuritiesCYAView(cya, taxYear)))
              }
              else {
                Future.successful(cya.getNextInJourney(taxYear))
              }
            case (None, Some(prior)) =>
              savingsSessionService.createSessionData(prior.toCYAModel, taxYear)(errorHandler.internalServerError())(
                Ok(interestSecuritiesCYAView(prior.toCYAModel, taxYear))
              )
            case (Some(cya), None) =>
              Future.successful(Ok(interestSecuritiesCYAView(cya, taxYear)))
            case _ =>
              Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          }
        }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = (authorisedAction andThen journeyFilterAction(taxYear, SAVINGS)).async { implicit user =>
    savingsSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>
      if (cya.flatMap(_.gateway).contains(false)) {
        // Exclude journey would be implemented here, but may not be required due to new GDS statuses. Ref: SASS-8192
        submitSavings(taxYear, cya, prior)
      }
      else {
        submitSavings(taxYear, cya, prior)
      }
    }
  }

  private def redirectResult(taxYear: Int): Result = {
      if (appConfig.sectionCompletedQuestionEnabled) {
        Redirect(controllers.routes.SectionCompletedStateController.show(taxYear = taxYear, journey = "gilt-edged"))
      } else {
        Redirect(s"${appConfig.incomeTaxSubmissionBaseUrl}/$taxYear/tasklist")
      }
  }

  def submitSavings(taxYear: Int, cyaData: Option[SavingsIncomeCYAModel],
                    priorData: Option[SavingsIncomeDataModel])(implicit user: User[_], hc: HeaderCarrier): Future[Result] = {
    cyaData match {
      case Some(cya) =>
        priorData.fold(handleSubmissionRedirect(taxYear, cyaData))(
          prior => if (prior.toCYAModel == cya) {
            Future.successful(redirectResult(taxYear))
          } else {
            handleSubmissionRedirect(taxYear, cyaData, priorData)
          }
        )
      case None => Future.successful(errorHandler.internalServerError())
    }
  }

  def handleSubmissionRedirect(taxYear: Int, cyaData: Option[SavingsIncomeCYAModel], prior: Option[SavingsIncomeDataModel] = None)(
    implicit user: User[_], hc: HeaderCarrier): Future[Result] = {
    savingsSubmissionService.submitSavings(cyaData, prior, user.nino, user.mtditid, taxYear).flatMap {
      case Left(error) =>
        Future.successful(errorHandler.handleError(error.status))
      case Right(_) =>
        val model = CreateOrAmendSavingsAuditDetail(
          cyaData.flatMap(_.gateway), cyaData.flatMap(_.grossAmount),
          cyaData.flatMap(_.taxTakenOff), cyaData.flatMap(_.taxTakenOffAmount),
          prior.flatMap(_.securities), prior.isDefined, user.nino, user.mtditid, user.affinityGroup.toLowerCase, taxYear
        )
        auditSubmission(model)
        savingsSessionService.clear(taxYear)(errorHandler.internalServerError())(redirectResult(taxYear))
    }
  }

  private def auditSubmission(details: CreateOrAmendSavingsAuditDetail)
                             (implicit hc: HeaderCarrier,
                              executionContext: ExecutionContext): Future[AuditResult] = {
    val event = AuditModel("CreateOrAmendSavingsUpdate", "create-or-amend-savings-update", details)
    auditService.auditModel(event)
  }

}
