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

import forms.TaxedInterestAmountForm._
import models.TaxedInterestModel
import play.api.data.{Form, FormError}
import utils.UnitTest

class TaxedInterestAmountFormSpec extends UnitTest{

  def form: Form[TaxedInterestModel] = {
    TaxedInterestAmountForm.taxedInterestAmountForm()
  }

  lazy val nameValid = "someName"
  lazy val nameInvalid = ""
  lazy val amountValid = "45.56"
  lazy val amountInvalidEmpty = ""
  lazy val amountInvalidInt = "!"
  lazy val amountInvalidFormat = "12345.123"
  lazy val amountTooBig = "100000000000"

  "TaxedInterestFormSpec" should {

    "Correctly validate name" when {
      "valid name is supplied" in {
        val testInput = Map(taxedAccountName -> nameValid, taxedAmount -> amountValid)
        val expected = TaxedInterestModel(nameValid, amountValid)
        val actual = form.bind(testInput).value

        actual shouldBe Some(expected)
      }
      "Invalid name is supplied" in {
        val testInput = Map(taxedAccountName -> nameInvalid, taxedAmount -> amountValid)
        val result = form.bind(testInput).errors

        result should contain(FormError(taxedAccountName, "interest.taxed-uk-interest-name.error.empty"))
      }
    }
    "Correctly validate currency amount" when {
      "valid currency is supplied" in {
        val testInput = Map(taxedAccountName -> nameValid, taxedAmount -> amountValid)
        val expected = TaxedInterestModel(nameValid, amountValid)
        val actual = form.bind(testInput).value

        actual shouldBe Some(expected)
      }
      "currency is empty" in {
        val testInput = Map(taxedAccountName -> nameValid, taxedAmount -> amountInvalidEmpty)
        val result = form.bind(testInput).errors

        result should contain(FormError(taxedAmount, "interest.taxed-uk-interest-amount.error.empty"))
      }
      "currency is invalid number" in {
        val testInput = Map(taxedAccountName -> nameValid, taxedAmount -> amountInvalidInt)
        val result = form.bind(testInput).errors

        result should contain(FormError(taxedAmount, "common.error.invalid_number"))
      }

      "currency is invalid format" in {
        val testInput = Map(taxedAccountName -> nameValid, taxedAmount -> amountInvalidFormat)
        val result = form.bind(testInput).errors

        result should contain(FormError(taxedAmount, "common.error.invalid_currency"))
      }

      "currency is too big" in {
        val testInput = Map(taxedAccountName -> nameValid, taxedAmount -> amountTooBig)
        val result = form.bind(testInput).errors

        result should contain(FormError(taxedAmount, "common.error.amountMaxLimit"))
      }
    }
  }

}
