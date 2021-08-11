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

class GiftAidDonatedAmountControllerISpec extends CharityITHelper {


  def url: String = s"$appUrl/$year/charity/amount-donated-using-gift-aid"

  object Selectors {
    val expectedErrorLink = "#amount"
    val captionSelector = ".govuk-caption-l"
    val paragraphSelector = "#main-content > div > div > form > div > label > p"
    val inputFieldSelector = "#amount"
    val buttonSelector = ".govuk-button"
    val inputLabelSelector = "#main-content > div > div > form > div > label > div"
    val inputHintTextSelector = ".govuk-hint"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedParagraph: String
    val expectedErrorEmpty: String
    val expectedErrorOverMax: String
    val expectedErrorBadFormat: String
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
    val expectedInputLabelText = "Total amount for the year"
    val expectedInputHintText = "For example, £600 or £193.54"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val expectedInputName = "amount"
    val expectedButtonText = "Continue"
    val expectedInputLabelText = "Total amount for the year"
    val expectedInputHintText = "For example, £600 or £193.54"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much did you donate to charity by using Gift Aid?"
    val expectedH1 = "How much did you donate to charity by using Gift Aid?"
    val expectedParagraph = "Do not include the Gift Aid that was added to your donation."
    val expectedErrorEmpty = "Enter the amount you donated to charity by using Gift Aid"
    val expectedErrorOverMax = "The amount you donated to charity must be less than £100,000,000,000"
    val expectedErrorBadFormat = "Enter the amount you donated to charity in the correct format"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much did your client donate to charity by using Gift Aid?"
    val expectedH1 = "How much did your client donate to charity by using Gift Aid?"
    val expectedParagraph = "Do not include the Gift Aid that was added to your client’s donation."
    val expectedErrorEmpty = "Enter the amount your client donated to charity by using Gift Aid"
    val expectedErrorOverMax = "The amount your client donated to charity must be less than £100,000,000,000"
    val expectedErrorBadFormat = "Enter the amount your client donated to charity in the correct format"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "How much did you donate to charity by using Gift Aid?"
    val expectedH1 = "How much did you donate to charity by using Gift Aid?"
    val expectedParagraph = "Do not include the Gift Aid that was added to your donation."
    val expectedErrorEmpty = "Enter the amount you donated to charity by using Gift Aid"
    val expectedErrorOverMax = "The amount you donated to charity must be less than £100,000,000,000"
    val expectedErrorBadFormat = "Enter the amount you donated to charity in the correct format"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "How much did your client donate to charity by using Gift Aid?"
    val expectedH1 = "How much did your client donate to charity by using Gift Aid?"
    val expectedParagraph = "Do not include the Gift Aid that was added to your client’s donation."
    val expectedErrorEmpty = "Enter the amount your client donated to charity by using Gift Aid"
    val expectedErrorOverMax = "The amount your client donated to charity must be less than £100,000,000,000"
    val expectedErrorBadFormat = "Enter the amount your client donated to charity in the correct format"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  val requiredSessionModel: GiftAidCYAModel = GiftAidCYAModel(donationsViaGiftAid = Some(true))
  val requiredSessionData: Some[GiftAidCYAModel] = Some(requiredSessionModel)

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

          titleCheck(user.specificExpectedResults.get.expectedTitle)
          h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
          textOnPageCheck(expectedInputLabelText, inputLabelSelector)
          textOnPageCheck(expectedInputHintText, inputHintTextSelector)
          inputFieldCheck(expectedInputName, inputFieldSelector)
          buttonCheck(expectedButtonText, buttonSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "there is no cya data" should {
      lazy val result = getResult(url, None, None)

      "redirect the user to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${appConfig.incomeTaxSubmissionOverviewUrl(year)}"
      }
    }

    "there is cya data, but 'donationsViaGiftAid' has not been stored" should {
      lazy val result = getResult(url, Some(GiftAidCYAModel()), None)

      "redirect the user to the gift aid donation page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidDonationsController.show(year)}"
      }
    }

    "'donationsViaGiftAid' exists and is false" which {
      lazy val result: WSResponse = getResult(url, Some(GiftAidCYAModel(donationsViaGiftAid = Some(false))), None)

      "redirect the user to the gift aid donation page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidDonationsController.show(year)}"
      }
    }
  }

  ".submit" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an error" when {

          "the submitted data is empty" which {
            lazy val form: Map[String, String] = Map("amount" -> "")

            lazy val result = postResult(url, requiredSessionData, None, form, user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
            textOnPageCheck(expectedInputLabelText, inputLabelSelector)
            textOnPageCheck(expectedInputHintText, inputHintTextSelector)
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, buttonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorEmpty, Selectors.expectedErrorLink)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorEmpty)
          }

          "the submitted data is too long" which {
            lazy val form: Map[String, String] = Map("amount" -> "999999999999999999999999999999999999999999999999")

            lazy val result = postResult(url, requiredSessionData, None, form, user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
            textOnPageCheck(expectedInputLabelText, inputLabelSelector)
            textOnPageCheck(expectedInputHintText, inputHintTextSelector)
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, buttonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorOverMax, Selectors.expectedErrorLink)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorOverMax)
          }

          "the submitted data is in the incorrect format" which {
            lazy val form: Map[String, String] = Map("amount" -> ":@~{}<>?")

            lazy val result = postResult(url, requiredSessionData, None, form, user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
            h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
            textOnPageCheck(expectedInputLabelText, inputLabelSelector)
            textOnPageCheck(expectedInputHintText, inputHintTextSelector)
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, buttonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorBadFormat, Selectors.expectedErrorLink)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorBadFormat)
          }
        }
      }
    }

    "there is no cya data stored" should {

      lazy val result = postResult(url, None, None, Map("amount" -> "123000.42"))

      "redirect the user to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${appConfig.incomeTaxSubmissionOverviewUrl(year)}"
      }
    }

    "there is cya data stored and the user has entered a valid amount" when {
      val validAmount = 123

      "the cya data is updated successfully" should {
        lazy val result: WSResponse = postResult(url, requiredSessionData, None, Map("amount" -> s"$validAmount"))

        "redirect the user to the 'One off donations' page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOneOffController.show(year)}"
        }

        "update the cya data" in {
          findGiftAidDb shouldBe Some(requiredSessionModel.copy(donationsViaGiftAidAmount = Some(validAmount)))
        }
      }
    }
  }
}