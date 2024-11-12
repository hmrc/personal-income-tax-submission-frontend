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

package connectors

import config.{AppConfig, FrontendAppConfig}
import models.Journey
import models.mongo.JourneyAnswers
import models.mongo.JourneyStatus.Completed
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.Status._
import play.api.libs.json.Json
import test.utils.IntegrationTest
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.time.Instant

class SectionCompletedConnectorISpec extends IntegrationTest {

  implicit private lazy val hc: HeaderCarrier = HeaderCarrier()
  private lazy val connector: SectionCompletedConnector = app.injector.instanceOf[SectionCompletedConnector]
  lazy val httpClient: HttpClientV2 = app.injector.instanceOf[HttpClientV2]

  def appConfig(host: String): AppConfig = new FrontendAppConfig(app.injector.instanceOf[ServicesConfig]) {
    override lazy val giftAidBaseUrl: String = s"http://$host:$wiremockPort/income-tax-gift-aid"
    override lazy val dividendsBaseUrl: String = s"http://$host:$wiremockPort/income-tax-dividends"
  }

  private def keepAliveUrl(journey: String, taxYear: Int) =
    s"/income-tax-gift-aid/income-tax/journey-answers/keep-alive/$journey/$taxYear"

  private def completedSectionUrl(journey: String, taxYear: Int) =
    s"/income-tax-gift-aid/income-tax/journey-answers/$journey/$taxYear"


  private val mtditId: String = "1234567890"
  val journeyName: Journey = Journey.GiftAid
  val data = Json.obj(
    "status" -> Completed.toString
  )
  private val answers = JourneyAnswers(mtditId, taxYear, journeyName.entryName, data, lastUpdated = Instant.ofEpochSecond(1))

  s".get" when {

    "request is made, return user answers when the server returns them" in {

      stubGet(s"${completedSectionUrl(journeyName.entryName, taxYear)}", OK, Json.toJson(answers).toString)


      val result = connector.get(mtditId, taxYear, journeyName).futureValue

      result.value mustEqual answers
    }

    "must return None when the server returns NOT_FOUND" in {

      stubGet(s"${completedSectionUrl(journeyName.entryName, taxYear)}", NOT_FOUND, "{}")

      val result = connector.get(mtditId, taxYear, journeyName).futureValue

      result must not be defined
    }

    "must return a failed future when the server returns an error" in {

      stubGet(s"${completedSectionUrl(journeyName.entryName, taxYear)}", INTERNAL_SERVER_ERROR, "{}")

      connector.get(mtditId, taxYear, journeyName).failed.futureValue
    }

    "must return a failed future when the server returns an unexpected response" in {

      stubGet(s"${completedSectionUrl(journeyName.entryName, taxYear)}", OK, "{}")

      connector.get(mtditId, taxYear, journeyName).failed.futureValue
    }
  }

  ".set" when {

    "must post user answers to the server" in {

      stubPost(s"/income-tax-gift-aid/income-tax/journey-answers", NO_CONTENT, "{}")


      connector.set(answers, journeyName).futureValue
    }

    "must return a failed future when the server returns error" in {
      stubPost(s"/income-tax-gift-aid/income-tax/journey-answers", INTERNAL_SERVER_ERROR, "{}")


      connector.set(answers, journeyName).failed.futureValue
    }

    "must return a failed future when the server returns an unexpected response code" in {
      stubPost(s"/income-tax-gift-aid/income-tax/journey-answers", OK, "{}")


      connector.set(answers, journeyName).failed.futureValue
    }
  }

  ".keepAlive" when {

    "must post to the server" in {
      stubPost(s"/income-tax-gift-aid/income-tax/journey-answers/keep-alive/$journeyName/$taxYear", NO_CONTENT, "{}")

      connector.keepAlive(mtditId, taxYear, journeyName).futureValue
    }

    "must return a failed future when the server returns error" in {

      stubPost(s"/income-tax-gift-aid/income-tax/journey-answers/keep-alive/$journeyName/$taxYear", INTERNAL_SERVER_ERROR, "{}")


      connector.keepAlive(mtditId, taxYear, journeyName).failed.futureValue
    }

    "must return a failed future when the server returns an unexpected response code" in {

      stubPost(s"/income-tax-gift-aid/income-tax/journey-answers/keep-alive/$journeyName/$taxYear", OK, "{}")


      connector.keepAlive(mtditId, taxYear, journeyName).failed.futureValue
    }
  }

}
