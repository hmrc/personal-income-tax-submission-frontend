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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.{OK, SEE_OTHER}
import utils.CharityITHelper

class LastTaxYearAmountControllerISpec extends CharityITHelper {

  object Selectors {
    val para = "label > p"
    val errorSummary = "#error-summary-title"
    val noSelectionError = ".govuk-error-summary__body > ul > li > a"
    val amount = "#amount"
    val errorMessage = "#value-error"
  }

  def url: String = s"$appUrl/$year/charity/amount-added-to-last-tax-year"

  trait SpecificExpectedResults {
    val heading: String
    val para: String
    val noSelectionError: String
    val tooLongError: String
    val invalidFormatError: String
  }

  trait CommonExpectedResults {
    val caption: String
    val hint: String
    val button: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val caption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val hint = "For example, £600 or £193.54"
    val button = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val caption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val hint = "For example, £600 or £193.54"
    val button = "Continue"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val heading = "How much of your donation do you want to add to the last tax year?"
    val para = "Do not include the Gift Aid added to your donation."
    val noSelectionError = "Enter the amount of your donation you want to add to the last tax year"
    val tooLongError = "The amount of your donation you add to the last tax year must be less than £100,000,000,000"
    val invalidFormatError = "Enter the amount you want to add to the last tax year in the correct format"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val heading = "How much of your client’s donation do you want to add to the last tax year?"
    val para = "Do not include the Gift Aid added to your client’s donation."
    val noSelectionError = "Enter the amount of your client’s donation you want to add to the last tax year"
    val tooLongError = "The amount of your client’s donation you add to the last tax year must be less than £100,000,000,000"
    val invalidFormatError = "Enter the amount you want to add to the last tax year in the correct format"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val heading = "How much of your donation do you want to add to the last tax year?"
    val para = "Do not include the Gift Aid added to your donation."
    val noSelectionError = "Enter the amount of your donation you want to add to the last tax year"
    val tooLongError = "The amount of your donation you add to the last tax year must be less than £100,000,000,000"
    val invalidFormatError = "Enter the amount you want to add to the last tax year in the correct format"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val heading = "How much of your client’s donation do you want to add to the last tax year?"
    val para = "Do not include the Gift Aid added to your client’s donation."
    val noSelectionError = "Enter the amount of your client’s donation you want to add to the last tax year"
    val tooLongError =  "The amount of your client’s donation you add to the last tax year must be less than £100,000,000,000"
    val invalidFormatError = "Enter the amount you want to add to the last tax year in the correct format"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  val amountValue: Int = 1000

  val requiredSessionModel: GiftAidCYAModel = GiftAidCYAModel(addDonationToLastYear = Some(true))
  val requiredSessionData: Option[GiftAidCYAModel] = Some(requiredSessionModel)

  val requiredSessionModelPrefill: GiftAidCYAModel = GiftAidCYAModel(
    addDonationToLastYear = Some(true),
    addDonationToLastYearAmount = Some(amountValue)
  )

  val requiredSessionDataPrefill: Option[GiftAidCYAModel] = Some(requiredSessionModelPrefill)

  val validAmount = 1234

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

          titleCheck(user.specificExpectedResults.get.heading)
          h1Check(user.specificExpectedResults.get.heading + " " + caption)
          textOnPageCheck(user.specificExpectedResults.get.para, para)
          inputFieldCheck("amount", ".govuk-input")
          hintTextCheck(hint)
          captionCheck(caption)
          buttonCheck(button)
          elementExtinct(errorSummary)
          elementExtinct(noSelectionError)
          elementExtinct(errorMessage)
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

          titleCheck(user.specificExpectedResults.get.heading)
          h1Check(user.specificExpectedResults.get.heading + " " + caption)
          textOnPageCheck(user.specificExpectedResults.get.para, para)
          inputFieldCheck("amount", ".govuk-input")
          hintTextCheck(hint)
          captionCheck(caption)
          buttonCheck(button)
          elementExtinct(errorSummary)
          elementExtinct(noSelectionError)
          elementExtinct(errorMessage)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "there is no session data" should {
      lazy val result = getResult(url, None, None)

      "redirects to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe overviewUrl
      }
    }

