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
import connectors.httpParsers.IncomeTaxUserDataHttpParser.IncomeTaxUserDataResponse
import connectors.{IncomeSourceConnector, IncomeTaxUserDataConnector}
import models.User
import models.mongo.{DatabaseError, SavingsIncomeUserDataModel}
import models.savings.{SavingsIncomeCYAModel, SavingsIncomeDataModel}
import play.api.Logger
import repositories.SavingsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Instant
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class SavingsSessionService @Inject()(
                                         savingsUserDataRepository: SavingsUserDataRepository,
                                         incomeTaxUserDataConnector: IncomeTaxUserDataConnector,
                                         incomeSourceConnector: IncomeSourceConnector
                                       ) {

  lazy val logger: Logger = Logger(this.getClass)

  def getPriorData(taxYear: Int)(implicit user: User[_], hc: HeaderCarrier): Future[IncomeTaxUserDataResponse] = {
    incomeTaxUserDataConnector.getUserData(taxYear)(user, hc.withExtraHeaders("mtditid" -> user.mtditid))
  }

  def createSessionData[A](cyaModel: SavingsIncomeCYAModel, taxYear: Int)(onFail: A)(onSuccess: A)
                          (implicit user: User[_], ec: ExecutionContext): Future[A] = {

    val userData = SavingsIncomeUserDataModel(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear,
      Some(cyaModel),
      Instant.now()
    )

    savingsUserDataRepository.create(userData)().map {
      case Right(_) => onSuccess
      case Left(_) => onFail
    }
  }

  def getSessionData(taxYear: Int)(implicit user: User[_], ec: ExecutionContext): Future[Either[DatabaseError, Option[SavingsIncomeUserDataModel]]] = {

    savingsUserDataRepository.find(taxYear).map {
      case Left(error) =>
        logger.error("[SavingsSessionService][getSessionData] Could not find user session.")
        Left(error)
      case Right(userData) => Right(userData)
    }
  }

  def updateSessionData[A](cyaModel: SavingsIncomeCYAModel, taxYear: Int, needsCreating: Boolean = false)(onFail: A)(onSuccess: A)
                          (implicit user: User[_], ec: ExecutionContext): Future[A] = {
    val userData = SavingsIncomeUserDataModel(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear,
      Some(cyaModel),
      Instant.now()
    )

    if (needsCreating) {
      savingsUserDataRepository.create(userData)().map {
        case Right(_) => onSuccess
        case Left(_) => onFail
      }
    } else {
      savingsUserDataRepository.update(userData).map {
        case Right(_) => onSuccess
        case Left(_) => onFail
      }
    }
  }

  def getAndHandle[R](taxYear: Int)(onFail: R)(block: (Option[SavingsIncomeCYAModel], Option[SavingsIncomeDataModel]) => Future[R])
                     (implicit executionContext: ExecutionContext, user: User[_], hc: HeaderCarrier): Future[R] = {
    val result = for {
      optionalCya <- getSessionData(taxYear)
      priorDataResponse <- getPriorData(taxYear)
    } yield {
      priorDataResponse.map(_.interestSavings) match {
        case Right(prior) => optionalCya match {
          case Left(_) =>  Future(onFail)
          case Right(cyaData) => block(cyaData.flatMap(_.savingsIncome), prior)
        }
        case Left(_) =>  Future(onFail)
      }
    }
    result.flatten
  }

  def clear[R](taxYear: Int)(onFail: R)(onSuccess: R)(implicit user: User[_], ec: ExecutionContext, hc: HeaderCarrier): Future[R] = {
    incomeSourceConnector.put(taxYear, user.nino, IncomeSources.INTEREST_SAVINGS)(hc.withExtraHeaders("mtditid" -> user.mtditid)).flatMap {
      case Left(_) => Future.successful(onFail)
      case _ =>
        savingsUserDataRepository.clear(taxYear).map {
          case true => onSuccess
          case false => onFail
        }
    }
  }

}
