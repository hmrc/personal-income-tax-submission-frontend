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
import controllers.predicates.AuthorisedAction
import models.interest.{InterestAccountModel, InterestCYAModel}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers.SEE_OTHER
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.ViewTest
import views.html.interest.ChangeAccountAmountView

import scala.concurrent.Future

class ChangeAccountAmountControllerSpec extends ViewTest {

  lazy val controller = new ChangeAccountAmountController(
    mockMessagesControllerComponents,
    authorisedAction,
    app.injector.instanceOf[ChangeAccountAmountView],
    mockAppConfig
  )

  val amountTypeField = "whichAmount"
  val otherAmountInputField = "amount"

  val taxYear = 2022
  val untaxedId = "UntaxedId"
  val taxedId = "TaxedId"

  val UNTAXED = "untaxed"
  val TAXED = "taxed"

  lazy val untaxedPriorDataModel = Json.arr(
    Json.obj(
      "accountName" -> "Untaxed Account",
      "incomeSourceId" -> "UntaxedId",
      "untaxedUkInterest" -> 5000
    )
  )

  lazy val taxedPriorDataModel = Json.arr(
    Json.obj(
      "accountName" -> "Taxed Account",
      "incomeSourceId" -> "TaxedId",
      "taxedUkInterest" -> 2500
    )
  )

  lazy val untaxedInterestCyaModel = InterestCYAModel(
    Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", 50))),
    Some(false), None
  )

