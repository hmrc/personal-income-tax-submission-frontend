/*
 * Copyright 2023 HM Revenue & Customs
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

package test.controllers.charity

import forms.YesNoForm
import models.charity.GiftAidCYAModel
import models.charity.prior.{GiftAidSubmissionModel, GiftsModel}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import test.utils.CharityITHelper

class GiftAidQualifyingSharesSecuritiesControllerISpec extends CharityITHelper {

  def url: String = s"$appUrl/$taxYear/charity/donation-of-shares-or-securities"

  object Selectors {
    val captionSelector = ".govuk-caption-l"
    val continueSelector = "#continue"
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
    val captionText = s"Donations to charity for 6 April $taxYearEOY to 5 April $taxYear"
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
    val captionText = s"Rhoddion i elusennau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val yesText = "Iawn"
    val noText = "Na"
    val continueText = "Yn eich blaen"
    val disclosureContentTitle = "Beth yw cyfranddaliadau a gwarantau cymwys?"
    val disclosureContentParagraph = "Mae cyfranddaliadau a gwarantau cymwys yn cynnwys:"
    val disclosureContentBullet1 = "cael eu restri ar gyfnewidfa stoc gydnabyddedig neu yr ymdrinnir â nhw ar farchnad ddynodedig yn y DU"
    val disclosureContentBullet2 = "unedau mewn ymddiriedolaeth unedol awdurdodedig"
    val disclosureContentBullet3 = "cyfranddaliadau mewn cwmni buddsoddi penagored"
    val disclosureContentBullet4 = "buddiant mewn cronfa alltraeth"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1 = "Did you donate qualifying shares or securities to charity?"
    val expectedTitle = "Did you donate qualifying shares or securities to charity?"
    val expectedError: String = "Select yes if you donated shares or securities to charity"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1 = "Did your client donate qualifying shares or securities to charity?"
    val expectedTitle = "Did your client donate qualifying shares or securities to charity?"
    val expectedError: String = "Select yes if your client donated shares or securities to charity"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1 = "A wnaethoch roi cyfranddaliadau neu warantau cymwys i elusen?"
    val expectedTitle = "A wnaethoch roi cyfranddaliadau neu warantau cymwys i elusen?"
    val expectedError: String = "Dewiswch ‘Iawn’ os wnaethoch roi cyfranddaliadau neu warantau cymwys i elusen"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1 = "A wnaeth eich cleient rhoi cyfranddaliadau neu warantau cymwys i elusen?"
    val expectedTitle = "A wnaeth eich cleient rhoi cyfranddaliadau neu warantau cymwys i elusen?"
    val expectedError: String = "Dewiswch ‘Iawn’ os wnaeth eich cleient rhoi cyfranddaliadau neu warantau cymwys i elusen"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  val requiredSessionModel: GiftAidCYAModel = GiftAidCYAModel(addDonationToThisYear = Some(false))
  val requiredSessionData: Option[GiftAidCYAModel] = Some(requiredSessionModel)

  val requiredSessionModelPrefill: GiftAidCYAModel = GiftAidCYAModel(
    addDonationToThisYear = Some(false),
    donatedSharesOrSecurities = Some(true),
    donatedLandOrProperty = Some(false)
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
          textOnPageCheck(disclosureContentTitle, disclosureSelectorTitle)
          textOnPageCheck(disclosureContentParagraph, disclosureSelectorParagraph)
          textOnPageCheck(disclosureContentBullet1, disclosureSelectorBullet1)
          textOnPageCheck(disclosureContentBullet2, disclosureSelectorBullet2)
          textOnPageCheck(disclosureContentBullet3, disclosureSelectorBullet3)
          textOnPageCheck(disclosureContentBullet4, disclosureSelectorBullet4)
          buttonCheck(continueText, continueSelector)
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
          radioButtonHasChecked(yesText, 1)
          textOnPageCheck(disclosureContentTitle, disclosureSelectorTitle)
          textOnPageCheck(disclosureContentParagraph, disclosureSelectorParagraph)
          textOnPageCheck(disclosureContentBullet1, disclosureSelectorBullet1)
          textOnPageCheck(disclosureContentBullet2, disclosureSelectorBullet2)
          textOnPageCheck(disclosureContentBullet3, disclosureSelectorBullet3)
          textOnPageCheck(disclosureContentBullet4, disclosureSelectorBullet4)
          buttonCheck(continueText, continueSelector)
          noErrorsCheck()
        }
      }
    }

    "there is no cya data" should {
      lazy val result = getResult(url, None, None)

      "redirect to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe overviewUrl
      }
    }

    "there is priorData for sharesOrSecurities" should {
      val priorData = IncomeSourcesModel(
        giftAid = Some(
          GiftAidSubmissionModel(gifts = Some(
            GiftsModel(
              sharesOrSecurities = Some(100.00)
            )
          ))
        )
      )

      lazy val result = getResult(url, requiredSessionData, Some(priorData))

      "redirect to the check your answers page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe cyaUrl(taxYear)
      }
    }

    "there is no addDonationToThisYear" should {
      val cyaModel = completeGiftAidCYAModel.copy(addDonationToThisYear = None)
      lazy val result = getResult(url, Some(cyaModel), None)

      "redirect to the donations to previous tax year page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, otherTaxYear = taxYear)}"
      }
    }

    "there is no addDonationToThisYearAmount" should {
      val cyaModel = GiftAidCYAModel(addDonationToThisYear = Some(true), addDonationToThisYearAmount = None)
      lazy val result = getResult(url, Some(cyaModel), None)

      "redirect to the donations to previous tax year amount page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidAppendNextYearTaxAmountController.show(taxYear, someTaxYear = taxYear)}"
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
          errorSummaryCheck(user.specificExpectedResults.get.expectedError, errorSummaryHref, user.isWelsh)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedError)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }
      }
    }

    "the user submits 'yes'" should {
      lazy val result = postResult(url, requiredSessionData, None, Map(YesNoForm.yesNo -> YesNoForm.yes))

      "redirect to the 'shares, securities amount' page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidTotalShareSecurityAmountController.show(taxYear)}"
      }

      "update the cya data" in {
        findGiftAidDb shouldBe Some(requiredSessionModel.copy(donatedSharesOrSecurities = Some(true)))
      }
    }

    "the user submits 'no'" when {
      "this completes the cya data" should {
        lazy val result = postResult(url, Some(completeGiftAidCYAModel), None, Map(YesNoForm.yesNo -> YesNoForm.no))

        "redirect to the 'check your answers' page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe cyaUrl(taxYear)
        }

        "update the cya data" in {
          findGiftAidDb shouldBe
            Some(completeGiftAidCYAModel.copy(
              donatedSharesOrSecurities = Some(false),
              donatedSharesOrSecuritiesAmount = None)
            )
        }
      }

      "the user answered no to land or property" should {
        lazy val result = postResult(
          url, Some(requiredSessionModelPrefill.copy(donatedSharesOrSecurities = Some(false))), None, Map(YesNoForm.yesNo -> YesNoForm.no)
        )

        "redirect to the cya page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe cyaUrl(taxYear)
        }
      }

      "this does not complete the cya data" should {
        lazy val result = postResult(url, requiredSessionData, None, Map(YesNoForm.yesNo -> YesNoForm.no))

        "redirect to the 'land or property donation' page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidDonateLandOrPropertyController.show(taxYear)}"
        }

        "update the cya data" in {
          findGiftAidDb shouldBe Some(requiredSessionModel.copy(donatedSharesOrSecurities = Some(false)))
        }
      }
    }

    "there is no cya data " should {
      lazy val result = postResult(url, None, None, Map(YesNoForm.yesNo -> YesNoForm.no))

      "redirect to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe overviewUrl
      }
    }

    "there is prior data " should {
      val priorData = IncomeSourcesModel(
        giftAid = Some(
          GiftAidSubmissionModel(gifts = Some(
            GiftsModel(
              sharesOrSecurities = Some(100.00)
            )
          ))
        )
      )
      lazy val result = postResult(url, requiredSessionData, Some(priorData), Map(YesNoForm.yesNo -> YesNoForm.no))

      "redirect to the cya page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe cyaUrl(taxYear)
      }
    }
  }
}
