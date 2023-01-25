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

package controllers.charity

import models.charity.GiftAidCYAModel
import models.charity.prior.{GiftAidSubmissionModel, GiftsModel}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import utils.CharityITHelper

class GiftAidTotalShareSecurityAmountControllerSpec extends CharityITHelper {

  object Selectors {
    val titleSelector = "title"
    val inputField = ".govuk-input"
    val errorHref = "#amount"
    val inputLabel = "#p1"
  }

  def url: String = s"$appUrl/$taxYear/charity/value-of-shares-or-securities"

  trait SpecificExpectedResults {
    val tooLong: String
    val emptyField: String
    val incorrectFormat: String
  }

  trait CommonExpectedResults {
    val heading: String
    val hintText: String
    val caption: String
    val button: String
    val inputName: String
    val inputLabel: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val heading: String = "What is the total value of qualifying shares or securities donated to charity?"
    val hintText: String = "For example, £193.52"
    val caption = s"Donations to charity for 6 April $taxYearEOY to 5 April $taxYear"
    val button = "Continue"
    val inputName = "amount"
    val inputLabel = "Total value, in pounds"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val heading: String = "Beth yw cyfanswm gwerth cyfranddaliadau neu warantau cymwys a roddir i elusen?"
    val hintText: String = "Er enghraifft, £193.52"
    val caption = s"Rhoddion i elusennau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val button = "Yn eich blaen"
    val inputName = "amount"
    val inputLabel = "Cyfanswm y gwerth, mewn punnoedd"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val tooLong = "The value of your shares or securities must be less than £100,000,000,000"
    val emptyField = "Enter the value of shares or securities you donated to charity"
    val incorrectFormat = "Enter the value of shares or securities you donated to charity in the correct format"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val tooLong = "The value of your client’s shares or securities must be less than £100,000,000,000"
    val emptyField = "Enter the value of shares or securities your client donated to charity"
    val incorrectFormat = "Enter the value of shares or securities your client donated to charity in the correct format"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val tooLong = "Mae’n rhaid i werth eich cyfranddaliadau neu warantau fod yn llai na £100,000,000,000"
    val emptyField = "Nodwch werth cyfranddaliadau neu warantau a roddwyd gennych i elusen"
    val incorrectFormat = "Nodwch werth cyfranddaliadau neu warantau a roesoch i elusen yn y fformat cywir"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val tooLong = "Mae’n rhaid i werth cyfranddaliadau neu warantau eich cleient fod yn llai na £100,000,000,000"
    val emptyField = "Nodwch werth cyfranddaliadau neu warantau a roddwyd gan eich cleient i elusen"
    val incorrectFormat = "Nodwch werth cyfranddaliadau neu warantau a roddwyd gan eich cleient i elusen yn y fformat cywir"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  val requiredSessionModel: GiftAidCYAModel = GiftAidCYAModel(donatedSharesOrSecurities = Some(true))
  val requiredSessionData: Option[GiftAidCYAModel] = Some(requiredSessionModel)

  val amount: Int = 2000

  val requiredSessionModelPrefill: GiftAidCYAModel = GiftAidCYAModel(
    gateway = Some(true),
    donatedSharesOrSecurities = Some(true),
    donatedSharesOrSecuritiesAmount = Some(amount)
  )
  val requiredSessionDataPrefill: Option[GiftAidCYAModel] = Some(requiredSessionModelPrefill)

  val validAmount: Int = 1234
  val requiredPriorData: Option[IncomeSourcesModel] = Some(IncomeSourcesModel(None, None, Some(priorDataMax)))

  ".show" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the page with correct content" which {
          lazy val result = getResult(url, requiredSessionData, None, user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(heading, user.isWelsh)
          h1Check(heading + " " + caption)
          inputFieldCheck(inputName, Selectors.inputField)
          textOnPageCheck(inputLabel, Selectors.inputLabel)
          hintTextCheck(hintText)
          captionCheck(caption)
          buttonCheck(button)
          noErrorsCheck()
          welshToggleCheck(user.isWelsh)
        }

