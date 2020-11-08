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

package forms

import models.formatHelpers.PriorOrNewAmountModel
import play.api.data.{Form, FormError}
import utils.ViewTest

class PriorOrNewAmountFormSpec extends ViewTest {

  import PriorOrNewAmountForm._

  val amountInput = 10
  val newAmountInput = 20

  lazy val form: Form[PriorOrNewAmountModel] = priorOrNewAmountForm(amountInput, "dividends.uk-dividends-amount")

 "PriorOrNewAmountForm" should {

   "return a priorOrNewAmountForm" when {

     "the whichAmount field is prior" which {
       lazy val result = form.bind(Map(
         amountTypeField -> priorAmount
       )).get

       "the whichAmount field is prior" in {
         result.whichAmount shouldBe priorAmount
       }

       "the amount field should be 10" in {
         result.amount shouldBe Some(amountInput)
       }

     }

     "the whichAmount field is other, and the amount field is 20" which {
       lazy val result = form.bind(Map(
         amountTypeField -> otherAmount,
         otherAmountInputField -> newAmountInput.toString
       )).get

       "the whichAmount field is other" in {
         result.whichAmount shouldBe otherAmount
       }

       "the amount field should be 20" in {
         result.amount shouldBe Some(newAmountInput)
       }

     }

   }

   "return form errors" when {

     "the amount type field is missing" in {
       lazy val result = form.bind(Map[String, String]()).errors

       val expectedError = FormError("prior-amount", Seq("Select £10 or enter a different amount"))

       result.distinct shouldBe Seq(expectedError)
     }

     "the amount type field is invalid" in {
       lazy val result = form.bind(Map(
         amountTypeField -> "notathing"
       )).errors

       val expectedError = FormError("prior-amount", Seq("Select £10 or enter a different amount"))

       result shouldBe Seq(expectedError)
     }

     "the amount type field is other but the amount field is not a number" in {
       lazy val result = form.bind(Map(
         amountTypeField -> "other",
         otherAmountInputField -> "notANumber"
       )).errors

       val expectedError = FormError("prior-amount", Seq("Enter an amount using numbers 0 to 9"))

       result shouldBe Seq(expectedError)
     }

     "the amount type field is other but the amount field is empty" in {
       lazy val result = form.bind(Map(
         amountTypeField -> "other"
       )).errors

       val expectedError = FormError("prior-amount", Seq("Select £10 or enter a different amount"))

       result shouldBe Seq(expectedError)
     }

   }

 }

  "stringFormatter.unbind" should {

    "return form data" in {
      lazy val formatter = stringFormatter(amountInput, "")

      formatter.unbind(amountTypeField, "prior") shouldBe Map(amountTypeField -> "prior")
    }

  }

  "otherAmountFormatter.unbind" should {

    "return form data" in {
      lazy val formatter = otherAmountFormatter(amountInput, "")

      formatter.unbind(otherAmountInputField, Some(amountInput)) shouldBe Map(otherAmountInputField -> amountInput.toString)
    }

    "return an empty map" when {

      "a None is provided for the unbind" in {
        lazy val formatter = otherAmountFormatter(amountInput, "")

        formatter.unbind(otherAmountInputField, None) shouldBe Map[String, String]()
      }

    }

  }

}
