/*
 * Copyright 2022 HM Revenue & Customs
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

import forms.charity.GiftAidOverseasNameForm._
import play.api.data.{Form, FormError}
import utils.UnitTest

class GiftAidOverseasNameFormSpec extends UnitTest {

  def form(previousNames: List[String] ,isAgent: Boolean): Form[String] = {
    GiftAidOverseasNameForm.giftAidOverseasNameForm(previousNames, isAgent)
  }

  lazy val testNameValid = "john"
  lazy val testNameEmpty = ""
  lazy val testNameInvalidChar = "%"
  lazy val testNameTooBig = "ukHzoBYHkKGGk2V5iuYgS137gN7EB7LRw3uDjvujYg00ZtHwo3sokyOOCEoAK9vuPiP374QKOelo"
  lazy val emptyPreviousNames = List("")
  lazy val previousNames = List("john")


  "GiftAidOneOffAmountFormSpec" should {

    "as an Agent user" should {

      "correctly validate a name" when {
        "a valid name is entered" in {
          val testInput = Map(giftAidOverseasName -> testNameValid)
          val expected = testNameValid
          val actual = form(emptyPreviousNames, isAgent = true).bind(testInput).value

          actual shouldBe Some(expected)
        }
      }

      "invalidate an empty name" in {
        val testInput = Map(giftAidOverseasName -> testNameEmpty)

        val emptyTest = form(emptyPreviousNames, isAgent = true).bind(testInput)
        emptyTest.errors should contain(FormError(giftAidOverseasName, "charity.gift-aid-overseas-name.error.empty.agent"))
      }

      "invalidate a name that includes invalid characters" in {

        val testInput = Map(giftAidOverseasName -> testNameInvalidChar)

        val invalidCharTest = form(emptyPreviousNames, isAgent = true).bind(testInput)
        invalidCharTest.errors should contain(FormError(giftAidOverseasName, "charity.common.name.error.invalid"))
      }

      "invalidate a name that is to long" in {
        val testInput = Map(giftAidOverseasName -> testNameTooBig)

        val invalidLengthTest = form(emptyPreviousNames, isAgent = true).bind(testInput)
        invalidLengthTest.errors should contain(FormError(giftAidOverseasName, "charity.common.name.error.limit"))
      }


      "invalidate a name that is a duplicate" in {
        val testInput = Map(giftAidOverseasName -> testNameValid)

        val bigCurrencyTest = form(previousNames, isAgent = true).bind(testInput)
        bigCurrencyTest.errors should contain(FormError(giftAidOverseasName, "charity.common.name.error.duplicate"))
      }

    }

    "as an individual user" should {

      "correctly validate a name" when {
        "a valid name is entered" in {
          val testInput = Map(giftAidOverseasName -> testNameValid)
          val expected = testNameValid
          val actual = form(emptyPreviousNames, isAgent = false).bind(testInput).value

          actual shouldBe Some(expected)
        }
      }

      "invalidate an empty name" in {
        val testInput = Map(giftAidOverseasName -> testNameEmpty)

        val emptyTest = form(emptyPreviousNames, isAgent = false).bind(testInput)
        emptyTest.errors should contain(FormError(giftAidOverseasName, "charity.gift-aid-overseas-name.error.empty.individual"))
      }

      "invalidate a name that includes invalid characters" in {

        val testInput = Map(giftAidOverseasName -> testNameInvalidChar)

        val invalidCharTest = form(emptyPreviousNames, isAgent = false).bind(testInput)
        invalidCharTest.errors should contain(FormError(giftAidOverseasName, "charity.common.name.error.invalid"))
      }

      "invalidate a name that is to long" in {
        val testInput = Map(giftAidOverseasName -> testNameTooBig)

        val invalidLengthTest = form(emptyPreviousNames, isAgent = false).bind(testInput)
        invalidLengthTest.errors should contain(FormError(giftAidOverseasName, "charity.common.name.error.limit"))
      }


      "invalidate a name that is a duplicate" in {
        val testInput = Map(giftAidOverseasName -> testNameValid)

        val bigCurrencyTest = form(previousNames, isAgent = false).bind(testInput)
        bigCurrencyTest.errors should contain(FormError(giftAidOverseasName, "charity.common.name.error.duplicate"))
      }

    }
  }
}
