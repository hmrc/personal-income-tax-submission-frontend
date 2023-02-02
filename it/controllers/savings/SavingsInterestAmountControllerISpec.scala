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


import models.savings.SavingsIncomeCYAModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.DefaultBodyWritables
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, route}
import utils.{IntegrationTest, SavingsDatabaseHelper, ViewHelpers}

import play.api.libs.ws.DefaultBodyWritables

class SavingsInterestAmountControllerISpec extends IntegrationTest with ViewHelpers with DefaultBodyWritables with SavingsDatabaseHelper {

  val savingsInterestAmountUrl: String = s"/update-and-submit-income-tax-return/personal-income/2023/interest/interest-amount"
  val postURL: String = s"$appUrl/2023/interest/interest-amount  "
  val errorSummaryHref = "#amount"
  val poundPrefixText = "£"

  val cyaDataComplete: Option[SavingsIncomeCYAModel] = Some(SavingsIncomeCYAModel(Some(true), Some(100.00), Some(true), Some(50.00)))
  val cyaDataValid: Option[SavingsIncomeCYAModel] = Some(SavingsIncomeCYAModel(Some(true)))

  object Selectors {
    val poundPrefixSelector = ".govuk-input__prefix"
    val captionSelector = ".govuk-caption-l"
    val headingSelector = ".govuk-heading-l"
    val inputSelector = ".govuk-input"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val amountSelector = "#amount"
    val hintTextSelector = "#amount-hint"
    val p1Selector = "#main-content > div > div > p:nth-child(2)"
    val bulletHead = "#main-content > div > div > p:nth-child(3)"
    val bulletOne = "#bullet1"
    val bulletTwo = "#bullet2"
    val subHeading = "#main-content > div > div > form > div > label"
  }

  trait CommonExpectedResults {
    val captionExpected: String
    val continueText: String
    val expectedHintText: String
    val expectedH1: String
    val expectedErrorTitle: String


  }

  object CommonExpectedResultsEN extends CommonExpectedResults {
    val continueText = "Continue"
    val expectedHintText = "For example, £1935.46"
    val captionExpected = s"Interest from gilt-edged or accrued income securities for 6 April $taxYearEOY to 5 April $taxYear"
    val expectedH1 = "Total interest"
    val expectedErrorTitle = s"Error: $expectedErrorTitle"
  }

  object CommonExpectedResultsCY extends CommonExpectedResults {
    val continueText = "Yn eich blaen"
    val expectedHintText = "Er enghraifft, £1935.46"
    val captionExpected = s"Llog o warantau gilt neu warantau incwm cronedig ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val expectedH1 = "Cyfanswm y llog"
    val expectedErrorTitle = s"Error: $expectedErrorTitle"
  }

  trait SpecificUserTypeResults {
    val p1: String
    val bulletHead: String
    val bullet1: String
    val bullet2: String
    val subHeading: String
    val expectedErrorEmpty: String
    val expectedErrorOverMax: String
    val expectedErrorInvalid: String
  }

  object IndividualResultsEN extends SpecificUserTypeResults {
    val p1 = "Enter the amount you have received from gilt-edged or accrued income securities. This is the amount before any tax deductions. If you have joint income, only tell us your share."
    val bulletHead = "If you:"
    val bullet1 = "want to claim bad debt relief on a peer-to-peer loan, deduct the total bad debt from the interest you receive"
    val bullet2 = "invest in deeply discounted securities, enter the difference between what you paid for the bond and what you redeem or sell it for"
    val subHeading = "How much interest did you get?"
    val expectedErrorEmpty = "Enter the interest you got. For example, £1935.46"
    val expectedErrorOverMax = "The amount of your interest must be less than £100,000,000,000"
    val expectedErrorInvalid = "Enter the interest you got in the correct format. For example, £1935.46"
  }

  object AgentResultsEN extends SpecificUserTypeResults {
    val p1 = "Enter the amount your client has received from gilt-edged or accrued income securities. This is the amount before any tax deductions. If your client has joint income, only tell us their share."
    val bulletHead = "If your client:"
    val bullet1 = "wants to claim bad debt relief on a peer-to-peer loan, deduct the total bad debt from the interest your client received"
    val bullet2 = "invests in deeply discounted securities, enter the difference between what your client paid for the bond and what they redeemed or sold it for"
    val subHeading = "How much interest did your client get?"
    val expectedErrorEmpty = "Enter the interest your client got. For example, £1935.46"
    val expectedErrorOverMax = "The amount of your client’s interest must be less than £100,000,000,000"
    val expectedErrorInvalid = "Enter the interest your client got in the correct format. For example, £1935.46"
  }


