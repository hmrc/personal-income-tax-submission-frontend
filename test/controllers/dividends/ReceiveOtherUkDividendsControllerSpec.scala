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

package controllers.dividends

import common.SessionValues
import config.AppConfig
import controllers.predicates.{AuthorisedAction, QuestionsJourneyValidator}
import forms.YesNoForm
import play.api.http.HeaderNames
import models.dividends.{DividendsCheckYourAnswersModel, DividendsPriorSubmission}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers
import play.api.test.Helpers.defaultAwaitTimeout
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.UnitTestWithApp
import views.html.dividends.ReceiveOtherUkDividendsView

import scala.concurrent.Future

class ReceiveOtherUkDividendsControllerSpec extends UnitTestWithApp {

  lazy val controller = new ReceiveOtherUkDividendsController()(
    mockMessagesControllerComponents,
    authorisedAction,
    app.injector.instanceOf[ReceiveOtherUkDividendsView],
    app.injector.instanceOf[QuestionsJourneyValidator],
    mockDividendsSessionService,
    mockErrorHandler,
    mockAppConfig,
    mockExecutionContext
  )

  val taxYear: Int = mockAppConfig.defaultTaxYear

  ".show" should {

    "return a result" which {

      s"has an OK($OK) status" in new TestWithAuth {
        val result: Future[Result] = controller.show(taxYear)(fakeRequest
          .withSession(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(None, None, Some(true), None).asJsonString)
        )

        status(result) shouldBe OK
      }

    }

    "return a result when there is a dividends CYA model in session" which {

      s"has an OK($OK) status" in new TestWithAuth {
        val result: Future[Result] = controller.show(taxYear)(fakeRequest
          .withSession(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(None, None, Some(true), Some(67.00)).asJsonString)
        )

        status(result) shouldBe OK
      }

    }

    "redirect the user to CYA" when {

      "UK Dividends in the prior submission contains a value" which {
        lazy val result: Future[Result] = controller.show(taxYear)(fakeRequest.withSession(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.DIVIDENDS_PRIOR_SUB -> DividendsPriorSubmission(None, Some(100.00)).asJsonString
        ))

        s"has the SEE_OTHER($SEE_OTHER) status" in new TestWithAuth {
          status(result) shouldBe SEE_OTHER
        }

        "the redirect URL is the CYA page" in {
          redirectUrl(result) shouldBe controllers.dividends.routes.DividendsCYAController.show(taxYear).url
        }
      }

    }

    "Redirect to the overview page" when {

      "there is no prior submission and no cya data in session" in new TestWithAuth {
        lazy val result: Future[Result] = {
          controller.show(taxYear)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
        }

        status(result) shouldBe SEE_OTHER
        Helpers.header(HeaderNames.LOCATION, result) shouldBe Some(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }

      "there is a prior submission in session, but otherDividends is set to None" in new TestWithAuth {
        lazy val result: Future[Result] = {
          controller.show(taxYear)(fakeRequest.withSession(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.DIVIDENDS_PRIOR_SUB -> Json.toJson(DividendsPriorSubmission(None, None)).toString()
          ))
        }

        status(result) shouldBe SEE_OTHER
        Helpers.header(HeaderNames.LOCATION, result) shouldBe Some(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear))
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

        lazy val featureSwitchController = new ReceiveOtherUkDividendsController()(
          mockMessagesControllerComponents,
          authorisedActionFeatureSwitch,
          app.injector.instanceOf[ReceiveOtherUkDividendsView],
          app.injector.instanceOf[QuestionsJourneyValidator],
          mockDividendsSessionService,
          mockErrorHandler,
          mockAppConfFeatureSwitch,
          mockExecutionContext
        )

        val invalidTaxYear = 2023
        lazy val result: Future[Result] = featureSwitchController.show(invalidTaxYear)(
          fakeRequest.withSession(SessionValues.TAX_YEAR -> mockAppConfFeatureSwitch.defaultTaxYear.toString)
        )

        redirectUrl(result) shouldBe controllers.routes.TaxYearErrorController.show.url

      }
    }

  }

  ".submit" should {

    "redirect to the other dividends amounts page" when {

      "the submitted answer is yes" when {

        "the session data is empty" in new TestWithAuth {

          val result: Future[Result] = controller.submit(taxYear)(fakeRequest.withFormUrlEncodedBody(
            YesNoForm.yesNo -> YesNoForm.yes
          ))

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe controllers.dividends.routes.OtherUkDividendsAmountController.show(taxYear).url

        }

        "the session data exist" in new TestWithAuth {

          val result: Future[Result] = controller.submit(taxYear)(fakeRequest
            .withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes)
            .withSession(SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel().asJsonString)
          )

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe controllers.dividends.routes.OtherUkDividendsAmountController.show(taxYear).url

        }

      }

    }

    "redirect to the cya page" when {

      "the submitted answer is yes" when {

        "the session data is empty" in new TestWithAuth {

          val result: Future[Result] = controller.submit(taxYear)(fakeRequest.withFormUrlEncodedBody(
            YesNoForm.yesNo -> YesNoForm.no
          ))

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe controllers.dividends.routes.DividendsCYAController.show(taxYear).url

        }

        "the session data exist" in new TestWithAuth {

          val result: Future[Result] = controller.submit(taxYear)(fakeRequest
            .withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.no)
            .withSession(SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel().asJsonString)
          )

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe controllers.dividends.routes.DividendsCYAController.show(taxYear).url

        }

      }

    }

    "add the cya data to session" when {

      "cya data already exist" in new TestWithAuth {
        val expectedModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(
          ukDividends = Some(true),
          otherUkDividends = Some(true)
        )

        val result: Future[Result] = controller.submit(taxYear)(fakeRequest
          .withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes)
          .withSession(SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true)).asJsonString)
        )

        Json.parse(await(result).session.get(SessionValues.DIVIDENDS_CYA).get).as[DividendsCheckYourAnswersModel] shouldBe expectedModel
      }

      "cya data does not exist" in new TestWithAuth {
        val expectedModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(
          otherUkDividends = Some(true)
        )

        val result: Future[Result] = controller.submit(taxYear)(fakeRequest
          .withFormUrlEncodedBody(YesNoForm.yesNo -> YesNoForm.yes)
        )

        Json.parse(await(result).session.get(SessionValues.DIVIDENDS_CYA).get).as[DividendsCheckYourAnswersModel] shouldBe expectedModel
      }

    }

    "throw a bad request" when {

      "the form is not filled" in new TestWithAuth {
        val result: Future[Result] = controller.submit(taxYear)(fakeRequest)

        status(result) shouldBe BAD_REQUEST
      }
    }

  }

}
