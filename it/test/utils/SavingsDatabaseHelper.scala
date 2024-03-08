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

package test.utils

import models.mongo.SavingsIncomeUserDataModel
import models.savings.SavingsIncomeCYAModel
import repositories.SavingsUserDataRepository
import services.EncryptionService

trait SavingsDatabaseHelper {
  self: IntegrationTest =>

  lazy val savingsDatabase: SavingsUserDataRepository = app.injector.instanceOf[SavingsUserDataRepository]
  val encryptionService: EncryptionService = app.injector.instanceOf[EncryptionService]

  //noinspection ScalaStyle
  def dropSavingsDB(): Seq[String] = {
    await(savingsDatabase.collection.drop().toFutureOption())
    await(savingsDatabase.ensureIndexes())
  }

  //noinspection ScalaStyle
  def insertSavingsCyaData(
                              cya: Option[SavingsIncomeCYAModel],
                              taxYear: Int = taxYear,
                              overrideMtditid: Option[String] = None,
                              overrideNino: Option[String] = None
                   ): Boolean = {

    await(savingsDatabase.create(
      SavingsIncomeUserDataModel(sessionId, overrideMtditid.fold(mtditid)(value => value), overrideNino.fold(nino)(value => value), taxYear, cya)
    )()) match {
      case Right(value) => value
      case Left(_) => false
    }
  }

  def findSavingsDb: Option[SavingsIncomeCYAModel] = encryptionService.decryptSavingsIncomeUserData(
    await(savingsDatabase.collection.find().toFuture()).head).savingsIncome


}
