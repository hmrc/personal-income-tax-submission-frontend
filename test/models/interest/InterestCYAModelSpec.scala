/*
 * Copyright 2023 HM Revenue & Customs
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
    Some(100.00),
    Some(100.00)
  )

  val modelMax: InterestCYAModel = InterestCYAModel(
    None,
    untaxedUkInterest = Some(true),
    taxedUkInterest = Some(true),
    Seq(account)
  )

  val jsonMax: JsObject = Json.obj(
    "untaxedUkInterest" -> true,
    "taxedUkInterest" -> true,
    "accounts" -> Json.arr(
      Json.obj(
        "id" -> "someId",
        "accountName" -> "someName",
        "untaxedAmount" -> 100.00,
        "taxedAmount" -> 100.00
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
    accounts = Seq()
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
      taxedAmount = Some(100.01)
    ),
    InterestAccountModel(
      Some("4"),
      "New Account",
      taxedAmount = Some(50.0)
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
          Seq(account.copy(taxedAmount = None))
        ).isFinished shouldBe false
      }

      "untaxed interest exist with no accounts and taxed interest exist with accounts" in {
        InterestCYAModel(
          None,
          Some(true),
          Some(true),
          Seq(account.copy(untaxedAmount = None))
        ).isFinished shouldBe false
      }

      "untaxed and taxed interest exist, each with no accounts" in {
        InterestCYAModel(
          None,
          Some(true),
          Some(true),
          Seq()
        ).isFinished shouldBe false
      }

      "untaxed interest is true with accounts and taxed interest does not exist" in {
        InterestCYAModel(
          untaxedUkInterest = Some(true),
          taxedUkInterest = None,
          accounts = Seq(account.copy(taxedAmount = None))
        ).isFinished shouldBe false
      }

      "taxed interest is true with accounts and untaxed interest does not exist" in {
        InterestCYAModel(
          untaxedUkInterest = None,
          taxedUkInterest = Some(true),
          accounts = Seq(account.copy(untaxedAmount = None))
        ).isFinished shouldBe false
      }

    }

    "return true" when {

        "untaxed interest is true with accounts and taxed interest is false" in {
          InterestCYAModel(
            untaxedUkInterest = Some(true),
            taxedUkInterest = Some(false),
            accounts = Seq(account.copy(taxedAmount = None))
          ).isFinished shouldBe true
        }

      "taxed interest is true with accounts and untaxed interest is false" in {
        InterestCYAModel(
          untaxedUkInterest = Some(false),
          taxedUkInterest = Some(true),
          accounts = Seq(account.copy(untaxedAmount = None))
        ).isFinished shouldBe true
      }
    }
  }

  ".disallowedDuplicateNames" should {

    "return names of specified accounts where accounts only have taxed amount defined and if tax type specified is 'TAXED'" in {
      val cyaModel = Some(InterestCYAModel(
        untaxedUkInterest = Some(false),
        taxedUkInterest = Some(true),
        accounts = accounts
      ))

      val accountNames = InterestCYAModel.disallowedDuplicateNames(cyaModel, "2", InterestTaxTypes.TAXED)

      accountNames.length shouldBe 2
      accountNames shouldBe Seq("Account 1", "New Account")
    }

    "return names of specified accounts where accounts only have untaxed amount defined and if tax type specified is 'UNTAXED'" in {
      val cyaModel = Some(InterestCYAModel(
        untaxedUkInterest = Some(false),
        taxedUkInterest = Some(true),
        accounts = accounts
      ))

      val accountNames = InterestCYAModel.disallowedDuplicateNames(cyaModel, "4", InterestTaxTypes.UNTAXED)

      accountNames.length shouldBe 2
      accountNames shouldBe Seq("TSB Account", "Lloyds Savings")
    }

    "return empty list if cya model is None" in {
      val accountNames = InterestCYAModel.disallowedDuplicateNames(None, "4", InterestTaxTypes.UNTAXED)

      accountNames shouldBe empty
    }
  }

  ".getCyaModel" should {

    "return cya data if prior is defined " in {
      val prior = Some(InterestPriorSubmission(true, false, submissions = accounts))

      val cyaData = InterestCYAModel.getCyaModel(None, prior)

      cyaData shouldBe Some(InterestCYAModel(Some(true), Some(true), Some(false),
        List(InterestAccountModel(Some("1"),
          "TSB Account",
          Some(500.0),
          None, None),
          InterestAccountModel(Some("2"), "Lloyds Savings", Some(3000.0), None, None),
          InterestAccountModel(Some("3"), "Account 1", None, Some(100.01), None),
          InterestAccountModel(Some("4"), "New Account", None, Some(50.0), None))))
    }

    "return cya data if cya model is defined and prior isn't defined " in {
      val cyaModel = Some(InterestCYAModel(
        untaxedUkInterest = Some(false),
        taxedUkInterest = Some(true),
        accounts = accounts
      ))

      val cyaData = InterestCYAModel.getCyaModel(cyaModel, None)

      cyaData shouldBe Some(InterestCYAModel(None, Some(false), Some(true),
        List(InterestAccountModel(Some("1"), "TSB Account", Some(500.0), None, None),
          InterestAccountModel(Some("2"), "Lloyds Savings", Some(3000.0), None, None),
          InterestAccountModel(Some("3"), "Account 1", None, Some(100.01), None),
          InterestAccountModel(Some("4"), "New Account", None, Some(50.0), None))))
    }

    "return None if cya model isn't defined and prior isn't defined " in {
      val cyaData = InterestCYAModel.getCyaModel(None, None)

      cyaData shouldBe None
    }
  }
}