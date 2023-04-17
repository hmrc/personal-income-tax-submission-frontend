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

package controllers.dividends

import play.api.http.HeaderNames
import play.api.http.Status.OK
import utils.IntegrationTest
import models.dividends.{DividendsCheckYourAnswersModel, DividendsPriorSubmission}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, route}
import utils.{DividendsDatabaseHelper, ViewHelpers}

class DividendsSummaryControllerISpec extends IntegrationTest with ViewHelpers with DividendsDatabaseHelper {


  val ukDividends: BigDecimal = 10
  val otherDividends: BigDecimal = 10.50

  val relativeUrl: String = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/summary"

  val dividendsSummaryUrl = s"$appUrl/$taxYear/dividends/summary"

  val dividendsFromStocksAndSharesHref = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/dividends-from-stocks-and-shares"
  val dividendsStatusHref = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/dividends-from-uk-companies"
  val dividendsAmountHref = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/how-much-dividends-from-uk-companies"
  val dividendsOtherStatusHref : String = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends" +
    "/dividends-from-uk-trusts-or-open-ended-investment-companies"
  val dividendsOtherAmountHref : String = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends" +
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

  lazy val priorData: IncomeSourcesModel = IncomeSourcesModel(
    dividends = Some(DividendsPriorSubmission(
      Some(ukDividends),
      Some(otherDividends)
    ))
  )

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

    val dividendsFromStocksAndSharesHiddenText = "Change dividends status"
    val dividendsStatusHiddenText = "Change status of dividends from UK companies"
    val dividendsAmountHiddenText = "Change amount of dividends from UK companies"
    val dividendsOtherStatusHiddenText = "Change status of dividends from unit trusts or investment companies"
    val dividendsOtherAmountHiddenText = "Change amount of dividends from unit trusts or investment companies"
    val stockDividendsStatusHiddenText = "Change stock dividends status"
    val stockDividendsAmountHiddenText = "Change value of stock dividends"
    val redeemableSharesStatusHiddenText = "Change free or redeemable shares status"
    val redeemableSharesAmountHiddenText = "Change value of free or redeemable shares"
    val closeCompanyLoanStatusHiddenText = "Change status of close company loan written off or released"
    val closeCompanyLoanAmountHiddenText = "Change value of close company loan written off or released"
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

    val dividendsFromStocksAndSharesHiddenText = "Newid statws difidendau"
    val dividendsStatusHiddenText = "Newid statws difidendau o gwmnïau yn y DU"
    val dividendsAmountHiddenText = "Newid swm y difidendau o gwmnïau yn y DU"
    val dividendsOtherStatusHiddenText = "Newid statws difidendau o ymddiriedolaethau unedol neu gwmnïau buddsoddi"
    val dividendsOtherAmountHiddenText = "Newid swm y difidendau o ymddiriedolaethau unedol neu gwmnïau buddsoddi"
    val stockDividendsStatusHiddenText = "Newid statws y difidendau stoc"
    val stockDividendsAmountHiddenText = "Newid gwerth y difidendau stoc"
    val redeemableSharesStatusHiddenText = "Newid statws difidendau adbryn neu ddifidendau sy’n rhad ac am ddim"
    val redeemableSharesAmountHiddenText = "Newid gwerth y difidendau adbryn neu’r difidendau sy’n rhad ac am ddim"
    val closeCompanyLoanStatusHiddenText = "Newid statws benthyciadau gan gwmnïau caeedig a ddilëwyd neu a ryddhawyd"
    val closeCompanyLoanAmountHiddenText = "Newid gwerth benthyciadau gan gwmnïau caeedig a ddilëwyd neu a ryddhawyd"

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
            dropDividendsDB()
            emptyUserDataStub()
            insertDividendsCyaData(Some(dividendsCyaModel))
            route(appWithTailoring, request, "{}").get
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
      }
    }
  }
}

