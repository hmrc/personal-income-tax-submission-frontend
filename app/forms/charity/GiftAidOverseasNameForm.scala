/*
 * Copyright 2023 HM Revenue & Customs
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

import filters.InputFilters
import forms.validation.StringConstraints.{nonEmpty, validateChar, validateNotDuplicateGiftAidAccount, validateSize}
import forms.validation.mappings.MappingUtil.trimmedText
import forms.validation.utils.ConstraintUtil.ConstraintUtil
import play.api.data.Form
import play.api.data.validation.Constraint

object GiftAidOverseasNameForm extends InputFilters {

  val giftAidOverseasName: String = "name"
  val charLimit: Int = 500

  def notEmpty(isAgent: Boolean): Constraint[String] =
    nonEmpty(if(isAgent) "charity.gift-aid-overseas-name.error.empty.agent" else "charity.gift-aid-overseas-name.error.empty.individual")

  val NotCharLimit: Constraint[String] = validateSize(charLimit)("charity.common.name.error.limit")
  val NotInvalidChar: Constraint[String] = validateChar("charity.common.name.error.invalid")

  def notDuplicate(previousNames: List[String]): Constraint[String] = validateNotDuplicateGiftAidAccount(previousNames)("charity.common.name.error.duplicate")

  def giftAidOverseasNameForm(previousNames: List[String], isAgent: Boolean): Form[String] = Form(
      giftAidOverseasName -> trimmedText.transform[String](filter, identity).verifying(
        notEmpty(isAgent) andThen NotCharLimit andThen NotInvalidChar andThen notDuplicate(previousNames)
      )
  )
}
