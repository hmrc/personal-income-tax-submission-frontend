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

package controllers.dividends

import audit.{AuditModel, AuditService, CreateOrAmendDividendsAuditDetail}
import common.SessionValues
import config.{AppConfig, DIVIDENDS, ErrorHandler}
import connectors.httpParsers.IncomeTaxUserDataHttpParser.IncomeTaxUserDataResponse
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import models.User
import models.dividends.{DividendsCheckYourAnswersModel, DividendsResponseModel}
import models.mongo.DividendsUserDataModel
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{DividendsSessionService, DividendsSubmissionService}
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
                                        auditService: AuditService,
                                        errorHandler: ErrorHandler
                                      )
                                      (
                                        implicit appConfig: AppConfig,
                                        authorisedAction: AuthorisedAction,
                                        implicit val mcc: MessagesControllerComponents
                                      ) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  lazy val logger: Logger = Logger(this.getClass.getName)
  implicit val executionContext: ExecutionContext = mcc.executionContext

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, DIVIDENDS).async { implicit user =>
    lazy val futurePriorSubmissionData: Future[IncomeTaxUserDataResponse] = session.getPriorData(taxYear)
    lazy val futureCyaSessionData: Future[Option[DividendsUserDataModel]] = session.getSessionData(taxYear)

    (for {
      priorSubmissionData <- futurePriorSubmissionData
      cyaSessionData <- futureCyaSessionData
    } yield {
      (cyaSessionData.flatMap(_.dividends), priorSubmissionData.map(_.dividends)) match {
        case (Some(cyaData), Right(Some(priorData))) =>
          val ukDividendsExist = cyaData.ukDividends.getOrElse(priorData.ukDividends.nonEmpty)
          val otherDividendsExist = cyaData.otherUkDividends.getOrElse(priorData.otherUkDividends.nonEmpty)

          val ukDividendsValue: Option[BigDecimal] = priorityOrderOrNone(cyaData.ukDividendsAmount, priorData.ukDividends, ukDividendsExist)
          val otherDividendsValue: Option[BigDecimal] = priorityOrderOrNone(cyaData.otherUkDividendsAmount, priorData.otherUkDividends, otherDividendsExist)

          val cyaModel = DividendsCheckYourAnswersModel(
            Some(ukDividendsExist),
            ukDividendsValue,
            Some(otherDividendsExist),
            otherDividendsValue
          )

          Future.successful(Ok(dividendsCyaView(cyaModel, priorData, taxYear)))
        case (Some(cyaData), Right(None)) if !cyaData.isFinished => Future.successful(handleUnfinishedRedirect(cyaData, taxYear))
        case (Some(cyaData), Right(None)) => Future.successful(Ok(dividendsCyaView(cyaData, taxYear = taxYear)))
        case (None, Right(Some(priorData))) =>
          val cyaModel = DividendsCheckYourAnswersModel(
            Some(priorData.ukDividends.nonEmpty),
            priorData.ukDividends,
            Some(priorData.otherUkDividends.nonEmpty),
            priorData.otherUkDividends
          )

          session.createSessionData(cyaModel, taxYear)(
            InternalServerError(errorHandler.internalServerErrorTemplate)
          )(
            Ok(dividendsCyaView(cyaModel, priorData, taxYear))
          )
        case _ =>
          logger.info("[DividendsCYAController][show] No Check Your Answers data or Prior Submission data. Redirecting to overview.")
          Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }).flatten

  }

  def submit(taxYear: Int): Action[AnyContent] = (authorisedAction andThen journeyFilterAction(taxYear, DIVIDENDS)).async { implicit user =>
    val futureCyaData: Future[Option[DividendsUserDataModel]] = session.getSessionData(taxYear)
    val futurePriorData: Future[IncomeTaxUserDataResponse] = session.getPriorData(taxYear)

    (for {
      optionalCyaData <- futureCyaData
      priorDataLeftRight <- futurePriorData
    } yield {
      val cyaData: Option[DividendsCheckYourAnswersModel] = optionalCyaData.flatMap(_.dividends)
      val priorData = priorDataLeftRight match {
        case Right(incomeModel) => incomeModel.dividends
        case _ => None
      }

      dividendsSubmissionService.submitDividends(cyaData, user.nino, user.mtditid, taxYear).flatMap {
        case Right(DividendsResponseModel(_)) =>
          auditSubmission(
            CreateOrAmendDividendsAuditDetail(cyaData, priorData, priorData.isDefined, user.nino, user.mtditid, user.affinityGroup.toLowerCase(), taxYear)
          )
          session.clear(taxYear)(errorHandler.internalServerError())(
            Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
          )
        case Left(error) => Future.successful(errorHandler.handleError(error.status))
      }
    }).flatten
  }

  private[dividends] def priorityOrderOrNone(priority: Option[BigDecimal], other: Option[BigDecimal], yesNoResult: Boolean): Option[BigDecimal] = {
    if (yesNoResult) {
      (priority, other) match {
        case (Some(priorityValue), _) => Some(priorityValue)
        case (None, Some(otherValue)) => Some(otherValue)
        case _ => None
      }
    } else {
      None
    }
  }

  private def auditSubmission(details: CreateOrAmendDividendsAuditDetail)
                             (implicit hc: HeaderCarrier,
                              executionContext: ExecutionContext): Future[AuditResult] = {
    val event = AuditModel("CreateOrAmendDividendsUpdate", "createOrAmendDividendsUpdate", details)
    auditService.auditModel(event)
  }

  private[dividends] def handleUnfinishedRedirect(cya: DividendsCheckYourAnswersModel, taxYear: Int): Result = {
    DividendsCheckYourAnswersModel.unapply(cya).getOrElse(None, None, None, None) match {
      case (Some(true), None, None, None) => Redirect(controllers.dividends.routes.UkDividendsAmountController.show(taxYear))
      case (Some(false), None, None, None) => Redirect(controllers.dividends.routes.ReceiveOtherUkDividendsController.show(taxYear))
      case (Some(true), Some(_), None, None) => Redirect(controllers.dividends.routes.ReceiveOtherUkDividendsController.show(taxYear))
      case _ => Redirect(controllers.dividends.routes.OtherUkDividendsAmountController.show(taxYear))
    }
  }

}
