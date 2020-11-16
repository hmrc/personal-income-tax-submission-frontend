/*
 * Copyright 2020 HM Revenue & Customs
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

package models

import play.api.libs.json.{JsObject, Json}
import utils.UnitTest

class DividendsCheckYourAnswersModelSpec extends UnitTest {

  val jsonMax: JsObject = Json.obj(
    "ukDividends" -> Some(true),
    "ukDividendsAmount" -> 5,
    "otherUkDividends" -> Some(true),
    "otherUkDividendsAmount" -> 10
  )

  val jsonMin: JsObject = Json.obj(
    "ukDividends" -> false,
    "otherUkDividends" -> false
  )

  val modelMax = DividendsCheckYourAnswersModel(
    ukDividends = Some(true),
    Some(5),
    otherUkDividends = Some(true),
    Some(10)
  )

  val modelMin = DividendsCheckYourAnswersModel(Some(false), None, Some(false), None)

  "DividendsCheckYourAnswersModel" should {

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

        jsonMax.as[DividendsCheckYourAnswersModel] shouldBe modelMax

      }

      "all optional fields are empty" in {

        jsonMin.as[DividendsCheckYourAnswersModel] shouldBe modelMin

      }

    }

  }

  ".isFinished " should {

    "return true" when {

      "ukDividends are false and otherDividends are false" in {

        DividendsCheckYourAnswersModel(ukDividends = Some(false), otherUkDividends = Some(false)).isFinished shouldBe(true)
      }
      "ukDividends are false and otherDividends are true with an amount" in {

        DividendsCheckYourAnswersModel(ukDividends = Some(false), otherUkDividends = Some(true), otherUkDividendsAmount = Some(500))
          .isFinished shouldBe(true)
      }
      "ukDividends are true with an amount and otherDividends are false" in {

        DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(500), otherUkDividends = Some(false))
          .isFinished shouldBe(true)
      }
      "ukDividends are true with an amount and otherDividends are true with an amount" in {

        DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(500), otherUkDividends = Some(true),
          otherUkDividendsAmount = Some(500))
          .isFinished shouldBe(true)
      }
    }
  }

}
