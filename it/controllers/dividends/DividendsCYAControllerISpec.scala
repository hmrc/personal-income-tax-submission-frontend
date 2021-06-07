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
import connectors.DividendsSubmissionConnector
import helpers.PlaySessionCookieBaker
import models.dividends.{DividendsCheckYourAnswersModel, DividendsPriorSubmission, DividendsSubmissionModel}
import models.priorDataModels.IncomeSourcesModel
import play.api.http.Status._
import play.api.libs.ws.{WSClient, WSResponse}
import play.mvc.Http.HeaderNames
import utils.IntegrationTest

class DividendsCYAControllerISpec extends IntegrationTest {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val connector: DividendsSubmissionConnector = app.injector.instanceOf[DividendsSubmissionConnector]

  val taxYear = 2022

  val dividends: BigDecimal = 10
  val dividendsCheckYourAnswersUrl = s"$startUrl/$taxYear/dividends/check-income-from-dividends"

  lazy val dividendsBody: DividendsSubmissionModel = DividendsSubmissionModel(
    Some(dividends),
    Some(dividends)
  )

  val firstAmount = 10
  val secondAmount = 20
  val successResponseCode = 204
  val internalServerErrorResponse = 500
  val serviceUnavailableResponse = 503
  val individualAffinityGroup: String = "Individual"

  val fullDividendsNino = "AA000003A"

