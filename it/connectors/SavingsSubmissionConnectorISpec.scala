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

package connectors

import com.github.tomakehurst.wiremock.http.HttpHeader
import config.{AppConfig, FrontendAppConfig}
import connectors.httpParsers.SavingsSubmissionHttpParser.SavingsSubmissionResponse
import models.savings.{SavingsSubmissionModel, SecuritiesModel}
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.IntegrationTest

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class SavingsSubmissionConnectorISpec extends IntegrationTest{

  lazy val connector: SavingsSubmissionConnector = app.injector.instanceOf[SavingsSubmissionConnector]

  lazy val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

  def appConfig(host: String): AppConfig = new FrontendAppConfig(app.injector.instanceOf[ServicesConfig]) {
    override lazy val interestBaseUrl: String = s"http://$host:$wiremockPort/income-tax-interest"
  }

  val body: SavingsSubmissionModel =  SavingsSubmissionModel(
    Some(SecuritiesModel(taxTakenOff = Some(500), grossAmount = 2000, netAmount = Some(1500))),
    None
  )

  val expectedHeaders: Seq[HttpHeader] = Seq(new HttpHeader("mtditid", mtditid))

  "SavingsSubmissionConnectorSpec" should {

    "include internal headers" when {
      val headersSent = Seq(new HttpHeader(HeaderNames.xSessionId, "sessionIdValue"), new HttpHeader("mtditid", mtditid))

      val internalHost = "localhost"

      "the host for Savings is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue"))).withExtraHeaders("mtditid" -> mtditid)
        val connector = new SavingsSubmissionConnector(httpClient, appConfig(internalHost))

        stubPut(s"/income-tax-interest/income-tax/nino/$nino/savings\\?taxYear=$taxYear", NO_CONTENT, "{}",
          headersSent)

        val result: SavingsSubmissionResponse = Await.result(connector.submitSavings(body, nino, taxYear)(hc, ec), Duration.Inf)

        result shouldBe Right(true)
      }
    }

    "Return a success result" when {
      "Savings returns a 204" in {
        stubPut(s"/income-tax-interest/income-tax/nino/$nino/savings\\?taxYear=$taxYear", NO_CONTENT, "{}", expectedHeaders)
        val result: SavingsSubmissionResponse = Await.result(connector.submitSavings(body, nino, taxYear), Duration.Inf)
        result shouldBe Right(true)
      }
    }
    "Return a fail result" when {

      "Savings returns a 400" in {
        stubPut(s"/income-tax-interest/income-tax/nino/$nino/savings\\?taxYear=$taxYear", BAD_REQUEST, "{}", expectedHeaders)
        val result = Await.result(connector.submitSavings(body, nino, taxYear), Duration.Inf)
        result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API")))
      }

      "Savings returns a 503" in {
        stubPut(s"/income-tax-interest/income-tax/nino/$nino/savings\\?taxYear=$taxYear", SERVICE_UNAVAILABLE, "{}", expectedHeaders)
        val result = Await.result(connector.submitSavings(body, nino, taxYear), Duration.Inf)
        result shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API")))
      }

      "Savings returns a 500" in {
        stubPut(s"/income-tax-interest/income-tax/nino/$nino/savings\\?taxYear=$taxYear", INTERNAL_SERVER_ERROR, "{}", expectedHeaders)
        val result = Await.result(connector.submitSavings(body, nino, taxYear), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API")))
      }

      "Savings returns a 511" in {
        stubPut(s"/income-tax-interest/income-tax/nino/$nino/savings\\?taxYear=$taxYear", NETWORK_AUTHENTICATION_REQUIRED, "{}", expectedHeaders)
        val result = Await.result(connector.submitSavings(body, nino, taxYear), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API")))
      }
    }
  }

}
