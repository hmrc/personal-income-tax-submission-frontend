/*
 * Copyright 2023 HM Revenue & Customs
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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.DefaultBodyWritables
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, route}
import utils.{IntegrationTest, ViewHelpers}

class CloseCompanyLoanAmountControllerISpec extends IntegrationTest with ViewHelpers with DefaultBodyWritables {

  val amount: BigDecimal = 500
  val closeCompanyLoanAmountUrl: String = s"/update-and-submit-income-tax-return/personal-income/2023/dividends/close-company-loan-amount"
  val postURL: String = s"$appUrl/2023/dividends/close-company-loan-amount"
  val poundPrefixText = "£"

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedH1: String
    val expectedErrorEmpty: String
  }

  trait CommonExpectedResults {
    val captionExpected: String
    val continueText: String
    val hintText: String
    val expectedErrorOverMax: String
    val expectedErrorInvalid: String
  }

  object IndividualExpectedEnglish extends SpecificExpectedResults {
    val expectedTitle = "How much did the close company write off or release from your loan?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedH1 = s"$expectedTitle"
    val expectedErrorEmpty = "Enter the amount written off or released from your loan"
  }

  object AgentExpectedEnglish extends SpecificExpectedResults {
    val expectedTitle = "How much did the close company write off or release from your client's loan?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedH1 = s"$expectedTitle"
    val expectedErrorEmpty = "Enter the amount written off or released from your client's loan"
  }

  object AllExpectedEnglish extends CommonExpectedResults {
    val continueText = "Continue"
    val captionExpected = s"Dividends for 6 April $taxYearEOY to 5 April $taxYear"
    val hintText = "For example, £193.52"
    val expectedErrorInvalid = "Enter the amount in the correct format. For example, £193.52"
    val expectedErrorOverMax = "Enter an amount less than £100,000,000,000"
  }

  object IndividualExpectedWelsh extends SpecificExpectedResults {
    val expectedTitle = "Faint gwnaeth y cwmni caeedig ei ddileu neu ei ryddhau o’ch benthyciad?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedH1 = s"$expectedTitle"
     val expectedErrorEmpty = "Nodwch y swm a gafodd ei ddileu neu ei ryddhau o’ch benthyciad"
  }

  object AgentExpectedWelsh extends SpecificExpectedResults {
    val expectedTitle = "Faint gwnaeth y cwmni caeedig ei ddileu neu ei ryddhau o fenthyciad eich cleient?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val expectedH1 = s"$expectedTitle"
    val expectedErrorEmpty = "Nodwch y swm a gafodd ei ddileu neu ei ryddhau o fenthyciad eich cleient"
  }

  object AllExpectedWelsh extends CommonExpectedResults {
    val continueText = "Yn eich blaen"
    val captionExpected = s"Difidendau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val hintText = "Er enghraifft, £193.52"
    val expectedErrorInvalid = "Nodwch y swm yn y fformat cywir. Er enghraifft, £193.52"
    val expectedErrorOverMax = "Nodwch swm sy’n llai na £100,000,000,000"
  }

  object Selectors {
    val poundPrefixSelector = ".govuk-input__prefix"
    val captionSelector = ".govuk-caption-l"
    val continueButtonSelector = "#continue"
    val formSelector = "#main-content > div > div > form"
    val inputSelector = "#amount"
    val errorSummaryHref = "#amount"
    val errorSelector = "#amount-error"
    val hint = "#amount-hint"
    val headingSelector = "#main-content > div > div > h1"
  }

  val amountInputName = "amount"

  protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, AllExpectedEnglish, Some(IndividualExpectedEnglish)),
    UserScenario(isWelsh = false, isAgent = true, AllExpectedEnglish, Some(AgentExpectedEnglish)),
    UserScenario(isWelsh = true, isAgent = false, AllExpectedWelsh, Some(IndividualExpectedWelsh)),
    UserScenario(isWelsh = true, isAgent = true, AllExpectedWelsh, Some(AgentExpectedWelsh))
  )

  userScenarios.foreach { scenario =>

    lazy val uniqueResults = scenario.specificExpectedResults.get
    import scenario.commonExpectedResults._
    import uniqueResults._

    val testNameWelsh = if (scenario.isWelsh) "in Welsh" else "in English"
    val testNameAgent = if (scenario.isAgent) "an agent" else "an individual"

    s".show when $testNameWelsh and the user is $testNameAgent" should {

      "display close company loan amount page" which {
        lazy val headers = playSessionCookie(scenario.isAgent) ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
        lazy val request = FakeRequest("GET", closeCompanyLoanAmountUrl).withHeaders(headers: _*)

        lazy val result = {
          authoriseAgentOrIndividual(scenario.isAgent)
          route(app, request, "{}").get
        }

        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of OK(200)" in {
          status(result) shouldBe OK
        }

        titleCheck(expectedTitle, scenario.isWelsh)
        h1Check(expectedH1 + " " + captionExpected)
        captionCheck(captionExpected)
        formPostLinkCheck(closeCompanyLoanAmountUrl, Selectors.formSelector)
        textOnPageCheck(hintText, Selectors.hint)
        buttonCheck(continueText, Selectors.continueButtonSelector)
        inputFieldCheck(amountInputName, Selectors.inputSelector)
      }
    }

    s".submit when $testNameWelsh and the user is $testNameAgent" should {

      "return a 200 status" in {
        lazy val result = {
          authoriseAgentOrIndividual(scenario.isAgent)
          urlPost(postURL, follow = false, headers = playSessionCookie(scenario.isAgent), body = Map("amount" -> "123"))
        }

        result.status shouldBe OK
      }

      "return a error" when {
        "the form is empty" which {

          lazy val result = {
            authoriseAgentOrIndividual(scenario.isAgent)
            urlPost(postURL, welsh = scenario.isWelsh, follow = false, headers = playSessionCookie(scenario.isAgent), body = Map("amount" -> ""))
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          "has a 400 BAD_REQUEST status " in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(errorPrefix(scenario.isWelsh) + expectedTitle, scenario.isWelsh)
          errorAboveElementCheck(expectedErrorEmpty)
          errorSummaryCheck(expectedErrorEmpty, Selectors.errorSummaryHref, scenario.isWelsh)
        }

        "the form is invalid" which {
          lazy val result = {
            authoriseAgentOrIndividual(scenario.isAgent)
            urlPost(postURL, welsh = scenario.isWelsh, follow = false, headers = playSessionCookie(scenario.isAgent), body = Map("amount" -> "$$$"))
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          "has a 400 BAD_REQUEST status " in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(errorPrefix(scenario.isWelsh) + expectedTitle, scenario.isWelsh)
          errorAboveElementCheck(expectedErrorInvalid)
          errorSummaryCheck(expectedErrorInvalid, Selectors.errorSummaryHref, scenario.isWelsh)
        }

        "the form is overmax" which {
          lazy val result = {
            authoriseAgentOrIndividual(scenario.isAgent)
            urlPost(postURL, welsh = scenario.isWelsh, follow = false,
              headers = playSessionCookie(scenario.isAgent), body = Map("amount" -> "103242424234242342423423"))
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          "has a 400 BAD_REQUEST status " in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(errorPrefix(scenario.isWelsh) + expectedTitle, scenario.isWelsh)
          errorAboveElementCheck(expectedErrorOverMax)
          errorSummaryCheck(expectedErrorOverMax, Selectors.errorSummaryHref, scenario.isWelsh)
        }
      }
    }
  }

}

