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

package controllers.dividends

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

class DividendsGatewayControllerISpec extends IntegrationTest with ViewHelpers with DividendsDatabaseHelper {

  lazy val tailoringWsClient: WSClient = appWithTailoring.injector.instanceOf[WSClient]

  val relativeUrl: String = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/dividends-from-stocks-and-shares"
  val absoluteUrl: String = appUrl + s"/$taxYear/dividends/dividends-from-stocks-and-shares"

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
    val caption: String

    val yesRedirectUrl = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/dividends-from-uk-companies"
    val noRedirectUrl = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/check-income-from-dividends"
  }

  object CommonExpectedResultsEN extends CommonExpectedResults {
    override val hintText = "For example, dividends from UK companies, authorised unit trusts or open ended investment companies."
    override val continueText: String = "Continue"
    override val yesText: String = "Yes"
    override val noText: String = "No"
    override val caption: String = s"Dividends for 6 April ${(taxYear - 1).toString} to 5 April ${taxYear.toString}"
  }

  object CommonExpectedResultsCY extends CommonExpectedResults {
    override val hintText = "Er enghraifft, difidendau gan gwmnïau, ymddiriedolaethau unedol awdurdodedig neu gwmnïau buddsoddi penagored yn y DU."
    override val continueText: String = "Yn eich blaen"
    override val yesText: String = "Iawn"
    override val noText: String = "Na"
    override val caption: String = s"Difidendau ar gyfer 6 Ebrill ${(taxYear - 1).toString} i 5 Ebrill ${taxYear.toString}"
  }

  trait SpecificUserTypeResults {
    val heading: String
    val errorText: String
  }

  object IndividualResultsEN extends SpecificUserTypeResults {
    override val heading: String = "Did you get dividends from shares?"
    override val errorText: String = "Select yes if you got dividends from shares"
  }

  object AgentResultsEN extends SpecificUserTypeResults {
    override val heading: String = "Did your client get dividends from shares?"
    override val errorText: String = "Select yes if your client got dividends from shares"
  }

  object IndividualResultsCY extends SpecificUserTypeResults {
    override val heading: String = "A gawsoch ddifidendau o gyfranddaliadau?"
    override val errorText: String = "Dewiswch ‘Iawn’ os cawsoch ddifidendau o gyfranddaliadau"
  }

  object AgentResultsCY extends SpecificUserTypeResults {
    override val heading: String = "A gafodd eich cleient ddifidendau o gyfranddaliadau?"
    override val errorText: String = "Dewiswch ‘Iawn’ os cafodd eich cleient ddifidendau o gyfranddaliadau"
  }

  val userScenarios = Seq(
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


    s".show when $testNameWelsh and the user is $testNameAgent" when {

      "the tailoring is turned on" should {

        "display the gateway page" which {

          lazy val headers = playSessionCookie(scenario.isAgent) ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
          lazy val request = FakeRequest("GET", relativeUrl).withHeaders(headers: _*)

          lazy val result = {
            authoriseAgentOrIndividual(scenario.isAgent)
            dropDividendsDB()
            emptyUserDataStub()
            route(appWithTailoring, request, "{}").get
          }

          implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

          "has a status of OK(200)" in {
            status(result) shouldBe OK
          }

          titleCheck(heading, scenario.isWelsh)
          h1Check(s"$heading $caption")
          captionCheck(caption)
          hintTextCheck(hintText)
          formPostLinkCheck(relativeUrl, Selectors.formSelector)
          textOnPageCheck(yesText, Selectors.yesSelector)
          textOnPageCheck(noText, Selectors.noSelector)
          buttonCheck(continueText)
        }

      }

      "the tailoring is turn off" should {

        "redirect the user to the overview page" which {

          lazy val request = {
            authoriseAgentOrIndividual(scenario.isAgent)
            dropDividendsDB()
            emptyUserDataStub()
            urlGet(absoluteUrl, follow = false, headers = playSessionCookie(scenario.isAgent))
          }

          "has a status of SEE_OTHER(303)" in {
            request.status shouldBe SEE_OTHER
          }

          "have the correct redirect location" in {
            request.header("Location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
          }

        }

      }

    }

    s".submit when $testNameWelsh and the user is $testNameAgent" when {
      lazy val headers = playSessionCookie(scenario.isAgent) ++ Map(csrfContent) ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
      lazy val request = FakeRequest("POST", relativeUrl).withHeaders(headers: _*)

      "the feature switch is turned on" when {

        "the user submits a yes, when no previous data exists" should {

          "redirect the user to the ReceiveUkDividends page" which {

            lazy val result = {
              authoriseAgentOrIndividual(scenario.isAgent)
              dropDividendsDB()
              emptyUserDataStub()
              route(appWithTailoring, request, Json.obj("value" -> "true")).get
            }

            "has a status of SEE_OTHER(303)" in {
              status(result) shouldBe SEE_OTHER
            }

            "has the correct redirect location" in {
              await(result).header.headers("Location") shouldBe yesRedirectUrl
            }

          }

        }

        "the user submits a yes, when previous data exists" should {

          "redirect the user to the ReceiveUkDividends page" which {

            lazy val result = {
              authoriseAgentOrIndividual(scenario.isAgent)
              dropDividendsDB()
              insertCyaData(Some(DividendsCheckYourAnswersModel(
                gateway = Some(false)
              )))
              emptyUserDataStub()
              route(appWithTailoring, request, Json.obj("value" -> "true")).get
            }

            "has a status of SEE_OTHER(303)" in {
              status(result) shouldBe SEE_OTHER
            }

            "has the correct redirect location" in {
              await(result).header.headers("Location") shouldBe yesRedirectUrl
            }

          }

        }

        "the user submits a no" should {

          "redirect the user to the CYA page" which { //TODO this will need updating once the 0ing page and flow is implemented
            lazy val result = {
              authoriseAgentOrIndividual(scenario.isAgent)
              dropDividendsDB()
              emptyUserDataStub()
              route(appWithTailoring, request, Json.obj("value" -> "false")).get
            }

            "has a status of SEE_OTHER(303)" in {
              status(result) shouldBe SEE_OTHER
            }

            "has the correct redirect location" in {
              await(result).header.headers("Location") shouldBe noRedirectUrl
            }
          }

        }

        "the user submits incorrect data" should {

          "display an error on the form" which {

            lazy val result = {
              authoriseAgentOrIndividual(scenario.isAgent)
              dropDividendsDB()
              emptyUserDataStub()
              route(appWithTailoring, request, Json.obj("value" -> "oops").toString()).get
            }

            implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

            "has a status of BAD_REQUEST(400)" in {
              status(result) shouldBe BAD_REQUEST
            }

            errorSummaryCheck(errorText, "#value", scenario.isWelsh)
            titleCheck(heading, scenario.isWelsh)
            h1Check(s"$heading $caption")
            captionCheck(caption)
            hintTextCheck(hintText)
            formPostLinkCheck(relativeUrl, Selectors.formSelector)
            textOnPageCheck(yesText, Selectors.yesSelector)
            textOnPageCheck(noText, Selectors.noSelector)
            buttonCheck(continueText)

          }

        }

      }

      "the feature switch is turned off" should {

        "redirect to the overview page" which {
          lazy val request = {
            authoriseAgentOrIndividual(scenario.isAgent)
            dropDividendsDB()
            emptyUserDataStub()
            urlPost(absoluteUrl, follow = false, headers = playSessionCookie(scenario.isAgent), body = "{}")
          }

          "has a status of SEE_OTHER(303)" in {
            request.status shouldBe SEE_OTHER
          }

          "have the correct redirect location" in {
            request.header("Location") shouldBe Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
          }
        }

      }

    }
  }

}
