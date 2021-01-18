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
import forms.YesNoForm
import models.interest.{InterestAccountModel, InterestCYAModel}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Result
import utils.ViewTest
import views.html.interest.TaxedInterestView

import scala.concurrent.Future

class TaxedInterestControllerSpec extends ViewTest{

  implicit def wrapOption[T](input: T): Option[T] = Some(input)

  lazy val controller = new TaxedInterestController(
    mockMessagesControllerComponents,
    authorisedAction,
    app.injector.instanceOf[TaxedInterestView]
  )(mockAppConfig)

  val taxYear = 2020

  ".show for an individual" should {

    "return a result" which {

      s"has an OK($OK) status" in new TestWithAuth {
        val result: Future[Result] = controller.show(taxYear)(fakeRequest.withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes))

        status(result) shouldBe OK
        getSession(result).get(SessionValues.PAGE_BACK_TAXED_AMOUNT).get should include(
          controllers.interest.routes.TaxedInterestController.show(taxYear).url
        )
      }
    }
  }

  ".show for an agent" should {

    "return a result" which {

      s"has an OK($OK) status" in new TestWithAuth(isAgent = true) {
        val result: Future[Result] = controller.show(taxYear)(fakeRequestWithMtditid.withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes))

        status(result) shouldBe OK
        getSession(result).get(SessionValues.PAGE_BACK_TAXED_AMOUNT).get should include(
          controllers.interest.routes.TaxedInterestController.show(taxYear).url
        )
      }
    }
  }

  ".submit as an individual" should {

    def getCyaModel(result: Future[Result]): InterestCYAModel = {
      Json.parse(await(result).session.get(SessionValues.INTEREST_CYA).get).as[InterestCYAModel]
    }

    "redirect to the taxed interest amount page" when {

      "yes is selected" which {

        lazy val result = controller.submit(taxYear)(fakeRequest
          .withFormUrlEncodedBody(
            YesNoForm.yesNo -> YesNoForm.yes
          )
          .withSession(
            SessionValues.INTEREST_CYA -> InterestCYAModel(
              false,
              None,
              false,
              None
            ).asJsonString
          ))

        s"has a status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct redirect URL" in {
          redirectUrl(result) shouldBe controllers.interest.routes.TaxedInterestAmountController.show(taxYear, None).url
        }

        "has updated the CYA model" in {
          getCyaModel(result).taxedUkInterest shouldBe Some(true)
        }

      }

    }

    "redirect to the CYA page" when {

      "no is selected" which {

        lazy val result = controller.submit(taxYear)(fakeRequest
          .withFormUrlEncodedBody(
            YesNoForm.yesNo -> YesNoForm.no
          )
          .withSession(
            SessionValues.INTEREST_CYA -> InterestCYAModel(
              false, None,
              true, Seq(InterestAccountModel(None, "asdf", 100.00, None))
            ).asJsonString
          ))

        s"has a status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct redirect url" in {
          redirectUrl(result) shouldBe controllers.interest.routes.InterestCYAController.show(taxYear).url
        }

        lazy val model = getCyaModel(result)

        "has updated taxed interest to false" in {
          model.taxedUkInterest shouldBe Some(false)
        }

        "removed any taxed accounts from session" in {
          model.taxedUkAccounts shouldBe None
        }

      }

    }

    "redirect to the overview page" when {

      "there is no CYA data" which {

        lazy val result = controller.submit(taxYear)(fakeRequest.withFormUrlEncodedBody(
          YesNoForm.yesNo -> YesNoForm.yes
        ))

        s"has status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct redirect URL" in {
          redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
        }

      }

    }

    "return a bad request" when {

      "there is an issue with the form submission" in new TestWithAuth {
        lazy val result: Future[Result] = controller.submit(taxYear)(fakeRequest)

        status(result) shouldBe BAD_REQUEST
      }

    }

  }

  ".submit as an agent" should {

    def getCyaModel(result: Future[Result]): InterestCYAModel = {
      Json.parse(await(result).session.get(SessionValues.INTEREST_CYA).get).as[InterestCYAModel]
    }

    "redirect to the taxed interest amount page" when {

      "yes is selected" which {

        lazy val result = controller.submit(taxYear)(fakeRequestWithMtditid
          .withFormUrlEncodedBody(
            YesNoForm.yesNo -> YesNoForm.yes
          )
          .withSession(
            SessionValues.INTEREST_CYA -> InterestCYAModel(
              false,
              None,
              false,
              None
            ).asJsonString
          ))

        s"has a status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth(isAgent = true) {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct redirect URL" in {
          redirectUrl(result) shouldBe controllers.interest.routes.TaxedInterestAmountController.show(taxYear, None).url
        }

        "has updated the CYA model" in {
          getCyaModel(result).taxedUkInterest shouldBe Some(true)
        }

      }

    }

    "redirect to the CYA page" when {

      "no is selected" which {

        lazy val result = controller.submit(taxYear)(fakeRequestWithMtditid
          .withFormUrlEncodedBody(
            YesNoForm.yesNo -> YesNoForm.no
          )
          .withSession(
            SessionValues.INTEREST_CYA -> InterestCYAModel(
              false, None,
              true, Seq(InterestAccountModel(None, "asdf", 100.00, None))
            ).asJsonString
          ))

        s"has a status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth(isAgent = true) {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct redirect url" in {
          redirectUrl(result) shouldBe controllers.interest.routes.InterestCYAController.show(taxYear).url
        }

        lazy val model = getCyaModel(result)

        "has updated taxed interest to false" in {
          model.taxedUkInterest shouldBe Some(false)
        }

        "removed any taxed accounts from session" in {
          model.taxedUkAccounts shouldBe None
        }

      }

    }

    "redirect to the overview page" when {

      "there is no CYA data" which {

        lazy val result = controller.submit(taxYear)(fakeRequestWithMtditid.withFormUrlEncodedBody(
          YesNoForm.yesNo -> YesNoForm.yes
        ))

        s"has status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct redirect URL" in {
          redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
        }

      }

    }

    "return a bad request" when {

      "there is an issue with the form submission" in new TestWithAuth(isAgent = true) {
        lazy val result: Future[Result] = controller.submit(taxYear)(fakeRequestWithMtditid)

        status(result) shouldBe BAD_REQUEST
      }

    }

  }

  ".backLink" should {

    "return the CYA link" when {

      "the cya model indicates the journey is finished" in {

        controller.backLink(taxYear)(fakeRequest.withSession(
          SessionValues.INTEREST_CYA -> InterestCYAModel(Some(false), None, Some(false), None).asJsonString
        )) shouldBe Some(controllers.interest.routes.InterestCYAController.show(taxYear).url)

      }

    }

    "return the untaxed accounts overview link" when {

      "the cya model indicates the journey is not finished" in {

        controller.backLink(taxYear)(fakeRequest.withSession(
          SessionValues.INTEREST_CYA -> InterestCYAModel(Some(false), None, None, None).asJsonString
        )) shouldBe Some(controllers.interest.routes.AccountsController.show(taxYear, InterestTaxTypes.UNTAXED).url)

      }

    }

    "return the overview url" when {

      "the cya model is missing" in {

        controller.backLink(taxYear)(fakeRequest) shouldBe Some(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear))

      }

    }

  }

}
