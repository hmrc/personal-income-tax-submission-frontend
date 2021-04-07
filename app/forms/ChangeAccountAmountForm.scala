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


object ChangeAccountAmountForm {

  val amount: String = "amount"

  def changeAccountAmountForm(isAgent: Boolean): Form[BigDecimal] = Form(
    amount -> currency(
      if (isAgent) "changeAccountAmount.required.agent" else "changeAccountAmount.required",
      invalidNumeric = "changeAccountAmount.format",
      maxAmountKey = "changeAccountAmount.amountMaxLimit"
    )
  )
}


