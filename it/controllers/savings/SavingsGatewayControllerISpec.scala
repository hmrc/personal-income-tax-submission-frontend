/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.savings

import models.dividends.DividendsCheckYourAnswersModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, route}
import utils.{DividendsDatabaseHelper, IntegrationTest, ViewHelpers}

import scala.collection.immutable.Map

class SavingsGatewayControllerISpec extends IntegrationTest with ViewHelpers{

  val relativeUrl: String = s"/update-and-submit-income-tax-return/personal-income/2023/interest/interest-from-securities"
  val errorSummaryHref = "#value"

  object Selectors {
    val errorSummarySelector = "#main-content > div > div > div.govuk-error-summary"
    val yesSelector = "#main-content > div > div > form > div > fieldset > div > div:nth-child(1)"
    val noSelector = "#main-content > div > div > form > div > fieldset > div > div:nth-child(2)"
    val formSelector = "#main-content > div > div > form"
    val p1Selector = "#main-content > div > div > p"
    val bullet1 = "#main-content > div > div > ul > li:nth-child(1)"
    val bullet2 = "#main-content > div > div > ul > li:nth-child(2)"
    val bullet3 = "#main-content > div > div > ul > li:nth-child(3)"
    val bullet4 = "#main-content > div > div > ul > li:nth-child(4)"
    val insetSelector = "#main-content > div > div > div.govuk-inset-text"
    val radioHeadSelector = "#main-content > div > div > form > div > fieldset > legend"
  }

  trait CommonExpectedResults {
    val title: String
    val caption: String
    val p1: String
    val bullet1: String
    val bullet2: String
    val bullet3: String
    val bullet4: String
    val yesText: String
    val noText: String
    val continueText: String
  }

  object CommonExpectedResultsEN extends CommonExpectedResults {
    override val title: String = "Interest from gilt-edged or accrued income securities"
    override val caption: String = s"Interest from gilt-edged or accrued income securities for 6 April $taxYearEOY to 5 April $taxYear"
    override val p1: String = "This could be disguised interest or interest from:"
    override val bullet1: String = "gilt-edged or deeply discounted securities"
    override val bullet2: String = "accrued income profit"
    override val bullet3: String = "agreements (known as loan notes), between UK borrowers and lenders, to repay debt"
    override val bullet4: String = "UK peer-to-peer loans"
    override val yesText: String = "Yes"
    override val noText: String = "No"
    override val continueText: String = "Continue"
  }

  object CommonExpectedResultsCY extends CommonExpectedResults {
    override val title: String = "Llog o warantau gilt neu warantau incwm cronedig"
    override val caption: String = s"Llog o warantau gilt neu warantau incwm cronedig ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    override val p1: String = "Gallai hyn fod yn llog cuddiedig neu’n llog o:"
    override val bullet1: String = "gwarantau gilt neu warantau â chryn ddisgownt"
    override val bullet2: String = "elw incwm cronedig"
    override val bullet3: String = "cytundebau (a elwir yn nodiadau benthyciad), rhwng benthycwyr a derbynwyr benthyciadau yn y DU, i ad-dalu dyled"
    override val bullet4: String = "benthyciadau cymar-i-gymar yn y DU"
    override val yesText: String = "Iawn"
    override val noText: String = "Na"
    override val continueText: String = "Yn eich blaen"
  }

  trait SpecificUserTypeResults {
    val insetText: String
    val radioHead: String
    val errorText: String
  }

  object IndividualResultsEN extends SpecificUserTypeResults {
    override val errorText: String = "Select yes if you got any interest from gilt-edged or accrued income securities"
    override val insetText: String = "Do not include interest you got from an Individual Savings Account or Personal Equity Plan."
    override val radioHead: String = "Did you get any interest from gilt-edged or accrued income securities?"
  }

  object AgentResultsEN extends SpecificUserTypeResults {
    override val errorText: String = "Select yes if your client got any interest from gilt-edged or accrued income securities"
    override val insetText: String = "Do not include interest your client got from an Individual Savings Account or Personal Equity Plan."
    override val radioHead: String = "Did your client get any interest from gilt-edged or accrued income securities?"
  }

