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
import play.api.test.Helpers.{OK, SEE_OTHER, UNAUTHORIZED}
import uk.gov.hmrc.auth.core._
import utils.IntegrationTest
import views.html.interest.TaxedInterestAmountView

import java.util.UUID.randomUUID
import scala.concurrent.Future

class TaxedInterestAmountControllerTest extends IntegrationTest {

  lazy val frontendAppConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val taxYear: Int = 2022
  val invalidTaxYear: Int = 2023

  def controller(stubbedRetrieval: Future[_], acceptedConfidenceLevels: Seq[ConfidenceLevel] = Seq()): TaxedInterestAmountController = {
    new TaxedInterestAmountController(
      mcc,
      authAction(stubbedRetrieval, acceptedConfidenceLevels),
      app.injector.instanceOf[TaxedInterestAmountView]
    )
  }

  "Hitting the show endpoint" should {

    s"return an OK ($OK)" when {

      "all auth requirements are met" in {
        lazy val uuid = randomUUID().toString
        val result = await(controller(successfulRetrieval).show(taxYear, uuid)(FakeRequest()))

        result.header.status shouldBe OK
      }
    }

    s"return an UNAUTHORISED ($UNAUTHORIZED)" when {

      "it contains the wrong credentials" in {
        lazy val uuid = randomUUID().toString

        val result = await(controller(incorrectCredsRetrieval).show(taxYear, uuid)(FakeRequest()))

        result.header.status shouldBe UNAUTHORIZED
      }

    }

    "return to the IV journey in income-tax-submission-frontend" when {

      "the confidence level is below 200" in {
        lazy val uuid = randomUUID().toString
        val result = await(controller(insufficientConfidenceRetrieval).show(taxYear, uuid)(FakeRequest()))

        result.header.status shouldBe SEE_OTHER
        result.header.headers("Location") shouldBe "http://localhost:11111/income-through-software/return/iv-uplift"
      }

    }

    "Redirect when an invalid tax year has been added to the url" in {
      lazy val uuid = randomUUID().toString
      val retrieval: Future[Enrolments ~ Some[AffinityGroup]] = Future.successful(new ~(
        Enrolments(Set(
          Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "1234567890")), "Activated", None),
          Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", "AA123456A")), "Activated", None)
        )),
        Some(AffinityGroup.Individual)
      ))

      val result = await(controller(retrieval).show(invalidTaxYear, uuid)(FakeRequest()))

      result.header.status shouldBe SEE_OTHER
    }

  }
}
