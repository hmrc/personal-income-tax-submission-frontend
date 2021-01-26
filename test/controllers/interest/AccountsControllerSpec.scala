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

import common.{InterestTaxTypes, SessionValues}
import models.interest.{InterestAccountModel, InterestCYAModel}
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.UnitTestWithApp
import views.html.interest.InterestAccountsView

import scala.concurrent.Future

class AccountsControllerSpec extends UnitTestWithApp {

  val TAXED = "taxed"
  val UNTAXED = "untaxed"

  val taxYear = 2020

  lazy val controller = new AccountsController(stubMessagesControllerComponents(), app.injector.instanceOf[InterestAccountsView], authorisedAction)

  lazy val interestCyaModel = InterestCYAModel(
    Some(true), Some(Seq(InterestAccountModel(Some("azerty"), "Account 1", 100.01))),
    Some(true), Some(Seq(InterestAccountModel(Some("qwerty"), "Account 2", 9001.01)))
  )

  def maxRequest(cyaModel: InterestCYAModel = interestCyaModel): FakeRequest[AnyContentAsEmpty.type] = fakeRequest
    .withSession(SessionValues.INTEREST_CYA -> cyaModel.asJsonString)

  ".show" when {

    "provided the taxType of 'taxed'" should {

      "return an OK" when {

        "there is cya data and account data" in new TestWithAuth {
          val result: Future[Result] = controller.show(taxYear, TAXED)(maxRequest())

          status(result) shouldBe OK
        }

      }

      "redirect to the do you want to add an account page" when {

        "there is cya data and no account data" which {
          lazy val result = controller.show(taxYear, TAXED)(maxRequest(interestCyaModel.copy(taxedUkAccounts = None)))

          s"has a status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "has the correct redirect url" in {
            redirectUrl(result) shouldBe controllers.interest.routes.TaxedInterestController.show(taxYear).url
          }
        }

      }

      "redirect to the overview page" when {

        "there is no cya data" which {
          lazy val result = controller.show(taxYear, TAXED)(fakeRequest)

          s"has a status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "has the correct redirect url" in {
            redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
          }
        }

      }

    }

    "provided the taxType of 'untaxed'" should {

      "return an OK" when {

        "there is cya data and account data" in new TestWithAuth {
          val result: Future[Result] = controller.show(taxYear, UNTAXED)(maxRequest())

          status(result) shouldBe OK
        }

      }

      "redirect to the do you want to add an account page" when {

        "there is cya data and no account data" which {
          lazy val result = controller.show(taxYear, UNTAXED)(maxRequest(interestCyaModel.copy(untaxedUkAccounts = None)))

          s"has a status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "has the correct redirect url" in {
            redirectUrl(result) shouldBe controllers.interest.routes.UntaxedInterestController.show(taxYear).url
          }
        }

      }

      "redirect to the overview page" when {

        "there is no cya data" which {
          lazy val result = controller.show(taxYear, TAXED)(fakeRequest)

          s"has a status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "has the correct redirect url" in {
            redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
          }
        }

      }

    }

  }

  ".submit" should {

    "redirect to the cya page" when {

      "accounts exist for the given tax type and the cya model is complete" which {

        "has tax type of taxed" which {

          lazy val result = controller.submit(taxYear, TAXED)(maxRequest(interestCyaModel.copy(
            untaxedUkInterest = Some(false),
            untaxedUkAccounts = None
          )))

          s"has a SEE_OTHER($SEE_OTHER) status" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "has the correct redirect url" in {
            redirectUrl(result) shouldBe controllers.interest.routes.InterestCYAController.show(taxYear).url
          }

        }

        "has tax type of untaxed" which {

          lazy val result = controller.submit(taxYear, UNTAXED)(maxRequest(interestCyaModel.copy(
            taxedUkInterest = Some(false),
            taxedUkAccounts = None
          )))

          s"has a SEE_OTHER($SEE_OTHER) status" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "has the correct redirect url" in {
            redirectUrl(result) shouldBe controllers.interest.routes.InterestCYAController.show(taxYear).url
          }

        }

      }

    }

    "redirect to the overview page" when {

      "there is no CYA data in session" which {

        lazy val result = controller.submit(taxYear, TAXED)(fakeRequest)

        s"has a status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct redirect url" in {
          redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
        }

      }

    }

    "redirect to the taxed start page" when {

      "the journey is still in progress" which {

        lazy val result = controller.submit(taxYear, UNTAXED)(maxRequest(interestCyaModel.copy(
          taxedUkInterest = None,
          taxedUkAccounts = None
        )))

        s"has a status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct redirect url" in {
          redirectUrl(result) shouldBe controllers.interest.routes.TaxedInterestController.show(taxYear).url
        }

      }

    }

    "redirect to the appropriate 'do you receive' page" when {

      "there is cya data but no account data" when {

        "tax type is TAXED" which {

          lazy val result = controller.submit(taxYear, TAXED)(maxRequest(interestCyaModel.copy(taxedUkAccounts = None)))

          s"has a status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "has the correct redirect url" in {
            redirectUrl(result) shouldBe controllers.interest.routes.TaxedInterestController.show(taxYear).url
          }

        }

        "tax type is UNTAXED" which {

          lazy val result = controller.submit(taxYear, UNTAXED)(maxRequest(interestCyaModel.copy(untaxedUkAccounts = None)))

          s"has a status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "has the correct redirect url" in {
            redirectUrl(result) shouldBe controllers.interest.routes.UntaxedInterestController.show(taxYear).url
          }

        }

      }

    }

  }

  ".getTaxAccounts" should {

    val taxedAccount = Seq(InterestAccountModel(Some("taxedId"), "Taxed Account", 9023.11))
    val untaxedAccount = Seq(InterestAccountModel(Some("untaxedId"), "Untaxed Account", 0.03))

    "return taxed accounts" when {

      "taxType is taxed" in {
        val cyaData = InterestCYAModel(Some(true), Some(untaxedAccount), Some(true), Some(taxedAccount))

        controller.getTaxAccounts(TAXED, cyaData) shouldBe Some(taxedAccount)
      }

    }

    "return untaxed accounts" when {

      "taxType is untaxed" in {
        val cyaData = InterestCYAModel(Some(true), Some(untaxedAccount), Some(true), Some(taxedAccount))

        controller.getTaxAccounts(UNTAXED, cyaData) shouldBe Some(untaxedAccount)
      }

    }

  }

  ".missingAccountRedirect" should {

    "redirect to the [taxed redirect] page" when {

      "taxType is taxed" which {
        lazy val result = Future.successful(controller.missingAccountsRedirect(TAXED, taxYear))

        s"has a status of SEE_OTHER($SEE_OTHER)" in {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct url" in {
          redirectUrl(result) shouldBe controllers.interest.routes.TaxedInterestController.show(taxYear).url
        }
      }

    }

    "redirect to the [untaxed redirect] page" when {

      "taxType is untaxed" which {
        lazy val result = Future.successful(controller.missingAccountsRedirect(UNTAXED, taxYear))

        s"has a status of SEE_OTHER($SEE_OTHER)" in {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct url" in {
          redirectUrl(result) shouldBe controllers.interest.routes.UntaxedInterestController.show(taxYear).url
        }
      }

    }

  }

}
