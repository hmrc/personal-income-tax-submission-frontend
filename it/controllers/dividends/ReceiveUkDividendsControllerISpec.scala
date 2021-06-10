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

import common.SessionValues
import controllers.dividends.routes.{DividendsCYAController, ReceiveOtherUkDividendsController, UkDividendsAmountController}
import forms.YesNoForm
import helpers.PlaySessionCookieBaker
import models.dividends.DividendsCheckYourAnswersModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, ViewHelpers}

class ReceiveUkDividendsControllerISpec extends IntegrationTest with ViewHelpers {

  val taxYear: Int = 2022
  val amount: BigDecimal = 500
  val receiveUkDividendsUrl = s"$startUrl/$taxYear/dividends/dividends-from-uk-companies"

  val cyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount),
    otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount))

  object Selectors {
    val yourDividendsSelector = "#main-content > div > div > form > div > fieldset > legend > p"
    val continueSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
  }

  trait ExpectedResultsUserType {
    val expectedH1: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val yourDividendsText: String
    val expectedErrorText: String
  }

  trait ExpectedResultsAllUsers {
    val captionExpected: String
    val yesNo: Boolean => String
    val continueText: String
    val continueLink: String
    val errorSummaryHref: String
  }


  object IndividualExpectedEnglish extends ExpectedResultsUserType {
    val expectedH1 = "Did you get dividends from UK-based companies?"
    val expectedTitle = "Did you get dividends from UK-based companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val yourDividendsText = "Your dividend voucher will tell you the shares you have in the company and the amount of the dividend you got."
    val expectedErrorText = "Select yes if you got dividends from UK-based companies"
  }

  object AgentExpectedEnglish extends ExpectedResultsUserType {
    val expectedH1 = "Did your client get dividends from UK-based companies?"
    val expectedTitle = "Did your client get dividends from UK-based companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val yourDividendsText = "Your client’s dividend voucher will tell you the shares they have in the company and the amount of the dividend they got."
    val expectedErrorText = "Select yes if your client got dividends from UK-based companies"
  }

  object AllExpectedEnglish extends ExpectedResultsAllUsers {
    val captionExpected = s"Dividends for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesNo: Boolean => String = isYes => if (isYes) "Yes" else "No"
    val continueText = "Continue"
    val continueLink = s"/income-through-software/return/personal-income/$taxYear/dividends/dividends-from-uk-companies"
    val errorSummaryHref = "#value"
  }

  object IndividualExpectedWelsh extends ExpectedResultsUserType {
    val expectedH1 = "Did you get dividends from UK-based companies?"
    val expectedTitle = "Did you get dividends from UK-based companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val yourDividendsText = "Your dividend voucher will tell you the shares you have in the company and the amount of the dividend you got."
    val expectedErrorText = "Select yes if you got dividends from UK-based companies"
  }

  object AgentExpectedWelsh extends ExpectedResultsUserType {
    val expectedH1 = "Did your client get dividends from UK-based companies?"
    val expectedTitle = "Did your client get dividends from UK-based companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val yourDividendsText = "Your client’s dividend voucher will tell you the shares they have in the company and the amount of the dividend they got."
    val expectedErrorText = "Select yes if your client got dividends from UK-based companies"
  }

  object AllExpectedWelsh extends ExpectedResultsAllUsers {
    val captionExpected = s"Dividends for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesNo: Boolean => String = isYes => if (isYes) "Yes" else "No"
    val continueText = "Continue"
    val continueLink = s"/income-through-software/return/personal-income/$taxYear/dividends/dividends-from-uk-companies"
    val errorSummaryHref = "#value"
  }

  val userScenarios =
    Seq(UserScenario(isWelsh = false, isAgent = false, Some(IndividualExpectedEnglish), AllExpectedEnglish),
      UserScenario(isWelsh = false, isAgent = true, Some(AgentExpectedEnglish), AllExpectedEnglish),
      UserScenario(isWelsh = true, isAgent = false, Some(IndividualExpectedWelsh), AllExpectedWelsh),
      UserScenario(isWelsh = true, isAgent = true, Some(AgentExpectedWelsh), AllExpectedWelsh))

  ".show" when {
    userScenarios.foreach { us =>
      s"language is ${printLang(us.isWelsh)} and request is from an ${printAgent(us.isAgent)}" should {

          "return the uk dividends page when there is no priorSubmission data and no cyaData in session" which {
            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(us.isAgent)
              urlGet(receiveUkDividendsUrl, us.isWelsh, headers = playSessionCookies(taxYear))
            }

            "has an OK(200) status" in {
              result.status shouldBe OK
            }

            implicit val document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(us.expectedResultsUserType.get.expectedTitle)
            h1Check(s"${us.expectedResultsUserType.get.expectedH1} ${us.expectedResultsAllUsers.captionExpected}")
            textOnPageCheck(us.expectedResultsUserType.get.yourDividendsText, Selectors.yourDividendsSelector)
            radioButtonCheck(us.expectedResultsAllUsers.yesNo(true), 1)
            radioButtonCheck(us.expectedResultsAllUsers.yesNo(false), 2)
            buttonCheck(us.expectedResultsAllUsers.continueText, Selectors.continueSelector)
            formPostLinkCheck(us.expectedResultsAllUsers.continueLink, Selectors.continueButtonFormSelector)

            welshToggleCheck(us.isWelsh)
          }

          "return the uk dividends page when there is cyaData in session" which {
            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(us.isAgent)
              urlGet(receiveUkDividendsUrl, us.isWelsh, headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(cyaModel)))
            }

            "has an OK(200) status" in {
              result.status shouldBe OK
            }

            implicit val document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(us.expectedResultsUserType.get.expectedTitle)
            h1Check(s"${us.expectedResultsUserType.get.expectedH1} ${us.expectedResultsAllUsers.captionExpected}")
            textOnPageCheck(us.expectedResultsUserType.get.yourDividendsText, Selectors.yourDividendsSelector)
            radioButtonCheck(us.expectedResultsAllUsers.yesNo(true), 1)
            radioButtonCheck(us.expectedResultsAllUsers.yesNo(false), 2)
            buttonCheck(us.expectedResultsAllUsers.continueText, Selectors.continueSelector)
            formPostLinkCheck(us.expectedResultsAllUsers.continueLink, Selectors.continueButtonFormSelector)

            welshToggleCheck(us.isWelsh)
          }

          "return an action when auth call fails" which {
            lazy val result: WSResponse = {
              if (us.isAgent) {
                authoriseAgentUnauthorized()
              }
              else {
                authoriseIndividualUnauthorized()
              }
              urlGet(receiveUkDividendsUrl, us.isWelsh, headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(cyaModel)))
            }
            "has an UNAUTHORIZED(401) status" in {
              result.status shouldBe UNAUTHORIZED
            }
          }
        }
    }
  }

  ".submit" when {
    userScenarios.foreach { us =>
      s"language is ${printLang(us.isWelsh)} and request is from an ${printAgent(us.isAgent)}" should {

        lazy val result: WSResponse = {
          authoriseAgentOrIndividual(us.isAgent)
          urlPost(receiveUkDividendsUrl, us.isWelsh, follow = false, headers = playSessionCookies(taxYear), postRequest = Map[String, String]())
        }

        "return a BadRequest(400) status" in {
          result.status shouldBe BAD_REQUEST
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(us.expectedResultsUserType.get.expectedErrorTitle)
        h1Check(s"${us.expectedResultsUserType.get.expectedH1} ${us.expectedResultsAllUsers.captionExpected}")
        errorSummaryCheck(us.expectedResultsUserType.get.expectedErrorText, us.expectedResultsAllUsers.errorSummaryHref)
        textOnPageCheck(us.expectedResultsUserType.get.yourDividendsText, Selectors.yourDividendsSelector)
        radioButtonCheck(us.expectedResultsAllUsers.yesNo(true), 1)
        radioButtonCheck(us.expectedResultsAllUsers.yesNo(false), 2)
        buttonCheck(us.expectedResultsAllUsers.continueText, Selectors.continueSelector)
        formPostLinkCheck(us.expectedResultsAllUsers.continueLink, Selectors.continueButtonFormSelector)

        welshToggleCheck(us.isWelsh)
      }
    }
  }

  ".submit" should  {

    s"return an Redirect($SEE_OTHER) status" when {

      "there is no CYA data in session" in {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(
            wsClient.url(receiveUkDividendsUrl)
              .withFollowRedirects(false)
              .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
          )
        }

        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION) shouldBe Some(UkDividendsAmountController.show(taxYear).url)
      }

      "there is form data and answer to question is 'No'" in {
        lazy val result: WSResponse = {

          authoriseIndividual()
          await(
            wsClient.url(receiveUkDividendsUrl)
              .withFollowRedirects(false)
              .withHttpHeaders("Csrf-Token" -> "nocheck")
              .post(Map(YesNoForm.yesNo -> YesNoForm.no))
          )
        }

        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION) shouldBe Some(ReceiveOtherUkDividendsController.show(taxYear).url)
      }

      "there is form data and answer to question is 'No' and the cyaModel is completed" in {
        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.DIVIDENDS_CYA ->
              DividendsCheckYourAnswersModel(ukDividends = Some(false), ukDividendsAmount = None, Some(false), None).asJsonString
          ))

          authoriseIndividual()
          await(
            wsClient.url(receiveUkDividendsUrl)
              .withFollowRedirects(false)
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map(YesNoForm.yesNo -> YesNoForm.no))
          )
        }

        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION) shouldBe Some(DividendsCYAController.show(taxYear).url)
      }
    }
  }

}