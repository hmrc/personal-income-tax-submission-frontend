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

import common.InterestTaxTypes
import utils.UnitTest

class AccountAmountModelSpec extends UnitTest {
  val untaxedAccounts: Seq[InterestAccountModel] = Seq(
    InterestAccountModel(
      None,
      "TSB Account",
      amount = Some(500.00),
      uniqueSessionId = Some("1")
    ),
    InterestAccountModel(
      None,
      "Lloyds Savings",
      amount = Some(3000.00),
      uniqueSessionId = Some("2")
    )
  )

  val taxedAccounts: Seq[InterestAccountModel] = Seq(
    InterestAccountModel(
      None,
      "Account 1",
      amount = Some(100.01),
      uniqueSessionId = Some("3")
    )
  )

  "apply" should {
    "return instance of AccountAmountModel containing name and tax amount if cya data, valid unique session id and tax type 'TAXED' is specified" in {
      val cyaModel = Some(InterestCYAModel(
        untaxedUkInterest = Some(false),
        taxedUkInterest = Some(true),
        untaxedAccounts = untaxedAccounts,
        taxedAccounts = taxedAccounts
      ))

      val accountAmountModel = AccountAmountModel.apply(cyaModel, "3", InterestTaxTypes.TAXED)

      accountAmountModel shouldBe Some(AccountAmountModel("Account 1", accountAmount = 100.01))
    }

    "return instance of AccountAmountModel containing name and untaxed amount if cya data, valid unique session id and tax type 'UNTAXED' is specified" in {
      val cyaModel = Some(InterestCYAModel(
        untaxedUkInterest = Some(false),
        taxedUkInterest = Some(true),
        untaxedAccounts = untaxedAccounts,
        taxedAccounts = taxedAccounts
      ))

      val accountAmountModel = AccountAmountModel.apply(cyaModel, "1", InterestTaxTypes.UNTAXED)

      accountAmountModel shouldBe Some(AccountAmountModel("TSB Account", accountAmount = 500.00))
    }

    "return None if account doesn't exist with specified unique session id" in {
      val cyaModel = Some(InterestCYAModel(
        untaxedUkInterest = Some(false),
        taxedUkInterest = Some(true),
        untaxedAccounts = untaxedAccounts,
        taxedAccounts = taxedAccounts
      ))

      val accountAmountModel = AccountAmountModel.apply(cyaModel, "4", InterestTaxTypes.UNTAXED)

      accountAmountModel shouldBe None
    }

    "return None if taxed amount is None and tax type specified is 'TAXED'" in {
      val cyaModel = Some(InterestCYAModel(
        untaxedUkInterest = Some(false),
        taxedUkInterest = Some(true),
        untaxedAccounts = untaxedAccounts,
        taxedAccounts = taxedAccounts
      ))

      val accountAmountModel = AccountAmountModel.apply(cyaModel, "1", InterestTaxTypes.TAXED)

      accountAmountModel shouldBe None
    }

    "return None if untaxed amount is None and tax type specified is 'UNTAXED'" in {
      val cyaModel = Some(InterestCYAModel(
        untaxedUkInterest = Some(false),
        taxedUkInterest = Some(true),
        untaxedAccounts = untaxedAccounts,
        taxedAccounts = taxedAccounts
      ))

      val accountAmountModel = AccountAmountModel.apply(cyaModel, "3", InterestTaxTypes.UNTAXED)

      accountAmountModel shouldBe None
    }
  }
}
