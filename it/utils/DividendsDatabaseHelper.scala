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

package utils

import models.dividends.DividendsCheckYourAnswersModel
import models.mongo.DividendsUserDataModel
import repositories.DividendsUserDataRepository

trait DividendsDatabaseHelper {
  self: IntegrationTest =>

  lazy val dividendsDatabase: DividendsUserDataRepository = app.injector.instanceOf[DividendsUserDataRepository]

  //noinspection ScalaStyle
  def dropDividendsDB() = {
    await(dividendsDatabase.collection.drop().toFutureOption())
    await(dividendsDatabase.ensureIndexes)
  }

  //noinspection ScalaStyle
  def insertCyaData(
                     cya: Option[DividendsCheckYourAnswersModel],
                     taxYear: Int = 2022,
                     overrideMtditid: Option[String] = None,
                     overrideNino: Option[String] = None
                   ): Boolean = {

    await(dividendsDatabase.create(
      DividendsUserDataModel(sessionId, overrideMtditid.fold(mtditid)(value => value), overrideNino.fold(nino)(value => value), taxYear, cya)
    )) match {
      case Right(value) => value
      case Left(value) => false
    }
  }

}
