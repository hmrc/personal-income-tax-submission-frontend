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
import helpers.PlaySessionCookieBaker
import models.dividends.{DividendsCheckYourAnswersModel, DividendsPriorSubmission}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{DividendsDatabaseHelper, IntegrationTest, ViewHelpers}

class OtherUkDividendsAmountControllerISpec extends IntegrationTest with ViewHelpers with DividendsDatabaseHelper {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  val taxYear: Int = 2022
  val taxYearMinusOne: Int = taxYear - 1
  val amount: BigDecimal = 500
  val otherUkDividendsAmountUrl = s"$startUrl/$taxYear/dividends/how-much-dividends-from-uk-trusts-and-open-ended-investment-companies"

  object IndividualExpected {
    val expectedH1 = "How much did you get in dividends from trusts and open-ended investment companies based in the UK?"
    val expectedTitle = "How much did you get in dividends from trusts and open-ended investment companies based in the UK?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val tellUsTheValue = "Tell us the value of the dividends you got, in pounds. You can find this information in your dividend voucher."
    val youToldUsPriorText = s"You told us you got £$amount in dividends from UK-based trusts and open-ended investment companies this year. Tell us if this has changed."
    val expectedErrorEmpty = "Enter how much you got in dividends from trusts and open-ended investment companies"
    val expectedErrorInvalid = "Enter how much you got in dividends in the correct format"
    val expectedErrorOverMax = "The amount of dividends from trusts and open-ended investment companies based in the UK must be less than £100,000,000,000"

    val expectedH1Cy = "How much did you get in dividends from trusts and open-ended investment companies based in the UK?"
    val expectedTitleCy = "How much did you get in dividends from trusts and open-ended investment companies based in the UK?"
    val expectedErrorTitleCy = s"Error: $expectedTitle"
    val tellUsTheValueCy = "Tell us the value of the dividends you got, in pounds. You can find this information in your dividend voucher."
    val youToldUsPriorTextCy = s"You told us you got £$amount in dividends from UK-based trusts and open-ended investment companies this year. Tell us if this has changed."
    val expectedErrorEmptyCy = "Enter how much you got in dividends from trusts and open-ended investment companies"
    val expectedErrorInvalidCy = "Enter how much you got in dividends in the correct format"
    val expectedErrorOverMaxCy = "The amount of dividends from trusts and open-ended investment companies based in the UK must be less than £100,000,000,000"
  }

  object AgentExpected {
    val expectedH1 = "How much did your client get in dividends from trusts and open-ended investment companies based in the UK?"
    val expectedTitle = "How much did your client get in dividends from trusts and open-ended investment companies based in the UK?"
    val expectedErrorTitleAgent = s"Error: $expectedTitle"
    val tellUsTheValue = "Tell us the value of the dividends your client got, in pounds. You can find this information in their dividend voucher."
    val youToldUsPriorText = s"You told us your client got £$amount in dividends from UK-based trusts and open-ended investment companies this year. Tell us if this has changed."
    val expectedErrorEmpty = "Enter how much your client got in dividends from trusts and open-ended investment companies"
    val expectedErrorInvalid = "Enter how much your client got in dividends in the correct format"
    val expectedErrorOverMax = "The amount of dividends from trusts and open-ended investment companies based in the UK must be less than £100,000,000,000"

    val expectedH1Cy = "How much did your client get in dividends from trusts and open-ended investment companies based in the UK?"
    val expectedTitleCy = "How much did your client get in dividends from trusts and open-ended investment companies based in the UK?"
    val expectedErrorTitleAgentCy = s"Error: $expectedTitle"
    val tellUsTheValueCy = "Tell us the value of the dividends your client got, in pounds. You can find this information in their dividend voucher."
    val youToldUsPriorTextCy = s"You told us your client got £$amount in dividends from UK-based trusts and open-ended investment companies this year. Tell us if this has changed."
    val expectedErrorEmptyCy = "Enter how much your client got in dividends from trusts and open-ended investment companies"
    val expectedErrorInvalidCy = "Enter how much your client got in dividends in the correct format"
    val expectedErrorOverMaxCy = "The amount of dividends from trusts and open-ended investment companies based in the UK must be less than £100,000,000,000"
  }

  val poundPrefixSelector = ".govuk-input__prefix"
  val captionSelector = ".govuk-caption-l"
  val inputSelector = ".govuk-input"
  val continueButtonSelector = "#continue"
  val continueButtonFormSelector = "#main-content > div > div > form"
  val enterAmountSelector = "#amount"
  val youToldUsSelector = "#main-content > div > div > form > div > label > p"
  val tellUsTheValueSelector = "#main-content > div > div > form > div > label > p"
  val expectedErrorLink = "#amount"

