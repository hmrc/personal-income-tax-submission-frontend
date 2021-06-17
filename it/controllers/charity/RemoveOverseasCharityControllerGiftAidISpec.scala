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
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, ViewHelpers}

class RemoveOverseasCharityControllerGiftAidISpec extends IntegrationTest with ViewHelpers {

  object Selectors {
    val heading = "h1"
    val caption = ".govuk-caption-l"
    val content = "#main-content > div > div > form > div > fieldset > legend > p"
    val errorSummaryNoSelection = ".govuk-error-summary__body > ul > li > a"
    val yesRadioButton = ".govuk-radios__item:nth-child(1) > label"
    val noRadioButton = ".govuk-radios__item:nth-child(2) > label"
    val errorHref = "#value"
  }

  val charityName = "TestCharity"
  val taxYear: Int = 2022

  def url: String = s"$appUrl/$taxYear/charity/remove-overseas-charity-gift-aid?charityName=$charityName"

  trait CommonExpectedResults {
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedH1: String
    val expectedContent: String
    val expectedCaption: String
    val noSelectionError: String
    val yesText: String
    val noText: String
    val button: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedTitle = s"Are you sure you want to remove $charityName?"
    val expectedErrorTitle = "Select yes to remove this overseas charity"
    val expectedH1 = s"Are you sure you want to remove $charityName?"
    val expectedContent = "This will remove all overseas charities."
    val expectedCaption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val noSelectionError = "Select yes to remove this overseas charity"
    val yesText = "Yes"
    val noText = "No"
    val button = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val charityName = "TestCharity"
    val expectedTitle = s"Are you sure you want to remove $charityName?"
    val expectedErrorTitle = "Select yes to remove this overseas charity"
    val expectedH1 = s"Are you sure you want to remove $charityName?"
    val expectedContent = "This will remove all overseas charities."
    val expectedCaption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val noSelectionError = "Select yes to remove this overseas charity"
    val yesText = "Yes"
    val noText = "No"
    val button = "Continue"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, CommonExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, None),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, None),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, None),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, None))
  }

  ".show" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the page with correct content" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import user.commonExpectedResults._

          titleCheck(expectedTitle)
          h1Check(expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedContent, Selectors.content)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          captionCheck(expectedCaption)
          buttonCheck(button)
          noErrorsCheck()
          welshToggleCheck(user.isWelsh)
        }
      }
    }
  }

  ".submit" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an OK" in {
          lazy val form: Map[String, Seq[String]] = Map(YesNoForm.yesNo -> Seq(YesNoForm.yes))

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(url, body = form, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          result.status shouldBe OK
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

            titleCheck(errorPrefix + expectedTitle)
            h1Check(expectedH1 + " " + expectedCaption)
            radioButtonCheck(yesText, 1)
            radioButtonCheck(noText, 2)
            captionCheck(expectedCaption)
            buttonCheck(button)
            welshToggleCheck(user.isWelsh)
            errorSummaryCheck(expectedErrorTitle, Selectors.errorHref)
            errorAboveElementCheck(expectedErrorTitle)
          }
        }
      }
    }
  }
}

