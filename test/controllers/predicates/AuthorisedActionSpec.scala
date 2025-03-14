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

package controllers.predicates

import common.{EnrolmentIdentifiers, EnrolmentKeys, SessionValues}
import config.{AppConfig, MockErrorHandler}
import models.User
import org.scalamock.handlers.{CallHandler0, CallHandler4}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.http.Status._
import play.api.mvc.Results._
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.status
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{allEnrolments, confidenceLevel}
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.play.bootstrap.tools.Stubs.stubMessagesControllerComponents
import utils.UnitTest
import views.html.authErrorPages.AgentAuthErrorPageView

import scala.concurrent.{ExecutionContext, Future}

class AuthorisedActionSpec extends UnitTest with GuiceOneAppPerSuite with MockErrorHandler {

  val agentAuthErrorPageView: AgentAuthErrorPageView = app.injector.instanceOf[AgentAuthErrorPageView]
  val authorisedAction = new AuthorisedAction(mockAppConfig, agentAuthErrorPageView, mockErrorHandler)(mockAuthService, stubMessagesControllerComponents())

  val auth: AuthorisedAction = authorisedAction
  val nino: String = "AA123456A"

  trait AgentTest {
    val arn: String = "0987654321"

    val baseUrl = "/update-and-submit-income-tax-return/personal-income"

    val validHeaderCarrier: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionId")))

    val testBlock: User[AnyContent] => Future[Result] = user => Future.successful(Ok(s"${user.mtditid} ${user.arn.get}"))

    val mockAppConfig: AppConfig = mock[AppConfig]

    val viewAndChangeUrl: String = "/report-quarterly/income-and-expenses/view/agents/client-utr"
    val signInUrl: String = s"$baseUrl/signIn"

    def primaryAgentPredicate(mtdId: String): Predicate =
      Enrolment("HMRC-MTD-IT")
        .withIdentifier("MTDITID", mtdId)
        .withDelegatedAuthRule("mtd-it-auth")

    def secondaryAgentPredicate(mtdId: String): Predicate =
      Enrolment("HMRC-MTD-IT-SUPP")
        .withIdentifier("MTDITID", mtdId)
        .withDelegatedAuthRule("mtd-it-auth-supp")

