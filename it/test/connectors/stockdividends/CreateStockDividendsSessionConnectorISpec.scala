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

import com.github.tomakehurst.wiremock.http.HttpHeader
import connectors.httpParsers.stockdividends.CreateStockDividendsSessionHttpParser.CreateStockDividendsSessionResponse
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status._
import play.api.libs.json.Json
import test.utils.IntegrationTest
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, SessionId}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class CreateStockDividendsSessionConnectorISpec extends IntegrationTest {

  private val connector: CreateStockDividendsSessionConnector = app.injector.instanceOf[CreateStockDividendsSessionConnector]

  private val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

  private val hc: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid, "X-Session-ID" -> sessionId)

  private val url = s"/income-tax-dividends/income-tax/income/dividends/$taxYear/stock-dividends/session"

  "CreateStockDividendsSessionConnector" should {

    "include internal headers" when {
      val headers = Seq(new HttpHeader("X-Session-Id", "sessionIdValue"), new HttpHeader("mtditid", mtditid))

      "the host is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue"))).withExtraHeaders("mtditid" -> mtditid)
        val connector = new CreateStockDividendsSessionConnector(httpClient, appConfig)

        stubPost(url, NO_CONTENT, "{}", headers)

        val result: CreateStockDividendsSessionResponse =
          Await.result(connector.createSessionData(stockDividendsCheckYourAnswersModel, taxYear)(hc), Duration.Inf)

        result shouldBe Right(NO_CONTENT)
      }
    }

    "Return a success result" when {
      "create session returns a 204" in {
        stubPost(url, NO_CONTENT, "{}")
        val result: CreateStockDividendsSessionResponse =
          Await.result(connector.createSessionData(stockDividendsCheckYourAnswersModel, taxYear)(hc), Duration.Inf)
        result shouldBe Right(NO_CONTENT)
      }

      "create session returns a 400" in {
        stubPost(url, BAD_REQUEST, "{}")
        val result: CreateStockDividendsSessionResponse =
          Await.result(connector.createSessionData(stockDividendsCheckYourAnswersModel, taxYear)(hc), Duration.Inf)
        result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API")))
      }

      "create session returns an error parsing from API 500 response" in {
        stubPost(url, INTERNAL_SERVER_ERROR, "{}")
        val result: CreateStockDividendsSessionResponse =
          Await.result(connector.createSessionData(stockDividendsCheckYourAnswersModel, taxYear)(hc), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API")))
      }

      "create session returns an unexpected status error 500 response" in {
        val responseBody = Json.obj(
          "code" -> "INTERNAL_SERVER_ERROR",
          "reason" -> "Unexpected status returned from API"
        )

        stubPost(url, CREATED, responseBody.toString())
        val result: CreateStockDividendsSessionResponse =
          Await.result(connector.createSessionData(stockDividendsCheckYourAnswersModel, taxYear)(hc), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Unexpected status returned from API")))
      }

      "create session returns a 500 when service is unavailable" in {
        val responseBody = Json.obj(
          "code" -> "SERVICE_UNAVAILABLE",
          "reason" -> "the service is currently unavailable"
        )

        stubPost(url, SERVICE_UNAVAILABLE, responseBody.toString())
        val result: CreateStockDividendsSessionResponse =
          Await.result(connector.createSessionData(stockDividendsCheckYourAnswersModel, taxYear)(hc), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("SERVICE_UNAVAILABLE", "the service is currently unavailable")))
      }
    }
  }
}
