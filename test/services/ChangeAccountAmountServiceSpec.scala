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
import utils.UnitTest

class ChangeAccountAmountServiceSpec extends UnitTest {

  val service = new ChangeAccountAmountService()

  val accounts: Seq[InterestAccountModel] = Seq(
    InterestAccountModel(
      Some("1"),
      "TSB Account",
      untaxedAmount = Some(500.00)
    ),
    InterestAccountModel(
      Some("2"),
      "Lloyds Savings",
      untaxedAmount = Some(3000.00)
    ),
    InterestAccountModel(
      Some("3"),
      "Account 1",
      taxedAmount = Some(100.01))
  )

  "priorAmount" should {
    "return taxed amount for specified account if specified tax type is 'TAXED'" in {
      val account = accounts.last

      val priorAmount = service.priorAmount(account, InterestTaxTypes.TAXED)

      priorAmount shouldBe Some(100.01)
    }

    "return untaxed amount for specified account if specified tax type is 'UNTAXED'" in {
      val account = accounts.head

      val priorAmount = service.priorAmount(account, InterestTaxTypes.UNTAXED)

      priorAmount shouldBe Some(500.00)
    }
  }

  "getSingleAccount" should {
    "return account if account exists in prior or cya model data with matching account id" in {
      val cyaModel = Some(InterestCYAModel(
        untaxedUkInterest = Some(false),
        taxedUkInterest = Some(true),
        accounts = accounts
      ))

      val prior = Some(InterestPriorSubmission(true, false, submissions = cyaModel.get.accounts))

      val account: Option[InterestAccountModel] = service.getSingleAccount("1", prior, cyaModel)

      account shouldBe Some(InterestAccountModel(Some("1"), "TSB Account", Some(500.0), None, None))
    }

    "return None if account doesn't exist in prior or cya model data with given account id" in {
      val cyaModel = Some(InterestCYAModel(
        untaxedUkInterest = Some(false),
        taxedUkInterest = Some(true),
        accounts = accounts
      ))

      val prior = Some(InterestPriorSubmission(true, false, submissions = cyaModel.get.accounts))

      val account: Option[InterestAccountModel] = service.getSingleAccount("4", prior, cyaModel)

      account shouldBe None
    }
  }

  "replaceAccounts" should {
    "return updated cya model where untaxed uk interest is set to true if tax type specified is 'UNTAXED'" in {
      val cyaModel = InterestCYAModel(untaxedUkInterest = None, taxedUkInterest = None)

      val updatedCyaModel = service.replaceAccounts(InterestTaxTypes.UNTAXED, cyaModel, accounts)

      updatedCyaModel shouldBe InterestCYAModel(untaxedUkInterest = Some(true), taxedUkInterest = None, accounts = accounts)
    }

    "return updated cya model where taxed uk interest is set to true if tax type specified is 'TAXED'" in {
      val cyaModel = InterestCYAModel(untaxedUkInterest = None, taxedUkInterest = None)

      val updatedCyaModel = service.replaceAccounts(InterestTaxTypes.TAXED, cyaModel, accounts)

      updatedCyaModel shouldBe InterestCYAModel(untaxedUkInterest = None, taxedUkInterest = Some(true), accounts = accounts)
    }
  }

  "extractPreAmount" should {
    "return untaxed amount if account found with given id and tax type specified is 'UNTAXED'" in {
      val cyaModel = InterestCYAModel(untaxedUkInterest = None, taxedUkInterest = None, accounts = accounts)

      val preAmount = service.extractPreAmount(InterestTaxTypes.UNTAXED, cyaModel, "1")

      preAmount shouldBe Some(500.00)
    }

    "return taxed amount if account found with given id and tax type specified is 'TAXED'" in {
      val cyaModel = InterestCYAModel(untaxedUkInterest = None, taxedUkInterest = None, accounts = accounts)

      val preAmount = service.extractPreAmount(InterestTaxTypes.TAXED, cyaModel, "3")

      preAmount shouldBe Some(100.01)
    }
  }

