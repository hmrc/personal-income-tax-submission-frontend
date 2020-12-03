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

package controllers.interest

import config.AppConfig
import controllers.predicates.AuthorisedAction
import forms.TaxedInterestAmountForm
import javax.inject.Inject
import models.TaxedInterestModel
import play.api.data.Form
import play.api.i18n.{Lang, Messages}
import play.api.mvc.MessagesControllerComponents
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.interest.TaxedInterestAmountView

import scala.concurrent.ExecutionContext

class TaxedInterestAmountController @Inject()(mcc: MessagesControllerComponents,
                                              authorisedAction: AuthorisedAction,
                                              taxedInterestAmountView: TaxedInterestAmountView
                                             )(
                                               implicit appConfig: AppConfig) extends FrontendController(mcc) {

  implicit val executionContext: ExecutionContext = mcc.executionContext
  implicit val messages: Messages = mcc.messagesApi.preferred(Seq(Lang("en")))
  val taxedInterestAmountForm: Form[TaxedInterestModel] = TaxedInterestAmountForm.taxedInterestAmountForm()

  def show(taxYear: Int) = {
    authorisedAction { implicit user =>
      Ok(taxedInterestAmountView(TaxedInterestAmountForm.taxedInterestAmountForm, taxYear,
        controllers.interest.routes.TaxedInterestAmountController.submit(taxYear),
        appConfig.signInUrl))
    }
  }

  def submit(taxYear: Int) = {
    authorisedAction { implicit user =>
      taxedInterestAmountForm.bindFromRequest().fold({
          formWithErrors =>
            println("\n\n\n\n" + formWithErrors.errors)
            BadRequest(taxedInterestAmountView(formWithErrors, taxYear, controllers.interest.routes.TaxedInterestAmountController.submit(taxYear),
            appConfig.signInUrl))
      }, {
        completeForm => Redirect(appConfig.signInUrl)
      })
    }
  }

}
