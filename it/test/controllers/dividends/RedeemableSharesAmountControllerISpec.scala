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

class RedeemableSharesAmountControllerISpec extends IntegrationTest with ViewHelpers with DefaultBodyWritables with DividendsDatabaseHelper {

  val amount: BigDecimal = 500
  val redeemableSharesAmountUrl: String = controllers.dividendsBase.routes.RedeemableSharesAmountBaseController.show(taxYear).url
  val closeCompanyLoansStatusUrl: String = routes.CloseCompanyLoanStatusController.show(taxYear).url
  val dividendsSummaryUrl: String = routes.DividendsSummaryController.show(taxYear).url
  val relativepostURL: String = routes.RedeemableSharesAmountController.submit(taxYear).url

  val postURL: String = s"$appUrl/$taxYear/dividends/redeemable-shares-amount"
  val poundPrefixText = "£"

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
    val expectedH1: String
    val expectedHint: String
    val expectedErrorEmpty: String
    val expectedErrorInvalid: String
  }

  trait CommonExpectedResults {
    val captionExpected: String
    val continueText: String
    val expectedErrorOverMax: String
  }

  object IndividualExpectedEnglish extends SpecificExpectedResults {
    val expectedTitle = "How much did you get in free or redeemable shares?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedH1 = s"$expectedTitle"
    val expectedHint = "Enter the sum of all the shares. You can find this information on your dividend vouchers."
    val expectedErrorEmpty = "Enter the amount you got in free or redeemable shares"
    val expectedErrorInvalid = "Enter the amount you received in the correct format. For example, £193.54"
  }

  object AgentExpectedEnglish extends SpecificExpectedResults {
    val expectedTitle = "How much did your client get in free or redeemable shares?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedH1 = s"$expectedTitle"
    val expectedHint = "Enter the sum of all the shares. You can find this information on your client's dividend vouchers."
    val expectedErrorEmpty = "Enter the amount your client got in free or redeemable shares"
    val expectedErrorInvalid = "Enter the amount your client received in the correct format. For example, £193.54"
  }

  object AllExpectedEnglish extends CommonExpectedResults {
    val continueText = "Continue"
    val captionExpected = s"Dividends for 6 April $taxYearEOY to 5 April $taxYear"
    val expectedErrorOverMax = "Enter an amount less than £100,000,000,000"
  }

  object IndividualExpectedWelsh extends SpecificExpectedResults {
    val expectedTitle = "Faint gawsoch chi mewn cyfranddaliadau adbryn neu mewn cyfranddaliadau yn rhad ac am ddim?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedH1 = s"$expectedTitle"
    val expectedHint = "Nodwch swm yr holl gyfranddaliadau. Gallwch ddod o hyd i’r wybodaeth hon ar eich talebau difidend."
    val expectedErrorEmpty = "Nodwch y swm a gawsoch mewn cyfranddaliadau adbryn neu mewn cyfranddaliadau yn rhad ac am ddim"
    val expectedErrorInvalid = "Nodwch y swm a gawsoch yn y fformat cywir. Er enghraifft, £193.54"
  }

  object AgentExpectedWelsh extends SpecificExpectedResults {
    val expectedTitle = "Faint gafodd eich cleient mewn cyfranddaliadau adbryn neu mewn cyfranddaliadau yn rhad ac am ddim?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val expectedH1 = s"$expectedTitle"
    val expectedHint = "Nodwch swm yr holl gyfranddaliadau. Gallwch ddod o hyd i’r wybodaeth hon ar dalebau difidend eich cleient."
    val expectedErrorEmpty = "Nodwch y swm a gafodd eich cleient mewn cyfranddaliadau adbryn neu mewn cyfranddaliadau yn rhad ac am ddim"
    val expectedErrorInvalid = "Nodwch y swm a gafodd eich cleient yn y fformat cywir. Er enghraifft, £193.54"
  }

  object AllExpectedWelsh extends CommonExpectedResults {
    val continueText = "Yn eich blaen"
    val captionExpected = s"Difidendau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val expectedErrorOverMax = "Nodwch swm sy’n llai na £100,000,000,000"
  }

  object Selectors {
    val poundPrefixSelector = ".govuk-input__prefix"
    val captionSelector = ".govuk-caption-l"
    val continueButtonSelector = "#continue"
    val formSelector = "#main-content > div > div > form"
    val inputSelector = "#amount"
    val errorSummaryHref = "#amount"
    val errorSelector = "#amount-error"
    val hintSelector = "#amount-hint"
    val headingSelector = "#main-content > div > div > h1"
  }

  val amountInputName = "amount"

  protected val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = Seq(
    UserScenario(isWelsh = false, isAgent = false, AllExpectedEnglish, Some(IndividualExpectedEnglish)),
    UserScenario(isWelsh = false, isAgent = true, AllExpectedEnglish, Some(AgentExpectedEnglish)),
    UserScenario(isWelsh = true, isAgent = false, AllExpectedWelsh, Some(IndividualExpectedWelsh)),
    UserScenario(isWelsh = true, isAgent = true, AllExpectedWelsh, Some(AgentExpectedWelsh))
  )

  userScenarios.foreach { scenario =>

    def postRedeemableSharesAmount(body: Seq[(String, String)],
                                   application: Application): Future[Result] = {
      val headers = Seq("Csrf-Token" -> "nocheck") ++
        Option.when(scenario.isWelsh)(HeaderNames.ACCEPT_LANGUAGE -> "cy").toSeq ++
        playSessionCookie(scenario.isAgent)
      val request = FakeRequest("POST", relativepostURL).withHeaders(headers: _*).withFormUrlEncodedBody(body: _*)

      authoriseAgentOrIndividual(scenario.isAgent)
      route(application, request).get
    }

    def getRedeemableSharesAmount(application: Application): Future[Result] = {
      val headers = Option.when(scenario.isWelsh)(HeaderNames.ACCEPT_LANGUAGE -> "cy").toSeq ++ playSessionCookie(scenario.isAgent)
      lazy val request = FakeRequest("GET", redeemableSharesAmountUrl).withHeaders(headers: _*)

      authoriseAgentOrIndividual(scenario.isAgent)
      route(application, request, "{}").get
    }

    lazy val uniqueResults = scenario.specificExpectedResults.get
    import scenario.commonExpectedResults._
    import uniqueResults._

    val testNameWelsh = if (scenario.isWelsh) "in Welsh" else "in English"
    val testNameAgent = if (scenario.isAgent) "an agent" else "an individual"

    s".show when $testNameWelsh and the user is $testNameAgent" should {

      "display the redeemable shares amount page" which {
        lazy val headers = playSessionCookie(scenario.isAgent) ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
        lazy val request = FakeRequest("GET", redeemableSharesAmountUrl).withHeaders(headers: _*)

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

        titleCheck(expectedTitle, scenario.isWelsh)
        h1Check(expectedH1 + " " + captionExpected)
        captionCheck(captionExpected)
        formPostLinkCheck(redeemableSharesAmountUrl, Selectors.formSelector)
        textOnPageCheck(expectedHint, Selectors.hintSelector)
        buttonCheck(continueText, Selectors.continueButtonSelector)
        inputFieldCheck(amountInputName, Selectors.inputSelector)
      }

      "display the redeemable shares amount page with session data" which {
        lazy val headers = playSessionCookie(scenario.isAgent) ++ (if (scenario.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
        lazy val request = FakeRequest("GET", redeemableSharesAmountUrl).withHeaders(headers: _*)

        lazy val result = {
          dropStockDividendsDB()
          insertStockDividendsCyaData(Some(cyaModel))
          emptyStockDividendsUserDataStub()
          authoriseAgentOrIndividual(scenario.isAgent)
          route(appWithStockDividends, request, "{}").get
        }

        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of OK(200)" in {
          status(result) shouldBe OK
        }

        titleCheck(expectedTitle, scenario.isWelsh)
        h1Check(expectedH1 + " " + captionExpected)
        captionCheck(captionExpected)
        formPostLinkCheck(redeemableSharesAmountUrl, Selectors.formSelector)
        textOnPageCheck(expectedHint, Selectors.hintSelector)
        buttonCheck(continueText, Selectors.continueButtonSelector)
        inputFieldCheck(amountInputName, Selectors.inputSelector)
      }
    }

    s".submit when $testNameWelsh and the user is $testNameAgent" should {

      "return a 303 status and redirect to next status page with appWithStockDividends" in {
        implicit lazy val app: Application = appWithStockDividends

        lazy val result = {
          dropStockDividendsDB()
          insertStockDividendsCyaData(Some(StockDividendsCheckYourAnswersModel()))
          postRedeemableSharesAmount(Seq("amount" -> "123"), app)
        }
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe closeCompanyLoansStatusUrl
      }

      "return a 303 status and redirect to next status page with appWithStockDividendsBackendMongo" in {
        implicit lazy val app: Application = appWithStockDividendsBackendMongo

        lazy val resultBackEnd = {
          getSessionDataStub(Some(stockDividendsUserDataModel.copy(
            stockDividends = Some(StockDividendsCheckYourAnswersModel()))))
          updateSessionDataStub()
          postRedeemableSharesAmount(Seq("amount" -> "123"), app)
        }
        status(resultBackEnd) shouldBe SEE_OTHER
        redirectLocation(resultBackEnd).value shouldBe closeCompanyLoansStatusUrl
      }


      "return a 303 status and redirect to cya page when isFinished is true with appWithStockDividends" in {
        implicit lazy val app: Application = appWithStockDividends

        lazy val result = {
          dropStockDividendsDB()
          insertStockDividendsCyaData(Some(cyaModel))
          postRedeemableSharesAmount(Seq("amount" -> "123"), app)
        }
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe dividendsSummaryUrl
      }

      "return a 303 status and redirect to cya page when isFinished is true with appWithStockDividendsBackendMongo" in {
        implicit lazy val app: Application = appWithStockDividendsBackendMongo

        lazy val result = {
          getSessionDataStub()
          updateSessionDataStub()
          postRedeemableSharesAmount(Seq("amount" -> "123"), app)
        }
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe dividendsSummaryUrl

      }

      "return a error" when {
        "the form is empty appWithStockDividends" which {
          implicit lazy val app: Application = appWithStockDividends

          lazy val result = postRedeemableSharesAmount(Seq.empty, app)

          implicit val document: () => Document = () => Jsoup.parse(bodyOf(result))

          "has a 400 BAD_REQUEST status " in {
            status(result) shouldBe BAD_REQUEST
          }

          titleCheck(errorPrefix(scenario.isWelsh) + expectedTitle, scenario.isWelsh)
          errorAboveElementCheck(expectedErrorEmpty)
          errorSummaryCheck(expectedErrorEmpty, Selectors.errorSummaryHref, scenario.isWelsh)
        }

        "the form is empty with appWithStockDividendsBackendMongo" which {
          implicit lazy val app: Application = appWithStockDividendsBackendMongo

          lazy val resultBackEnd = postRedeemableSharesAmount(Seq.empty, app)

          implicit val document: () => Document = () => Jsoup.parse(bodyOf(resultBackEnd))

          "has a 400 BAD_REQUEST status " in {
            status(resultBackEnd) shouldBe BAD_REQUEST
          }

          titleCheck(errorPrefix(scenario.isWelsh) + expectedTitle, scenario.isWelsh)
          errorAboveElementCheck(expectedErrorEmpty)
          errorSummaryCheck(expectedErrorEmpty, Selectors.errorSummaryHref, scenario.isWelsh)
        }

        "the form is invalid with appWithStockDividends" which {
          implicit lazy val app: Application = appWithStockDividends

          lazy val result = postRedeemableSharesAmount(Seq("amount" -> "$$$"), app)

          implicit val document: () => Document = () => Jsoup.parse(bodyOf(result))

          "has a 400 BAD_REQUEST status " in {
            status(result) shouldBe BAD_REQUEST
          }

          titleCheck(errorPrefix(scenario.isWelsh) + expectedTitle, scenario.isWelsh)
          errorAboveElementCheck(expectedErrorInvalid)
          errorSummaryCheck(expectedErrorInvalid, Selectors.errorSummaryHref, scenario.isWelsh)
        }

        "the form is overmax with appWithStockDividends" which {
          implicit lazy val app: Application = appWithStockDividends

          lazy val result = postRedeemableSharesAmount(Seq("amount" -> "103242424234242342423423"), app)

          implicit val document: () => Document = () => Jsoup.parse(bodyOf(result))

          "has a 400 BAD_REQUEST status " in {
            status(result) shouldBe BAD_REQUEST
          }
          titleCheck(errorPrefix(scenario.isWelsh) + expectedTitle, scenario.isWelsh)
          errorAboveElementCheck(expectedErrorOverMax)
          errorSummaryCheck(expectedErrorOverMax, Selectors.errorSummaryHref, scenario.isWelsh)
        }

        "the form is overmax with appWithStockDividendsBackendMongo" which {
          implicit lazy val app: Application = appWithStockDividendsBackendMongo

          lazy val resultBackEnd = postRedeemableSharesAmount(Seq("amount" -> "103242424234242342423423"), app)

          implicit val document: () => Document = () => Jsoup.parse(bodyOf(resultBackEnd))

          "has a 400 BAD_REQUEST status " in {
            status(resultBackEnd) shouldBe BAD_REQUEST
          }

          titleCheck(errorPrefix(scenario.isWelsh) + expectedTitle, scenario.isWelsh)
          errorAboveElementCheck(expectedErrorOverMax)
          errorSummaryCheck(expectedErrorOverMax, Selectors.errorSummaryHref, scenario.isWelsh)
        }
      }
    }
  }}
