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
import connectors.stockdividends.{CreateStockDividendsBackendConnector, GetStockDividendsBackendConnector, UpdateStockDividendsBackendConnector}
import connectors.{IncomeSourceConnector, IncomeTaxUserDataConnector, StockDividendsUserDataConnector}
import models.dividends.StockDividendsCheckYourAnswersModel
import models.mongo.{DatabaseError, StockDividendsUserDataModel}
import models.priorDataModels.{IncomeSourcesModel, StockDividendsPriorDataModel}
import models.{APIErrorModel, User}
import play.api.Logger
import repositories.StockDividendsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StockDividendsSessionService @Inject()(
                                              stockDividendsUserDataRepository: StockDividendsUserDataRepository,
                                              stockDividendsUserDataConnector: StockDividendsUserDataConnector,
                                              createStockDividendsBackendConnector: CreateStockDividendsBackendConnector,
                                              updateStockDividendsBackendConnector: UpdateStockDividendsBackendConnector,
                                              getStockDividendsBackendConnector: GetStockDividendsBackendConnector,
                                              incomeTaxUserDataConnector: IncomeTaxUserDataConnector,
                                              incomeSourceConnector: IncomeSourceConnector
                                            ) {

  type StockDividendsPriorDataResponse = Either[APIErrorModel, Option[StockDividendsPriorDataModel]]

  lazy val logger: Logger = Logger(this.getClass)

  def getPriorData(taxYear: Int)(implicit user: User[_], ec: ExecutionContext, hc: HeaderCarrier): Future[StockDividendsPriorDataResponse] = {
    incomeTaxUserDataConnector.getUserData(taxYear)(user, hc.withExtraHeaders("mtditid" -> user.mtditid)).flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(ukDividends: IncomeSourcesModel) =>
        stockDividendsUserDataConnector.getUserData(taxYear)(user, hc.withExtraHeaders("mtditid" -> user.mtditid)).map{
          case Left(error) => Left(error)
          case Right(stockDividends) =>
            if (ukDividends.dividends.isDefined || stockDividends.isDefined) {
              Right(Some(StockDividendsPriorDataModel.getFromPrior(ukDividends, stockDividends)))
            } else {
              Right(None)
            }
        }
    }
  }

  def createSessionData[A](cyaModel: StockDividendsCheckYourAnswersModel, taxYear: Int)(onFail: A)(onSuccess: A)
                          (implicit user: User[_], ec: ExecutionContext, hc: HeaderCarrier): Future[A] = {

    createStockDividendsBackendConnector.createSessionData(cyaModel, taxYear)(hc.withExtraHeaders("mtditid" -> user.mtditid)).map {
      case Right(_) => onSuccess
      case Left(_) => onFail
    }
  }

  def getSessionData(taxYear: Int)(implicit user: User[_], ec: ExecutionContext, hc: HeaderCarrier):
  Future[Either[APIErrorModel, Option[StockDividendsUserDataModel]]] = {
    getStockDividendsBackendConnector.getSessionData(taxYear)(hc.withExtraHeaders("mtditid" -> user.mtditid)).map {
      case Left(error) =>
        logger.error("[StockDividendsSessionService][getSessionData] Could not find user session.")
        Left(error)
      case Right(userData) => Right(userData)
    }

  }

  def updateSessionData[A](cyaModel: StockDividendsCheckYourAnswersModel, taxYear: Int, needsCreating: Boolean = false)(onFail: A)(onSuccess: A)
                          (implicit user: User[_], ec: ExecutionContext, hc: HeaderCarrier): Future[A] = {

    if (needsCreating) {
      createStockDividendsBackendConnector.createSessionData(cyaModel, taxYear)(hc.withExtraHeaders("mtditid" -> user.mtditid)).map {
        case Right(_) => onSuccess
        case Left(_) => onFail
      }
    } else {
      updateStockDividendsBackendConnector.updateSessionData(cyaModel, taxYear)(hc.withExtraHeaders("mtditid" -> user.mtditid)).map {
        case Right(_) => onSuccess
        case Left(_) => onFail
      }
    }
  }

  def getAndHandle[R](taxYear: Int)(onFail: R)(block: (Option[StockDividendsUserDataModel], Option[StockDividendsPriorDataModel]) => Future[R])
                     (implicit executionContext: ExecutionContext, user: User[_], hc: HeaderCarrier): Future[R] = {
    val result = for {
      optionalCya <- getSessionData(taxYear)
      priorDataResponse <- getPriorData(taxYear)
    } yield {
      priorDataResponse match {
        case Right(prior) => optionalCya match {
          case Left(_) => Future(onFail)
          case Right(cyaData) => block(cyaData, prior)
        }
        case Left(_) => Future(onFail)
      }
    }
    result.flatten
  }

  def clear[R](taxYear: Int)(onFail: R)(onSuccess: R)(implicit user: User[_], ec: ExecutionContext, hc: HeaderCarrier): Future[R] = {
    incomeSourceConnector.put(taxYear, user.nino, IncomeSources.STOCK_DIVIDENDS)(hc.withExtraHeaders("mtditid" -> user.mtditid)).flatMap {
      case Left(_) => Future.successful(onFail)
      case _ =>
        stockDividendsUserDataRepository.clear(taxYear).map {
          case true => onSuccess
          case false => onFail
        }
    }
  }

}