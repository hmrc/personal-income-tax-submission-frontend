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

package services

import connectors.InterestSubmissionConnector
import connectors.httpparsers.InterestSubmissionHttpParser.InterestSubmissionsResponse
import models.{ApiErrorBodyModel, ApiErrorModel}
import models.httpResponses.ErrorResponse
import models.interest.{InterestAccountModel, InterestCYAModel, InterestSubmissionModel}
import play.api.test.Helpers.{BAD_REQUEST, NO_CONTENT}
import uk.gov.hmrc.http.HeaderCarrier
import utils.UnitTest
import play.api.http.Status._

import scala.concurrent.{ExecutionContext, Future}

class InterestSubmissionServiceSpec extends UnitTest {

  val connector: InterestSubmissionConnector = mock[InterestSubmissionConnector]
  val service: InterestSubmissionService = new InterestSubmissionService(connector)

  val taxYear: Int = 2020

  ".submit" should {

    "return a successful response" when {

      lazy val cyaModel = InterestCYAModel(
        Some(true),
        Some(Seq(InterestAccountModel(Some("anId"), "dis account yo", 100.00, None, None))),
        Some(true),
        Some(Seq(InterestAccountModel(Some("anotherId"), "a bank thing", 200.00, None, None)))
      )

      lazy val accounts: Seq[InterestSubmissionModel] = Seq(
        InterestSubmissionModel(Some("anId"), "dis account yo", Some(100.00), None),
        InterestSubmissionModel(Some("anotherId"), "a bank thing", None, Some(200.00))
      )

      "the connector returns a successful response" when {

        "the cya model has accounts" in {
          lazy val result: InterestSubmissionsResponse = {
            (connector.submit(_: Seq[InterestSubmissionModel], _: String, _: Int, _: String)(_: HeaderCarrier, _: ExecutionContext))
              .expects(accounts, "AA123456A", taxYear, "1234567890", *, *)
              .returning(Future.successful(
                Right(NO_CONTENT)
              ))

            await(service.submit(cyaModel, "AA123456A", taxYear, "1234567890"))
          }

          result shouldBe Right(NO_CONTENT)
        }

        "the cya model has no accounts" in {
          lazy val result: InterestSubmissionsResponse = {
            (connector.submit(_: Seq[InterestSubmissionModel], _: String, _: Int, _: String)(_: HeaderCarrier, _: ExecutionContext))
              .expects(Seq.empty[InterestSubmissionModel], "AA123456A", taxYear, "1234567890", *, *)
              .returning(Future.successful(
                Right(NO_CONTENT)
              ))

            await(service.submit(cyaModel.copy(Some(false), None, Some(false), None), "AA123456A", taxYear, "1234567890"))
          }

          result shouldBe Right(NO_CONTENT)
        }
      }

      "the connector returns a left error response" in {

        val error = ErrorResponse(BAD_REQUEST, "oh noes")

        lazy val result: InterestSubmissionsResponse = {
          (connector.submit(_: Seq[InterestSubmissionModel], _: String, _: Int, _: String)(_: HeaderCarrier, _: ExecutionContext))
            .expects(accounts, "AA123456A", taxYear, "1234567890", *, *)
            .returning(Future.successful(
              Left(ApiErrorModel(INTERNAL_SERVER_ERROR, ApiErrorBodyModel("test", "test")))
            ))

          await(service.submit(cyaModel, "AA123456A", taxYear, "1234567890"))
        }

        result shouldBe Left(ApiErrorModel(INTERNAL_SERVER_ERROR, ApiErrorBodyModel("test", "test")))
      }

    }

  }

}
