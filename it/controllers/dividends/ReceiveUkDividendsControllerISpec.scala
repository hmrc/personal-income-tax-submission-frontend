/*
 * Copyright 2021 HM Revenue & Customs
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

import forms.YesNoForm
import models.dividends.{DividendsCheckYourAnswersModel, DividendsPriorSubmission}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import utils.{DividendsDatabaseHelper, IntegrationTest, ViewHelpers}

class ReceiveUkDividendsControllerISpec extends IntegrationTest with ViewHelpers with DividendsDatabaseHelper {

  val taxYear: Int = 2022
  val amount: BigDecimal = 500
  val receiveUkDividendsUrl = s"$appUrl/$taxYear/dividends/dividends-from-uk-companies"
  val expectedErrorLink = "#value"

  val cyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount),
    otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount))

  object Selectors {
    val yourDividendsSelector = "#main-content > div > div > form > div > fieldset > legend > p"
    val continueSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
  }

  trait SpecificExpectedReults {
    val expectedH1: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val yourDividendsText: String
    val expectedErrorText: String
  }

  trait CommonExpectedResults {
    val captionExpected: String
    val yesNo: Boolean => String
    val continueText: String
    val continueLink: String
    val errorSummaryHref: String
  }


  object IndividualExpectedEnglish extends SpecificExpectedReults {
    val expectedH1 = "Did you get dividends from UK-based companies?"
    val expectedTitle = "Did you get dividends from UK-based companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val yourDividendsText = "Your dividend voucher will tell you the shares you have in the company and the amount of the dividend you got."
    val expectedErrorText = "Select yes if you got dividends from UK-based companies"
  }

  object AgentExpectedEnglish extends SpecificExpectedReults {
    val expectedH1 = "Did your client get dividends from UK-based companies?"
    val expectedTitle = "Did your client get dividends from UK-based companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val yourDividendsText = "Your client’s dividend voucher will tell you the shares they have in the company and the amount of the dividend they got."
    val expectedErrorText = "Select yes if your client got dividends from UK-based companies"
  }

  object AllExpectedEnglish extends CommonExpectedResults {
    val captionExpected = s"Dividends for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesNo: Boolean => String = isYes => if (isYes) "Yes" else "No"
    val continueText = "Continue"
    val continueLink = s"/income-through-software/return/personal-income/$taxYear/dividends/dividends-from-uk-companies"
    val errorSummaryHref = "#value"
  }

  object IndividualExpectedWelsh extends SpecificExpectedReults {
    val expectedH1 = "A gawsoch ddifidendau gan gwmnïau yn y DU?"
    val expectedTitle = "A gawsoch ddifidendau gan gwmnïau yn y DU?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val yourDividendsText = "Bydd eich taleb ddifidend yn rhoi gwybod i chi am y cyfranddaliadau sydd gennych yn y cwmni a swm y difidend a gawsoch."
    val expectedErrorText = "Dewiswch ‘Iawn’ os cawsoch ddifidendau gan gwmnïau yn y DU"
  }

  object AgentExpectedWelsh extends SpecificExpectedReults {
    val expectedH1 = "A gafodd eich cleient ddifidendau gan gwmnïau yn y DU?"
    val expectedTitle = "A gafodd eich cleient ddifidendau gan gwmnïau yn y DU?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val yourDividendsText = "Bydd taleb ddifidend eich cleient yn rhoi gwybod i chi am y cyfranddaliadau sydd ganddynt yn y cwmni a swm y difidend a gafodd."
    val expectedErrorText = "Dewiswch ‘Iawn’ os cafodd eich cleient ddifidendau gan gwmnïau yn y DU"
  }

  object AllExpectedWelsh extends CommonExpectedResults {
    val captionExpected = s"Difidendau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val yesNo: Boolean => String = isYes => if (isYes) "Yes" else "No"
    val continueText = "Yn eich blaen"
    val continueLink = s"/income-through-software/return/personal-income/$taxYear/dividends/dividends-from-uk-companies"
    val errorSummaryHref = "#value"
  }

  val userScenarios =
    Seq(UserScenario(isWelsh = false, isAgent = false, AllExpectedEnglish, Some(IndividualExpectedEnglish)),
      UserScenario(isWelsh = false, isAgent = true, AllExpectedEnglish,  Some(AgentExpectedEnglish)),
      UserScenario(isWelsh = true, isAgent = false, AllExpectedWelsh, Some(IndividualExpectedWelsh) ),
      UserScenario(isWelsh = true, isAgent = true, AllExpectedWelsh, Some(AgentExpectedWelsh)))

  ".show" when {
    userScenarios.foreach { us =>
      
      import Selectors._
      import us.commonExpectedResults._
      import us.specificExpectedResults._
      
      s"language is ${welshTest(us.isWelsh)} and request is from an ${agentTest(us.isAgent)}" should {

        "return the uk dividends page when there is no priorSubmission data and no cyaData in session" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            dropDividendsDB()
            emptyUserDataStub()
            urlGet(receiveUkDividendsUrl, us.isWelsh, headers = playSessionCookie(us.isAgent))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(get.expectedTitle, us.isWelsh)
          h1Check(s"${get.expectedH1} ${captionExpected}")
          textOnPageCheck(get.yourDividendsText, yourDividendsSelector)
          radioButtonCheck(yesNo(true), 1)
          radioButtonCheck(yesNo(false), 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)

          welshToggleCheck(us.isWelsh)
        }

        "return the uk dividends page when there is cyaData in session" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            dropDividendsDB()
            emptyUserDataStub()
            insertCyaData(Some(cyaModel))
            urlGet(receiveUkDividendsUrl, us.isWelsh, headers = playSessionCookie(us.isAgent))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(get.expectedTitle, us.isWelsh)
          h1Check(s"${get.expectedH1} ${captionExpected}")
          textOnPageCheck(get.yourDividendsText, yourDividendsSelector)
          radioButtonCheck(yesNo(true), 1)
          radioButtonCheck(yesNo(false), 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)

          welshToggleCheck(us.isWelsh)
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
            urlPost(receiveUkDividendsUrl, welsh=us.isWelsh, follow = false, headers = playSessionCookie(us.isAgent), body = Map[String, String]())
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
            urlPost(receiveUkDividendsUrl, welsh=us.isWelsh, follow = false, headers = playSessionCookie(us.isAgent), body = Map[String, String]())
          }

          "return a BadRequest status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(get.expectedErrorTitle, us.isWelsh)
          h1Check(s"${get.expectedH1} ${captionExpected}")
          errorSummaryCheck(get.expectedErrorText, errorSummaryHref, us.isWelsh)
          textOnPageCheck(get.yourDividendsText, yourDividendsSelector)
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
          result.header(HeaderNames.LOCATION) shouldBe Some(routes.UkDividendsAmountController.show(taxYear).url)
        }
      }

      s"redirect to uk dividends amount page when 'yes' is submitted and there is already cya data. Updates cya data model in mongo. " when {
        lazy val result: WSResponse = {
          authoriseIndividual()
          dropDividendsDB()
          emptyUserDataStub()
          insertCyaData(Some(cyaModel))
          urlPost(receiveUkDividendsUrl, follow = false, headers = playSessionCookie(), body = Map(YesNoForm.yesNo -> YesNoForm.yes))
        }

        s"has a $SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header(HeaderNames.LOCATION) shouldBe Some(routes.UkDividendsAmountController.show(taxYear).url)
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
          insertCyaData(Some(DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount))))
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
          insertCyaData(Some(cyaModel))
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
    }

}
