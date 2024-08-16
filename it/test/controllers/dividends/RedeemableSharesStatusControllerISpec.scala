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

class RedeemableSharesStatusControllerISpec extends IntegrationTest with ViewHelpers with DefaultBodyWritables with DividendsDatabaseHelper {

  val amount: BigDecimal = 123.45
  val redeemableSharesStatusUrl: String = routes.RedeemableSharesStatusController.show(taxYear).url
  val redeemableSharesAmountUrl: String = routes.RedeemableSharesAmountController.show(taxYear).url
  val relativepostURL: String = routes.RedeemableSharesStatusController.submit(taxYear).url
  val closeCompanyLoansStatusUrl: String = routes.CloseCompanyLoanStatusController.show(taxYear).url

  val dividendsSummaryUrl: String = routes.DividendsSummaryController.show(taxYear).url
  val postURL: String = s"$appUrl/$taxYear/dividends/redeemable-shares-status"

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
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedP1: String
    val expectedErrorText: String
  }

  trait CommonExpectedResults {
    val expectedP2: String
    val expectedP3: String
    val captionExpected: String
    val yesNo: Boolean => String
    val continueText: String
  }

  object AllExpectedEnglish extends CommonExpectedResults {
    val expectedP2 = "Redeemable shares are ones a company can buy back at an agreed price on a future date."
    val expectedP3 = "Free additional shares are also known as 'bonus issues of securities'."
    val captionExpected = s"Dividends for 6 April $taxYearEOY to 5 April $taxYear"
    val yesNo: Boolean => String = isYes => if (isYes) "Yes" else "No"
    val continueText = "Continue"
  }

  object AllExpectedWelsh extends CommonExpectedResults {
    val expectedP2 = "Cyfranddaliadau y mae cwmnïau’n gallu prynu’n ôl yn y dyfodol am bris y cytunwyd arno yw ‘cyfranddaliadau adbryn’."
    val expectedP3 = "Enw arall ar gyfranddaliadau ychwanegol a gawsoch yn rhad ac am ddim yw ‘dyroddiadau bonws o warantau’."
    val captionExpected = s"Difidendau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val yesNo: Boolean => String = isYes => if (isYes) "Iawn" else "Na"
    val continueText = "Yn eich blaen"
  }

  object IndividualExpectedEnglish extends SpecificExpectedResults {
    val expectedTitle = "Did you get free or redeemable shares?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedP1 = "You can hold both free and redeemable shares."
    val expectedErrorText = "Select Yes if you got free or redeemable shares"
  }

  object IndividualExpectedWelsh extends SpecificExpectedResults {
    val expectedTitle = "A gawsoch chi gyfranddaliadau adbryn neu gyfranddaliadau yn rhad ac am ddim?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedP1 = "Gallwch ddal cyfranddaliadau adbryn a chyfranddaliadau a gawsoch yn rhad ac am ddim."
    val expectedErrorText = "Dewiswch ‘Iawn’ os cawsoch gyfranddaliadau adbryn neu gyfranddaliadau yn rhad ac am ddim"
  }

  object AgentExpectedEnglish extends SpecificExpectedResults {
    val expectedTitle = "Did your client get free or redeemable shares?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedP1 = "Your client can hold both free and redeemable shares."
    val expectedErrorText = "Select Yes if your client got free or redeemable shares"
  }

  object AgentExpectedWelsh extends SpecificExpectedResults {
    val expectedTitle = "A gafodd eich cleient gyfranddaliadau adbryn neu gyfranddaliadau yn rhad ac am ddim?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedP1 = "Gall eich cleient ddal cyfranddaliadau adbryn a chyfranddaliadau a gafodd yn rhad ac am ddim."
    val expectedErrorText = "Dewiswch ‘Iawn’ os cafodd eich cleient gyfranddaliadau adbryn neu gyfranddaliadau yn rhad ac am ddim"
  }

  object Selectors {
    val titleSelector = "#main-content > div > div > form > div > fieldset > legend"
    val captionSelector = ".govuk-caption-l"
    val p1Selector = "#p1"
    val p2Selector = "#p2"
    val p3Selector = "#p3"
    val continueButtonSelector = "#continue"
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

    s".show when $testNameWelsh and the user is $testNameAgent" should {

      "display the redeemable shares status page" which {
        lazy val headers = playSessionCookie(scenario.isAgent) ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
        lazy val request = FakeRequest("GET", redeemableSharesStatusUrl).withHeaders(headers: _*)

        lazy val result = {
          dropStockDividendsDB()
          emptyStockDividendsUserDataStub()
          authoriseAgentOrIndividual(scenario.isAgent)
          route(appWithStockDividends, request, "{}").get
        }

        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of OK(200)" in {
          status(result) shouldBe OK
        }

        h1Check(expectedTitle + " " + captionExpected)
        captionCheck(captionExpected)
        formPostLinkCheck(redeemableSharesStatusUrl, Selectors.formSelector)
        textOnPageCheck(expectedP1, Selectors.p1Selector)
        textOnPageCheck(expectedP2, Selectors.p2Selector)
        textOnPageCheck(expectedP3, Selectors.p3Selector)
        buttonCheck(continueText, Selectors.continueButtonSelector)
        radioButtonCheck(yesNo(true), 1)
        radioButtonCheck(yesNo(false), 2)
        welshToggleCheck(scenario.isWelsh)
      }

      "display the redeemable shares status page with session data" which {
        lazy val headers = playSessionCookie(scenario.isAgent) ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
        lazy val request = FakeRequest("GET", redeemableSharesStatusUrl).withHeaders(headers: _*)

        lazy val result: Future[Result] = {
          authoriseAgentOrIndividual(scenario.isAgent)
          dropStockDividendsDB()
          insertStockDividendsCyaData(Some(cyaModel))
          route(appWithStockDividends, request, "{}").get
        }

        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of OK(200)" in {
          status(result) shouldBe OK
        }

        h1Check(expectedTitle + " " + captionExpected)
        captionCheck(captionExpected)
        formPostLinkCheck(redeemableSharesStatusUrl, Selectors.formSelector)
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

      def getCloseCompanyLoanAmount(application: Application): Future[Result] = {
        val headers = Option.when(scenario.isWelsh)(HeaderNames.ACCEPT_LANGUAGE -> "cy").toSeq ++ playSessionCookie(scenario.isAgent)
        lazy val request = FakeRequest("GET", redeemableSharesStatusUrl).withHeaders(headers: _*)

        authoriseAgentOrIndividual(scenario.isAgent)
        route(application, request, "{}").get
      }

      def postRedeemableSharesStatus(body: Seq[(String, String)],
                                     application: Application): Future[Result] = {
        val headers = Seq("Csrf-Token" -> "nocheck") ++
          Option.when(scenario.isWelsh)(HeaderNames.ACCEPT_LANGUAGE -> "cy").toSeq ++
          playSessionCookie(scenario.isAgent)
        val request = FakeRequest("POST", relativepostURL).withHeaders(headers: _*).withFormUrlEncodedBody(body: _*)

        authoriseAgentOrIndividual(scenario.isAgent)
        route(application, request).get
      }


      "return a 303 status and redirect to amount page when true selected with appWithStockDividends" in {
        implicit lazy val app: Application = appWithStockDividends

        lazy val result = {
          dropStockDividendsDB()
          postRedeemableSharesStatus(Seq("value" -> "true"), app)
        }
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe redeemableSharesAmountUrl
      }

      "return a 303 status and redirect to amount page when true selected with appWithStockDividendsBackendMongo" in {
        implicit lazy val application: Application = appWithStockDividendsBackendMongo

        lazy val result = {
          getSessionDataStub(Some(stockDividendsUserDataModel.copy(stockDividends = Some(StockDividendsCheckYourAnswersModel()))))
          updateSessionDataStub()
          postRedeemableSharesStatus(Seq("value" -> "true"), application)
        }
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe redeemableSharesAmountUrl
      }

      "return a 303 status and redirect to next status page when false selected with appWithStockDividends" in {
        implicit lazy val app: Application = appWithStockDividends

        lazy val result = {
          dropStockDividendsDB()
          postRedeemableSharesStatus(Seq("value" -> "false"), app)
        }
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe closeCompanyLoansStatusUrl
    }


      "return a 303 status and redirect to next status page when false selected with appWithStockDividendsBackendMongo" in {
        implicit lazy val application: Application = appWithStockDividendsBackendMongo

        lazy val result = {
          getSessionDataStub(Some(stockDividendsUserDataModel.copy(stockDividends = Some(StockDividendsCheckYourAnswersModel()))))
          updateSessionDataStub()
          postRedeemableSharesStatus(Seq("value" -> "false"), application)
        }
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe closeCompanyLoansStatusUrl
      }

      "return a 303 status and redirect to cya page when isFinished is true with appWithStockDividends" in {
        implicit lazy val app: Application = appWithStockDividends

        lazy val result = {
          dropStockDividendsDB()
          emptyStockDividendsUserDataStub()
          insertStockDividendsCyaData(Some(cyaModel))
          postRedeemableSharesStatus(Seq("value" -> "true"), app)
        }
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe dividendsSummaryUrl
      }

      "return a 303 status and redirect to cya page when isFinished is true with appWithStockDividendsBackendMongo" in {

        implicit lazy val application: Application = appWithStockDividendsBackendMongo

        lazy val result = {
          getSessionDataStub()
          updateSessionDataStub()
          postRedeemableSharesStatus(Seq("value" -> "true"), application)
        }
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe dividendsSummaryUrl
      }

        "return a error" when {
        "the form is empty with appWithStockDividends" which {
          implicit lazy val application: Application = appWithStockDividends

          lazy val result = postRedeemableSharesStatus(Seq("value" -> ""), application)

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

            lazy val result = postRedeemableSharesStatus(Seq("value" -> ""), application)

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