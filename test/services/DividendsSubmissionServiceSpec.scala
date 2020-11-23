/*
 * Copyright 2020 HM Revenue & Customs
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
import connectors.httpparsers.DividendsSubmissionHttpParser.BadRequestDividendsSubmissionException
import models.{DividendsCheckYourAnswersModel, DividendsResponseModel, DividendsSubmissionModel}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import utils.ViewTest

import scala.concurrent.Future

class DividendsSubmissionServiceSpec extends ViewTest{

  val connector: DividendsSubmissionConnector = mock[DividendsSubmissionConnector]
  val auth: AuthConnector = mock[AuthConnector]
  val service = new DividendsSubmissionService(connector)

  ".submitDividends" should {

    "return the connector response" when {

      val cyaData = DividendsCheckYourAnswersModel(
        ukDividends = Some(true),
        Some(10),
        otherUkDividends = Some(true),
        Some(10)
      )
      val dsmData = DividendsSubmissionModel(
        Some(10),
        Some(10)
      )
      val nino = "someNino"
      val mtdItd = "SomeMtdItd"
      val taxYear = 2020

      "Given connector returns a right" in  {

        (connector.submitDividends(_: DividendsSubmissionModel, _: String, _: String, _: Int)(_: HeaderCarrier))
          .expects(dsmData, nino, mtdItd, taxYear, *).returning(Future.successful(Right(DividendsResponseModel(204))))

        val result = await(service.submitDividends(Some(cyaData), nino, mtdItd, taxYear))
        result.isRight shouldBe true

      }
      "Given connector returns a left" in {

          (connector.submitDividends(_: DividendsSubmissionModel, _: String, _: String, _: Int)(_: HeaderCarrier))
            .expects(dsmData, nino, mtdItd, taxYear, *).returning(Future.successful(Left(BadRequestDividendsSubmissionException)))

          val result = await(service.submitDividends(Some(cyaData), nino, mtdItd, taxYear))
          result.isLeft shouldBe true

        }
      }
    }
}
