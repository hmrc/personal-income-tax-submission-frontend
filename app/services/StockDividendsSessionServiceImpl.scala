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

package services

import connectors.httpParsers.StockDividendsBackendUserDataHttpParser.StockDividendsBackendUserDataResponse
import connectors.stockdividends.GetStockDividendsBackendConnector
import models.User
import models.dividends.StockDividendsCheckYourAnswersModel
import models.mongo.{DatabaseError, StockDividendsUserDataModel}
import play.api.Logging
import repositories.StockDividendsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StockDividendsSessionServiceImpl @Inject()(
                                                  stockDividendsUserDataRepository: StockDividendsUserDataRepository,
                                                  getStockDividendsBackendConnector: GetStockDividendsBackendConnector
                                       )(implicit correlationId: String, executionContext: ExecutionContext) extends StockDividendsSessionServiceProvider with Logging {

  def getPriorData(taxYear: Int)(implicit request: User[_], hc: HeaderCarrier): Future[StockDividendsBackendUserDataResponse] = {
    getStockDividendsBackendConnector.getSessionData(taxYear)(hc.withExtraHeaders("mtditid" -> request.mtditid))
  }

  def createSessionData[A](cyaModel: StockDividendsCheckYourAnswersModel, taxYear: Int)(onFail: A)(onSuccess: A)
                          (implicit request: User[_], hc: HeaderCarrier): Future[A] = {

    val userData = StockDividendsUserDataModel(
      request.sessionId,
      request.mtditid,
      request.nino,
      taxYear,
      Some(cyaModel),
      Instant.now
    )

    stockDividendsUserDataRepository.create(userData).map {
      case Right(_) =>
        onSuccess
      case Left(_) =>
        logger.error(s"[StockDividendsSessionService][createSessionData] session create failed. correlation id: " + correlationId)
        onFail
    }
  }

  def getSessionData(taxYear: Int)(implicit request: User[_], hc: HeaderCarrier):
  Future[Either[DatabaseError, Option[StockDividendsUserDataModel]]] = {

    stockDividendsUserDataRepository.find(taxYear)(request).map {
      case Left(error) =>
        logger.error("[StockDividendsSessionService][getSessionData] Could not find user session. correlation id: " + correlationId)
        Left(error)
      case Right(userData) =>
        Right(userData)
    }
  }

  def updateSessionData[A](cyaModel: StockDividendsCheckYourAnswersModel, taxYear: Int)(onFail: A)(onSuccess: A)
                          (implicit request: User[_], hc: HeaderCarrier): Future[A] = {

    val userData = StockDividendsUserDataModel(
      request.sessionId,
      request.mtditid,
      request.nino,
      taxYear,
      Some(cyaModel),
      Instant.now)

    stockDividendsUserDataRepository.update(userData).map {
      case Right(_) =>
        onSuccess
      case Left(_) =>
        logger.error(s"[StockDividendsSessionService][updateSessionData] session update failure. correlation id: " + correlationId)
        onFail
    }

  }

  def deleteSessionData[A](taxYear: Int)(onFail: A)(onSuccess: A)
                          (implicit request: User[_], hc: HeaderCarrier): Future[A] = {

    stockDividendsUserDataRepository.clear(taxYear)(request).map {
      case true =>
        onSuccess
      case _ =>
        logger.error(s"[StockDividendsSessionService][deleteSessionData] session delete failure. correlation id: " + correlationId)
        onFail
    }

  }

  def getAndHandle[R](taxYear: Int)(onFail: R)(block: (Option[StockDividendsCheckYourAnswersModel], Option[StockDividendsUserDataModel]) => R)
                     (implicit request: User[_], hc: HeaderCarrier): Future[R] = {
    for {
      optionalCya <- getSessionData(taxYear)
      priorDataResponse <- getPriorData(taxYear)
    } yield {
      priorDataResponse match {
        case Right(prior) => optionalCya match {
          case Left(_) =>
            logger.error(s"[StockDividendsSessionService][getAndHandle] No session data. correlation id: " + correlationId)
            onFail
          case Right(cyaData) => block(cyaData.flatMap(_.stockDividends), prior)
        }
        case Left(_) =>
          logger.error(s"[StockDividendsSessionService][getAndHandle] No prior data. correlation id: " + correlationId)
          onFail
      }
    }
  }
}