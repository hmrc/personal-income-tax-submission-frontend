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
import forms.charity.GiftAidOneOffAmountForm
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

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, GIFT_AID).apply { implicit user =>
    lazy val form: Form[BigDecimal] = GiftAidOneOffAmountForm.giftAidOneOffAmountForm(user.isAgent)

    Ok(view(taxYear, form, None))
  }

  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, GIFT_AID)) { implicit user =>
    lazy val form: Form[BigDecimal] = GiftAidOneOffAmountForm.giftAidOneOffAmountForm(user.isAgent)

    form.bindFromRequest().fold(
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
