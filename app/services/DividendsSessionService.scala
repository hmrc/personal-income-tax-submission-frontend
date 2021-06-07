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

package services

import connectors.IncomeTaxUserDataConnector
import connectors.httpParsers.IncomeTaxUserDataHttpParser.IncomeTaxUserDataResponse
import models.User
import models.dividends.{DividendsCheckYourAnswersModel, DividendsPriorSubmission}
import models.mongo.DividendsUserDataModel
import org.joda.time.{DateTime, DateTimeZone}
import repositories.DividendsUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DividendsSessionService @Inject()(
                                        dividendsUserDataRepository: DividendsUserDataRepository,
                                        incomeTaxUserDataConnector: IncomeTaxUserDataConnector
                                      ) {

  def getPriorData(taxYear: Int)(implicit user: User[_], hc: HeaderCarrier): Future[IncomeTaxUserDataResponse] = {
    incomeTaxUserDataConnector.getUserData(taxYear)(user, hc.withExtraHeaders("mtditid" -> user.mtditid))
  }

  def createSessionData[A](cyaModel: DividendsCheckYourAnswersModel, taxYear: Int)(onFail: A)(onSuccess: A)
                          (implicit user: User[_], ec: ExecutionContext): Future[A] = {

    val userData = DividendsUserDataModel(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear,
      Some(cyaModel),
      DateTime.now(DateTimeZone.UTC)
    )

    dividendsUserDataRepository.create(userData).map {
      case true => onSuccess
      case false => onFail
    }
  }

  def getSessionData(taxYear: Int)(implicit user: User[_]): Future[Option[DividendsUserDataModel]] = {
    dividendsUserDataRepository.find(taxYear)
  }

  def updateSessionData[A](cyaModel: DividendsCheckYourAnswersModel, taxYear: Int)(onFail: A)(onSuccess: A)
                          (implicit user: User[_], ec: ExecutionContext): Future[A] = {

    val userData = DividendsUserDataModel(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear,
      Some(cyaModel),
      DateTime.now(DateTimeZone.UTC)
    )

    dividendsUserDataRepository.update(userData).map {
      case true => onSuccess
      case false => onFail
    }
  }

  def getAndHandle[R](taxYear: Int)(onFail: R)(block: (Option[DividendsCheckYourAnswersModel], Option[DividendsPriorSubmission]) => R)
                                         (implicit executionContext: ExecutionContext, user: User[_], hc: HeaderCarrier): Future[R] = {
    for {
      optionalCya <- getSessionData(taxYear)
      priorDataResponse <- getPriorData(taxYear)
    } yield {
      priorDataResponse.map(_.dividends) match {
        case Right(prior) => block(optionalCya.flatMap(_.dividends), prior)
        case Left(_) => onFail
      }
    }
  }

  def clear[R](taxYear: Int)(onFail: R)(onSuccess: R)(implicit user: User[_], ec: ExecutionContext): Future[R] = {
    dividendsUserDataRepository.clear(taxYear).map {
      case true => onSuccess
      case false => onFail
    }
  }

}
