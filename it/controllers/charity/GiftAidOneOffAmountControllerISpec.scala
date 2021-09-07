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
import models.charity.prior.{GiftAidPaymentsModel, GiftAidSubmissionModel}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import utils.CharityITHelper

class GiftAidOneOffAmountControllerISpec extends CharityITHelper {

  def url: String = s"$appUrl/$year/charity/amount-donated-as-one-off"

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
    val expectedPriorP1: String
    val expectedCyaP1: String
    val expectedErrorEmpty: String
    val expectedErrorInvalid: String
    val expectedErrorOverMax: String
    val expectedErrorTitle: String
    val expectedErrorExceeds: String
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
    val expectedInputLabelText = "Total amount for the year, in pounds"
    val expectedInputHintText = "For example, £600 or £193.54"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = "Rhoddion i elusennau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022"
    val expectedInputName = "amount"
    val expectedButtonText = "Yn eich blaen"
    val expectedInputLabelText = "Cyfanswm ar gyfer y flwyddyn, mewn punnoedd"
    val expectedInputHintText = "Er enghraifft, £600 neu £193.54"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much did you donate to charity as one-off payments?"
    val expectedH1 = "How much did you donate to charity as one-off payments?"
    val expectedParagraph = "Do not include the Gift Aid added to your donation."
    val expectedPriorP1 = "You told us you donated £555 to charity as one-off payments. Tell us if this has changed."
    val expectedCyaP1 = "You told us you donated £50 to charity as one-off payments. Tell us if this has changed."
    val expectedErrorEmpty = "Enter the amount you donated to charity as one-off payments"
    val expectedErrorInvalid = "Enter the amount you donated as one-off payments in the correct format"
    val expectedErrorOverMax = "The amount you donated as one-off payments must be less than £100,000,000,000"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorExceeds = "The amount you donated as one-off payments must not be more than the amount you donated to charity by using Gift Aid"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much did your client donate to charity as one-off payments?"
    val expectedH1 = "How much did your client donate to charity as one-off payments?"
    val expectedParagraph = "Do not include the Gift Aid added to your client’s donation."
    val expectedPriorP1 = "You told us your client donated £555 to charity as one-off payments. Tell us if this has changed."
    val expectedCyaP1 = "You told us your client donated £50 to charity as one-off payments. Tell us if this has changed."
    val expectedErrorEmpty = "Enter the amount your client donated to charity as one-off payments"
    val expectedErrorInvalid = "Enter the amount your client donated as one-off payments in the correct format"
    val expectedErrorOverMax = "The amount your client donated as one-off payments must be less than £100,000,000,000"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorExceeds =
      "The amount your client donated as one-off payments must not be more than the amount your client donated to charity by using Gift Aid"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Faint wnaethoch ei roi i elusen fel taliadau untro?"
    val expectedH1 = "Faint wnaethoch ei roi i elusen fel taliadau untro?"
    val expectedParagraph = "Peidiwch â chynnwys y Rhodd Cymorth a ychwanegwyd at eich rhodd."
    val expectedPriorP1 = "You told us you donated £555 to charity as one-off payments. Tell us if this has changed."
    val expectedCyaP1 = "You told us you donated £50 to charity as one-off payments. Tell us if this has changed."
    val expectedErrorEmpty = "Nodwch y swm a roesoch i elusen fel taliadau untro"
    val expectedErrorInvalid = "Nodwch y swm a roesoch fel taliadau untro yn y fformat cywir"
    val expectedErrorOverMax = "Mae’n rhaid i’r swm a roesoch fel taliadau untro fod yn llai na £100,000,000,000"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorExceeds = "Ni chaiff y swm a roesoch fel taliadau untro fod yn fwy na’r swm a roesoch i elusennau drwy ddefnyddio Rhodd Cymorth"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Faint wnaeth eich cleient ei roi i elusen fel taliadau untro?"
    val expectedH1 = "Faint wnaeth eich cleient ei roi i elusen fel taliadau untro?"
    val expectedParagraph = "Peidiwch â chynnwys y Rhodd Cymorth a ychwanegwyd at rodd eich cleient."
    val expectedPriorP1 = "You told us your client donated £555 to charity as one-off payments. Tell us if this has changed."
    val expectedCyaP1 = "You told us your client donated £50 to charity as one-off payments. Tell us if this has changed."
    val expectedErrorEmpty = "Nodwch y swm a roddodd eich cleient i elusen fel taliadau untro"
    val expectedErrorInvalid = "Nodwch y swm a roddodd eich cleient fel taliadau untro yn y fformat cywir"
    val expectedErrorOverMax = "Mae’n rhaid i’r swm a roddodd eich cleient fel taliadau untro fod yn llai na £100,000,000,000"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorExceeds =
      "Ni chaiff y swm a roddodd eich cleient fel taliadau untro fod yn fwy na’r swm a roddodd eich cleient i elusennau drwy ddefnyddio Rhodd Cymorth"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  val totalDonatedAmount = 50
  val validAmount = 25

  val oneOffCyaAmount = 50

  val requiredSessionModel = GiftAidCYAModel(oneOffDonationsViaGiftAid = Some(true), donationsViaGiftAidAmount = Some(totalDonatedAmount))
  val requiredSessionData = Some(requiredSessionModel)

