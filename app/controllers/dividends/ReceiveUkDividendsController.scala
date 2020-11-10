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
import forms.YesNoForm
import javax.inject.Inject
import models.DividendsCheckYourAnswersModel
import models.formatHelpers.YesNoModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.dividends.ReceiveUkDividendsView

class ReceiveUkDividendsController @Inject()(
                                              cc: MessagesControllerComponents,
                                              authAction: AuthorisedAction,
                                              receiveUkDividendsView: ReceiveUkDividendsView,
                                              implicit val appConfig: AppConfig
                                            ) extends FrontendController(cc) with I18nSupport {

  val yesNoForm: Form[YesNoModel] = YesNoForm.yesNoForm("dividends.uk-dividends.errors.noChoice")

  def show: Action[AnyContent] = authAction { implicit user =>
    Ok(receiveUkDividendsView("dividends.uk-dividends.heading." + (if (user.isAgent) "agent" else "individual"), yesNoForm))
  }

  def submit: Action[AnyContent] = authAction { implicit user =>
    yesNoForm.bindFromRequest().fold(
      {
        formWithErrors => BadRequest(receiveUkDividendsView("dividends.uk-dividends.heading." + (if (user.isAgent) "agent" else "individual"), formWithErrors))
      },
      {
        yesNoModel =>
          val cyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel.fromSession() match {
            case Some(model) => model
            case None => DividendsCheckYourAnswersModel()
          }

          if (yesNoModel.asBoolean) {
            Redirect(controllers.dividends.routes.UkDividendsAmountController.show())
              .addingToSession(SessionValues.DIVIDENDS_CYA -> cyaModel.copy(ukDividends = true).asJsonString)
          } else {
            Redirect(controllers.dividends.routes.ReceiveOtherUkDividendsController.show())
              .addingToSession(SessionValues.DIVIDENDS_CYA -> cyaModel.copy(ukDividends = false, ukDividendsAmount = None).asJsonString)
          }
      }
    )
  }

}
