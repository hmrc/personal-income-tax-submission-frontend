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
import forms.OtherDividendsAmountForm
import helpers.PlaySessionCookieBaker
import models.DividendsCheckYourAnswersModel
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.{WSClient, WSResponse}
import utils.IntegrationTest

class OtherUkDividendsAmountControllerISpec extends IntegrationTest {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  lazy val controller: OtherUkDividendsAmountController = app.injector.instanceOf[OtherUkDividendsAmountController]

  val taxYear: Int = 2022
  val amount: BigDecimal = 500
  val otherUkDividendsAmountUrl = s"$startUrl/$taxYear/dividends/how-much-dividends-from-uk-trusts-and-open-ended-investment-companies"

  "as an individual" when {

    ".show" should {

      "returns an action when there is no data in session" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(otherUkDividendsAmountUrl).get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }
      }

      "returns an action when there is data in session" which {

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount)).asJsonString
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

      }

      "return unauthorized when the authorization fails" which {
        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount)).asJsonString
        ))
        lazy val result: WSResponse = {
          authoriseIndividualUnauthorized()
          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get())
        }

        s"has an UNAUTHORIZED($UNAUTHORIZED) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

    ".submit" should {

      s"return an OK($OK) status" in {
        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(otherUkDividends = Some(true)).asJsonString
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(
            wsClient.url(otherUkDividendsAmountUrl)
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map(OtherDividendsAmountForm.otherDividendsAmount -> "123"))
          )
        }

        result.status shouldBe OK
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status" in {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(otherUkDividendsAmountUrl).post(Map[String, String]()))
        }

        result.status shouldBe BAD_REQUEST
      }

      "return unauthorized when the authorization fails" which {
        lazy val result: WSResponse = {
          authoriseIndividualUnauthorized()
          await(wsClient.url(otherUkDividendsAmountUrl)
            .post(Map[String, String]()))
        }

        s"has an UNAUTHORIZED($UNAUTHORIZED) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

  }

  "as an agent" when {

    ".show" should {

      "returns an action" which {
        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie)
            .get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }
      }

      "return unauthorized when the authorization fails" which {
        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount)).asJsonString
        ))
        lazy val result: WSResponse = {
          authoriseAgentUnauthorized()
          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie)
            .get())
        }

        s"has an UNAUTHORIZED($UNAUTHORIZED) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }
    }

    ".submit" should {

      s"return an OK($OK) status" when {

        "there is form data" in {
          lazy val result: WSResponse = {
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A",
              SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true)).asJsonString
            ))

            authoriseAgent()
            await(
              wsClient.url(otherUkDividendsAmountUrl)
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
                .post(Map(OtherDividendsAmountForm.otherDividendsAmount -> "123"))
            )
          }

          result.status shouldBe OK
        }
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status" when {

        "there is no form data" in {
          lazy val result: WSResponse = {
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"
            ))

            authoriseAgent()
            await(wsClient.url(otherUkDividendsAmountUrl)
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map[String, String]()))
          }

          result.status shouldBe BAD_REQUEST
        }
      }

      "return unauthorized when the authorization fails" which {
        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(otherUkDividends = Some(true), otherUkDividendsAmount = Some(amount)).asJsonString,
          SessionValues.CLIENT_NINO -> "AA123456A"
        ))
        lazy val result: WSResponse = {
          authoriseAgentUnauthorized()
          await(wsClient.url(otherUkDividendsAmountUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post(Map[String, String]()))
        }

        s"has an UNAUTHORIZED($UNAUTHORIZED) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

  }

}

