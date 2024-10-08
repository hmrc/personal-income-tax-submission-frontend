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

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar.mock
import repositories.StockDividendsUserDataRepository
import services.StockDividendsSessionServiceImpl
import test.utils.IntegrationTest

import scala.concurrent.Future


class StockDividendsSessionServiceImplISpec extends IntegrationTest {

  val stockDividendsSessionServiceInvalidEncryption: StockDividendsSessionServiceImpl =
    appWithInvalidEncryptionKey.injector.instanceOf[StockDividendsSessionServiceImpl]

  "create" should {
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

  "update" should {
    "return false when failing to decrypt the model" in {
      val result = await(stockDividendsSessionServiceInvalidEncryption.updateSessionData(completeStockDividendsCYAModel, taxYear)(false)(true))
      result shouldBe false
    }
  }

  "clear" should {
    "return true when success" in {
      val result = await(stockDividendsSessionService.clear(taxYear)(false)(true))
      result shouldBe true
    }

    "return false when failure" in {
      val mockRepo = mock[StockDividendsUserDataRepository]

      val service = new StockDividendsSessionServiceImpl(mockRepo, stockDividendsUserDataConnector , incomeTaxUserDataConnector ,incomeSourceConnector)

      when(mockRepo.clear(any())(any())).thenReturn(
        Future.successful(false)
      )
      val result = await(service.clear(taxYear)(false)(true))
      result shouldBe false
    }
  }
}
