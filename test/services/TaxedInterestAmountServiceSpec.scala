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

import models.interest.{InterestAccountModel, TaxedInterestModel}
import utils.UnitTest

class TaxedInterestAmountServiceSpec extends UnitTest {

  val service = new TaxedInterestAmountService()

  val taxedAccounts: Seq[InterestAccountModel] = Seq(
    InterestAccountModel(
      Some("1"),
      "TSB Account",
      amount = Some(500.00)
    ),
    InterestAccountModel(
      Some("2"),
      "Lloyds Savings",
      amount = Some(3000.00)
    ),
    InterestAccountModel(
      Some("3"),
      "Account 1",
      amount = Some(100.01))
  )

  val untaxedAccounts: Seq[InterestAccountModel] = Seq(
    InterestAccountModel(
      Some("1"),
      "TSB Account",
      amount = Some(500.00)
    ),
    InterestAccountModel(
      Some("2"),
      "Lloyds Savings",
      amount = Some(3000.00)
    ),
    InterestAccountModel(
      Some("3"),
      "Account 1",
      amount = Some(100.01))
  )

  "createNewAccountsList" should {
    "return new accounts list containing updated account if existing account with name and no tax amount specified" in {
      val formModel = TaxedInterestModel("Lloyds Savings", 5000.00)

      val existingAccount = Some(taxedAccounts(1))

      val newAccounts = service.createNewAccountsList(formModel, existingAccount, taxedAccounts, "2")

      newAccounts.length shouldBe 3
      newAccounts.head shouldBe InterestAccountModel(Some("2"), "Lloyds Savings", amount = Some(5000.00))
      newAccounts (1) shouldBe taxedAccounts.head
      newAccounts.last shouldBe taxedAccounts.last
    }

    "return new accounts list containing updated account if existing account with name and tax amount specified" in {
      val formModel = TaxedInterestModel("TSB Account", 5000.00)

      val existingAccount = Some(taxedAccounts(1).copy(amount = Some(300.00)))

      val newAccounts = service.createNewAccountsList(formModel, existingAccount, taxedAccounts, "1")

      newAccounts.length shouldBe 3
      newAccounts.head shouldBe InterestAccountModel(Some("2"), "Lloyds Savings", Some(5000.0), None)
      newAccounts(1) shouldBe taxedAccounts(1)
      newAccounts.last shouldBe taxedAccounts.last
    }

    "return new accounts list containing new account if account already exists with taxed amount and name has changed" in {
      val formModel = TaxedInterestModel("New Name", 5000.00)

      val newAccounts = service.createNewAccountsList(formModel, None, taxedAccounts, "2")

      newAccounts.length shouldBe 4
      newAccounts.head shouldBe taxedAccounts.head
      newAccounts(1) shouldBe taxedAccounts.last
      newAccounts(2).accountName shouldBe formModel.taxedAccountName
      newAccounts(2).amount shouldBe Some(formModel.taxedAmount)
      newAccounts.last shouldBe InterestAccountModel(Some("2"), "Lloyds Savings", None, None)
    }

    "return new accounts list containing updated account if existing account with name specified is untaxed and in accounts list" in {
      val formModel = TaxedInterestModel("New Name", 5000.00)

      val newAccounts = service.createNewAccountsList(formModel, None, taxedAccounts, "3")

      newAccounts.length shouldBe 4
      newAccounts.head shouldBe taxedAccounts.head
      newAccounts(1) shouldBe taxedAccounts(1)
      newAccounts.last shouldBe InterestAccountModel(Some("3"), "Account 1", None, None)
    }

    "return new accounts list containing new account if existing account with name specified is untaxed and in accounts list" in {
      val formModel = TaxedInterestModel("New Name", 5000.00)

      val newAccounts = service.createNewAccountsList(formModel, None, taxedAccounts, "4")

      newAccounts.length shouldBe 4
      newAccounts.head shouldBe taxedAccounts.head
      newAccounts(1) shouldBe taxedAccounts(1)
      newAccounts(2) shouldBe taxedAccounts.last
      newAccounts.last shouldBe InterestAccountModel(None, formModel.taxedAccountName, Some(formModel.taxedAmount), Some("4"))
    }
  }
}
