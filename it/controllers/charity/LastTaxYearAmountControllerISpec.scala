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
import helpers.PlaySessionCookieBaker
import models.charity.GiftAidCYAModel
import models.charity.prior.{GiftAidPaymentsModel, GiftAidSubmissionModel}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.libs.ws.WSClient
import utils.{GiftAidDatabaseHelper, IntegrationTest, ViewHelpers}

class LastTaxYearAmountControllerISpec extends IntegrationTest with ViewHelpers with GiftAidDatabaseHelper {

  object Selectors {
    val para = "label > p"
    val errorSummary = "#error-summary-title"
    val noSelectionError = ".govuk-error-summary__body > ul > li > a"
    val errorMessage = "#value-error"
  }

  object Content {
    val heading = "How much of your donation do you want to add to the last tax year?"
    val headingAgent = "How much of your client’s donation do you want to add to the last tax year?"
    val caption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val para = "Do not include the Gift Aid added to your donation."
    val paraAgent = "Do not include the Gift Aid added to your client’s donation."
    val hint = "For example, £600 or £193.54"
    val button = "Continue"

    val noSelectionError = "Enter the amount of your donation you want to add to the last tax year"
    val noSelectionErrorAgent = "Enter the amount of your client’s donation you want to add to the last tax year"
    val tooLongError = "The amount of your donation you add to the last tax year must be less than £100,000,000,000"
    val tooLongErrorAgent = "The amount of your client’s donation you add to the last tax year must be less than £100,000,000,000"
    val invalidFormatError = "Enter the amount you want to add to the last tax year in the correct format"

    val priorPageTitle = "Do you want to add any of your donations to the last tax year?"
  }

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  val taxYear: Int = 2022

  val lastTaxYearAmountUrl = s"$startUrl/$taxYear/charity/amount-added-to-last-tax-year"

