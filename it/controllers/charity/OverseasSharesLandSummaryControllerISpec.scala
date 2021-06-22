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

package controllers.charity

import common.SessionValues
import forms.YesNoForm
import helpers.PlaySessionCookieBaker
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{OK, UNAUTHORIZED}
import play.api.libs.ws.WSClient
import utils.{IntegrationTest, ViewHelpers}

class OverseasSharesLandSummaryControllerISpec  extends IntegrationTest with ViewHelpers {

  object Selectors {
    val question = ".govuk-fieldset__legend"
    val errorSummary = "#error-summary-title"
    val noSelectionError = ".govuk-error-summary__body > ul > li > a"
    val errorMessage = "#value-error"
  }

  object Content {
    val headingIndividualSingle = "Overseas charity you donated shares, securities, land or property to"
    val headingIndividualMultiple = "Overseas charities you donated shares, securities, land or property to"
    val headingAgentSingle = "Overseas charity your client donated shares, securities, land or property to"
    val headingAgentMultiple = "Overseas charities your client donated shares, securities, land or property to"
    val caption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val charity1 = "overseasCharity1"
    val charity2 = "overseasCharity2"
    val question = "Do you need to add another overseas charity?"
    val hintIndividual = "You must tell us about all the overseas charities you donated shares, securities, land or property to."
    val hintAgent = "You must tell us about all the overseas charities your client donated shares, securities, land or property to."
    val change = "Change"
    val remove = "Remove"
    val hiddenChange1 = "Change the details you’ve entered for overseasCharity1."
    val hiddenRemove1 = "Remove overseasCharity1."
    val hiddenChange2 = "Change the details you’ve entered for overseasCharity2."
    val hiddenRemove2 = "Remove overseasCharity2."
    val yes = "Yes"
    val no = "No"
    val errorSummary = "There is a problem"
    val noSelectionError = "Select yes if you need to add another overseas charity"
    val button = "Continue"
  }

  object WelshContent {
    val headingIndividualSingle = "Overseas charity you donated shares, securities, land or property to"
    val headingIndividualMultiple = "Overseas charities you donated shares, securities, land or property to"
    val headingAgentSingle = "Overseas charity your client donated shares, securities, land or property to"
    val headingAgentMultiple = "Overseas charities your client donated shares, securities, land or property to"
    val caption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val charity1 = "overseasCharity1"
    val charity2 = "overseasCharity2"
    val question = "Do you need to add another overseas charity?"
    val hintIndividual = "You must tell us about all the overseas charities you donated shares, securities, land or property to."
    val hintAgent = "You must tell us about all the overseas charities your client donated shares, securities, land or property to."
    val change = "Change"
    val remove = "Remove"
    val hiddenChange1 = "Change the details you’ve entered for overseasCharity1."
    val hiddenRemove1 = "Remove overseasCharity1."
    val hiddenChange2 = "Change the details you’ve entered for overseasCharity2."
    val hiddenRemove2 = "Remove overseasCharity2."
    val yes = "Yes"
    val no = "No"
    val errorSummary = "There is a problem"
    val noSelectionError = "Select yes if you need to add another overseas charity"
    val button = "Continue"
  }

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  val taxYear: Int = 2022

  val overseasSharesLandSummaryUrl = s"$startUrl/$taxYear/charity/overseas-charities-donated-shares-securities-land-or-property-to"

  "Calling GET /charity/overseas-charities-donated-shares-securities-land-or-property-to" when {

    "the user is authorised" when {

      "the user is a non-agent" should {

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(overseasSharesLandSummaryUrl)
            .withHttpHeaders(xSessionId, csrfContent)
            .get())
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return the page" which {
          titleCheck(Content.headingIndividualSingle)
          h1Check(s"${Content.headingIndividualSingle} ${Content.caption}")
          captionCheck(Content.caption)
          taskListCheck(Seq((Content.charity1, Content.hiddenChange1, Content.hiddenRemove1)))
          textOnPageCheck(Content.question, Selectors.question)
          hintTextCheck(Content.hintIndividual)
          buttonCheck(Content.button)

          elementExtinct(Selectors.errorSummary)
          elementExtinct(Selectors.noSelectionError)
          elementExtinct(Selectors.errorMessage)
        }

        s"have an OK($OK) status" in {
          result.status shouldBe OK
        }
      }

