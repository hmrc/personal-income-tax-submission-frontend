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

package models.charity

import common.UUID
import play.api.libs.json.Json
import utils.UnitTest

class CharityNameModelSpec extends UnitTest {

  private val charityId = UUID().randomUUID
  private val model = CharityNameModel(charityId, "some-name")
  private val jsonModel = Json.obj(
    "id" -> charityId,
    "name" -> "some-name"
  )

  "CharityNameModel.apply(...)" should {
    "return model with random UUID" in {
      val model_1 = CharityNameModel.apply("name-1")
      val model_2 = CharityNameModel.apply("name-2")

      model_1.name shouldBe "name-1"
      model_2.name shouldBe "name-2"
      model_1.id.equals(model_2.id) shouldBe false
    }
  }

  "CharityNameModel" should {
    "correctly parse to Json" in {
      Json.toJson(model) shouldBe jsonModel
    }

    "correctly parse to a model" in {
      jsonModel.as[CharityNameModel] shouldBe model
    }
  }
}
