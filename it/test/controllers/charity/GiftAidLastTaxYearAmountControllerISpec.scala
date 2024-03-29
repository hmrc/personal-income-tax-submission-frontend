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
import models.charity.prior.{GiftAidPaymentsModel, GiftAidSubmissionModel}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.{OK, SEE_OTHER}
import test.utils.CharityITHelper

class GiftAidLastTaxYearAmountControllerISpec extends CharityITHelper {

  object Selectors {
    val paragraph = "#p1"
    val errorSummary = "#error-summary-title"
    val noSelectionError = ".govuk-error-summary__body > ul > li > a"
    val amount = "#amount"
    val errorMessage = "#value-error"
  }

  def url: String = s"$appUrl/$taxYear/charity/amount-added-to-last-tax-year"

  trait SpecificExpectedResults {
    val heading: String
    val paragraph: String
    val noSelectionError: String
    val tooLongError: String
    val invalidFormatError: String
    val expectedErrorExceeds: String
  }

  trait CommonExpectedResults {
    val caption: String
    val hint: String
    val button: String
    val inputName: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val caption = s"Donations to charity for 6 April $taxYearEOY to 5 April $taxYear"
    val hint = "For example, £193.52"
    val button = "Continue"
    val inputName = "amount"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val caption = s"Rhoddion i elusennau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val hint = "Er enghraifft, £193.52"
    val button = "Yn eich blaen"
    val inputName = "amount"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val heading = s"How much of your donation did you add to the ${taxYearEOY - 1} to $taxYearEOY tax year?"
    val paragraph = "Do not include the Gift Aid added to your donation."
    val noSelectionError = "Enter the amount of your donation you want to add to the last tax year"
    val tooLongError = "The amount of your donation you add to the last tax year must be less than £100,000,000,000"
    val invalidFormatError = "Enter the amount you want to add to the last tax year in the correct format"
    val expectedErrorExceeds =
      "The amount of your donation you want to add to the last tax year must not be more than the amount you donated to charity by using Gift Aid"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val heading = s"How much of your client’s donation did you add to the ${taxYearEOY -1} to $taxYearEOY tax year?"
    val paragraph = "Do not include the Gift Aid added to your client’s donation."
    val noSelectionError = "Enter the amount of your client’s donation you want to add to the last tax year"
    val tooLongError = "The amount of your client’s donation you add to the last tax year must be less than £100,000,000,000"
    val invalidFormatError = "Enter the amount you want to add to the last tax year in the correct format"
    val expectedErrorExceeds =
      "The amount of your client’s donation you want to add to the last tax year must not be more than the amount your client donated to charity by using Gift Aid"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val heading = s"Faint o’ch cyfraniad y gwnaethoch ei ychwanegu at flwyddyn dreth ${taxYearEOY - 1} i $taxYearEOY?"
    val paragraph = "Peidiwch â chynnwys y Rhodd Cymorth a ychwanegwyd at eich rhodd."
    val noSelectionError = "Nodwch faint o’ch rhodd rydych am ei ychwanegu at y flwyddyn dreth ddiwethaf"
    val tooLongError = "Mae’n rhaid i swm eich rhodd yr ydych yn ychwanegu at y flwyddyn dreth ddiwethaf fod yn llai na £100,000,000,000"
    val invalidFormatError = "Nodwch y maint rydych am ei ychwanegu at y flwyddyn dreth ddiwethaf yn y fformat cywir"
    val expectedErrorExceeds =
      "Ni chaiff swm eich rhodd, yr hoffech ei hychwanegu at y flwyddyn dreth ddiwethaf, fod yn fwy na’r swm a roesoch i elusennau drwy ddefnyddio Rhodd Cymorth"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val heading = s"Faint o gyfraniad eich cleient y gwnaethoch ei ychwanegu at flwyddyn dreth ${taxYearEOY - 1} i $taxYearEOY?"
    val paragraph = "Peidiwch â chynnwys y Rhodd Cymorth a ychwanegwyd at rodd eich cleient."
    val noSelectionError = "Nodwch faint o rodd eich cleient rydych am ei ychwanegu at y flwyddyn dreth ddiwethaf"
    val tooLongError =  "Mae’n rhaid i swm rhodd eich cleient yr ydych yn ychwanegu at y flwyddyn dreth ddiwethaf fod yn llai na £100,000,000,000"
    val invalidFormatError = "Nodwch y maint rydych am ei ychwanegu at y flwyddyn dreth ddiwethaf yn y fformat cywir"
    val expectedErrorExceeds =
      "Ni chaiff swm rhodd eich cleient, yr hoffech ei hychwanegu at y flwyddyn dreth ddiwethaf, fod yn fwy na’r swm a roddodd eich cleient i elusennau drwy ddefnyddio Rhodd Cymorth"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  val totalDonatedAmount = 50
  val validAmount = 25

