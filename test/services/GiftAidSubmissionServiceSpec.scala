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

import connectors.GiftAidSubmissionConnector
import connectors.httpParsers.GiftAidSubmissionHttpParser._
import models.charity.prior.{GiftAidPaymentsModel, GiftAidSubmissionModel, GiftsModel}
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import utils.UnitTest

import scala.concurrent.Future

class GiftAidSubmissionServiceSpec extends UnitTest {

  val connector: GiftAidSubmissionConnector = mock[GiftAidSubmissionConnector]
  val auth: AuthConnector = mock[AuthConnector]
  val service = new GiftAidSubmissionService(connector)

  ".submitGiftAid" should {

    "return the connector response" when {

      val amount: Option[BigDecimal] = Some(100.00)

      val cyaData = GiftAidSubmissionModel(
        Some(GiftAidPaymentsModel(amount,None,amount,amount,amount,amount)),
        Some(GiftsModel(amount,None,amount,amount))
      )


      val nino = "AA123456A"
      val mtdItid = "SomeMtdItid"
      val taxYear = 2020

      "Given connector returns a right" in  {

        (connector.submitGiftAid(_: GiftAidSubmissionModel, _: String, _: Int)(_: HeaderCarrier))
          .expects(cyaData, nino, taxYear, emptyHeaderCarrier.withExtraHeaders("mtditid"-> mtdItid)).returning(Future.successful(Right((NO_CONTENT))))

        val result = await(service.submitGiftAid(Some(cyaData), nino, mtdItid, taxYear))
        result.isRight shouldBe true

      }
      "Given connector returns a left" in {

          (connector.submitGiftAid(_: GiftAidSubmissionModel, _: String, _: Int)(_: HeaderCarrier))
            .expects(cyaData, nino, taxYear, emptyHeaderCarrier.withExtraHeaders("mtditid"-> mtdItid))
            .returning(Future.successful(Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("test","test")))))

          val result = await(service.submitGiftAid(Some(cyaData), nino, mtdItid, taxYear))
          result.isLeft shouldBe true

        }

      "Given no model is supplied" in {

       lazy val result: GiftAidSubmissionsResponse = {
          val blankData = GiftAidSubmissionModel(None, None)

          (blankData, nino, mtdItid, taxYear, emptyHeaderCarrier.withExtraHeaders("mtditid"-> mtdItid))
          Future.successful(Right(NO_CONTENT))

          await(service.submitGiftAid(None, nino, mtdItid, taxYear))
        }
        result.isRight shouldBe true
        result shouldBe Right((NO_CONTENT))
      }
    }
  }
}
