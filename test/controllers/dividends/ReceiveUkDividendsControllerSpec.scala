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
import views.html.dividends.ReceiveUkDividendsView

import scala.concurrent.Future

class ReceiveUkDividendsControllerSpec extends UnitTestWithApp {

  lazy val controller = new ReceiveUkDividendsController(
    mockMessagesControllerComponents,
    authorisedAction,
    app.injector.instanceOf[ReceiveUkDividendsView],
    mockAppConfig
  )

  val taxYear = 2022

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
          .withSession(SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(Some(true), Some(67.00), None, None).asJsonString))

        status(result) shouldBe OK
      }

    }

    "redirect the user to CYA" when {

      "UK Dividends in the prior submission contains a value" which {
        lazy val result: Future[Result] = controller.show(taxYear)(fakeRequest.withSession(
          SessionValues.DIVIDENDS_PRIOR_SUB -> DividendsPriorSubmission(Some(100.00), None).asJsonString
        ))

        s"has the SEE_OTHER($SEE_OTHER) status" in new TestWithAuth {
          status(result) shouldBe SEE_OTHER
        }

        "the redirect URL is the CYA page" in {
          redirectUrl(result) shouldBe controllers.dividends.routes.DividendsCYAController.show(taxYear).url
        }
      }

    }

    "Redirect to the tax year error " when {

      "an invalid tax year has been added to the url" in new TestWithAuth() {

        val invalidTaxYear = 2023
        lazy val result: Future[Result] = controller.show(invalidTaxYear)(fakeRequest)

        redirectUrl(result) shouldBe controllers.routes.TaxYearErrorController.show().url

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
        val cyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(Some(true), Some(amount), Some(true), Some(amount))

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
        val expectedModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(ukDividends = Some(true))

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
