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

import play.api.libs.json.{JsObject, JsResultException, Json}
import utils.{StubClock, UnitTest}

import java.time.{LocalDate, LocalDateTime, LocalTime}

class InterestAccountModelSpec extends UnitTest {

  val validJsonStandardReadsMax: JsObject = Json.obj(
    "id" -> "qwerty",
    "accountName" -> "TSB",
    "untaxedAmount" -> 500,
    "taxedAmount" -> 500,
    "uniqueSessionId" -> "ytrewq",
    "createdAt" -> "2021-01-01T10:10:10.00000001"
  )

  val validModelStandardReadsMax: InterestAccountModel = InterestAccountModel(
    Some("qwerty"),
    "TSB",
    Some(500.00),
    Some(500.00),
    Some("ytrewq"),
    createdAt = StubClock.localDateTimeNow()
  )

  val validJsonStandardReadsMin: JsObject = Json.obj(
    "accountName" -> "TSB",
    "createdAt" -> "2021-01-01T10:10:10.00000001"
  )

  val validModelStandardReadsMin: InterestAccountModel = InterestAccountModel(
    None,
    "TSB",
    None,
    None,
    None,
    StubClock.localDateTimeNow()
  )

  "using the normal json parsing" should {

    "correctly parse from json" when {

      "the json contains all possible values" in {
        validJsonStandardReadsMax.as[InterestAccountModel] shouldBe validModelStandardReadsMax
      }

      "the json contains only mandatory fields" in {
        validJsonStandardReadsMin.as[InterestAccountModel] shouldBe validModelStandardReadsMin
      }

    }

    "correctly parse to json" when {

      "the model contains all possible values" in {
        Json.toJson(validModelStandardReadsMax) shouldBe validJsonStandardReadsMax
      }

      "the model contains only mandatory fields" in {
        Json.toJson(validModelStandardReadsMin) shouldBe validJsonStandardReadsMin
      }

    }

  }

  val validJsonAlternativeUntaxed: JsObject = Json.obj(
    "accountName" -> "TSB",
    "incomeSourceId" -> "qwerty",
    "untaxedUkInterest" -> 500.00
  )

  val validJsonAlternativeTaxed: JsObject = Json.obj(
    "accountName" -> "Lloyds",
    "incomeSourceId" -> "azerty",
    "untaxedUkInterest" -> 300.00
  )

  val validUntaxedModel: InterestAccountModel = InterestAccountModel(
    Some("qwerty"),
    "TSB",
    Some(500.00),
    createdAt = StubClock.localDateTimeNow()
  )

  val validTaxedModel: InterestAccountModel = InterestAccountModel(
    Some("azerty"),
    "Lloyds",
    taxedAmount = Some(300.00),
    createdAt = StubClock.localDateTimeNow()
  )

  "using alternative json reads" should {

    "correctly read json into model" when {

      "the json is a valid untaxed submission" in {
        validJsonAlternativeUntaxed.as[InterestAccountModel](InterestAccountModel.priorSubmissionReads)
      }

      "the json is a valid taxed submission" in {
        validJsonAlternativeTaxed.as[InterestAccountModel](InterestAccountModel.priorSubmissionReads)
      }

    }

    "return a json read exception" when {

      "the unique id is missing" in {
        intercept[JsResultException](
          Json.obj(
            "accountName" -> "Lloyds",
            "untaxedUkInterest" -> 300.00
          ).as[InterestAccountModel](InterestAccountModel.priorSubmissionReads)
        )
      }

      "the account name is missing" in {
        intercept[JsResultException](
          Json.obj(
            "incomeSourceId" -> "qwerty",
            "untaxedUkInterest" -> 300.00
          ).as[InterestAccountModel](InterestAccountModel.priorSubmissionReads)
        )
      }

    }

  }

  "sorting seq of accounts using .sorted" should {
    "return accounts in ascending order by date created" in {

      val account1 = InterestAccountModel(
        Some("account 1"),
        "TSB",
        Some(500.00),
        createdAt = LocalDateTime.of(LocalDate.of(2022, 5, 1), LocalTime.of(10, 10, 10, 9))
      )

      val account2 = InterestAccountModel(
        Some("account 2"),
        "Lloyds",
        taxedAmount = Some(300.00),
        createdAt = LocalDateTime.of(LocalDate.of(2022, 5, 1), LocalTime.of(23, 0, 0, 2))
      )

      val account3 = InterestAccountModel(
        Some("account 3"),
        "Barclays",
        taxedAmount = Some(300.00),
        createdAt = LocalDateTime.of(LocalDate.of(2022, 4, 4), LocalTime.of(10, 10, 10, 10))
      )

      val account4 = InterestAccountModel(
        Some("account 4"),
        "NatWest",
        taxedAmount = Some(300.00),
        createdAt = LocalDateTime.of(LocalDate.of(2022, 5, 1), LocalTime.of(23, 0, 0, 0))
      )

      val account5 = InterestAccountModel(
        Some("account 5"),
        "Nationwide",
        taxedAmount = Some(300.00),
        createdAt = LocalDateTime.of(LocalDate.of(2022, 4, 4), LocalTime.of(9, 15, 0, 0))
      )

      val accounts = Seq(account1, account2, account3, account4, account5)

      accounts.sorted shouldBe Seq(account5, account3, account1, account4, account2)
    }
  }
}
