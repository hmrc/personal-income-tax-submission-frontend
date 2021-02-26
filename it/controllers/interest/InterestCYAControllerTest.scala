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

import audit.AuditService
import common.SessionValues
import config.{AppConfig, ErrorHandler}
import models.interest.{InterestAccountModel, InterestCYAModel}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{OK, UNAUTHORIZED}
import services.InterestSubmissionService
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core._
import utils.IntegrationTest
import views.html.interest.InterestCYAView

import scala.concurrent.Future

class InterestCYAControllerTest extends IntegrationTest {

  lazy val frontendAppConfig: AppConfig = app.injector.instanceOf[AppConfig]

  def controller(stubbedRetrieval: Future[_], acceptedConfidenceLevels: Seq[ConfidenceLevel] = Seq()): InterestCYAController = {
    new InterestCYAController(
      mcc,
      authAction(stubbedRetrieval, acceptedConfidenceLevels),
      app.injector.instanceOf[InterestCYAView],
      app.injector.instanceOf[InterestSubmissionService],
      app.injector.instanceOf[AuditService],
      app.injector.instanceOf[ErrorHandler]
    )
  }

  "Hitting the show endpoint" should {

    s"return an OK ($OK)" when {

      "all auth requirements are met" in {
        lazy val interestCYA = InterestCYAModel(
          Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", 25))),
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25)))
        )
        val retrieval: Future[Enrolments ~ Some[AffinityGroup]] = Future.successful(new ~(
          Enrolments(Set(
            Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "1234567890")), "Activated", None),
            Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", "AA123456A")), "Activated", None)
          )),
          Some(AffinityGroup.Individual)
        ))

        val result = await(controller(retrieval).show(2021)
        (FakeRequest().withSession(SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA)))))

        result.header.status shouldBe OK
      }
    }

    s"return an UNAUTHORISED ($UNAUTHORIZED)" when {

      "the confidence level is too low" in {
        val retrieval: Future[Enrolments ~ Some[AffinityGroup]] = Future.successful(new ~(
          Enrolments(Set(
            Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "1234567890")), "Activated", None),
            Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", "AA123456A")), "Activated", None)
          )),
          Some(AffinityGroup.Individual)
        ))

        val result = await(controller(retrieval, Seq(ConfidenceLevel.L500)).show(2021)(FakeRequest()))

        result.header.status shouldBe UNAUTHORIZED
      }

      "it contains the wrong credentials" in {
        val retrieval: Future[Enrolments ~ Some[AffinityGroup]] = Future.successful(new ~(
          Enrolments(Set(
            Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("UTR", "1234567890")), "Activated", None),
            Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", "AA123456A")), "Activated", None)
          )),
          Some(AffinityGroup.Individual)
        ))

        val result = await(controller(retrieval).show(2021)(FakeRequest()))

        result.header.status shouldBe UNAUTHORIZED
      }

    }
  }

}
