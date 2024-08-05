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

import common.IncomeSources
import connectors.dividends.{CreateDividendsBackendConnector, GetDividendsBackendConnector, UpdateDividendsBackendConnector}
import connectors.httpParsers.DividendsSubmissionHttpParser.DividendsSubmissionsResponse
import connectors.httpParsers.IncomeTaxUserDataHttpParser.IncomeTaxUserDataResponse
import connectors.httpParsers.StockDividendsBackendUserDataHttpParser.StockDividendsBackendUserDataResponse
import connectors.httpParsers.StockDividendsSubmissionHttpParser.StockDividendsSubmissionResponse
import connectors.stockdividends.{CreateStockDividendsBackendConnector, GetStockDividendsBackendConnector, UpdateStockDividendsBackendConnector}
import connectors.{IncomeSourceConnector, IncomeTaxUserDataConnector}
import models.requests.AuthorisationRequest
import models.User
import models.dividends.{DividendsCheckYourAnswersModel, DividendsPriorSubmission, EncryptedStockDividendsCheckYourAnswersModel, StockDividendsCheckYourAnswersModel}
import models.mongo.{DataNotFound, DatabaseError, DividendsUserDataModel, StockDividendsUserDataModel}
import play.api.{Logger, Logging}
import repositories.{DividendsUserDataRepository, StockDividendsUserDataRepository}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class NewStockDividendsSessionServiceImpl @Inject()(
                                                     createDividendsBackendConnector: CreateStockDividendsBackendConnector,
                                                     getStockDividendsBackendConnector: GetStockDividendsBackendConnector,
                                                     updateStockDividendsBackendConnector: UpdateStockDividendsBackendConnector,
                                                     stockDividendsUserDataRepository: StockDividendsUserDataRepository,
                                                     incomeSourceConnector: IncomeSourceConnector
                                       )(implicit correlationId: String, executionContext: ExecutionContext) extends StockDividendsSessionServiceProvider with Logging {

  def createSessionData[A](cyaModel: StockDividendsCheckYourAnswersModel, taxYear: Int)(onFail: A)(onSuccess: A)
                          (implicit hc: HeaderCarrier): Future[A] = {

    createDividendsBackendConnector.createSessionData(cyaModel, taxYear).map {
      case Right(_) => onSuccess
      case Left(_) =>
        logger.error(s"[GainsSessionService][createSessionData] session create failed. correlation id: " + correlationId)
        onFail
    }
  }

  def getSessionData(taxYear: Int)(implicit request: AuthorisationRequest[_],
                                   hc: HeaderCarrier): Future[StockDividendsBackendUserDataResponse] = {

    getStockDividendsBackendConnector.getSessionData(taxYear)(
      hc.withExtraHeaders("mtditid" -> request.user.mtditid).withExtraHeaders("X-CorrelationId" -> correlationId), executionContext).map {
      case Right(userData) =>
        Right(userData)
      case Left(_) =>
        logger.error("[StockDividendsSessionService][getSessionData] Could not find user session. correlation id: " + correlationId)
        Left(DataNotFound)
    }
  }


    def updateSessionData[A](cyaModel: StockDividendsCheckYourAnswersModel, taxYear: Int)(onFail: A)(onSuccess: A)
                          (implicit request: AuthorisationRequest[_], hc: HeaderCarrier): Future[A] = {

      updateStockDividendsBackendConnector.updateSessionData(cyaModel, taxYear)(
         User[_], hc.withExtraHeaders("mtditid" -> request.user.mtditid).withExtraHeaders("X-CorrelationId" -> correlationId), executionContext).map {
        case Right(_) =>
          onSuccess
        case Left(_) =>
          logger.error(s"[StockDividendsSessionService][updateSessionData] session update failure. correlation id: " + correlationId)
          onFail
      }
    }


  def clear[R](taxYear: Int)(onFail: R)(onSuccess: R)(implicit user: User[_], hc: HeaderCarrier): Future[R] = {
    incomeSourceConnector.put(taxYear, user.nino, IncomeSources.DIVIDENDS)(hc.withExtraHeaders("mtditid" -> user.mtditid)).flatMap {
      case Left(_) => Future.successful(onFail)
      case _ =>
        stockDividendsUserDataRepository.clear(taxYear).map {
          case true => onSuccess
          case false => onFail
        }
    }
  }

}
