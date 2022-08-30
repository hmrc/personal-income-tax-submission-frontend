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

package services

import common.InterestTaxTypes
import models.interest.{InterestAccountModel, InterestAccountSourceModel, InterestCYAModel, InterestPriorSubmission}
import utils.UnitTest

class ChooseAccountServiceSpec extends UnitTest {

  val service = new ChooseAccountService()

  val untaxedAccounts = Seq(
    InterestAccountModel(
      Some("1"),
      "TSB Account",
      amount = Some(500.00)
    ),
    InterestAccountModel(
      Some("2"),
      "Lloyds Savings",
      amount = Some(3000.00)
    )
  )

  val taxedAccounts: Seq[InterestAccountModel] = Seq(
    InterestAccountModel(
      Some("3"),
      "Account 1",
      amount = Some(100.01))
  )

  val allAccounts = untaxedAccounts ++ taxedAccounts

  "accountsIgnoringAmounts" should {
    "return set of accounts where each account has untaxed and taxed amounts set to None" in {
      val accountsIgnorningAmounts = service.accountsIgnoringAmounts(allAccounts)

      accountsIgnorningAmounts shouldBe Set(
        InterestAccountModel(
          Some("1"),
          "TSB Account",
          amount = None
        ),
        InterestAccountModel(
          Some("2"),
          "Lloyds Savings",
          amount = None
        ),
        InterestAccountModel(
          Some("3"),
          "Account 1",
          amount = None
        )
      )
    }
  }

  "getPreviousAccounts" should {
    "return accounts which have taxed defined if tax type 'UNTAXED' is specified" in {
      val cyaModel = Some(InterestCYAModel(
        untaxedUkInterest = Some(false),
        taxedUkInterest = Some(true),
        taxedAccounts = taxedAccounts
      ))

      val prior = Some(InterestPriorSubmission(true, false, submissions = taxedAccounts.map(x => InterestAccountSourceModel(x.id, x.accountName, None, x.amount, x.uniqueSessionId))))

      val previousAccounts = service.getPreviousAccounts(cyaModel, prior, InterestTaxTypes.UNTAXED)

      previousAccounts shouldBe Set(InterestAccountModel(Some("3"), "Account 1", None, None))
    }

    "return accounts which have untaxed defined if tax type 'TAXED' is specified" in {
      val cyaModel = Some(InterestCYAModel(
        untaxedUkInterest = Some(false),
        taxedUkInterest = Some(true),
        untaxedAccounts = untaxedAccounts
      ))

      val prior = Some(InterestPriorSubmission(true, false, submissions = untaxedAccounts.map(x => InterestAccountSourceModel(x.id, x.accountName, None, x.amount, x.uniqueSessionId))))

      val newAccounts = service.getPreviousAccounts(cyaModel, prior, InterestTaxTypes.TAXED)

      newAccounts shouldBe Set(InterestAccountModel(Some("1"), "TSB Account", None, None), InterestAccountModel(Some("2"), "Lloyds Savings", None, None))
    }

    "return no accounts if there are no accounts or submissions defined" in {
      val cyaModel = Some(InterestCYAModel(
        untaxedUkInterest = Some(false),
        taxedUkInterest = Some(true)
      ))

      val prior = Some(InterestPriorSubmission(true, false, Seq.empty))

      val previousAccounts = service.getPreviousAccounts(cyaModel, prior, InterestTaxTypes.TAXED)

      previousAccounts shouldBe Set()
    }

    "return no accounts if cya model is None" in {
      val prior = Some(InterestPriorSubmission(true, false, Seq.empty))

      val previousAccounts = service.getPreviousAccounts(None, prior, InterestTaxTypes.TAXED)

      previousAccounts shouldBe Set()
    }

    "return no accounts if prior is None" in {
      val cyaModel = Some(InterestCYAModel(
        untaxedUkInterest = Some(false),
        taxedUkInterest = Some(true)
      ))

      val previousAccounts = service.getPreviousAccounts(cyaModel, None, InterestTaxTypes.TAXED)

      previousAccounts shouldBe Set()
    }
  }
}