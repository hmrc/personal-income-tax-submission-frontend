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

package controllers.savingsBase

import models.savings.SavingsIncomeCYAModel
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Environment, Mode}
import test.utils.{DividendsDatabaseHelper, IntegrationTest, SavingsDatabaseHelper}

class SavingsInterestAmountBaseControllerISpec extends IntegrationTest with SavingsDatabaseHelper {

  private val url: String = controllers.savingsBase.routes.SavingsInterestAmountBaseController.show(taxYear).url
  private val headers: Seq[(String, String)] = playSessionCookie() ++ Seq("Csrf-Token" -> "nocheck")

  "SavingsInterestAmountBaseController.show" should {
    "direct to the original savings interest amount controller when 'split-dividends' is false" in {
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

    "direct to the new savings interest amount controller when 'split-dividends' is true" in {
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

  "SavingsInterestAmountBaseController.submit" should {
    "direct to next page of the journey when 'split-dividends' is false and CYA is incomplete" in {
      val application = GuiceApplicationBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .configure(config(stockDividends = true))
        .build()

      running(application) {

        authoriseIndividual(Some(nino))

        emptyUserDataStub()

        dropSavingsDB()

        insertSavingsCyaData(cyaDataValid)

        val request = FakeRequest(POST, url).withHeaders(headers: _*).withFormUrlEncodedBody("amount" -> "123")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/tax-taken-from-interest")
      }
    }

    "direct to next page of the journey when 'split-dividends' is true and CYA is incomplete" in {
      val application = GuiceApplicationBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .configure(config(stockDividends = true, splitStockDividends = true))
        .build()

      running(application) {

        authoriseIndividual(Some(nino))

        emptyUserDataStub()

        dropSavingsDB()

        insertSavingsCyaData(cyaDataValid)

        val request = FakeRequest(POST, url).withHeaders(headers: _*).withFormUrlEncodedBody("amount" -> "123")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/tax-taken-from-interest")
      }
    }

    "direct to the CYA page controller when 'split-dividends' is true and CYA data is finished" in {
      val application = GuiceApplicationBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .configure(config(stockDividends = true, splitStockDividends = true))
        .build()

      running(application) {

        authoriseIndividual(Some(nino))

        emptyUserDataStub()

        dropSavingsDB()

        insertSavingsCyaData(cyaDataComplete)

        val request = FakeRequest(POST, url).withHeaders(headers: _*).withFormUrlEncodedBody("amount" -> "123")

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/check-interest-from-securities")
      }
    }
  }
}