      "the user is an agent" should {

        lazy val result = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(overseasSharesLandSummaryUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
            .get())
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return the page" which {
          titleCheck(Content.headingAgentSingle)
          h1Check(s"${Content.headingAgentSingle} ${Content.caption}")
          captionCheck(Content.caption)
          taskListCheck(Seq((Content.charity1, Content.hiddenChange1, Content.hiddenRemove1)))
          textOnPageCheck(Content.question, Selectors.question)
          hintTextCheck(Content.hintAgent)
          buttonCheck(Content.button)

          elementExtinct(Selectors.errorSummary)
          elementExtinct(Selectors.noSelectionError)
          elementExtinct(Selectors.errorMessage)
        }

        s"have an OK($OK) status" in {
          result.status shouldBe OK
        }
      }
    }

    "the user is authorised with welsh selected" when {

      "the user is a non-agent" should {

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(overseasSharesLandSummaryUrl).withHttpHeaders(
            HeaderNames.ACCEPT_LANGUAGE -> "cy",
            xSessionId, csrfContent
          ).get())
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return the page" which {
          titleCheck(WelshContent.headingIndividualSingle)
          h1Check(s"${WelshContent.headingIndividualSingle} ${WelshContent.caption}")
          captionCheck(WelshContent.caption)
          taskListCheck(Seq((WelshContent.charity1, WelshContent.hiddenChange1, WelshContent.hiddenRemove1)))
          textOnPageCheck(WelshContent.question, Selectors.question)
          hintTextCheck(WelshContent.hintIndividual)
          buttonCheck(WelshContent.button)

          elementExtinct(Selectors.errorSummary)
          elementExtinct(Selectors.noSelectionError)
          elementExtinct(Selectors.errorMessage)
        }

        s"have an OK($OK) status" in {
          result.status shouldBe OK
        }
      }

      "the user is an agent" should {

        lazy val result = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(overseasSharesLandSummaryUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", xSessionId, csrfContent)
            .get())
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return the page" which {
          titleCheck(WelshContent.headingAgentSingle)
          h1Check(s"${WelshContent.headingAgentSingle} ${WelshContent.caption}")
          captionCheck(WelshContent.caption)
          taskListCheck(Seq((WelshContent.charity1, WelshContent.hiddenChange1, WelshContent.hiddenRemove1)))
          textOnPageCheck(WelshContent.question, Selectors.question)
          hintTextCheck(WelshContent.hintAgent)
          buttonCheck(WelshContent.button)

          elementExtinct(Selectors.errorSummary)
          elementExtinct(Selectors.noSelectionError)
          elementExtinct(Selectors.errorMessage)
        }

        s"have an OK($OK) status" in {
          result.status shouldBe OK
        }
      }
    }

    "the user is unauthorized" should {

      lazy val result = {
        authoriseIndividualUnauthorized()
        await(
          wsClient.url(overseasSharesLandSummaryUrl)
            .withHttpHeaders(xSessionId, csrfContent)
            .get()
        )
      }

      s"return an Unauthorised($UNAUTHORIZED) status" in {
        result.status shouldBe UNAUTHORIZED
      }
    }
  }

  "Calling POST /charity/overseas-charities-donated-shares-securities-land-or-property-to" when {

    "the user is an individual" when {

      "return an error" when {

        "the submitted data is empty" which {
          lazy val result = {
            authoriseIndividual()
            await(wsClient.url(overseasSharesLandSummaryUrl)
              .withHttpHeaders(xSessionId, csrfContent)
              .post(Map[String, String](YesNoForm.yesNo -> ""
            )))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck("Error: " + Content.headingIndividualSingle)
          h1Check(s"${Content.headingIndividualSingle} ${Content.caption}")
          radioButtonCheck(Content.yes, 1)
          radioButtonCheck(Content.no, 2)
          hintTextCheck(Content.hintIndividual)
          captionCheck(Content.caption)
          buttonCheck(Content.button)

          errorSummaryCheck(Content.noSelectionError, "#value")
          errorAboveElementCheck(Content.noSelectionError)
        }
      }

      "an option is selected" should {

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(overseasSharesLandSummaryUrl)
            .withHttpHeaders(xSessionId, csrfContent)
            .post(Map(YesNoForm.yesNo -> YesNoForm.no)))
        }

        "return a 200(Ok) status" in {
          result.status shouldBe OK
        }
      }
    }

    "the user is an agent" should {

      "return an error" when {

        "the submitted data is empty" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890"
          ))

          lazy val result = {
            authoriseAgent()
            await(wsClient.url(overseasSharesLandSummaryUrl).withHttpHeaders(
              HeaderNames.COOKIE -> playSessionCookies,
              xSessionId, csrfContent
            ).post(Map[String, String](
              YesNoForm.yesNo -> ""
            )))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck("Error: " + Content.headingAgentSingle)
          h1Check(s"${Content.headingAgentSingle} ${Content.caption}")
          radioButtonCheck(Content.yes, 1)
          radioButtonCheck(Content.no, 2)
          hintTextCheck(Content.hintAgent)
          captionCheck(Content.caption)
          buttonCheck(Content.button)

          errorSummaryCheck(Content.noSelectionError, "#value")
          errorAboveElementCheck(Content.noSelectionError)
        }
      }

      "an option is selected" should {

        lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> "1234567890"
        ))

        lazy val result = {
          authoriseAgent()
          await(wsClient.url(overseasSharesLandSummaryUrl).withHttpHeaders(
            HeaderNames.COOKIE -> playSessionCookies,
            xSessionId, csrfContent
          ).post(Map[String, String](
            YesNoForm.yesNo -> YesNoForm.yes
          )))
        }

        "return a 200(Ok) status" in {
          result.status shouldBe OK
        }
      }
    }

    "welsh is selected" when {

      "the user is an individual" when {

        "return an error" when {

          "the submitted data is empty" which {
            lazy val result = {
              authoriseIndividual()
              await(wsClient.url(overseasSharesLandSummaryUrl)
                .withHttpHeaders(xSessionId, csrfContent)
                .post(Map[String, String](YesNoForm.yesNo -> ""
                )))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck("Error: " + WelshContent.headingIndividualSingle)
            h1Check(s"${WelshContent.headingIndividualSingle} ${WelshContent.caption}")
            radioButtonCheck(WelshContent.yes, 1)
            radioButtonCheck(WelshContent.no, 2)
            hintTextCheck(WelshContent.hintIndividual)
            captionCheck(WelshContent.caption)
            buttonCheck(WelshContent.button)

            errorSummaryCheck(WelshContent.noSelectionError, "#value")
            errorAboveElementCheck(WelshContent.noSelectionError)
          }
        }

        "an option is selected" should {

          lazy val result = {
            authoriseIndividual()
            await(wsClient.url(overseasSharesLandSummaryUrl)
              .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy", xSessionId, csrfContent)
              .post(Map(YesNoForm.yesNo -> YesNoForm.no)))
          }

          "return a 200(Ok) status" in {
            result.status shouldBe OK
          }
        }
      }

      "the user is an agent" should {

        "return an error" when {

          "the submitted data is empty" which {
            lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
              SessionValues.TAX_YEAR -> taxYear.toString,
              SessionValues.CLIENT_NINO -> "AA123456A",
              SessionValues.CLIENT_MTDITID -> "1234567890"
            ))

            lazy val result = {
              authoriseAgent()
              await(wsClient.url(overseasSharesLandSummaryUrl).withHttpHeaders(
                HeaderNames.COOKIE -> playSessionCookies,
                HeaderNames.ACCEPT_LANGUAGE -> "cy",
                xSessionId, csrfContent
              ).post(Map[String, String](
                YesNoForm.yesNo -> ""
              )))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck("Error: " + WelshContent.headingAgentSingle)
            h1Check(s"${WelshContent.headingAgentSingle} ${WelshContent.caption}")
            radioButtonCheck(WelshContent.yes, 1)
            radioButtonCheck(WelshContent.no, 2)
            hintTextCheck(WelshContent.hintAgent)
            captionCheck(WelshContent.caption)
            buttonCheck(WelshContent.button)

            errorSummaryCheck(WelshContent.noSelectionError, "#value")
            errorAboveElementCheck(WelshContent.noSelectionError)
          }
        }

        "an option is selected" should {

          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890"
          ))

          lazy val result = {
            authoriseAgent()
            await(wsClient.url(overseasSharesLandSummaryUrl).withHttpHeaders(
              HeaderNames.COOKIE -> playSessionCookies,
              HeaderNames.ACCEPT_LANGUAGE -> "cy",
              xSessionId, csrfContent
            ).post(Map[String, String](
              YesNoForm.yesNo -> YesNoForm.yes
            )))
          }

          "return a 200(Ok) status" in {
            result.status shouldBe OK
          }
        }
      }
    }
  }
}
