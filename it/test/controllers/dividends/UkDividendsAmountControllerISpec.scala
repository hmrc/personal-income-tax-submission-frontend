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
import models.dividends.{DividendsCheckYourAnswersModel, DividendsPriorSubmission, StockDividendsCheckYourAnswersModel}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.Application
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, headers, route, writeableOf_AnyContentAsFormUrlEncoded}
import test.utils.{DividendsDatabaseHelper, IntegrationTest, ViewHelpers}

import scala.concurrent.Future

class UkDividendsAmountControllerISpec extends IntegrationTest with ViewHelpers with DividendsDatabaseHelper {

  val amount: BigDecimal = 500
  val ukDividendsAmountUrl: String = routes.UkDividendsAmountController.show(taxYear).url

  val cyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = None)
  val cyaModelWithAmount: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount))

  lazy val priorData: IncomeSourcesModel = IncomeSourcesModel(
    dividends = Some(DividendsPriorSubmission(
      Some(amount),
      Some(amount)
    ))
  )

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val tellUsTheValue: String
    val expectedErrorEmpty: String
    val expectedErrorOverMax: String
    val expectedErrorInvalid: String
  }

  trait CommonExpectedResults {
    val captionExpected: String
    val continueText: String
    val continueLink: String
  }


  object IndividualExpectedEnglish extends SpecificExpectedResults {
    val expectedH1 = "How much did you get in dividends from UK-based companies?"
    val expectedTitle = "How much did you get in dividends from UK-based companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val tellUsTheValue = "Tell us the value of the dividends you got, in pounds. You can find this information in your dividend voucher."
    val expectedErrorEmpty = "Enter how much you got in dividends from UK-based companies"
    val expectedErrorOverMax = "The amount of dividends from UK-based companies must be less than £100,000,000,000"
    val expectedErrorInvalid = "Enter how much you got in dividends in the correct format"
  }

  object AgentExpectedEnglish extends SpecificExpectedResults {
    val expectedH1 = "How much did your client get in dividends from UK-based companies?"
    val expectedTitle = "How much did your client get in dividends from UK-based companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val tellUsTheValue = "Tell us the value of the dividends your client got, in pounds. You can find this information in their dividend voucher."
    val expectedErrorEmpty = "Enter how much your client got in dividends from UK-based companies"
    val expectedErrorOverMax = "The amount of dividends from UK-based companies must be less than £100,000,000,000"
    val expectedErrorInvalid = "Enter how much your client got in dividends in the correct format"
  }

  object AllExpectedEnglish extends CommonExpectedResults {
    val continueText = "Continue"
    val continueLink = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/how-much-dividends-from-uk-companies"
    val captionExpected = s"Dividends for 6 April $taxYearEOY to 5 April $taxYear"
  }

  object IndividualExpectedWelsh extends SpecificExpectedResults {
    val expectedH1 = "Faint a gawsoch mewn difidendau gan gwmnïau yn y DU?"
    val expectedTitle = "Faint a gawsoch mewn difidendau gan gwmnïau yn y DU?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val tellUsTheValue = "Rhowch wybod i ni beth yw gwerth y difidendau a gawsoch, mewn punnoedd. Gallwch ddod o hyd i’r wybodaeth hon yn eich taleb ddifidend."
    val expectedErrorEmpty = "Nodwch faint a gawsoch mewn difidendau gan gwmnïau yn y DU"
    val expectedErrorOverMax = "Mae’n rhaid i swm y difidendau gan gwmnïau yn y DU fod yn llai na £100,000,000,000"
    val expectedErrorInvalid = "Nodwch faint a gawsoch mewn difidendau yn y fformat cywir"
  }

  object AgentExpectedWelsh extends SpecificExpectedResults {
    val expectedH1 = "Faint gafodd eich cleient mewn difidendau gan gwmnïau yn y DU?"
    val expectedTitle = "Faint gafodd eich cleient mewn difidendau gan gwmnïau yn y DU?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val tellUsTheValue = "Rhowch wybod i ni beth yw gwerth y difidendau a gafodd eich cleient, mewn punnoedd. Gallwch ddod o hyd i’r wybodaeth hon yn eu taleb ddifidend."
    val expectedErrorEmpty = "Nodwch faint gafodd eich cleient mewn difidendau gan gwmnïau yn y DU"
    val expectedErrorOverMax = "Mae’n rhaid i swm y difidendau gan gwmnïau yn y DU fod yn llai na £100,000,000,000"
    val expectedErrorInvalid = "Nodwch faint gafodd eich cleient mewn difidendau yn y fformat cywir"
  }

  object AllExpectedWelsh extends CommonExpectedResults {
    val continueText = "Yn eich blaen"
    val continueLink = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/how-much-dividends-from-uk-companies"
    val captionExpected = s"Difidendau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
  }

  object Selectors {

    val poundPrefixSelector = ".govuk-input__prefix"
    val captionSelector = ".govuk-caption-l"
    val inputSelector = ".govuk-input"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val tellUsTheValueSelector = "#p1"
    val expectedErrorLink = "#amount"
    val inputAmountField = "#amount"
  }

  val poundPrefixText = "£"
  val continueLink = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/how-much-dividends-from-uk-companies"
  val amountInputName = "amount"

  val userScenarios =
    Seq(UserScenario(isWelsh = false, isAgent = false, AllExpectedEnglish, Some(IndividualExpectedEnglish)),
      UserScenario(isWelsh = false, isAgent = true, AllExpectedEnglish, Some(AgentExpectedEnglish)),
      UserScenario(isWelsh = true, isAgent = false, AllExpectedWelsh, Some(IndividualExpectedWelsh)),
      UserScenario(isWelsh = true, isAgent = true, AllExpectedWelsh, Some(AgentExpectedWelsh)))

  ".show" should {

    userScenarios.foreach { scenario =>

      def getUkDividendsAmount(application: Application): Future[Result] = {
        val headers = Option.when(scenario.isWelsh)(HeaderNames.ACCEPT_LANGUAGE -> "cy").toSeq ++ playSessionCookie(scenario.isAgent)
        lazy val request = FakeRequest("GET", ukDividendsAmountUrl).withHeaders(headers: _*)

        authoriseAgentOrIndividual(scenario.isAgent)
        route(application, request, "{}").get
      }

      import Selectors._
      import scenario.commonExpectedResults._
      import scenario.specificExpectedResults._

      s"language is ${welshTest(scenario.isWelsh)} and request is from an ${agentTest(scenario.isAgent)}" should {

        "returns uk dividends amount page with empty amount field" which {
          implicit lazy val application: Application = app

          lazy val result = {
            dropDividendsDB()
            emptyUserDataStub()
            insertDividendsCyaData(Some(cyaModel))
            getUkDividendsAmount(application)
          }

          "has an OK(200) status" in {
            status(result) shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

          titleCheck(get.expectedTitle, scenario.isWelsh)
          h1Check(get.expectedH1 + " " + captionExpected)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(get.tellUsTheValue, tellUsTheValueSelector)
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck("", inputAmountField)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(scenario.commonExpectedResults.continueLink, continueButtonFormSelector)
          welshToggleCheck(scenario.isWelsh)
        }

        "returns uk dividends amount page with pre-filled amount" which {
          implicit lazy val application: Application = app

          lazy val result = {
            dropDividendsDB()
            emptyUserDataStub()
            insertDividendsCyaData(Some(cyaModelWithAmount))
            getUkDividendsAmount(application)
          }

          "has an OK(200) status" in {
            status(result) shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

          titleCheck(get.expectedTitle, scenario.isWelsh)
          h1Check(get.expectedH1 + " " + captionExpected)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck(amount.toString(), inputAmountField)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(scenario.commonExpectedResults.continueLink, continueButtonFormSelector)
          welshToggleCheck(scenario.isWelsh)
        }
      }
    }
  }

  ".show" should {

    def getUkDividendsAmount(application: Application): Future[Result] = {
      val headers = playSessionCookie()
      lazy val request = FakeRequest("GET", ukDividendsAmountUrl).withHeaders(headers: _*)

      authoriseIndividual()
      route(application, request, "{}").get
    }

    "returns uk dividends amount page with cya amount pre-filled even if there is prior submission" which {
      implicit lazy val application: Application = app

      lazy val result = {
        dropDividendsDB()
        emptyUserDataStub()
        insertDividendsCyaData(Some(cyaModelWithAmount))
        userDataStub(IncomeSourcesModel(
          dividends = Some(DividendsPriorSubmission(
            Some(1),
            None
          ))), nino, taxYear)

        getUkDividendsAmount(application)
      }

      "has an OK(200) status" in {
        status(result) shouldBe OK
      }

      implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

      inputFieldValueCheck(amount.toString(), "#amount")
    }

    "redirects user to overview page when there is no data in session" which {
      implicit lazy val application: Application = app

      lazy val result = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        stubGet(s"/update-and-submit-income-tax-return/$taxYear/view", OK, "overview page content")
        getUkDividendsAmount(application)
      }

      "has an OK(200) status" in {
        status(result) shouldBe SEE_OTHER
        headers(result).get(HeaderNames.LOCATION).value shouldBe s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view"
      }
    }

    "redirects user to overview page when there is prior submission data and no cya data in session" which {
      implicit lazy val application: Application = app

      lazy val result = {
        dropDividendsDB()
        emptyUserDataStub()
        userDataStub(IncomeSourcesModel(Some(
          DividendsPriorSubmission(ukDividends = Some(amount))
        )), nino, taxYear)
        insertDividendsCyaData(None)
        stubGet(s"/update-and-submit-income-tax-return/$taxYear/view", SEE_OTHER, "overview page content")
        getUkDividendsAmount(application)
      }

      "has a SEE_OTHER(303) status" in {
        status(result) shouldBe SEE_OTHER
      }

      "have the correct redirect URL" in {
        headers(result).get(HeaderNames.LOCATION).value shouldBe s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view"
      }
    }

    "display the stock dividend uk dividends amount page" which {
      implicit lazy val application: Application = appWithStockDividends

      lazy val result = {
        dropStockDividendsDB()
        emptyStockDividendsUserDataStub()
        getUkDividendsAmount(application)
      }

      "has a status of OK(200)" in {
        status(result) shouldBe OK
      }
    }

    "display the stock dividend uk dividends amount page with session data" which {
      implicit lazy val application: Application = appWithStockDividends

      lazy val result = {
        dropStockDividendsDB()
        insertStockDividendsCyaData(Some(completeStockDividendsCYAModel))
        emptyStockDividendsUserDataStub()
        getUkDividendsAmount(application)
      }

      "has a status of OK(200)" in {
        status(result) shouldBe OK
      }
    }
  }

  ".submit" when {


    userScenarios.foreach { scenario =>

      import Selectors._
      import scenario.specificExpectedResults._

      def postUkDividendsAmount(body: Seq[(String, String)],
                                application: Application): Future[Result] = {
        val headers = Seq("Csrf-Token" -> "nocheck") ++
          Option.when(scenario.isWelsh)(HeaderNames.ACCEPT_LANGUAGE -> "cy").toSeq ++
          playSessionCookie(scenario.isAgent)
        val request = FakeRequest("POST", ukDividendsAmountUrl).withHeaders(headers: _*).withFormUrlEncodedBody(body: _*)
        authoriseAgentOrIndividual(scenario.isAgent)
        route(application, request).get
      }

      s"language is ${welshTest(scenario.isWelsh)} and request is from an ${agentTest(scenario.isAgent)}" should {


        s"return a BAD_REQUEST($BAD_REQUEST) status with an empty error" which {
          implicit lazy val application: Application = app

          lazy val result = {
            emptyUserDataStub()
            postUkDividendsAmount(Seq.empty, application)
          }

          "return the correct status" in {
            status(result) shouldBe BAD_REQUEST
          }

          implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))
          errorSummaryCheck(get.expectedErrorEmpty, expectedErrorLink, scenario.isWelsh)
          errorAboveElementCheck(get.expectedErrorEmpty)
        }

        s"return a BAD_REQUEST($BAD_REQUEST) status with an invalid error" which {
          implicit lazy val application: Application = app

          lazy val result = {
            emptyUserDataStub()
            postUkDividendsAmount(Seq("amount" -> "|"), application)
          }

          "return the correct status" in {
            status(result) shouldBe BAD_REQUEST
          }

          implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))
          errorSummaryCheck(get.expectedErrorInvalid, expectedErrorLink, scenario.isWelsh)
          errorAboveElementCheck(get.expectedErrorInvalid)
        }

        s"return a BAD_REQUEST($BAD_REQUEST) status with an OverMax error" which {
          implicit lazy val application: Application = app

          lazy val result = {
            emptyUserDataStub()
            postUkDividendsAmount(Seq("amount" -> "9999999999999999999999999"), application)
          }

          "return the correct status" in {
            status(result) shouldBe BAD_REQUEST
          }

          implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))
          errorSummaryCheck(get.expectedErrorOverMax, expectedErrorLink, scenario.isWelsh)
          errorAboveElementCheck(get.expectedErrorOverMax)
        }

      }
    }
  }

  ".submit" should {

    def postUkDividendsAmount(body: Seq[(String, String)],
                              application: Application): Future[Result] = {
      val headers = Seq("Csrf-Token" -> "nocheck") ++ playSessionCookie()
      val request = FakeRequest("POST", ukDividendsAmountUrl).withHeaders(headers: _*).withFormUrlEncodedBody(body: _*)
      authoriseIndividual()
      route(application, request).get
    }

    "redirects User to overview page if no CYA data is in session" when {
      implicit lazy val application: Application = app

      lazy val result = {
        dropDividendsDB()
        emptyUserDataStub()
        postUkDividendsAmount(Seq("amount" -> "123"), application)
      }

      "has a SEE_OTHER(303) status" in {
        status(result) shouldBe SEE_OTHER
      }

      "have the correct redirect URL" in {
        headers(result).get(HeaderNames.LOCATION).value shouldBe s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view"
      }
    }

    "redirect to Did you receive other dividends page if form is valid and there is incomplete cya data and Did you receive uk dividends is yes" when {
      implicit lazy val application: Application = app

      lazy val result = {
        dropDividendsDB()
        emptyUserDataStub()
        insertDividendsCyaData(Some(cyaModel))
        postUkDividendsAmount(Seq("amount" -> "123"), application)
      }

      s"has a $SEE_OTHER(303) status" in {
        status(result) shouldBe SEE_OTHER
        headers(result).get(HeaderNames.LOCATION).value shouldBe routes.ReceiveOtherUkDividendsController.show(taxYear).url
      }
    }

    "redirect to Dividends CYA page if form is valid and there is complete cya data" when {
      implicit lazy val application: Application = app

      lazy val result = {
        dropDividendsDB()
        emptyUserDataStub()
        userDataStub(priorData, nino, taxYear)
        insertDividendsCyaData(Some(DividendsCheckYourAnswersModel(None, Some(true), Some(amount), Some(true), Some(amount))))
        postUkDividendsAmount(Seq("amount" -> "123"), application)
      }

      s"has a $SEE_OTHER(303) status" in {
        status(result) shouldBe SEE_OTHER
        headers(result).get(HeaderNames.LOCATION).value shouldBe routes.DividendsCYAController.show(taxYear).url
      }
    }

    "return a 303 status and redirect to next status page" in {
      implicit lazy val application: Application = appWithStockDividends

      lazy val result = {
        dropStockDividendsDB()
        insertStockDividendsCyaData(Some(StockDividendsCheckYourAnswersModel()))
        emptyStockDividendsUserDataStub()
        emptyUserDataStub()
        postUkDividendsAmount(Seq("amount" -> "123"), application)
      }

      status(result) shouldBe SEE_OTHER
      headers(result).get(HeaderNames.LOCATION).value shouldBe routes.ReceiveOtherUkDividendsController.show(taxYear).url
    }

    "return a 303 status and redirect to cya page when isFinished is true" in {
      implicit lazy val application: Application = appWithStockDividends

      lazy val result = {
        dropStockDividendsDB()
        insertStockDividendsCyaData(Some(completeStockDividendsCYAModel))
        emptyUserDataStub()
        emptyStockDividendsUserDataStub()
        postUkDividendsAmount(Seq("amount" -> "123"), application)
      }

      status(result) shouldBe SEE_OTHER
      headers(result).get(HeaderNames.LOCATION).value shouldBe routes.DividendsSummaryController.show(taxYear).url
    }
  }
}