        "render the page with correct content with prefilled CYA data" which {
          lazy val result = getResult(url, requiredSessionDataPrefill, None, user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(heading, user.isWelsh)
          h1Check(heading + " " + caption)
          inputFieldCheck(inputName, Selectors.inputField)
          textOnPageCheck(inputLabel, Selectors.inputLabel)
          hintTextCheck(hintText)
          captionCheck(caption)
          buttonCheck(button)
          noErrorsCheck()
          welshToggleCheck(user.isWelsh)
        }

        "display the correct prior amount when returning after submission" which {
          lazy val result = getResult(url, requiredSessionData, requiredPriorData, user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          inputFieldCheck(inputName, Selectors.inputField)
          inputFieldValueCheck("", Selectors.inputField)
        }

        "display the correct cya amount when returning before resubmitting" which {
          lazy val result = getResult(url, Some(completeGiftAidCYAModel), requiredPriorData, user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(heading, user.isWelsh)
          h1Check(heading + " " + caption)
          inputFieldCheck(inputName, Selectors.inputField)
          inputFieldValueCheck("50", Selectors.inputField)
        }
      }
    }

    "there is no data" should {
      lazy val result = getResult(url, None, None)

      "redirect to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe overviewUrl
      }
    }

    "there is prior data for sharesOrSecurities" should {

      "display the GiftAidShareSecurityAmount page when the 'Change' link is clicked on the CYA page" which {

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

            import user.commonExpectedResults._

            titleCheck(errorPrefix(user.isWelsh) + heading, user.isWelsh)
            h1Check(heading + " " + caption)
            inputFieldCheck(inputName, Selectors.inputField)
            textOnPageCheck(inputLabel, Selectors.inputLabel)
            hintTextCheck(hintText)
            captionCheck(caption)
            buttonCheck(button)
            welshToggleCheck(user.isWelsh)
            errorSummaryCheck(user.specificExpectedResults.get.emptyField, Selectors.errorHref, user.isWelsh)
            errorAboveElementCheck(user.specificExpectedResults.get.emptyField)
          }

          "the submitted data is too long" which {
            lazy val result = postResult(url, requiredSessionData, None, Map("amount" -> "999999999999999"), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import user.commonExpectedResults._

            titleCheck(errorPrefix(user.isWelsh) + heading, user.isWelsh)
            h1Check(heading + " " + caption)
            inputFieldCheck(inputName, Selectors.inputField)
            textOnPageCheck(inputLabel, Selectors.inputLabel)
            hintTextCheck(hintText)
            captionCheck(caption)
            buttonCheck(button)
            welshToggleCheck(user.isWelsh)
            errorSummaryCheck(user.specificExpectedResults.get.tooLong, Selectors.errorHref, user.isWelsh)
            errorAboveElementCheck(user.specificExpectedResults.get.tooLong)
          }

          "the submitted data is in the incorrect format" which {
            lazy val result = postResult(url, requiredSessionData, None, Map("amount" -> ":@~{}<>?"), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import user.commonExpectedResults._

            titleCheck(errorPrefix(user.isWelsh) + heading, user.isWelsh)
            h1Check(heading + " " + caption)
            inputFieldCheck(inputName, Selectors.inputField)
            textOnPageCheck(inputLabel, Selectors.inputLabel)
            hintTextCheck(hintText)
            captionCheck(caption)
            buttonCheck(button)
            welshToggleCheck(user.isWelsh)
            errorSummaryCheck(user.specificExpectedResults.get.incorrectFormat, Selectors.errorHref, user.isWelsh)
            errorAboveElementCheck(user.specificExpectedResults.get.incorrectFormat)
          }
        }
      }
    }

    "there is no cyaData" should {
      lazy val result = postResult(url, None, None, Map("amount" -> s"$validAmount"))

      "redirect to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe overviewUrl
      }
    }

    "the form data is valid" when {

      "this completes the cya data" should {
        lazy val result = postResult(url, Some(completeGiftAidCYAModel), None, Map("amount" -> s"$validAmount"))

        "redirect to the cya page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe cyaUrl(taxYear)
        }

        "update the cya data" in {
          findGiftAidDb shouldBe
            Some(completeGiftAidCYAModel.copy(donatedSharesOrSecuritiesAmount = Some(validAmount)))
        }
      }

      "this does not complete the cya data" should {
        lazy val result = postResult(url, requiredSessionData, None, Map("amount" -> s"$validAmount"))

        "redirect to the 'land or property donation' page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe controllers.charity.routes.GiftAidDonateLandOrPropertyController.show(taxYear).url
        }

        "update the cya data" in {
          findGiftAidDb shouldBe
            Some(requiredSessionModel.copy(donatedSharesOrSecuritiesAmount = Some(validAmount)))
        }
      }
    }
  }
}