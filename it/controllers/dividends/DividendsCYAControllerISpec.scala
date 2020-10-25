/*
 * Copyright 2020 HM Revenue & Customs
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
import helpers.PlaySessionCookieBaker
import models.DividendsCheckYourAnswersModel
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import utils.IntegrationTest

class DividendsCYAControllerISpec extends IntegrationTest {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  ".show" should {

    s"returns an action" when {

      "there is CYA data in session" which {

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.DIVIDENDS_CYA -> Json.prettyPrint(Json.toJson(DividendsCheckYourAnswersModel()))
        ))

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/dividends/check-your-answers")
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
          stubGet("/income-through-software/return/view", OK, "<title>Overview Page</title>")

          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/dividends/check-your-answers")
            .get())
        }

        s"has an OK($OK) status" in {
          result.status shouldBe OK
        }

      }

    }

  }

  ".post" should {

    "return an action" which {

      lazy val result = {
        authoriseIndividual()
        stubGet("/income-through-software/return/view", OK, "")

        await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/dividends/check-your-answers")
          .post("{}"))
      }

      s"has an OK($OK) status" in {
        result.status shouldBe OK
      }

    }

  }

}
