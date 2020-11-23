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
import forms.YesNoForm
import models.DividendsCheckYourAnswersModel
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Result
import utils.ViewTest
import views.html.dividends.ReceiveOtherUkDividendsView

import scala.concurrent.Future

class ReceiveOtherUkDividendsControllerSpec extends ViewTest {

  lazy val controller = new ReceiveOtherUkDividendsController(
    mockMessagesControllerComponents,
    authorisedAction,
    app.injector.instanceOf[ReceiveOtherUkDividendsView],
    mockAppConfig
  )

  ".show" should {

    "return a result" which {

      s"has an OK($OK) status" in new TestWithAuth {
        val result: Future[Result] = controller.show(2020)(fakeRequest)

        status(result) shouldBe OK
      }

    }

  }

  ".submit" should {

    "redirect to the other dividends amounts page" when {

      "the submitted answer is yes" when {

        "the session data is empty" in new TestWithAuth {

          val result: Future[Result] = controller.submit(2020)(fakeRequest.withFormUrlEncodedBody(
            YesNoForm.yesNo -> YesNoForm.yes
          ))

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe controllers.dividends.routes.OtherUkDividendsAmountController.show(2020).url

        }

        "the session data exist" in new TestWithAuth {

          val result: Future[Result] = controller.submit(2020)(fakeRequest
            .withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes)
            .withSession(SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel().asJsonString)
          )

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe controllers.dividends.routes.OtherUkDividendsAmountController.show(2020).url

        }

      }

    }

    "redirect to the cya page" when {

      "the submitted answer is yes" when {

        "the session data is empty" in new TestWithAuth {

          val result: Future[Result] = controller.submit(2020)(fakeRequest.withFormUrlEncodedBody(
            YesNoForm.yesNo -> YesNoForm.no
          ))

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe controllers.dividends.routes.DividendsCYAController.show(2020).url

        }

        "the session data exist" in new TestWithAuth {

          val result: Future[Result] = controller.submit(2020)(fakeRequest
            .withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.no)
            .withSession(SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel().asJsonString)
          )

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe controllers.dividends.routes.DividendsCYAController.show(2020).url

        }

      }

    }

    "add the cya data to session" when {

      "cya data already exist" in new TestWithAuth {
        val expectedModel = DividendsCheckYourAnswersModel(
          ukDividends = Some(true),
          otherUkDividends = Some(true)
        )

        val result: Future[Result] = controller.submit(2020)(fakeRequest
          .withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes)
          .withSession(SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true)).asJsonString)
        )

        Json.parse(await(result).session.get(SessionValues.DIVIDENDS_CYA).get).as[DividendsCheckYourAnswersModel] shouldBe expectedModel
      }

      "cya data does not exist" in new TestWithAuth {
        val expectedModel = DividendsCheckYourAnswersModel(
          otherUkDividends = Some(true)
        )

        val result: Future[Result] = controller.submit(2020)(fakeRequest
          .withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes)
        )

        Json.parse(await(result).session.get(SessionValues.DIVIDENDS_CYA).get).as[DividendsCheckYourAnswersModel] shouldBe expectedModel
      }

    }

  }

}