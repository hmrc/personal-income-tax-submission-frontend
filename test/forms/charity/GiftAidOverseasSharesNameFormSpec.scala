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

package forms.charity

import forms.charity.GiftAidOverseasSharesNameForm._
import play.api.data.{Form, FormError}
import utils.UnitTest


class GiftAidOverseasSharesNameFormSpec extends UnitTest {

  def form(previousNames: List[String] ,isAgent: Boolean): Form[String] = {
    GiftAidOverseasSharesNameForm.giftAidOverseasSharesNameForm(previousNames, isAgent)
  }

  lazy val testNameValid = "john"
  lazy val testNameEmpty = ""
  lazy val testNameInvalidChar = "|"
  lazy val testNameTooBig = "ukHzoBYHkKGGk2V5iuYgS137gN7EB7LRw3uDjvujYg00ZtHwo3sokyOOCEoAK9vuPiP374QKOelo"
  lazy val emptyPreviousNames = List("")
  lazy val previousNames = List("john")


  "GiftAidOverseasSharesNameFormSpec" should {

    "as an Agent user" should {

      "correctly validate a name" when {
        "a valid name is entered" in {
          val testInput = Map(giftAidOverseasSharesName -> testNameValid)
          val expected = testNameValid
          val actual = form(emptyPreviousNames, isAgent = true).bind(testInput).value

          actual shouldBe Some(expected)
        }
      }

      "invalidate an empty name" in {
        val testInput = Map(giftAidOverseasSharesName -> testNameEmpty)

        val emptyTest = form(emptyPreviousNames, isAgent = true).bind(testInput)
        emptyTest.errors should contain(FormError(giftAidOverseasSharesName, "charity.overseas-shares-donated-name.error.empty-field.agent"))
      }

      "invalidate a name that includes invalid characters" in {

        val testInput = Map(giftAidOverseasSharesName -> testNameInvalidChar)

        val invalidCharTest = form(emptyPreviousNames, isAgent = true).bind(testInput)
        invalidCharTest.errors should contain(FormError(giftAidOverseasSharesName, "charity.gift-aid-overseas-name.error.invalid"))
      }

      "invalidate a name that is to long" in {
        val testInput = Map(giftAidOverseasSharesName -> testNameTooBig)

        val invalidLengthTest = form(emptyPreviousNames, isAgent = true).bind(testInput)
        invalidLengthTest.errors should contain(FormError(giftAidOverseasSharesName, "charity.gift-aid-overseas-name.error.limit"))
      }


      "invalidate a name that is a duplicate" in {
        val testInput = Map(giftAidOverseasSharesName -> testNameValid)

        val bigCurrencyTest = form(previousNames, isAgent = true).bind(testInput)
        bigCurrencyTest.errors should contain(FormError(giftAidOverseasSharesName, "charity.gift-aid-overseas-name.error.duplicate"))
      }

    }

    "as an individual user" should {

      "correctly validate a name" when {
        "a valid name is entered" in {
          val testInput = Map(giftAidOverseasSharesName -> testNameValid)
          val expected = testNameValid
          val actual = form(emptyPreviousNames, isAgent = false).bind(testInput).value

          actual shouldBe Some(expected)
        }
      }

      "invalidate an empty name" in {
        val testInput = Map(giftAidOverseasSharesName -> testNameEmpty)

        val emptyTest = form(emptyPreviousNames, isAgent = false).bind(testInput)
        emptyTest.errors should contain(FormError(giftAidOverseasSharesName, "charity.overseas-shares-donated-name.error.empty-field.individual"))
      }

      "invalidate a name that includes invalid characters" in {

        val testInput = Map(giftAidOverseasSharesName -> testNameInvalidChar)

        val invalidCharTest = form(emptyPreviousNames, isAgent = false).bind(testInput)
        invalidCharTest.errors should contain(FormError(giftAidOverseasSharesName, "charity.gift-aid-overseas-name.error.invalid"))
      }

      "invalidate a name that is to long" in {
        val testInput = Map(giftAidOverseasSharesName -> testNameTooBig)

        val invalidLengthTest = form(emptyPreviousNames, isAgent = false).bind(testInput)
        invalidLengthTest.errors should contain(FormError(giftAidOverseasSharesName, "charity.gift-aid-overseas-name.error.limit"))
      }


      "invalidate a name that is a duplicate" in {
        val testInput = Map(giftAidOverseasSharesName -> testNameValid)

        val bigCurrencyTest = form(previousNames, isAgent = false).bind(testInput)
        bigCurrencyTest.errors should contain(FormError(giftAidOverseasSharesName, "charity.gift-aid-overseas-name.error.duplicate"))
      }

    }
  }
}
