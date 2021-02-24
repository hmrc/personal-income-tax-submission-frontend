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
import models.{DividendsCheckYourAnswersModel, DividendsPriorSubmission}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Result
import utils.UnitTestWithApp
import views.html.dividends.ReceiveOtherUkDividendsView

import scala.concurrent.Future

class ReceiveOtherUkDividendsControllerSpec extends UnitTestWithApp {

  lazy val controller = new ReceiveOtherUkDividendsController(
    mockMessagesControllerComponents,
    authorisedAction,
    app.injector.instanceOf[ReceiveOtherUkDividendsView],
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
        val result: Future[Result] = controller.show(taxYear)(fakeRequest
          .withSession(SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(None, None, Some(true), Some(67)).asJsonString))

        status(result) shouldBe OK
      }

    }

    "redirect the user to CYA" when {

      "UK Dividends in the prior submission contains a value" which {
        lazy val result: Future[Result] = controller.show(taxYear)(fakeRequest.withSession(
          SessionValues.DIVIDENDS_PRIOR_SUB -> DividendsPriorSubmission(None, Some(100.00)).asJsonString
        ))

        s"has the SEE_OTHER($SEE_OTHER) status" in new TestWithAuth {
          status(result) shouldBe SEE_OTHER
        }

        "the redirect URL is the CYA page" in {
          redirectUrl(result) shouldBe controllers.dividends.routes.DividendsCYAController.show(taxYear).url
        }
      }

    }

  }

  ".submit" should {

    "redirect to the other dividends amounts page" when {

      "the submitted answer is yes" when {

        "the session data is empty" in new TestWithAuth {

          val result: Future[Result] = controller.submit(taxYear)(fakeRequest.withFormUrlEncodedBody(
            YesNoForm.yesNo -> YesNoForm.yes
          ))

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe controllers.dividends.routes.OtherUkDividendsAmountController.show(taxYear).url

        }

        "the session data exist" in new TestWithAuth {

          val result: Future[Result] = controller.submit(taxYear)(fakeRequest
            .withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes)
            .withSession(SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel().asJsonString)
          )

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe controllers.dividends.routes.OtherUkDividendsAmountController.show(taxYear).url

        }

      }

    }

    "redirect to the cya page" when {

      "the submitted answer is yes" when {

        "the session data is empty" in new TestWithAuth {

          val result: Future[Result] = controller.submit(taxYear)(fakeRequest.withFormUrlEncodedBody(
            YesNoForm.yesNo -> YesNoForm.no
          ))

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe controllers.dividends.routes.DividendsCYAController.show(taxYear).url

        }

        "the session data exist" in new TestWithAuth {

          val result: Future[Result] = controller.submit(taxYear)(fakeRequest
            .withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.no)
            .withSession(SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel().asJsonString)
          )

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe controllers.dividends.routes.DividendsCYAController.show(taxYear).url

        }

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
          .withSession(SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true)).asJsonString)
        )

        Json.parse(await(result).session.get(SessionValues.DIVIDENDS_CYA).get).as[DividendsCheckYourAnswersModel] shouldBe expectedModel
      }

      "cya data does not exist" in new TestWithAuth {
        val expectedModel = DividendsCheckYourAnswersModel(
          otherUkDividends = Some(true)
        )

        val result: Future[Result] = controller.submit(taxYear)(fakeRequest
          .withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes)
        )

        Json.parse(await(result).session.get(SessionValues.DIVIDENDS_CYA).get).as[DividendsCheckYourAnswersModel] shouldBe expectedModel
      }

    }

    "throw a bad request" when {

      "the form is not filled" in new TestWithAuth{
        val result: Future[Result] = controller.submit(taxYear)(fakeRequest)

        status(result) shouldBe BAD_REQUEST
      }
    }

  }

}
