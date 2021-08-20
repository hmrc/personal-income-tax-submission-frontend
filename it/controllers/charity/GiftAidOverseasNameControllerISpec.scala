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
import models.charity.prior.{GiftAidPaymentsModel, GiftAidSubmissionModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import utils.CharityITHelper

class GiftAidOverseasNameControllerISpec extends CharityITHelper {

  val charLimit: String = "ukHzoBYHkKGGk2V5iuYgS137gN7EB7LRw3uDjvujYg00ZtHwo3sokyOOCEoAK9vuPiP374QKOelo"
  val testModel: GiftAidSubmissionModel = GiftAidSubmissionModel(Some(GiftAidPaymentsModel(None, Some(List("dupe")), None, None, None, None)),None)

  def url(changeCharity: Option[String] = None): String =
    s"$appUrl/$year/charity/name-of-overseas-charity${if (changeCharity.nonEmpty) s"?changeCharity=${changeCharity.get}" else ""}"

  object Selectors {
    val captionSelector: String = ".govuk-caption-l"
    val inputFieldSelector: String = "#name"
    val buttonSelector: String = ".govuk-button"
    val inputHintTextSelector: String = "#main-content > div > div > form > div > label > p"
    val errorSelector: String = "#main-content > div > div > div.govuk-error-summary > div > ul > li > a"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedError: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedInputName: String
    val expectedButtonText: String
    val expectedInputHintText: String
    val expectedInvalidError: String
    val expectedLengthError: String
    val expectedDuplicateError: String
    val serviceName: String
    val govUkExtension: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: String = "Donations to charity for 6 April 2021 to 5 April 2022"
    val expectedInputName: String = "name"
    val expectedButtonText: String = "Continue"
    val expectedInputHintText: String = "You can add more than one charity."
    val serviceName = "Update and submit an Income Tax Return"
    val govUkExtension = "GOV.UK"
    val expectedInvalidError: String = "Name of overseas charity must only include numbers 0-9, letters a to z," +
      " hyphens, spaces, apostrophes, commas, full stops, round brackets and the special characters, &, /, @, £, *"
    val expectedLengthError: String = "The name of the overseas charity must be 75 characters or fewer"
    val expectedDuplicateError: String = "You cannot add 2 charities with the same name"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: String = "Rhoddion i elusennau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022"
    val expectedInputName: String = "name"
    val expectedButtonText: String = "Yn eich blaen"
    val expectedInputHintText: String = "You can add more than one charity."
    val serviceName = "Update and submit an Income Tax Return"
    val govUkExtension = "GOV.UK"
    val expectedInvalidError: String = "Name of overseas charity must only include numbers 0-9, letters a to z," +
      " hyphens, spaces, apostrophes, commas, full stops, round brackets and the special characters, &, /, @, £, *"
    val expectedLengthError: String = "The name of the overseas charity must be 75 characters or fewer"
    val expectedDuplicateError: String = "You cannot add 2 charities with the same name"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "Name of overseas charity you used Gift Aid to donate to"
    val expectedH1: String = "Name of overseas charity you used Gift Aid to donate to"
    val expectedError: String = "Enter the name of the overseas charity you used Gift Aid to donate to"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "Name of overseas charity your client used Gift Aid to donate to"
    val expectedH1: String = "Name of overseas charity your client used Gift Aid to donate to"
    val expectedError: String = "Enter the name of the overseas charity your client used Gift Aid to donate to"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "Name of overseas charity you used Gift Aid to donate to"
    val expectedH1: String = "Name of overseas charity you used Gift Aid to donate to"
    val expectedError: String = "Enter the name of the overseas charity you used Gift Aid to donate to"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "Name of overseas charity your client used Gift Aid to donate to"
    val expectedH1: String = "Name of overseas charity your client used Gift Aid to donate to"
    val expectedError: String = "Enter the name of the overseas charity your client used Gift Aid to donate to"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  val requiredSessionModel: GiftAidCYAModel = GiftAidCYAModel(overseasDonationsViaGiftAidAmount = Some(BigDecimal(1)))
  val requiredSessionData: Option[GiftAidCYAModel] = Some(requiredSessionModel)

  val validAmount = 445.30

  ".show" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the page with correct content" which {
          lazy val result = getResult(url(), requiredSessionData, None, user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(expectedInputHintText, inputHintTextSelector)
          inputFieldCheck(expectedInputName, inputFieldSelector)
          buttonCheck(expectedButtonText, buttonSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "there is no cya data" should {

      lazy val result = getResult(url(), None, None)

      "redirect the user to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe overviewUrl
      }
    }

    "there is cya data, but 'overseasDonationsViaGiftAidAmount' has not been stored" should {

      lazy val result =
        getResult(
          url(),
          Some(GiftAidCYAModel(overseasDonationsViaGiftAid = Some(true), donationsViaGiftAidAmount = Some(validAmount))),
          None
        )

      "redirect the user to the overseas donation amount page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOverseasAmountController.show(year)}"
      }
    }


  }

  ".submit" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an error" when {

          "the submitted data is empty" which {
            lazy val result = postResult(url(), requiredSessionData, None, Map("name" -> ""), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(errorPrefix(user.isWelsh) + user.specificExpectedResults.get.expectedTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(expectedInputHintText, inputHintTextSelector)
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, buttonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedError, Selectors.inputFieldSelector, user.isWelsh)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedError)
          }

          "the submitted data is too long" which {
            lazy val result = postResult(url(), requiredSessionData, None, Map("name" -> charLimit), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(errorPrefix(user.isWelsh) + user.specificExpectedResults.get.expectedTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(expectedInputHintText, inputHintTextSelector)
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, buttonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(expectedLengthError, Selectors.inputFieldSelector, user.isWelsh)
            errorAboveElementCheck(expectedLengthError)
          }

          "the submitted data is in the incorrect format" which {
            lazy val result = postResult(url(), requiredSessionData, None, Map("name" -> ":@~{}<>?"), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(errorPrefix(user.isWelsh) + user.specificExpectedResults.get.expectedTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(expectedInputHintText, inputHintTextSelector)
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, buttonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(expectedInvalidError, Selectors.inputFieldSelector, user.isWelsh)
            errorAboveElementCheck(expectedInvalidError)
          }

          "the submitted data is a duplicate name" which {
            val cyaModel = requiredSessionModel.copy(overseasCharityNames = Some(Seq("Dudes In Need")))
            lazy val result = postResult(url(), Some(cyaModel), None, Map("name" -> "Dudes In Need"), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(errorPrefix(user.isWelsh) + user.specificExpectedResults.get.expectedTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(expectedInputHintText, inputHintTextSelector)
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, buttonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(expectedDuplicateError, Selectors.inputFieldSelector, user.isWelsh)
            errorAboveElementCheck(expectedDuplicateError)
          }
        }
      }
    }

    "there is no cya data stored" should {

      lazy val result = postResult(url(), None, None, Map("amount" -> s"$validAmount"))

      "redirect the user to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${appConfig.incomeTaxSubmissionOverviewUrl(year)}"
      }
    }

    "the user enters a valid name" when {
      lazy val result = postResult(url(), requiredSessionData, None, Map("name" -> "Dudes In Need"))

      "redirect the user to the 'overseas charity summary' page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.OverseasGiftAidSummaryController.show(year)}"
      }

      "store the data" in {
        findGiftAidDb shouldBe Some(requiredSessionModel.copy(overseasCharityNames = Some(Seq("Dudes In Need"))))
      }
    }

    "the user enters a valid name and is changing an existing name" when {
      lazy val result = postResult(url(Some("Dudes")),
        Some(requiredSessionModel.copy(overseasCharityNames = Some(Seq("Dudes")))),
        None, Map("name" -> "Dudess"))

      "redirect the user to the 'overseas charity summary' page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.OverseasGiftAidSummaryController.show(year)}"
      }

      "store the data" in {
        findGiftAidDb shouldBe Some(requiredSessionModel.copy(overseasCharityNames = Some(Seq("Dudess"))))
      }
    }
  }
}