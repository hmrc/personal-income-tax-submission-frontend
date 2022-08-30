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

class ChangeAccountAmountServiceSpec extends UnitTest {

  val service = new ChangeAccountAmountService()

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
    )
  )

  val taxedAccounts: Seq[InterestAccountModel] = Seq(
    InterestAccountModel(
      Some("3"),
      "Account 1",
      amount = Some(100.01))
  )

  val allAccounts = untaxedAccounts ++ taxedAccounts

  "getSingleAccount" should {
    "return account if account exists in prior or cya model data with matching account id" in {
      val cyaModel = Some(InterestCYAModel(
        untaxedUkInterest = Some(false),
        taxedUkInterest = Some(true),
        untaxedAccounts = untaxedAccounts
      ))

      val prior = Some(InterestPriorSubmission(true, false, submissions = cyaModel.get.untaxedAccounts.map(x => InterestAccountSourceModel(x.id, x.accountName, x.amount, None, x.uniqueSessionId))))

      val account: Option[InterestAccountModel] = service.getSingleAccount("1", prior, cyaModel)

      account shouldBe Some(InterestAccountModel(Some("1"), "TSB Account", Some(500.0), None))
    }

    "return None if account doesn't exist in prior or cya model data with given account id" in {
      val cyaModel = Some(InterestCYAModel(
        untaxedUkInterest = Some(false),
        taxedUkInterest = Some(true),
        untaxedAccounts = untaxedAccounts
      ))

      val prior = Some(InterestPriorSubmission(true, false, submissions = cyaModel.get.untaxedAccounts.map(x => InterestAccountSourceModel(x.id, x.accountName, x.amount, None, x.uniqueSessionId))))

      val account: Option[InterestAccountModel] = service.getSingleAccount("4", prior, cyaModel)

      account shouldBe None
    }
  }

  "replaceAccounts" should {
    "return updated cya model where untaxed uk interest is set to true if tax type specified is 'UNTAXED'" in {
      val cyaModel = InterestCYAModel(untaxedUkInterest = None, taxedUkInterest = None)

      val updatedCyaModel = service.replaceAccounts(InterestTaxTypes.UNTAXED, cyaModel, untaxedAccounts)

      updatedCyaModel shouldBe InterestCYAModel(untaxedUkInterest = Some(true), taxedUkInterest = None, untaxedAccounts = untaxedAccounts)
    }

    "return updated cya model where taxed uk interest is set to true if tax type specified is 'TAXED'" in {
      val cyaModel = InterestCYAModel(untaxedUkInterest = None, taxedUkInterest = None)

      val updatedCyaModel = service.replaceAccounts(InterestTaxTypes.TAXED, cyaModel, taxedAccounts)

      updatedCyaModel shouldBe InterestCYAModel(untaxedUkInterest = None, taxedUkInterest = Some(true), taxedAccounts = taxedAccounts)
    }
  }

  "extractPreAmount" should {
    "return untaxed amount if account found with given id and tax type specified is 'UNTAXED'" in {
      val cyaModel = InterestCYAModel(untaxedUkInterest = None, taxedUkInterest = None, untaxedAccounts = untaxedAccounts)

      val preAmount = service.extractPreAmount(InterestTaxTypes.UNTAXED, cyaModel, "1")

      preAmount shouldBe Some(500.00)
    }

    "return taxed amount if account found with given id and tax type specified is 'TAXED'" in {
      val cyaModel = InterestCYAModel(untaxedUkInterest = None, taxedUkInterest = None, taxedAccounts = taxedAccounts)

      val preAmount = service.extractPreAmount(InterestTaxTypes.TAXED, cyaModel, "3")

      preAmount shouldBe Some(100.01)
    }
  }

  "updateAccounts" should {
    "return accounts with updated account for given account id and if tax type specified is 'TAXED' and if account is only in cya data" in {
      val cyaModel = InterestCYAModel(untaxedUkInterest = None, taxedUkInterest = None, taxedAccounts = taxedAccounts)

      val prior = Some(InterestPriorSubmission(hasTaxed = false, hasUntaxed = true, submissions = Seq.empty))

      val updatedAccounts = service.updateAccounts(InterestTaxTypes.TAXED, cyaModel, prior, "3", 3000)

      updatedAccounts shouldBe Seq(InterestAccountModel(Some("3"),"Account 1",Some(3000.0),None))
    }

    "return accounts with updated account for given account id and if tax type specified is 'UNTAXED' and if account is only in cya data" in {
      val cyaModel = InterestCYAModel(untaxedUkInterest = None, taxedUkInterest = None, untaxedAccounts = untaxedAccounts)

      val prior = Some(InterestPriorSubmission(hasTaxed = false, hasUntaxed = true, submissions = Seq.empty))

      val updatedAccounts = service.updateAccounts(InterestTaxTypes.UNTAXED, cyaModel, prior, "1", 3000)

      updatedAccounts shouldBe Seq(
        InterestAccountModel(
          Some("1"),
          "TSB Account",
          amount = Some(3000.00)
        ),
        InterestAccountModel(
          Some("2"),
          "Lloyds Savings",
          amount = Some(3000.00)
        )
      )
    }

    "return accounts with updated account for given account id and if tax type specified is 'TAXED' and if account is only in prior data" in {
      val cyaModel = InterestCYAModel(untaxedUkInterest = None, taxedUkInterest = None, taxedAccounts = Seq.empty)

      val prior = Some(InterestPriorSubmission(hasTaxed = false, hasUntaxed = true, submissions = taxedAccounts.map(x => InterestAccountSourceModel(x.id, x.accountName, None, x.amount, x.uniqueSessionId))))

      val updatedAccounts = service.updateAccounts(InterestTaxTypes.TAXED, cyaModel, prior, "3", 3000)

      updatedAccounts shouldBe Seq(InterestAccountModel(Some("3"), "Account 1", Some(3000), None))
    }

    "return accounts with updated account for given account id and if tax type specified is 'UNTAXED' and if account is only in prior data" in {
      val cyaModel = InterestCYAModel(untaxedUkInterest = None, taxedUkInterest = None, untaxedAccounts = Seq.empty)

      val prior = Some(InterestPriorSubmission(hasTaxed = false, hasUntaxed = true, submissions = untaxedAccounts.map(x => InterestAccountSourceModel(x.id, x.accountName, x.amount, None, x.uniqueSessionId))))

      val updatedAccounts = service.updateAccounts(InterestTaxTypes.UNTAXED, cyaModel, prior, "1", 3000)

      updatedAccounts shouldBe Seq(
        InterestAccountModel(
          Some("1"),
          "TSB Account",
          amount = Some(3000.00)
        )
      )
    }

    "return empty accounts if tax type specified is 'TAXED' and no account exists with given account id in prior data or cya data" in {
      val cyaModel = InterestCYAModel(untaxedUkInterest = None, taxedUkInterest = None, taxedAccounts = Seq.empty)

      val prior = Some(InterestPriorSubmission(hasTaxed = false, hasUntaxed = true, submissions = taxedAccounts.map(x => InterestAccountSourceModel(x.id, x.accountName, None, x.amount, x.uniqueSessionId))))

      val updatedAccounts = service.updateAccounts(InterestTaxTypes.TAXED, cyaModel, prior, "4", 3000)

      updatedAccounts shouldBe empty
    }

    "return empty accounts if tax type specified is 'UNTAXED' and no account exists with given account id in prior data or cya data" in {
      val cyaModel = InterestCYAModel(untaxedUkInterest = None, taxedUkInterest = None, untaxedAccounts = Seq.empty)

      val prior = Some(InterestPriorSubmission(hasTaxed = false, hasUntaxed = true, submissions = untaxedAccounts.map(x => InterestAccountSourceModel(x.id, x.accountName, x.amount, None, x.uniqueSessionId))))

      val updatedAccounts = service.updateAccounts(InterestTaxTypes.UNTAXED, cyaModel, prior, "4", 3000)

      updatedAccounts shouldBe empty
    }
  }
}
