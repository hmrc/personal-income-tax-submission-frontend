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

package controllers.interest

import common.{PageLocations, SessionValues}
import config.AppConfig
import controllers.predicates.AuthorisedAction
import forms.YesNoForm
import javax.inject.Inject
import models.formatHelpers.YesNoModel
import models.interest.InterestCYAModel
import play.api.data.Form
import play.api.i18n.{I18nSupport, Lang, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.InterestSessionHelper
import views.html.interest.UntaxedInterestView

import scala.concurrent.ExecutionContext

class UntaxedInterestController @Inject()(
                                           mcc: MessagesControllerComponents,
                                           authAction: AuthorisedAction,
                                           untaxedInterestView: UntaxedInterestView)(
                                           implicit val appConfig: AppConfig)
  extends FrontendController(mcc) with I18nSupport with InterestSessionHelper {

  implicit val executionContext: ExecutionContext = mcc.executionContext
  implicit val messages: Messages = mcc.messagesApi.preferred(Seq(Lang("en")))
  val yesNoForm: Form[YesNoModel] = YesNoForm.yesNoForm("interest.untaxed-uk-interest.errors.noRadioSelected")

  private[interest] def backLink(taxYear: Int)(implicit request: Request[_]): Option[String] = {
    if(getModelFromSession[InterestCYAModel](SessionValues.INTEREST_CYA).exists(_.isFinished)) {
      Some(PageLocations.Interest.cya(taxYear))
    } else {
      Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }
  }

  def show(taxYear: Int): Action[AnyContent] = authAction { implicit user =>
    val pageTitle: String = "interest.untaxed-uk-interest.heading." + (if (user.isAgent) "agent" else "individual")
    Ok(untaxedInterestView(pageTitle, yesNoForm, taxYear, backLink = backLink(taxYear)))
      .updateUntaxedAmountRedirect(PageLocations.Interest.UntaxedView(taxYear))
      .updateCyaRedirect(PageLocations.Interest.UntaxedView(taxYear))
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction { implicit user =>
    val cyaData: InterestCYAModel = getModelFromSession[InterestCYAModel](SessionValues.INTEREST_CYA)
      .getOrElse(InterestCYAModel(None, None, None, None))

    yesNoForm.bindFromRequest().fold(
      {
        formWithErrors =>
          BadRequest(
            untaxedInterestView(
              "interest.untaxed-uk-interest.heading." + (if (user.isAgent) "agent" else "individual"),
              formWithErrors,
              taxYear,
              backLink = backLink(taxYear)
            )
          )
      },
      {
        yesNoModel =>
          val updatedCya = cyaData.copy(untaxedUkInterest = Some(yesNoModel.asBoolean), untaxedUkAccounts = if (yesNoModel.asBoolean) {
            cyaData.untaxedUkAccounts
          } else {
            None
          })

          (yesNoModel.asBoolean, updatedCya.isFinished) match {
            case (true, false) =>
              Redirect(controllers.interest.routes.UntaxedInterestAmountController.show(taxYear))
                .addingToSession(SessionValues.INTEREST_CYA -> updatedCya.asJsonString)
            case (false, false) =>
              Redirect(controllers.interest.routes.TaxedInterestController.show(taxYear))
                .addingToSession(SessionValues.INTEREST_CYA -> updatedCya.asJsonString)
            case (_, true) =>
              Redirect(controllers.interest.routes.InterestCYAController.show(taxYear))
                .addingToSession(SessionValues.INTEREST_CYA -> updatedCya.asJsonString)
          }
      }
    )
  }


}
