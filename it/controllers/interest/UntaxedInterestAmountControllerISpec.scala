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
import forms.UntaxedInterestAmountForm
import helpers.PlaySessionCookieBaker
import models.interest.{InterestAccountModel, InterestCYAModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{OK, UNAUTHORIZED}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import utils.{IntegrationTest, ViewHelpers}

import java.util.UUID

class UntaxedInterestAmountControllerISpec extends IntegrationTest with ViewHelpers {

  val taxYear: Int = 2022

  val amountSelector = "#untaxedAmount"
  val accountNameSelector = "#untaxedAccountName"

  val amount: BigDecimal = 25
  val accountName: String = "HSBC"
  val untaxedAmountPageTitle = "UK untaxed interest account details"
  val changeAmountPageTitle = "How much untaxed UK interest did you get?"

  lazy val id: String = UUID.randomUUID().toString

  def url(newId: String): String = s"$startUrl/$taxYear/interest/untaxed-uk-interest-details/$newId"

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  "calling /GET" should {

    "render the untaxed interest amount page" when {

      "the id is unique and is also a UUID" which {

        lazy val interestCYA = InterestCYAModel(
          Some(true), Some(Seq(
            InterestAccountModel(Some("differentId"), accountName, amount),
            InterestAccountModel(None, accountName, amount, Some(id))
          )),
          Some(false), None
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(url(id)).withHttpHeaders(HeaderNames.COOKIE -> sessionCookie).get())
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(untaxedAmountPageTitle)
        inputFieldValueCheck(accountName, accountNameSelector)
        inputFieldValueCheck(amount.toString(), amountSelector)

      }

      "the id is not a UUID" which {

        lazy val interestCYA = InterestCYAModel(Some(true), None, Some(false), None)

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(url(id)).withHttpHeaders(HeaderNames.COOKIE -> sessionCookie).get())
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(untaxedAmountPageTitle)

        "id in url is UUID" in {
          UUID.fromString(result.uri.toString.split("/").last)
        }

      }

    }

    "redirect to the change amount page" when {

      "the id matches a prior submission" which {

        lazy val interestCYA = InterestCYAModel(
          Some(true), Some(Seq(InterestAccountModel(Some(id), accountName, amount))),
          Some(false), None
        )

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA)),
          SessionValues.INTEREST_PRIOR_SUB -> Json.arr(
            Json.obj(
              "accountName" -> accountName,
              "incomeSourceId" -> id,
              "untaxedUkInterest" -> amount
            )
          ).toString()
        ))

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(url(id)).withHttpHeaders(HeaderNames.COOKIE -> sessionCookie).get())
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(changeAmountPageTitle)

      }
    }

    "redirect to the overview page" when {

      "there is no cya data in session" in {
        {
          authoriseIndividual()
          await(wsClient.url(url(id)).get())
        }

        stubGet("/income-through-software/return/2022/view",303,"")
        verifyGet("/income-through-software/return/2022/view")
        wireMockServer.resetAll()

      }
    }

  }


  ".submit" should {

    "return an action" when {

      "there is no CYA data in session" which {
        lazy val result = {
          authoriseIndividual()
          stubGet(s"/income-through-software/return/$taxYear/view", OK, "<title>Overview Page</title>")


          await(wsClient.url(s"$startUrl/$taxYear/interest/untaxed-uk-interest-details/UntaxedId")
            .post(Map(UntaxedInterestAmountForm.untaxedAmount -> "67.66",
              UntaxedInterestAmountForm.untaxedAccountName -> "Santander")))
        }
        s"has an OK($OK) status" in {
          result.status shouldBe OK
        }
      }

      s"there is form data" which {

        lazy val interestCYA = InterestCYAModel(
          Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
          Some(false), None
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(s"$startUrl/$taxYear/interest/untaxed-uk-interest-details/UntaxedId")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post(Map(UntaxedInterestAmountForm.untaxedAmount -> "67.66",
              UntaxedInterestAmountForm.untaxedAccountName -> "Santander")))
        }

        s"has an OK($OK) status" in {
          result.status shouldBe OK
        }

      }

      s"there is no form data" which {

        lazy val interestCYA = InterestCYAModel(
          Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
          Some(false), None
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(s"$startUrl/$taxYear/interest/untaxed-uk-interest-details/UntaxedId")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post(Map[String, String]()))
        }

        s"has an BAD_REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }

      }

      s"return an UNAUTHORIZED($UNAUTHORIZED) status" which {

        lazy val interestCYA = InterestCYAModel(
          Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
          Some(false), None
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))

        lazy val result = {
          authoriseIndividualUnauthorized()
          await(wsClient.url(s"$startUrl/$taxYear/interest/untaxed-uk-interest-details/UntaxedId")
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
