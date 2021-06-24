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

import common.SessionValues.GIFT_AID_PRIOR_SUB
import forms.YesNoForm
import models.charity.prior.{GiftAidPaymentsModel, GiftAidSubmissionModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, ViewHelpers}

class GiftAidLastTaxYearControllerISpec extends IntegrationTest with ViewHelpers {

  val taxYear: Int = 2022

  val testModel: GiftAidSubmissionModel =
    GiftAidSubmissionModel(Some(GiftAidPaymentsModel(None, Some(List("JaneDoe")), None, Some(150.00), None, None)),None)

  def url: String = s"$appUrl/$taxYear/charity/add-charity-donations-to-last-tax-year"

  object Selectors {
    val continueSelector = "#continue"
    val captionSelector: String = ".govuk-caption-l"
    val contentSelector1: String = "#main-content > div > div > form > div > fieldset > legend > div > p:nth-child(1)"
    val contentSelector2: String = "#main-content > div > div > form > div > fieldset > legend > div > p:nth-child(2)"
    val errorSummaryHref = "#value"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedError: String
    val expectedContent1: String
    val expectedContent2: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val yesText: String
    val noText: String
    val expectedContinue: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: String = "Donations to charity for 6 April 2021 to 5 April 2022"
    val yesText = "Yes"
    val noText = "No"
    val expectedContinue = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: String = "Donations to charity for 6 April 2021 to 5 April 2022"
    val yesText = "Yes"
    val noText = "No"
    val expectedContinue = "Continue"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "Do you want to add any of your donations to the last tax year?"
    val expectedH1: String = "Do you want to add any of your donations to the last tax year?"
    val expectedError: String = "Select yes to add any of your donations to the last tax year"
    val expectedContent1: String = "You told us you donated £150 to charity by using Gift Aid. You can add some of this donation" +
      " to the 6 April 2020 to 5 April 2021 tax year."
    val expectedContent2: String = "You might want to do this if you paid higher rate tax last year but will not this year."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "Do you want to add any of your client’s donations to the last tax year?"
    val expectedH1: String = "Do you want to add any of your client’s donations to the last tax year?"
    val expectedError: String = "Select yes to add any of your client’s donations to the last tax year"
    val expectedContent1: String = "You told us your client donated £150 to charity by using Gift Aid. You can add some of this donation" +
      " to the 6 April 2020 to 5 April 2021 tax year."
    val expectedContent2: String = "You might want to do this if your client paid higher rate tax last year but will not this year."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "Do you want to add any of your donations to the last tax year?"
    val expectedH1: String = "Do you want to add any of your donations to the last tax year?"
    val expectedError: String = "Select yes to add any of your donations to the last tax year"
    val expectedContent1: String = "You told us you donated £150 to charity by using Gift Aid. You can add some of this donation" +
      " to the 6 April 2020 to 5 April 2021 tax year."
    val expectedContent2: String = "You might want to do this if you paid higher rate tax last year but will not this year."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "Do you want to add any of your client’s donations to the last tax year?"
    val expectedH1: String = "Do you want to add any of your client’s donations to the last tax year?"
    val expectedError: String = "Select yes to add any of your client’s donations to the last tax year"
    val expectedContent1: String = "You told us your client donated £150 to charity by using Gift Aid. You can add some of this donation" +
      " to the 6 April 2020 to 5 April 2021 tax year."
    val expectedContent2: String = "You might want to do this if your client paid higher rate tax last year but will not this year."
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

        "return a redirect when no data" in {

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url, follow = false, welsh = user.isWelsh, headers =  playSessionCookie(user.isAgent))
          }

          result.status shouldBe SEE_OTHER
        }

        "render the page with correct content" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url, welsh = user.isWelsh, headers =  playSessionCookie(user.isAgent, Map(GIFT_AID_PRIOR_SUB -> Json.toJson(testModel).toString())))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedContent1, contentSelector1)
          textOnPageCheck(user.specificExpectedResults.get.expectedContent2, contentSelector2)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(expectedContinue, continueSelector)
          noErrorsCheck()
          welshToggleCheck(user.isWelsh)
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
            urlPost(url, body = form, follow = false, welsh = user.isWelsh, headers =  playSessionCookie(user.isAgent, Map(GIFT_AID_PRIOR_SUB -> Json.toJson(testModel).toString())))
          }

          result.status shouldBe OK
        }

        "return a redirect when no data with an invalid form" in {
          lazy val form: Map[String, Seq[String]] = Map(YesNoForm.yesNo -> Seq(""))

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(url, body = form, follow = false, welsh = user.isWelsh, headers =  playSessionCookie(user.isAgent))
          }

          result.status shouldBe SEE_OTHER
        }

        "no radio button has been selected" should {

          lazy val form: Map[String, Seq[String]] = Map(YesNoForm.yesNo -> Seq(""))

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(url, body = form, follow = false, welsh = user.isWelsh, headers =  playSessionCookie(user.isAgent,
              Map(GIFT_AID_PRIOR_SUB -> Json.toJson(testModel).toString())))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          titleCheck(errorPrefix + user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedContent1, contentSelector1)
          textOnPageCheck(user.specificExpectedResults.get.expectedContent2, contentSelector2)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(expectedContinue, continueSelector)
          errorSummaryCheck(user.specificExpectedResults.get.expectedError, errorSummaryHref)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedError)
          welshToggleCheck(user.isWelsh)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }
      }
    }
  }
}