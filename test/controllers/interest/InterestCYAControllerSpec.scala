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

import audit.{AuditModel, CreateOrAmendInterestAuditDetail}
import common.{InterestTaxTypes, SessionValues}
import config.{AppConfig, ErrorHandler, MockAuditService}
import controllers.predicates.AuthorisedAction
import models.interest.{InterestAccountModel, InterestCYAModel, InterestPriorSubmission}
import models.{APIErrorBodyModel, APIErrorModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalamock.handlers.CallHandler
import org.scalatest.GivenWhenThen
import play.api.http.Status._
import play.api.libs.json.{JsArray, Json}
import play.api.mvc.Results._
import play.api.mvc.{AnyContentAsEmpty, Request, Result}
import play.api.test.FakeRequest
import services.InterestSubmissionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.UnitTestWithApp
import views.html.interest.InterestCYAView
import views.html.templates.{InternalServerErrorTemplate, ServiceUnavailableTemplate}

import scala.concurrent.{ExecutionContext, Future}

class InterestCYAControllerSpec extends UnitTestWithApp with GivenWhenThen with MockAuditService{

  lazy val view: InterestCYAView = app.injector.instanceOf[InterestCYAView]
  lazy val submissionService: InterestSubmissionService = mock[InterestSubmissionService]
  val errorHandler: ErrorHandler = mock[ErrorHandler]
  val serviceUnavailableTemplate: ServiceUnavailableTemplate = app.injector.instanceOf[ServiceUnavailableTemplate]
  val unauthorisedTemplate: InternalServerErrorTemplate = app.injector.instanceOf[InternalServerErrorTemplate]

  lazy val controller: InterestCYAController = new InterestCYAController(
    view,
    submissionService,
    mockAuditService,
    errorHandler
  )(mockAppConfig, authorisedAction, mockMessagesControllerComponents)

  val taxYear: Int = 2022
  val arbitraryAmount: Int = 100
  val agentAffinityGroup: String = "Agent"
  val individualAffinityGroup: String = "Individual"

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
      }

    }

    s"redirect to the overview page" when {

      "there is no CYA data in session" in new TestWithAuth {

        lazy val result: Future[Result] = controller.show(taxYear)(FakeRequest())

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
      }

    }

    "redirect the user to the most relevant page if journey has not been completed" when {

      "upto Receive UK Untaxed Interest is filled in" when {

        "the answer is yes" which {
          lazy val result = controller.show(taxYear)(fakeRequest.withSession(
            SessionValues.INTEREST_CYA -> InterestCYAModel(
              Some(true), None, None, None
            ).asJsonString
          ))

          s"has the SEE_OTHER($SEE_OTHER) status" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "redirects to the \"Untaxed UK Aamount\" page" in {
            redirectUrl(result) should include(controllers.interest.routes.UntaxedInterestAmountController.show(taxYear, "").url)
          }

        }

        "the answer is no" which {
          lazy val result = controller.show(taxYear)(fakeRequest.withSession(
            SessionValues.INTEREST_CYA -> InterestCYAModel(
              Some(false), None, None, None
            ).asJsonString
          ))

          s"has the SEE_OTHER($SEE_OTHER) status" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "redirects to the \"Receive Taxed UK Interest\" page" in {
            redirectUrl(result) shouldBe controllers.interest.routes.TaxedInterestController.show(taxYear).url
          }

        }

      }

      "upto UK Untaxed Accounts is filled in" which {
        lazy val result = controller.show(taxYear)(fakeRequest.withSession(
          SessionValues.INTEREST_CYA -> InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(
              None, "Some accounts", 100.00
            ))), None, None
          ).asJsonString
        ))

        s"has the SEE_OTHER($SEE_OTHER) status" in new TestWithAuth {
          status(result) shouldBe SEE_OTHER
        }

        "redirects to the \"Receive Taxed UK Interest\" page" in {
          redirectUrl(result) shouldBe controllers.interest.routes.TaxedInterestController.show(taxYear).url
        }

      }

      "upto Receive UK Taxed Interest is filled in with a \"Yes\"" which {
        lazy val result = controller.show(taxYear)(fakeRequest.withSession(
          SessionValues.INTEREST_CYA -> InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(
              None, "Some accounts", 100.00
            ))), Some(true), None
          ).asJsonString
        ))

        s"has the SEE_OTHER($SEE_OTHER) status" in new TestWithAuth {
          status(result) shouldBe SEE_OTHER
        }

        "redirects to the \"Taxed UK Accounts\" page" in {
          redirectUrl(result) should include(controllers.interest.routes.TaxedInterestAmountController.show(taxYear, "").url)
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

        lazy val featureSwitchController: InterestCYAController = new InterestCYAController(
          view,
          submissionService,
          mockAuditService,
          errorHandler
        )(mockAppConfFeatureSwitch, authorisedActionFeatureSwitch, mockMessagesControllerComponents)

        val invalidTaxYear = 2023
        lazy val result: Future[Result] = featureSwitchController.show(invalidTaxYear)(fakeRequest)

        redirectUrl(result) shouldBe controllers.routes.TaxYearErrorController.show().url

      }
    }

  }

  ".submit" should {

    "return an InternalServerError template page" when {

      "cya data is missing from session" in new TestWithAuth {

        (errorHandler.handleError(_: Int)(_: Request[_]))
          .expects(400, *)
          .returning(InternalServerError(unauthorisedTemplate()))

        lazy val result: Future[Result] = controller.submit(taxYear)(fakeRequest.withSession(
          SessionValues.CLIENT_NINO -> "AA123456A"
        ))

        val document: Document = Jsoup.parse(bodyOf(result))
        document.select("h1").first().text() shouldBe "Sorry, there is a problem with the service"

      }

      "both the NINO and CYA Data are present" when {

        "the submission is successful" in new TestWithAuth {

          lazy val detail: CreateOrAmendInterestAuditDetail = CreateOrAmendInterestAuditDetail(Some(cyaModel), None, "AA123456A", "1234567890", individualAffinityGroup.toLowerCase, taxYear)

          lazy val event: AuditModel[CreateOrAmendInterestAuditDetail] = AuditModel("CreateOrAmendInterestUpdate", "createOrAmendInterestUpdate", detail)

          def verifyInterestAudit: CallHandler[Future[AuditResult]] = verifyAuditEvent[CreateOrAmendInterestAuditDetail](event)

          val cyaModel: InterestCYAModel = InterestCYAModel(
            Some(true),
            Some(Vector(InterestAccountModel(None, "Dis bank m8", 250000.69, None))),
            Some(false), None
          )

          (submissionService.submit(_: InterestCYAModel, _: String, _: Int, _: String)(_: HeaderCarrier, _: ExecutionContext))
            .expects(cyaModel, "AA123456A", taxYear, "1234567890", *, *)
            .returning(Future.successful(Right(NO_CONTENT)))

          verifyInterestAudit

          lazy val result: Future[Result] = {
            controller.submit(taxYear)(fakeRequestWithMtditidAndNino.withSession(
                SessionValues.CLIENT_NINO -> "AA123456A",
                SessionValues.INTEREST_CYA -> cyaModel.asJsonString
            ))
          }

          Then(s"should return a SEE_OTHER ($SEE_OTHER) status")
          status(result) shouldBe SEE_OTHER

          And("has the correct redirect url")
          redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
        }

        "the submission is successful when there is a prior submission" in new TestWithAuth(isAgent = true) {

          lazy val priorDataModel: JsArray = Json.arr(
            Json.obj(
              "accountName" -> "Untaxed Account",
              "incomeSourceId" -> "UntaxedId1",
              "untaxedUkInterest" -> 100.01
            ),
            Json.obj(
              "accountName" -> "Taxed Account",
              "incomeSourceId" -> "TaxedId1",
              "taxedUkInterest" -> 9001.01
            )
          )

          val previousSubmission: InterestPriorSubmission = InterestPriorSubmission(
            hasUntaxed = true,
            hasTaxed = true,
            Some(Seq(
              InterestAccountModel(
                Some("UntaxedId1"),
                "Untaxed Account",
                100.01,
                priorType = Some(InterestTaxTypes.UNTAXED)
              ),
              InterestAccountModel(
                Some("TaxedId1"),
                "Taxed Account",
                9001.01,
                priorType = Some(InterestTaxTypes.TAXED)
              )
            ))
          )
          lazy val detail: CreateOrAmendInterestAuditDetail = CreateOrAmendInterestAuditDetail(Some(cyaModel),
            Some(previousSubmission), "AA123456A", "1234567890", agentAffinityGroup.toLowerCase(), taxYear)

          lazy val event: AuditModel[CreateOrAmendInterestAuditDetail] = AuditModel("CreateOrAmendInterestUpdate", "createOrAmendInterestUpdate", detail)

          def verifyInterestAudit: CallHandler[Future[AuditResult]] = verifyAuditEvent[CreateOrAmendInterestAuditDetail](event)

          val cyaModel: InterestCYAModel = InterestCYAModel(
            Some(true),
            Some(Vector(InterestAccountModel(None, "Dis bank m8", 250000.69, None))),
            Some(false), None
          )

          (submissionService.submit(_: InterestCYAModel, _: String, _: Int, _: String)(_: HeaderCarrier, _: ExecutionContext))
            .expects(cyaModel, "AA123456A", taxYear, "1234567890", *, *)
            .returning(Future.successful(Right(NO_CONTENT)))

          verifyInterestAudit

          lazy val result: Future[Result] = {
            controller.submit(taxYear)(fakeRequestWithMtditidAndNino.withSession(
              SessionValues.CLIENT_NINO -> "AA123456A",
              SessionValues.INTEREST_CYA -> cyaModel.asJsonString,
              SessionValues.INTEREST_PRIOR_SUB -> priorDataModel.toString
            ))
          }

          Then(s"should return a SEE_OTHER ($SEE_OTHER) status")
          status(result) shouldBe SEE_OTHER

          And("has the correct redirect url")
          redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
        }

        "there is an error posting downsteam" should {

          "redirect to the 500 error template page" in new TestWithAuth {

            val cyaModel: InterestCYAModel = InterestCYAModel(
              Some(true),
              Some(Seq(InterestAccountModel(None, "Santander", 250000.00, None))),
              Some(false),
              None
            )

            val errorResponseFromDes: Either[APIErrorModel, Int] =
              Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("error", "error")))

            lazy val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
              SessionValues.CLIENT_MTDITID -> Json.toJson("someMtdItid").toString(),
              SessionValues.INTEREST_CYA -> Json.toJson(cyaModel).toString(),
              SessionValues.CLIENT_NINO -> "AA123456A",
            )

            (submissionService.submit(_: InterestCYAModel, _: String, _: Int, _: String)(_: HeaderCarrier, _: ExecutionContext))
              .expects(cyaModel, "AA123456A", taxYear, "1234567890", *, *)
              .returning(Future.successful(errorResponseFromDes))

            (errorHandler.handleError(_: Int)(_: Request[_]))
              .expects(500, *)
              .returning(InternalServerError(unauthorisedTemplate()))

            val result: Future[Result] = controller.submit(taxYear)(request)

            val document: Document = Jsoup.parse(bodyOf(result))
            document.select("h1").first().text() shouldBe "Sorry, there is a problem with the service"
          }

          "redirect to the 503 error template page when the service is unavailable" in new TestWithAuth {
            val cyaModel: InterestCYAModel = InterestCYAModel(
              Some(true),
              Some(Seq(InterestAccountModel(None, "Santander", 250000.00, None))),
              Some(false),
              None
            )

            val errorResponseFromDes: Either[APIErrorModel, Int] =
              Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("error", "error")))

            lazy val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
              SessionValues.CLIENT_MTDITID -> Json.toJson("someMtdItid").toString(),
              SessionValues.INTEREST_CYA -> Json.toJson(cyaModel).toString(),
              SessionValues.CLIENT_NINO -> "AA123456A",
            )

            (submissionService.submit(_: InterestCYAModel, _: String, _: Int, _: String)(_: HeaderCarrier, _: ExecutionContext))
              .expects(cyaModel, "AA123456A", taxYear, "1234567890", *, *)
              .returning(Future.successful(errorResponseFromDes))

            (errorHandler.handleError(_: Int)(_: Request[_]))
              .expects(503, *)
              .returning(ServiceUnavailable(serviceUnavailableTemplate()))

            val result: Future[Result] = controller.submit(taxYear)(request)

            val document: Document = Jsoup.parse(bodyOf(result))
            document.select("h1").first().text() shouldBe "Sorry, the service is unavailable"

          }
        }

      }

    }

    "redirect to the Sign In page" when {

      "the NINO is missing from session" in new TestWithAuth(nino = None) {

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
        redirectUrl(result) shouldBe mockAppConfig.signInUrl
      }

    }

  }

}
