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
import connectors.httpParsers.stockdividends.UpdateStockDividendsSessionHttpParser.UpdateStockDividendsSessionResponse
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.NO_CONTENT
import test.utils.IntegrationTest
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, SessionId}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class UpdateStockDividendsBackendConnectorISpec extends IntegrationTest {

  lazy val connector: UpdateStockDividendsSessionConnector = app.injector.instanceOf[UpdateStockDividendsSessionConnector]

  lazy val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

  implicit val hc: HeaderCarrier = HeaderCarrier()

  private val url = s"/income-tax-dividends/income-tax/income/dividends/$taxYear/stock-dividends/session"
  
  "UpdateStockDividendsBackendConnector" should {

    "include internal headers" when {
      val headersSentToGainsSubmission= Seq(new HttpHeader(HeaderNames.xSessionId, "sessionIdValue"), new HttpHeader("mtditid", mtditid))


      "the host is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue"))).withExtraHeaders("mtditid"->mtditid)
        val connector = new UpdateStockDividendsSessionConnector(httpClient, appConfig)

        stubPut(url, NO_CONTENT, "{}", headersSentToGainsSubmission)

        val result: UpdateStockDividendsSessionResponse =
          Await.result(connector.updateSessionData(stockDividendsCheckYourAnswersModel, taxYear)(hc), Duration.Inf)

        result shouldBe Right(NO_CONTENT)
      }
    }

    "Return a success result" when {
      "Gains submission returns a 204" in {
        stubPut(url, NO_CONTENT, "{}")
        val result: UpdateStockDividendsSessionResponse =
          Await.result(connector.updateSessionData(stockDividendsCheckYourAnswersModel, taxYear)(hc), Duration.Inf)
        result shouldBe Right(NO_CONTENT)
      }

      "Gains submission returns a 400" in {
        stubPut(url, BAD_REQUEST, "{}")
        val result: UpdateStockDividendsSessionResponse =
          Await.result(connector.updateSessionData(stockDividendsCheckYourAnswersModel, taxYear)(hc), Duration.Inf)
        result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API")))
      }

      "Gains submission returns an error parsing from API 500 response" in {
        stubPut(url, INTERNAL_SERVER_ERROR, "{}")
        val result: UpdateStockDividendsSessionResponse =
          Await.result(connector.updateSessionData(stockDividendsCheckYourAnswersModel, taxYear)(hc), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API")))
      }

      "Gains submission returns an unexpected status error 500 response" in {
        val responseBody = Json.obj(
          "code" -> "INTERNAL_SERVER_ERROR",
          "reason" -> "Unexpected status returned from API"
        )

        stubPut(url, CREATED, responseBody.toString())
        val result: UpdateStockDividendsSessionResponse =
          Await.result(connector.updateSessionData(stockDividendsCheckYourAnswersModel, taxYear)(hc), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Unexpected status returned from API")))
      }

      "Gains submission returns a 500 when service is unavailable" in {
        val responseBody = Json.obj(
          "code" -> "SERVICE_UNAVAILABLE",
          "reason" -> "the service is currently unavailable"
        )

        stubPut(url, SERVICE_UNAVAILABLE, responseBody.toString())
        val result: UpdateStockDividendsSessionResponse =
          Await.result(connector.updateSessionData(stockDividendsCheckYourAnswersModel, taxYear)(hc), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("SERVICE_UNAVAILABLE", "the service is currently unavailable")))
      }

    }
  }
}
