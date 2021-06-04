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

import forms.interest.TaxedInterestAmountForm._
import models.interest.TaxedInterestModel
import play.api.data.{Form, FormError}
import utils.UnitTest

class TaxedInterestAmountFormSpec extends UnitTest{

  def agentOrIndividual(implicit isAgent: Boolean): String = if (isAgent) "agent" else "individual"

  def form(implicit isAgent: Boolean): Form[TaxedInterestModel] = {
    TaxedInterestAmountForm.taxedInterestAmountForm(
      emptyAmountKey = "interest.taxed-uk-interest-amount.error.empty." + agentOrIndividual,
      invalidNumericKey = "interest.taxed-uk-interest-amount.error.invalid-numeric",
      maxAmountInvalidKey = "interest.taxed-uk-interest-amount.error.max-amount"
    )
  }

  lazy val nameValid = "someName"
  lazy val nameInvalid = ""
  lazy val amountValid = 45.56
  lazy val amountInvalidEmpty = ""
  lazy val amountInvalidInt = "!"
  lazy val amountInvalidFormat = "12345.123"
  lazy val amountTooBig = "100000000000"

  "TaxedInterestFormSpec" should {

    "Correctly validate name" when {
      "valid name is supplied" in {
        val testInput = Map(taxedAccountName -> nameValid, taxedAmount -> amountValid.toString)
        val expected = TaxedInterestModel(nameValid, amountValid)
        val actual = form(isAgent = false).bind(testInput).value

        actual shouldBe Some(expected)
      }

      "invalid name is supplied" in {
        val testInput = Map(taxedAccountName -> nameInvalid, taxedAmount -> amountValid.toString)
        val result = form(isAgent = false).bind(testInput).errors

        result should contain(FormError(taxedAccountName, "interest.common.error.name.empty"))
      }
    }

    "Correctly validate currency amount" when {
      "valid currency is supplied" in {
        val testInput = Map(taxedAccountName -> nameValid, taxedAmount -> amountValid.toString)
        val expected = TaxedInterestModel(nameValid, amountValid)
        val actual = form(isAgent = false).bind(testInput).value

        actual shouldBe Some(expected)
      }

      "currency is empty as an individual" in {
        val testInput = Map(taxedAccountName -> nameValid, taxedAmount -> amountInvalidEmpty)
        val result = form(isAgent = false).bind(testInput).errors

        result should contain(FormError(taxedAmount, "interest.taxed-uk-interest-amount.error.empty.individual"))
      }

      "currency is empty as an agent" in {
        val testInput = Map(taxedAccountName -> nameValid, taxedAmount -> amountInvalidEmpty)
        val result = form(isAgent = true).bind(testInput).errors

        result should contain(FormError(taxedAmount, "interest.taxed-uk-interest-amount.error.empty.agent"))
      }

      "currency is invalid number" in {
        val testInput = Map(taxedAccountName -> nameValid, taxedAmount -> amountInvalidInt)
        val result = form(isAgent = false).bind(testInput).errors

        result should contain(FormError(taxedAmount, "interest.taxed-uk-interest-amount.error.invalid-numeric"))
      }

      "currency is invalid format" in {
        val testInput = Map(taxedAccountName -> nameValid, taxedAmount -> amountInvalidFormat)
        val result = form(isAgent = false).bind(testInput).errors

        result should contain(FormError(taxedAmount, "interest.taxed-uk-interest-amount.error.invalid-numeric"))
      }

      "currency is too big" in {
        val testInput = Map(taxedAccountName -> nameValid, taxedAmount -> amountTooBig)
        val result = form(isAgent = false).bind(testInput).errors

        result should contain(FormError(taxedAmount, "interest.taxed-uk-interest-amount.error.max-amount"))
      }
    }
  }

}
