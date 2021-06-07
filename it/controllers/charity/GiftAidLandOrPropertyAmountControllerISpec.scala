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

class GiftAidLandOrPropertyAmountControllerISpec extends IntegrationTest with ViewHelpers {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val taxYear: Int = 2022

  object individualExpected {
    val expectedTitle = "What is the value of land or property donated to charity?"
    val expectedHeading = "What is the value of land or property donated to charity?"
    val expectedContent = "Total value, in pounds"
    val expectedErrorEmpty = "Enter the value of land or property you donated to charity"
    val expectedErrorInvalid = "Enter the value of land or property you donated to charity in the correct format"
    val expectedErrorOverMax = "The value of your land or property must be less than £100,000,000,000"

    val expectedTitleCy = "What is the value of land or property donated to charity?"
    val expectedHeadingCy = "What is the value of land or property donated to charity?"
    val expectedContentCy = "Total value, in pounds"
    val expectedErrorEmptyCy = "Enter the value of land or property you donated to charity"
    val expectedErrorInvalidCy = "Enter the value of land or property you donated to charity in the correct format"
    val expectedErrorOverMaxCy = "The value of your land or property must be less than £100,000,000,000"
  }

  object agentExpected {
    val expectedTitle = "What is the value of land or property donated to charity?"
    val expectedHeading = "What is the value of land or property donated to charity?"
    val expectedContent = "Total value, in pounds"
    val expectedErrorEmpty = "Enter the value of land or property your client donated to charity"
    val expectedErrorInvalid = "Enter the value of land or property your client donated to charity in the correct format"
    val expectedErrorOverMax = "The value of your client’s land or property must be less than £100,000,000,000"

    val expectedTitleCy = "What is the value of land or property donated to charity?"
    val expectedHeadingCy = "What is the value of land or property donated to charity?"
    val expectedContentCy = "Total value, in pounds"
    val expectedErrorEmptyCy = "Enter the value of land or property your client donated to charity"
    val expectedErrorInvalidCy = "Enter the value of land or property your client donated to charity in the correct format"
    val expectedErrorOverMaxCy = "The value of your client’s land or property must be less than £100,000,000,000"
  }

  val expectedCaption = "Donations to charity for 6 April 2021 to 5 April 2022"
  val expectedCaptionCy = "Donations to charity for 6 April 2021 to 5 April 2022"
  val expectedHint = "For example, £600 or £193.54"
  val expectedHintCy = "For example, £600 or £193.54"
  val expectedInputName = "amount"
  val expectedButtonText = "Continue"
  val expectedErrorLink = "#amount"

  val captionSelector = ".govuk-caption-l"
  val inputFieldSelector = "#amount"
  val buttonSelector = ".govuk-button"
  val contentSelector = "#main-content > div > div > form > div > label > div"
  val inputHintTextSelector = "#amount-hint"

  val invalidAmount = "1000000000000"


