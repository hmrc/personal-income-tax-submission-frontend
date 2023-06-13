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
import connectors.httpParsers.StockDividendsSubmissionHttpParser.StockDividendsSubmissionResponse
import models.dividends.{ForeignDividendModel, StockDividendModel, StockDividendsSubmissionModel}
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.IntegrationTest

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class StockDividendsSubmissionConnectorSpec extends IntegrationTest{

  lazy val connector: StockDividendsSubmissionConnector = app.injector.instanceOf[StockDividendsSubmissionConnector]

  lazy val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

  def appConfig(host: String): AppConfig = new FrontendAppConfig(app.injector.instanceOf[ServicesConfig]) {
    override lazy val dividendsBaseUrl: String = s"http://$host:$wiremockPort/income-tax-dividends"
  }

  val reference: String = "RefNo13254687"
  val countryCode: String = "JLB"
  val decimalValue: BigDecimal = 123.45

  val body: StockDividendsSubmissionModel = StockDividendsSubmissionModel(
    foreignDividend =
      Some(Seq(
        ForeignDividendModel(countryCode, Some(decimalValue), Some(decimalValue), Some(decimalValue), Some(true), decimalValue)
      ))
    ,
    dividendIncomeReceivedWhilstAbroad = Some(
      Seq(
        ForeignDividendModel(countryCode, Some(decimalValue), Some(decimalValue), Some(decimalValue), Some(true), decimalValue)
      )
    ),
    stockDividend = Some(StockDividendModel(Some(reference), decimalValue)),
    redeemableShares = Some(StockDividendModel(Some(reference), decimalValue)),
    bonusIssuesOfSecurities = Some(StockDividendModel(Some(reference), decimalValue)),
    closeCompanyLoansWrittenOff = Some(StockDividendModel(Some(reference), decimalValue))
  )

  val expectedHeaders = Seq(new HttpHeader("mtditid", mtditid))

  "StockDividendsSubmissionConnectorSpec" should {

    "include internal headers" when {
      val headersSentToDividends = Seq(new HttpHeader(HeaderNames.xSessionId, "sessionIdValue"), new HttpHeader("mtditid", mtditid))

      val internalHost = "localhost"
      val externalHost = "127.0.0.1"

      "the host for Dividends is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue"))).withExtraHeaders("mtditid"->mtditid)
        val connector = new StockDividendsSubmissionConnector(httpClient, appConfig(internalHost))

        stubPut(s"/income-tax-dividends/income-tax/income/dividends/$nino/$taxYear", NO_CONTENT, "{}",
          headersSentToDividends)

        val result: StockDividendsSubmissionResponse = Await.result(connector.submitDividends(body, nino, taxYear)(hc), Duration.Inf)

        result shouldBe Right(true)
      }
      "the host for Dividends is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue"))).withExtraHeaders("mtditid"->mtditid)

        val connector = new StockDividendsSubmissionConnector(httpClient, appConfig(externalHost))

        stubPut(s"/income-tax-dividends/income-tax/income/dividends/$nino/$taxYear", NO_CONTENT, "{}",
          headersSentToDividends)

        val result: StockDividendsSubmissionResponse = Await.result(connector.submitDividends(body, nino, taxYear)(hc), Duration.Inf)

        result shouldBe Left(APIErrorModel(NOT_FOUND, APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API")))
      }
    }

    "Return a success result" when {
      "Dividends returns a 204" in {
        stubPut(s"/income-tax-dividends/income-tax/income/dividends/$nino/$taxYear", NO_CONTENT, "{}", expectedHeaders)
        val result: StockDividendsSubmissionResponse = Await.result(connector.submitDividends(body, nino, taxYear), Duration.Inf)
        result shouldBe Right(true)
      }

      "Dividends returns a 400" in {
        stubPut(s"/income-tax-dividends/income-tax/income/dividends/$nino/$taxYear", BAD_REQUEST, "{}", expectedHeaders)
        val result = Await.result(connector.submitDividends(body, nino, taxYear), Duration.Inf)
        result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API")))
      }

      "Dividends returns a 500" in {
        stubPut(s"/income-tax-dividends/income-tax/income/dividends/$nino/$taxYear", INTERNAL_SERVER_ERROR, "{}", expectedHeaders)
        val result = Await.result(connector.submitDividends(body, nino, taxYear), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API")))
      }
    }
  }

}
