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

import audit.{AuditModel, CreateOrAmendDividendsAuditDetail}
import common.SessionValues
import config.{AppConfig, ErrorHandler, MockAuditService}
import controllers.predicates.AuthorisedAction
import models._
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalamock.handlers.CallHandler
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.Results._
import play.api.mvc.{AnyContentAsEmpty, Request, Result}
import play.api.test.FakeRequest
import services.DividendsSubmissionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditResult
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.UnitTestWithApp
import views.html.dividends.DividendsCYAView
import views.html.templates.{InternalServerErrorTemplate, ServiceUnavailableTemplate}

import scala.concurrent.Future

class DividendsCYAControllerSpec extends UnitTestWithApp with MockAuditService {

  val service: DividendsSubmissionService = mock[DividendsSubmissionService]
  val errorHandler: ErrorHandler = mock[ErrorHandler]
  val serviceUnavailableTemplate: ServiceUnavailableTemplate = app.injector.instanceOf[ServiceUnavailableTemplate]
  val unauthorisedTemplate: InternalServerErrorTemplate = app.injector.instanceOf[InternalServerErrorTemplate]


  val controller = new DividendsCYAController(
    app.injector.instanceOf[DividendsCYAView],
    service,
    mockAuditService,
    errorHandler
  )(
    mockAppConfig,
    authorisedAction,
    mockMessagesControllerComponents
  )

  val taxYear: Int = mockAppConfig.defaultTaxYear
  val firstAmount = 10
  val secondAmount = 20
  val successResponseCode = 204
  val internalServerErrorResponse = 500
  val serviceUnavailableResponse = 503
  val individualAffinityGroup: String = "Individual"

  val internalServerErrorModel: DividendsResponseModel = DividendsResponseModel(internalServerErrorResponse)

  ".show" should {

    s"return an OK($OK)" when {

      val cyaSessionData = DividendsCheckYourAnswersModel(
        ukDividends = Some(true),
        Some(firstAmount),
        otherUkDividends = Some(true),
        Some(firstAmount)
      )

      val priorData = DividendsPriorSubmission(
        Some(firstAmount),
        Some(secondAmount)
      )

      "there is CYA session data and prior submission data" in new TestWithAuth {
        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.DIVIDENDS_CYA -> Json.toJson(cyaSessionData).toString,
          SessionValues.DIVIDENDS_PRIOR_SUB -> Json.toJson(priorData).toString
        )

        val result: Future[Result] = controller.show(taxYear)(request)

        status(result) shouldBe OK
      }

      "there is CYA session data and no prior submission data" in new TestWithAuth {
        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.DIVIDENDS_CYA -> Json.toJson(cyaSessionData).toString
        )

        val result: Future[Result] = controller.show(taxYear)(request)

        status(result) shouldBe OK
      }

      "there is prior submission data and no CYA session data" in new TestWithAuth {
        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
          SessionValues.TAX_YEAR -> taxYear.toString,
          SessionValues.DIVIDENDS_PRIOR_SUB -> Json.toJson(priorData).toString
        )

        val result: Future[Result] = controller.show(taxYear)(request)

