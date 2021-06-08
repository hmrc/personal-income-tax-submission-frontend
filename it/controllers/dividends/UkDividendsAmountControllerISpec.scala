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

package controllers.dividends

import common.SessionValues
import helpers.PlaySessionCookieBaker
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import models.dividends.{DividendsCheckYourAnswersModel, DividendsPriorSubmission}
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{IntegrationTest, ViewHelpers}

class UkDividendsAmountControllerISpec extends IntegrationTest with ViewHelpers {



  val taxYear: Int = 2022
  val taxYearMinusOne: Int = taxYear - 1
  val amount: BigDecimal = 500
  val ukDividendsAmountUrl = s"$startUrl/$taxYear/dividends/how-much-dividends-from-uk-companies"

  object IndividualExpected {
    val expectedH1 = "How much did you get in dividends from UK-based companies?"
    val expectedTitle = "How much did you get in dividends from UK-based companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val tellUsTheValue = "Tell us the value of the dividends you got, in pounds. You can find this information in your dividend voucher."
    val youToldUsPriorText = s"You told us you got £$amount in dividends from UK-based companies this year. Tell us if this has changed."
    val expectedErrorEmpty = "Enter how much you got in dividends from UK-based companies"
    val expectedErrorOverMax = "The amount of dividends from UK-based companies must be less than £100,000,000,000"
    val expectedErrorInvalid = "Enter how much you got in dividends in the correct format"

    val expectedH1Cy = "How much did you get in dividends from UK-based companies?"
    val expectedTitleCy = "How much did you get in dividends from UK-based companies?"
    val expectedErrorTitleCy = s"Error: $expectedTitleCy"
    val tellUsTheValueCy = "Tell us the value of the dividends you got, in pounds. You can find this information in your dividend voucher."
    val youToldUsPriorTextCy = s"You told us you got £$amount in dividends from UK-based companies this year. Tell us if this has changed."
    val expectedErrorEmptyCy = "Enter how much you got in dividends from UK-based companies"
    val expectedErrorOverMaxCy = "The amount of dividends from UK-based companies must be less than £100,000,000,000"
    val expectedErrorInvalidCy = "Enter how much you got in dividends in the correct format"

  }

  object AgentExpected {
    val expectedH1 = "How much did your client get in dividends from UK-based companies?"
    val expectedTitle= "How much did your client get in dividends from UK-based companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val tellUsTheValue = "Tell us the value of the dividends your client got, in pounds. You can find this information in their dividend voucher."
    val youToldUsPriorText = s"You told us your client got £$amount in dividends from UK-based companies this year. Tell us if this has changed."
    val expectedErrorEmpty = "Enter how much your client got in dividends from UK-based companies"
    val expectedErrorOverMax = "The amount of dividends from UK-based companies must be less than £100,000,000,000"
    val expectedErrorInvalid = "Enter how much your client got in dividends in the correct format"

    val expectedH1Cy = "How much did your client get in dividends from UK-based companies?"
    val expectedTitleCy = "How much did your client get in dividends from UK-based companies?"
    val expectedErrorTitleCy = s"Error: $expectedTitleCy"
    val tellUsTheValueCy = "Tell us the value of the dividends your client got, in pounds. You can find this information in their dividend voucher."
    val youToldUsPriorTextCy = s"You told us your client got £$amount in dividends from UK-based companies this year. Tell us if this has changed."
    val expectedErrorEmptyCy = "Enter how much your client got in dividends from UK-based companies"
    val expectedErrorOverMaxCy = "The amount of dividends from UK-based companies must be less than £100,000,000,000"
    val expectedErrorInvalidCy = "Enter how much your client got in dividends in the correct format"
  }

  val poundPrefixSelector = ".govuk-input__prefix"
  val captionSelector = ".govuk-caption-l"
  val inputSelector = ".govuk-input"
  val continueButtonSelector = "#continue"
  val continueButtonFormSelector = "#main-content > div > div > form"
  val youToldUsSelector = "#main-content > div > div > form > div > label > p"
  val expectedErrorLink = "#amount"

