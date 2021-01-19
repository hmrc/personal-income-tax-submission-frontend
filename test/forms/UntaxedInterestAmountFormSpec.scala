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

class UntaxedInterestAmountFormSpec extends UnitTest {

  def form: Form[UntaxedInterestModel] = {
    UntaxedInterestAmountForm.untaxedInterestAmountForm()
  }

  lazy val nameValid = "someName"
  lazy val nameInvalid = ""
  lazy val amountValid = "99.99"
  lazy val amountInvalid = ""
  lazy val amountInvalidEntry = "!"

  "UntaxedInterestAmountForm" should {

    "correctly validate name" when {
      "a valid name has been added" in {

        val testInput = Map(untaxedAccountName -> nameValid, untaxedAmount -> amountValid)
        val expected = UntaxedInterestModel(nameValid, amountValid)
        val actual = form.bind(testInput).value

        actual shouldBe Some(expected)
      }

      "an invalid name has been added" in {

        val testInput = Map(untaxedAccountName -> nameInvalid, untaxedAmount -> amountValid)
        val result = form.bind(testInput).errors

        result should contain(FormError(untaxedAccountName, "interest.untaxed-uk-interest-name.error.empty"))
      }
    }

    "correctly validate the interest amount" when {
      "a valid interest amount has been added" in {
        val testInput = Map(untaxedAccountName -> nameValid, untaxedAmount -> amountValid)
        val expected = UntaxedInterestModel(nameValid, amountValid)
        val actual = form.bind(testInput).value

        actual shouldBe Some(expected)
      }

      "no entry has been added to the amount input box" in {
        val testInput = Map(untaxedAccountName -> nameValid, untaxedAmount -> amountInvalid)
        val result = form.bind(testInput).errors

        result should contain(FormError(untaxedAmount, "interest.untaxed-uk-interest-amount.error.empty"))
      }

      "an invalid amount entry has been inputted" in {
        val testInput = Map(untaxedAccountName -> nameValid, untaxedAmount -> amountInvalidEntry)
        val result = form.bind(testInput).errors

        result should contain(FormError(untaxedAmount, "common.error.invalid_number"))
      }
    }
  }

}
