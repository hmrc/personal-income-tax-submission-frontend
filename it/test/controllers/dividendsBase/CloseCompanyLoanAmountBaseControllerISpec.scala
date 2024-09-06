/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.dividendsBase

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Environment, Mode}
import test.utils.{DividendsDatabaseHelper, IntegrationTest}

class CloseCompanyLoanAmountBaseControllerISpec extends IntegrationTest with DividendsDatabaseHelper {

  val url: String = controllers.dividendsBase.routes.CloseCompanyLoanAmountBaseController.show(taxYear).url
  val headers: Seq[(String, String)] = playSessionCookie() ++ Seq("Csrf-Token" -> "nocheck")

  "CloseCompanyLoanAmountBaseController.show" should {
    "direct to the original close company loan amount controller when 'split-dividends' is false" in {
      val application = GuiceApplicationBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .configure(config(stockDividends = true))
        .build()

      running(application) {

        authoriseIndividual(Some(nino))

        val request = FakeRequest(GET, url).withHeaders(headers: _*)

        val result = route(application, request).value

        status(result) mustEqual OK

      }
    }

    "direct to the new close company loan amount controller when 'split-dividends' is true" in {
      val application = GuiceApplicationBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .configure(config(stockDividends = true, splitStockDividends = true))
        .build()

      running(application) {

        authoriseIndividual(Some(nino))

        val request = FakeRequest(GET, url).withHeaders(headers: _*)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }
  }

  "CloseCompanyLoanAmountBaseController.submit" should {
    "direct to next page of the journey when 'split-dividends' is false" in {
      val application = GuiceApplicationBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .configure(config(stockDividends = true))
        .build()

      running(application) {

        authoriseIndividual(Some(nino))

        dropStockDividendsDB()

        insertStockDividendsCyaData(Some(completeStockDividendsCYAModel.copy(
          Some(true), Some(false), None, Some(false), None, Some(true), None, None, None, Some(true), None
        )))

        val request = FakeRequest(POST, url).withHeaders(headers: _*).withFormUrlEncodedBody("amount" -> "123")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/summary")
      }
    }

    "direct to the new check stock dividend amount controller when 'split-dividends' is true" in {
      val application = GuiceApplicationBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .configure(config(stockDividends = true, splitStockDividends = true))
        .build()

      running(application) {

        authoriseIndividual(Some(nino))

        dropStockDividendsDB()

        insertStockDividendsCyaData(Some(completeStockDividendsCYAModel))

        val request = FakeRequest(POST, url).withHeaders(headers: _*).withFormUrlEncodedBody("amount" -> "123")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/check-close-company-loan-amount")
      }
    }
  }
}
