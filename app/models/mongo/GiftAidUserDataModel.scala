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

import models.charity.{EncryptedGiftAidCYAModel, GiftAidCYAModel}
import play.api.libs.json._

import java.time.Instant

case class GiftAidUserDataModel(
                                 sessionId: String,
                                 mtdItId: String,
                                 nino: String,
                                 taxYear: Int,
                                 giftAid: Option[GiftAidCYAModel] = None,
                                 lastUpdated: Instant = Instant.now()
                               ) extends UserDataTemplate


object GiftAidUserDataModel {

  implicit lazy val formats: OFormat[GiftAidUserDataModel] = Json.format[GiftAidUserDataModel]
}

case class EncryptedGiftAidUserDataModel(
                                          sessionId: String,
                                          mtdItId: String,
                                          nino: String,
                                          taxYear: Int,
                                          giftAid: Option[EncryptedGiftAidCYAModel] = None,
                                          lastUpdated:Instant = Instant.now()
                                        ) extends UserDataTemplate

object EncryptedGiftAidUserDataModel {

  implicit lazy val formats: OFormat[EncryptedGiftAidUserDataModel] = Json.format[EncryptedGiftAidUserDataModel]
}
