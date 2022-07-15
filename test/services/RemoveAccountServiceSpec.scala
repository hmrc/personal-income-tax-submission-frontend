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

class RemoveAccountServiceSpec extends UnitTest {

  val service = new RemoveAccountService()

  val accountWithId =
    InterestAccountModel(
      Some("3"),
      "Account 1",
      taxedAmount = Some(100.01),
      createdAt = StubClock.localDateTimeNow()
    )

  val accountWithUniqueSessionId =
    InterestAccountModel(
      None,
      "Account 1",
      taxedAmount = Some(100.01),
      uniqueSessionId = Some("3"),
      createdAt = StubClock.localDateTimeNow()
    )

  val accountWithNoIds =
    InterestAccountModel(
      None,
      "Account 1",
      taxedAmount = Some(100.01),
      createdAt = StubClock.localDateTimeNow()
    )

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
    "return accounts containing updated account if account to update has untaxed amount defined" in {
      val cyaData = InterestCYAModel(
        untaxedUkInterest = Some(true), taxedUkInterest = Some(true), accounts = Seq(
          InterestAccountModel(Some("azerty"), "Account 1", untaxedAmount = Some(100.01), createdAt = StubClock.localDateTimeNow()),
          InterestAccountModel(Some("qwerty"), "Account 2", untaxedAmount = Some(50), taxedAmount = Some(9001.01), createdAt = StubClock.localDateTimeNow())
        )
      )

      val accounts = Seq(
        InterestAccountModel(Some("azerty"), "Account 1", untaxedAmount = Some(100.01), createdAt = StubClock.localDateTimeNow()),
        InterestAccountModel(Some("qwerty"), "Account 2", untaxedAmount = Some(50), taxedAmount = Some(9001.01), createdAt = StubClock.localDateTimeNow())
      )

      val untaxedUpdate = service.calculateTaxedUpdate(cyaData, accounts, "qwerty")

      val updatedCyaData = untaxedUpdate._1
      val updatedAccounts = untaxedUpdate._2

      updatedAccounts shouldBe Seq(
        InterestAccountModel(Some("azerty"), "Account 1", untaxedAmount = Some(100.01), createdAt = StubClock.localDateTimeNow()),
        InterestAccountModel(Some("qwerty"), "Account 2", untaxedAmount = Some(50), taxedAmount = None, createdAt = StubClock.localDateTimeNow())
      )

      updatedCyaData shouldBe InterestCYAModel(
        untaxedUkInterest = Some(true), taxedUkInterest = Some(false), accounts = Seq(
          InterestAccountModel(Some("azerty"), "Account 1", untaxedAmount = Some(100.01), createdAt = StubClock.localDateTimeNow()),
          InterestAccountModel(Some("qwerty"), "Account 2", untaxedAmount = Some(50), taxedAmount = None, createdAt = StubClock.localDateTimeNow())
        )
      )
    }