  lazy val taxedInterestCyaModel = InterestCYAModel(
    Some(false), None,
    Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25)))
  )


  ".show" should {

    "with a tax type of untaxed" should {

      "return an ok" when {

        "there is prior and cya data in session" in new TestWithAuth {
          lazy val result: Future[Result] = {
            controller.show(taxYear, UNTAXED, untaxedId)(fakeRequest
              .withSession(SessionValues.INTEREST_PRIOR_SUB -> untaxedPriorDataModel.toString,
                SessionValues.INTEREST_CYA -> untaxedInterestCyaModel.asJsonString))
          }
          status(result) shouldBe OK
          bodyOf(result) should include("5000")
        }

        "there is prior and cya data in session with a unique session id" in new TestWithAuth {
          lazy val result: Future[Result] = {
            controller.show(taxYear, UNTAXED, untaxedId)(fakeRequest
              .withSession(SessionValues.INTEREST_PRIOR_SUB -> untaxedPriorDataModel.toString,
                SessionValues.INTEREST_CYA ->
                  InterestCYAModel(
                    Some(true), Some(Seq(InterestAccountModel(None, "Untaxed Account", 50, Some("UntaxedId")))),
                    Some(false), None
                  ).asJsonString))
          }
          status(result) shouldBe OK
          bodyOf(result) should include("5000")
        }

      }

      "redirect to the accounts page" when {
        "there is no prior data but there is cya data in session" in new TestWithAuth {
          lazy val result: Future[Result] = {
            controller.show(taxYear, UNTAXED, untaxedId)(fakeRequest
              .withSession(SessionValues.INTEREST_CYA -> untaxedInterestCyaModel.asJsonString))
          }
          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe controllers.interest.routes.AccountsController.show(taxYear, UNTAXED).url
        }

      }


      "redirect to the overview page" when {
        "there is neither prior nor cya data in session" in new TestWithAuth {
          lazy val result: Future[Result] = {
            controller.show(taxYear, UNTAXED, untaxedId)(fakeRequest)
          }
          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
        }

      }

    }

    "with a tax type of taxed" should {

      "return an ok" when {

        "there is prior and cya data in session" in new TestWithAuth {
          lazy val result: Future[Result] = {
            controller.show(taxYear, TAXED, taxedId)(fakeRequest
              .withSession(SessionValues.INTEREST_PRIOR_SUB -> taxedPriorDataModel.toString,
                SessionValues.INTEREST_CYA -> taxedInterestCyaModel.asJsonString))
          }
          status(result) shouldBe OK
          bodyOf(result) should include("2500")

        }

        "there is prior and cya data in session with a unique session id" in new TestWithAuth {
          lazy val result: Future[Result] = {
            controller.show(taxYear, TAXED, taxedId)(fakeRequest
              .withSession(SessionValues.INTEREST_PRIOR_SUB -> taxedPriorDataModel.toString,
                SessionValues.INTEREST_CYA ->
                  InterestCYAModel(
                    Some(false), None,
                    Some(true), Some(Seq(InterestAccountModel(None, "Taxed Account", 25, Some("TaxedId"))))
                  ).asJsonString))
          }
          status(result) shouldBe OK
          bodyOf(result) should include("2500")
        }

      }

      "redirect to the accounts page" when {
        "there is no prior data but there is cya data in session" in new TestWithAuth {
          lazy val result: Future[Result] = {
            controller.show(taxYear, TAXED, taxedId)(fakeRequest
              .withSession(SessionValues.INTEREST_CYA -> taxedInterestCyaModel.asJsonString))
          }
          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe controllers.interest.routes.AccountsController.show(taxYear, TAXED).url
        }

      }


      "redirect to the overview page" when {
        "there is neither prior nor cya data in session" in new TestWithAuth {
          lazy val result: Future[Result] = {
            controller.show(taxYear, TAXED, taxedId)(fakeRequest)
          }
          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
        }

      }

    }

    "Redirect to the tax year error " when {

      "an invalid tax year has been added to the url" in new TestWithAuth() {


        val mockAppConfFeatureSwitch: AppConfig = new AppConfig(mock[ServicesConfig]){
          override lazy val defaultTaxYear: Int = 2022
          override lazy val taxYearErrorFeature = true
        }

        val authorisedActionFeatureSwitch = new AuthorisedAction(mockAppConfFeatureSwitch,
          agentAuthErrorPageView)(mockAuthService, stubMessagesControllerComponents())

        lazy val featureSwitchController = new ChangeAccountAmountController(
          mockMessagesControllerComponents,
          authorisedActionFeatureSwitch,
          app.injector.instanceOf[ChangeAccountAmountView],
          mockAppConfFeatureSwitch
        )

        val invalidTaxYear = 2023
        lazy val result: Future[Result] = featureSwitchController.show(invalidTaxYear, TAXED, taxedId)(fakeRequest)

        redirectUrl(result) shouldBe controllers.routes.TaxYearErrorController.show().url

      }
    }

  }

  ".submit" should {

    def getAccountModel(result: Future[Result]): InterestCYAModel = {
      Json.parse(await(result).session.get(SessionValues.INTEREST_CYA).get).as[InterestCYAModel]
    }

    "with a tax type of untaxed" should {

      "return errors" when {

        "the input does not match the required numerical validation with prior and cya data" in new TestWithAuth {
          lazy val result: Future[Result] = controller.submit(taxYear, UNTAXED, untaxedId)(fakeRequest
            .withSession(SessionValues.INTEREST_PRIOR_SUB -> untaxedPriorDataModel.toString,
              SessionValues.INTEREST_CYA -> untaxedInterestCyaModel.asJsonString)
            .withFormUrlEncodedBody(
              amountTypeField -> "other",
              otherAmountInputField -> "ASDFGHJ"
            ))

          status(result) shouldBe BAD_REQUEST
          bodyOf(result) should include("common.error.invalid_number")
        }

      }

      "successfully redirect to the Account overview page" when {
        "the prior account  amount is selected as the chosen value" in new TestWithAuth {
          lazy val result: Future[Result] = controller.submit(taxYear, UNTAXED, untaxedId)(fakeRequest
            .withSession(SessionValues.INTEREST_PRIOR_SUB -> untaxedPriorDataModel.toString,
              SessionValues.INTEREST_CYA -> untaxedInterestCyaModel.asJsonString)
            .withFormUrlEncodedBody(
              amountTypeField -> "prior"
            ))

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe controllers.interest.routes.AccountsController.show(taxYear, UNTAXED).url
        }

        "the account amount is updated" in new TestWithAuth {
          lazy val result: Future[Result] = controller.submit(taxYear, UNTAXED, untaxedId)(fakeRequest
            .withSession(SessionValues.INTEREST_PRIOR_SUB -> untaxedPriorDataModel.toString,
              SessionValues.INTEREST_CYA -> untaxedInterestCyaModel.asJsonString)
            .withFormUrlEncodedBody(
              amountTypeField -> "other",
              otherAmountInputField -> "75"
            ))

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe controllers.interest.routes.AccountsController.show(taxYear, UNTAXED).url

          val account = getAccountModel(result).untaxedUkAccounts.head

          account.head.amount shouldBe 75
        }

        "the account amount is updated with a unique session id" in new TestWithAuth {
          lazy val result: Future[Result] = controller.submit(taxYear, UNTAXED, untaxedId)(fakeRequest
            .withSession(SessionValues.INTEREST_PRIOR_SUB -> untaxedPriorDataModel.toString,
              SessionValues.INTEREST_CYA ->
                InterestCYAModel(
                Some(true), Some(Seq(InterestAccountModel(None, "Untaxed Account", 50, Some("UntaxedId")))),
                Some(false), None
              ).asJsonString)
            .withFormUrlEncodedBody(
              amountTypeField -> "other",
              otherAmountInputField -> "75"
            ))

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe controllers.interest.routes.AccountsController.show(taxYear, UNTAXED).url

          val account = getAccountModel(result).untaxedUkAccounts.head

          account.head.amount shouldBe 75
        }

      }

    }

    "with a tax type of taxed" should {

      "return errors" when {

        "the input does not match the required numerical validation with prior and cya data" in new TestWithAuth {
          lazy val result: Future[Result] = controller.submit(taxYear, TAXED, taxedId)(fakeRequest
            .withSession(SessionValues.INTEREST_PRIOR_SUB -> taxedPriorDataModel.toString,
              SessionValues.INTEREST_CYA -> taxedInterestCyaModel.asJsonString)
            .withFormUrlEncodedBody(
              amountTypeField -> "other",
              otherAmountInputField -> "ASDFGHJ"
            ))

          status(result) shouldBe BAD_REQUEST
          bodyOf(result) should include("common.error.invalid_number")
        }

      }

      "successfully redirect to the Account overview page" when {
        "the prior account amount is selected as the chosen value" in new TestWithAuth {
          lazy val result: Future[Result] = controller.submit(taxYear, TAXED, taxedId)(fakeRequest
            .withSession(SessionValues.INTEREST_PRIOR_SUB -> taxedPriorDataModel.toString,
              SessionValues.INTEREST_CYA -> taxedInterestCyaModel.asJsonString)
            .withFormUrlEncodedBody(
              amountTypeField -> "prior"
            ))

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe controllers.interest.routes.AccountsController.show(taxYear, TAXED).url
        }

        "the account amount is updated" in new TestWithAuth {
          lazy val result: Future[Result] = controller.submit(taxYear, TAXED, taxedId)(fakeRequest
            .withSession(SessionValues.INTEREST_PRIOR_SUB -> taxedPriorDataModel.toString,
              SessionValues.INTEREST_CYA -> taxedInterestCyaModel.asJsonString)
            .withFormUrlEncodedBody(
              amountTypeField -> "other",
              otherAmountInputField -> "100"
            ))

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe controllers.interest.routes.AccountsController.show(taxYear, TAXED).url

          val account = getAccountModel(result).taxedUkAccounts.head

          account.head.amount shouldBe 100
        }

        "the account amount is updated with a unique session id" in new TestWithAuth {
          lazy val result: Future[Result] = controller.submit(taxYear, TAXED, taxedId)(fakeRequest
            .withSession(SessionValues.INTEREST_PRIOR_SUB -> taxedPriorDataModel.toString,
              SessionValues.INTEREST_CYA ->
                InterestCYAModel(
                  Some(false), None,
                  Some(true), Some(Seq(InterestAccountModel(None, "Taxed Account", 25, Some("TaxedId"))))
                ).asJsonString)
            .withFormUrlEncodedBody(
              amountTypeField -> "other",
              otherAmountInputField -> "100"
            ))

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe controllers.interest.routes.AccountsController.show(taxYear, TAXED).url

          val account = getAccountModel(result).taxedUkAccounts.head

          account.head.amount shouldBe 100
        }

      }

    }

    "redirect to the Account overview page" when {
      "there is no cya data in session" in new TestWithAuth {
        lazy val result: Future[Result] = controller.submit(taxYear, UNTAXED, untaxedId)(fakeRequest
          .withSession(SessionValues.INTEREST_PRIOR_SUB -> untaxedPriorDataModel.toString)
          .withFormUrlEncodedBody(
            amountTypeField -> "other",
            otherAmountInputField -> "100"
          ))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe controllers.interest.routes.AccountsController.show(taxYear, UNTAXED).url
      }

      "there is no prior account data in session" in new TestWithAuth {
        lazy val result: Future[Result] = controller.submit(taxYear, UNTAXED, untaxedId)(fakeRequest
          .withSession(SessionValues.INTEREST_CYA -> untaxedInterestCyaModel.asJsonString)
          .withFormUrlEncodedBody(
            amountTypeField -> "other",
            otherAmountInputField -> "100"
          ))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe controllers.interest.routes.AccountsController.show(taxYear, UNTAXED).url
      }

    }

  }

  ".updateAccounts" should {

    def getAccountModel(result: Future[Result]): InterestCYAModel = {
      Json.parse(await(result).session.get(SessionValues.INTEREST_CYA).get).as[InterestCYAModel]
    }

    "with a tax type of untaxed" should {
      "not update the prior value with missing identifiers in the cya model" in new TestWithAuth {
        lazy val result: Future[Result] = controller.submit(taxYear, UNTAXED, untaxedId)(fakeRequest
          .withSession(SessionValues.INTEREST_PRIOR_SUB -> untaxedPriorDataModel.toString,
            SessionValues.INTEREST_CYA ->
              InterestCYAModel(
                Some(true), Some(Seq(InterestAccountModel(None, "Untaxed Account", 75, None))),
                Some(false), None
              ).asJsonString)
          .withFormUrlEncodedBody(
            amountTypeField -> "other",
            otherAmountInputField -> "100"
          ))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe controllers.interest.routes.AccountsController.show(taxYear, UNTAXED).url

        val account = getAccountModel(result).untaxedUkAccounts.head

        account.head.amount shouldBe 75
      }

    }

    "with a tax type of taxed" should {

      "not update the prior value with missing identifiers in the cya model" in new TestWithAuth {
        lazy val result: Future[Result] = controller.submit(taxYear, TAXED, taxedId)(fakeRequest
          .withSession(SessionValues.INTEREST_PRIOR_SUB -> taxedPriorDataModel.toString,
            SessionValues.INTEREST_CYA ->
              InterestCYAModel(
                Some(false), None,
                Some(true), Some(Seq(InterestAccountModel(None, "Taxed Account", 25, None)))
              ).asJsonString)
          .withFormUrlEncodedBody(
            amountTypeField -> "other",
            otherAmountInputField -> "100"
          ))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe controllers.interest.routes.AccountsController.show(taxYear, TAXED).url

        val account = getAccountModel(result).taxedUkAccounts.head

        account.head.amount shouldBe 25
      }

    }

  }

}
