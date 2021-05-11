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
import controllers.Assets.{UNAUTHORIZED, NOT_FOUND}
import helpers.PlaySessionCookieBaker
import models.interest.{InterestAccountModel, InterestCYAModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.OK
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import utils.{IntegrationTest, ViewHelpers}

import java.util.UUID

class ChangeAccountAmountControllerISpec extends IntegrationTest with ViewHelpers {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  val taxYear: Int = 2022

  val amountSelector = "#amount"

  val amount: BigDecimal = 25
  val differentAmount: BigDecimal = 30
  val accountName: String = "HSBC"

  val overviewPageTitle = "Your Income Tax Return"
  val untaxedAccountPageTitle = "UK untaxed interest account"
  val taxedAccountPageTitle = "UK taxed interest account"
  val untaxedChangeAmountPageTitle = "How much untaxed UK interest did you get?"
  val taxedChangeAmountPageTitle = "How much taxed UK interest did you get?"

  def url(newId: String, accountType: String): String = s"$startUrl/$taxYear/interest/change-$accountType-interest-account?accountId=$newId"

  lazy val id: String = UUID.randomUUID().toString

  "calling /GET" when {

    "untaxed" should {

      "rending the untaxed change amount page without pre-populated amount box" which {

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
          await(wsClient.url(url(id, "untaxed")).withHttpHeaders(HeaderNames.COOKIE -> sessionCookie).get())
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(untaxedChangeAmountPageTitle)
        inputFieldValueCheck("", amountSelector)
      }

      "rending the untaxed change amount page with pre-populated amount box" which {

        lazy val interestCYA = InterestCYAModel(
          Some(true), Some(Seq(InterestAccountModel(Some(id), accountName, differentAmount))),
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
          await(wsClient.url(url(id, "untaxed")).withHttpHeaders(HeaderNames.COOKIE -> sessionCookie).get())
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(untaxedChangeAmountPageTitle)
        inputFieldValueCheck(differentAmount.toString(), amountSelector)
      }

      "redirect to the overview page" when {

        "there is no prior or cya data" in {

          {
            authoriseIndividual()
            await(wsClient.url(url(id, "untaxed")).get())
          }

          stubGet("/income-through-software/return/2022/view",303,"")
          verifyGet("/income-through-software/return/2022/view")
          wireMockServer.resetAll()
        }

      }

      "redirect to untaxed account summary page" when {

        "there is cya data but no prior data" which {

          lazy val interestCYA = InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some(id), accountName, amount))),
            Some(false), None
          )

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
          ))

          lazy val result = {
            authoriseIndividual()
            await(wsClient.url(url(id, "untaxed")).withHttpHeaders(HeaderNames.COOKIE -> sessionCookie).get())
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(untaxedAccountPageTitle)

        }

      }

    }

    "taxed" should {

      "rending the taxed change amount page without pre-populated amount box" which {

        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some(id), accountName, amount)))
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
          await(wsClient.url(url(id, "taxed")).withHttpHeaders(HeaderNames.COOKIE -> sessionCookie).get())
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(taxedChangeAmountPageTitle)
        inputFieldValueCheck("", amountSelector)

      }

      "rending the taxed change amount page with pre-populated amount box" which {

        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some(id), accountName, differentAmount)))
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
          await(wsClient.url(url(id, "taxed")).withHttpHeaders(HeaderNames.COOKIE -> sessionCookie).get())
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(taxedChangeAmountPageTitle)
        inputFieldValueCheck(differentAmount.toString(), amountSelector)

      }

      "redirect to the overview page" when {

        "there is no prior or cya data" in {

          {
            authoriseIndividual()
            await(wsClient.url(url(id, "taxed")).get())
          }

          stubGet("/income-through-software/return/2022/view",303,"")
          verifyGet("/income-through-software/return/2022/view")
          wireMockServer.resetAll()
        }

      }

      "redirect to taxed account summary page" when {

        "there is cya data but no prior data" which {

          lazy val interestCYA = InterestCYAModel(
            Some(false), None,
            Some(true), Some(Seq(InterestAccountModel(Some(id), accountName, amount)))
          )

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
          ))

          lazy val result = {
            authoriseIndividual()
            await(wsClient.url(url(id, "taxed")).withHttpHeaders(HeaderNames.COOKIE -> sessionCookie).get())
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(taxedAccountPageTitle)

        }

      }

    }

  }

  ".submit taxed" should {

    "return an action" when {

      "there is CYA data in session" which {

        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", amount)))
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))
        lazy val url: String = s"$startUrl/$taxYear/interest/change-taxed-interest-account?accountId=TaxedId"

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

        lazy val url: String = s"$startUrl/$taxYear/interest/change-taxed-interest-account?accountId=TaxedId"

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
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", amount)))
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))
        lazy val url: String = s"$startUrl/$taxYear/interest/change-taxed-interest-account?accountId=TaxedId"

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
          Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
          Some(false), None
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))
        lazy val url: String = s"$startUrl/$taxYear/interest/change-untaxed-interest-account?accountId=UntaxedId"

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

        lazy val url: String = s"$startUrl/$taxYear/interest/change-untaxed-interest-account?accountId=UntaxedId"

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
          Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
          Some(false), None
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))
        lazy val url: String = s"$startUrl/$taxYear/interest/change-untaxed-interest-account?accountId=UntaxedId"

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
