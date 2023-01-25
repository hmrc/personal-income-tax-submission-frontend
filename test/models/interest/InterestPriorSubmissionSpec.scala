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

import play.api.libs.json.{JsArray, JsObject, Json}
import utils.UnitTest

class InterestPriorSubmissionSpec extends UnitTest {

  val validJsonRead: JsArray = Json.arr(
    Json.obj(
      "accountName" -> "TSB Account",
      "incomeSourceId" -> "qwerty",
      "taxedUkInterest" -> 500
    ),
    Json.obj(
      "accountName" -> "Lloyds Savings",
      "incomeSourceId" -> "azerty",
      "untaxedUkInterest" -> 3000
    )
  )

  val validJsonReadUntaxed: JsArray = Json.arr(
    Json.obj(
      "accountName" -> "Lloyds Savings",
      "incomeSourceId" -> "azerty",
      "untaxedUkInterest" -> 3000
    )
  )

  val validJsonReadTaxed: JsArray = Json.arr(
    Json.obj(
      "accountName" -> "TSB Account",
      "incomeSourceId" -> "qwerty",
      "taxedUkInterest" -> 500
    )
  )

  val validJsonWrite: JsObject = Json.obj(
    "submissions" -> Json.arr(
      Json.obj(
        "id" -> "qwerty",
        "accountName" -> "TSB Account",
        "taxedAmount" -> 500
      ),
      Json.obj(
        "id" -> "azerty",
        "accountName" -> "Lloyds Savings",
        "untaxedAmount" -> 3000
      )
    )
  )

  val validModel = InterestPriorSubmission(
    hasUntaxed = true,
    hasTaxed = true,
    Seq(
      InterestAccountModel(
        Some("qwerty"),
        "TSB Account",
        taxedAmount = Some(500.00)
      ),
      InterestAccountModel(
        Some("azerty"),
        "Lloyds Savings",
        untaxedAmount = Some(3000.00)
      )
    )
  )

  val validModelUntaxed = InterestPriorSubmission(
    hasUntaxed = true,
    hasTaxed = false,
    Seq(
      InterestAccountModel(
        Some("azerty"),
        "Lloyds Savings",
        untaxedAmount = Some(3000.00)
      )
    )
  )

  val validModelTaxed = InterestPriorSubmission(
    hasUntaxed = false,
    hasTaxed = true,
    Seq(
      InterestAccountModel(
        Some("qwerty"),
        "TSB Account",
        taxedAmount = Some(500.00)
      )
    )
  )

  "should correctly parse to json" in {
    implicit val writes = InterestPriorSubmission.writes
    Json.toJson(validModel) shouldBe validJsonWrite
  }

}
