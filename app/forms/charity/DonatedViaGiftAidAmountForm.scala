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

import forms.validation.mappings.MappingUtil.currency
import play.api.data.Form

object DonatedViaGiftAidAmountForm {
  val donatedViaGiftAidAmount: String = "amount"

  def donatedViaGiftAidForm(isAgent: Boolean): Form[BigDecimal] = Form(
    donatedViaGiftAidAmount -> currency(
      requiredKey = if(isAgent) "charity.amount-via-gift-aid.error.agent.no-input" else "charity.amount-via-gift-aid.error.individual.no-input",
      invalidNumeric = if (isAgent) "charity.amount-via-gift-aid.error.agent.incorrect-format" else {
        "charity.amount-via-gift-aid.error.individual.incorrect-format"
      },
      maxAmountKey = if(isAgent) "charity.amount-via-gift-aid.error.agent.too-high" else "charity.amount-via-gift-aid.error.individual.too-high"
    )
  )
}
