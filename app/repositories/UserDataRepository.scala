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

package repositories

import com.mongodb.client.model.ReturnDocument
import models.User
import models.mongo.UserDataTemplate
import org.joda.time.{DateTime, DateTimeZone}
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.{and, equal}
import org.mongodb.scala.model.Updates.set
import org.mongodb.scala.model.{FindOneAndReplaceOptions, FindOneAndUpdateOptions}
import uk.gov.hmrc.mongo.play.json.Codecs.toBson
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats

import scala.concurrent.{ExecutionContext, Future}

trait UserDataRepository[C <: UserDataTemplate] { self: PlayMongoRepository[C] =>
  implicit val ec: ExecutionContext

  def create[T](userData: C)(implicit user: User[T]): Future[Boolean] = collection.insertOne(userData).toFutureOption().map(_.isDefined)

  def find[T](taxYear: Int)(implicit user: User[T]): Future[Option[C]] = collection.findOneAndUpdate(
    filter = filter(user.sessionId, user.mtditid, user.nino, taxYear),
    update = set("lastUpdated", toBson(DateTime.now(DateTimeZone.UTC))(MongoJodaFormats.dateTimeWrites)),
    options = FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
  ).toFutureOption()

  def update(userData: C): Future[Boolean] = {
    collection.findOneAndReplace(
      filter = filter(userData.sessionId, userData.mtditid, userData.nino, userData.taxYear),
      replacement = userData,
      options = FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER)
    ).toFutureOption().map(_.isDefined)
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
}
