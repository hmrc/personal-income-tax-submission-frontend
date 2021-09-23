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
import utils.CharityITHelper

class GiftAidOneOffControllerISpec extends CharityITHelper {

  val giftAidDonations = 100

  def url: String = s"$appUrl/$year/charity/one-off-charity-donations"

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
    val captionText = s"Donations to charity for 6 April ${year - 1} to 5 April $year"
    val yesText = "Yes"
    val noText = "No"
    val continueText = "Continue"
    val continueLink = s"/income-through-software/return/personal-income/$year/charity/one-off-charity-donations"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val captionText = s"Rhoddion i elusennau ar gyfer 6 Ebrill ${year - 1} i 5 Ebrill $year"
    val yesText = "Iawn"
    val noText = "Na"
    val continueText = "Yn eich blaen"
    val continueLink = s"/income-through-software/return/personal-income/$year/charity/one-off-charity-donations"
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
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorText = "Select yes if your client made a one-off donation to charity"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1 = "A wnaethoch roddion untro?"
    val expectedTitle = "A wnaethoch roddion untro?"
    val expectedPara1= s"Gwnaethoch roi gwybod i ni eich bod wedi defnyddio Rhodd Cymorth i roi £$giftAidDonations i elusen." +
      s" Rhowch wybod i ni a wnaed unrhyw ran o hyn fel taliadau untro."
    val expectedPara2 = "Taliadau na wnaethoch eu hailadrodd yw rhoddion untro."
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedErrorText = "Dewiswch ‘Iawn’ os gwnaethoch rodd untro i elusen"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1 = "A wnaeth eich cleient roddion untro?"
    val expectedTitle = "A wnaeth eich cleient roddion untro?"
    val expectedPara1 = s"Gwnaethoch roi gwybod i ni fod eich cleient wedi defnyddio Rhodd Cymorth i roi £$giftAidDonations i elusen." +
      s" Rhowch wybod i ni a wnaed unrhyw ran o hyn fel taliadau untro."
    val expectedPara2 = "Taliadau na wnaeth eich cleient eu hailadrodd yw rhoddion untro."
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedErrorText = "Dewiswch ‘Iawn’ os gwnaeth eich cleient rhodd untro i elusen"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  val requiredSessionModel: GiftAidCYAModel = GiftAidCYAModel(donationsViaGiftAid = Some(true), donationsViaGiftAidAmount = Some(BigDecimal(giftAidDonations)))
  val requiredSessionData: Some[GiftAidCYAModel] = Some(requiredSessionModel)

  val requiredSessionModelPrefill: GiftAidCYAModel = GiftAidCYAModel(
    donationsViaGiftAid = Some(true),
    donationsViaGiftAidAmount = Some(BigDecimal(giftAidDonations)),
    oneOffDonationsViaGiftAid = Some(true)
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
          h1Check(captionText + " " + user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(captionText, captionSelector)
          hintTextCheck(s"${user.specificExpectedResults.get.expectedPara1} ${user.specificExpectedResults.get.expectedPara2}")
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
        }

        "render the page with correct content and with prefilled CYA data" which {
          lazy val result = getResult(url, requiredSessionDataPrefill, None, user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          welshToggleCheck(user.isWelsh)
          h1Check(captionText + " " + user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(captionText, captionSelector)
          hintTextCheck(s"${user.specificExpectedResults.get.expectedPara1} ${user.specificExpectedResults.get.expectedPara2}")
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          radioButtonHasChecked(yesText, 1)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
        }
      }
    }

    "there is no cya data stored" should {
      lazy val result = getResult(url, None, None)

      "redirect the user to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${appConfig.incomeTaxSubmissionOverviewUrl(year)}"
      }
    }

    "there is prior 'One off' data" should {
      val priorData = IncomeSourcesModel(
        giftAid = Some(
          GiftAidSubmissionModel(Some(
            GiftAidPaymentsModel(
              oneOffCurrentYear = Some(1000.00)
            )
          ))
        )
      )
      lazy val result = getResult(url, requiredSessionData, Some(priorData))

      "redirect the user to the CYA page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(year)}"
      }
    }

    "'donated amount' does not exist in cya data" should {
      lazy val result = getResult(url, Some(GiftAidCYAModel(donationsViaGiftAid = Some(true))), None)

      "redirect the user to the 'donated amount' page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidDonatedAmountController.show(year)}"
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
          h1Check(captionText + " " + user.specificExpectedResults.get.expectedH1)
          textOnPageCheck(captionText, captionSelector)
          hintTextCheck(s"${user.specificExpectedResults.get.expectedPara1} ${user.specificExpectedResults.get.expectedPara2}")
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

    "prior data exists for oneOffCurrentYear" should {
      val priorData = IncomeSourcesModel(
        giftAid = Some(
          GiftAidSubmissionModel(Some(
            GiftAidPaymentsModel(
              oneOffCurrentYear = Some(1000.00)
            )
          ))
        )
      )
      lazy val result = postResult(url, None, Some(priorData), Map(YesNoForm.yesNo -> YesNoForm.yes))

      "redirect the user to the CYA page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(year)}"
      }
    }

    "there is no cya data" should {
      lazy val result = postResult(url, None, None, Map(YesNoForm.yesNo -> YesNoForm.yes))

      "redirect the user to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${appConfig.incomeTaxSubmissionOverviewUrl(year)}"
      }
    }

    "there is cya data stored" when {

      "the user has selected 'yes'" should {
        lazy val result = postResult(url, requiredSessionData, None, Map(YesNoForm.yesNo -> YesNoForm.yes))

        "redirect the user to the 'One off amount' page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOneOffAmountController.show(year)}"
        }

        "update the cya data" in {
          findGiftAidDb shouldBe Some(requiredSessionModel.copy(oneOffDonationsViaGiftAid = Some(true)))
        }
      }

      "the user has selected 'no'" when {

        "this completes the cya data" should {
          lazy val result =
            postResult(url, Some(completeGiftAidCYAModel.copy(oneOffDonationsViaGiftAid = None)), None, Map(YesNoForm.yesNo -> YesNoForm.no))

          "redirect the user to the 'check your answers' page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(year)}"
          }

          "update the cya data" in {
            findGiftAidDb shouldBe
              Some(completeGiftAidCYAModel.copy(oneOffDonationsViaGiftAid = Some(false), oneOffDonationsViaGiftAidAmount = None))
          }
        }

        "this does not complete the cya data" should {
          lazy val result =
            postResult(url, requiredSessionData, None, Map(YesNoForm.yesNo -> YesNoForm.no))

          "redirect the user to the 'overseas donations' page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.OverseasGiftAidDonationsController.show(year)}"
          }

          "update the cya data" in {
            findGiftAidDb shouldBe Some(requiredSessionModel.copy(oneOffDonationsViaGiftAid = Some(false)))
          }
        }
      }
    }
  }
}