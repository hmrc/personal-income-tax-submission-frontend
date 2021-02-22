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

import java.util.UUID.randomUUID

import common.SessionValues
import config.AppConfig
import controllers.predicates.AuthorisedAction
import forms.YesNoForm
import javax.inject.Inject

import models.interest.InterestCYAModel
import play.api.Logger
import play.api.data.Form
import play.api.i18n.{Lang, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.InterestSessionHelper
import views.html.interest.TaxedInterestView

import scala.concurrent.ExecutionContext


class TaxedInterestController @Inject()(
                                         mcc: MessagesControllerComponents,
                                         authorisedAction: AuthorisedAction,
                                         taxedInterestView: TaxedInterestView
                                       )(implicit appConfig: AppConfig) extends FrontendController(mcc) with InterestSessionHelper {

  implicit val executionContext: ExecutionContext = mcc.executionContext
  implicit val messages: Messages = mcc.messagesApi.preferred(Seq(Lang("en")))
  val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm("interest.taxed-uk-interest.errors.noRadioSelected")

  def show(taxYear: Int): Action[AnyContent] = authorisedAction { implicit user =>
    val cyaData: Option[Boolean] = getModelFromSession[InterestCYAModel](SessionValues.INTEREST_CYA).flatMap(_.taxedUkInterest)
    Ok(taxedInterestView(cyaData.fold(yesNoForm)(yesNoForm.fill), taxYear))
  }

  def submit(taxYear: Int): Action[AnyContent] = authorisedAction { implicit user =>
    val optionalCyaData: Option[InterestCYAModel] = getModelFromSession[InterestCYAModel](SessionValues.INTEREST_CYA)

    yesNoForm.bindFromRequest().fold(
      {
        formWithErrors =>
          BadRequest(taxedInterestView(
            form = formWithErrors,
            taxYear = taxYear
          ))
      },
      {
        yesNoModel =>
          optionalCyaData match {
            case Some(cyaData) =>
              val updatedCya = cyaData.copy(taxedUkInterest = Some(yesNoModel), taxedUkAccounts = if (yesNoModel) {
                cyaData.taxedUkAccounts
              } else {
                None
              })

              if (yesNoModel) {
                Redirect(controllers.interest.routes.TaxedInterestAmountController.show(taxYear, id = randomUUID().toString))
                  .addingToSession(SessionValues.INTEREST_CYA -> updatedCya.asJsonString)
              } else {
                Redirect(controllers.interest.routes.InterestCYAController.show(taxYear))
                  .addingToSession(SessionValues.INTEREST_CYA -> updatedCya.asJsonString)
              }
            case _ =>
              Logger.logger.info("[TaxedInterestController][submit] No CYA data in session. Redirecting to overview page.")
              Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
          }
      }
    )
  }

}
