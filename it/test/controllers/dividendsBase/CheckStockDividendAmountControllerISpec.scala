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

import models.dividends.{DividendsPriorSubmission, StockDividendModel, StockDividendsPriorSubmission}
import models.priorDataModels.IncomeSourcesModel
import org.scalatest.OptionValues.convertOptionToValuable
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{NO_CONTENT, OK, SEE_OTHER}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Environment, Mode}
import test.utils.{DividendsDatabaseHelper, IntegrationTest}

class CheckStockDividendAmountControllerISpec extends IntegrationTest with DividendsDatabaseHelper {

  val url: String = controllers.dividendsSplit.routes.CheckStockDividendsAmountController.show(taxYear).url
  val headers: Seq[(String, String)] = playSessionCookie() ++ Seq("Csrf-Token" -> "nocheck")

  val stockDividendsPrior: StockDividendsPriorSubmission =
    StockDividendsPriorSubmission(None, None, None, Some(StockDividendModel(None, grossAmount = 123.45)), None, None, None)
  val dividendsPrior: IncomeSourcesModel = IncomeSourcesModel(Some(DividendsPriorSubmission(Some(123.45), Some(123.45))))

  "CheckStockDividendAmountController.show" should {
    "render the page when a session exists" in {
      val application = GuiceApplicationBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .configure(config(stockDividends = true))
        .build()

      running(application) {

        authoriseIndividual(Some(nino))
        dropStockDividendsDB()
        emptyUserDataStub()
        emptyStockDividendsUserDataStub()
        insertStockDividendsCyaData(Some(completeStockDividendsCYAModel))

        val request = FakeRequest(GET, url).withHeaders(headers: _*)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) contains completeStockDividendsCYAModel.stockDividendsAmount.get.toString()
      }
    }

    "redirect to overview when no cya or prior exists" in {
      val application = GuiceApplicationBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .configure(config(stockDividends = true))
        .build()

      running(application) {

        authoriseIndividual(Some(nino))
        dropStockDividendsDB()
        emptyUserDataStub()
        emptyStockDividendsUserDataStub()

        val request = FakeRequest(GET, url).withHeaders(headers: _*)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }

    "render the page when a new session needs to be created from prior data" in {
      val application = GuiceApplicationBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .configure(config(stockDividends = true))
        .build()

      running(application) {

        authoriseIndividual(Some(nino))
        dropStockDividendsDB()
        userDataStub(dividendsPrior, nino, taxYear)
        stockDividendsUserDataStub(Some(stockDividendsPrior), nino, taxYear)

        val request = FakeRequest(GET, url).withHeaders(headers: _*)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "render the page when a new session needs to be created from prior data with only dividends" in {
      val application = GuiceApplicationBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .configure(config(stockDividends = true))
        .build()

      running(application) {

        authoriseIndividual(Some(nino))
        dropStockDividendsDB()
        userDataStub(dividendsPrior, nino, taxYear)
        emptyStockDividendsUserDataStub()

        val request = FakeRequest(GET, url).withHeaders(headers: _*)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "render the page when a new session needs to be created from prior data with only stock dividends" in {
      val application = GuiceApplicationBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .configure(config(stockDividends = true))
        .build()

      running(application) {

        authoriseIndividual(Some(nino))
        dropStockDividendsDB()
        emptyUserDataStub()
        stockDividendsUserDataStub(Some(stockDividendsPrior), nino, taxYear)

        val request = FakeRequest(GET, url).withHeaders(headers: _*)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "return an error when downstream service returns INTERNAL_SERVER_ERROR" in {
      val application = GuiceApplicationBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .configure(config(stockDividends = true))
        .build()

      running(application) {

        authoriseIndividual(Some(nino))
        dropStockDividendsDB()
        userDataStubWithError(nino, taxYear)
        emptyStockDividendsUserDataStub()

        val request = FakeRequest(GET, url).withHeaders(headers: _*)

        val result = route(application, request).value

        status(result) mustEqual INTERNAL_SERVER_ERROR
      }
    }
  }

  "StockDividendAmountBaseController.submit" should {
    "submit data and redirect to overview" in {
      val application = GuiceApplicationBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .configure(config(stockDividends = true))
        .build()

      running(application) {

        authoriseIndividual(Some(nino))
        dropStockDividendsDB()
        emptyUserDataStub()
        emptyStockDividendsUserDataStub()
        insertStockDividendsCyaData(Some(completeStockDividendsCYAModel))
        stubPut(s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", NO_CONTENT, "")
        stubPut(s"/income-tax-dividends/income-tax/nino/$nino/sources\\?taxYear=$taxYear", NO_CONTENT, "")
        stubPut(s"/income-tax-dividends/income-tax/income/dividends/$nino/$taxYear", NO_CONTENT, "")

        val request = FakeRequest(POST, url).withHeaders(headers: _*)

        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }
  }
}
