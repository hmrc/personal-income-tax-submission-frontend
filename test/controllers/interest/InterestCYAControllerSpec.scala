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
import models.httpResponses.ErrorResponse
import models.interest.{InterestAccountModel, InterestCYAModel}
import org.scalatest.GivenWhenThen
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.{AnyContentAsEmpty, Result, Session}
import play.api.test.FakeRequest
import services.InterestSubmissionService
import uk.gov.hmrc.http.HeaderCarrier
import utils.ViewTest
import views.html.interest.InterestCYAView

import scala.concurrent.{ExecutionContext, Future}

class InterestCYAControllerSpec extends ViewTest with GivenWhenThen {

  lazy val view: InterestCYAView = app.injector.instanceOf[InterestCYAView]
  lazy val submissionService: InterestSubmissionService = mock[InterestSubmissionService]
  lazy val controller: InterestCYAController = new InterestCYAController(
    mockMessagesControllerComponents,
    authorisedAction,
    view,
    submissionService
  )(mockAppConfig, mockExecutionContext)

  val taxYear: Int = 2020
  val arbitraryAmount: Int = 100

  ".show" should {

    s"return an OK($OK)" when {

      "there is CYA data in session" in new TestWithAuth {
        lazy val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
          SessionValues.INTEREST_CYA -> InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(None, "", arbitraryAmount))),
            Some(true), Some(Seq(InterestAccountModel(None, "", arbitraryAmount)))
          ).asJsonString
        )

        lazy val result: Future[Result] = controller.show(taxYear)(request)

        status(result) shouldBe OK
      }

      "there is no CYA data but is prior submission data in session" in new TestWithAuth {
        lazy val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
          SessionValues.INTEREST_PRIOR_SUB -> Json.arr(
            Json.obj(
              "accountName" -> "Bank of Winterhold",
              "incomeSourceId" -> "qwerty",
              "untaxedUkInterest" -> 500.00
            ),
            Json.obj(
              "accountName" -> "Bank of Riften",
              "incomeSourceId" -> "azerty",
              "taxedUkInterest" -> 200.00
            )
          ).toString()
        )

        lazy val result: Future[Result] = controller.show(taxYear)(request)

        status(result) shouldBe OK

        val expectedBackLink: Some[String] = Some(controllers.interest.routes.InterestCYAController.show(taxYear).url)

        getSession(result).get(SessionValues.PAGE_BACK_TAXED_ACCOUNTS) shouldBe expectedBackLink
        getSession(result).get(SessionValues.PAGE_BACK_UNTAXED_ACCOUNTS) shouldBe expectedBackLink
      }

    }

    s"redirect to the overview page" when {

      "there is no CYA data in session" in new TestWithAuth {

        lazy val result: Future[Result] = controller.show(taxYear)(FakeRequest())

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
      }

    }

  }

  ".submit" should {

    "redirect to the overview page" when {

      "cya data is missing from session" in new TestWithAuth {

        lazy val result: Future[Result] = controller.submit(taxYear)(fakeRequest.withSession(
          SessionValues.CLIENT_NINO -> "AA123456A"
        ))

        Then(s"has a status of SEE_OTHER ($SEE_OTHER)")
        status(result) shouldBe SEE_OTHER

        And("the correct redirect URL")
        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)

      }

      "the NINO is missing from session" in new TestWithAuth {

        lazy val result: Future[Result] = controller.submit(taxYear)(fakeRequest.withSession(
          SessionValues.INTEREST_CYA -> InterestCYAModel(
            Some(true),
            Some(Seq(InterestAccountModel(None, "Muh Bank Bruh", 1000.00, None, None))),
            Some(false), None
          ).asJsonString
        ))

        Then(s"has a return status of SEE_OTHER ($SEE_OTHER)")
        status(result) shouldBe SEE_OTHER

        And("the correct redirect URL")
        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
      }

      "both the NINO and CYA Data are present" when {

        "the submission is successful" in new TestWithAuth {

          val cyaModel: InterestCYAModel = InterestCYAModel(
            Some(true),
            Some(Seq(InterestAccountModel(None, "Dis bank m8", 250000.00, None))),
            Some(false), None
          )

          lazy val result: Future[Result] = {
            (submissionService.submit(_: InterestCYAModel, _: String, _: Int, _: String)(_: HeaderCarrier, _: ExecutionContext))
              .expects(cyaModel, "AA123456A", taxYear, "1234567890", *, *)
              .returning(Future.successful(Right(NO_CONTENT)))

            controller.submit(taxYear)(fakeRequestWithMtditid.withSession(
              SessionValues.CLIENT_NINO -> "AA123456A",
              SessionValues.INTEREST_CYA -> cyaModel.asJsonString,
              SessionValues.PAGE_BACK_TAXED_ACCOUNTS -> "/taxedAccounts",
              SessionValues.PAGE_BACK_UNTAXED_ACCOUNTS -> "/untaxedAccounts",
              SessionValues.PAGE_BACK_TAXED_AMOUNT -> "/taxedAmount",
              SessionValues.PAGE_BACK_UNTAXED_AMOUNT -> "/taxedAmount",
              SessionValues.PAGE_BACK_CYA -> "/cya"
            ))
          }

          Then(s"should return a SEE_OTHER ($SEE_OTHER) status")
          status(result) shouldBe SEE_OTHER

          And("has the correct redirect url")
          redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)

          And("cleared all interest url redirects")
          val resultSession: Session = getSession(result)
          resultSession.get(SessionValues.PAGE_BACK_TAXED_ACCOUNTS) shouldBe None
          resultSession.get(SessionValues.PAGE_BACK_UNTAXED_ACCOUNTS) shouldBe None
          resultSession.get(SessionValues.PAGE_BACK_TAXED_AMOUNT) shouldBe None
          resultSession.get(SessionValues.PAGE_BACK_UNTAXED_AMOUNT) shouldBe None
          resultSession.get(SessionValues.PAGE_BACK_CYA) shouldBe None
        }

        "the submission is unsuccessful" in new TestWithAuth {

          val cyaModel: InterestCYAModel = InterestCYAModel(
            Some(true),
            Some(Seq(InterestAccountModel(None, "Dis bank m8", 250000.00, None))),
            Some(false), None
          )

          lazy val result: Future[Result] = {
            (submissionService.submit(_: InterestCYAModel, _: String, _: Int, _: String)(_: HeaderCarrier, _: ExecutionContext))
              .expects(cyaModel, "AA123456A", taxYear, "1234567890", *, *)
              .returning(Future.successful(Left(ErrorResponse(BAD_REQUEST, "uh oh"))))

            controller.submit(taxYear)(fakeRequestWithMtditid.withSession(
              SessionValues.CLIENT_NINO -> "AA123456A",
              SessionValues.INTEREST_CYA -> cyaModel.asJsonString
            ))
          }

          Then(s"should return a SEE_OTHER ($SEE_OTHER) status")
          status(result) shouldBe SEE_OTHER

          And("has the correct redirect url")
          redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
        }

      }

    }

  }

  ".backLink" should {

    "return whatever the cya back link" when {

      "it is in session" in {
        val requestWithSessionValues: FakeRequest[AnyContentAsEmpty.type] = fakeRequest.withSession(
          SessionValues.PAGE_BACK_CYA -> "/cyaRedirectLink"
        )

        val result = controller.backLink(taxYear)(requestWithSessionValues)
        result shouldBe Some("/cyaRedirectLink")
      }

    }

    "return the overview link" when {

      "there are no backlink values in session" in {
        controller.backLink(taxYear)(fakeRequest) shouldBe Some(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }

    }

  }

}
