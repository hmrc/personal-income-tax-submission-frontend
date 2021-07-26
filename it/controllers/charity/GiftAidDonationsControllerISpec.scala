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
import models.charity.prior.{GiftAidPaymentsModel, GiftAidSubmissionModel}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{GiftAidDatabaseHelper, IntegrationTest, ViewHelpers}

class GiftAidDonationsControllerISpec extends IntegrationTest with ViewHelpers with GiftAidDatabaseHelper {

  object Content {
    val heading = "Did you use Gift Aid to donate to charity?"
    val headingAgent = "Did your client use Gift Aid to donate to charity?"
    val caption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val button = "Continue"
    val yesText = "Yes"
    val noText = "No"
    val errorText = "Select yes if you used Gift Aid to donate to charity"
    val errorTextAgent = "Select yes if your client used Gift Aid to donate to charity"
  }

  object Selectors {
    val errorSummary: String = "#error-summary-title"
    val firstError: String = ".govuk-error-summary__body > ul > li:nth-child(1) > a"
    val errorMessage: String = "#value-error"
    val errorHref = "#value"
  }

  val cyaModelMax: GiftAidCYAModel = GiftAidCYAModel(
    Some(true), Some(100.00),
    Some(true), Some(100.00),
    Some(true), Some(100.00), Some(Seq("Belgian Trust", "American Trust")),
    Some(true), Some(100.00),
    Some(true), Some(100.00),
    Some(true), Some(true), Some(100.00), Some(true), Some(100.00),
    Some(true), Some(100.00), Some(Seq("Belgian Trust", "American Trust"))
  )

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  lazy val controller: GiftAidDonationsController = app.injector.instanceOf[GiftAidDonationsController]

  val yesNoFormNo: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
  val yesNoFormYes: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
  val yesNoFormEmpty: Map[String, String] = Map(YesNoForm.yesNo -> "")

  val taxYear: Int = 2022
  val giftAidDonationsUrl = s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/charity-donation-using-gift-aid"

  "as an individual" when {

    s"Calling GET $giftAidDonationsUrl" when {

      "there is no prior data" should {

        lazy val result: WSResponse = {
          dropGiftAidDB()

          emptyUserDataStub()
          insertCyaData(None)

          authoriseIndividual()
          await(wsClient.url(giftAidDonationsUrl).withHttpHeaders(xSessionId, csrfContent).get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return the page" which {
          titleCheck(Content.heading)
          welshToggleCheck("English")
          h1Check(s"${Content.heading} ${Content.caption}")
          captionCheck(Content.caption)
          radioButtonCheck(Content.yesText, 1)
          radioButtonCheck(Content.noText, 2)
          buttonCheck(Content.button)

          elementExtinct(Selectors.errorSummary)
          elementExtinct(Selectors.firstError)
          elementExtinct(Selectors.errorMessage)
        }

        s"have an OK($OK) status" in {
          result.status shouldBe OK
        }
      }

      "there is prior data" should {

        lazy val result: WSResponse = {
          dropGiftAidDB()

          userDataStub(IncomeSourcesModel(giftAid = Some(GiftAidSubmissionModel(Some(GiftAidPaymentsModel(
            currentYear = Some(1000.00)
          ))))), nino, taxYear)
          insertCyaData(None)

          authoriseIndividual()
          await(wsClient.url(giftAidDonationsUrl).withHttpHeaders(xSessionId, csrfContent)
            .withFollowRedirects(false).get())
        }

        "redirect the user to the CYA page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(taxYear)}"
        }
      }
    }

    s"Calling POST $giftAidDonationsUrl" when {

      "cya is finished" should {

        lazy val result: WSResponse = {
          dropGiftAidDB()

          emptyUserDataStub()
          insertCyaData(Some(cyaModelMax.copy(donationsViaGiftAid = None)))

          authoriseIndividual()
          await(wsClient.url(giftAidDonationsUrl).withHttpHeaders(xSessionId, csrfContent)
            .withFollowRedirects(false).post(yesNoFormYes))
        }

        "redirect the user to the CYA page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(taxYear)}"
        }
      }

      "the user has selected 'Yes'" should {

        lazy val result: WSResponse = {
          dropGiftAidDB()

          emptyUserDataStub()
          insertCyaData(None)

          authoriseIndividual()
          await(wsClient.url(giftAidDonationsUrl).withHttpHeaders(xSessionId, csrfContent)
            .withFollowRedirects(false).post(yesNoFormYes))
        }

        "redirect the user to the donated amount page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidDonatedAmountController.show(taxYear)}"
        }

        "update the cya data" in {
          findGiftAidDb shouldBe Some(GiftAidCYAModel(donationsViaGiftAid = Some(true)))
        }
      }

