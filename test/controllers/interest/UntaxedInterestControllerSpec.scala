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

package controllers.interest

import forms.YesNoForm
import play.api.http.Status._
import play.api.mvc.Result
import utils.ViewTest
import views.html.interest.UntaxedInterestView

import scala.concurrent.Future

class UntaxedInterestControllerSpec extends ViewTest {

  val view = app.injector.instanceOf[UntaxedInterestView]

  lazy val controller = new UntaxedInterestController(
    mockMessagesControllerComponents,
    authorisedAction,
    app.injector.instanceOf[UntaxedInterestView])(mockAppConfig)


  val taxYear = 2020

  ".show for an individual" should {

    "return a result" which {

      s"has an OK($OK) status" in new TestWithAuth {
        val result: Future[Result] = controller.show(taxYear)(fakeRequest)

        status(result) shouldBe OK
      }
    }
  }

  ".submit for an individual" should {

    "return a result" which {

      s"has a redirect($SEE_OTHER) status" in new TestWithAuth {
        val result: Future[Result] = controller.submit(taxYear)(fakeRequest
          .withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes))

        status(result) shouldBe SEE_OTHER
      }

      s"returns a bad request ($BAD_REQUEST) when a box isn't ticked" in new TestWithAuth {
        val result: Future[Result] = controller.submit(taxYear)(fakeRequest)

        status(result) shouldBe BAD_REQUEST
      }
    }
  }

  ".show for an agent" should {

    "return a result" which {

      s"has an OK($OK) status" in new TestWithAuth(isAgent = true) {
        val result: Future[Result] = controller.show(taxYear)(fakeRequestWithMtditid)

        status(result) shouldBe OK
      }
    }
  }

  ".submit for an agent" should {

    "return a result" which {

      s"has a redirect($SEE_OTHER) status" in new TestWithAuth(isAgent = true) {
        val result: Future[Result] = controller.submit(taxYear)(fakeRequestWithMtditid.withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes))

        status(result) shouldBe SEE_OTHER
      }

      s"returns a bad request ($BAD_REQUEST) when a box isn't ticked" in new TestWithAuth(isAgent = true) {
        val result: Future[Result] = controller.submit(taxYear)(fakeRequestWithMtditid)

        status(result) shouldBe BAD_REQUEST
      }
    }
  }

}
