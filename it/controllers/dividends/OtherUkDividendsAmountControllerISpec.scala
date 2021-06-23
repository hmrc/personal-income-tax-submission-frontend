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

import models.dividends.{DividendsCheckYourAnswersModel, DividendsPriorSubmission}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import utils.{DividendsDatabaseHelper, IntegrationTest, ViewHelpers}

class OtherUkDividendsAmountControllerISpec extends IntegrationTest with ViewHelpers with DividendsDatabaseHelper {


  val taxYear: Int = 2022
  val taxYearMinusOne: Int = taxYear - 1
  val amount: BigDecimal = 500
  val otherUkDividendsAmountUrl = s"$startUrl/$taxYear/dividends/how-much-dividends-from-uk-trusts-and-open-ended-investment-companies"

  val validCyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = None)
  val validCyaModelWithAmount: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(otherUkDividends = Some(true),
    otherUkDividendsAmount = Some(amount))

  val poundPrefixText = "£"

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val tellUsTheValue: String
    val youToldUsPriorText: String
    val expectedErrorEmpty: String
    val expectedErrorOverMax: String
    val expectedErrorInvalid: String
  }

  trait CommonExpectedResults {
    val captionExpected: String
    val continueText: String
    val expectedHintText: String
  }


  object IndividualExpectedEnglish extends SpecificExpectedResults {
    val expectedH1 = "How much did you get in dividends from trusts and open-ended investment companies based in the UK?"
    val expectedTitle = "How much did you get in dividends from trusts and open-ended investment companies based in the UK?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val tellUsTheValue = "Tell us the value of the dividends you got, in pounds. You can find this information in your dividend voucher."
    val youToldUsPriorText: String = s"You told us you got £$amount in dividends from UK-based trusts and open-ended investment companies this year. " +
      s"Tell us if this has changed."
    val expectedErrorEmpty = "Enter how much you got in dividends from trusts and open-ended investment companies"
    val expectedErrorInvalid = "Enter how much you got in dividends in the correct format"
    val expectedErrorOverMax = "The amount of dividends from trusts and open-ended investment companies based in the UK must be less than £100,000,000,000"
  }

  object AgentExpectedEnglish extends SpecificExpectedResults {
    val expectedH1 = "How much did your client get in dividends from trusts and open-ended investment companies based in the UK?"
    val expectedTitle = "How much did your client get in dividends from trusts and open-ended investment companies based in the UK?"
    val expectedErrorTitleAgent = s"Error: $expectedTitle"
    val tellUsTheValue = "Tell us the value of the dividends your client got, in pounds. You can find this information in their dividend voucher."
    val youToldUsPriorText: String = s"You told us your client got £$amount in dividends from UK-based trusts and open-ended investment companies this year. " +
      s"Tell us if this has changed."
    val expectedErrorEmpty = "Enter how much your client got in dividends from trusts and open-ended investment companies"
    val expectedErrorInvalid = "Enter how much your client got in dividends in the correct format"
    val expectedErrorOverMax = "The amount of dividends from trusts and open-ended investment companies based in the UK must be less than £100,000,000,000"
    val expectedErrorTitle: String = s"Error $expectedTitle"
  }

  object AllExpectedEnglish extends CommonExpectedResults {
    val continueText = "Continue"
    val expectedHintText = "For example, £600 or £193.54"
    val captionExpected = s"Dividends for 6 April $taxYearMinusOne to 5 April $taxYear"
  }

  object IndividualExpectedWelsh extends SpecificExpectedResults {
    val expectedH1 = "How much did you get in dividends from trusts and open-ended investment companies based in the UK?"
    val expectedTitle = "How much did you get in dividends from trusts and open-ended investment companies based in the UK?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val tellUsTheValue = "Tell us the value of the dividends you got, in pounds. You can find this information in your dividend voucher."
    val youToldUsPriorText: String = s"You told us you got £$amount in dividends from UK-based trusts and open-ended investment companies this year. " +
      s"Tell us if this has changed."
    val expectedErrorEmpty = "Enter how much you got in dividends from trusts and open-ended investment companies"
    val expectedErrorInvalid = "Enter how much you got in dividends in the correct format"
    val expectedErrorOverMax = "The amount of dividends from trusts and open-ended investment companies based in the UK must be less than £100,000,000,000"
  }

  object AgentExpectedWelsh extends SpecificExpectedResults {
    val expectedH1 = "How much did your client get in dividends from trusts and open-ended investment companies based in the UK?"
    val expectedTitle = "How much did your client get in dividends from trusts and open-ended investment companies based in the UK?"
    val expectedErrorTitleAgent = s"Error: $expectedTitle"
    val tellUsTheValue = "Tell us the value of the dividends your client got, in pounds. You can find this information in their dividend voucher."
    val youToldUsPriorText: String = s"You told us your client got £$amount in dividends from UK-based trusts and open-ended investment companies this year. " +
      s"Tell us if this has changed."
    val expectedErrorEmpty = "Enter how much your client got in dividends from trusts and open-ended investment companies"
    val expectedErrorInvalid = "Enter how much your client got in dividends in the correct format"
    val expectedErrorOverMax = "The amount of dividends from trusts and open-ended investment companies based in the UK must be less than £100,000,000,000"
    val expectedErrorTitle: String = s"Error $expectedTitle"
  }

  object AllExpectedWelsh extends CommonExpectedResults {
    val continueText = "Continue"
    val expectedHintText = "For example, £600 or £193.54"
    val captionExpected = s"Dividends for 6 April $taxYearMinusOne to 5 April $taxYear"
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

  val userScenarios =
    Seq(UserScenario(isWelsh = false, isAgent = false, AllExpectedEnglish, Some(IndividualExpectedEnglish)),
      UserScenario(isWelsh = false, isAgent = true, AllExpectedEnglish, Some(AgentExpectedEnglish)),
      UserScenario(isWelsh = true, isAgent = false, AllExpectedWelsh, Some(IndividualExpectedWelsh)),
      UserScenario(isWelsh = true, isAgent = true, AllExpectedWelsh, Some(AgentExpectedWelsh)))


  ".show" when {
    
    userScenarios.foreach { us =>
      s"language is ${welshTest(us.isWelsh)} and request is from an ${agentTest(us.isAgent)}" should {

        import Selectors._
        import us.specificExpectedResults._
        import us.commonExpectedResults._

        "returns other uk dividends amount page with relevant content and amount field empty" which {


          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            dropDividendsDB()
            emptyUserDataStub()
            insertCyaData(Some(validCyaModel))
            urlGet(otherUkDividendsAmountUrl, us.isWelsh, headers = playSessionCookies(us.isAgent))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(get.expectedTitle)
          h1Check(get.expectedH1 + " " + captionExpected)
          textOnPageCheck(captionExpected, captionSelector)
          textOnPageCheck(get.tellUsTheValue, tellUsTheValueSelector)
          hintTextCheck(expectedHintText)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)

          welshToggleCheck(us.isWelsh)
        }

        "returns other uk dividends amount page with with relevant content and amount field pre-filled" which {

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            dropDividendsDB()
            emptyUserDataStub()
            insertCyaData(Some(validCyaModelWithAmount))
            urlGet(otherUkDividendsAmountUrl, us.isWelsh,
              headers = playSessionCookies(us.isAgent))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(get.expectedTitle)
          h1Check(get.expectedH1 + " " + captionExpected)
          textOnPageCheck(captionExpected, captionSelector)
          textOnPageCheck(get.youToldUsPriorText, tellUsTheValueSelector)
          hintTextCheck(expectedHintText)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          inputFieldValueCheck(amount.toString(), amountSelector)

          welshToggleCheck(us.isWelsh)

        }
      }
    }
  }
  ".show" should {

    "redirects user to overview page when there is no data in session" which {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        stubGet(s"/income-through-software/return/$taxYear/view", OK, "overview page content")
        urlGet(otherUkDividendsAmountUrl, headers = playSessionCookies(false))
      }

      "has an OK(200) status" in {
        result.status shouldBe OK
        result.body shouldBe "overview page content"
      }
    }

    "returns other uk dividends amount page with cya amount pre-filled even if there is prior submission" which {

      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        insertCyaData(Some(validCyaModelWithAmount))
        userDataStub(IncomeSourcesModel(
          dividends = Some(DividendsPriorSubmission(
            None,
            Some(1)))),
          nino, taxYear)
        urlGet(otherUkDividendsAmountUrl, headers = playSessionCookies(false))
      }

      "has an OK(200) status" in {
        result.status shouldBe OK
      }

      implicit val document: () => Document = () => Jsoup.parse(result.body)

      inputFieldValueCheck(amount.toString(), "#amount")
    }

    "returns other uk dividends with empty amount field when priorSubmissionData and cyaData amounts are equal" which {


      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        insertCyaData(Some(validCyaModel))
        userDataStub(IncomeSourcesModel(
          dividends = Some(DividendsPriorSubmission(
            None,
            Some(amount)))),
          nino, taxYear)
        urlGet(otherUkDividendsAmountUrl, headers = playSessionCookies(false))
      }

      "has an OK(200) status" in {
        result.status shouldBe OK
      }

      implicit val document: () => Document = () => Jsoup.parse(result.body)

      inputFieldValueCheck("", "#amount")
    }
  }

  ".submit" when {

    userScenarios.foreach { us =>

      import us.specificExpectedResults._

      s"language is ${welshTest(us.isWelsh)} and request is from an ${agentTest(us.isAgent)}" should {

        s"return a BAD_REQUEST($BAD_REQUEST) status with an Empty Error" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(otherUkDividendsAmountUrl, us.isWelsh, headers = playSessionCookies(us.isAgent), postRequest = Map[String, String]())
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)
          errorSummaryCheck(get.expectedErrorEmpty, expectedErrorLink)
          errorAboveElementCheck(get.expectedErrorEmpty)
        }
        s"return a BAD_REQUEST($BAD_REQUEST) status with an Invalid Error" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(otherUkDividendsAmountUrl, us.isWelsh, headers = playSessionCookies(us.isAgent), postRequest = Map("amount" -> "|"))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)
          errorSummaryCheck(get.expectedErrorInvalid, expectedErrorLink)
          errorAboveElementCheck(get.expectedErrorInvalid)
        }
        s"return a BAD_REQUEST($BAD_REQUEST) status with an OverMax Error" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(otherUkDividendsAmountUrl, us.isWelsh, headers = playSessionCookies(us.isAgent),
              postRequest = Map("amount" -> "9999999999999999999999999999"))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)
          errorSummaryCheck(get.expectedErrorOverMax, expectedErrorLink)
          errorAboveElementCheck(get.expectedErrorOverMax)
        }
      }
    }
  }


  ".submit" should {

    "redirect to Dividends CYA page if answer to Did you get other uk dividends is yes" in {

      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        insertCyaData(Some(DividendsCheckYourAnswersModel(Some(true), Some(amount),Some(true))))
        urlPost(otherUkDividendsAmountUrl, follow=false, headers = playSessionCookies(false), postRequest = Map("amount" -> "123"))
      }

      result.status shouldBe SEE_OTHER
      result.header(HeaderNames.LOCATION) shouldBe Some(routes.DividendsCYAController.show(taxYear).url)
    }
  }

}

