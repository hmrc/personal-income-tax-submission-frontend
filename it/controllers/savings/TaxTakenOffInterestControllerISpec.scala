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

package controllers.savings

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.DefaultBodyWritables
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, route}
import utils.{DividendsDatabaseHelper, IntegrationTest, ViewHelpers}

class TaxTakenOffInterestControllerISpec extends IntegrationTest with ViewHelpers with DefaultBodyWritables {

  val amount: BigDecimal = 500
  val taxTakenOffInterestAmountUrl: String = s"/update-and-submit-income-tax-return/personal-income/2023/interest/how-much-tax-taken-off-interest"
  val postURL: String = s"$appUrl/2023/interest/how-much-tax-taken-off-interest"
  val errorSummaryHref = "#amount"
  val poundPrefixText = "£"

  object Selectors {

    val poundPrefixSelector = ".govuk-input__prefix"
    val captionSelector = ".govuk-caption-l"
    val headingSelector = ".govuk-heading-l"
    val inputSelector = ".govuk-input"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val amountSelector = "#amount"
    val hintTextSelector = "#amount-hint"
  }

  trait CommonExpectedResults {
    val captionExpected: String
    val continueText: String
    val expectedHintText: String
    val expectedErrorEmpty: String
    val expectedErrorOverMax: String
    val expectedErrorInvalid: String
    val expectedErrorDecimal: String
    val expectedErrorTitle: String

  }

  object CommonExpectedResultsEN extends CommonExpectedResults {
    val continueText = "Continue"
    val expectedHintText = "For example, £193.52"
    val captionExpected = s"Interest from gilt-edged or accrued income securities for 6 April $taxYearEOY to 5 April $taxYear"
    val expectedErrorTitle = s"Error: $expectedErrorTitle"
    val expectedErrorEmpty = "Enter the amount of tax taken off the interest"
    val expectedErrorInvalid = "Amount of tax must be a number, like 600 or 193.54"
    val expectedErrorOverMax = "Amount of tax must be less than £100,000,000,000"
    val expectedErrorDecimal = "Amount of tax can only include pounds and pence, like 600 or 193.54"

  }

  object CommonExpectedResultsCY extends CommonExpectedResults {
    val continueText = "Yn eich blaen"
    val expectedHintText = "Er enghraifft, £193.52"
    val captionExpected = s"Llog o warantau gilt neu warantau incwm cronedig ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val expectedErrorTitle = s"Error: $expectedErrorTitle"
    val expectedErrorEmpty = "Nodwch swm y dreth a ddidynnwyd o’r llog"
    val expectedErrorInvalid = "Rhaid i swm y dreth fod yn rhif, megis 600 neu 193.54"
    val expectedErrorOverMax = "Rhaid i swm y dreth fod yn llai na £100,000,000,000"
    val expectedErrorDecimal = "Dim ond punnoedd a cheiniogau y gellir eu cynnwys yn swm y dreth, megis 600 neu 193.45"
  }

  trait SpecificUserTypeResults {
    val expectedH1: String
    val expectedTitle: String
  }

  object IndividualResultsEN extends SpecificUserTypeResults {
    val expectedH1 = "How much tax was taken off your interest?"
    val expectedTitle = "How much tax was taken off your interest?"
  }

  object AgentResultsEN extends SpecificUserTypeResults {
    val expectedH1 = "How much tax was taken off your client’s interest?"
    val expectedTitle = "How much tax was taken off your client’s interest?"
  }


  object IndividualResultsCY extends SpecificUserTypeResults {
    val expectedH1 = "Faint o dreth a ddidynnwyd o’ch llog?"
    val expectedTitle = "Faint o dreth a ddidynnwyd o’ch llog?"
  }

  object AgentResultsCY extends SpecificUserTypeResults {
    val expectedH1 = "Faint o dreth a ddidynnwyd o log eich cleient?"
    val expectedTitle = "Faint o dreth a ddidynnwyd o log eich cleient?"
  }

  val newAmountInput = "#amount"
  val amountInputName = "amount"
  val expectedErrorLink = "#amount"

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

