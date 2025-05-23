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

package utils

import com.codahale.metrics.SharedMetricRegistries
import common.{EnrolmentIdentifiers, EnrolmentKeys, SessionValues}
import config.{AppConfig, MockAppConfig}
import models.User
import models.session.SessionData
import org.apache.pekko.actor.ActorSystem
import org.scalamock.handlers.CallHandler4
import org.scalamock.scalatest.MockFactory
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test.{FakeRequest, Helpers}
import services.{AuthService, DividendsSessionService, GiftAidSessionService, InterestSessionService}
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait UnitTest extends AnyWordSpec with Matchers with MockFactory with BeforeAndAfterEach{

  class TestWithAuth(isAgent: Boolean = false, nino: Option[String] = Some("AA123456A")) {
    if(isAgent) mockAuthAsAgent() else mockAuth(nino)
  }

  override def beforeEach(): Unit = {
    super.beforeEach()
    SharedMetricRegistries.clear()
  }

  implicit val actorSystem: ActorSystem = ActorSystem()

  val sessionId = "eb3158c2-0aff-4ce8-8d1b-f2208ace52fe"
  val mtdItId: String = "1234567890"
  val nino: String = "AA123456A"
  val utr: String = "9999912345"
  val sessionData: SessionData = SessionData(sessionId, mtdItId, nino, Some(utr))

  def await[T](awaitable: Future[T]): T = Helpers.await(awaitable)

  lazy val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest().withHeaders("sessionId" -> sessionId)
  lazy val fakeRequestWithMtditidAndNino: FakeRequest[AnyContentAsEmpty.type] = fakeRequest.withSession(
    SessionValues.TAX_YEAR -> "2022",
    SessionValues.CLIENT_MTDITID -> mtdItId,
    SessionValues.CLIENT_NINO -> "A123456A"
  )
  val fakeRequestWithNino: FakeRequest[AnyContentAsEmpty.type] = fakeRequest.withSession(
    SessionValues.CLIENT_NINO -> "AA123456A"
  )
  implicit val emptyHeaderCarrier: HeaderCarrier = HeaderCarrier()

  implicit val mockAppConfig: AppConfig = new MockAppConfig
  implicit val mockControllerComponents: ControllerComponents = Helpers.stubControllerComponents()
  implicit val mockExecutionContext: ExecutionContext = ExecutionContext.Implicits.global
  implicit val mockAuthConnector: AuthConnector = mock[AuthConnector]
  implicit val mockAuthService: AuthService = new AuthService(mockAuthConnector)
  implicit val mockDividendsSessionService: DividendsSessionService = mock[DividendsSessionService]
  implicit val mockInterestSessionService: InterestSessionService = mock[InterestSessionService]
  implicit val mockGiftAidSessionService: GiftAidSessionService = mock[GiftAidSessionService]

  implicit lazy val mockMessagesControllerComponents: MessagesControllerComponents = Helpers.stubMessagesControllerComponents()
  implicit lazy val user: User[AnyContent] = new User[AnyContent](mtdItId, None, "AA123456A", "Individual", sessionId)(fakeRequest)


  def status(awaitable: Future[Result]): Int = await(awaitable).header.status

  def bodyOf(awaitable: Future[Result]): String = {
    val awaited = await(awaitable)
    await(awaited.body.consumeData.map(_.utf8String))
  }

  def redirectUrl(awaitable: Future[Result]): String = {
    await(awaitable).header.headers.getOrElse("Location", "/")
  }

  def getSession(awaitable: Future[Result]): Session = {
    await(awaitable).session
  }

  //noinspection ScalaStyle
  def mockAuth(nino: Option[String], returnedConfidenceLevel: ConfidenceLevel = ConfidenceLevel.L250): CallHandler4[Predicate, Retrieval[_], HeaderCarrier, ExecutionContext, Future[Any]] = {
    val enrolments = Enrolments(Set(
      Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtdItId)), "Activated"),
      Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, "0987654321")), "Activated")
    ) ++ nino.fold(Seq.empty[Enrolment])(unwrappedNino =>
      Seq(Enrolment(EnrolmentKeys.nino, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, unwrappedNino)), "Activated"))
    ))

    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, Retrievals.affinityGroup, *, *)
      .returning(Future.successful(Some(AffinityGroup.Individual)))

    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, Retrievals.allEnrolments and Retrievals.confidenceLevel, *, *)
      .returning(Future.successful(enrolments and returnedConfidenceLevel))
  }

  //noinspection ScalaStyle
  def mockAuthAsAgent(): CallHandler4[Predicate, Retrieval[_], HeaderCarrier, ExecutionContext, Future[Any]] = {
    val enrolments: Enrolments = Enrolments(Set(
      Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtdItId)), "Activated"),
      Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, "0987654321")), "Activated")
    ))

    val agentRetrievals: Some[AffinityGroup] = Some(AffinityGroup.Agent)

    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, Retrievals.affinityGroup, *, *)
      .returning(Future.successful(agentRetrievals))

    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, Retrievals.allEnrolments, *, *)
      .returning(Future.successful(enrolments))
  }

  //noinspection ScalaStyle
  def mockAuthReturnException(exception: Exception): CallHandler4[Predicate, Retrieval[_], HeaderCarrier, ExecutionContext, Future[Any]] = {
    (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(*, *, *, *)
      .returning(Future.failed(exception))
  }
}
