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

class RedeemableSharesStatusControllerISpec extends IntegrationTest with ViewHelpers with DefaultBodyWritables {

  val redeemableSharesStatusUrl: String = s"/update-and-submit-income-tax-return/personal-income/2023/dividends/redeemable-shares-status"

  val postURL: String = s"$appUrl/2023/dividends/redeemable-shares-status"

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedP1: String
    val expectedErrorText: String
  }

  trait CommonExpectedResults {
    val expectedP2: String
    val expectedP3: String
    val captionExpected: String
    val yesNo: Boolean => String
    val continueText: String
  }

  object AllExpectedEnglish extends CommonExpectedResults {
    val expectedP2 = "Redeemable shares are ones a company can buy back at an agreed price on a future date."
    val expectedP3 = "Free additional shares are also known as 'bonus issues of securities'."
    val captionExpected = s"Dividends for 6 April $taxYearEOY to 5 April $taxYear"
    val yesNo: Boolean => String = isYes => if (isYes) "Yes" else "No"
    val continueText = "Continue"
  }

  object AllExpectedWelsh extends CommonExpectedResults {
    val expectedP2 = "Cyfranddaliadau y mae cwmnïau’n gallu prynu’n ôl yn y dyfodol am bris y cytunwyd arno yw ‘cyfranddaliadau adbryn’."
    val expectedP3 = "Enw arall ar gyfranddaliadau ychwanegol a gawsoch yn rhad ac am ddim yw ‘dyroddiadau bonws o warantau’."
    val captionExpected = s"Difidendau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val yesNo: Boolean => String = isYes => if (isYes) "Iawn" else "Na"
    val continueText = "Yn eich blaen"
  }

  object IndividualExpectedEnglish extends SpecificExpectedResults {
    val expectedTitle = "Did you get free or redeemable shares?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedP1 = "You can hold both free and redeemable shares."
    val expectedErrorText = "Select Yes if you got free or redeemable shares"
  }

  object IndividualExpectedWelsh extends SpecificExpectedResults {
    val expectedTitle = "A gawsoch chi gyfranddaliadau adbryn neu gyfranddaliadau yn rhad ac am ddim?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedP1 = "Gallwch ddal cyfranddaliadau adbryn a chyfranddaliadau a gawsoch yn rhad ac am ddim."
    val expectedErrorText = "Dewiswch ‘Iawn’ os cawsoch gyfranddaliadau adbryn neu gyfranddaliadau yn rhad ac am ddim"
  }

  object AgentExpectedEnglish extends SpecificExpectedResults {
    val expectedTitle = "Did your client get free or redeemable shares?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedP1 = "Your client can hold both free and redeemable shares."
    val expectedErrorText = "Select Yes if your client got free or redeemable shares"
  }

  object AgentExpectedWelsh extends SpecificExpectedResults {
    val expectedTitle = "A gafodd eich cleient gyfranddaliadau adbryn neu gyfranddaliadau yn rhad ac am ddim?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedP1 = "Gall eich cleient ddal cyfranddaliadau adbryn a chyfranddaliadau a gafodd yn rhad ac am ddim."
    val expectedErrorText = "Dewiswch ‘Iawn’ os cafodd eich cleient gyfranddaliadau adbryn neu gyfranddaliadau yn rhad ac am ddim"
  }

  object Selectors {
    val titleSelector = "#main-content > div > div > form > div > fieldset > legend"
    val captionSelector = ".govuk-caption-l"
    val p1Selector = "#p1"
    val p2Selector = "#p2"
    val p3Selector = "#p3"
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

      "display the redeemable shares status page" which {
        lazy val headers = playSessionCookie(scenario.isAgent) ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
        lazy val request = FakeRequest("GET", redeemableSharesStatusUrl).withHeaders(headers: _*)

        lazy val result = {
          authoriseAgentOrIndividual(scenario.isAgent)
          route(app, request, "{}").get
        }

        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of OK(200)" in {
          status(result) shouldBe OK
        }

        h1Check(expectedTitle + " " + captionExpected)
        captionCheck(captionExpected)
        formPostLinkCheck(redeemableSharesStatusUrl, Selectors.formSelector)
        textOnPageCheck(expectedP1, Selectors.p1Selector)
        textOnPageCheck(expectedP2, Selectors.p2Selector)
        textOnPageCheck(expectedP3, Selectors.p3Selector)
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