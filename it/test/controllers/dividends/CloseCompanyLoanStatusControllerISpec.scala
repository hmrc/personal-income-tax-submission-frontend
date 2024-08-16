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

package test.controllers.dividends

import controllers.dividends.routes
import models.dividends.StockDividendsCheckYourAnswersModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.DefaultBodyWritables
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, redirectLocation, route, writeableOf_AnyContentAsFormUrlEncoded}
import test.utils.{DividendsDatabaseHelper, IntegrationTest, ViewHelpers}

import scala.concurrent.Future

class CloseCompanyLoanStatusControllerISpec extends IntegrationTest with ViewHelpers with DefaultBodyWritables with DividendsDatabaseHelper {

  val amount: BigDecimal = 123.45
  val closeCompanyLoanStatusUrl: String = routes.CloseCompanyLoanStatusController.show(taxYear).url
  val closeCompanyLoansAmountUrl: String = routes.CloseCompanyLoanAmountController.show(taxYear).url
  val dividendsSummaryUrl: String = routes.DividendsSummaryController.show(taxYear).url
  val postURL: String = s"$appUrl/$taxYear/dividends/close-company-loan-status"

  val cyaModel: StockDividendsCheckYourAnswersModel =
    StockDividendsCheckYourAnswersModel(
      gateway = Some(true),
      ukDividends = Some(true),
      ukDividendsAmount = Some(amount),
      otherUkDividends = Some(true),
      otherUkDividendsAmount = Some(amount),
      stockDividends = Some(true),
      stockDividendsAmount = Some(amount),
      redeemableShares = Some(true),
      redeemableSharesAmount = Some(amount),
      closeCompanyLoansWrittenOff = Some(true),
      closeCompanyLoansWrittenOffAmount = Some(amount)
    )

  trait SpecificExpectedResults {
    val expectedHeading: String
    val expectedP2: String
    val expectedP3: String
    val expectedErrorText: String
  }

  trait CommonExpectedResults {
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedP1: String
    val captionExpected: String
    val yesNo: Boolean => String
    val continueText: String
  }

  object AllExpectedEnglish extends CommonExpectedResults {
    val expectedTitle = "Close company loans written off"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedP1 = "A 'close company' is a limited company with 5 or fewer shareholders or one managed by shareholders who are also directors."
    val captionExpected = s"Dividends for 6 April $taxYearEOY to 5 April $taxYear"
    val yesNo: Boolean => String = isYes => if (isYes) "Yes" else "No"
    val continueText = "Continue"
  }

  object AllExpectedWelsh extends CommonExpectedResults {
    val expectedTitle = "Benthyciadau gan gwmnïau caeedig a ddilëwyd"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedP1 = "‘Cwmni caeedig’ yw cwmni cyfyngedig sydd â 5 o gyfranddalwyr neu lai, neu sy’n cael ei reoli gan gyfranddalwyr sydd hefyd yn gyfarwyddwyr."
    val captionExpected = s"Difidendau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val yesNo: Boolean => String = isYes => if (isYes) "Iawn" else "Na"
    val continueText = "Yn eich blaen"
  }

  object IndividualExpectedEnglish extends SpecificExpectedResults {
    val expectedHeading = "Did you have a close company loan written off or released?"
    val expectedP2 = "Money you take from your company, but not as salary or dividends, is normally considered a loan."
    val expectedP3 = "If your company writes off the loan, the company will get a repayment of the tax previously paid."
    val expectedErrorText = "Select Yes if you had a close company loan written off or released"
  }

  object IndividualExpectedWelsh extends SpecificExpectedResults {
    val expectedHeading = "A gawsoch fenthyciad gan gwmni caeedig a gafodd ei ddileu neu ei ryddhau?"
    val expectedP2 = "Fel arfer, ystyrir arian rydych chi’n ei gymryd o’ch cwmni – ond nid fel cyflog neu ddifidendau – yn fenthyciad."
    val expectedP3 = "Os bydd eich cwmni yn dileu’r benthyciad, bydd y cwmni’n cael ad-daliad o’r dreth a dalwyd yn flaenorol."
    val expectedErrorText = "Dewiswch ‘Iawn’ os cafodd benthyciad a gawsoch gan gwmni caeedig ei ddileu neu ei ryddhau"
  }