  ".show" should {

    s"return an OK($OK)" when {

      val priorData = IncomeSourcesModel(
        dividends = Some(DividendsPriorSubmission(
          Some(firstAmount),
          Some(firstAmount)
        ))
      )

      "there is CYA session data and prior submission data" in {
        val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        val result: WSResponse = {
          authoriseIndividual(Some(fullDividendsNino))
          userDataStub(priorData, fullDividendsNino, taxYear)
          await(wsClient.url(dividendsCheckYourAnswersUrl)
            .withHttpHeaders(
              "X-Session-ID" -> sessionId,
              "mtditid" -> mtditid,
              "Csrf-Token" -> "nocheck",
              HeaderNames.COOKIE -> playSessionCookie
            )
            .get())
        }

        result.status shouldBe OK
      }

      "there is CYA session data and no prior submission data" in {
        val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        val result: WSResponse = {
          authoriseIndividual(Some(fullDividendsNino))
          userDataStub(IncomeSourcesModel(), fullDividendsNino, taxYear)
          await(wsClient.url(dividendsCheckYourAnswersUrl)
            .withHttpHeaders(
              "X-Session-ID" -> sessionId,
              "mtditid" -> mtditid,
              "Csrf-Token" -> "nocheck",
              HeaderNames.COOKIE -> playSessionCookie
            )
            .get())
        }

        result.status shouldBe OK
      }

      "there is prior submission data and no CYA session data" in {
        val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        val result: WSResponse = {
          authoriseIndividual(Some("AA112233B"))
          userDataStub(priorData, "AA112233B", taxYear)
          await(wsClient.url(dividendsCheckYourAnswersUrl)
            .withHttpHeaders(
              "X-Session-ID" -> sessionId,
              "mtditid" -> mtditid,
              "Csrf-Token" -> "nocheck",
              HeaderNames.COOKIE -> playSessionCookie
            )
            .get())
        }

        result.status shouldBe OK
      }

    }

    "redirect to the overview page" when {

      "there is no session data" in {
        val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        val result: WSResponse = {
          authoriseIndividual(Some("AA119293B"))
          userDataStub(IncomeSourcesModel(), "AA119293B", taxYear)
          stubGet("/income-through-software/return/2022/view", SEE_OTHER, "overview")
          await(wsClient.url(dividendsCheckYourAnswersUrl)
            .withHttpHeaders(
              "X-Session-ID" -> sessionId,
              "mtditid" -> mtditid,
              "Csrf-Token" -> "nocheck",
              HeaderNames.COOKIE -> playSessionCookie
            )
            .withFollowRedirects(false)
            .get())
        }

        result.status shouldBe SEE_OTHER
      }

    }

        "redirect the user to the most relevant page if journey has not been completed" when {

          "up to receive UK Dividends is filled in" when {

//            "the answer is Yes" which {
//              lazy val result = {
//                mockDividendsGetPrior(None)
//                mockDividendGetCya(Some(
//                  DividendsCheckYourAnswersModel(Some(true))
//                ))
//                controller.show(taxYear)(fakeRequest.withSession(
//                  SessionValues.TAX_YEAR -> taxYear.toString
//                ))
//              }
//
//              s"has the SEE_OTHER($SEE_OTHER) status" in {
//                status(result) shouldBe SEE_OTHER
//              }
//
//              "redirects to the UK Dividends Amount page" in {
//                redirectUrl(result) shouldBe controllers.dividends.routes.UkDividendsAmountController.show(taxYear).url
//              }
//            }
    //
    //        "the answer is No" which {
    //          lazy val result = {
    //            mockDividendsGetPrior(None)
    //            mockDividendGetCya(Some(
    //              DividendsCheckYourAnswersModel(Some(false))
    //            ))
    //            controller.show(taxYear)(fakeRequest.withSession(
    //              SessionValues.TAX_YEAR -> taxYear.toString
    //            ))
    //          }
    //
    //          s"has the SEE_OTHER($SEE_OTHER) status" in new TestWithAuth {
    //            status(result) shouldBe SEE_OTHER
    //          }
    //
    //          "redirects to the Receive Other UK Dividends page" in {
    //            redirectUrl(result) shouldBe controllers.dividends.routes.ReceiveOtherUkDividendsController.show(taxYear).url
    //          }
    //        }
    //      }
    //
    //      "up to UK Dividends Amount is filled in" which {
    //        lazy val result = {
    //          mockDividendsGetPrior(None)
    //          mockDividendGetCya(Some(
    //            DividendsCheckYourAnswersModel(Some(true), Some(100.00), None, None)
    //          ))
    //          controller.show(taxYear)(fakeRequest.withSession(
    //            SessionValues.TAX_YEAR -> taxYear.toString
    //          ))
    //        }
    //
    //        s"has the SEE_OTHER($SEE_OTHER) status" in new TestWithAuth {
    //          status(result) shouldBe SEE_OTHER
    //        }
    //
    //        "redirects to the Receive Other UK Dividends page" in {
    //          redirectUrl(result) shouldBe controllers.dividends.routes.ReceiveOtherUkDividendsController.show(taxYear).url
    //        }
    //      }
    //
    //      "up to Receive Other UK Dividends is filled in" which {
    //        lazy val result = {
    //          mockDividendsGetPrior(None)
    //          mockDividendGetCya(Some(
    //            DividendsCheckYourAnswersModel(Some(true), Some(100.00), Some(true), None)
    //          ))
    //          controller.show(taxYear)(fakeRequest.withSession(SessionValues.TAX_YEAR -> taxYear.toString))
    //        }
    //
    //        s"has the SEE_OTHER($SEE_OTHER) status" in new TestWithAuth {
    //          status(result) shouldBe SEE_OTHER
    //        }
    //
    //        "redirects to the Other UK Dividends Amount page" in {
    //          redirectUrl(result) shouldBe controllers.dividends.routes.OtherUkDividendsAmountController.show(taxYear).url
    //        }
    //      }
    //
    //    }

    //    "Redirect to the tax year error " when {
    //
    //      "an invalid tax year has been added to the url" in new TestWithAuth() {
    //
    //        val mockAppConfFeatureSwitch: AppConfig = new AppConfig(mock[ServicesConfig]){
    //          override lazy val defaultTaxYear: Int = 2022
    //          override lazy val taxYearErrorFeature = true
    //        }
    //
    //        val authorisedActionFeatureSwitch = new AuthorisedAction(mockAppConfFeatureSwitch,
    //          agentAuthErrorPageView)(mockAuthService, stubMessagesControllerComponents())
    //
    //        val featureSwitchController = new DividendsCYAController(
    //          app.injector.instanceOf[DividendsCYAView],
    //          service,
    //          mockDividendsSessionService,
    //          mockAuditService,
    //          errorHandler
    //        )(
    //          mockAppConfFeatureSwitch,
    //          authorisedActionFeatureSwitch,
    //          mockMessagesControllerComponents
    //        )
    //
    //        val invalidTaxYear = 2023
    //
    //        lazy val result: Future[Result] = featureSwitchController.show(invalidTaxYear)(
    //          fakeRequest.withSession(SessionValues.TAX_YEAR -> mockAppConfFeatureSwitch.defaultTaxYear.toString)
    //        )
    //
    //        redirectUrl(result) shouldBe controllers.routes.TaxYearErrorController.show().url
    //
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

    //    "redirect to the overview page" when {
    //
    //      "there is session data " in new TestWithAuth {
    //        lazy val detail: CreateOrAmendDividendsAuditDetail =
    //          CreateOrAmendDividendsAuditDetail(
    //            Some(cyaSessionData),
    //            Some(priorData),
    //            isUpdate = true,
    //            "AA123456A",
    //            "1234567890",
    //            individualAffinityGroup.toLowerCase(),
    //            taxYear
    //          )
    //
    //        lazy val event: AuditModel[CreateOrAmendDividendsAuditDetail] =
    //          AuditModel("CreateOrAmendDividendsUpdate", "createOrAmendDividendsUpdate", detail)
    //
    //        def verifyDividendsAudit: CallHandler[Future[AuditResult]] = verifyAuditEvent(event)
    //
    //        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    //          SessionValues.CLIENT_MTDITID -> Json.toJson("someMtdItid").toString(),
    //          SessionValues.DIVIDENDS_CYA -> Json.toJson(cyaSessionData).toString(),
    //          SessionValues.DIVIDENDS_PRIOR_SUB -> Json.toJson(priorData).toString()
    //        )
    //
    //        (service.submitDividends(_: Option[DividendsCheckYourAnswersModel], _: String, _: String, _: Int)(_: HeaderCarrier))
    //          .expects(Some(cyaSessionData), "AA123456A", "1234567890", taxYear, *)
    //          .returning(Future.successful(Right(DividendsResponseModel(successResponseCode))))
    //        verifyDividendsAudit
    //
    //        lazy val result: Future[Result] = controller.submit(taxYear)(request)
    //        status(result) shouldBe SEE_OTHER
    //        redirectUrl(result) shouldBe mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear)
    //        getSession(result).get(SessionValues.DIVIDENDS_CYA) shouldBe None
    //        getSession(result).get(SessionValues.DIVIDENDS_PRIOR_SUB) shouldBe None
    //
    //      }
    //    }

    //    "there is an error posting downstream" should {
    //
    //      "redirect to the 500 unauthorised error template page when there is a problem posting data" in new TestWithAuth {
    //        val httpErrorCode = 500
    //
    //        val errorResponseFromDes: Either[APIErrorModel, DividendsResponseModel] =
    //          Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("error", "error")))
    //
    //        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    //          SessionValues.CLIENT_MTDITID -> Json.toJson("someMtdItid").toString(),
    //          SessionValues.DIVIDENDS_CYA -> Json.toJson(cyaSessionData).toString()
    //        )
    //
    //        (service.submitDividends(_: Option[DividendsCheckYourAnswersModel], _: String, _: String, _: Int)(_: HeaderCarrier))
    //          .expects(Some(cyaSessionData), "AA123456A", "1234567890", taxYear2020, *)
    //          .returning(Future.successful(errorResponseFromDes))
    //
    //
    //        (errorHandler.handleError(_: Int)(_: Request[_]))
    //          .expects(httpErrorCode, *)
    //          .returning(InternalServerError(unauthorisedTemplate()))
    //
    //        val result: Future[Result] = controller.submit(taxYear2020)(request)
    //
    //        val document: Document = Jsoup.parse(bodyOf(result))
    //        document.select("h1").first().text() shouldBe "Sorry, there is a problem with the service"
    //      }
    //
    //      "redirect to the 503 service unavailable page when the service is unavailable" in new TestWithAuth() {
    //        val errorResponseFromDes: Either[APIErrorModel, DividendsResponseModel] = Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("error", "error")))
    //
    //        val request: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withSession(
    //          SessionValues.CLIENT_MTDITID -> Json.toJson("someMtdItid").toString(),
    //          SessionValues.DIVIDENDS_CYA -> Json.toJson(cyaSessionData).toString()
    //        )
    //
    //        (service.submitDividends(_: Option[DividendsCheckYourAnswersModel], _: String, _: String, _: Int)(_: HeaderCarrier))
    //          .expects(Some(cyaSessionData), "AA123456A", "1234567890", taxYear2020, *)
    //          .returning(Future.successful(errorResponseFromDes))
    //
    //
    //        (errorHandler.handleError(_: Int)(_: Request[_]))
    //          .expects(*, *)
    //          .returning(ServiceUnavailable(serviceUnavailableTemplate()))
    //
    //        val result: Future[Result] = controller.submit(taxYear2020)(request)
    //
    //        val document: Document = Jsoup.parse(bodyOf(result))
    //        document.select("h1").first().text() shouldBe "Sorry, the service is unavailable"
    //
    //      }
    //    }

  }

}
