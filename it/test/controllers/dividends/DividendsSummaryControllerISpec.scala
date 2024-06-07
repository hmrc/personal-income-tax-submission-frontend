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

import config.STOCK_DIVIDENDS
import controllers.dividends.routes
import models.dividends._
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{INTERNAL_SERVER_ERROR, NO_CONTENT, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.mvc.Headers
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, route}
import test.utils.{DividendsDatabaseHelper, IntegrationTest, ViewHelpers}

class DividendsSummaryControllerISpec extends IntegrationTest with ViewHelpers with DividendsDatabaseHelper {


  val ukDividends: BigDecimal = 10
  val otherDividends: BigDecimal = 10.50

  val amount: BigDecimal = 123.45
  val relativeUrl: String = routes.DividendsSummaryController.show(taxYear).url

  val dividendsSummaryUrl = s"$appUrl/$taxYear/dividends/summary"

  val dividendsFromStocksAndSharesHref = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/dividends-from-stocks-and-shares"
  val dividendsStatusHref = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/dividends-from-uk-companies"
  val dividendsAmountHref = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/how-much-dividends-from-uk-companies"
  val dividendsOtherStatusHref: String = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends" +
    "/dividends-from-uk-trusts-or-open-ended-investment-companies"
  val dividendsOtherAmountHref: String = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends" +
    "/how-much-dividends-from-uk-trusts-and-open-ended-investment-companies"
  val stockDividendsStatusHref = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/stock-dividend-status"
  val stockDividendsAmountHref = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/stock-dividend-amount"
  val redeemableSharesStatusHref = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/redeemable-shares-status"
  val redeemableSharesAmountHref = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/redeemable-shares-amount"
  val closeCompanyLoanStatusHref = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/close-company-loan-status"
  val closeCompanyLoanAmountHref = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/close-company-loan-amount"

  lazy val dividendsCyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(
    Some(true),
    Some(true), Some(ukDividends),
    Some(true), Some(otherDividends)
  )
  lazy val dividendsNoModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(Some(false), Some(false), None, Some(false))

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

  val stockDividend: Option[StockDividendModel] = Some(StockDividendModel(customerReference = Some("ref"), grossAmount = amount))

  object Selectors {

    final val CYA_TITLE_1 = 1
    final val CYA_TITLE_2 = 2
    final val CYA_TITLE_3 = 3
    final val CYA_TITLE_4 = 4
    final val CYA_TITLE_5 = 5
    final val CYA_TITLE_6 = 6
    final val CYA_TITLE_7 = 7
    final val CYA_TITLE_8 = 8
    final val CYA_TITLE_9 = 9
    final val CYA_TITLE_10 = 10
    final val CYA_TITLE_11 = 11

    def cyaTitle(i: Int): String = s"#main-content > div > div > dl > div:nth-child($i) > dt"

    def cyaChangeLink(i: Int): String = s"#main-content > div > div > dl > div:nth-child($i) > dd.govuk-summary-list__actions > a"

    val captionSelector = "#main-content > div > div > h1 > span"
    val continueButtonSelector = "#continue"
  }

  trait SpecificExpectedResults {
    val headingExpected: String
  }

  trait CommonExpectedResults {
    val captionExpected: String
    val continueButtonText: String
    val changeLinkExpected: String
    val dividendsFromStocksAndSharesText: String
    val dividendsStatusText: String
    val dividendsAmountText: String
    val dividendsOtherStatusText: String
    val dividendsOtherAmountText: String
    val stockDividendsStatusText: String
    val stockDividendsAmountText: String
    val redeemableSharesStatusText: String
    val redeemableSharesAmountText: String
    val closeCompanyLoanStatusText: String
    val closeCompanyLoanAmountText: String

    val dividendsFromStocksAndSharesHiddenText: String
    val dividendsStatusHiddenText: String
    val dividendsAmountHiddenText: String
    val dividendsOtherStatusHiddenText: String
    val dividendsOtherAmountHiddenText: String
    val stockDividendsStatusHiddenText: String
    val stockDividendsAmountHiddenText: String
    val redeemableSharesStatusHiddenText: String
    val redeemableSharesAmountHiddenText: String
    val closeCompanyLoanStatusHiddenText: String
    val closeCompanyLoanAmountHiddenText: String

  }

