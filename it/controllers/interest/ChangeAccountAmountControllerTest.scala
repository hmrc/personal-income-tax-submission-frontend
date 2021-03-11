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

import common.SessionValues
import config.AppConfig
import models.interest.{InterestAccountModel, InterestCYAModel}
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers.{OK, SEE_OTHER, UNAUTHORIZED}
import uk.gov.hmrc.auth.core._
import utils.IntegrationTest
import views.html.interest.ChangeAccountAmountView

import scala.concurrent.Future

class ChangeAccountAmountControllerTest extends IntegrationTest{

  lazy val frontendAppConfig: AppConfig = app.injector.instanceOf[AppConfig]

  val taxYear: Int = 2022
  val invalidTaxYear: Int = 2023
  val amount: BigDecimal = 25

  def controller(stubbedRetrieval: Future[_], acceptedConfidenceLevels: Seq[ConfidenceLevel] = Seq()): ChangeAccountAmountController = {
    new ChangeAccountAmountController(
      mcc,
      authAction(stubbedRetrieval, acceptedConfidenceLevels),
      app.injector.instanceOf[ChangeAccountAmountView],
      frontendAppConfig
    )
  }

  "Hitting the show endpoint" should {

    s"return an OK ($OK)" when {

      "all auth requirements are met" in {
        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", amount)))
        )
        lazy val priorSub = Json.arr(
          Json.obj(
            "accountName" -> "Taxed Account",
            "incomeSourceId" -> "TaxedId",
            "taxedUkInterest" -> 25
          )
        )
        val result = await(controller(successfulRetrieval).show(taxYear, "taxed", "TaxedId")
        (FakeRequest().withSession(
          (SessionValues.INTEREST_CYA, Json.prettyPrint(Json.toJson(interestCYA))),
          (SessionValues.INTEREST_PRIOR_SUB, priorSub.toString()))
        ))

        result.header.status shouldBe OK
      }
    }

    s"return an UNAUTHORISED ($UNAUTHORIZED)" when {

      "it contains the wrong credentials" in {
        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", amount)))
        )
        lazy val priorSub = Json.arr(
          Json.obj(
            "accountName" -> "Taxed Account",
            "incomeSourceId" -> "TaxedId",
            "taxedUkInterest" -> 25
          )
        )

        val result = await(controller(incorrectCredsRetrieval).show(taxYear, "taxed", "TaxedId")
        (FakeRequest().withSession(
          (SessionValues.INTEREST_CYA, Json.prettyPrint(Json.toJson(interestCYA))),
          (SessionValues.INTEREST_PRIOR_SUB, priorSub.toString()))
        ))

        result.header.status shouldBe UNAUTHORIZED
      }

    }

    "redirect to the IV journey in income-tax-submission-frontend" when {

      "the confidence level is too low" in {
        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", amount)))
        )
        lazy val priorSub = Json.arr(
          Json.obj(
            "accountName" -> "Taxed Account",
            "incomeSourceId" -> "TaxedId",
            "taxedUkInterest" -> 25
          )
        )

        val result = await(controller(insufficientConfidenceRetrieval, Seq(ConfidenceLevel.L500)).show(taxYear, "taxed", "TaxedId")
        (FakeRequest().withSession(
          (SessionValues.INTEREST_CYA, Json.prettyPrint(Json.toJson(interestCYA))),
          (SessionValues.INTEREST_PRIOR_SUB, priorSub.toString()))
        ))

        result.header.status shouldBe SEE_OTHER
        result.header.headers("Location") shouldBe "http://localhost:11111/income-through-software/return/iv-uplift"
      }

    }
    "Redirect when an invalid tax year has been added to the url" in {

      lazy val interestCYA = InterestCYAModel(
        Some(false), None,
        Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", amount)))
      )
      lazy val priorSub = Json.arr(
        Json.obj(
          "accountName" -> "Taxed Account",
          "incomeSourceId" -> "TaxedId",
          "taxedUkInterest" -> 25
        )
      )

      val result = await(controller(successfulRetrieval).show(invalidTaxYear, "taxed", "TaxedId")
      (FakeRequest().withSession(
        (SessionValues.INTEREST_CYA, Json.prettyPrint(Json.toJson(interestCYA))),
        (SessionValues.INTEREST_PRIOR_SUB, priorSub.toString()))
      ))

      result.header.status shouldBe SEE_OTHER
    }

  }

}
