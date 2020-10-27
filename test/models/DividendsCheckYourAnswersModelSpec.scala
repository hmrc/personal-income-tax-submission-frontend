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
    "ukDividends" -> true,
    "ukDividendsAmount" -> 5,
    "otherDividends" -> true,
    "otherDividendsAmount" -> 10
  )

  val jsonMin: JsObject = Json.obj(
    "ukDividends" -> false,
    "otherDividends" -> false
  )

  val modelMax = DividendsCheckYourAnswersModel(
    ukDividends = true,
    Some(5),
    otherDividends = true,
    Some(10)
  )

  val modelMin = DividendsCheckYourAnswersModel()

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

}
