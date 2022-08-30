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

class RemoveAccountServiceSpec extends UnitTest {

  val service = new RemoveAccountService()

  val accountWithId =
    InterestAccountModel(
      Some("3"),
      "Account 1",
      amount = Some(100.01))

  val accountWithUniqueSessionId =
    InterestAccountModel(
      None,
      "Account 1",
      amount = Some(100.01),
      uniqueSessionId = Some("3"))

  val accountWithNoIds =
    InterestAccountModel(
      None,
      "Account 1",
      amount = Some(100.01))

  "accountLookup" should {
    "return true if account id matches given id" in {
      val accountLookup = service.accountLookup(accountWithId, "3")

      accountLookup shouldBe true
    }

    "return true if account has no id and unique session id matches given id" in {
      val accountLookup = service.accountLookup(accountWithUniqueSessionId, "3")

      accountLookup shouldBe true
    }

    "return true if account has no id and no unique session id and given id is empty string" in {
      val accountLookup = service.accountLookup(accountWithNoIds, "")

      accountLookup shouldBe true
    }

    "return false if account has id and doesn't match given id" in {
      val accountLookup = service.accountLookup(accountWithId, "4")

      accountLookup shouldBe false
    }

    "return false if account has no id and unique session id doesn't match given id" in {
      val accountLookup = service.accountLookup(accountWithUniqueSessionId, "4")

      accountLookup shouldBe false
    }

    "return false if account has no id and no unique session id and given id is not an empty string" in {
      val accountLookup = service.accountLookup(accountWithNoIds, "4")

      accountLookup shouldBe false
    }
  }

  "calculateTaxedUpdate" should {
    "return defined taxedUkInterest if account exists with amount specified and accounts excluding account specified" in {
      val cyaData = InterestCYAModel(untaxedUkInterest = Some(false), taxedUkInterest = Some(false))

      val accounts = Seq(
        InterestAccountModel(Some("azerty"), "Account 1", amount = Some(100.01)),
        InterestAccountModel(Some("qwerty"), "Account 2", amount = Some(50))
      )

      val untaxedUpdate = service.calculateTaxedUpdate(cyaData, accounts, "qwerty")

      val updatedCyaData = untaxedUpdate._1
      val updatedAccounts = untaxedUpdate._2

      updatedAccounts shouldBe Seq(accounts.head)

      updatedCyaData shouldBe InterestCYAModel(
        untaxedUkInterest = Some(false), taxedUkInterest = Some(true), taxedAccounts = updatedAccounts
      )
    }

    "return undefined taxedUkInterest if account doesn't exist with amount specified and accounts excluding account specified" in {
      val cyaData = InterestCYAModel(untaxedUkInterest = Some(false), taxedUkInterest = Some(true))

      val accounts = Seq(
        InterestAccountModel(Some("azerty"), "Account 1", None),
        InterestAccountModel(Some("qwerty"), "Account 2", None)
      )

      val untaxedUpdate = service.calculateTaxedUpdate(cyaData, accounts, "qwerty")

      val updatedCyaData = untaxedUpdate._1
      val updatedAccounts = untaxedUpdate._2

      updatedAccounts shouldBe Seq(accounts.head)

      updatedCyaData shouldBe InterestCYAModel(
        untaxedUkInterest = Some(false), taxedUkInterest = Some(false), taxedAccounts = updatedAccounts
      )
    }
  }

  "calculateUntaxedUpdate" should {
    "return defined untaxedUkInterest if account exists with amount specified and accounts excluding account specified" in {
      val cyaData = InterestCYAModel(untaxedUkInterest = Some(false), taxedUkInterest = Some(true))

      val accounts = Seq(
        InterestAccountModel(Some("azerty"), "Account 1", amount = Some(100.01)),
        InterestAccountModel(Some("qwerty"), "Account 2", amount = Some(50))
      )

      val untaxedUpdate = service.calculateUntaxedUpdate(cyaData, accounts, "qwerty")

      val updatedCyaData = untaxedUpdate._1
      val updatedAccounts = untaxedUpdate._2

      updatedAccounts shouldBe Seq(accounts.head)

      updatedCyaData shouldBe InterestCYAModel(
        untaxedUkInterest = Some(true), taxedUkInterest = Some(true), untaxedAccounts = updatedAccounts
      )
    }
  }

