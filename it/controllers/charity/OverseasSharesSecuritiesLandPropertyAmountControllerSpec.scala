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
import play.api.http.Status._
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{IntegrationTest, ViewHelpers}

class OverseasSharesSecuritiesLandPropertyAmountControllerSpec extends IntegrationTest with ViewHelpers {

  val defaultTaxYear = 2022


  def url: String =
    s"http://localhost:$port/income-through-software/return/personal-income/$defaultTaxYear/charity/value-of-shares-securities-land-or-property-to-overseas-charities"

  object Selectors {
    val titleSelector = "title"
    val inputField = ".govuk-input"
    val inputLabel = ".govuk-label > div"
  }

  object ExpectedResultsEn {
    val heading: String = "What is the value of qualifying shares, securities, land or property donated to overseas charities?"
    val hintText: String = "For example, £600 or £193.54"
    val caption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val button = "Continue"
    val inputName = "amount"
    val inputLabel = "Total value, in pounds"
  }

  object ExpectedErrorsEn {
    val tooLong = "The value of your shares, securities, land or property must be less than £100,000,000,000"
    val emptyField = "Enter the value of shares, securities, land or property you donated to overseas charities"
    val incorrectFormatIndividual = "Enter the value of shares, securities, land or property you donated to overseas charities in the correct format"

    val tooLongAgent = "The value of your client’s shares, securities, land or property must be less than £100,000,000,000"
    val emptyFieldAgent = "Enter the value of shares, securities, land or property your client donated to overseas charities"
    val incorrectFormatAgent = "Enter the value of shares, securities, land or property your client donated to overseas charities in the correct format"

    val errorHref = "#amount"
  }

  object ExpectedResultsCy {
    val heading: String = "What is the value of qualifying shares, securities, land or property donated to overseas charities?"
    val hintText: String = "For example, £600 or £193.54"
    val caption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val button = "Continue"
    val inputName = "amount"
    val inputLabel = "Total value, in pounds"
  }

  object ExpectedErrorsCy {
    val tooLong = "The value of your shares, securities, land or property must be less than £100,000,000,000"
    val emptyField = "Enter the value of shares, securities, land or property you donated to overseas charities"
    val incorrectFormatIndividual = "Enter the value of shares, securities, land or property you donated to overseas charities in the correct format"

    val tooLongAgent = "The value of your client’s shares, securities, land or property must be less than £100,000,000,000"
    val emptyFieldAgent = "Enter the value of shares, securities, land or property your client donated to overseas charities"
    val incorrectFormatAgent = "Enter the value of shares, securities, land or property your client donated to overseas charities in the correct format"

    val errorHref = "#amount"
  }

  "in english" when {

    "calling GET /2022/charity/value-of-shares-securities-land-or-property-to-overseas-charities" when {

      "an individual" should {

        "return a page" which {
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(url).get())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(ExpectedResultsEn.heading)
          h1Check(ExpectedResultsEn.heading + " " + ExpectedResultsEn.caption)
          inputFieldCheck(ExpectedResultsEn.inputName, Selectors.inputField)
          textOnPageCheck(ExpectedResultsEn.inputLabel, Selectors.inputLabel)
          hintTextCheck(ExpectedResultsEn.hintText)
          captionCheck(ExpectedResultsEn.caption)
          buttonCheck(ExpectedResultsEn.button)

          noErrorsCheck()

          welshToggleCheck(ENGLISH)
        }
      }

      "an agent" should {

        "return a page" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> defaultTaxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890"
          ))

