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
import models.interest.{InterestAccountModel, InterestCYAModel, InterestPriorSubmission}
import utils.{StubClock, UnitTest}

class ChooseAccountServiceSpec extends UnitTest {

  val service = new ChooseAccountService()

  val accounts: Seq[InterestAccountModel] = Seq(
    InterestAccountModel(
      Some("1"),
      "TSB Account",
      untaxedAmount = Some(500.00),
      createdAt = StubClock.localDateTimeNow()
    ),
    InterestAccountModel(
      Some("2"),
      "Lloyds Savings",
      untaxedAmount = Some(3000.00),
      createdAt = StubClock.localDateTimeNow()
    ),
    InterestAccountModel(
      Some("3"),
      "Account 1",
      taxedAmount = Some(100.01),
      createdAt = StubClock.localDateTimeNow()
    )
  )

  "accountsIgnoringAmounts" should {
    "return set of accounts where each account has untaxed and taxed amounts set to None" in {
      val accountsIgnorningAmounts = service.accountsIgnoringAmounts(accounts)

      accountsIgnorningAmounts shouldBe Set(
        InterestAccountModel(
          Some("1"),
          "TSB Account",
          untaxedAmount = None,
          taxedAmount = None,
          createdAt = StubClock.localDateTimeNow()
        ),
        InterestAccountModel(
          Some("2"),
          "Lloyds Savings",
          untaxedAmount = None,
          taxedAmount = None,
          createdAt = StubClock.localDateTimeNow()
        ),
        InterestAccountModel(
          Some("3"),
          "Account 1",
          untaxedAmount = None,
          taxedAmount = None,
          createdAt = StubClock.localDateTimeNow()
        )
      )
    }
  }

  "getPreviousAccounts" should {
    "return accounts which have taxed defined if tax type 'UNTAXED' is specified" in {
      val cyaModel = Some(InterestCYAModel(
        untaxedUkInterest = Some(false),
        taxedUkInterest = Some(true),
        accounts = accounts
      ))

      val prior = Some(InterestPriorSubmission(true, false, submissions = accounts))

      val previousAccounts = service.getPreviousAccounts(cyaModel, prior, InterestTaxTypes.UNTAXED)

      previousAccounts shouldBe Set(InterestAccountModel(Some("3"), "Account 1", None, None, None, createdAt = StubClock.localDateTimeNow()))
    }

    "return accounts which have untaxed defined if tax type 'TAXED' is specified" in {
      val cyaModel = Some(InterestCYAModel(
        untaxedUkInterest = Some(false),
        taxedUkInterest = Some(true),
        accounts = accounts
      ))

      val prior = Some(InterestPriorSubmission(true, false, submissions = accounts))

      val newAccounts = service.getPreviousAccounts(cyaModel, prior, InterestTaxTypes.TAXED)

      newAccounts shouldBe Set(InterestAccountModel(Some("1"), "TSB Account", None, None, None, createdAt = StubClock.localDateTimeNow()), InterestAccountModel(Some("2"), "Lloyds Savings", None, None, None, createdAt = StubClock.localDateTimeNow()))
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