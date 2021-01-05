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

import play.api.data.Form
import play.api.data.Forms._
import play.api.data.validation.Constraint
import forms.validation.StringConstraints._
import forms.validation.utils.ConstraintUtil._
import forms.validation.utils.MappingUtil._
import models.UntaxedInterestModel

object UntaxedInterestAmountForm {

  val accountName = "accountName"
  val untaxedAmount = "untaxedAmount"

  val nameNotEmpty: Constraint[String] = nonEmpty("interest.untaxed-uk-interest-name.error.empty")
  val amountNotEmpty: Constraint[String] = nonEmpty("interest.untaxed-uk-interest-amount.error.empty")
  val amountValCur: Constraint[String] = validateCurrency("interest.error.invalid-number")

  def untaxedInterestAmountForm(): Form[UntaxedInterestModel] = Form(
    mapping(
      accountName -> trimmedText.verifying(nameNotEmpty),
      untaxedAmount -> trimmedText.verifying(
        amountNotEmpty andThen amountValCur
      )
    )(UntaxedInterestModel.apply)(UntaxedInterestModel.unapply)
  )
}
