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

package repositories

import com.mongodb.client.model.ReturnDocument
import models.User
import models.mongo._
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.{and, equal}
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model.{FindOneAndReplaceOptions, FindOneAndUpdateOptions}
import play.api.libs.json.Format
import uk.gov.hmrc.mongo.play.json.Codecs.{logger, toBson}
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.play.http.logging.Mdc
import utils.PagerDutyHelper.PagerDutyKeys.{ENCRYPTION_DECRYPTION_ERROR, FAILED_TO_CREATE_DATA, FAILED_TO_FIND_DATA, FAILED_TO_UPDATE_DATA}

import java.time.{Clock, Instant}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try
import scala.util.control.NonFatal

trait UserDataRepository[C <: UserDataTemplate] {
  self: PlayMongoRepository[C] =>
  implicit val ec: ExecutionContext
  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat

  type UserData

  def encryptionMethod: UserData => C

  def decryptionMethod: C => UserData

  def create(userData: UserData)(): Future[Either[DatabaseError, Boolean]] =
    Try(encryptionMethod(userData)).fold(
      exception => {
        logger.error(s"$ENCRYPTION_DECRYPTION_ERROR [create]", exception)
        Future.successful(Left(EncryptionDecryptionError(exception.getMessage)))
      },
      encryptedData =>
        Mdc.preservingMdc(collection.insertOne(encryptedData).toFutureOption()).map {
          case Some(_) => Right(true)
          case None => Left(DataNotUpdated)
        }.recover {
          case NonFatal(throwable) =>
            logger.error(s"$FAILED_TO_CREATE_DATA [create] Failed to create user data", throwable)
            Left(DataNotUpdated)
        }
    )

  def find(taxYear: Int)(implicit user: User[_]): Future[Either[DatabaseError, Option[UserData]]] =
    Mdc.preservingMdc(collection.findOneAndUpdate(
      filter = filter(user.sessionId, user.mtditid, user.nino, taxYear),
      update = set("lastUpdated", toBson(Instant.now(Clock.systemUTC()))),
      options = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
    ).toFutureOption()).map {
      case Some(data) =>
        Try(decryptionMethod(data)).fold(
          throwable => {
            logger.error(s"$ENCRYPTION_DECRYPTION_ERROR [find]", throwable)
            Left(EncryptionDecryptionError(throwable.getMessage))
          },
          userData => Right(Some(userData))
        )
      case None =>
        logger.info(s"[find] No CYA data found for user. SessionId: ${user.sessionId}")
        Right(None)
    }.recover {
      case NonFatal(throwable) =>
        logger.error(s"$FAILED_TO_FIND_DATA [find] Failed to find user data", throwable)
        Left(DataNotFound)
    }

  def update(userData: UserData): Future[Either[DatabaseError, Boolean]] = {
    Try(encryptionMethod.apply(userData)).fold(
      throwable => {
        logger.error(s"$ENCRYPTION_DECRYPTION_ERROR [update]", throwable)
        Future.successful(Left(EncryptionDecryptionError(throwable.getMessage)))
      },
      encryptedData =>
        Mdc.preservingMdc(collection.findOneAndReplace(
          filter = filter(encryptedData.sessionId, encryptedData.mtdItId, encryptedData.nino, encryptedData.taxYear),
          replacement = encryptedData,
          options = FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER)
        ).toFutureOption()).map {
          case Some(_) => Right(true)
          case None => Left(DataNotUpdated)
        }.recover {
          case NonFatal(throwable) =>
            logger.error(s"$FAILED_TO_UPDATE_DATA [update] Failed to update user data", throwable)
            Left(DataNotUpdated)
        }
    )
  }

  def clear(taxYear: Int)(implicit user: User[_]): Future[Boolean] = Mdc.preservingMdc(collection.deleteOne(
    filter = filter(user.sessionId, user.mtditid, user.nino, taxYear)
  ).toFutureOption()).map(_.isDefined)

  def filter(sessionId: String, mtdItId: String, nino: String, taxYear: Int): Bson = and(
    equal("sessionId", toBson(sessionId)),
    equal("mtdItId", toBson(mtdItId)),
    equal("nino", toBson(nino)),
    equal("taxYear", toBson(taxYear))
  )

}
