/*
 * Copyright 2021 HM Revenue & Customs
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
import connectors.httpParsers.DividendsSubmissionHttpParser.DividendsSubmissionsResponse
import models.dividends.{DividendsResponseModel, DividendsSubmissionModel}
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status._
import utils.IntegrationTest

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class DividendsSubmissionConnectorSpec extends IntegrationTest{

  lazy val connector = app.injector.instanceOf[DividendsSubmissionConnector]
  val body =  DividendsSubmissionModel(
    Some(10),
    Some(10)
  )

  val taxYear = 2022

  val expectedHeaders = Seq(new HttpHeader("mtditid", mtditid))

  "DividendsSubmissionConnectorSpec" should {
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
