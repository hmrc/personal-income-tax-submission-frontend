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

package controllers.dividends

import common.SessionValues
import config.AppConfig
import controllers.predicates.{AuthorisedAction, TaxYearFilter}
import forms.YesNoForm

import javax.inject.Inject
import models.{DividendsCheckYourAnswersModel, DividendsPriorSubmission}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.dividends.ReceiveOtherUkDividendsView

class ReceiveOtherUkDividendsController @Inject()(
                                                 cc: MessagesControllerComponents,
                                                 authAction: AuthorisedAction,
                                                 receiveOtherDividendsView: ReceiveOtherUkDividendsView,
                                                 implicit val appConfig: AppConfig
                                          ) extends FrontendController(cc) with I18nSupport with SessionHelper with TaxYearFilter{

  val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm("dividends.other-dividends.errors.noChoice")

  def show(taxYear: Int): Action[AnyContent] = authAction { implicit user =>
    taxYearFilter(taxYear)(
    DividendsPriorSubmission.fromSession() match {
      case Some(prior) if prior.otherUkDividends.nonEmpty => Redirect(controllers.dividends.routes.DividendsCYAController.show(taxYear))
      case _ =>
        val cyaData: Option[Boolean] = getModelFromSession[DividendsCheckYourAnswersModel](SessionValues.DIVIDENDS_CYA).flatMap(_.otherUkDividends)
        Ok(receiveOtherDividendsView(cyaData.fold(yesNoForm)(yesNoForm.fill), taxYear))
    }
    )
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction { implicit user =>
    yesNoForm.bindFromRequest().fold (
      {
        formWithErrors => BadRequest(
          receiveOtherDividendsView(formWithErrors, taxYear)
        )
      },
      {
        yesNoModel =>
          val cyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel.fromSession() match {
            case Some(model) => model
            case None => DividendsCheckYourAnswersModel()
          }

          if (yesNoModel) {
            Redirect(controllers.dividends.routes.OtherUkDividendsAmountController.show(taxYear))
              .addingToSession(SessionValues.DIVIDENDS_CYA -> cyaModel.copy(otherUkDividends = Some(true)).asJsonString)
          } else {
            Redirect(controllers.dividends.routes.DividendsCYAController.show(taxYear))
              .addingToSession(SessionValues.DIVIDENDS_CYA -> cyaModel.copy(otherUkDividends = Some(false), otherUkDividendsAmount = None).asJsonString)
          }
      }
    )
  }

}
