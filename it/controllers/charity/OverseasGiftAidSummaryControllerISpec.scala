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
import forms.YesNoForm
import helpers.PlaySessionCookieBaker
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, UNAUTHORIZED}
import play.api.libs.ws.WSClient
import utils.IntegrationTest

class OverseasGiftAidSummaryControllerISpec extends IntegrationTest {

  object Selectors {
    val heading = "h1"
    val caption = ".govuk-caption-l"
    val charity1 = ".hmrc-add-to-a-list__contents:nth-child(1) > .hmrc-add-to-a-list__identifier"
    val charity2 = ".hmrc-add-to-a-list__contents:nth-child(2) > .hmrc-add-to-a-list__identifier"
    val change1 = ".hmrc-add-to-a-list__contents:nth-child(1) > .hmrc-add-to-a-list__change > a > span:nth-child(1)"
    val change1hidden = ".hmrc-add-to-a-list__contents:nth-child(1) > .hmrc-add-to-a-list__change > a > span:nth-child(2)"
    val change2 = ".hmrc-add-to-a-list__contents:nth-child(2) > .hmrc-add-to-a-list__change > a > span:nth-child(1)"
    val change2hidden = ".hmrc-add-to-a-list__contents:nth-child(2) > .hmrc-add-to-a-list__change > a > span:nth-child(2)"
    val remove1 = ".hmrc-add-to-a-list__contents:nth-child(1) > .hmrc-add-to-a-list__remove > a > span:nth-child(1)"
    val remove1hidden = ".hmrc-add-to-a-list__contents:nth-child(1) > .hmrc-add-to-a-list__remove > a > span:nth-child(2)"
    val remove2 = ".hmrc-add-to-a-list__contents:nth-child(2) > .hmrc-add-to-a-list__remove > a > span:nth-child(1)"
    val remove2hidden = ".hmrc-add-to-a-list__contents:nth-child(2) > .hmrc-add-to-a-list__remove > a > span:nth-child(2)"
    val question = ".govuk-fieldset__legend"
    val hint = "#value-hint"
    val yesRadio = ".govuk-radios__item:nth-child(1) > label"
    val noRadio = ".govuk-radios__item:nth-child(2) > label"
    val errorSummary = "#error-summary-title"
    val noSelectionError = ".govuk-error-summary__body > ul > li > a"
    val errorMessage = "#value-error"
  }

  object Content {
    val heading = "Overseas charities you used Gift Aid to donate to"
    val headingAgent = "Overseas charities your client used Gift Aid to donate to"
    val caption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val charity1 = "overseasCharity1"
    val charity2 = "overseasCharity2"
    val question = "Do you need to add another overseas charity?"
    val hint = "You must tell us about all the overseas charities you donated to."
    val hintAgent = "You must tell us about all the overseas charities your client donated to."
    val change = "Change"
    val remove = "Remove"
    val hiddenChange1 = "Change details you’ve entered for overseasCharity1"
    val hiddenRemove1 = "Remove overseasCharity1"
    val hiddenChange2 = "Change details you’ve entered for overseasCharity2"
    val hiddenRemove2 = "Remove overseasCharity2"
    val yes = "Yes"
    val no = "No"
    val errorSummary = "There is a problem"
    val noSelectionError = "Select yes if you need to add another overseas charity"
  }

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  val taxYear: Int = 2022

  val overseasGiftAidSummaryUrl = s"$startUrl/$taxYear/charity/overseas-charities-donated-to"

