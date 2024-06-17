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

import connectors.DividendsSubmissionConnector
import connectors.httpParsers.DividendsSubmissionHttpParser.DividendsSubmissionsResponse
import models.dividends.{DividendsCheckYourAnswersModel, DividendsResponseModel, DividendsSubmissionModel}
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import utils.UnitTest

import scala.concurrent.Future

class DividendsSubmissionServiceSpec extends UnitTest {

  val connector: DividendsSubmissionConnector = mock[DividendsSubmissionConnector]
  val auth: AuthConnector = mock[AuthConnector]
  val service = new DividendsSubmissionService(connector)

  ".submitDividends" should {

    "return the connector response" when {

      val cyaData = DividendsCheckYourAnswersModel(
        None,
        ukDividends = Some(true),
        Some(10.00),
        otherUkDividends = Some(true),
        Some(10.00)
      )
      val dsmData = DividendsSubmissionModel(
        Some(10.00),
        Some(10.00)
      )
      val nino = "AA123456A"
      val mtdItid = "SomeMtdItid"
      val taxYear = 2020

      "Given connector returns a Right(DividendsResponseModel) of a Dividends Submission Response" in  {
        (connector.submitDividends(_: DividendsSubmissionModel, _: String, _: Int)(_: HeaderCarrier))
          .expects(
            dsmData, nino, taxYear, emptyHeaderCarrier.withExtraHeaders("mtditid"-> mtdItid)
          ).returning(Future.successful(Right(DividendsResponseModel(NO_CONTENT))))

        val result = await(service.submitDividends(cyaData, nino, mtdItid, taxYear))
        result.isRight shouldBe true
      }

      "Given connector returns a Left(APIErrorModel) of a Dividends Submission Response" in {
        (connector.submitDividends(_: DividendsSubmissionModel, _: String, _: Int)(_: HeaderCarrier))
          .expects(dsmData, nino, taxYear, emptyHeaderCarrier.withExtraHeaders("mtditid"-> mtdItid))
          .returning(Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("test","test")))))

        val result = await(service.submitDividends(cyaData, nino, mtdItid, taxYear))
        result.isLeft shouldBe true
      }

    }
  }
}
