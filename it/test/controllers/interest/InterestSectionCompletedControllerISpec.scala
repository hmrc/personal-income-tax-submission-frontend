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
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.mvc.Headers
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, route}
import test.utils.{IntegrationTest, InterestDatabaseHelper, ViewHelpers}


class InterestSectionCompletedControllerISpec extends IntegrationTest with InterestDatabaseHelper with ViewHelpers {

  val relativeUrl = s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/section-completed"
  val absoluteUrl: String = appUrl + relativeUrl

  object Selectors {
    val yesSelector = "#main-content > div > div > form > div > fieldset > div.govuk-radios.govuk-radios--inline > div:nth-child(1) > label"
    val noSelector = "#main-content > div > div > form > div > fieldset > div.govuk-radios.govuk-radios--inline > div:nth-child(2) > label"

    val formSelector = "#main-content > div > div > form"
  }

  trait CommonExpectedResults {
    val hintText: String
    val continueText: String
    val yesText: String
    val noText: String
  }

  trait SpecificUserTypeResults {
    val heading: String
    val errorText: String
    val errorTitle: String
  }

  object CommonExpectedResultsEN extends CommonExpectedResults {
    override val hintText = "You’ll still be able to go back and review the information that you’ve given us."
    override val continueText: String = "Continue"
    override val yesText: String = "Yes"
    override val noText: String = "No"
  }

  object CommonExpectedResultsCY extends CommonExpectedResults {
    override val hintText = "Byddwch yn dal i allu mynd yn ôl ac adolygu’r wybodaeth rydych wedi’i rhoi i ni."
    override val continueText: String = "Yn eich blaen"
    override val yesText: String = "Iawn"
    override val noText: String = "Na"
  }

  object IndividualResultsEN extends SpecificUserTypeResults {
    override val heading: String = "Have you finished this section?"
    override val errorText: String = "Select if you’ve completed this section"
    override val errorTitle: String = "Error: Have you finished this section?"

  }

  object AgentResultsEN extends SpecificUserTypeResults {
    override val heading: String = "Have you finished this section?"
    override val errorText: String = "Select if you’ve completed this section"
    override val errorTitle: String = "Error: Have you finished this section?"

  }

  object IndividualResultsCY extends SpecificUserTypeResults {
    override val heading: String = "A ydych wedi gorffen yr adran hon?"
    override val errorText: String = "Dewiswch a ydych wedi llenwi’r adran hon"
    override val errorTitle: String = "Gwall: A ydych wedi gorffen yr adran hon?"

  }

  object AgentResultsCY extends SpecificUserTypeResults {
    override val heading: String = "A ydych wedi gorffen yr adran hon?"
    override val errorText: String = "Dewiswch a ydych wedi llenwi’r adran hon"
    override val errorTitle: String = "Gwall: A ydych wedi gorffen yr adran hon?"

  }

  private val userScenarios = Seq(
    UserScenario(isWelsh = false, isAgent = false, commonExpectedResults = CommonExpectedResultsEN, Some(IndividualResultsEN)),
    UserScenario(isWelsh = true, isAgent = false, commonExpectedResults = CommonExpectedResultsCY, Some(IndividualResultsCY))
  )

  userScenarios.foreach { scenario =>
    lazy val uniqueResults = scenario.specificExpectedResults.get

    import scenario.commonExpectedResults._
    import uniqueResults._

    val testNameWelsh = if (scenario.isWelsh) "in Welsh" else "in English"


    s".show when $testNameWelsh" when {

      s"display the gateway page in $testNameWelsh" which {

        lazy val headers = playSessionCookie() ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
        lazy val request = FakeRequest("GET", relativeUrl).withHeaders(headers: _*)

        lazy val result = {
          authoriseIndividual()
          dropInterestDB()
          emptyUserDataStub()
          route(appWithCommonTaskList, request, "{}").get
        }

        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of OK(200)" in {
          status(result) shouldBe OK
        }

        titleCheck(heading, scenario.isWelsh)
        h1Check(s"$heading")
        hintTextCheck(hintText)
        formPostLinkCheck(relativeUrl, Selectors.formSelector)
        textOnPageCheck(yesText, Selectors.yesSelector)
        textOnPageCheck(noText, Selectors.noSelector)
        buttonCheck(continueText)
      }
    }

    s".submit when $testNameWelsh" when {

      "redirect to the overview page" which {
        lazy val result = {
          authoriseIndividual()
          dropInterestDB()
          emptyUserDataStub()
          emptyStockDividendsUserDataStub()
          val request = FakeRequest("POST", s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/section-completed",
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
