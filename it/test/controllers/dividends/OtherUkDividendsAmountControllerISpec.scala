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

class OtherUkDividendsAmountControllerISpec extends IntegrationTest with ViewHelpers with DividendsDatabaseHelper {

  val amount: BigDecimal = 500
  val otherUkDividendsAmountUrl: String = controllers.dividendsBase.routes.OtherUkDividendsAmountBaseController.show(taxYear).url

  val validCyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = None)
  val validCyaModelWithAmount: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(otherUkDividends = Some(true),
    otherUkDividendsAmount = Some(amount))

  val poundPrefixText = "£"

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
    val expectedHintText: String
  }


  object IndividualExpectedEnglish extends SpecificExpectedResults {
    val expectedH1 = "How much did you get in dividends from trusts and open-ended investment companies based in the UK?"
    val expectedTitle = "How much did you get in dividends from trusts and open-ended investment companies based in the UK?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val tellUsTheValue = "Tell us the value of the dividends you got, in pounds. You can find this information in your dividend voucher."
    val expectedErrorEmpty = "Enter how much you got in dividends from trusts and open-ended investment companies"
    val expectedErrorInvalid = "Enter how much you got in dividends in the correct format"
    val expectedErrorOverMax = "The amount of dividends from trusts and open-ended investment companies based in the UK must be less than £100,000,000,000"
  }

  object AgentExpectedEnglish extends SpecificExpectedResults {
    val expectedH1 = "How much did your client get in dividends from trusts and open-ended investment companies based in the UK?"
    val expectedTitle = "How much did your client get in dividends from trusts and open-ended investment companies based in the UK?"
    val expectedErrorTitleAgent = s"Error: $expectedTitle"
    val tellUsTheValue = "Tell us the value of the dividends your client got, in pounds. You can find this information in their dividend voucher."
    val expectedErrorEmpty = "Enter how much your client got in dividends from trusts and open-ended investment companies"
    val expectedErrorInvalid = "Enter how much your client got in dividends in the correct format"
    val expectedErrorOverMax = "The amount of dividends from trusts and open-ended investment companies based in the UK must be less than £100,000,000,000"
    val expectedErrorTitle: String = s"Error $expectedTitle"
  }

  object AllExpectedEnglish extends CommonExpectedResults {
    val continueText = "Continue"
    val expectedHintText = "For example, £193.52"
    val captionExpected = s"Dividends for 6 April $taxYearEOY to 5 April $taxYear"
  }

  object IndividualExpectedWelsh extends SpecificExpectedResults {
    val expectedH1 = "Faint a gawsoch mewn difidendau gan ymddiriedolaethau a chwmnïau buddsoddi penagored sydd wedi’u lleoli yn y DU?"
    val expectedTitle = "Faint a gawsoch mewn difidendau gan ymddiriedolaethau a chwmnïau buddsoddi penagored sydd wedi’u lleoli yn y DU?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val tellUsTheValue = "Rhowch wybod i ni beth yw gwerth y difidendau a gawsoch, mewn punnoedd. Gallwch ddod o hyd i’r wybodaeth hon yn eich taleb ddifidend."
    val expectedErrorEmpty = "Nodwch faint a gawsoch mewn difidendau gan ymddiriedolaethau a chwmnïau buddsoddi penagored"
    val expectedErrorInvalid = "Nodwch faint a gawsoch mewn difidendau yn y fformat cywir"
    val expectedErrorOverMax = "Mae’n rhaid i swm y difidendau gan ymddiriedolaethau a chwmnïau buddsoddi penagored sydd wedi’u lleoli yn y DU fod yn llai na £100,000,000,000"
  }

  object AgentExpectedWelsh extends SpecificExpectedResults {
    val expectedH1 = "Faint wnaeth eich cleient gael mewn difidendau gan ymddiriedolaethau a chwmnïau buddsoddi penagored sydd wedi’u lleoli yn y DU?"
    val expectedTitle = "Faint wnaeth eich cleient gael mewn difidendau gan ymddiriedolaethau a chwmnïau buddsoddi penagored sydd wedi’u lleoli yn y DU?"
    val expectedErrorTitleAgent = s"Gwall: $expectedTitle"
    val tellUsTheValue = "Rhowch wybod i ni beth yw gwerth y difidendau a gafodd eich cleient, mewn punnoedd. Gallwch ddod o hyd i’r wybodaeth hon yn eu taleb ddifidend."
    val expectedErrorEmpty = "Nodwch faint a gafodd eich cleient gan ymddiriedolaethau a chwmnïau buddsoddi penagored"
    val expectedErrorInvalid = "Nodwch faint gafodd eich cleient mewn difidendau yn y fformat cywir"
    val expectedErrorOverMax = "Mae’n rhaid i swm y difidendau gan ymddiriedolaethau a chwmnïau buddsoddi penagored sydd wedi’u lleoli yn y DU fod yn llai na £100,000,000,000"
    val expectedErrorTitle: String = s"Gwall: $expectedTitle"
  }

  object AllExpectedWelsh extends CommonExpectedResults {
    val continueText = "Yn eich blaen"
    val expectedHintText = "Er enghraifft, £193.52"
    val captionExpected = s"Difidendau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
  }

  object Selectors {

    val poundPrefixSelector = ".govuk-input__prefix"
    val captionSelector = ".govuk-caption-l"
    val inputSelector = ".govuk-input"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val enterAmountSelector = "#amount"
    val tellUsTheValueSelector = "#p1"
    val amountSelector = "#amount"
  }

  val continueLink = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/how-much-dividends-from-uk-trusts-and-open-ended-investment-companies"
  val newAmountInput = "#amount"
  val amountInputName = "amount"
  val expectedErrorLink = "#amount"

  val userScenarios =
    Seq(UserScenario(isWelsh = false, isAgent = false, AllExpectedEnglish, Some(IndividualExpectedEnglish)),
      UserScenario(isWelsh = false, isAgent = true, AllExpectedEnglish, Some(AgentExpectedEnglish)),
      UserScenario(isWelsh = true, isAgent = false, AllExpectedWelsh, Some(IndividualExpectedWelsh)),
      UserScenario(isWelsh = true, isAgent = true, AllExpectedWelsh, Some(AgentExpectedWelsh)))


  ".show" when {

    userScenarios.foreach { scenario =>
      def getOtherUkDividendsAmount(application: Application): Future[Result] = {
        val headers = Option.when(scenario.isWelsh)(HeaderNames.ACCEPT_LANGUAGE -> "cy").toSeq ++ playSessionCookie(scenario.isAgent)
        lazy val request = FakeRequest("GET", otherUkDividendsAmountUrl).withHeaders(headers: _*)

        authoriseAgentOrIndividual(scenario.isAgent)
        route(application, request, "{}").get
      }

      s"language is ${welshTest(scenario.isWelsh)} and request is from an ${agentTest(scenario.isAgent)}" should {

        import Selectors._
        import scenario.commonExpectedResults._
        import scenario.specificExpectedResults._

        "returns other uk dividends amount page with relevant content and amount field empty" which {
          implicit lazy val application: Application = app

          lazy val result = {
            dropDividendsDB()
            emptyUserDataStub()
            insertDividendsCyaData(Some(validCyaModel))
            getOtherUkDividendsAmount(application)
          }

          "has an OK(200) status" in {
            status(result) shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

          titleCheck(get.expectedTitle, scenario.isWelsh)
          h1Check(get.expectedH1 + " " + captionExpected)
          textOnPageCheck(captionExpected, captionSelector)
          textOnPageCheck(get.tellUsTheValue, tellUsTheValueSelector)
          hintTextCheck(expectedHintText)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)

          welshToggleCheck(scenario.isWelsh)
        }

        "returns other uk dividends amount page with with relevant content and amount field pre-filled" which {
          implicit lazy val application: Application = app

          lazy val result = {
            dropDividendsDB()
            emptyUserDataStub()
            insertDividendsCyaData(Some(validCyaModelWithAmount))
            getOtherUkDividendsAmount(application)
          }

          "has an OK(200) status" in {
            status(result) shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

          titleCheck(get.expectedTitle, scenario.isWelsh)
          h1Check(get.expectedH1 + " " + captionExpected)
          textOnPageCheck(captionExpected, captionSelector)
          hintTextCheck(expectedHintText)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          inputFieldValueCheck(amount.toString(), amountSelector)

          welshToggleCheck(scenario.isWelsh)

        }
      }
    }
  }

  ".show" should {
    def getOtherUkDividendsAmount(application: Application): Future[Result] = {
      val headers = playSessionCookie()
      lazy val request = FakeRequest("GET", otherUkDividendsAmountUrl).withHeaders(headers: _*)

      authoriseIndividual()
      route(application, request, "{}").get
    }

    "redirects user to overview page when there is no data in session" which {
      implicit lazy val application: Application = app

      lazy val result = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        stubGet(s"/update-and-submit-income-tax-return/$taxYear/view", OK, "overview page content")
        getOtherUkDividendsAmount(application)
      }

      "has a SEE_OTHER(303) status" in {
        status(result) shouldBe SEE_OTHER
      }

      "have the correct redirect URL" in {
        headers(result).get(HeaderNames.LOCATION).value shouldBe s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view"
      }
    }

    "returns other uk dividends amount page with cya amount pre-filled even if there is prior submission" which {
      implicit lazy val application: Application = app

      lazy val result = {
        dropDividendsDB()
        emptyUserDataStub()
        insertDividendsCyaData(Some(validCyaModelWithAmount))
        userDataStub(IncomeSourcesModel(
          dividends = Some(DividendsPriorSubmission(
            None,
            Some(1)))),
          nino, taxYear)
        getOtherUkDividendsAmount(application)
      }

      "has an OK(200) status" in {
        status(result) shouldBe OK
      }

      implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

      inputFieldValueCheck(amount.toString(), "#amount")
    }

    "returns other uk dividends with empty amount field when priorSubmissionData and cyaData amounts are equal" which {
      implicit lazy val application: Application = app

      lazy val result = {
        dropDividendsDB()
        emptyUserDataStub()
        insertDividendsCyaData(Some(validCyaModel))
        userDataStub(IncomeSourcesModel(
          dividends = Some(DividendsPriorSubmission(
            None,
            Some(amount)))),
          nino, taxYear)
        getOtherUkDividendsAmount(application)
      }

      "has an OK(200) status" in {
        status(result) shouldBe OK
      }

      implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))

      inputFieldValueCheck("", "#amount")
    }

    "display the stock dividend other uk dividends amount page with appWithStockDividends" which {
      implicit lazy val application: Application = appWithStockDividends

      lazy val result = {
        dropStockDividendsDB()
        emptyStockDividendsUserDataStub()
        getOtherUkDividendsAmount(application)
      }

      "has a status of OK(200)" in {
        status(result) shouldBe OK
      }
    }

    "display the stock dividend other uk dividends amount page with appWithStockDividendsBackendMongo" which {
      implicit lazy val application: Application = appWithStockDividendsBackendMongo

      lazy val result = {
        getSessionDataStub()
        emptyStockDividendsUserDataStub()
        getOtherUkDividendsAmount(application)
      }

      "has a status of OK(200)" in {
        status(result) shouldBe OK
      }
    }

    "display the stock dividend other uk dividends amount page with session data" which {
      implicit lazy val application: Application = appWithStockDividends

      lazy val result = {
        dropStockDividendsDB()
        insertStockDividendsCyaData(Some(completeStockDividendsCYAModel))
        emptyStockDividendsUserDataStub()
        getOtherUkDividendsAmount(application)
      }

      "has a status of OK(200)" in {
        status(result) shouldBe OK
      }
    }
  }

  ".submit" when {

    userScenarios.foreach { scenario =>

      def postOtherUkDividendsAmount(body: Seq[(String, String)], application: Application): Future[Result] = {
        val headers = Seq("Csrf-Token" -> "nocheck") ++
          Option.when(scenario.isWelsh)(HeaderNames.ACCEPT_LANGUAGE -> "cy").toSeq ++
          playSessionCookie(scenario.isAgent)
        lazy val request = FakeRequest("POST", otherUkDividendsAmountUrl).withHeaders(headers: _*).withFormUrlEncodedBody(body: _*)

        authoriseAgentOrIndividual(scenario.isAgent)
        route(application, request).get
      }

      import scenario.specificExpectedResults._

      s"language is ${welshTest(scenario.isWelsh)} and request is from an ${agentTest(scenario.isAgent)}" should {

        s"return a BAD_REQUEST($BAD_REQUEST) status with an Empty Error" which {
          implicit lazy val application: Application = app

          lazy val result = {
            emptyUserDataStub()
            postOtherUkDividendsAmount(Seq.empty, application)
          }

          "has the correct status" in {
            status(result) shouldBe BAD_REQUEST
          }

          implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))
          errorSummaryCheck(get.expectedErrorEmpty, expectedErrorLink, scenario.isWelsh)
          errorAboveElementCheck(get.expectedErrorEmpty)
        }

        s"return a BAD_REQUEST($BAD_REQUEST) status with an Invalid Error" which {
          implicit lazy val application: Application = app

          lazy val result = {
            emptyUserDataStub()
            postOtherUkDividendsAmount(Seq("amount" -> "|"), application)
          }

          "has the correct status" in {
            status(result) shouldBe BAD_REQUEST

          }

          implicit val document: () => Document = () => Jsoup.parse(contentAsString(result))
          errorSummaryCheck(get.expectedErrorInvalid, expectedErrorLink, scenario.isWelsh)
          errorAboveElementCheck(get.expectedErrorInvalid)
        }

        s"return a BAD_REQUEST($BAD_REQUEST) status with an OverMax Error" which {
          implicit lazy val application: Application = app

          lazy val result = {
            emptyUserDataStub()
            postOtherUkDividendsAmount(Seq("amount" -> "9999999999999999999999999999"), application)
          }

          "has the correct status" in {
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

    def postOtherUkDividendsAmount(body: Seq[(String, String)], application: Application): Future[Result] = {
      val headers = Seq("Csrf-Token" -> "nocheck") ++ playSessionCookie()
      lazy val request = FakeRequest("POST", otherUkDividendsAmountUrl).withHeaders(headers: _*).withFormUrlEncodedBody(body: _*)

      authoriseIndividual()
      route(application, request).get
    }

    "redirects User to overview page if no CYA data is in session" when {
      implicit lazy val application: Application = app

      lazy val result = {
        dropDividendsDB()
        emptyUserDataStub()
        postOtherUkDividendsAmount(Seq("amount" -> "123"), application)
      }

      "has a SEE_OTHER(303) status" in {
        status(result) shouldBe SEE_OTHER
      }

      "have the correct redirect URL" in {
        headers(result).get(HeaderNames.LOCATION).value shouldBe s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view"
      }
    }

    "redirect to Dividends CYA page if answer to Did you get other uk dividends is yes" should {
      implicit lazy val application: Application = app

      lazy val result = {
        dropDividendsDB()
        emptyUserDataStub()
        insertDividendsCyaData(Some(DividendsCheckYourAnswersModel(None, Some(true), Some(amount), Some(true))))
        postOtherUkDividendsAmount(Seq("amount" -> "123"), application)
      }

      "has a SEE_OTHER(303) status" in {
        status(result) shouldBe SEE_OTHER
      }

      "have the correct redirect URL" in {
        headers(result).get(HeaderNames.LOCATION).value shouldBe routes.DividendsCYAController.show(taxYear).url
      }
    }

    "return a 303 status and redirect to next status page with appWithStockDividends" in {
      implicit lazy val application: Application = appWithStockDividends

      lazy val result = {
        dropStockDividendsDB()
        insertStockDividendsCyaData(Some(StockDividendsCheckYourAnswersModel()))
        emptyStockDividendsUserDataStub()
        emptyUserDataStub()
        postOtherUkDividendsAmount(Seq("amount" -> "123"), application)
      }

      status(result) shouldBe SEE_OTHER
      headers(result).get(HeaderNames.LOCATION).value shouldBe routes.StockDividendStatusController.show(taxYear).url
    }

    "return a 303 status and redirect to next status page with appWithStockDividendsBackendMongo" in {
      implicit lazy val application: Application = appWithStockDividendsBackendMongo

      lazy val result = {
        getSessionDataStub(Some(stockDividendsUserDataModel.copy(
            stockDividends = Some(StockDividendsCheckYourAnswersModel()))))
        updateSessionDataStub()
        emptyStockDividendsUserDataStub()
        emptyUserDataStub()
        postOtherUkDividendsAmount(Seq("amount" -> "123"), application)
      }

      status(result) shouldBe SEE_OTHER
      headers(result).get(HeaderNames.LOCATION).value shouldBe routes.StockDividendStatusController.show(taxYear).url
    }

    "return a 303 status and redirect to cya page when isFinished is true with appWithStockDividends" in {
      implicit lazy val application: Application = appWithStockDividends

      lazy val result = {
        dropStockDividendsDB()
        insertStockDividendsCyaData(Some(completeStockDividendsCYAModel))
        emptyUserDataStub()
        emptyStockDividendsUserDataStub()
        postOtherUkDividendsAmount(Seq("amount" -> "123"), application)
      }
      status(result) shouldBe SEE_OTHER
      headers(result).get(HeaderNames.LOCATION).value shouldBe routes.DividendsSummaryController.show(taxYear).url
    }

    "return a 303 status and redirect to cya page when isFinished is true with appWithStockDividendsBackendMongo" in {
      implicit lazy val application: Application = appWithStockDividendsBackendMongo

      lazy val result = {
        getSessionDataStub(Some(stockDividendsUserDataModel.copy(
            stockDividends = Some(completeStockDividendsCYAModel))))
        updateSessionDataStub()
        emptyUserDataStub()
        emptyStockDividendsUserDataStub()
        postOtherUkDividendsAmount(Seq("amount" -> "123"), application)
      }
      status(result) shouldBe SEE_OTHER
      headers(result).get(HeaderNames.LOCATION).value shouldBe routes.DividendsSummaryController.show(taxYear).url
    }
  }
}

