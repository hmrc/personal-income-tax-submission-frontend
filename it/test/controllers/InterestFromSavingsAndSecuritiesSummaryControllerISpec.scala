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

package test.controllers

import models.interest.InterestCYAModel
import models.priorDataModels.IncomeSourcesModel
import models.savings.SavingsIncomeCYAModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.DefaultBodyWritables
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, route}
import test.utils.{IntegrationTest, InterestDatabaseHelper, SavingsDatabaseHelper, ViewHelpers}


class InterestFromSavingsAndSecuritiesSummaryControllerISpec extends IntegrationTest with ViewHelpers with DefaultBodyWritables
  with SavingsDatabaseHelper with InterestDatabaseHelper {
  val relativeUrl: String = s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/interest-summary"
  val link1Url : String = s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/untaxed-uk-interest"
  val link2Url : String = s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/interest-from-securities"

  val cyaDataComplete: Option[SavingsIncomeCYAModel] = Some(SavingsIncomeCYAModel(Some(true), Some(100.00), Some(true), Some(100.00)))
  val cyaModel: InterestCYAModel = InterestCYAModel(
    None,
    untaxedUkInterest = Some(false),
    taxedUkInterest = Some(true)
  )
  object Selectors {
    val captionSelector = ".govuk-caption-l"
    val headingSelector = ".govuk-heading-l"
    val p1Selector = ".govuk-body"
    val titleSelector = "title"
    val Link1Selector = "#main-content > div > div > ol > li:nth-child(1) > span.app-task-list__task-name > a"
    val Link2Selector = "#main-content > div > div > ol > li:nth-child(2) > span.app-task-list__task-name > a"
    val returnToOverviewSelector = ".govuk-button"
  }

  trait CommonExpectedResults {
    val captionExpectedText: String
    val link1ExpectedText: String
    val link2ExpectedText: String
    val returnToOverviewExpectedText : String
  }

  object CommonExpectedResultsEN extends CommonExpectedResults {
    val link1ExpectedText = "UK Interest"
    val link2ExpectedText = "Interest from gilt-edged or accrued income securities"
    val captionExpectedText = s"Interest for 6 April $taxYearEOY to 5 April $taxYear"
    val returnToOverviewExpectedText = "Return to overview"
  }

  object CommonExpectedResultsCY extends CommonExpectedResults {
    val link1ExpectedText = "Llog y DU"
    val link2ExpectedText = "Llog o warantau gilt neu warantau incwm cronedig"
    val captionExpectedText = s"Llog ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val returnToOverviewExpectedText = "Yn ôl i’r trosolwg"
  }

  trait SpecificUserTypeResults {
    val headingExpectedText: String
    val expectedTitle: String
    val p1ExpectedText: String
  }

  object IndividualResultsEN extends SpecificUserTypeResults {
    val headingExpectedText = "Interest from savings and securities"
    val expectedTitle = "Interest from savings and securities"
    val p1ExpectedText = "You only need to fill in the sections that apply to you."
  }

  object AgentResultsEN extends SpecificUserTypeResults {
    val headingExpectedText = "Interest from savings and securities"
    val expectedTitle = "Interest from savings and securities"
    val p1ExpectedText = "You only need to fill in the sections that apply to your client."
  }

  object IndividualResultsCY extends SpecificUserTypeResults {
    val headingExpectedText = "Llog o gynilion a gwarantau"
    val expectedTitle = "Llog o gynilion a gwarantau"
    val p1ExpectedText = "Dim ond yr adrannau sy’n berthnasol i chi y mae angen i chi eu llenwi."
  }

  object AgentResultsCY extends SpecificUserTypeResults {
    val headingExpectedText = "Llog o gynilion a gwarantau"
    val expectedTitle = "Llog o gynilion a gwarantau"
    val p1ExpectedText = "Dim ond yr adrannau sy’n berthnasol i’ch cleient y mae angen i chi eu llenwi."
  }

  private val userScenarios = Seq(
    UserScenario(isWelsh = false, isAgent = false, CommonExpectedResultsEN, Some(IndividualResultsEN)),
    UserScenario(isWelsh = false, isAgent = true, CommonExpectedResultsEN, Some(AgentResultsEN)),
    UserScenario(isWelsh = true, isAgent = false, CommonExpectedResultsCY, Some(IndividualResultsCY)),
    UserScenario(isWelsh = true, isAgent = true, CommonExpectedResultsCY, Some(AgentResultsCY))
  )

  userScenarios.foreach { scenario =>

    lazy val uniqueResults = scenario.specificExpectedResults.get

    import scenario.commonExpectedResults._
    import uniqueResults._

    val testNameWelsh = if (scenario.isWelsh) "in Welsh" else "in English"
    val testNameAgent = if (scenario.isAgent) "an agent" else "an individual"

    s".show when $testNameWelsh and the user is $testNameAgent" should {

      "display the interest summary page" which {

        lazy val headers = playSessionCookie(scenario.isAgent) ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
        lazy val request = FakeRequest("GET", relativeUrl).withHeaders(headers: _*)
        val data = IncomeSourcesModel()

        lazy val result = {
          authoriseAgentOrIndividual(scenario.isAgent)
          emptyUserDataStub(nino, taxYear)
          userDataStub(data, nino, taxYear)
          route(app, request, "{}").get
        }

        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of OK(200)" in {
          status(result) shouldBe OK
        }

        titleCheck(expectedTitle, scenario.isWelsh)
        h1Check(headingExpectedText + " " + captionExpectedText)
        captionCheck(captionExpectedText)
        textOnPageCheck(p1ExpectedText, Selectors.p1Selector)
        linkCheck(link1ExpectedText, Selectors.Link1Selector, link1Url)
        linkCheck(link2ExpectedText, Selectors.Link2Selector, link2Url)
        buttonCheck(returnToOverviewExpectedText, Selectors.returnToOverviewSelector)
      }
      "the authorization fails" which {

        lazy val result = {
          unauthorisedAgentOrIndividual(scenario.isAgent)

          urlGet(s"$appUrl/$taxYear/interest/interest-summary", scenario.isWelsh, follow = true, playSessionCookie(scenario.isAgent))
        }

        s"has an Unauthorised($UNAUTHORIZED) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }
    }
  }
}
