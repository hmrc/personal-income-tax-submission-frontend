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
import models.charity.prior.{GiftAidSubmissionModel, GiftsModel}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import utils.CharityITHelper

class GiftAidQualifyingSharesSecuritiesControllerISpec extends CharityITHelper {

  def url: String = s"$appUrl/$year/charity/donation-of-shares-or-securities"

  object Selectors {
    val captionSelector = ".govuk-caption-l"
    val continueSelector = "#continue"
    val errorSummaryHref = "#value"
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
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val captionText = s"Donations to charity for 6 April ${year - 1} to 5 April $year"
    val yesText = "Yes"
    val noText = "No"
    val continueText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val captionText = s"Rhoddion i elusennau ar gyfer 6 Ebrill ${year - 1} i 5 Ebrill $year"
    val yesText = "Iawn"
    val noText = "Na"
    val continueText = "Yn eich blaen"
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

  val requiredSessionModel: GiftAidCYAModel = GiftAidCYAModel(donatedSharesSecuritiesLandOrProperty = Some(true))
  val requiredSessionData: Option[GiftAidCYAModel] = Some(requiredSessionModel)

  val requiredSessionModelPrefill: GiftAidCYAModel = GiftAidCYAModel(
    donatedSharesSecuritiesLandOrProperty = Some(true),
    donatedSharesOrSecurities = Some(true)
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
        result.headers("Location").head shouldBe cyaUrl(year)
      }
    }

    "the previous value is false" should {
      lazy val result = getResult(url, Some(GiftAidCYAModel(donatedSharesSecuritiesLandOrProperty = Some(false))), None)

      "has a status of SEE_OTHER(303)" in {
        result.status shouldBe SEE_OTHER
      }

      "redirect to the check your answers page" in {
        result.headers("Location").head shouldBe cyaUrl(year)
      }
    }

    "there is no addDonationToThisYearAmount" should {
      val cyaModel = GiftAidCYAModel(addDonationToThisYear = Some(true), addDonationToThisYearAmount = Some(100.00))
      lazy val result = getResult(url, Some(cyaModel), None)

      "redirect to the giftAidSharesSecuritiesLandPropertyDonationController page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidSharesSecuritiesLandPropertyDonationController.show(year)}"
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
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidTotalShareSecurityAmountController.show(year)}"
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
          result.headers("Location").head shouldBe cyaUrl(year)
        }

        "update the cya data" in {
          findGiftAidDb shouldBe
            Some(completeGiftAidCYAModel.copy(
              donatedSharesOrSecurities = Some(false),
              donatedSharesOrSecuritiesAmount = None)
            )
        }
      }

      "removes all donated shares security and land or property" should {
        val model = completeGiftAidCYAModel.copy(donatedLandOrProperty = Some(false), donatedLandOrPropertyAmount = None)
        lazy val result = postResult(url, Some(model), None, Map(YesNoForm.yesNo -> YesNoForm.no))

        "redirect to the 'check your answers' page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe cyaUrl(year)
        }

        "update the cya data" in {
          findGiftAidDb shouldBe
            Some(completeGiftAidCYAModel.copy(
              donatedSharesSecuritiesLandOrProperty = Some(false),
              donatedSharesOrSecurities = None,
              donatedSharesOrSecuritiesAmount = None,
              donatedLandOrProperty = None,
              donatedLandOrPropertyAmount = None,
              overseasDonatedSharesSecuritiesLandOrProperty = None,
              overseasDonatedSharesSecuritiesLandOrPropertyAmount = None,
              overseasDonatedSharesSecuritiesLandOrPropertyCharityNames = Some(Seq.empty[String])
            ))
        }
      }

      "this does not complete the cya data" should {
        lazy val result = postResult(url, requiredSessionData, None, Map(YesNoForm.yesNo -> YesNoForm.no))

        "redirect to the 'land or property donation' page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidDonateLandOrPropertyController.show(year)}"
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
        result.headers("Location").head shouldBe cyaUrl(year)
      }
    }
  }
}