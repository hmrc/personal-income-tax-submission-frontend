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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, ViewHelpers}

class OverseasSharesLandSummaryControllerISpec  extends IntegrationTest with ViewHelpers {

  object Selectors {
    val question = ".govuk-fieldset__legend"
  }

  val charity1 = "overseasCharity1"
  val charity2 = "overseasCharity2"

  val taxYear: Int = 2022

  def url: String = s"$appUrl/$taxYear/charity/overseas-charities-donated-shares-securities-land-or-property-to"

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

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val headingSingle = "Overseas charity you donated shares, securities, land or property to"
    val headingMultiple = "Overseas charities you donated shares, securities, land or property to"
    val hint = "You must tell us about all the overseas charities you donated shares, securities, land or property to."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val headingSingle = "Overseas charity your client donated shares, securities, land or property to"
    val headingMultiple = "Overseas charities your client donated shares, securities, land or property to"
    val hint = "You must tell us about all the overseas charities your client donated shares, securities, land or property to."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val headingSingle = "Overseas charity you donated shares, securities, land or property to"
    val headingMultiple = "Overseas charities you donated shares, securities, land or property to"
    val hint = "You must tell us about all the overseas charities you donated shares, securities, land or property to."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val headingSingle = "Overseas charity your client donated shares, securities, land or property to"
    val headingMultiple = "Overseas charities your client donated shares, securities, land or property to"
    val hint = "You must tell us about all the overseas charities your client donated shares, securities, land or property to."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  ".show" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the page with correct content with single charity" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.headingSingle)
          h1Check(s"${user.specificExpectedResults.get.headingSingle} $caption")
          captionCheck(caption)
          taskListCheck(Seq((charity1, hiddenChange1, hiddenRemove1)))
          textOnPageCheck(question, Selectors.question)
          radioButtonCheck(user.commonExpectedResults.yes, 1)
          radioButtonCheck(user.commonExpectedResults.no, 2)
          hintTextCheck(user.specificExpectedResults.get.hint)
          buttonCheck(button)
          noErrorsCheck()
        }
      }
    }
  }

  ".submit" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return SEE_OTHER" in {
          lazy val form: Map[String, Seq[String]] = Map(YesNoForm.yesNo -> Seq(YesNoForm.yes))

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(url, body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          result.status shouldBe SEE_OTHER
        }

        "return an error" when {

          "the submitted data is empty" which {
            lazy val form: Map[String, Seq[String]] = Map(YesNoForm.yesNo -> Seq(""))

            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(url, body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import user.commonExpectedResults._

            titleCheck(errorPrefix + user.specificExpectedResults.get.headingSingle)
            h1Check(s"${user.specificExpectedResults.get.headingSingle} $caption")
            radioButtonCheck(user.commonExpectedResults.yes, 1)
            radioButtonCheck(user.commonExpectedResults.no, 2)
            hintTextCheck(user.specificExpectedResults.get.hint)
            captionCheck(caption)
            buttonCheck(button)
            errorSummaryCheck(noSelectionError, "#value")
            errorAboveElementCheck(noSelectionError)
            welshToggleCheck(user.isWelsh)
          }
        }
      }
    }
  }
}
