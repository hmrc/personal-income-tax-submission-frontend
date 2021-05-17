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

package forms.interest

import filters.InputFilters
import forms.validation.StringConstraints.nonEmpty
import forms.validation.mappings.MappingUtil.{currency, trimmedText}
import models.interest.UntaxedInterestModel
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.Constraint

object UntaxedInterestAmountForm extends InputFilters{

  val untaxedAccountName = "untaxedAccountName"
  val untaxedAmount = "untaxedAmount"

  val nameNotEmpty: Constraint[String] = nonEmpty("interest.common.error.name.empty")
  val amountNotEmpty: Constraint[String] = nonEmpty("interest.untaxed-uk-interest-amount.error.empty")

  def untaxedInterestAmountForm(): Form[UntaxedInterestModel] = Form(
    mapping(
      untaxedAccountName -> trimmedText.verifying(nameNotEmpty),
      untaxedAmount -> currency("interest.untaxed-uk-interest-amount.error.empty")
    )(UntaxedInterestModel.apply)(UntaxedInterestModel.unapply).transform[UntaxedInterestModel](
      details => details.copy(
        untaxedAccountName = filter(details.untaxedAccountName)
      ), x => x
    )
  )
}
