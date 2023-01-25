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

import play.api.libs.json.{JsObject, JsResultException, Json}
import utils.UnitTest

class InterestAccountModelSpec extends UnitTest {

  val validJsonStandardReadsMax: JsObject = Json.obj(
    "id" -> "qwerty",
    "accountName" -> "TSB",
    "untaxedAmount" -> 500,
    "taxedAmount" -> 500,
    "uniqueSessionId" -> "ytrewq"
  )

  val validModelStandardReadsMax: InterestAccountModel = InterestAccountModel(
    Some("qwerty"),
    "TSB",
    Some(500.00),
    Some(500.00),
    Some("ytrewq")
  )

  val validJsonStandardReadsMin: JsObject = Json.obj(
    "accountName" -> "TSB"
  )

  val validModelStandardReadsMin: InterestAccountModel = InterestAccountModel(
    None,
    "TSB",
    None,
    None,
    None
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
    Some(500.00)
  )

  val validTaxedModel: InterestAccountModel = InterestAccountModel(
    Some("azerty"),
    "Lloyds",
    taxedAmount = Some(300.00)
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

}
