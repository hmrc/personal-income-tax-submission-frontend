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
import forms.{OtherDividendsAmountForm, PriorOrNewAmountForm}
import javax.inject.Inject
import models.formatHelpers.PriorOrNewAmountModel
import models.{CurrencyAmountModel, DividendsCheckYourAnswersModel, DividendsPriorSubmission, User}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.libs.json.{Json, Reads}
import play.api.mvc._
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.dividends.OtherDividendsAmountView

class OtherDividendsAmountController @Inject()(
                                             cc: MessagesControllerComponents,
                                             authAction: AuthorisedAction,
                                             otherDividendsAmountView: OtherDividendsAmountView,
                                             implicit val appConfig: AppConfig
                                           ) extends FrontendController(cc) with I18nSupport {

  val radioErrorLocation = "dividends.other-dividends-amount"

  def view(
            formInput: Either[Form[PriorOrNewAmountModel], Form[CurrencyAmountModel]],
            priorSubmission: Option[DividendsPriorSubmission] = None
          )(implicit user: User[AnyContent]): Html = {

    otherDividendsAmountView(
      form = formInput,
      priorAmount = priorSubmission,
      postAction = controllers.dividends.routes.OtherDividendsAmountController.submit(),
      backUrl = controllers.dividends.routes.ReceiveOtherDividendsController.show().url
    )

  }

  def show: Action[AnyContent] = authAction { implicit user =>
    getSessionData[DividendsPriorSubmission](SessionValues.DIVIDENDS_PRIOR_SUB) match {
      case Some(priorSubmission) if priorSubmission.otherDividends.nonEmpty =>
        Ok(view(Left(PriorOrNewAmountForm.priorOrNewAmountForm(priorSubmission.otherDividends.get, radioErrorLocation)), Some(priorSubmission)))
      case _ =>
        Ok(view(Right(OtherDividendsAmountForm.otherDividendsAmountForm())))
    }

  }

  def submit: Action[AnyContent] = authAction { implicit user =>

    implicit val priorSubmissionSessionData: Option[DividendsPriorSubmission] =
      getSessionData[DividendsPriorSubmission](SessionValues.DIVIDENDS_PRIOR_SUB)
    implicit val cyaSessionData: Option[DividendsCheckYourAnswersModel] =
      getSessionData[DividendsCheckYourAnswersModel](SessionValues.DIVIDENDS_CYA)

    priorSubmissionSessionData match {
      case Some(priorSubmission) if priorSubmission.otherDividends.nonEmpty =>
        PriorOrNewAmountForm.priorOrNewAmountForm(priorSubmission.otherDividends.get, radioErrorLocation).bindFromRequest().fold(
          {
            formWithErrors => BadRequest(view(Left(formWithErrors), Some(priorSubmission)))
          },
          {
            formModel =>
              DividendsCheckYourAnswersModel.fromSession().fold {
                Redirect(appConfig.incomeTaxSubmissionOverviewUrl)
              } {
                cyaModel =>
                  import PriorOrNewAmountModel._
                  formModel.whichAmount match {
                    case `prior` =>
                      Redirect(controllers.dividends.routes.DividendsCYAController.show())
                        .addingToSession(SessionValues.DIVIDENDS_CYA -> cyaModel.copy(otherDividendsAmount = priorSubmission.otherDividends).asJsonString)
                    case `other` =>
                      Redirect(controllers.dividends.routes.DividendsCYAController.show())
                        .addingToSession(SessionValues.DIVIDENDS_CYA -> cyaModel.copy(otherDividendsAmount = formModel.amount).asJsonString)
                  }
              }
          }
        )
      case _ =>
        OtherDividendsAmountForm.otherDividendsAmountForm().bindFromRequest().fold(
          {
            formWithErrors => BadRequest(view(Right(formWithErrors)))
          },
          {
            formModel =>
              DividendsCheckYourAnswersModel.fromSession().fold {
                Redirect(appConfig.incomeTaxSubmissionOverviewUrl)
              } {
                cyaModel =>
                  Redirect(controllers.dividends.routes.DividendsCYAController.show())
                    .addingToSession(SessionValues.DIVIDENDS_CYA -> cyaModel.copy(otherDividendsAmount = Some(BigDecimal(formModel.amount))).asJsonString)
              }
          }
        )

    }
  }

  private[dividends] def getSessionData[T](key: String)(implicit user: User[_], reads: Reads[T]): Option[T] = {
    user.session.get(key).flatMap { stringValue =>
      Json.parse(stringValue).asOpt[T]
    }
  }

}
