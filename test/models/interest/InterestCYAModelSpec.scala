/*
 * Copyright 2021 HM Revenue & Customs
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

import play.api.libs.json.{JsObject, Json}
import utils.UnitTest

class InterestCYAModelSpec extends UnitTest {
  val account = InterestAccountModel(
    id = Some("someId"),
    accountName = "someName",
    amount = 100.00
  )

  val modelMax: InterestCYAModel = InterestCYAModel(
    untaxedUkInterest = Some(true),
    untaxedUkAccounts = Some(Seq(account)),
    taxedUkInterest = Some(true),
    taxedUkAccounts = Some(Seq(account, account))
  )

  val jsonMax: JsObject = Json.obj(
    "untaxedUkInterest" -> true,
    "untaxedUkAccounts" -> Json.arr(
      Json.obj(
        "id" -> "someId",
        "accountName" -> "someName",
        "amount" -> 100.00
      )
    ),
    "taxedUkInterest" -> true,
    "taxedUkAccounts" -> Json.arr(
      Json.obj(
        "id" -> "someId",
        "accountName" -> "someName",
        "amount" -> 100.00
      ),
      Json.obj(
        "id" -> "someId",
        "accountName" -> "someName",
        "amount" -> 100.00
      )
    )
  )

  val modelMin: InterestCYAModel = InterestCYAModel(
    untaxedUkInterest = None,
    untaxedUkAccounts = None,
    taxedUkInterest = None,
    taxedUkAccounts = None
  )

  val jsonMin: JsObject = Json.obj()

  "InterestCYAModel" should {

    "correctly parse to json" when {

      "all fields are present" in {
        Json.toJson(modelMax) shouldBe jsonMax
      }

      "no fields are present" in {
        Json.toJson(modelMin) shouldBe jsonMin
      }

    }

    "correctly parse from json" when {

      "all fields are present" in {
        jsonMax.as[InterestCYAModel] shouldBe modelMax
      }

      "no fields are present" in {
        jsonMin.as[InterestCYAModel] shouldBe modelMin
      }
    }

  }

  ".isFinished" should {

    "return false" when {

      "untaxed interest exist with accounts and taxed interest exist with no accounts" in {
        InterestCYAModel(
          Some(true),
          Some(Seq(account)),
          Some(true),
          None
        ).isFinished shouldBe false
      }

      "untaxed interest exist with no accounts and taxed interest exist with accounts" in {
        InterestCYAModel(
          Some(true),
          None,
          Some(true),
          Some(Seq(account))
        ).isFinished shouldBe false
      }

      "untaxed and taxed interest exist, each with no accounts" in {
        InterestCYAModel(
          Some(true),
          None,
          Some(true),
          None
        ).isFinished shouldBe false
      }

      "untaxed interest is true with accounts and taxed interest does not exist" in {
        InterestCYAModel(
          untaxedUkInterest = Some(true),
          untaxedUkAccounts = Some(Seq(account)),
          taxedUkInterest = None,
          taxedUkAccounts = None
        ).isFinished shouldBe false
      }

      "taxed interest is true with accounts and untaxed interest does not exist" in {
        InterestCYAModel(
          untaxedUkInterest = None,
          untaxedUkAccounts = None,
          taxedUkInterest = Some(true),
          taxedUkAccounts = Some(Seq(account))
        ).isFinished shouldBe false
      }

    }

    "return true" when {

        "untaxed interest is true with accounts and taxed interest is false" in {
          InterestCYAModel(
            untaxedUkInterest = Some(true),
            untaxedUkAccounts = Some(Seq(account)),
            taxedUkInterest = Some(false),
            taxedUkAccounts = None
          ).isFinished shouldBe true
        }

        "taxed interest is true with accounts and untaxed interest is false" in {
          InterestCYAModel(
            untaxedUkInterest = Some(false),
            untaxedUkAccounts = None,
            taxedUkInterest = Some(true),
            taxedUkAccounts = Some(Seq(account))
          ).isFinished shouldBe true
        }

    }

  }

}