    "the addDonationToLastYear field is missing" should {
      lazy val result =
        getResult(url, Some(GiftAidCYAModel(donationsViaGiftAidAmount = Some(validAmount), overseasDonationsViaGiftAid = Some(false))), None)

      "redirect to the 'add donation to last tax year' page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe controllers.charity.routes.GiftAidLastTaxYearController.show(year).url
      }
    }

    "the addDonationToLastYear field is false" should {
      lazy val result =
        getResult(url, Some(GiftAidCYAModel(addDonationToLastYear = Some(false))), None)

      "redirect to the 'add donation to this tax year' page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe controllers.charity.routes.DonationsToPreviousTaxYearController.show(year, year).url
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

            titleCheck(errorPrefix + user.specificExpectedResults.get.heading)
            h1Check(user.specificExpectedResults.get.heading + " " + caption)
            textOnPageCheck(user.specificExpectedResults.get.para, para)
            inputFieldCheck("amount", ".govuk-input")
            hintTextCheck(hint)
            captionCheck(caption)
            buttonCheck(button)
            errorSummaryCheck(user.specificExpectedResults.get.noSelectionError, amount)
            errorAboveElementCheck(user.specificExpectedResults.get.noSelectionError)
            welshToggleCheck(user.isWelsh)
          }

          "the submitted data is too long" which {
            lazy val result = postResult(url, requiredSessionData, None, Map("amount" -> "999999999999999"), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(errorPrefix + user.specificExpectedResults.get.heading)
            h1Check(user.specificExpectedResults.get.heading + " " + caption)
            textOnPageCheck(user.specificExpectedResults.get.para, para)
            inputFieldCheck("amount", ".govuk-input")
            hintTextCheck(hint)
            captionCheck(caption)
            buttonCheck(button)
            errorSummaryCheck(user.specificExpectedResults.get.tooLongError, amount)
            errorAboveElementCheck(user.specificExpectedResults.get.tooLongError)
            welshToggleCheck(user.isWelsh)
          }

          "the submitted data is in the incorrect format" which {
            lazy val result = postResult(url, requiredSessionData, None, Map("amount" -> ":@~{}<>?"), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(errorPrefix + user.specificExpectedResults.get.heading)
            h1Check(user.specificExpectedResults.get.heading + " " + caption)
            textOnPageCheck(user.specificExpectedResults.get.para, para)
            inputFieldCheck("amount", ".govuk-input")
            hintTextCheck(hint)
            captionCheck(caption)
            buttonCheck(button)
            errorSummaryCheck(user.specificExpectedResults.get.invalidFormatError, amount)
            errorAboveElementCheck(user.specificExpectedResults.get.invalidFormatError)
            welshToggleCheck(user.isWelsh)
          }
        }
      }
    }

    "there is no session data" should {
      lazy val result = postResult(url, None, None, Map("amount" -> s"$validAmount"))

      "redirect to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe overviewUrl
      }
    }

    "the user enters a valid amount" when {

      "this completes the cya data" should {
        lazy val result = postResult(url, Some(completeGiftAidCYAModel), None, Map("amount" -> s"$validAmount"))

        "redirect to the cya page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe cyaUrl(year)
        }

        "update the cya data" in {
          findGiftAidDb shouldBe Some(completeGiftAidCYAModel.copy(addDonationToLastYearAmount = Some(validAmount)))
        }
      }

      "this does not complete the cya data" should {
        lazy val result = postResult(url, requiredSessionData, None, Map("amount" -> s"$validAmount"))

        "redirect to the 'add donation to this tax year' page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe controllers.charity.routes.DonationsToPreviousTaxYearController.show(year, year).url
        }

        "update the cya data" in {
          findGiftAidDb shouldBe Some(requiredSessionModel.copy(addDonationToLastYearAmount = Some(validAmount)))
        }
      }
    }
  }
}