  object AllExpectedEnglish extends CommonExpectedResults {
    val captionExpected = s"Dividends for 6 April $taxYearEOY to 5 April $taxYear"
    val continueButtonText = "Save and continue"
    val changeLinkExpected = "Change"
    val dividendsFromStocksAndSharesText = "Dividends from stocks and shares"
    val dividendsStatusText = "Dividends from UK companies"
    val dividendsAmountText = "Amount of dividends from UK companies"
    val dividendsOtherStatusText = "Dividends from unit trusts or investment companies"
    val dividendsOtherAmountText = "Amount of dividends from unit trusts or investment companies"
    val stockDividendsStatusText = "Stock dividends"
    val stockDividendsAmountText = "Value of stock dividends"
    val redeemableSharesStatusText = "Free or redeemable shares"
    val redeemableSharesAmountText = "Value of free or redeemable shares"
    val closeCompanyLoanStatusText = "Close company loan written off or released"
    val closeCompanyLoanAmountText = "Value of close company loan written off or released"

    val dividendsFromStocksAndSharesHiddenText = "dividends from stocks and shares"
    val dividendsStatusHiddenText = "dividends from UK companies"
    val dividendsAmountHiddenText = "amount of dividends from UK companies"
    val dividendsOtherStatusHiddenText = "dividends from unit trusts or investment companies"
    val dividendsOtherAmountHiddenText = "amount of dividends from unit trusts or investment companies"
    val stockDividendsStatusHiddenText = "stock dividends"
    val stockDividendsAmountHiddenText = "value of stock dividends"
    val redeemableSharesStatusHiddenText = "free or redeemable shares"
    val redeemableSharesAmountHiddenText = "value of free or redeemable shares"
    val closeCompanyLoanStatusHiddenText = "close company loan written off or released"
    val closeCompanyLoanAmountHiddenText = "value of close company loan written off or released"
  }

  object AllExpectedWelsh extends CommonExpectedResults {
    val captionExpected = s"Difidendau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val continueButtonText = "Cadw ac yn eich blaen"
    val changeLinkExpected = "Newid"
    val dividendsFromStocksAndSharesText = "Difidendau o stociau a chyfranddaliadau"
    val dividendsStatusText = "Difidendau o gwmnïau yn y DU"
    val dividendsAmountText = "Swm y difidendau o gwmnïau yn DU"
    val dividendsOtherStatusText = "Difidendau o ymddiriedolaethau unedol neu gwmnïau buddsoddi"
    val dividendsOtherAmountText = "Swm y difidendau o ymddiriedolaethau unedol neu gwmnïau buddsoddi"
    val stockDividendsStatusText = "Difidendau stoc"
    val stockDividendsAmountText = "Gwerth y difidendau stoc"
    val redeemableSharesStatusText = "Difidendau adbryn neu ddifidendau sy’n rhad ac am ddim"
    val redeemableSharesAmountText = "Gwerth y difidendau adbryn neu’r difidendau sy’n rhad ac am ddim"
    val closeCompanyLoanStatusText = "Benthyciadau gan gwmnïau caeedig a ddilëwyd neu a ryddhawyd"
    val closeCompanyLoanAmountText = "Gwerth benthyciadau gan gwmnïau caeedig a ddilëwyd neu a ryddhawyd"

    val dividendsFromStocksAndSharesHiddenText = "difidendau o stociau a chyfranddaliadau"
    val dividendsStatusHiddenText = "difidendau o gwmnïau yn DU"
    val dividendsAmountHiddenText = "swm y difidendau o gwmnïau yn DU"
    val dividendsOtherStatusHiddenText = "difidendau o ymddiriedolaethau unedol neu gwmnïau buddsoddi"
    val dividendsOtherAmountHiddenText = "swm y difidendau o ymddiriedolaethau unedol neu gwmnïau buddsoddi"
    val stockDividendsStatusHiddenText = "difidendau stoc"
    val stockDividendsAmountHiddenText = "gwerth y difidendau stoc"
    val redeemableSharesStatusHiddenText = "difidendau adbryn neu ddifidendau sy’n rhad ac am ddim"
    val redeemableSharesAmountHiddenText = "gwerth y difidendau adbryn neu’r difidendau sy’n rhad ac am ddim"
    val closeCompanyLoanStatusHiddenText = "benthyciadau gan gwmnïau caeedig a ddilëwyd neu a ryddhawyd"
    val closeCompanyLoanAmountHiddenText = "gwerth benthyciadau gan gwmnïau caeedig a ddilëwyd neu a ryddhawyd"

  }

  object IndividualExpectedEnglish extends SpecificExpectedResults {
    val headingExpected = "Check your dividends"
  }


