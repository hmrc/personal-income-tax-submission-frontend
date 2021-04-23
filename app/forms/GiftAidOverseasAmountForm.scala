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

import forms.validation.mappings.MappingUtil._
import play.api.data.Form

object GiftAidOverseasAmountForm {

  val giftAidOverseasAmount: String = "amount"

  def giftAidOverseasAmountForm(isAgent: Boolean): Form[BigDecimal] = Form(
    giftAidOverseasAmount -> currency(
      requiredKey = if(isAgent) "charity.amount-overseas-gift-aid.error.empty.agent" else "charity.amount-overseas-gift-aid.error.empty.individual",
      invalidNumeric = if (isAgent) "charity.amount-overseas-gift-aid.error.incorrect-format.agent" else {
        "charity.amount-overseas-gift-aid.error.incorrect-format.individual"
      },
      nonNumericKey = if (isAgent) "charity.amount-overseas-gift-aid.error.incorrect-format.agent" else {
        "charity.amount-overseas-gift-aid.error.incorrect-format.individual"
      },
      maxAmountKey = if(isAgent) "charity.amount-overseas-gift-aid.error.too-high.agent" else "charity.amount-overseas-gift-aid.error.too-high.individual"
    )
  )

}

