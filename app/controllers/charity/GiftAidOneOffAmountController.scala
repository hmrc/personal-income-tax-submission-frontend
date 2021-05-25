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
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import forms.AmountForm
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.charity.GiftAidOneOffAmountView

import javax.inject.Inject

class GiftAidOneOffAmountController @Inject()(
                                                implicit cc: MessagesControllerComponents,
                                                authAction: AuthorisedAction,
                                                appConfig: AppConfig,
                                                view: GiftAidOneOffAmountView
                                              ) extends FrontendController(cc) with I18nSupport {

  def agentOrIndividual(implicit isAgent: Boolean): String = if (isAgent) "agent" else "individual"

  def form(implicit isAgent: Boolean, taxYear: Int): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "charity.gift-aid-one-off-amount.error.empty." + agentOrIndividual,
    wrongFormatKey = "charity.gift-aid-one-off-amount.error.incorrect-format." + agentOrIndividual,
    exceedsMaxAmountKey = "charity.gift-aid-one-off-amount.error.too-high." + agentOrIndividual,
    emptyFieldArguments = Seq(taxYear.toString)
  )

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).apply { implicit user =>

    Ok(view(taxYear, form(user.isAgent, taxYear), None))
  }

  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, GIFT_AID)) { implicit user =>

    form(user.isAgent, taxYear).bindFromRequest().fold(
      { formWithErrors =>
        BadRequest(view(taxYear, formWithErrors, None))
      },
      { submittedAmount =>
        //TODO Add to data model during wireup
        Ok("Redirect to Did you use Gift Aid to donate to an overseas charity? page") //TODO direct to next page during wireup
      }
    )

  }

}
