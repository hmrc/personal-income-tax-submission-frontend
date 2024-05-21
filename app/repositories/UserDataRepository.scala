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
import uk.gov.hmrc.mongo.play.json.Codecs.{logger, toBson}
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import utils.PagerDutyHelper.PagerDutyKeys.{ENCRYPTION_DECRYPTION_ERROR, FAILED_TO_CREATE_DATA, FAILED_TO_FIND_DATA, FAILED_TO_UPDATE_DATA}
import utils.PagerDutyHelper.pagerDutyLog

import java.time.Instant
import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

trait UserDataRepository[C <: UserDataTemplate] {
  self: PlayMongoRepository[C] =>
  implicit val ec: ExecutionContext

  val repoName: String
  type UserData

  def encryptionMethod: UserData => C

  def decryptionMethod: C => UserData

  def create[T](userData: UserData)(): Future[Either[DatabaseError, Boolean]] = {
    lazy val start = s"[$repoName][create]"
    Try {
      encryptionMethod(userData)
    }.toEither match {
      case Left(exception: Exception) => Future.successful(handleEncryptionDecryptionException(exception, start))
      case Right(encryptedData) =>
        collection.insertOne(encryptedData).toFutureOption().map {
          case Some(_) => Right(true)
        }.recover {
          case exception: Exception =>
            pagerDutyLog(FAILED_TO_CREATE_DATA, Some(s"$start Failed to create user data. Exception: ${exception.getMessage}"))
            Left(DataNotUpdated)
        }
    }
  }

  def find[T](taxYear: Int)(implicit user: User[T]): Future[Either[DatabaseError, Option[UserData]]] = {
    lazy val start = s"[$repoName][find]"

    val userData = collection.findOneAndUpdate(
      filter = filter(user.sessionId, user.mtditid, user.nino, taxYear),
      update = set("lastUpdated", toBson(Instant.now())),
      options = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
    ).toFutureOption().map {
      case Some(data) => Right(Some(data))
      case None =>
        logger.info(s"$start No CYA data found for user. SessionId: ${user.sessionId}")
        Right(None)
    }.recover {
      case exception: Exception =>
        pagerDutyLog(FAILED_TO_FIND_DATA, Some(s"$start Failed to find user data. Exception: ${exception.getMessage}"))
        Left(DataNotFound)
    }

    userData.map {
      case Left(_) => Left(DataNotFound)
      case Right(data) =>
        Try {
          data.map(decryptionMethod)
        }.toEither match {
          case Left(value) => handleEncryptionDecryptionException(value.asInstanceOf[Exception], start)
          case Right(value) => Right(value)
        }
    }

  }

  def update(userData: UserData): Future[Either[DatabaseError, Boolean]] = {
    lazy val start = s"[$repoName][update]"

    Try {
      encryptionMethod.apply(userData)
    }.toEither match {
      case Left(exception: Exception) =>
        Future.successful(handleEncryptionDecryptionException(exception, start))
      case Right(encryptedData) =>
        collection.findOneAndReplace(
          filter = filter(encryptedData.sessionId, encryptedData.mtdItId, encryptedData.nino, encryptedData.taxYear),
          replacement = encryptedData,
          options = FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER)
        ).toFutureOption().map {
          case Some(_) =>
            Right(true)
        }.recover {
          case exception: Exception =>
            pagerDutyLog(FAILED_TO_UPDATE_DATA, Some(s"$start Failed to update user data. Exception: ${exception.getMessage}"))
            Left(DataNotUpdated)
        }
    }
  }

  def clear(taxYear: Int)(implicit user: User[_]): Future[Boolean] = collection.deleteOne(
    filter = filter(user.sessionId, user.mtditid, user.nino, taxYear)
  ).toFutureOption().map(_.isDefined)

  def filter(sessionId: String, mtdItId: String, nino: String, taxYear: Int): Bson = and(
    equal("sessionId", toBson(sessionId)),
    equal("mtdItId", toBson(mtdItId)),
    equal("nino", toBson(nino)),
    equal("taxYear", toBson(taxYear))
  )

  private def handleEncryptionDecryptionException[T](exception: Exception, startOfMessage: String): Left[DatabaseError, T] = {
    pagerDutyLog(ENCRYPTION_DECRYPTION_ERROR, Some(s"$startOfMessage ${exception.getMessage}"))
    Left(EncryptionDecryptionError(exception.getMessage))
  }


}