  val expectedCaption = s"Dividends for 6 April $taxYearMinusOne to 5 April $taxYear"
  val expectedHintText = "For example, £600 or £193.54"
  val poundPrefixText = "£"
  val differentAmountText = "A different amount"
  val continueText = "Continue"

  val expectedCaptionCy = s"Dividends for 6 April $taxYearMinusOne to 5 April $taxYear"
  val expectedHintTextCy = "For example, £600 or £193.54"
  val continueTextCy = "Continue"

  val continueLink = s"/income-through-software/return/personal-income/$taxYear/dividends/how-much-dividends-from-uk-trusts-and-open-ended-investment-companies"
  val newAmountInput = "#amount"
  val amountInputName = "amount"

  val xSessionId: (String, String) = "X-Session-ID" -> sessionId
  val csrfContent: (String, String) = "Csrf-Token" -> "nocheck"

  "as an individual" when {
    import IndividualExpected._
    ".show" should {

      "redirects user to overview page when there is no data in session" which {
        lazy val result: WSResponse = {
          dropDividendsDB()

          authoriseIndividual(Some(nino))
          userDataStub(IncomeSourcesModel(), nino, taxYear)
          stubGet(s"/income-through-software/return/$taxYear/view", SEE_OTHER, "overview page content")
          await(
            wsClient.url(otherUkDividendsAmountUrl)
              .withHttpHeaders(
                "X-Session-ID" -> sessionId,
                "Csrf-Token" -> "nocheck"
              )
              .withFollowRedirects(false).get()
          )
        }

        "has a  SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
        }
      }

      "returns an action when there is cya data in session with correct content" which {

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        lazy val result: WSResponse = {
          dropDividendsDB()

          authoriseIndividual(Some(nino))

          insertCyaData(Some(DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = None)))
          userDataStub(IncomeSourcesModel(), nino, taxYear)

          await(
            wsClient.url(otherUkDividendsAmountUrl)
              .withHttpHeaders(
                "X-Session-ID" -> sessionId,
                HeaderNames.COOKIE -> sessionCookie,
                "Csrf-Token" -> "nocheck"
              ).get()
          )
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedTitle)
        welshToggleCheck("English")
        h1Check(expectedH1 + " " + expectedCaption)
        textOnPageCheck(expectedCaption, captionSelector)
        textOnPageCheck(tellUsTheValue, tellUsTheValueSelector)
        hintTextCheck(expectedHintText)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldCheck(amountInputName, inputSelector)
        buttonCheck(continueText, continueButtonSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }

      "returns an action when there is cya data in session with correct prior data content" which {

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        lazy val result: WSResponse = {
          dropDividendsDB()
          authoriseIndividual(Some(nino))

          insertCyaData(Some(DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount))))
          userDataStub(IncomeSourcesModel(Some(DividendsPriorSubmission(
            Some(amount)
          ))), nino, taxYear)

          await(
            wsClient.url(otherUkDividendsAmountUrl)
              .withHttpHeaders(
                "X-Session-ID" -> sessionId,
                HeaderNames.COOKIE -> sessionCookie,
                "Csrf-Token" -> "nocheck"
              )
              .get()
          )
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedTitle)
        welshToggleCheck("English")
        h1Check(expectedH1 + " " + expectedCaption)
        textOnPageCheck(expectedCaption, captionSelector)
        textOnPageCheck(youToldUsPriorText, youToldUsSelector)
        hintTextCheck(expectedHintText)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldCheck(amountInputName, inputSelector)
        buttonCheck(continueText, continueButtonSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
        inputFieldValueCheck(amount.toString(), "#amount")
      }

      "returns an action when there is cya data in session with correct content - Welsh" which {

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        lazy val result: WSResponse = {
          dropDividendsDB()

          authoriseIndividual(Some(nino))

          insertCyaData(Some(DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = None)))
          userDataStub(IncomeSourcesModel(), nino, taxYear)

          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(
              "X-Session-ID" -> sessionId,
              HeaderNames.COOKIE -> sessionCookie,
              HeaderNames.ACCEPT_LANGUAGE -> "cy",
              "Csrf-Token" -> "nocheck"
            )
            .get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedTitleCy)
        welshToggleCheck("Welsh")
        h1Check(expectedH1Cy + " " + expectedCaptionCy)
        textOnPageCheck(expectedCaptionCy, captionSelector)
        textOnPageCheck(tellUsTheValueCy, tellUsTheValueSelector)
        hintTextCheck(expectedHintTextCy)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldCheck(amountInputName, inputSelector)
        buttonCheck(continueTextCy, continueButtonSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }

