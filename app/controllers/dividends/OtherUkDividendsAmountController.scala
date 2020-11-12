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
import views.html.dividends.OtherUkDividendsAmountView

class OtherUkDividendsAmountController @Inject()(
                                             cc: MessagesControllerComponents,
                                             authAction: AuthorisedAction,
                                             otherDividendsAmountView: OtherUkDividendsAmountView,
                                             implicit val appConfig: AppConfig
                                           ) extends FrontendController(cc) with I18nSupport {

  val radioErrorLocation = "dividends.other-dividends-amount"

  def view(
            formInput: Either[Form[PriorOrNewAmountModel], Form[CurrencyAmountModel]],
            priorSubmission: Option[DividendsPriorSubmission] = None,
            previousAmount: Option[String] = None,
            isEditMode: Boolean,
            taxYear: Int
          )(implicit user: User[AnyContent]): Html = {

    otherDividendsAmountView(
      form = formInput,
      priorSubmission = priorSubmission,
      postAction = controllers.dividends.routes.OtherUkDividendsAmountController.submit(taxYear),
      backUrl = backLink(taxYear, isEditMode),
      preAmount = previousAmount
    )

  }

  def show(taxYear: Int, isEditMode: Boolean): Action[AnyContent] = authAction { implicit user =>
    getSessionData[DividendsPriorSubmission](SessionValues.DIVIDENDS_PRIOR_SUB) match {
      case Some(priorSubmission) if priorSubmission.otherUkDividends.nonEmpty =>
        Ok(view(Left(PriorOrNewAmountForm.priorOrNewAmountForm(priorSubmission.otherUkDividends.get, radioErrorLocation)),
          Some(priorSubmission), isEditMode = isEditMode, taxYear = taxYear))
      case _ =>
        DividendsCheckYourAnswersModel.fromSession() match {
          case Some(model) => Ok(view(Right(OtherDividendsAmountForm.otherDividendsAmountForm()), previousAmount = Some(model.otherUkDividendsAmount.fold {
            ""
          } { data => data.toString()}), isEditMode = isEditMode, taxYear = taxYear))
          case None => Ok(view(Right(OtherDividendsAmountForm.otherDividendsAmountForm()),isEditMode = isEditMode, taxYear = taxYear))
        }
    }

  }

  def submit(taxYear: Int, isEditMode: Boolean): Action[AnyContent] = authAction { implicit user =>

    implicit val priorSubmissionSessionData: Option[DividendsPriorSubmission] =
      getSessionData[DividendsPriorSubmission](SessionValues.DIVIDENDS_PRIOR_SUB)
    implicit val cyaSessionData: Option[DividendsCheckYourAnswersModel] =
      getSessionData[DividendsCheckYourAnswersModel](SessionValues.DIVIDENDS_CYA)

    priorSubmissionSessionData match {
      case Some(priorSubmission) if priorSubmission.otherUkDividends.nonEmpty =>
        PriorOrNewAmountForm.priorOrNewAmountForm(priorSubmission.otherUkDividends.get, radioErrorLocation).bindFromRequest().fold(
          {
            formWithErrors => BadRequest(view(Left(formWithErrors), Some(priorSubmission),isEditMode = isEditMode, taxYear = taxYear))
          },
          {
            formModel =>
              DividendsCheckYourAnswersModel.fromSession().fold {
                Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
              } {
                cyaModel =>
                  import PriorOrNewAmountModel._
                  formModel.whichAmount match {
                    case `prior` =>
                      Redirect(controllers.dividends.routes.DividendsCYAController.show(taxYear))
                        .addingToSession(SessionValues.DIVIDENDS_CYA -> cyaModel.copy(otherUkDividendsAmount = priorSubmission.otherUkDividends).asJsonString)
                    case `other` =>
                      Redirect(controllers.dividends.routes.DividendsCYAController.show(taxYear))
                        .addingToSession(SessionValues.DIVIDENDS_CYA -> cyaModel.copy(otherUkDividendsAmount = formModel.amount).asJsonString)
                  }
              }
          }
        )
      case _ =>
        OtherDividendsAmountForm.otherDividendsAmountForm().bindFromRequest().fold(
          {
            formWithErrors => BadRequest(view(Right(formWithErrors), isEditMode = isEditMode, taxYear = taxYear))
          },
          {
            formModel =>
              DividendsCheckYourAnswersModel.fromSession().fold {
                Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
              } {
                cyaModel =>
                  Redirect(controllers.dividends.routes.DividendsCYAController.show(taxYear))
                    .addingToSession(SessionValues.DIVIDENDS_CYA -> cyaModel.copy(otherUkDividendsAmount = Some(BigDecimal(formModel.amount))).asJsonString)
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

  def backLink(taxYear: Int, isEditMode: Boolean): String ={
    if(isEditMode){
      controllers.dividends.routes.DividendsCYAController.show(taxYear).url
    } else {
      controllers.dividends.routes.ReceiveOtherUkDividendsController.show(taxYear).url
    }
  }

}
