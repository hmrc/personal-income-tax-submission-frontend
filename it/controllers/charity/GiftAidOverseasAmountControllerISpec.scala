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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, ViewHelpers}

class GiftAidOverseasAmountControllerISpec extends IntegrationTest with ViewHelpers {

  val taxYear: Int = 2022

  def url: String = s"$appUrl/$taxYear/charity/amount-donated-to-overseas-charities"

  object Selectors {
    val expectedErrorLink = "#amount"
    val captionSelector = ".govuk-caption-l"
    val inputFieldSelector = "#amount"
    val buttonSelector = ".govuk-button"
    val inputLabelSelector = "#main-content > div > div > form > div > label > div"
    val inputHintTextSelector = ".govuk-hint"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedErrorEmpty: String
    val expectedErrorInvalid: String
    val expectedErrorOverMax: String
    val expectedErrorTitle: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedInputName: String
    val expectedButtonText: String
    val expectedInputLabelText: String
    val expectedInputHintText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val expectedInputName = "amount"
    val expectedButtonText = "Continue"
    val expectedInputLabelText = "Total amount, in pounds"
    val expectedInputHintText = "For example, £600 or £193.54"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val expectedInputName = "amount"
    val expectedButtonText = "Continue"
    val expectedInputLabelText = "Total amount, in pounds"
    val expectedInputHintText = "For example, £600 or £193.54"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much did you donate to overseas charities by using Gift Aid?"
    val expectedH1 = "How much did you donate to overseas charities by using Gift Aid?"
    val expectedErrorEmpty = "Enter the amount you donated to overseas charities"
    val expectedErrorInvalid = "Enter the amount you donated to overseas charities in the correct format"
    val expectedErrorOverMax = "The amount you donated to overseas charities must be less than £100,000,000,000"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much did your client donate to overseas charities by using Gift Aid?"
    val expectedH1 = "How much did your client donate to overseas charities by using Gift Aid?"
    val expectedErrorEmpty = "Enter the amount your client donated to overseas charities"
    val expectedErrorInvalid = "Enter the amount your client donated to overseas charities in the correct format"
    val expectedErrorOverMax = "The amount your client donated to overseas charities must be less than £100,000,000,000"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "How much did you donate to overseas charities by using Gift Aid?"
    val expectedH1 = "How much did you donate to overseas charities by using Gift Aid?"
    val expectedErrorEmpty = "Enter the amount you donated to overseas charities"
    val expectedErrorInvalid = "Enter the amount you donated to overseas charities in the correct format"
    val expectedErrorOverMax = "The amount you donated to overseas charities must be less than £100,000,000,000"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "How much did your client donate to overseas charities by using Gift Aid?"
    val expectedH1 = "How much did your client donate to overseas charities by using Gift Aid?"
    val expectedErrorEmpty = "Enter the amount your client donated to overseas charities"
    val expectedErrorInvalid = "Enter the amount your client donated to overseas charities in the correct format"
    val expectedErrorOverMax = "The amount your client donated to overseas charities must be less than £100,000,000,000"
    val expectedErrorTitle = s"Error: $expectedTitle"
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
          h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(expectedInputLabelText, inputLabelSelector)
          textOnPageCheck(expectedInputHintText, inputHintTextSelector)
          inputFieldCheck(expectedInputName, inputFieldSelector)
          buttonCheck(expectedButtonText, buttonSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }
  }

  ".submit" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an OK" in {
          lazy val form: Map[String, Seq[String]] = Map("amount" -> Seq("1234"))

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(url, body = form, follow = false, welsh = user.isWelsh, headers =  playSessionCookie(user.isAgent))
          }

          result.status shouldBe OK
        }

        "return an error" when {

          "the submitted data is empty" which {
            lazy val form: Map[String, Seq[String]] = Map("amount" -> Seq(""))

            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(url, body = form, follow = false, welsh = user.isWelsh, headers =  playSessionCookie(user.isAgent))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(expectedInputLabelText, inputLabelSelector)
            textOnPageCheck(expectedInputHintText, inputHintTextSelector)
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, buttonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorEmpty, Selectors.expectedErrorLink)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorEmpty)
          }

          "the submitted data is too long" which {
            lazy val form: Map[String, Seq[String]] = Map("amount" -> Seq("999999999999999999999999999999999999999999999999"))

            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(url, body = form, follow = false, welsh = user.isWelsh, headers =  playSessionCookie(user.isAgent))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(expectedInputLabelText, inputLabelSelector)
            textOnPageCheck(expectedInputHintText, inputHintTextSelector)
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, buttonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorOverMax, Selectors.expectedErrorLink)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorOverMax)
          }

          "the submitted data is in the incorrect format" which {
            lazy val form: Map[String, Seq[String]] = Map("amount" -> Seq(":@~{}<>?"))

            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(url, body = form, follow = false, welsh = user.isWelsh, headers =  playSessionCookie(user.isAgent))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(expectedInputLabelText, inputLabelSelector)
            textOnPageCheck(expectedInputHintText, inputHintTextSelector)
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, buttonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorInvalid, Selectors.expectedErrorLink)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorInvalid)
          }
        }
      }
    }
  }
}