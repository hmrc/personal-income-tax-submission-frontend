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

package controllers.interest

import config.{AppConfig, INTEREST}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import forms.YesNoForm
import models.mongo.JourneyStatus.{Completed, InProgress}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.interest.SectionCompletedStateView

import javax.inject.Inject
import scala.annotation.unused
import scala.concurrent.{ExecutionContext, Future}

class SectionCompletedStateController @Inject()(implicit val cc: MessagesControllerComponents,
                                                authAction: AuthorisedAction,
                                                view: SectionCompletedStateView,
                                                implicit val appConfig: AppConfig,
                                                ec: ExecutionContext
                                                ) extends FrontendController(cc) with I18nSupport {

  def form(): Form[Boolean] = YesNoForm.yesNoForm("sectionCompletedState.error.required")

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, INTEREST).async { implicit request =>
    Future.successful(Ok(view(form(), taxYear)))
  }

  def submit(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, INTEREST).async {implicit user =>
    form()
      .bindFromRequest()
      .fold(
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear))),
        answer => saveAndRedirect(answer, taxYear)
      )
  }

// TODO: Add implementation to change status of this to in progress or completed & Redirect to common tasklist page
  private def saveAndRedirect(answer: Boolean, taxYear : Int)(implicit request: Request[_]): Future[Result] = {
    @unused
    val status = if (answer) Completed else InProgress
     Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
  }

}
