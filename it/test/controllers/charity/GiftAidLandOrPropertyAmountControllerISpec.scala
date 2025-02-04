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

import models.charity.GiftAidCYAModel
import models.charity.prior.{GiftAidSubmissionModel, GiftsModel}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import test.utils.CharityITHelper

class GiftAidLandOrPropertyAmountControllerISpec extends CharityITHelper {

  def url: String = s"$appUrl/$taxYear/charity/value-of-land-or-property"

  object Selectors {
    val expectedErrorLink = "#amount"
    val captionSelector = ".govuk-caption-l"
    val inputFieldSelector = "#amount"
    val buttonSelector = ".govuk-button"
    val inputLabelSelector = "#p1"
    val inputHintTextSelector = "#amount-hint"
  }

  val invalidAmount = "1000000000000"

  trait SpecificExpectedResults {
    val expectedErrorEmpty: String
    val expectedErrorInvalid: String
    val expectedErrorOverMax: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedHint: String
    val expectedInputName: String
    val expectedButtonText: String
    val expectedTitle: String
    val expectedHeading: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Donations to charity for 6 April $taxYearEOY to 5 April $taxYear"
    val expectedHint = "For example, £193.52"
    val expectedInputName = "amount"
    val expectedButtonText = "Continue"
    val expectedTitle = "What is the value of land or property donated to charity?"
    val expectedHeading = "What is the value of land or property donated to charity?"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Rhoddion i elusennau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val expectedHint = "Er enghraifft, £193.52"
    val expectedInputName = "amount"
    val expectedButtonText = "Yn eich blaen"
    val expectedTitle = "Beth yw gwerth tir neu eiddo a roddwyd i elusen?"
    val expectedHeading = "Beth yw gwerth tir neu eiddo a roddwyd i elusen?"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedErrorEmpty = "Enter the value of land or property you donated to charity"
    val expectedErrorInvalid = "Enter the value of land or property you donated to charity in the correct format"
    val expectedErrorOverMax = "The value of your land or property must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedErrorEmpty = "Enter the value of land or property your client donated to charity"
    val expectedErrorInvalid = "Enter the value of land or property your client donated to charity in the correct format"
    val expectedErrorOverMax = "The value of your client’s land or property must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedErrorEmpty = "Nodwch werth y tir neu eiddo a roddwyd gennych i elusen"
    val expectedErrorInvalid = "Nodwch werth y tir neu eiddo a roddwyd gennych i elusen yn y fformat cywir"
    val expectedErrorOverMax = "Mae’n rhaid i werth eich tir neu eiddo fod yn llai na £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedErrorEmpty = "Nodwch werth y tir neu eiddo a roddwyd gan eich cleient i elusen"
    val expectedErrorInvalid = "Nodwch werth y tir neu eiddo a roddwyd gan eich cleient i elusen yn y fformat cywir"
    val expectedErrorOverMax = "Mae’n rhaid i werth tir neu eiddo eich cleient fod yn llai na £100,000,000,000"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  val amount: Int = 2000

  val requiredSessionModel: GiftAidCYAModel = GiftAidCYAModel(donatedLandOrProperty = Some(true))
  val requiredSessionData: Option[GiftAidCYAModel] = Some(requiredSessionModel)

  val requiredSessionModelPrefill: GiftAidCYAModel = GiftAidCYAModel(
    donatedLandOrProperty = Some(true),
    donatedLandOrPropertyAmount = Some(amount)
  )

  val requiredSessionDataPrefill: Option[GiftAidCYAModel] = Some(requiredSessionModelPrefill)

  val requiredPriorData: Option[IncomeSourcesModel] = Some(IncomeSourcesModel(None, None, Some(priorDataMax)))

  val validAmount = 50

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

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(expectedHint, inputHintTextSelector)
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

          titleCheck(expectedTitle, user.isWelsh)
          h1Check(expectedHeading + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(expectedHint, inputHintTextSelector)
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

    "when there is no donatedLandOrProperty" should {
      lazy val result = getResult(url, Some(GiftAidCYAModel(donatedSharesOrSecurities = Some(false))), None)

      "redirect to the DonateLandOrProperty page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidDonateLandOrPropertyController.show(taxYear)}"
      }
    }

    "there is prior data for landAndBuildings" should {

      "display the GiftAidLandOrPropertyAmount page when the 'Change' link is clicked on the CYA page" which {

        val priorData: IncomeSourcesModel = IncomeSourcesModel(None, None,
          giftAid = Some(GiftAidSubmissionModel(None, Some(GiftsModel(landAndBuildings = Some(1000.21)))))
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
            lazy val result = postResult(url, requiredSessionData, None, Map("amount" -> ""), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(errorPrefix(user.isWelsh) + expectedTitle, user.isWelsh)
            h1Check(expectedHeading + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(expectedHint, inputHintTextSelector)
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, buttonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorEmpty, Selectors.expectedErrorLink, user.isWelsh)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorEmpty)
          }

          "the submitted data is too long" which {

            lazy val result = postResult(url, requiredSessionData, None, Map("amount" -> "99999999999999"), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(errorPrefix(user.isWelsh) + expectedTitle, user.isWelsh)
            h1Check(expectedHeading + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(expectedHint, inputHintTextSelector)
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

            titleCheck(errorPrefix(user.isWelsh) + expectedTitle, user.isWelsh)
            h1Check(expectedHeading + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(expectedHint, inputHintTextSelector)
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, buttonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorInvalid, Selectors.expectedErrorLink, user.isWelsh)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorInvalid)
          }
        }
      }
    }

    "the form is valid and doesn't complete the cya model" should {
      lazy val result = postResult(url, requiredSessionData, None, Map("amount" -> s"$validAmount"))

      "redirect to the SSLP overseas page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe
          controllers.charity.routes.GiftAidSharesSecuritiesLandPropertyOverseasController.show(taxYear).url
      }

      "update the cya data" in {
        findGiftAidDb shouldBe Some(requiredSessionModel.copy(donatedLandOrPropertyAmount = Some(validAmount)))
      }
    }

    "the form is valid and completes the cya model" should {
      lazy val result = postResult(url, Some(completeGiftAidCYAModel), None, Map("amount" -> s"$validAmount"))

      "redirect to the cya page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe cyaUrl(taxYear)
      }

      "update the cya data" in {
        findGiftAidDb shouldBe Some(completeGiftAidCYAModel.copy(donatedLandOrPropertyAmount = Some(validAmount)))
      }
    }

    "there is no cya data" should {
      lazy val result = postResult(url, None, None, Map("amount" -> s"$validAmount"))

      "redirect to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe overviewUrl
      }
    }
  }
}
