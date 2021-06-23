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

package services

import common.InterestTaxTypes
import connectors.IncomeTaxUserDataConnector
import connectors.httpParsers.IncomeTaxUserDataHttpParser.IncomeTaxUserDataResponse
import models.User
import models.interest.{InterestAccountModel, InterestCYAModel, InterestPriorSubmission}
import models.mongo.InterestUserDataModel
import models.priorDataModels.InterestModel
import org.joda.time.{DateTime, DateTimeZone}
import repositories.InterestUserDataRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class InterestSessionService @Inject()(
                                        interestUserDataRepository: InterestUserDataRepository,
                                        incomeTaxUserDataConnector: IncomeTaxUserDataConnector
                                      ) {

  def getPriorData(taxYear: Int)(implicit user: User[_], hc: HeaderCarrier): Future[IncomeTaxUserDataResponse] = {
    incomeTaxUserDataConnector.getUserData(taxYear)(user, hc.withExtraHeaders("mtditid" -> user.mtditid))
  }

  def createSessionData[A](cyaModel: InterestCYAModel, taxYear: Int)(onFail: A)(onSuccess: A)
                          (implicit user: User[_], ec: ExecutionContext): Future[A] = {

    val userData = InterestUserDataModel(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear,
      Some(cyaModel),
      DateTime.now(DateTimeZone.UTC)
    )

    interestUserDataRepository.create(userData).map {
      case true => onSuccess
      case false => onFail
    }
  }

  def getSessionData(taxYear: Int)(implicit user: User[_]): Future[Option[InterestUserDataModel]] = {
    interestUserDataRepository.find(taxYear)
  }

  def updateSessionData[A](cyaModel: InterestCYAModel, taxYear: Int)(onFail: A)(onSuccess: A)
                          (implicit user: User[_], ec: ExecutionContext): Future[A] = {

    val userData = InterestUserDataModel(
      user.sessionId,
      user.mtditid,
      user.nino,
      taxYear,
      Some(cyaModel),
      DateTime.now(DateTimeZone.UTC)
    )

    interestUserDataRepository.update(userData).map {
      case true => onSuccess
      case false => onFail
    }
  }

  private[services] def interestModelToInterestAccount(input: InterestModel, taxType: String): Option[InterestAccountModel] = {
    if (taxType == "untaxed") {
      input.untaxedUkInterest.map { amount =>
        InterestAccountModel(
          Some(input.incomeSourceId),
          input.accountName,
          amount,
          priorType = Some(InterestTaxTypes.UNTAXED)
        )
      }
    } else {
      input.taxedUkInterest.map { amount =>
        InterestAccountModel(
          Some(input.incomeSourceId),
          input.accountName,
          amount,
          priorType = Some(InterestTaxTypes.TAXED)
        )
      }
    }

  }

  def getAndHandle[R](taxYear: Int)(onFail: R)(block: (Option[InterestCYAModel], Option[InterestPriorSubmission]) => R)
                     (implicit executionContext: ExecutionContext, user: User[_], hc: HeaderCarrier): Future[R] = {
    for {
      optionalCya <- getSessionData(taxYear)
      priorDataResponse <- getPriorData(taxYear)
    } yield {
      val priorUntaxed: Seq[InterestAccountModel] = priorDataResponse.map(_.interest.map(_.filter(_.untaxedUkInterest.nonEmpty))).right
        .getOrElse(None)
        .getOrElse(Seq.empty[InterestModel])
        .flatMap(interestModelToInterestAccount(_, "untaxed"))

      priorDataResponse.map(_.interest.map(_.filter(_.taxedUkInterest.nonEmpty))) match {
        case Right(prior) =>
          val priorTaxed = prior
            .getOrElse(Seq.empty[InterestModel])
            .flatMap(interestModelToInterestAccount(_, "taxed"))

          val priorDataManipulation = priorDataResponse.map { prior =>
            Some(InterestPriorSubmission(
              prior.interest.map(_.filter(_.untaxedUkInterest.nonEmpty)).exists(_.nonEmpty),
              prior.interest.map(_.filter(_.taxedUkInterest.nonEmpty)).exists(_.nonEmpty),
              Some(priorUntaxed ++ priorTaxed)
            ))
          }.right.getOrElse(None)

          block(optionalCya.flatMap(_.interest), priorDataManipulation)
        case Left(_) => onFail
      }
    }
  }

  def clear[R](taxYear: Int)(onFail: R)(onSuccess: R)(implicit user: User[_], ec: ExecutionContext): Future[R] = {
    interestUserDataRepository.clear(taxYear).map {
      case true => onSuccess
      case false => onFail
    }
  }

}
