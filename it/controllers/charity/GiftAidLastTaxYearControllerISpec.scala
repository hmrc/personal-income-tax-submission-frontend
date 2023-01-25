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

import forms.YesNoForm
import models.charity.GiftAidCYAModel
import models.charity.prior.{GiftAidPaymentsModel, GiftAidSubmissionModel}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import utils.CharityITHelper

class GiftAidLastTaxYearControllerISpec extends CharityITHelper {

  val testModel: GiftAidSubmissionModel =
    GiftAidSubmissionModel(Some(GiftAidPaymentsModel(None, Some(List("JaneDoe")), None, Some(150.00), None, None)),None)

  def url: String = s"$appUrl/$taxYear/charity/add-charity-donations-to-last-tax-year"


  object Selectors {
    val continueSelector = "#continue"
    val captionSelector: String = ".govuk-caption-l"
    val contentSelector1: String = "#main-content > div > div > form > div > fieldset > legend > div > p:nth-child(1)"
    val contentSelector2: String = "#main-content > div > div > form > div > fieldset > legend > div > p:nth-child(2)"
    val errorSummaryHref = "#value"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedError: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val yesText: String
    val noText: String
    val expectedContinue: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: String = s"Donations to charity for 6 April $taxYearEOY to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val expectedContinue = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: String = s"Rhoddion i elusennau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val yesText = "Iawn"
    val noText = "Na"
    val expectedContinue = "Yn eich blaen"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = s"Did you add any of your donations to the ${taxYearEOY - 1} to $taxYearEOY tax year?"
    val expectedH1: String = s"Did you add any of your donations to the ${taxYearEOY - 1} to $taxYearEOY tax year?"
    val expectedError: String = "Select yes to add any of your donations to the last tax year"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = s"Did you add any of your client’s donations to the ${taxYearEOY - 1} to $taxYearEOY tax year?"
    val expectedH1: String = s"Did you add any of your client’s donations to the ${taxYearEOY - 1} to $taxYearEOY tax year?"
    val expectedError: String = "Select yes to add any of your client’s donations to the last tax year"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = s"A wnaethoch ychwanegu unrhyw rai o’ch cyfraniadau at flwyddyn dreth ${taxYearEOY - 1} i $taxYearEOY?"
    val expectedH1: String = s"A wnaethoch ychwanegu unrhyw rai o’ch cyfraniadau at flwyddyn dreth ${taxYearEOY - 1} i $taxYearEOY?"
    val expectedError: String = "Dewiswch ‘Iawn’ i ychwanegu unrhyw un o’ch rhoddion at y flwyddyn dreth ddiwethaf"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = s"A wnaethoch ychwanegu unrhyw rai o gyfraniadau eich cleient at flwyddyn dreth ${taxYearEOY - 1} i $taxYearEOY?"
    val expectedH1: String = s"A wnaethoch ychwanegu unrhyw rai o gyfraniadau eich cleient at flwyddyn dreth ${taxYearEOY -1} i $taxYearEOY?"
    val expectedError: String = "Dewiswch ‘Iawn’ i ychwanegu unrhyw un o roddion eich cleient at y flwyddyn dreth ddiwethaf"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  val requiredSessionModel: GiftAidCYAModel = GiftAidCYAModel(donationsViaGiftAidAmount = Some(150.00), overseasDonationsViaGiftAid = Some(false))
  val requiredSessionData: Option[GiftAidCYAModel] = Some(requiredSessionModel)

  val requiredSessionModelPrefill: GiftAidCYAModel = GiftAidCYAModel(
    donationsViaGiftAidAmount = Some(150.00),
    overseasDonationsViaGiftAid = Some(false),
    addDonationToLastYear = Some(true)
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
          h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(expectedContinue, continueSelector)
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
          h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          radioButtonHasChecked(yesText, 1)
          buttonCheck(expectedContinue, continueSelector)
          noErrorsCheck()
          welshToggleCheck(user.isWelsh)
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

    "there is prior data for currentYearTreatedAsPreviousYear" when {
      val priorData = IncomeSourcesModel(
        giftAid = Some(
          GiftAidSubmissionModel(Some(
            GiftAidPaymentsModel(currentYearTreatedAsPreviousYear = Some(1000.00))
          ))
        )
      )

      lazy val result = getResult(url, requiredSessionData, Some(priorData))

      "redirect to the cya page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(taxYear)}"
      }
    }

    "the user has indicated they donated to overseas charities, but have no charity names" when {

      "redirect to the Name of overseas charity page" which {
        val cyaModel = GiftAidCYAModel(
          overseasDonationsViaGiftAid = Some(true),
          overseasDonationsViaGiftAidAmount = Some(1000.23)
        )

        lazy val result = getResult(url, Some(cyaModel), None)

        "redirects to the gift aid overseas name page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe controllers.charity.routes.GiftAidOverseasNameController.show(taxYear, None).url
        }
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
          h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(expectedContinue, continueSelector)
          errorSummaryCheck(user.specificExpectedResults.get.expectedError, errorSummaryHref, user.isWelsh)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedError)
          welshToggleCheck(user.isWelsh)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
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

    "there is prior data for currentYearTreatedAsPreviousYear" should {
      val priorData = IncomeSourcesModel(
        giftAid = Some(
          GiftAidSubmissionModel(Some(
            GiftAidPaymentsModel(
              currentYearTreatedAsPreviousYear = Some(1000.00)
            )
          ))
        )
      )
      lazy val result = postResult(url, requiredSessionData, Some(priorData), Map(YesNoForm.yesNo -> YesNoForm.yes))

      "redirect to the cya page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe cyaUrl(taxYear)
      }
    }

    "there is valid session data available and the user answers 'yes'" should {
      lazy val result = postResult(url, requiredSessionData, None, Map(YesNoForm.yesNo -> YesNoForm.yes))

      "redirect to the last tax year amount page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe controllers.charity.routes.GiftAidLastTaxYearAmountController.show(taxYear).url
      }

      "updated the addDonationsToLastTaxYear field to true" in {
        findGiftAidDb.get.addDonationToLastYear.get shouldBe true
      }
    }

    "there is valid session data available and the user answers 'no'" when {

      "this completes the cya model" should {
        lazy val result = postResult(url, Some(completeGiftAidCYAModel), None, Map(YesNoForm.yesNo -> YesNoForm.no))

        "has a status of SEE_OTHER" in {
          result.status shouldBe SEE_OTHER
        }

        "redirect to the 'check your answers' page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe cyaUrl(taxYear)
        }

        "updated the addDonationsToLastTaxYear field to false" in {
          findGiftAidDb shouldBe
            Some(completeGiftAidCYAModel.copy(addDonationToLastYear = Some(false), addDonationToLastYearAmount = None))
        }
      }

      "this does not complete the cya model" should {
        lazy val result = postResult(url, requiredSessionData, None, Map(YesNoForm.yesNo -> YesNoForm.no))

        "has a status of SEE_OTHER" in {
          result.status shouldBe SEE_OTHER
        }

        "redirect to the 'add donations to this tax year' page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear).url
        }

        "updated the addDonationsToLastTaxYear field to false" in {
          findGiftAidDb shouldBe Some(requiredSessionModel.copy(addDonationToLastYear = Some(false)))
        }
      }
    }
  }
}