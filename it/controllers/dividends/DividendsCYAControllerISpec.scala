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
import play.api.libs.ws.WSClient
import utils.IntegrationTest

class DividendsCYAControllerISpec extends IntegrationTest {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val connector: DividendsSubmissionConnector = app.injector.instanceOf[DividendsSubmissionConnector]

  val taxYear = 2022
  val mtdidid = "1234567890"
  val nino = "AA123456A"

  val dividends: BigDecimal = 10

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
          await(wsClient.url(s"$startUrl/$taxYear/dividends/check-your-answers")
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


          await(wsClient.url(s"$startUrl/$taxYear/dividends/check-your-answers")
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


          await(wsClient.url(s"$startUrl/2020/dividends/check-your-answers")
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

      s"has an OK($OK) status" in {

        val responseBody = DividendsSubmissionModel(
          Some(dividends),
          Some(dividends))

        val connector = app.injector.instanceOf[DividendsSubmissionConnector]
        stubPut(s"/income-tax-dividends/income-tax/nino/AA123456A/sources\\?taxYear=$taxYear&mtditid=1234567890", NO_CONTENT, "{}")
        val result = await(connector.submitDividends(responseBody, "AA123456A", "1234567890", taxYear))

        result shouldBe Right(DividendsResponseModel(NO_CONTENT))
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

          await(wsClient.url(s"$startUrl/$taxYear/dividends/check-your-answers")
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
          await(wsClient.url(s"$startUrl/$taxYear/dividends/check-your-answers")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post("{}"))
        }

        result.status shouldBe UNAUTHORIZED
      }
    }

    "return a service unavailable response" when {

      "one is retrieved from the endpoint" in {

        val responseBody = Json.obj(
          "code" -> "SERVICE_UNAVAILABLE",
          "reason" -> "the service is currently unavailable"
        )

        stubPut(s"/income-tax-dividends/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=$mtdidid", SERVICE_UNAVAILABLE, responseBody.toString())

        val result = await(connector.submitDividends(dividendsBody, nino, mtdidid, taxYear))

        result shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("SERVICE_UNAVAILABLE", "the service is currently unavailable")))
      }
    }

    "return an unexpected response" when {

      "one is retrieved from the endpoint" in {

        val responseBody = Json.obj(
          "code" -> "INTERNAL_SERVER_ERROR",
          "reason" -> "unexpected status returned from DES"
        )

        stubPut(s"/income-tax-dividends/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=$mtdidid", CREATED, responseBody.toString())

        val result = await(connector.submitDividends(dividendsBody, nino, mtdidid, taxYear))

        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "unexpected status returned from DES")))
      }
    }
  }
}
