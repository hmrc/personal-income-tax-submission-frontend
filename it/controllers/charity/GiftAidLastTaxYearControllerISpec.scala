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
import common.SessionValues.GIFT_AID_PRIOR_SUB
import forms.YesNoForm
import helpers.PlaySessionCookieBaker
import models.giftAid.{GiftAidPaymentsModel, GiftAidSubmissionModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{IntegrationTest, ViewHelpers}

class GiftAidLastTaxYearControllerISpec extends IntegrationTest with ViewHelpers {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  lazy val controller: GiftAidLastTaxYearController = app.injector.instanceOf[GiftAidLastTaxYearController]
  val taxYear: Int = 2022

  object IndividualExpected {
    val expectedTitle: String = "Do you want to add any of your donations to the last tax year?"
    val expectedH1: String = "Do you want to add any of your donations to the last tax year?"
    val expectedError: String = "Select yes to add any of your donations to the last tax year"
    val expectedContent1: String = "You told us you donated £150 to charity by using Gift Aid. You can add some of this donation" +
      " to the 6 April 2020 to 5 April 2021 tax year."
    val expectedContent2: String = "You might want to do this if you paid higher rate tax last year but will not this year."
  }

  object AgentExpected {
    val expectedTitle: String = "Do you want to add any of your client’s donations to the last tax year?"
    val expectedH1: String = "Do you want to add any of your client’s donations to the last tax year?"
    val expectedError: String = "Select yes to add any of your client’s donations to the last tax year"
    val expectedContent1: String = "You told us your client donated £150 to charity by using Gift Aid. You can add some of this donation" +
      " to the 6 April 2020 to 5 April 2021 tax year."
    val expectedContent2: String = "You might want to do this if your client paid higher rate tax last year but will not this year."
  }

  val expectedCaption: String = "Donations to charity for 6 April 2021 to 5 April 2022"
  val yesText = "Yes"
  val noText = "No"
  val expectedContinue = "Continue"

  val continueSelector = "#continue"
  val captionSelector: String = "#main-content > div > div > form > div > fieldset > legend > header > p"
  val contentSelector1: String = "#main-content > div > div > form > div > fieldset > legend > div > p:nth-child(1)"
  val contentSelector2: String = "#main-content > div > div > form > div > fieldset > legend > div > p:nth-child(2)"
  val errorSummaryHref = "#value"

  val testModel: GiftAidSubmissionModel =
    GiftAidSubmissionModel(Some(GiftAidPaymentsModel(Some(List("JaneDoe")), None, None, Some(150.00), None, None)),None)


    "as an individual" when {
    import IndividualExpected._

      val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
        GIFT_AID_PRIOR_SUB -> Json.toJson(testModel).toString()
      ))
      ".show" should {

        "returns an action" which {
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/add-charity-donations-to-last-tax-year")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .get())
          }
          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedTitle)
          welshToggleCheck("English")
          h1Check(expectedH1)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(expectedContent1, contentSelector1)
          textOnPageCheck(expectedContent2, contentSelector2)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(expectedContinue, continueSelector)

        }
        "returns an redirect" which {
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/add-charity-donations-to-last-tax-year")
              .withHttpHeaders("Csrf-Token" -> "nocheck")
              .get())
          }
          "has an Bad_request status" in {
            result.status shouldBe NOT_FOUND
            result.uri.toString shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
          }
        }

      }

      ".submit" should {

        s"return an OK($OK) status" which {
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(
              wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/add-charity-donations-to-last-tax-year")
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
            )
          }
          "has an OK(200) status" in {
            result.status shouldBe OK
          }
        }

        s"return a BAD_REQUEST($BAD_REQUEST) status" which {
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/add-charity-donations-to-last-tax-year")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map[String, String]()))
          }
          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          errorSummaryCheck(expectedError,errorSummaryHref)
          errorAboveElementCheck(expectedError)
        }
        s"return a redirect when without session data" which {
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/add-charity-donations-to-last-tax-year")
              .withHttpHeaders("Csrf-Token" -> "nocheck")
              .post(Map[String, String]()))
          }

          "has an NOT_FOUND status and redirect to overview page" in {
            result.status shouldBe NOT_FOUND
            result.uri.toString shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
          }

        }

      }

    }

  "as an agent" when {
  import AgentExpected._
    ".show" should {

      "returns an action" which {
        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A",
            GIFT_AID_PRIOR_SUB -> Json.toJson(testModel).toString()
          ))

          authoriseAgent()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/add-charity-donations-to-last-tax-year")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie)
            .get())
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an OK(200) status" in {
          result.status shouldBe OK
        }
        titleCheck(expectedTitle)
        welshToggleCheck("English")
        h1Check(expectedH1)
        textOnPageCheck(expectedCaption, captionSelector)
        textOnPageCheck(expectedContent1, contentSelector1)
        textOnPageCheck(expectedContent2, contentSelector2)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(expectedContinue, continueSelector)
      }
      "returns an redirect" which {
        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A",
          ))

          authoriseAgent()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/add-charity-donations-to-last-tax-year")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie)
            .get())
        }
        "has an NOT_FOUND status" in {
          result.status shouldBe NOT_FOUND
          result.uri.toString shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
        }
      }
    }

    ".submit" should {

      s"return an OK($OK) status" when {

        "there is form data" in {
          lazy val result: WSResponse = {
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A",
              GIFT_AID_PRIOR_SUB -> Json.toJson(testModel).toString()
            ))

            authoriseAgent()
            await(
              wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/add-charity-donations-to-last-tax-year")
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
            )
          }

          result.status shouldBe OK
        }
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status" when {

        "there is no form data" which {
          lazy val result: WSResponse = {
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A",
              GIFT_AID_PRIOR_SUB -> Json.toJson(testModel).toString()
            ))

            authoriseAgent()
            await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/add-charity-donations-to-last-tax-year")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map[String, String]()))
          }
          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an BAD_REQUEST status" in {
            result.status shouldBe BAD_REQUEST
          }

          errorSummaryCheck(expectedError,errorSummaryHref)
          errorAboveElementCheck(expectedError)
        }
      }
      s"return a redirect when without session data" which {
        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A",
          ))

          authoriseAgent()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/add-charity-donations-to-last-tax-year")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post(Map[String, String]()))
        }

        "has an NOT_FOUND status and redirect to overview page" in {
          result.status shouldBe NOT_FOUND
          result.uri.toString shouldBe appConfig.incomeTaxSubmissionOverviewUrl(taxYear)
        }

      }

    }

  }

}
