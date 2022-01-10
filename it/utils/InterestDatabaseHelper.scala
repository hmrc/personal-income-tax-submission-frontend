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
import models.interest.InterestCYAModel
import models.mongo.InterestUserDataModel
import repositories.InterestUserDataRepository
import services.EncryptionService

trait InterestDatabaseHelper { self: IntegrationTest =>

  lazy val interestDatabase: InterestUserDataRepository = app.injector.instanceOf[InterestUserDataRepository]
  val encryptionService = app.injector.instanceOf[EncryptionService]

  //noinspection ScalaStyle
  def dropInterestDB() = {
    await(interestDatabase.collection.drop().toFutureOption())
    await(interestDatabase.ensureIndexes)
  }

  //noinspection ScalaStyle
  def insertCyaData(
                     cya: Option[InterestCYAModel],
                     taxYear: Int = 2022,
                     overrideMtditid: Option[String] = None,
                     overrideNino: Option[String] = None
                   ): Boolean = {

    await(interestDatabase.create(
      InterestUserDataModel(sessionId, overrideMtditid.fold(mtditid)(value => value), overrideNino.fold(nino)(value => value), taxYear, cya)
    )) match {
      case Left(value) => false
      case Right(value) => true
    }

  }

  def findInterestDb: Option[InterestCYAModel] = encryptionService.decryptInterestUserData(await(interestDatabase.collection.find().toFuture()).head).interest
}
