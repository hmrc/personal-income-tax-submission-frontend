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

package forms.interest

import forms.interest.UntaxedInterestAmountForm._
import models.interest.UntaxedInterestModel
import play.api.data.{Form, FormError}
import utils.UnitTest

class UntaxedInterestAmountFormSpec extends UnitTest {

  def form(isAgent: Boolean,
           previousNames: Seq[String]
           ): Form[UntaxedInterestModel] = {
    UntaxedInterestAmountForm.untaxedInterestAmountForm(
      isAgent,
      previousNames
      )
  }

  lazy val nameValid = "someName"
  lazy val nameInvalid = ""
  lazy val emptyPreviousNames = Seq("")
  lazy val previousNames = Seq("someName")
  lazy val amountValid = 99.99
  lazy val amountInvalid = ""
  lazy val amountInvalidEntry = "!"
  lazy val amountInvalidFormat = "12345.123"
  lazy val amountTooBig = "100000000000"

  "UntaxedInterestAmountForm" should {

    "correctly validate name" when {
      "a valid name has been added" in {

        val testInput = Map(untaxedAccountName -> nameValid, untaxedAmount -> amountValid.toString)
        val expected = UntaxedInterestModel(nameValid, amountValid)
        val actual = form(isAgent = true, emptyPreviousNames).bind(testInput).value

        actual shouldBe Some(expected)
      }

      "an invalid name has been added" in {

        val testInput = Map(untaxedAccountName -> nameInvalid, untaxedAmount -> amountValid.toString)
        val result = form(isAgent = true, emptyPreviousNames).bind(testInput).errors

        result should contain(FormError(untaxedAccountName, "interest.common.error.name.empty"))
      }

      "Name is a duplicate of an existing account name" in {
        val testInput = Map(untaxedAccountName -> nameValid, untaxedAmount -> amountValid.toString)
        val result = form(isAgent = true, previousNames).bind(testInput).errors

        result should contain(FormError(untaxedAccountName, "interest.common.error.name.duplicate"))
      }
    }

    "correctly validate the interest amount" when {
      "a valid interest amount has been added" in {
        val testInput = Map(untaxedAccountName -> nameValid, untaxedAmount -> amountValid.toString)
        val expected = UntaxedInterestModel(nameValid, amountValid)
        val actual = form(isAgent = true, emptyPreviousNames).bind(testInput).value

        actual shouldBe Some(expected)
      }

      "no entry has been added to the amount input box as an individual" in {
        val testInput = Map(untaxedAccountName -> nameValid, untaxedAmount -> amountInvalid)
        val result = form(isAgent = false, emptyPreviousNames).bind(testInput).errors

        result should contain(FormError(untaxedAmount, "interest.untaxed-uk-interest-amount.error.empty.individual"))
      }

      "no entry has been added to the amount input box as an agent" in {
        val testInput = Map(untaxedAccountName -> nameValid, untaxedAmount -> amountInvalid)
        val result = form(isAgent = true, emptyPreviousNames).bind(testInput).errors

        result should contain(FormError(untaxedAmount, "interest.untaxed-uk-interest-amount.error.empty.agent"))
      }

      "an invalid amount entry has been inputted" in {
        val testInput = Map(untaxedAccountName -> nameValid, untaxedAmount -> amountInvalidEntry)
        val result = form(isAgent = true, emptyPreviousNames).bind(testInput).errors

        result should contain(FormError(untaxedAmount, "interest.untaxed-uk-interest-amount.error.invalid-numeric"))
      }

      "an invalid amount entry has incorrect formatting" in {
        val testInput = Map(untaxedAccountName -> nameValid, untaxedAmount -> amountInvalidFormat)
        val result = form(isAgent = true, emptyPreviousNames).bind(testInput).errors

        result should contain(FormError(untaxedAmount, "interest.untaxed-uk-interest-amount.error.invalid-numeric"))
      }

      "an amount entry is too big" in {
        val testInput = Map(untaxedAccountName -> nameValid, untaxedAmount -> amountTooBig)
        val result = form(isAgent = true, emptyPreviousNames).bind(testInput).errors

        result should contain(FormError(untaxedAmount, "interest.untaxed-uk-interest-amount.error.max-amount"))
      }
    }
  }

}
