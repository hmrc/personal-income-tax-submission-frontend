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

package models.dividends

import config.MockAppConfig
import play.api.libs.json.{JsObject, Json}
import utils.UnitTest

class StockDividendsCheckYourAnswersModelSpec extends UnitTest {

  object TailoringAppConfig extends MockAppConfig {
    override val tailoringEnabled: Boolean = true
    override val interestTailoringEnabled: Boolean = true
    override val dividendsTailoringEnabled: Boolean = true
  }

  val jsonMax: JsObject = Json.obj(
    "ukDividends" -> Some(true),
    "ukDividendsAmount" -> 5.00,
    "otherUkDividends" -> Some(true),
    "otherUkDividendsAmount" -> 10.00,
    "stockDividends" -> Some(true),
    "stockDividendsAmount" -> 10.00,
    "redeemableShares" -> Some(true),
    "redeemableSharesAmount" -> 10.00,
    "closeCompanyLoansWrittenOff" -> Some(true),
    "closeCompanyLoansWrittenOffAmount" -> 10.00
  )

  val jsonMin: JsObject = Json.obj(
    "ukDividends" -> false,
    "otherUkDividends" -> false,
    "stockDividends" -> false,
    "redeemableShares" -> false,
    "closeCompanyLoansWrittenOff" -> false
  )

  val modelMax: StockDividendsCheckYourAnswersModel = StockDividendsCheckYourAnswersModel(
    None,
    ukDividends = Some(true),
    Some(5.00),
    otherUkDividends = Some(true),
    Some(10.00),
    stockDividends = Some(true),
    Some(10.00),
    redeemableShares = Some(true),
    Some(10.00),
    closeCompanyLoansWrittenOff = Some(true),
    Some(10.00)
  )

  val modelMin: StockDividendsCheckYourAnswersModel =
    StockDividendsCheckYourAnswersModel(
      None,
      Some(false), None,
      Some(false), None,
      Some(false), None,
      Some(false), None,
      Some(false), None
    )

  "StockDividendsCheckYourAnswersModel" should {

    "correctly parse to Json" when {

      "all fields are populated" in {
        Json.toJson(modelMax) shouldBe jsonMax
      }

      "all optional fields are empty" in {
        Json.toJson(modelMin) shouldBe jsonMin
      }

    }

    "correctly parse from Json" when {

      "all fields are populated" in {
        jsonMax.as[StockDividendsCheckYourAnswersModel] shouldBe modelMax
      }

      "all optional fields are empty" in {
        jsonMin.as[StockDividendsCheckYourAnswersModel] shouldBe modelMin
      }

    }
  }

}
