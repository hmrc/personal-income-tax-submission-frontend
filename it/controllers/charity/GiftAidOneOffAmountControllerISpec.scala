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

class GiftAidOneOffAmountControllerISpec extends IntegrationTest with ViewHelpers {


  val taxYear: Int = 2022

  object IndividualExpected {
    val expectedTitle = "How much did you donate to charity as one-off payments?"
    val expectedH1 = "How much did you donate to charity as one-off payments?"
    val expectedParagraph = "Do not include the Gift Aid added to your donation."
    val expectedErrorEmpty = "Enter the amount you donated to charity as one-off payments"
    val expectedErrorInvalid = "Enter the amount you donated as one-off payments in the correct format"
    val expectedErrorOverMax = "The amount you donated as one-off payments must be less than £100,000,000,000"
    val expectedErrorTitle = s"Error: $expectedTitle"

    val expectedTitleCy = "How much did you donate to charity as one-off payments?"
    val expectedH1Cy = "How much did you donate to charity as one-off payments?"
    val expectedParagraphCy = "Do not include the Gift Aid added to your donation."
    val expectedErrorEmptyCy = "Enter the amount you donated to charity as one-off payments"
    val expectedErrorInvalidCy = "Enter the amount you donated as one-off payments in the correct format"
    val expectedErrorOverMaxCy = "The amount you donated as one-off payments must be less than £100,000,000,000"
    val expectedErrorTitleCy = s"Error: $expectedTitle"
  }

  object AgentExpected {
    val expectedTitle = "How much did your client donate to charity as one-off payments?"
    val expectedH1 = "How much did your client donate to charity as one-off payments?"
    val expectedParagraph = "Do not include the Gift Aid added to your client’s donation."
    val expectedErrorEmpty = "Enter the amount your client donated to charity as one-off payments"
    val expectedErrorInvalid = "Enter the amount your client donated as one-off payments in the correct format"
    val expectedErrorOverMax = "The amount your client donated as one-off payments must be less than £100,000,000,000"
    val expectedErrorTitle = s"Error: $expectedTitle"

    val expectedTitleCy = "How much did your client donate to charity as one-off payments?"
    val expectedH1Cy = "How much did your client donate to charity as one-off payments?"
    val expectedParagraphCy = "Do not include the Gift Aid added to your client’s donation."
    val expectedErrorEmptyCy = "Enter the amount your client donated to charity as one-off payments"
    val expectedErrorInvalidCy = "Enter the amount your client donated as one-off payments in the correct format"
    val expectedErrorOverMaxCy = "The amount your client donated as one-off payments must be less than £100,000,000,000"
    val expectedErrorTitleCy = s"Error: $expectedTitle"
  }

  val expectedCaption = "Donations to charity for 6 April 2021 to 5 April 2022"
  val expectedInputName = "amount"
  val expectedButtonText = "Continue"
  val expectedInputLabelText = "Total amount for the year, in pounds"
  val expectedInputHintText = "For example, £600 or £193.54"

  val expectedCaptionCy = "Donations to charity for 6 April 2021 to 5 April 2022"
  val expectedInputNameCy = "amount"
  val expectedButtonTextCy = "Continue"
  val expectedInputLabelTextCy = "Total amount for the year, in pounds"
  val expectedInputHintTextCy = "For example, £600 or £193.54"

  val expectedErrorLink = "#amount"
  val captionSelector = ".govuk-caption-l"
  val paragraphSelector = "#main-content > div > div > form > div > label > p"
  val inputFieldSelector = "#amount"
  val buttonSelector = ".govuk-button"
  val inputLabelSelector = "#main-content > div > div > form > div > label > div"
  val inputHintTextSelector = ".govuk-hint"

