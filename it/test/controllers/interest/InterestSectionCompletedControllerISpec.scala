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

package controllers.interest

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import play.api.mvc.Headers
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, route}
import play.mvc.Http.HeaderNames
import test.utils.{IntegrationTest, InterestDatabaseHelper, ViewHelpers}


class InterestSectionCompletedControllerISpec extends IntegrationTest with InterestDatabaseHelper with ViewHelpers {

  val relativeUrl = s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/section-completed"
  val absoluteUrl: String = appUrl + relativeUrl

  object Selectors {
    val yesSelector = "#main-content > div > div > form > div > fieldset > div.govuk-radios.govuk-radios--inline > div:nth-child(1) > label"
    val noSelector = "#main-content > div > div > form > div > fieldset > div.govuk-radios.govuk-radios--inline > div:nth-child(2) > label"

    val formSelector = "#main-content > div > div > form"
    val valueHref = "#value"
    val continueSelector = "#continue"
  }

  trait CommonExpectedResults {
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedRadioHeading: String
    val expectedErrorText: String
    val yesText: String
    val noText: String
    val expectedButtonText: String
    val expectedHelpLinkText: String
    val expectedHint: String
  }

  object CommonExpectedResultsEN extends CommonExpectedResults {
    override val expectedTitle: String = "Have you finished this section?"
    override val expectedErrorTitle: String = "Error: Have you finished this section?"
    override val expectedRadioHeading: String = "Have you finished this section?"
    override val yesText: String = "Yes"
    override val noText: String = "No"
    override val expectedErrorText: String = "Select if you’ve completed this section"
    override val expectedButtonText: String = "Continue"
    override val expectedHelpLinkText: String = "Get help with this page"
    override val expectedHint: String = "You’ll still be able to go back and review the information that you’ve given us."
  }

  object CommonExpectedResultsCY extends CommonExpectedResults {
    override val expectedTitle: String = "A ydych wedi gorffen yr adran hon?"
    override val expectedErrorTitle: String = "Gwall: A ydych wedi gorffen yr adran hon?"
    override val expectedRadioHeading: String = "A ydych wedi gorffen yr adran hon?"
    override val yesText: String = "Iawn"
    override val noText: String = "Na"
    override val expectedButtonText: String = "Yn eich blaen"
    override val expectedHelpLinkText: String = "Help gyda’r dudalen hon"
    override val expectedErrorText: String = "Dewiswch a ydych wedi llenwi’r adran hon"
    override val expectedHint: String = "Byddwch yn dal i allu mynd yn ôl ac adolygu’r wybodaeth rydych wedi’i rhoi i ni."
  }

 //TODO: Include agent scenarios (failing as it does not redirect correctly)
  private val userScenarios = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedResultsEN),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedResultsCY)
  )

  s".show" when {
    userScenarios.foreach { scenario =>
      import scenario.commonExpectedResults._

      val testNameWelsh = if (scenario.isWelsh) "in Welsh" else "in English"
      val testNameAgent = if (scenario.isAgent) "is an Agent" else "is an Individual"

      s"$testNameWelsh and the user $testNameAgent" should {

        "display the section completed page" which {
          lazy val headers = playSessionCookie() ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
          lazy val request = FakeRequest("GET", relativeUrl).withHeaders(headers: _*)

          lazy val result = {
            authoriseAgentOrIndividual(scenario.isAgent)
            dropInterestDB()
            emptyUserDataStub()
            route(appWithCommonTaskList, request, "{}").get
          }

          implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

          "have a status of OK(200)" in {
            status(result) shouldBe OK
          }

          titleCheck(expectedTitle, scenario.isWelsh)
          welshToggleCheck(scenario.isWelsh)
          h1Check(s"${expectedTitle}")
          hintTextCheck(expectedHint)
          formPostLinkCheck(relativeUrl, Selectors.formSelector)
          textOnPageCheck(yesText, Selectors.yesSelector)
          textOnPageCheck(noText, Selectors.noSelector)
          buttonCheck(expectedButtonText)
        }
      }
    }
  }


  s".submit" when {
    userScenarios.foreach {
      scenario =>
        import scenario.commonExpectedResults._

        val testNameWelsh = if (scenario.isWelsh) "in Welsh" else "in English"

        s"the request is $testNameWelsh" should {

          "the user submit incorrect data, the page" should {

            "display an error" which {
              lazy val headers = playSessionCookie() ++ Map(csrfContent) ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
              lazy val request = FakeRequest("POST", s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/section-completed")
                .withHeaders(headers: _*)

              lazy val result = {
                authoriseAgentOrIndividual(isAgent = false)
                dropInterestDB()
                emptyUserDataStub()
                route(appWithTailoring, request, Map("value" -> Seq("error"))).get
              }

              implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

              "has a status of BAD_REQUEST" in {
                status(result) shouldBe BAD_REQUEST
              }

              titleCheck(expectedTitle, scenario.isWelsh)
              welshToggleCheck(scenario.isWelsh)
              h1Check(s"$expectedTitle")
              hintTextCheck(expectedHint)
              formPostLinkCheck(relativeUrl, Selectors.formSelector)
              textOnPageCheck(yesText, Selectors.yesSelector)
              textOnPageCheck(noText, Selectors.noSelector)
              buttonCheck(expectedButtonText)
              errorSummaryCheck(expectedErrorText, "#value", scenario.isWelsh)
              errorAboveElementCheck(expectedErrorText)
            }
          }

          "redirect to the overview page if user chooses 'No'" which {
            lazy val result = {
              authoriseAgentOrIndividual(isAgent = false)
              dropInterestDB()
              emptyUserDataStub()
              val request = FakeRequest("POST", s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/section-completed",
                Headers.apply(playSessionCookie() :+ ("Csrf-Token" -> "nocheck"): _*), "{}")

              await(route(appWithCommonTaskList, request, Map("value" -> Seq("false"))).get)
            }

            "has a status of SEE_OTHER(303)" in {
              result.header.status shouldBe SEE_OTHER
            }

            "have the correct redirect location" in {
              result.header.headers("location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
            }
          }

          "redirect to the overview page if user chooses 'Yes'" which {
            lazy val result = {
              authoriseAgentOrIndividual(isAgent = false)
              dropInterestDB()
              emptyUserDataStub()
              val request = FakeRequest("POST", s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/section-completed",
                Headers.apply(playSessionCookie() :+ ("Csrf-Token" -> "nocheck"): _*), "{}")

              await(route(appWithCommonTaskList, request, Map("value" -> Seq("true"))).get)
            }

            "has a status of SEE_OTHER(303)" in {
              result.header.status shouldBe SEE_OTHER
            }

            "have the correct redirect location" in {
              result.header.headers("location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
            }
          }
      }
    }
  }
}
