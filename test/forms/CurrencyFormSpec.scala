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

package forms

import models.CurrencyModel
import forms.CurrencyForm._
import play.api.data.{Form, FormError}
import utils.UnitTest


class CurrencyFormSpec extends UnitTest {

  def form: Form[CurrencyModel] = {
    CurrencyForm.currencyAmountForm()
  }

  lazy val testCurrencyValid = "1000"
  lazy val testCurrencyEmpty = ""
  lazy val testCurrencyInvalidInt = "!"

  "CurrencyFormSpec" should {

    "correctly validate a currency" when {
      "a valid currency is entered" in {
        val testInput = Map(currencyAmount -> testCurrencyValid)
        val expected = CurrencyModel(testCurrencyValid)
        val actual = form.bind(testInput).value

        actual shouldBe Some(expected)
      }
    }

    "invalidate an empty currency" in {
      val testInput = Map(currencyAmount -> testCurrencyEmpty)

      val emptyTest = form.bind(testInput)
      emptyTest.errors should contain(FormError(currencyAmount, "error.currency.empty"))
    }

    "invalidate a currency that includes invalid characters" in {

      val testInput = Map(currencyAmount -> testCurrencyInvalidInt)

      val invalidCharTest = form.bind(testInput)
      invalidCharTest.errors should contain(FormError(currencyAmount, "error.currency.invalid_number"))
    }

    "remove a leading space from a currency" in {
      val testInput = Map(currencyAmount -> (" " + testCurrencyValid))
      val expected = CurrencyModel(testCurrencyValid)
      val leadingSpaceTest = form.bind(testInput).value

      leadingSpaceTest shouldBe Some(expected)
    }
  }
}