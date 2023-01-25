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

package services

import common.IncomeSources
import connectors.{IncomeSourceConnector, IncomeTaxUserDataConnector}
import connectors.httpParsers.IncomeTaxUserDataHttpParser.IncomeTaxUserDataResponse
import models.User
import models.charity.GiftAidCYAModel
import models.charity.prior.GiftAidSubmissionModel
import models.mongo.{DatabaseError, GiftAidUserDataModel}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.i18n.Lang.logger
import repositories.GiftAidUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class GiftAidSessionService @Inject()(
                                       giftAidUserDataRepository: GiftAidUserDataRepository,
                                       incomeTaxUserDataConnector: IncomeTaxUserDataConnector,
                                       incomeSourceConnector: IncomeSourceConnector
                                     ) {

  def getPriorData(taxYear: Int)(implicit user: User[_], hc: HeaderCarrier): Future[IncomeTaxUserDataResponse] = {
    incomeTaxUserDataConnector.getUserData(taxYear)(user, hc.withExtraHeaders("mtditid" -> user.mtditid))
  }

  def createSessionData[A](cyaModel: GiftAidCYAModel, taxYear: Int)(onFail: A)(onSuccess: A)
                          (implicit user: User[_], ec: ExecutionContext): Future[A] = {

    val userData = GiftAidUserDataModel(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear,
      Some(cyaModel),
      DateTime.now(DateTimeZone.UTC)
    )

    giftAidUserDataRepository.create(userData)().map {
      case Right(_) => onSuccess
      case Left(_) => onFail

    }
  }

  def getSessionData(taxYear: Int)(implicit user: User[_], ec: ExecutionContext): Future[Either[DatabaseError, Option[GiftAidUserDataModel]]] = {

    giftAidUserDataRepository.find(taxYear).map {
      case Left(value) =>
        logger.error("[GiftAidSessionService][getSessionData] Could not find user session.")
        Left(value)
      case Right(userData) => Right(userData)
    }
  }

  def updateSessionData[A](cyaModel: GiftAidCYAModel, taxYear: Int)(onFail: A)(onSuccess: A)
                          (implicit user: User[_], ec: ExecutionContext): Future[A] = {

    val userData = GiftAidUserDataModel(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear,
      Some(cyaModel),
      DateTime.now(DateTimeZone.UTC)
    )

    giftAidUserDataRepository.update(userData).map {
      case Right(_) => onSuccess
      case Left(_) => onFail
    }

  }

  def getAndHandle[R](taxYear: Int)(onFail: R)(block: (Option[GiftAidCYAModel], Option[GiftAidSubmissionModel]) => R)
                     (implicit executionContext: ExecutionContext, user: User[_], hc: HeaderCarrier): Future[R] = {
    for {
      optionalCya <- getSessionData(taxYear)
      priorDataResponse <- getPriorData(taxYear)
    } yield {
      priorDataResponse.map(_.giftAid) match {
        case Right(prior) => optionalCya match {
          case Left(_) => onFail
          case Right(cyaData) => block(cyaData.flatMap(_.giftAid), prior)
        }
        case Left(_) => onFail
      }
    }
  }

  def clear[R](taxYear: Int)(onFail: R)(onSuccess: R)(implicit user: User[_], ec: ExecutionContext, hc: HeaderCarrier): Future[R] = {
    incomeSourceConnector.put(taxYear, user.nino, IncomeSources.GIFT_AID)(hc.withExtraHeaders("mtditid" -> user.mtditid)).flatMap {
      case Left(_) => Future.successful(onFail)
      case _ =>
        giftAidUserDataRepository.clear(taxYear).map {
          case true => onSuccess
          case false => onFail
        }
    }
  }

}
