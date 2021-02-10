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

import forms.validation.StringConstraints._
import forms.validation.utils.ConstraintUtil._
import forms.validation.utils.MappingUtil._
import models.CurrencyAmountModel
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.Constraint

object OtherDividendsAmountForm {

  val otherDividendsAmount: String = "amount"

  val amountNotEmpty: Constraint[String] = nonEmpty("dividends.other-dividends-amount.error.empty")
  val amountValidNumericalCharacters: Constraint[String] = validateNumericalCharacters("common.error.invalid_number")
  val amountValidCurrency: Constraint[String] = validateCurrency("common.error.invalid_currency")
  val amountMaxLimit: Constraint[String] = maxAmount("common.error.amountMaxLimit")

  def otherDividendsAmountForm(): Form[CurrencyAmountModel] = Form(
    mapping(
      otherDividendsAmount -> trimmedText.verifying(
        amountNotEmpty andThen amountValidNumericalCharacters andThen amountValidCurrency andThen amountMaxLimit
      )
    )(CurrencyAmountModel.apply)(CurrencyAmountModel.unapply)
  )
}
