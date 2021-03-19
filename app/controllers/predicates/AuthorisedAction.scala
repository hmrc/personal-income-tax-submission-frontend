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

package controllers.predicates

import common.{EnrolmentIdentifiers, EnrolmentKeys, SessionValues}
import config.AppConfig
import models.User
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results._
import play.api.mvc._
import services.AuthService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments, confidenceLevel}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter
import views.html.authErrorPages.AgentAuthErrorPageView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class AuthorisedAction @Inject()(
                                  appConfig: AppConfig,
                                  val agentAuthErrorPage: AgentAuthErrorPageView
                                )(
                                  implicit val authService: AuthService,
                                  val mcc: MessagesControllerComponents
                                ) extends ActionBuilder[User, AnyContent] with I18nSupport {

  implicit val executionContext: ExecutionContext = mcc.executionContext
  lazy val logger: Logger = Logger.apply(this.getClass)
  implicit val config: AppConfig = appConfig
  implicit val messagesApi: MessagesApi = mcc.messagesApi

  implicit def toFuture[T]: T => Future[T] = Future(_)

  override def parser: BodyParser[AnyContent] = mcc.parsers.default

  val minimumConfidenceLevel: Int = ConfidenceLevel.L200.level

  override def invokeBlock[A](request: Request[A], block: User[A] => Future[Result]): Future[Result] = {

    implicit lazy val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authService.authorised().retrieve(allEnrolments and affinityGroup and confidenceLevel) {
      case enrolments ~ Some(AffinityGroup.Agent) ~ _ =>
        checkAuthorisation(block, enrolments, isAgent = true)(request, headerCarrier)
      case enrolments ~ _ ~ confidenceLevel if confidenceLevel.level >= minimumConfidenceLevel =>
        checkAuthorisation(block, enrolments)(request, headerCarrier)
      case _ => Redirect(appConfig.incomeTaxSubmissionIvRedirect)
    } recover {
      case _: NoActiveSession =>
        logger.error(s"[AuthorisedAction][invokeBlock] - No active session. Redirecting to ${appConfig.signInUrl}")
        Redirect(appConfig.signInUrl)
      case _: AuthorisationException =>
        logger.error(s"[AuthorisedAction][invokeBlock] - Customer is not authorised to use this service.")
        Redirect(controllers.errors.routes.UnauthorisedUserErrorController.show())
    }
  }

  def checkAuthorisation[A](block: User[A] => Future[Result], enrolments: Enrolments, isAgent: Boolean = false)
                           (implicit request: Request[A], hc: HeaderCarrier): Future[Result] = {

    val neededKey = if (isAgent) EnrolmentKeys.Agent else EnrolmentKeys.Individual
    val neededIdentifier = if (isAgent) EnrolmentIdentifiers.agentReference else EnrolmentIdentifiers.individualId

    enrolmentGetIdentifierValue(neededKey, neededIdentifier, enrolments).fold[Future[Result]] {
      logger.error(s"[AuthorisedAction][checkAuthorisation] Relevant identifier missing. Agent: $isAgent")
      if (isAgent) {
        Future.successful(Redirect(controllers.errors.routes.YouNeedAgentServicesController.show()))
      } else {
        logger.error(s"[AuthorisedAction][checkAuthorisation] Relevant identifier missing. Agent: $isAgent")
        Redirect(controllers.errors.routes.IndividualAuthErrorController.show())
      }
    } { userId =>
      if (isAgent) agentAuthentication(block) else individualAuthentication(block, enrolments, userId)
    }

  }

  private[predicates] def agentAuthentication[A](block: User[A] => Future[Result])
                                                (implicit request: Request[A], hc: HeaderCarrier): Future[Result] = {

    val agentDelegatedAuthRuleKey = "mtd-it-auth"

    val agentAuthPredicate: String => Enrolment = identifierId =>
      Enrolment(EnrolmentKeys.Individual)
        .withIdentifier(EnrolmentIdentifiers.individualId, identifierId)
        .withDelegatedAuthRule(agentDelegatedAuthRuleKey)

    val mtditidOptional = request.session.get(SessionValues.CLIENT_MTDITID)
    val ninoOptional = request.session.get(SessionValues.CLIENT_NINO)

    (mtditidOptional, ninoOptional) match {
      case (Some(mtditid), Some(nino)) =>
        authService
          .authorised(agentAuthPredicate(mtditid))
          .retrieve(allEnrolments) { enrolments =>
            enrolmentGetIdentifierValue(EnrolmentKeys.Agent, EnrolmentIdentifiers.agentReference, enrolments) match {
              case Some(arn) =>
                block(User(mtditid, Some(arn),nino))
              case None =>
                logger.error("[AuthorisedAction][agentAuthentication] Agent with no HMRC-AS-AGENT enrolment. Rendering unauthorised view.")
                Future.successful(Redirect(controllers.errors.routes.YouNeedAgentServicesController.show()))
            }
          } recover {
          case _: NoActiveSession =>
            logger.error(s"AgentPredicate][agentAuthentication] - No active session. Redirecting to ${appConfig.signInUrl}")
            Redirect(appConfig.signInUrl)
          case ex: AuthorisationException =>
            logger.error(s"[AgentPredicate][agentAuthentication] - Agent does not have delegated authority for Client.")
            Unauthorized(agentAuthErrorPage())
        }
      case (_, None) =>
        logger.error("[AuthorisedAction][agentAuthentication] Agent expecting NINO in session, but NINO is missing. Redirecting to log in.")
        Redirect(appConfig.signInUrl)
      case (None, _) =>
        Unauthorized("No MTDITID in session.")
    }
  }

  private[predicates] def individualAuthentication[A](block: User[A] => Future[Result], enrolments: Enrolments, mtditid: String)
                                                     (implicit request: Request[A]): Future[Result] = {

    val ninoOptional = enrolmentGetIdentifierValue(EnrolmentKeys.nino, EnrolmentIdentifiers.nino, enrolments)

    ninoOptional match {
      case Some(nino) => block(User(mtditid, None, nino))
      case _ =>
        logger.error("[AuthorisedAction][individualAuthentication] ")
        Redirect(appConfig.signInUrl)
    }

  }

  private[predicates] def enrolmentGetIdentifierValue(
                                                       checkedKey: String,
                                                       checkedIdentifier: String,
                                                       enrolments: Enrolments
                                                     ): Option[String] = enrolments.enrolments.collectFirst {
    case Enrolment(`checkedKey`, enrolmentIdentifiers, _, _) => enrolmentIdentifiers.collectFirst {
      case EnrolmentIdentifier(`checkedIdentifier`, identifierValue) => identifierValue
    }
  }.flatten

}
