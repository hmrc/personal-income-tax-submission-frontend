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

class GiftAidSharesSecuritiesLandPropertyOverseasControllerISpec extends IntegrationTest with ViewHelpers {

  val taxYear: Int = 2022
  val taxYearMinusOne: Int = taxYear - 1

  def url: String = s"$appUrl/$taxYear/charity/donation-of-shares-securities-land-or-property-to-overseas-charities"

  object Selectors {
    val captionSelector = ".govuk-caption-l"
    val continueSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val errorSummaryHref = "#value"
    val disclosureSelectorTitle = "#main-content > div > div > form > details > summary > span"
    val disclosureSelectorParagraph = "#main-content > div > div > form > details > div > p"
    val disclosureSelectorBullet1 = "#main-content > div > div > form > details > div > ul > li:nth-child(1)"
    val disclosureSelectorBullet2 = "#main-content > div > div > form > details > div > ul > li:nth-child(2)"
    val disclosureSelectorBullet3 = "#main-content > div > div > form > details > div > ul > li:nth-child(3)"
    val disclosureSelectorBullet4 = "#main-content > div > div > form > details > div > ul > li:nth-child(4)"
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedError: String
  }

  trait CommonExpectedResults {
    val captionText: String
    val yesText: String
    val noText: String
    val continueText: String
    val disclosureContentTitle: String
    val disclosureContentParagraph: String
    val disclosureContentBullet1: String
    val disclosureContentBullet2: String
    val disclosureContentBullet3: String
    val disclosureContentBullet4: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val captionText = s"Donations to charity for 6 April $taxYearMinusOne to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val continueText = "Continue"
    val disclosureContentTitle = "What are qualifying shares and securities?"
    val disclosureContentParagraph = "Qualifying shares and securities are:"
    val disclosureContentBullet1 = "listed on a recognised stock exchange or dealt in on a designated market in the UK"
    val disclosureContentBullet2 = "units in an authorised unit trust"
    val disclosureContentBullet3 = "shares in an open-ended investment company"
    val disclosureContentBullet4 = "an interest in an offshore fund"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val captionText = s"Donations to charity for 6 April $taxYearMinusOne to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val continueText = "Continue"
    val disclosureContentTitle = "What are qualifying shares and securities?"
    val disclosureContentParagraph = "Qualifying shares and securities are:"
    val disclosureContentBullet1 = "listed on a recognised stock exchange or dealt in on a designated market in the UK"
    val disclosureContentBullet2 = "units in an authorised unit trust"
    val disclosureContentBullet3 = "shares in an open-ended investment company"
    val disclosureContentBullet4 = "an interest in an offshore fund"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you donate qualifying shares, securities, land or property to overseas charities?"
    val expectedH1 = "Did you donate qualifying shares, securities, land or property to overseas charities?"
    val expectedError = "Select yes if you donated shares, securities, land or property to overseas charities"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client donate qualifying shares, securities, land or property to overseas charities?"
    val expectedH1 = "Did your client donate qualifying shares, securities, land or property to overseas charities?"
    val expectedError = "Select yes if your client donated shares, securities, land or property to overseas charities"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did you donate qualifying shares, securities, land or property to overseas charities?"
    val expectedH1 = "Did you donate qualifying shares, securities, land or property to overseas charities?"
    val expectedError = "Select yes if you donated shares, securities, land or property to overseas charities"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client donate qualifying shares, securities, land or property to overseas charities?"
    val expectedH1 = "Did your client donate qualifying shares, securities, land or property to overseas charities?"
    val expectedError = "Select yes if your client donated shares, securities, land or property to overseas charities"
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

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          welshToggleCheck(user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1 + " " + captionText)
          textOnPageCheck(captionText, captionSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(continueText, continueSelector)
          textOnPageCheck(disclosureContentTitle, disclosureSelectorTitle)
          textOnPageCheck(disclosureContentParagraph, disclosureSelectorParagraph)
          textOnPageCheck(disclosureContentBullet1, disclosureSelectorBullet1)
          textOnPageCheck(disclosureContentBullet2, disclosureSelectorBullet2)
          textOnPageCheck(disclosureContentBullet3, disclosureSelectorBullet3)
          textOnPageCheck(disclosureContentBullet4, disclosureSelectorBullet4)
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
          textOnPageCheck(disclosureContentTitle, disclosureSelectorTitle)
          textOnPageCheck(disclosureContentParagraph, disclosureSelectorParagraph)
          textOnPageCheck(disclosureContentBullet1, disclosureSelectorBullet1)
          textOnPageCheck(disclosureContentBullet2, disclosureSelectorBullet2)
          textOnPageCheck(disclosureContentBullet3, disclosureSelectorBullet3)
          textOnPageCheck(disclosureContentBullet4, disclosureSelectorBullet4)
          errorSummaryCheck(user.specificExpectedResults.get.expectedError, errorSummaryHref)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedError)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }
      }
    }
  }
}