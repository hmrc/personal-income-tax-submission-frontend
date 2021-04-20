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
import models.User
import play.api.data.{Form, FormError}
import play.api.mvc.AnyContent
import utils.UnitTest


class ChangeAccountAmountFormSpec extends UnitTest {

  def agentForm(taxType: String): Form[BigDecimal] = {
    val agentUser: User[AnyContent] = user.copy(arn = Some("XARN1234567890"))
    changeAccountAmountForm(taxType)(agentUser)
  }

  def individualForm(taxType: String): Form[BigDecimal] = {
    val individualUser: User[AnyContent] = user
    changeAccountAmountForm(taxType)(individualUser)
  }

  lazy val testCurrencyValid = 1000
  lazy val testCurrencyEmpty = ""
  lazy val testCurrencyInvalidInt = "!"
  lazy val testCurrencyInvalidFormat = 12345.123
  lazy val testCurrencyTooBig = "100000000000.00"

  val TAXED = "taxed"
  val UNTAXED = "untaxed"

  "ChangeAccountAmountFormSpec" should {

    "for an individual with an untaxed account" should {

      "correctly validate a currency" when {

        "a valid currency is entered" in {
          val testInput = Map(amount -> testCurrencyValid.toString)
          val expected = testCurrencyValid
          val actual = individualForm(UNTAXED).bind(testInput).value
          actual shouldBe Some(expected)
        }
      }

      "invalidate an empty currency" in {
        val testInput = Map(amount -> testCurrencyEmpty)
        val emptyTest = individualForm(UNTAXED).bind(testInput)
        emptyTest.errors should contain(FormError(amount, "changeAccountAmount.required.individual", List(UNTAXED)))
      }

      "invalidate currency that includes invalid characters" in {
        val testInput = Map(amount -> testCurrencyInvalidInt)
        val invalidCharTest = individualForm(UNTAXED).bind(testInput)
        invalidCharTest.errors should contain(FormError(amount, "common.error.invalid_number", List(UNTAXED)))
      }

      "invalidate a currency that has incorrect formatting" in {
        val testInput = Map(amount -> testCurrencyInvalidFormat.toString)
        val invalidFormatTest = individualForm(UNTAXED).bind(testInput)
        invalidFormatTest.errors should contain(FormError(amount, "changeAccountAmount.format", List(UNTAXED)))
      }

      "invalidate a currency that is too big" in {
        val testInput = Map(amount -> testCurrencyTooBig)
        val bigCurrencyTest = individualForm(UNTAXED).bind(testInput)
        bigCurrencyTest.errors should contain(FormError(amount, "changeAccountAmount.amountMaxLimit", List(UNTAXED)))
      }

      "remove a leading space from a currency" in {
        val testInput = Map(amount -> (" " + testCurrencyValid))
        val expected = testCurrencyValid
        val leadingSpaceTest = individualForm(UNTAXED).bind(testInput).value
        leadingSpaceTest shouldBe Some(expected)
      }
    }

    "for an individual with an taxed account" should {

      "correctly validate a currency" when {

        "a valid currency is entered" in {
          val testInput = Map(amount -> testCurrencyValid.toString)
          val expected = testCurrencyValid
          val actual = individualForm(TAXED).bind(testInput).value
          actual shouldBe Some(expected)
        }
      }

      "invalidate an empty currency" in {
        val testInput = Map(amount -> testCurrencyEmpty)
        val emptyTest = individualForm(TAXED).bind(testInput)
        emptyTest.errors should contain(FormError(amount, "changeAccountAmount.required.individual", List(TAXED)))
      }

      "invalidate currency that includes invalid characters" in {
        val testInput = Map(amount -> testCurrencyInvalidInt)
        val invalidCharTest = individualForm(TAXED).bind(testInput)
        invalidCharTest.errors should contain(FormError(amount, "common.error.invalid_number", List(TAXED)))
      }

      "invalidate a currency that has incorrect formatting" in {
        val testInput = Map(amount -> testCurrencyInvalidFormat.toString)
        val invalidFormatTest = individualForm(TAXED).bind(testInput)
        invalidFormatTest.errors should contain(FormError(amount, "changeAccountAmount.format", List(TAXED)))
      }

      "invalidate a currency that is too big" in {
        val testInput = Map(amount -> testCurrencyTooBig)
        val bigCurrencyTest = individualForm(TAXED).bind(testInput)
        bigCurrencyTest.errors should contain(FormError(amount, "changeAccountAmount.amountMaxLimit", List(TAXED)))
      }

      "remove a leading space from a currency" in {
        val testInput = Map(amount -> (" " + testCurrencyValid))
        val expected = testCurrencyValid
        val leadingSpaceTest = individualForm(TAXED).bind(testInput).value
        leadingSpaceTest shouldBe Some(expected)
      }
    }

    "for an agent with an untaxed account" should {

      "correctly validate a currency" when {

        "a valid currency is entered" in {
          val testInput = Map(amount -> testCurrencyValid.toString)
          val expected = testCurrencyValid
          val actual = agentForm(UNTAXED).bind(testInput).value
          actual shouldBe Some(expected)
        }
      }

      "invalidate an empty currency" in {
        val testInput = Map(amount -> testCurrencyEmpty)
        val emptyTest = agentForm(UNTAXED).bind(testInput)
        emptyTest.errors should contain(FormError(amount, "changeAccountAmount.required.agent", List(UNTAXED)))
      }

      "invalidate currency that includes invalid characters" in {
        val testInput = Map(amount -> testCurrencyInvalidInt)
        val invalidCharTest = agentForm(UNTAXED).bind(testInput)
        invalidCharTest.errors should contain(FormError(amount, "common.error.invalid_number", List(UNTAXED)))
      }

      "invalidate a currency that has incorrect formatting" in {
        val testInput = Map(amount -> testCurrencyInvalidFormat.toString)
        val invalidFormatTest = agentForm(UNTAXED).bind(testInput)
        invalidFormatTest.errors should contain(FormError(amount, "changeAccountAmount.format", List(UNTAXED)))
      }

      "invalidate a currency that is too big" in {
        val testInput = Map(amount -> testCurrencyTooBig)
        val bigCurrencyTest = agentForm(UNTAXED).bind(testInput)
        bigCurrencyTest.errors should contain(FormError(amount, "changeAccountAmount.amountMaxLimit", List(UNTAXED)))
      }

      "remove a leading space from a currency" in {
        val testInput = Map(amount -> (" " + testCurrencyValid))
        val expected = testCurrencyValid
        val leadingSpaceTest = agentForm(UNTAXED).bind(testInput).value
        leadingSpaceTest shouldBe Some(expected)
      }
    }

    "for an agent with an taxed account" should {

      "correctly validate a currency" when {

        "a valid currency is entered" in {
          val testInput = Map(amount -> testCurrencyValid.toString)
          val expected = testCurrencyValid
          val actual = agentForm(TAXED).bind(testInput).value
          actual shouldBe Some(expected)
        }
      }

      "invalidate an empty currency" in {
        val testInput = Map(amount -> testCurrencyEmpty)
        val emptyTest = agentForm(TAXED).bind(testInput)
        emptyTest.errors should contain(FormError(amount, "changeAccountAmount.required.agent", List(TAXED)))
      }

      "invalidate currency that includes invalid characters" in {
        val testInput = Map(amount -> testCurrencyInvalidInt)
        val invalidCharTest = agentForm(TAXED).bind(testInput)
        invalidCharTest.errors should contain(FormError(amount, "common.error.invalid_number", List(TAXED)))
      }

      "invalidate a currency that has incorrect formatting" in {
        val testInput = Map(amount -> testCurrencyInvalidFormat.toString)
        val invalidFormatTest = agentForm(TAXED).bind(testInput)
        invalidFormatTest.errors should contain(FormError(amount, "changeAccountAmount.format", List(TAXED)))
      }

      "invalidate a currency that is too big" in {
        val testInput = Map(amount -> testCurrencyTooBig)
        val bigCurrencyTest = agentForm(TAXED).bind(testInput)
        bigCurrencyTest.errors should contain(FormError(amount, "changeAccountAmount.amountMaxLimit", List(TAXED)))
      }

      "remove a leading space from a currency" in {
        val testInput = Map(amount -> (" " + testCurrencyValid))
        val expected = testCurrencyValid
        val leadingSpaceTest = agentForm(TAXED).bind(testInput).value
        leadingSpaceTest shouldBe Some(expected)
      }
    }
  }
}

