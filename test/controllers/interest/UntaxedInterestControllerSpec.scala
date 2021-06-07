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
import controllers.predicates.{AuthorisedAction, QuestionsJourneyValidator}
import forms.YesNoForm
import models.interest.{InterestAccountModel, InterestCYAModel}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.UnitTestWithApp
import views.html.interest.UntaxedInterestView

import scala.concurrent.Future

class UntaxedInterestControllerSpec extends UnitTestWithApp {

  implicit def wrapOption[T](input: T): Option[T] = Some(input)

  val view: UntaxedInterestView = app.injector.instanceOf[UntaxedInterestView]

  lazy val controller = new UntaxedInterestController(
    view
  )(mockAppConfig, authorisedAction, app.injector.instanceOf[QuestionsJourneyValidator], mockInterestSessionService, mockErrorHandler, mockMessagesControllerComponents)

  val taxYear: Int = mockAppConfig.defaultTaxYear
  val id = "9563b361-6333-449f-8721-eab2572b3437"

  ".show for an individual" should {

    "return a result" which {

      s"has an OK($OK) status" in new TestWithAuth {
        val result: Future[Result] = controller.show(taxYear)(fakeRequest
          .withSession(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(
              InterestCYAModel(Some(false), None, Some(false), None))
            )
          )
        )

        status(result) shouldBe OK
      }
    }

    "redirect the user to CYA" when {

      "hasUntaxed in the prior submission is set to true" which {
        lazy val result: Future[Result] = controller.show(taxYear)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.INTEREST_PRIOR_SUB -> Json.arr(Json.obj(
            "accountName" -> "Account",
            "incomeSourceId" -> "anId",
            "untaxedUkInterest" -> 100.00
          )).toString()
        ))

        s"has the SEE_OTHER($SEE_OTHER) status" in new TestWithAuth {
          status(result) shouldBe SEE_OTHER
        }

