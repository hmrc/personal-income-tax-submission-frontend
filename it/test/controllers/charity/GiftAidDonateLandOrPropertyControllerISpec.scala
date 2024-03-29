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

class GiftAidDonateLandOrPropertyControllerISpec extends CharityITHelper {

  def url: String = s"$appUrl/$taxYear/charity/donation-of-land-or-property"

  object Selectors {
    val captionSelector = ".govuk-caption-l"
    val continueSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val errorSummaryHref = "#value"
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedError: String
    val expectedErrorTitle: String
  }

  trait CommonExpectedResults {
    val captionText: String
    val yesText: String
    val noText: String
    val continueText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val captionText = s"Donations to charity for 6 April $taxYearEOY to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val continueText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val captionText = s"Rhoddion i elusennau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val yesText = "Iawn"
    val noText = "Na"
    val continueText = "Yn eich blaen"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1 = "Did you donate land or property to charity?"
    val expectedTitle = "Did you donate land or property to charity?"
    val expectedError: String = "Select yes if you donated land or property to charity"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "Did your client donate land or property to charity?"
    val expectedH1: String = "Did your client donate land or property to charity?"
    val expectedError: String = "Select yes if your client donated land or property to charity"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1 = "A wnaethoch roi tir neu eiddo i elusen?"
    val expectedTitle = "A wnaethoch roi tir neu eiddo i elusen?"
    val expectedError: String = "Dewiswch ‘Iawn’ os wnaethoch roi tir neu eiddo i elusen"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "A wnaeth eich cleient rhoi tir neu eiddo i elusen?"
    val expectedH1: String = "A wnaeth eich cleient rhoi tir neu eiddo i elusen?"
    val expectedError: String = "Dewiswch ‘Iawn’ os wnaeth eich cleient rhoi tir neu eiddo i elusen"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  val requiredSessionModel: GiftAidCYAModel = GiftAidCYAModel(donatedSharesOrSecurities = Some(false))
  val requiredSessionData: Option[GiftAidCYAModel] = Some(requiredSessionModel)

  val requiredSessionModelPrefill: GiftAidCYAModel = GiftAidCYAModel(
    donatedSharesOrSecurities = Some(true),
    donatedSharesOrSecuritiesAmount = Some(50.00),
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
          h1Check(user.specificExpectedResults.get.expectedH1 + " " + captionText)
          textOnPageCheck(captionText, captionSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(continueText, continueSelector)
          noErrorsCheck()
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
          h1Check(user.specificExpectedResults.get.expectedH1 + " " + captionText)
          textOnPageCheck(captionText, captionSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          radioButtonHasChecked(noText, 2)
          buttonCheck(continueText, continueSelector)
          noErrorsCheck()
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "there is no cya data" should {
      lazy val result = getResult(url, None, None)

      "redirects to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe overviewUrl
      }
    }

    "there is no donatedSharesOrSecurities" should {
      lazy val result = getResult(url, Some(completeGiftAidCYAModel.copy(donatedSharesOrSecurities = None)), None)

      "redirect to the sharesOrSecurities page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidQualifyingSharesSecuritiesController.show(taxYear)}"
      }
    }

    "there is donatedSharesOrSecurities, but no sharesOrSecuritiesAmount" should {
      lazy val result = getResult(url, Some(GiftAidCYAModel(donatedSharesOrSecurities = Some(true))), None)


      "redirect to the sharesOrSecuritiesAmount page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidTotalShareSecurityAmountController.show(taxYear)}"
      }
    }

    "return the check your answers page when there is prior data" which {
      val priorData = IncomeSourcesModel(
        giftAid = Some(
          GiftAidSubmissionModel(
            gifts = Some(GiftsModel(landAndBuildings = Some(50)))
          )
        )
      )

      lazy val result = getResult(url, requiredSessionData, Some(priorData))

      "redirect to the checkYourAnswers page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(taxYear)}"
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

          titleCheck(user.specificExpectedResults.get.expectedErrorTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1 + " " + captionText)
          textOnPageCheck(captionText, captionSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(continueText, continueSelector)
          welshToggleCheck(user.isWelsh)
          errorSummaryCheck(user.specificExpectedResults.get.expectedError, Selectors.errorSummaryHref, user.isWelsh)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedError)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }
      }
    }

    "the user has selected 'yes'" should {
      lazy val result = postResult(url, requiredSessionData, None, Map(YesNoForm.yesNo -> YesNoForm.yes))

      "redirect to the land and property amount" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe
          controllers.charity.routes.GiftAidLandOrPropertyAmountController.show(taxYear).url
      }

      "update the cya data" in {
        findGiftAidDb shouldBe Some(requiredSessionModel.copy(donatedLandOrProperty = Some(true)))
      }
    }

    "the user has selected 'no'" when {
      "this completes the cya model" should {
        lazy val result = postResult(url, Some(completeGiftAidCYAModel), None, Map(YesNoForm.yesNo -> YesNoForm.no))

        "redirect to the cya page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe cyaUrl(taxYear)
        }

        "update the cya data" in {
          findGiftAidDb shouldBe
            Some(completeGiftAidCYAModel.copy(
              donatedLandOrProperty = Some(false),
              donatedLandOrPropertyAmount = None
            ))
        }
      }

      "the user answered no to shares or securities" should {
        lazy val result = postResult(url, requiredSessionData, None, Map(YesNoForm.yesNo -> YesNoForm.no))

        "redirect to the cya page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe cyaUrl(taxYear)
        }
      }

      "this does not complete the cya data" should {
        lazy val result = postResult(url, requiredSessionDataPrefill, None, Map(YesNoForm.yesNo -> YesNoForm.no))

        "redirect to the overseas shares,securities, land or property page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidSharesSecuritiesLandPropertyOverseasController.show(taxYear)}"
        }

        "update the cya data" in {
          findGiftAidDb shouldBe Some(requiredSessionModelPrefill.copy(donatedLandOrProperty = Some(false)))
        }
      }
    }

    "there is no cya data" should {
      lazy val result = postResult(url, None, None, Map(YesNoForm.yesNo -> YesNoForm.no))

      "redirect to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe overviewUrl
      }
    }

    "there is prior data for landOrProperty" should {
      val priorData = IncomeSourcesModel(
        giftAid = Some(
          GiftAidSubmissionModel(
            gifts = Some(GiftsModel(landAndBuildings = Some(50)))
          )
        )
      )

      lazy val result = postResult(url, requiredSessionData, Some(priorData), Map(YesNoForm.yesNo -> YesNoForm.no))

      "redirect to the cya page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(taxYear)}"
      }
    }
  }
}