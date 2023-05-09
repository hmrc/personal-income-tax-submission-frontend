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

import connectors.httpParsers.StockDividendsUserDataHttpParser.StockDividendsUserDataResponse
import models.dividends.{ForeignDividendModel, StockDividendModel, StockDividendsPriorSubmission}
import models.{APIErrorBodyModel, APIErrorModel, User}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.OK
import uk.gov.hmrc.http.HeaderCarrier
import utils.IntegrationTest

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class StockDividendsUserDataConnectorISpec extends IntegrationTest {

  lazy val connector: StockDividendsUserDataConnector = app.injector.instanceOf[StockDividendsUserDataConnector]

  val testUser: User[AnyContentAsEmpty.type] = User(mtditid, None, nino, "individual", sessionId)(FakeRequest())

  implicit override val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid, "X-Session-ID" -> sessionId)

  val url = s"/income-tax-dividends/income-tax/income/dividends/${user.nino}/$taxYear"

  "StockDividendsUserDataConnector" should {

    "return a success result" when {

      "submission returns a 404" in {

        stubGetWithHeadersCheck(url, NOT_FOUND,"{}", xSessionId, "mtditid" -> mtditid)

        val result: StockDividendsUserDataResponse = Await.result(connector.getUserData(taxYear)(testUser, headerCarrier), Duration.Inf)
        result shouldBe Right(StockDividendsPriorSubmission())
      }

      "submission returns a 200" in {
        val stockDividendsPriorSubmission = StockDividendsPriorSubmission(
          submittedOn = Some("2020-06-17T10:53:38Z"),
          foreignDividend = Some(Seq(ForeignDividendModel("BES", Some(2323.56), Some(5435.56), Some(4564.67), Some(true), 4564.67))),
          dividendIncomeReceivedWhilstAbroad = Some(Seq(ForeignDividendModel("CHN", Some(5664.67), Some(5657.56),
            Some(4644.56), Some(true), 4654.56))),
          stockDividend = Some(StockDividendModel(Some("Stock Dividend Customer Reference"), 2525.89)),
          redeemableShares = Some(StockDividendModel(Some("Redeemable Shares Customer Reference"), 3535.56)),
          bonusIssuesOfSecurities = Some(StockDividendModel(Some("Bonus Issues Of Securities Customer Reference"), 5633.67)),
          closeCompanyLoansWrittenOff = Some(StockDividendModel(Some("Close Company Loans WrittenOff Customer Reference"), 6743.23))
        )

        stockDividendsUserDataStub(stockDividendsPriorSubmission, nino, taxYear)

        val result: StockDividendsUserDataResponse = Await.result(connector.getUserData(taxYear)(testUser, headerCarrier), Duration.Inf)
        result shouldBe Right(stockDividendsPriorSubmission)
      }
    }

    "Return an error result" when {

      "submission returns a 200 but invalid json" in {

        stubGetWithHeadersCheck(url, OK, Json.toJson("""{"invalid": true}""").toString(), "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: StockDividendsUserDataResponse = Await.result(connector.getUserData(taxYear)(testUser, headerCarrier), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      "submission returns a 422" in {

        stubGetWithHeadersCheck(url, UNPROCESSABLE_ENTITY,"""{"code": "FAILED", "reason": "failed"}""", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: StockDividendsUserDataResponse = Await.result(connector.getUserData(taxYear)(testUser, headerCarrier), Duration.Inf)
        result shouldBe Left(APIErrorModel(UNPROCESSABLE_ENTITY, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns a 500" in {

        stubGetWithHeadersCheck(url, INTERNAL_SERVER_ERROR,"""{"code": "FAILED", "reason": "failed"}""", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: StockDividendsUserDataResponse = Await.result(connector.getUserData(taxYear)(testUser, headerCarrier), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns a 503" in {

        stubGetWithHeadersCheck(url, SERVICE_UNAVAILABLE,"""{"code": "FAILED", "reason": "failed"}""", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: StockDividendsUserDataResponse = Await.result(connector.getUserData(taxYear)(testUser, headerCarrier), Duration.Inf)
        result shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns an unexpected result" in {

        stubGetWithHeadersCheck(url, BAD_REQUEST,"""{"code": "FAILED", "reason": "failed"}""", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: StockDividendsUserDataResponse = Await.result(connector.getUserData(taxYear)(testUser, headerCarrier), Duration.Inf)
        result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns a 511" in {
        stubGetWithHeadersCheck(url, NETWORK_AUTHENTICATION_REQUIRED,
        """{"code": "FAILED", "reason": "failed"}""", "X-Session-ID" -> sessionId, "mtditid" -> mtditid)

        val result: StockDividendsUserDataResponse = Await.result(connector.getUserData(taxYear)(testUser, headerCarrier), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
      }
    }
  }

}
