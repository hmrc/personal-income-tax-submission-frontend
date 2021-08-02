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
import models.charity.GiftAidCYAModel
import models.charity.prior.{GiftAidPaymentsModel, GiftAidSubmissionModel, GiftsModel}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{GiftAidDatabaseHelper, IntegrationTest, ViewHelpers}

class GiftAidDonateLandOrPropertyControllerISpec extends IntegrationTest with ViewHelpers with GiftAidDatabaseHelper {

  object IndividualExpectedEN {
    val expectedH1 = "Did you donate land or property to charity?"
    val expectedTitle = "Did you donate land or property to charity?"
    val expectedError: String = "Select yes if you donated land or property to charity"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object AgentExpectedEN {
    val expectedTitle: String = "Did your client donate land or property to charity?"
    val expectedH1: String = "Did your client donate land or property to charity?"
    val expectedError: String = "Select yes if your client donated land or property to charity"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object IndividualExpectedCY {
    val expectedH1 = "Did you donate land or property to charity?"
    val expectedTitle = "Did you donate land or property to charity?"
    val expectedError: String = "Select yes if you donated land or property to charity"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object AgentExpectedCY {
    val expectedTitle: String = "Did your client donate land or property to charity?"
    val expectedH1: String = "Did your client donate land or property to charity?"
    val expectedError: String = "Select yes if your client donated land or property to charity"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  val expectedCyaTitle = "Check your donations to charity"

  val taxYear: Int = 2022
  val taxYearMinusOne: Int = taxYear - 1

  val captionTextEN = s"Donations to charity for 6 April $taxYearMinusOne to 5 April $taxYear"
  val yesTextEN = "Yes"
  val noTextEN = "No"
  val continueTextEN = "Continue"

  val captionTextCY = s"Donations to charity for 6 April $taxYearMinusOne to 5 April $taxYear"
  val yesTextCY = "Yes"
  val noTextCY = "No"
  val continueTextCY = "Continue"

  val captionSelector = ".govuk-caption-l"
  val continueSelector = "#continue"
  val continueButtonFormSelector = "#main-content > div > div > form"
  val errorSummaryHref = "#value"

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  lazy val controller: GiftAidDonateLandOrPropertyController = app.injector.instanceOf[GiftAidDonateLandOrPropertyController]

  val testModel: GiftAidCYAModel =
    GiftAidCYAModel(donatedSharesOrSecurities = Some(true))

  val testModelFalse: GiftAidCYAModel =
    GiftAidCYAModel(donatedSharesOrSecurities = Some(false), donatedSharesSecuritiesLandOrProperty = Some(true))

  val priorDataMin: GiftAidSubmissionModel = GiftAidSubmissionModel(
    Some(GiftAidPaymentsModel(
      Some(100.00),
      Some(List("JohnDoe")),
      Some(100.00),
      Some(100.00),
      Some(100.00),
      Some(100.00)
    )),
    Some(GiftsModel(
      Some(100.00),
      Some(List("JaneDoe")),
      Some(100.00),
      Some(100.00)
    ))
  )

  val priorModel: IncomeSourcesModel = IncomeSourcesModel(giftAid = Some(priorDataMin))


  "in english" when {

    "as an individual" when {
      import IndividualExpectedEN._

      ".show is called" should {

        "return the DonateLandOrPropertyPage" which {
          lazy val result: WSResponse = {
            dropGiftAidDB()
            emptyUserDataStub()
            insertCyaData(Some(testModel))
            authoriseIndividual()
            await(wsClient
              .url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donation-of-land-or-property")
              .withHttpHeaders(xSessionId, csrfContent)
              .get())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          "has the following page elements" which {
            titleCheck(expectedTitle)
            h1Check(expectedH1 + " " + captionTextEN)
            textOnPageCheck(captionTextEN, captionSelector)
            radioButtonCheck(yesTextEN, 1)
            radioButtonCheck(noTextEN, 2)
            buttonCheck(continueTextEN, continueSelector)

            noErrorsCheck()

            welshToggleCheck(ENGLISH)
          }
        }
        "return the overview page when there is no data" which {
          lazy val result: WSResponse = {
            dropGiftAidDB()
            emptyUserDataStub()
            authoriseIndividual()
            await(wsClient
              .url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donation-of-land-or-property")
              .withHttpHeaders(xSessionId, csrfContent)
              .withFollowRedirects(false)
              .get())
          }

          "has a status of SEE_OTHER(303)" in {
            result.status shouldBe SEE_OTHER
          }

          "redirects to the overview page" in {
            result.headers("Location").head shouldBe overviewUrl
          }
        }
        "return the sharesOrSecurities page when there is no donatedSharesOrSecurities" which {
          lazy val result: WSResponse = {
            dropGiftAidDB()
            emptyUserDataStub()
            insertCyaData(Some(testModelFalse))
            authoriseIndividual()
            await(wsClient
              .url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donation-of-land-or-property")
              .withHttpHeaders(xSessionId, csrfContent)
              .withFollowRedirects(false)
              .get())
          }

          "has a status of SEE_OTHER(303)" in {
            result.status shouldBe SEE_OTHER
          }

          "redirects to the QualifyingSharesSecurities page" in {
            result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidQualifyingSharesSecuritiesController.show(taxYear)}"
          }
        }
        "return the check your answers page when there is prior data" which {
          lazy val result: WSResponse = {
            dropGiftAidDB()
            emptyUserDataStub()
            insertCyaData(Some(testModelFalse))
            userDataStub(priorModel, nino, taxYear)
            authoriseIndividual()
            await(wsClient
              .url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donation-of-land-or-property")
              .withHttpHeaders(xSessionId, csrfContent)
              .withFollowRedirects(false)
              .get())
          }

          "has a status of SEE_OTHER(303)" in {
            result.status shouldBe SEE_OTHER
          }

          "redirects to the checkYourAnswers page" in {
            result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(taxYear)}"
          }
        }
      }

      ".submit is called" should {

        s"return an OK($OK) status" when {

          "there is form data (yes)" in {

            lazy val result: WSResponse = {
              dropGiftAidDB()
              emptyUserDataStub()
              insertCyaData(Some(testModel))
              authoriseIndividual()
              await(
                wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donation-of-land-or-property")
                  .withHttpHeaders(xSessionId, csrfContent)
                  .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
              )
            }

            result.status shouldBe OK
          }
          "there is form data (no)" in {

            lazy val result: WSResponse = {
              dropGiftAidDB()
              emptyUserDataStub()
              insertCyaData(Some(testModel))
              authoriseIndividual()
              await(
                wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donation-of-land-or-property")
                  .withHttpHeaders(xSessionId, csrfContent)
                  .post(Map(YesNoForm.yesNo -> YesNoForm.no))
              )
            }

            result.status shouldBe OK
          }
        }
        s"return the overview page" when {

          "there is no cya data" in {

            lazy val result: WSResponse = {
              dropGiftAidDB()
              emptyUserDataStub()
              authoriseIndividual()
              await(
                wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donation-of-land-or-property")
                  .withHttpHeaders(xSessionId, csrfContent)
                  .withFollowRedirects(false)
                  .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
              )
            }

            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe overviewUrl
          }
          "there is empty cya data" in {

            lazy val result: WSResponse = {
              dropGiftAidDB()
              emptyUserDataStub()
              insertCyaData(None)
              authoriseIndividual()
              await(
                wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donation-of-land-or-property")
                  .withHttpHeaders(xSessionId, csrfContent)
                  .withFollowRedirects(false)
                  .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
              )
            }

            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe overviewUrl
          }
        }
        s"return the checkYourAnswers page" when {

          "there is prior data" in {

            lazy val result: WSResponse = {
              dropGiftAidDB()
              emptyUserDataStub()
              authoriseIndividual()
              userDataStub(priorModel, nino, taxYear)
              await(
                wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donation-of-land-or-property")
                  .withHttpHeaders(xSessionId, csrfContent)
                  .withFollowRedirects(false)
                  .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
              )
            }

            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(taxYear)}"
          }
        }

        s"return an error page" when {

          "there is no form data" which {

            lazy val result: WSResponse = {
              dropGiftAidDB()
              emptyUserDataStub()
              insertCyaData(Some(testModel))
              authoriseIndividual()
              await(wsClient
                .url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donation-of-land-or-property")
                .withHttpHeaders(xSessionId, csrfContent)
                .post(Map[String, String]()))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            s"has a BAD_REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            s"has the following elements" which {
              titleCheck(expectedErrorTitle)
              h1Check(expectedH1 + " " + captionTextEN)
              textOnPageCheck(captionTextEN, captionSelector)
              errorSummaryCheck(expectedError, errorSummaryHref)
              errorAboveElementCheck(expectedError)
              radioButtonCheck(yesTextEN, 1)
              radioButtonCheck(noTextEN, 2)
              buttonCheck(continueTextEN, continueSelector)

              welshToggleCheck(ENGLISH)
            }
          }
        }

      }

    }

    "as an agent" when {
      import AgentExpectedEN._

      ".show is called" should {

        "return a page" which {
          lazy val result: WSResponse = {
            dropGiftAidDB()
            emptyUserDataStub()
            insertCyaData(Some(testModel))
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"
            ))

            authoriseAgent()
            await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donation-of-land-or-property")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
              .get())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          "has the following page elements" which {
            titleCheck(expectedTitle)
            h1Check(expectedH1 + " " + captionTextEN)
            textOnPageCheck(captionTextEN, captionSelector)
            radioButtonCheck(yesTextEN, 1)
            radioButtonCheck(noTextEN, 2)
            buttonCheck(continueTextEN, continueSelector)

            noErrorsCheck()

            welshToggleCheck(ENGLISH)
          }
        }
      }

      ".submit is called" should {

        s"return an OK($OK) status" when {

          "there is form data" in {
            lazy val result: WSResponse = {
              dropGiftAidDB()
              emptyUserDataStub()
              insertCyaData(Some(testModel))
              lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
                SessionValues.CLIENT_MTDITID -> "1234567890",
                SessionValues.CLIENT_NINO -> "AA123456A"))

              authoriseAgent()
              await(
                wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donation-of-land-or-property")
                  .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
                  .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
              )
            }

            result.status shouldBe OK
          }
        }