  "updateAccounts" should {
    "return accounts with updated account for given account id and if tax type specified is 'TAXED' and if account is only in cya data" in {
      val cyaModel = InterestCYAModel(untaxedUkInterest = None, taxedUkInterest = None, accounts = accounts)

      val prior = Some(InterestPriorSubmission(hasTaxed = false, hasUntaxed = true, submissions = Seq.empty))

      val updatedAccounts = service.updateAccounts(InterestTaxTypes.TAXED, cyaModel, prior, "1", 3000)

      updatedAccounts shouldBe Seq(InterestAccountModel(Some("2"),"Lloyds Savings",Some(3000.0),None,None),
        InterestAccountModel(Some("3"),"Account 1",None,Some(100.01),None),
        InterestAccountModel(Some("1"), "TSB Account", Some(500.0), Some(3000), None))
    }

    "return accounts with updated account for given account id and if tax type specified is 'UNTAXED' and if account is only in cya data" in {
      val cyaModel = InterestCYAModel(untaxedUkInterest = None, taxedUkInterest = None, accounts = accounts)

      val prior = Some(InterestPriorSubmission(hasTaxed = false, hasUntaxed = true, submissions = Seq.empty))

      val updatedAccounts = service.updateAccounts(InterestTaxTypes.UNTAXED, cyaModel, prior, "1", 3000)

      updatedAccounts shouldBe List(InterestAccountModel(Some("2"), "Lloyds Savings", Some(3000.0), None, None),
        InterestAccountModel(Some("3"), "Account 1", None, Some(100.01), None),
        InterestAccountModel(Some("1"), "TSB Account", Some(3000), None, None))
    }

    "return accounts with updated account for given account id and if tax type specified is 'TAXED' and if account is only in prior data" in {
      val cyaModel = InterestCYAModel(untaxedUkInterest = None, taxedUkInterest = None, accounts = Seq.empty)

      val prior = Some(InterestPriorSubmission(hasTaxed = false, hasUntaxed = true, submissions = accounts))

      val updatedAccounts = service.updateAccounts(InterestTaxTypes.TAXED, cyaModel, prior, "1", 3000)

      updatedAccounts shouldBe Seq(InterestAccountModel(Some("1"), "TSB Account", Some(500.0), Some(3000), None))
    }

    "return accounts with updated account for given account id and if tax type specified is 'UNTAXED' and if account is only in prior data" in {
      val cyaModel = InterestCYAModel(untaxedUkInterest = None, taxedUkInterest = None, accounts = Seq.empty)

      val prior = Some(InterestPriorSubmission(hasTaxed = false, hasUntaxed = true, submissions = accounts))

      val updatedAccounts = service.updateAccounts(InterestTaxTypes.UNTAXED, cyaModel, prior, "1", 3000)

      updatedAccounts shouldBe Seq(InterestAccountModel(Some("1"), "TSB Account", Some(3000), None, None))
    }

    "return empty accounts if tax type specified is 'TAXED' and no account exists with given account id in prior data or cya data" in {
      val cyaModel = InterestCYAModel(untaxedUkInterest = None, taxedUkInterest = None, accounts = Seq.empty)

      val prior = Some(InterestPriorSubmission(hasTaxed = false, hasUntaxed = true, submissions = accounts))

      val updatedAccounts = service.updateAccounts(InterestTaxTypes.TAXED, cyaModel, prior, "4", 3000)

      updatedAccounts shouldBe empty
    }

    "return empty accounts if tax type specified is 'UNTAXED' and no account exists with given account id in prior data or cya data" in {
      val cyaModel = InterestCYAModel(untaxedUkInterest = None, taxedUkInterest = None, accounts = Seq.empty)

      val prior = Some(InterestPriorSubmission(hasTaxed = false, hasUntaxed = true, submissions = accounts))

      val updatedAccounts = service.updateAccounts(InterestTaxTypes.UNTAXED, cyaModel, prior, "4", 3000)

      updatedAccounts shouldBe empty
    }
  }
}
