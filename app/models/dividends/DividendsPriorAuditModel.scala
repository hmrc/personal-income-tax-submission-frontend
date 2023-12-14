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

case class DividendsPriorAuditModel(ukDividends: Option[BigDecimal] = None,
                               otherUkDividends: Option[BigDecimal] = None,
                               foreignDividend: Option[Seq[ForeignDividendModel]] = None,
                               dividendIncomeReceivedWhilstAbroad: Option[Seq[ForeignDividendModel]] = None,
                               stockDividend: Option[StockDividendModel] = None,
                               redeemableShares: Option[StockDividendModel] = None,
                               bonusIssuesOfSecurities: Option[StockDividendModel] = None,
                               closeCompanyLoansWrittenOff: Option[StockDividendModel] = None
                              ) {
  def isEmpty(): Boolean = {
    this.productIterator.isEmpty
  }

}

object DividendsPriorAuditModel {

  def createFromPrior(dividendsPriorSubmission: Option[DividendsPriorSubmission],
                      stockDividendsPriorSubmission: Option[StockDividendsPriorSubmission]): DividendsPriorAuditModel = {
    DividendsPriorAuditModel(
      dividendsPriorSubmission.flatMap(_.ukDividends),
      dividendsPriorSubmission.flatMap(_.otherUkDividends),
      stockDividendsPriorSubmission.flatMap(_.foreignDividend),
      stockDividendsPriorSubmission.flatMap(_.dividendIncomeReceivedWhilstAbroad),
      stockDividendsPriorSubmission.flatMap(_.stockDividend),
      stockDividendsPriorSubmission.flatMap(_.redeemableShares),
      stockDividendsPriorSubmission.flatMap(_.bonusIssuesOfSecurities),
      stockDividendsPriorSubmission.flatMap(_.closeCompanyLoansWrittenOff)
    )
  }

  implicit val formats: OFormat[DividendsPriorAuditModel] = Json.format[DividendsPriorAuditModel]
}
