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

import models.mongo.DataNotFound
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import org.scalatestplus.mockito.MockitoSugar.mock
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.DefaultBodyWritables
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.api.{Environment, Mode}
import services.StockDividendsSessionServiceProvider
import test.utils.{DividendsDatabaseHelper, IntegrationTest, ViewHelpers}

import scala.concurrent.Future

class CloseCompanyLoanAmountSplitControllerISpec extends IntegrationTest with ViewHelpers with DefaultBodyWritables with DividendsDatabaseHelper {

  val url: String = controllers.dividendsBase.routes.CloseCompanyLoanAmountBaseController.show(taxYear).url
  val headers: Seq[(String, String)] = playSessionCookie() ++ Seq("Csrf-Token" -> "nocheck")

  "CloseCompanyLoanAmountSplitController.show" should {
    "render the page" in {
      val application = GuiceApplicationBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .configure(config(stockDividends = true, splitStockDividends = true))
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
        contentAsString(result) contains completeStockDividendsCYAModel.closeCompanyLoansWrittenOffAmount.get.toString()
      }
    }

    "render the page for an agent" in {
      val application = GuiceApplicationBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .configure(config(stockDividends = true, splitStockDividends = true))
        .build()

      val headers = playSessionCookie(agent = true)

      running(application) {

        authoriseAgentOrIndividual(isAgent = true)
        dropStockDividendsDB()
        emptyUserDataStub()
        emptyStockDividendsUserDataStub()
        insertStockDividendsCyaData(Some(completeStockDividendsCYAModel))

        val request = FakeRequest(GET, url).withHeaders(headers: _*)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) contains completeStockDividendsCYAModel.closeCompanyLoansWrittenOffAmount.get.toString
      }
    }

    "render the page when no session is defined" in {
      val application = GuiceApplicationBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .configure(config(stockDividends = true, splitStockDividends = true))
        .build()

      running(application) {

        authoriseIndividual(Some(nino))
        dropStockDividendsDB()
        emptyUserDataStub()
        emptyStockDividendsUserDataStub()

        val request = FakeRequest(GET, url).withHeaders(headers: _*)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "render the page when session is defined without close company loan amount" in {
      val application = GuiceApplicationBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .configure(config(stockDividends = true, splitStockDividends = true))
        .build()

      running(application) {

        authoriseIndividual(Some(nino))
        dropStockDividendsDB()
        emptyUserDataStub()
        emptyStockDividendsUserDataStub()
        insertStockDividendsCyaData(Some(completeStockDividendsCYAModel.copy(None, None, None, None, None, None, None, None, None, None, None)))

        val request = FakeRequest(GET, url).withHeaders(headers: _*)

        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "return an INTERNAL_SERVER_ERROR" in {
      val mockService = mock[StockDividendsSessionServiceProvider]

      when(mockService.getSessionData(any())(any(), any())).thenReturn(
        Future.successful(Left(DataNotFound))
      )

      val application = GuiceApplicationBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .configure(config(stockDividends = true, splitStockDividends = true))
        .overrides(bind[StockDividendsSessionServiceProvider].toInstance(mockService))
        .build()

      running(application) {

        authoriseIndividual(Some(nino))
        dropStockDividendsDB()
        emptyUserDataStub()
        emptyStockDividendsUserDataStub()
        insertStockDividendsCyaData(Some(completeStockDividendsCYAModel))

        val request = FakeRequest(GET, url).withHeaders(headers: _*)

        val result = route(application, request).value

        status(result) mustEqual INTERNAL_SERVER_ERROR
      }
    }
  }

  "CloseCompanyLoanAmountSplitController.submit" should {
    "direct to the new check close company loan amount controller" in {
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

    "return BAD_REQUEST with invalid body" in {
      val application = GuiceApplicationBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .configure(config(stockDividends = true, splitStockDividends = true))
        .build()

      running(application) {

        authoriseIndividual(Some(nino))

        dropStockDividendsDB()

        insertStockDividendsCyaData(Some(completeStockDividendsCYAModel))

        val request = FakeRequest(POST, url).withHeaders(headers: _*).withFormUrlEncodedBody("invalid" -> "123")

        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "return an INTERNAL_SERVER_ERROR" in {
      val mockService = mock[StockDividendsSessionServiceProvider]

      when(mockService.getSessionData(any())(any(), any())).thenReturn(
        Future.successful(Left(DataNotFound))
      )

      val application = GuiceApplicationBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .configure(config(stockDividends = true, splitStockDividends = true))
        .overrides(bind[StockDividendsSessionServiceProvider].toInstance(mockService))
        .build()

      running(application) {

        authoriseIndividual(Some(nino))

        dropStockDividendsDB()

        insertStockDividendsCyaData(Some(completeStockDividendsCYAModel))

        val request = FakeRequest(POST, url).withHeaders(headers: _*).withFormUrlEncodedBody("amount" -> "123")

        val result = route(application, request).value

        status(result) mustEqual INTERNAL_SERVER_ERROR
      }
    }
  }
}
