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
import connectors.httpParsers.StockDividendsSubmissionHttpParser._
import connectors.{DividendsSubmissionConnector, StockDividendsSubmissionConnector, StockDividendsUserDataConnector}
import models.User
import models.dividends.{DividendsPriorSubmission, StockDividendsCheckYourAnswersModel, StockDividendsPriorSubmission}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StockDividendsSubmissionService @Inject()(
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
      case Right(priorStockDividends) =>
        dividendsSessionService.getPriorData(taxYear)(user, hc).flatMap {
          case Left(error) => Future.successful(Left(error))
          case Right(priorDividends) =>
            auditSubmission(CreateOrAmendDividendsAuditDetail.createFromStockCyaData(
              cya, priorDividends.dividends, priorStockDividends,
              priorStockDividends.exists(_.stockDividend.isDefined) || priorDividends.dividends.isDefined,
              user.nino, user.mtditid, user.affinityGroup, taxYear
            ))
            performSubmissions(cya, priorDividends.dividends, nino, taxYear, hc, user, priorStockDividends).map {
              results => {
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



  private def performSubmissions(cya: StockDividendsCheckYourAnswersModel, priorDividends: Option[DividendsPriorSubmission],
                                 nino: String, taxYear: Int, hc: HeaderCarrier, user: User[_], result: Option[StockDividendsPriorSubmission]
                                ): Future[Seq[StockDividendsSubmissionResponse]] = {

    val priorStockDividends = result match {
      case Some(value) => value
      case None => StockDividendsPriorSubmission()
    }

    Future.sequence(
      Seq(
        hasDividendsValuesChanged(cya, priorDividends, nino, taxYear, hc, user),
        hasStockDividendsValuesChanged(cya, priorStockDividends, nino, taxYear, hc, user)
      )
    )
  }

  private def hasDividendsValuesChanged(cya: StockDividendsCheckYourAnswersModel, priorDividends: Option[DividendsPriorSubmission],
                                        nino: String, taxYear: Int, hc: HeaderCarrier, user: User[_]): Future[StockDividendsSubmissionResponse] = {

    val hasUkDividendsChanged = checkUpdatedValues(cya.ukDividendsAmount, priorDividends.flatMap(_.ukDividends))
    val hasOtherUkDividendsChanged = checkUpdatedValues(cya.otherUkDividendsAmount, priorDividends.flatMap(_.otherUkDividends))
    val hasDataChanged = Seq(hasUkDividendsChanged, hasOtherUkDividendsChanged).contains(true)

    if (hasDataChanged) {
      dividendsSubmissionConnector.submitDividends(cya.toDividendsSubmissionModel, nino, taxYear)(hc.withExtraHeaders("mtditid" -> user.mtditid)).map {
        case Right(_) => Right(true)
        case Left(error) => Left(error)
      }
    } else {
      Future.successful(Right(true))
    }
  }

  private def hasStockDividendsValuesChanged(cya: StockDividendsCheckYourAnswersModel, priorStockDividends: StockDividendsPriorSubmission,
                                             nino: String, taxYear: Int, hc: HeaderCarrier, user: User[_]): Future[StockDividendsSubmissionResponse] = {

    val hasStocksChanged = checkUpdatedValues(cya.stockDividendsAmount, priorStockDividends.stockDividend.map(_.grossAmount))
    val hasSharesChanged = checkUpdatedValues(cya.redeemableSharesAmount, priorStockDividends.redeemableShares.map(_.grossAmount))
    val hasLoansChanged = checkUpdatedValues(cya.closeCompanyLoansWrittenOffAmount, priorStockDividends.closeCompanyLoansWrittenOff.map(_.grossAmount))
    val hasDataChanged = Seq(hasStocksChanged, hasSharesChanged, hasLoansChanged).contains(true)

    if (hasDataChanged) {
      stockDividendsSubmissionConnector.submitDividends(priorStockDividends.toSubmission(cya), nino, taxYear)(hc.withExtraHeaders("mtditid" -> user.mtditid))
    } else {
      Future.successful(Right(true))
    }
  }

  private def checkUpdatedValues(cyaData: Option[BigDecimal], priorData: Option[BigDecimal]): Boolean = {
    (cyaData, priorData) match {
      case (Some(cyaAmount), Some(grossAmount)) => cyaAmount != grossAmount
      case (Some(_), None) => true
      case (None, Some(_)) => true
      case (_, _) => false
    }
  }

  private def auditSubmission(details: CreateOrAmendDividendsAuditDetail)
                             (implicit hc: HeaderCarrier): Future[AuditResult] = {
    val event = AuditModel("CreateOrAmendDividendsUpdate", "create-or-amend-dividends-update", details)
    auditService.auditModel(event)
  }
}
