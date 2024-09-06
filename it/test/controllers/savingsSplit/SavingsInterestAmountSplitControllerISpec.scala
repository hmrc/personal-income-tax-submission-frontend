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

package controllers.savingsSplit

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
import services.{SavingsSessionService, StockDividendsSessionServiceProvider}
import test.utils.{IntegrationTest, SavingsDatabaseHelper, ViewHelpers}

import scala.concurrent.Future

class SavingsInterestAmountSplitControllerISpec extends IntegrationTest with ViewHelpers with DefaultBodyWritables with SavingsDatabaseHelper {

  private val url: String = controllers.savingsBase.routes.SavingsInterestAmountBaseController.show(taxYear).url
  private val headers: Seq[(String, String)] = playSessionCookie() ++ Seq("Csrf-Token" -> "nocheck")
  private val monetaryValue: BigDecimal = 100

  "SavingsInterestAmountSplitController.show" should {
    "render the page" in {
      val application = buildApplication( miniJourneyEnabled = true)

      running(application) {
        authoriseIndividual(Some(nino))
        dropSavingsDB()
        emptyUserDataStub()

        val request = FakeRequest(GET, url).withHeaders(headers: _*)
        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) contains completeStockDividendsCYAModel.stockDividendsAmount.get.toString()
      }
    }

    "render the page for an agent" in {
      val application = buildApplication( miniJourneyEnabled = true)

      running(application) {
        authoriseAgentOrIndividual(isAgent = true)
        dropSavingsDB()
        emptyUserDataStub()

        val headers = playSessionCookie(agent = true)
        val request = FakeRequest(GET, url).withHeaders(headers: _*)

        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) contains completeStockDividendsCYAModel.stockDividendsAmount.get.toString
      }
    }

    "render the page with value from session" in {
      val application = buildApplication( miniJourneyEnabled = true)

      running(application) {
        authoriseIndividual(Some(nino))
        dropSavingsDB()
        emptyUserDataStub()
        insertSavingsCyaData(Some(cyaDataValid.get.copy(grossAmount = Some(monetaryValue))))

        val request = FakeRequest(GET, url).withHeaders(headers: _*)
        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) contains completeStockDividendsCYAModel.stockDividendsAmount.get.toString()
      }
    }

    "render the page when session is 'None'" in {
      val application = buildApplication( miniJourneyEnabled = true)

      running(application) {
        authoriseIndividual(Some(nino))
        dropSavingsDB()
        emptyUserDataStub()
        insertSavingsCyaData(None)

        val request = FakeRequest(GET, url).withHeaders(headers: _*)
        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "render the page when no session is defined" in {
      val application = buildApplication( miniJourneyEnabled = true)

      running(application) {

        authoriseIndividual(Some(nino))
        dropSavingsDB()
        emptyUserDataStub()

        val request = FakeRequest(GET, url).withHeaders(headers: _*)
        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "render the page when session is defined without savings interest amount" in {
      val application = buildApplication( miniJourneyEnabled = true)

      running(application) {
        authoriseIndividual(Some(nino))
        dropSavingsDB()
        emptyUserDataStub()
        emptyStockDividendsUserDataStub()
        insertSavingsCyaData(cyaDataValid)

        val request = FakeRequest(GET, url).withHeaders(headers: _*)
        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "return an INTERNAL_SERVER_ERROR" in {
      val mockService = mock[SavingsSessionService]

      when(mockService.getSessionData(any())(any(), any())).thenReturn(Future.successful(Left(DataNotFound)))

      val application = GuiceApplicationBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .configure(config(stockDividends = true, miniJourneyEnabled = true))
        .overrides(bind[SavingsSessionService].toInstance(mockService))
        .build()

      running(application) {
        authoriseIndividual(Some(nino))
        dropSavingsDB()
        emptyUserDataStub()
        emptyStockDividendsUserDataStub()
        insertSavingsCyaData(cyaDataValid)

        val request = FakeRequest(GET, url).withHeaders(headers: _*)
        val result = route(application, request).value

        status(result) mustEqual INTERNAL_SERVER_ERROR
      }
    }
  }

  "SavingsInterestAmountSplitController.submit" should {
    "direct to the check savings interest amount controller when CYA data is finished" in {
      val application = buildApplication( miniJourneyEnabled = true)

      running(application) {
        authoriseIndividual(Some(nino))
        dropSavingsDB()
        emptyUserDataStub()
        insertSavingsCyaData(cyaDataComplete)

        val request = FakeRequest(POST, url).withHeaders(headers: _*).withFormUrlEncodedBody("amount" -> "123")
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/check-interest-from-securities")
      }
    }

    "direct to the check savings interest amount controller when no session or prior are found" in {
      val application = buildApplication( miniJourneyEnabled = true)

      running(application) {
        authoriseIndividual(Some(nino))
        dropSavingsDB()
        emptyUserDataStub()

        val request = FakeRequest(POST, url).withHeaders(headers: _*).withFormUrlEncodedBody("amount" -> "123")
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/check-interest-from-securities")
      }
    }

    "return BAD_REQUEST with invalid body" in {
      val application = buildApplication( miniJourneyEnabled = true)

      running(application) {
        authoriseIndividual(Some(nino))
        dropSavingsDB()
        insertSavingsCyaData(cyaDataValid)

        val request = FakeRequest(POST, url).withHeaders(headers: _*).withFormUrlEncodedBody("invalid" -> "123")
        val result = route(application, request).value

        status(result) mustEqual BAD_REQUEST
      }
    }

    "return an INTERNAL_SERVER_ERROR" in {
      val mockService = mock[StockDividendsSessionServiceProvider]

      when(mockService.getSessionData(any())(any(), any())).thenReturn(Future.successful(Left(DataNotFound)))

      val application = GuiceApplicationBuilder()
        .in(Environment.simple(mode = Mode.Dev))
        .configure(config(stockDividends = true, miniJourneyEnabled = true))
        .overrides(bind[StockDividendsSessionServiceProvider].toInstance(mockService))
        .build()

      running(application) {
        authoriseIndividual(Some(nino))
        dropSavingsDB()
        insertSavingsCyaData(cyaDataValid)

        val request = FakeRequest(POST, url).withHeaders(headers: _*).withFormUrlEncodedBody("amount" -> "123")
        val result = route(application, request).value

        status(result) mustEqual INTERNAL_SERVER_ERROR
      }
    }
  }
}