  val oneOffCyaAmount = 50

  val requiredSessionModel: GiftAidCYAModel = GiftAidCYAModel(addDonationToLastYear = Some(true), donationsViaGiftAidAmount = Some(totalDonatedAmount))
  val requiredSessionData: Option[GiftAidCYAModel] = Some(requiredSessionModel)

  val requiredSessionModelPrefill: GiftAidCYAModel = requiredSessionModel.copy(
    addDonationToLastYearAmount = Some(oneOffCyaAmount)
  )
  val requiredSessionDataPrefill: Option[GiftAidCYAModel] = Some(requiredSessionModelPrefill)
  val requiredPriorData: Option[IncomeSourcesModel] = Some(IncomeSourcesModel(None, None, Some(priorDataMax)))

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

          titleCheck(user.specificExpectedResults.get.heading, user.isWelsh)
          h1Check(user.specificExpectedResults.get.heading + " " + caption)
          textOnPageCheck(user.specificExpectedResults.get.paragraph, paragraph)
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

          titleCheck(user.specificExpectedResults.get.heading, user.isWelsh)
          h1Check(user.specificExpectedResults.get.heading + " " + caption)
          inputFieldCheck("amount", ".govuk-input")
          hintTextCheck(hint)
          captionCheck(caption)
          buttonCheck(button)
          elementExtinct(errorSummary)
          elementExtinct(noSelectionError)
          elementExtinct(errorMessage)
          welshToggleCheck(user.isWelsh)
        }


        "display the correct prior amount when returning after submission" which {
          lazy val result = getResult(url, requiredSessionData, requiredPriorData, user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          inputFieldCheck(inputName, Selectors.amount)
          inputFieldValueCheck("", Selectors.amount)
        }

        "display the correct cya amount when returning before resubmitting" which {
          lazy val result = getResult(url, Some(completeGiftAidCYAModel), requiredPriorData, user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          inputFieldCheck(inputName, Selectors.amount)
          inputFieldValueCheck("50", Selectors.amount)
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
        result.headers("Location").head shouldBe controllers.charity.routes.GiftAidLastTaxYearController.show(taxYear).url
      }
    }

    "there is cya data, but 'donationsViaGiftAidAmount' has not been stored" should {
      lazy val result = getResult(url, Some(GiftAidCYAModel(addDonationToLastYear = Some(true))), None)

      "redirect the user to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe overviewUrl
      }
    }

