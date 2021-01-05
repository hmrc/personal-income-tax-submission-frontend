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
import forms.YesNoForm
import models.DividendsCheckYourAnswersModel
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Result
import utils.ViewTest
import views.html.dividends.ReceiveUkDividendsView

import scala.concurrent.Future

class ReceiveUkDividendsControllerSpec extends ViewTest {

  lazy val controller = new ReceiveUkDividendsController(
    mockMessagesControllerComponents,
    authorisedAction,
    app.injector.instanceOf[ReceiveUkDividendsView],
    mockAppConfig
  )

  val taxYear = 2020

  ".show" should {

    "return a result" which {

      s"has an OK($OK) status" in new TestWithAuth {
        val result: Future[Result] = controller.show(taxYear)(fakeRequest)

        status(result) shouldBe OK
      }

    }

    "return a result when there is a dividends CYA model in session" which {

      s"has an OK($OK) status" in new TestWithAuth {
        val result: Future[Result] = controller.show(2020)(fakeRequest
          .withSession(SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(Some(true), Some(67), None, None).asJsonString))

        status(result) shouldBe OK
      }

    }

  }

  ".submit" should {

    "redirect to the uk dividends amounts page" when {

      "the submitted answer is yes" when {

        "the session data is empty" in new TestWithAuth {

          val result: Future[Result] = controller.submit(taxYear)(fakeRequest.withFormUrlEncodedBody(
            YesNoForm.yesNo -> YesNoForm.yes
          ))

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe controllers.dividends.routes.UkDividendsAmountController.show(taxYear).url

        }

        "the session data exist" in new TestWithAuth {

          val result: Future[Result] = controller.submit(taxYear)(fakeRequest
            .withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes)
            .withSession(SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel().asJsonString)
          )

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe controllers.dividends.routes.UkDividendsAmountController.show(taxYear).url

        }

      }

    }

    "redirect to the receive other dividends page" when {

      "the submitted answer is yes" when {

        "the session data is empty" in new TestWithAuth {

          val result: Future[Result] = controller.submit(taxYear)(fakeRequest.withFormUrlEncodedBody(
            YesNoForm.yesNo -> YesNoForm.no
          ))

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe controllers.dividends.routes.ReceiveOtherUkDividendsController.show(taxYear).url

        }

        "the session data exist" in new TestWithAuth {

          val result: Future[Result] = controller.submit(taxYear)(fakeRequest
            .withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.no)
            .withSession(SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel().asJsonString)
          )

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe controllers.dividends.routes.ReceiveOtherUkDividendsController.show(taxYear).url

        }

      }

    }

    "redirect to the CYA page" when {

      "the CYA model indicates it is finished and the user has pressed no" in new TestWithAuth {
        val amount = 100
        val cyaModel = DividendsCheckYourAnswersModel(Some(true), Some(amount), Some(true), Some(amount))

        lazy val result: Future[Result] = controller.submit(taxYear)(fakeRequest
            .withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.no)
            .withSession(SessionValues.DIVIDENDS_CYA -> cyaModel.asJsonString)
        )

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe controllers.dividends.routes.DividendsCYAController.show(taxYear).url
      }

    }

    "add the cya data to session" when {

      "cya data already exist" in new TestWithAuth {
        val expectedModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(
          ukDividends = Some(true),
          otherUkDividends = Some(true)
        )

        val result: Future[Result] = controller.submit(taxYear)(fakeRequest
          .withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes)
          .withSession(SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(otherUkDividends = Some(true)).asJsonString)
        )

        Json.parse(await(result).session.get(SessionValues.DIVIDENDS_CYA).get).as[DividendsCheckYourAnswersModel] shouldBe expectedModel
      }

      "cya data does not exist" in new TestWithAuth {
        val expectedModel = DividendsCheckYourAnswersModel(
        )

        val result: Future[Result] = controller.submit(taxYear)(fakeRequest
          .withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes)
        )

        await(result).session.get(SessionValues.DIVIDENDS_CYA).getOrElse(DividendsCheckYourAnswersModel()) shouldBe expectedModel
      }

    }

    "throw a bad request" when {

      "the form is not filled" in new TestWithAuth{
        val result: Future[Result] = controller.submit(2020)(fakeRequest)

        status(result) shouldBe BAD_REQUEST
      }
    }

  }

}
