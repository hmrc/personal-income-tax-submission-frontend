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

class StockDividendAmountControllerISpec extends IntegrationTest with ViewHelpers with DefaultBodyWritables with DividendsDatabaseHelper {

  val amount: BigDecimal = 500
  val stockDividendAmountUrl: String = controllers.dividendsBase.routes.StockDividendAmountBaseController.show(taxYear).url
  val redeemableSharesStatusUrl: String = routes.RedeemableSharesStatusController.show(taxYear).url
  val dividendsSummaryUrl: String = routes.DividendsSummaryController.show(taxYear).url
  val postURL: String = routes.StockDividendAmountController.submit(taxYear).url
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
    val expectedParagraph1: String
    val expectedParagraph2: String
    val expectedLabel: String
    val expectedErrorEmpty: String
    val expectedErrorOverMax: String
    val expectedErrorInvalid: String
  }

  trait CommonExpectedResults {
    val expectedH1: String
    val captionExpected: String
    val continueText: String
  }

  object IndividualExpectedEnglish extends SpecificExpectedResults {
    val expectedTitle = "How much did you get?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedLabel = s"$expectedTitle"
    val expectedParagraph1 = "This is the cash value of the stock dividends you received."
    val expectedParagraph2 = "Your dividend statements should show 'the appropriate amount in cash', or the 'cash equivalent in share capital'."
    val expectedErrorEmpty = "Enter the amount of stock dividends you got"
    val expectedErrorInvalid = "Enter the amount of stock dividends you received in the correct format. For example, £193.54"
    val expectedErrorOverMax = "The amount of your stock dividends must be less than £100,000,000,000"
  }

  object AgentExpectedEnglish extends SpecificExpectedResults {
    val expectedTitle = "How much did your client get?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedLabel = s"$expectedTitle"
    val expectedParagraph1 = "This is the cash value of the stock dividends your client received."
    val expectedParagraph2 = "Your client's dividend statements should show 'the appropriate amount in cash', or the 'cash equivalent in share capital'."
    val expectedErrorEmpty = "Enter the amount of stock dividends your client got"
    val expectedErrorInvalid = "Enter the amount of stock dividends your client received in the correct format. For example, £193.54"
    val expectedErrorOverMax = "The amount of your client's stock dividends must be less than £100,000,000,000"
  }

  object AllExpectedEnglish extends CommonExpectedResults {
    val expectedH1 = "Stock dividends amount"
    val continueText = "Continue"
    val captionExpected = s"Dividends for 6 April $taxYearEOY to 5 April $taxYear"
  }

  object IndividualExpectedWelsh extends SpecificExpectedResults {
    val expectedTitle = "Faint gawsoch chi?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedLabel = s"$expectedTitle"
    val expectedParagraph1 = "Dyma werth ariannol y difidendau stoc a gawsoch."
    val expectedParagraph2 =
      "Dylai’ch datganiadau difidendau ddangos y swm priodol mewn arian parod, neu’r cyfwerth mewn arian parod o gyfranddaliadau cyfalaf."
    val expectedErrorEmpty = "Nodwch swm y difidendau stoc a gawsoch"
    val expectedErrorInvalid = "Nodwch swm y difidendau stoc a gawsoch yn y fformat cywir. Er enghraifft, £193.54"
    val expectedErrorOverMax = "Mae’n rhaid i swm eich difidendau stoc fod yn llai na £100,000,000,000"
  }

  object AgentExpectedWelsh extends SpecificExpectedResults {
    val expectedTitle = "Faint gafodd eich cleient?"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
    val expectedLabel = s"$expectedTitle"
    val expectedParagraph1 = "Dyma werth ariannol y difidendau stoc a gafodd eich cleient."
    val expectedParagraph2 =
      "Dylai datganiadau difidend eich cleient ddangos y swm priodol mewn arian parod, neu’r cyfwerth mewn arian parod o gyfranddaliadau cyfalaf."
    val expectedErrorEmpty = "Nodwch swm y difidendau stoc a gafodd eich cleient"
    val expectedErrorInvalid = "Nodwch swm y difidendau stoc a gafodd eich cleient yn y fformat cywir. Er enghraifft, £193.54"
    val expectedErrorOverMax = "Mae’n rhaid i swm difidendau stoc eich cleient fod yn llai na £100,000,000,000"
  }

  object AllExpectedWelsh extends CommonExpectedResults {
    val expectedH1 = "Swm y difidendau stoc"
    val continueText = "Yn eich blaen"
    val captionExpected = s"Difidendau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
  }

  object Selectors {
    val poundPrefixSelector = ".govuk-input__prefix"
    val captionSelector = ".govuk-caption-l"
    val continueButtonSelector = "#continue"
    val formSelector = "#main-content > div > div > form"
    val paragraph1 = "#p1"
    val paragraph2 = "#p2"
    val inputSelector = "#amount"
    val errorSummaryHref = "#amount"
    val errorSelector = "#amount-error"
    val label = "#main-content > div > div > form > div > label"
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

    def postStockDividendAmount(body: Seq[(String, String)],
                                application: Application): Future[Result] = {
      val headers = Seq("Csrf-Token" -> "nocheck") ++
        Option.when(scenario.isWelsh)(HeaderNames.ACCEPT_LANGUAGE -> "cy").toSeq ++
        playSessionCookie(scenario.isAgent)
      val request = FakeRequest("POST", postURL).withHeaders(headers: _*).withFormUrlEncodedBody(body: _*)

      authoriseAgentOrIndividual(scenario.isAgent)
      route(application, request).get
    }

    def getStockDividendAmount(application: Application): Future[Result] = {
      val headers = Option.when(scenario.isWelsh)(HeaderNames.ACCEPT_LANGUAGE -> "cy").toSeq ++ playSessionCookie(scenario.isAgent)
      lazy val request = FakeRequest("GET", stockDividendAmountUrl).withHeaders(headers: _*)

      authoriseAgentOrIndividual(scenario.isAgent)
      route(application, request, "{}").get
    }

    lazy val uniqueResults = scenario.specificExpectedResults.get
    import scenario.commonExpectedResults._
    import uniqueResults._

    val testNameWelsh = if (scenario.isWelsh) "in Welsh" else "in English"
    val testNameAgent = if (scenario.isAgent) "an agent" else "an individual"

    s".show when $testNameWelsh and the user is $testNameAgent" should {

      "display the stock dividend amount page with appWithStockDividendsBackendMongo" which {
        implicit lazy val application: Application = appWithStockDividendsBackendMongo

        lazy val result = getStockDividendAmount(application)
        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of OK(200)" in {
          getSessionDataStub()
          status(result) shouldBe OK
        }

        titleCheck(expectedTitle, scenario.isWelsh)
        h1Check(expectedH1 + " " + captionExpected)
        captionCheck(captionExpected)
        formPostLinkCheck(stockDividendAmountUrl, Selectors.formSelector)
        textOnPageCheck(expectedParagraph1, Selectors.paragraph1)
        textOnPageCheck(expectedParagraph2, Selectors.paragraph2)
        textOnPageCheck(expectedLabel, Selectors.label)
        buttonCheck(continueText, Selectors.continueButtonSelector)
        inputFieldCheck(amountInputName, Selectors.inputSelector)
      }

      "display the stock dividend amount page with appWithStockDividends" which {
        implicit lazy val application: Application = appWithStockDividends

        lazy val result = getStockDividendAmount(application)
        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of OK(200)" in {
          status(result) shouldBe OK
        }

        titleCheck(expectedTitle, scenario.isWelsh)
        h1Check(expectedH1 + " " + captionExpected)
        captionCheck(captionExpected)
        formPostLinkCheck(stockDividendAmountUrl, Selectors.formSelector)
        textOnPageCheck(expectedParagraph1, Selectors.paragraph1)
        textOnPageCheck(expectedParagraph2, Selectors.paragraph2)
        textOnPageCheck(expectedLabel, Selectors.label)
        buttonCheck(continueText, Selectors.continueButtonSelector)
        inputFieldCheck(amountInputName, Selectors.inputSelector)
      }

      "display the stock dividend amount page with session data with appWithStockDividendsBackendMongo" which {
        implicit lazy val application: Application = appWithStockDividendsBackendMongo

        lazy val result = getStockDividendAmount(application)
        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of OK(200)" in {
          getSessionDataStub()
          status(result) shouldBe OK
        }

        titleCheck(expectedTitle, scenario.isWelsh)
        h1Check(expectedH1 + " " + captionExpected)
        captionCheck(captionExpected)
        formPostLinkCheck(stockDividendAmountUrl, Selectors.formSelector)
        textOnPageCheck(expectedParagraph1, Selectors.paragraph1)
        textOnPageCheck(expectedParagraph2, Selectors.paragraph2)
        textOnPageCheck(expectedLabel, Selectors.label)
        buttonCheck(continueText, Selectors.continueButtonSelector)
        inputFieldCheck(amountInputName, Selectors.inputSelector)
      }

      "display the stock dividend amount page with session data with appWithStockDividends" which {
        implicit lazy val application: Application = appWithStockDividends

        lazy val result = getStockDividendAmount(application)
        implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

        "has a status of OK(200)" in {
          clearSession()
          emptyUserDataStub()
          emptyStockDividendsUserDataStub()
          insertStockDividendsCyaData(Some(cyaModel))
          status(result) shouldBe OK
        }

        titleCheck(expectedTitle, scenario.isWelsh)
        h1Check(expectedH1 + " " + captionExpected)
        captionCheck(captionExpected)
        formPostLinkCheck(stockDividendAmountUrl, Selectors.formSelector)
        textOnPageCheck(expectedParagraph1, Selectors.paragraph1)
        textOnPageCheck(expectedParagraph2, Selectors.paragraph2)
        textOnPageCheck(expectedLabel, Selectors.label)
        buttonCheck(continueText, Selectors.continueButtonSelector)
        inputFieldCheck(amountInputName, Selectors.inputSelector)
      }
    }

    s".submit when $testNameWelsh and the user is $testNameAgent" should {

      "return a 303 status and redirect to next status page with appWithStockDividendsBackendMongo" in {
        implicit lazy val application: Application = appWithStockDividendsBackendMongo

        lazy val result = {
          getSessionDataStub(Some(stockDividendsUserDataModel.copy(
            stockDividends = Some(StockDividendsCheckYourAnswersModel()))))
          updateSessionDataStub()
          postStockDividendAmount(Seq("amount" -> "123"), application)
        }
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe redeemableSharesStatusUrl
      }

      "return a 303 status and redirect to next status page with appWithStockDividends" in {
        implicit lazy val application: Application = appWithStockDividends

        lazy val result = {
          dropStockDividendsDB()
          insertStockDividendsCyaData(Some(StockDividendsCheckYourAnswersModel()))
          postStockDividendAmount(Seq("amount" -> "123"), application)
        }
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe redeemableSharesStatusUrl
      }

      "return a 303 status and redirect to cya page when isFinished is true with appWithStockDividendsBackendMongo" in {
        implicit lazy val application: Application = appWithStockDividendsBackendMongo

        lazy val result = {
          getSessionDataStub()
          updateSessionDataStub()
          postStockDividendAmount(Seq("amount" -> "123"), application)
        }
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe dividendsSummaryUrl
      }

      "return a 303 status and redirect to cya page when isFinished is true with appWithStockDividends" in {
        implicit lazy val application: Application = appWithStockDividends

        lazy val result = {
          dropStockDividendsDB()
          insertStockDividendsCyaData(Some(cyaModel))
          postStockDividendAmount(Seq("amount" -> "123"), application)
        }
        status(result) shouldBe SEE_OTHER
        redirectLocation(result).value shouldBe dividendsSummaryUrl
      }

      "return a error" when {
        "the form is empty with appWithStockDividendsBackendMongo" which {
          implicit lazy val application: Application = appWithStockDividendsBackendMongo

          lazy val result = postStockDividendAmount(Seq.empty, application)

          implicit val document: () => Document = () => Jsoup.parse(bodyOf(result))

          "has a 400 BAD_REQUEST status " in {
            status(result) shouldBe BAD_REQUEST
          }

          titleCheck(errorPrefix(scenario.isWelsh) + expectedTitle, scenario.isWelsh)
          errorAboveElementCheck(expectedErrorEmpty)
          errorSummaryCheck(expectedErrorEmpty, Selectors.errorSummaryHref, scenario.isWelsh)
        }

        "the form is empty with appWithStockDividends" which {
          implicit lazy val application: Application = appWithStockDividends

          lazy val result = postStockDividendAmount(Seq.empty, application)

          implicit val document: () => Document = () => Jsoup.parse(bodyOf(result))

          "has a 400 BAD_REQUEST status " in {
            status(result) shouldBe BAD_REQUEST
          }

          titleCheck(errorPrefix(scenario.isWelsh) + expectedTitle, scenario.isWelsh)
          errorAboveElementCheck(expectedErrorEmpty)
          errorSummaryCheck(expectedErrorEmpty, Selectors.errorSummaryHref, scenario.isWelsh)
        }

        "the form is invalid with appWithStockDividendsBackendMongo" which {
          implicit lazy val application: Application = appWithStockDividendsBackendMongo

          lazy val result = postStockDividendAmount(Seq("amount" -> "$$$"), application)

          implicit val document: () => Document = () => Jsoup.parse(bodyOf(result))

          "has a 400 BAD_REQUEST status " in {
            status(result) shouldBe BAD_REQUEST
          }

          titleCheck(errorPrefix(scenario.isWelsh) + expectedTitle, scenario.isWelsh)
          errorAboveElementCheck(expectedErrorInvalid)
          errorSummaryCheck(expectedErrorInvalid, Selectors.errorSummaryHref, scenario.isWelsh)
        }

        "the form is invalid with appWithStockDividends" which {
          implicit lazy val application: Application = appWithStockDividends

          lazy val result = postStockDividendAmount(Seq("amount" -> "$$$"), application)

          implicit val document: () => Document = () => Jsoup.parse(bodyOf(result))

          "has a 400 BAD_REQUEST status " in {
            status(result) shouldBe BAD_REQUEST
          }

          titleCheck(errorPrefix(scenario.isWelsh) + expectedTitle, scenario.isWelsh)
          errorAboveElementCheck(expectedErrorInvalid)
          errorSummaryCheck(expectedErrorInvalid, Selectors.errorSummaryHref, scenario.isWelsh)
        }

        "the form is overmax with appWithStockDividendsBackendMongo" which {
          implicit lazy val application: Application = appWithStockDividendsBackendMongo

          lazy val result = postStockDividendAmount(Seq("amount" -> "103242424234242342423423"), application)

          implicit val document: () => Document = () => Jsoup.parse(bodyOf(result))

          "has a 400 BAD_REQUEST status " in {
            status(result) shouldBe BAD_REQUEST
          }

          titleCheck(errorPrefix(scenario.isWelsh) + expectedTitle, scenario.isWelsh)
          errorAboveElementCheck(expectedErrorOverMax)
          errorSummaryCheck(expectedErrorOverMax, Selectors.errorSummaryHref, scenario.isWelsh)
        }

        "the form is overmax with appWithStockDividends" which {
          implicit lazy val application: Application = appWithStockDividends

          lazy val result = postStockDividendAmount(Seq("amount" -> "103242424234242342423423"), application)

          implicit val document: () => Document = () => Jsoup.parse(bodyOf(result))

          "has a 400 BAD_REQUEST status " in {
            status(result) shouldBe BAD_REQUEST
          }

          titleCheck(errorPrefix(scenario.isWelsh) + expectedTitle, scenario.isWelsh)
          errorAboveElementCheck(expectedErrorOverMax)
          errorSummaryCheck(expectedErrorOverMax, Selectors.errorSummaryHref, scenario.isWelsh)
        }
      }
    }
  }
}