    "the addDonationToLastYear field is false" should {
      lazy val result =
        getResult(url, Some(GiftAidCYAModel(addDonationToLastYear = Some(false))), None)

      "redirect to the 'add donation to this tax year' page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear).url
      }
    }

    "there is prior data for currentYearTreatedAsPreviousYear" should {

      "display the GiftAidLastTaxYearAmount page when the 'Change' link is clicked on the CYA page" which {

        val priorData = IncomeSourcesModel(None, None,
          giftAid = Some(GiftAidSubmissionModel(Some(GiftAidPaymentsModel(nextYearTreatedAsCurrentYear = Some(1000.56)))))
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

          import Selectors._
          import user.commonExpectedResults._

          "the submitted data is empty" which {
            lazy val result = postResult(url, requiredSessionData, None, Map("amount" -> ""), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(errorPrefix(user.isWelsh) + user.specificExpectedResults.get.heading, user.isWelsh)
            h1Check(user.specificExpectedResults.get.heading + " " + caption)
            textOnPageCheck(user.specificExpectedResults.get.paragraph, paragraph)
            inputFieldCheck("amount", ".govuk-input")
            hintTextCheck(hint)
            captionCheck(caption)
            buttonCheck(button)
            errorSummaryCheck(user.specificExpectedResults.get.noSelectionError, amount, user.isWelsh)
            errorAboveElementCheck(user.specificExpectedResults.get.noSelectionError)
            welshToggleCheck(user.isWelsh)
          }

          "the submitted data is too long" which {
            lazy val result = postResult(url, requiredSessionData, None, Map("amount" -> "999999999999999"), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(errorPrefix(user.isWelsh) + user.specificExpectedResults.get.heading, user.isWelsh)
            h1Check(user.specificExpectedResults.get.heading + " " + caption)
            textOnPageCheck(user.specificExpectedResults.get.paragraph, paragraph)
            inputFieldCheck("amount", ".govuk-input")
            hintTextCheck(hint)
            captionCheck(caption)
            buttonCheck(button)
            errorSummaryCheck(user.specificExpectedResults.get.tooLongError, amount, user.isWelsh)
            errorAboveElementCheck(user.specificExpectedResults.get.tooLongError)
            welshToggleCheck(user.isWelsh)
          }

          "the submitted data is in the incorrect format" which {
            lazy val result = postResult(url, requiredSessionData, None, Map("amount" -> ":@~{}<>?"), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(errorPrefix(user.isWelsh) + user.specificExpectedResults.get.heading, user.isWelsh)
            h1Check(user.specificExpectedResults.get.heading + " " + caption)
            textOnPageCheck(user.specificExpectedResults.get.paragraph, paragraph)
            inputFieldCheck("amount", ".govuk-input")
            hintTextCheck(hint)
            captionCheck(caption)
            buttonCheck(button)
            errorSummaryCheck(user.specificExpectedResults.get.invalidFormatError, amount, user.isWelsh)
            errorAboveElementCheck(user.specificExpectedResults.get.invalidFormatError)
            welshToggleCheck(user.isWelsh)
          }

          "the submitted amount is greater than the 'donationsViaGiftAidAmount'" which {
            lazy val result = postResult(url, requiredSessionData, None, Map("amount" -> "50.01"), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(errorPrefix(user.isWelsh) + user.specificExpectedResults.get.heading, user.isWelsh)
            h1Check(user.specificExpectedResults.get.heading + " " + caption)
            textOnPageCheck(user.specificExpectedResults.get.paragraph, paragraph)
            inputFieldCheck("amount", ".govuk-input")
            hintTextCheck(hint)
            captionCheck(caption)
            buttonCheck(button)
            errorSummaryCheck(user.specificExpectedResults.get.expectedErrorExceeds, amount, user.isWelsh)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedErrorExceeds)
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
          result.headers("Location").head shouldBe cyaUrl(taxYear)
        }

        "update the cya data" in {
          findGiftAidDb shouldBe Some(completeGiftAidCYAModel.copy(addDonationToLastYearAmount = Some(validAmount)))
        }
      }

      "this does not complete the cya data" should {
        lazy val result = postResult(url, requiredSessionData, None, Map("amount" -> s"$validAmount"))

        "redirect to the 'add donation to this tax year' page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear).url
        }

        "update the cya data" in {
          findGiftAidDb shouldBe Some(requiredSessionModel.copy(addDonationToLastYearAmount = Some(validAmount)))
        }
      }
    }
  }
}