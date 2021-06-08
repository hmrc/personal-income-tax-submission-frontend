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
  val receiveUkDividendsUrl = s"${appUrl(port)}/$taxYear/dividends/dividends-from-uk-companies"

  val cyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount),
    otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount))

  object Selectors {
    val yourDividendsSelector = "#main-content > div > div > form > div > fieldset > legend > p"
    val continueSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"

  }

  object ExpectedResults {

    object EnglishLang {
      object IndividualExpected {
        val expectedH1 = "Did you get dividends from UK-based companies?"
        val expectedTitle = "Did you get dividends from UK-based companies?"
        val expectedErrorTitle = s"Error: $expectedTitle"
        val yourDividendsText = "Your dividend voucher will tell you the shares you have in the company and the amount of the dividend you got."
      }

      object AgentExpected {
        val expectedH1 = "Did your client get dividends from UK-based companies?"
        val expectedTitle = "Did your client get dividends from UK-based companies?"
        val expectedErrorTitle = s"Error: $expectedTitle"
        val yourDividendsText = "Your client’s dividend voucher will tell you the shares they have in the company and the amount of the dividend they got."
      }

      object AllExpected {
        val captionExpected = s"Dividends for 6 April ${taxYear - 1} to 5 April $taxYear"
        val yesNo: Boolean => String = isYes => if (isYes) "Yes" else "No"
        val continueText = "Continue"
        val continueLink = s"/income-through-software/return/personal-income/$taxYear/dividends/dividends-from-uk-companies"
      }
    }

    object WelshLang {
      object IndividualExpected {
        val expectedH1 = "Did you get dividends from UK-based companies?"
        val expectedTitle = "Did you get dividends from UK-based companies?"
        val expectedErrorTitle = s"Error: $expectedTitle"
        val yourDividendsText = "Your dividend voucher will tell you the shares you have in the company and the amount of the dividend you got."
      }

      object AgentExpected {
        val expectedH1 = "Did your client get dividends from UK-based companies?"
        val expectedTitle = "Did your client get dividends from UK-based companies?"
        val expectedErrorTitle = s"Error: $expectedTitle"
        val yourDividendsText = "Your client’s dividend voucher will tell you the shares they have in the company and the amount of the dividend they got."
      }

      object AllExpected {
        val captionExpected = s"Dividends for 6 April ${taxYear - 1} to 5 April $taxYear"
        val yesNo: Boolean => String = isYes => if (isYes) "Yes" else "No"
        val continueText = "Continue"
        val continueLink = s"/income-through-software/return/personal-income/$taxYear/dividends/dividends-from-uk-companies"
      }
    }
  }

  ".show" when {

    import Selectors._

    "in English" should {

      import ExpectedResults.EnglishLang._
      import ExpectedResults.EnglishLang.AllExpected._

      "as an Individual" should {

        "returns the uk dividends page when there is no priorSubmission data and no cyaData in session" which {
          lazy val result: WSResponse = {
            authoriseIndividual()
            urlGet(receiveUkDividendsUrl)
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(IndividualExpected.expectedTitle)
          h1Check(s"${IndividualExpected.expectedH1} $captionExpected")
          textOnPageCheck(IndividualExpected.yourDividendsText, yourDividendsSelector)
          radioButtonCheck(yesNo(true), 1)
          radioButtonCheck(yesNo(false), 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)

          welshToggleCheck(ENGLISH)

        }

        "returns the uk dividends page when there is cyaData in session" which {

          lazy val result: WSResponse = {
            authoriseIndividual()
            urlGet(receiveUkDividendsUrl, headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(cyaModel)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(IndividualExpected.expectedTitle)
          h1Check(s"${IndividualExpected.expectedH1} $captionExpected")
          textOnPageCheck(IndividualExpected.yourDividendsText, yourDividendsSelector)
          radioButtonCheck(yesNo(true), 1)
          radioButtonCheck(yesNo(false), 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)

          welshToggleCheck(ENGLISH)
        }

        "returns an action when auth call fails" which {
          val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount),
              otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount)).asJsonString
          ))
          lazy val result: WSResponse = {
            authoriseIndividualUnauthorized()
            await(wsClient.url(receiveUkDividendsUrl)
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
          }
          "has an UNAUTHORIZED(401) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }
      }

      "as an Agent" should {

        "returns the uk dividends page when there is no priorSubmission data and no cyaData in session" which {
          lazy val result: WSResponse = {
            authoriseAgent()
            urlGet(receiveUkDividendsUrl, headers = playSessionCookies(taxYear))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(AgentExpected.expectedTitle)
          h1Check(s"${AgentExpected.expectedH1} $captionExpected")
          textOnPageCheck(AgentExpected.yourDividendsText, yourDividendsSelector)
          radioButtonCheck(yesNo(true), 1)
          radioButtonCheck(yesNo(false), 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)

          welshToggleCheck(ENGLISH)

        }

        "returns the uk dividends page when there is cyaData in session" which {

          lazy val result: WSResponse = {
            authoriseAgent()
            urlGet(receiveUkDividendsUrl, headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(cyaModel)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(AgentExpected.expectedTitle)
          h1Check(s"${AgentExpected.expectedH1} $captionExpected")
          textOnPageCheck(AgentExpected.yourDividendsText, yourDividendsSelector)
          radioButtonCheck(yesNo(true), 1)
          radioButtonCheck(yesNo(false), 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)

          welshToggleCheck(ENGLISH)
        }

        "returns an action when auth call fails" which {
          val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount),
              otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount)).asJsonString
          ))
          lazy val result: WSResponse = {
            authoriseIndividualUnauthorized()
            await(wsClient.url(receiveUkDividendsUrl)
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
          }
          "has an UNAUTHORIZED(401) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }
      }

    }

    "in Welsh" should {

      import ExpectedResults.WelshLang._
      import ExpectedResults.WelshLang.AllExpected._

      "as an Individual" should {

        "returns the uk dividends page when there is no priorSubmission data and no cyaData in session" which {
          lazy val result: WSResponse = {
            authoriseIndividual()
            urlGet(receiveUkDividendsUrl, welsh = true)
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(IndividualExpected.expectedTitle)
          h1Check(s"${IndividualExpected.expectedH1} $captionExpected")
          textOnPageCheck(IndividualExpected.yourDividendsText, yourDividendsSelector)
          radioButtonCheck(yesNo(true), 1)
          radioButtonCheck(yesNo(false), 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)

          welshToggleCheck(WELSH)

        }

        "returns the uk dividends page when there is cyaData in session" which {

          lazy val result: WSResponse = {
            authoriseIndividual()
            urlGet(receiveUkDividendsUrl, welsh = true, headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(cyaModel)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(IndividualExpected.expectedTitle)
          h1Check(s"${IndividualExpected.expectedH1} $captionExpected")
          textOnPageCheck(IndividualExpected.yourDividendsText, yourDividendsSelector)
          radioButtonCheck(yesNo(true), 1)
          radioButtonCheck(yesNo(false), 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)

          welshToggleCheck(WELSH)
        }
      }

      "as an Agent" should {

        "returns the uk dividends page when there is no priorSubmission data and no cyaData in session" which {
          lazy val result: WSResponse = {
            authoriseAgent()
            urlGet(receiveUkDividendsUrl, welsh = true, headers = playSessionCookies(taxYear))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(AgentExpected.expectedTitle)
          h1Check(s"${AgentExpected.expectedH1} $captionExpected")
          textOnPageCheck(AgentExpected.yourDividendsText, yourDividendsSelector)
          radioButtonCheck(yesNo(true), 1)
          radioButtonCheck(yesNo(false), 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)

          welshToggleCheck(WELSH)

        }

        "returns the uk dividends page when there is cyaData in session" which {

          lazy val result: WSResponse = {
            authoriseAgent()
            urlGet(receiveUkDividendsUrl, welsh = true, headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(cyaModel)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(AgentExpected.expectedTitle)
          h1Check(s"${AgentExpected.expectedH1} $captionExpected")
          textOnPageCheck(AgentExpected.yourDividendsText, yourDividendsSelector)
          radioButtonCheck(yesNo(true), 1)
          radioButtonCheck(yesNo(false), 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)

          welshToggleCheck(WELSH)
        }
      }

    }
  }

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

      "there is an invalid with no radio button clicked. Show page with errors" when {

        import Selectors._

        "in English" when {
          import ExpectedResults.EnglishLang.AllExpected._
          import ExpectedResults.EnglishLang._

          lazy val result: WSResponse = {
            urlPost(receiveUkDividendsUrl, postRequest = Map())
          }

          "has an OK(400) status" in {
            result.status shouldBe BAD_REQUEST
          }


          implicit val document: () => Document = () => Jsoup.parse(result.body)

          val expectedErrorText = "Select yes if you got dividends from UK-based companies"
          val errorSummaryHref = "#value"

          titleCheck(IndividualExpected.expectedErrorTitle)
          h1Check(s"${IndividualExpected.expectedH1} $captionExpected")
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(IndividualExpected.yourDividendsText, yourDividendsSelector)
          radioButtonCheck(yesNo(true), 1)
          radioButtonCheck(yesNo(false), 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)

          welshToggleCheck(ENGLISH)
        }

        "in Welsh" when {
          import ExpectedResults.WelshLang.AllExpected._
          import ExpectedResults.WelshLang._

          lazy val result: WSResponse = {
            urlPost(receiveUkDividendsUrl, postRequest = Map())
          }

          "has an OK(400) status" in {
            result.status shouldBe BAD_REQUEST
          }


          implicit val document: () => Document = () => Jsoup.parse(result.body)

          val expectedErrorText = "Select yes if you got dividends from UK-based companies"
          val errorSummaryHref = "#value"

          titleCheck(IndividualExpected.expectedErrorTitle)
          h1Check(s"${IndividualExpected.expectedH1} $captionExpected")
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(IndividualExpected.yourDividendsText, yourDividendsSelector)
          radioButtonCheck(yesNo(true), 1)
          radioButtonCheck(yesNo(false), 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)

          welshToggleCheck(ENGLISH)
        }


      }

    }


    "returns an action when auth call fails" which {
      lazy val result: WSResponse = {
        authoriseIndividualUnauthorized()
        await(wsClient.url(receiveUkDividendsUrl)
          .post(Map[String, String]()))
      }
      "has an UNAUTHORIZED(401) status" in {
        result.status shouldBe UNAUTHORIZED
      }
    }

    "as an Agent" should {

      s"return Redirect($SEE_OTHER) status" when {

        "there is form data and answer to question is 'YES'" in {
          lazy val result: WSResponse = {
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"
            ))

            authoriseAgent()
            await(
              wsClient.url(receiveUkDividendsUrl)
                .withFollowRedirects(false)
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
            )
          }

          result.status shouldBe SEE_OTHER
          result.header(HeaderNames.LOCATION) shouldBe Some(UkDividendsAmountController.show(taxYear).url)
        }
      }

      "there is an invalid with no radio button clicked. Show page with errors" when {

        import Selectors._

        "in English" when {
          import ExpectedResults.EnglishLang.AllExpected._
          import ExpectedResults.EnglishLang._

          lazy val result: WSResponse = {
            authoriseAgent()
            urlPost(receiveUkDividendsUrl, headers = playSessionCookies(taxYear), postRequest = Map())
          }

          "has an OK(400) status" in {
            result.status shouldBe BAD_REQUEST
          }


          implicit val document: () => Document = () => Jsoup.parse(result.body)

          val expectedErrorText = "Select yes if you got dividends from UK-based companies"
          val errorSummaryHref = "#value"

          titleCheck(AgentExpected.expectedErrorTitle)
          h1Check(s"${AgentExpected.expectedH1} $captionExpected")
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(AgentExpected.yourDividendsText, yourDividendsSelector)
          radioButtonCheck(yesNo(true), 1)
          radioButtonCheck(yesNo(false), 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)

          welshToggleCheck(ENGLISH)
        }

        "in Welsh" when {
          import ExpectedResults.WelshLang.AllExpected._
          import ExpectedResults.WelshLang._

          lazy val result: WSResponse = {
            authoriseAgent()
            urlPost(receiveUkDividendsUrl, welsh = true, headers = playSessionCookies(taxYear), postRequest = Map())
          }

          "has an OK(400) status" in {
            result.status shouldBe BAD_REQUEST
          }


          implicit val document: () => Document = () => Jsoup.parse(result.body)

          val expectedErrorText = "Select yes if you got dividends from UK-based companies"
          val errorSummaryHref = "#value"

          titleCheck(AgentExpected.expectedErrorTitle)
          h1Check(s"${AgentExpected.expectedH1} $captionExpected")
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(AgentExpected.yourDividendsText, yourDividendsSelector)
          radioButtonCheck(yesNo(true), 1)
          radioButtonCheck(yesNo(false), 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)

          welshToggleCheck(WELSH)
        }


      }
      "returns an action when auth call fails" when {
        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgentUnauthorized()
          await(wsClient.url(receiveUkDividendsUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post(Map[String, String]()))
        }

        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }
  }


}
