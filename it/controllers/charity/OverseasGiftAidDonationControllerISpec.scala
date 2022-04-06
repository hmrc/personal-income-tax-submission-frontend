/*
 * Copyright 2022 HM Revenue & Customs
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
import utils.CharityITHelper

class OverseasGiftAidDonationControllerISpec extends CharityITHelper {

  def url: String = s"$appUrl/$taxYear/charity/overseas-charity-donations-using-gift-aid"

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
    val captionText = s"Donations to charity for 6 April $taxYearEOY to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val continueText = "Continue"
    val continueLink = s"/update-and-submit-income-tax-return/personal-income/$taxYear/charity/overseas-charity-donations-using-gift-aid"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val captionText = s"Rhoddion i elusennau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val yesText = "Iawn"
    val noText = "Na"
    val continueText = "Yn eich blaen"
    val continueLink = s"/update-and-submit-income-tax-return/personal-income/$taxYear/charity/overseas-charity-donations-using-gift-aid"
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
    val expectedH1 = "A wnaethoch ddefnyddio Rhodd Cymorth i roi rhodd i elusen o dramor?"
    val expectedTitle = "A wnaethoch ddefnyddio Rhodd Cymorth i roi rhodd i elusen o dramor?"
    val expectedErrorText = "Dewiswch ‘Iawn’ os gwnaethoch ddefnyddio Rhodd Cymorth i roi rhodd i elusen o dramor"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1 = "A wnaeth eich cleient ddefnyddio Rhodd Cymorth i roi rhodd i elusen o dramor?"
    val expectedTitle = "A wnaeth eich cleient ddefnyddio Rhodd Cymorth i roi rhodd i elusen o dramor?"
    val expectedErrorText = "Dewiswch ‘Iawn’ os gwnaeth eich cleient ddefnyddio Rhodd Cymorth i roi rhodd i elusen o dramor"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  val validAmount = 50

  val requiredSessionModel: GiftAidCYAModel = GiftAidCYAModel(oneOffDonationsViaGiftAid = Some(false))
  val requiredSessionData: Option[GiftAidCYAModel] = Some(requiredSessionModel)

  val requiredSessionModelPrefill: GiftAidCYAModel = GiftAidCYAModel(
    oneOffDonationsViaGiftAid = Some(false),
    overseasDonationsViaGiftAid = Some(false)
  )
  val requiredSessionDataPrefill: Option[GiftAidCYAModel] = Some(requiredSessionModelPrefill)

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
          welshToggleCheck(user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1 + " " + captionText)
          textOnPageCheck(captionText, captionSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          noErrorsCheck()
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
          welshToggleCheck(user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1 + " " + captionText)
          textOnPageCheck(captionText, captionSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          radioButtonHasChecked(noText, 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          noErrorsCheck()
        }
      }
    }

    "there is no cya data stored" should {
      lazy val result = getResult(url, None, None)

      "redirect the user to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe overviewUrl
      }
    }

    "there is prior overseas donations data" should {
      val priorData = IncomeSourcesModel(giftAid = Some(
        GiftAidSubmissionModel(Some(
          GiftAidPaymentsModel(
            nonUkCharities = Some(validAmount)
          )
        ))
      ))
      lazy val result = getResult(url, requiredSessionData, Some(priorData))

      "redirect the user to the CYA page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(taxYear)}"
      }
    }

    "'one off donations' does not exist in cya data" should {
      lazy val result = getResult(url, Some(GiftAidCYAModel(donationsViaGiftAidAmount = Some(validAmount))), None)

      "redirect the user to the 'one off donations' page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOneOffController.show(taxYear)}"
      }
    }

    "'one off donations' is true, but 'one off amount' does not exist in cya data" should {

      lazy val result =
        getResult(
          url,
          Some(GiftAidCYAModel(oneOffDonationsViaGiftAid = Some(true), donationsViaGiftAidAmount = Some(validAmount))),
          None
        )

      "redirect the user to the 'one off amount' page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOneOffAmountController.show(taxYear)}"
      }
    }


  }

  ".submit" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "no radio button has been selected" should {
          lazy val result = postResult(url, requiredSessionData, None, Map(YesNoForm.yesNo -> ""), user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          titleCheck(errorPrefix(user.isWelsh) + user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          welshToggleCheck(user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1 + " " + captionText)
          textOnPageCheck(captionText, captionSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          errorSummaryCheck(user.specificExpectedResults.get.expectedErrorText, errorSummaryHref, user.isWelsh)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorText)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }
      }
    }

    "prior data exists for nonUkCharities" should {
      val priorData = IncomeSourcesModel(giftAid = Some(
        GiftAidSubmissionModel(Some(
          GiftAidPaymentsModel(
            nonUkCharities = Some(validAmount)
          )
        ))
      ))
      lazy val result = postResult(url, requiredSessionData, Some(priorData), Map(YesNoForm.yesNo -> YesNoForm.yes))

      "redirect the user to the CYA page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(taxYear)}"
      }
    }

    "there is no cya data" should {
      lazy val result = postResult(url, None, None, Map(YesNoForm.yesNo -> YesNoForm.yes))

      "redirect the user to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe overviewUrl
      }
    }

    "the user has selected 'Yes'" should {
      lazy val result = postResult(url, requiredSessionData, None, Map(YesNoForm.yesNo -> YesNoForm.yes))

      "redirect the user to the 'Overseas donation amount' page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOverseasAmountController.show(taxYear)}"
      }

      "update the cya data" in {
        findGiftAidDb shouldBe Some(requiredSessionModel.copy(overseasDonationsViaGiftAid = Some(true)))
      }
    }

    "the user selects 'no'" when {

      "this completes the cya model" should {
        lazy val result = postResult(url, Some(completeGiftAidCYAModel), None, Map(YesNoForm.yesNo -> YesNoForm.no))

        "redirect the user to the 'check your answers' page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe cyaUrl(taxYear)
        }

        "update the cya data" in {
          findGiftAidDb shouldBe
            Some(completeGiftAidCYAModel.copy(
              overseasDonationsViaGiftAid = Some(false),
              overseasDonationsViaGiftAidAmount = None,
              overseasCharityNames = Seq.empty
            ))
        }
      }

      "this does not complete the cya model" should {
        lazy val result = postResult(url, requiredSessionData, None, Map(YesNoForm.yesNo -> YesNoForm.no))

        "redirect the user to the 'Add donation to last tax year' page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidLastTaxYearController.show(taxYear)}"
        }

        "update the cya data" in {
          findGiftAidDb shouldBe Some(requiredSessionModel.copy(overseasDonationsViaGiftAid = Some(false)))
        }
      }
    }
  }
}