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

package controllers.charity

import common.SessionValues
import forms.charity.LastTaxYearAmountForm
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.mvc.Result
import utils.UnitTestWithApp
import views.html.charity.LastTaxYearAmountView

import scala.concurrent.Future

class LastTaxYearAmountControllerSpec extends UnitTestWithApp {

  val taxYear: Int = 2022

  lazy val controller = new LastTaxYearAmountController(
    app.injector.instanceOf[LastTaxYearAmountView])(
    mockMessagesControllerComponents,
    authorisedAction,
    mockAppConfig)

  "Calling the .show method" should {

    "return a 200 status" in new TestWithAuth {
      val result: Future[Result] = controller.show(taxYear)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
      status(result) shouldBe OK
    }
  }

  "Calling the .submit method" when {

    "an amount has been input" should {
      lazy val result = controller.submit(taxYear)(fakeRequest.withFormUrlEncodedBody(
        LastTaxYearAmountForm.lastTaxYearAmount -> "8008135"
      ))

      s"has a status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
        status(result) shouldBe SEE_OTHER
      }
    }
  }

}