  object IndividualExpectedWelsh extends SpecificExpectedResults {
    val headingExpected = "Gwirio’ch difidendau"
  }

  object AgentExpectedEnglish extends SpecificExpectedResults {
    val headingExpected = "Check your client's dividends"

  }

  object AgentExpectedWelsh extends SpecificExpectedResults {
    val headingExpected = "Gwiriwch ddifidendau eich cleient"

  }

  private val userScenarios =
    Seq(UserScenario(isWelsh = false, isAgent = false, AllExpectedEnglish, Some(IndividualExpectedEnglish)),
      UserScenario(isWelsh = false, isAgent = true, AllExpectedEnglish, Some(AgentExpectedEnglish)),
      UserScenario(isWelsh = true, isAgent = false, AllExpectedWelsh, Some(IndividualExpectedWelsh)),
      UserScenario(isWelsh = true, isAgent = true, AllExpectedWelsh, Some(AgentExpectedWelsh)))

  ".show" when {


    userScenarios.foreach { us =>

      import Selectors._
      import us.commonExpectedResults._
      import us.specificExpectedResults._

      s"language is ${welshTest(us.isWelsh)} and request is from an ${agentTest(us.isAgent)}" should {


       "renders Dividends summary page with correct content when there is data in session" which {

          lazy val headers = playSessionCookie(us.isAgent) ++ (if (us.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
          lazy val request = FakeRequest("GET", relativeUrl).withHeaders(headers: _*)

          lazy val result = {
            authoriseAgentOrIndividual(us.isAgent)
            emptyUserDataStub()
            dropStockDividendsDB()
            emptyStockDividendsUserDataStub()
            insertStockDividendsCyaData(Some(cyaModel))
            route(appWithStockDividends, request, "{}").get
          }

          "has an OK(200) status" in {
            status(result) shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

          h1Check(get.headingExpected + " " + captionExpected)
          textOnPageCheck(captionExpected, Selectors.captionSelector)
          "has an area for section 1" which {
            textOnPageCheck(dividendsFromStocksAndSharesText, Selectors.cyaTitle(CYA_TITLE_1))
            linkCheck(s"$changeLinkExpected $dividendsFromStocksAndSharesHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_1), dividendsFromStocksAndSharesHref)
          }
          "has an area for section 2" which {
            textOnPageCheck(dividendsStatusText, Selectors.cyaTitle(CYA_TITLE_2))
            linkCheck(s"$changeLinkExpected $dividendsStatusHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_2), dividendsStatusHref)
          }
          "has an area for section 3" which {
            textOnPageCheck(dividendsAmountText, Selectors.cyaTitle(CYA_TITLE_3))
            linkCheck(s"$changeLinkExpected $dividendsAmountHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_3), dividendsAmountHref)
          }
          "has an area for section 4" which {
            textOnPageCheck(dividendsOtherStatusText, Selectors.cyaTitle(CYA_TITLE_4))
            linkCheck(s"$changeLinkExpected $dividendsOtherStatusHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_4), dividendsOtherStatusHref)
          }
          "has an area for section 5" which {
            textOnPageCheck(dividendsOtherAmountText, Selectors.cyaTitle(CYA_TITLE_5))
            linkCheck(s"$changeLinkExpected $dividendsOtherAmountHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_5), dividendsOtherAmountHref)
          }
          "has an area for section 6" which {
            textOnPageCheck(stockDividendsStatusText, Selectors.cyaTitle(CYA_TITLE_6))
            linkCheck(s"$changeLinkExpected $stockDividendsStatusHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_6), stockDividendsStatusHref)
          }
          "has an area for section 7" which {
            textOnPageCheck(stockDividendsAmountText, Selectors.cyaTitle(CYA_TITLE_7))
            linkCheck(s"$changeLinkExpected $stockDividendsAmountHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_7), stockDividendsAmountHref)
          }
          "has an area for section 8" which {
            textOnPageCheck(redeemableSharesStatusText, Selectors.cyaTitle(CYA_TITLE_8))
            linkCheck(s"$changeLinkExpected $redeemableSharesStatusHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_8), redeemableSharesStatusHref)
          }
          "has an area for section 9" which {
            textOnPageCheck(redeemableSharesAmountText, Selectors.cyaTitle(CYA_TITLE_9))
            linkCheck(s"$changeLinkExpected $redeemableSharesAmountHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_9), redeemableSharesAmountHref)
          }
          "has an area for section 10" which {
            textOnPageCheck(closeCompanyLoanStatusText, Selectors.cyaTitle(CYA_TITLE_10))
            linkCheck(s"$changeLinkExpected $closeCompanyLoanStatusHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_10), closeCompanyLoanStatusHref)
          }
          "has an area for section 11" which {
            textOnPageCheck(closeCompanyLoanAmountText, Selectors.cyaTitle(CYA_TITLE_11))
            linkCheck(s"$changeLinkExpected $closeCompanyLoanAmountHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_11), closeCompanyLoanAmountHref)
          }

          buttonCheck(continueButtonText, continueButtonSelector)
          welshToggleCheck(us.isWelsh)

        }

        "render Dividends summary page with correct content when there is data with false values in session" which {

          lazy val headers = playSessionCookie(us.isAgent) ++ (if (us.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
          lazy val request = FakeRequest("GET", relativeUrl).withHeaders(headers: _*)

          lazy val result = {
            authoriseAgentOrIndividual(us.isAgent)
            dropStockDividendsDB()
            emptyUserDataStub()
            emptyStockDividendsUserDataStub()
            insertStockDividendsCyaData(Some(cyaModel.copy(
              gateway = Some(true),
              ukDividends = Some(false),
              None,
              otherUkDividends = Some(false),
              None,
              stockDividends = Some(false),
              None,
              redeemableShares = Some(false),
              None,
              closeCompanyLoansWrittenOff = Some(false),
              None
            )))
            route(appWithStockDividends, request, "{}").get
          }

          "has an OK(200) status" in {
            status(result) shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

          h1Check(get.headingExpected + " " + captionExpected)
          textOnPageCheck(captionExpected, Selectors.captionSelector)
          "has an area for section 1" which {
            textOnPageCheck(dividendsFromStocksAndSharesText, Selectors.cyaTitle(CYA_TITLE_1))
            linkCheck(s"$changeLinkExpected $dividendsFromStocksAndSharesHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_1), dividendsFromStocksAndSharesHref)
          }
          "has an area for section 2" which {
            textOnPageCheck(dividendsStatusText, Selectors.cyaTitle(CYA_TITLE_2))
            linkCheck(s"$changeLinkExpected $dividendsStatusHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_2), dividendsStatusHref)
          }
          "has an area for section 4" which {
            textOnPageCheck(dividendsOtherStatusText, Selectors.cyaTitle(CYA_TITLE_3))
            linkCheck(s"$changeLinkExpected $dividendsOtherStatusHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_3), dividendsOtherStatusHref)
          }
          "has an area for section 6" which {
            textOnPageCheck(stockDividendsStatusText, Selectors.cyaTitle(CYA_TITLE_4))
            linkCheck(s"$changeLinkExpected $stockDividendsStatusHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_4), stockDividendsStatusHref)
          }
          "has an area for section 8" which {
            textOnPageCheck(redeemableSharesStatusText, Selectors.cyaTitle(CYA_TITLE_5))
            linkCheck(s"$changeLinkExpected $redeemableSharesStatusHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_5), redeemableSharesStatusHref)
          }
          "has an area for section 10" which {
            textOnPageCheck(closeCompanyLoanStatusText, Selectors.cyaTitle(CYA_TITLE_6))
            linkCheck(s"$changeLinkExpected $closeCompanyLoanStatusHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_6), closeCompanyLoanStatusHref)
          }

          buttonCheck(continueButtonText, continueButtonSelector)
          welshToggleCheck(us.isWelsh)

        }

        "renders Dividends summary page with correct content from prior data" which {

          lazy val headers = playSessionCookie(us.isAgent) ++ (if (us.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
          lazy val request = FakeRequest("GET", relativeUrl).withHeaders(headers: _*)

          lazy val result = {
            authoriseAgentOrIndividual(us.isAgent)
            dropStockDividendsDB()
            userDataStub(IncomeSourcesModel(Some(DividendsPriorSubmission(
              ukDividends = Some(amount), otherUkDividends = Some(amount)
            ))), nino, taxYear)
            stockDividendsUserDataStub(Some(StockDividendsPriorSubmission(
              submittedOn = Some(""),
              foreignDividend = None,
              dividendIncomeReceivedWhilstAbroad = None,
              stockDividend = stockDividend,
              redeemableShares = stockDividend,
              bonusIssuesOfSecurities = None,
              closeCompanyLoansWrittenOff = stockDividend
            )), nino, taxYear)
            route(appWithStockDividends, request, "{}").get
          }

          "has an OK(200) status" in {
            status(result) shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

          h1Check(get.headingExpected + " " + captionExpected)
          textOnPageCheck(captionExpected, Selectors.captionSelector)
          "has an area for section 1" which {
            textOnPageCheck(dividendsFromStocksAndSharesText, Selectors.cyaTitle(CYA_TITLE_1))
            linkCheck(s"$changeLinkExpected $dividendsFromStocksAndSharesHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_1), dividendsFromStocksAndSharesHref)
          }
          "has an area for section 2" which {
            textOnPageCheck(dividendsStatusText, Selectors.cyaTitle(CYA_TITLE_2))
            linkCheck(s"$changeLinkExpected $dividendsStatusHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_2), dividendsStatusHref)
          }
          "has an area for section 3" which {
            textOnPageCheck(dividendsAmountText, Selectors.cyaTitle(CYA_TITLE_3))
            linkCheck(s"$changeLinkExpected $dividendsAmountHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_3), dividendsAmountHref)
          }
          "has an area for section 4" which {
            textOnPageCheck(dividendsOtherStatusText, Selectors.cyaTitle(CYA_TITLE_4))
            linkCheck(s"$changeLinkExpected $dividendsOtherStatusHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_4), dividendsOtherStatusHref)
          }
          "has an area for section 5" which {
            textOnPageCheck(dividendsOtherAmountText, Selectors.cyaTitle(CYA_TITLE_5))
            linkCheck(s"$changeLinkExpected $dividendsOtherAmountHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_5), dividendsOtherAmountHref)
          }
          "has an area for section 6" which {
            textOnPageCheck(stockDividendsStatusText, Selectors.cyaTitle(CYA_TITLE_6))
            linkCheck(s"$changeLinkExpected $stockDividendsStatusHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_6), stockDividendsStatusHref)
          }
          "has an area for section 7" which {
            textOnPageCheck(stockDividendsAmountText, Selectors.cyaTitle(CYA_TITLE_7))
            linkCheck(s"$changeLinkExpected $stockDividendsAmountHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_7), stockDividendsAmountHref)
          }
          "has an area for section 8" which {
            textOnPageCheck(redeemableSharesStatusText, Selectors.cyaTitle(CYA_TITLE_8))
            linkCheck(s"$changeLinkExpected $redeemableSharesStatusHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_8), redeemableSharesStatusHref)
          }
          "has an area for section 9" which {
            textOnPageCheck(redeemableSharesAmountText, Selectors.cyaTitle(CYA_TITLE_9))
            linkCheck(s"$changeLinkExpected $redeemableSharesAmountHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_9), redeemableSharesAmountHref)
          }
          "has an area for section 10" which {
            textOnPageCheck(closeCompanyLoanStatusText, Selectors.cyaTitle(CYA_TITLE_10))
            linkCheck(s"$changeLinkExpected $closeCompanyLoanStatusHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_10), closeCompanyLoanStatusHref)
          }
          "has an area for section 11" which {
            textOnPageCheck(closeCompanyLoanAmountText, Selectors.cyaTitle(CYA_TITLE_11))
            linkCheck(s"$changeLinkExpected $closeCompanyLoanAmountHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_11), closeCompanyLoanAmountHref)
          }

          buttonCheck(continueButtonText, continueButtonSelector)
          welshToggleCheck(us.isWelsh)

        }

        "renders Dividends summary page with correct content when dividends prior data exists and no stock dividends prior data" which {

          lazy val headers = playSessionCookie(us.isAgent) ++ (if (us.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
          lazy val request = FakeRequest("GET", relativeUrl).withHeaders(headers: _*)

          lazy val result = {
            authoriseAgentOrIndividual(us.isAgent)
            dropDividendsDB()
            emptyUserDataStub()
            dropStockDividendsDB()
            emptyStockDividendsUserDataStub()
            userDataStub(IncomeSourcesModel(Some(DividendsPriorSubmission(
              ukDividends = Some(amount), otherUkDividends = Some(amount)
            ))), nino, taxYear)
            route(appWithStockDividends, request, "{}").get
          }

          "has an OK(200) status" in {
            status(result) shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

          h1Check(get.headingExpected + " " + captionExpected)
          textOnPageCheck(captionExpected, Selectors.captionSelector)
          "has an area for section 1" which {
            textOnPageCheck(dividendsFromStocksAndSharesText, Selectors.cyaTitle(CYA_TITLE_1))
            linkCheck(s"$changeLinkExpected $dividendsFromStocksAndSharesHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_1), dividendsFromStocksAndSharesHref)
          }
          "has an area for section 2" which {
            textOnPageCheck(dividendsStatusText, Selectors.cyaTitle(CYA_TITLE_2))
            linkCheck(s"$changeLinkExpected $dividendsStatusHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_2), dividendsStatusHref)
          }
          "has an area for section 3" which {
            textOnPageCheck(dividendsAmountText, Selectors.cyaTitle(CYA_TITLE_3))
            linkCheck(s"$changeLinkExpected $dividendsAmountHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_3), dividendsAmountHref)
          }
          "has an area for section 4" which {
            textOnPageCheck(dividendsOtherStatusText, Selectors.cyaTitle(CYA_TITLE_4))
            linkCheck(s"$changeLinkExpected $dividendsOtherStatusHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_4), dividendsOtherStatusHref)
          }
          "has an area for section 5" which {
            textOnPageCheck(dividendsOtherAmountText, Selectors.cyaTitle(CYA_TITLE_5))
            linkCheck(s"$changeLinkExpected $dividendsOtherAmountHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_5), dividendsOtherAmountHref)
          }
        }

        "renders internal server error page when error in getting dividends prior data" which {

          lazy val headers = playSessionCookie(us.isAgent) ++ (if (us.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
          lazy val request = FakeRequest("GET", relativeUrl).withHeaders(headers: _*)

          lazy val result = {
            authoriseAgentOrIndividual(us.isAgent)
            dropDividendsDB()
            emptyUserDataStub()
            dropStockDividendsDB()
            emptyStockDividendsUserDataStub()
            userDataStubWithError(nino, taxYear)
            route(appWithStockDividends, request, "{}").get
          }

          "has an Internal server error(500) status" in {
            status(result) shouldBe INTERNAL_SERVER_ERROR
          }

        }

        "renders Dividends summary page with correct content when only stock dividends prior data exists and no dividends data" which {

          lazy val headers = playSessionCookie(us.isAgent) ++ (if (us.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
          lazy val request = FakeRequest("GET", relativeUrl).withHeaders(headers: _*)

          lazy val result = {
            authoriseAgentOrIndividual(us.isAgent)
            dropDividendsDB()
            emptyUserDataStub()
            dropStockDividendsDB()
            emptyStockDividendsUserDataStub()
            stockDividendsUserDataStub(Some(StockDividendsPriorSubmission(
              submittedOn = Some(""),
              foreignDividend = None,
              dividendIncomeReceivedWhilstAbroad = None,
              stockDividend = stockDividend,
              redeemableShares = stockDividend,
              bonusIssuesOfSecurities = None,
              closeCompanyLoansWrittenOff = stockDividend
            )), nino, taxYear)
            route(appWithStockDividends, request, "{}").get
          }

          "has an OK(200) status" in {
            status(result) shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

          h1Check(get.headingExpected + " " + captionExpected)
          textOnPageCheck(captionExpected, Selectors.captionSelector)
        }

        "renders Dividends summary page with correct content when gateway is false" which {

          lazy val headers = playSessionCookie(us.isAgent) ++ (if (us.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
          lazy val request = FakeRequest("GET", relativeUrl).withHeaders(headers: _*)

          lazy val result = {
            authoriseAgentOrIndividual(us.isAgent)
            dropStockDividendsDB()
            insertStockDividendsCyaData(Some(cyaModel.copy(
              gateway = Some(false),
              None, None, None, None, None, None, None, None, None, None
            )))
            emptyUserDataStub()
            emptyStockDividendsUserDataStub()
            route(appWithStockDividends, request, "{}").get
          }

          "has an OK(200) status" in {
            status(result) shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

          h1Check(get.headingExpected + " " + captionExpected)
          textOnPageCheck(captionExpected, Selectors.captionSelector)
          "has an area for section 1" which {
            textOnPageCheck(dividendsFromStocksAndSharesText, Selectors.cyaTitle(CYA_TITLE_1))
            linkCheck(s"$changeLinkExpected $dividendsFromStocksAndSharesHiddenText",
              Selectors.cyaChangeLink(CYA_TITLE_1), dividendsFromStocksAndSharesHref)
          }

          buttonCheck(continueButtonText, continueButtonSelector)
          welshToggleCheck(us.isWelsh)

        }

        "redirect to close company loan amount when unfinished" which {

          lazy val headers = playSessionCookie(us.isAgent) ++ (if (us.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
          lazy val request = FakeRequest("GET", relativeUrl).withHeaders(headers: _*)

          lazy val result = {
            authoriseAgentOrIndividual(us.isAgent)
            dropStockDividendsDB()
            emptyUserDataStub()
            emptyStockDividendsUserDataStub()
            insertStockDividendsCyaData(Some(cyaModel.copy(closeCompanyLoansWrittenOff = Some(true), closeCompanyLoansWrittenOffAmount = None)))
            route(appWithStockDividends, request, "{}").get
          }

          "has an SEE_OTHER(303) status" in {
            status(result) shouldBe SEE_OTHER
          }
        }

        "redirect to redeemable shares amount when unfinished" which {

          lazy val headers = playSessionCookie(us.isAgent) ++ (if (us.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
          lazy val request = FakeRequest("GET", relativeUrl).withHeaders(headers: _*)

          lazy val result = {
            authoriseAgentOrIndividual(us.isAgent)
            dropStockDividendsDB()
            emptyUserDataStub()
            emptyStockDividendsUserDataStub()
            insertStockDividendsCyaData(Some(StockDividendsCheckYourAnswersModel(
              redeemableShares = Some(true), redeemableSharesAmount = None,
              closeCompanyLoansWrittenOff = None, closeCompanyLoansWrittenOffAmount = None)))
            route(appWithStockDividends, request, "{}").get
          }

          "has an SEE_OTHER(303) status" in {
            status(result) shouldBe SEE_OTHER
          }
        }

        "redirect to stock dividends amount when unfinished" which {

          lazy val headers = playSessionCookie(us.isAgent) ++ (if (us.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
          lazy val request = FakeRequest("GET", relativeUrl).withHeaders(headers: _*)

          lazy val result = {
            authoriseAgentOrIndividual(us.isAgent)
            dropStockDividendsDB()
            emptyUserDataStub()
            emptyStockDividendsUserDataStub()
            insertStockDividendsCyaData(Some(StockDividendsCheckYourAnswersModel(
              stockDividends = Some(true))))
            route(appWithStockDividends, request, "{}").get
          }

          "has an SEE_OTHER(303) status" in {
            status(result) shouldBe SEE_OTHER
          }
        }

        "redirect to uk dividends amount when unfinished" which {

          lazy val headers = playSessionCookie(us.isAgent) ++ (if (us.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
          lazy val request = FakeRequest("GET", relativeUrl).withHeaders(headers: _*)

          lazy val result = {
            authoriseAgentOrIndividual(us.isAgent)
            dropStockDividendsDB()
            emptyUserDataStub()
            emptyStockDividendsUserDataStub()
            insertStockDividendsCyaData(Some(cyaModel.copy(ukDividends = Some(true), ukDividendsAmount = None)))
            route(appWithStockDividends, request, "{}").get
          }

          "has an SEE_OTHER(303) status" in {
            status(result) shouldBe SEE_OTHER
          }
        }

        "redirect to other uk dividends amount when unfinished" which {

          lazy val headers = playSessionCookie(us.isAgent) ++ (if (us.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
          lazy val request = FakeRequest("GET", relativeUrl).withHeaders(headers: _*)

          lazy val result = {
            authoriseAgentOrIndividual(us.isAgent)
            dropStockDividendsDB()
            emptyUserDataStub()
            emptyStockDividendsUserDataStub()
            insertStockDividendsCyaData(Some(cyaModel.copy(otherUkDividends = Some(true), otherUkDividendsAmount = None)))
            route(appWithStockDividends, request, "{}").get
          }

          "has an SEE_OTHER(303) status" in {
            status(result) shouldBe SEE_OTHER
          }
        }

        "redirect to the overview page" when {
          "there is no session data or prior data" in {

            val result: WSResponse = {
              authoriseIndividual()
              dropDividendsDB()
              dropStockDividendsDB()
              emptyUserDataStub()
              emptyStockDividendsUserDataStub()
              stubGet(s"/update-and-submit-income-tax-return/$taxYear/view", SEE_OTHER, "overview")
              urlGet(dividendsSummaryUrl, follow = false, headers = playSessionCookie())
            }

            result.status shouldBe SEE_OTHER
          }

        }

      }
    }
  }

  ".submit" should {

    s"redirect to the overview page when there is valid session data " when {

      lazy val result: WSResponse = {
        authoriseIndividual()
        dropStockDividendsDB()
        emptyUserDataStub()
        emptyStockDividendsUserDataStub()
        insertStockDividendsCyaData(Some(cyaModel))
        stubPut(s"/income-tax-dividends/income-tax/nino/AA123456A/sources\\?taxYear=$taxYear", NO_CONTENT, "")
        stubPut(s"/income-tax-dividends/income-tax/income/dividends/$nino/$taxYear", NO_CONTENT, "")
        urlPost(dividendsSummaryUrl, follow = false, headers = playSessionCookie(), body = "")
      }
      s"has a status of 303" in {
        result.status shouldBe SEE_OTHER
      }

      "has the correct title" in {
        result.headers("Location").head shouldBe
          s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view"
      }
    }

    s"redirect to the overview page" when {

      "tailoring is on, and the gateway question is false" which {
        lazy val result = {
          dropDividendsDB()
          dropStockDividendsDB()
          emptyUserDataStub()
          emptyStockDividendsUserDataStub()
          insertStockDividendsCyaData(Some(StockDividendsCheckYourAnswersModel(gateway=Some(false))), taxYear, Some(mtditid), None)
          authoriseIndividual()
          stubGet(s"/update-and-submit-income-tax-return/$taxYear/view", OK, "")
          stubPost(s"/income-tax-submission-service/income-tax/nino/$nino/sources/exclude-journey/$taxYear", NO_CONTENT, "{}")
          stubPut(s"/income-tax-dividends/income-tax/income/dividends/$nino/$taxYear", NO_CONTENT, "")
          val request = FakeRequest("POST", s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/summary",
            Headers.apply(playSessionCookie() :+ ("Csrf-Token" -> "nocheck"): _*), "{}")

          await(route(appWithTailoring, request, "{}").get)
        }

        "has a status of SEE_OTHER(303)" in {
          result.header.status shouldBe SEE_OTHER
        }

        "has the redirect location of the overview page" in {
          result.header.headers("Location") shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
        }
      }

    }

    "redirect the user to the zeroing warning page" when {

      "gateway is true and remaining questions are false or zero" which {
        lazy val result = {
          authoriseIndividual()
          dropStockDividendsDB()
          emptyUserDataStub()
          emptyStockDividendsUserDataStub()
          insertStockDividendsCyaData(Some(StockDividendsCheckYourAnswersModel(gateway = Some(true))), taxYear, Some(mtditid), None)

          val request = FakeRequest("POST", s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/summary",
            Headers.apply(playSessionCookie() :+ ("Csrf-Token" -> "nocheck"): _*), "{}")

          await(route(appWithStockDividends, request, "{}").get)
        }

        "has a status of SEE_OTHER(303)" in {
          result.header.status shouldBe SEE_OTHER
        }

        "has the correct redirect location" in {
          result.header.headers("Location") shouldBe controllers.routes.ZeroingWarningController.show(taxYear, STOCK_DIVIDENDS.stringify).url
        }
      }
    }

    s"supply empty model if no data found" when {

      lazy val result: WSResponse = {
        authoriseIndividual()
        dropStockDividendsDB()
        emptyUserDataStub()
        emptyStockDividendsUserDataStub()
        stubPut(s"/income-tax-dividends/income-tax/nino/AA123456A/sources\\?taxYear=$taxYear", NO_CONTENT, "")
        stubPut(s"/income-tax-dividends/income-tax/income/dividends/$nino/$taxYear", NO_CONTENT, "")
        urlPost(dividendsSummaryUrl, follow = false, headers = playSessionCookie(), body = "")
      }
      s"has a status of 500" in {
        result.status shouldBe INTERNAL_SERVER_ERROR
      }

      /*"has the correct title" in {
        result.headers("Location").head shouldBe
          s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view"
      }*/
    }

    s"return an internal server error" when {

      "the tailoring feature switch is on, but the exclude journey call fails" which {
        lazy val result = {
          dropDividendsDB()
          dropStockDividendsDB()
          emptyUserDataStub()
          insertStockDividendsCyaData(Some(cyaModel.copy(gateway = Some(false))))
          authoriseIndividual()
          stubPost(s"/income-tax-submission-service/income-tax/nino/$nino/sources/exclude-journey/$taxYear", INTERNAL_SERVER_ERROR,
            Json.stringify(Json.obj("code" -> "failed", "reason" -> "I made it fail"))
          )
          val request = FakeRequest("POST", s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/summary",
            Headers.apply(playSessionCookie() :+ ("Csrf-Token" -> "nocheck"): _*), "{}")

          await(route(appWithTailoring, request, "{}").get)
        }

        "has a status of 500" in {
          result.header.status shouldBe INTERNAL_SERVER_ERROR
        }
      }
    }
  }
}

