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

package test.controllers.savings

import models.savings.SavingsIncomeCYAModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, route}
import test.utils.{IntegrationTest, SavingsDatabaseHelper, ViewHelpers}

class TaxTakenFromInterestControllerISpec extends IntegrationTest with ViewHelpers with SavingsDatabaseHelper {

  val relativeUrl: String = s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/tax-taken-from-interest"

  val cyaDataValid: Option[SavingsIncomeCYAModel] = Some(SavingsIncomeCYAModel(Some(true), Some(100.00)))

  object Selectors {
    val captionSelector = ".govuk-caption-l"
    val yesSelector = "#main-content > div > div > form > div > fieldset > div > div:nth-child(1)"
    val noSelector = "#main-content > div > div > form > div > fieldset > div > div:nth-child(2)"
    val formSelector = "#main-content > div > div > form"
    val heading = "#main-content > div > div > h1"
    val continueSelector = "#continue"
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val yesText: String
    val noText: String
    val continueText: String
    val errorText: String
  }

  trait SpecificUserTypeResults {
    val heading: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: String = s"Interest from gilt-edged or accrued income securities for 6 April $taxYearEOY to 5 April $taxYear"
    val yesText: String = "Yes"
    val noText: String = "No"
    val continueText: String = "Continue"
    val errorText: String = "Select yes if tax was taken off the interest"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: String = s"Llog o warantau gilt neu warantau incwm cronedig ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val yesText: String = "Iawn"
    val noText: String = "Na"
    val continueText: String = "Yn eich blaen"
    val errorText: String = "Dewiswch ‘Iawn’ os cafodd treth ei didynnu oddi wrth y llog"
  }

  object IndividualResultsEN extends SpecificUserTypeResults {
    override val heading: String = "Was tax taken off your interest?"
  }

  object AgentResultsEN extends SpecificUserTypeResults {
    override val heading: String = "Was tax taken off your client’s interest?"
  }

  object IndividualResultsCY extends SpecificUserTypeResults {
    override val heading: String = "A gafodd treth ei didynnu oddi wrth eich llog?"
  }

  object AgentResultsCY extends SpecificUserTypeResults {
    override val heading: String = "A gafodd treth ei didynnu oddi wrth log eich cleient?"
  }

