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

  val taxYear = 2020
  val amount10 = 10
  val amount20 = 20

  ".show" should {

    s"return an OK($OK)" when {

      val cyaSessionData = DividendsCheckYourAnswersModel(
        ukDividends = Some(true),
        Some(amount10),
        otherUkDividends = Some(true),
        Some(amount10)
      )

      val priorData = DividendsPriorSubmission(
        Some(amount10),
        Some(amount20)
      )

      "there is CYA session data and prior submission data" in new TestWithAuth {
        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
          SessionValues.DIVIDENDS_CYA -> Json.toJson(cyaSessionData).toString,
          SessionValues.DIVIDENDS_PRIOR_SUB -> Json.toJson(priorData).toString
        )

        val result: Future[Result] = controller.show(taxYear)(request)

        status(result) shouldBe OK
      }

      "there is CYA session data and no prior submission data" in new TestWithAuth {
        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
          SessionValues.DIVIDENDS_CYA -> Json.toJson(cyaSessionData).toString
        )

        val result: Future[Result] = controller.show(taxYear)(request)

        status(result) shouldBe OK
      }

      "there is prior submission data and no CYA session data" in new TestWithAuth {
        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
          SessionValues.DIVIDENDS_PRIOR_SUB -> Json.toJson(priorData).toString
        )

        val result: Future[Result] = controller.show(taxYear)(request)

        status(result) shouldBe OK
      }

    }

    "redirect to the overview page" when {

      "there is no session data" in new TestWithAuth {
        val result: Future[Result] = controller.show(taxYear)(FakeRequest())

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
      }

    }

  }

  ".priorityOrderOrNone" should {

    val amount1: BigDecimal = 120
    val amount2: BigDecimal = 140

    val priorityValue: Option[BigDecimal] = Some(amount1)
    val otherValue: Option[BigDecimal] = Some(amount2)

    "return the priority value" when {

      "the priority value is provided on it's own" in {
        controller.priorityOrderOrNone(priorityValue, None, yesNoResult = true) shouldBe Some(amount1)
      }

      "the priority value is provided along side the other value" in {
        controller.priorityOrderOrNone(priorityValue, otherValue, yesNoResult = true) shouldBe Some(amount1)
      }

    }

    "return the other value" when {

      "no priority value is provided" in {
        controller.priorityOrderOrNone(None, otherValue, yesNoResult = true) shouldBe Some(amount2)
      }

    }

    "return None" when {

      "no priority or other value is provided" in {
        controller.priorityOrderOrNone(None, None, yesNoResult = true) shouldBe None
      }

      "yesNoResult is false regardless of provided values" in {
        controller.priorityOrderOrNone(priorityValue, otherValue, yesNoResult = false) shouldBe None
      }

    }

  }

}
