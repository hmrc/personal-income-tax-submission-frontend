

package controllers.dividends

import common.SessionValues
import forms.UkDividendsAmountForm
import helpers.PlaySessionCookieBaker
import models.DividendsCheckYourAnswersModel
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.{WSClient, WSResponse}
import utils.IntegrationTest

class UkDividendsAmountControllerISpec extends IntegrationTest {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  lazy val controller: UkDividendsAmountController = app.injector.instanceOf[UkDividendsAmountController]

  "as an individual" when {

    ".show" should {

      "returns an action without data in session" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/2020/dividends/uk-dividends-amount").get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

      }

      "returns an action when data is in session" which {

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(500)).asJsonString
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/2020/dividends/uk-dividends-amount")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

      }

      "returns an action when the auth call fails" which {
        lazy val result: WSResponse = {
          authoriseIndividualUnauthorized()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/2020/dividends/uk-dividends-amount").get())
        }

        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }

      }

    }

    ".submit" should {

      s"return an OK($OK) status" in {

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true)).asJsonString
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(
            wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/2020/dividends/uk-dividends-amount")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map(UkDividendsAmountForm.ukDividendsAmount -> "123"))
          )
        }

        result.status shouldBe OK
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status" in {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/2020/dividends/uk-dividends-amount").post(Map[String, String]()))
        }

        result.status shouldBe BAD_REQUEST
      }

      s"return an UNAUTHORIZED($UNAUTHORIZED) status" in {
        lazy val result: WSResponse = {
          authoriseIndividualUnauthorized()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/2020/dividends/uk-dividends-amount").post(Map[String, String]()))
        }

        result.status shouldBe UNAUTHORIZED
      }

    }

  }

  "as an agent" when {

    ".show" should {

      "returns an action" which {
        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890"
          ))

          authoriseAgent()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/2020/dividends/uk-dividends-amount")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie)
            .get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }
      }

      "returns an action when auth call fails" which {
        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890"
          ))

          authoriseAgentUnauthorized()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/2020/dividends/uk-dividends-amount")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie)
            .get())
        }

        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }
    }

    ".submit" should {

      s"return an OK($OK) status" when {

        "there is form data" in {
          lazy val result: WSResponse = {
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true)).asJsonString
            ))

            authoriseAgent()
            await(
              wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/2020/dividends/uk-dividends-amount")
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
                .post(Map(UkDividendsAmountForm.ukDividendsAmount -> "123"))
            )
          }

          result.status shouldBe OK
        }
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status" when {

        "there is no form data" in {
          lazy val result: WSResponse = {
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890"
            ))

            authoriseAgent()
            await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/2020/dividends/uk-dividends-amount")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map[String, String]()))
          }

          result.status shouldBe BAD_REQUEST
        }
      }

      s"return an UNAUTHORIZED($UNAUTHORIZED) status" in {
        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890"
          ))

          authoriseAgentUnauthorized()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/2020/dividends/uk-dividends-amount")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post(Map[String, String]()))
        }

        result.status shouldBe UNAUTHORIZED
      }

    }

  }

}