  "as an individual" when {
    import individualExpected._
    ".show" should {

      "returns an action with english content" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/value-of-land-or-property ")
            .withHttpHeaders(xSessionId, csrfContent)
            .get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an OK(200) status" in {
          result.status shouldBe OK
        }
        titleCheck(expectedTitle)
        h1Check(expectedHeading + " " + expectedCaption)
        welshToggleCheck("English")
        textOnPageCheck(expectedCaption, captionSelector)
        textOnPageCheck(expectedContent, contentSelector)
        textOnPageCheck(expectedHint, inputHintTextSelector)
        inputFieldCheck(expectedInputName, inputFieldSelector)
        buttonCheck(expectedButtonText, buttonSelector)
      }
      "returns an action with welsh content" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/value-of-land-or-property ")
            .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy", xSessionId, csrfContent)
            .get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an OK(200) status" in {
          result.status shouldBe OK
        }
        titleCheck(expectedTitleCy)
        h1Check(expectedHeadingCy + " " + expectedCaptionCy)
        welshToggleCheck(WELSH)
        textOnPageCheck(expectedCaptionCy, captionSelector)
        textOnPageCheck(expectedContentCy, contentSelector)
        textOnPageCheck(expectedHintCy, inputHintTextSelector)
        inputFieldCheck(expectedInputName, inputFieldSelector)
        buttonCheck(expectedButtonText, buttonSelector)
      }
    }

    ".submit" should {

      s"return an OK($OK) status" in {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(
            wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/value-of-land-or-property ")
              .withHttpHeaders(xSessionId, csrfContent)
              .post(Map("amount" -> "123000.42"))
          )
        }

        result.status shouldBe OK
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with the expectedEmptyError" which {

        lazy val result: WSResponse = {

          authoriseIndividual()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/value-of-land-or-property ")
            .withHttpHeaders(xSessionId, csrfContent)
            .post(Map[String, String]()))
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return a bad request status" in {
          result.status shouldBe BAD_REQUEST
        }
        errorSummaryCheck(expectedErrorEmpty, expectedErrorLink)
        errorAboveElementCheck(expectedErrorEmpty)

      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with the expectedErrorOverMax" which {

        lazy val result: WSResponse = {

          authoriseIndividual()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/value-of-land-or-property ")
            .withHttpHeaders(xSessionId, csrfContent)
            .post(Map("amount" -> invalidAmount)))
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return a bad request status" in {
          result.status shouldBe BAD_REQUEST
        }
        errorSummaryCheck(expectedErrorOverMax, expectedErrorLink)
        errorAboveElementCheck(expectedErrorOverMax)

      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with the expectedInvalidCharacters" which {

        lazy val result: WSResponse = {

          authoriseIndividual()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/value-of-land-or-property ")
            .withHttpHeaders(xSessionId, csrfContent)
            .post(Map("amount" -> "12344.98...")))
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return a bad request status" in {
          result.status shouldBe BAD_REQUEST
        }
        errorSummaryCheck(expectedErrorInvalid, expectedErrorLink)
        errorAboveElementCheck(expectedErrorInvalid)

      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with the expectedEmptyError in welsh" which {

        lazy val result: WSResponse = {

          authoriseIndividual()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/value-of-land-or-property ")
            .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy", xSessionId, csrfContent)
            .post(Map[String, String]()))
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return a bad request status" in {
          result.status shouldBe BAD_REQUEST
        }
        errorSummaryCheck(expectedErrorEmptyCy, expectedErrorLink)
        errorAboveElementCheck(expectedErrorEmptyCy)

      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with the expectedErrorOverMax in welsh" which {

        lazy val result: WSResponse = {

          authoriseIndividual()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/value-of-land-or-property ")
            .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy", xSessionId, csrfContent)
            .post(Map("amount" -> invalidAmount)))
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return a bad request status" in {
          result.status shouldBe BAD_REQUEST
        }
        errorSummaryCheck(expectedErrorOverMaxCy, expectedErrorLink)
        errorAboveElementCheck(expectedErrorOverMaxCy)

      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with the expectedInvalidCharacters in welsh" which {

        lazy val result: WSResponse = {

          authoriseIndividual()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/value-of-land-or-property ")
            .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy", xSessionId, csrfContent)
            .post(Map("amount" -> "12344.98...")))
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return a bad request status" in {
          result.status shouldBe BAD_REQUEST
        }
        errorSummaryCheck(expectedErrorInvalidCy, expectedErrorLink)
        errorAboveElementCheck(expectedErrorInvalidCy)

      }


    }

  }

  "as an agent" when {
    import agentExpected._
    ".show" should {

      "returns an action with english content" which {
        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/value-of-land-or-property ")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
            .get())
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an OK(200) status" in {
          result.status shouldBe OK
        }
        titleCheck(expectedTitle)
        h1Check(expectedHeading + " " + expectedCaption)
        textOnPageCheck(expectedCaption, captionSelector)
        textOnPageCheck(expectedContent, contentSelector)
        textOnPageCheck(expectedHint, inputHintTextSelector)
        inputFieldCheck(expectedInputName, inputFieldSelector)
        buttonCheck(expectedButtonText, buttonSelector)
      }
      "returns an action with welsh content" which {
        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/value-of-land-or-property ")
            .withHttpHeaders(
              HeaderNames.COOKIE -> sessionCookie,
              HeaderNames.ACCEPT_LANGUAGE -> "cy",
              xSessionId, csrfContent
            )
            .get())
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an OK(200) status" in {
          result.status shouldBe OK
        }
        titleCheck(expectedTitleCy)
        h1Check(expectedHeadingCy + " " + expectedCaptionCy)
        textOnPageCheck(expectedCaptionCy, captionSelector)
        textOnPageCheck(expectedContentCy, contentSelector)
        textOnPageCheck(expectedHintCy, inputHintTextSelector)
        inputFieldCheck(expectedInputName, inputFieldSelector)
        buttonCheck(expectedButtonText, buttonSelector)
      }
    }

    ".submit" should {

      s"return an OK($OK) status" when {

        "there is form data" in {
          lazy val result: WSResponse = {
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"))

            authoriseAgent()
            await(
              wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/value-of-land-or-property ")
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
                .post(Map("amount" -> "12344.98"))
            )
          }

          result.status shouldBe OK
        }
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with the expectedEmptyError" which {

        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/value-of-land-or-property ")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
            .post(Map[String, String]()))
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return a bad request status" in {
          result.status shouldBe BAD_REQUEST
        }
        errorSummaryCheck(expectedErrorEmpty, expectedErrorLink)
        errorAboveElementCheck(expectedErrorEmpty)

      }
      s"return a BAD_REQUEST($BAD_REQUEST) status with the expectedErrorOverMax" which {

        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/value-of-land-or-property ")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
            .post(Map("amount" -> invalidAmount)))
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return a bad request status" in {
          result.status shouldBe BAD_REQUEST
        }
        errorSummaryCheck(expectedErrorOverMax, expectedErrorLink)
        errorAboveElementCheck(expectedErrorOverMax)

      }
      s"return a BAD_REQUEST($BAD_REQUEST) status with the expectedInvalidCharacters" which {

        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/value-of-land-or-property ")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
            .post(Map("amount" -> "12344.98...")))
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return a bad request status" in {
          result.status shouldBe BAD_REQUEST
        }
        errorSummaryCheck(expectedErrorInvalid, expectedErrorLink)
        errorAboveElementCheck(expectedErrorInvalid)

      }
      s"return a BAD_REQUEST($BAD_REQUEST) status with the expectedEmptyError in welsh" which {

        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/value-of-land-or-property ")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent, HeaderNames.ACCEPT_LANGUAGE -> "cy")
            .post(Map[String, String]()))
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return a bad request status" in {
          result.status shouldBe BAD_REQUEST
        }
        errorSummaryCheck(expectedErrorEmptyCy, expectedErrorLink)
        errorAboveElementCheck(expectedErrorEmptyCy)

      }
      s"return a BAD_REQUEST($BAD_REQUEST) status with the expectedErrorOverMax in welsh" which {

        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/value-of-land-or-property ")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent, HeaderNames.ACCEPT_LANGUAGE -> "cy")
            .post(Map("amount" -> invalidAmount)))
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return a bad request status" in {
          result.status shouldBe BAD_REQUEST
        }
        errorSummaryCheck(expectedErrorOverMaxCy, expectedErrorLink)
        errorAboveElementCheck(expectedErrorOverMaxCy)

      }
      s"return a BAD_REQUEST($BAD_REQUEST) status with the expectedInvalidCharacters in welsh" which {

        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/value-of-land-or-property ")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent, HeaderNames.ACCEPT_LANGUAGE -> "cy")
            .post(Map("amount" -> "12344.98...")))
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return a bad request status" in {
          result.status shouldBe BAD_REQUEST
        }
        errorSummaryCheck(expectedErrorInvalidCy, expectedErrorLink)
        errorAboveElementCheck(expectedErrorInvalidCy)

      }

    }

  }

}
