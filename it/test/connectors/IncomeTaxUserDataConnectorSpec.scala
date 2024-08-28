/*
 * Copyright 2023 HM Revenue & Customs
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

package connectors

import connectors.httpParsers.IncomeTaxUserDataHttpParser.IncomeTaxUserDataResponse
import models.charity.prior.{GiftAidPaymentsModel, GiftAidSubmissionModel, GiftsModel}
import models.dividends.DividendsPriorSubmission
import models.priorDataModels.{IncomeSourcesModel, InterestModel}
import models.{APIErrorBodyModel, APIErrorModel, User}
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import play.api.test.Helpers.OK
import test.utils.IntegrationTest
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class IncomeTaxUserDataConnectorSpec extends IntegrationTest {

  lazy val connector: IncomeTaxUserDataConnector = app.injector.instanceOf[IncomeTaxUserDataConnector]

  val testUser: User[AnyContentAsEmpty.type] = User(mtditid, None, nino, "individual", sessionId)(FakeRequest())

  implicit override val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid, "X-Session-ID" -> sessionId)

  "IncomeTaxUserDataConnector" should {

    "return a success result" when {

      "submission returns a 204" in {

        stubGetWithHeadersCheck(s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", NO_CONTENT,
          "{}", xSessionId, "mtditid" -> mtditid)

        val result: IncomeTaxUserDataResponse = Await.result(connector.getUserData(taxYear)(testUser, headerCarrier), Duration.Inf)
        result shouldBe Right(IncomeSourcesModel())
      }

      "submission returns a 200" in {
        val incomeSourcesModel = IncomeSourcesModel(
          Some(DividendsPriorSubmission(
            Some(199.93), Some(1222342.88)
          )),
          Some(Seq(InterestModel(
            "A persons account", "asdjajdlfkj", Some(19999999.99), None
          ))),
          Some(GiftAidSubmissionModel(
            Some(GiftAidPaymentsModel(Some(9388889.54), currentYearTreatedAsPreviousYear = Some(2333109.98))),
            Some(GiftsModel(
              Some(192.98)
            ))
          ))
        )

        userDataStub(incomeSourcesModel, nino, taxYear)

        val result: IncomeTaxUserDataResponse = Await.result(connector.getUserData(taxYear)(testUser, headerCarrier), Duration.Inf)
        result shouldBe Right(incomeSourcesModel)
      }
    }

    "Return an error result" when {

      "submission returns a 200 but invalid json" in {

        stubGetWithHeadersCheck(s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", OK,
          Json.toJson("""{"invalid": true}""").toString(), ("X-Session-ID" -> sessionId), ("mtditid" -> mtditid))

        val result: IncomeTaxUserDataResponse = Await.result(connector.getUserData(taxYear)(testUser, headerCarrier), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel.parsingError))
      }

      "submission returns a 500" in {

        stubGetWithHeadersCheck(s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", INTERNAL_SERVER_ERROR,
          """{"code": "FAILED", "reason": "failed"}""", ("X-Session-ID" -> sessionId), ("mtditid" -> mtditid))

        val result: IncomeTaxUserDataResponse = Await.result(connector.getUserData(taxYear)(testUser, headerCarrier), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns a 503" in {

        stubGetWithHeadersCheck(s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", SERVICE_UNAVAILABLE,
          """{"code": "FAILED", "reason": "failed"}""", ("X-Session-ID" -> sessionId), ("mtditid" -> mtditid))

        val result: IncomeTaxUserDataResponse = Await.result(connector.getUserData(taxYear)(testUser, headerCarrier), Duration.Inf)
        result shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("FAILED", "failed")))
      }

      "submission returns an unexpected result" in {

        stubGetWithHeadersCheck(s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", BAD_REQUEST,
          """{"code": "FAILED", "reason": "failed"}""", ("X-Session-ID" -> sessionId), ("mtditid" -> mtditid))

        val result: IncomeTaxUserDataResponse = Await.result(connector.getUserData(taxYear)(testUser, headerCarrier), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("FAILED", "failed")))
      }
    }
  }

}
