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

package controllers.dividendsSplit

import connectors.IncomeSourceConnector
import models.dividends.{DividendsPriorSubmission, StockDividendModel, StockDividendsPriorSubmission}
import models.priorDataModels.IncomeSourcesModel
import models.{APIErrorBodyModel, APIErrorModel}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.http.Status.{NO_CONTENT, OK, SEE_OTHER}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Environment, Mode}
import test.utils.{DividendsDatabaseHelper, IntegrationTest}

import scala.concurrent.Future

class CheckCloseCompanyLoanAmountControllerISpec extends IntegrationTest with DividendsDatabaseHelper {

  val url: String = controllers.dividendsSplit.routes.CheckCloseCompanyLoanAmountController.show(taxYear).url
  val headers: Seq[(String, String)] = playSessionCookie() ++ Seq("Csrf-Token" -> "nocheck")
  val monetaryValue: BigDecimal = 123.45
  val stockDividendsPrior: StockDividendsPriorSubmission =
    StockDividendsPriorSubmission(None, None, None, Some(StockDividendModel(None, grossAmount = monetaryValue)), None, None, None)
  val dividendsPrior: IncomeSourcesModel = IncomeSourcesModel(Some(DividendsPriorSubmission(Some(monetaryValue), Some(monetaryValue))))

  "CheckCloseCompanyLoanAmountController.show" should {
    "render the page when a session exists" in {
      val application = buildApplication(stockDividends = true, miniJourneyEnabled = true)

      running(application) {
        authoriseIndividual(Some(nino))
        dropStockDividendsDB()
        emptyUserDataStub()
        emptyStockDividendsUserDataStub()
        insertStockDividendsCyaData(Some(completeStockDividendsCYAModel))

        val request = FakeRequest(GET, url).withHeaders(headers: _*)
        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) contains completeStockDividendsCYAModel.closeCompanyLoansWrittenOffAmount.get.toString()
      }
    }

    "redirect to task list when no cya or prior exists" in {
      val application = buildApplication(stockDividends = true, miniJourneyEnabled = true)

      running(application) {
        authoriseIndividual(Some(nino))
        dropStockDividendsDB()
        emptyUserDataStub()
        emptyStockDividendsUserDataStub()

        val request = FakeRequest(GET, url).withHeaders(headers: _*)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(s"${appConfig.incomeTaxSubmissionBaseUrl}/$taxYear/tasklist")
      }
    }

    "render the page when a new session needs to be created from prior data" in {
      val application = buildApplication(stockDividends = true, miniJourneyEnabled = true)

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
      val application = buildApplication(stockDividends = true, miniJourneyEnabled = true)

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
      val application = buildApplication(stockDividends = true, miniJourneyEnabled = true)

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
      val application = buildApplication(stockDividends = true, miniJourneyEnabled = true)

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

    "return an error when refresh cache service returns INTERNAL_SERVER_ERROR" in {
      val mockRefreshCache = mock[IncomeSourceConnector]

      when(mockRefreshCache.put(any(), any(), any())(any())).thenReturn(
        Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "reason"))))
      )

      val application = GuiceApplicationBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .configure(config(stockDividends = true, miniJourneyEnabled = true))
        .overrides(bind[IncomeSourceConnector].toInstance(mockRefreshCache))
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

  "CheckCloseCompanyLoanAmountBaseController.submit" should {
    "submit data and redirect to task list when sectionCompletedQuestionEnabled is false" in {
      val application = buildApplication(stockDividends = true, miniJourneyEnabled = true)

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
        redirectLocation(result) mustBe Some(s"${appConfig.incomeTaxSubmissionBaseUrl}/$taxYear/tasklist")
      }
    }

    "submit data and redirect to task list when sectionCompletedQuestionEnabled is true" in {
      val application = buildApplication(stockDividends = true, miniJourneyEnabled = true, sectionCompletedQuestionEnabled = true)

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
        redirectLocation(result) mustBe Some(s"/update-and-submit-income-tax-return/personal-income/$taxYear/close-company-loans/section-completed")
      }
    }

    "return SERVICE_UNAVAILABLE when API is unavailable" in {
      val application = buildApplication(stockDividends = true, miniJourneyEnabled = true)

      running(application) {
        authoriseIndividual(Some(nino))
        dropStockDividendsDB()
        emptyUserDataStub()
        emptyStockDividendsUserDataStub()
        insertStockDividendsCyaData(Some(completeStockDividendsCYAModel))
        stubPut(s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", NO_CONTENT, "")
        stubPut(s"/income-tax-dividends/income-tax/nino/$nino/sources\\?taxYear=$taxYear", SERVICE_UNAVAILABLE, "")
        stubPut(s"/income-tax-dividends/income-tax/income/dividends/$nino/$taxYear", SERVICE_UNAVAILABLE, "")

        val request = FakeRequest(POST, url).withHeaders(headers: _*)
        val result = route(application, request).value

        status(result) mustEqual SERVICE_UNAVAILABLE
      }
    }

    "return INTERNAL_SERVER_ERROR due to no session data" in {
      val application = buildApplication(stockDividends = true, miniJourneyEnabled = true)

      running(application) {
        authoriseIndividual(Some(nino))
        dropStockDividendsDB()
        emptyUserDataStub()
        emptyStockDividendsUserDataStub()

        val request = FakeRequest(POST, url).withHeaders(headers: _*)
        val result = route(application, request).value

        status(result) mustEqual INTERNAL_SERVER_ERROR
      }
    }
  }

}
