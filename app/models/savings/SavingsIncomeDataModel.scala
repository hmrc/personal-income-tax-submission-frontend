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


case class SavingsIncomeDataModel(
  submittedOn: Option[String],
  securities: Option[SecuritiesModel],
  foreignInterest: Option[Seq[ForeignInterestModel]]
) {
  def toCYAModel: SavingsIncomeCYAModel = {
    securities.fold(SavingsIncomeCYAModel())(
      securities =>
        SavingsIncomeCYAModel(
          gateway = Some(true),
          grossAmount = Some(securities.grossAmount),
          taxTakenOff = Some(securities.taxTakenOff.nonEmpty),
          taxTakenOffAmount = securities.taxTakenOff
        )
    )
  }

  val hasNonZeroData: Boolean =
    securities.exists(_.hasNonZeroData) ||
    foreignInterest.exists(foreignInterestModel => foreignInterestModel.exists(_.hasNonZeroData))
}

object SavingsIncomeDataModel{
  implicit val formats: OFormat[SavingsIncomeDataModel] = Json.format[SavingsIncomeDataModel]
}