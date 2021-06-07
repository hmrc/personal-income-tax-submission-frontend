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
import play.api.http.Status._
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{IntegrationTest, ViewHelpers}

class RemoveOverseasCharityControllerSharesPropertyISpec extends IntegrationTest with ViewHelpers {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  object Selectors {

    val heading = "h1"
    val caption = ".govuk-caption-l"
    val content = "#main-content > div > div > form > div > fieldset > legend > p"
    val errorSummaryNoSelection = ".govuk-error-summary__body > ul > li > a"
    val yesRadioButton = ".govuk-radios__item:nth-child(1) > label"
    val noRadioButton = ".govuk-radios__item:nth-child(2) > label"

  }

  object Content {

    val charityName = "TestCharity"
    val expectedTitle = s"Are you sure you want to remove $charityName?"
    val expectedErrorTitle = "Select yes to remove this overseas charity"
    val expectedH1 = s"Are you sure you want to remove $charityName?"
    val expectedContent = "This will remove all overseas charities."
    val expectedCaption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val noSelectionError = "Select yes to remove this overseas charity"
    val yesText = "Yes"
    val noText = "No"
    val button = "Continue"
    val errorHref = "#value"

    val charityNameCy = "TestCharity"
    val expectedTitleCy = s"Are you sure you want to remove $charityName?"
    val expectedErrorTitleCy = "Select yes to remove this overseas charity"
    val expectedH1Cy = s"Are you sure you want to remove $charityName?"
    val expectedContentCy = "This will remove all overseas charities."
    val expectedCaptionCy = "Donations to charity for 6 April 2021 to 5 April 2022"
    val noSelectionErrorCy = "Select yes to remove this overseas charity"
    val yesTextCy = "Yes"
    val noTextCy = "No"
    val buttonCy = "Continue"
    val errorHrefCy = "#value"
  }

  val taxYear: Int = 2022

  "as an individual" when {

    ".show" should {

      "return an action with english content" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(
            wsClient
              .url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/remove-overseas-charity-shares-and-property?charityName=TestCharity")
              .withHttpHeaders(xSessionId, csrfContent)
              .get()
          )
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        titleCheck(Content.expectedTitle)
        h1Check(Content.expectedH1 + " " + Content.expectedCaption)
        textOnPageCheck(Content.expectedContent, Selectors.content)
        radioButtonCheck(Content.yesText, 1)
        radioButtonCheck(Content.noText, 2)
        captionCheck(Content.expectedCaption)
        buttonCheck(Content.button)

      }
      "return an action with welsh content" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(
            wsClient
              .url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/remove-overseas-charity-shares-and-property?charityName=TestCharity")
              .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy", xSessionId, csrfContent)
              .get()
          )
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        titleCheck(Content.expectedTitleCy)
        h1Check(Content.expectedH1Cy + " " + Content.expectedCaptionCy)
        textOnPageCheck(Content.expectedContentCy, Selectors.content)
        radioButtonCheck(Content.yesTextCy, 1)
        radioButtonCheck(Content.noTextCy, 2)
        captionCheck(Content.expectedCaptionCy)
        buttonCheck(Content.buttonCy)

      }

      ".submit" should {

        "return an OK(200) status" in {
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(
              wsClient
                .url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/remove-overseas-charity-shares-and-property?charityName=TestCharity")
                .withHttpHeaders(xSessionId, csrfContent)
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
            )
          }
          result.status shouldBe OK
        }

        "when there is no input" should {
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(
              wsClient
                .url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/remove-overseas-charity-shares-and-property?charityName=TestCharity")
                .withHttpHeaders(xSessionId, csrfContent)
                .post(Map[String, String]())
            )
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"return a BAD_REQUEST($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          errorSummaryCheck(Content.expectedErrorTitle, Content.errorHref)
          errorAboveElementCheck(Content.expectedErrorTitle)
        }

        "when there is no input welsh content" should {
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(
              wsClient
                .url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/remove-overseas-charity-shares-and-property?charityName=TestCharity")
                .withHttpHeaders(xSessionId, csrfContent)
                .post(Map[String, String]())
            )
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"return a BAD_REQUEST($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          errorSummaryCheck(Content.expectedErrorTitleCy, Content.errorHrefCy)
          errorAboveElementCheck(Content.expectedErrorTitleCy)
        }
      }
    }
  }

  "as an agent" when {

    ".show" should {

      "returns an action" which {
        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(
            wsClient
              .url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/remove-overseas-charity-shares-and-property?charityName=TestCharity")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
              .get()
          )
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }
      }
    }

    ".submit" should {

      s"return an OK($OK) status" when {

        "there is form data" in {
          lazy val result: WSResponse = {
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"))

            authoriseAgent()
            await(
              wsClient
                .url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/remove-overseas-charity-shares-and-property?charityName=TestCharity")
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
            )
          }

          result.status shouldBe OK
        }
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status" when {

        "there is no form data" in {
          lazy val result: WSResponse = {
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"
            ))

            authoriseAgent()
            await(
              wsClient
                .url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/remove-overseas-charity-shares-and-property?charityName=TestCharity")
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
                .post(Map[String, String]())
            )
          }

          result.status shouldBe BAD_REQUEST
        }
      }
    }
  }
}
