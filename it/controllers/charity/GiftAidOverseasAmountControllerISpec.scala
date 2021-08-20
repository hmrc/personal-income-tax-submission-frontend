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

import models.charity.GiftAidCYAModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import utils.CharityITHelper

class GiftAidOverseasAmountControllerISpec extends CharityITHelper {

  def url: String = s"$appUrl/$year/charity/amount-donated-to-overseas-charities"

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
    val expectedCaption = "Rhoddion i elusennau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022"
    val expectedInputName = "amount"
    val expectedButtonText = "Yn eich blaen"
    val expectedInputLabelText = "Total amount, in pounds"
    val expectedInputHintText = "Er enghraifft, £600 neu £193.54"
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
    val expectedErrorTitle = s"Gwall: $expectedTitle"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "How much did your client donate to overseas charities by using Gift Aid?"
    val expectedH1 = "How much did your client donate to overseas charities by using Gift Aid?"
    val expectedErrorEmpty = "Enter the amount your client donated to overseas charities"
    val expectedErrorInvalid = "Enter the amount your client donated to overseas charities in the correct format"
    val expectedErrorOverMax = "The amount your client donated to overseas charities must be less than £100,000,000,000"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  val requiredSessionModel: GiftAidCYAModel = GiftAidCYAModel(overseasDonationsViaGiftAid = Some(true))
  val requiredSessionData: Some[GiftAidCYAModel] = Some(requiredSessionModel)

  val validAmount = 250

  ".show" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the page with correct content" which {
          lazy val result = getResult(url, requiredSessionData, None, user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
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

    "there is no cya data" should {

      lazy val result: WSResponse = getResult(url, None, None)

      "redirect the user to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe overviewUrl
      }
    }

    "there is cya data, but 'overseasDonationsViaGiftAid' has not been stored" should {

      lazy val result: WSResponse = getResult(url, Some(GiftAidCYAModel(oneOffDonationsViaGiftAid = Some(false))), None)

      "redirect the user to the overseas donation page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.OverseasGiftAidDonationsController.show(year)}"
      }
    }
  }

  ".submit" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an error" when {

          "the submitted data is empty" which {
            lazy val result = postResult(url, requiredSessionData, None, Map("amount" -> ""), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(expectedInputLabelText, inputLabelSelector)
            textOnPageCheck(expectedInputHintText, inputHintTextSelector)
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, buttonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorEmpty, Selectors.expectedErrorLink, user.isWelsh)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorEmpty)
          }

          "the submitted data is too long" which {
            lazy val result = postResult(url, requiredSessionData, None, Map("amount" -> "999999999999999"), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(expectedInputLabelText, inputLabelSelector)
            textOnPageCheck(expectedInputHintText, inputHintTextSelector)
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, buttonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorOverMax, Selectors.expectedErrorLink, user.isWelsh)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorOverMax)
          }

          "the submitted data is in the incorrect format" which {
            lazy val result = postResult(url, requiredSessionData, None, Map("amount" -> ":@~{}<>?"), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(expectedInputLabelText, inputLabelSelector)
            textOnPageCheck(expectedInputHintText, inputHintTextSelector)
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, buttonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorInvalid, Selectors.expectedErrorLink, user.isWelsh)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorInvalid)
          }
        }
      }
    }

    "there is no cya data stored" should {

      lazy val result: WSResponse = postResult(url, None, None, Map("amount" -> s"$validAmount"))

      "redirect the user to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${appConfig.incomeTaxSubmissionOverviewUrl(year)}"
      }
    }

    "the user submits a valid amount" when {

      "this completes the cya data" should {
        lazy val result: WSResponse =
          postResult(url, Some(completeGiftAidCYAModel.copy(overseasDonationsViaGiftAidAmount = None)), None, Map("amount" -> s"$validAmount"))

        "redirect the user to the 'check your answers' page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(year)}"
        }
        "update the cya data" in {
          findGiftAidDb shouldBe Some(completeGiftAidCYAModel.copy(overseasDonationsViaGiftAidAmount = Some(validAmount)))
        }
      }

      "this does not complete the cya data" should {
        lazy val result: WSResponse = postResult(url, requiredSessionData, None, Map("amount" -> s"$validAmount"))

        "redirect the user to the 'overseas charity name' page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOverseasNameController.show(year, None)}"
        }
        "update the cya data" in {
          findGiftAidDb shouldBe Some(requiredSessionModel.copy(overseasDonationsViaGiftAidAmount = Some(validAmount)))
        }
      }
    }
  }
}