  "Calling GET /charity/amount-added-to-last-tax-year" should {

    "the user is authorised" when {

      "the user is a non-agent" should {

        "return the page when there is valid CYA data" which {
          lazy val result = {
            dropGiftAidDB()

            emptyUserDataStub()
            insertCyaData(Some(GiftAidCYAModel(
              addDonationToLastYear = Some(true)
            )))

            authoriseIndividual()
            await(wsClient.url(lastTaxYearAmountUrl)
              .withHttpHeaders(xSessionId, csrfContent)
              .get())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          titleCheck(Content.heading)
          h1Check(Content.heading + " " + Content.caption)
          textOnPageCheck(Content.para, Selectors.para)
          inputFieldCheck("amount", ".govuk-input")
          hintTextCheck(Content.hint)
          captionCheck(Content.caption)
          buttonCheck(Content.button)

          elementExtinct(Selectors.errorSummary)
          elementExtinct(Selectors.noSelectionError)
          elementExtinct(Selectors.errorMessage)
        }

        "redirect the user to the overview page" when {

          "there is no session data" which {
            lazy val result = {
              dropGiftAidDB()

              emptyUserDataStub()
              insertCyaData(None)

              authoriseIndividual()
              await(wsClient.url(lastTaxYearAmountUrl)
                .withFollowRedirects(false)
                .withHttpHeaders(xSessionId, csrfContent)
                .get())
            }

            "has a status of SEE_OTHER (303)" in {
              result.status shouldBe SEE_OTHER
            }

            "redirects to the overview page" in {
              result.headers("Location").head shouldBe overviewUrl
            }
          }
        }

        "redirect the user to the donations added to last tax year page" when {

          "the session data is missing the addDonationToLastYear field" when {

            "there have been no overseas donations via gift aid" which {
              lazy val result = {
                dropGiftAidDB()

                userDataStub(IncomeSourcesModel(giftAid = Some(GiftAidSubmissionModel(Some(GiftAidPaymentsModel(
                  currentYearTreatedAsPreviousYear = Some(1000.00)
                ))))), nino, taxYear)

                insertCyaData(Some(GiftAidCYAModel(
                  overseasDonationsViaGiftAid = Some(false)
                )))

                authoriseIndividual()
                await(wsClient.url(lastTaxYearAmountUrl)
                  .withFollowRedirects(false)
                  .withHttpHeaders(xSessionId, csrfContent)
                  .get())
              }

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              "has a status of SEE_OTHER(303)" in {
                result.status shouldBe SEE_OTHER
              }

              "has the correct redirect URL" in {
                result.headers("Location").head shouldBe controllers.charity.routes.GiftAidLastTaxYearController.show(taxYear).url
              }
            }

            "there have been overseas donations via gift aid, and charity names are present" which {
              lazy val result = {
                dropGiftAidDB()

                userDataStub(IncomeSourcesModel(giftAid = Some(GiftAidSubmissionModel(Some(GiftAidPaymentsModel(
                  currentYearTreatedAsPreviousYear = Some(1000.00)
                ))))), nino, taxYear)

                insertCyaData(Some(GiftAidCYAModel(
                  overseasDonationsViaGiftAid = Some(true),
                  overseasDonationsViaGiftAidAmount = Some(123.45),
                  overseasCharityNames = Some(Seq(
                    "Whiterun Knee Fund",
                    "Champions Guild Litter Fund"
                  ))
                )))

                authoriseIndividual()
                await(wsClient.url(lastTaxYearAmountUrl)
                  .withFollowRedirects(false)
                  .withHttpHeaders(xSessionId, csrfContent)
                  .get())
              }

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              "has a status of SEE_OTHER(303)" in {
                result.status shouldBe SEE_OTHER
              }

              "has the correct redirect URL" in {
                result.headers("Location").head shouldBe controllers.charity.routes.GiftAidLastTaxYearController.show(taxYear).url
              }
            }

          }

        }

        "redirect the user to the add donations made next tax year to this tax year page" when {

          "the user has answered no to the add donations made this year to last year question" which {
            lazy val result = {
              dropGiftAidDB()

              emptyUserDataStub()
              insertCyaData(Some(GiftAidCYAModel(
                overseasDonationsViaGiftAid = Some(false),
                addDonationToLastYear = Some(false)
              )))

              authoriseIndividual()
              await(wsClient.url(lastTaxYearAmountUrl)
                .withFollowRedirects(false)
                .withHttpHeaders(xSessionId, csrfContent)
                .get())
            }

            "has a status of SEE_OTHER(303)" in {
              result.status shouldBe SEE_OTHER
            }

            "redirect to the correct URL" in {
              result.headers("Location").head shouldBe controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear).url
            }
          }

        }
      }

      "the user is an agent" should {

        lazy val result = {
          dropGiftAidDB()

          emptyUserDataStub()
          insertCyaData(Some(GiftAidCYAModel(
            addDonationToLastYear = Some(true)
          )))

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))
          authoriseAgent()
          await(wsClient.url(lastTaxYearAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
            .get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return the page" which {
          titleCheck(Content.headingAgent)
          h1Check(Content.headingAgent + " " + Content.caption)
          textOnPageCheck(Content.paraAgent, Selectors.para)
          inputFieldCheck("amount", ".govuk-input")
          hintTextCheck(Content.hint)
          captionCheck(Content.caption)
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
  }

  "calling POST" when {

    "an individual" when {

      "the form data is valid" should {

        "redirect to the donate land share securities or properties page" which {
          lazy val result = {
            dropGiftAidDB()

            emptyUserDataStub()
            insertCyaData(Some(GiftAidCYAModel(
              addDonationToLastYear = Some(true)
            )))

            authoriseIndividual()
            await(wsClient.url(lastTaxYearAmountUrl)
              .withFollowRedirects(false)
              .withHttpHeaders(xSessionId, csrfContent)
              .post(Map[String, String](
                "amount" -> "1234"
              )))
          }

          "has a status of SEE_OTHER(303)" in {
            result.status shouldBe SEE_OTHER
          }

          "has the correct redirect URL" in {
            result.headers("Location").head shouldBe controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear).url
          }
        }

      }
      
      "the form data is valid, but there is no session data" should {
        
        "redirect to the overview page" which {
          lazy val result = {
            dropGiftAidDB()

            emptyUserDataStub()
            insertCyaData(None)

            authoriseIndividual()
            await(wsClient.url(lastTaxYearAmountUrl)
              .withFollowRedirects(false)
              .withHttpHeaders(xSessionId, csrfContent)
              .post(Map[String, String](
                "amount" -> "1234"
              )))
          }

          "has a status of SEE_OTHER(303)" in {
            result.status shouldBe SEE_OTHER
          }

          "has the correct redirect URL" in {
            result.headers("Location").head shouldBe overviewUrl
          }
        }
        
      }

      "return an error" when {

        "the submitted data is empty" which {
          lazy val result = {
            authoriseIndividual()
            await(wsClient.url(lastTaxYearAmountUrl)
              .withHttpHeaders(xSessionId, csrfContent)
              .post(Map[String, String](
                "amount" -> ""
              )))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck("Error: " + Content.heading)
          h1Check(Content.heading + " " + Content.caption)
          textOnPageCheck(Content.para, Selectors.para)
          inputFieldCheck("amount", ".govuk-input")
          hintTextCheck(Content.hint)
          captionCheck(Content.caption)
          buttonCheck(Content.button)

          errorSummaryCheck(Content.noSelectionError, "#amount")
          errorAboveElementCheck(Content.noSelectionError)
        }

        "the submitted data is too long" which {
          lazy val result = {
            authoriseIndividual()
            await(wsClient.url(lastTaxYearAmountUrl)
              .withHttpHeaders(xSessionId, csrfContent)
              .post(Map(
                "amount" -> "999999999999999999999999999999999999999999999999"
              )))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck("Error: " + Content.heading)
          h1Check(Content.heading + " " + Content.caption)
          textOnPageCheck(Content.para, Selectors.para)
          inputFieldCheck("amount", ".govuk-input")
          hintTextCheck(Content.hint)
          captionCheck(Content.caption)
          buttonCheck(Content.button)

          errorSummaryCheck(Content.tooLongError, "#amount")
          errorAboveElementCheck(Content.tooLongError)
        }

        "the submitted data is in the incorrect format" which {
          lazy val result = {
            authoriseIndividual()
            await(wsClient.url(lastTaxYearAmountUrl)
              .withHttpHeaders(xSessionId, csrfContent)
              .post(Map(
                "amount" -> ".."
              )))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck("Error: " + Content.heading)
          h1Check(Content.heading + " " + Content.caption)
          textOnPageCheck(Content.para, Selectors.para)
          inputFieldCheck("amount", ".govuk-input")
          hintTextCheck(Content.hint)
          captionCheck(Content.caption)
          buttonCheck(Content.button)

          errorSummaryCheck(Content.invalidFormatError, "#amount")
          errorAboveElementCheck(Content.invalidFormatError)
        }
      }

    }

    "an agent" when {

      "the form data is valid" should {

        "redirect to the gift aid shares, securities, land or properties yes/no page" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890"
          ))

          lazy val result = {
            dropGiftAidDB()
            
            emptyUserDataStub()
            insertCyaData(Some(GiftAidCYAModel()))
            
            authoriseAgent()
            await(
              wsClient
                .url(lastTaxYearAmountUrl)
                .withHttpHeaders(
                  HeaderNames.COOKIE -> playSessionCookies,
                  xSessionId, csrfContent
                )
                .withFollowRedirects(false)
                .post(Map[String, String](
                  "amount" -> "1234"
                )))
          }

          "has a status of SEE_OTHER" in {
            result.status shouldBe SEE_OTHER
          }
          
          "has the correct URL" in {
            result.headers("Location").head shouldBe controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear).url
          }
        }

      }

      "return an error" when {

        "the submitted data is empty" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890"
          ))

          lazy val result = {
            authoriseAgent()
            await(wsClient.url(lastTaxYearAmountUrl).withHttpHeaders(
              HeaderNames.COOKIE -> playSessionCookies,
              xSessionId, csrfContent
            ).post(Map[String, String](
              "amount" -> ""
            )))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck("Error: " + Content.headingAgent)
          h1Check(Content.headingAgent + " " + Content.caption)
          textOnPageCheck(Content.paraAgent, Selectors.para)
          inputFieldCheck("amount", ".govuk-input")
          hintTextCheck(Content.hint)
          captionCheck(Content.caption)
          buttonCheck(Content.button)

          errorSummaryCheck(Content.noSelectionErrorAgent, "#amount")
          errorAboveElementCheck(Content.noSelectionErrorAgent)
        }

        "the submitted data is too long" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890"
          ))

