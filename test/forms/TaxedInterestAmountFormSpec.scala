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

import models.TaxedInterestModel
import forms.TaxedInterestAmountForm._
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

  "TaxedInterestFormSpec" should {

    "Correctly validate name" when {
      "valid name is supplied" in {
        val testInput = Map(friendlyName -> nameValid, incomeTaxAmount -> amountValid)
        val expected = TaxedInterestModel(nameValid, amountValid)
        val actual = form.bind(testInput).value

        actual shouldBe Some(expected)
      }
      "Invalid name is supplied" in {
        val testInput = Map(friendlyName -> nameInvalid, incomeTaxAmount -> amountValid)
        val result = form.bind(testInput).errors

        result should contain(FormError(friendlyName, "interest.taxed-uk-interest-name.error.empty"))
      }
    }
    "Correctly validate currency amount" when {
      "valid currency is supplied" in {
        val testInput = Map(friendlyName -> nameValid, incomeTaxAmount -> amountValid)
        val expected = TaxedInterestModel(nameValid, amountValid)
        val actual = form.bind(testInput).value

        actual shouldBe Some(expected)
      }
      "currency is empty" in {
        val testInput = Map(friendlyName -> nameValid, incomeTaxAmount -> amountInvalidEmpty)
        val result = form.bind(testInput).errors

        result should contain(FormError(incomeTaxAmount, "interest.taxed-uk-interest-amount.error.empty"))
      }
      "currency is invalid number" in {
        val testInput = Map(friendlyName -> nameValid, incomeTaxAmount -> amountInvalidInt)
        val result = form.bind(testInput).errors

        result should contain(FormError(incomeTaxAmount, "interest.error.invalid_number"))
      }
    }
  }

}
