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

import models.savings.{EncryptedSavingsIncomeCYAModel, SavingsIncomeCYAModel}
import play.api.libs.json._
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats

import java.time.Instant

case class SavingsIncomeUserDataModel(
                                   sessionId: String,
                                   mtdItId: String,
                                   nino: String,
                                   taxYear: Int,
                                   savingsIncome: Option[SavingsIncomeCYAModel] = None,
                                   lastUpdated: Instant = Instant.now()
                                 ) extends UserDataTemplate

object SavingsIncomeUserDataModel{
  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
  implicit lazy val formats: OFormat[SavingsIncomeUserDataModel] = Json.format[SavingsIncomeUserDataModel]
}

case class EncryptedSavingsIncomeUserDataModel(
                                                sessionId: String,
                                                mtdItId: String,
                                                nino: String,
                                                taxYear: Int,
                                                savingsIncome: Option[EncryptedSavingsIncomeCYAModel] = None,
                                                lastUpdated: Instant = Instant.now()
                                 ) extends UserDataTemplate

object EncryptedSavingsIncomeUserDataModel{
  implicit val instantFormat: Format[Instant] = MongoJavatimeFormats.instantFormat
  implicit lazy val formats: OFormat[EncryptedSavingsIncomeUserDataModel] = Json.format[EncryptedSavingsIncomeUserDataModel]
}
