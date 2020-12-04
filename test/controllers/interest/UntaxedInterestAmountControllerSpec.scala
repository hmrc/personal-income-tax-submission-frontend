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

import play.api.mvc.Result
import utils.ViewTest
import views.html.interest.UntaxedInterestAmountView
import play.api.http.Status._

import scala.concurrent.Future

class UntaxedInterestAmountControllerSpec extends ViewTest {

  lazy val view = app.injector.instanceOf[UntaxedInterestAmountView]
  lazy val controller = new UntaxedInterestAmountController(mockMessagesControllerComponents, authorisedAction,view, mockAppConfig)

  val taxYear = 2020

  ".show" should {

    "return a result" which {
      "returns an OK status result" in new TestWithAuth {
        val result: Future[Result] = controller.show(taxYear)(fakeRequest)

        status(result) shouldBe OK
      }
    }
  }

  ".submit" should {

    "return a result" which {
      "returns has a OK status result" in new TestWithAuth {
        val result: Future[Result] = controller.submit(taxYear)(fakeRequest.withFormUrlEncodedBody(
          "accountName" -> "name", "untaxedAmount" -> "100"))

        status(result) shouldBe OK
      }

      "returns a BadRequest status error when the box has not been filled" in new TestWithAuth {
        val result: Future[Result] = controller.submit(taxYear)(fakeRequest.withFormUrlEncodedBody(
          "accountName" -> "name", "untaxedAmount" -> ""))

        status(result) shouldBe BAD_REQUEST
      }

    }

  }

}
