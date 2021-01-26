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
import config.AppConfig
import controllers.predicates.AuthorisedAction
import javax.inject.Inject
import models.{DividendsCheckYourAnswersModel, DividendsPriorSubmission, DividendsResponseModel, User}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.libs.json.{Json, Reads}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.DividendsSubmissionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.dividends.DividendsCYAView

import scala.concurrent.{ExecutionContext, Future}

class DividendsCYAController @Inject()(
                                        mcc: MessagesControllerComponents,
                                        dividendsCyaView: DividendsCYAView,
                                        dividendsSubmissionService: DividendsSubmissionService,
                                        authorisedAction: AuthorisedAction,
                                        auditService: AuditService
                                      )
                                      (
                                        implicit appConfig: AppConfig
                                      ) extends FrontendController(mcc) with I18nSupport {

  lazy val logger: Logger = Logger(this.getClass.getName)
  implicit val executionContext: ExecutionContext = mcc.executionContext


  def show(taxYear: Int): Action[AnyContent] = authorisedAction { implicit user =>
    val priorSubmissionData: Option[DividendsPriorSubmission] = getSessionData[DividendsPriorSubmission](SessionValues.DIVIDENDS_PRIOR_SUB)
    val cyaSessionData: Option[DividendsCheckYourAnswersModel] = getCya()

    (cyaSessionData, priorSubmissionData) match {
      case (Some(cyaData), Some(priorData)) =>
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

        Ok(dividendsCyaView(cyaModel, priorData, taxYear))
      case (Some(cyaData), None) => Ok(dividendsCyaView(cyaData, taxYear = taxYear))
      case (None, Some(priorData)) =>
        val cyaModel = DividendsCheckYourAnswersModel(
          Some(priorData.ukDividends.nonEmpty),
          priorData.ukDividends,
          Some(priorData.otherUkDividends.nonEmpty),
          priorData.otherUkDividends
        )
        Ok(dividendsCyaView(cyaModel, priorData, taxYear))
          .addingToSession(SessionValues.DIVIDENDS_CYA -> Json.toJson(cyaModel).toString())
      case _ =>
        logger.debug("[DividendsCYAController][show] No Check Your Answers data or Prior Submission data. Redirecting to overview.")
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

  }

  def submit(taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit user =>
    val cyaData: Option[DividendsCheckYourAnswersModel] = getCya()
    val priorData: Option[DividendsPriorSubmission] = getSessionData[DividendsPriorSubmission](SessionValues.DIVIDENDS_PRIOR_SUB)

    dividendsSubmissionService.submitDividends(cyaData, user.nino, user.mtditid, taxYear).map {
      case Right(DividendsResponseModel(NO_CONTENT)) =>
        auditSubmission(CreateOrAmendDividendsAuditDetail(cyaData, priorData, user.nino, user.mtditid, taxYear))
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)).removingFromSession(SessionValues.DIVIDENDS_CYA, SessionValues.DIVIDENDS_PRIOR_SUB)
      case _ => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }
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

  private[controllers] def getSessionData[T](key: String)(implicit user: User[_], reads: Reads[T]): Option[T] = {
    user.session.get(key).flatMap { stringValue =>
      Json.parse(stringValue).asOpt[T]
    }
  }

  private def auditSubmission(details: CreateOrAmendDividendsAuditDetail)
                             (implicit hc: HeaderCarrier,
                              executionContext: ExecutionContext): Future[AuditResult] = {
    val event = AuditModel("CreateOrAmendDividendsUpdate", "createOrAmendDividendsUpdate", details)
    auditService.auditModel(event)
  }

  private def getCya()(implicit user: User[_]): Option[DividendsCheckYourAnswersModel] = {
    getSessionData[DividendsCheckYourAnswersModel](SessionValues.DIVIDENDS_CYA)
  }

}
