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

import common.InterestTaxTypes._
import common.{InterestTaxTypes, SessionValues}
import models.interest.{InterestAccountModel, InterestCYAModel}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import utils.ViewTest
import views.html.interest.TaxedInterestAmountView

import scala.concurrent.Future

class TaxedInterestAmountControllerSpec extends ViewTest {

  implicit def wrapOptional[T](input: T): Option[T] = Some(input)

  lazy val controller = new TaxedInterestAmountController(mockMessagesControllerComponents,
    authorisedAction, app.injector.instanceOf[TaxedInterestAmountView])(mockAppConfig)

  val taxYear = 2020

  ".show" should {

    "return an OK" when {

      "modify is None" in new TestWithAuth {
        lazy val result: Future[Result] = controller.show(taxYear, None)(fakeRequest)

        status(result) shouldBe OK
      }

      "modify has a value, with CYA data" in new TestWithAuth {
        lazy val result: Future[Result] = controller.show(taxYear, Some("qwerty"))(fakeRequest
          .withSession(
            SessionValues.INTEREST_CYA -> InterestCYAModel(false, None, true, Seq(
              InterestAccountModel(Some("qwerty"), "TSB 1", 300.00, None),
              InterestAccountModel(None, "TSB 2", 300.00, Some("qwerty")),
              InterestAccountModel(Some(""), "TSB 3", 300.00, None),
              InterestAccountModel(None, "TSB 3", 300.00, None)
            )).asJsonString
          ))

        status(result) shouldBe OK
      }

    }

  }

  ".submit" should {

    "redirect to the taxed accounts view page" when {

      "there is a correctly submitted form and there is no modify value, with CYA data" which {
        lazy val result = controller.submit(taxYear, None)(fakeRequest.withFormUrlEncodedBody(
          "friendlyName" -> "Some Name",
          "incomeTaxAmount" -> "100"
        ).withSession(
          SessionValues.INTEREST_CYA -> InterestCYAModel(
            false,
            None,
            true,
            Seq(InterestAccountModel("asdf", "TSB", 100.00))
          ).asJsonString
        ))

        s"has a status of SEE_OTHER($SEE_OTHER" in new TestWithAuth {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct redirect URL" in {
          redirectUrl(result) shouldBe controllers.interest.routes.AccountsController.show(taxYear, TAXED).url
        }

        "has updated the taxed accounts overview back link" in {
          val expectedResultContains = controllers.interest.routes.TaxedInterestAmountController.show(taxYear).url

          getSession(result).get(SessionValues.PAGE_BACK_TAXED_ACCOUNTS).get should include (expectedResultContains)
        }

      }

      "there is a correctly submitted form, with a modify value (that matches an account id with no session identifier) and CYA data" which {

        val startingAccount = InterestAccountModel(Some("qwerty"), "TSB", 200.00, None)
        val otherAccount = InterestAccountModel(Some("azerty"), "TSB", 200.00, None)
        val expectedAccount = InterestAccountModel(Some("qwerty"), "TSB Account", 500.00, None)

        lazy val result = controller.submit(taxYear, Some("qwerty"))(fakeRequest.withFormUrlEncodedBody(
          "friendlyName" -> "TSB Account",
          "incomeTaxAmount" -> "500.00"
        ).withSession(
          SessionValues.INTEREST_CYA -> InterestCYAModel(
            false,
            None,
            true,
            Seq(startingAccount, otherAccount)
          ).asJsonString
        ))

        s"has a status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct redirect url" in {
          redirectUrl(result) shouldBe controllers.interest.routes.AccountsController.show(taxYear, TAXED).url
        }

        "has updated the correct account" in {
          val model = Json.parse(getSession(result).get(SessionValues.INTEREST_CYA).get).as[InterestCYAModel]
          model.taxedUkAccounts shouldBe Some(Seq(expectedAccount, otherAccount))
        }
      }

      "there is a correctly submitted form, with a modify value (that matches an account id with a session identifier) and CYA data" which {

        val startingAccount = InterestAccountModel(Some("qwerty"), "TSB", 200.00, Some("otherValue"))
        val otherAccount = InterestAccountModel(Some("azerty"), "TSB", 200.00, None)
        val expectedAccount = InterestAccountModel(Some("qwerty"), "TSB Account", 500.00, Some("otherValue"))

        lazy val result = controller.submit(taxYear, Some("qwerty"))(fakeRequest.withFormUrlEncodedBody(
          "friendlyName" -> "TSB Account",
          "incomeTaxAmount" -> "500.00"
        ).withSession(
          SessionValues.INTEREST_CYA -> InterestCYAModel(
            false,
            None,
            true,
            Seq(startingAccount, otherAccount)
          ).asJsonString
        ))

        s"has a status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct redirect url" in {
          redirectUrl(result) shouldBe controllers.interest.routes.AccountsController.show(taxYear, TAXED).url
        }

        "has updated the correct account" in {
          val model = Json.parse(getSession(result).get(SessionValues.INTEREST_CYA).get).as[InterestCYAModel]
          model.taxedUkAccounts shouldBe Some(Seq(expectedAccount, otherAccount))
        }
      }

      "there is a correctly submitted form, with a modify value (that matches a session identifier) and CYA data" which {

        val startingAccount = InterestAccountModel(None, "TSB", 200.00, Some("qwerty"))
        val otherAccount = InterestAccountModel(Some("azerty"), "TSB", 200.00, None)
        val expectedAccount = InterestAccountModel(None, "TSB Account", 500.00, Some("qwerty"))

        lazy val result = controller.submit(taxYear, Some("qwerty"))(fakeRequest.withFormUrlEncodedBody(
          "friendlyName" -> "TSB Account",
          "incomeTaxAmount" -> "500.00"
        ).withSession(
          SessionValues.INTEREST_CYA -> InterestCYAModel(
            false,
            None,
            true,
            Seq(startingAccount, otherAccount)
          ).asJsonString
        ))

        s"has a status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct redirect url" in {
          redirectUrl(result) shouldBe controllers.interest.routes.AccountsController.show(taxYear, TAXED).url
        }

        "has updated the correct account" in {
          val model = Json.parse(getSession(result).get(SessionValues.INTEREST_CYA).get).as[InterestCYAModel]
          model.taxedUkAccounts shouldBe Some(Seq(expectedAccount, otherAccount))
        }
      }

      "there is a correctly submitted form, without a modify value and no interest account CYA data" which {
        lazy val result = controller.submit(taxYear, None)(fakeRequest.withFormUrlEncodedBody(
          "friendlyName" -> "TSB Account",
          "incomeTaxAmount" -> "500.00"
        ).withSession(
          SessionValues.INTEREST_CYA -> InterestCYAModel(
            false,
            None,
            true,
            None
          ).asJsonString
        ))

        s"has a status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct redirect url" in {
          redirectUrl(result) shouldBe controllers.interest.routes.AccountsController.show(taxYear, TAXED).url
        }

        "has updated the correct account" in {
          val model = Json.parse(getSession(result).get(SessionValues.INTEREST_CYA).get).as[InterestCYAModel]
          val account = model.taxedUkAccounts.get.head

          account.accountName shouldBe "TSB Account"
          account.amount shouldBe 500.00
        }
      }

    }

    "redirect to the overview page" when {

      "there is a correctly submitted form, but no CYA data" which {

        lazy val result = controller.submit(taxYear, None)(fakeRequest.withFormUrlEncodedBody(
          "friendlyName" -> "Some Name",
          "incomeTaxAmount" -> "100"
        ))

        s"has a redirect status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct redirect url" in {
          redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
        }

      }

    }

    s"return a BAD_REQUEST($BAD_REQUEST)" when {

      "the form data is invalid" in new TestWithAuth {
        val result: Future[Result] = controller.submit(taxYear, None)(fakeRequest.withFormUrlEncodedBody(
          "invalidField" -> "someValue"
        ))

        status(result) shouldBe BAD_REQUEST
      }

    }

  }

