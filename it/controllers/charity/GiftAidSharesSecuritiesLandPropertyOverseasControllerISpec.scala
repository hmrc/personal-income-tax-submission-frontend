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
import models.charity.prior.{GiftAidSubmissionModel, GiftsModel}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import utils.CharityITHelper

class GiftAidSharesSecuritiesLandPropertyOverseasControllerISpec extends CharityITHelper {

  def url: String = s"$appUrl/$year/charity/donation-of-shares-securities-land-or-property-to-overseas-charities"

  object Selectors {
    val captionSelector = ".govuk-caption-l"
    val continueSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
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
    val captionText = s"Donations to charity for 6 April ${year - 1} to 5 April $year"
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
    val captionText = s"Rhoddion i elusennau ar gyfer 6 Ebrill ${year - 1} i 5 Ebrill $year"
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
    val expectedTitle = "Did you donate qualifying shares, securities, land or property to overseas charities?"
    val expectedH1 = "Did you donate qualifying shares, securities, land or property to overseas charities?"
    val expectedError = "Select yes if you donated shares, securities, land or property to overseas charities"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client donate qualifying shares, securities, land or property to overseas charities?"
    val expectedH1 = "Did your client donate qualifying shares, securities, land or property to overseas charities?"
    val expectedError = "Select yes if your client donated shares, securities, land or property to overseas charities"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "A wnaethoch roi cyfranddaliadau, gwarantau, tir neu eiddo cymwys i elusennau tramor?"
    val expectedH1 = "A wnaethoch roi cyfranddaliadau, gwarantau, tir neu eiddo cymwys i elusennau tramor?"
    val expectedError = "Dewiswch ‘Iawn’ os wnaethoch roi cyfranddaliadau, gwarantau, tir neu eiddo cymwys i elusennau tramor"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "A wnaeth eich cleient rhoi cyfranddaliadau, gwarantau, tir neu eiddo cymwys i elusennau tramor?"
    val expectedH1 = "A wnaeth eich cleient rhoi cyfranddaliadau, gwarantau, tir neu eiddo cymwys i elusennau tramor?"
    val expectedError = "Dewiswch ‘Iawn’ os wnaeth eich cleient rhoi cyfranddaliadau, gwarantau, tir neu eiddo cymwys i elusennau tramor"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  val amount: Int = 2000
  val charity: String = "Mind"

  val requiredSessionModel: GiftAidCYAModel = GiftAidCYAModel(donatedLandOrProperty = Some(false))
  val requiredSessionData: Option[GiftAidCYAModel] = Some(requiredSessionModel)

  val requiredSessionModelPrefill: GiftAidCYAModel = GiftAidCYAModel(
    donatedLandOrProperty = Some(false),
    overseasDonatedSharesSecuritiesLandOrProperty = Some(false)

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
          textOnPageCheck(disclosureContentTitle, disclosureSelectorTitle)
          textOnPageCheck(disclosureContentParagraph, disclosureSelectorParagraph)
          textOnPageCheck(disclosureContentBullet1, disclosureSelectorBullet1)
          textOnPageCheck(disclosureContentBullet2, disclosureSelectorBullet2)
          textOnPageCheck(disclosureContentBullet3, disclosureSelectorBullet3)
          textOnPageCheck(disclosureContentBullet4, disclosureSelectorBullet4)
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
          textOnPageCheck(disclosureContentTitle, disclosureSelectorTitle)
          textOnPageCheck(disclosureContentParagraph, disclosureSelectorParagraph)
          textOnPageCheck(disclosureContentBullet1, disclosureSelectorBullet1)
          textOnPageCheck(disclosureContentBullet2, disclosureSelectorBullet2)
          textOnPageCheck(disclosureContentBullet3, disclosureSelectorBullet3)
          textOnPageCheck(disclosureContentBullet4, disclosureSelectorBullet4)
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

    "there is prior data" should {
      val priorData = IncomeSourcesModel(
        giftAid = Some(
          GiftAidSubmissionModel(gifts = Some(
            GiftsModel(
              investmentsNonUkCharities = Some(100.00)
            )
          ))
        )
      )
      lazy val result = getResult(url, requiredSessionData, Some(priorData))

      "redirects to the checkYourAnswers page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe cyaUrl(year)
      }
    }

    "there is no donatedLandOrProperty" should {
      lazy val result = getResult(url, Some(GiftAidCYAModel(donatedSharesOrSecurities = Some(false))), None)

      "redirect to the LandOrProperty page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidDonateLandOrPropertyController.show(year)}"
      }
    }

    "there is donatedLandOrProperty, but no corresponding amount" should {
      lazy val result = getResult(url, Some(GiftAidCYAModel(donatedLandOrProperty = Some(true))), None)

      "redirect to the LandOrPropertyAmount page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidLandOrPropertyAmountController.show(year)}"
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
          textOnPageCheck(disclosureContentTitle, disclosureSelectorTitle)
          textOnPageCheck(disclosureContentParagraph, disclosureSelectorParagraph)
          textOnPageCheck(disclosureContentBullet1, disclosureSelectorBullet1)
          textOnPageCheck(disclosureContentBullet2, disclosureSelectorBullet2)
          textOnPageCheck(disclosureContentBullet3, disclosureSelectorBullet3)
          textOnPageCheck(disclosureContentBullet4, disclosureSelectorBullet4)
          errorSummaryCheck(user.specificExpectedResults.get.expectedError, errorSummaryHref, user.isWelsh)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedError)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }
      }
    }

    "there is form data (no)" should {
      lazy val result = postResult(url, requiredSessionData, None, Map(YesNoForm.yesNo -> YesNoForm.no))

      "redirect to the cya page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe cyaUrl(year)
      }

      "update the cya data" in {
        findGiftAidDb shouldBe
          Some(requiredSessionModel.copy(
            overseasDonatedSharesSecuritiesLandOrProperty = Some(false),
            overseasDonatedSharesSecuritiesLandOrPropertyAmount = None,
            overseasDonatedSharesSecuritiesLandOrPropertyCharityNames = Seq.empty
          ))
      }
    }

    "there is form data (yes)" should {
      lazy val result = postResult(url, requiredSessionData, None, Map(YesNoForm.yesNo -> YesNoForm.yes))

      "redirect to the 'overseas SSLP donations amount' page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe controllers.charity.routes.OverseasSharesSecuritiesLandPropertyAmountController.show(year).url
      }

      "update the cya data" in {
        findGiftAidDb shouldBe Some(requiredSessionModel.copy(overseasDonatedSharesSecuritiesLandOrProperty = Some(true)))
      }
    }

    "there is no data" should {
      lazy val result = postResult(url, None, None, Map(YesNoForm.yesNo -> YesNoForm.yes))

      "redirect to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe overviewUrl
      }
    }

    "there is prior data" should {
      val priorData = IncomeSourcesModel(
        giftAid = Some(
          GiftAidSubmissionModel(gifts = Some(
            GiftsModel(
              investmentsNonUkCharities = Some(100.00)
            )
          ))
        )
      )

      lazy val result = postResult(url, requiredSessionData, Some(priorData), Map(YesNoForm.yesNo -> YesNoForm.yes))

      "redirect to the cya page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe cyaUrl(year)
      }
    }
  }
}