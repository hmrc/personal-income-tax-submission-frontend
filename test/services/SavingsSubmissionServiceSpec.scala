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

package services

import connectors.SavingsSubmissionConnector
import connectors.httpParsers.SavingsSubmissionHttpParser.SavingsSubmissionResponse
import models.savings.{ForeignInterestModel, SavingsIncomeCYAModel, SavingsSubmissionModel, SecuritiesModel}
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import utils.UnitTest

import scala.concurrent.{ExecutionContext, Future}

class SavingsSubmissionServiceSpec extends UnitTest {

  val connector: SavingsSubmissionConnector = mock[SavingsSubmissionConnector]
  val auth: AuthConnector = mock[AuthConnector]
  val service = new SavingsSubmissionService(connector)

  ".submitSavings" should {

    "return the connector response" when {

      val cyaData = SavingsIncomeCYAModel(
        Some(true),
        Some(1000.00),
        Some(true),
        Some(500.00)
      )
      val smData = SavingsSubmissionModel(
        securities = Some(SecuritiesModel(Some(500.00),1000.00,Some(500.00))),
        foreignInterest = Some(Seq[ForeignInterestModel]())
      )
      val nino = "AA123456A"
      val mtdItid = "SomeMtdItid"
      val taxYear = 2020

      "Given connector returns a right" in  {

        (connector.submitSavings(_: SavingsSubmissionModel, _: String, _: Int)(_: HeaderCarrier, _: ExecutionContext))
          .expects(smData, nino, taxYear, emptyHeaderCarrier.withExtraHeaders("mtditid"-> mtdItid), *).returning(Future.successful(Right(true)))

        val result = await(service.submitSavings(Some(cyaData), None, nino, mtdItid, taxYear))
        result.isRight shouldBe true

      }
      "Given connector returns a left" in {

          (connector.submitSavings(_: SavingsSubmissionModel, _: String, _: Int)(_: HeaderCarrier, _: ExecutionContext))
            .expects(smData, nino, taxYear, emptyHeaderCarrier.withExtraHeaders("mtditid"-> mtdItid), *)
            .returning(Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("test","test")))))

          val result = await(service.submitSavings(Some(cyaData), None, nino, mtdItid, taxYear))
          result.isLeft shouldBe true

        }
    }
  }
}
