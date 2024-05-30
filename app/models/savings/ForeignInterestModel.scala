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

package models.savings

import play.api.libs.json.{Json, OFormat}

case class ForeignInterestModel(
  countryCode: String,
  amountBeforeTax: Option[BigDecimal],
  taxTakenOff: Option[BigDecimal],
  specialWithholdingTax: Option[BigDecimal],
  foreignTaxCreditRelief: Option[Boolean],
  taxableAmount: BigDecimal
) {
  val hasNonZeroData: Boolean =
    amountBeforeTax.exists(_ != 0) || taxTakenOff.exists(_ != 0) || specialWithholdingTax.exists(_ != 0) || taxableAmount != 0
}

object ForeignInterestModel{
  implicit val formats: OFormat[ForeignInterestModel] = Json.format[ForeignInterestModel]
}
