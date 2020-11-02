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
import models.{DividendsCheckYourAnswersModel, DividendsPriorSubmission}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import utils.ViewTest
import views.html.dividends.DividendsCYAView

import scala.concurrent.Future

class DividendsCYAControllerSpec extends ViewTest {

  val controller = new DividendsCYAController(
    mockMessagesControllerComponents,
    app.injector.instanceOf[DividendsCYAView],
    authorisedAction
  )(
    mockAppConfig
  )

  ".show" should {

    s"return an OK($OK)" when {

      val cyaSessionData = DividendsCheckYourAnswersModel(
        ukDividends = true,
        Some(10),
        otherDividends = true,
        Some(10)
      )

      val priorData = DividendsPriorSubmission(
        Some(10),
        Some(20)
      )

      "there is CYA session data and prior submission data" in new TestWithAuth {
        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
          SessionValues.DIVIDENDS_CYA -> Json.toJson(cyaSessionData).toString,
          SessionValues.DIVIDENDS_PRIOR_SUB -> Json.toJson(priorData).toString
        )

        val result: Future[Result] = controller.show()(request)

        status(result) shouldBe OK
      }

      "there is CYA session data and no prior submission data" in new TestWithAuth {
        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
          SessionValues.DIVIDENDS_CYA -> Json.toJson(cyaSessionData).toString
        )

        val result: Future[Result] = controller.show()(request)

        status(result) shouldBe OK
      }

      "there is prior submission data and no CYA session data" in new TestWithAuth {
        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
          SessionValues.DIVIDENDS_PRIOR_SUB -> Json.toJson(priorData).toString
        )

        val result: Future[Result] = controller.show()(request)

        status(result) shouldBe OK
      }

    }

    "redirect to the overview page" when {

      "there is no session data" in new TestWithAuth {
        val result: Future[Result] = controller.show()(FakeRequest())

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl
      }

    }

  }

}