    "return accounts excluding account if account to update doesn't have untaxed amount defined" in {
      val cyaData = InterestCYAModel(
        untaxedUkInterest = Some(true), taxedUkInterest = Some(true), accounts = Seq(
          InterestAccountModel(Some("azerty"), "Account 1", untaxedAmount = Some(100.01), createdAt = StubClock.localDateTimeNow()),
          InterestAccountModel(Some("qwerty"), "Account 2", untaxedAmount = None, taxedAmount = None, createdAt = StubClock.localDateTimeNow()))
      )

      val accounts = Seq(
        InterestAccountModel(Some("azerty"), "Account 1", untaxedAmount = Some(100.01), taxedAmount = Some(100.01), createdAt = StubClock.localDateTimeNow()),
        InterestAccountModel(Some("qwerty"), "Account 2", untaxedAmount = None, taxedAmount = None, createdAt = StubClock.localDateTimeNow())
      )

      val untaxedUpdate = service.calculateTaxedUpdate(cyaData, accounts, "qwerty")

      val updatedCyaData = untaxedUpdate._1
      val updatedAccounts = untaxedUpdate._2

      updatedAccounts shouldBe Seq(
        InterestAccountModel(Some("azerty"), "Account 1", untaxedAmount = Some(100.01), taxedAmount = Some(100.01), createdAt = StubClock.localDateTimeNow()),
      )

      updatedCyaData shouldBe InterestCYAModel(
        untaxedUkInterest = Some(true), taxedUkInterest = Some(true), accounts = Seq(
          InterestAccountModel(Some("azerty"), "Account 1", untaxedAmount = Some(100.01), taxedAmount = Some(100.01), createdAt = StubClock.localDateTimeNow())
        )
      )
    }
  }

  "calculateUntaxedUpdate" should {
    "return accounts containing updated account if account to update has taxed amount defined" in {
      val cyaData = InterestCYAModel(
        untaxedUkInterest = Some(true), taxedUkInterest = Some(true), accounts = Seq(
          InterestAccountModel(Some("azerty"), "Account 1", untaxedAmount = Some(100.01), createdAt = StubClock.localDateTimeNow()),
          InterestAccountModel(Some("qwerty"), "Account 2", untaxedAmount = Some(50), taxedAmount = Some(9001.01), createdAt = StubClock.localDateTimeNow())
        )
      )

      val accounts = Seq(
        InterestAccountModel(Some("azerty"), "Account 1", untaxedAmount = Some(100.01), createdAt = StubClock.localDateTimeNow()),
        InterestAccountModel(Some("qwerty"), "Account 2", untaxedAmount = Some(50), taxedAmount = Some(9001.01), createdAt = StubClock.localDateTimeNow())
      )

      val untaxedUpdate = service.calculateUntaxedUpdate(cyaData, accounts, "qwerty")

      val updatedCyaData = untaxedUpdate._1
      val updatedAccounts = untaxedUpdate._2

      updatedAccounts shouldBe Seq(
        InterestAccountModel(Some("azerty"), "Account 1", untaxedAmount = Some(100.01), createdAt = StubClock.localDateTimeNow()),
        InterestAccountModel(Some("qwerty"), "Account 2", untaxedAmount = None, taxedAmount = Some(9001.01), createdAt = StubClock.localDateTimeNow())
      )

      updatedCyaData shouldBe InterestCYAModel(
        untaxedUkInterest = Some(true), taxedUkInterest = Some(true), accounts = Seq(
          InterestAccountModel(Some("azerty"), "Account 1", untaxedAmount = Some(100.01), createdAt = StubClock.localDateTimeNow()),
          InterestAccountModel(Some("qwerty"), "Account 2", untaxedAmount = None, taxedAmount = Some(9001.01), createdAt = StubClock.localDateTimeNow())
        )
      )
    }

    "return accounts excluding account if account to update doesn't have taxed amount defined" in {
      val cyaData = InterestCYAModel(
        untaxedUkInterest = Some(true), taxedUkInterest = Some(true), accounts = Seq(
          InterestAccountModel(Some("azerty"), "Account 1", untaxedAmount = Some(100.01), createdAt = StubClock.localDateTimeNow()),
          InterestAccountModel(Some("qwerty"), "Account 2", untaxedAmount = Some(50), taxedAmount = None, createdAt = StubClock.localDateTimeNow())
        )
      )

      val accounts = Seq(
        InterestAccountModel(Some("azerty"), "Account 1", untaxedAmount = Some(100.01), createdAt = StubClock.localDateTimeNow()),
        InterestAccountModel(Some("qwerty"), "Account 2", untaxedAmount = Some(50), taxedAmount = None, createdAt = StubClock.localDateTimeNow())
      )

      val untaxedUpdate = service.calculateUntaxedUpdate(cyaData, accounts, "qwerty")

      val updatedCyaData = untaxedUpdate._1
      val updatedAccounts = untaxedUpdate._2

      updatedAccounts shouldBe Seq(
        InterestAccountModel(Some("azerty"), "Account 1", untaxedAmount = Some(100.01), createdAt = StubClock.localDateTimeNow()),
      )

      updatedCyaData shouldBe InterestCYAModel(
        untaxedUkInterest = Some(true), taxedUkInterest = Some(true), accounts = Seq(
          InterestAccountModel(Some("azerty"), "Account 1", untaxedAmount = Some(100.01), createdAt = StubClock.localDateTimeNow())
        )
      )
    }
  }

  "isLastAccount" should {
    "return true if tax type specified is 'TAXED' and if prior submission doesn't have taxed defined and length of accounts is 1" in {
      val taxType = InterestTaxTypes.TAXED

      val priorSubmission = Some(InterestPriorSubmission(hasTaxed = false, hasUntaxed = true, submissions = Seq.empty))

      val isLastAccount = service.isLastAccount(taxType, priorSubmission, Seq(InterestAccountModel(Some("azerty"), "Account 1", untaxedAmount = Some(100.01), createdAt = StubClock.localDateTimeNow())))

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

      val isLastAccount = service.isLastAccount(taxType, priorSubmission, Seq(InterestAccountModel(Some("azerty"), "Account 1", untaxedAmount = Some(100.01), createdAt = StubClock.localDateTimeNow())))

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