      "returns an action when there is cya data in session with correct prior data content - Welsh" which {

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        lazy val result: WSResponse = {
          dropDividendsDB()
          authoriseIndividual()

          insertCyaData(Some(DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount))))
          userDataStub(IncomeSourcesModel(Some(DividendsPriorSubmission(
            Some(amount)
          ))), nino, taxYear)

          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(xSessionId, HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", "Csrf-Token" -> "nocheck").get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedTitleCy)
        welshToggleCheck("Welsh")
        h1Check(expectedH1Cy + " " + expectedCaptionCy)
        textOnPageCheck(expectedCaptionCy, captionSelector)
        textOnPageCheck(youToldUsPriorTextCy, youToldUsSelector)
        hintTextCheck(expectedHintTextCy)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldCheck(amountInputName, inputSelector)
        buttonCheck(continueTextCy, continueButtonSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
        inputFieldValueCheck(amount.toString(), "#amount")
      }

      "returns an action when there is prior submissions data and cya data in session and the amounts are different" which {

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        lazy val result: WSResponse = {
          dropDividendsDB()
          authoriseIndividual(Some(nino))

          insertCyaData(Some(DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount))))
          userDataStub(IncomeSourcesModel(
            Some(DividendsPriorSubmission(otherUkDividends = Some(1)))
          ), nino, taxYear)

          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(
              xSessionId,
              HeaderNames.COOKIE -> sessionCookie,
              "Csrf-Token" -> "nocheck"
            ).get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        inputFieldValueCheck(amount.toString(), "#amount")
      }

