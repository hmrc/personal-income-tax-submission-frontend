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
import play.api.test.FakeRequest
import play.api.test.Helpers._
import test.utils.{DividendsDatabaseHelper, IntegrationTest}

class OtherUkDividendsAmountBaseControllerISpec extends IntegrationTest with DividendsDatabaseHelper{

  val url: String = controllers.dividendsBase.routes.OtherUkDividendsAmountBaseController.show(taxYear).url
  val headers: Seq[(String, String)] = playSessionCookie() ++ Seq("Csrf-Token" -> "nocheck")

  ".show" should {
    "direct to the original other uk dividends amount controller when 'miniJourneyEnabled' is false" in {
      val application = buildApplication(stockDividends = true)

      running(application) {
        authoriseIndividual(Some(nino))

        val request = FakeRequest(GET, url).withHeaders(headers: _*)
        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "direct to the new other uk dividends amount controller when 'miniJourneyEnabled' is true" in {
      val application = buildApplication(stockDividends = true, miniJourneyEnabled = true)

      running(application) {
        authoriseIndividual(Some(nino))

        val request = FakeRequest(GET, url).withHeaders(headers: _*)
        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }
  }

  ".submit" should {
    "direct to next page of the journey when 'miniJourneyEnabled' is false" in {
      val application = buildApplication(stockDividends = true)

      running(application) {
        authoriseIndividual(Some(nino))
        dropStockDividendsDB()
        emptyUserDataStub()
        emptyStockDividendsUserDataStub()
        insertStockDividendsCyaData(
          Some(completeStockDividendsCYAModel.copy(Some(true), Some(false), None, Some(true), None, None, None, None, None, None, None))
        )

        val request = FakeRequest(POST, url).withHeaders(headers: _*).withFormUrlEncodedBody("amount" -> "123")
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/stock-dividend-status")
      }
    }

    "direct to the new check other uk dividends amount controller when 'miniJourneyEnabled' is true" in {
      val application = buildApplication(miniJourneyEnabled = true)

      running(application) {
        authoriseIndividual(Some(nino))
        dropDividendsDB()
        insertDividendsCyaData(Some(completeDividendsCYAModel))

        val request = FakeRequest(POST, url).withHeaders(headers: _*).withFormUrlEncodedBody("amount" -> "123")
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(
          s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/check-how-much-dividends-from-uk-trusts-and-open-ended-investment-companies"
        )
      }
    }
  }

}
