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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{GiftAidDatabaseHelper, IntegrationTest, ViewHelpers}
import play.api.http.Status._
import play.api.mvc.Result
import play.api.test.FakeRequest


class GiftAidAppendNextYearTaxAmountControllerSpec extends IntegrationTest with ViewHelpers with GiftAidDatabaseHelper {

  val defaultTaxYear = 2022
  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  def url(taxYear: Int, someTaxYear: Int): String =
    s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/amount-after-5-april-$someTaxYear-added-to-this-tax-year"

  def url: String = url(defaultTaxYear, defaultTaxYear)

  object Selectors {
    val titleSelector = "title"
    val inputField = ".govuk-input"
  }

  object ExpectedResults {
    val headingIndividual: String = "How much of the donations you made after 5 April 2022 do you want to add to this tax year?"
    val headingAgent: String = "How much of the donations your client made after 5 April 2022 do you want to add to this tax year?"
    val hintText: String = "For example, £600 or £193.54"
    val caption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val button = "Continue"
    val inputName = "amount"

    val previousPageTitle = "Do you want to add any donations made after 5 April 2022 to this tax year?"
  }

  object ExpectedErrors {
    val tooLong = "The amount of your donation made after 5 April 2022 you add to the last tax year must be less than £100,000,000,000"
    val emptyField = "Enter the amount of your donation made after 5 April 2022 you want to add to this tax year"
    val incorrectFormat = "Enter the amount you want to add to this tax year in the correct format"

    val tooLongAgent = "The amount of your client’s donation made after 5 April 2022 you add to the last tax year must be less than £100,000,000,000"
    val emptyFieldAgent = "Enter the amount of your client’s donation made after 5 April 2022 you want to add to this tax year"

    val errorHref = "#amount"
  }

  lazy val controller: GiftAidAppendNextYearTaxAmountController = app.injector.instanceOf[GiftAidAppendNextYearTaxAmountController]

