/*
 * Copyright 2020 HM Revenue & Customs
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

package forms.testUtils

import forms.validation.StringConstraints
import org.scalatest.{Matchers, WordSpecLike}
import play.api.data.validation.{Constraints, Invalid, Valid}

class StringConstraintsSpec extends Constraints with WordSpecLike with Matchers {

  val maxLength = 2
  val errMsgMaxLength = "Too Long"
  val errMsgNonEmpty = "it is empty"
  val errMsgInvalidChar = "there are invalid chars"
  val errMsgNoLeadingSpace = "there are leading spaces"
  val errMsgInvalidInt = "contains non numerical chars"

  "The StringConstraints.maxLength method" when {

    "supplied with a string which exceeds the max length" should {

      "return invalid with the correct message" in {
        StringConstraints.maxLength(maxLength, errMsgMaxLength)("abc") shouldBe Invalid(errMsgMaxLength)
      }

    }

    "supplied with a string which equals the max length" should {

      "return valid" in {
        StringConstraints.maxLength(maxLength, errMsgMaxLength)("ab") shouldBe Valid
      }

    }

    "supplied with a string which is less than the max length" should {

      "return valid" in {
        StringConstraints.maxLength(maxLength, errMsgMaxLength)("a") shouldBe Valid
      }

    }
  }

  "The StringConstraints.nonEmpty method" when {

    "supplied with empty value" should {

      "return invalid" in {
        StringConstraints.nonEmpty(errMsgNonEmpty)("") shouldBe Invalid(errMsgNonEmpty)
      }

    }

    "supplied with some value" should {

      "return valid" in {
        StringConstraints.nonEmpty(errMsgNonEmpty)("someValue") shouldBe Valid
      }

    }
  }

  "The StringConstraints.validateChar method" when {

    "supplied with a valid string" should {

      "return valid" in {
        val lowerCaseAlphabet = ('a' to 'z').mkString
        val upperCaseAlphabet = lowerCaseAlphabet.toUpperCase()
        val oneToNine = (1 to 9).mkString
        val otherChar = "&@£$€¥#.,:;-"
        val space = ""

        StringConstraints.validateChar(errMsgInvalidChar)(lowerCaseAlphabet + upperCaseAlphabet + space + oneToNine + otherChar + space) shouldBe Valid
      }
    }

    "supplied with a string which contains invalid characters" should {

      "return invalid" in {
        StringConstraints.validateChar(errMsgInvalidChar)("!()+{}?^~") shouldBe Invalid(errMsgInvalidChar)
      }

    }
  }

  "The StringConstraints.noLeadingSpace method" when {

    "supplied with a string which contains no leading space" should {

      "return valid" in {
        StringConstraints.noLeadingSpace(errMsgNoLeadingSpace)("TEST Business") shouldBe Valid
      }
    }

    "supplied with a string which contains a leading space" should {

      "return invalid" in {
        StringConstraints.noLeadingSpace(errMsgNoLeadingSpace)(" TEST Business") shouldBe Invalid(errMsgNoLeadingSpace)
      }

    }

    "supplied with a string which contains two leading spaces" should {

      "return invalid" in {
        StringConstraints.noLeadingSpace(errMsgNoLeadingSpace)("  TEST Business") shouldBe Invalid(errMsgNoLeadingSpace)
      }

    }
  }

  "The StringConstraints.validateInt method" when {

    "supplied with a string which contains non numerical chars" should {

      "return invalid" in {
        StringConstraints.validateInt(errMsgInvalidInt)("123456789?") shouldBe Invalid(errMsgInvalidInt)
      }
    }

    "supplied with a string which contains all numerical chars" should {

      "return valid" in {
        StringConstraints.noLeadingSpace(errMsgNoLeadingSpace)("123456789") shouldBe Valid
      }

    }

  }

  "The StringConstraints.validateCurrency method" when {

    "supplied with a string which contains too many decimal places" should {

      "return invalid" in {
        StringConstraints.validateCurrency(errMsgInvalidInt)("1234.2323") shouldBe Invalid(errMsgInvalidInt)
      }

    }

    "supplied with a string which contains all numerical chars" should {

      "return valid" in {
        StringConstraints.noLeadingSpace(errMsgNoLeadingSpace)("1234567.89") shouldBe Valid
      }

    }

  }

}
