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
import utils.UnitTestWithApp
import views.html.charity.RemoveOverseasCharityView
import play.api.http.Status._
import play.api.mvc.Result

import scala.concurrent.Future

class RemoveOverseasControllerSpec extends UnitTestWithApp {

  lazy val controller: RemoveOverseasCharityController = new RemoveOverseasCharityController()(
      mockMessagesControllerComponents,
      authorisedAction,
      app.injector.instanceOf[RemoveOverseasCharityView],
      mockAppConfig
  )

  val taxYear: Int = 2022

  ".show" should {

    "return a result" which {

      s"has an OK($OK) result" in new TestWithAuth {

        val result: Future[Result] = controller.show(taxYear)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))

        status(result) shouldBe OK
      }
    }
  }
  
}