  object IndividualResultsCY extends SpecificUserTypeResults {
    val p1 = "Nodwch y swm rydych wedi’i gael o warantau gilt neu warantau incwm cronedig. Dyma’r swm cyn unrhyw ddidyniadau treth. Os oes gennych incwm ar y cyd, nodwch eich cyfran chi yn unig."
    val bulletHead = "Os ydych:"
    val bullet1 = "am wneud cais am ryddhad rhag drwgddyledion ar fenthyciad cymar-i-gymar, didynnwch gyfanswm y drwgddyled o’r llog a gewch"
    val bullet2 = "yn buddsoddi mewn gwarantau â chryn ddisgownt, nodwch y gwahaniaeth rhwng yr hyn a daloch am y bond a’r hyn y gwnaethoch ei adenill neu ei werthu amdano"
    val subHeading = "Faint o log a gawsoch?"
    val expectedErrorEmpty = "Nodwch y llog a gawsoch. Er enghraifft, £1935.46"
    val expectedErrorOverMax = "Mae’n rhaid i swm eich llog fod yn llai na £100,000,000,000"
    val expectedErrorInvalid = "Nodwch y llog a gawsoch yn y fformat cywir. Er enghraifft, £1935.46"
  }

  object AgentResultsCY extends SpecificUserTypeResults {
    val p1 = "Nodwch y swm y mae’ch cleient wedi’i gael o warantau gilt neu warantau incwm cronedig. Dyma’r swm cyn unrhyw ddidyniadau treth. Os oes gan eich cleient incwm ar y cyd, nodwch ei gyfran ef yn unig."
    val bulletHead = "Os yw’ch cleient:"
    val bullet1 = "am wneud cais am ryddhad rhag drwgddyledion ar fenthyciad cymar-i-gymar, didynnwch gyfanswm y drwgddyled o’r llog a gafodd eich cleient"
    val bullet2 = "yn buddsoddi mewn gwarantau â chryn ddisgownt, nodwch y gwahaniaeth rhwng yr hyn a dalodd eich cleient am y bond a’r hyn y gwnaeth eich cleient ei adennill neu ei werthu amdano"
    val subHeading = "Faint o log a gafodd eich cleient?"
    val expectedErrorEmpty = "Nodwch y llog a gafodd eich cleient. Er enghraifft, £1935.46"
    val expectedErrorOverMax = "Mae’n rhaid i swm llog eich cleient fod yn llai na £100,000,000,000"
    val expectedErrorInvalid = "Nodwch y llog a gafodd eich cleient yn y fformat cywir. Er enghraifft, £1935.46"
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

      "display the interest amount page" which {

        lazy val headers = playSessionCookie(scenario.isAgent) ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
        lazy val request = FakeRequest("GET", savingsInterestAmountUrl).withHeaders(headers: _*)

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

        h1Check(expectedH1 + " " + captionExpected)
        captionCheck(captionExpected)
        textOnPageCheck(p1, Selectors.p1Selector)
        textOnPageCheck(bulletHead, Selectors.bulletHead)
        textOnPageCheck(bullet1, Selectors.bulletOne)
        textOnPageCheck(bullet2, Selectors.bulletTwo)
        textOnPageCheck(subHeading, Selectors.subHeading)

        formPostLinkCheck(savingsInterestAmountUrl, Selectors.continueButtonFormSelector)
        buttonCheck(continueText, Selectors.continueButtonSelector)
        inputFieldCheck(amountInputName, Selectors.inputSelector)
        hintTextCheck(expectedHintText, Selectors.hintTextSelector)
      }
      "display the interest amount page prefilled" which {

        lazy val headers = playSessionCookie(scenario.isAgent) ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
        lazy val request = FakeRequest("GET", savingsInterestAmountUrl).withHeaders(headers: _*)

        lazy val result = {
          authoriseAgentOrIndividual(scenario.isAgent)
          dropSavingsDB()
          emptyUserDataStub()
          insertSavingsCyaData(cyaDataComplete)
          route(app, request, "{}").get
        }

        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of OK(200)" in {
          status(result) shouldBe OK
        }

