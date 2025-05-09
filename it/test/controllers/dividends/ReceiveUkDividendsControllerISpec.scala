/*
 * Copyright 2024 HM Revenue & Customs
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
import forms.YesNoForm
import models.dividends._
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.{DefaultBodyWritables, WSResponse}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.route
import test.utils.{DividendsDatabaseHelper, IntegrationTest, ViewHelpers}

import scala.concurrent.Future

class ReceiveUkDividendsControllerISpec extends IntegrationTest with ViewHelpers with DividendsDatabaseHelper with DefaultBodyWritables {

  val amount: BigDecimal = 500
  val receiveUkDividendsUrl = s"$appUrl/$taxYear/dividends/dividends-from-uk-companies"
  val expectedErrorLink = "#value"

  val cyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount),
    otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount))

  val stockCyaModel: StockDividendsCheckYourAnswersModel =
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

  object Selectors {
    val continueSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedErrorText: String
    val expectedHintText: String
  }

  trait CommonExpectedResults {
    val captionExpected: String
    val yesNo: Boolean => String
    val continueText: String
    val continueLink: String
    val errorSummaryHref: String
  }


  object IndividualExpectedEnglish extends SpecificExpectedResults {
    val expectedH1 = "Did you get dividends from UK-based companies?"
    val expectedTitle = "Did you get dividends from UK-based companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedHintText = "Your dividend voucher will tell you the shares you have in the company and the amount of the dividend you got."
    val expectedErrorText = "Select yes if you got dividends from UK-based companies"
  }

  object AgentExpectedEnglish extends SpecificExpectedResults {
    val expectedH1 = "Did your client get dividends from UK-based companies?"
    val expectedTitle = "Did your client get dividends from UK-based companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedHintText = "Your client’s dividend voucher will tell you the shares they have in the company and the amount of the dividend they got."
    val expectedErrorText = "Select yes if your client got dividends from UK-based companies"
  }

  object AllExpectedEnglish extends CommonExpectedResults {
    val captionExpected = s"Dividends for 6 April $taxYearEOY to 5 April $taxYear"
    val yesNo: Boolean => String = isYes => if (isYes) "Yes" else "No"
    val continueText = "Continue"
    val continueLink = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/dividends-from-uk-companies"
    val errorSummaryHref = "#value"
  }

  object IndividualExpectedWelsh extends SpecificExpectedResults {
    val expectedH1 = "A gawsoch ddifidendau gan gwmnïau yn y DU?"
    val expectedTitle = "A gawsoch ddifidendau gan gwmnïau yn y DU?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedHintText = "Bydd eich taleb ddifidend yn rhoi gwybod i chi am y cyfranddaliadau sydd gennych yn y cwmni a swm y difidend a gawsoch."
    val expectedErrorText = "Dewiswch ‘Iawn’ os cawsoch ddifidendau gan gwmnïau yn y DU"
  }

  object AgentExpectedWelsh extends SpecificExpectedResults {
    val expectedH1 = "A gafodd eich cleient ddifidendau gan gwmnïau yn y DU?"
    val expectedTitle = "A gafodd eich cleient ddifidendau gan gwmnïau yn y DU?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedHintText = "Bydd taleb ddifidend eich cleient yn rhoi gwybod i chi am y cyfranddaliadau sydd ganddynt yn y cwmni a swm y difidend a gafodd."
    val expectedErrorText = "Dewiswch ‘Iawn’ os cafodd eich cleient ddifidendau gan gwmnïau yn y DU"
  }

  object AllExpectedWelsh extends CommonExpectedResults {
    val captionExpected = s"Difidendau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val yesNo: Boolean => String = isYes => if (isYes) "Iawn" else "Na"
    val continueText = "Yn eich blaen"
    val continueLink = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/dividends-from-uk-companies"
    val errorSummaryHref = "#value"
  }

  val userScenarios =
    Seq(UserScenario(isWelsh = false, isAgent = false, AllExpectedEnglish, Some(IndividualExpectedEnglish)),
      UserScenario(isWelsh = false, isAgent = true, AllExpectedEnglish, Some(AgentExpectedEnglish)),
      UserScenario(isWelsh = true, isAgent = false, AllExpectedWelsh, Some(IndividualExpectedWelsh)),
      UserScenario(isWelsh = true, isAgent = true, AllExpectedWelsh, Some(AgentExpectedWelsh)))

  ".show" when {
    userScenarios.foreach { scenario =>

      import Selectors._
      import scenario.commonExpectedResults._
      import scenario.specificExpectedResults._

      s"language is ${welshTest(scenario.isWelsh)} and request is from an ${agentTest(scenario.isAgent)}" should {

        "return the uk dividends page when there is no priorSubmission data and no cyaData in session" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(scenario.isAgent)
            dropDividendsDB()
            emptyUserDataStub()
            urlGet(receiveUkDividendsUrl, scenario.isWelsh, headers = playSessionCookie(scenario.isAgent))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(get.expectedTitle, scenario.isWelsh)
          legendHeadingCheck(s"${get.expectedH1} ${captionExpected}")
          hintTextCheck(get.expectedHintText)
          radioButtonCheck(yesNo(true), 1)
          radioButtonCheck(yesNo(false), 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)

          welshToggleCheck(scenario.isWelsh)
        }

        "return the uk dividends page when there is cyaData in session" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(scenario.isAgent)
            dropDividendsDB()
            emptyUserDataStub()
            insertDividendsCyaData(Some(cyaModel))
            urlGet(receiveUkDividendsUrl, scenario.isWelsh, headers = playSessionCookie(scenario.isAgent))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(get.expectedTitle, scenario.isWelsh)
          legendHeadingCheck(s"${get.expectedH1} ${captionExpected}")
          hintTextCheck(get.expectedHintText)
          radioButtonCheck(yesNo(true), 1)
          radioButtonCheck(yesNo(false), 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)

          welshToggleCheck(scenario.isWelsh)
        }
      }
    }
  }

  ".show" should {


    "redirect to Dividends CYA page when there is prior data in otherUkDividends" when {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        userDataStub(IncomeSourcesModel(
          dividends = Some(DividendsPriorSubmission(
            Some(amount),
            Some(amount)
          ))), nino, taxYear)
        urlGet(receiveUkDividendsUrl, follow = false, headers = playSessionCookie())
      }
      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION) shouldBe Some(routes.DividendsCYAController.show(taxYear).url)
      }
    }

    "returns an action when auth call fails" which {
      lazy val result: WSResponse = {
        authoriseIndividualUnauthorized()
        dropDividendsDB()
        emptyUserDataStub()
        urlGet(receiveUkDividendsUrl, headers = playSessionCookie())
      }
      "has an UNAUTHORIZED(401) status" in {
        result.status shouldBe UNAUTHORIZED
      }
    }

    "display the stock dividend uk dividends status page" which {
      lazy val headers = playSessionCookie(userScenarios.head.isAgent) ++ (if (userScenarios.head.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
      lazy val request =
        FakeRequest("GET", s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/dividends-from-uk-companies").withHeaders(headers: _*)

      lazy val result: Future[Result] = {
        authoriseAgentOrIndividual(userScenarios.head.isAgent)
        dropStockDividendsDB()
        emptyUserDataStub()
        emptyStockDividendsUserDataStub()
        route(appWithStockDividends, request, "{}").get
      }

      "has status of OK(200)" in {
        status(result) shouldBe OK
      }
    }

    "display the stock dividend uk dividends status page with session data" which {
      lazy val headers = playSessionCookie(userScenarios.head.isAgent) ++ (if (userScenarios.head.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
      lazy val request =
        FakeRequest("GET", s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/dividends-from-uk-companies").withHeaders(headers: _*)

      lazy val result: Future[Result] = {
        authoriseAgentOrIndividual(userScenarios.head.isAgent)
        emptyUserDataStub()
        emptyStockDividendsUserDataStub()
        dropStockDividendsDB()
        insertStockDividendsCyaData(Some(stockCyaModel))
        route(appWithStockDividends, request, "{}").get
      }

      "has a status of OK(200)" in {
        status(result) shouldBe OK
      }
    }
  }

  ".submit" when {
    userScenarios.foreach { us =>

      import Selectors._
      import us.commonExpectedResults._
      import us.specificExpectedResults._

      s"language is ${welshTest(us.isWelsh)} and request is from an ${agentTest(us.isAgent)}" should {

        s"if form has errors" should {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            dropDividendsDB()
            emptyUserDataStub()
            urlPost(receiveUkDividendsUrl, welsh = us.isWelsh, follow = false, headers = playSessionCookie(us.isAgent), body = Map[String, String]())
          }

          s"has a $BAD_REQUEST(400) status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)
          errorSummaryCheck(get.expectedErrorText, expectedErrorLink, us.isWelsh)
          errorAboveElementCheck(get.expectedErrorText)
        }


        "return same page with error text when form is invalid" which {

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            dropDividendsDB()
            emptyUserDataStub()
            urlPost(receiveUkDividendsUrl, welsh = us.isWelsh, follow = false, headers = playSessionCookie(us.isAgent), body = Map[String, String]())
          }

          "return a BadRequest status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(get.expectedErrorTitle, us.isWelsh)
          legendHeadingCheck(s"${get.expectedH1} ${captionExpected}")
          errorSummaryCheck(get.expectedErrorText, errorSummaryHref, us.isWelsh)
          hintTextCheck(get.expectedHintText)
          radioButtonCheck(yesNo(true), 1)
          radioButtonCheck(yesNo(false), 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)

          welshToggleCheck(us.isWelsh)
        }
      }

    }
  }

  ".submit" should {

    s"redirect to uk dividends amount page when 'yes' is submitted and there is no cya data. Creates new cya data model in mongo. " when {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        urlPost(receiveUkDividendsUrl, follow = false, headers = playSessionCookie(), body = Map(YesNoForm.yesNo -> YesNoForm.yes))
      }

      s"has a $SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION) shouldBe Some(controllers.dividendsBase.routes.UkDividendsAmountBaseController.show(taxYear).url)
      }
    }

    s"redirect to uk dividends amount page when 'yes' is submitted and there is already cya data. Updates cya data model in mongo. " when {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        insertDividendsCyaData(Some(cyaModel))
        urlPost(receiveUkDividendsUrl, follow = false, headers = playSessionCookie(), body = Map(YesNoForm.yesNo -> YesNoForm.yes))
      }

      s"has a $SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION) shouldBe Some(controllers.dividendsBase.routes.UkDividendsAmountBaseController.show(taxYear).url)
      }
    }

    "redirect to Did you receive other Dividends page when 'no' is submitted and there is no cya data. Creates new record in Mongo." when {

      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        urlPost(receiveUkDividendsUrl, follow = false, headers = playSessionCookie(), body = Map(YesNoForm.yesNo -> YesNoForm.no))
      }

      s"has a $SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION) shouldBe Some(routes.ReceiveOtherUkDividendsController.show(taxYear).url)
      }
    }

    "redirect to Did you receive other Dividends page when 'no' is submitted and cya model isn't complete. Updates Mongo." when {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        insertDividendsCyaData(Some(DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount))))
        urlPost(receiveUkDividendsUrl, follow = false, headers = playSessionCookie(), body = Map(YesNoForm.yesNo -> YesNoForm.no))
      }

      s"has a $SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION) shouldBe Some(routes.ReceiveOtherUkDividendsController.show(taxYear).url)
      }
    }

    "redirect to DividendsCYA page when 'no' is submitted and cya model is complete. Updates Mongo." when {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        insertDividendsCyaData(Some(cyaModel))
        urlPost(receiveUkDividendsUrl, follow = false, headers = playSessionCookie(), body = Map(YesNoForm.yesNo -> YesNoForm.no))
      }

      s"has a $SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION) shouldBe Some(routes.DividendsCYAController.show(taxYear).url)
      }
    }
    "return an action when auth call fails" which {
      lazy val result: WSResponse = {
        authoriseIndividualUnauthorized()
        dropDividendsDB()
        emptyUserDataStub()
        urlPost(receiveUkDividendsUrl, headers = playSessionCookie(), body = Map(YesNoForm.yesNo -> YesNoForm.no))
      }
      "has an UNAUTHORIZED(401) status" in {
        result.status shouldBe UNAUTHORIZED
      }
    }

    "return a 303 status and redirect to amount page when true selected" in {
      lazy val headers =
        playSessionCookie(userScenarios.head.isAgent) ++
          (if (userScenarios.head.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq()) ++
          Seq("Csrf-Token" -> "nocheck")
      lazy val request =
        FakeRequest("POST", s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/dividends-from-uk-companies").withHeaders(headers: _*)

      lazy val result = {
        dropStockDividendsDB()
        authoriseAgentOrIndividual(userScenarios.head.isAgent)
        emptyStockDividendsUserDataStub()
        route(appWithStockDividends, request, body = Map("value" -> Seq("true"))).get
      }

      status(result) shouldBe SEE_OTHER
      await(result).header.headers
        .get(HeaderNames.LOCATION) shouldBe Some(s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/how-much-dividends-from-uk-companies")
    }

    "return a 303 status and redirect to next status page when false selected" in {
      lazy val headers =
        playSessionCookie(userScenarios.head.isAgent) ++
          (if (userScenarios.head.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq()) ++
          Seq("Csrf-Token" -> "nocheck")
      lazy val request =
        FakeRequest("POST", s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/dividends-from-uk-companies").withHeaders(headers: _*)

      lazy val result = {
        dropStockDividendsDB()
        authoriseAgentOrIndividual(userScenarios.head.isAgent)
        emptyStockDividendsUserDataStub()
        route(appWithStockDividends, request, body = Map("value" -> Seq("false"))).get
      }

      status(result) shouldBe SEE_OTHER
      await(result).header.headers
        .get(HeaderNames.LOCATION) shouldBe Some(s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/dividends-from-uk-trusts-or-open-ended-investment-companies")
    }

    "return a 303 status and redirect to cya page when isFinished is true" in {
      lazy val headers =
        playSessionCookie(userScenarios.head.isAgent) ++
          (if (userScenarios.head.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq()) ++
          Seq("Csrf-Token" -> "nocheck")
      lazy val request =
        FakeRequest("POST", s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/dividends-from-uk-companies").withHeaders(headers: _*)

      lazy val result = {
        authoriseIndividual()
        dropStockDividendsDB()
        emptyStockDividendsUserDataStub()
        insertStockDividendsCyaData(Some(stockCyaModel))
        route(appWithStockDividends, request, body = Map("value" -> Seq("false"))).get
      }
      status(result) shouldBe SEE_OTHER
      await(result).header.headers
        .get(HeaderNames.LOCATION) shouldBe Some(routes.DividendsSummaryController.show(taxYear).url)
    }
  }

}