        s"return an error page" when {

          "there is no form data" which {
            lazy val result: WSResponse = {
              dropGiftAidDB()
              emptyUserDataStub()
              insertCyaData(Some(testModel))
              lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
                SessionValues.CLIENT_MTDITID -> "1234567890",
                SessionValues.CLIENT_NINO -> "AA123456A"
              ))

              authoriseAgent()
              await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donation-of-land-or-property")
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
                .post(Map[String, String]()))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            s"has a BAD_REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            "has the following page elements" which {
              titleCheck(expectedErrorTitle)
              h1Check(expectedH1 + " " + captionTextEN)
              textOnPageCheck(captionTextEN, captionSelector)
              errorSummaryCheck(expectedError, errorSummaryHref)
              errorAboveElementCheck(expectedError)
              radioButtonCheck(yesTextEN, 1)
              radioButtonCheck(noTextEN, 2)
              buttonCheck(continueTextEN, continueSelector)

              welshToggleCheck(ENGLISH)
            }
          }
        }

      }

    }
  }

  "in welsh" when {

    "as an individual" when {
      import IndividualExpectedCY._

      ".show is called" should {

        "return a page" which {
          lazy val result: WSResponse = {
            dropGiftAidDB()
            emptyUserDataStub()
            insertCyaData(Some(testModel))
            authoriseIndividual()
            await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donation-of-land-or-property")
              .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy", xSessionId, csrfContent)
              .get())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          "has the following page elements" which {
            titleCheck(expectedTitle)
            h1Check(expectedH1 + " " + captionTextCY)
            textOnPageCheck(captionTextCY, captionSelector)
            radioButtonCheck(yesTextCY, 1)
            radioButtonCheck(noTextCY, 2)
            buttonCheck(continueTextCY, continueSelector)

            noErrorsCheck()

            welshToggleCheck(WELSH)
          }
        }
      }

      ".submit is called" should {

        s"return an OK($OK) status" when {

          "there is form data" in {

            lazy val result: WSResponse = {
              dropGiftAidDB()
              emptyUserDataStub()
              insertCyaData(Some(testModel))
              authoriseIndividual()
              await(
                wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donation-of-land-or-property")
                  .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy", xSessionId, csrfContent)
                  .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
              )
            }

            result.status shouldBe OK
          }
        }

        s"return an error page" when {

          "there is no form data" which {

            lazy val result: WSResponse = {
              dropGiftAidDB()
              emptyUserDataStub()
              insertCyaData(Some(testModel))
              authoriseIndividual()
              await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donation-of-land-or-property")
                .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy", xSessionId, csrfContent)
                .post(Map[String, String]()))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            s"has a BAD_REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            s"has the following elements" which {
              titleCheck(expectedErrorTitle)
              h1Check(expectedH1 + " " + captionTextCY)
              textOnPageCheck(captionTextCY, captionSelector)
              errorSummaryCheck(expectedError, errorSummaryHref)
              errorAboveElementCheck(expectedError)
              radioButtonCheck(yesTextCY, 1)
              radioButtonCheck(noTextCY, 2)
              buttonCheck(continueTextCY, continueSelector)

              welshToggleCheck(WELSH)
            }
          }
        }

      }

    }

    "as an agent" when {
      import AgentExpectedCY._

      ".show is called" should {

        "return a page" which {
          lazy val result: WSResponse = {
            dropGiftAidDB()
            emptyUserDataStub()
            insertCyaData(Some(testModel))
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"
            ))

            authoriseAgent()
            await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donation-of-land-or-property")
              .withHttpHeaders(
                HeaderNames.COOKIE -> sessionCookie,
                HeaderNames.ACCEPT_LANGUAGE -> "cy",
                xSessionId, csrfContent
              )
              .get())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          "has the following page elements" which {
            titleCheck(expectedTitle)
            h1Check(expectedH1 + " " + captionTextCY)
            textOnPageCheck(captionTextCY, captionSelector)
            radioButtonCheck(yesTextCY, 1)
            radioButtonCheck(noTextCY, 2)
            buttonCheck(continueTextCY, continueSelector)

            noErrorsCheck()

            welshToggleCheck(WELSH)
          }
        }
      }

      ".submit is called" should {

        s"return an OK($OK) status" when {

          "there is form data" in {
            lazy val result: WSResponse = {
              dropGiftAidDB()
              emptyUserDataStub()
              insertCyaData(Some(testModel))
              lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
                SessionValues.CLIENT_MTDITID -> "1234567890",
                SessionValues.CLIENT_NINO -> "AA123456A"))

              authoriseAgent()
              await(
                wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donation-of-land-or-property")
                  .withHttpHeaders(
                    HeaderNames.COOKIE -> sessionCookie,
                    xSessionId, csrfContent,
                    HeaderNames.ACCEPT_LANGUAGE -> "cy"
                  )
                  .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
              )
            }

            result.status shouldBe OK
          }
        }

        s"return an error page" when {

          "there is no form data" which {
            lazy val result: WSResponse = {
              dropGiftAidDB()
              emptyUserDataStub()
              insertCyaData(Some(testModel))
              lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
                SessionValues.CLIENT_MTDITID -> "1234567890",
                SessionValues.CLIENT_NINO -> "AA123456A"
              ))

              authoriseAgent()
              await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donation-of-land-or-property")
                .withHttpHeaders(
                  HeaderNames.COOKIE -> sessionCookie,
                  xSessionId, csrfContent,
                  HeaderNames.ACCEPT_LANGUAGE -> "cy"
                )
                .post(Map[String, String]()))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            s"has a BAD_REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            "has the following page elements" which {
              titleCheck(expectedErrorTitle)
              h1Check(expectedH1 + " " + captionTextCY)
              textOnPageCheck(captionTextCY, captionSelector)
              errorSummaryCheck(expectedError, errorSummaryHref)
              errorAboveElementCheck(expectedError)
              radioButtonCheck(yesTextCY, 1)
              radioButtonCheck(noTextCY, 2)
              buttonCheck(continueTextCY, continueSelector)

              welshToggleCheck(WELSH)
            }
          }
        }

      }

    }
  }
}
