/*
 * Copyright 2022 HM Revenue & Customs
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

import models.dividends.{DividendsCheckYourAnswersModel, DividendsPriorSubmission}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import utils.{DividendsDatabaseHelper, IntegrationTest, ViewHelpers}

class UkDividendsAmountControllerISpec extends IntegrationTest with ViewHelpers with DividendsDatabaseHelper {


  val taxYear: Int = 2022
  val taxYearMinusOne: Int = taxYear - 1
  val amount: BigDecimal = 500
  val ukDividendsAmountUrl = s"$appUrl/$taxYear/dividends/how-much-dividends-from-uk-companies"

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
    val youToldUsPriorText: String
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
    val youToldUsPriorText = s"You told us you got £$amount in dividends from UK-based companies this year. Tell us if this has changed."
    val expectedErrorEmpty = "Enter how much you got in dividends from UK-based companies"
    val expectedErrorOverMax = "The amount of dividends from UK-based companies must be less than £100,000,000,000"
    val expectedErrorInvalid = "Enter how much you got in dividends in the correct format"
  }

  object AgentExpectedEnglish extends SpecificExpectedResults {
    val expectedH1 = "How much did your client get in dividends from UK-based companies?"
    val expectedTitle = "How much did your client get in dividends from UK-based companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val tellUsTheValue = "Tell us the value of the dividends your client got, in pounds. You can find this information in their dividend voucher."
    val youToldUsPriorText = s"You told us your client got £$amount in dividends from UK-based companies this year. Tell us if this has changed."
    val expectedErrorEmpty = "Enter how much your client got in dividends from UK-based companies"
    val expectedErrorOverMax = "The amount of dividends from UK-based companies must be less than £100,000,000,000"
    val expectedErrorInvalid = "Enter how much your client got in dividends in the correct format"
  }

  object AllExpectedEnglish extends CommonExpectedResults {
    val continueText = "Continue"
    val continueLink = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/how-much-dividends-from-uk-companies"
    val captionExpected = s"Dividends for 6 April $taxYearMinusOne to 5 April $taxYear"
  }

  object IndividualExpectedWelsh extends SpecificExpectedResults {
    val expectedH1 = "Faint a gawsoch mewn difidendau gan gwmnïau yn y DU?"
    val expectedTitle = "Faint a gawsoch mewn difidendau gan gwmnïau yn y DU?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val tellUsTheValue = "Rhowch wybod i ni beth yw gwerth y difidendau a gawsoch, mewn punnoedd. Gallwch ddod o hyd i’r wybodaeth hon yn eich taleb ddifidend."
    val youToldUsPriorText = s"Gwnaethoch ddweud wrthym cawsoch £$amount mewn difidendau gan gwmnïau yn y DU y flwyddyn hon. Rhowch wybod i ni a yw hyn wedi newid."
    val expectedErrorEmpty = "Nodwch faint a gawsoch mewn difidendau gan gwmnïau yn y DU"
    val expectedErrorOverMax = "Mae’n rhaid i swm y difidendau gan gwmnïau yn y DU fod yn llai na £100,000,000,000"
    val expectedErrorInvalid = "Nodwch faint a gawsoch mewn difidendau yn y fformat cywir"
  }

  object AgentExpectedWelsh extends SpecificExpectedResults {
    val expectedH1 = "Faint gafodd eich cleient mewn difidendau gan gwmnïau yn y DU?"
    val expectedTitle = "Faint gafodd eich cleient mewn difidendau gan gwmnïau yn y DU?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val tellUsTheValue = "Rhowch wybod i ni beth yw gwerth y difidendau a gafodd eich cleient, mewn punnoedd. Gallwch ddod o hyd i’r wybodaeth hon yn eu taleb ddifidend."
    val youToldUsPriorText = s"Gwnaethoch ddweud wrthym cafodd eich cleient £$amount mewn difidendau gan gwmnïau yn y DU y flwyddyn hon. Rhowch wybod i ni a yw hyn wedi newid."
    val expectedErrorEmpty = "Nodwch faint gafodd eich cleient mewn difidendau gan gwmnïau yn y DU"
    val expectedErrorOverMax = "Mae’n rhaid i swm y difidendau gan gwmnïau yn y DU fod yn llai na £100,000,000,000"
    val expectedErrorInvalid = "Nodwch faint gafodd eich cleient mewn difidendau yn y fformat cywir"
  }

  object AllExpectedWelsh extends CommonExpectedResults {
    val continueText = "Yn eich blaen"
    val continueLink = s"/update-and-submit-income-tax-return/personal-income/$taxYear/dividends/how-much-dividends-from-uk-companies"
    val captionExpected = s"Difidendau ar gyfer 6 Ebrill $taxYearMinusOne i 5 Ebrill $taxYear"
  }

  object Selectors {

    val poundPrefixSelector = ".govuk-input__prefix"
    val captionSelector = ".govuk-caption-l"
    val inputSelector = ".govuk-input"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val youToldUsSelector = "#main-content > div > div > form > div > label > p"
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

    userScenarios.foreach { us =>

      import Selectors._
      import us.commonExpectedResults._
      import us.specificExpectedResults._

      s"language is ${welshTest(us.isWelsh)} and request is from an ${agentTest(us.isAgent)}" should {

        "returns uk dividends amount page with empty amount field" which {

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            dropDividendsDB()
            emptyUserDataStub()
            insertCyaData(Some(cyaModel))
            urlGet(ukDividendsAmountUrl, us.isWelsh, headers = playSessionCookie(us.isAgent))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(get.expectedTitle, us.isWelsh)
          h1Check(get.expectedH1 + " " + captionExpected)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(get.tellUsTheValue, youToldUsSelector)
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck("", inputAmountField)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(us.commonExpectedResults.continueLink, continueButtonFormSelector)
          welshToggleCheck(us.isWelsh)
        }

        "returns uk dividends amount page with pre-filled amount" which {


          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            dropDividendsDB()
            emptyUserDataStub()
            insertCyaData(Some(cyaModelWithAmount))
            urlGet(ukDividendsAmountUrl, us.isWelsh, headers = playSessionCookie(us.isAgent))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(get.expectedTitle, us.isWelsh)
          h1Check(get.expectedH1 + " " + captionExpected)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(get.youToldUsPriorText, youToldUsSelector)
          inputFieldCheck(amountInputName, inputSelector)
          inputFieldValueCheck(amount.toString(), inputAmountField)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(us.commonExpectedResults.continueLink, continueButtonFormSelector)
          welshToggleCheck(us.isWelsh)
        }
      }
    }
  }

  ".show" should {

    "returns uk dividends amount page with cya amount pre-filled even if there is prior submission" which {

      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        insertCyaData(Some(cyaModelWithAmount))
        userDataStub(IncomeSourcesModel(
          dividends = Some(DividendsPriorSubmission(
            Some(1),
            None
          ))), nino, taxYear)

        urlGet(ukDividendsAmountUrl, headers = playSessionCookie())
      }

      "has an OK(200) status" in {
        result.status shouldBe OK
      }

      implicit val document: () => Document = () => Jsoup.parse(result.body)

      inputFieldValueCheck(amount.toString(), "#amount")
    }

    "returns uk dividends with empty amount field when priorSubmissionData and cyaData amounts are equal" which {

      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        insertCyaData(Some(cyaModelWithAmount))
        userDataStub(IncomeSourcesModel(
          dividends = Some(DividendsPriorSubmission(
            Some(amount),
            None
          ))), nino, taxYear)
        urlGet(ukDividendsAmountUrl, headers = playSessionCookie())
      }

      "has an OK(200) status" in {
        result.status shouldBe OK
      }

      implicit val document: () => Document = () => Jsoup.parse(result.body)

      inputFieldValueCheck("", "#amount")

    }

    "redirects user to overview page when there is no data in session" which {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        stubGet(s"/update-and-submit-income-tax-return/$taxYear/view", OK, "overview page content")
        urlGet(ukDividendsAmountUrl, headers = playSessionCookie())
      }

      "has an OK(200) status" in {
        result.status shouldBe OK
        result.body shouldBe "overview page content"
      }
    }

    "redirects user to overview page when there is prior submission data and no cya data in session" which {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        userDataStub(IncomeSourcesModel(Some(
          DividendsPriorSubmission(ukDividends = Some(amount))
        )), nino, taxYear)
        insertCyaData(None)
        stubGet(s"/update-and-submit-income-tax-return/$taxYear/view", SEE_OTHER, "overview page content")
        urlGet(ukDividendsAmountUrl, follow = false, headers = playSessionCookie())
      }

      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
      }

      "have the correct redirect URL" in {
        result.headers(HeaderNames.LOCATION).head shouldBe "http://localhost:11111/update-and-submit-income-tax-return/2022/view"
      }
    }
  }

  ".submit" when {


    userScenarios.foreach { us =>

      import Selectors._
      import us.specificExpectedResults._

      s"language is ${welshTest(us.isWelsh)} and request is from an ${agentTest(us.isAgent)}" should {


        s"return a BAD_REQUEST($BAD_REQUEST) status with an empty error" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(ukDividendsAmountUrl, welsh=us.isWelsh, headers = playSessionCookie(us.isAgent), body = Map[String, String]())
          }

          "return the correct status" in {
            result.status shouldBe BAD_REQUEST
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)
          errorSummaryCheck(get.expectedErrorEmpty, expectedErrorLink, us.isWelsh)
          errorAboveElementCheck(get.expectedErrorEmpty)
        }
        s"return a BAD_REQUEST($BAD_REQUEST) status with an invalid error" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(ukDividendsAmountUrl, welsh=us.isWelsh, headers = playSessionCookie(us.isAgent), body = Map("amount" -> "|"))
          }

          "return the correct status" in {
            result.status shouldBe BAD_REQUEST
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)
          errorSummaryCheck(get.expectedErrorInvalid, expectedErrorLink, us.isWelsh)
          errorAboveElementCheck(get.expectedErrorInvalid)
        }
        s"return a BAD_REQUEST($BAD_REQUEST) status with an OverMax error" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(ukDividendsAmountUrl, welsh=
              us.isWelsh, headers = playSessionCookie(us.isAgent), body = Map("amount" -> "9999999999999999999999999"))
          }

          "return the correct status" in {
            result.status shouldBe BAD_REQUEST
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)
          errorSummaryCheck(get.expectedErrorOverMax, expectedErrorLink, us.isWelsh)
          errorAboveElementCheck(get.expectedErrorOverMax)
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
        urlGet(ukDividendsAmountUrl, follow = false, headers = playSessionCookie())
        urlPost(ukDividendsAmountUrl, follow = false, headers = playSessionCookie(), body = Map("amount" -> "123"))
      }
      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
      }

      "have the correct redirect URL" in {
        result.headers(HeaderNames.LOCATION).head shouldBe "http://localhost:11111/update-and-submit-income-tax-return/2022/view"
      }
    }


    "redirect to Did you receive other dividends page if form is valid and there is incomplete cya data and Did you receive uk dividends is yes" when {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        insertCyaData(Some(cyaModel))
        urlPost(ukDividendsAmountUrl, follow = false, headers = playSessionCookie(), body = Map("amount" -> "123"))
      }

      s"has a $SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION) shouldBe Some(routes.ReceiveOtherUkDividendsController.show(taxYear).url)
      }
    }

    "redirect to Dividends CYA page if form is valid and there is complete cya data" when {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        userDataStub(priorData, nino, taxYear)
        insertCyaData(Some(DividendsCheckYourAnswersModel(Some(true), Some(amount), Some(true), Some(amount))))
        urlPost(ukDividendsAmountUrl, follow = false, headers = playSessionCookie(), body = Map("amount" -> "123"))
      }

      s"has a $SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION) shouldBe Some(routes.DividendsCYAController.show(taxYear).url)
      }
    }

  }
}
