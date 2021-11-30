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

import connectors.NrsConnector
import connectors.httpParsers.NrsSubmissionHttpParser.NrsSubmissionResponse
import models.dividends.{DecodedDividendsSubmissionPayload, DividendsCheckYourAnswersModel, DividendsPriorSubmission}
import play.api.libs.json.{JsString, Writes}
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import utils.UnitTest

import scala.concurrent.Future

class NrsServiceSpec extends UnitTest {

  val connector: NrsConnector = mock[NrsConnector]
  val service: NrsService = new NrsService(connector)

  implicit val headerCarrierWithSession: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(sessionId)))
  implicit val writesObject: Writes[DecodedDividendsSubmissionPayload] = (o: DecodedDividendsSubmissionPayload) => JsString(o.toString)

  val nino: String = "AA123456A"
  val mtditid: String = "968501689"

  val ukDividends: BigDecimal = 10
  val otherDividends: BigDecimal = 10.50

  val dividendsCyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(
    Some(true), Some(ukDividends),
    Some(true), Some(otherDividends)
  )

  val oriorData = DividendsPriorSubmission(
      Some(ukDividends),
      Some(otherDividends)
  )

  val decodedModel = DecodedDividendsSubmissionPayload(Some(dividendsCyaModel), Some(oriorData))

  ".postNrsConnector" when {

    "there is a true client ip and port" should {

      "return the connector response" in {

        val expectedResult: NrsSubmissionResponse = Right()

        val headerCarrierWithTrueClientDetails = headerCarrierWithSession.copy(trueClientIp = Some("127.0.0.1"), trueClientPort = Some("80"))

        (connector.postNrsConnector(_: String, _: DecodedDividendsSubmissionPayload)(_: HeaderCarrier, _: Writes[DecodedDividendsSubmissionPayload]))
          .expects(nino, decodedModel, headerCarrierWithTrueClientDetails.withExtraHeaders("mtditid" -> mtditid, "clientIP" -> "127.0.0.1", "clientPort" -> "80"), writesObject)
          .returning(Future.successful(expectedResult))

        val result = await(service.submit(nino, decodedModel, mtditid)(headerCarrierWithTrueClientDetails, writesObject))

        result shouldBe expectedResult
      }
    }

    "there isn't a true client ip and port" should {

      "return the connector response" in {

        val expectedResult: NrsSubmissionResponse = Right()

        (connector.postNrsConnector(_: String, _: DecodedDividendsSubmissionPayload)(_: HeaderCarrier, _: Writes[DecodedDividendsSubmissionPayload]))
          .expects(nino, decodedModel, headerCarrierWithSession.withExtraHeaders("mtditid" -> mtditid), writesObject)
          .returning(Future.successful(expectedResult))

        val result = await(service.submit(nino, decodedModel, mtditid))

        result shouldBe expectedResult
      }
    }

  }

}
