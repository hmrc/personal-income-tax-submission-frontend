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

package models.mongo

import models.mongo.JourneyStatus._
import play.api.libs.json.{JsString, JsSuccess, Json}
import utils.UnitTest

class JourneyStatusSpec extends UnitTest {

  "JourneyStatus" should {
    "contain the correct values" in {
      JourneyStatus.values shouldBe Seq(
        JourneyStatus.NotStarted,
        JourneyStatus.InProgress,
        JourneyStatus.Completed
      )
    }
  }

  "JourneyStatus values" should {
    "parse to and from json" in {
      val jsValues = values.map(s => Json.toJson(s))
      jsValues shouldBe Seq(JsString("notStarted"), JsString("inProgress"), JsString("completed"))

      val results = jsValues.map(s => s.validate[JourneyStatus])
      results shouldBe Seq(JsSuccess(NotStarted), JsSuccess(InProgress), JsSuccess(Completed))
    }
  }
}
