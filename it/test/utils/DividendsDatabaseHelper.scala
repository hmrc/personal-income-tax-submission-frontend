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

import models.dividends.{DividendsCheckYourAnswersModel, StockDividendsCheckYourAnswersModel}
import models.mongo.{DividendsUserDataModel, StockDividendsUserDataModel}
import repositories.{DividendsUserDataRepository, StockDividendsUserDataRepository}

trait DividendsDatabaseHelper {
  self: IntegrationTest =>

  lazy val dividendsDatabase: DividendsUserDataRepository = app.injector.instanceOf[DividendsUserDataRepository]
  lazy val stockDividendsDatabase: StockDividendsUserDataRepository = app.injector.instanceOf[StockDividendsUserDataRepository]

  //noinspection ScalaStyle
  def dropDividendsDB(): Seq[String] = {
    await(dividendsDatabase.collection.drop().toFutureOption())
    await(dividendsDatabase.ensureIndexes())
  }

  // noinspection ScalaStyle
  def dropStockDividendsDB(): Seq[String] = {
    await(stockDividendsDatabase.collection.drop().toFutureOption())
    await(dividendsDatabase.ensureIndexes())
  }

  //noinspection ScalaStyle
  def insertDividendsCyaData(
                              cya: Option[DividendsCheckYourAnswersModel],
                              taxYear: Int = taxYear,
                              overrideMtditid: Option[String] = None,
                              overrideNino: Option[String] = None
                            ): Boolean = {

    await(dividendsDatabase.create(
      DividendsUserDataModel(sessionId, overrideMtditid.fold(mtditid)(value => value), overrideNino.fold(nino)(value => value), taxYear, cya)
    )()) match {
      case Right(value) => value
      case Left(_) => false
    }
  }

  //noinspection ScalaStyle
  def insertStockDividendsCyaData(
                                   cya: Option[StockDividendsCheckYourAnswersModel],
                                   taxYear: Int = taxYear,
                                   overrideMtditid: Option[String] = None,
                                   overrideNino: Option[String] = None
                                 ): Boolean = {

    await(stockDividendsDatabase.create(
      StockDividendsUserDataModel(sessionId, overrideMtditid.fold(mtditid)(value => value), overrideNino.fold(nino)(value => value), taxYear, cya)
    )()) match {
      case Right(value) => value
      case Left(_) => false
    }
  }

}
