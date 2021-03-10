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
import play.api.test.Helpers.{OK, UNAUTHORIZED, SEE_OTHER}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core._
import utils.IntegrationTest
import views.html.interest.UntaxedInterestView

import scala.concurrent.Future

class UntaxedInterestControllerTest extends IntegrationTest{

  lazy val frontendAppConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val taxYear: Int = 2022
  val invalidTaxYear: Int = 2023

  def controller(stubbedRetrieval: Future[_], acceptedConfidenceLevels: Seq[ConfidenceLevel] = Seq()): UntaxedInterestController = {
    new UntaxedInterestController(
      mcc,
      authAction(stubbedRetrieval, acceptedConfidenceLevels),
      app.injector.instanceOf[UntaxedInterestView])(
      frontendAppConfig
    )
  }

  "Hitting the show endpoint" should {

    s"return an OK ($OK)" when {

      "all auth requirements are met" in {
        val result = await(controller(successfulRetrieval).show(taxYear)(FakeRequest()))

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

      "Redirect when an invalid tax year has been added to the url" in {
        val retrieval: Future[Enrolments ~ Some[AffinityGroup]] = Future.successful(new ~(
          Enrolments(Set(
            Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "1234567890")), "Activated", None),
            Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", "AA123456A")), "Activated", None)
          )),
          Some(AffinityGroup.Individual)
        ))

        val result = await(controller(retrieval).show(invalidTaxYear)
        (FakeRequest()))

        result.header.status shouldBe SEE_OTHER
      }
    }
  }

}
