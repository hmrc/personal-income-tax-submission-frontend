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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{OK, UNAUTHORIZED}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import utils.{IntegrationTest, ViewHelpers}

import java.util.UUID

class TaxedInterestAmountControllerISpec extends IntegrationTest with ViewHelpers {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  val taxYear: Int = 2022

  val amountSelector = "#taxedAmount"
  val accountNameSelector = "#taxedAccountName"

  val amount: BigDecimal = 25
  val accountName: String = "HSBC"
  val taxedAmountPageTitle = "UK taxed interest account details"
  val changeAmountPageTitle = "How much taxed UK interest did you get?"

  lazy val id: String = UUID.randomUUID().toString

  def url(newId: String): String = s"$startUrl/$taxYear/interest/taxed-uk-interest-details/$newId"


  "calling /GET" should {

    "render the taxed interest amount page" when {

      "the id is unique and is also a UUID" which {

        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(
            InterestAccountModel(Some("differentId"), accountName, amount),
            InterestAccountModel(None, accountName, amount, Some(id))
          ))
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(url(id)).withHttpHeaders(HeaderNames.COOKIE -> sessionCookie).get())
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(taxedAmountPageTitle)
        inputFieldValueCheck(accountName, accountNameSelector)
        inputFieldValueCheck(amount.toString(), amountSelector)

      }

      "the id is not a UUID" which {

        lazy val interestCYA = InterestCYAModel(Some(false), None, Some(true), None)

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(url(id)).withHttpHeaders(HeaderNames.COOKIE -> sessionCookie).get())
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(taxedAmountPageTitle)

        "id in url is UUID" in {
          UUID.fromString(result.uri.toString.split("/").last)
        }

      }

    }

    "redirect to the change amount page" when {

      "the id matches a prior submission" which {

        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(
            InterestAccountModel(Some(id), accountName, amount),
          ))
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
