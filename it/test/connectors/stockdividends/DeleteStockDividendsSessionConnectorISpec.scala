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

import connectors.httpParsers.stockdividends.DeleteStockDividendsSessionHttpParser.DeleteStockDividendsSessionResponse
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status._
import test.utils.IntegrationTest
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, SessionId}

class DeleteStockDividendsSessionConnectorISpec extends IntegrationTest {

  private val connector: DeleteStockDividendsSessionConnector = app.injector.instanceOf[DeleteStockDividendsSessionConnector]
  private val httpClient: HttpClient = app.injector.instanceOf[HttpClient]
  private val hc: HeaderCarrier = HeaderCarrier()
  private val url = s"/income-tax-dividends/income-tax/income/dividends/$taxYear/stock-dividends/session"


  "DeleteStockDividendsSessionConnectorISpec " should {

    "include internal headers" when {

      "the host is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new DeleteStockDividendsSessionConnector(httpClient, appConfig)

        stubDelete(url, NO_CONTENT, "{}")

        val result: DeleteStockDividendsSessionResponse = await(connector.deleteSessionData(taxYear)(hc))

        result shouldBe Right(true)
      }

      "the host is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue")))
        val connector = new DeleteStockDividendsSessionConnector(httpClient, appConfig)

        stubDelete(url, NO_CONTENT, "{}")

        val result: DeleteStockDividendsSessionResponse = await(connector.deleteSessionData(taxYear)(hc))

        result shouldBe Right(true)
      }
    }

    "handle error" when {

      val errorBodyModel = APIErrorBodyModel("CODE", "REASON")

      Seq(INTERNAL_SERVER_ERROR, SERVICE_UNAVAILABLE, NOT_FOUND, BAD_REQUEST).foreach { status =>

        s"The endpoint returns $status" in {
          val ifError = APIErrorModel(status, errorBodyModel)

          stubDelete(url, status, ifError.toJson.toString())

          val result: DeleteStockDividendsSessionResponse = await(connector.deleteSessionData(taxYear)(hc))

          result shouldBe Left(ifError)
        }
      }

      "The endpoint returns an unexpected error code - 502" in {
        val ifError = APIErrorModel(BAD_GATEWAY, errorBodyModel)

        stubDelete(url, BAD_GATEWAY, ifError.toJson.toString())

        val result: DeleteStockDividendsSessionResponse = await(connector.deleteSessionData(taxYear)(hc))

        result shouldBe Left(ifError)
      }
    }
  }
}
