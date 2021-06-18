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

package models.mongo

import models.charity.GiftAidCYAModel
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats

case class GiftAidUserDataModel(
                                 sessionId: String,
                                 mtditid: String,
                                 nino: String,
                                 taxYear: Int,
                                 giftAid: Option[GiftAidCYAModel] = None,
                                 lastUpdated: DateTime = DateTime.now(DateTimeZone.UTC)
                               ) extends UserDataTemplate

object GiftAidUserDataModel {
  implicit lazy val formats: OFormat[GiftAidUserDataModel] = OFormat(reads, writes)

  lazy val reads: Reads[GiftAidUserDataModel] = {
    for {
      sessionId <- (__ \ "sessionId").read[String]
      mtditid <- (__ \ "mtdItId").read[String]
      nino <- (__ \ "nino").read[String]
      taxYear <- (__ \ "taxYear").read[Int]
      giftAid <- (__ \ "giftAid").readNullable[GiftAidCYAModel]
      lastUpdated <- (__ \ "lastUpdated").read(MongoJodaFormats.dateTimeReads)
    } yield {
      GiftAidUserDataModel(
        sessionId, mtditid, nino, taxYear,
        giftAid,
        lastUpdated
      )
    }
  }

  lazy val writes: OWrites[GiftAidUserDataModel] = OWrites[GiftAidUserDataModel] { model =>
    Json.obj(
      "sessionId" -> model.sessionId,
      "mtdItId" -> model.mtditid,
      "nino" -> model.nino,
      "taxYear" -> model.taxYear,
      "giftAid" -> model.giftAid,
      "lastUpdated" -> Json.toJson(model.lastUpdated)(MongoJodaFormats.dateTimeWrites)
    )
  }

}
