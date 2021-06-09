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

class OtherUkDividendsAmountControllerISpec extends IntegrationTest with ViewHelpers {


  val taxYear: Int = 2022
  val taxYearMinusOne: Int = taxYear - 1
  val amount: BigDecimal = 500
  val otherUkDividendsAmountUrl = s"$startUrl/$taxYear/dividends/how-much-dividends-from-uk-trusts-and-open-ended-investment-companies"

  val validCyaModel = DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = None)
  val validCyaModelWithAmount = DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount))

  val poundPrefixText = "£"

  object ExpectedResults {

    object EnglishLang {

      object IndividualExpected {
        val expectedH1 = "How much did you get in dividends from trusts and open-ended investment companies based in the UK?"
        val expectedTitle = "How much did you get in dividends from trusts and open-ended investment companies based in the UK?"
        val expectedErrorTitle = s"Error: $expectedTitle"
        val tellUsTheValue = "Tell us the value of the dividends you got, in pounds. You can find this information in your dividend voucher."
        val youToldUsPriorText = s"You told us you got £$amount in dividends from UK-based trusts and open-ended investment companies this year. Tell us if this has changed."
        val expectedErrorEmpty = "Enter how much you got in dividends from trusts and open-ended investment companies"
        val expectedErrorInvalid = "Enter how much you got in dividends in the correct format"
        val expectedErrorOverMax = "The amount of dividends from trusts and open-ended investment companies based in the UK must be less than £100,000,000,000"

      }

      object AgentExpected {
        val expectedH1 = "How much did your client get in dividends from trusts and open-ended investment companies based in the UK?"
        val expectedTitle = "How much did your client get in dividends from trusts and open-ended investment companies based in the UK?"
        val expectedErrorTitleAgent = s"Error: $expectedTitle"
        val tellUsTheValue = "Tell us the value of the dividends your client got, in pounds. You can find this information in their dividend voucher."
        val youToldUsPriorText = s"You told us your client got £$amount in dividends from UK-based trusts and open-ended investment companies this year. Tell us if this has changed."
        val expectedErrorEmpty = "Enter how much your client got in dividends from trusts and open-ended investment companies"
        val expectedErrorInvalid = "Enter how much your client got in dividends in the correct format"
        val expectedErrorOverMax = "The amount of dividends from trusts and open-ended investment companies based in the UK must be less than £100,000,000,000"

      }

      object AllExpected {
        val expectedCaption = s"Dividends for 6 April $taxYearMinusOne to 5 April $taxYear"
        val expectedHintText = "For example, £600 or £193.54"
        val continueText = "Continue"
      }
    }

    object WeslhLang {

      object IndividualExpected {
        val expectedH1 = "How much did you get in dividends from trusts and open-ended investment companies based in the UK?"
        val expectedTitle = "How much did you get in dividends from trusts and open-ended investment companies based in the UK?"
        val expectedErrorTitle = s"Error: $expectedTitle"
        val tellUsTheValue = "Tell us the value of the dividends you got, in pounds. You can find this information in your dividend voucher."
        val youToldUsPriorText = s"You told us you got £$amount in dividends from UK-based trusts and open-ended investment companies this year. Tell us if this has changed."
        val expectedErrorEmpty = "Enter how much you got in dividends from trusts and open-ended investment companies"
        val expectedErrorInvalid = "Enter how much you got in dividends in the correct format"
        val expectedErrorOverMax = "The amount of dividends from trusts and open-ended investment companies based in the UK must be less than £100,000,000,000"
      }

      object AgentExpected {
        val expectedH1 = "How much did your client get in dividends from trusts and open-ended investment companies based in the UK?"
        val expectedTitle = "How much did your client get in dividends from trusts and open-ended investment companies based in the UK?"
        val expectedErrorTitleAgent = s"Error: $expectedTitle"
        val tellUsTheValue = "Tell us the value of the dividends your client got, in pounds. You can find this information in their dividend voucher."
        val youToldUsPriorText = s"You told us your client got £$amount in dividends from UK-based trusts and open-ended investment companies this year. Tell us if this has changed."
        val expectedErrorEmpty = "Enter how much your client got in dividends from trusts and open-ended investment companies"
        val expectedErrorInvalid = "Enter how much your client got in dividends in the correct format"
        val expectedErrorOverMax = "The amount of dividends from trusts and open-ended investment companies based in the UK must be less than £100,000,000,000"
      }

      object AllExpected {
        val expectedCaption = s"Dividends for 6 April $taxYearMinusOne to 5 April $taxYear"
        val expectedHintText = "For example, £600 or £193.54"
        val continueText = "Continue"
      }
    }

  }

  object Selectors {

    val poundPrefixSelector = ".govuk-input__prefix"
    val captionSelector = ".govuk-caption-l"
    val inputSelector = ".govuk-input"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val enterAmountSelector = "#amount"
    val youToldUsSelector = "#main-content > div > div > form > div > label > p"
    val tellUsTheValueSelector = "#main-content > div > div > form > div > label > p"
    val amountSelector = "#amount"
  }


  val continueLink = s"/income-through-software/return/personal-income/$taxYear/dividends/how-much-dividends-from-uk-trusts-and-open-ended-investment-companies"
  val newAmountInput = "#amount"
  val amountInputName = "amount"
  val expectedErrorLink = "#amount"


  ".show" when {
    import Selectors._
    ".in English" should {

      import ExpectedResults.EnglishLang._
      import ExpectedResults.EnglishLang.AllExpected._

      "as an Individual" should {

        "redirects user to overview page when there is no data in session" which {
          lazy val result: WSResponse = {
            authoriseIndividual()
            stubGet(s"/income-through-software/return/$taxYear/view", 200, "overview page content")
            urlGet(otherUkDividendsAmountUrl)
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
            result.body shouldBe "overview page content"
          }
        }

        "returns other uk dividends amount page with amount field empty" which {


          lazy val result: WSResponse = {
            authoriseIndividual()
            urlGet(otherUkDividendsAmountUrl, headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(validCyaModel)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(IndividualExpected.expectedTitle)
          welshToggleCheck(ENGLISH)
          h1Check(IndividualExpected.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(IndividualExpected.tellUsTheValue, tellUsTheValueSelector)
          hintTextCheck(expectedHintText)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
        }

        "returns other uk dividends amount page with amount field pre-filled" which {

          lazy val result: WSResponse = {
            authoriseIndividual()
            urlGet(otherUkDividendsAmountUrl, headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(validCyaModelWithAmount)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(IndividualExpected.expectedTitle)
          welshToggleCheck(ENGLISH)
          h1Check(IndividualExpected.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(IndividualExpected.youToldUsPriorText, youToldUsSelector)
          hintTextCheck(expectedHintText)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          inputFieldValueCheck(amount.toString(), amountSelector)

        }

        "returns other uk dividends amount page with cya amount pre-filled even if there is prior submission" which {

          val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount)).asJsonString,
            SessionValues.DIVIDENDS_PRIOR_SUB -> DividendsPriorSubmission(otherUkDividends = Some(1)).asJsonString
          ))

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(otherUkDividendsAmountUrl)
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          inputFieldValueCheck(amount.toString(), "#amount")
        }

        "returns other uk dividends with empty amount field when priorSubmissionData and cyaData amounts are equal" which {
          val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount)).asJsonString,
            SessionValues.DIVIDENDS_PRIOR_SUB -> DividendsPriorSubmission(otherUkDividends = Some(amount)).asJsonString
          ))

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(otherUkDividendsAmountUrl)
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          inputFieldValueCheck("", "#amount")
        }

        "return unauthorized when the authorization fails" which {

          lazy val result: WSResponse = {
            authoriseIndividualUnauthorized()
            urlGet(otherUkDividendsAmountUrl, headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(validCyaModelWithAmount)))
          }

          s"has an UNAUTHORIZED($UNAUTHORIZED) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }
      }

      "as an Agent" should {

        "returns other uk dividends amount page with amount field empty" which {
          lazy val result: WSResponse = {
            authoriseAgent()
            urlGet(otherUkDividendsAmountUrl, headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(validCyaModel)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(AgentExpected.expectedTitle)
          welshToggleCheck(ENGLISH)
          h1Check(AgentExpected.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(AgentExpected.tellUsTheValue, tellUsTheValueSelector)
          hintTextCheck(expectedHintText)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
        }

        "returns other uk dividends amount page with amount field pre-filled" which {
          lazy val result: WSResponse = {
            authoriseAgent()
            urlGet(otherUkDividendsAmountUrl, headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(validCyaModelWithAmount)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(AgentExpected.expectedTitle)
          welshToggleCheck(ENGLISH)
          h1Check(AgentExpected.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(AgentExpected.youToldUsPriorText, youToldUsSelector)
          hintTextCheck(expectedHintText)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
        }

        "return unauthorized when the authorization fails" which {

          lazy val result: WSResponse = {
            authoriseAgentUnauthorized()
            urlGet(otherUkDividendsAmountUrl, headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(validCyaModel)))
          }

          s"has an UNAUTHORIZED($UNAUTHORIZED) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }
      }
    }

    "in welsh" when {

      import ExpectedResults.WeslhLang._
      import ExpectedResults.WeslhLang.AllExpected._

      "as an Individual" should {

        "redirects user to overview page when there is no data in session" which {
          lazy val result: WSResponse = {
            authoriseIndividual()
            stubGet(s"/income-through-software/return/$taxYear/view", 200, "overview page content")
            urlGet(otherUkDividendsAmountUrl, welsh = true)
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
            result.body shouldBe "overview page content"
          }
        }

        "returns other uk dividends amount page with amount field empty" which {


          lazy val result: WSResponse = {
            authoriseIndividual()
            urlGet(otherUkDividendsAmountUrl, welsh = true,
              headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(validCyaModel)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(IndividualExpected.expectedTitle)
          welshToggleCheck(WELSH)
          h1Check(IndividualExpected.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(IndividualExpected.tellUsTheValue, tellUsTheValueSelector)
          hintTextCheck(expectedHintText)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
        }

        "returns other uk dividends amount page with amount field pre-filled" which {

          lazy val result: WSResponse = {
            authoriseIndividual()
            urlGet(otherUkDividendsAmountUrl, welsh = true,
              headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(validCyaModelWithAmount)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(IndividualExpected.expectedTitle)
          welshToggleCheck(WELSH)
          h1Check(IndividualExpected.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(IndividualExpected.youToldUsPriorText, youToldUsSelector)
          hintTextCheck(expectedHintText)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          inputFieldValueCheck(amount.toString(), amountSelector)

        }

        "returns other uk dividends amount page with cya amount pre-filled even if there is prior submission" which {

          val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount)).asJsonString,
            SessionValues.DIVIDENDS_PRIOR_SUB -> DividendsPriorSubmission(otherUkDividends = Some(1)).asJsonString
          ))

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(otherUkDividendsAmountUrl)
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", "Csrf-Token" -> "nocheck").get())
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          inputFieldValueCheck(amount.toString(), "#amount")
        }

        "returns other uk dividends with empty amount field when priorSubmissionData and cyaData amounts are equal" which {
          val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount)).asJsonString,
            SessionValues.DIVIDENDS_PRIOR_SUB -> DividendsPriorSubmission(otherUkDividends = Some(amount)).asJsonString
          ))

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(otherUkDividendsAmountUrl)
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", "Csrf-Token" -> "nocheck").get())
          }


          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          inputFieldValueCheck("", "#amount")
        }

        "return unauthorized when the authorization fails" which {

          lazy val result: WSResponse = {
            authoriseIndividualUnauthorized()
            urlGet(otherUkDividendsAmountUrl, welsh=true,
              headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(validCyaModelWithAmount)))
          }

          s"has an UNAUTHORIZED($UNAUTHORIZED) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }
      }

      "as an Agent" should {

        "returns other uk dividends amount page with amount field empty" which {
          lazy val result: WSResponse = {
            authoriseAgent()
            urlGet(otherUkDividendsAmountUrl, welsh =true,
              headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(validCyaModel)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(AgentExpected.expectedTitle)
          welshToggleCheck(WELSH)
          h1Check(AgentExpected.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(AgentExpected.tellUsTheValue, tellUsTheValueSelector)
          hintTextCheck(expectedHintText)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
        }

        "returns other uk dividends amount page with amount field pre-filled" which {
          lazy val result: WSResponse = {
            authoriseAgent()
            urlGet(otherUkDividendsAmountUrl, welsh=true,
              headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(validCyaModelWithAmount)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(AgentExpected.expectedTitle)
          welshToggleCheck(WELSH)
          h1Check(AgentExpected.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(AgentExpected.youToldUsPriorText, youToldUsSelector)
          hintTextCheck(expectedHintText)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
        }

        "return unauthorized when the authorization fails" which {

          lazy val result: WSResponse = {
            authoriseAgentUnauthorized()
            urlGet(otherUkDividendsAmountUrl, welsh =true,
              headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(validCyaModel)))
          }

          s"has an UNAUTHORIZED($UNAUTHORIZED) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }
      }
    }
  }

  ".submit" should {

    "in English" should {

      import ExpectedResults.EnglishLang.IndividualExpected._

      "as an Individual" should {

        s"return an OK($OK) status" in {
          val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(otherUkDividends = Some(true)).asJsonString
          ))

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(
              wsClient.url(otherUkDividendsAmountUrl)
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
                .post(Map("amount" -> "123"))
            )
          }

          result.status shouldBe OK
        }

        s"return a BAD_REQUEST($BAD_REQUEST) status with an Empty Error" which {
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(otherUkDividendsAmountUrl).post(Map[String, String]()))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)
          errorSummaryCheck(expectedErrorEmpty, expectedErrorLink)
          errorAboveElementCheck(expectedErrorEmpty)
        }
        s"return a BAD_REQUEST($BAD_REQUEST) status with an Invalid Error" which {
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(otherUkDividendsAmountUrl).post(Map("amount" -> "|")))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)
          errorSummaryCheck(expectedErrorInvalid, expectedErrorLink)
          errorAboveElementCheck(expectedErrorInvalid)
        }
        s"return a BAD_REQUEST($BAD_REQUEST) status with an OverMax Error" which {
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(otherUkDividendsAmountUrl).post(Map("amount" -> "9999999999999999999999999999")))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)
          errorSummaryCheck(expectedErrorOverMax, expectedErrorLink)
          errorAboveElementCheck(expectedErrorOverMax)
        }
      }

        "as an agent" when {

          import ExpectedResults.EnglishLang.AgentExpected._

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
                  wsClient.url(otherUkDividendsAmountUrl)
                    .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
                    .post(Map("amount" -> "123"))
                )
              }

              result.status shouldBe OK
            }
          }

          s"return a BAD_REQUEST($BAD_REQUEST) status with an Empty Error" which {

            lazy val result: WSResponse = {
              lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
                SessionValues.CLIENT_MTDITID -> "1234567890",
                SessionValues.CLIENT_NINO -> "AA123456A"
              ))

              authoriseAgent()
              await(wsClient.url(otherUkDividendsAmountUrl)
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
                .post(Map[String, String]()))
            }

            "have the correct status" in {
              result.status shouldBe BAD_REQUEST
            }
            implicit val document: () => Document = () => Jsoup.parse(result.body)
            errorSummaryCheck(expectedErrorEmpty, expectedErrorLink)
            errorAboveElementCheck(expectedErrorEmpty)

          }
          s"return a BAD_REQUEST($BAD_REQUEST) status with an Inavlid Error" which {

            lazy val result: WSResponse = {
              lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
                SessionValues.CLIENT_MTDITID -> "1234567890",
                SessionValues.CLIENT_NINO -> "AA123456A"
              ))

              authoriseAgent()
              await(wsClient.url(otherUkDividendsAmountUrl)
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
                .post(Map("amount" -> "|")))
            }

            "have the correct status" in {
              result.status shouldBe BAD_REQUEST
            }
            implicit val document: () => Document = () => Jsoup.parse(result.body)
            errorSummaryCheck(expectedErrorInvalid, expectedErrorLink)
            errorAboveElementCheck(expectedErrorInvalid)

          }
          s"return a BAD_REQUEST($BAD_REQUEST) status with an OverMax Error" which {

            lazy val result: WSResponse = {
              lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
                SessionValues.CLIENT_MTDITID -> "1234567890",
                SessionValues.CLIENT_NINO -> "AA123456A"
              ))

              authoriseAgent()
              await(wsClient.url(otherUkDividendsAmountUrl)
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
                .post(Map("amount" -> "999999999999999999999999999999999")))
            }

            "have the correct status" in {
              result.status shouldBe BAD_REQUEST
            }
            implicit val document: () => Document = () => Jsoup.parse(result.body)
            errorSummaryCheck(expectedErrorOverMax, expectedErrorLink)
            errorAboveElementCheck(expectedErrorOverMax)

          }

        }
      }


      "in Welsh" should {

        import ExpectedResults.WeslhLang.IndividualExpected._

        s"return a BAD_REQUEST($BAD_REQUEST) status with an Empty Error - Welsh" which {
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(otherUkDividendsAmountUrl)
              .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy")
              .post(Map[String, String]()))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)
          welshToggleCheck(WELSH)
          errorSummaryCheck(expectedErrorEmpty, expectedErrorLink)
          errorAboveElementCheck(expectedErrorEmpty)
        }

        s"return a BAD_REQUEST($BAD_REQUEST) status with an Invalid Error - Welsh" which {
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(otherUkDividendsAmountUrl)
              .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy")
              .post(Map("amount" -> "|")))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)
          welshToggleCheck(WELSH)
          errorSummaryCheck(expectedErrorInvalid, expectedErrorLink)
          errorAboveElementCheck(expectedErrorInvalid)
        }

        s"return a BAD_REQUEST($BAD_REQUEST) status with an OverMax Error - Welsh" which {
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(otherUkDividendsAmountUrl)
              .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy")
              .post(Map("amount" -> "9999999999999999999999999999")))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)
          welshToggleCheck(WELSH)
          errorSummaryCheck(expectedErrorOverMax, expectedErrorLink)
          errorAboveElementCheck(expectedErrorOverMax)
        }

        "return unauthorized when the authorization fails" which {
          lazy val result: WSResponse = {
            authoriseIndividualUnauthorized()
            await(wsClient.url(otherUkDividendsAmountUrl)
              .post(Map[String, String]()))
          }

          s"has an UNAUTHORIZED($UNAUTHORIZED) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }

        "as an Agent" should{

          import ExpectedResults.WeslhLang.AgentExpected._
          s"return a BAD_REQUEST($BAD_REQUEST) status with an OverMax Error - Welsh" which {

            lazy val result: WSResponse = {
              lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
                SessionValues.CLIENT_MTDITID -> "1234567890",
                SessionValues.CLIENT_NINO -> "AA123456A"
              ))

              authoriseAgent()
              await(wsClient.url(otherUkDividendsAmountUrl)
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", "Csrf-Token" -> "nocheck")
                .post(Map("amount" -> "999999999999999999999999999999999")))
            }

            "have the correct status" in {
              result.status shouldBe BAD_REQUEST
            }
            implicit val document: () => Document = () => Jsoup.parse(result.body)
            welshToggleCheck(WELSH)
            errorSummaryCheck(expectedErrorOverMax, expectedErrorLink)
            errorAboveElementCheck(expectedErrorOverMax)

          }

          "return unauthorized when the authorization fails" which {
            val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount)).asJsonString,
              SessionValues.CLIENT_NINO -> "AA123456A"
            ))
            lazy val result: WSResponse = {
              authoriseAgentUnauthorized()
              await(wsClient.url(otherUkDividendsAmountUrl)
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
                .post(Map[String, String]()))
            }

            s"has an UNAUTHORIZED($UNAUTHORIZED) status" in {
              result.status shouldBe UNAUTHORIZED
            }
          }

      }

  }




  }

}