  "return undefined untaxedUkInterest if account doesn't exist with amount specified and accounts excluding account specified" in {
    val cyaData = InterestCYAModel(untaxedUkInterest = Some(false), taxedUkInterest = Some(false))

    val accounts = Seq(
      InterestAccountModel(Some("azerty"), "Account 1", None),
      InterestAccountModel(Some("qwerty"), "Account 2", None)
    )

    val untaxedUpdate = service.calculateUntaxedUpdate(cyaData, accounts, "qwerty")

    val updatedCyaData = untaxedUpdate._1
    val updatedAccounts = untaxedUpdate._2

    updatedAccounts shouldBe Seq(accounts.head)

    updatedCyaData shouldBe InterestCYAModel(
      untaxedUkInterest = Some(false), taxedUkInterest = Some(false), untaxedAccounts = updatedAccounts
    )
  }

  "isLastAccount" should {
    "return true if tax type specified is 'TAXED' and if prior submission doesn't have taxed defined and length of accounts is 1" in {
      val taxType = InterestTaxTypes.TAXED

      val priorSubmission = Some(InterestPriorSubmission(hasTaxed = false, hasUntaxed = true, submissions = Seq.empty))

      val isLastAccount = service.isLastAccount(taxType, priorSubmission, Seq(InterestAccountModel(Some("azerty"), "Account 1", amount = Some(100.01))))

      isLastAccount shouldBe true
    }

    "return false if tax type specified is 'TAXED' and if prior submission has taxed defined" in {
      val taxType = InterestTaxTypes.TAXED

      val priorSubmission = Some(InterestPriorSubmission(hasTaxed = true, hasUntaxed = true, submissions = Seq.empty))

      val isLastAccount = service.isLastAccount(taxType, priorSubmission, Seq())

      isLastAccount shouldBe false
    }

    "return false if tax type specified is 'TAXED' and if prior submission doesn't have taxed defined and length of accounts isn't 1" in {
      val taxType = InterestTaxTypes.TAXED

      val priorSubmission = Some(InterestPriorSubmission(hasTaxed = false, hasUntaxed = true, submissions = Seq.empty))

      val isLastAccount = service.isLastAccount(taxType, priorSubmission, Seq())

      isLastAccount shouldBe false
    }

    "return true if tax type specified is 'UNTAXED' and if prior submission doesn't have untaxed defined and length of accounts is 1" in {
      val taxType = InterestTaxTypes.UNTAXED

      val priorSubmission = Some(InterestPriorSubmission(hasTaxed = false, hasUntaxed = false, submissions = Seq.empty))

      val isLastAccount = service.isLastAccount(taxType, priorSubmission, Seq(InterestAccountModel(Some("azerty"), "Account 1", amount = Some(100.01))))

      isLastAccount shouldBe true
    }

    "return false if tax type specified is 'UNTAXED' and if prior submission has untaxed defined" in {
      val taxType = InterestTaxTypes.UNTAXED

      val priorSubmission = Some(InterestPriorSubmission(hasTaxed = true, hasUntaxed = true, submissions = Seq.empty))

      val isLastAccount = service.isLastAccount(taxType, priorSubmission, Seq())

      isLastAccount shouldBe false
    }

    "return false if tax type specified is 'UNTAXED' and if prior submission doesn't have untaxed defined and length of accounts isn't 1" in {
      val taxType = InterestTaxTypes.UNTAXED

      val priorSubmission = Some(InterestPriorSubmission(hasTaxed = false, hasUntaxed = true, submissions = Seq.empty))

      val isLastAccount = service.isLastAccount(taxType, priorSubmission, Seq())

      isLastAccount shouldBe false
    }
  }
}
