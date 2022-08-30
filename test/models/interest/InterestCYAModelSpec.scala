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
import play.api.libs.json.{JsObject, Json}
import utils.UnitTest

class InterestCYAModelSpec extends UnitTest {
  val account = InterestAccountModel(
    id = Some("someId"),
    accountName = "someName",
    Some(100.00)
  )

  val modelMax: InterestCYAModel = InterestCYAModel(
    None,
    untaxedUkInterest = Some(true),
    taxedUkInterest = Some(true),
    untaxedAccounts = Seq(account)
  )

  val jsonMax: JsObject = Json.obj(
    "untaxedUkInterest" -> true,
    "taxedUkInterest" -> true,
    "untaxedAccounts" -> Json.arr(
      Json.obj(
        "id" -> "someId",
        "accountName" -> "someName",
        "amount" -> 100.00
      )
    )
  )

  val jsonNoAccounts: JsObject = Json.obj(
    "untaxedUkInterest" -> true,
    "taxedUkInterest" -> false
  )

  val modelMin: InterestCYAModel = InterestCYAModel(
    untaxedUkInterest = None,
    taxedUkInterest = None,
    untaxedAccounts = Seq.empty,
    taxedAccounts = Seq.empty
  )

  val modelNoAccounts: InterestCYAModel = InterestCYAModel(
    untaxedUkInterest = Some(true),
    taxedUkInterest = Some(false)
  )

  val jsonMin: JsObject = Json.obj()

