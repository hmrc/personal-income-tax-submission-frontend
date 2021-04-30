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

package controllers.charity

import common.SessionValues
import helpers.PlaySessionCookieBaker
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.ws.WSClient
import utils.IntegrationTest

class LastTaxYearAmountControllerISpec extends IntegrationTest {

  object Selectors {
    val heading = "h1"
    val caption = ".govuk-caption-l"
    val para = "label > p"
    val hint = "#amount-hint"

    val errorSummary = "#error-summary-title"
    val noSelectionError = ".govuk-error-summary__body > ul > li > a"
    val errorMessage = "#value-error"
  }

  object Content {
    val heading = "How much of your donation do you want to add to the last tax year?"
    val headingAgent = "How much of your client’s donation do you want to add to the last tax year?"
    val caption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val para = "Do not include the Gift Aid added to your donation."
    val paraAgent = "Do not include the Gift Aid added to your client’s donation."
    val hint = "For example, £600 or £193.54"

    val errorSummary = "There is a problem"
    val noSelectionError = "Enter the amount of your donation you want to add to the last tax year"
    val noSelectionErrorAgent = "Enter the amount of your client’s donation you want to add to the last tax year"
  }

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  val taxYear: Int = 2022

  val lastTaxYearAmountUrl = s"$startUrl/$taxYear/charity/amount-added-to-last-tax-year"

  "Calling GET /charity/amount-added-to-last-tax-year" should {

    "the user is authorised" when {

      "the user is a non-agent" should {

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(lastTaxYearAmountUrl)
            .withHttpHeaders("Csrf-Token" -> "nocheck")
            .get())
        }
        lazy val document: Document = Jsoup.parse(result.body)

        s"have an OK($OK) status" in {
          result.status shouldBe OK
        }

        "display the page content" in {
          document.select(Selectors.heading).text() shouldBe Content.heading
          document.select(Selectors.caption).text() shouldBe Content.caption
          document.select(Selectors.para).text() shouldBe Content.para
          document.select(Selectors.hint).text() shouldBe Content.hint
        }

        "not display an error" in {
          document.select(Selectors.errorSummary).isEmpty shouldBe true
          document.select(Selectors.noSelectionError).isEmpty shouldBe true
          document.select(Selectors.errorMessage).isEmpty shouldBe true
        }
      }

      "the user is an agent" should {

        lazy val result = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))
          authoriseAgent()
          await(wsClient.url(lastTaxYearAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie)
            .get())
        }

        lazy val document: Document = Jsoup.parse(result.body)

        s"have an OK($OK) status" in {
          result.status shouldBe OK
        }

        "display the page content" in {
          document.select(Selectors.heading).text() shouldBe Content.headingAgent
          document.select(Selectors.caption).text() shouldBe Content.caption
          document.select(Selectors.para).text() shouldBe Content.paraAgent
          document.select(Selectors.hint).text() shouldBe Content.hint
        }

        "not display an error" in {
          document.select(Selectors.errorSummary).isEmpty shouldBe true
          document.select(Selectors.noSelectionError).isEmpty shouldBe true
          document.select(Selectors.errorMessage).isEmpty shouldBe true
        }
      }

    }

  }

}
