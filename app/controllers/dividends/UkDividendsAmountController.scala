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
import forms.{PriorOrNewAmountForm, UkDividendsAmountForm}
import javax.inject.Inject
import models.formatHelpers.PriorOrNewAmountModel
import models.{CurrencyAmountModel, DividendsCheckYourAnswersModel, DividendsPriorSubmission, User}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.libs.json.{Json, Reads}
import play.api.mvc._
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.dividends.UkDividendsAmountView

class UkDividendsAmountController @Inject()(
                                             cc: MessagesControllerComponents,
                                             authAction: AuthorisedAction,
                                             ukDividendsAmountView: UkDividendsAmountView,
                                             implicit val appConfig: AppConfig
                                           ) extends FrontendController(cc) with I18nSupport {

  val radioErrorLocation = "dividends.uk-dividends-amount"

  def view(
            formInput: Either[Form[PriorOrNewAmountModel], Form[CurrencyAmountModel]],
            priorSubmission: Option[DividendsPriorSubmission] = None, taxYear: Int
          )(implicit user: User[AnyContent]): Html = {

    ukDividendsAmountView(
      form = formInput,
      priorSubmission = priorSubmission,
      postAction = controllers.dividends.routes.UkDividendsAmountController.submit(taxYear),
      backUrl = controllers.dividends.routes.ReceiveUkDividendsController.show(taxYear).url
    )

  }

  def show(taxYear: Int): Action[AnyContent] = authAction { implicit user =>
    getSessionData[DividendsPriorSubmission](SessionValues.DIVIDENDS_PRIOR_SUB) match {
      case Some(priorSubmission) if priorSubmission.ukDividends.nonEmpty =>
        Ok(view(Left(PriorOrNewAmountForm.priorOrNewAmountForm(priorSubmission.ukDividends.get, radioErrorLocation)), Some(priorSubmission), taxYear))
      case _ =>
        Ok(view(Right(UkDividendsAmountForm.ukDividendsAmountForm()), taxYear = taxYear))
    }

  }

  def submit(taxYear: Int): Action[AnyContent] = authAction { implicit user =>

    implicit val priorSubmissionSessionData: Option[DividendsPriorSubmission] =
      getSessionData[DividendsPriorSubmission](SessionValues.DIVIDENDS_PRIOR_SUB)
    implicit val cyaSessionData: Option[DividendsCheckYourAnswersModel] =
      getSessionData[DividendsCheckYourAnswersModel](SessionValues.DIVIDENDS_CYA)

    priorSubmissionSessionData match {
      case Some(priorSubmission) if priorSubmission.ukDividends.nonEmpty =>
        PriorOrNewAmountForm.priorOrNewAmountForm(priorSubmission.ukDividends.get, radioErrorLocation).bindFromRequest().fold(
          {
            formWithErrors => BadRequest(view(Left(formWithErrors), Some(priorSubmission), taxYear))
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
                      Redirect(redirectLocation(taxYear))
                        .addingToSession(SessionValues.DIVIDENDS_CYA -> cyaModel.copy(ukDividendsAmount = priorSubmission.ukDividends).asJsonString)
                    case `other` =>
                      Redirect(redirectLocation(taxYear))
                        .addingToSession(SessionValues.DIVIDENDS_CYA -> cyaModel.copy(ukDividendsAmount = formModel.amount).asJsonString)
                  }
              }
          }
        )
      case _ =>
        UkDividendsAmountForm.ukDividendsAmountForm().bindFromRequest().fold(
          {
            formWithErrors => BadRequest(view(Right(formWithErrors), taxYear = taxYear))
          },
          {
            formModel =>
              DividendsCheckYourAnswersModel.fromSession().fold {
                Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
              } {
                cyaModel =>
                  Redirect(redirectLocation(taxYear))
                    .addingToSession(SessionValues.DIVIDENDS_CYA -> cyaModel.copy(ukDividendsAmount = Some(BigDecimal(formModel.amount))).asJsonString)
              }
          }
        )

    }
  }

  private[dividends] def redirectLocation(taxYear: Int)(
    implicit priorSub: Option[DividendsPriorSubmission], cyaData: Option[DividendsCheckYourAnswersModel]
  ): Call = {
    if (
      priorSub.flatMap(_.ukDividends.map(_ => true)).getOrElse(false) ||
        cyaData.flatMap(_.ukDividendsAmount.map(_ => true)).getOrElse(false)
    ) {
      controllers.dividends.routes.DividendsCYAController.show(taxYear)
    } else {
      controllers.dividends.routes.ReceiveOtherUkDividendsController.show(taxYear)
    }

  }

  private[dividends] def getSessionData[T](key: String)(implicit user: User[_], reads: Reads[T]): Option[T] = {
    user.session.get(key).flatMap { stringValue =>
      Json.parse(stringValue).asOpt[T]
    }
  }

}
