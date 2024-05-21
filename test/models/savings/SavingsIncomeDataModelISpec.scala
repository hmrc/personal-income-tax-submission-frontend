/*
 * Copyright 2024 HM Revenue & Customs
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

package models.savings

import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.libs.json.{JsObject, Json}
import utils.UnitTest

class SavingsIncomeDataModelISpec extends UnitTest {

  val securities: SecuritiesModel =
    SecuritiesModel(
      Some(800.67),
      7455.99,
      Some(6123.2)
    )

  val foreignInterest: Seq[ForeignInterestModel] =
    Seq(
      ForeignInterestModel(
        "BES",
        Some(1232.56),
        Some(3422.22),
        Some(5622.67),
        Some(true),
        2821.92
      )
    )

  val aSavingsIncomeDataModel: SavingsIncomeDataModel =
    SavingsIncomeDataModel(
      submittedOn = Some(""),
      securities = Some(securities),
      foreignInterest = Some(foreignInterest)
    )

  val validJson: JsObject = Json.obj(
    "submittedOn" -> "",
    "securities" -> Json.toJson(securities),
    "foreignInterest" -> Json.toJson(foreignInterest)
  )

  val validModel: SavingsIncomeDataModel = aSavingsIncomeDataModel

  "SavingsIncomeDataModel" should {

    "correctly parse from Json" in {
      validJson.as[SavingsIncomeDataModel] mustBe validModel
    }

    "correctly parse to Json" in {
      Json.toJson(validModel) mustBe validJson
    }

  }

  "SavingsIncomeDataModel.hasNonZeroData" should {

    "return true" when {

      "securities has non zero data" in {
        aSavingsIncomeDataModel.securities.exists(_.hasNonZeroData) shouldBe true
      }

      "foreignInterest has non zero data" in {
        aSavingsIncomeDataModel.foreignInterest.exists(_.exists(_.hasNonZeroData)) shouldBe true
      }

    }

    "return false" when {

      "all fields have no values" in {
        aSavingsIncomeDataModel.copy(
          submittedOn = Some(""),
          securities = None,
          foreignInterest = None
        ).hasNonZeroData shouldBe false
      }

      "all fields have zero values" in {
        aSavingsIncomeDataModel.copy(
          submittedOn = Some(""),
          securities = Some(securities.copy(Some(0), 0, Some(0))),
          foreignInterest = Some(Seq(ForeignInterestModel("GB", Some(0), Some(0), Some(0), None, 0)))
        ).hasNonZeroData shouldBe false
      }

    }

  }

  "securities.hasNonZeroData" should {

    "return true" when {

      "securities has non zero data" in {
        securities.hasNonZeroData shouldBe true
      }

      "all fields have no values" in {
        securities.copy(None, 0, None).hasNonZeroData shouldBe true
      }

    }

    "return false" when {

      "all fields have zero values" in {
        securities.copy(Some(0), 0, Some(0)).hasNonZeroData shouldBe false
      }

    }

  }

  "foreignInterest.hasNonZeroData" should {

    "return true" when {

      "amountBeforeTax has non zero data" in {
        foreignInterest.exists(_.hasNonZeroData) shouldBe true
      }

      "taxTakenOff has non zero data" in {
        foreignInterest.exists(_.copy("GB", None, Some(2), None, None, 0).hasNonZeroData) shouldBe true
      }

      "specialWithholdingTax has non zero data" in {
        foreignInterest.exists(_.copy("GB", None, None, Some(2), None, 0).hasNonZeroData) shouldBe true
      }

      "taxableAmount has non zero data" in {
        foreignInterest.exists(_.copy("GB", None, None, None, None, 2).hasNonZeroData) shouldBe true
      }

      "all fields except countryCode and taxableAmount have no values" in {
        foreignInterest.exists(_.copy("GB", None, None, None, None, 0).hasNonZeroData) shouldBe true
      }

    }

    "return false" when {

      "all fields except countryCode have zero values" in {
        foreignInterest.exists(_.copy("GB", Some(0), Some(0), Some(0), None, 0).hasNonZeroData) shouldBe false
      }

    }

  }

}
