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

import com.github.tomakehurst.wiremock.client.WireMock._
import connectors.SectionCompletedConnector
import models.Journey
import models.mongo.JourneyAnswers
import models.mongo.JourneyStatus.Completed
import org.apache.pekko.Done
import play.api.libs.json.Json
import play.api.test.Helpers._
import test.utils.IntegrationTest

import java.time.Instant

class SectionCompletedServiceISpec extends IntegrationTest {

  val mockConnector: SectionCompletedConnector = app.injector.instanceOf[SectionCompletedConnector]

  val mtdItId = "1234567890"
  override val taxYear = 2023
  val journey = Journey.GiftAid
  val data = Json.obj("status" -> Completed.toString)
  val journeyAnswers = JourneyAnswers(
    mtdItId = mtdItId,
    taxYear = taxYear,
    journey = Journey.GiftAid.entryName,
    data = data,
    lastUpdated = Instant.ofEpochSecond(1)
  )

  val service: SectionCompletedService = new SectionCompletedService(mockConnector)

  "SectionCompletedService" should {

    "return the correct journey answers from the connector" in {
      stubFor(get(urlEqualTo(s"/income-tax-gift-aid/income-tax/journey-answers/$journey/$taxYear"))
        .withHeader("MTDITID", equalTo(mtdItId))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withBody(Json.toJson(journeyAnswers).toString())
        )
      )

      val result = await(service.get(mtdItId, taxYear, journey))
      result shouldBe Some(journeyAnswers)

      verify(getRequestedFor(urlEqualTo(s"/income-tax-gift-aid/income-tax/journey-answers/$journey/$taxYear"))
        .withHeader("MTDITID", equalTo(mtdItId)))
    }

    "set journey answers successfully via the connector" in {
      stubFor(post(urlEqualTo("/income-tax-gift-aid/income-tax/journey-answers"))
        .withHeader("MTDITID", equalTo(mtdItId))
        .withRequestBody(equalTo(Json.toJson(journeyAnswers).toString()))
        .willReturn(aResponse().withStatus(NO_CONTENT))
      )

      val result = await(service.set(journeyAnswers, journey))
      result shouldBe Done

      verify(postRequestedFor(urlEqualTo("/income-tax-gift-aid/income-tax/journey-answers"))
        .withHeader("MTDITID", equalTo(mtdItId))
        .withRequestBody(equalTo(Json.toJson(journeyAnswers).toString())))
    }

    "keep journey alive successfully via the connector" in {
      stubFor(post(urlEqualTo(s"/income-tax-gift-aid/income-tax/journey-answers/keep-alive/$journey/$taxYear"))
        .withHeader("MTDITID", equalTo(mtdItId))
        .willReturn(aResponse().withStatus(NO_CONTENT))
      )

      val result = await(service.keepAlive(mtdItId, taxYear, journey))
      result shouldBe Done

      verify(postRequestedFor(urlEqualTo(s"/income-tax-gift-aid/income-tax/journey-answers/keep-alive/$journey/$taxYear"))
        .withHeader("MTDITID", equalTo(mtdItId)))
    }
  }
}