    val primaryAgentEnrolment: Enrolments = Enrolments(Set(
      Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtdItId)), "Activated"),
      Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, arn)), "Activated")
    ))

    val supportingAgentEnrolment: Enrolments = Enrolments(Set(
      Enrolment(EnrolmentKeys.Supporting, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtdItId)), "Activated"),
      Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, arn)), "Activated")
    ))

    def mockAuthReturnException(exception: Exception,
                                predicate: Predicate): CallHandler4[Predicate, Retrieval[_], HeaderCarrier, ExecutionContext, Future[Any]] =
      (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
        .expects(predicate, *, *, *)
        .returning(Future.failed(exception))

    def mockAuthReturn(enrolments: Enrolments, predicate: Predicate): CallHandler4[Predicate, Retrieval[_], HeaderCarrier, ExecutionContext, Future[Any]] =
      (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
      .expects(predicate, *, *, *)
      .returning(Future.successful(enrolments))

    def mockSignInUrl(): CallHandler0[String] =
      (mockAppConfig.signInUrl _: () => String)
        .expects()
        .returning(signInUrl)
        .anyNumberOfTimes()

    def mockViewAndChangeUrl(): CallHandler0[String] =
      (mockAppConfig.viewAndChangeEnterUtrUrl _: () => String)
        .expects()
        .returning(viewAndChangeUrl)
        .anyNumberOfTimes()

    def testAuth: AuthorisedAction = {
      mockViewAndChangeUrl()
      mockSignInUrl()

      new AuthorisedAction(
        appConfig = mockAppConfig,
        agentAuthErrorPage = agentAuthErrorPageView,
        errorHandler = mockErrorHandler
      )(
        authService = mockAuthService,
        mcc = stubMessagesControllerComponents()
      )
    }

    lazy val fakeRequestWithMtditidAndNino: FakeRequest[AnyContentAsEmpty.type] = fakeRequest.withSession(
      SessionValues.TAX_YEAR -> "2022",
      SessionValues.CLIENT_MTDITID -> mtdItId,
      SessionValues.CLIENT_NINO -> nino
    )
  }

  ".enrolmentGetIdentifierValue" should {
    "return the value for the given identifier" in {
      val returnValue = "anIdentifierValue"
      val returnValueAgent = "anAgentIdentifierValue"

      val enrolments = Enrolments(Set(
        Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, returnValue)), "Activated"),
        Enrolment(EnrolmentKeys.Agent, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.agentReference, returnValueAgent)), "Activated")
      ))

      auth.enrolmentGetIdentifierValue(EnrolmentKeys.Individual, EnrolmentIdentifiers.individualId, enrolments) shouldBe Some(returnValue)
      auth.enrolmentGetIdentifierValue(EnrolmentKeys.Agent, EnrolmentIdentifiers.agentReference, enrolments) shouldBe Some(returnValueAgent)
    }

    "return a None" when {
      val key = "someKey"
      val identifierKey = "anIdentifier"
      val returnValue = "anIdentifierValue"

      val enrolments = Enrolments(Set(Enrolment(key, Seq(EnrolmentIdentifier(identifierKey, returnValue)), "someState")))


      "the given identifier cannot be found" in {
        auth.enrolmentGetIdentifierValue(key, "someOtherIdentifier", enrolments) shouldBe None
      }

      "the given key cannot be found" in {
        auth.enrolmentGetIdentifierValue("someOtherKey", identifierKey, enrolments) shouldBe None
      }

    }

  }

  ".individualAuthentication" should {
    "perform the block action" when {
      "the correct enrolment exist" which {
        val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
        val mtditid = "AAAAAA"
        val enrolments = Enrolments(Set(
          Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated"),
          Enrolment(EnrolmentKeys.nino, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, nino)), "Activated")
        ))

        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, allEnrolments and confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L250))
          auth.individualAuthentication[AnyContent](block, AffinityGroup.Individual)(fakeRequest, emptyHeaderCarrier)
        }

        "returns an OK status" in {
          status(result) shouldBe OK
        }

        "returns a body of the mtditid" in {
          bodyOf(result) shouldBe mtditid
        }
      }

    }

    "return a redirect" when {

      "the nino enrolment is missing" which {
        val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
        val enrolments = Enrolments(Set())

        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, allEnrolments and confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L250))
          auth.individualAuthentication[AnyContent](block, AffinityGroup.Individual)(fakeRequest, emptyHeaderCarrier)
        }

        "returns a forbidden" in {
          status(result) shouldBe SEE_OTHER
        }
      }

      "the individual enrolment is missing but there is a nino" which {
        val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
        val nino = "AA123456A"
        val enrolments = Enrolments(Set(Enrolment("HMRC-NI", Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, nino)), "Activated")))

        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, allEnrolments and confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L250))
          auth.individualAuthentication[AnyContent](block, AffinityGroup.Individual)(fakeRequest, emptyHeaderCarrier)
        }

        "returns an Unauthorised" in {
          status(result) shouldBe SEE_OTHER
        }
        "returns an redirect to the correct page" in {
          redirectUrl(result) shouldBe "/update-and-submit-income-tax-return/personal-income/error/you-need-to-sign-up"
        }
      }

      "there is no session id" which {
        val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
        val mtditid = "AAAAAA"
        val enrolments = Enrolments(Set(
          Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated"),
          Enrolment(EnrolmentKeys.nino, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, nino)), "Activated")
        ))

        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, allEnrolments and confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L250))
          auth.individualAuthentication[AnyContent](block, AffinityGroup.Individual)(FakeRequest(), emptyHeaderCarrier)
        }

        "returns an OK status" in {
          status(result) shouldBe SEE_OTHER
        }

        "returns a body of the mtditid" in {
          redirectUrl(result) shouldBe "/signIn"
        }
      }
    }

    "return the user to IV Uplift" when {

      "the confidence level is below minimum" which {
        val block: User[AnyContent] => Future[Result] = user => Future.successful(Ok(user.mtditid))
        val mtditid = mtdItId
        val enrolments = Enrolments(Set(
          Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtditid)), "Activated"),
          Enrolment(EnrolmentKeys.nino, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.nino, "AA123456A")), "Activated")
        ))

        lazy val result: Future[Result] = {
          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, allEnrolments and confidenceLevel, *, *)
            .returning(Future.successful(enrolments and ConfidenceLevel.L50))
          auth.individualAuthentication[AnyContent](block, AffinityGroup.Individual)(fakeRequest, emptyHeaderCarrier)
        }

        "has a status of 303" in {
          status(result) shouldBe SEE_OTHER
        }

        "redirects to the iv url" in {
          await(result).header.headers("Location") shouldBe "/update-and-submit-income-tax-return/iv-uplift"
        }
      }
    }
  }

  ".agentAuthenticated" when {
    "MTD ID and/or NINO are not found in the session" should {
      "return a redirect to View and Change service" in new AgentTest {
        val result: Future[Result] = testAuth.agentAuthentication(testBlock)(
          request = FakeRequest().withSession(fakeRequest.session.data.toSeq :_*),
          hc = emptyHeaderCarrier
        )

        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe viewAndChangeUrl
      }
    }

    "NINO and MTD IT ID are present in the session" which {
      "results in a NoActiveSession error to be returned from Auth" should {
        "return a redirect to the login page" in new AgentTest {
          object AuthException extends NoActiveSession("Some reason")
          mockAuthReturnException(AuthException, primaryAgentPredicate(mtdItId))

          val result: Future[Result] = testAuth.agentAuthentication(testBlock)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = emptyHeaderCarrier
          )

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe s"$baseUrl/signIn"
        }
      }

      "results in an Exception other than an AuthException error being returned for Primary Agent check" should {
        "render an ISE page" in new AgentTest {
          mockAuthReturnException(new Exception("bang"), primaryAgentPredicate(mtdItId))
          mockFutureInternalServerError()

          val result: Future[Result] = testAuth.agentAuthentication(testBlock)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = emptyHeaderCarrier
          )

          status(result) shouldBe INTERNAL_SERVER_ERROR
          bodyOf(result) shouldBe "There is a problem."
        }
      }

      "results in an AuthorisationException error being returned from Auth" should {
        "render an ISE page when secondary agent auth call also fails with non-Auth exception" in new AgentTest {
          mockAuthReturnException(InsufficientEnrolments(), primaryAgentPredicate(mtdItId))
          mockAuthReturnException(new Exception("bang"), secondaryAgentPredicate(mtdItId))
          mockInternalServerError()

          lazy val result: Future[Result] = testAuth.agentAuthentication(testBlock)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = emptyHeaderCarrier
          )

          status(result) shouldBe INTERNAL_SERVER_ERROR
          bodyOf(result) shouldBe "There is a problem."
        }

        "return a redirect to the agent error page when secondary agent auth call also fails" in new AgentTest {
          object AuthException extends AuthorisationException("Some reason")
          mockAuthReturnException(AuthException, primaryAgentPredicate(mtdItId))
          mockAuthReturnException(AuthException, secondaryAgentPredicate(mtdItId))

          lazy val result: Future[Result] = testAuth.agentAuthentication(testBlock)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = emptyHeaderCarrier
          )

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe s"$baseUrl/error/you-need-client-authorisation"
        }

        "handle appropriately when a supporting agent is properly authorised" in new AgentTest {
          object AuthException extends AuthorisationException("Some reason")
          mockAuthReturnException(AuthException, primaryAgentPredicate(mtdItId))
          mockAuthReturn(supportingAgentEnrolment, secondaryAgentPredicate(mtdItId))

          lazy val result: Future[Result] = testAuth.agentAuthentication(testBlock)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = validHeaderCarrier
          )

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe s"$baseUrl/error/supporting-agent-not-authorised"
        }
      }

      "results in successful authorisation for a primary agent" should {
        "return a redirect to You Need Agent Services page when an ARN cannot be found" in new AgentTest {
          val primaryAgentEnrolmentNoArn: Enrolments = Enrolments(Set(
            Enrolment(EnrolmentKeys.Individual, Seq(EnrolmentIdentifier(EnrolmentIdentifiers.individualId, mtdItId)), "Activated"),
            Enrolment(EnrolmentKeys.Agent, Seq.empty, "Activated")
          ))

          mockAuthReturn(primaryAgentEnrolmentNoArn, primaryAgentPredicate(mtdItId))

          lazy val result: Future[Result] = testAuth.agentAuthentication(testBlock)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = validHeaderCarrier
          )

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe s"$baseUrl/error/you-need-agent-services-account"
        }

        "return a redirect to Sign In page when a session ID cannot be found" in new AgentTest {
          mockAuthReturn(primaryAgentEnrolment, primaryAgentPredicate(mtdItId))

          lazy val result: Future[Result] = testAuth.agentAuthentication(testBlock)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = emptyHeaderCarrier
          )

          status(result) shouldBe SEE_OTHER
          redirectUrl(result) shouldBe s"$baseUrl/signIn"
        }

        "invoke block when the user is properly authenticated" in new AgentTest {
          mockAuthReturn(primaryAgentEnrolment, primaryAgentPredicate(mtdItId))

          lazy val result: Future[Result] = testAuth.agentAuthentication(testBlock)(
            request = FakeRequest().withSession(fakeRequestWithMtditidAndNino.session.data.toSeq :_*),
            hc = validHeaderCarrier
          )

          status(result) shouldBe OK
          bodyOf(result) shouldBe s"$mtdItId $arn"
        }
      }
    }
  }

  ".invokeBlock" should {

    lazy val block: User[AnyContent] => Future[Result] = user =>
      Future.successful(Ok(s"mtditid: ${user.mtditid}${user.arn.fold("")(arn => " arn: " + arn)}"))

    "perform the block action" when {

      "the user is successfully verified as an agent" which {

        lazy val result = {
          mockAuthAsAgent()
          auth.invokeBlock(fakeRequestWithMtditidAndNino, block)
        }

        "should return an OK(200) status" in {
          status(result) shouldBe OK
          bodyOf(result) shouldBe s"mtditid: $mtdItId arn: 0987654321"
        }
      }

      "the user is successfully verified as an individual" in {

        lazy val result = {
          mockAuth(Some("AA123456A"))
          auth.invokeBlock(fakeRequest, block)
        }

        status(result) shouldBe OK

        bodyOf(result) shouldBe s"mtditid: $mtdItId"
      }
    }

    "return a redirect" when {

      "the authorisation service returns an AuthorisationException exception" in {
        object AuthException extends AuthorisationException("Some reason")

        lazy val result = {
          mockAuthReturnException(AuthException)
          auth.invokeBlock(fakeRequest, block)
        }
        status(result) shouldBe SEE_OTHER
      }

      "there is no MTDITID value in session for an agent" in {
        lazy val result = {

          (mockAuthConnector.authorise(_: Predicate, _: Retrieval[_])(_: HeaderCarrier, _: ExecutionContext))
            .expects(*, Retrievals.affinityGroup, *, *)
            .returning(Future.successful(Some(AffinityGroup.Agent)))

          auth.invokeBlock(fakeRequestWithNino, block)
        }
        status(result) shouldBe SEE_OTHER
        redirectUrl(result) shouldBe "/report-quarterly/income-and-expenses/view/agents/client-utr"
      }

    }

    "redirect to the sign in page" when {
      "the authorisation service returns a NoActiveSession exception" in {
        object NoActiveSession extends NoActiveSession("Some reason")

        lazy val result = {
          mockAuthReturnException(NoActiveSession)
          auth.invokeBlock(fakeRequest, block)
        }

        status(result) shouldBe SEE_OTHER
      }
    }

    "render ISE" when {

      "Any other type of unexpected exception is caught in the recovery block" in {

        mockAuthReturnException(new Exception("bang"))
        mockInternalServerError()

        val result = auth.invokeBlock(fakeRequest, block)

        status(result) shouldBe INTERNAL_SERVER_ERROR
        bodyOf(result) shouldBe "There is a problem."
      }
    }
  }
}

