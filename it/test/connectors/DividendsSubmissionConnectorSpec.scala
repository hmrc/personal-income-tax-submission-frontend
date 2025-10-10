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

package test.connectors

import com.github.tomakehurst.wiremock.http.HttpHeader
import config.{AppConfig, FrontendAppConfig}
import connectors.DividendsSubmissionConnector
import connectors.httpParsers.DividendsSubmissionHttpParser.DividendsSubmissionsResponse
import models.dividends.{DividendsResponseModel, DividendsSubmissionModel}
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.Configuration
import play.api.http.Status._
import test.utils.IntegrationTest
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class DividendsSubmissionConnectorSpec extends IntegrationTest {

  lazy val connector: DividendsSubmissionConnector = app.injector.instanceOf[DividendsSubmissionConnector]

  lazy val httpClient: HttpClientV2 = app.injector.instanceOf[HttpClientV2]

  def appConfig(host: String): AppConfig = new FrontendAppConfig(
    app.injector.instanceOf[ServicesConfig],
    app.injector.instanceOf[Configuration]
  ) {
    override val dividendsBaseUrl: String = s"http://$host:$wiremockPort/income-tax-dividends"
  }

  val body: DividendsSubmissionModel =  DividendsSubmissionModel(
    Some(10),
    Some(10)
  )

  val expectedHeaders = Seq(new HttpHeader("mtditid", mtditid))

  "DividendsSubmissionConnectorSpec" should {

    "include internal headers" when {
      val headersSentToDividends = Seq(new HttpHeader(HeaderNames.xSessionId, "sessionIdValue"), new HttpHeader("mtditid", mtditid))

      val internalHost = "localhost"
      val externalHost = "127.0.0.1"

      "the host for Dividends is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue"))).withExtraHeaders("mtditid"->mtditid)
        val connector = new DividendsSubmissionConnector(httpClient, appConfig(internalHost))

        stubPut(s"/income-tax-dividends/income-tax/nino/$nino/sources\\?taxYear=$taxYear", NO_CONTENT, "{}",
          headersSentToDividends)

        val result: DividendsSubmissionsResponse = Await.result(connector.submitDividends(body, nino, taxYear)(hc), Duration.Inf)

        result shouldBe Right(DividendsResponseModel(NO_CONTENT))
      }
      "the host for Dividends is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue"))).withExtraHeaders("mtditid"->mtditid)

        val connector = new DividendsSubmissionConnector(httpClient, appConfig(externalHost))

        stubPut(s"/income-tax-dividends/income-tax/nino/$nino/sources\\?taxYear=$taxYear", NO_CONTENT, "{}",
          headersSentToDividends)

        val result: DividendsSubmissionsResponse = Await.result(connector.submitDividends(body, nino, taxYear)(hc), Duration.Inf)

        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API")))
      }
    }

    "Return a success result" when {
      "Dividends returns a 204" in {
        stubPut(s"/income-tax-dividends/income-tax/nino/$nino/sources\\?taxYear=$taxYear", NO_CONTENT, "{}", expectedHeaders)
        val result: DividendsSubmissionsResponse = Await.result(connector.submitDividends(body, nino, taxYear), Duration.Inf)
        result shouldBe Right(DividendsResponseModel(NO_CONTENT))
      }

      "Dividends returns a 400" in {
        stubPut(s"/income-tax-dividends/income-tax/nino/$nino/sources\\?taxYear=$taxYear", BAD_REQUEST, "{}", expectedHeaders)
        val result = Await.result(connector.submitDividends(body, nino, taxYear), Duration.Inf)
        result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API")))
      }

      "Dividends returns a 500" in {
        stubPut(s"/income-tax-dividends/income-tax/nino/$nino/sources\\?taxYear=$taxYear", INTERNAL_SERVER_ERROR, "{}", expectedHeaders)
        val result = Await.result(connector.submitDividends(body, nino, taxYear), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API")))
      }
    }
  }

}
