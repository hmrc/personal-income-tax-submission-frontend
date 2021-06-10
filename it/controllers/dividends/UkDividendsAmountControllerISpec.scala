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
import models.dividends.{DividendsCheckYourAnswersModel, DividendsPriorSubmission}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, ViewHelpers}

class UkDividendsAmountControllerISpec extends IntegrationTest with ViewHelpers {


  val taxYear: Int = 2022
  val taxYearMinusOne: Int = taxYear - 1
  val amount: BigDecimal = 500
  val ukDividendsAmountUrl = s"$startUrl/$taxYear/dividends/how-much-dividends-from-uk-companies"

  val cyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = None)
  val cyaModelWithAmount: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount))

  trait ExpectedResultsUserType {
    val expectedH1: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val tellUsTheValue: String
    val youToldUsPriorText: String
    val expectedErrorEmpty: String
    val expectedErrorOverMax: String
    val expectedErrorInvalid: String
  }

  trait ExpectedResultsAllUsers {
    val captionExpected: String
    val continueText: String
    val continueLink: String
  }


  object IndividualExpectedEnglish extends ExpectedResultsUserType {
    val expectedH1 = "How much did you get in dividends from UK-based companies?"
    val expectedTitle = "How much did you get in dividends from UK-based companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val tellUsTheValue = "Tell us the value of the dividends you got, in pounds. You can find this information in your dividend voucher."
    val youToldUsPriorText = s"You told us you got £$amount in dividends from UK-based companies this year. Tell us if this has changed."
    val expectedErrorEmpty = "Enter how much you got in dividends from UK-based companies"
    val expectedErrorOverMax = "The amount of dividends from UK-based companies must be less than £100,000,000,000"
    val expectedErrorInvalid = "Enter how much you got in dividends in the correct format"
  }

  object AgentExpectedEnglish extends ExpectedResultsUserType {
    val expectedH1 = "How much did your client get in dividends from UK-based companies?"
    val expectedTitle = "How much did your client get in dividends from UK-based companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val tellUsTheValue = "Tell us the value of the dividends your client got, in pounds. You can find this information in their dividend voucher."
    val youToldUsPriorText = s"You told us your client got £$amount in dividends from UK-based companies this year. Tell us if this has changed."
    val expectedErrorEmpty = "Enter how much your client got in dividends from UK-based companies"
    val expectedErrorOverMax = "The amount of dividends from UK-based companies must be less than £100,000,000,000"
    val expectedErrorInvalid = "Enter how much your client got in dividends in the correct format"
  }

  object AllExpectedEnglish extends ExpectedResultsAllUsers {
    val continueText = "Continue"
    val continueLink = s"/income-through-software/return/personal-income/$taxYear/dividends/how-much-dividends-from-uk-companies"
    val captionExpected = s"Dividends for 6 April $taxYearMinusOne to 5 April $taxYear"
  }

  object IndividualExpectedWelsh extends ExpectedResultsUserType {
    val expectedH1 = "How much did you get in dividends from UK-based companies?"
    val expectedTitle = "How much did you get in dividends from UK-based companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val tellUsTheValue = "Tell us the value of the dividends you got, in pounds. You can find this information in your dividend voucher."
    val youToldUsPriorText = s"You told us you got £$amount in dividends from UK-based companies this year. Tell us if this has changed."
    val expectedErrorEmpty = "Enter how much you got in dividends from UK-based companies"
    val expectedErrorOverMax = "The amount of dividends from UK-based companies must be less than £100,000,000,000"
    val expectedErrorInvalid = "Enter how much you got in dividends in the correct format"
  }

  object AgentExpectedWelsh extends ExpectedResultsUserType {
    val expectedH1 = "How much did your client get in dividends from UK-based companies?"
    val expectedTitle = "How much did your client get in dividends from UK-based companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val tellUsTheValue = "Tell us the value of the dividends your client got, in pounds. You can find this information in their dividend voucher."
    val youToldUsPriorText = s"You told us your client got £$amount in dividends from UK-based companies this year. Tell us if this has changed."
    val expectedErrorEmpty = "Enter how much your client got in dividends from UK-based companies"
    val expectedErrorOverMax = "The amount of dividends from UK-based companies must be less than £100,000,000,000"
    val expectedErrorInvalid = "Enter how much your client got in dividends in the correct format"
  }

  object AllExpectedWelsh extends ExpectedResultsAllUsers {
    val continueText = "Continue"
    val continueLink = s"/income-through-software/return/personal-income/$taxYear/dividends/how-much-dividends-from-uk-companies"
    val captionExpected = s"Dividends for 6 April $taxYearMinusOne to 5 April $taxYear"
  }

  object Selectors {

    val poundPrefixSelector = ".govuk-input__prefix"
    val captionSelector = ".govuk-caption-l"
    val inputSelector = ".govuk-input"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val youToldUsSelector = "#main-content > div > div > form > div > label > p"
    val expectedErrorLink = "#amount"
    val inputAmountField = "#amount"
  }

  val poundPrefixText = "£"
  val continueLink = s"/income-through-software/return/personal-income/$taxYear/dividends/how-much-dividends-from-uk-companies"
  val amountInputName = "amount"

  val userScenarios =
    Seq(UserScenario(isWelsh = false, isAgent = false, Some(IndividualExpectedEnglish), AllExpectedEnglish),
      UserScenario(isWelsh = false, isAgent = true, Some(AgentExpectedEnglish), AllExpectedEnglish),
      UserScenario(isWelsh = true, isAgent = false, Some(IndividualExpectedWelsh), AllExpectedWelsh),
      UserScenario(isWelsh = true, isAgent = true, Some(AgentExpectedWelsh), AllExpectedWelsh))

  ".show" when {
    import Selectors._
    userScenarios.foreach { us =>
      s"language is ${printLang(us.isWelsh)} and request is from an ${printAgent(us.isAgent)}" should {

        "redirects user to overview page when there is no data in session" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            stubGet(s"/income-through-software/return/$taxYear/view", OK, "overview page content")
            urlGet(ukDividendsAmountUrl, us.isWelsh, headers = playSessionCookies(taxYear))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
            result.body shouldBe "overview page content"
          }

        }

        "returns uk dividends amount page with empty amount field" which {

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            urlGet(ukDividendsAmountUrl, us.isWelsh, headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(cyaModel)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(us.expectedResultsUserType.get.expectedTitle)
          h1Check(us.expectedResultsUserType.get.expectedH1 + " " + us.expectedResultsAllUsers.captionExpected)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(us.expectedResultsUserType.get.tellUsTheValue, youToldUsSelector)
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck("", inputAmountField)
          buttonCheck(us.expectedResultsAllUsers.continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(us.isWelsh)
        }

        "returns uk dividends amount page with pre-filled amount" which {

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            urlGet(ukDividendsAmountUrl, us.isWelsh, headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(cyaModelWithAmount)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(us.expectedResultsUserType.get.expectedTitle)
          h1Check(us.expectedResultsUserType.get.expectedH1 + " " + us.expectedResultsAllUsers.captionExpected)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(us.expectedResultsUserType.get.youToldUsPriorText, youToldUsSelector)
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck(amount.toString(), inputAmountField)
          buttonCheck(us.expectedResultsAllUsers.continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          welshToggleCheck(us.isWelsh)
        }

        "returns other uk dividends amount page with cya amount pre-filled even if there is prior submission" which {

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            urlGet(ukDividendsAmountUrl, us.isWelsh, headers = playSessionCookies(taxYear,
              SessionValues.DIVIDENDS_CYA, Json.toJson(DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount))),
              SessionValues.DIVIDENDS_PRIOR_SUB, Json.toJson(DividendsPriorSubmission(ukDividends = Some(1)))))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          inputFieldValueCheck(amount.toString(), "#amount")
        }

        "returns other uk dividends with empty amount field when priorSubmissionData and cyaData amounts are equal" which {


          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            urlGet(ukDividendsAmountUrl, us.isWelsh, headers = playSessionCookies(taxYear,
              SessionValues.DIVIDENDS_CYA, Json.toJson(DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount))),
              SessionValues.DIVIDENDS_PRIOR_SUB, Json.toJson(DividendsPriorSubmission(ukDividends = Some(amount)))))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          inputFieldValueCheck("", "#amount")
        }


        "return an action when auth call fails" which {
          lazy val result: WSResponse = {
            if (us.isAgent) {
              authoriseAgentUnauthorized()
            }
            else {
              authoriseIndividualUnauthorized()
            }
            urlGet(ukDividendsAmountUrl, us.isWelsh, headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(cyaModel)))
          }
          "has an UNAUTHORIZED(401) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }
      }

    }
  }

  ".submit" when {
    import Selectors._

    userScenarios.foreach { us =>
      s"language is ${printLang(us.isWelsh)} and request is from an ${printAgent(us.isAgent)}" should {


        s"return a BAD_REQUEST($BAD_REQUEST) status with an empty error" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(ukDividendsAmountUrl, us.isWelsh, headers = playSessionCookies(taxYear), postRequest = Map[String, String]())
          }

          "return the correct status" in {
            result.status shouldBe BAD_REQUEST
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)
          errorSummaryCheck(us.expectedResultsUserType.get.expectedErrorEmpty, expectedErrorLink)
          errorAboveElementCheck(us.expectedResultsUserType.get.expectedErrorEmpty)
        }
        s"return a BAD_REQUEST($BAD_REQUEST) status with an invalid error" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(ukDividendsAmountUrl, us.isWelsh, headers = playSessionCookies(taxYear), postRequest = Map("amount" -> "|"))
          }

          "return the correct status" in {
            result.status shouldBe BAD_REQUEST
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)
          errorSummaryCheck(us.expectedResultsUserType.get.expectedErrorInvalid, expectedErrorLink)
          errorAboveElementCheck(us.expectedResultsUserType.get.expectedErrorInvalid)
        }
        s"return a BAD_REQUEST($BAD_REQUEST) status with an OverMax error" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(ukDividendsAmountUrl, us.isWelsh, headers = playSessionCookies(taxYear), postRequest = Map("amount" -> "9999999999999999999999999"))
          }

          "return the correct status" in {
            result.status shouldBe BAD_REQUEST
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)
          errorSummaryCheck(us.expectedResultsUserType.get.expectedErrorOverMax, expectedErrorLink)
          errorAboveElementCheck(us.expectedResultsUserType.get.expectedErrorOverMax)
        }

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
  }
}



