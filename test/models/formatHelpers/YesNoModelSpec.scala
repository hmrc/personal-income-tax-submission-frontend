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

package models.formatHelpers

import utils.UnitTest

class YesNoModelSpec extends UnitTest {

  ".asBoolean" should {

    "convert the value into a boolean" when {

      "the value is yes" in {

        YesNoModel("yes").asBoolean shouldBe true

      }

      "the value is no" in {

        YesNoModel("no").asBoolean shouldBe false

      }

    }

  }

}