  "as an individual" when {
    import IndividualExpected._
    ".show" should {

      "returns an action with the correct english content" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/amount-donated-as-one-off ")
            .get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an OK(200) status" in {
          result.status shouldBe OK
        }
        titleCheck(expectedTitle)
        h1Check(expectedH1 + " " + expectedCaption)
        welshToggleCheck("English")
        textOnPageCheck(expectedCaption, captionSelector)
        textOnPageCheck(expectedParagraph, paragraphSelector)
        textOnPageCheck(expectedInputLabelText, inputLabelSelector)
        textOnPageCheck(expectedInputHintText, inputHintTextSelector)
        inputFieldCheck(expectedInputName, inputFieldSelector)
        buttonCheck(expectedButtonText, buttonSelector)

      }
      "returns an action with the correct welsh content" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/amount-donated-as-one-off ")
            .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy")
            .get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an OK(200) status" in {
          result.status shouldBe OK
        }
        titleCheck(expectedTitleCy)
        h1Check(expectedH1Cy + " " + expectedCaptionCy)
        welshToggleCheck("Welsh")
        textOnPageCheck(expectedCaptionCy, captionSelector)
        textOnPageCheck(expectedParagraphCy, paragraphSelector)
        textOnPageCheck(expectedInputLabelTextCy, inputLabelSelector)
        textOnPageCheck(expectedInputHintTextCy, inputHintTextSelector)
        inputFieldCheck(expectedInputNameCy, inputFieldSelector)
        buttonCheck(expectedButtonTextCy, buttonSelector)

      }
    }

    ".submit" should {

      s"return an OK($OK) status" in {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(
            wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/amount-donated-as-one-off ")
              .post(Map("amount" -> "123000.42"))
          )
        }

        result.status shouldBe OK
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with the correct Empty error" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/amount-donated-as-one-off ")
            .post(Map[String, String]()))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)
        errorSummaryCheck(expectedErrorEmpty, expectedErrorLink)
        errorAboveElementCheck(expectedErrorEmpty)
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with the correct invalid error" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/amount-donated-as-one-off ")
            .post(Map("amount" -> "|")))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)
        errorSummaryCheck(expectedErrorInvalid, expectedErrorLink)
        errorAboveElementCheck(expectedErrorInvalid)
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with the correct overMax error" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/amount-donated-as-one-off ")
            .post(Map("amount" -> "9999999999999999999999999")))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)
        errorSummaryCheck(expectedErrorOverMax, expectedErrorLink)
        errorAboveElementCheck(expectedErrorOverMax)
      }
      s"return a BAD_REQUEST($BAD_REQUEST) status with the correct Empty error - Welsh" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/amount-donated-as-one-off ")
            .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy")
            .post(Map[String, String]()))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)
        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorEmptyCy, expectedErrorLink)
        errorAboveElementCheck(expectedErrorEmptyCy)
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with the correct invalid error - Welsh" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/amount-donated-as-one-off ")
            .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy")
            .post(Map("amount" -> "|")))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)
        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorInvalidCy, expectedErrorLink)
        errorAboveElementCheck(expectedErrorInvalidCy)
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with the correct overMax error - Welsh" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/amount-donated-as-one-off ")
            .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy")
            .post(Map("amount" -> "9999999999999999999999999")))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)
        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorOverMaxCy, expectedErrorLink)
        errorAboveElementCheck(expectedErrorOverMaxCy)
      }

    }

  }

  "as an agent" when {
    import AgentExpected._
    ".show" should {

      "returns an action with the correct english content" which {
        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/amount-donated-as-one-off ")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie)
            .get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an OK(200) status" in {
          result.status shouldBe OK
        }
        titleCheck(expectedTitle)
        h1Check(expectedH1 + " " + expectedCaption)
        welshToggleCheck("English")
        textOnPageCheck(expectedCaption, captionSelector)
        textOnPageCheck(expectedParagraph, paragraphSelector)
        textOnPageCheck(expectedInputLabelText, inputLabelSelector)
        textOnPageCheck(expectedInputHintText, inputHintTextSelector)
        inputFieldCheck(expectedInputName, inputFieldSelector)
        buttonCheck(expectedButtonText, buttonSelector)
      }
      "returns an action with the correct welsh content" which {
        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/amount-donated-as-one-off ")
            .withHttpHeaders(
              HeaderNames.COOKIE -> sessionCookie,
              HeaderNames.ACCEPT_LANGUAGE -> "cy"
            )
            .get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an OK(200) status" in {
          result.status shouldBe OK
        }
        titleCheck(expectedTitleCy)
        h1Check(expectedH1Cy + " " + expectedCaptionCy)
        welshToggleCheck("Welsh")
        textOnPageCheck(expectedCaptionCy, captionSelector)
        textOnPageCheck(expectedParagraphCy, paragraphSelector)
        textOnPageCheck(expectedInputLabelTextCy, inputLabelSelector)
        textOnPageCheck(expectedInputHintTextCy, inputHintTextSelector)
        inputFieldCheck(expectedInputNameCy, inputFieldSelector)
        buttonCheck(expectedButtonTextCy, buttonSelector)
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
              wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/amount-donated-as-one-off ")
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
                .post(Map("amount" -> "12344.98"))
            )
          }

          result.status shouldBe OK
        }
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with the Empty Error" which {

        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/amount-donated-as-one-off ")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post(Map[String, String]()))
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "returns the the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        errorSummaryCheck(expectedErrorEmpty, expectedErrorLink)
        errorAboveElementCheck(expectedErrorEmpty)

      }
      s"return a BAD_REQUEST($BAD_REQUEST) status with the Invalid Format error" which {

        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/amount-donated-as-one-off ")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post(Map("amount" -> "|")))
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "returns the the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        errorSummaryCheck(expectedErrorInvalid, expectedErrorLink)
        errorAboveElementCheck(expectedErrorInvalid)

      }
      s"return a BAD_REQUEST($BAD_REQUEST) status with the OverMax error" which {

        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/amount-donated-as-one-off ")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post(Map("amount" -> "9999999999999999999999999999999")))
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "returns the the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        errorSummaryCheck(expectedErrorOverMax, expectedErrorLink)
        errorAboveElementCheck(expectedErrorOverMax)

      }
      s"return a BAD_REQUEST($BAD_REQUEST) status with the Empty Error - Welsh" which {

        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/amount-donated-as-one-off ")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", "Csrf-Token" -> "nocheck")
            .post(Map[String, String]()))
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "returns the the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorEmptyCy, expectedErrorLink)
        errorAboveElementCheck(expectedErrorEmptyCy)

      }
      s"return a BAD_REQUEST($BAD_REQUEST) status with the Invalid Format error - Welsh" which {

        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/amount-donated-as-one-off ")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", "Csrf-Token" -> "nocheck")
            .post(Map("amount" -> "|")))
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "returns the the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorInvalidCy, expectedErrorLink)
        errorAboveElementCheck(expectedErrorInvalidCy)

      }
      s"return a BAD_REQUEST($BAD_REQUEST) status with the OverMax error - Welsh" which {

        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/amount-donated-as-one-off ")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", "Csrf-Token" -> "nocheck")
            .post(Map("amount" -> "9999999999999999999999999999999")))
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "returns the the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorOverMaxCy, expectedErrorLink)
        errorAboveElementCheck(expectedErrorOverMaxCy)

      }

    }

  }

}
