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
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, ViewHelpers}

class DonationsToPreviousTaxYearControllerISpec extends IntegrationTest with ViewHelpers {

  lazy val controller: DonationsToPreviousTaxYearController = app.injector.instanceOf[DonationsToPreviousTaxYearController]

  val taxYear: Int = 2022
  def url(overrideYear: Int = taxYear): String = s"$appUrl/$taxYear/charity/donations-after-5-april-$overrideYear"
  val urlWithSameYears = "/income-through-software/return/personal-income/2022/charity/donations-after-5-april-2022"

  object Selectors {
    val paragraph1HintText = "#main-content > div > div > form > div > fieldset > legend > div > p:nth-child(1)"
    val paragraph2HintText = "#main-content > div > div > form > div > fieldset > legend > div > p:nth-child(2)"
    val errorHref = "#value"
  }

  trait SpecificExpectedResults {
    val errorText: String
    val expectedParagraph1: String
    val expectedParagraph2: String
  }

  trait CommonExpectedResults {
    val expectedHeading: String
    val expectedCaption: String
    val yesText: String
    val noText: String
    val button: String
    val error: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedHeading = "Do you want to add any donations made after 5 April 2022 to this tax year?"
    val expectedCaption = s"Donations to charity for 6 April 2021 to 5 April 2022"
    val yesText = "Yes"
    val noText = "No"
    val button = "Continue"
    val error = "Error: "
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedHeading = "Do you want to add any donations made after 5 April 2022 to this tax year?"
    val expectedCaption = s"Donations to charity for 6 April 2021 to 5 April 2022"
    val yesText = "Yes"
    val noText = "No"
    val button = "Continue"
    val error = "Error: "
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val errorText = "Select yes to add any of your donations made after 5 April 2022 to this tax year"
    val expectedParagraph1: String = "If you made donations after 5 April 2022, you can add them to the 6 April 2021 to 5 April 2022 tax year."
    val expectedParagraph2: String = "You might want to do this if you want tax relief sooner."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val errorText = "Select yes to add any of your client’s donations made after 5 April 2022 to this tax year"
    val expectedParagraph1: String = "If your client made donations after 5 April 2022, you can add them to the 6 April 2021 to 5 April 2022 tax year."
    val expectedParagraph2: String = "You might want to do this if your client wants tax relief sooner."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val errorText = "Select yes to add any of your donations made after 5 April 2022 to this tax year"
    val expectedParagraph1: String = "If you made donations after 5 April 2022, you can add them to the 6 April 2021 to 5 April 2022 tax year."
    val expectedParagraph2: String = "You might want to do this if you want tax relief sooner."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val errorText = "Select yes to add any of your client’s donations made after 5 April 2022 to this tax year"
    val expectedParagraph1: String = "If your client made donations after 5 April 2022, you can add them to the 6 April 2021 to 5 April 2022 tax year."
    val expectedParagraph2: String = "You might want to do this if your client wants tax relief sooner."
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

        "redirect to a correct URL when years don't match up" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url(taxYear + 1), follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").headOption shouldBe Some(urlWithSameYears)
          }
        }

        "render the page with correct content" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url(taxYear), welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import user.commonExpectedResults._

          titleCheck(expectedHeading)
          h1Check(expectedHeading + " " + expectedCaption)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph1, Selectors.paragraph1HintText)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph2, Selectors.paragraph2HintText)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          captionCheck(expectedCaption)
          buttonCheck(button)
          welshToggleCheck(user.isWelsh)
        }
      }
    }
  }

  ".submit" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "redirect to a correct URL when years don't match up" which {
          lazy val form: Map[String, Seq[String]] = Map(YesNoForm.yesNo -> Seq(YesNoForm.yes))

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(url(taxYear + 1), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").headOption shouldBe Some(urlWithSameYears)
          }
        }

        "return an OK" in {
          lazy val form: Map[String, Seq[String]] = Map(YesNoForm.yesNo -> Seq(YesNoForm.yes))

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(url(taxYear), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          result.status shouldBe OK
        }

        "no radio button has been selected" should {

          lazy val form: Map[String, Seq[String]] = Map(YesNoForm.yesNo -> Seq(""))

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(url(taxYear), body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import user.commonExpectedResults._

          titleCheck(error + expectedHeading)
          h1Check(expectedHeading + " " + expectedCaption)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph1, Selectors.paragraph1HintText)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph2, Selectors.paragraph2HintText)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          captionCheck(expectedCaption)
          buttonCheck(button)
          errorSummaryCheck(user.specificExpectedResults.get.errorText, Selectors.errorHref)
          errorAboveElementCheck(user.specificExpectedResults.get.errorText)
          welshToggleCheck(user.isWelsh)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }
      }
    }
  }
}
