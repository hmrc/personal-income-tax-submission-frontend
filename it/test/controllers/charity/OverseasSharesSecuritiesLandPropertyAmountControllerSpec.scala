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

class OverseasSharesSecuritiesLandPropertyAmountControllerSpec extends CharityITHelper {

  object Selectors {
    val titleSelector = "title"
    val inputField = ".govuk-input"
    val errorHref = "#amount"
  }

  def url: String = s"$appUrl/$taxYear/charity/value-of-shares-securities-land-or-property-to-overseas-charities"

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
    val expectedErrorExceeds: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val heading: String = "What is the value of qualifying shares, securities, land or property donated to overseas charities?"
    val hintText: String = "For example, £193.52"
    val caption = s"Donations to charity for 6 April $taxYearEOY to 5 April $taxYear"
    val button = "Continue"
    val inputName = "amount"
    val expectedErrorExceeds: String = "The value of shares, securities, land or property donated to overseas charities cannot be more than the " +
      "‘value of shares and securities donated to charity’ plus the ‘value of land or property donated to charity’"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val heading: String = "Beth yw gwerth cyfranddaliadau cymwys, gwarantau, tir neu eiddo a roddwyd i elusennau tramor?"
    val hintText: String = "Er enghraifft, £193.52"
    val caption = s"Rhoddion i elusennau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val button = "Yn eich blaen"
    val inputName = "amount"
    val expectedErrorExceeds: String = "Ni all gwerth cyfranddaliadau, gwarantau, tir neu eiddo a roddir i elusennau fod yn fwy na" +
      " ‘gwerth cyfranddaliadau a gwarantau a roddir i elusennau’ plws ‘gwerth tir neu eiddo a roddir i elusennau’"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val tooLong = "The value of your shares, securities, land or property must be less than £100,000,000,000"
    val emptyField = "Enter the value of shares, securities, land or property you donated to overseas charities"
    val incorrectFormat = "Enter the value of shares, securities, land or property you donated to overseas charities in the correct format"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val tooLong = "The value of your client’s shares, securities, land or property must be less than £100,000,000,000"
    val emptyField = "Enter the value of shares, securities, land or property your client donated to overseas charities"
    val incorrectFormat = "Enter the value of shares, securities, land or property your client donated to overseas charities in the correct format"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val tooLong = "Mae’n rhaid i werth eich cyfranddaliadau, gwarantau, tir neu eiddo fod yn llai na £100,000,000,000"
    val emptyField = "Nodwch werth cyfranddaliadau cymwys, gwarantau, tir neu eiddo a roesoch i elusennau tramor"
    val incorrectFormat = "Nodwch werth cyfranddaliadau, gwarantau, tir neu eiddo a roddwyd gennych i elusennau tramor yn y fformat cywir"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val tooLong = "Mae’n rhaid i werth cyfranddaliadau, gwarantau, tir neu eiddo eich cleient fod yn llai na £100,000,000,000"
    val emptyField = "Nodwch werth cyfranddaliadau, gwarantau, tir neu eiddo a roddwyd gan eich cleient i elusennau tramor"
    val incorrectFormat = "Nodwch werth cyfranddaliadau, gwarantau, tir neu eiddo a roddwyd gan eich cleient i elusennau tramor yn y fformat cywir"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  val donatedAmount = 50
  val validAmount = 99

  val requiredSessionModel: GiftAidCYAModel =
    GiftAidCYAModel(
      overseasDonatedSharesSecuritiesLandOrProperty = Some(true),
      donatedLandOrPropertyAmount = Some(donatedAmount),
      donatedSharesOrSecuritiesAmount = Some(donatedAmount)
    )
  val requiredSessionData: Option[GiftAidCYAModel] = Some(requiredSessionModel)

  val requiredSessionModelPrefill: GiftAidCYAModel = requiredSessionModel.copy(
    overseasDonatedSharesSecuritiesLandOrPropertyAmount = Some(validAmount)
  )
  val requiredSessionDataPrefill: Option[GiftAidCYAModel] = Some(requiredSessionModelPrefill)
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


