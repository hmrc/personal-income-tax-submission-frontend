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

  def agentOrIndividual(implicit isAgent: Boolean): String = if (isAgent) "agent" else "individual"

  def form(implicit isAgent: Boolean): Form[UntaxedInterestModel] = {
    UntaxedInterestAmountForm.untaxedInterestAmountForm(
      emptyAmountKey = "interest.untaxed-uk-interest-amount.error.empty." + agentOrIndividual,
      invalidNumericKey = "interest.untaxed-uk-interest-amount.error.invalid-numeric",
      maxAmountInvalidKey = "interest.untaxed-uk-interest-amount.error.max-amount"
    )
  }

  lazy val nameValid = "someName"
  lazy val nameInvalid = ""
  lazy val amountValid = 99.99
  lazy val amountInvalidEmpty = ""
  lazy val amountInvalidEntry = "!"
  lazy val amountInvalidFormat = "12345.123"
  lazy val amountTooBig = "100000000000"

  "UntaxedInterestAmountForm" should {

    "Correctly validate name" when {
      "valid name is supplied" in {

        val testInput = Map(untaxedAccountName -> nameValid, untaxedAmount -> amountValid.toString)
        val expected = UntaxedInterestModel(nameValid, amountValid)
        val actual = form(isAgent = false).bind(testInput).value

        actual shouldBe Some(expected)
      }

      "invalid name is supplied" in {

        val testInput = Map(untaxedAccountName -> nameInvalid, untaxedAmount -> amountValid.toString)
        val result = form(isAgent = false).bind(testInput).errors

        result should contain(FormError(untaxedAccountName, "interest.common.error.name.empty"))
      }
    }

    "Correctly validate currency amount" when {
      "valid currency is supplied" in {
        val testInput = Map(untaxedAccountName -> nameValid, untaxedAmount -> amountValid.toString)
        val expected = UntaxedInterestModel(nameValid, amountValid)
        val actual = form(isAgent = false).bind(testInput).value

        actual shouldBe Some(expected)
      }

      "currency is empty as an individual" in {
        val testInput = Map(untaxedAccountName -> nameValid, untaxedAmount -> amountInvalidEmpty)
        val result = form(isAgent = false).bind(testInput).errors

        result should contain(FormError(untaxedAmount, "interest.untaxed-uk-interest-amount.error.empty.individual"))
      }

      "currency is empty as an agent" in {
        val testInput = Map(untaxedAccountName -> nameValid, untaxedAmount -> amountInvalidEmpty)
        val result = form(isAgent = true).bind(testInput).errors

        result should contain(FormError(untaxedAmount, "interest.untaxed-uk-interest-amount.error.empty.agent"))
      }

      "currency is invalid number" in {
        val testInput = Map(untaxedAccountName -> nameValid, untaxedAmount -> amountInvalidEntry)
        val result = form(isAgent = false).bind(testInput).errors

        result should contain(FormError(untaxedAmount, "interest.untaxed-uk-interest-amount.error.invalid-numeric"))
      }

      "currency is invalid format" in {
        val testInput = Map(untaxedAccountName -> nameValid, untaxedAmount -> amountInvalidFormat)
        val result = form(isAgent = false).bind(testInput).errors

        result should contain(FormError(untaxedAmount, "interest.untaxed-uk-interest-amount.error.invalid-numeric"))
      }

      "currency is too big" in {
        val testInput = Map(untaxedAccountName -> nameValid, untaxedAmount -> amountTooBig)
        val result = form(isAgent = false).bind(testInput).errors

        result should contain(FormError(untaxedAmount, "interest.untaxed-uk-interest-amount.error.max-amount"))
      }
    }
  }

}
