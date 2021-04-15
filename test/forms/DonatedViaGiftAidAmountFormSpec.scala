/*
 * Copyright 2021 HM Revenue & Customs
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

package forms

import forms.UntaxedInterestAmountForm._
import models.UntaxedInterestModel
import play.api.data.{Form, FormError}
import utils.UnitTest

class DonatedViaGiftAidAmountFormSpec extends UnitTest {

  def form(isAgent: Boolean): Form[BigDecimal] = {
    DonatedViaGiftAidAmountForm.donatedViaGiftAidForm(isAgent)
  }

  "DonatedViaGiftAidAmountForm" should {

    "return the amount entered" in {
      val testInput = Map[String, String]("amount" -> "12.00")
      val expectedOutcome: BigDecimal = 12

      val result = form(true).bind(testInput)
      result.value.get shouldBe expectedOutcome
    }

    "return an error" when {

      "there is no input" in {
        val testInput = Map[String, String]()
        val expectedResult = "charity.amount-via-gift-aid.error.agent.no-input"

        val result = form(true).bind(testInput)
        result.errors.head.message shouldBe expectedResult
      }

      "the amount input is in an incorrect format" in {
        val testInput = Map[String, String]("amount" -> "111.111")
        val expectedResult = "charity.amount-via-gift-aid.error.agent.incorrect-format"

        val result = form(true).bind(testInput)
        result.errors.head.message shouldBe expectedResult
      }

      "the amount is above the maximum allowed" in {
        val testInput = Map[String, String]("amount" -> "1110000000000000000000000.11")
        val expectedResult = "charity.amount-via-gift-aid.error.agent.too-high"

        val result = form(true).bind(testInput)
        result.errors.head.message shouldBe expectedResult
      }

    }

  }

}
