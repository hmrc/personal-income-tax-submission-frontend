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

import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK}
import play.api.libs.json.Json
import test.utils.IntegrationTest


class StockDividendsBackendSessionServiceImplISpec extends IntegrationTest {

  val service: StockDividendsBackendSessionServiceImpl =
    appWithStockDividendsBackendMongo.injector.instanceOf[StockDividendsBackendSessionServiceImpl]

  private val url = s"/income-tax-dividends/income-tax/income/dividends/$taxYear/stock-dividends/session"

  "create" should {

    "return true when successful" in {
      stubPost(url, NO_CONTENT, Json.toJson(completeStockDividendsCYAModel).toString())
      val result = await(service.createSessionData(completeStockDividendsCYAModel, taxYear)(false)(true))
      result shouldBe true
    }

    "return false when fails" in {
      stubPost(url, INTERNAL_SERVER_ERROR, Json.toJson(completeStockDividendsCYAModel).toString())
      val result = await(service.createSessionData(completeStockDividendsCYAModel, taxYear)(false)(true))
      result shouldBe false
    }
  }

  "update" should {
    "return true when success" in {
      stubPutWithRequestBody(url, NO_CONTENT, Json.toJson(completeStockDividendsCYAModel).toString())
      val result = await(service.updateSessionData(completeStockDividendsCYAModel, taxYear)(false)(true))
      result shouldBe true
    }

    "return false when fails" in {
      stubPutWithRequestBody(url, INTERNAL_SERVER_ERROR, Json.toJson(completeStockDividendsCYAModel).toString())
      val result = await(service.updateSessionData(completeStockDividendsCYAModel, taxYear)(false)(true))
      result shouldBe false
    }
  }

  "delete" should {
    "return true when success" in {
      stubDelete(url, NO_CONTENT, "{}")
      val result = await(service.deleteSessionData(taxYear)(false)(true))
      result shouldBe true
    }

    "return false when fails" in {
      stubDelete(url, INTERNAL_SERVER_ERROR, "{}")
      val result = await(service.deleteSessionData(taxYear)(false)(true))
      result shouldBe false
    }
  }

  "clear" should {
    "return true when success" in {
      stubDelete(url, NO_CONTENT, "{}")
      val result = await(service.clear(taxYear)(false)(true))
      result shouldBe true
    }

    "return false when fails" in {
      stubDelete(url, INTERNAL_SERVER_ERROR, "{}")
      val result = await(service.clear(taxYear)(false)(true))
      result shouldBe false
    }
  }
}
