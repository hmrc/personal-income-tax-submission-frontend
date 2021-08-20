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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.CharityITHelper

class OverseasGiftAidSummaryControllerISpec extends CharityITHelper {

  object Selectors {
    val question = ".govuk-fieldset__legend"
  }

  val charity1 = "Pirate leg replacement fund"
  val charity2 = "Save the Rathalos foundation"

  def url: String = s"$appUrl/$year/charity/overseas-charities-donated-to"

  trait SpecificExpectedResults {
    val headingSingle: String
    val headingMultiple: String
    val hint: String
  }

  trait CommonExpectedResults {
    val caption: String
    val question: String
    val yes: String
    val no: String
    val errorSummary: String
    val change: String
    val remove: String
    val hiddenChange1: String
    val hiddenRemove1: String
    val hiddenChange2: String
    val hiddenRemove2: String
    val noSelectionError: String
    val button: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val caption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val question = "Do you need to add another overseas charity?"
    val yes = "Yes"
    val no = "No"
    val errorSummary = "There is a problem"
    val change = "Change"
    val remove = "Remove"
    val hiddenChange1 = s"Change the details you’ve entered for $charity1."
    val hiddenRemove1 = s"Remove $charity1."
    val hiddenChange2 = s"Change the details you’ve entered for $charity2."
    val hiddenRemove2 = s"Remove $charity2."
    val noSelectionError = "Select yes if you need to add another overseas charity"
    val button = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val caption = "Rhoddion i elusennau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022"
    val question = "Do you need to add another overseas charity?"
    val yes = "Iawn"
    val no = "Na"
    val errorSummary = "There is a problem"
    val change = "Change"
    val remove = "Remove"
    val hiddenChange1 = s"Change the details you’ve entered for $charity1."
    val hiddenRemove1 = s"Remove $charity1."
    val hiddenChange2 = s"Change the details you’ve entered for $charity2."
    val hiddenRemove2 = s"Remove $charity2."
    val noSelectionError = "Select yes if you need to add another overseas charity"
    val button = "Yn eich blaen"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val headingSingle = "Overseas charities you used Gift Aid to donate to"
    val headingMultiple = "Overseas charities you used Gift Aid to donate to"
    val hint = "You must tell us about all the overseas charities you donated to."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val headingSingle = "Overseas charities your client used Gift Aid to donate to"
    val headingMultiple = "Overseas charities your client used Gift Aid to donate to"
    val hint = "You must tell us about all the overseas charities your client donated to."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val headingSingle = "Overseas charities you used Gift Aid to donate to"
    val headingMultiple = "Overseas charities you used Gift Aid to donate to"
    val hint = "You must tell us about all the overseas charities you donated to."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val headingSingle = "Overseas charities your client used Gift Aid to donate to"
    val headingMultiple = "Overseas charities your client used Gift Aid to donate to"
    val hint = "You must tell us about all the overseas charities your client donated to."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  val requiredSessionModel: GiftAidCYAModel = GiftAidCYAModel(overseasCharityNames = Some(Seq(charity1, charity2)))
  val requiredSessionData: Option[GiftAidCYAModel] = Some(requiredSessionModel)

  ".show" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the page with correct content with multiple charities" which {
          lazy val result = getResult(url, requiredSessionData, None, user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.headingMultiple, user.isWelsh)
          h1Check(s"${user.specificExpectedResults.get.headingMultiple} $caption")
          captionCheck(caption)
          taskListCheck(Seq((charity1, hiddenChange1, hiddenRemove1)), user.isWelsh)
          textOnPageCheck(question, Selectors.question)
          radioButtonCheck(user.commonExpectedResults.yes, 1)
          radioButtonCheck(user.commonExpectedResults.no, 2)
          hintTextCheck(user.specificExpectedResults.get.hint)
          buttonCheck(button)
          noErrorsCheck()
        }
      }
    }

    "there is no cya data" should {

      lazy val result: WSResponse = getResult(url, None, None)

      "redirect the user to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe overviewUrl
      }
    }

    "there is cya data, but 'overseasCharityNames' has not been stored" should {
      lazy val result: WSResponse = getResult(url, Some(GiftAidCYAModel(overseasDonationsViaGiftAidAmount = Some(50))), None)

      "redirect the user to the overseas charity name page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOverseasNameController.show(year, None)}"
      }
    }
  }

  ".submit" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an error" when {

          "the submitted data is empty" which {
            lazy val result = postResult(url, requiredSessionData, None, Map(YesNoForm.yesNo -> ""), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import user.commonExpectedResults._

            titleCheck(errorPrefix(user.isWelsh) + user.specificExpectedResults.get.headingMultiple, user.isWelsh)
            h1Check(s"${user.specificExpectedResults.get.headingMultiple} $caption")
            radioButtonCheck(user.commonExpectedResults.yes, 1)
            radioButtonCheck(user.commonExpectedResults.no, 2)
            hintTextCheck(user.specificExpectedResults.get.hint)
            captionCheck(caption)
            buttonCheck(button)
            errorSummaryCheck(noSelectionError, "#value", user.isWelsh)
            errorAboveElementCheck(noSelectionError)
            welshToggleCheck(user.isWelsh)
          }
        }
      }
    }

    "there is no cya data stored" should {

      lazy val result: WSResponse = postResult(url, None, None, Map(YesNoForm.yesNo -> YesNoForm.yes))

      "redirect the user to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe overviewUrl
      }
    }

    "the user has selected 'Yes'" should {
      lazy val result = postResult(url, requiredSessionData, None, Map(YesNoForm.yesNo -> YesNoForm.yes))

      "redirect the user to the overseas charity name page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOverseasNameController.show(year, None)}"
      }
    }

    "the user selects 'no'" when {

      "this completes the cya model" should {

        lazy val result = postResult(url, Some(completeGiftAidCYAModel), None, Map(YesNoForm.yesNo -> YesNoForm.no))

        "redirect the user to the 'check your answers' page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(year)}"
        }
      }

      "this does not complete the cya model" should {

        lazy val result = postResult(url, requiredSessionData, None, Map(YesNoForm.yesNo -> YesNoForm.no))

        "redirect the user to the 'Add donation to last tax year' page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidLastTaxYearController.show(year)}"
        }
      }
    }
  }
}