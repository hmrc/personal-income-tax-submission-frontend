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

package models.priorDataModels

import models.dividends.{StockDividendsCheckYourAnswersModel, StockDividendsPriorSubmission, StockDividendsSubmissionModel}
import play.api.libs.json.{Json, OFormat}

case class StockDividendsPriorDataModel(
                                         ukDividendsAmount: Option[BigDecimal] = None,
                                         otherUkDividendsAmount: Option[BigDecimal] = None,
                                         stockDividendsAmount: Option[BigDecimal] = None,
                                         redeemableSharesAmount: Option[BigDecimal] = None,
                                         closeCompanyLoansWrittenOffAmount: Option[BigDecimal] = None
                                       )

object StockDividendsPriorDataModel {

  implicit val formats: OFormat[StockDividendsPriorDataModel] = Json.format[StockDividendsPriorDataModel]

  def getFromPrior(ukDividends: IncomeSourcesModel, stockDividends: StockDividendsPriorSubmission): StockDividendsPriorDataModel = {
    StockDividendsPriorDataModel(
      ukDividendsAmount = ukDividends.dividends.flatMap(_.ukDividends),
      otherUkDividendsAmount = ukDividends.dividends.flatMap(_.otherUkDividends),
      stockDividendsAmount = stockDividends.stockDividend.map(_.grossAmount),
      redeemableSharesAmount = stockDividends.redeemableShares.map(_.grossAmount),
      closeCompanyLoansWrittenOffAmount = stockDividends.closeCompanyLoansWrittenOff.map(_.grossAmount)
    )
  }
}