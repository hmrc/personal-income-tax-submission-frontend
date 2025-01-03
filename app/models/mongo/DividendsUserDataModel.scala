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

package models.mongo

import models.dividends.{DividendsCheckYourAnswersModel, EncryptedDividendsCheckYourAnswersModel}
import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class DividendsUserDataModel(
                                   sessionId: String,
                                   mtdItId: String,
                                   nino: String,
                                   taxYear: Int,
                                   dividends: Option[DividendsCheckYourAnswersModel] = None,
                                   lastUpdated: Instant = Instant.now()
                                 ) extends UserDataTemplate

object DividendsUserDataModel {
  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
  implicit lazy val formats: OFormat[DividendsUserDataModel] = Json.format[DividendsUserDataModel]
}

case class EncryptedDividendsUserDataModel(
                                   sessionId: String,
                                   mtdItId: String,
                                   nino: String,
                                   taxYear: Int,
                                   dividends: Option[EncryptedDividendsCheckYourAnswersModel] = None,
                                   lastUpdated: Instant = Instant.now()
                                 ) extends UserDataTemplate

object EncryptedDividendsUserDataModel {
  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
  implicit lazy val formats: OFormat[EncryptedDividendsUserDataModel] = Json.format[EncryptedDividendsUserDataModel]
}
