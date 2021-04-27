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

import forms.validation.StringConstraints.{nonEmpty, validateChar, validateNotDuplicate, validateSize}
import forms.validation.mappings.MappingUtil.trimmedText
import forms.validation.utils.ConstraintUtil.ConstraintUtil
import play.api.data.Form
import play.api.data.validation.Constraint

object GiftAidOverseasNameForm {

  val giftAidOverseasName: String = "name"
  val charLimit: Int = 75

  def notEmpty(isAgent: Boolean): Constraint[String] =
    nonEmpty(if(isAgent) "charity.gift-aid-overseas-name.error.empty.agent" else "charity.gift-aid-overseas-name.error.empty.individual")

  val NotCharLimit: Constraint[String] = validateSize(charLimit)("charity.gift-aid-overseas-name.error.limit")
  val NotInvalidChar: Constraint[String] = validateChar("charity.gift-aid-overseas-name.error.invalid")

  def notDuplicate(previousNames: List[String]): Constraint[String] = validateNotDuplicate(previousNames)("charity.gift-aid-overseas-name.error.duplicate")

  def giftAidOverseasNameForm(previousNames: List[String], isAgent: Boolean): Form[String] = Form(
      giftAidOverseasName -> trimmedText.verifying(
        notEmpty(isAgent) andThen NotCharLimit andThen NotInvalidChar andThen notDuplicate(previousNames)
      )
  )
}
