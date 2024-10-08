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
import forms.YesNoForm
import models.dividends._
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.route
import test.utils.{DividendsDatabaseHelper, IntegrationTest, ViewHelpers}

import scala.concurrent.Future

class ReceiveOtherUkDividendsControllerISpec extends IntegrationTest with ViewHelpers with DividendsDatabaseHelper {


  val amount: BigDecimal = 500
  val receivedOtherDividendsUrl = s"$appUrl/$taxYear/dividends/dividends-from-uk-trusts-or-open-ended-investment-companies"

  lazy val cyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount),
    otherUkDividends = Some(true), otherUkDividendsAmount = None)

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

  lazy val priorData: IncomeSourcesModel = IncomeSourcesModel(
    dividends = Some(DividendsPriorSubmission(
      ukDividends = None,
      otherUkDividends = Some(amount)
    ))
  )


  object Selectors {
    val youMustAlsoSelector = "#p1"
    def listContentSelector(i: Int): String = s"#main-content > div > div > ul > li:nth-child($i)"

    val youDoNotNeedSelector = "#p2"

    val investmentTitleSelector =  "#main-content > div > div > details:nth-child(2) > summary"
    val investmentTitleErrorSelector =  "#main-content > div > div > details:nth-child(3) > summary"
    val investmentsContentP1Selector = "#p3"
    val investmentsContentP2Selector = "#p4"
    val investmentsContentP3Selector = "#p5"

    val equalisationTitleSelector = "#main-content > div > div > details:nth-child(3) > summary"
    val equalisationTitleErrorSelector = "#main-content > div > div > details:nth-child(4) > summary"
    val equalisationContentSelector = "#p6"

    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#value"

  }

  trait SpecificExpectedResults {
    val radioHeading: String
    val youDoNotNeedText: String
    val expectedErrorText: String
    val redirectTitle: String
    val redirectH1: String
  }

  trait CommonExpectedResults {
    val captionExpected: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val yesNo: Boolean => String
    val continueButtonText: String
    val continueLink: String
    val youMustAlsoText: String
    val authorisedBulletText: String
    val investmentBulletText: String
    val yourDividendsBulletText: String
    val whatAreInvestmentText: String
    val investmentTrustText: String
    val unitTrustsText: String
    val openEndedText: String
    val whatAreEqualisationText: String
    val equalisationPaymentsText: String
  }


  object IndividualExpectedEnglish extends SpecificExpectedResults {
    val radioHeading = "Did you get dividends from UK-based trusts or open-ended investment companies?"
    val youDoNotNeedText = "You do not need to tell us about amounts shown as 'equalisation' on your dividend voucher."
    val expectedErrorText = "Select yes if you got dividends from UK-based trusts or open-ended investment companies"
    val redirectTitle = "Check your income from dividends"
    val redirectH1 = s"Check your income from dividends Dividends for 6 April $taxYearEOY to 5 April $taxYear"
  }

  object AgentExpectedEnglish extends SpecificExpectedResults {
    val radioHeading = "Did your client get dividends from UK-based trusts or open-ended investment companies?"
    val youDoNotNeedText = "You do not need to tell us about amounts shown as 'equalisation' on your client’s dividend voucher."
    val expectedErrorText = "Select yes if your client got dividends from UK-based trusts or open-ended investment companies"
    val redirectTitle = "Check your client’s income from dividends"
    val redirectH1 = s"Check your client’s income from dividends Dividends for 6 April $taxYearEOY to 5 April $taxYear"
  }

  object AllExpectedEnglish extends CommonExpectedResults {
    val captionExpected = s"Dividends for 6 April $taxYearEOY to 5 April $taxYear"
    val expectedTitle = "Dividends from UK-based trusts or open-ended investment companies"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val youMustAlsoText = "You must also tell us about:"
    val authorisedBulletText = "authorised unit trusts"
    val investmentBulletText = "investment trusts"
    val yourDividendsBulletText = "your dividends that were automatically reinvested"
    val whatAreInvestmentText = "What are investment trusts, unit trusts and open-ended investment companies?"
    val investmentTrustText = "Investment trusts make money through buying and selling shares or assets in other companies."
    val unitTrustsText: String = "Unit trusts make money by buying and selling bonds or shares on the stock market. The fund is split " +
      "into units which an investor buys. A fund manager creates and cancels units when investors join and leave the trust."
    val openEndedText = "Open-ended investment companies are like unit trusts but create and cancel shares, rather than units, when investors join or leave."
    val whatAreEqualisationText = "What are equalisation payments?"
    val equalisationPaymentsText: String = "Equalisation payments are given to investors to make sure they’re charged fairly based " +
      "on the performance of the trust. Equalisation payments are not counted as income because they’re a return of part of an investment."
    val yesNo: Boolean => String = isYes => if (isYes) "Yes" else "No"
    val continueButtonText = "Continue"
    val continueLink = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/dividends-from-uk-trusts-or-open-ended-investment-companies"
  }

  object IndividualExpectedWelsh extends SpecificExpectedResults {
    val radioHeading = "A gawsoch ddifidendau gan ymddiriedolaethau yn y DU neu gwmnïau buddsoddi penagored?"
    val expectedTitle = "A gawsoch ddifidendau gan ymddiriedolaethau yn y DU neu gwmnïau buddsoddi penagored?"
    val youDoNotNeedText = "Nid oes angen i chi roi gwybod i ni am symiau a ddangosir fel 'cyfartaliad' ar eich taleb ddifidend."
    val expectedErrorText = "Dewiswch ‘Iawn’ os cawsoch ddifidendau gan ymddiriedolaethau yn y DU neu gwmnïau buddsoddi penagored"
    val redirectTitle = "Gwiriwch eich incwm o ddifidendau"
    val redirectH1 = s"Gwiriwch eich incwm o ddifidendau Difidendau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
  }

  object AgentExpectedWelsh extends SpecificExpectedResults {
    val radioHeading = "A gafodd eich cleient ddifidendau gan ymddiriedolaethau yn y DU neu gwmnïau buddsoddi penagored?"
    val expectedTitle = "A gafodd eich cleient ddifidendau gan ymddiriedolaethau yn y DU neu gwmnïau buddsoddi penagored?"
    val youDoNotNeedText = "Nid oes angen i chi roi gwybod i ni am symiau a ddangosir fel 'cyfartaliad' ar daleb ddifidend eich cleient."
    val expectedErrorText = "Dewiswch ‘Iawn’ os cafodd eich cleient ddifidendau gan ymddiriedolaethau yn y DU neu gwmnïau buddsoddi penagored"
    val redirectTitle = "Gwiriwch incwm eich cleient o ddifidendau"
    val redirectH1 = s"Gwiriwch incwm eich cleient o ddifidendau Difidendau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
  }

  object AllExpectedWelsh extends CommonExpectedResults {
    val captionExpected = s"Difidendau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val expectedTitle = "Difidendau gan ymddiriedolaethau neu gwmnïau buddsoddi penagored yn y DU"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val youMustAlsoText = "Mae’n rhaid i chi hefyd rhoi gwybod i ni am y canlynol:"
    val authorisedBulletText = "ymddiriedolaethau unedol awdurdodedig"
    val investmentBulletText = "ymddiriedolaethau buddsoddi"
    val yourDividendsBulletText = "eich difidendau a gafodd eu hail-fuddsoddi’n awtomatig"
    val whatAreInvestmentText = "Beth yw ymddiriedolaethau buddsoddi, ymddiriedolaethau unedol a chwmnïau buddsoddi penagored?"
    val investmentTrustText = "Mae ymddiriedolaethau buddsoddi yn gwneud arian drwy brynu a gwerthu cyfranddaliadau neu asedau mewn cwmnïau eraill."
    val unitTrustsText: String = "Mae ymddiriedolaethau unedol yn gwneud arian drwy brynu a gwerthu bondiau neu gyfranddaliadau ar y " +
      "farchnad stoc. Mae’r gronfa wedi’i rhannu’n unedau y mae buddsoddwr yn eu prynu. Mae rheolwr cronfa yn creu ac yn canslo unedau pan fydd buddsoddwyr yn ymuno ac yn gadael yr ymddiriedolaeth."
    val openEndedText = "Mae cwmnïau buddsoddi penagored yn debyg i ymddiriedolaethau unedol ond yn creu ac yn canslo cyfranddaliadau, yn hytrach nag unedau, pan fydd buddsoddwyr yn ymuno neu’n gadael."
    val whatAreEqualisationText = "Beth yw taliadau cyfartaliad?"
    val equalisationPaymentsText: String = "Rhoddir taliadau cyfartaliad i fuddsoddwyr i sicrhau y codir taliadau arnynt yn deg ar sail perfformiad yr ymddiriedolaeth. " +
      "Nid yw taliadau cyfartaliad yn cael eu cyfrif fel incwm oherwydd eu bod yn rhan o fuddsoddiad."
    val yesNo: Boolean => String = isYes => if (isYes) "Iawn" else "Na"
    val continueButtonText = "Yn eich blaen"
    val continueLink = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/dividends-from-uk-trusts-or-open-ended-investment-companies"
  }

  val userScenarios =
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

        "render correct content if first question in dividends journey has been answered in session" which {

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            dropDividendsDB()
            emptyUserDataStub()
            insertDividendsCyaData(Some(cyaModel))
            urlGet(receivedOtherDividendsUrl, us.isWelsh, headers = playSessionCookie(us.isAgent))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle, us.isWelsh)
          h1Check(expectedTitle + " " + captionExpected)
          textOnPageCheck(youMustAlsoText, youMustAlsoSelector)
          textOnPageCheck(authorisedBulletText, listContentSelector(1))
          textOnPageCheck(investmentBulletText, listContentSelector(2))
          textOnPageCheck(yourDividendsBulletText, listContentSelector(3))
          textOnPageCheck(get.youDoNotNeedText, youDoNotNeedSelector)
          radioButtonCheck(yesNo(true), 1)
          radioButtonCheck(yesNo(false), 2)
          textOnPageCheck(whatAreInvestmentText, investmentTitleSelector)
          textOnPageCheck(investmentTrustText, investmentsContentP1Selector)
          textOnPageCheck(unitTrustsText, investmentsContentP2Selector)
          textOnPageCheck(openEndedText, investmentsContentP3Selector)
          textOnPageCheck(whatAreEqualisationText, equalisationTitleSelector)
          textOnPageCheck(equalisationPaymentsText, equalisationContentSelector)

          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)

          welshToggleCheck(us.isWelsh)
        }
      }
    }
  }

  ".show" should {

    "redirects user to dividendsCya page when there is prior submission data but no cyaData in session" which {

      lazy val result: WSResponse = {
        authoriseIndividual()
        emptyUserDataStub()
        userDataStub(priorData, nino, taxYear)
        stubGet(s"/update-and-submit-income-tax-return/$taxYear/view", OK, "overview page content")
        urlGet(receivedOtherDividendsUrl, follow = false, headers = playSessionCookie())
      }

      s"has an $SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION) shouldBe
          Some(s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/check-income-from-dividends")
      }
    }

    "redirects user to overview page when there is no cyaData or prior data" which {

      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        stubGet(s"/update-and-submit-income-tax-return/$taxYear/view", SEE_OTHER, "overview page content")
        urlGet(receivedOtherDividendsUrl, follow = false, headers = playSessionCookie())
      }

      "has an OK(200) status" in {
        result.status shouldBe SEE_OTHER
      }
    }

    "returns an action when auth call fails" which {
      lazy val result: WSResponse = {
        authoriseIndividualUnauthorized()
        dropDividendsDB()
        emptyUserDataStub()
        urlGet(receivedOtherDividendsUrl, headers = playSessionCookie())
      }
      "has an UNAUTHORIZED(401) status" in {
        result.status shouldBe UNAUTHORIZED
      }
    }

    "display the stock dividend other uk dividends status page" which {
      lazy val headers = playSessionCookie(userScenarios.head.isAgent) ++ (if (userScenarios.head.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
      lazy val request =
        FakeRequest(
          "GET", s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/dividends-from-uk-trusts-or-open-ended-investment-companies"
        ).withHeaders(headers: _*)

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

    "display the stock dividend other uk dividends status page with session data" which {
      lazy val headers = playSessionCookie(userScenarios.head.isAgent) ++ (if (userScenarios.head.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
      lazy val request =
        FakeRequest("GET", s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/dividends-from-uk-trusts-or-open-ended-investment-companies")
          .withHeaders(headers: _*)

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

    userScenarios.foreach { scenario =>

      import scenario.commonExpectedResults._
      import scenario.specificExpectedResults._

      s"language is ${welshTest(scenario.isWelsh)} and request is from an ${agentTest(scenario.isAgent)}" should {

        "when form is invalid - no radio button clicked. Show page with Error text" should {

          import Selectors._

          "in English" when {

            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(scenario.isAgent)
              urlPost(receivedOtherDividendsUrl, welsh = scenario.isWelsh, headers = playSessionCookie(scenario.isAgent), body = Map[String, String]())
            }

            "has an BAD_REQUEST(400) status" in {
              result.status shouldBe BAD_REQUEST
            }


            implicit val document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(expectedErrorTitle, scenario.isWelsh)
            h1Check(expectedTitle + " " + captionExpected)
            errorSummaryCheck(get.expectedErrorText, expectedErrorHref, scenario.isWelsh)
            textOnPageCheck(youMustAlsoText, youMustAlsoSelector)
            textOnPageCheck(authorisedBulletText, listContentSelector(1))
            textOnPageCheck(investmentBulletText, listContentSelector(2))
            textOnPageCheck(yourDividendsBulletText, listContentSelector(3))
            textOnPageCheck(get.youDoNotNeedText, youDoNotNeedSelector)
            radioButtonCheck(yesNo(true), 1)
            radioButtonCheck(yesNo(false), 2)
            textOnPageCheck(whatAreInvestmentText, investmentTitleErrorSelector)
            textOnPageCheck(investmentTrustText, investmentsContentP1Selector)
            textOnPageCheck(unitTrustsText, investmentsContentP2Selector)
            textOnPageCheck(openEndedText, investmentsContentP3Selector)
            textOnPageCheck(whatAreEqualisationText, equalisationTitleErrorSelector)
            textOnPageCheck(equalisationPaymentsText, equalisationContentSelector)

            welshToggleCheck(scenario.isWelsh)
          }
        }
      }
    }
  }

  ".submit" should {

    "redirects User to overview page if no CYA data is in session" when {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        urlGet(receivedOtherDividendsUrl, follow = false, headers = playSessionCookie())
        urlPost(receivedOtherDividendsUrl, follow = false, headers = playSessionCookie(), body = Map(YesNoForm.yesNo -> YesNoForm.yes))
      }
      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
      }

      "have the correct redirect URL" in {
        result.headers(HeaderNames.LOCATION).head shouldBe s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view"
      }
    }

    "return a Redirect to other uk dividends amount page when form is valid and answer is yes and cya model already has data. Update Mongo" should {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        insertDividendsCyaData(Some(DividendsCheckYourAnswersModel(
          ukDividends = Some(true), ukDividendsAmount = Some(amount)
        )))
        urlPost(receivedOtherDividendsUrl, follow = false, headers = playSessionCookie(), body = Map(YesNoForm.yesNo -> YesNoForm.yes)
        )
      }

      s"return a Redirect to the other uk dividend amount page" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION) shouldBe Some(controllers.dividendsBase.routes.OtherUkDividendsAmountBaseController.show(taxYear).url)
      }
    }

    "return a Redirect to dividend cya page when form is valid and answer is no. update Mongo" should {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        insertDividendsCyaData(Some(DividendsCheckYourAnswersModel(
          ukDividends = Some(true), ukDividendsAmount = Some(amount)
        )))
        urlPost(receivedOtherDividendsUrl, follow = false, headers = playSessionCookie(), body = Map(YesNoForm.yesNo -> YesNoForm.no))
      }

      s"return a Redirect($SEE_OTHER) to the other uk dividend amount page" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION) shouldBe Some(routes.DividendsCYAController.show(taxYear).url)
      }
    }

    "returns an action when auth call fails" which {
      lazy val result: WSResponse = {
        authoriseIndividualUnauthorized()
        dropDividendsDB()
        emptyUserDataStub()
        urlPost(receivedOtherDividendsUrl, headers = playSessionCookie(), body = Map(YesNoForm.yesNo -> YesNoForm.no))
      }
      "has an UNAUTHORIZED(401) status" in {
        result.status shouldBe UNAUTHORIZED
      }
    }

    "return a 303 status and redirect to amount page when true selected" in {
      lazy val headers = playSessionCookie(userScenarios.head.isAgent) ++
        (if (userScenarios.head.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq()) ++
        Seq("Csrf-Token" -> "nocheck")

      lazy val request = FakeRequest(
        "POST", s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/dividends-from-uk-trusts-or-open-ended-investment-companies"
      ).withHeaders(headers: _*)

      lazy val result = {
        dropStockDividendsDB()
        authoriseAgentOrIndividual(userScenarios.head.isAgent)
        emptyStockDividendsUserDataStub()
        route(appWithStockDividends, request, body = Map("value" -> Seq("true"))).get
      }

      status(result) shouldBe SEE_OTHER
      await(result).header.headers
        .get(HeaderNames.LOCATION) shouldBe Some(controllers.dividendsBase.routes.OtherUkDividendsAmountBaseController.show(taxYear).url)
    }

    "return a 303 status and redirect to next status page when false selected" in {
      lazy val headers =
        playSessionCookie(userScenarios.head.isAgent) ++
          (if (userScenarios.head.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq()) ++
          Seq("Csrf-Token" -> "nocheck")

      lazy val request = FakeRequest(
        "POST", s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/dividends-from-uk-trusts-or-open-ended-investment-companies"
      ).withHeaders(headers: _*)

      lazy val result = {
        dropStockDividendsDB()
        authoriseAgentOrIndividual(userScenarios.head.isAgent)
        emptyStockDividendsUserDataStub()
        route(appWithStockDividends, request, body = Map("value" -> Seq("false"))).get
      }

      status(result) shouldBe SEE_OTHER
      await(result).header.headers
        .get(HeaderNames.LOCATION) shouldBe Some(routes.StockDividendStatusController.show(taxYear).url)
    }

    "return a 303 status and redirect to cya page when isFinished is true" in {
      lazy val headers = playSessionCookie(userScenarios.head.isAgent) ++
        (if (userScenarios.head.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq()) ++
        Seq("Csrf-Token" -> "nocheck")

      lazy val request = FakeRequest(
        "POST", s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/dividends-from-uk-trusts-or-open-ended-investment-companies"
      ).withHeaders(headers: _*)

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