      "the user has selected 'No'" should {

        lazy val result: WSResponse = {
          dropGiftAidDB()

          emptyUserDataStub()
          insertCyaData(None)

          authoriseIndividual()
          await(wsClient.url(giftAidDonationsUrl).withHttpHeaders(xSessionId, csrfContent)
            .withFollowRedirects(false).post(yesNoFormNo))
        }

        "redirect the user to the 'Add donations to this tax year' page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear)}"
        }

        "update the cya data" in {
          findGiftAidDb shouldBe
            Some(GiftAidCYAModel(
              donationsViaGiftAid = Some(false),
              oneOffDonationsViaGiftAid = Some(false),
              overseasDonationsViaGiftAid = Some(false),
              addDonationToLastYear = Some(false)
            ))
        }
      }

      "the user has not selected an option" should {

        lazy val result: WSResponse = {
          dropGiftAidDB()

          emptyUserDataStub()
          insertCyaData(None)

          authoriseIndividual()
          await(wsClient.url(giftAidDonationsUrl).withHttpHeaders(xSessionId, csrfContent)
            .withFollowRedirects(false).post(yesNoFormEmpty))
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return the page" which {
          titleCheck("Error: " + Content.heading)
          welshToggleCheck("English")
          h1Check(s"${Content.heading} ${Content.caption}")
          captionCheck(Content.caption)
          radioButtonCheck(Content.yesText, 1)
          radioButtonCheck(Content.noText, 2)
          buttonCheck(Content.button)

          errorSummaryCheck(Content.errorText, Selectors.errorHref)
          errorAboveElementCheck(Content.errorText)
        }

        "return a BAD_REQUEST" in {
          result.status shouldBe BAD_REQUEST
        }
      }
    }
  }

  "As an agent" when {

    s"Calling GET $giftAidDonationsUrl" when {

      "there is no prior data" should {

        lazy val result: WSResponse = {
          dropGiftAidDB()

          emptyUserDataStub()
          insertCyaData(None)

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(giftAidDonationsUrl).withHttpHeaders(
            HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent).get()
          )
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return the page" which {
          titleCheck(Content.headingAgent)
          welshToggleCheck("English")
          h1Check(s"${Content.headingAgent} ${Content.caption}")
          captionCheck(Content.caption)
          radioButtonCheck(Content.yesText, 1)
          radioButtonCheck(Content.noText, 2)
          buttonCheck(Content.button)

          elementExtinct(Selectors.errorSummary)
          elementExtinct(Selectors.firstError)
          elementExtinct(Selectors.errorMessage)
        }

        s"have an OK($OK) status" in {
          result.status shouldBe OK
        }
      }

      "there is prior data" should {

        lazy val result: WSResponse = {
          dropGiftAidDB()

          userDataStub(IncomeSourcesModel(giftAid = Some(GiftAidSubmissionModel(Some(GiftAidPaymentsModel(
            currentYear = Some(1000.00)
          ))))), nino, taxYear)
          insertCyaData(None)

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(giftAidDonationsUrl).withHttpHeaders(
            HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
            .withFollowRedirects(false).get()
          )
        }


        "redirect the user to the CYA page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(taxYear)}"
        }
      }
    }

    s"Calling POST $giftAidDonationsUrl" when {

      "cya is finished" should {

        lazy val result: WSResponse = {
          dropGiftAidDB()

          emptyUserDataStub()
          insertCyaData(Some(cyaModelMax.copy(donationsViaGiftAid = None)))

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(giftAidDonationsUrl).withHttpHeaders(
            HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
            .withFollowRedirects(false).post(yesNoFormYes)
          )
        }

        "redirect the user to the CYA page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(taxYear)}"
        }
      }

      "the user has selected 'Yes'" should {

        lazy val result: WSResponse = {
          dropGiftAidDB()

          emptyUserDataStub()
          insertCyaData(None)

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(giftAidDonationsUrl).withHttpHeaders(
            HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
            .withFollowRedirects(false).post(yesNoFormYes)
          )
        }

        "redirect the user to the donated amount page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidDonatedAmountController.show(taxYear)}"
        }

        "update the cya data" in {
          findGiftAidDb shouldBe Some(GiftAidCYAModel(donationsViaGiftAid = Some(true)))
        }
      }

      "the user has selected 'No'" when {

        "this completes the CYA model" should {
          lazy val result: WSResponse = {
            dropGiftAidDB()

            emptyUserDataStub()
            insertCyaData(Some(completeGiftAidCYAModel.copy(donationsViaGiftAid = None)))

            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"
            ))

            authoriseAgent()
            await(wsClient.url(giftAidDonationsUrl).withHttpHeaders(
              HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
              .withFollowRedirects(false).post(yesNoFormNo)
            )
          }

          "redirect the user to the 'check your answers' page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(taxYear)}"
          }

          "update the cya data" in {
            findGiftAidDb shouldBe
              Some(completeGiftAidCYAModel.copy(
                donationsViaGiftAid = Some(false),
                donationsViaGiftAidAmount = None,
                oneOffDonationsViaGiftAid = Some(false),
                oneOffDonationsViaGiftAidAmount = None,
                overseasDonationsViaGiftAid = Some(false),
                overseasDonationsViaGiftAidAmount = None,
                overseasCharityNames = Some(Seq.empty[String]),
                addDonationToLastYear = Some(false),
                addDonationToLastYearAmount = None
              ))
          }
        }

        "this does not complete the CYA model" should {
          lazy val result: WSResponse = {
            dropGiftAidDB()

            emptyUserDataStub()
            insertCyaData(None)

            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"
            ))

            authoriseAgent()
            await(wsClient.url(giftAidDonationsUrl).withHttpHeaders(
              HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
              .withFollowRedirects(false).post(yesNoFormNo)
            )
          }

          "redirect the user to the 'Add donations to this tax year' page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear)}"
          }

          "update the cya data" in {
            findGiftAidDb shouldBe
              Some(GiftAidCYAModel(
                donationsViaGiftAid = Some(false),
                oneOffDonationsViaGiftAid = Some(false),
                overseasDonationsViaGiftAid = Some(false),
                addDonationToLastYear = Some(false)
              ))
          }
        }

        lazy val result: WSResponse = {
          dropGiftAidDB()

          emptyUserDataStub()
          insertCyaData(None)

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(giftAidDonationsUrl).withHttpHeaders(
            HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
            .withFollowRedirects(false).post(yesNoFormNo)
          )
        }

        "redirect the user to the 'Add donations to this tax year' page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear)}"
        }

        "update the cya data" in {
          findGiftAidDb shouldBe
            Some(GiftAidCYAModel(
              donationsViaGiftAid = Some(false),
              oneOffDonationsViaGiftAid = Some(false),
              overseasDonationsViaGiftAid = Some(false),
              addDonationToLastYear = Some(false)
            ))
        }
      }

      "the user has not selected an option" should {

        lazy val result: WSResponse = {

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(giftAidDonationsUrl).withHttpHeaders(
            HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
            .withFollowRedirects(false).post(yesNoFormEmpty))
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return the page" which {
          titleCheck("Error: " + Content.headingAgent)
          welshToggleCheck("English")
          h1Check(s"${Content.headingAgent} ${Content.caption}")
          captionCheck(Content.caption)
          radioButtonCheck(Content.yesText, 1)
          radioButtonCheck(Content.noText, 2)
          buttonCheck(Content.button)

          errorSummaryCheck(Content.errorTextAgent, Selectors.errorHref)
          errorAboveElementCheck(Content.errorTextAgent)
        }

        "return a BAD_REQUEST" in {
          result.status shouldBe BAD_REQUEST
        }
      }
    }
  }
}
