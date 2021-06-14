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
import forms.YesNoForm
import helpers.PlaySessionCookieBaker
import models.interest.{InterestAccountModel, InterestCYAModel}
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import utils.IntegrationTest

class TaxedInterestControllerISpec extends IntegrationTest {

  "as an individual" when {

    ".show" should {

      "returns an action when data is not in session" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(s"$appUrl/2020/interest/taxed-uk-interest").get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

      }
      "returns an action when data is in session" which {

        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25)))
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(s"$appUrl/2020/interest/taxed-uk-interest")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

      }

      "returns an action when auth call fails" which {
        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25)))
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))
        lazy val result: WSResponse = {
          authoriseIndividualUnauthorized()
          await(wsClient.url(s"$appUrl/2020/interest/taxed-uk-interest")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }
        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

    ".submit" should {

      s"return an OK($OK) status" when {

        "returns an action when data is not in session" which {
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(s"$appUrl/2020/interest/taxed-uk-interest").post(Map(YesNoForm.yesNo -> YesNoForm.yes)))
          }

          s"has an NOT_FOUND($NOT_FOUND) status" in {
            result.status shouldBe NOT_FOUND
          }

        }

        "there is CYA data in session" in {
          lazy val interestCYA = InterestCYAModel(
            Some(false), None,
            Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25)))
          )
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
          ))
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(
              wsClient.url(s"$appUrl/2020/interest/taxed-uk-interest")
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
            )
          }

          result.status shouldBe OK
        }

      }

      s"return a BAD_REQUEST($BAD_REQUEST) status" in {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(s"$appUrl/2020/interest/taxed-uk-interest").post(Map[String, String]()))
        }

        result.status shouldBe BAD_REQUEST
      }

      "returns an action when auth call fails" which {
        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25)))
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))
        lazy val result: WSResponse = {
          authoriseIndividualUnauthorized()
          await(
            wsClient.url(s"$appUrl/2020/interest/taxed-uk-interest")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
          )        }
        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

  }

  "as an agent" when {

    ".show" should {

      "returns an action when data is in session" which {

        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25)))
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA)),
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A")
        )

        lazy val result: WSResponse = {
          authoriseAgent()
          await(wsClient.url(s"$appUrl/2020/interest/taxed-uk-interest")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

      }

      "returns an action when auth call fails" which {
        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25)))
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA)),
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A")
        )
        lazy val result: WSResponse = {
          authoriseAgentUnauthorized()
          await(wsClient.url(s"$appUrl/2020/interest/taxed-uk-interest")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }
        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

    ".submit" should {

      s"return an OK($OK) status" when {

        "returns an action when data is not in session" which {
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(s"$appUrl/2020/interest/taxed-uk-interest").post(Map(YesNoForm.yesNo -> YesNoForm.yes)))
          }

          s"has an NOT_FOUND($NOT_FOUND) status" in {
            result.status shouldBe NOT_FOUND
          }

        }

        "there is CYA data in session" in {
          lazy val interestCYA = InterestCYAModel(
            Some(false), None,
            Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25)))
          )
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA)),
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A")
          )
          lazy val result: WSResponse = {
            authoriseAgent()
            await(
              wsClient.url(s"$appUrl/2020/interest/taxed-uk-interest")
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
            )
          }

          result.status shouldBe OK
        }

      }

      s"return a BAD_REQUEST($BAD_REQUEST) status" in {
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A")
        )
        lazy val result: WSResponse = {
          authoriseAgent()
          await(
            wsClient.url(s"$appUrl/2020/interest/taxed-uk-interest")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map[String, String]())
          )
        }

        result.status shouldBe BAD_REQUEST
      }

      "returns an action when auth call fails" which {
        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25)))
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA)),
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A")
        )
        lazy val result: WSResponse = {
          authoriseAgentUnauthorized()
          await(
            wsClient.url(s"$appUrl/2020/interest/taxed-uk-interest")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
          )        }
        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

  }
}
