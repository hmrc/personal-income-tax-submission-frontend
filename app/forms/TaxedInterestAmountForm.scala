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

import forms.validation.StringConstraints.{nonEmpty, validateCurrency}
import forms.validation.utils.MappingUtil.trimmedText
import forms.validation.utils.ConstraintUtil._
import models.TaxedInterestModel
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.Constraint

object TaxedInterestAmountForm {

  val friendlyName = "friendlyName"
  val incomeTaxAmount = "incomeTaxAmount"

  val nameNotEmpty: Constraint[String] = nonEmpty("interest.taxed-uk-interest-name.error.empty")
  val amountNotEmpty: Constraint[String] = nonEmpty("interest.taxed-uk-interest-amount.error.empty")
  val amountValidCur: Constraint[String] = validateCurrency("interest.error.invalid_number")

  def taxedInterestAmountForm(): Form[TaxedInterestModel] = Form(
    mapping(
      friendlyName -> trimmedText.verifying(nameNotEmpty),
      incomeTaxAmount -> trimmedText.verifying(
        amountNotEmpty andThen amountValidCur
      )
    )(TaxedInterestModel.apply)(TaxedInterestModel.unapply)
  )
}
