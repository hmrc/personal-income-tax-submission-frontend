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

package utils

import common.InterestTaxTypes.{TAXED, UNTAXED}
import models.interest.InterestAccountModel
import utils.SeqConversions.SeqInterestAccountModel

class SeqConversionsSpec extends UnitTest {

  val accounts: Seq[InterestAccountModel] = Seq(
    InterestAccountModel(
      Some("1"),
      "TSB Account",
      taxedAmount = Some(500.00),
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
      untaxedAmount = Some(100.01),
      createdAt = StubClock.localDateTimeNow()
    )
  )

  "SeqInterestAccountModel" when {
    "filterByTaxType" should {
      "return taxed accounts when 'TAXED' tax type is specified" in {
        val filteredAccounts: Seq[InterestAccountModel] = accounts.filterByTaxType(TAXED)

        filteredAccounts.length shouldBe 1
        filteredAccounts.head shouldBe accounts.head
      }

      "return untaxed accounts when 'UNTAXED' tax type is specified" in {
        val filteredAccounts: Seq[InterestAccountModel] = accounts.filterByTaxType(UNTAXED)

        filteredAccounts.length shouldBe 2
        filteredAccounts.head shouldBe accounts(1)
        filteredAccounts(1) shouldBe accounts(2)
      }

      "return untaxed accounts when neither 'TAXED' or 'UNTAXED' tax type is specified" in {
        val filteredAccounts: Seq[InterestAccountModel] = accounts.filterByTaxType("type")

        filteredAccounts.length shouldBe 2
        filteredAccounts.head shouldBe accounts(1)
        filteredAccounts(1) shouldBe accounts(2)
      }
    }
  }
}