  val requiredSessionModelPrefill: GiftAidCYAModel = requiredSessionModel.copy(
    oneOffDonationsViaGiftAidAmount = Some(oneOffCyaAmount)
  )
  val requiredSessionDataPrefill: Option[GiftAidCYAModel] = Some(requiredSessionModelPrefill)

  val requiredPriorData = Some(IncomeSourcesModel(None, None, Some(priorDataMax)))

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
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
          textOnPageCheck(expectedInputLabelText, inputLabelSelector)
          textOnPageCheck(expectedInputHintText, inputHintTextSelector)
          inputFieldCheck(expectedInputName, inputFieldSelector)
          buttonCheck(expectedButtonText, buttonSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the page with correct content with prefill CYA data" which {
          lazy val result = getResult(url, requiredSessionDataPrefill, None, user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedCyaP1, paragraphSelector)
          textOnPageCheck(expectedInputLabelText, inputLabelSelector)
          textOnPageCheck(expectedInputHintText, inputHintTextSelector)
          inputFieldCheck(expectedInputName, inputFieldSelector)
          buttonCheck(expectedButtonText, buttonSelector)
          welshToggleCheck(user.isWelsh)
        }

        "display the correct prior amount when returning after submission" which {
          lazy val result = getResult(url, requiredSessionData, requiredPriorData, user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          inputFieldCheck(expectedInputName, Selectors.inputFieldSelector)
          inputFieldValueCheck("", Selectors.inputFieldSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedPriorP1, Selectors.paragraphSelector)
        }

        "display the correct cya amount when returning before resubmitting" which {
          lazy val result = getResult(url, Some(completeGiftAidCYAModel), requiredPriorData, user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          inputFieldCheck(expectedInputName, Selectors.inputFieldSelector)
          inputFieldValueCheck("50", Selectors.inputFieldSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedCyaP1, Selectors.paragraphSelector)
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

    "there is cya data, but 'oneOffDonationsViaGiftAid' has not been stored" should {
      lazy val result: WSResponse = getResult(url, Some(GiftAidCYAModel(donationsViaGiftAidAmount = Some(validAmount))), None)

      "redirect the user to the one off donation page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOneOffController.show(year)}"
      }
    }

    "there is cya data, but 'donationsViaGiftAidAmount' has not been stored" should {
      lazy val result: WSResponse = getResult(url, Some(GiftAidCYAModel(oneOffDonationsViaGiftAidAmount = Some(validAmount))), None)

      "redirect the user to the gift aid donation page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidDonationsController.show(year)}"
      }
    }

    "'oneOffDonationsViaGiftAid' exists and is false" should {
      lazy val result: WSResponse = getResult(
        url,
        Some(GiftAidCYAModel(donationsViaGiftAidAmount = Some(validAmount), oneOffDonationsViaGiftAid = Some(false))),
        None
      )

      "redirect the user to the one off donation page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOneOffController.show(year)}"
      }
    }

    "there is prior data for oneOffCurrentYear" should {

      "display the GiftAidOneOffAmountController page when the 'Change' link is click on the CYA page" which {

        val priorData = IncomeSourcesModel(None, None,
          giftAid = Some(GiftAidSubmissionModel(Some(GiftAidPaymentsModel(oneOffCurrentYear = Some(1000.21))))))


        lazy val result = getResult(url, requiredSessionData, Some(priorData))

        "has an OK 200 status" in {
          result.status shouldBe OK
        }
      }
    }
  }

  ".submit" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an error" when {

          import Selectors._
          import user.commonExpectedResults._

          "the submitted data is empty" which {
            lazy val result = postResult(url, requiredSessionData, None, Map("amount" -> ""), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
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
            textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
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
            textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
            textOnPageCheck(expectedInputLabelText, inputLabelSelector)
            textOnPageCheck(expectedInputHintText, inputHintTextSelector)
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, buttonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorInvalid, Selectors.expectedErrorLink, user.isWelsh)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorInvalid)
          }

          "the submitted amount is greater than the 'donationsViaGiftAidAmount'" which {
            lazy val result = postResult(url, requiredSessionData, None, Map("amount" -> "50.01"), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedParagraph, paragraphSelector)
            textOnPageCheck(expectedInputLabelText, inputLabelSelector)
            textOnPageCheck(expectedInputHintText, inputHintTextSelector)
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, buttonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorExceeds, Selectors.expectedErrorLink, user.isWelsh)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorExceeds)
          }
        }
      }
    }

    "there is no cya data stored" should {
      lazy val result = postResult(url, None, None, Map("amount" -> s"$validAmount"))

      "redirect the user to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe overviewUrl
      }
    }

    "there is cya data stored and the user enters a valid amount" when {

      "this completes the cya data" should {
        lazy val result = postResult(url, Some(completeGiftAidCYAModel), None, Map("amount" -> s"$validAmount"))

        "redirect the user to the 'check your answers' page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(year)}"
        }

        "update the cya data" in {
          findGiftAidDb shouldBe Some(completeGiftAidCYAModel.copy(oneOffDonationsViaGiftAidAmount = Some(validAmount)))
        }
      }

      "this does not complete the cya data" should {
        lazy val result = postResult(url, requiredSessionData, None, Map("amount" -> s"$validAmount"))

        "redirect the user to the 'overseas donations' page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.OverseasGiftAidDonationsController.show(year)}"
        }

        "update the cya data" in {
          findGiftAidDb shouldBe Some(requiredSessionModel.copy(oneOffDonationsViaGiftAidAmount = Some(validAmount)))
        }
      }
    }
  }
}