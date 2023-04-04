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

package controllers.dividends

import config.{AppConfig, DIVIDENDS}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import forms.YesNoForm
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.dividends.RedeemableSharesStatusView

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RedeemableSharesStatusController @Inject()(

                                                  implicit val cc: MessagesControllerComponents,
                                                  authAction: AuthorisedAction,
                                                  view: RedeemableSharesStatusView,
                                                  implicit val appConfig: AppConfig,
                                                  ec: ExecutionContext
                                                ) extends FrontendController(cc) with I18nSupport {

  def form(implicit isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(
    s"dividends.redeemable-shares-status.errors.noChoice.${if (isAgent) "agent" else "individual"}")


  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, DIVIDENDS).async { implicit user =>
    Future.successful(Ok(view(form(user.isAgent), taxYear)))
  }

  def submit(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, DIVIDENDS).async { implicit user =>
    form(user.isAgent).bindFromRequest().fold(
      formWithErrors =>
        Future.successful(BadRequest(view(formWithErrors, taxYear))),
      _ =>
        Future.successful(Ok))
  }
}