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

package controllers

import forms.YesNoForm
import play.api.http.Status._
import play.api.libs.ws.{WSClient, WSResponse}
import utils.IntegrationTest

class ReceiveDividendsControllerISpec extends IntegrationTest {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  lazy val controller: ReceiveDividendsController = app.injector.instanceOf[ReceiveDividendsController]

  ".show" should {

    "returns an action" which {
      lazy val result: WSResponse = {
        authoriseIndividual()
        await(wsClient.url(s"http://localhost:$port/report-quarterly/income-and-expenses/submissions/dividends/other-dividends").get())
      }

      "has an OK(200) status" in {
        result.status shouldBe OK
      }

    }

  }

  ".submit" should {

    s"return an OK($OK) status" in {
      lazy val result: WSResponse = {
        authoriseIndividual()
        await(
          wsClient.url(s"http://localhost:$port/report-quarterly/income-and-expenses/submissions/dividends/other-dividends")
            .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
        )
      }

      result.status shouldBe OK
    }

    s"return a BAD_REQUEST($BAD_REQUEST) status" in {
      lazy val result: WSResponse = {
        authoriseIndividual()
        await(wsClient.url(s"http://localhost:$port/report-quarterly/income-and-expenses/submissions/dividends/other-dividends").post(Map[String, String]()))
      }

      result.status shouldBe BAD_REQUEST
    }

  }

}
