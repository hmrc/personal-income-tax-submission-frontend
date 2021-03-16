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

package controllers.interest

import config.AppConfig
import play.api.test.FakeRequest
import play.api.test.Helpers.{OK, UNAUTHORIZED}
import uk.gov.hmrc.auth.core._
import utils.IntegrationTest
import views.html.interest.TaxedInterestView

import scala.concurrent.Future

class TaxedInterestControllerTest extends IntegrationTest{

  lazy val frontendAppConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val taxYear: Int = 2022

  def controller(stubbedRetrieval: Future[_], acceptedConfidenceLevels: Seq[ConfidenceLevel] = Seq()): TaxedInterestController = {
    new TaxedInterestController(
      authAction(stubbedRetrieval, acceptedConfidenceLevels),
      app.injector.instanceOf[TaxedInterestView]
    )(
      frontendAppConfig,
      mcc,
    )
  }

  "Hitting the show endpoint" should {

    s"return an OK ($OK)" when {

      "all auth requirements are met" in {
        val result = await(controller(successfulRetrieval).show(taxYear)
        (FakeRequest()))

        result.header.status shouldBe OK
      }
    }

    s"return an UNAUTHORISED ($UNAUTHORIZED)" when {

      "the confidence level is too low" in {
        val result = await(controller(incorrectCredsRetrieval, Seq(ConfidenceLevel.L500)).show(taxYear)(FakeRequest()))

        result.header.status shouldBe UNAUTHORIZED
      }

      "it contains the wrong credentials" in {
        val result = await(controller(incorrectCredsRetrieval).show(taxYear)(FakeRequest()))

        result.header.status shouldBe UNAUTHORIZED
      }

    }

  }

}
