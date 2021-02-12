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
import controllers.Assets.{NOT_FOUND, UNAUTHORIZED}
import helpers.PlaySessionCookieBaker
import models.interest.{InterestAccountModel, InterestCYAModel}
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import utils.IntegrationTest

class ChangeAccountAmountControllerISpec extends IntegrationTest {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  ".show taxed" should {

    "return an action" when {

      "there is CYA data in session" which {

        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25)))
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))
        lazy val url: String = s"$startUrl/2020/interest/change-taxed-interest-account?accountId=TaxedId"

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(url)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .get())
        }
        s"has an OK($OK) status" in {
          result.status shouldBe OK
        }
      }

      "there is no CYA data in session" which {

        lazy val url: String = s"$startUrl/2020/interest/change-taxed-interest-account?accountId=TaxedId"

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(url)
            .withHttpHeaders("Csrf-Token" -> "nocheck")
            .get())
        }
        s"has an NOT_FOUND($NOT_FOUND) status" in {
          result.status shouldBe NOT_FOUND
        }
      }

      "the authorization fails" which {

        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25)))
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))
        lazy val url: String = s"$startUrl/2020/interest/change-taxed-interest-account?accountId=TaxedId"

        lazy val result = {
          authoriseIndividualUnauthorized()
          await(wsClient.url(url)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .get())
        }
        s"has an UNAUTHORIZED($UNAUTHORIZED) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }
    }

  }

  ".show untaxed" should {

    "return an action" when {

      "there is CYA data in session" which {

        lazy val interestCYA = InterestCYAModel(
          Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", 25))),
          Some(false), None
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))
        lazy val url: String = s"$startUrl/2020/interest/change-untaxed-interest-account?accountId=UntaxedId"

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(url)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .get())
        }
        s"has an OK($OK) status" in {
          result.status shouldBe OK
        }
      }

      "there is no CYA data in session" which {

        lazy val url: String = s"$startUrl/2020/interest/change-untaxed-interest-account?accountId=UntaxedId"

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(url)
            .withHttpHeaders("Csrf-Token" -> "nocheck")
            .get())
        }
        s"has an NOT_FOUND($NOT_FOUND) status" in {
          result.status shouldBe NOT_FOUND
        }
      }

      "the authorization fails" which {

        lazy val interestCYA = InterestCYAModel(
          Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", 25))),
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25)))
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))
        lazy val url: String = s"$startUrl/2020/interest/change-untaxed-interest-account?accountId=UntaxedId"

        lazy val result = {
          authoriseIndividualUnauthorized()
          await(wsClient.url(url)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .get())
        }
        s"has an UNAUTHORIZED($UNAUTHORIZED) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }
    }

  }

  ".submit taxed" should {

    "return an action" when {

      "there is CYA data in session" which {

        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25)))
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))
        lazy val url: String = s"$startUrl/2020/interest/change-taxed-interest-account?accountId=TaxedId"

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(url)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post("{}"))
        }
        s"has an OK($OK) status" in {
          result.status shouldBe OK
        }
      }

      "there is no CYA data in session" which {

        lazy val url: String = s"$startUrl/2020/interest/change-taxed-interest-account?accountId=TaxedId"

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(url)
            .withHttpHeaders("Csrf-Token" -> "nocheck")
            .post("{}"))
        }
        s"has an NOT_FOUND($NOT_FOUND) status" in {
          result.status shouldBe NOT_FOUND
        }
      }

      "the authorization fails" which {

        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25)))
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))
        lazy val url: String = s"$startUrl/2020/interest/change-taxed-interest-account?accountId=TaxedId"

        lazy val result = {
          authoriseIndividualUnauthorized()
          await(wsClient.url(url)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post("{}"))
        }
        s"has an UNAUTHORIZED($UNAUTHORIZED) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }
    }

  }

  ".submit untaxed" should {

    "return an action" when {

      "there is CYA data in session" which {

        lazy val interestCYA = InterestCYAModel(
          Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", 25))),
          Some(false), None
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))
        lazy val url: String = s"$startUrl/2020/interest/change-untaxed-interest-account?accountId=UntaxedId"

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(url)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post("{}"))
        }
        s"has an OK($OK) status" in {
          result.status shouldBe OK
        }
      }

      "there is no CYA data in session" which {

        lazy val url: String = s"$startUrl/2020/interest/change-untaxed-interest-account?accountId=UntaxedId"

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(url)
            .withHttpHeaders("Csrf-Token" -> "nocheck")
            .post("{}"))
        }
        s"has an NOT_FOUND($NOT_FOUND) status" in {
          result.status shouldBe NOT_FOUND
        }
      }

      "the authorization fails" which {

        lazy val interestCYA = InterestCYAModel(
          Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", 25))),
          Some(false), None
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))
        lazy val url: String = s"$startUrl/2020/interest/change-untaxed-interest-account?accountId=UntaxedId"

        lazy val result = {
          authoriseIndividualUnauthorized()
          await(wsClient.url(url)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post("{}"))
        }
        s"has an UNAUTHORIZED($UNAUTHORIZED) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }
    }

  }

}
