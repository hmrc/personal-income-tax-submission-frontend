/*
 * Copyright 2020 HM Revenue & Customs
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

package controllers.dividends

import common.SessionValues
import config.AppConfig
import controllers.predicates.AuthorisedAction
import forms.UkDividendsAmountForm
import javax.inject.Inject
import models.{CurrencyAmountModel, DividendsCheckYourAnswersModel}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.dividends.UkDividendsAmountView

class UkDividendsAmountController @Inject()(
                                             cc: MessagesControllerComponents,
                                             authAction: AuthorisedAction,
                                             ukDividendsAmountView: UkDividendsAmountView,
                                             implicit val appConfig: AppConfig
                                          ) extends FrontendController(cc) with I18nSupport {

  def view(ukDividendsAmountForm: Form[CurrencyAmountModel])(implicit request: Request[AnyContent]): Html =
    ukDividendsAmountView(
      ukDividendsAmountForm = ukDividendsAmountForm,
      postAction = controllers.dividends.routes.UkDividendsAmountController.submit(),
      backUrl = controllers.dividends.routes.ReceiveUkDividendsController.show().url
    )

  def show: Action[AnyContent] = authAction { implicit user =>
    Ok(view(UkDividendsAmountForm.ukDividendsAmountForm()))
  }

  def submit: Action[AnyContent] = authAction { implicit user =>
    UkDividendsAmountForm.ukDividendsAmountForm().bindFromRequest().fold (
      {
        formWithErrors => BadRequest(view(formWithErrors))
      },
      {
        formModel =>
          DividendsCheckYourAnswersModel.fromSession().fold {
            Redirect(appConfig.incomeTaxSubmissionOverviewUrl)
          } {
            cyaModel =>
              Redirect(controllers.dividends.routes.ReceiveOtherDividendsController.show())
                .addingToSession(SessionValues.DIVIDENDS_CYA -> cyaModel.copy(ukDividendsAmount = Some(BigDecimal(formModel.amount))).asJsonString)
          }
      }
    )
  }

}

