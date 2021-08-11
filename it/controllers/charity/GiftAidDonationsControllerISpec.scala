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
import models.charity.GiftAidCYAModel
import models.charity.prior.{GiftAidPaymentsModel, GiftAidSubmissionModel}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import utils.CharityITHelper

class GiftAidDonationsControllerISpec extends CharityITHelper {

  def url: String = s"$appUrl/$year/charity/charity-donation-using-gift-aid"

  object Selectors {
    val captionSelector = ".govuk-caption-l"
    val continueSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val errorSummaryHref = "#value"
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
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
    val captionText = s"Donations to charity for 6 April ${year - 1} to 5 April $year"
    val yesText = "Yes"
    val noText = "No"
    val continueText = "Continue"
    val continueLink = s"/income-through-software/return/personal-income/$year/charity/charity-donation-using-gift-aid"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val captionText = s"Donations to charity for 6 April ${year - 1} to 5 April $year"
    val yesText = "Yes"
    val noText = "No"
    val continueText = "Continue"
    val continueLink = s"/income-through-software/return/personal-income/$year/charity/charity-donation-using-gift-aid"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1 = "Did you use Gift Aid to donate to charity?"
    val expectedTitle = "Did you use Gift Aid to donate to charity?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you used Gift Aid to donate to charity"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1 = "Did your client use Gift Aid to donate to charity?"
    val expectedTitle = "Did your client use Gift Aid to donate to charity?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if your client used Gift Aid to donate to charity"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1 = "Did you use Gift Aid to donate to charity?"
    val expectedTitle = "Did you use Gift Aid to donate to charity?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if you used Gift Aid to donate to charity"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1 = "Did your client use Gift Aid to donate to charity?"
    val expectedTitle = "Did your client use Gift Aid to donate to charity?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Select yes if your client used Gift Aid to donate to charity"
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
          lazy val result = getResult(url, None, None, user.isAgent, user.isWelsh)

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
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          noErrorsCheck()
        }
      }
    }

    "there is prior data" should {
      val priorData = IncomeSourcesModel(giftAid = Some(GiftAidSubmissionModel(Some(GiftAidPaymentsModel(
        currentYear = Some(1000.00)
      )))))

      lazy val result = getResult(url, None, Some(priorData))

      "redirect the user to the CYA page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(year)}"
      }
    }


  }

  ".submit" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "no radio button has been selected" should {
          lazy val result = postResult(url, None, None, Map(YesNoForm.yesNo -> ""), user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle)
          h1Check(user.specificExpectedResults.get.expectedH1 + " " + captionText)
          textOnPageCheck(captionText, captionSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          errorSummaryCheck(user.specificExpectedResults.get.expectedErrorText, errorSummaryHref)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorText)
          welshToggleCheck(user.isWelsh)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }
      }
    }

    "the user has selected 'no'" when {

      "this completes the cya data" should {
        lazy val result = postResult(url, Some(completeGiftAidCYAModel), None, Map(YesNoForm.yesNo -> YesNoForm.no))

        "redirect the user to the CYA page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(year)}"
        }

        "update the cya data" in {
          findGiftAidDb shouldBe
            Some(completeGiftAidCYAModel.copy(
              donationsViaGiftAid = Some(false),
              donationsViaGiftAidAmount = None,
              oneOffDonationsViaGiftAid = Some(false),
              oneOffDonationsViaGiftAidAmount = None,
              overseasDonationsViaGiftAid = Some(false),
              overseasDonationsViaGiftAidAmount = None,
              overseasCharityNames = Some(List()),
              addDonationToLastYear = Some(false),
              addDonationToLastYearAmount = None
            ))
        }
      }

      "this does not complete the cya data" should {
        lazy val result = postResult(url, None, None, Map(YesNoForm.yesNo -> YesNoForm.no))

        "redirect the user to the 'Add donations to this tax year' page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.DonationsToPreviousTaxYearController.show(year, year)}"
        }

        "update the cya data" in {
          findGiftAidDb shouldBe
            Some(GiftAidCYAModel(
              donationsViaGiftAid = Some(false),
              oneOffDonationsViaGiftAid = Some(false),
              overseasDonationsViaGiftAid = Some(false),
              addDonationToLastYear = Some(false)
            ))
        }
      }
    }

    "the user has selected 'yes'" should {
      lazy val result = postResult(url, None, None, Map(YesNoForm.yesNo -> YesNoForm.yes))

      "redirect the user to the donated amount page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidDonatedAmountController.show(year)}"
      }

      "update the cya data" in {
        findGiftAidDb shouldBe Some(GiftAidCYAModel(donationsViaGiftAid = Some(true)))
      }
    }
  }
}