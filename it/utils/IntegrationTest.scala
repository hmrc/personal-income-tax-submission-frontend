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

package utils

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import config.AppConfig
import helpers.WireMockHelper
import models.User
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.{AnyContent, Result}
import play.api.{Application, Environment, Mode}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable, ExecutionContext, Future}

trait IntegrationTest extends AnyWordSpec with Matchers with GuiceOneServerPerSuite with WireMockHelper with BeforeAndAfterAll{

  implicit lazy val user: User[AnyContent] = new User[AnyContent]("1234567890", None)

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val emptyHeaderCarrier: HeaderCarrier = HeaderCarrier()

  implicit val actorSystem: ActorSystem = ActorSystem()
  implicit val materializer: Materializer = ActorMaterializer()

  def await[T](awaitable: Awaitable[T]): T = Await.result(awaitable, Duration.Inf)

  def config: Map[String, String] = Map(
    "auditing.enabled" -> "false",
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "microservice.services.income-tax-submission-frontend.host" -> wiremockHost,
    "microservice.services.income-tax-submission-frontend.port" -> wiremockPort.toString,
    "microservice.services.auth.host" -> wiremockHost,
    "microservice.services.auth.port" -> wiremockPort.toString
  )

  override implicit lazy val app: Application = new GuiceApplicationBuilder()
    .in(Environment.simple(mode = Mode.Dev))
    .configure(config)
    .build

  implicit lazy val appConfig: AppConfig = app.injector.instanceOf[AppConfig]

  override def beforeAll(): Unit = {
    super.beforeAll()
    startWiremock()
  }

  override def afterAll(): Unit = {
    stopWiremock()
    super.afterAll()
  }

  def status(awaitable: Future[Result]): Int = await(awaitable).header.status

  def bodyOf(awaitable: Future[Result]): String = {
    val awaited = await(awaitable)
    await(awaited.body.consumeData.map(_.utf8String))
  }

}
