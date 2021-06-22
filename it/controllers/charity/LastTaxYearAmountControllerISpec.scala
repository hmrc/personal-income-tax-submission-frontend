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
import utils.{IntegrationTest, ViewHelpers}

class LastTaxYearAmountControllerISpec extends IntegrationTest with ViewHelpers {

  object Selectors {
    val para = "label > p"
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
    val button = "Continue"

    val noSelectionError = "Enter the amount of your donation you want to add to the last tax year"
    val noSelectionErrorAgent = "Enter the amount of your client’s donation you want to add to the last tax year"
    val tooLongError = "The amount of your donation you add to the last tax year must be less than £100,000,000,000"
    val tooLongErrorAgent = "The amount of your client’s donation you add to the last tax year must be less than £100,000,000,000"
    val invalidFormatError = "Enter the amount you want to add to the last tax year in the correct format"
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
            .withHttpHeaders(xSessionId, csrfContent)
            .get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return the page" which {
          titleCheck(Content.heading)
          h1Check(Content.heading + " " + Content.caption)
          textOnPageCheck(Content.para, Selectors.para)
          inputFieldCheck("amount", ".govuk-input")
          hintTextCheck(Content.hint)
          captionCheck(Content.caption)
          buttonCheck(Content.button)

          elementExtinct(Selectors.errorSummary)
          elementExtinct(Selectors.noSelectionError)
          elementExtinct(Selectors.errorMessage)
        }

        s"have an OK($OK) status" in {
          result.status shouldBe OK
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
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
            .get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return the page" which {
          titleCheck(Content.headingAgent)
          h1Check(Content.headingAgent + " " + Content.caption)
          textOnPageCheck(Content.paraAgent, Selectors.para)
          inputFieldCheck("amount", ".govuk-input")
          hintTextCheck(Content.hint)
          captionCheck(Content.caption)
          buttonCheck(Content.button)

          elementExtinct(Selectors.errorSummary)
          elementExtinct(Selectors.noSelectionError)
          elementExtinct(Selectors.errorMessage)
        }

        s"have an OK($OK) status" in {
          result.status shouldBe OK
        }
      }
    }
  }

  "calling POST" when {

    "an individual" when {

      "the form data is valid" should {

        "return an OK" in {
          lazy val result = {
            authoriseIndividual()
            await(wsClient.url(lastTaxYearAmountUrl)
              .withHttpHeaders(xSessionId, csrfContent)
              .post(Map[String, String](
                "amount" -> "1234"
              )))
          }

          result.status shouldBe OK
        }

      }

      "return an error" when {

        "the submitted data is empty" which {
          lazy val result = {
            authoriseIndividual()
            await(wsClient.url(lastTaxYearAmountUrl)
              .withHttpHeaders(xSessionId, csrfContent)
              .post(Map[String, String](
                "amount" -> ""
              )))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck("Error: " + Content.heading)
          h1Check(Content.heading + " " + Content.caption)
          textOnPageCheck(Content.para, Selectors.para)
          inputFieldCheck("amount", ".govuk-input")
          hintTextCheck(Content.hint)
          captionCheck(Content.caption)
          buttonCheck(Content.button)

          errorSummaryCheck(Content.noSelectionError, "#amount")
          errorAboveElementCheck(Content.noSelectionError)
        }

        "the submitted data is too long" which {
          lazy val result = {
            authoriseIndividual()
            await(wsClient.url(lastTaxYearAmountUrl)
              .withHttpHeaders(xSessionId, csrfContent)
              .post(Map(
                "amount" -> "999999999999999999999999999999999999999999999999"
              )))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck("Error: " + Content.heading)
          h1Check(Content.heading + " " + Content.caption)
          textOnPageCheck(Content.para, Selectors.para)
          inputFieldCheck("amount", ".govuk-input")
          hintTextCheck(Content.hint)
          captionCheck(Content.caption)
          buttonCheck(Content.button)

          errorSummaryCheck(Content.tooLongError, "#amount")
          errorAboveElementCheck(Content.tooLongError)
        }

        "the submitted data is in the incorrect format" which {
          lazy val result = {
            authoriseIndividual()
            await(wsClient.url(lastTaxYearAmountUrl)
              .withHttpHeaders(xSessionId, csrfContent)
              .post(Map(
                "amount" -> ".."
              )))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck("Error: " + Content.heading)
          h1Check(Content.heading + " " + Content.caption)
          textOnPageCheck(Content.para, Selectors.para)
          inputFieldCheck("amount", ".govuk-input")
          hintTextCheck(Content.hint)
          captionCheck(Content.caption)
          buttonCheck(Content.button)

          errorSummaryCheck(Content.invalidFormatError, "#amount")
          errorAboveElementCheck(Content.invalidFormatError)
        }
      }

    }

    "an agent" when {

      "the form data is valid" should {

        "return an OK" in {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890"
          ))

          lazy val result = {
            authoriseAgent()
            await(wsClient.url(lastTaxYearAmountUrl).withHttpHeaders(
              HeaderNames.COOKIE -> playSessionCookies,
              xSessionId, csrfContent
            ).post(Map[String, String](
              "amount" -> "1234"
            )))
          }

          result.status shouldBe OK
        }

      }

      "return an error" when {

        "the submitted data is empty" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890"
          ))

          lazy val result = {
            authoriseAgent()
            await(wsClient.url(lastTaxYearAmountUrl).withHttpHeaders(
              HeaderNames.COOKIE -> playSessionCookies,
              xSessionId, csrfContent
            ).post(Map[String, String](
              "amount" -> ""
            )))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck("Error: " + Content.headingAgent)
          h1Check(Content.headingAgent + " " + Content.caption)
          textOnPageCheck(Content.paraAgent, Selectors.para)
          inputFieldCheck("amount", ".govuk-input")
          hintTextCheck(Content.hint)
          captionCheck(Content.caption)
          buttonCheck(Content.button)

          errorSummaryCheck(Content.noSelectionErrorAgent, "#amount")
          errorAboveElementCheck(Content.noSelectionErrorAgent)
        }

        "the submitted data is too long" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890"
          ))

          lazy val result = {
            authoriseAgent()
            await(wsClient.url(lastTaxYearAmountUrl).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, xSessionId, csrfContent).post(Map(
              "amount" -> "999999999999999999999999999999999999999999999999"
            )))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck("Error: " + Content.headingAgent)
          h1Check(Content.headingAgent + " " + Content.caption)
          textOnPageCheck(Content.paraAgent, Selectors.para)
          inputFieldCheck("amount", ".govuk-input")
          hintTextCheck(Content.hint)
          captionCheck(Content.caption)
          buttonCheck(Content.button)

          errorSummaryCheck(Content.tooLongErrorAgent, "#amount")
          errorAboveElementCheck(Content.tooLongErrorAgent)
        }

        "the submitted data is in the incorrect format" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890"
          ))

          lazy val result = {
            authoriseAgent()
            await(wsClient.url(lastTaxYearAmountUrl).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, xSessionId, csrfContent).post(Map(
              "amount" -> ".."
            )))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck("Error: " + Content.headingAgent)
          h1Check(Content.headingAgent + " " + Content.caption)
          textOnPageCheck(Content.paraAgent, Selectors.para)
          inputFieldCheck("amount", ".govuk-input")
          hintTextCheck(Content.hint)
          captionCheck(Content.caption)
          buttonCheck(Content.button)

          errorSummaryCheck(Content.invalidFormatError, "#amount")
          errorAboveElementCheck(Content.invalidFormatError)
        }
      }

    }

  }

}