  "Calling GET /charity/overseas-charities-donated-to" when {

    "the user is authorised" when {

      "the user is a non-agent" should {

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(overseasGiftAidSummaryUrl)
            .withHttpHeaders(xSessionId, csrfContent)
            .get())
        }
        lazy val document: Document = Jsoup.parse(result.body)

        s"have an OK($OK) status" in {
          result.status shouldBe OK
        }

        "display the page content" in {
          document.select(Selectors.heading).text() shouldBe Content.heading + " " + Content.caption
          document.select(Selectors.caption).text() shouldBe Content.caption
          document.select(Selectors.charity1).text() shouldBe Content.charity1
          document.select(Selectors.change1).text() shouldBe Content.change
          document.select(Selectors.change1hidden).text() shouldBe Content.hiddenChange1
          document.select(Selectors.remove1).text() shouldBe Content.remove
          document.select(Selectors.remove1hidden).text() shouldBe Content.hiddenRemove1
          document.select(Selectors.charity2).text() shouldBe Content.charity2
          document.select(Selectors.change2).text() shouldBe Content.change
          document.select(Selectors.change2hidden).text() shouldBe Content.hiddenChange2
          document.select(Selectors.remove2).text() shouldBe Content.remove
          document.select(Selectors.remove2hidden).text() shouldBe Content.hiddenRemove2
          document.select(Selectors.question).text() shouldBe Content.question
          document.select(Selectors.hint).text() shouldBe Content.hint
          document.select(Selectors.yesRadio).text() shouldBe Content.yes
          document.select(Selectors.noRadio).text() shouldBe Content.no
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
          await(wsClient.url(overseasGiftAidSummaryUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
            .get())
        }
        lazy val document: Document = Jsoup.parse(result.body)

        s"have an OK($OK) status" in {
          result.status shouldBe OK
        }

        "display the page content" in {
          document.select(Selectors.heading).text() shouldBe Content.headingAgent + " " + Content.caption
          document.select(Selectors.caption).text() shouldBe Content.caption
          document.select(Selectors.charity1).text() shouldBe Content.charity1
          document.select(Selectors.change1).text() should include(Content.change)
          document.select(Selectors.change1hidden).text() shouldBe Content.hiddenChange1
          document.select(Selectors.remove1).text() shouldBe Content.remove
          document.select(Selectors.remove1hidden).text() shouldBe Content.hiddenRemove1
          document.select(Selectors.charity2).text() shouldBe Content.charity2
          document.select(Selectors.change2).text() shouldBe Content.change
          document.select(Selectors.change2hidden).text() shouldBe Content.hiddenChange2
          document.select(Selectors.remove2).text() shouldBe Content.remove
          document.select(Selectors.remove2hidden).text() shouldBe Content.hiddenRemove2
          document.select(Selectors.question).text() shouldBe Content.question
          document.select(Selectors.hint).text() shouldBe Content.hintAgent
          document.select(Selectors.yesRadio).text() shouldBe Content.yes
          document.select(Selectors.noRadio).text() shouldBe Content.no
        }

        "not display an error" in {
          document.select(Selectors.errorSummary).isEmpty shouldBe true
          document.select(Selectors.noSelectionError).isEmpty shouldBe true
          document.select(Selectors.errorMessage).isEmpty shouldBe true
        }
      }
    }

    "the user is unauthorized" should {

      lazy val result = {
        authoriseIndividualUnauthorized()
        await(wsClient.url(overseasGiftAidSummaryUrl)
          .withHttpHeaders(xSessionId, csrfContent)
          .get())
      }

      s"return an Unauthorised($UNAUTHORIZED) status" in {
        result.status shouldBe UNAUTHORIZED
      }
    }
  }

  "Calling POST /charity/overseas-charities-donated-to" when {

    "the user is authorised" when {

      lazy val result = {
        authoriseIndividual()
        await(wsClient.url(overseasGiftAidSummaryUrl)
          .withHttpHeaders(xSessionId, csrfContent)
          .post(Map(YesNoForm.yesNo -> "")))
      }
      lazy val document: Document = Jsoup.parse(result.body)

      "no radio button in selected" should {

        s"return a 400(BadRequest) status" in {
          result.status shouldBe BAD_REQUEST
        }

        "display an error message" in {
          document.select(Selectors.noSelectionError).text() shouldBe Content.noSelectionError
        }
      }

      "an option is selected" should {

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(overseasGiftAidSummaryUrl)
            .withHttpHeaders(xSessionId, csrfContent)
            .post(Map(YesNoForm.yesNo -> YesNoForm.no)))
        }

        "return a 200(Ok) status" in {
          result.status shouldBe OK
        }
      }

    }

  }

}