          lazy val result: WSResponse = {
            authoriseAgent()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").get())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(ExpectedResultsEn.heading)
          h1Check(ExpectedResultsEn.heading + " " + ExpectedResultsEn.caption)
          inputFieldCheck(ExpectedResultsEn.inputName, Selectors.inputField)
          textOnPageCheck(ExpectedResultsEn.inputLabel, Selectors.inputLabel)
          hintTextCheck(ExpectedResultsEn.hintText)
          captionCheck(ExpectedResultsEn.caption)
          buttonCheck(ExpectedResultsEn.button)

          noErrorsCheck()

          welshToggleCheck(ENGLISH)
        }

      }

    }

    "calling POST /2022/charity/value-of-shares-securities-land-or-property-to-overseas-charities" when {

      "an individual" when {

        "the form data is valid" should {

          "return an OK" in {
            lazy val result: WSResponse = {
              authoriseIndividual()
              await(wsClient.url(url).post(Map[String, String](
                "amount" -> "1234"
              )))
            }

            result.status shouldBe OK
          }

        }

        "return an error" when {

          "the submitted data is empty" which {
            lazy val result: WSResponse = {
              authoriseIndividual()
              await(wsClient.url(url).post(Map[String, String](
                "amount" -> ""
              )))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck("Error: " + ExpectedResultsEn.heading)
            h1Check(ExpectedResultsEn.heading + " " + ExpectedResultsEn.caption)
            inputFieldCheck(ExpectedResultsEn.inputName, Selectors.inputField)
            textOnPageCheck(ExpectedResultsEn.inputLabel, Selectors.inputLabel)
            hintTextCheck(ExpectedResultsEn.hintText)
            captionCheck(ExpectedResultsEn.caption)
            buttonCheck(ExpectedResultsEn.button)

            errorSummaryCheck(ExpectedErrorsEn.emptyField, ExpectedErrorsEn.errorHref)
            errorAboveElementCheck(ExpectedErrorsEn.emptyField)

            welshToggleCheck(ENGLISH)
          }

          "the submitted data is too long" which {
            lazy val result: WSResponse = {
              authoriseIndividual()
              await(wsClient.url(url).post(Map(
                "amount" -> "999999999999999999999999999999999999999999999999"
              )))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck("Error: " + ExpectedResultsEn.heading)
            h1Check(ExpectedResultsEn.heading + " " + ExpectedResultsEn.caption)
            inputFieldCheck(ExpectedResultsEn.inputName, Selectors.inputField)
            textOnPageCheck(ExpectedResultsEn.inputLabel, Selectors.inputLabel)
            hintTextCheck(ExpectedResultsEn.hintText)
            captionCheck(ExpectedResultsEn.caption)
            buttonCheck(ExpectedResultsEn.button)

            errorSummaryCheck(ExpectedErrorsEn.tooLong, ExpectedErrorsEn.errorHref)
            errorAboveElementCheck(ExpectedErrorsEn.tooLong)

            welshToggleCheck(ENGLISH)
          }

          "the submitted data is in the incorrect format" which {
            lazy val result: WSResponse = {
              authoriseIndividual()
              await(wsClient.url(url).post(Map(
                "amount" -> ":@~{}<>?"
              )))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck("Error: " + ExpectedResultsEn.heading)
            h1Check(ExpectedResultsEn.heading + " " + ExpectedResultsEn.caption)
            inputFieldCheck(ExpectedResultsEn.inputName, Selectors.inputField)
            textOnPageCheck(ExpectedResultsEn.inputLabel, Selectors.inputLabel)
            hintTextCheck(ExpectedResultsEn.hintText)
            captionCheck(ExpectedResultsEn.caption)
            buttonCheck(ExpectedResultsEn.button)

            errorSummaryCheck(ExpectedErrorsEn.incorrectFormatIndividual, ExpectedErrorsEn.errorHref)
            errorAboveElementCheck(ExpectedErrorsEn.incorrectFormatIndividual)

            welshToggleCheck(ENGLISH)
          }
        }

      }

      "an agent" when {

        "the form data is valid" should {

          "return an OK" in {
            lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
              SessionValues.TAX_YEAR -> defaultTaxYear.toString,
              SessionValues.CLIENT_NINO -> "AA123456A",
              SessionValues.CLIENT_MTDITID -> "1234567890"
            ))

            lazy val result: WSResponse = {
              authoriseAgent()
              await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").post(Map[String, String](
                "amount" -> "1234"
              )))
            }

            result.status shouldBe OK
          }

        }

        "return an error" when {

          "the submitted data is empty" which {
            lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
              SessionValues.TAX_YEAR -> defaultTaxYear.toString,
              SessionValues.CLIENT_NINO -> "AA123456A",
              SessionValues.CLIENT_MTDITID -> "1234567890"
            ))

            lazy val result: WSResponse = {
              authoriseAgent()
              await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").post(Map[String, String](
                "amount" -> ""
              )))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck("Error: " + ExpectedResultsEn.heading)
            h1Check(ExpectedResultsEn.heading + " " + ExpectedResultsEn.caption)
            inputFieldCheck(ExpectedResultsEn.inputName, Selectors.inputField)
            textOnPageCheck(ExpectedResultsEn.inputLabel, Selectors.inputLabel)
            hintTextCheck(ExpectedResultsEn.hintText)
            captionCheck(ExpectedResultsEn.caption)
            buttonCheck(ExpectedResultsEn.button)

            errorSummaryCheck(ExpectedErrorsEn.emptyFieldAgent, ExpectedErrorsEn.errorHref)
            errorAboveElementCheck(ExpectedErrorsEn.emptyFieldAgent)

            welshToggleCheck(ENGLISH)
          }

          "the submitted data is too long" which {
            lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
              SessionValues.TAX_YEAR -> defaultTaxYear.toString,
              SessionValues.CLIENT_NINO -> "AA123456A",
              SessionValues.CLIENT_MTDITID -> "1234567890"
            ))

            lazy val result: WSResponse = {
              authoriseAgent()
              await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").post(Map(
                "amount" -> "999999999999999999999999999999999999999999999999"
              )))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck("Error: " + ExpectedResultsEn.heading)
            h1Check(ExpectedResultsEn.heading + " " + ExpectedResultsEn.caption)
            inputFieldCheck(ExpectedResultsEn.inputName, Selectors.inputField)
            textOnPageCheck(ExpectedResultsEn.inputLabel, Selectors.inputLabel)
            hintTextCheck(ExpectedResultsEn.hintText)
            captionCheck(ExpectedResultsEn.caption)
            buttonCheck(ExpectedResultsEn.button)

            errorSummaryCheck(ExpectedErrorsEn.tooLongAgent, ExpectedErrorsEn.errorHref)
            errorAboveElementCheck(ExpectedErrorsEn.tooLongAgent)

            welshToggleCheck(ENGLISH)
          }

          "the submitted data is in the incorrect format" which {
            lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
              SessionValues.TAX_YEAR -> defaultTaxYear.toString,
              SessionValues.CLIENT_NINO -> "AA123456A",
              SessionValues.CLIENT_MTDITID -> "1234567890"
            ))

            lazy val result: WSResponse = {
              authoriseAgent()
              await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").post(Map(
                "amount" -> ":@~{}<>?"
              )))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck("Error: " + ExpectedResultsEn.heading)
            h1Check(ExpectedResultsEn.heading + " " + ExpectedResultsEn.caption)
            inputFieldCheck(ExpectedResultsEn.inputName, Selectors.inputField)
            textOnPageCheck(ExpectedResultsEn.inputLabel, Selectors.inputLabel)
            hintTextCheck(ExpectedResultsEn.hintText)
            captionCheck(ExpectedResultsEn.caption)
            buttonCheck(ExpectedResultsEn.button)

            errorSummaryCheck(ExpectedErrorsEn.incorrectFormatAgent, ExpectedErrorsEn.errorHref)
            errorAboveElementCheck(ExpectedErrorsEn.incorrectFormatAgent)

            welshToggleCheck(ENGLISH)
          }
        }

      }

    }

  }

  "in welsh" when {

    "calling GET /2022/charity/value-of-shares-securities-land-or-property-to-overseas-charities" when {

      "an individual" should {

        "return a page" which {
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(url).withHttpHeaders(
              HeaderNames.ACCEPT_LANGUAGE -> "cy"
            ).get())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(ExpectedResultsCy.heading)
          h1Check(ExpectedResultsCy.heading + " " + ExpectedResultsCy.caption)
          inputFieldCheck(ExpectedResultsCy.inputName, Selectors.inputField)
          textOnPageCheck(ExpectedResultsCy.inputLabel, Selectors.inputLabel)
          hintTextCheck(ExpectedResultsCy.hintText)
          captionCheck(ExpectedResultsCy.caption)
          buttonCheck(ExpectedResultsCy.button)

          noErrorsCheck()

          welshToggleCheck(WELSH)
        }
      }

      "an agent" should {

        "return a page" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> defaultTaxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890"
          ))

          lazy val result: WSResponse = {
            authoriseAgent()
            await(wsClient.url(url).withHttpHeaders(
              HeaderNames.COOKIE -> playSessionCookies,
              "Csrf-Token" -> "nocheck",
              HeaderNames.ACCEPT_LANGUAGE -> "cy"
            ).get())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(ExpectedResultsCy.heading)
          h1Check(ExpectedResultsCy.heading + " " + ExpectedResultsCy.caption)
          inputFieldCheck(ExpectedResultsCy.inputName, Selectors.inputField)
          textOnPageCheck(ExpectedResultsCy.inputLabel, Selectors.inputLabel)
          hintTextCheck(ExpectedResultsCy.hintText)
          captionCheck(ExpectedResultsCy.caption)
          buttonCheck(ExpectedResultsCy.button)

          noErrorsCheck()

          welshToggleCheck(WELSH)
        }

      }

    }

    "calling POST /2022/charity/value-of-shares-securities-land-or-property-to-overseas-charities" when {

      "an individual" when {

        "the form data is valid" should {

          "return an OK" in {
            lazy val result: WSResponse = {
              authoriseIndividual()
              await(wsClient.url(url).withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy").post(Map[String, String](
                "amount" -> "1234"
              )))
            }

            result.status shouldBe OK
          }

        }

        "return an error" when {

          "the submitted data is empty" which {
            lazy val result: WSResponse = {
              authoriseIndividual()
              await(wsClient.url(url).withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy").post(Map[String, String](
                "amount" -> ""
              )))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck("Error: " + ExpectedResultsCy.heading)
            h1Check(ExpectedResultsCy.heading + " " + ExpectedResultsCy.caption)
            inputFieldCheck(ExpectedResultsCy.inputName, Selectors.inputField)
            textOnPageCheck(ExpectedResultsCy.inputLabel, Selectors.inputLabel)
            hintTextCheck(ExpectedResultsCy.hintText)
            captionCheck(ExpectedResultsCy.caption)
            buttonCheck(ExpectedResultsCy.button)

            errorSummaryCheck(ExpectedErrorsCy.emptyField, ExpectedErrorsCy.errorHref)
            errorAboveElementCheck(ExpectedErrorsCy.emptyField)

            welshToggleCheck(WELSH)
          }

          "the submitted data is too long" which {
            lazy val result: WSResponse = {
              authoriseIndividual()
              await(wsClient.url(url).withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy").post(Map(
                "amount" -> "999999999999999999999999999999999999999999999999"
              )))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck("Error: " + ExpectedResultsCy.heading)
            h1Check(ExpectedResultsCy.heading + " " + ExpectedResultsCy.caption)
            inputFieldCheck(ExpectedResultsCy.inputName, Selectors.inputField)
            textOnPageCheck(ExpectedResultsCy.inputLabel, Selectors.inputLabel)
            hintTextCheck(ExpectedResultsCy.hintText)
            captionCheck(ExpectedResultsCy.caption)
            buttonCheck(ExpectedResultsCy.button)

            errorSummaryCheck(ExpectedErrorsCy.tooLong, ExpectedErrorsCy.errorHref)
            errorAboveElementCheck(ExpectedErrorsCy.tooLong)

            welshToggleCheck(WELSH)
          }

          "the submitted data is in the incorrect format" which {
            lazy val result: WSResponse = {
              authoriseIndividual()
              await(wsClient.url(url).withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy").post(Map(
                "amount" -> ":@~{}<>?"
              )))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck("Error: " + ExpectedResultsCy.heading)
            h1Check(ExpectedResultsCy.heading + " " + ExpectedResultsCy.caption)
            inputFieldCheck(ExpectedResultsCy.inputName, Selectors.inputField)
            textOnPageCheck(ExpectedResultsCy.inputLabel, Selectors.inputLabel)
            hintTextCheck(ExpectedResultsCy.hintText)
            captionCheck(ExpectedResultsCy.caption)
            buttonCheck(ExpectedResultsCy.button)

            errorSummaryCheck(ExpectedErrorsCy.incorrectFormatIndividual, ExpectedErrorsCy.errorHref)
            errorAboveElementCheck(ExpectedErrorsCy.incorrectFormatIndividual)

            welshToggleCheck(WELSH)
          }
        }

      }

      "an agent" when {

        "the form data is valid" should {

          "return an OK" in {
            lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
              SessionValues.TAX_YEAR -> defaultTaxYear.toString,
              SessionValues.CLIENT_NINO -> "AA123456A",
              SessionValues.CLIENT_MTDITID -> "1234567890"
            ))

            lazy val result: WSResponse = {
              authoriseAgent()
              await(wsClient.url(url).withHttpHeaders(
                HeaderNames.COOKIE -> playSessionCookies,
                "Csrf-Token" -> "nocheck",
                HeaderNames.ACCEPT_LANGUAGE -> "cy"
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
              SessionValues.TAX_YEAR -> defaultTaxYear.toString,
              SessionValues.CLIENT_NINO -> "AA123456A",
              SessionValues.CLIENT_MTDITID -> "1234567890"
            ))

            lazy val result: WSResponse = {
              authoriseAgent()
              await(wsClient.url(url).withHttpHeaders(
                HeaderNames.COOKIE -> playSessionCookies,
                "Csrf-Token" -> "nocheck",
                HeaderNames.ACCEPT_LANGUAGE -> "cy"
              ).post(Map[String, String](
                "amount" -> ""
              )))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck("Error: " + ExpectedResultsCy.heading)
            h1Check(ExpectedResultsCy.heading + " " + ExpectedResultsCy.caption)
            inputFieldCheck(ExpectedResultsCy.inputName, Selectors.inputField)
            textOnPageCheck(ExpectedResultsCy.inputLabel, Selectors.inputLabel)
            hintTextCheck(ExpectedResultsCy.hintText)
            captionCheck(ExpectedResultsCy.caption)
            buttonCheck(ExpectedResultsCy.button)

            errorSummaryCheck(ExpectedErrorsCy.emptyFieldAgent, ExpectedErrorsCy.errorHref)
            errorAboveElementCheck(ExpectedErrorsCy.emptyFieldAgent)

            welshToggleCheck(WELSH)
          }

          "the submitted data is too long" which {
            lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
              SessionValues.TAX_YEAR -> defaultTaxYear.toString,
              SessionValues.CLIENT_NINO -> "AA123456A",
              SessionValues.CLIENT_MTDITID -> "1234567890"
            ))

            lazy val result: WSResponse = {
              authoriseAgent()
              await(wsClient.url(url).withHttpHeaders(
                HeaderNames.COOKIE -> playSessionCookies,
                "Csrf-Token" -> "nocheck",
                HeaderNames.ACCEPT_LANGUAGE -> "cy"
              ).post(Map(
                "amount" -> "999999999999999999999999999999999999999999999999"
              )))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck("Error: " + ExpectedResultsCy.heading)
            h1Check(ExpectedResultsCy.heading + " " + ExpectedResultsCy.caption)
            inputFieldCheck(ExpectedResultsCy.inputName, Selectors.inputField)
            textOnPageCheck(ExpectedResultsCy.inputLabel, Selectors.inputLabel)
            hintTextCheck(ExpectedResultsCy.hintText)
            captionCheck(ExpectedResultsCy.caption)
            buttonCheck(ExpectedResultsCy.button)

            errorSummaryCheck(ExpectedErrorsCy.tooLongAgent, ExpectedErrorsCy.errorHref)
            errorAboveElementCheck(ExpectedErrorsCy.tooLongAgent)

            welshToggleCheck(WELSH)
          }

          "the submitted data is in the incorrect format" which {
            lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
              SessionValues.TAX_YEAR -> defaultTaxYear.toString,
              SessionValues.CLIENT_NINO -> "AA123456A",
              SessionValues.CLIENT_MTDITID -> "1234567890"
            ))

            lazy val result: WSResponse = {
              authoriseAgent()
              await(wsClient.url(url).withHttpHeaders(
                HeaderNames.COOKIE -> playSessionCookies,
                "Csrf-Token" -> "nocheck",
                HeaderNames.ACCEPT_LANGUAGE -> "cy"
              ).post(Map(
                "amount" -> ":@~{}<>?"
              )))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck("Error: " + ExpectedResultsCy.heading)
            h1Check(ExpectedResultsCy.heading + " " + ExpectedResultsCy.caption)
            inputFieldCheck(ExpectedResultsCy.inputName, Selectors.inputField)
            textOnPageCheck(ExpectedResultsCy.inputLabel, Selectors.inputLabel)
            hintTextCheck(ExpectedResultsCy.hintText)
            captionCheck(ExpectedResultsCy.caption)
            buttonCheck(ExpectedResultsCy.button)

            errorSummaryCheck(ExpectedErrorsCy.incorrectFormatAgent, ExpectedErrorsCy.errorHref)
            errorAboveElementCheck(ExpectedErrorsCy.incorrectFormatAgent)

            welshToggleCheck(WELSH)
          }
        }

      }

    }

  }

}
