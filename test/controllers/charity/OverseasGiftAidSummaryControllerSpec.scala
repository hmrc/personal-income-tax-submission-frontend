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

import forms.YesNoForm
import play.api.mvc.Result
import utils.UnitTestWithApp
import views.html.charity.OverseasGiftAidSummaryView
import play.api.http.Status._

import scala.concurrent.Future

class OverseasGiftAidSummaryControllerSpec extends UnitTestWithApp {

  val taxYear: Int = 2022

  lazy val controller = new OverseasGiftAidSummaryController(
    app.injector.instanceOf[OverseasGiftAidSummaryView])(
    mockMessagesControllerComponents,
    authorisedAction,
    mockAppConfig)

  "Calling the 'getOverseasCharities' method" should {

    "return the list of overseas charities donated to" in {

      controller.getOverseasCharities shouldBe List("overseasCharity1", "overseasCharity2")
    }
  }

  "Calling the .show method" should {

    "return a 200 status" in new TestWithAuth {
      val result: Future[Result] = controller.show(taxYear)(fakeRequest)
      status(result) shouldBe OK
    }
  }

  "Calling the .submit method" when {

    "an option has been selected" should {
      lazy val result = controller.submit(taxYear)(fakeRequest.withFormUrlEncodedBody(
        YesNoForm.yesNo -> YesNoForm.yes
      ))

      s"has a status of SEE_OTHER($SEE_OTHER" in new TestWithAuth {
        status(result) shouldBe SEE_OTHER
      }
    }
  }

}
