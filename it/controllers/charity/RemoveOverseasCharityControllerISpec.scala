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
import utils.IntegrationTest
import play.api.http.Status._

class RemoveOverseasCharityControllerISpec extends IntegrationTest {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  lazy val controller: RemoveOverseasCharityController = app.injector.instanceOf[RemoveOverseasCharityController]

  object Selectors {

    val heading = "h1"
    val caption = ".govuk-caption-l"
    val errorSummary = "#error-summary-title"
    val yesRadioButton = ".govuk-radios__item:nth-child(1) > label"
    val noRadioButton = ".govuk-radios__item:nth-child(2) > label"

  }

  object Content {

    val expectedTitle = "Are you sure you want to remove this overseas charity? - Update and submit an Income Tax Return - GOV.UK"
    val expectedErrorTitle = "Error: Are you sure you want to remove this overseas charity? - Update and submit an Income Tax Return - GOV.UK"
    val expectedH1 = "Are you sure you want to remove this overseas charity?"
    val expectedCaption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val noSelectionError = "Select yes to remove this overseas charity"
    val yesText = "Yes"
    val noText = "No"
  }

  val taxYear: Int = 2022

  "as an individual" when {

    ".show" should {

      "return an action" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/remove-overseas-charity").get())
        }

        lazy val document: Document = Jsoup.parse(result.body)


        "has an OK(200) status" in {

          result.status shouldBe OK


        }


        "displays the page content" in {

          document.title() shouldBe Content.expectedTitle
          document.select(Selectors.heading).text() shouldBe Content.expectedH1
          document.select(Selectors.caption).text() shouldBe Content.expectedCaption
          document.select(Selectors.yesRadioButton).text() shouldBe Content.yesText
          document.select(Selectors.noRadioButton).text() shouldBe Content.noText

        }

      }
      ".submit" should {

        "return an OK(200) status" in {
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(
              wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/remove-overseas-charity")
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
            )
          }
          result.status shouldBe OK
        }

        s"return a BAD_REQUEST($BAD_REQUEST) status" in {
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/remove-overseas-charity").post(Map[String, String]()))
          }
          lazy val document: Document = Jsoup.parse(result.body)

          result.status shouldBe BAD_REQUEST

          "have the correct page content" in {

            document.title() shouldBe Content.expectedErrorTitle
            document.select(Selectors.errorSummary) shouldBe Content.noSelectionError
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
            await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/remove-overseas-charity")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie)
              .get())
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
                wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/remove-overseas-charity")
                  .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
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
              await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/remove-overseas-charity")
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
                .post(Map[String, String]()))
            }

            result.status shouldBe BAD_REQUEST
          }
        }
      }
    }
  }
}