        "the redirect URL is the CYA page" in {
          redirectUrl(result) shouldBe controllers.interest.routes.InterestCYAController.show(taxYear).url
        }
      }

    }

    "Redirect to the tax year error " when {

      "an invalid tax year has been added to the url" in new TestWithAuth() {

        val mockAppConfFeatureSwitch: AppConfig = new AppConfig(mock[ServicesConfig]) {
          override lazy val defaultTaxYear: Int = 2022
          override lazy val taxYearErrorFeature = true
        }

        val authorisedActionFeatureSwitch = new AuthorisedAction(mockAppConfFeatureSwitch,
          agentAuthErrorPageView)(mockAuthService, stubMessagesControllerComponents())

        lazy val featureSwitchController = new UntaxedInterestController(
          view
        )(mockAppConfFeatureSwitch, authorisedActionFeatureSwitch, app.injector.instanceOf[QuestionsJourneyValidator], mockInterestSessionService, mockErrorHandler, mockMessagesControllerComponents)

        val invalidTaxYear = 2023
        lazy val result: Future[Result] = featureSwitchController.show(invalidTaxYear)(
          fakeRequest.withSession(SessionValues.TAX_YEAR -> mockAppConfFeatureSwitch.defaultTaxYear.toString)
        )

        redirectUrl(result) shouldBe controllers.routes.TaxYearErrorController.show.url

      }
    }
  }

  ".show for an agent" should {

    "return a result" which {

      s"has an OK($OK) status" in new TestWithAuth(isAgent = true) {
        val result: Future[Result] = controller.show(taxYear)(fakeRequestWithMtditidAndNino.withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes))

        status(result) shouldBe OK
      }
    }
  }

  ".submit as an individual" should {

    def getCyaModel(result: Future[Result]): InterestCYAModel = {
      Json.parse(await(result).session.get(SessionValues.INTEREST_CYA).get).as[InterestCYAModel]
    }

    "redirect to the untaxed interest amount page" when {

      "yes is selected and the model indicates the journey is not finished" which {

        lazy val result = controller.submit(taxYear)(fakeRequest
          .withFormUrlEncodedBody(
            YesNoForm.yesNo -> YesNoForm.yes
          )
          .withSession(
            SessionValues.INTEREST_CYA -> InterestCYAModel(
              false,
              None,
              None,
              None
            ).asJsonString
          ))

        s"has a status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct redirect URL" in {
          redirectUrl(result) should include(controllers.interest.routes.UntaxedInterestAmountController.show(taxYear, id).url.dropRight(36))
        }

        "has updated the CYA model" in {
          getCyaModel(result).untaxedUkInterest shouldBe Some(true)
        }

      }

    }

    "redirect to the CYA page" when {

      "no is selected and the model indicates the journey is complete" which {

        lazy val result = controller.submit(taxYear)(fakeRequest
          .withFormUrlEncodedBody(
            YesNoForm.yesNo -> YesNoForm.no
          )
          .withSession(
            SessionValues.INTEREST_CYA -> InterestCYAModel(
              true, Seq(InterestAccountModel(None, "asdf", 100.00, None)),
              false, None
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
          model.untaxedUkInterest shouldBe Some(false)
        }

        "removed any taxed accounts from session" in {
          model.untaxedUkAccounts shouldBe None
        }

      }

    }

    "redirect to the taxed interest page" when {

      "no is selected and the model indicates the journey is not complete" which {

        lazy val result = controller.submit(taxYear)(fakeRequest.withFormUrlEncodedBody(
          YesNoForm.yesNo -> YesNoForm.no
        ))

        s"has status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct redirect URL" in {
          redirectUrl(result) shouldBe controllers.interest.routes.TaxedInterestController.show(taxYear).url
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

    "redirect to the untaxed interest amount page" when {

      "yes is selected and the model indicates the journey is not finished" which {

        lazy val result = controller.submit(taxYear)(fakeRequestWithMtditidAndNino
          .withFormUrlEncodedBody(
            YesNoForm.yesNo -> YesNoForm.yes
          )
          .withSession(
            SessionValues.INTEREST_CYA -> InterestCYAModel(
              false,
              None,
              None,
              None
            ).asJsonString
          ))

        s"has a status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth(isAgent = true) {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct redirect URL" in {
          redirectUrl(result) should include(controllers.interest.routes.UntaxedInterestAmountController.show(taxYear, id).url.dropRight(36))
        }

        "has updated the CYA model" in {
          getCyaModel(result).untaxedUkInterest shouldBe Some(true)
        }

      }

    }

    "redirect to the CYA page" when {

      "no is selected and the model indicates the journey is complete" which {

        lazy val result = controller.submit(taxYear)(fakeRequestWithMtditidAndNino
          .withFormUrlEncodedBody(
            YesNoForm.yesNo -> YesNoForm.no
          )
          .withSession(
            SessionValues.INTEREST_CYA -> InterestCYAModel(
              true, Seq(InterestAccountModel(None, "asdf", 100.00, None)),
              false, None
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
          model.untaxedUkInterest shouldBe Some(false)
        }

        "removed any taxed accounts from session" in {
          model.untaxedUkAccounts shouldBe None
        }

      }

    }

    "redirect to the taxed interest page" when {

      "no is selected and the model indicates the journey is not complete" which {

        lazy val result = controller.submit(taxYear)(fakeRequestWithMtditidAndNino.withFormUrlEncodedBody(
          YesNoForm.yesNo -> YesNoForm.no
        ))

        s"has status of SEE_OTHER($SEE_OTHER)" in new TestWithAuth(isAgent = true) {
          status(result) shouldBe SEE_OTHER
        }

        "has the correct redirect URL" in {
          redirectUrl(result) shouldBe controllers.interest.routes.TaxedInterestController.show(taxYear).url
        }

      }

    }

    "return a bad request" when {

      "there is an issue with the form submission" in new TestWithAuth(isAgent = true) {
        lazy val result: Future[Result] = controller.submit(taxYear)(fakeRequestWithMtditidAndNino)

        status(result) shouldBe BAD_REQUEST
      }

    }

  }

}
