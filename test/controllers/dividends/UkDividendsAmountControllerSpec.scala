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
import models.DividendsCheckYourAnswersModel
import play.api.http.Status._
import play.api.mvc.Result
import utils.ViewTest
import views.html.dividends.UkDividendsAmountView

import scala.concurrent.Future

class UkDividendsAmountControllerSpec extends ViewTest {

  lazy val controller = new UkDividendsAmountController(
    mockMessagesControllerComponents,
    authorisedAction,
    app.injector.instanceOf[UkDividendsAmountView],
    mockAppConfig
  )

  ".show" should {

    "return a result with an OK status" in new TestWithAuth{
      status(controller.show()(fakeRequest)) shouldBe OK
    }

  }

  ".submit" should {

    "redirect to the overview page" when {

      "there is no cya model insession" in new TestWithAuth {

        val result: Future[Result] = controller.submit()(fakeRequest
          .withFormUrlEncodedBody("amount" -> "12000"))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl
      }

    }

    "redirect to the receive other dividends page when data submitted" in new TestWithAuth {

      val cyaModel = DividendsCheckYourAnswersModel(ukDividends = true, otherDividends = true)

      val result: Future[Result] = controller.submit()(fakeRequest
        .withFormUrlEncodedBody("amount" -> "120000")
        .withSession(SessionValues.DIVIDENDS_CYA -> cyaModel.asJsonString))

      status(result) shouldBe SEE_OTHER
      redirectUrl(result) shouldBe controllers.dividends.routes.ReceiveOtherDividendsController.show().url

    }

  }

}
