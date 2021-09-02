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

package utils

import akka.actor.ActorSystem
import common.SessionValues
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import config.AppConfig
import controllers.predicates.AuthorisedAction
import helpers.{PlaySessionCookieBaker, WireMockHelper}
import models.User
import models.charity.GiftAidCYAModel
import models.charity.prior.{GiftAidPaymentsModel, GiftAidSubmissionModel, GiftsModel}
import models.priorDataModels.IncomeSourcesModel
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.http.HeaderNames
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.libs.ws.{BodyWritable, WSClient, WSRequest, WSResponse}
import play.api.libs.ws.{BodyWritable, WSClient, WSResponse}
import play.api.mvc.{AnyContent, MessagesControllerComponents, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{OK, await}
import play.api.{Application, Environment, Mode}
import services.AuthService
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.auth.core.{AffinityGroup, ConfidenceLevel, Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.http.SessionKeys
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.http.HeaderCarrier
import views.html.authErrorPages.AgentAuthErrorPageView

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable, ExecutionContext, Future}

trait IntegrationTest extends AnyWordSpecLike with Matchers with GuiceOneServerPerSuite with WireMockHelper with BeforeAndAfterAll {

  val year = 2022
  val nino = "AA123456A"
  val mtditid = "1234567890"
  val sessionId = "sessionId-eb3158c2-0aff-4ce8-8d1b-f2208ace52fe"
  val affinityGroup = "Individual"

  val xSessionId: (String, String) = "X-Session-ID" -> sessionId
  val csrfContent: (String, String) = "Csrf-Token" -> "nocheck"

  val completeGiftAidCYAModel = GiftAidCYAModel(
    donationsViaGiftAid = Some(true),
    donationsViaGiftAidAmount = Some(50),
    oneOffDonationsViaGiftAid = Some(true),
    oneOffDonationsViaGiftAidAmount = Some(50),
    overseasDonationsViaGiftAid = Some(true),
    overseasDonationsViaGiftAidAmount = Some(50),
    overseasCharityNames = Some(Seq("Dudes In Need")),
    addDonationToLastYear = Some(true),
    addDonationToLastYearAmount = Some(50),
    addDonationToThisYear = Some(true),
    addDonationToThisYearAmount = Some(50),
    donatedSharesSecuritiesLandOrProperty = Some(true),
    donatedSharesOrSecurities = Some(true),
    donatedSharesOrSecuritiesAmount = Some(50),
    donatedLandOrProperty = Some(true),
    donatedLandOrPropertyAmount = Some(50),
    overseasDonatedSharesSecuritiesLandOrProperty = Some(true),
    overseasDonatedSharesSecuritiesLandOrPropertyAmount = Some(50),
    overseasDonatedSharesSecuritiesLandOrPropertyCharityNames = Some(Seq("Awesome Devs initiative")),
  )

  val priorDataMax: GiftAidSubmissionModel = GiftAidSubmissionModel(
    Some(GiftAidPaymentsModel(
      Some(1111.00),
      Some(List("Jello Corporation")),
      Some(1222.00),
      Some(333.00),
      Some(444.00),
      Some(555.00)
    )),
    Some(GiftsModel(
      Some(666.00),
      Some(List("Simbas College Fund")),
      Some(777.00),
      Some(888.00)
    ))
  )

  implicit lazy val user: User[AnyContent] = new User[AnyContent](mtditid, None, nino, affinityGroup, sessionId)(FakeRequest())

  implicit val ec: ExecutionContext = ExecutionContext.Implicits.global
  implicit val headerCarrier: HeaderCarrier = HeaderCarrier().withExtraHeaders("mtditid" -> mtditid)

  implicit val actorSystem: ActorSystem = ActorSystem()

  val startUrl = s"http://localhost:$port/income-through-software/return/personal-income"
  val overviewUrl = "http://localhost:11111/income-through-software/return/2022/view"
  val cyaUrl: Int => String = taxYear => s"${controllers.charity.routes.GiftAidCYAController.show(taxYear)}"
  implicit def wsClient: WSClient = app.injector.instanceOf[WSClient]

  val appUrl = s"http://localhost:$port/income-through-software/return/personal-income"

  def await[T](awaitable: Awaitable[T]): T = Await.result(awaitable, Duration.Inf)

  def config: Map[String, Any] = Map(
    "auditing.enabled" -> false,
    "metrics.enabled" -> false,
    "play.filters.csrf.header.bypassHeaders.Csrf-Token" -> "nocheck",
    "microservice.services.income-tax-submission-frontend.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.income-tax-submission.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.auth.host" -> wiremockHost,
    "microservice.services.auth.port" -> wiremockPort,
    "microservice.services.income-tax-dividends.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.income-tax-interest.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.income-tax-gift-aid.url" -> s"http://$wiremockHost:$wiremockPort",
    "microservice.services.sign-in.url" -> s"/auth-login-stub/gg-sign-in",
    "taxYearChangeResetsSession" -> false
  )

  lazy val agentAuthErrorPage: AgentAuthErrorPageView = app.injector.instanceOf[AgentAuthErrorPageView]

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

  lazy val mcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  def urlGet(url: String, welsh: Boolean = false, follow: Boolean = true, headers: Seq[(String, String)] = Seq())(implicit wsClient: WSClient): WSResponse = {

    val newHeaders = if(welsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") ++ headers else headers
    await(wsClient.url(url).withFollowRedirects(follow).withHttpHeaders(newHeaders: _*).get())
  }

  def urlPost[T](url: String,
                 body: T,
                 welsh: Boolean = false,
                 follow: Boolean = true,
                 headers: Seq[(String, String)] = Seq())
                (implicit wsClient: WSClient, bodyWritable: BodyWritable[T]): WSResponse = {

    val headersWithNoCheck = headers ++ Seq("Csrf-Token" -> "nocheck")
    val newHeaders = if(welsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") ++ headersWithNoCheck else headersWithNoCheck
    await(wsClient.url(url).withFollowRedirects(follow).withHttpHeaders(newHeaders: _*).post(body))
  }

  def playSessionCookie(agent: Boolean = false, extraData: Map[String, String] = Map.empty): Seq[(String, String)] = {
    {
      if (agent) {
        Seq(HeaderNames.COOKIE -> PlaySessionCookieBaker.bakeSessionCookie(extraData ++ Map(
          SessionValues.CLIENT_NINO -> "AA123456A",
          SessionValues.CLIENT_MTDITID -> mtditid))
        )
      } else {
        Seq(HeaderNames.COOKIE -> PlaySessionCookieBaker.bakeSessionCookie(extraData), "mtditid" -> mtditid)
      }
    } ++
      Seq(xSessionId)
  }

  val defaultAcceptedConfidenceLevels = Seq(
    ConfidenceLevel.L200,
    ConfidenceLevel.L500
  )

  def authService(stubbedRetrieval: Future[_], acceptedConfidenceLevel: Seq[ConfidenceLevel]): AuthService = new AuthService(
    new MockAuthConnector(stubbedRetrieval, acceptedConfidenceLevel)
  )

  def authAction(
                  stubbedRetrieval: Future[_],
                  acceptedConfidenceLevel: Seq[ConfidenceLevel] = Seq.empty[ConfidenceLevel]
                ): AuthorisedAction = new AuthorisedAction(
    appConfig,
    agentAuthErrorPage
  )(
    authService(stubbedRetrieval, if (acceptedConfidenceLevel.nonEmpty) {
      acceptedConfidenceLevel
    } else {
      defaultAcceptedConfidenceLevels
    }),
    mcc
  )

  def successfulRetrieval: Future[Enrolments ~ Some[AffinityGroup] ~ ConfidenceLevel] = Future.successful(
    Enrolments(Set(
      Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "1234567890")), "Activated", None),
      Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", "AA123456A")), "Activated", None)
    )) and Some(AffinityGroup.Individual) and ConfidenceLevel.L200
  )

  def insufficientConfidenceRetrieval: Future[Enrolments ~ Some[AffinityGroup] ~ ConfidenceLevel] = Future.successful(
    Enrolments(Set(
      Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("MTDITID", "1234567890")), "Activated", None),
      Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", "AA123456A")), "Activated", None)
    )) and Some(AffinityGroup.Individual) and ConfidenceLevel.L50
  )

  def incorrectCredsRetrieval: Future[Enrolments ~ Some[AffinityGroup] ~ ConfidenceLevel] = Future.successful(
    Enrolments(Set(
      Enrolment("HMRC-MTD-IT", Seq(EnrolmentIdentifier("UTR", "1234567890")), "Activated", None),
      Enrolment("HMRC-NI", Seq(EnrolmentIdentifier("NINO", "AA123456A")), "Activated", None)
    )) and Some(AffinityGroup.Individual) and ConfidenceLevel.L200
  )

  def userDataStub(userData: IncomeSourcesModel, nino: String, taxYear: Int): StubMapping ={
    stubGetWithHeadersCheck(
      s"/income-tax-submission-service/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", OK,
      Json.toJson(userData).toString(),"X-Session-ID" -> sessionId, "mtditid" -> mtditid)
  }


  def emptyUserDataStub(nino: String = nino, taxYear: Int = year): StubMapping = {
    userDataStub(IncomeSourcesModel(), nino, taxYear)
  }
}
