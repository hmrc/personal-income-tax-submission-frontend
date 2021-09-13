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
import forms.validation.StringConstraints._
import forms.validation.mappings.MappingUtil.{currency, trimmedText}
import models.interest.UntaxedInterestModel
import play.api.data.Form
import play.api.data.Forms.mapping
import play.api.data.validation.Constraint

object UntaxedInterestAmountForm extends InputFilters {

  val untaxedAccountName = "untaxedAccountName"
  val untaxedAmount = "untaxedAmount"
  val charLimit: Int = 32

  val nameNotEmpty: Constraint[String] = nonEmpty("interest.common.error.name.empty")

  val noInvalidChar: Constraint[String] = validateChar("interest.untaxed-uk-interest-details.error.invalidChars", charRegexInterest)

  val exceedCharLimit: Constraint[String] = validateSize(charLimit)("interest.accounts.error.tooLong")

  def emptyAmountKey(isAgent: Boolean): String = s"interest.untaxed-uk-interest-amount.error.empty.${if (isAgent) "agent" else "individual"}"

  val invalidNumericKey: String = "interest.untaxed-uk-interest-amount.error.invalid-numeric"

  val maxAmountInvalidKey: String = "interest.untaxed-uk-interest-amount.error.max-amount"

  def notDuplicate(disallowedDuplicateNames: Seq[String]): Constraint[String] = {
    validateNotDuplicateInterestAccount(disallowedDuplicateNames)("interest.common.error.name.duplicate")
  }

  def untaxedInterestAmountForm(isAgent: Boolean, disallowedDuplicateNames: Seq[String]): Form[UntaxedInterestModel] = Form(
    mapping(
      untaxedAccountName -> trimmedText.verifying(nameNotEmpty, noInvalidChar, exceedCharLimit, notDuplicate(disallowedDuplicateNames)),
      untaxedAmount -> currency(requiredKey = emptyAmountKey(isAgent: Boolean),
        invalidNumeric = invalidNumericKey,
        nonNumericKey = invalidNumericKey,
        maxAmountKey = maxAmountInvalidKey)
    )(UntaxedInterestModel.apply)(UntaxedInterestModel.unapply).transform[UntaxedInterestModel](
      details => details.copy(
        untaxedAccountName = filter(details.untaxedAccountName)
      ), x => x
    )
  )
}
