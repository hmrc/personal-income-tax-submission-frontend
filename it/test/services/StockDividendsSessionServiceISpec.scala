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

package test.services

import connectors.{IncomeSourceConnector, IncomeTaxUserDataConnector, StockDividendsBackendConnector, StockDividendsUserDataConnector}
import repositories.StockDividendsUserDataRepository
import services.StockDividendsSessionService
import test.utils.IntegrationTest


class StockDividendsSessionServiceISpec extends IntegrationTest{

  val stockDividendsUserDataRepository: StockDividendsUserDataRepository = app.injector.instanceOf[StockDividendsUserDataRepository]
  val stockDividendsUserDataConnector: StockDividendsUserDataConnector = app.injector.instanceOf[StockDividendsUserDataConnector]
  val stockDividendsBackendConnector: StockDividendsBackendConnector = app.injector.instanceOf[StockDividendsBackendConnector]
  val incomeTaxUserDataConnector: IncomeTaxUserDataConnector = app.injector.instanceOf[IncomeTaxUserDataConnector]
  val incomeSourceConnector: IncomeSourceConnector = app.injector.instanceOf[IncomeSourceConnector]

  val stockDividendsSessionServiceInvalidEncryption: StockDividendsSessionService =
    appWithInvalidEncryptionKey.injector.instanceOf[StockDividendsSessionService]

  val stockDividendsSessionService: StockDividendsSessionService = new StockDividendsSessionService(
    stockDividendsUserDataRepository,
    stockDividendsUserDataConnector,
    stockDividendsBackendConnector,
    incomeTaxUserDataConnector,
    incomeSourceConnector)

  "create" should{
    "return false when failing to decrypt the model" in {
      val result = await(stockDividendsSessionServiceInvalidEncryption.createSessionData(completeStockDividendsCYAModel, taxYear)(false)(true))
      result shouldBe false
    }
    "return true when successful and false when adding a duplicate" in {
      await(stockDividendsUserDataRepository.collection.drop().toFuture())
      await(stockDividendsUserDataRepository.ensureIndexes())
      val initialResult = await(stockDividendsSessionService.createSessionData(completeStockDividendsCYAModel, taxYear)(false)(true))
      val duplicateResult = await(stockDividendsSessionService.createSessionData(completeStockDividendsCYAModel, taxYear)(false)(true))
      initialResult shouldBe true
      duplicateResult shouldBe false
    }
  }

  "update" should{
    "return false when failing to decrypt the model" in {
      val result = await(stockDividendsSessionServiceInvalidEncryption.updateSessionData(completeStockDividendsCYAModel, taxYear)(false)(true))
      result shouldBe false
    }
  }

}