  ".show" when {

    "the dates provided are different" should {

      "redirect to a correct URL" in {
        val result: Result = {
          authoriseIndividual()
          await(controller.show(defaultTaxYear, defaultTaxYear + 1)(FakeRequest().withHeaders(xSessionId, csrfContent).withSession(
            SessionValues.TAX_YEAR -> defaultTaxYear.toString
          )))
        }

        val url = "/income-through-software/return/personal-income/2022/charity/amount-after-5-april-2022-added-to-this-tax-year"

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
          await(controller.submit(defaultTaxYear, defaultTaxYear + 1)(FakeRequest().withHeaders(xSessionId, csrfContent)
            .withFormUrlEncodedBody("amount" -> "123")
            .withSession(
              SessionValues.TAX_YEAR -> defaultTaxYear.toString
            )))
        }

        val url = "/income-through-software/return/personal-income/2022/charity/amount-after-5-april-2022-added-to-this-tax-year"

        result.header.status shouldBe SEE_OTHER
        result.header.headers("Location") shouldBe url
      }

    }

  }

  "calling GET" when {

    "an individual" should {

      "return a page when valid session data is present" which {
        lazy val result: WSResponse = {
          dropGiftAidDB()

          emptyUserDataStub()
          insertCyaData(Some(GiftAidCYAModel(
            addDonationToThisYear = Some(true)
          )))

          authoriseIndividual()
          await(
            wsClient.url(url).withHttpHeaders(xSessionId, csrfContent).get()
          )
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(ExpectedResults.headingIndividual)
        h1Check(ExpectedResults.headingIndividual + " " + ExpectedResults.caption)
        inputFieldCheck(ExpectedResults.inputName, Selectors.inputField)
        hintTextCheck(ExpectedResults.hintText)
        captionCheck(ExpectedResults.caption)
        buttonCheck(ExpectedResults.button)
      }

      "redirect to the overview page" when {

        "there is no session data present" which {
          lazy val result = {
            dropGiftAidDB()

            emptyUserDataStub()
            insertCyaData(None)

            authoriseIndividual()
            await(
              wsClient.url(url)
                .withHttpHeaders(xSessionId, csrfContent)
                .withFollowRedirects(false)
                .get()
            )
          }

          "has a status of SEE_OTHER (303)" in {
            result.status shouldBe SEE_OTHER
          }

          "has the redirect location as overview" in {
            result.headers("Location").head shouldBe overviewUrl
          }
        }
      }

      "redirect to the donations to previous tax year controller" when {

        "the addDonationToThisYear field is empty" which {
          lazy val result = {
            dropGiftAidDB()

            emptyUserDataStub()
            insertCyaData(Some(GiftAidCYAModel(
              donationsViaGiftAid = Some(true),
              addDonationToLastYear = Some(false)
            )))

            authoriseIndividual()
            await(
              wsClient.url(url)
                .withHttpHeaders(xSessionId, csrfContent)
                .withFollowRedirects(false)
                .get()
            )
          }

          "has a status of SEE_OTHER (303)" in {
            result.status shouldBe SEE_OTHER
          }

          "has the correct redirect URL" in {
            result.headers("Location").head shouldBe controllers.charity.routes.DonationsToPreviousTaxYearController.show(defaultTaxYear, defaultTaxYear).url
          }
        }

      }

      "redirect to the did you donate shares, securities, land or property page" when {

        "addDonationToThisYear is false" which {
          lazy val result = {
            dropGiftAidDB()

            emptyUserDataStub()
            insertCyaData(Some(GiftAidCYAModel(
              addDonationToThisYear = Some(false)
            )))

            authoriseIndividual()
            await(
              wsClient.url(url)
                .withHttpHeaders(xSessionId, csrfContent)
                .withFollowRedirects(false)
                .get()
            )
          }

          "has a status of SEE_OTHER (303)" in {
            result.status shouldBe SEE_OTHER
          }

          "has a redirect location of the shares, securities, land or property yes/no page" in {
            result.headers("Location").head shouldBe controllers.charity.routes.GiftAidSharesSecuritiesLandPropertyDonationController.show(defaultTaxYear).url
          }
        }

      }
    }

    "an agent" should {

      "return a page" which {
        lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> defaultTaxYear.toString,
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> "1234567890"
        ))

        lazy val result: WSResponse = {
          dropGiftAidDB()

          emptyUserDataStub()
          insertCyaData(Some(GiftAidCYAModel(
            addDonationToThisYear = Some(true)
          )))

          authoriseAgent()
          await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, xSessionId, csrfContent).get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(ExpectedResults.headingAgent)
        h1Check(ExpectedResults.headingAgent + " " + ExpectedResults.caption)
        inputFieldCheck(ExpectedResults.inputName, Selectors.inputField)
        hintTextCheck(ExpectedResults.hintText)
        captionCheck(ExpectedResults.caption)
        buttonCheck(ExpectedResults.button)
      }

    }

  }

  "calling POST" when {

    "an individual" when {

      "the form data is valid" when {
        val validAmount = 1234

        "this completes the cya model" should {

          "redirect the user to the check your answers page" which {
            lazy val result: WSResponse = {
              dropGiftAidDB()

              insertCyaData(Some(completeGiftAidCYAModel))

              authoriseIndividual()
              await(
                wsClient.url(url)
                  .withHttpHeaders(xSessionId, csrfContent)
                  .withFollowRedirects(false)
                  .post(Map[String, String]("amount" -> s"$validAmount"))
              )
            }

            "has a status of SEE_OTHER (303)" in {
              result.status shouldBe SEE_OTHER
            }

            "has the redirect url to the check your answers page" in {
              result.headers("Location").head shouldBe controllers.charity.routes.GiftAidCYAController.show(defaultTaxYear).url
            }

            "update the cya data" in {
              findGiftAidDb shouldBe Some(completeGiftAidCYAModel.copy(addDonationToThisYearAmount = Some(validAmount)))
            }
          }
        }

        "this does not complete the cya model" should {

          "redirect the user to the shares, securities, land or property page" which {
            lazy val result: WSResponse = {
              dropGiftAidDB()

              insertCyaData(Some(GiftAidCYAModel()))

              authoriseIndividual()
              await(
                wsClient.url(url)
                  .withHttpHeaders(xSessionId, csrfContent)
                  .withFollowRedirects(false)
                  .post(Map[String, String]("amount" -> "1234"))
              )
            }

            "has a status of SEE_OTHER (303)" in {
              result.status shouldBe SEE_OTHER
            }

            "has the redirect url to the shares, securities, land or property yes/no page" in {
              result.headers("Location").head shouldBe controllers.charity.routes.GiftAidSharesSecuritiesLandPropertyDonationController.show(defaultTaxYear).url
            }

            "update the cya data" in {
              findGiftAidDb shouldBe Some(GiftAidCYAModel(addDonationToThisYearAmount = Some(validAmount)))
            }
          }
        }
      }

      "there is no session data" should {

        "redirect to the overview page" which {
          lazy val result: WSResponse = {
            dropGiftAidDB()

            insertCyaData(None)

            authoriseIndividual()
            await(
              wsClient.url(url)
                .withHttpHeaders(xSessionId, csrfContent)
                .withFollowRedirects(false)
                .post(Map[String, String]("amount" -> "1234"))
            )
          }

          "has a status of SEE_OTHER (303)" in {
            result.status shouldBe SEE_OTHER
          }

          "has the redirect url to the overview page" in {
            result.headers("Location").head shouldBe overviewUrl
          }
        }
      }

      "return an error" when {

        "the submitted data is empty" which {
          lazy val result: WSResponse = {
            dropGiftAidDB()

            authoriseIndividual()
            await(wsClient.url(url).withHttpHeaders(xSessionId, csrfContent).post(Map[String, String](
              "amount" -> ""
            )))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck("Error: " + ExpectedResults.headingIndividual)
          h1Check(ExpectedResults.headingIndividual + " " + ExpectedResults.caption)
          inputFieldCheck(ExpectedResults.inputName, Selectors.inputField)
          hintTextCheck(ExpectedResults.hintText)
          captionCheck(ExpectedResults.caption)
          buttonCheck(ExpectedResults.button)

          errorSummaryCheck(ExpectedErrors.emptyField, ExpectedErrors.errorHref)
          errorAboveElementCheck(ExpectedErrors.emptyField)
        }

        "the submitted data is too long" which {
          lazy val result: WSResponse = {
            dropGiftAidDB()

            authoriseIndividual()
            await(wsClient.url(url).withHttpHeaders(xSessionId, csrfContent).post(Map(
              "amount" -> "999999999999999999999999999999999999999999999999"
            )))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck("Error: " + ExpectedResults.headingIndividual)
          h1Check(ExpectedResults.headingIndividual + " " + ExpectedResults.caption)
          inputFieldCheck(ExpectedResults.inputName, Selectors.inputField)
          hintTextCheck(ExpectedResults.hintText)
          captionCheck(ExpectedResults.caption)
          buttonCheck(ExpectedResults.button)

          errorSummaryCheck(ExpectedErrors.tooLong, ExpectedErrors.errorHref)
          errorAboveElementCheck(ExpectedErrors.tooLong)
        }

        "the submitted data is in the incorrect format" which {
          lazy val result: WSResponse = {
            dropGiftAidDB()

            authoriseIndividual()
            await(wsClient.url(url).withHttpHeaders(xSessionId, csrfContent).post(Map(
              "amount" -> ":@~{}<>?"
            )))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck("Error: " + ExpectedResults.headingIndividual)
          h1Check(ExpectedResults.headingIndividual + " " + ExpectedResults.caption)
          inputFieldCheck(ExpectedResults.inputName, Selectors.inputField)
          hintTextCheck(ExpectedResults.hintText)
          captionCheck(ExpectedResults.caption)
          buttonCheck(ExpectedResults.button)

          errorSummaryCheck(ExpectedErrors.incorrectFormat, ExpectedErrors.errorHref)
          errorAboveElementCheck(ExpectedErrors.incorrectFormat)
        }
      }

    }

    "an agent" when {

      "the form data is valid" should {

        "redirect to the gift aid shares, securities, land or properties page" in {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> defaultTaxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890"
          ))

          lazy val result: WSResponse = {
            dropGiftAidDB()

            emptyUserDataStub()
            insertCyaData(Some(GiftAidCYAModel()))

            authoriseAgent()
            await(
              wsClient.url(url)
                .withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, xSessionId, csrfContent)
                .withFollowRedirects(false)
                .post(Map[String, String](
                  "amount" -> "1234"
                )))
          }

          result.status shouldBe SEE_OTHER
        }

      }

      "return an error" when {

        "the submitted data is empty" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> defaultTaxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890"
          ))

          lazy val result: WSResponse = {
            dropGiftAidDB()

            authoriseAgent()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, xSessionId, csrfContent).post(Map[String, String](
              "amount" -> ""
            )))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck("Error: " + ExpectedResults.headingAgent)
          h1Check(ExpectedResults.headingAgent + " " + ExpectedResults.caption)
          inputFieldCheck(ExpectedResults.inputName, Selectors.inputField)
          hintTextCheck(ExpectedResults.hintText)
          captionCheck(ExpectedResults.caption)
          buttonCheck(ExpectedResults.button)

          errorSummaryCheck(ExpectedErrors.emptyFieldAgent, ExpectedErrors.errorHref)
          errorAboveElementCheck(ExpectedErrors.emptyFieldAgent)
        }

        "the submitted data is too long" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> defaultTaxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890"
          ))

          lazy val result: WSResponse = {
            dropGiftAidDB()

            authoriseAgent()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, xSessionId, csrfContent).post(Map(
              "amount" -> "999999999999999999999999999999999999999999999999"
            )))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck("Error: " + ExpectedResults.headingAgent)
          h1Check(ExpectedResults.headingAgent + " " + ExpectedResults.caption)
          inputFieldCheck(ExpectedResults.inputName, Selectors.inputField)
          hintTextCheck(ExpectedResults.hintText)
          captionCheck(ExpectedResults.caption)
          buttonCheck(ExpectedResults.button)

          errorSummaryCheck(ExpectedErrors.tooLongAgent, ExpectedErrors.errorHref)
          errorAboveElementCheck(ExpectedErrors.tooLongAgent)
        }

        "the submitted data is in the incorrect format" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> defaultTaxYear.toString,
            SessionValues.CLIENT_NINO -> "AA123456A",
            SessionValues.CLIENT_MTDITID -> "1234567890"
          ))

          lazy val result: WSResponse = {
            dropGiftAidDB()

            authoriseAgent()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, xSessionId, csrfContent).post(Map(
              "amount" -> ":@~{}<>?"
            )))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck("Error: " + ExpectedResults.headingAgent)
          h1Check(ExpectedResults.headingAgent + " " + ExpectedResults.caption)
          inputFieldCheck(ExpectedResults.inputName, Selectors.inputField)
          hintTextCheck(ExpectedResults.hintText)
          captionCheck(ExpectedResults.caption)
          buttonCheck(ExpectedResults.button)

          errorSummaryCheck(ExpectedErrors.incorrectFormat, ExpectedErrors.errorHref)
          errorAboveElementCheck(ExpectedErrors.incorrectFormat)
        }
      }

    }

  }

}
