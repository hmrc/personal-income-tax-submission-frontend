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

package controllers.dividends

import common.SessionValues
import connectors.DividendsSubmissionConnector
import helpers.PlaySessionCookieBaker
import models._
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import utils.IntegrationTest

class DividendsCYAControllerISpec extends IntegrationTest {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val connector: DividendsSubmissionConnector = app.injector.instanceOf[DividendsSubmissionConnector]

  val taxYear = 2022

  val dividends: BigDecimal = 10
  val dividendsCheckYourAnswersUrl = s"$startUrl/$taxYear/dividends/check-income-from-dividends"

  lazy val dividendsBody: DividendsSubmissionModel = DividendsSubmissionModel(
    Some(dividends),
    Some(dividends)
  )

  ".show" should {

    s"returns an action" when {

      "there is CYA data in session" which {

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.DIVIDENDS_CYA -> Json.prettyPrint(Json.toJson(DividendsCheckYourAnswersModel()))
        ))

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(dividendsCheckYourAnswersUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .get())
        }

        s"has an OK($OK) status" in {
          result.status shouldBe OK
        }

      }

      "there is no CYA data in session" which {
        lazy val result = {
          authoriseIndividual()
          stubGet(s"/income-through-software/return/$taxYear/view", OK, "<title>Overview Page</title>")


          await(wsClient.url(dividendsCheckYourAnswersUrl)
            .get())
        }

        s"has an OK($OK) status" in {
          result.status shouldBe OK
        }

      }

      "the authorization fails" which {
        lazy val result = {
          authoriseIndividualUnauthorized()
          stubGet(s"/income-through-software/return/$taxYear/view", OK, "<title>Overview Page</title>")


          await(wsClient.url(dividendsCheckYourAnswersUrl)
            .get())
        }

        s"has an OK($UNAUTHORIZED) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

  }

  ".submit" should {

    "return an action" which {

      s"has an OK(200) status" in {
        authoriseIndividual()
        stubPut(s"/income-tax-dividends/income-tax/nino/AA123456A/sources\\?taxYear=$taxYear", NO_CONTENT, "{}")
        stubGet("/income-through-software/return/2022/view", OK, "{}")

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.DIVIDENDS_CYA -> Json.prettyPrint(Json.toJson(DividendsCheckYourAnswersModel(
            ukDividends = Some(true),
            Some(dividends),
            otherUkDividends = Some(true),
            Some(dividends)
          ))),
        ))

        val result: WSResponse = await(wsClient.url(dividendsCheckYourAnswersUrl)
          .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
          .post("{}"))

        result.status shouldBe OK
      }

      s"handle no nino is in the enrolments" in {
        lazy val result = {
          authoriseIndividual(false)
          stubGet(s"/income-through-software/return/$taxYear/view", OK, "")
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.DIVIDENDS_CYA -> Json.prettyPrint(Json.toJson(DividendsCheckYourAnswersModel(
              ukDividends = Some(true),
              Some(dividends),
              otherUkDividends = Some(true),
              Some(dividends)
            ))),
          ))

          await(wsClient.url(dividendsCheckYourAnswersUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post("{}"))
        }

        result.status shouldBe NOT_FOUND
      }

      "the authorization fails" in {
        lazy val result = {
          authoriseIndividualUnauthorized()
          stubGet(s"/income-through-software/return/$taxYear/view", OK, "")
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.DIVIDENDS_CYA -> Json.prettyPrint(Json.toJson(DividendsCheckYourAnswersModel(ukDividends = Some(true),
              Some(dividends),
              otherUkDividends = Some(true),
              Some(dividends)))),
          ))
          await(wsClient.url(dividendsCheckYourAnswersUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post("{}"))
        }

        result.status shouldBe UNAUTHORIZED
      }
    }

    "return a service is unavailable" when {

      "one is retrieved from the endpoint" in {
        authoriseIndividual()
        stubPut(s"/income-tax-dividends/income-tax/nino/AA123456A/sources\\?taxYear=$taxYear", SERVICE_UNAVAILABLE, "{}")

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.DIVIDENDS_CYA -> Json.prettyPrint(Json.toJson(DividendsCheckYourAnswersModel(
            ukDividends = Some(true),
            Some(dividends),
            otherUkDividends = Some(true),
            Some(dividends)
          ))),
        ))

        val result: WSResponse = await(wsClient.url(dividendsCheckYourAnswersUrl)
          .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
          .post("{}"))

        result.status shouldBe SERVICE_UNAVAILABLE
      }

    }

    "return an internal server error" when {

      "an unhandled response is returned from the income-tax backend" in {
        authoriseIndividual()
        stubPut(s"/income-tax-dividends/income-tax/nino/AA123456A/sources\\?taxYear=$taxYear", IM_A_TEAPOT, "{}")

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.DIVIDENDS_CYA -> Json.prettyPrint(Json.toJson(DividendsCheckYourAnswersModel(
            ukDividends = Some(true),
            Some(dividends),
            otherUkDividends = Some(true),
            Some(dividends)
          ))),
        ))

        val result: WSResponse = await(wsClient.url(dividendsCheckYourAnswersUrl)
          .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
          .post("{}"))

        result.status shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
