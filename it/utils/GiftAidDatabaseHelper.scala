/*
 * Copyright 2022 HM Revenue & Customs
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

package utils

import models.charity.GiftAidCYAModel
import models.mongo.GiftAidUserDataModel
import repositories.GiftAidUserDataRepository
import services.EncryptionService

trait GiftAidDatabaseHelper {
  self: IntegrationTest =>

  lazy val giftAidDatabase: GiftAidUserDataRepository = app.injector.instanceOf[GiftAidUserDataRepository]
  val giftAidEncryptionService = app.injector.instanceOf[EncryptionService]

  //noinspection ScalaStyle
  def dropGiftAidDB() = {
    await(giftAidDatabase.collection.drop().toFutureOption())
    await(giftAidDatabase.ensureIndexes)
  }

  //noinspection ScalaStyle
  def insertGiftAidCyaData(
                     cya: Option[GiftAidCYAModel],
                     taxYear: Int = taxYear,
                     overrideMtditid: Option[String] = None,
                     overrideNino: Option[String] = None
                   ): Boolean = {

    await(giftAidDatabase.create(
      GiftAidUserDataModel(sessionId, overrideMtditid.fold(mtditid)(value => value), overrideNino.fold(nino)(value => value), taxYear, cya))
    ) match {
      case Left(_) => false
      case Right(value) => value
    }

  }

  //noinspection ScalaStyle
  def findGiftAidDb: Option[GiftAidCYAModel] = giftAidEncryptionService.decryptGiftAidUserData(await(giftAidDatabase.collection.find().toFuture()).head).giftAid

}
