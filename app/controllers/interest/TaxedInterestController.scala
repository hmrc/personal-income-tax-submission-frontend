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
import views.html.interest.TaxedInterestView
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.api.mvc.Results._
import controllers.predicates.AuthorisedAction
import forms.YesNoForm
import javax.inject.Inject
import models.formatHelpers.YesNoModel
import play.api.data.Form
import play.api.i18n.{Lang, Messages}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.ExecutionContext


class TaxedInterestController @Inject()(mcc: MessagesControllerComponents,
                                        authorisedAction: AuthorisedAction,
                                        taxedInterestView: TaxedInterestView
                                                 )(
                                                 implicit appConfig: AppConfig) extends FrontendController(mcc) {

  implicit val executionContext: ExecutionContext = mcc.executionContext
  implicit val messages: Messages = mcc.messagesApi.preferred(Seq(Lang("en")))
  val yesNoForm: Form[YesNoModel] = YesNoForm.yesNoForm("interest.taxed-uk-interest.errors.noRadioSelected")


  def show(taxYear: Int): Action[AnyContent] = { authorisedAction { implicit user =>
    Ok(taxedInterestView("interest.taxed-uk-interest.heading." + (if (user.isAgent) "agent" else "individual"), yesNoForm, taxYear))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = { authorisedAction { user =>
    Redirect(appConfig.signInUrl)
    }
  }

}
