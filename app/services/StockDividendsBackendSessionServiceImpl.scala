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
import connectors.IncomeSourceConnector
import connectors.stockdividends.{CreateStockDividendsSessionConnector, DeleteStockDividendsSessionConnector, GetStockDividendsSessionConnector, UpdateStockDividendsSessionConnector}
import models.User
import models.dividends.StockDividendsCheckYourAnswersModel
import models.mongo.{DataNotFound, DatabaseError, StockDividendsUserDataModel}
import models.priorDataModels.StockDividendsPriorDataModel
import play.api.Logging
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StockDividendsBackendSessionServiceImpl @Inject()(createDividendsBackendConnector: CreateStockDividendsSessionConnector,
                                                        getStockDividendsBackendConnector: GetStockDividendsSessionConnector,
                                                        updateStockDividendsBackendConnector: UpdateStockDividendsSessionConnector,
                                                        deleteStockDividendsBackendConnector: DeleteStockDividendsSessionConnector,
                                                        incomeSourceConnector: IncomeSourceConnector,
                                                        stockDividendsPriorDataService: StockDividendsPriorDataService)
                                                       (implicit correlationId: String, executionContext: ExecutionContext)
  extends StockDividendsSessionServiceProvider with Logging {

  def createSessionData[A](cyaModel: StockDividendsCheckYourAnswersModel, taxYear: Int)(onFail: A)(onSuccess: A)
                          (implicit request: User[_], hc: HeaderCarrier): Future[A] = {
    createDividendsBackendConnector.createSessionData(cyaModel, taxYear)(
      hc.withExtraHeaders("mtditid" -> request.mtditid).withExtraHeaders("X-CorrelationId" -> correlationId)).map {
      case Right(_) => onSuccess
      case Left(_) =>
        logger.error(s"[StockDividendsSessionService][createSessionData] session create failed. correlation id: " + correlationId)
        onFail
    }
  }

  def getSessionData(taxYear: Int)
                    (implicit request: User[_], hc: HeaderCarrier): Future[Either[DatabaseError, Option[StockDividendsUserDataModel]]] = {

    getStockDividendsBackendConnector.getSessionData(taxYear)(
      hc.withExtraHeaders("mtditid" -> request.mtditid).withExtraHeaders("X-CorrelationId" -> correlationId)).map {
      case Right(userData) =>
        Right(userData)
      case Left(_) =>
        logger.error("[StockDividendsSessionService][getSessionData] Could not find user session. correlation id: " + correlationId)
        Left(DataNotFound)
    }
  }


  def updateSessionData[A](cyaModel: StockDividendsCheckYourAnswersModel, taxYear: Int)(onFail: A)(onSuccess: A)
                          (implicit request: User[_], hc: HeaderCarrier): Future[A] = {

    updateStockDividendsBackendConnector.updateSessionData(cyaModel, taxYear)(
      hc.withExtraHeaders("mtditid" -> request.mtditid).withExtraHeaders("X-CorrelationId" -> correlationId)).map {
      case Right(_) =>
        onSuccess
      case Left(_) =>
        logger.error(s"[StockDividendsSessionService][updateSessionData] session update failure. correlation id: " + correlationId)
        onFail
    }
  }

  def deleteSessionData[A](taxYear: Int)(onFail: A)(onSuccess: A)
                          (implicit user: User[_], hc: HeaderCarrier): Future[A] = {

    deleteStockDividendsBackendConnector.deleteSessionData(taxYear)(
      hc.withExtraHeaders("mtditid" -> user.mtditid).withExtraHeaders("X-CorrelationId" -> correlationId)).map {
      case Right(_) =>
        onSuccess
      case _ =>
        logger.error(s"[StockDividendsSessionService][deleteSessionData] session delete failure. correlation id: " + correlationId)
        onFail
    }
  }

  def getAndHandle[R](taxYear: Int)(onFail: Future[R])(block: (Option[StockDividendsCheckYourAnswersModel], Option[StockDividendsPriorDataModel]) => Future[R])
                     (implicit user: User[_], hc: HeaderCarrier): Future[R] = {
    for {
      optionalCya <- getSessionData(taxYear)
      priorDataResponse <- stockDividendsPriorDataService.getPriorData(taxYear)
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
  }.flatten

  def clear[R](taxYear: Int)(onFail: R)(onSuccess: R)(implicit user: User[_], hc: HeaderCarrier): Future[R] = {
    incomeSourceConnector.put(taxYear, user.nino, IncomeSources.STOCK_DIVIDENDS)(hc.withExtraHeaders("mtditid" -> user.mtditid)).flatMap {
      case Left(_) => Future.successful(onFail)
      case _ => deleteSessionData(taxYear)(onFail)(onSuccess)
    }
  }
}