  object IndividualResultsCY extends SpecificUserTypeResults {
    override val errorText: String = "Dewiswch ‘Iawn’ os cawsoch unrhyw log o warantau gilt neu warantau incwm cronedig"
    override val insetText: String = "Peidiwch â chynnwys llog a gawsoch o Gyfrif Cynilo Unigol neu Gynllun Ecwiti Personol."
    override val radioHead: String = "A gawsoch unrhyw log o warantau gilt neu warantau incwm cronedig?"
  }

  object AgentResultsCY extends SpecificUserTypeResults {
    override val errorText: String = "Dewiswch ‘Iawn’ os cafodd eich cleient unrhyw log o warantau gilt neu warantau incwm cronedig"
    override val insetText: String = "Peidiwch â chynnwys llog a gafodd eich cleient o Gyfrif Cynilo Unigol neu Gynllun Ecwiti Personol."
    override val radioHead: String = "A gafodd eich cleient unrhyw log o warantau gilt neu warantau incwm cronedig?"
  }

  private val userScenarios = Seq(
    UserScenario(isWelsh = false, isAgent = false, commonExpectedResults = CommonExpectedResultsEN, Some(IndividualResultsEN)),
    UserScenario(isWelsh = false, isAgent = true, commonExpectedResults = CommonExpectedResultsEN, Some(AgentResultsEN)),
    UserScenario(isWelsh = true, isAgent = false, commonExpectedResults = CommonExpectedResultsCY, Some(IndividualResultsCY)),
    UserScenario(isWelsh = true, isAgent = true, commonExpectedResults = CommonExpectedResultsCY, Some(AgentResultsCY))
  )

  userScenarios.foreach { scenario =>
    lazy val uniqueResults = scenario.specificExpectedResults.get

    import scenario.commonExpectedResults._
    import uniqueResults._

    val testNameWelsh = if (scenario.isWelsh) "in Welsh" else "in English"
    val testNameAgent = if (scenario.isAgent) "an agent" else "an individual"


    s".show when $testNameWelsh and the user is $testNameAgent" should {

        "display the gateway page" which {

          lazy val headers = playSessionCookie(scenario.isAgent) ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
          lazy val request = FakeRequest("GET", relativeUrl).withHeaders(headers: _*)

          lazy val result = {
            authoriseAgentOrIndividual(scenario.isAgent)
            route(app, request, "{}").get
          }

          implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

          "has a status of OK(200)" in {
            status(result) shouldBe OK
          }

          titleCheck(title, scenario.isWelsh)
          h1Check(title  + " " + caption)
          captionCheck(caption)
          textOnPageCheck(p1, Selectors.p1Selector)
          textOnPageCheck(bullet1, Selectors.bullet1)
          textOnPageCheck(bullet2, Selectors.bullet2)
          textOnPageCheck(bullet3, Selectors.bullet3)
          textOnPageCheck(bullet4, Selectors.bullet4)
          textOnPageCheck(insetText, Selectors.insetSelector)
          textOnPageCheck(radioHead, Selectors.radioHeadSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText,2)
          formPostLinkCheck(relativeUrl, Selectors.formSelector)
          buttonCheck(continueText)
        }

    }

    s".submit when $testNameWelsh and the user is $testNameAgent" should {

        "return a 200 status" in {

          lazy val headers = playSessionCookie(scenario.isAgent) ++ Map(csrfContent) ++
            (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
          lazy val request = FakeRequest("POST", relativeUrl).withHeaders(headers: _*)

          lazy val result = {
            authoriseAgentOrIndividual(scenario.isAgent)
            route(app, request, Map("value" -> Seq("true"))).get
          }

            status(result) shouldBe OK

        }
        "return a error" which {

          lazy val headers = playSessionCookie(scenario.isAgent) ++ Map(csrfContent) ++
            (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
          lazy val request = FakeRequest("POST", relativeUrl).withHeaders(headers: _*)

          lazy val result = {
            authoriseAgentOrIndividual(scenario.isAgent)
            route(app, request, Map("value" -> Seq(""))).get
          }
          implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

          "has a 400 BAD_REQUEST status " in{
            status(result) shouldBe BAD_REQUEST
          }
          titleCheck(errorPrefix(scenario.isWelsh) + title, scenario.isWelsh)
          errorAboveElementCheck(errorText)
          errorSummaryCheck(errorText, errorSummaryHref, scenario.isWelsh)
        }

    }

  }

}
