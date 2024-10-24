/*
 * Copyright 2024 HM Revenue & Customs
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

import connectors.SectionCompletedConnector
import models.mongo.JourneyAnswers
import models.mongo.JourneyStatus.Completed
import org.apache.pekko.Done
import play.api.libs.json.Json
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import utils.UnitTest

import java.time.Instant
import scala.concurrent.Future

class SectionCompletedServiceSpec extends UnitTest {
  val auth: AuthConnector = mock[AuthConnector]
  val mockConnector: SectionCompletedConnector = mock[SectionCompletedConnector]
  val mtdItId = "1234567890"
  val taxYear = 2023
  val journeyName = "test-journey"
  val data = Json.obj("status" -> Completed.toString)
  implicit val correlationId: String = "someCorrelationId"
  val journeyAnswers = JourneyAnswers(
    mtdItId = mtdItId,
    taxYear = taxYear,
    journey = journeyName,
    data = data,
    lastUpdated = Instant.ofEpochSecond(1))



  val service = new SectionCompletedService(mockConnector)

  override def beforeEach(): Unit = {
    super.beforeEach()
  }

  "SectionCompletedService" should {

    "get journey answers" in {
      (mockConnector.get(_:String, _:Int, _:String)(_: HeaderCarrier))
        .expects(mtdItId, taxYear, journeyName, *)
        .returning(Future.successful(Some(journeyAnswers)))

      val result = await(service.get(mtdItId, taxYear, journeyName))

      result shouldBe Some(journeyAnswers)
    }

    "set journey answers" in {
      (mockConnector.set(_: JourneyAnswers)(_: HeaderCarrier))
        .expects(journeyAnswers, *)
        .returning(Future.successful(Done))

      val result = await(service.set(journeyAnswers))

      result shouldBe Done
    }

    "keep journey alive" in {
      (mockConnector.keepAlive(_: String, _: Int, _: String)(_: HeaderCarrier))
        .expects(mtdItId, taxYear, journeyName, *)
        .returning(Future.successful(Done))

      val result = await(service.keepAlive(mtdItId, taxYear, journeyName))

      result shouldBe Done
    }
  }






//  implicit val hc: HeaderCarrier = HeaderCarrier()
//  implicit val correlationId: String = "test-correlation-id"
//
//  val mockConnector: SectionCompletedConnector = mock[SectionCompletedConnector]
//  val service = new SectionCompletedService(mockConnector)
//  val data = Json.obj(
//    "status" -> Completed.toString
//  )
//
//  "SectionCompletedService" should {
//
//    "get journey answers" in {
//      val mtdItId = "test-mtdItId"
//      val taxYear = 2023
//      val journeyName = "test-journey"
//
//      val journeyAnswers = Some(JourneyAnswers(
//        mtdItId = mtdItId,
//        taxYear = taxYear,
//        journey = journeyName, data = data, lastUpdated = Instant.ofEpochSecond(1)))
//
//      when(mockConnector.get(mtdItId, taxYear, journeyName))
//        .thenReturn(Future.successful(journeyAnswers))
//
//      whenReady(service.get(mtdItId, taxYear, journeyName)) { result =>
//        result shouldBe journeyAnswers
//      }
//
//      verify(mockConnector).get(mtdItId, taxYear, journeyName)
//    }
//
//    "set journey answers" in {
//      val mtdItId = "test-mtdItId"
//      val taxYear = 2023
//      val journeyName = "test-journey"
//      val journeyAnswers = JourneyAnswers(
//        mtdItId = mtdItId,
//        taxYear = taxYear,
//        journey = journeyName, data = data, lastUpdated = Instant.ofEpochSecond(1))
//
//        when(mockConnector.set(journeyAnswers))
//        .thenReturn(Future.successful(Done))
//
//      whenReady(service.set(journeyAnswers)) { result =>
//        result shouldBe Done
//      }
//
//      verify(mockConnector).set(journeyAnswers)
//    }
//
//    "keep journey alive" in {
//      val mtdItId = "test-mtdItId"
//      val taxYear = 2023
//      val journeyName = "test-journey"
//
//      val journeyAnswers = Some(JourneyAnswers(
//        mtdItId = mtdItId,
//        taxYear = taxYear,
//        journey = journeyName, data = data, lastUpdated = Instant.ofEpochSecond(1)))
//
//      when(mockConnector.keepAlive(mtdItId, taxYear, journeyName))
//        .thenReturn(Future.successful(Done))
//
//      whenReady(service.keepAlive(mtdItId, taxYear, journeyName)) { result =>
//        result shouldBe Done
//      }
//
//      verify(mockConnector).keepAlive(mtdItId, taxYear, journeyName)
//    }
//  }
}

