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

import common.SessionValues
import models.interest.{InterestAccountModel, InterestCYAModel}
import play.api.http.Status._
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import utils.ViewTest
import views.html.interest.InterestCYAView

import scala.concurrent.Future

class InterestCYAControllerSpec extends ViewTest {

  lazy val view: InterestCYAView = app.injector.instanceOf[InterestCYAView]
  lazy val controller: InterestCYAController = new InterestCYAController(mockMessagesControllerComponents, authorisedAction, view)(mockAppConfig)

  val taxYear: Int = 2020
  val arbitraryAmount: Int = 100

  ".show" should {

    s"return an OK($OK)" when {

      "there is CYA data in session" in new TestWithAuth {
        lazy val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
          SessionValues.INTEREST_CYA -> InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel("", "", arbitraryAmount))),
            Some(true), Some(Seq(InterestAccountModel("", "", arbitraryAmount)))
          ).asJsonString
        )

        lazy val result: Future[Result] = controller.show(taxYear)(request)

        status(result) shouldBe OK
      }

    }

    s"redirect to the overview page" when {

      "there is no CYA data in session" in new TestWithAuth {

        lazy val result: Future[Result] = controller.show(taxYear)(FakeRequest())

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
      }

    }

  }

  ".submit" should {

    "redirect to the overview page" in new TestWithAuth {
      val result: Future[Result] = controller.submit(taxYear)(FakeRequest())

      status(result) shouldBe SEE_OTHER
      redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
    }

  }

}
