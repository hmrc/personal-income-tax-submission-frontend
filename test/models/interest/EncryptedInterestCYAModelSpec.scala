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

import play.api.libs.json.{JsObject, Json}
import utils.{EncryptedValue, UnitTest}

class EncryptedInterestCYAModelSpec extends UnitTest {
  val account = EncryptedInterestAccountModel(
    id = Some(EncryptedValue("someId", "someId-Nonce")),
    accountName = EncryptedValue("someName", "someName-Nonce"),
    amount = Some(EncryptedValue("100.00", "100.00-Nonce"))
  )

  val modelMax: EncryptedInterestCYAModel = EncryptedInterestCYAModel(
    gateway = None,
    untaxedUkInterest = Some(EncryptedValue("true", "true-Nonce")),
    taxedUkInterest = Some(EncryptedValue("true", "true-Nonce")),
    untaxedAccounts = Seq(account),
    taxedAccounts = Seq(account)
  )

  val jsonMax: JsObject = Json.obj(
    "untaxedUkInterest" -> Json.obj(
      "value" -> "true",
      "nonce" -> "true-Nonce"
    ),
    "taxedUkInterest" -> Json.obj(
      "value" -> "true",
      "nonce" -> "true-Nonce"
    ),
    "untaxedAccounts" -> Json.arr(
      Json.obj(
        "id" -> Json.obj(
          "value" -> "someId",
          "nonce" -> "someId-Nonce"
        ),
        "accountName" -> Json.obj(
          "value" -> "someName",
          "nonce" -> "someName-Nonce"
        ),
        "amount" -> Json.obj(
          "value" -> "100.00",
          "nonce" -> "100.00-Nonce"
        )
      )
    ),
    "taxedAccounts" -> Json.arr(
      Json.obj(
        "id" -> Json.obj(
          "value" -> "someId",
          "nonce" -> "someId-Nonce"
        ),
        "accountName" -> Json.obj(
          "value" -> "someName",
          "nonce" -> "someName-Nonce"
        ),
        "amount" -> Json.obj(
          "value" -> "100.00",
          "nonce" -> "100.00-Nonce"
        )
      )
    )
  )

  val jsonNoAccounts: JsObject = Json.obj(
    "untaxedUkInterest" -> Json.obj(
      "value" -> "true",
      "nonce" -> "true-Nonce"
    ),
    "taxedUkInterest" -> Json.obj(
      "value" -> "false",
      "nonce" -> "false-Nonce"
    )
  )

  val modelMin: EncryptedInterestCYAModel = EncryptedInterestCYAModel(
    untaxedUkInterest = None,
    taxedUkInterest = None,
    untaxedAccounts = Seq.empty,
    taxedAccounts = Seq.empty
  )

  val modelNoAccounts: EncryptedInterestCYAModel = EncryptedInterestCYAModel(
    untaxedUkInterest = Some(EncryptedValue("true", "true-Nonce")),
    taxedUkInterest = Some(EncryptedValue("false", "false-Nonce"))
  )

  val jsonMin: JsObject = Json.obj()

  "EncryptedInterestCYAModel" should {

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
        jsonMax.as[EncryptedInterestCYAModel] shouldBe modelMax
      }

      "no fields are present" in {
        jsonMin.as[EncryptedInterestCYAModel] shouldBe modelMin
      }

      "accounts not specified" in {
        jsonNoAccounts.as[EncryptedInterestCYAModel] shouldBe modelNoAccounts
      }
    }

  }
}