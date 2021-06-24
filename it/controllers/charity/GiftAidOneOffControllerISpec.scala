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
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, ViewHelpers}

class GiftAidOneOffControllerISpec extends IntegrationTest with ViewHelpers {

  val taxYear = 2022
  val giftAidDonations = 100

  val taxYearMinusOne: Int = taxYear -1

  def url: String = s"$appUrl/$taxYear/charity/one-off-charity-donations"

  object Selectors {
    val captionSelector = ".govuk-caption-l"
    val p1Selector = "#main-content > div > div > form > div > fieldset > legend > div > p:nth-child(1)"
    val p2Selector = "#main-content > div > div > form > div > fieldset > legend > div > p:nth-child(2)"
    val continueSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val errorSummaryHref = "#value"
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedPara1: String
    val expectedPara2: String
    val expectedErrorTitle: String
    val expectedErrorText: String
  }

  trait CommonExpectedResults {
    val captionText: String
    val yesText: String
    val noText: String
    val continueText: String
    val continueLink: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val captionText = s"Donations to charity for 6 April $taxYearMinusOne to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val continueText = "Continue"
    val continueLink = s"/income-through-software/return/personal-income/$taxYear/charity/one-off-charity-donations"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val captionText = s"Donations to charity for 6 April $taxYearMinusOne to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val continueText = "Continue"
    val continueLink = s"/income-through-software/return/personal-income/$taxYear/charity/one-off-charity-donations"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1 = "Did you make one-off donations?"
    val expectedTitle = "Did you make one-off donations?"
    val expectedPara1= s"You told us you used Gift Aid to donate £$giftAidDonations to charity. Tell us if any of this was made as one-off payments."
    val expectedPara2 = "One-off donations are payments you did not repeat."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you made a one-off donation to charity"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1 = "Did your client make one-off donations?"
    val expectedTitle = "Did your client make one-off donations?"
    val expectedPara1 = s"You told us your client used Gift Aid to donate £$giftAidDonations to charity. Tell us if any of this was made as one-off payments."
    val expectedPara2 = "One-off donations are payments your client did not repeat."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if your client made a one-off donation to charity"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1 = "Did you make one-off donations?"
    val expectedTitle = "Did you make one-off donations?"
    val expectedPara1= s"You told us you used Gift Aid to donate £$giftAidDonations to charity. Tell us if any of this was made as one-off payments."
    val expectedPara2 = "One-off donations are payments you did not repeat."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you made a one-off donation to charity"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1 = "Did your client make one-off donations?"
    val expectedTitle = "Did your client make one-off donations?"
    val expectedPara1 = s"You told us your client used Gift Aid to donate £$giftAidDonations to charity. Tell us if any of this was made as one-off payments."
    val expectedPara2 = "One-off donations are payments your client did not repeat."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if your client made a one-off donation to charity"
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
            urlGet(url, welsh = user.isWelsh, headers =  playSessionCookie(user.isAgent))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          welshToggleCheck(user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1 + " " + captionText)
          textOnPageCheck(captionText, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedPara1, p1Selector)
          textOnPageCheck(user.specificExpectedResults.get.expectedPara2, p2Selector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
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
            urlPost(url, body = form, follow = false, welsh = user.isWelsh, headers =  playSessionCookie(user.isAgent))
          }

          result.status shouldBe OK
        }

        "no radio button has been selected" should {

          lazy val form: Map[String, Seq[String]] = Map(YesNoForm.yesNo -> Seq(""))

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(url, body = form, follow = false, welsh = user.isWelsh, headers =  playSessionCookie(user.isAgent))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          titleCheck(errorPrefix + user.specificExpectedResults.get.expectedTitle)
          welshToggleCheck(user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1 + " " + captionText)
          textOnPageCheck(captionText, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedPara1, p1Selector)
          textOnPageCheck(user.specificExpectedResults.get.expectedPara2, p2Selector)
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