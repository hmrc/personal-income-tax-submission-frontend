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

package models.dividends

import play.api.libs.json.{Json, OFormat}

case class StockDividendsPriorSubmission(submittedOn: Option[String] = None,
                                         foreignDividend: Option[Seq[ForeignDividendModel]] = None,
                                         dividendIncomeReceivedWhilstAbroad: Option[Seq[ForeignDividendModel]] = None,
                                         stockDividend: Option[StockDividendModel] = None,
                                         redeemableShares: Option[StockDividendModel] = None,
                                         bonusIssuesOfSecurities: Option[StockDividendModel] = None,
                                         closeCompanyLoansWrittenOff: Option[StockDividendModel] = None){

  def toSubmission(cya: StockDividendsCheckYourAnswersModel): StockDividendsSubmissionModel = {
    StockDividendsSubmissionModel(
      foreignDividend = this.foreignDividend,//This is not in our journey we get from IFS and pass it back
      dividendIncomeReceivedWhilstAbroad = this.dividendIncomeReceivedWhilstAbroad,//This is not in our journey we get from IFS and pass it back
      stockDividend = cya.stockDividendsAmount.map(amount => StockDividendModel(None, amount)),
      redeemableShares = cya.redeemableSharesAmount.map(amount => StockDividendModel(None, amount)),
      bonusIssuesOfSecurities = this.bonusIssuesOfSecurities,//This is not in our journey we get from IFS and pass it back
      closeCompanyLoansWrittenOff = cya.closeCompanyLoansWrittenOffAmount.map(amount => StockDividendModel(None, amount))
    )
  }
}

object StockDividendsPriorSubmission {
  implicit val formats: OFormat[StockDividendsPriorSubmission] = Json.format[StockDividendsPriorSubmission]
}

case class ForeignDividendModel(
                                 countryCode: String,
                                 amountBeforeTax: Option[BigDecimal],
                                 taxTakenOff: Option[BigDecimal],
                                 specialWithholdingTax: Option[BigDecimal],
                                 foreignTaxCreditRelief: Option[Boolean],
                                 taxableAmount: BigDecimal
                               )

object ForeignDividendModel {
  implicit val formats: OFormat[ForeignDividendModel] = Json.format[ForeignDividendModel]
}

case class StockDividendModel(
                               customerReference: Option[String],
                               grossAmount: BigDecimal
                             )

object StockDividendModel {
  implicit val formats: OFormat[StockDividendModel] = Json.format[StockDividendModel]
}