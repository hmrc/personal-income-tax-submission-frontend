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
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.{WSClient, WSResponse}
import play.api.mvc.Result
import play.api.test.FakeRequest
import utils.{IntegrationTest, ViewHelpers}

class DonationsToPreviousTaxYearControllerISpec extends IntegrationTest with ViewHelpers {

  lazy val controller: DonationsToPreviousTaxYearController = app.injector.instanceOf[DonationsToPreviousTaxYearController]

  val taxYear: Int = 2022

  val url: String = s"${appUrl(port)}/$taxYear/charity/donations-after-5-april-$taxYear"

  object Selectors {
    val paragraph1HintText = "#main-content > div > div > form > div > fieldset > legend > div > p:nth-child(1)"
    val paragraph2HintText = "#main-content > div > div > form > div > fieldset > legend > div > p:nth-child(2)"
  }

  object Content {

    val expectedHeading = "Do you want to add any donations made after 5 April 2022 to this tax year?"
    val expectedCaption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val expectedParagraph1Individual = "If you made donations after 5 April 2022, you can add them to the 6 April 2021 to 5 April 2022 tax year."
    val expectedParagraph1Agent = "If your client made donations after 5 April 2022, you can add them to the 6 April 2021 to 5 April 2022 tax year."
    val expectedParagraph2Individual = "You might want to do this if you want tax relief sooner."
    val expectedParagraph2Agent = "You might want to do this if your client wants tax relief sooner."
    val yesText = "Yes"
    val noText = "No"
    val button = "Continue"

    val errorText = "Select yes to add any of your donations made after 5 April 2022 to this tax year"
    val errorTextAgent = "Select yes to add any of your client’s donations made after 5 April 2022 to this tax year"
    val errorHref = "#value"

  }

  object WelshContent {

    val expectedHeading = "Do you want to add any donations made after 5 April 2022 to this tax year?"
    val expectedCaption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val expectedParagraph1Individual = "If you made donations after 5 April 2022, you can add them to the 6 April 2021 to 5 April 2022 tax year."
    val expectedParagraph1Agent = "If your client made donations after 5 April 2022, you can add them to the 6 April 2021 to 5 April 2022 tax year."
    val expectedParagraph2Individual = "You might want to do this if you want tax relief sooner."
    val expectedParagraph2Agent = "You might want to do this if your client wants tax relief sooner."
    val yesText = "Yes"
    val noText = "No"
    val button = "Continue"

    val errorText = "Select yes to add any of your donations made after 5 April 2022 to this tax year"
    val errorTextAgent = "Select yes to add any of your client’s donations made after 5 April 2022 to this tax year"
    val errorHref = "#value"

  }

  ".show" when {

    "the dates provided are different" should {

      "redirect to a correct URL" in {
        val result: Result = {
          authoriseIndividual()
          await(controller.show(taxYear, taxYear + 1)(FakeRequest().withSession(
            SessionValues.TAX_YEAR -> taxYear.toString
          )))
        }

        val url = "/income-through-software/return/personal-income/2022/charity/donations-after-5-april-2022"

        result.header.status shouldBe SEE_OTHER
        result.header.headers("Location") shouldBe url
      }

    }

  }

  ".submit" when {

    "the dates provided are different" should {

      "redirect to a correct URL" in {
        val result: Result = {
          authoriseIndividual()
          await(controller.submit(taxYear, taxYear + 1)(FakeRequest()
            .withFormUrlEncodedBody("amount" -> "123")
            .withSession(
              SessionValues.TAX_YEAR -> taxYear.toString
            )))
        }

        val url = "/income-through-software/return/personal-income/2022/charity/donations-after-5-april-2022"

        result.header.status shouldBe SEE_OTHER
        result.header.headers("Location") shouldBe url
      }

    }

  }

  "calling GET" when {

    "the language is set to ENGLISH" when {

      "the user is an individual" should {

        "return a page" which {
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(url).get())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(Content.expectedHeading)
          h1Check(Content.expectedHeading + " " + Content.expectedCaption)
          textOnPageCheck(Content.expectedParagraph1Individual, Selectors.paragraph1HintText)
          textOnPageCheck(Content.expectedParagraph2Individual, Selectors.paragraph2HintText)
          radioButtonCheck(Content.yesText, 1)
          radioButtonCheck(Content.noText, 2)
          captionCheck(Content.expectedCaption)
          buttonCheck(Content.button)
        }
      }

      "the user is an agent" should {

        "return a page" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890"
          ))