  ".backLink" should {

    "return the back link in session" when {

      "the cya model indicates the user has not finished the journey" in {
        val requestWithSessionValues: FakeRequest[AnyContentAsEmpty.type] = fakeRequest.withSession(
          SessionValues.PAGE_BACK_TAXED_AMOUNT -> "/back",
          SessionValues.INTEREST_CYA -> InterestCYAModel(Some(false), None, Some(true), None).asJsonString
        )

        val result = controller.backLink(taxYear)(requestWithSessionValues)
        result shouldBe Some("/back")
      }

      "the cya model does not exist" in {
        val requestWithSessionValues: FakeRequest[AnyContentAsEmpty.type] = fakeRequest.withSession(
          SessionValues.PAGE_BACK_TAXED_AMOUNT -> "/back"
        )

        val result = controller.backLink(taxYear)(requestWithSessionValues)
        result shouldBe Some("/back")
      }
    }

    "return the taxed accounts overview page url" when {

      "the cya model indicates the user has finished the journey" in {
        val requestWithSessionValues: FakeRequest[AnyContentAsEmpty.type] = fakeRequest.withSession(
          SessionValues.PAGE_BACK_TAXED_ACCOUNTS -> "/back",
          SessionValues.INTEREST_CYA -> InterestCYAModel(Some(false), None, Some(false), None).asJsonString
        )

        val result = controller.backLink(taxYear)(requestWithSessionValues)
        result shouldBe Some(controllers.interest.routes.AccountsController.show(taxYear, InterestTaxTypes.TAXED).url)
      }

    }

    "return the overview link" when {

      "there is no back link value in session and the journey is not complete" in {
        controller.backLink(taxYear)(fakeRequest) shouldBe Some(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }

    }

  }

}