      "returns an action when there is priorSubmissionData and cyaData in session and the amounts are equal" which {
        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount)).asJsonString,
          SessionValues.DIVIDENDS_PRIOR_SUB -> DividendsPriorSubmission(otherUkDividends = Some(amount)).asJsonString
        ))

        lazy val result: WSResponse = {
          dropDividendsDB()

          userDataStub(
            IncomeSourcesModel(Some(DividendsPriorSubmission(otherUkDividends = Some(amount)))),
            nino, taxYear
          )
          insertCyaData(Some(DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount))))

          authoriseIndividual()
          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(
              "X-Session-ID" -> sessionId,
              HeaderNames.COOKIE -> sessionCookie,
              "Csrf-Token" -> "nocheck"
            ).get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        inputFieldValueCheck("", "#amount")
      }

      "returns an action when there is cyaData but no priorSubmission data in session" which {
        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount)).asJsonString,
        ))

        lazy val result: WSResponse = {
          dropDividendsDB()

          userDataStub(IncomeSourcesModel(), nino, taxYear)
          insertCyaData(Some(DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount))))

          authoriseIndividual(Some(nino))
          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(xSessionId, HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        inputFieldValueCheck(amount.toString(), "#amount")
      }

      "return unauthorized when the authorization fails" which {
        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount)).asJsonString
        ))
        lazy val result: WSResponse = {
          authoriseIndividualUnauthorized()
          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }

        s"has an UNAUTHORIZED($UNAUTHORIZED) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

    ".submit" should {

      s"return an OK($OK) status" in {
        lazy val result: WSResponse = {
          dropDividendsDB()

          emptyUserDataStub(nino, taxYear)
          insertCyaData(Some(
            DividendsCheckYourAnswersModel(otherUkDividends = Some(true))
          ))

          authoriseIndividual(Some(nino))
          await(
            wsClient.url(otherUkDividendsAmountUrl)
              .withHttpHeaders(xSessionId, csrfContent)
              .post(Map("amount" -> "123"))
          )
        }

        result.status shouldBe OK
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with an Empty Error" which {
        lazy val result: WSResponse = {
          dropDividendsDB()

          emptyUserDataStub(nino, taxYear)
          insertCyaData(Some(
            DividendsCheckYourAnswersModel(otherUkDividends = Some(true))
          ))

          authoriseIndividual(Some(nino))
          await(wsClient.url(otherUkDividendsAmountUrl).withHttpHeaders(xSessionId, csrfContent).post(Map[String, String]()))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)
        errorSummaryCheck(expectedErrorEmpty, expectedErrorLink)
        errorAboveElementCheck(expectedErrorEmpty)
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with an Invalid Error" which {
        lazy val result: WSResponse = {
          dropDividendsDB()

          emptyUserDataStub(nino, taxYear)
          insertCyaData(Some(
            DividendsCheckYourAnswersModel(otherUkDividends = Some(true))
          ))

          authoriseIndividual(Some(nino))
          await(wsClient.url(otherUkDividendsAmountUrl).withHttpHeaders(xSessionId, csrfContent).post(Map("amount" -> "|")))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)
        errorSummaryCheck(expectedErrorInvalid, expectedErrorLink)
        errorAboveElementCheck(expectedErrorInvalid)
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with an OverMax Error" which {
        lazy val result: WSResponse = {
          dropDividendsDB()

          emptyUserDataStub(nino, taxYear)
          insertCyaData(Some(
            DividendsCheckYourAnswersModel(otherUkDividends = Some(true))
          ))

          authoriseIndividual()
          await(wsClient.url(otherUkDividendsAmountUrl).withHttpHeaders(xSessionId, csrfContent).post(Map("amount" -> "9999999999999999999999999999")))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)
        errorSummaryCheck(expectedErrorOverMax, expectedErrorLink)
        errorAboveElementCheck(expectedErrorOverMax)
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with an Empty Error - Welsh" which {
        lazy val result: WSResponse = {
          dropDividendsDB()

          emptyUserDataStub()
          insertCyaData(Some(
            DividendsCheckYourAnswersModel(otherUkDividends = Some(true))
          ))

          authoriseIndividual(Some(nino))
          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(
              HeaderNames.ACCEPT_LANGUAGE -> "cy",
              xSessionId,
              csrfContent
            )
            .post(Map[String, String]()))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)
        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorEmptyCy, expectedErrorLink)
        errorAboveElementCheck(expectedErrorEmptyCy)
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with an Invalid Error - Welsh" which {
        lazy val result: WSResponse = {
          dropDividendsDB()

          emptyUserDataStub()
          insertCyaData(Some(
            DividendsCheckYourAnswersModel(otherUkDividends = Some(true))
          ))

          authoriseIndividual(Some(nino))
          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(
              HeaderNames.ACCEPT_LANGUAGE -> "cy",
              xSessionId,
              csrfContent
            )
            .post(Map("amount" -> "|")))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)
        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorInvalidCy, expectedErrorLink)
        errorAboveElementCheck(expectedErrorInvalidCy)
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with an OverMax Error - Welsh" which {
        lazy val result: WSResponse = {
          dropDividendsDB()

          emptyUserDataStub()
          insertCyaData(Some(
            DividendsCheckYourAnswersModel(otherUkDividends = Some(true))
          ))

          authoriseIndividual(Some(nino))
          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(
              HeaderNames.ACCEPT_LANGUAGE -> "cy",
              xSessionId,
              csrfContent
            )
            .post(Map("amount" -> "9999999999999999999999999999")))
        }

        "has the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)
        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorOverMaxCy, expectedErrorLink)
        errorAboveElementCheck(expectedErrorOverMaxCy)
      }

      "return unauthorized when the authorization fails" which {
        lazy val result: WSResponse = {
          authoriseIndividualUnauthorized()
          await(wsClient.url(otherUkDividendsAmountUrl)
            .post(Map[String, String]()))
        }

        s"has an UNAUTHORIZED($UNAUTHORIZED) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

  }

  "as an agent" when {
    import AgentExpected._
    ".show" should {

      "returns an action" which {
        lazy val result: WSResponse = {
          dropDividendsDB()

          emptyUserDataStub()
          insertCyaData(Some(
            DividendsCheckYourAnswersModel(otherUkDividends = Some(true))
          ))

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
            .get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }
      }

      "returns an action when there is data in session with correct content" which {
        lazy val result: WSResponse = {
          dropDividendsDB()

          emptyUserDataStub()
          insertCyaData(Some(
            DividendsCheckYourAnswersModel(otherUkDividends = Some(true))
          ))

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
            .get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedTitle)
        welshToggleCheck("English")
        h1Check(expectedH1 + " " + expectedCaption)
        textOnPageCheck(expectedCaption, captionSelector)
        textOnPageCheck(tellUsTheValue, tellUsTheValueSelector)
        hintTextCheck(expectedHintText)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldCheck(amountInputName, inputSelector)
        buttonCheck(continueText, continueButtonSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }
      "returns an action when there is data in session with correct prior data content" which {
        lazy val result: WSResponse = {
          dropDividendsDB()

          userDataStub(IncomeSourcesModel(
            Some(DividendsPriorSubmission())),
            nino, taxYear
          )
          insertCyaData(Some(DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount))))

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
            .get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedTitle)
        welshToggleCheck("English")
        h1Check(expectedH1 + " " + expectedCaption)
        textOnPageCheck(expectedCaption, captionSelector)
        textOnPageCheck(youToldUsPriorText, youToldUsSelector)
        hintTextCheck(expectedHintText)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldCheck(amountInputName, inputSelector)
        buttonCheck(continueText, continueButtonSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }

      "returns an action when there is data in session with correct content - Welsh" which {
        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = None).asJsonString
          ))

          authoriseAgent()
          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy")
            .get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedTitleCy)
        welshToggleCheck("Welsh")
        h1Check(expectedH1Cy + " " + expectedCaptionCy)
        textOnPageCheck(expectedCaptionCy, captionSelector)
        textOnPageCheck(tellUsTheValueCy, tellUsTheValueSelector)
        hintTextCheck(expectedHintTextCy)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldCheck(amountInputName, inputSelector)
        buttonCheck(continueTextCy, continueButtonSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }

      "returns an action when there is data in session with correct prior data content - Welsh" which {
        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount)).asJsonString
          ))

          authoriseAgent()
          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy")
            .get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedTitleCy)
        welshToggleCheck("Welsh")
        h1Check(expectedH1Cy + " " + expectedCaptionCy)
        textOnPageCheck(expectedCaptionCy, captionSelector)
        textOnPageCheck(youToldUsPriorTextCy, youToldUsSelector)
        hintTextCheck(expectedHintTextCy)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldCheck(amountInputName, inputSelector)
        buttonCheck(continueTextCy, continueButtonSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }

      "return unauthorized when the authorization fails" which {
        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount)).asJsonString
        ))
        lazy val result: WSResponse = {
          authoriseAgentUnauthorized()
          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie)
            .get())
        }

        s"has an UNAUTHORIZED($UNAUTHORIZED) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }
    }

    ".submit" should {

      s"return an OK($OK) status" when {

        "there is form data" in {
          lazy val result: WSResponse = {
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A",
              SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true)).asJsonString
            ))

            authoriseAgent()
            await(
              wsClient.url(otherUkDividendsAmountUrl)
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
                .post(Map("amount" -> "123"))
            )
          }

          result.status shouldBe OK
        }
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with an Empty Error" which {

        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post(Map[String, String]()))
        }

        "have the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)
        errorSummaryCheck(expectedErrorEmpty, expectedErrorLink)
        errorAboveElementCheck(expectedErrorEmpty)

      }
      s"return a BAD_REQUEST($BAD_REQUEST) status with an Inavlid Error" which {

        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post(Map("amount" -> "|")))
        }

        "have the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)
        errorSummaryCheck(expectedErrorInvalid, expectedErrorLink)
        errorAboveElementCheck(expectedErrorInvalid)

      }
      s"return a BAD_REQUEST($BAD_REQUEST) status with an OverMax Error" which {

        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post(Map("amount" -> "999999999999999999999999999999999")))
        }

        "have the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)
        errorSummaryCheck(expectedErrorOverMax, expectedErrorLink)
        errorAboveElementCheck(expectedErrorOverMax)

      }
      s"return a BAD_REQUEST($BAD_REQUEST) status with an Empty Error - Welsh" which {

        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", "Csrf-Token" -> "nocheck")
            .post(Map[String, String]()))
        }

        "have the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)
        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorEmptyCy, expectedErrorLink)
        errorAboveElementCheck(expectedErrorEmptyCy)

      }
      s"return a BAD_REQUEST($BAD_REQUEST) status with an Inavlid Error - Welsh" which {

        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", "Csrf-Token" -> "nocheck")
            .post(Map("amount" -> "|")))
        }

        "have the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)
        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorInvalidCy, expectedErrorLink)
        errorAboveElementCheck(expectedErrorInvalidCy)

      }
      s"return a BAD_REQUEST($BAD_REQUEST) status with an OverMax Error - Welsh" which {

        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", "Csrf-Token" -> "nocheck")
            .post(Map("amount" -> "999999999999999999999999999999999")))
        }

        "have the correct status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)
        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorOverMaxCy, expectedErrorLink)
        errorAboveElementCheck(expectedErrorOverMaxCy)

      }

      "return unauthorized when the authorization fails" which {
        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount)).asJsonString,
          SessionValues.CLIENT_NINO -> "AA123456A"
        ))
        lazy val result: WSResponse = {
          authoriseAgentUnauthorized()
          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post(Map[String, String]()))
        }

        s"has an UNAUTHORIZED($UNAUTHORIZED) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

  }

}

