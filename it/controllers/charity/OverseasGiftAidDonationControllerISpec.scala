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

import forms.YesNoForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, ViewHelpers}

class OverseasGiftAidDonationControllerISpec extends IntegrationTest with ViewHelpers {

  val taxYear: Int = 2022

  def url: String = s"$appUrl/$taxYear/charity/overseas-charity-donations-using-gift-aid"

  val taxYearMinusOne: Int = taxYear - 1

  object Selectors {
    val captionSelector = ".govuk-caption-l"
    val yourDividendsSelector = "#value-hint"
    val continueSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val errorSummaryHref = "#value"
  }

  trait CommonExpectedResults {
    val captionText: String
    val yesText: String
    val noText: String
    val continueText: String
    val continueLink: String
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val captionText = s"Donations to charity for 6 April $taxYearMinusOne to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val continueText = "Continue"
    val continueLink = s"/income-through-software/return/personal-income/$taxYear/charity/overseas-charity-donations-using-gift-aid"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val captionText = s"Donations to charity for 6 April $taxYearMinusOne to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val continueText = "Continue"
    val continueLink = s"/income-through-software/return/personal-income/$taxYear/charity/overseas-charity-donations-using-gift-aid"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1 = "Did you use Gift Aid to donate to an overseas charity?"
    val expectedTitle = "Did you use Gift Aid to donate to an overseas charity?"
    val expectedErrorText = "Select yes if you used Gift Aid to donate to an overseas charity"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1 = "Did your client use Gift Aid to donate to an overseas charity?"
    val expectedTitle = "Did your client use Gift Aid to donate to an overseas charity?"
    val expectedErrorText = "Select yes if your client used Gift Aid to donate to an overseas charity"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1 = "Did you use Gift Aid to donate to an overseas charity?"
    val expectedTitle = "Did you use Gift Aid to donate to an overseas charity?"
    val expectedErrorText = "Select yes if you used Gift Aid to donate to an overseas charity"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1 = "Did your client use Gift Aid to donate to an overseas charity?"
    val expectedTitle = "Did your client use Gift Aid to donate to an overseas charity?"
    val expectedErrorText = "Select yes if your client used Gift Aid to donate to an overseas charity"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  ".show" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the page with correct content" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          welshToggleCheck(user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1 + " " + captionText)
          textOnPageCheck(captionText, captionSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          noErrorsCheck()
        }
      }
    }
  }

  ".submit" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an OK" in {
          lazy val form: Map[String, Seq[String]] = Map(YesNoForm.yesNo -> Seq(YesNoForm.yes))

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(url, body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          result.status shouldBe OK
        }

        "no radio button has been selected" should {

          lazy val form: Map[String, Seq[String]] = Map(YesNoForm.yesNo -> Seq(""))

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(url, body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          titleCheck(errorPrefix + user.specificExpectedResults.get.expectedTitle)
          welshToggleCheck(user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1 + " " + captionText)
          textOnPageCheck(captionText, captionSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          errorSummaryCheck(user.specificExpectedResults.get.expectedErrorText, errorSummaryHref)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorText)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }
      }
    }
  }
}
