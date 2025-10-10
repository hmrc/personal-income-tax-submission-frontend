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

package connectors.stockdividends

import connectors.httpParsers.stockdividends.GetStockDividendsSessionHttpParser.GetStockDividendsSessionResponse
import models.mongo.StockDividendsUserDataModel
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.OK
import test.utils.IntegrationTest
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class GetStockDividendsSessionConnectorISpec extends IntegrationTest {

  val connector: GetStockDividendsSessionConnector = app.injector.instanceOf[GetStockDividendsSessionConnector]

  implicit override val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid, "X-Session-ID" -> sessionId)

  private val url = s"/income-tax-dividends/income-tax/income/dividends/$taxYear/stock-dividends/session"

  "GetStockDividendsSessionConnectorISpec" should {
    "Return a success result" when {
      "request returns a 404" in {
        stubGetWithHeadersCheck(url, NOT_FOUND, "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: GetStockDividendsSessionResponse = Await.result(connector.getSessionData(taxYear), Duration.Inf)
        result shouldBe Right(None)
      }

      "request returns a 204" in {
        stubGetWithHeadersCheck(url, NO_CONTENT, "{}", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: GetStockDividendsSessionResponse = Await.result(connector.getSessionData(taxYear), Duration.Inf)
        result shouldBe Right(None)
      }

      "request returns a 200" in {
        stubGetWithHeadersCheck(url, OK, Json.toJson(stockDividendsUserDataModel).toString(), "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val parsedModel = Json.toJson(stockDividendsUserDataModel).validate[StockDividendsUserDataModel].fold(
          _ => StockDividendsUserDataModel(sessionId, mtditid, nino, taxYear, None),
          model => model
        )

        val result: GetStockDividendsSessionResponse = Await.result(connector.getSessionData(taxYear), Duration.Inf)
        result shouldBe Right(Some(parsedModel))
      }
    }

    "Return an error result" when {

      "request returns a 200 but invalid json" in {
        stubGetWithHeadersCheck(url, OK, Json.toJson("""{"invalid": true}""").toString(), "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: GetStockDividendsSessionResponse = Await.result(connector.getSessionData(taxYear), Duration.Inf)

        result shouldBe Left(
          APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API"))
        )
      }

      "request returns a 500" in {
        stubGetWithHeadersCheck(url, INTERNAL_SERVER_ERROR, """{"code": "FAILED", "reason": "failed"}""", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: GetStockDividendsSessionResponse = Await.result(connector.getSessionData(taxYear), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
      }

      "request returns a 503" in {
        stubGetWithHeadersCheck(url, SERVICE_UNAVAILABLE,
          """{"code": "FAILED", "reason": "failed"}""", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: GetStockDividendsSessionResponse = Await.result(connector.getSessionData(taxYear), Duration.Inf)
        result shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("FAILED", "failed")))
      }

      "request returns an unexpected result" in {
        stubGetWithHeadersCheck(url, IM_A_TEAPOT, """{"code": "FAILED", "reason": "failed"}""", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: GetStockDividendsSessionResponse = Await.result(connector.getSessionData(taxYear), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
      }
    }
  }
}