        h1Check(expectedH1 + " " + captionExpected)
        captionCheck(captionExpected)
        textOnPageCheck(p1, Selectors.p1Selector)
        formPostLinkCheck(savingsInterestAmountUrl, Selectors.continueButtonFormSelector)
        buttonCheck(continueText, Selectors.continueButtonSelector)
        inputFieldCheck(amountInputName, Selectors.inputSelector)
        hintTextCheck(expectedHintText, Selectors.hintTextSelector)
        inputFieldValueCheck("100", Selectors.amountSelector)
      }
      "display the interest amount page when savingsIncome is empty" which {

        lazy val headers = playSessionCookie(scenario.isAgent) ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
        lazy val request = FakeRequest("GET", savingsInterestAmountUrl).withHeaders(headers: _*)

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

        h1Check(expectedH1 + " " + captionExpected)
        captionCheck(captionExpected)
        textOnPageCheck(p1, Selectors.p1Selector)
        formPostLinkCheck(savingsInterestAmountUrl, Selectors.continueButtonFormSelector)
        buttonCheck(continueText, Selectors.continueButtonSelector)
        inputFieldCheck(amountInputName, Selectors.inputSelector)
        hintTextCheck(expectedHintText, Selectors.hintTextSelector)
      }
      "redirect to overview page when there is no cyaData" which {

        lazy val headers = playSessionCookie(scenario.isAgent) ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
        lazy val request = FakeRequest("GET", savingsInterestAmountUrl).withHeaders(headers: _*)

        lazy val result = {
          authoriseAgentOrIndividual(scenario.isAgent)
          dropSavingsDB()
          emptyUserDataStub()
          route(app, request, "{}").get
        }

        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of REDIRECT(303)" in {
          status(result) shouldBe SEE_OTHER
        }

      }
    }

    s".submit when $testNameWelsh and the user is $testNameAgent" should {

      "return a 303 status with correct redirect when successful without previous amount" in {

        lazy val headers = playSessionCookie(scenario.isAgent) ++ Map(csrfContent) ++
          (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())

        lazy val result = {
          authoriseAgentOrIndividual(scenario.isAgent)
          dropSavingsDB()
          emptyUserDataStub()
          insertSavingsCyaData(cyaDataValid)
          urlPost(postURL, follow = false, headers = playSessionCookie(scenario.isAgent), body = Map("amount" -> "123"))

        }

        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.savings.routes.TaxTakenFromInterestController.show(taxYear)}"

      }
      "return a 303 status with correct redirect when successful with previous amount" in {

        lazy val headers = playSessionCookie(scenario.isAgent) ++ Map(csrfContent) ++
          (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())

        lazy val result = {
          authoriseAgentOrIndividual(scenario.isAgent)
          dropSavingsDB()
          emptyUserDataStub()
          insertSavingsCyaData(cyaDataComplete)
          urlPost(postURL, follow = false, headers = playSessionCookie(scenario.isAgent), body = Map("amount" -> "123"))

        }

        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.savings.routes.InterestSecuritiesCYAController.show(taxYear)}"

      }
      "return a to CYA page when there is no cyaData" in {

        lazy val result = {
          authoriseAgentOrIndividual(scenario.isAgent)
          dropSavingsDB()
          urlPost(postURL, follow = false, headers = playSessionCookie(scenario.isAgent), body = Map("amount" -> "123"))

        }

        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.savings.routes.InterestSecuritiesCYAController.show(taxYear)}"

      }
      "return a error" when {

        "the form is empty" which {

          lazy val headers = playSessionCookie(scenario.isAgent) ++ Map(csrfContent) ++
            (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())

          lazy val result = {
            authoriseAgentOrIndividual(scenario.isAgent)
            dropSavingsDB()
            emptyUserDataStub()
            insertSavingsCyaData(cyaDataValid)
            urlPost(postURL, welsh = scenario.isWelsh, follow = false, headers = playSessionCookie(scenario.isAgent), body = Map("amount" -> ""))
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)

          "has a 400 BAD_REQUEST status " in {
            result.status shouldBe BAD_REQUEST
          }

          errorAboveElementCheck(expectedErrorEmpty)
          errorSummaryCheck(expectedErrorEmpty, errorSummaryHref, scenario.isWelsh)


        }


        "the form is invalid" which {

          lazy val result = {
            authoriseAgentOrIndividual(scenario.isAgent)
            dropSavingsDB()
            emptyUserDataStub()
            insertSavingsCyaData(cyaDataValid)
            urlPost(postURL, welsh = scenario.isWelsh, follow = false, headers = playSessionCookie(scenario.isAgent), body = Map("amount" -> "$$$"))
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)

          "has a 400 BAD_REQUEST status " in {
            result.status shouldBe BAD_REQUEST
          }

          errorAboveElementCheck(expectedErrorInvalid)
          errorSummaryCheck(expectedErrorInvalid, errorSummaryHref, scenario.isWelsh)


        }


        "the form is overmax" which {

          lazy val result = {
            authoriseAgentOrIndividual(scenario.isAgent)
            dropSavingsDB()
            emptyUserDataStub()
            insertSavingsCyaData(cyaDataValid)
            urlPost(postURL, welsh = scenario.isWelsh, follow = false, headers = playSessionCookie(scenario.isAgent), body = Map("amount" -> "103242424234242342423423"))
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)

          "has a 400 BAD_REQUEST status " in {
            result.status shouldBe BAD_REQUEST
          }

          errorAboveElementCheck(expectedErrorOverMax)
          errorSummaryCheck(expectedErrorOverMax, errorSummaryHref, scenario.isWelsh)

        }


      }
    }
  }
}