        status(result) shouldBe OK
      }

    }

    "redirect to the overview page" when {

      "there is no session data" in new TestWithAuth {
        val result: Future[Result] = controller.show(taxYear)(FakeRequest().withSession(SessionValues.TAX_YEAR -> taxYear.toString))

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
      }

    }

    "redirect the user to the most relevant page if journey has not been completed" when {

      "up to receive UK Dividends is filled in" when {

        "the answer is Yes" which {
          lazy val result = controller.show(taxYear)(fakeRequest.withSession(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(
              Some(true), None, None, None
            ).asJsonString
          ))

          s"has the SEE_OTHER($SEE_OTHER) status" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "redirects to the UK Dividends Amount page" in {
            redirectUrl(result) shouldBe controllers.dividends.routes.UkDividendsAmountController.show(taxYear).url
          }
        }
        "the answer is No" which {
          lazy val result = controller.show(taxYear)(fakeRequest.withSession(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(
              Some(false), None, None, None
            ).asJsonString
          ))

          s"has the SEE_OTHER($SEE_OTHER) status" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "redirects to the Receive Other UK Dividends page" in {
            redirectUrl(result) shouldBe controllers.dividends.routes.ReceiveOtherUkDividendsController.show(taxYear).url
          }
        }
      }

      "up to UK Dividends Amount is filled in" which {
          lazy val result = controller.show(taxYear)(fakeRequest.withSession(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(
              Some(true), Some(100.00), None, None
            ).asJsonString
          ))

          s"has the SEE_OTHER($SEE_OTHER) status" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "redirects to the Receive Other UK Dividends page" in {
            redirectUrl(result) shouldBe controllers.dividends.routes.ReceiveOtherUkDividendsController.show(taxYear).url
          }
      }

      "up to Receive Other UK Dividends is filled in" which {
          lazy val result = controller.show(taxYear)(fakeRequest.withSession(
            SessionValues.TAX_YEAR -> taxYear.toString,
            SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(
              Some(true), Some(100.00), Some(true), None
            ).asJsonString
          ))

          s"has the SEE_OTHER($SEE_OTHER) status" in new TestWithAuth {
            status(result) shouldBe SEE_OTHER
          }

          "redirects to the Other UK Dividends Amount page" in {
            redirectUrl(result) shouldBe controllers.dividends.routes.OtherUkDividendsAmountController.show(taxYear).url
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

        val featureSwitchController = new DividendsCYAController(
          app.injector.instanceOf[DividendsCYAView],
          service,
          mockAuditService,
          errorHandler
        )(
          mockAppConfFeatureSwitch,
          authorisedActionFeatureSwitch,
          mockMessagesControllerComponents
        )

        val invalidTaxYear = 2023

        lazy val result: Future[Result] = featureSwitchController.show(invalidTaxYear)(
          fakeRequest.withSession(SessionValues.TAX_YEAR -> mockAppConfFeatureSwitch.defaultTaxYear.toString)
        )

        redirectUrl(result) shouldBe controllers.routes.TaxYearErrorController.show().url

      }
    }

  }

  ".priorityOrderOrNone" should {

    val amount1: BigDecimal = 120
    val amount2: BigDecimal = 140

    val priorityValue: Option[BigDecimal] = Some(amount1)
    val otherValue: Option[BigDecimal] = Some(amount2)

    "return the priority value" when {

      "the priority value is provided on it's own" in {
        controller.priorityOrderOrNone(priorityValue, None, yesNoResult = true) shouldBe Some(amount1)
      }

      "the priority value is provided along side the other value" in {
        controller.priorityOrderOrNone(priorityValue, otherValue, yesNoResult = true) shouldBe Some(amount1)
      }

    }

    "return the other value" when {

      "no priority value is provided" in {
        controller.priorityOrderOrNone(None, otherValue, yesNoResult = true) shouldBe Some(amount2)
      }

    }

    "return None" when {

      "no priority or other value is provided" in {
        controller.priorityOrderOrNone(None, None, yesNoResult = true) shouldBe None
      }

      "yesNoResult is false regardless of provided values" in {
        controller.priorityOrderOrNone(priorityValue, otherValue, yesNoResult = false) shouldBe None
      }

    }

  }

  ".submit" should {

    val cyaSessionData = DividendsCheckYourAnswersModel(
      ukDividends = Some(true),
      Some(firstAmount),
      otherUkDividends = Some(true),
      Some(firstAmount)
    )

    val priorData = DividendsPriorSubmission(
      Some(firstAmount),
      Some(firstAmount)
    )
    "redirect to the overview page" when {

      "there is session data " in new TestWithAuth {
        lazy val detail: CreateOrAmendDividendsAuditDetail =
          CreateOrAmendDividendsAuditDetail(Some(cyaSessionData), Some(priorData), "AA123456A", "1234567890", individualAffinityGroup.toLowerCase(), taxYear)

        lazy val event: AuditModel[CreateOrAmendDividendsAuditDetail] =
          AuditModel("CreateOrAmendDividendsUpdate", "createOrAmendDividendsUpdate", detail)

        def verifyDividendsAudit: CallHandler[Future[AuditResult]] = verifyAuditEvent(event)

        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
          SessionValues.CLIENT_MTDITID -> Json.toJson("someMtdItid").toString(),
          SessionValues.DIVIDENDS_CYA -> Json.toJson(cyaSessionData).toString(),
          SessionValues.DIVIDENDS_PRIOR_SUB -> Json.toJson(priorData).toString()
        )

        (service.submitDividends(_: Option[DividendsCheckYourAnswersModel], _: String, _: String, _: Int)(_:HeaderCarrier))
          .expects(Some(cyaSessionData), "AA123456A", "1234567890", taxYear, *)
          .returning(Future.successful(Right(DividendsResponseModel(successResponseCode))))
        verifyDividendsAudit

        lazy val result: Future[Result] = controller.submit(taxYear)(request)
        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
        getSession(result).get(SessionValues.DIVIDENDS_CYA) shouldBe None
        getSession(result).get(SessionValues.DIVIDENDS_PRIOR_SUB) shouldBe None

      }
    }

    "there is an error posting downstream" should {

      "redirect to the 500 unauthorised error template page when there is a problem posting data" in new TestWithAuth {

        val errorResponseFromDes: Either[APIErrorModel, DividendsResponseModel] =
          Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("error", "error")))

        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
          SessionValues.CLIENT_MTDITID -> Json.toJson("someMtdItid").toString(),
          SessionValues.DIVIDENDS_CYA -> Json.toJson(cyaSessionData).toString()
        )

        (service.submitDividends(_: Option[DividendsCheckYourAnswersModel], _: String, _: String, _: Int)(_: HeaderCarrier))
          .expects(Some(cyaSessionData), "AA123456A", "1234567890", 2020, *)
          .returning(Future.successful(errorResponseFromDes))


        (errorHandler.handleError(_: Int)(_: Request[_]))
          .expects(500, *)
          .returning(InternalServerError(unauthorisedTemplate()))

        val result: Future[Result] = controller.submit(2020)(request)

        val document: Document = Jsoup.parse(bodyOf(result))
        document.select("h1").first().text() shouldBe "Sorry, there is a problem with the service"
      }

      "redirect to the 503 service unavailable page when the service is unavailable" in new TestWithAuth(){

        val errorResponseFromDes: Either[APIErrorModel, DividendsResponseModel] = Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("error", "error")))

        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
          SessionValues.CLIENT_MTDITID -> Json.toJson("someMtdItid").toString(),
          SessionValues.DIVIDENDS_CYA -> Json.toJson(cyaSessionData).toString()
        )

        (service.submitDividends(_: Option[DividendsCheckYourAnswersModel], _: String, _: String, _: Int)(_: HeaderCarrier))
          .expects(Some(cyaSessionData), "AA123456A", "1234567890", 2020, *)
          .returning(Future.successful(errorResponseFromDes))


        (errorHandler.handleError(_: Int)(_: Request[_]))
          .expects(*, *)
          .returning(ServiceUnavailable(serviceUnavailableTemplate()))

        val result: Future[Result] = controller.submit(2020)(request)

        val document: Document = Jsoup.parse(bodyOf(result))
        document.select("h1").first().text() shouldBe "Sorry, the service is unavailable"

    }
  }

}

}
