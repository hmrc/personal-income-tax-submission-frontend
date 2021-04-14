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

import forms.ChangeAccountAmountForm._
import play.api.data.{Form, FormError}
import utils.UnitTest


class ChangeAccountAmountFormSpec extends UnitTest {

  def form(isAgent: Boolean, taxType: String): Form[BigDecimal] = changeAccountAmountForm(isAgent, taxType)

  lazy val testCurrencyValid = 1000
  lazy val testCurrencyEmpty = ""
  lazy val testCurrencyInvalidInt = "!"
  lazy val testCurrencyInvalidFormat = 12345.123
  lazy val testCurrencyTooBig = "100000000000.00"

  val TAXED = "taxed"
  val UNTAXED = "untaxed"
  val anAgent = true
  val anIndividual = false

  "ChangeAccountAmountFormSpec" should {

    "for an individual with an untaxed account" should {

      "correctly validate a currency" when {

        "a valid currency is entered" in {
          val testInput = Map(amount -> testCurrencyValid.toString)
          val expected = testCurrencyValid
          val actual = form(anIndividual, UNTAXED).bind(testInput).value
          actual shouldBe Some(expected)
        }
      }

      "invalidate an empty currency" in {
        val testInput = Map(amount -> testCurrencyEmpty)
        val emptyTest = form(anIndividual, UNTAXED).bind(testInput)
        emptyTest.errors should contain(FormError(amount, "changeAccountAmount.required.individual", List(UNTAXED)))
      }

      "invalidate currency that includes invalid characters" in {
        val testInput = Map(amount -> testCurrencyInvalidInt)
        val invalidCharTest = form(anIndividual, UNTAXED).bind(testInput)
        invalidCharTest.errors should contain(FormError(amount, "common.error.invalid_number", List(UNTAXED)))
      }

      "invalidate a currency that has incorrect formatting" in {
        val testInput = Map(amount -> testCurrencyInvalidFormat.toString)
        val invalidFormatTest = form(anIndividual, UNTAXED).bind(testInput)
        invalidFormatTest.errors should contain(FormError(amount, "changeAccountAmount.format", List(UNTAXED)))
      }

      "invalidate a currency that is too big" in {
        val testInput = Map(amount -> testCurrencyTooBig)
        val bigCurrencyTest = form(anIndividual, UNTAXED).bind(testInput)
        bigCurrencyTest.errors should contain(FormError(amount, "changeAccountAmount.amountMaxLimit", List(UNTAXED)))
      }

      "remove a leading space from a currency" in {
        val testInput = Map(amount -> (" " + testCurrencyValid))
        val expected = testCurrencyValid
        val leadingSpaceTest = form(anIndividual, UNTAXED).bind(testInput).value
        leadingSpaceTest shouldBe Some(expected)
      }
    }

    "for an individual with an taxed account" should {

      "correctly validate a currency" when {

        "a valid currency is entered" in {
          val testInput = Map(amount -> testCurrencyValid.toString)
          val expected = testCurrencyValid
          val actual = form(anIndividual, TAXED).bind(testInput).value
          actual shouldBe Some(expected)
        }
      }

      "invalidate an empty currency" in {
        val testInput = Map(amount -> testCurrencyEmpty)
        val emptyTest = form(anIndividual, TAXED).bind(testInput)
        emptyTest.errors should contain(FormError(amount, "changeAccountAmount.required.individual", List(TAXED)))
      }

      "invalidate currency that includes invalid characters" in {
        val testInput = Map(amount -> testCurrencyInvalidInt)
        val invalidCharTest = form(anIndividual, TAXED).bind(testInput)
        invalidCharTest.errors should contain(FormError(amount, "common.error.invalid_number", List(TAXED)))
      }

      "invalidate a currency that has incorrect formatting" in {
        val testInput = Map(amount -> testCurrencyInvalidFormat.toString)
        val invalidFormatTest = form(anIndividual, TAXED).bind(testInput)
        invalidFormatTest.errors should contain(FormError(amount, "changeAccountAmount.format", List(TAXED)))
      }

      "invalidate a currency that is too big" in {
        val testInput = Map(amount -> testCurrencyTooBig)
        val bigCurrencyTest = form(anIndividual, TAXED).bind(testInput)
        bigCurrencyTest.errors should contain(FormError(amount, "changeAccountAmount.amountMaxLimit", List(TAXED)))
      }

      "remove a leading space from a currency" in {
        val testInput = Map(amount -> (" " + testCurrencyValid))
        val expected = testCurrencyValid
        val leadingSpaceTest = form(anIndividual, TAXED).bind(testInput).value
        leadingSpaceTest shouldBe Some(expected)
      }
    }

    "for an agent with an untaxed account" should {

      "correctly validate a currency" when {

        "a valid currency is entered" in {
          val testInput = Map(amount -> testCurrencyValid.toString)
          val expected = testCurrencyValid
          val actual = form(anAgent, UNTAXED).bind(testInput).value
          actual shouldBe Some(expected)
        }
      }

      "invalidate an empty currency" in {
        val testInput = Map(amount -> testCurrencyEmpty)
        val emptyTest = form(anAgent, UNTAXED).bind(testInput)
        emptyTest.errors should contain(FormError(amount, "changeAccountAmount.required.agent", List(UNTAXED)))
      }

      "invalidate currency that includes invalid characters" in {
        val testInput = Map(amount -> testCurrencyInvalidInt)
        val invalidCharTest = form(anAgent, UNTAXED).bind(testInput)
        invalidCharTest.errors should contain(FormError(amount, "common.error.invalid_number", List(UNTAXED)))
      }

      "invalidate a currency that has incorrect formatting" in {
        val testInput = Map(amount -> testCurrencyInvalidFormat.toString)
        val invalidFormatTest = form(anAgent, UNTAXED).bind(testInput)
        invalidFormatTest.errors should contain(FormError(amount, "changeAccountAmount.format", List(UNTAXED)))
      }

      "invalidate a currency that is too big" in {
        val testInput = Map(amount -> testCurrencyTooBig)
        val bigCurrencyTest = form(anAgent, UNTAXED).bind(testInput)
        bigCurrencyTest.errors should contain(FormError(amount, "changeAccountAmount.amountMaxLimit", List(UNTAXED)))
      }

      "remove a leading space from a currency" in {
        val testInput = Map(amount -> (" " + testCurrencyValid))
        val expected = testCurrencyValid
        val leadingSpaceTest = form(anAgent, UNTAXED).bind(testInput).value
        leadingSpaceTest shouldBe Some(expected)
      }
    }

    "for an agent with an taxed account" should {

      "correctly validate a currency" when {

        "a valid currency is entered" in {
          val testInput = Map(amount -> testCurrencyValid.toString)
          val expected = testCurrencyValid
          val actual = form(anAgent, TAXED).bind(testInput).value
          actual shouldBe Some(expected)
        }
      }

      "invalidate an empty currency" in {
        val testInput = Map(amount -> testCurrencyEmpty)
        val emptyTest = form(anAgent, TAXED).bind(testInput)
        emptyTest.errors should contain(FormError(amount, "changeAccountAmount.required.agent", List(TAXED)))
      }

      "invalidate currency that includes invalid characters" in {
        val testInput = Map(amount -> testCurrencyInvalidInt)
        val invalidCharTest = form(anAgent, TAXED).bind(testInput)
        invalidCharTest.errors should contain(FormError(amount, "common.error.invalid_number", List(TAXED)))
      }

      "invalidate a currency that has incorrect formatting" in {
        val testInput = Map(amount -> testCurrencyInvalidFormat.toString)
        val invalidFormatTest = form(anAgent, TAXED).bind(testInput)
        invalidFormatTest.errors should contain(FormError(amount, "changeAccountAmount.format", List(TAXED)))
      }

      "invalidate a currency that is too big" in {
        val testInput = Map(amount -> testCurrencyTooBig)
        val bigCurrencyTest = form(anAgent, TAXED).bind(testInput)
        bigCurrencyTest.errors should contain(FormError(amount, "changeAccountAmount.amountMaxLimit", List(TAXED)))
      }

      "remove a leading space from a currency" in {
        val testInput = Map(amount -> (" " + testCurrencyValid))
        val expected = testCurrencyValid
        val leadingSpaceTest = form(anAgent, TAXED).bind(testInput).value
        leadingSpaceTest shouldBe Some(expected)
      }
    }
  }
}

