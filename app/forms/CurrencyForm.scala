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

import forms.validation.StringConstraints._
import forms.validation.utils.ConstraintUtil._
import forms.validation.utils.MappingUtil._
import models.CurrencyModel
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.Constraint

object CurrencyForm {

  val currencyAmount: String = "amount"


  val amountNotEmpty: Constraint[String] = nonEmpty("error.currency.empty")
  val amountValidInt: Constraint[String] = validateInt("error.currency.invalid_number")


  def currencyAmountForm(): Form[CurrencyModel] = Form(
    mapping(
      currencyAmount -> trimmedText.verifying(
        amountNotEmpty andThen amountValidInt
      )
    )(CurrencyModel.apply)(CurrencyModel.unapply)
  )
}

