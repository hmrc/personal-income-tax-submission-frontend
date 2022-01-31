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

package models.interest

import common.InterestTaxTypes.TAXED

case class AccountAmountModel(accountName: String, accountAmount: BigDecimal)

object AccountAmountModel {
  def apply(cya: Option[InterestCYAModel], uniqueSessionId: String, taxType: String): Option[AccountAmountModel] = {
    val account: Option[InterestAccountModel] = cya.flatMap(_.accounts.find(_.uniqueSessionId.contains(uniqueSessionId)))

    val accountName: Option[String] = account.map(_.accountName)
    val accountAmount: Option[BigDecimal] = if (taxType == TAXED) account.flatMap(_.taxedAmount) else account.flatMap(_.untaxedAmount)

    val model: Option[AccountAmountModel] = (accountName, accountAmount) match {
      case (Some(name), Some(amount)) => Some(AccountAmountModel(name, amount))
      case _ => None
    }

    model
  }
}
