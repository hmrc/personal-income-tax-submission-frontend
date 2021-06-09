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

class TestReceiveUkDividendsControllerISpec extends IntegrationTest with ViewHelpers {

  val taxYear: Int = 2022
  val amount: BigDecimal = 500
  val receiveUkDividendsUrl = s"$startUrl/$taxYear/dividends/dividends-from-uk-companies"

  val cyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount),
    otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount))

  case class SelectorsTry() {
    val yourDividendsSelector = "#main-content > div > div > form > div > fieldset > legend > p"
    val continueSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"

  }

  trait ExpectedResultsLang {
    val expectedH1: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val yourDividendsText: String
    val expectedErrorText: String
  }

  trait ExpectedResultsAll {
    val captionExpected: String
    val yesNo: Boolean => String
    val continueText: String
    val continueLink: String
    val errorSummaryHref: String
  }


  object IndividualExpectedEnglish extends ExpectedResultsLang {
    val expectedH1 = "Did you get dividends from UK-based companies?"
    val expectedTitle = "Did you get dividends from UK-based companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val yourDividendsText = "Your dividend voucher will tell you the shares you have in the company and the amount of the dividend you got."
    val expectedErrorText = "Select yes if you got dividends from UK-based companies"
  }

  object AgentExpectedEnglish extends ExpectedResultsLang {
    val expectedH1 = "Did your client get dividends from UK-based companies?"
    val expectedTitle = "Did your client get dividends from UK-based companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val yourDividendsText = "Your client’s dividend voucher will tell you the shares they have in the company and the amount of the dividend they got."
    val expectedErrorText = "Select yes if your client got dividends from UK-based companies"
  }

  object AllExpectedEnglish extends ExpectedResultsAll {
    val captionExpected = s"Dividends for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesNo: Boolean => String = isYes => if (isYes) "Yes" else "No"
    val continueText = "Continue"
    val continueLink = s"/income-through-software/return/personal-income/$taxYear/dividends/dividends-from-uk-companies"
    val errorSummaryHref = "#value"
  }

  object IndividualExpectedWelsh extends ExpectedResultsLang {
    val expectedH1 = "Did you get dividends from UK-based companies?"
    val expectedTitle = "Did you get dividends from UK-based companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val yourDividendsText = "Your dividend voucher will tell you the shares you have in the company and the amount of the dividend you got."
    val expectedErrorText = "Select yes if you got dividends from UK-based companies"
  }

  object AgentExpectedWelsh extends ExpectedResultsLang {
    val expectedH1 = "Did your client get dividends from UK-based companies?"
    val expectedTitle = "Did your client get dividends from UK-based companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val yourDividendsText = "Your client’s dividend voucher will tell you the shares they have in the company and the amount of the dividend they got."
    val expectedErrorText = "Select yes if your client got dividends from UK-based companies"
  }

  object AllExpectedWelsh extends ExpectedResultsAll {
    val captionExpected = s"Dividends for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesNo: Boolean => String = isYes => if (isYes) "Yes" else "No"
    val continueText = "Continue"
    val continueLink = s"/income-through-software/return/personal-income/$taxYear/dividends/dividends-from-uk-companies"
    val errorSummaryHref = "#value"
  }


  def printLang(isWelsh: Boolean): String = if (isWelsh) "Welsh" else "English"

  def printAgent(isAgent: Boolean): String = if (isAgent) "Agent" else "Individual"

  def welshToggle(isWelsh: Boolean)(implicit document: () => Document): Unit = if (isWelsh) welshToggleCheck(WELSH) else welshToggleCheck(ENGLISH)

  //noinspection ScalaStyle
  def show(isWelsh: Boolean, isAgent: Boolean, resultsExpectedLang: ExpectedResultsLang, resultsExpectedAll:
  ExpectedResultsAll, selectorsTry: SelectorsTry): Unit = {

    ".show" should {

      s"in ${printLang(isWelsh)}" should {

        s"as an ${printAgent(isAgent)}" should {

          "returns the uk dividends page when there is no priorSubmission data and no cyaData in session" which {
            lazy val result: WSResponse = {
              if (isAgent) {
                authoriseAgent()
              } else {
                authoriseIndividual()
              }
              urlGet(receiveUkDividendsUrl, isWelsh, headers = playSessionCookies(taxYear))
            }

            "has an OK(200) status" in {
              result.status shouldBe OK
            }

            implicit val document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(resultsExpectedLang.expectedTitle)
            h1Check(s"${resultsExpectedLang.expectedH1} ${resultsExpectedAll.captionExpected}")
            textOnPageCheck(resultsExpectedLang.yourDividendsText, selectorsTry.yourDividendsSelector)
            radioButtonCheck(resultsExpectedAll.yesNo(true), 1)
            radioButtonCheck(resultsExpectedAll.yesNo(false), 2)
            buttonCheck(resultsExpectedAll.continueText, selectorsTry.continueSelector)
            formPostLinkCheck(resultsExpectedAll.continueLink, selectorsTry.continueButtonFormSelector)

            if (isWelsh) {
              welshToggleCheck(WELSH)
            } else {
              welshToggleCheck(ENGLISH)
            }

          }

          "returns the uk dividends page when there is cyaData in session" which {

            lazy val result: WSResponse = {
              if (isAgent) {
                authoriseAgent()
              } else {
                authoriseIndividual()
              }
              urlGet(receiveUkDividendsUrl, isWelsh, headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(cyaModel)))
            }

            "has an OK(200) status" in {
              result.status shouldBe OK
            }

            implicit val document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(resultsExpectedLang.expectedTitle)
            h1Check(s"${resultsExpectedLang.expectedH1} ${resultsExpectedAll.captionExpected}")
            textOnPageCheck(resultsExpectedLang.yourDividendsText, selectorsTry.yourDividendsSelector)
            radioButtonCheck(resultsExpectedAll.yesNo(true), 1)
            radioButtonCheck(resultsExpectedAll.yesNo(false), 2)
            buttonCheck(resultsExpectedAll.continueText, selectorsTry.continueSelector)
            formPostLinkCheck(resultsExpectedAll.continueLink, selectorsTry.continueButtonFormSelector)

            if (isWelsh) {
              welshToggleCheck(WELSH)
            } else {
              welshToggleCheck(ENGLISH)
            }
          }

          "returns an action when auth call fails" which {
            val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount),
                otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount)).asJsonString
            ))
            lazy val result: WSResponse = {
              if (isAgent) {
                authoriseAgentUnauthorized()
              }
              else {
                authoriseIndividualUnauthorized()
              }
              await(wsClient.url(receiveUkDividendsUrl)
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
            }
            "has an UNAUTHORIZED(401) status" in {
              result.status shouldBe UNAUTHORIZED
            }
          }
        }

      }
    }
  }

  show(false, false, IndividualExpectedEnglish, AllExpectedEnglish, SelectorsTry())
  show(false, true, AgentExpectedEnglish, AllExpectedEnglish, SelectorsTry())
  show(true, false, IndividualExpectedEnglish, AllExpectedWelsh, SelectorsTry())
  show(true, true, AgentExpectedWelsh, AllExpectedWelsh, SelectorsTry())

  def submitView(isWelsh: Boolean, isAgent: Boolean, resultsExpectedLang: ExpectedResultsLang, resultsExpectedAll:
  ExpectedResultsAll, selectorsTry: SelectorsTry): Unit = {

    ".submit" should {

      s"in ${printLang(isWelsh)}" should {

        s"as an ${printAgent(isAgent)}" should {

          lazy val result: WSResponse = {
            if (isAgent) {
              authoriseAgent()
            } else {
              authoriseIndividual()
            }
            urlPost(receiveUkDividendsUrl, isWelsh, postRequest = Map[String, String]())
          }

          "has an OK(400) status" in {
            result.status shouldBe BAD_REQUEST
          }


          implicit val document: () => Document = () => Jsoup.parse(result.body)


          titleCheck(resultsExpectedLang.expectedErrorTitle)
          h1Check(s"${resultsExpectedLang.expectedH1} ${resultsExpectedAll.captionExpected}")
          errorSummaryCheck(resultsExpectedLang.expectedErrorText, resultsExpectedAll.errorSummaryHref)
          textOnPageCheck(resultsExpectedLang.yourDividendsText, selectorsTry.yourDividendsSelector)
          radioButtonCheck(resultsExpectedAll.yesNo(true), 1)
          radioButtonCheck(resultsExpectedAll.yesNo(false), 2)
          buttonCheck(resultsExpectedAll.continueText, selectorsTry.continueSelector)
          formPostLinkCheck(resultsExpectedAll.continueLink, selectorsTry.continueButtonFormSelector)

          welshToggle(isWelsh)(document)
        }
      }
    }
  }


  submitView(false, false, IndividualExpectedEnglish, AllExpectedEnglish, SelectorsTry())
  //submitView(false, true, AgentExpectedEnglish,AllExpectedEnglish,SelectorsTry())

  //noinspection ScalaStyle
  def submitController() {
    ".submit" should {

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
}