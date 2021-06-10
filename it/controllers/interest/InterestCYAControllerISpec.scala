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

package controllers.interest

import com.github.tomakehurst.wiremock.http.HttpHeader
import common.SessionValues
import helpers.PlaySessionCookieBaker
import models.interest.{InterestAccountModel, InterestCYAModel}
import play.api.http.HeaderNames
import play.api.http.Status.{NOT_FOUND, NO_CONTENT, OK, UNAUTHORIZED}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import utils.IntegrationTest

class InterestCYAControllerISpec extends IntegrationTest{

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  val taxYear: Int = 2022
  val amount: BigDecimal = 25

  ".show" should {

    s"returns an action" when {

      "there is CYA data in session" which {

        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", amount)))
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(s"$startUrl/$taxYear/interest/check-interest")
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


          await(wsClient.url(s"$startUrl/$taxYear/interest/check-interest")
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


          await(wsClient.url(s"$startUrl/$taxYear/interest/check-interest")
            .get())
        }

        s"has an OK($UNAUTHORIZED) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

  }

  ".post" should {

    val expectedHeaders = Seq(new HttpHeader("mtditid", mtditid))

    "return an action" which {

      s"has an OK($OK) status" in {

        lazy val result = {
          authoriseIndividual()
          stubGet(s"/income-through-software/return/$taxYear/view", OK, "")
          lazy val interestCYA = InterestCYAModel(
            Some(false), None,
            Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", amount)))
          )
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
          ))
          stubPost(s"/income-tax-interest/income-tax/nino/AA123456A/sources\\?taxYear=$taxYear", NO_CONTENT, "", expectedHeaders)

          await(wsClient.url(s"$startUrl/$taxYear/interest/check-interest")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post("{}"))
        }

        result.status shouldBe OK
      }

      s"handle no nino is in the enrolments" in {
        lazy val result = {
          authoriseIndividual(false)
          stubGet(s"/income-through-software/return/$taxYear/view", OK, "")
          lazy val interestCYA = InterestCYAModel(
            Some(false), None,
            Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", amount)))
          )
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
          ))

          await(wsClient.url(s"$startUrl/$taxYear/interest/check-interest")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post("{}"))
        }

        result.status shouldBe NOT_FOUND
      }

      "the authorization fails" in {
        lazy val result = {
          authoriseIndividualUnauthorized()
          stubGet(s"/income-through-software/return/$taxYear/view", OK, "")
          lazy val interestCYA = InterestCYAModel(
            Some(false), None,
            Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", amount)))
          )
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
          ))
          await(wsClient.url(s"$startUrl/$taxYear/interest/check-interest")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post("{}"))
        }

        result.status shouldBe UNAUTHORIZED

      }

    }
  }
}