  val accounts: Seq[InterestAccountModel] = Seq(
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
      amount = Some(100.01)
    ),
    InterestAccountModel(
      Some("4"),
      "New Account",
      amount = Some(50.0)
    )
  )

  "InterestCYAModel" should {

    "correctly parse to json" when {

      "all fields are present" in {
        Json.toJson(modelMax) shouldBe jsonMax
      }

      "no fields are present" in {
        Json.toJson(modelMin) shouldBe jsonMin
      }

      "accounts not specified" in {
        Json.toJson(modelNoAccounts) shouldBe jsonNoAccounts
      }
    }

    "correctly parse from json" when {

      "all fields are present" in {
        jsonMax.as[InterestCYAModel] shouldBe modelMax
      }

      "no fields are present" in {
        jsonMin.as[InterestCYAModel] shouldBe modelMin
      }

      "accounts not specified" in {
        jsonNoAccounts.as[InterestCYAModel] shouldBe modelNoAccounts
      }
    }

  }

  ".isFinished" should {

    "return false" when {

      "untaxed interest exist with accounts and taxed interest exist with no accounts" in {
        InterestCYAModel(
          None,
          Some(true),
          Some(true),
          untaxedAccounts = Seq.empty,
          taxedAccounts = Seq(account.copy(amount = None))
        ).isFinished shouldBe false
      }

      "untaxed interest exist with no accounts and taxed interest exist with accounts" in {
        InterestCYAModel(
          None,
          Some(true),
          Some(true),
          untaxedAccounts = Seq.empty,
          taxedAccounts = Seq(account.copy(amount = None))
        ).isFinished shouldBe false
      }

      "untaxed and taxed interest exist, each with no accounts" in {
        InterestCYAModel(
          None,
          Some(true),
          Some(true),
          untaxedAccounts = Seq.empty,
          taxedAccounts = Seq.empty
        ).isFinished shouldBe false
      }

      "untaxed interest is true with accounts and taxed interest does not exist" in {
        InterestCYAModel(
          untaxedUkInterest = Some(true),
          taxedUkInterest = None,
          untaxedAccounts = Seq.empty,
          taxedAccounts = Seq(account.copy(amount = None))
        ).isFinished shouldBe false
      }

      "taxed interest is true with accounts and untaxed interest does not exist" in {
        InterestCYAModel(
          untaxedUkInterest = None,
          taxedUkInterest = Some(true),
          untaxedAccounts = Seq.empty,
          taxedAccounts = Seq(account.copy(amount = None))
        ).isFinished shouldBe false
      }

    }

    "return true" when {

      "gateway is false and untaxed interest is false and taxed interest is false" in {
        InterestCYAModel(
          gateway = Some(true),
          untaxedUkInterest = Some(false),
          taxedUkInterest = Some(false)
        ).isFinished shouldBe true
      }

      "untaxed interest is false and taxed interest is false" in {
        InterestCYAModel(
          untaxedUkInterest = Some(false),
          taxedUkInterest = Some(false)
        ).isFinished shouldBe true
      }

      "taxed interest is true with accounts and untaxed interest is true" in {
        InterestCYAModel(
          untaxedUkInterest = Some(true),
          taxedUkInterest = Some(true),
          untaxedAccounts = Seq(account.copy(amount = Some(300))),
          taxedAccounts = Seq(account.copy(amount = Some(300)))
        ).isFinished shouldBe true
      }
    }
  }

  ".disallowedDuplicateNames" should {

    "return names of specified accounts where accounts only have taxed amount defined and if tax type specified is 'TAXED'" in {
      val cyaModel = Some(InterestCYAModel(
        untaxedUkInterest = Some(false),
        taxedUkInterest = Some(true),
        untaxedAccounts = accounts,
        taxedAccounts = accounts
      ))

      val accountNames = InterestCYAModel.disallowedDuplicateNames(cyaModel, "2", InterestTaxTypes.TAXED)

      accountNames.length shouldBe 6
      accountNames shouldBe Seq("TSB Account", "Account 1", "New Account", "TSB Account", "Account 1", "New Account")
    }

    "return names of specified accounts where accounts only have untaxed amount defined and if tax type specified is 'UNTAXED'" in {
      val cyaModel = Some(InterestCYAModel(
        untaxedUkInterest = Some(false),
        taxedUkInterest = Some(true),
        untaxedAccounts = accounts,
        taxedAccounts = accounts
      ))

      val accountNames = InterestCYAModel.disallowedDuplicateNames(cyaModel, "4", InterestTaxTypes.UNTAXED)

      accountNames.length shouldBe 6
      accountNames shouldBe Seq("TSB Account", "Lloyds Savings", "Account 1", "TSB Account", "Lloyds Savings", "Account 1")
    }

    "return empty list if cya model is None" in {
      val accountNames = InterestCYAModel.disallowedDuplicateNames(None, "4", InterestTaxTypes.UNTAXED)

      accountNames shouldBe empty
    }
  }

  ".getCyaModel" should {

    "return cya data if prior is defined " in {
      val prior = Some(InterestPriorSubmission(true, false, submissions = accounts.map(x => InterestAccountSourceModel(x.id, x.accountName, x.amount, None, x.uniqueSessionId))))

      val cyaData = InterestCYAModel.getCyaModel(None, prior)

      cyaData shouldBe Some(InterestCYAModel(Some(true), Some(true), Some(false),
        List(InterestAccountModel(Some("1"),
          "TSB Account",
          Some(500.0)),
          InterestAccountModel(Some("2"), "Lloyds Savings", Some(3000.0), None),
          InterestAccountModel(Some("3"), "Account 1", Some(100.01), None),
          InterestAccountModel(Some("4"), "New Account", Some(50.0), None))))
    }

    "return cya data if cya model is defined and prior isn't defined " in {
      val cyaModel = Some(InterestCYAModel(
        untaxedUkInterest = Some(false),
        taxedUkInterest = Some(true),
        untaxedAccounts = accounts
      ))

      val cyaData = InterestCYAModel.getCyaModel(cyaModel, None)

      cyaData shouldBe Some(InterestCYAModel(None, Some(false), Some(true),
        List(InterestAccountModel(Some("1"), "TSB Account", Some(500.0), None),
          InterestAccountModel(Some("2"), "Lloyds Savings", Some(3000.0), None),
          InterestAccountModel(Some("3"), "Account 1", Some(100.01), None),
          InterestAccountModel(Some("4"), "New Account", Some(50.0), None))))
    }

    "return None if cya model isn't defined and prior isn't defined " in {
      val cyaData = InterestCYAModel.getCyaModel(None, None)

      cyaData shouldBe None
    }
  }
}