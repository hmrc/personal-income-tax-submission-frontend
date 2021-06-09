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
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{IntegrationTest, ViewHelpers}

class UkDividendsAmountControllerISpec extends IntegrationTest with ViewHelpers {


  val taxYear: Int = 2022
  val taxYearMinusOne: Int = taxYear - 1
  val amount: BigDecimal = 500
  val ukDividendsAmountUrl = s"$startUrl/$taxYear/dividends/how-much-dividends-from-uk-companies"

  val cyaModel = DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = None)
  val cyaModelWithAmount = DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount))

  object ExpectedResults {

    object EnglishLang {

      object IndividualExpected {
        val expectedH1 = "How much did you get in dividends from UK-based companies?"
        val expectedTitle = "How much did you get in dividends from UK-based companies?"
        val expectedErrorTitle = s"Error: $expectedTitle"
        val tellUsTheValue = "Tell us the value of the dividends you got, in pounds. You can find this information in your dividend voucher."
        val youToldUsPriorText = s"You told us you got £$amount in dividends from UK-based companies this year. Tell us if this has changed."
        val expectedErrorEmpty = "Enter how much you got in dividends from UK-based companies"
        val expectedErrorOverMax = "The amount of dividends from UK-based companies must be less than £100,000,000,000"
        val expectedErrorInvalid = "Enter how much you got in dividends in the correct format"
      }

      object AgentExpected {
        val expectedH1 = "How much did your client get in dividends from UK-based companies?"
        val expectedTitle = "How much did your client get in dividends from UK-based companies?"
        val expectedErrorTitle = s"Error: $expectedTitle"
        val tellUsTheValue = "Tell us the value of the dividends your client got, in pounds. You can find this information in their dividend voucher."
        val youToldUsPriorText = s"You told us your client got £$amount in dividends from UK-based companies this year. Tell us if this has changed."
        val expectedErrorEmpty = "Enter how much your client got in dividends from UK-based companies"
        val expectedErrorOverMax = "The amount of dividends from UK-based companies must be less than £100,000,000,000"
        val expectedErrorInvalid = "Enter how much your client got in dividends in the correct format"

      }
    }

    object WelshLang {

      object IndividualExpected {
        val expectedH1 = "How much did you get in dividends from UK-based companies?"
        val expectedTitle = "How much did you get in dividends from UK-based companies?"
        val expectedErrorTitle = s"Error: $expectedTitle"
        val tellUsTheValue = "Tell us the value of the dividends you got, in pounds. You can find this information in your dividend voucher."
        val youToldUsPriorText = s"You told us you got £$amount in dividends from UK-based companies this year. Tell us if this has changed."
        val expectedErrorEmpty = "Enter how much you got in dividends from UK-based companies"
        val expectedErrorOverMax = "The amount of dividends from UK-based companies must be less than £100,000,000,000"
        val expectedErrorInvalid = "Enter how much you got in dividends in the correct format"
      }

      object AgentExpected {
        val expectedH1 = "How much did your client get in dividends from UK-based companies?"
        val expectedTitle = "How much did your client get in dividends from UK-based companies?"
        val expectedErrorTitle = s"Error: $expectedTitle"
        val tellUsTheValue = "Tell us the value of the dividends your client got, in pounds. You can find this information in their dividend voucher."
        val youToldUsPriorText = s"You told us your client got £$amount in dividends from UK-based companies this year. Tell us if this has changed."
        val expectedErrorEmpty = "Enter how much your client got in dividends from UK-based companies"
        val expectedErrorOverMax = "The amount of dividends from UK-based companies must be less than £100,000,000,000"
        val expectedErrorInvalid = "Enter how much your client got in dividends in the correct format"

      }
    }
  }

  object Selectors {

    val poundPrefixSelector = ".govuk-input__prefix"
    val captionSelector = ".govuk-caption-l"
    val inputSelector = ".govuk-input"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val youToldUsSelector = "#main-content > div > div > form > div > label > p"
    val expectedErrorLink = "#amount"
  }

  val expectedCaption = s"Dividends for 6 April $taxYearMinusOne to 5 April $taxYear"
  val continueText = "Continue"
  val expectedCaptionCy = s"Dividends for 6 April $taxYearMinusOne to 5 April $taxYear"
  val continueTextCy = "Continue"
  val poundPrefixText = "£"
  val continueLink = s"/income-through-software/return/personal-income/$taxYear/dividends/how-much-dividends-from-uk-companies"

  val amountInputName = "amount"

  ".show" should {
    import Selectors._

    "in English" should {

      import ExpectedResults.EnglishLang._

      "as an individual" when {

        "redirects user to overview page when there is no data in session" which {
          lazy val result: WSResponse = {
            authoriseIndividual()
            stubGet(s"/income-through-software/return/$taxYear/view", 200, "overview page content")
            urlGet(ukDividendsAmountUrl)
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
            result.body shouldBe "overview page content"
          }

        }

        "returns uk dividends amount page with empty amount field" which {

          lazy val result: WSResponse = {
            authoriseIndividual()
            urlGet(ukDividendsAmountUrl, headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(cyaModel)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(IndividualExpected.expectedTitle)
          welshToggleCheck("English")
          h1Check(IndividualExpected.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(IndividualExpected.tellUsTheValue, youToldUsSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
        }

        "returns uk dividends amount page with pre-filled amount" which {

          lazy val result: WSResponse = {
            authoriseIndividual()
            urlGet(ukDividendsAmountUrl, headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(cyaModelWithAmount)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(IndividualExpected.expectedTitle)
          welshToggleCheck("English")
          h1Check(IndividualExpected.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(IndividualExpected.youToldUsPriorText, youToldUsSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
        }
      }

      "as an agent" when {

        "returns an action with correct content" which {
          lazy val result: WSResponse = {
            authoriseAgent()
            urlGet(ukDividendsAmountUrl, headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(cyaModel)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(AgentExpected.expectedTitle)
          welshToggleCheck("English")
          h1Check(AgentExpected.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(AgentExpected.tellUsTheValue, youToldUsSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
        }
        "returns an action with correct prior data content" which {
          lazy val result: WSResponse = {

            authoriseAgent()
            urlGet(ukDividendsAmountUrl, headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(cyaModelWithAmount)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(AgentExpected.expectedTitle)
          welshToggleCheck("English")
          h1Check(AgentExpected.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(AgentExpected.youToldUsPriorText, youToldUsSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
        }
      }
    }

    "in Welsh" should {

      import ExpectedResults.WelshLang._

      "as an individual" when {

        "redirects user to overview page when there is no data in session" which {
          lazy val result: WSResponse = {
            authoriseIndividual()
            stubGet(s"/income-through-software/return/$taxYear/view", 200, "overview page content")
            urlGet(ukDividendsAmountUrl, welsh = true)
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
            result.body shouldBe "overview page content"
          }

        }

        "returns uk dividends amount page with empty amount field" which {

          lazy val result: WSResponse = {
            authoriseIndividual()
            urlGet(ukDividendsAmountUrl, welsh = true,
              headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(cyaModel)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(IndividualExpected.expectedTitle)
          welshToggleCheck(WELSH)
          h1Check(IndividualExpected.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(IndividualExpected.tellUsTheValue, youToldUsSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
        }

        "returns uk dividends amount page with pre-filled amount" which {

          lazy val result: WSResponse = {
            authoriseIndividual()
            urlGet(ukDividendsAmountUrl, welsh = true,
              headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(cyaModelWithAmount)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(IndividualExpected.expectedTitle)
          welshToggleCheck(WELSH)
          h1Check(IndividualExpected.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(IndividualExpected.youToldUsPriorText, youToldUsSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
        }
      }

      "as an agent" when {

        "returns an action with correct content" which {
          lazy val result: WSResponse = {
            authoriseAgent()
            urlGet(ukDividendsAmountUrl, welsh = true,
              headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(cyaModel)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(AgentExpected.expectedTitle)
          welshToggleCheck(WELSH)
          h1Check(AgentExpected.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(AgentExpected.tellUsTheValue, youToldUsSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
        }
        "returns an action with correct prior data content" which {
          lazy val result: WSResponse = {

            authoriseAgent()
            urlGet(ukDividendsAmountUrl, welsh = true,
              headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(cyaModelWithAmount)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(AgentExpected.expectedTitle)
          welshToggleCheck(WELSH)
          h1Check(AgentExpected.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(AgentExpected.youToldUsPriorText, youToldUsSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
        }
      }
    }

  }


  ".submit" should {

    import Selectors._

    "in English" should {

      import ExpectedResults.EnglishLang._

      "as an Individual" when {


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
          errorSummaryCheck(IndividualExpected.expectedErrorEmpty, expectedErrorLink)
          errorAboveElementCheck(IndividualExpected.expectedErrorEmpty)
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
          errorSummaryCheck(IndividualExpected.expectedErrorInvalid, expectedErrorLink)
          errorAboveElementCheck(IndividualExpected.expectedErrorInvalid)
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
          errorSummaryCheck(IndividualExpected.expectedErrorOverMax, expectedErrorLink)
          errorAboveElementCheck(IndividualExpected.expectedErrorOverMax)
        }

      }

      "as an agent" when {
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
        errorSummaryCheck(AgentExpected.expectedErrorEmpty, expectedErrorLink)
        errorAboveElementCheck(AgentExpected.expectedErrorEmpty)
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
        errorSummaryCheck(AgentExpected.expectedErrorInvalid, expectedErrorLink)
        errorAboveElementCheck(AgentExpected.expectedErrorInvalid)
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
        errorSummaryCheck(AgentExpected.expectedErrorOverMax, expectedErrorLink)
        errorAboveElementCheck(AgentExpected.expectedErrorOverMax)
      }

    }

    "in Welsh" should {

      import ExpectedResults.EnglishLang._

      "as an Individual" when {


        s"return an OK($OK) status" in {

          val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true)).asJsonString
          ))

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(
              wsClient.url(ukDividendsAmountUrl)
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", "Csrf-Token" -> "nocheck")
                .post(Map("amount" -> "123"))
            )
          }

          result.status shouldBe OK
        }

        s"return a BAD_REQUEST($BAD_REQUEST) status with an empty error" which {
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(ukDividendsAmountUrl)
              .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy", "Csrf-Token" -> "nocheck")
              .post(Map[String, String]()))
          }

          "return the correct status" in {
            result.status shouldBe BAD_REQUEST
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)
          errorSummaryCheck(IndividualExpected.expectedErrorEmpty, expectedErrorLink)
          errorAboveElementCheck(IndividualExpected.expectedErrorEmpty)
        }
        s"return a BAD_REQUEST($BAD_REQUEST) status with an invalid error" which {
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(ukDividendsAmountUrl)
              .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy", "Csrf-Token" -> "nocheck")
              .post(Map("amount" -> "|")))
          }

          "return the correct status" in {
            result.status shouldBe BAD_REQUEST
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)
          errorSummaryCheck(IndividualExpected.expectedErrorInvalid, expectedErrorLink)
          errorAboveElementCheck(IndividualExpected.expectedErrorInvalid)
        }
        s"return a BAD_REQUEST($BAD_REQUEST) status with an OverMax error" which {
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(ukDividendsAmountUrl)
              .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy", "Csrf-Token" -> "nocheck")
              .post(Map("amount" -> "9999999999999999999999999")))
          }

          "return the correct status" in {
            result.status shouldBe BAD_REQUEST
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)
          errorSummaryCheck(IndividualExpected.expectedErrorOverMax, expectedErrorLink)
          errorAboveElementCheck(IndividualExpected.expectedErrorOverMax)
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
          errorSummaryCheck(IndividualExpected.expectedErrorEmpty, expectedErrorLink)
          errorAboveElementCheck(IndividualExpected.expectedErrorEmpty)
        }
      }

      "as an agent" when {
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
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", "Csrf-Token" -> "nocheck")
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
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", "Csrf-Token" -> "nocheck")
            .post(Map[String, String]()))
        }

        "return the correct status" in {
          result.status shouldBe BAD_REQUEST
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)
        errorSummaryCheck(AgentExpected.expectedErrorEmpty, expectedErrorLink)
        errorAboveElementCheck(AgentExpected.expectedErrorEmpty)
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
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", "Csrf-Token" -> "nocheck")
            .post(Map("amount" -> "|")))
        }

        "return the correct status" in {
          result.status shouldBe BAD_REQUEST
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)
        errorSummaryCheck(AgentExpected.expectedErrorInvalid, expectedErrorLink)
        errorAboveElementCheck(AgentExpected.expectedErrorInvalid)
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
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", "Csrf-Token" -> "nocheck")
            .post(Map("amount" -> "999999999999999999999999999999999999")))
        }

        "return the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)
        errorSummaryCheck(AgentExpected.expectedErrorOverMax, expectedErrorLink)
        errorAboveElementCheck(AgentExpected.expectedErrorOverMax)
      }
    }

  }
}