  object AgentExpectedEnglish extends SpecificExpectedResults {
    val expectedHeading = "Did your client have a close company loan written off or released?"
    val expectedP2 = "Money your client takes from their company, but not as salary or dividends, is normally considered a loan."
    val expectedP3 = "If your client's company writes off the loan, the company will get a repayment of the tax previously paid."
    val expectedErrorText = "Select Yes if your client had a close company loan written off or released"
  }

  object AgentExpectedWelsh extends SpecificExpectedResults {
    val expectedHeading = "A gafodd eich cleient fenthyciad gan gwmni caeedig a gafodd ei ddileu neu ei ryddhau?"
    val expectedP2 = "Fel arfer, ystyrir arian mae’ch cleient yn ei gymryd o’i gwmni – ond nid fel cyflog neu ddifidendau – yn fenthyciad."
    val expectedP3 = "Os bydd cwmni eich cleient yn dileu’r benthyciad, bydd y cwmni’n cael ad-daliad o’r dreth a dalwyd yn flaenorol."
    val expectedErrorText = "Dewiswch ‘Iawn’ os oedd gan eich cleient fenthyciad gan gwmni caeedig a gafodd ei ddileu neu ei ryddhau"
  }

  object Selectors {
    val titleSelector = "#main-content > div > div > h1"
    val headingSelector = "#main-content > div > div > form > div > fieldset > legend"
    val captionSelector = ".govuk-caption-l"
    val p1Selector = "#p1"
    val p2Selector = "#p2"
    val p3Selector = "#p3"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val formSelector = "#main-content > div > div > form"
    val errorSummaryHref = "#value"
    val errorSelector = "#main-content > div > div > div.govuk-error-summary > div > h2"
  }

  protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, AllExpectedEnglish, Some(IndividualExpectedEnglish)),
    UserScenario(isWelsh = false, isAgent = true, AllExpectedEnglish, Some(AgentExpectedEnglish)),
    UserScenario(isWelsh = true, isAgent = false, AllExpectedWelsh, Some(IndividualExpectedWelsh)),
    UserScenario(isWelsh = true, isAgent = true, AllExpectedWelsh, Some(AgentExpectedWelsh))
  )

  userScenarios.foreach { scenario =>
    lazy val uniqueResults = scenario.specificExpectedResults.get
    import scenario.commonExpectedResults._
    import uniqueResults._

    val testNameWelsh = if (scenario.isWelsh) "in Welsh" else "in English"
    val testNameAgent = if (scenario.isAgent) "an agent" else "an individual"

    def getCloseCompanyLoanStatus(application: Application): Future[Result] = {
      val headers = Option.when(scenario.isWelsh)(HeaderNames.ACCEPT_LANGUAGE -> "cy").toSeq ++ playSessionCookie(scenario.isAgent)
      lazy val request = FakeRequest("GET", closeCompanyLoanStatusUrl).withHeaders(headers: _*)
      authoriseAgentOrIndividual(scenario.isAgent)
      route(application, request, "{}").get
    }

    def postCloseCompanyLoanStatus(body: Seq[(String, String)],
                                   application: Application): Future[Result] = {
      val headers = Seq("Csrf-Token" -> "nocheck") ++
        Option.when(scenario.isWelsh)(HeaderNames.ACCEPT_LANGUAGE -> "cy").toSeq ++
        playSessionCookie(scenario.isAgent)
      val request = FakeRequest("POST", closeCompanyLoanStatusUrl).withHeaders(headers: _*).withFormUrlEncodedBody(body: _*)

      authoriseAgentOrIndividual(scenario.isAgent)
      route(application, request).get
    }

    s".show when $testNameWelsh and the user is $testNameAgent" should {

      "display the close company loan status page with appWithStockDividendsBackendMongo" which {
        implicit lazy val application: Application = appWithStockDividendsBackendMongo

        lazy val result = {
          getSessionDataStub()
          getCloseCompanyLoanStatus(application)
        }

        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of OK(200)" in {
          status(result) shouldBe OK
        }

        h1Check(expectedTitle + " " + captionExpected)
        captionCheck(captionExpected)
        formPostLinkCheck(closeCompanyLoanStatusUrl, Selectors.formSelector)
        textOnPageCheck(expectedP1, Selectors.p1Selector)
        textOnPageCheck(expectedP2, Selectors.p2Selector)
        textOnPageCheck(expectedP3, Selectors.p3Selector)
        buttonCheck(continueText, Selectors.continueButtonSelector)
        radioButtonCheck(yesNo(true), 1)
        radioButtonCheck(yesNo(false), 2)
        welshToggleCheck(scenario.isWelsh)
      }

      "display the close company loan status page with appWithStockDividends" which {
        implicit lazy val application: Application = appWithStockDividends

        lazy val result = {
          dropStockDividendsDB()
          emptyStockDividendsUserDataStub()
          getCloseCompanyLoanStatus(application)
        }

        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of OK(200)" in {
          status(result) shouldBe OK
        }

        h1Check(expectedTitle + " " + captionExpected)
        captionCheck(captionExpected)
        formPostLinkCheck(closeCompanyLoanStatusUrl, Selectors.formSelector)
        textOnPageCheck(expectedP1, Selectors.p1Selector)
        textOnPageCheck(expectedP2, Selectors.p2Selector)
        textOnPageCheck(expectedP3, Selectors.p3Selector)
        buttonCheck(continueText, Selectors.continueButtonSelector)
        radioButtonCheck(yesNo(true), 1)
        radioButtonCheck(yesNo(false), 2)
        welshToggleCheck(scenario.isWelsh)
      }

      "display the redeemable shares status page with session data with appWithStockDividendsBackendMongo" which {
        implicit lazy val application: Application = appWithStockDividendsBackendMongo

        lazy val result: Future[Result] = {
          getSessionDataStub()
          getCloseCompanyLoanStatus(application)
        }

        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of OK(200)" in {
          status(result) shouldBe OK
        }

        h1Check(expectedTitle + " " + captionExpected)
        captionCheck(captionExpected)
        formPostLinkCheck(closeCompanyLoanStatusUrl, Selectors.formSelector)
        textOnPageCheck(expectedP1, Selectors.p1Selector)
        textOnPageCheck(expectedP2, Selectors.p2Selector)
        textOnPageCheck(expectedP3, Selectors.p3Selector)
        buttonCheck(continueText, Selectors.continueButtonSelector)
        radioButtonCheck(yesNo(true), 1)
        radioButtonCheck(yesNo(false), 2)
        welshToggleCheck(scenario.isWelsh)
      }

      "display the redeemable shares status page with session data with appWithStockDividends" which {
        implicit lazy val application: Application = appWithStockDividends

        lazy val result: Future[Result] = {
          dropStockDividendsDB()
          insertStockDividendsCyaData(Some(cyaModel))
          getCloseCompanyLoanStatus(application)
        }

        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of OK(200)" in {
          status(result) shouldBe OK
        }

        h1Check(expectedTitle + " " + captionExpected)
        captionCheck(captionExpected)
        formPostLinkCheck(closeCompanyLoanStatusUrl, Selectors.formSelector)
        textOnPageCheck(expectedP1, Selectors.p1Selector)
        textOnPageCheck(expectedP2, Selectors.p2Selector)
        textOnPageCheck(expectedP3, Selectors.p3Selector)
        buttonCheck(continueText, Selectors.continueButtonSelector)
        radioButtonCheck(yesNo(true), 1)
        radioButtonCheck(yesNo(false), 2)
        welshToggleCheck(scenario.isWelsh)
      }
    }

    s".submit when $testNameWelsh and the user is $testNameAgent" should {

      "return a 303 status and redirect to amount page when true selected with appWithStockDividendsBackendMongo" in {
        implicit lazy val application: Application = appWithStockDividendsBackendMongo

        lazy val result = {
          getSessionDataStub(Some(stockDividendsUserDataModel.copy(stockDividends = Some(StockDividendsCheckYourAnswersModel()))))
          updateSessionDataStub()
          postCloseCompanyLoanStatus(Seq("value" -> "true"), application)
        }
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe closeCompanyLoansAmountUrl
      }

      "return a 303 status and redirect to amount page when true selected with appWithStockDividends" in {
        implicit lazy val application: Application = appWithStockDividends

        lazy val result = {
          dropStockDividendsDB()
          postCloseCompanyLoanStatus(Seq("value" -> "true"), application)
        }
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe closeCompanyLoansAmountUrl
      }

      "return a 303 status and redirect to cya page when false selected with appWithStockDividendsBackendMongo" in {
        implicit lazy val application: Application = appWithStockDividendsBackendMongo

        lazy val result = {
          getSessionDataStub(Some(stockDividendsUserDataModel.copy(stockDividends = Some(StockDividendsCheckYourAnswersModel()))))
          updateSessionDataStub()
          postCloseCompanyLoanStatus(Seq("value" -> "false"), application)
        }
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe dividendsSummaryUrl
      }

      "return a 303 status and redirect to cya page when false selected with appWithStockDividends" in {
        implicit lazy val application: Application = appWithStockDividends

        lazy val result = {
          dropStockDividendsDB()
          postCloseCompanyLoanStatus(Seq("value" -> "false"), application)
        }
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe dividendsSummaryUrl
      }

      "return a 303 status and redirect to cya page when isFinished is true with appWithStockDividendsBackendMongo" in {
        implicit lazy val application: Application = appWithStockDividendsBackendMongo

        lazy val result = {
          getSessionDataStub()
          updateSessionDataStub()
          postCloseCompanyLoanStatus(Seq("value" -> "true"), application)
        }
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe dividendsSummaryUrl
      }

      "return a 303 status and redirect to cya page when isFinished is true with appWithStockDividends" in {
        implicit lazy val application: Application = appWithStockDividends

        lazy val result = {
          dropStockDividendsDB()
          emptyStockDividendsUserDataStub()
          insertStockDividendsCyaData(Some(cyaModel))
          postCloseCompanyLoanStatus(Seq("value" -> "true"), application)
        }
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe dividendsSummaryUrl
      }

      "return a error" when {
        "the form is empty with appWithStockDividends" which {

          implicit lazy val application: Application = appWithStockDividends

          lazy val result = postCloseCompanyLoanStatus(Seq("value" -> ""), application)

          implicit val document: () => Document = () => Jsoup.parse(bodyOf(result))

          "has a 400 BAD_REQUEST status " in {
            status(result) shouldBe BAD_REQUEST
          }

          titleCheck(errorPrefix(scenario.isWelsh) + expectedTitle, scenario.isWelsh)
          errorAboveElementCheck(expectedErrorText)
          errorSummaryCheck(expectedErrorText, Selectors.errorSummaryHref, scenario.isWelsh)
        }

        "the form is empty with appWithStockDividends with appWithStockDividendsBackendMongo" which {

          implicit lazy val application: Application = appWithStockDividendsBackendMongo

          lazy val result = postCloseCompanyLoanStatus(Seq("value" -> ""), application)

          implicit val document: () => Document = () => Jsoup.parse(bodyOf(result))

          "has a 400 BAD_REQUEST status " in {
            status(result) shouldBe BAD_REQUEST
          }

          titleCheck(errorPrefix(scenario.isWelsh) + expectedTitle, scenario.isWelsh)
          errorAboveElementCheck(expectedErrorText)
          errorSummaryCheck(expectedErrorText, Selectors.errorSummaryHref, scenario.isWelsh)
        }

      }

    }

  }
}