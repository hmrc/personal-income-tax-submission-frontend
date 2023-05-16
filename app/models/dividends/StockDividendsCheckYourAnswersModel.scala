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

import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.crypto.EncryptedValue

case class StockDividendsCheckYourAnswersModel(
                                                gateway: Option[Boolean] = None,
                                                ukDividends: Option[Boolean] = None,
                                                ukDividendsAmount: Option[BigDecimal] = None,
                                                otherUkDividends: Option[Boolean] = None,
                                                otherUkDividendsAmount: Option[BigDecimal] = None,
                                                stockDividends: Option[Boolean] = None,
                                                stockDividendsAmount: Option[BigDecimal] = None,
                                                redeemableShares: Option[Boolean] = None,
                                                redeemableSharesAmount: Option[BigDecimal] = None,
                                                closeCompanyLoansWrittenOff: Option[Boolean] = None,
                                                closeCompanyLoansWrittenOffAmount: Option[BigDecimal] = None
                                              ){
  def toDividendsSubmissionModel: DividendsSubmissionModel = {
    DividendsSubmissionModel(
      ukDividends = this.ukDividendsAmount,
      otherUkDividends = this.otherUkDividendsAmount)
  }
}

object StockDividendsCheckYourAnswersModel {

  implicit val formats: OFormat[StockDividendsCheckYourAnswersModel] = Json.format[StockDividendsCheckYourAnswersModel]
}

case class EncryptedStockDividendsCheckYourAnswersModel(
                                                         gateway: Option[EncryptedValue] = None,
                                                         ukDividends: Option[EncryptedValue] = None,
                                                         ukDividendsAmount: Option[EncryptedValue] = None,
                                                         otherUkDividends: Option[EncryptedValue] = None,
                                                         otherUkDividendsAmount: Option[EncryptedValue] = None,
                                                         stockDividends: Option[EncryptedValue] = None,
                                                         stockDividendsAmount: Option[EncryptedValue] = None,
                                                         redeemableShares: Option[EncryptedValue] = None,
                                                         redeemableSharesAmount: Option[EncryptedValue] = None,
                                                         closeCompanyLoansWrittenOff: Option[EncryptedValue] = None,
                                                         closeCompanyLoansWrittenOffAmount: Option[EncryptedValue] = None
                                                       )

object EncryptedStockDividendsCheckYourAnswersModel {

  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]
  implicit val formats: Format[EncryptedStockDividendsCheckYourAnswersModel] = Json.format[EncryptedStockDividendsCheckYourAnswersModel]

}
