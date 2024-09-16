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

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status.{NO_CONTENT, OK, SEE_OTHER}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import test.utils.{IntegrationTest, SavingsDatabaseHelper}

class InterestSecuritiesCyaBaseControllerISpec extends IntegrationTest with SavingsDatabaseHelper {

  private val url: String = controllers.savingsBase.routes.InterestSecuritiesCyaBaseController.show(taxYear).url
  private val headers: Seq[(String, String)] = playSessionCookie() ++ Seq("Csrf-Token" -> "nocheck")

  ".show" should {
    "direct to the original Interest Securities CYA controller when 'miniJourneyEnabled' is false" in {
      val application = buildApplication()

      running(application) {
        authoriseIndividual(Some(nino))
        emptyUserDataStub()
        dropSavingsDB()
        insertSavingsCyaData(cyaDataComplete)

        val request = FakeRequest(GET, url).withHeaders(headers: _*)
        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }

    "direct to the new Interest Securities CYA controller when 'miniJourneyEnabled' is true" in {
      val application = buildApplication(miniJourneyEnabled = true)

      running(application) {
        authoriseIndividual(Some(nino))
        emptyUserDataStub()
        dropSavingsDB()
        insertSavingsCyaData(cyaDataComplete)
        stubPut(s"/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", NO_CONTENT, "")

        val request = FakeRequest(GET, url).withHeaders(headers: _*)
        val result = route(application, request).value

        status(result) mustEqual OK
      }
    }
  }

  ".submit" should {
    "direct to Interest from Savings and Securities Summary page when 'miniJourneyEnabled' is false" in {
      val application = buildApplication()

      running(application) {
        authoriseIndividual(Some(nino))
        emptyUserDataStub()
        dropSavingsDB()
        stubPut(s"/income-tax-interest/income-tax/nino/AA123456A/savings\\?taxYear=$taxYear", NO_CONTENT, "")
        stubPut(s"/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", NO_CONTENT, "")
        insertSavingsCyaData(cyaDataComplete)

        val request = FakeRequest(POST, url).withHeaders(headers: _*)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(controllers.routes.InterestFromSavingsAndSecuritiesSummaryController.show(taxYear).url)
      }
    }

    "direct to Task List when 'miniJourneyEnabled' is true" in {
      val application = buildApplication(miniJourneyEnabled = true)

      running(application) {
        authoriseIndividual(Some(nino))
        emptyUserDataStub()
        dropSavingsDB()
        insertSavingsCyaData(cyaDataComplete)
        stubPut(s"/income-tax-interest/income-tax/nino/AA123456A/savings\\?taxYear=$taxYear", NO_CONTENT, "")
        stubPut(s"/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", NO_CONTENT, "")

        val request = FakeRequest(POST, url).withHeaders(headers: _*)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(s"${appConfig.incomeTaxSubmissionBaseUrl}/$taxYear/tasklist")
      }
    }
  }
}