          inputFieldCheck(inputName, Selectors.inputField)
          inputFieldValueCheck("50", Selectors.inputField)
        }
      }
    }

    "there is no cya data in session" should {
      lazy val result = getResult(url, None, None)

      "redirect to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe overviewUrl
      }
    }

    "there is no overseasDonatedSharesSecuritiesLandOrProperty" should {
      lazy val result = getResult(url, Some(GiftAidCYAModel(donatedLandOrProperty = Some(false))), None)

      "redirect to the SharesSecuritiesLandPropertyOverseas page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidSharesSecuritiesLandPropertyOverseasController.show(taxYear)}"
      }
    }

    "there is prior data for investmentsNonUkCharities" should {

      "display the overseasSharesSecuritiesLandPropertyAmount page when the 'Change' link is clicked on the CYA page" which {

        val priorData: IncomeSourcesModel = IncomeSourcesModel(None, None,
          giftAid = Some(GiftAidSubmissionModel(None, Some(GiftsModel(landAndBuildings = Some(1000.21)))))
        )

        lazy val result = getResult(url, requiredSessionData, Some(priorData))

        "has an OK 200 status" in {
          result.status shouldBe OK
        }
      }
    }

    "there is cya data, but 'donatedLandOrPropertyAmount' and 'donatedSharesOrSecuritiesAmount' have not been stored" should {
      lazy val result = getResult(url, Some(GiftAidCYAModel(overseasDonatedSharesSecuritiesLandOrProperty = Some(true))), None)

      "redirect the user to the gift aid donations page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidDonationsController.show(taxYear)}"
      }
    }
  }

  ".submit" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an error" when {

          import user.commonExpectedResults._

          "the submitted data is empty" which {
            lazy val result = postResult(url, requiredSessionData, None, Map("amount" -> ""), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)


            import user.commonExpectedResults._

            titleCheck(errorPrefix(user.isWelsh) + heading, user.isWelsh)
            h1Check(heading + " " + caption)
            inputFieldCheck(inputName, Selectors.inputField)
            hintTextCheck(hintText)
            captionCheck(caption)
            buttonCheck(button)
            errorSummaryCheck(user.specificExpectedResults.get.emptyField, Selectors.errorHref, user.isWelsh)
            errorAboveElementCheck(user.specificExpectedResults.get.emptyField)
            welshToggleCheck(user.isWelsh)
          }

          "the submitted data is too long" which {
            lazy val result = postResult(url, requiredSessionData, None, Map("amount" -> "999999999999999"), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import user.commonExpectedResults._

            titleCheck(errorPrefix(user.isWelsh) + heading, user.isWelsh)
            h1Check(heading + " " + caption)
            inputFieldCheck(inputName, Selectors.inputField)
            hintTextCheck(hintText)
            captionCheck(caption)
            buttonCheck(button)
            errorSummaryCheck(user.specificExpectedResults.get.tooLong, Selectors.errorHref, user.isWelsh)
            errorAboveElementCheck(user.specificExpectedResults.get.tooLong)
            welshToggleCheck(user.isWelsh)
          }

          "the submitted data is in the incorrect format" which {
            lazy val result = postResult(url, requiredSessionData, None, Map("amount" -> ":@~{}<>?"), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)


            import user.commonExpectedResults._

            titleCheck(errorPrefix(user.isWelsh) + heading, user.isWelsh)
            h1Check(heading + " " + caption)
            inputFieldCheck(inputName, Selectors.inputField)
            hintTextCheck(hintText)
            captionCheck(caption)
            buttonCheck(button)
            errorSummaryCheck(user.specificExpectedResults.get.incorrectFormat, Selectors.errorHref, user.isWelsh)
            errorAboveElementCheck(user.specificExpectedResults.get.incorrectFormat)
            welshToggleCheck(user.isWelsh)
          }

          "the submitted amount is greater than 'donatedLandOrPropertyAmount' + 'donatedSharesOrSecuritiesAmount'" which {
            lazy val result = postResult(url, requiredSessionData, None, Map("amount" -> "100.01"), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(errorPrefix(user.isWelsh) + heading, user.isWelsh)
            h1Check(heading + " " + caption)
            inputFieldCheck(inputName, Selectors.inputField)
            hintTextCheck(hintText)
            captionCheck(caption)
            buttonCheck(button)
            errorSummaryCheck(user.commonExpectedResults.expectedErrorExceeds, Selectors.errorHref, user.isWelsh)
            errorAboveElementCheck(user.commonExpectedResults.expectedErrorExceeds)
            welshToggleCheck(user.isWelsh)
          }
        }
      }
    }

    "there is no cyaData" should {
      lazy val result = getResult(url, None, None)

      "redirect to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe overviewUrl
      }
    }

    "the form is valid" should {
      lazy val result = postResult(url, requiredSessionData, None, Map("amount" -> s"$validAmount"))

      "should redirect to the 'overseas SSLP charity name' page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe
          controllers.charity.routes.GiftAidOverseasSharesNameController.show(taxYear, None).url
      }
    }

    "the form is valid with completed CYA data" should {
      lazy val result =
        postResult(url, Some(completeGiftAidCYAModel.copy(overseasDonatedSharesSecuritiesLandOrPropertyAmount = None)), None, Map("amount" -> s"$validAmount"))

      "redirect the user to the 'check your answers' page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(taxYear)}"
      }
    }
  }
}