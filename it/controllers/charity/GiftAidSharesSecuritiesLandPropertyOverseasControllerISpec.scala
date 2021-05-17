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
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{IntegrationTest, ViewHelpers}
import play.api.http.Status._

class GiftAidSharesSecuritiesLandPropertyOverseasControllerISpec extends IntegrationTest with ViewHelpers {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  lazy val controller: GiftAidSharesSecuritiesLandPropertyOverseasController = app.injector.instanceOf[GiftAidSharesSecuritiesLandPropertyOverseasController]


  val taxYear: Int = 2022
  val taxYearMinusOne: Int = taxYear - 1
  val captionText = s"Donations to charity for 6 April $taxYearMinusOne to 5 April $taxYear"
  val yesText = "Yes"
  val noText = "No"
  val continueText = "Continue"
  val captionSelector = ".govuk-caption-l"
  val continueSelector = "#continue"
  val continueButtonFormSelector = "#main-content > div > div > form"
  val errorSummaryHref = "#value"

  object IndividualExpected {

    val expectedTitle = "Did you donate qualifying shares, securities, land or property to overseas charities?"
    val expectedH1 = "Did you donate qualifying shares, securities, land or property to overseas charities?"
    val expectedError = "Select yes if you donated shares, securities, land or property to overseas charities"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object AgentExpected {
    val expectedTitle = "Did your client donate qualifying shares, securities, land or property to overseas charities?"
    val expectedH1 = "Did your client donate qualifying shares, securities, land or property to overseas charities?"
    val expectedError = "Select yes if your client donated shares, securities, land or property to overseas charities"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object IndividualExpectedCy {

    val expectedTitle = "Did you donate qualifying shares, securities, land or property to overseas charities?"
    val expectedH1 = "Did you donate qualifying shares, securities, land or property to overseas charities?"
    val expectedError = "Select yes if you donated shares, securities, land or property to overseas charities"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object AgentExpectedCy {

    val expectedTitle = "Did your client donate qualifying shares, securities, land or property to overseas charities?"
    val expectedH1 = "Did your client donate qualifying shares, securities, land or property to overseas charities?"
    val expectedError = "Select yes if your client donated shares, securities, land or property to overseas charities"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  "In english" when {

    "As an individual" when {

      import IndividualExpected._

      ".show" should {

        "return a page" which {

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/" +
              s"donation-of-shares-securities-land-or-property-to-overseas-charities")
              .get()
            )
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK(200) status" in {
            result.status shouldBe OK

          }

          "has the page elements" which {

            titleCheck(expectedTitle)
            welshToggleCheck(ENGLISH)
            h1Check(expectedH1 + " " + captionText)
            textOnPageCheck(captionText, captionSelector)
            radioButtonCheck(yesText, 1)
            radioButtonCheck(noText, 2)
            buttonCheck(continueText, continueSelector)

          }
        }
      }

      ".submit" should {

        "return an OK(200) status" when {

          "there is form data" in {

            lazy val result: WSResponse = {
              authoriseIndividual()
              await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/" +
                s"donation-of-shares-securities-land-or-property-to-overseas-charities")
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
              )
            }

            result.status shouldBe OK
          }

          "return an error page" when {

            "there is no form data" which {

              lazy val result: WSResponse = {
                authoriseIndividual()
                await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/" +
                  s"donation-of-shares-securities-land-or-property-to-overseas-charities")
                  .post(Map[String, String]())
                )
              }

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              "has a BadRequest(400) status" in {

                result.status shouldBe BAD_REQUEST
              }

              "has the page elements" which {

                titleCheck(expectedErrorTitle)
                welshToggleCheck(ENGLISH)
                h1Check(expectedH1 + " " + captionText)
                textOnPageCheck(captionText, captionSelector)
                errorSummaryCheck(expectedError, errorSummaryHref)
                errorAboveElementCheck(expectedError)
                radioButtonCheck(yesText, 1)
                radioButtonCheck(noText, 2)
                buttonCheck(continueText, continueSelector)

              }

            }

          }

        }

      }

    }

    "as an agent" when {

      import AgentExpected._

      ".show" should {

        "return a page" which {

          lazy val result: WSResponse = {
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"
            ))

            authoriseAgent()

            await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/" +
              s"donation-of-shares-securities-land-or-property-to-overseas-charities")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie)
              .get()
            )

          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an Ok (200) status" in {

            result.status shouldBe OK
          }

          "has the page elements" which {

            titleCheck(expectedTitle)
            welshToggleCheck(ENGLISH)
            h1Check(expectedH1 + " " + captionText)
            textOnPageCheck(captionText, captionSelector)
            radioButtonCheck(yesText, 1)
            radioButtonCheck(noText, 2)
            buttonCheck(continueText, continueSelector)
          }

        }

      }

      ".submit" should {

        "return an OK (200) status" when {

          "there is form data" in {

            lazy val result: WSResponse = {
              lazy val sessionCookie = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
                SessionValues.CLIENT_MTDITID -> "1234567890",
                SessionValues.CLIENT_NINO -> "AA123456A"
              ))

              authoriseAgent()

              await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/" +
                s"donation-of-shares-securities-land-or-property-to-overseas-charities")
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
              )
            }

            result.status shouldBe OK
          }

        }

        "return an error page with a BadRequest (400) status" when {

          "there is no form data" which {

            lazy val result: WSResponse = {
              lazy val sessionCookie = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
                SessionValues.CLIENT_MTDITID -> "1234567890",
                SessionValues.CLIENT_NINO -> "AA123456A"
              ))

              authoriseAgent()

              await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/" +
                s"donation-of-shares-securities-land-or-property-to-overseas-charities")
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
                .post(Map[String, String]())
              )
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            "returns a BadRequest (400) status" in {

              result.status shouldBe BAD_REQUEST
            }

            "has the page elements" which {
              titleCheck(expectedErrorTitle)
              welshToggleCheck(ENGLISH)
              h1Check(expectedH1 + " " + captionText)
              textOnPageCheck(captionText, captionSelector)
              errorSummaryCheck(expectedError, errorSummaryHref)
              errorAboveElementCheck(expectedError)
              radioButtonCheck(yesText, 1)
              radioButtonCheck(noText, 2)
              buttonCheck(continueText, continueSelector)
            }

          }

        }

      }

    }
  }

  "In welsh" when {

    "As an individual" when {

      import IndividualExpectedCy._

      ".show" should {

        "return a page" which {

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/" +
              s"donation-of-shares-securities-land-or-property-to-overseas-charities")
              .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy")
              .get()
            )
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK(200) status" in {
            result.status shouldBe OK

          }

          "has the page elements" which {

            titleCheck(expectedTitle)
            welshToggleCheck(WELSH)
            h1Check(expectedH1 + " " + captionText)
            textOnPageCheck(captionText, captionSelector)
            radioButtonCheck(yesText, 1)
            radioButtonCheck(noText, 2)
            buttonCheck(continueText, continueSelector)

          }
        }
      }

      ".submit" should {

        "return an OK(200) status" when {

          "there is form data" in {

            lazy val result: WSResponse = {
              authoriseIndividual()
              await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/" +
                s"donation-of-shares-securities-land-or-property-to-overseas-charities")
                .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy")
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
              )
            }

            result.status shouldBe OK
          }

          "return an error page" when {

            "there is no form data" which {

              lazy val result: WSResponse = {
                authoriseIndividual()
                await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/" +
                  s"donation-of-shares-securities-land-or-property-to-overseas-charities")
                  .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy")
                  .post(Map[String, String]())
                )
              }

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              "has a BadRequest(400) status" in {

                result.status shouldBe BAD_REQUEST
              }

              "has the page elements" which {

                titleCheck(expectedErrorTitle)
                welshToggleCheck(WELSH)
                h1Check(expectedH1 + " " + captionText)
                textOnPageCheck(captionText, captionSelector)
                errorSummaryCheck(expectedError, errorSummaryHref)
                errorAboveElementCheck(expectedError)
                radioButtonCheck(yesText, 1)
                radioButtonCheck(noText, 2)
                buttonCheck(continueText, continueSelector)

              }

            }

          }

        }

      }

    }

    "as an agent" when {

      import AgentExpectedCy._

      ".show" should {

        "return a page" which {

          lazy val result: WSResponse = {
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"
            ))

            authoriseAgent()

            await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/" +
              s"donation-of-shares-securities-land-or-property-to-overseas-charities")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy")
              .get()
            )

          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an Ok (200) status" in {

            result.status shouldBe OK
          }

          "has the page elements" which {

            titleCheck(expectedTitle)
            welshToggleCheck(WELSH)
            h1Check(expectedH1 + " " + captionText)
            textOnPageCheck(captionText, captionSelector)
            radioButtonCheck(yesText, 1)
            radioButtonCheck(noText, 2)
            buttonCheck(continueText, continueSelector)
          }

        }

      }

      ".submit" should {

        "return an OK (200) status" when {

          "there is form data" in {

            lazy val result: WSResponse = {
              lazy val sessionCookie = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
                SessionValues.CLIENT_MTDITID -> "1234567890",
                SessionValues.CLIENT_NINO -> "AA123456A"
              ))

              authoriseAgent()

              await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/" +
                s"donation-of-shares-securities-land-or-property-to-overseas-charities")
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck", HeaderNames.ACCEPT_LANGUAGE -> "cy")
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
              )
            }

            result.status shouldBe OK
          }

        }

        "return an error page with a BadRequest (400) status" when {

          "there is no form data" which {

            lazy val result: WSResponse = {
              lazy val sessionCookie = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
                SessionValues.CLIENT_MTDITID -> "1234567890",
                SessionValues.CLIENT_NINO -> "AA123456A"
              ))

              authoriseAgent()

              await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/" +
                s"donation-of-shares-securities-land-or-property-to-overseas-charities")
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck", HeaderNames.ACCEPT_LANGUAGE -> "cy")
                .post(Map[String, String]())
              )
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            "returns a BadRequest (400) status" in {

              result.status shouldBe BAD_REQUEST
            }

            "has the page elements" which {
              titleCheck(expectedErrorTitle)
              welshToggleCheck(WELSH)
              h1Check(expectedH1 + " " + captionText)
              textOnPageCheck(captionText, captionSelector)
              errorSummaryCheck(expectedError, errorSummaryHref)
              errorAboveElementCheck(expectedError)
              radioButtonCheck(yesText, 1)
              radioButtonCheck(noText, 2)
              buttonCheck(continueText, continueSelector)
            }

          }

        }

      }

    }
  }
}
