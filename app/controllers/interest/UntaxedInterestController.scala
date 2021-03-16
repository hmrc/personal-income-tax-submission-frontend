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

import common.SessionValues
import config.AppConfig
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import forms.YesNoForm
import models.interest.{InterestCYAModel, InterestPriorSubmission}
import play.api.data.Form
import play.api.i18n.{I18nSupport, Lang, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.InterestSessionHelper
import views.html.interest.UntaxedInterestView

import java.util.UUID.randomUUID
import javax.inject.Inject
import scala.concurrent.ExecutionContext

class UntaxedInterestController @Inject()(
                                           authAction: AuthorisedAction,
                                           untaxedInterestView: UntaxedInterestView)(
                                           implicit val appConfig: AppConfig,
                                           implicit val mcc: MessagesControllerComponents)
  extends FrontendController(mcc) with I18nSupport with InterestSessionHelper {

  implicit val executionContext: ExecutionContext = mcc.executionContext
  implicit val messages: Messages = mcc.messagesApi.preferred(Seq(Lang("en")))
  val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm("interest.untaxed-uk-interest.errors.noRadioSelected")

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)) { implicit user =>
    InterestPriorSubmission.fromSession() match {
      case Some(prior) if prior.hasUntaxed => Redirect(controllers.interest.routes.InterestCYAController.show(taxYear))
      case _ =>
        val cyaData: Option[Boolean] = getModelFromSession[InterestCYAModel](SessionValues.INTEREST_CYA).flatMap(_.untaxedUkInterest)
        val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm(s"interest.untaxed-uk-interest.errors.noRadioSelected.${if(user.isAgent) "agent" else "individual"}")
        Ok(untaxedInterestView(cyaData.fold(yesNoForm)(yesNoForm.fill), taxYear))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction { implicit user =>
    val cyaData: InterestCYAModel = getModelFromSession[InterestCYAModel](SessionValues.INTEREST_CYA)
      .getOrElse(InterestCYAModel(None, None, None, None))
    val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm(s"interest.untaxed-uk-interest.errors.noRadioSelected.${if(user.isAgent) "agent" else "individual"}")

    yesNoForm.bindFromRequest().fold(
      {
        formWithErrors =>
          BadRequest(
            untaxedInterestView(
              formWithErrors,
              taxYear
            )
          )
      },
      {
        yesNoModel =>
          val updatedCya = cyaData.copy(untaxedUkInterest = Some(yesNoModel), untaxedUkAccounts = if (yesNoModel) {
            cyaData.untaxedUkAccounts
          } else {
            None
          })

          (yesNoModel, updatedCya.isFinished) match {
            case (true, false) =>
              Redirect(controllers.interest.routes.UntaxedInterestAmountController.show(taxYear, id = randomUUID().toString))
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
