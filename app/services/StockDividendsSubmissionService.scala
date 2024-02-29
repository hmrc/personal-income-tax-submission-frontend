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

package services

import audit.{AuditModel, AuditService, CreateOrAmendDividendsAuditDetail}
import config.AppConfig
import connectors.httpParsers.StockDividendsSubmissionHttpParser._
import connectors.{DividendsSubmissionConnector, StockDividendsSubmissionConnector, StockDividendsUserDataConnector}
import models.User
import models.dividends.{StockDividendsCheckYourAnswersModel, StockDividendsPriorSubmission}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StockDividendsSubmissionService @Inject()(
                                                 appConfig: AppConfig,
                                                 stockDividendsSubmissionConnector: StockDividendsSubmissionConnector,
                                                 stockDividendsUserDataConnector: StockDividendsUserDataConnector,
                                                 dividendsSessionService: DividendsSessionService,
                                                 dividendsSubmissionConnector: DividendsSubmissionConnector,
                                                 auditService: AuditService
                                               )
                                               (implicit ec: ExecutionContext) {

  def submitDividends(cya: StockDividendsCheckYourAnswersModel, nino: String, taxYear: Int)
                     (implicit hc: HeaderCarrier, user: User[_], ec: ExecutionContext): Future[StockDividendsSubmissionResponse] = {
    stockDividendsUserDataConnector.getUserData(taxYear)(user, hc.withExtraHeaders("mtditid" -> user.mtditid)).flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(result) =>
        dividendsSessionService.getPriorData(taxYear)(user, hc).flatMap {
          case Left(error) => Future.successful(Left(error))
          case Right(priorDividends) =>
            auditSubmission(CreateOrAmendDividendsAuditDetail.createFromStockCyaData(
              cya, priorDividends.dividends, Some(result), result.stockDividend.isDefined || priorDividends.dividends.isDefined,
              user.nino, user.mtditid, user.affinityGroup, taxYear))
            performSubmissions(cya, nino, taxYear, hc, user, result).map { results => {
              val response = results.filter(_.isLeft)
              if (response.isEmpty) {
                Right(true)
              } else {
                response.head
              }
            }
            }
        }
    }
  }

  private def performSubmissions(cya: StockDividendsCheckYourAnswersModel, nino: String, taxYear: Int, hc: HeaderCarrier, user: User[_],
                                 result: StockDividendsPriorSubmission) = {
    Future.sequence(Seq(
      if (cya.hasDividendsData) {
        dividendsSubmissionConnector
          .submitDividends(cya.toDividendsSubmissionModel, nino, taxYear)(hc.withExtraHeaders("mtditid" -> user.mtditid)).map {
          case Right(_) => Right(true)
          case Left(_) => Right(false)
        }
      } else {
        Future.successful(Right(true))
      },
      if (cya.hasStockDividendsData) {
        stockDividendsSubmissionConnector.submitDividends(result.toSubmission(cya), nino, taxYear)(hc.withExtraHeaders("mtditid" -> user.mtditid))
      } else {
        Future.successful(Right(true))
      }
    ))
  }

  private def auditSubmission(details: CreateOrAmendDividendsAuditDetail)
                             (implicit hc: HeaderCarrier): Future[AuditResult] = {
    val event = AuditModel("CreateOrAmendDividendsUpdate", "create-or-amend-dividends-update", details)
    auditService.auditModel(event)
  }
}

