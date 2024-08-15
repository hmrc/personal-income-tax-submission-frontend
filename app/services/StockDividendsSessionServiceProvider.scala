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

import models.User
import models.dividends.StockDividendsCheckYourAnswersModel
import models.mongo.{DatabaseError, StockDividendsUserDataModel}
import models.priorDataModels.StockDividendsPriorDataModel
import models.requests.AuthorisationRequest
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait StockDividendsSessionServiceProvider {

  def createSessionData[A](cyaModel: StockDividendsCheckYourAnswersModel, taxYear: Int)(onFail: A)(onSuccess: A)
                          (implicit request: User[_], hc: HeaderCarrier): Future[A]

  def getSessionData(taxYear: Int)
                    (implicit request: User[_], hc: HeaderCarrier): Future[Either[DatabaseError, Option[StockDividendsUserDataModel]]]


  def updateSessionData[A](cyaModel: StockDividendsCheckYourAnswersModel, taxYear: Int)(onFail: A)(onSuccess: A)
                          (implicit request: User[_], hc: HeaderCarrier): Future[A]

  def deleteSessionData[A](taxYear: Int)(onFail: A)(onSuccess: A)
                          (implicit request: User[_], hc: HeaderCarrier): Future[A]

  def getAndHandle[R](taxYear: Int)(onFail: R)(block: (Option[StockDividendsCheckYourAnswersModel], Option[StockDividendsPriorDataModel]) => R)
                     (implicit request: User[_], hc: HeaderCarrier): Future[R]

  def clear[R](taxYear: Int)(onFail: R)(onSuccess: R)(implicit user: User[_], hc: HeaderCarrier): Future[R]

  def createOrUpdateSessionData[A](cyaModel: StockDividendsCheckYourAnswersModel, taxYear: Int, needsCreating: Boolean)(onFail: A)(onSuccess: A)
                          (implicit request: User[_], hc: HeaderCarrier): Future[A] = {
    if (needsCreating) createSessionData(cyaModel, taxYear)(onFail)(onSuccess)
    else updateSessionData(cyaModel, taxYear)(onFail)(onSuccess)
  }
}
