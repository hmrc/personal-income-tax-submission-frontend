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
import utils.{IntegrationTest, ViewHelpers}
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, route}

class StockDividendStatusControllerISpec extends IntegrationTest with ViewHelpers with DefaultBodyWritables {

  val stockDividendStatusUrl: String = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/stock-dividend-status"
  val postURL: String = s"$appUrl/$taxYear/dividends/stock-dividend-status"

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedP1: String
    val expectedErrorText: String
  }

  trait CommonExpectedResults {
    val expectedHeading: String
    val expectedP2: String
    val captionExpected: String
    val yesNo: Boolean => String
    val continueText: String
  }

  object AllExpectedEnglish extends CommonExpectedResults {
    val expectedHeading = "Stock dividends"
    val expectedP2 = "Find out more about stock dividends (opens in a new window)"
    val captionExpected = s"Dividends for 6 April $taxYearEOY to 5 April $taxYear"
    val yesNo: Boolean => String = isYes => if (isYes) "Yes" else "No"
    val continueText = "Continue"
  }

  object AllExpectedWelsh extends CommonExpectedResults {
    val expectedHeading = "Difidendau stoc"
    val expectedP2 = "Dysgwch ragor am ddifidendau stoc (yn agor ffenestr newydd)"
    val captionExpected = s"Difidendau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val yesNo: Boolean => String = isYes => if (isYes) "Iawn" else "Na"
    val continueText = "Yn eich blaen"
  }

  object IndividualExpectedEnglish extends SpecificExpectedResults {
    val expectedP1 = "If you chose shares instead of cash dividends, that is a stock dividend."
    val expectedTitle = "Did you get stock dividends?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select Yes if you got stock dividends"
  }

  object IndividualExpectedWelsh extends SpecificExpectedResults {
    val expectedP1 = "Os dewisoch gyfranddaliadau yn lle difidendau ar ffurf arian parod, mae'n golygu y cawsoch ddifidend stoc."
    val expectedTitle = "A gawsoch ddifidendau stoc?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorText = "Dewiswch 'Iawn' os cawsoch ddifidendau stoc"
  }

  object AgentExpectedEnglish extends SpecificExpectedResults {
    val expectedP1 = "If your client chose shares instead of cash dividends, that is a stock dividend."
    val expectedErrorText = "Select Yes if your client got stock dividends"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedTitle = "Did your client get stock dividends?"
  }

  object AgentExpectedWelsh extends SpecificExpectedResults {
    val expectedP1 = "Os gwnaeth eich cleient ddewis cyfranddaliadau yn lle difidendau ar ffurf arian parod, mae'n golygu y cafodd eich cleient ddifidend stoc."
    val expectedErrorText = "Dewiswch 'Iawn' os cafodd eich cleient ddifidendau stoc"
    val expectedTitle = "A gafodd eich cleient ddifidendau stoc?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
  }

  object Selectors {
    val headingSelector = "#main-content > div > div > h1"
    val titleSelector = "#main-content > div > div > form > div > fieldset > legend"
    val captionSelector = ".govuk-caption-l"
    val p1Selector = "#p1"
    val p2Selector = "#p2"
    val continueButtonSelector = "#continue"
    val formSelector = "#main-content > div > div > form"
    val errorSummaryHref = "#value"
    val errorSelector = "#main-content > div > div > div.govuk-error-summary > div > h2"
  }

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

      "display the stock dividend status page" which {
        lazy val headers = playSessionCookie(scenario.isAgent) ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
        lazy val request = FakeRequest("GET", stockDividendStatusUrl).withHeaders(headers: _*)

        lazy val result = {
          authoriseAgentOrIndividual(scenario.isAgent)
          route(app, request, "{}").get
        }

        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of OK(200)" in {
          status(result) shouldBe OK
        }

        titleCheck(expectedTitle, scenario.isWelsh)
        h1Check(expectedHeading + " " + captionExpected)
        captionCheck(captionExpected)
        formPostLinkCheck(stockDividendStatusUrl, Selectors.formSelector)
        textOnPageCheck(expectedP1, Selectors.p1Selector)
        textOnPageCheck(expectedP2, Selectors.p2Selector)
        buttonCheck(continueText, Selectors.continueButtonSelector)
        radioButtonCheck(yesNo(true), 1)
        radioButtonCheck(yesNo(false), 2)
        welshToggleCheck(scenario.isWelsh)
      }

    }

    s".submit when $testNameWelsh and the user is $testNameAgent" should {

      "return a 200 status" in {
        lazy val result = {
          authoriseAgentOrIndividual(scenario.isAgent)
          urlPost(postURL, follow = false, headers = playSessionCookie(scenario.isAgent), body = Map("value" -> Seq("true")))
        }
        result.status shouldBe OK
      }
      "return a error" when {
        "the form is empty" which {

          lazy val result = {
            authoriseAgentOrIndividual(scenario.isAgent)
            urlPost(postURL, welsh = scenario.isWelsh, follow = false, headers = playSessionCookie(scenario.isAgent), body = Map("value" -> ""))
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          "has a 400 BAD_REQUEST status " in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(errorPrefix(scenario.isWelsh) + expectedTitle, scenario.isWelsh)
          errorAboveElementCheck(expectedErrorText)
          errorSummaryCheck(expectedErrorText, Selectors.errorSummaryHref, scenario.isWelsh)
        }

      }

    }

  }
}