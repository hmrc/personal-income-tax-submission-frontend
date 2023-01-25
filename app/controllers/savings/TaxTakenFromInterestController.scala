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

package controllers.savings

import config.{AppConfig, ErrorHandler, INTEREST}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import forms.YesNoForm
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.savings.TaxTakenFromInterestView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaxTakenFromInterestController @Inject()(
                                                view: TaxTakenFromInterestView,
                                                errorHandler: ErrorHandler
                                              )(
                                                implicit appConfig: AppConfig,
                                                mcc: MessagesControllerComponents,
                                                ec: ExecutionContext,
                                                authorisedAction: AuthorisedAction
                                              ) extends FrontendController(mcc) with I18nSupport {

  def form(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    s"savings.tax-taken-from-interest.error")

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, INTEREST).async { implicit user =>
    Future.successful(Ok(view(form(user.isAgent), taxYear)))
  }

  def submit(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, INTEREST).async {
    implicit user =>
      form(user.isAgent).bindFromRequest().fold(formWithErrors => {
        Future.successful(BadRequest(view(formWithErrors, taxYear)))
      }, {
        yesNoValue => Future.successful(Ok)
      })

  }
}