  private val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificUserTypeResults]] = {
    Seq(
      UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(IndividualResultsEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(AgentResultsEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(IndividualResultsCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(AgentResultsCY))
    )
  }

  userScenarios.foreach { scenario =>
    import scenario.commonExpectedResults._
    lazy val specificResults = scenario.specificExpectedResults.get


    val testNameWelsh = if (scenario.isWelsh) "in Welsh" else "in English"
    val testNameAgent = if (scenario.isAgent) "an agent" else "an individual"

    s".show when $testNameWelsh and the user is $testNameAgent" should {

      "display the tax taken from interest page without prefill" which {

        lazy val headers = playSessionCookie(scenario.isAgent) ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
        lazy val request = FakeRequest("GET", relativeUrl).withHeaders(headers: _*)

        lazy val result = {
          authoriseAgentOrIndividual(scenario.isAgent)
          dropSavingsDB()
          emptyUserDataStub()
          insertSavingsCyaData(Some(SavingsIncomeCYAModel(Some(true), Some(100.00), Some(false))))
          route(app, request, "{}").get
        }

        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of OK(200)" in {
          status(result) shouldBe OK
        }

        titleCheck(specificResults.heading, scenario.isWelsh)
        welshToggleCheck(scenario.isWelsh)
        h1Check(specificResults.heading + " " + expectedCaption)
        textOnPageCheck(expectedCaption, Selectors.captionSelector)

        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, Selectors.continueSelector)

      }
      "display the tax taken from interest page prefilled" which {

        lazy val headers = playSessionCookie(scenario.isAgent) ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
        lazy val request = FakeRequest("GET", relativeUrl).withHeaders(headers: _*)

        lazy val result = {
          authoriseAgentOrIndividual(scenario.isAgent)
          dropSavingsDB()
          emptyUserDataStub()
          insertSavingsCyaData(cyaDataValid)
          route(app, request, "{}").get
        }

        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of OK(200)" in {
          status(result) shouldBe OK
        }

        titleCheck(specificResults.heading, scenario.isWelsh)
        welshToggleCheck(scenario.isWelsh)
        h1Check(specificResults.heading + " " + expectedCaption)
        textOnPageCheck(expectedCaption, Selectors.captionSelector)

        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, Selectors.continueSelector)

      }
      "display the tax taken from interest page when cyaData is empty" which {

        lazy val headers = playSessionCookie(scenario.isAgent) ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
        lazy val request = FakeRequest("GET", relativeUrl).withHeaders(headers: _*)

        lazy val result = {
          authoriseAgentOrIndividual(scenario.isAgent)
          dropSavingsDB()
          emptyUserDataStub()
          insertSavingsCyaData(None)
          route(app, request, "{}").get
        }

        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of OK(200)" in {
          status(result) shouldBe OK
        }

        titleCheck(specificResults.heading, scenario.isWelsh)
        welshToggleCheck(scenario.isWelsh)
        h1Check(specificResults.heading + " " + expectedCaption)
        textOnPageCheck(expectedCaption, Selectors.captionSelector)

        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, Selectors.continueSelector)

      }
      "redirect to the overview page" which {

        lazy val headers = playSessionCookie(scenario.isAgent) ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
        lazy val request = FakeRequest("GET", relativeUrl).withHeaders(headers: _*)

        lazy val result = {
          authoriseAgentOrIndividual(scenario.isAgent)
          dropSavingsDB()
          emptyUserDataStub()
          route(app, request, "{}").get
        }

        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of Redirect(303)" in {
          status(result) shouldBe SEE_OTHER
          await(result).header.headers("Location") shouldBe s"${appConfig.incomeTaxSubmissionOverviewUrl(taxYear)}"
        }

      }

    }

    s".submit when $testNameWelsh and the user is $testNameAgent" should {
      lazy val headers = playSessionCookie(scenario.isAgent) ++ Map(csrfContent) ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
      lazy val request = FakeRequest("POST", relativeUrl).withHeaders(headers: _*)

        "redirect to cya" which {

          lazy val result = {
            authoriseAgentOrIndividual(scenario.isAgent)
            dropSavingsDB()
            emptyUserDataStub()
            insertSavingsCyaData(cyaDataValid)
            route(app, request, Map("value" -> Seq("false"))).get
          }
          implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

          "has a status of Redirect(303)" in {
            status(result) shouldBe SEE_OTHER
            await(result).header.headers("Location") shouldBe s"${controllers.savings.routes.InterestSecuritiesCYAController.show(taxYear)}"
          }
      }
        "redirect to next taxTakenAmount page" which {

          lazy val result = {
            authoriseAgentOrIndividual(scenario.isAgent)
            dropSavingsDB()
            emptyUserDataStub()
            insertSavingsCyaData(cyaDataValid)
            route(app, request, Map("value" -> Seq("true"))).get
          }
          implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

          "has a status of Redirect(303)" in {
            status(result) shouldBe SEE_OTHER
            await(result).header.headers("Location") shouldBe s"${controllers.savings.routes.TaxTakenOffInterestController.show(taxYear)}"
          }
      }

      "the feature switch is on and user submits incorrect data" should {

        "display an error" which {

          lazy val result = {
            authoriseAgentOrIndividual(scenario.isAgent)
            dropSavingsDB()
            emptyUserDataStub()
            insertSavingsCyaData(cyaDataValid)
            route(app, request, Map("value" -> Seq("error"))).get
          }
          implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

          "has a status of BAD_REQUEST" in {
            status(result) shouldBe BAD_REQUEST
          }

          errorSummaryCheck(errorText, "#value", scenario.isWelsh)
        }
      }
    }

  }

}