          lazy val result: WSResponse = {
            authoriseAgent()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck").get())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(Content.expectedHeading)
          h1Check(Content.expectedHeading + " " + Content.expectedCaption)
          textOnPageCheck(Content.expectedParagraph1Agent, Selectors.paragraph1HintText)
          textOnPageCheck(Content.expectedParagraph2Agent, Selectors.paragraph2HintText)
          radioButtonCheck(Content.yesText, 1)
          radioButtonCheck(Content.noText, 2)
          captionCheck(Content.expectedCaption)
          buttonCheck(Content.button)
        }

      }

    }

    "the language is set to WELSH" when {

      "the user is an individual" should {

        "return a page" which {
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy").get())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(WelshContent.expectedHeading)
          h1Check(WelshContent.expectedHeading + " " + WelshContent.expectedCaption)
          textOnPageCheck(WelshContent.expectedParagraph1Individual, Selectors.paragraph1HintText)
          textOnPageCheck(WelshContent.expectedParagraph2Individual, Selectors.paragraph2HintText)
          radioButtonCheck(WelshContent.yesText, 1)
          radioButtonCheck(WelshContent.noText, 2)
          captionCheck(WelshContent.expectedCaption)
          buttonCheck(WelshContent.button)

          welshToggleCheck(WELSH)
        }
      }

      "the user is an agent" should {

        "return a page" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890"
          ))

          lazy val result: WSResponse = {
            authoriseAgent()
            await(wsClient.url(url).withHttpHeaders(
              HeaderNames.COOKIE -> playSessionCookies,
              "Csrf-Token" -> "nocheck",
              HeaderNames.ACCEPT_LANGUAGE -> "cy"
            ).get())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(WelshContent.expectedHeading)
          h1Check(WelshContent.expectedHeading + " " + WelshContent.expectedCaption)
          textOnPageCheck(WelshContent.expectedParagraph1Agent, Selectors.paragraph1HintText)
          textOnPageCheck(WelshContent.expectedParagraph2Agent, Selectors.paragraph2HintText)
          radioButtonCheck(WelshContent.yesText, 1)
          radioButtonCheck(WelshContent.noText, 2)
          captionCheck(WelshContent.expectedCaption)
          buttonCheck(WelshContent.button)
        }

      }

    }
  }

  "Calling POST" when {

    "the language is set to ENGLISH" when {

      "the user is an individual" when {

        "a radio button has been selected" should {

          "return an OK" in {
            lazy val result: WSResponse = {
              authoriseIndividual()
              await(wsClient.url(url)
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes)))
            }

            result.status shouldBe OK
          }
        }

        "no radio button has been selected" should {

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(url)
              .post(Map(YesNoForm.yesNo -> "")))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck("Error: " + Content.expectedHeading)
          h1Check(Content.expectedHeading + " " + Content.expectedCaption)
          textOnPageCheck(Content.expectedParagraph1Individual, Selectors.paragraph1HintText)
          textOnPageCheck(Content.expectedParagraph2Individual, Selectors.paragraph2HintText)
          radioButtonCheck(Content.yesText, 1)
          radioButtonCheck(Content.noText, 2)
          captionCheck(Content.expectedCaption)
          buttonCheck(Content.button)

          errorSummaryCheck(Content.errorText, Content.errorHref)
          errorAboveElementCheck(Content.errorText)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }
      }

      "the user is an agent" when {

        "a radio button has been selected" should {

          "return an OK" in {
            lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
              SessionValues.TAX_YEAR -> taxYear.toString,
              SessionValues.CLIENT_NINO -> "AA123456A",
              SessionValues.CLIENT_MTDITID -> "1234567890"
            ))

            lazy val result: WSResponse = {
              authoriseAgent()
              await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck")
                .post(Map[String, String](YesNoForm.yesNo -> YesNoForm.yes)))
            }

            result.status shouldBe OK
          }
        }

        "no radio button has been selected" should {

          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890"
          ))

          lazy val result: WSResponse = {
            authoriseAgent()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, "Csrf-Token" -> "nocheck")
              .post(Map[String, String](YesNoForm.yesNo -> "")))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck("Error: " + Content.expectedHeading)
          h1Check(Content.expectedHeading + " " + Content.expectedCaption)
          textOnPageCheck(Content.expectedParagraph1Agent, Selectors.paragraph1HintText)
          textOnPageCheck(Content.expectedParagraph2Agent, Selectors.paragraph2HintText)
          radioButtonCheck(Content.yesText, 1)
          radioButtonCheck(Content.noText, 2)
          captionCheck(Content.expectedCaption)
          buttonCheck(Content.button)

          errorSummaryCheck(Content.errorTextAgent, Content.errorHref)
          errorAboveElementCheck(Content.errorTextAgent)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }
      }
    }

    "the language is set to WELSH" when {

      "the user is an individual" when {

        "a radio button has been selected" should {

          "return an OK" in {
            lazy val result: WSResponse = {
              authoriseIndividual()
              await(wsClient.url(url).withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy")
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes)))
            }

            result.status shouldBe OK
          }
        }

        "no radio button has been selected" should {

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy")
              .post(Map(YesNoForm.yesNo -> "")))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck("Error: " + WelshContent.expectedHeading)
          h1Check(WelshContent.expectedHeading + " " + WelshContent.expectedCaption)
          textOnPageCheck(WelshContent.expectedParagraph1Individual, Selectors.paragraph1HintText)
          textOnPageCheck(WelshContent.expectedParagraph2Individual, Selectors.paragraph2HintText)
          radioButtonCheck(WelshContent.yesText, 1)
          radioButtonCheck(WelshContent.noText, 2)
          captionCheck(WelshContent.expectedCaption)
          buttonCheck(WelshContent.button)

          errorSummaryCheck(WelshContent.errorText, WelshContent.errorHref)
          errorAboveElementCheck(WelshContent.errorText)

          welshToggleCheck(WELSH)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }
      }

      "the user is an agent" when {

        "a radio button has been selected" should {

          "return an OK" in {
            lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
              SessionValues.TAX_YEAR -> taxYear.toString,
              SessionValues.CLIENT_NINO -> "AA123456A",
              SessionValues.CLIENT_MTDITID -> "1234567890"
            ))

            lazy val result: WSResponse = {
              authoriseAgent()
              await(wsClient.url(url).withHttpHeaders(
                HeaderNames.COOKIE -> playSessionCookies,
                "Csrf-Token" -> "nocheck",
                HeaderNames.ACCEPT_LANGUAGE -> "cy"
              )
                .post(Map[String, String](YesNoForm.yesNo -> YesNoForm.yes)))
            }

            result.status shouldBe OK
          }
        }

        "no radio button has been selected" should {

          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890"
          ))

          lazy val result: WSResponse = {
            authoriseAgent()
            await(wsClient.url(url).withHttpHeaders(
              HeaderNames.COOKIE -> playSessionCookies,
              "Csrf-Token" -> "nocheck",
              HeaderNames.ACCEPT_LANGUAGE -> "cy"
            )
              .post(Map[String, String](YesNoForm.yesNo -> "")))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck("Error: " + WelshContent.expectedHeading)
          h1Check(WelshContent.expectedHeading + " " + WelshContent.expectedCaption)
          textOnPageCheck(WelshContent.expectedParagraph1Agent, Selectors.paragraph1HintText)
          textOnPageCheck(WelshContent.expectedParagraph2Agent, Selectors.paragraph2HintText)
          radioButtonCheck(WelshContent.yesText, 1)
          radioButtonCheck(WelshContent.noText, 2)
          captionCheck(WelshContent.expectedCaption)
          buttonCheck(WelshContent.button)

          errorSummaryCheck(WelshContent.errorTextAgent, WelshContent.errorHref)
          errorAboveElementCheck(WelshContent.errorTextAgent)

          welshToggleCheck(WELSH)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }
      }
    }
  }
}
