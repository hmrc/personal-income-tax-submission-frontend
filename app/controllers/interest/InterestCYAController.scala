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

package controllers.interest

import audit._
import cats.data.EitherT
import common.InterestTaxTypes.{TAXED, UNTAXED}
import config.{AppConfig, ErrorHandler, INTEREST}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import models.interest.InterestCYAModel
import models.{APIErrorBodyModel, APIErrorModel, User}
import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{ExcludeJourneyService, InterestSessionService, InterestSubmissionService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.interest.InterestCYAView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.language.implicitConversions

class InterestCYAController @Inject()(
                                       interestCyaView: InterestCYAView,
                                       interestSubmissionService: InterestSubmissionService,
                                       auditService: AuditService,
                                       errorHandler: ErrorHandler,
                                       interestSessionService: InterestSessionService,
                                       excludeJourneyService: ExcludeJourneyService
                                     )
                                     (
                                       implicit appConfig: AppConfig,
                                       authorisedAction: AuthorisedAction,
                                       val mcc: MessagesControllerComponents
                                     ) extends FrontendController(mcc) with I18nSupport with SessionHelper with Logging {


  implicit val executionContext: ExecutionContext = mcc.executionContext

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, INTEREST).async { implicit user =>
    interestSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>
      InterestCYAModel.getCyaModel(cya, prior) match {
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

  private def handleTailoredExclusionRequest(taxYear: Int)(implicit user: User[_]): EitherT[Future, Result, Unit] = {
    val result = for {
      _ <- EitherT.right(auditTailorRemoveIncomeSources(TailorRemoveIncomeSourcesAuditDetail(
        nino = user.nino,
        mtditid = user.mtditid,
        userType = user.affinityGroup.toLowerCase,
        taxYear = taxYear,
        body = TailorRemoveIncomeSourcesBody(Seq(INTEREST.stringify))
      )))
      _ <- EitherT(excludeJourneyService.excludeJourney(INTEREST.stringify, taxYear, user.nino)
      )
    } yield ()

    result.leftMap(_ => errorHandler.internalServerError())
  }

  def submit(taxYear: Int): Action[AnyContent] = (authorisedAction andThen journeyFilterAction(taxYear, INTEREST)).async { implicit user =>
    interestSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cyaOpt, priorOpt) =>
      val isTailoredExclusion = appConfig.interestTailoringEnabled && cyaOpt.flatMap(_.gateway).contains(false)

      implicit class ResultFactory[T](res: EitherT[Future, APIErrorModel, T]) {
        def toResultModel: EitherT[Future, Result, T] =
          res.leftMap(error => errorHandler.handleError(error.status))
      }

      def cyaResult: EitherT[Future, Result, InterestCYAModel] = EitherT(Future.successful(
        cyaOpt.fold {
          logger.info("[InterestCYAController][submit] CYA data or NINO missing from session.")
          Left[APIErrorModel, InterestCYAModel](
            APIErrorModel(BAD_REQUEST, APIErrorBodyModel("MISSING_DATA", "CYA data or NINO missing from session."))
          ).withRight
        }(cya => Right(cya))
      )).toResultModel

      def redirectResult(): Result = {
        if (appConfig.sectionCompletedQuestionEnabled) {
          Redirect(controllers.routes.SectionCompletedStateController.show(taxYear, "uk-interest"))
        } else if (appConfig.interestSavingsEnabled) {
          Redirect(controllers.routes.InterestFromSavingsAndSecuritiesSummaryController.show(taxYear))
        } else {
          Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        }
      }

      def auditModel: CreateOrAmendInterestAuditDetail = CreateOrAmendInterestAuditDetail(
        body = cyaOpt,
        prior = priorOpt,
        isUpdate = priorOpt.isDefined,
        nino = user.nino,
        mtditid = user.mtditid,
        userType = user.affinityGroup.toLowerCase,
        taxYear = taxYear
      )

      val result: EitherT[Future, Result, Result] = for {
        _ <- if(isTailoredExclusion) handleTailoredExclusionRequest(taxYear) else EitherT.right(Future.successful())
        cya <- cyaResult
        _ <- EitherT(interestSubmissionService.submit(cya, user.nino, taxYear, user.mtditid)).toResultModel
        _ <- EitherT.right(auditSubmission(auditModel))
        sessionClearResult <- EitherT(interestSessionService.clear(taxYear)(
          Left[Result, Result](errorHandler.internalServerError()).withRight
        )(Right(redirectResult()))
        )
      } yield sessionClearResult

      result.merge
    }
  }

  private def auditSubmission(details: CreateOrAmendInterestAuditDetail)
                             (implicit hc: HeaderCarrier,
                              executionContext: ExecutionContext): Future[AuditResult] = {
    val event = AuditModel("CreateOrAmendInterestUpdate", "createOrAmendInterestUpdate", details)
    auditService.auditModel(event)
  }

  private def auditTailorRemoveIncomeSources(details: TailorRemoveIncomeSourcesAuditDetail)
                             (implicit hc: HeaderCarrier,
                              executionContext: ExecutionContext): Future[AuditResult] = {
    val event = AuditModel("TailorRemoveIncomeSources", "tailorRemoveIncomeSources", details)
    auditService.auditModel(event)
  }

  private def handleUnfinishedRedirect(cya: InterestCYAModel, taxYear: Int): Future[Result] = {
    Future(
      cya match {
        case InterestCYAModel(None, _, _, _) if appConfig.interestTailoringEnabled =>
          Redirect(controllers.interest.routes.InterestGatewayController.show(taxYear))
        case InterestCYAModel(_, Some(true), None, Seq()) => Redirect(controllers.interest.routes.ChooseAccountController.show(taxYear, UNTAXED))
        case InterestCYAModel(_, Some(false), None, Seq()) => Redirect(controllers.interest.routes.TaxedInterestController.show(taxYear))
        case InterestCYAModel(_, Some(true), None, accounts) if accounts.exists(_.hasUntaxed) =>
          Redirect(controllers.interest.routes.TaxedInterestController.show(taxYear))
        case _ => Redirect(controllers.interest.routes.ChooseAccountController.show(taxYear, TAXED))
      }
    )
  }
}
