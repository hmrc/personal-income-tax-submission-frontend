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
import play.api.http.Status.{BAD_REQUEST, OK, UNAUTHORIZED}
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import utils.IntegrationTest

class RemoveAccountControllerISpec extends IntegrationTest{

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  ".show untaxed" should {

    s"returns an action" when {

      "there is CYA data in session" which {

        lazy val interestCYA = InterestCYAModel(
          Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", 25))),
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25)))
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(s"$startUrl/2020/interest/remove-untaxed-interest-account?accountId=UntaxedId")
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
          stubGet("/income-through-software/return/2020/view", OK, "<title>Overview Page</title>")


          await(wsClient.url(s"$startUrl/2020/interest/remove-untaxed-interest-account?accountId=UntaxedId")
            .get())
        }

        s"has an OK($OK) status" in {
          result.status shouldBe OK
        }

      }

      "the authorization fails" which {
        lazy val result = {
          authoriseIndividualUnauthorized()
          stubGet("/income-through-software/return/2020/view", OK, "<title>Overview Page</title>")


          await(wsClient.url(s"$startUrl/2020/interest/remove-untaxed-interest-account?accountId=UntaxedId")
            .get())
        }

        s"has an OK($UNAUTHORIZED) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

  }

  ".show taxed" should {

    s"returns an action" when {

      "there is CYA data in session" which {

        lazy val interestCYA = InterestCYAModel(
          Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", 25))),
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25)))
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(s"$startUrl/2020/interest/remove-taxed-interest-account?accountId=TaxedId")
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
          stubGet("/income-through-software/return/2020/view", OK, "<title>Overview Page</title>")


          await(wsClient.url(s"$startUrl/2020/interest/remove-taxed-interest-account?accountId=TaxedId")
            .get())
        }

        s"has an OK($OK) status" in {
          result.status shouldBe OK
        }

      }

      "the authorization fails" which {
        lazy val result = {
          authoriseIndividualUnauthorized()
          stubGet("/income-through-software/return/2020/view", OK, "<title>Overview Page</title>")
          await(wsClient.url(s"$startUrl/2020/interest/remove-taxed-interest-account?accountId=TaxedId")
            .get())
        }

        s"has an OK($UNAUTHORIZED) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

  }

  ".submit untaxed" should {

    s"return an OK($OK) status" when {

      "there is no CYA data in session" in {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(
            wsClient.url(s"$startUrl/2020/interest/remove-untaxed-interest-account?accountId=UntaxedId")
              .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
          )
        }

        result.status shouldBe OK
      }

    }

    s"return a BAD_REQUEST($BAD_REQUEST) status" in {
      lazy val result: WSResponse = {
        authoriseIndividual()
        await(wsClient.url(s"$startUrl/2020/interest/remove-untaxed-interest-account")
          .post(Map[String, String]()))
      }

      result.status shouldBe BAD_REQUEST
    }

    "returns an action when auth call fails" which {
      lazy val result: WSResponse = {
        authoriseIndividualUnauthorized()
        await(wsClient.url(s"$startUrl/2020/interest/remove-untaxed-interest-account?accountId=UntaxedId")
          .post(Map(YesNoForm.yesNo -> YesNoForm.yes)))
      }
      "has an UNAUTHORIZED(401) status" in {
        result.status shouldBe UNAUTHORIZED
      }
    }

  }

  ".submit taxed" should {

    s"return an OK($OK) status" when {

      "there is no CYA data in session" in {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(
            wsClient.url(s"$startUrl/2020/interest/remove-taxed-interest-account?accountId=TaxedId")
              .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
          )
        }

        result.status shouldBe OK
      }

    }

    s"return a BAD_REQUEST($BAD_REQUEST) status" in {
      lazy val result: WSResponse = {
        authoriseIndividual()
        await(wsClient.url(s"$startUrl/2020/interest/remove-taxed-interest-account")
          .post(Map[String, String]()))
      }

      result.status shouldBe BAD_REQUEST
    }

    "returns an action when auth call fails" which {
      lazy val result: WSResponse = {
        authoriseIndividualUnauthorized()
        await(wsClient.url(s"$startUrl/2020/interest/remove-taxed-interest-account?accountId=TaxedId")
          .post(Map(YesNoForm.yesNo -> YesNoForm.yes)))
      }
      "has an UNAUTHORIZED(401) status" in {
        result.status shouldBe UNAUTHORIZED
      }
    }

  }

}
