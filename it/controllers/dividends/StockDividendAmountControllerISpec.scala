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

class StockDividendAmountControllerISpec extends IntegrationTest with ViewHelpers with DefaultBodyWritables {

  val amount: BigDecimal = 500
  val stockDividendAmountUrl: String = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/stock-dividend-amount"
  val postURL: String = s"$appUrl/$taxYear/dividends/stock-dividend-amount"
  val poundPrefixText = "£"

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedParagraph1: String
    val expectedParagraph2: String
    val expectedLabel: String
    val expectedErrorEmpty: String
    val expectedErrorOverMax: String
    val expectedErrorInvalid: String
  }

  trait CommonExpectedResults {
    val expectedH1: String
    val captionExpected: String
    val continueText: String
  }

  object IndividualExpectedEnglish extends SpecificExpectedResults {
    val expectedTitle = "How much did you get?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedLabel = s"$expectedTitle"
    val expectedParagraph1 = "This is the cash value of the stock dividends you received."
    val expectedParagraph2 = "Your dividend statements should show 'the appropriate amount in cash', or the 'cash equivalent in share capital'."
    val expectedErrorEmpty = "Enter the amount of stock dividends you got"
    val expectedErrorInvalid = "Enter the amount of stock dividends you received in the correct format. For example, £193.54"
    val expectedErrorOverMax = "The amount of your stock dividends must be less than £100,000,000,000"
  }

  object AgentExpectedEnglish extends SpecificExpectedResults {
    val expectedTitle = "How much did your client get?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedLabel = s"$expectedTitle"
    val expectedParagraph1 = "This is the cash value of the stock dividends your client received."
    val expectedParagraph2 = "Your client's dividend statements should show 'the appropriate amount in cash', or the 'cash equivalent in share capital'."
    val expectedErrorEmpty = "Enter the amount of stock dividends your client got"
    val expectedErrorInvalid = "Enter the amount of stock dividends your client received in the correct format. For example, £193.54"
    val expectedErrorOverMax = "The amount of your client's stock dividends must be less than £100,000,000,000"
  }

  object AllExpectedEnglish extends CommonExpectedResults {
    val expectedH1 = "Stock dividends amount"
    val continueText = "Continue"
    val captionExpected = s"Dividends for 6 April $taxYearEOY to 5 April $taxYear"
  }

  object IndividualExpectedWelsh extends SpecificExpectedResults {
    val expectedTitle = "Faint gawsoch chi?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedLabel = s"$expectedTitle"
    val expectedParagraph1 = "Dyma werth ariannol y difidendau stoc a gawsoch."
    val expectedParagraph2 =
      "Dylai’ch datganiadau difidendau ddangos y swm priodol mewn arian parod, neu’r cyfwerth mewn arian parod o gyfranddaliadau cyfalaf."
    val expectedErrorEmpty = "Nodwch swm y difidendau stoc a gawsoch"
    val expectedErrorInvalid = "Nodwch swm y difidendau stoc a gawsoch yn y fformat cywir. Er enghraifft, £193.54"
    val expectedErrorOverMax = "Mae’n rhaid i swm eich difidendau stoc fod yn llai na £100,000,000,000"
  }

  object AgentExpectedWelsh extends SpecificExpectedResults {
    val expectedTitle = "Faint gafodd eich cleient?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val expectedLabel = s"$expectedTitle"
    val expectedParagraph1 = "Dyma werth ariannol y difidendau stoc a gafodd eich cleient."
    val expectedParagraph2 =
      "Dylai datganiadau difidend eich cleient ddangos y swm priodol mewn arian parod, neu’r cyfwerth mewn arian parod o gyfranddaliadau cyfalaf."
    val expectedErrorEmpty = "Nodwch swm y difidendau stoc a gafodd eich cleient"
    val expectedErrorInvalid = "Nodwch swm y difidendau stoc a gafodd eich cleient yn y fformat cywir. Er enghraifft, £193.54"
    val expectedErrorOverMax = "Mae’n rhaid i swm difidendau stoc eich cleient fod yn llai na £100,000,000,000"
  }

  object AllExpectedWelsh extends CommonExpectedResults {
    val expectedH1 = "Swm y difidendau stoc"
    val continueText = "Yn eich blaen"
    val captionExpected = s"Difidendau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
  }

  object Selectors {
    val poundPrefixSelector = ".govuk-input__prefix"
    val captionSelector = ".govuk-caption-l"
    val continueButtonSelector = "#continue"
    val formSelector = "#main-content > div > div > form"
    val paragraph1 = "#p1"
    val paragraph2 = "#p2"
    val inputSelector = "#amount"
    val errorSummaryHref = "#amount"
    val errorSelector = "#amount-error"
    val label = "#main-content > div > div > form > div > label"
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

      "display the stock dividend amount page" which {
        lazy val headers = playSessionCookie(scenario.isAgent) ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
        lazy val request = FakeRequest("GET", stockDividendAmountUrl).withHeaders(headers: _*)

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
        formPostLinkCheck(stockDividendAmountUrl, Selectors.formSelector)
        textOnPageCheck(expectedParagraph1, Selectors.paragraph1)
        textOnPageCheck(expectedParagraph2, Selectors.paragraph2)
        textOnPageCheck(expectedLabel, Selectors.label)
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