          lazy val result = {
            authoriseAgent()
            await(wsClient.url(lastTaxYearAmountUrl).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, xSessionId, csrfContent).post(Map(
              "amount" -> "999999999999999999999999999999999999999999999999"
            )))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck("Error: " + Content.headingAgent)
          h1Check(Content.headingAgent + " " + Content.caption)
          textOnPageCheck(Content.paraAgent, Selectors.para)
          inputFieldCheck("amount", ".govuk-input")
          hintTextCheck(Content.hint)
          captionCheck(Content.caption)
          buttonCheck(Content.button)

          errorSummaryCheck(Content.tooLongErrorAgent, "#amount")
          errorAboveElementCheck(Content.tooLongErrorAgent)
        }

        "the submitted data is in the incorrect format" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890"
          ))

          lazy val result = {
            authoriseAgent()
            await(wsClient.url(lastTaxYearAmountUrl).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, xSessionId, csrfContent).post(Map(
              "amount" -> ".."
            )))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck("Error: " + Content.headingAgent)
          h1Check(Content.headingAgent + " " + Content.caption)
          textOnPageCheck(Content.paraAgent, Selectors.para)
          inputFieldCheck("amount", ".govuk-input")
          hintTextCheck(Content.hint)
          captionCheck(Content.caption)
          buttonCheck(Content.button)

          errorSummaryCheck(Content.invalidFormatError, "#amount")
          errorAboveElementCheck(Content.invalidFormatError)
        }
      }

    }

  }

}
