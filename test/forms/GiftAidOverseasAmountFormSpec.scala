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

import play.api.data.{Form, FormError}
import utils.UnitTest
import forms.GiftAidOverseasAmountForm._

class GiftAidOverseasAmountFormSpec extends UnitTest {

  def form(isAgent: Boolean): Form[BigDecimal] = {
    GiftAidOverseasAmountForm.giftAidOverseasAmountForm(isAgent)
  }

  lazy val testCurrencyValid = 1000
  lazy val testCurrencyEmpty = ""
  lazy val testCurrencyInvalidInt = "!"
  lazy val testCurrencyInvalidFormat = 12345.123
  lazy val testCurrencyTooBig = "100000000000.00"

  "GiftAidOverseasAmountFormSpec" should {

    "as an Agent user" should {

      "correctly validate a currency" when {
        "a valid currency is entered" in {
          val testInput = Map(giftAidOverseasAmount -> testCurrencyValid.toString)
          val expected = testCurrencyValid
          val actual = form(isAgent = true).bind(testInput).value

          actual shouldBe Some(expected)
        }
      }

      "invalidate an empty currency" in {
        val testInput = Map(giftAidOverseasAmount -> testCurrencyEmpty)

        val emptyTest = form(isAgent = true).bind(testInput)
        emptyTest.errors should contain(FormError(giftAidOverseasAmount, "charity.amount-overseas-gift-aid.error.empty.agent"))
      }

      "invalidate a currency that includes invalid characters" in {

        val testInput = Map(giftAidOverseasAmount -> testCurrencyInvalidInt)

        val invalidCharTest = form(isAgent = true).bind(testInput)
        invalidCharTest.errors should contain(FormError(giftAidOverseasAmount, "common.error.invalid_number"))
      }

      "invalidate a currency that has incorrect formatting" in {
        val testInput = Map(giftAidOverseasAmount -> testCurrencyInvalidFormat.toString)

        val invalidFormatTest = form(isAgent = true).bind(testInput)
        invalidFormatTest.errors should contain(FormError(giftAidOverseasAmount, "charity.amount-overseas-gift-aid.error.incorrect-format.agent"))
      }

      "invalidate a currency that is too big" in {
        val testInput = Map(giftAidOverseasAmount -> testCurrencyTooBig)

        val bigCurrencyTest = form(isAgent = true).bind(testInput)
        bigCurrencyTest.errors should contain(FormError(giftAidOverseasAmount, "charity.amount-overseas-gift-aid.error.too-high.agent"))
      }

      "remove a leading space from a currency" in {
        val testInput = Map(giftAidOverseasAmount -> (" " + testCurrencyValid))
        val expected = testCurrencyValid
        val leadingSpaceTest = form(isAgent = true).bind(testInput).value

        leadingSpaceTest shouldBe Some(expected)
      }
    }

    "as an Individual user" should {

      "correctly validate a currency" when {
        "a valid currency is entered" in {
          val testInput = Map(giftAidOverseasAmount -> testCurrencyValid.toString)
          val expected = testCurrencyValid
          val actual = form(isAgent = false).bind(testInput).value

          actual shouldBe Some(expected)
        }
      }

      "invalidate an empty currency" in {
        val testInput = Map(giftAidOverseasAmount -> testCurrencyEmpty)

        val emptyTest = form(isAgent = false).bind(testInput)
        emptyTest.errors should contain(FormError(giftAidOverseasAmount, "charity.amount-overseas-gift-aid.error.empty.individual"))
      }

      "invalidate a currency that includes invalid characters" in {

        val testInput = Map(giftAidOverseasAmount -> testCurrencyInvalidInt)

        val invalidCharTest = form(isAgent = false).bind(testInput)
        invalidCharTest.errors should contain(FormError(giftAidOverseasAmount, "common.error.invalid_number"))
      }

      "invalidate a currency that has incorrect formatting" in {
        val testInput = Map(giftAidOverseasAmount -> testCurrencyInvalidFormat.toString)

        val invalidFormatTest = form(isAgent = false).bind(testInput)
        invalidFormatTest.errors should contain(FormError(giftAidOverseasAmount, "charity.amount-overseas-gift-aid.error.incorrect-format.individual"))
      }

      "invalidate a currency that is too big" in {
        val testInput = Map(giftAidOverseasAmount -> testCurrencyTooBig)

        val bigCurrencyTest = form(isAgent = false).bind(testInput)
        bigCurrencyTest.errors should contain(FormError(giftAidOverseasAmount, "charity.amount-overseas-gift-aid.error.too-high.individual"))
      }

      "remove a leading space from a currency" in {
        val testInput = Map(giftAidOverseasAmount -> (" " + testCurrencyValid))
        val expected = testCurrencyValid
        val leadingSpaceTest = form(isAgent = false).bind(testInput).value

        leadingSpaceTest shouldBe Some(expected)
      }
    }
  }
}
