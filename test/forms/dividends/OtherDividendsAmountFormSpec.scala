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

package forms.dividends

import forms.dividends.OtherDividendsAmountForm._
import models.User
import play.api.data.{Form, FormError}
import play.api.mvc.AnyContent
import utils.UnitTest

class OtherDividendsAmountFormSpec extends UnitTest {

  def agentForm: Form[BigDecimal] = {
    val agentUser: User[AnyContent] = user.copy(arn = Some("XARN1234567890"))
    OtherDividendsAmountForm.otherDividendsAmountForm(agentUser)
  }

  def individualForm: Form[BigDecimal] = {
    val individualUser: User[AnyContent] = user
    OtherDividendsAmountForm.otherDividendsAmountForm(individualUser)
  }

  lazy val testCurrencyValid = "1000"
  lazy val testCurrencyEmpty = ""
  lazy val testCurrencyInvalidInt = "!"
  lazy val testCurrencyInvalidFormat = "12345.123"
  lazy val testCurrencyTooBig = "100000000000"

  "OtherDividendsFormSpec" should {

    "correctly validate a currency" when {
      "a valid currency is entered" in {
        val testInput = Map(otherDividendsAmount -> testCurrencyValid)
        val expected = BigDecimal(testCurrencyValid)
        val actual = individualForm.bind(testInput).value

        actual shouldBe Some(expected)
      }
    }

    "invalidate an empty currency for an individual" in {
      val testInput = Map(otherDividendsAmount -> testCurrencyEmpty)

      val emptyTest = individualForm.bind(testInput)
      emptyTest.errors should contain(FormError(otherDividendsAmount, "dividends.other-dividends-amount.error.empty.individual"))
    }

    "invalidate an empty currency for an agent" in {
      val testInput = Map(otherDividendsAmount -> testCurrencyEmpty)

      val emptyTest = agentForm.bind(testInput)
      emptyTest.errors should contain(FormError(otherDividendsAmount, "dividends.other-dividends-amount.error.empty.agent"))
    }

    "invalidate a currency that includes invalid characters for an individual" in {

      val testInput = Map(otherDividendsAmount -> testCurrencyInvalidInt)

      val invalidCharTest = individualForm.bind(testInput)
      invalidCharTest.errors should contain(FormError(otherDividendsAmount, "dividends.common.error.invalidFormat.individual"))
    }

    "invalidate a currency that includes invalid characters for an agent" in {

      val testInput = Map(otherDividendsAmount -> testCurrencyInvalidInt)

      val invalidCharTest = agentForm.bind(testInput)
      invalidCharTest.errors should contain(FormError(otherDividendsAmount, "dividends.common.error.invalidFormat.agent"))
    }

    "invalidate a currency that has incorrect formatting for an individual" in {
      val testInput = Map(otherDividendsAmount -> testCurrencyInvalidFormat)

      val invalidFormatTest = individualForm.bind(testInput)
      invalidFormatTest.errors should contain(FormError(otherDividendsAmount, "dividends.common.error.invalidFormat.individual"))
    }

    "invalidate a currency that has incorrect formatting for an agent" in {
      val testInput = Map(otherDividendsAmount -> testCurrencyInvalidFormat)

      val invalidFormatTest = agentForm.bind(testInput)
      invalidFormatTest.errors should contain(FormError(otherDividendsAmount, "dividends.common.error.invalidFormat.agent"))
    }

    "invalidate a currency that is too big" in {
      val testInput = Map(otherDividendsAmount -> testCurrencyTooBig)

      val bigCurrencyTest = individualForm.bind(testInput)
      bigCurrencyTest.errors should contain(FormError(otherDividendsAmount, "dividends.other-dividends-amount.error.amountMaxLimit"))
    }

    "remove a leading space from a currency" in {
      val testInput = Map(otherDividendsAmount -> (" " + testCurrencyValid))
      val expected = BigDecimal(testCurrencyValid)
      val leadingSpaceTest = individualForm.bind(testInput).value

      leadingSpaceTest shouldBe Some(expected)
    }
  }
}

