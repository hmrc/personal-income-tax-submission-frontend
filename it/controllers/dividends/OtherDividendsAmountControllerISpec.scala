

package controllers.dividends

import common.SessionValues
import forms.OtherDividendsAmountForm
import helpers.PlaySessionCookieBaker
import models.DividendsCheckYourAnswersModel
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.{WSClient, WSResponse}
import utils.IntegrationTest

class OtherDividendsAmountControllerISpec extends IntegrationTest {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  lazy val controller: OtherDividendsAmountController = app.injector.instanceOf[OtherDividendsAmountController]

  "as an individual" when {

    ".show" should {

      "returns an action" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/dividends/other-dividends-amount").get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

      }

    }

    ".submit" should {

      s"return an OK($OK) status" in {
        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(otherDividends = true).asJsonString
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(
            wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/dividends/other-dividends-amount")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map(OtherDividendsAmountForm.otherDividendsAmount -> "123"))
          )
        }

        result.status shouldBe OK
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status" in {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/dividends/other-dividends-amount").post(Map[String, String]()))
        }

        result.status shouldBe BAD_REQUEST
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
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/dividends/other-dividends-amount")
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
              SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = true).asJsonString
            ))

            authoriseAgent()
            await(
              wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/dividends/other-dividends-amount")
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
                .post(Map(OtherDividendsAmountForm.otherDividendsAmount -> "123"))
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
            await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/dividends/other-dividends-amount")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map[String, String]()))
          }

          result.status shouldBe BAD_REQUEST
        }
      }

    }

  }

}

