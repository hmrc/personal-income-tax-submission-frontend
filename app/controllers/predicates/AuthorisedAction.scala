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
import javax.inject.Inject
import models.User
import play.api.Logger
import play.api.mvc.Results._
import play.api.mvc._
import services.AuthService
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals.{affinityGroup, allEnrolments}
import uk.gov.hmrc.auth.core.retrieve.~
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.HeaderCarrierConverter

import scala.concurrent.{ExecutionContext, Future}

class AuthorisedAction @Inject()(
                                  appConfig: AppConfig
                                )(
                                  implicit val authService: AuthService,
                                  val mcc: MessagesControllerComponents
                                ) extends ActionBuilder[User, AnyContent] {

  implicit val executionContext: ExecutionContext = mcc.executionContext
  lazy val logger: Logger = Logger.apply(this.getClass)

  override def parser: BodyParser[AnyContent] = mcc.parsers.default

  override def invokeBlock[A](request: Request[A], block: User[A] => Future[Result]): Future[Result] = {

    implicit lazy val headerCarrier: HeaderCarrier = HeaderCarrierConverter.fromHeadersAndSession(request.headers, Some(request.session))

    authService.authorised(ConfidenceLevel.L200).retrieve(allEnrolments and affinityGroup) {
      case enrolments ~ Some(AffinityGroup.Agent) =>
        checkAuthorisation(block, enrolments, isAgent = true)(request, headerCarrier)
      case enrolments ~ _ =>
        checkAuthorisation(block, enrolments)(request, headerCarrier)
    } recover {
      case _: NoActiveSession =>
        logger.debug(s"AgentPredicate][authoriseAsAgent] - No active session. Redirecting to ${appConfig.signInUrl}")
        Redirect(appConfig.signInUrl) //TODO Check this is the correct location
      case _: AuthorisationException =>
        logger.debug(s"[AgentPredicate][authoriseAsAgent] - Agent does not have delegated authority for Client.")
        Unauthorized("") //TODO Redirect to unauthorised page
    }
  }

  def checkAuthorisation[A](block: User[A] => Future[Result], enrolments: Enrolments, isAgent: Boolean = false)
                           (implicit request: Request[A], hc: HeaderCarrier): Future[Result] = {

    val neededKey = if (isAgent) EnrolmentKeys.Agent else EnrolmentKeys.Individual
    val neededIdentifier = if (isAgent) EnrolmentIdentifiers.agentReference else EnrolmentIdentifiers.individualId

    enrolmentGetIdentifierValue(neededKey, neededIdentifier, enrolments).fold{
      Future.successful(Unauthorized("No relevant identifier. Is agent: " + isAgent))
    } { userId =>
      if (isAgent) agentAuthentication(block, userId) else individualAuthentication(block, enrolments, userId)
    }

  }

  private[predicates] def agentAuthentication[A](block: User[A] => Future[Result], userId: String)
                                                (implicit request: Request[A], hc: HeaderCarrier): Future[Result] = {

    val agentDelegatedAuthRuleKey = "mtd-it-auth"

    val agentAuthPredicate: String => Enrolment = identifierId =>
      Enrolment(EnrolmentKeys.Individual)
        .withIdentifier(EnrolmentIdentifiers.individualId, identifierId)
        .withDelegatedAuthRule(agentDelegatedAuthRuleKey)

    request.session.get(SessionValues.CLIENT_MTDITID) match {
      case Some(mtditid) =>
        authService
          .authorised(agentAuthPredicate(mtditid) and ConfidenceLevel.L200)
          .retrieve(allEnrolments) { enrolments =>

          enrolmentGetIdentifierValue(EnrolmentKeys.Agent, EnrolmentIdentifiers.agentReference, enrolments) match {
            case Some(arn) =>
              block(User(mtditid, Some(arn)))
            case None =>
              logger.debug("[AuthorisedAction][CheckAuthorisation] Agent with no HMRC-AS-AGENT enrolment. Rendering unauthorised view.")
              Future.successful(Forbidden("")) //TODO add agent unauthorised page
          }
        } recover {
          case _: NoActiveSession =>
            logger.debug(s"AgentPredicate][authoriseAsAgent] - No active session. Redirecting to ${appConfig.signInUrl}")
            Redirect(appConfig.signInUrl) //TODO Check this is the correct location
          case ex: AuthorisationException =>
            logger.debug(s"[AgentPredicate][authoriseAsAgent] - Agent does not have delegated authority for Client.")
            Unauthorized("") //TODO Redirect to unauthorised page
        }
      case None =>
        Future.successful(Unauthorized("No MTDITID in session."))
    }
  }

  private[predicates] def individualAuthentication[A](block: User[A] => Future[Result], enrolments: Enrolments, mtditid: String)
                                                     (implicit request: Request[A], hc: HeaderCarrier): Future[Result] = {
    enrolments.enrolments.collectFirst {
      case Enrolment(EnrolmentKeys.Individual, enrolmentIdentifiers, _, _)
        if enrolmentIdentifiers.exists(identifier => identifier.key == EnrolmentIdentifiers.individualId) =>
        block(User(mtditid, None))
    } getOrElse {
      logger.warn("[AuthorisedAction][IndividualAuthentication] Non-agent with an invalid MTDITID.")
      Future.successful(Forbidden("")) //TODO send to an unauthorised page
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