  val expectedCaption = s"Dividends for 6 April $taxYearMinusOne to 5 April $taxYear"
  val continueText = "Continue"
  val expectedCaptionCy = s"Dividends for 6 April $taxYearMinusOne to 5 April $taxYear"
  val continueTextCy = "Continue"
  val poundPrefixText = "£"
  val continueLink = s"/income-through-software/return/personal-income/$taxYear/dividends/how-much-dividends-from-uk-companies"

  val amountInputName = "amount"

  "as an individual" when {
  import IndividualExpected._
    ".show" should {

      "redirects user to overview page when there is no data in session" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          stubGet(s"/income-through-software/return/$taxYear/view", 200, "overview page content")
          await(wsClient.url(ukDividendsAmountUrl).get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
          result.body shouldBe "overview page content"
        }

      }

      "returns an action when cya data is in session with correct content" which {

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = None).asJsonString
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(ukDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedTitle)
        welshToggleCheck("English")
        h1Check( expectedH1 + " " + expectedCaption)
        textOnPageCheck(expectedCaption, captionSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        textOnPageCheck(tellUsTheValue, youToldUsSelector)
        inputFieldCheck(amountInputName, inputSelector)
        buttonCheck(continueText, continueButtonSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }
      "returns an action when cya data is in session with correct prior data content" which {

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount)).asJsonString
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(ukDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedTitle)
        welshToggleCheck("English")
        h1Check( expectedH1 + " " + expectedCaption)
        textOnPageCheck(expectedCaption, captionSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        textOnPageCheck(youToldUsPriorText, youToldUsSelector)
        inputFieldCheck(amountInputName, inputSelector)
        buttonCheck(continueText, continueButtonSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }
      "returns an action when cya data is in session with correct content - Welsh" which {

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = None).asJsonString
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(ukDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", "Csrf-Token" -> "nocheck").get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedTitleCy)
        welshToggleCheck("Welsh")
        h1Check( expectedH1Cy + " " + expectedCaptionCy)
        textOnPageCheck(expectedCaptionCy, captionSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        textOnPageCheck(tellUsTheValueCy, youToldUsSelector)
        inputFieldCheck(amountInputName, inputSelector)
        buttonCheck(continueTextCy, continueButtonSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }
      "returns an action when cya data is in session with correct prior data content - Welsh" which {

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount)).asJsonString
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(ukDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", "Csrf-Token" -> "nocheck").get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedTitleCy)
        welshToggleCheck("Welsh")
        h1Check( expectedH1Cy + " " + expectedCaptionCy)
        textOnPageCheck(expectedCaptionCy, captionSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        textOnPageCheck(youToldUsPriorTextCy, youToldUsSelector)
        inputFieldCheck(amountInputName, inputSelector)
        buttonCheck(continueTextCy, continueButtonSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }

      "redirects user to overview page when there is prior submission data and no cya data in session" which {

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.DIVIDENDS_PRIOR_SUB -> DividendsPriorSubmission(ukDividends = Some(amount)).asJsonString
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          stubGet(s"/income-through-software/return/$taxYear/view", 200, "overview page content")
          await(wsClient.url(ukDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
          result.body shouldBe "overview page content"
        }
      }

      "returns an action when there is prior submissions data and cya data in session and the amounts are different" which {

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount)).asJsonString,
          SessionValues.DIVIDENDS_PRIOR_SUB -> DividendsPriorSubmission(ukDividends = Some(1)).asJsonString
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(ukDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        inputFieldValueCheck(amount.toString(), "#amount")
      }

      "returns an action when there is priorSubmissionData and cyaData in session and the amounts are equal" which {
        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount)).asJsonString,
          SessionValues.DIVIDENDS_PRIOR_SUB -> DividendsPriorSubmission(ukDividends = Some(amount)).asJsonString
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(ukDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        inputFieldValueCheck("", "#amount")
      }

      "returns an action when there is cyaData but no priorSubmission data in session" which {
        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount)).asJsonString,
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(ukDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        inputFieldValueCheck(amount.toString(), "#amount")
      }


      "returns an action when the auth call fails" which {
        lazy val result: WSResponse = {
          authoriseIndividualUnauthorized()
          await(wsClient.url(ukDividendsAmountUrl).get())
        }

        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }

      }

    }

    ".submit" should {

      s"return an OK($OK) status" in {

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true)).asJsonString
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(
            wsClient.url(ukDividendsAmountUrl)
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map("amount" -> "123"))
          )
        }

        result.status shouldBe OK
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with an empty error" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(ukDividendsAmountUrl).post(Map[String, String]()))
        }

        "return the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)
        errorSummaryCheck(expectedErrorEmpty, expectedErrorLink)
        errorAboveElementCheck(expectedErrorEmpty)
      }
      s"return a BAD_REQUEST($BAD_REQUEST) status with an invalid error" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(ukDividendsAmountUrl).post(Map("amount" -> "|")))
        }

        "return the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)
        errorSummaryCheck(expectedErrorInvalid, expectedErrorLink)
        errorAboveElementCheck(expectedErrorInvalid)
      }
      s"return a BAD_REQUEST($BAD_REQUEST) status with an OverMax error" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(ukDividendsAmountUrl).post(Map("amount" -> "9999999999999999999999999")))
        }

        "return the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)
        errorSummaryCheck(expectedErrorOverMax, expectedErrorLink)
        errorAboveElementCheck(expectedErrorOverMax)
      }
      s"return a BAD_REQUEST($BAD_REQUEST) status with an empty error - Welsh" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(ukDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy")
            .post(Map[String, String]()))
        }

        "return the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)
        errorSummaryCheck(expectedErrorEmptyCy, expectedErrorLink)
        errorAboveElementCheck(expectedErrorEmptyCy)
      }
      s"return a BAD_REQUEST($BAD_REQUEST) status with an invalid error - Welsh" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(ukDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy")
            .post(Map("amount" -> "|")))
        }

        "return the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)
        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorInvalidCy, expectedErrorLink)
        errorAboveElementCheck(expectedErrorInvalidCy)
      }
      s"return a BAD_REQUEST($BAD_REQUEST) status with an OverMax error - Welsh" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(ukDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy")
            .post(Map("amount" -> "9999999999999999999999999")))
        }

        "return the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)
        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorOverMaxCy, expectedErrorLink)
        errorAboveElementCheck(expectedErrorOverMaxCy)
      }

      s"return an UNAUTHORIZED($UNAUTHORIZED) status" in {
        lazy val result: WSResponse = {
          authoriseIndividualUnauthorized()
          await(wsClient.url(ukDividendsAmountUrl).post(Map[String, String]()))
        }

        result.status shouldBe UNAUTHORIZED
      }

    }

  }

  "as an agent" when {
  import AgentExpected._
    ".show" should {

      "returns an action" which {
        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(ukDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie)
            .get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }
      }
      "returns an action with correct content" which {
        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = None).asJsonString
          ))

          authoriseAgent()
          await(wsClient.url(ukDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie)
            .get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedTitle)
        welshToggleCheck("English")
        h1Check( expectedH1 + " " + expectedCaption)
        textOnPageCheck(expectedCaption, captionSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        textOnPageCheck(tellUsTheValue, youToldUsSelector)
        inputFieldCheck(amountInputName, inputSelector)
        buttonCheck(continueText, continueButtonSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }
      "returns an action with correct prior data content" which {
        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount)).asJsonString
          ))

          authoriseAgent()
          await(wsClient.url(ukDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie)
            .get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedTitle)
        welshToggleCheck("English")
        h1Check( expectedH1 + " " + expectedCaption)
        textOnPageCheck(expectedCaption, captionSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        textOnPageCheck(youToldUsPriorText, youToldUsSelector)
        inputFieldCheck(amountInputName, inputSelector)
        buttonCheck(continueText, continueButtonSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }
      "returns an action with correct content - Welsh" which {
        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = None).asJsonString
          ))

          authoriseAgent()
          await(wsClient.url(ukDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy")
            .get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedTitleCy)
        welshToggleCheck("Welsh")
        h1Check( expectedH1Cy + " " + expectedCaptionCy)
        textOnPageCheck(expectedCaptionCy, captionSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        textOnPageCheck(tellUsTheValueCy, youToldUsSelector)
        inputFieldCheck(amountInputName, inputSelector)
        buttonCheck(continueTextCy, continueButtonSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }
      "returns an action with correct prior data content - Welsh" which {
        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount)).asJsonString
          ))

          authoriseAgent()
          await(wsClient.url(ukDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy")
            .get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedTitleCy)
        welshToggleCheck("Welsh")
        h1Check( expectedH1Cy + " " + expectedCaptionCy)
        textOnPageCheck(expectedCaptionCy, captionSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        textOnPageCheck(youToldUsPriorTextCy, youToldUsSelector)
        inputFieldCheck(amountInputName, inputSelector)
        buttonCheck(continueTextCy, continueButtonSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }

      "returns an action when auth call fails" which {
        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgentUnauthorized()
          await(wsClient.url(ukDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie)
            .get())
        }

        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }
    }

    ".submit" should {

      s"return an OK($OK) status" when {

        "there is form data" in {
          lazy val result: WSResponse = {
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A",
              SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true)).asJsonString
            ))

            authoriseAgent()
            await(
              wsClient.url(ukDividendsAmountUrl)
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
                .post(Map("amount" -> "123"))
            )
          }

          result.status shouldBe OK
        }
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with an empty error" when {

          lazy val result: WSResponse = {
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A",
              SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true)).asJsonString
            ))

            authoriseAgent()
            await(wsClient.url(ukDividendsAmountUrl)
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map[String, String]()))
          }

        "return the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)
        errorSummaryCheck(expectedErrorEmpty, expectedErrorLink)
        errorAboveElementCheck(expectedErrorEmpty)
      }
      s"return a BAD_REQUEST($BAD_REQUEST) status with an invalid error" when {

          lazy val result: WSResponse = {
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A",
              SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true)).asJsonString
            ))

            authoriseAgent()
            await(wsClient.url(ukDividendsAmountUrl)
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map("amount" -> "|")))
          }

        "return the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)
        errorSummaryCheck(expectedErrorInvalid, expectedErrorLink)
        errorAboveElementCheck(expectedErrorInvalid)
      }
      s"return a BAD_REQUEST($BAD_REQUEST) status with an overmax error" when {

          lazy val result: WSResponse = {
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A",
              SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true)).asJsonString
            ))

            authoriseAgent()
            await(wsClient.url(ukDividendsAmountUrl)
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map("amount" -> "999999999999999999999999999999999999")))
          }

        "return the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)
        errorSummaryCheck(expectedErrorOverMax, expectedErrorLink)
        errorAboveElementCheck(expectedErrorOverMax)
      }
      s"return a BAD_REQUEST($BAD_REQUEST) status with an empty error - Welsh" when {

          lazy val result: WSResponse = {
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A",
              SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true)).asJsonString
            ))

            authoriseAgent()
            await(wsClient.url(ukDividendsAmountUrl)
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", "Csrf-Token" -> "nocheck")
              .post(Map[String, String]()))
          }

        "return the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)
        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorEmptyCy, expectedErrorLink)
        errorAboveElementCheck(expectedErrorEmptyCy)
      }
      s"return a BAD_REQUEST($BAD_REQUEST) status with an invalid error - Welsh" when {

          lazy val result: WSResponse = {
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A",
              SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true)).asJsonString
            ))

            authoriseAgent()
            await(wsClient.url(ukDividendsAmountUrl)
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", "Csrf-Token" -> "nocheck")
              .post(Map("amount" -> "|")))
          }

        "return the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)
        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorInvalidCy, expectedErrorLink)
        errorAboveElementCheck(expectedErrorInvalidCy)
      }
      s"return a BAD_REQUEST($BAD_REQUEST) status with an overMax error - Welsh" when {

          lazy val result: WSResponse = {
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A",
              SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true)).asJsonString
            ))

            authoriseAgent()
            await(wsClient.url(ukDividendsAmountUrl)
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", "Csrf-Token" -> "nocheck")
              .post(Map("amount" -> "999999999999999999999999999999999999")))
          }

        "return the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)
        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorOverMaxCy, expectedErrorLink)
        errorAboveElementCheck(expectedErrorOverMaxCy)
      }

      s"return an UNAUTHORIZED($UNAUTHORIZED) status" in {
        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgentUnauthorized()
          await(wsClient.url(ukDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post(Map[String, String]()))
        }

        result.status shouldBe UNAUTHORIZED
      }

    }

  }

}

