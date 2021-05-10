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

package controllers.charity

import config.{AppConfig, GIFT_AID}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import forms.AmountForm
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.charity.GiftAidLandOrPropertyAmountView

import javax.inject.Inject

class GiftAidLandOrPropertyAmountController @Inject()(
                                                          implicit cc: MessagesControllerComponents,
                                                          view: GiftAidLandOrPropertyAmountView,
                                                          authorisedAction: AuthorisedAction,
                                                          appConfig: AppConfig
                                                        ) extends FrontendController(cc) with I18nSupport {
  def agentOrIndividual(implicit isAgent: Boolean): String = (if (isAgent) "agent" else "individual")

  def form(implicit isAgent: Boolean): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "charity.land-or-property.errors.no-entry." + agentOrIndividual,
    wrongFormatKey = "charity.land-or-property.errors.wrong-format." + agentOrIndividual,
    exceedsMaxAmountKey = "charity.Land-or-property.errors.max-amount." + agentOrIndividual
  )

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).apply { implicit user =>
      Ok(view(taxYear, form(user.isAgent)))
  }

  def submit(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).apply { implicit user =>

    form(user.isAgent).bindFromRequest().fold(
      { formWithErrors =>
        BadRequest(view(taxYear, formWithErrors))
      },
      { submittedAmount =>
        //TODO Add to data model during wireup
        Ok("Redirect to next page") //TODO direct to next page during wireup
      }
    )
  }

}