      "display the tax amount page" which {

        lazy val headers = playSessionCookie(scenario.isAgent) ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
        lazy val request = FakeRequest("GET", taxTakenOffInterestAmountUrl).withHeaders(headers: _*)

        lazy val result = {
          authoriseAgentOrIndividual(scenario.isAgent)
          route(app, request, "{}").get
        }

        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of OK(200)" in {
          status(result) shouldBe OK
        }

        titleCheck(expectedTitle, scenario.isWelsh)
        h1Check(expectedH1 + " " + captionExpected)
        captionCheck(captionExpected)
        formPostLinkCheck(taxTakenOffInterestAmountUrl, Selectors.continueButtonFormSelector)
        buttonCheck(continueText, Selectors.continueButtonSelector)
        inputFieldCheck(amountInputName, Selectors.inputSelector)
        hintTextCheck(expectedHintText, Selectors.hintTextSelector)
      }
    }

    s".submit when $testNameWelsh and the user is $testNameAgent" should {

      "return a 200 status" in {

        lazy val headers = playSessionCookie(scenario.isAgent) ++ Map(csrfContent) ++
          (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())

        lazy val result = {
          authoriseAgentOrIndividual(scenario.isAgent)
          urlPost(postURL, follow = false, headers = playSessionCookie(scenario.isAgent), body = Map("amount" -> "123"))

        }

        result.status shouldBe OK

      }
      "return a error" when {

        "the form is empty" which {

          lazy val headers = playSessionCookie(scenario.isAgent) ++ Map(csrfContent) ++
            (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())

          lazy val result = {
            authoriseAgentOrIndividual(scenario.isAgent)
            urlPost(postURL,welsh = scenario.isWelsh, follow = false, headers = playSessionCookie(scenario.isAgent), body = Map("amount" -> ""))
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)

          "has a 400 BAD_REQUEST status " in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(errorPrefix(scenario.isWelsh) + expectedTitle, scenario.isWelsh)
          errorAboveElementCheck(expectedErrorEmpty)
          errorSummaryCheck(expectedErrorEmpty, errorSummaryHref, scenario.isWelsh)


        }


        "the form is invalid" which {

          lazy val headers = playSessionCookie(scenario.isAgent) ++ Map(csrfContent) ++
            (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())

          lazy val result = {
            authoriseAgentOrIndividual(scenario.isAgent)
            urlPost(postURL, welsh = scenario.isWelsh, follow = false, headers = playSessionCookie(scenario.isAgent), body = Map("amount" -> "$$$"))
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)

          "has a 400 BAD_REQUEST status " in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(errorPrefix(scenario.isWelsh) + expectedTitle, scenario.isWelsh)
          errorAboveElementCheck(expectedErrorInvalid)
          errorSummaryCheck(expectedErrorInvalid, errorSummaryHref, scenario.isWelsh)


        }


        "the form is overmax" which {

          lazy val headers = playSessionCookie(scenario.isAgent) ++ Map(csrfContent) ++
            (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())

          lazy val result = {
            authoriseAgentOrIndividual(scenario.isAgent)
            urlPost(postURL, welsh = scenario.isWelsh, follow = false, headers = playSessionCookie(scenario.isAgent), body = Map("amount" -> "103242424234242342423423"))
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)

          "has a 400 BAD_REQUEST status " in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(errorPrefix(scenario.isWelsh) + expectedTitle, scenario.isWelsh)
          errorAboveElementCheck(expectedErrorOverMax)
          errorSummaryCheck(expectedErrorOverMax, errorSummaryHref, scenario.isWelsh)

        }


        "the form has too many decimals" which {

          lazy val headers = playSessionCookie(scenario.isAgent) ++ Map(csrfContent) ++
            (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())

          lazy val result = {
            authoriseAgentOrIndividual(scenario.isAgent)
            urlPost(postURL, welsh = scenario.isWelsh, follow = false, headers = playSessionCookie(scenario.isAgent), body = Map("amount" -> "10.232"))
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)

          "has a 400 BAD_REQUEST status " in {
            result.status shouldBe BAD_REQUEST
          }

          titleCheck(errorPrefix(scenario.isWelsh) + expectedTitle, scenario.isWelsh)
          errorAboveElementCheck(expectedErrorDecimal)
          errorSummaryCheck(expectedErrorDecimal, errorSummaryHref, scenario.isWelsh)

        }
      }
    }
  }
}
