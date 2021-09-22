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
    val expectedPriorP1: Int => String
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
    val expectedCaption = "Rhoddion i elusennau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022"
    val expectedInputName = "amount"
    val expectedButtonText = "Yn eich blaen"
    val expectedInputHintText = "Er enghraifft, £600 neu £193.54"
    val expectedInputLabelText = "Cyfanswm ar gyfer y flwyddyn"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "How much did you donate to charity by using Gift Aid?"
    val expectedH1 = "How much did you donate to charity by using Gift Aid?"
    val expectedParagraph = "Do not include the Gift Aid that was added to your donation."
    val expectedPriorP1: Int => String = amount => s"You told us you used Gift Aid to donate £$amount to charity. Tell us if this has changed."
    val expectedErrorEmpty = "Enter the amount you donated to charity by using Gift Aid"
    val expectedErrorOverMax = "The amount you donated to charity must be less than £100,000,000,000"
    val expectedErrorBadFormat = "Enter the amount you donated to charity in the correct format"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "How much did your client donate to charity by using Gift Aid?"
    val expectedH1 = "How much did your client donate to charity by using Gift Aid?"
    val expectedParagraph = "Do not include the Gift Aid that was added to your client’s donation."
    val expectedPriorP1: Int => String = amount => s"You told us your client used Gift Aid to donate £$amount to charity. Tell us if this has changed."
    val expectedErrorEmpty = "Enter the amount your client donated to charity by using Gift Aid"
    val expectedErrorOverMax = "The amount your client donated to charity must be less than £100,000,000,000"
    val expectedErrorBadFormat = "Enter the amount your client donated to charity in the correct format"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Faint wnaethoch ei roi i elusen drwy ddefnyddio Rhodd Cymorth?"
    val expectedH1 = "Faint wnaethoch ei roi i elusen drwy ddefnyddio Rhodd Cymorth?"
    val expectedParagraph = "Peidiwch â chynnwys y Rhodd Cymorth a ychwanegwyd at eich rhodd."
    val expectedPriorP1: Int => String = amount => s"You told us you used Gift Aid to donate £$amount to charity. Tell us if this has changed."
    val expectedErrorEmpty = "Nodwch y swm a roesoch i elusen drwy ddefnyddio Rhodd Cymorth"
    val expectedErrorOverMax = "Mae’n rhaid i’r swm a roesoch i elusen fod yn llai na £100,000,000,000"
    val expectedErrorBadFormat = "Nodwch y swm a roesoch i elusen yn y fformat cywir"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Faint wnaeth eich cleient ei roi i elusen drwy ddefnyddio Rhodd Cymorth?"
    val expectedH1 = "Faint wnaeth eich cleient ei roi i elusen drwy ddefnyddio Rhodd Cymorth?"
    val expectedParagraph = "Peidiwch â chynnwys y Rhodd Cymorth a ychwanegwyd at rodd eich cleient."
    val expectedPriorP1: Int => String = amount => s"You told us your client used Gift Aid to donate £$amount to charity. Tell us if this has changed."
    val expectedErrorEmpty = "Nodwch y swm a roddodd eich cleient i elusen drwy ddefnyddio Rhodd Cymorth"
    val expectedErrorOverMax = "Mae’n rhaid i’r swm a roddodd eich cleient i elusen fod yn llai na £100,000,000,000"
    val expectedErrorBadFormat = "Nodwch y swm a roddodd eich cleient i elusen yn y fformat cywir"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  val oneOffCyaAmount = 50

  val requiredSessionModel: GiftAidCYAModel = GiftAidCYAModel(donationsViaGiftAid = Some(true))
  val requiredSessionData: Some[GiftAidCYAModel] = Some(requiredSessionModel)
  val requiredSessionModelPrefill: GiftAidCYAModel = GiftAidCYAModel(
    donationsViaGiftAid = Some(true),
    donationsViaGiftAidAmount = Some(oneOffCyaAmount)
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
          h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption, labelAsHeading = true)
          textOnPageCheck(expectedCaption, captionSelector)
          hintTextCheck(s"${user.specificExpectedResults.get.expectedParagraph} $expectedInputLabelText $expectedInputHintText")
          inputFieldCheck(expectedInputName, inputFieldSelector)
          buttonCheck(expectedButtonText, buttonSelector)
          welshToggleCheck(user.isWelsh)
        }

        "render the page with correct content with prefilled CYA data" which {
          lazy val result = getResult(url, requiredSessionDataPrefill, None, user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption, labelAsHeading = true)
          textOnPageCheck(expectedCaption, captionSelector)
          hintTextCheck(s"${user.specificExpectedResults.get.expectedPriorP1(50)} $expectedInputLabelText $expectedInputHintText")
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
          hintTextCheck(s"${user.specificExpectedResults.get.expectedPriorP1(1222)} $expectedInputLabelText $expectedInputHintText")
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
          hintTextCheck(s"${user.specificExpectedResults.get.expectedPriorP1(50)} $expectedInputLabelText $expectedInputHintText")
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

    "there is prior data for currentYear" should {

      "display the GiftAidDonatedAmount page when the 'Change' link is clicked on the CYA page" which {

        val priorData = IncomeSourcesModel(None, None,
          giftAid = Some(GiftAidSubmissionModel(Some(GiftAidPaymentsModel(currentYear = Some(1000.56)))))
        )

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

          "the submitted data is empty" which {
            lazy val form: Map[String, String] = Map("amount" -> "")

            lazy val result = postResult(url, requiredSessionData, None, form, user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption, labelAsHeading = true)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(s"${user.specificExpectedResults.get.expectedParagraph} $expectedInputLabelText $expectedInputHintText")
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, buttonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorEmpty, Selectors.expectedErrorLink, user.isWelsh)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorEmpty)
          }

          "the submitted data is too long" which {
            lazy val form: Map[String, String] = Map("amount" -> "999999999999999999999999999999999999999999999999")

            lazy val result = postResult(url, requiredSessionData, None, form, user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption, labelAsHeading = true)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(s"${user.specificExpectedResults.get.expectedParagraph} $expectedInputLabelText $expectedInputHintText")
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, buttonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorOverMax, Selectors.expectedErrorLink, user.isWelsh)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorOverMax)
          }

          "the submitted data is in the incorrect format" which {
            lazy val form: Map[String, String] = Map("amount" -> ":@~{}<>?")

            lazy val result = postResult(url, requiredSessionData, None, form, user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption, labelAsHeading = true)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(s"${user.specificExpectedResults.get.expectedParagraph} $expectedInputLabelText $expectedInputHintText")
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, buttonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorBadFormat, Selectors.expectedErrorLink, user.isWelsh)
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

