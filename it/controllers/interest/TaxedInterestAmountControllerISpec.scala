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

import common.SessionValues
import controllers.Assets.BAD_REQUEST
import forms.TaxedInterestAmountForm
import helpers.PlaySessionCookieBaker
import models.interest.{InterestAccountModel, InterestCYAModel}
import play.api.http.HeaderNames
import play.api.http.Status.{OK, UNAUTHORIZED}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import utils.IntegrationTest

class TaxedInterestAmountControllerISpec extends IntegrationTest{

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  val taxYear: Int = 2022
  val amount: BigDecimal = 25

  ".show" should {

    "return an action" when {

      s"has an OK($OK) status" which {

        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", amount)))
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(s"$startUrl/$taxYear/interest/taxed-uk-interest-details/TaxedId")
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


          await(wsClient.url(s"$startUrl/$taxYear/interest/taxed-uk-interest-details/TaxedId")
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


          await(wsClient.url(s"$startUrl/$taxYear/interest/taxed-uk-interest-details/TaxedId")
            .get())
        }

        s"has an OK($UNAUTHORIZED) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }
    }
  }

  ".submit" should {

    "return an action" when {

      s"there is form data" which {

        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", amount)))
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(s"$startUrl/$taxYear/interest/taxed-uk-interest-details/TaxedId")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post(Map(TaxedInterestAmountForm.taxedAmount -> "67.66",
              TaxedInterestAmountForm.taxedAccountName -> "Santander")))
        }

        s"has an OK($OK) status" in {
          result.status shouldBe OK
        }

      }

      s"there is no form data" which {

        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", amount)))
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(s"$startUrl/$taxYear/interest/taxed-uk-interest-details/TaxedId")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post(Map[String, String]()))
        }

        s"has an BAD_REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }

      }

      s"return an UNAUTHORIZED($UNAUTHORIZED) status" which {

        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", amount)))
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))

        lazy val result = {
          authoriseIndividualUnauthorized()
          await(wsClient.url(s"$startUrl/$taxYear/interest/taxed-uk-interest-details/TaxedId")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post(Map[String, String]()))
        }

        s"has an BAD_REQUEST($UNAUTHORIZED) status" in {
          result.status shouldBe UNAUTHORIZED
        }

      }

    }
  }

}
