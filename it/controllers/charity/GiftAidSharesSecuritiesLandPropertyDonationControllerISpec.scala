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
import utils.{IntegrationTest,ViewHelpers}

class GiftAidSharesSecuritiesLandPropertyDonationControllerISpec extends IntegrationTest with ViewHelpers {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  lazy val controller: GiftAidSharesSecuritiesLandPropertyDonationController = app.injector.instanceOf[GiftAidSharesSecuritiesLandPropertyDonationController]

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

  val disclosureContentTitle = "What are qualifying shares and securities?"
  val disclosureContentParagraph = "Qualifying shares and securities are:"
  val disclosureContentBullet1 = "listed on a recognised stock exchange or dealt in on a designated market in the UK"
  val disclosureContentBullet2 = "units in an authorised unit trust"
  val disclosureContentBullet3 = "shares in an open-ended investment company"
  val disclosureContentBullet4 = "an interest in an offshore fund"

  val disclosureSelectorTitle = "#main-content > div > div > form > details > summary > span"
  val disclosureSelectorParagraph = "#main-content > div > div > form > details > div > p"
  val disclosureSelectorBullet1 = "#main-content > div > div > form > details > div > ul > li:nth-child(1)"
  val disclosureSelectorBullet2 = "#main-content > div > div > form > details > div > ul > li:nth-child(2)"
  val disclosureSelectorBullet3 = "#main-content > div > div > form > details > div > ul > li:nth-child(3)"
  val disclosureSelectorBullet4 = "#main-content > div > div > form > details > div > ul > li:nth-child(4)"

  object IndividualExpected {

    val expectedTitle = "Did you donate qualifying shares, securities, land or property to charity?"
    val expectedH1 = "Did you donate qualifying shares, securities, land or property to charity?"
    val expectedError = "Select yes if you donated shares, securities, land or property to charity"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object AgentExpected {
    val expectedTitle = "Did your client donate qualifying shares, securities, land or property to charity?"
    val expectedH1 = "Did your client donate qualifying shares, securities, land or property to charity?"
    val expectedError = "Select yes if your client donated shares, securities, land or property to charity"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  "as an individual" when {
    import IndividualExpected._

    ".show is called" should {

      "returns a page" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(
            s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donation-of-shares-securities-land-or-property"
          )
            .get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        "has the following page elements" which {
          titleCheck(expectedTitle)
          welshToggleCheck("English")
          h1Check(expectedH1 + " " + captionText)
          textOnPageCheck(captionText, captionSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(continueText, continueSelector)
          textOnPageCheck(disclosureContentTitle, disclosureSelectorTitle)
          textOnPageCheck(disclosureContentParagraph, disclosureSelectorParagraph)
          textOnPageCheck(disclosureContentBullet1, disclosureSelectorBullet1)
          textOnPageCheck(disclosureContentBullet2, disclosureSelectorBullet2)
          textOnPageCheck(disclosureContentBullet3, disclosureSelectorBullet3)
          textOnPageCheck(disclosureContentBullet4, disclosureSelectorBullet4)
        }

      }

    }

    ".submit is called" should {

      s"return an OK($OK) status" when {

        "there is form data" in {

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(
              s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donation-of-shares-securities-land-or-property"
            )
              .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
            )
          }

          result.status shouldBe OK
        }

        s"return an error page" when {

          "there is no form data" which {

            lazy val result: WSResponse = {
              authoriseIndividual()
              await(wsClient.url(
                s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donation-of-shares-securities-land-or-property"
              )
                .post(Map[String, String]()))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            s"has a BAD_REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            s"has the following elements" which {
              titleCheck(expectedErrorTitle)
              welshToggleCheck("English")
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

    ".show is called" should {

      "returns a page" which {
        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(
            s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donation-of-shares-securities-land-or-property"
          )
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie)
            .get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        "has the following page elements" which {
          titleCheck(expectedTitle)
          welshToggleCheck("English")
          h1Check(expectedH1 + " " + captionText)
          textOnPageCheck(captionText, captionSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(continueText, continueSelector)
          textOnPageCheck(disclosureContentTitle, disclosureSelectorTitle)
          textOnPageCheck(disclosureContentParagraph, disclosureSelectorParagraph)
          textOnPageCheck(disclosureContentBullet1, disclosureSelectorBullet1)
          textOnPageCheck(disclosureContentBullet2, disclosureSelectorBullet2)
          textOnPageCheck(disclosureContentBullet3, disclosureSelectorBullet3)
          textOnPageCheck(disclosureContentBullet4, disclosureSelectorBullet4)
        }

      }

    }

    ".submit is called" should {

      s"return an OK($OK) status" when {

        "there is form data" in {
          lazy val result: WSResponse = {
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"))

            authoriseAgent()
            await(wsClient.url(
              s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donation-of-shares-securities-land-or-property"
            )
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
            )
          }

          result.status shouldBe OK
        }
      }

      s"return an error page" when {

        "there is no form data" in {
          lazy val result: WSResponse = {
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"
            ))

            authoriseAgent()
            await(wsClient.url(
              s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donation-of-shares-securities-land-or-property"
            )
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map[String, String]()))
          }

            result.status shouldBe BAD_REQUEST

        }

      }

      ".submit" should {

        "when there is an incorrect input" should {
          lazy val result: WSResponse = {
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"
            ))

            authoriseAgent()
            await(wsClient.url(
              s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donation-of-shares-securities-land-or-property"
            )
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map[String, String]())
            )
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has the following page elements" which {
            titleCheck(expectedErrorTitle)
            welshToggleCheck("English")
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
