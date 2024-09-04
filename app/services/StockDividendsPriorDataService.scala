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


import connectors.{IncomeTaxUserDataConnector, StockDividendsUserDataConnector}
import models.priorDataModels.{IncomeSourcesModel, StockDividendsPriorDataModel}
import models.{APIErrorModel, User}
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class StockDividendsPriorDataService @Inject()(
                                  stockDividendsUserDataConnector: StockDividendsUserDataConnector,
                                  incomeTaxUserDataConnector: IncomeTaxUserDataConnector
                                )(implicit executionContext: ExecutionContext) {

  type StockDividendsPriorDataResponse = Either[APIErrorModel, Option[StockDividendsPriorDataModel]]

  def getPriorData(taxYear: Int)(implicit user: User[_], hc: HeaderCarrier): Future[StockDividendsPriorDataResponse] = {
    incomeTaxUserDataConnector.getUserData(taxYear)(user, hc.withExtraHeaders("mtditid" -> user.mtditid)).flatMap {
      case Left(error) => Future.successful(Left(error))
      case Right(ukDividends: IncomeSourcesModel) =>
        stockDividendsUserDataConnector.getUserData(taxYear)(user, hc.withExtraHeaders("mtditid" -> user.mtditid)).map {
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
}
