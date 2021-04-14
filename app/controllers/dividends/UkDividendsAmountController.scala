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
import controllers.predicates.AuthorisedAction
import controllers.predicates.TaxYearAction.taxYearAction
import forms.UkDividendsAmountForm
import models.{DividendsCheckYourAnswersModel, DividendsPriorSubmission, User}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.libs.json.{Json, Reads}
import play.api.mvc._
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.dividends.UkDividendsAmountView

import javax.inject.Inject

class UkDividendsAmountController @Inject()(
                                             implicit val cc: MessagesControllerComponents,
                                             authAction: AuthorisedAction,
                                             ukDividendsAmountView: UkDividendsAmountView,
                                             implicit val appConfig: AppConfig
                                           ) extends FrontendController(cc) with I18nSupport {

  def view(
            formInput: Form[BigDecimal],
            priorSubmission: Option[DividendsPriorSubmission] = None,
            taxYear: Int,
            preAmount: Option[BigDecimal] = None
          )(implicit user: User[AnyContent]): Html = {

    ukDividendsAmountView(
      form = formInput,
      priorSubmission = priorSubmission,
      taxYear = taxYear,
      postAction = controllers.dividends.routes.UkDividendsAmountController.submit(taxYear),
      preAmount = preAmount
    )

  }

  def show(taxYear: Int): Action[AnyContent] = (authAction andThen taxYearAction(taxYear)) { implicit user =>
    val dividendsPriorSubmissionSession = getSessionData[DividendsPriorSubmission](SessionValues.DIVIDENDS_PRIOR_SUB)
    val checkYourAnswerSession = getSessionData[DividendsCheckYourAnswersModel](SessionValues.DIVIDENDS_CYA)

    val previousAmount: Option[BigDecimal] = checkYourAnswerSession.flatMap(_.ukDividendsAmount)

    (dividendsPriorSubmissionSession, checkYourAnswerSession) match {
      case (Some(submission@DividendsPriorSubmission(Some(prior), _)), _) =>
          Ok(view(UkDividendsAmountForm.ukDividendsAmountForm().fill(previousAmount.getOrElse(prior)), Some(submission), taxYear, previousAmount))
      case (None, Some(cya)) =>
        Ok(view(cya.ukDividendsAmount.fold(
          UkDividendsAmountForm.ukDividendsAmountForm()
        )(amount => UkDividendsAmountForm.ukDividendsAmountForm().fill(amount)), taxYear = taxYear))


      case _ =>
        Ok(view(UkDividendsAmountForm.ukDividendsAmountForm(), taxYear = taxYear))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction { implicit user =>

    implicit val priorSubmissionSessionData: Option[DividendsPriorSubmission] = {
      getSessionData[DividendsPriorSubmission](SessionValues.DIVIDENDS_PRIOR_SUB)
    }
    val previousAmount: Option[BigDecimal] = getSessionData[DividendsCheckYourAnswersModel](SessionValues.DIVIDENDS_CYA)
      .flatMap(_.ukDividendsAmount)

    UkDividendsAmountForm.ukDividendsAmountForm().bindFromRequest().fold(
          {
            formWithErrors => BadRequest(view(formWithErrors, priorSubmission = priorSubmissionSessionData, taxYear = taxYear, preAmount = previousAmount))
          },
          {
            bigDecimal =>
              DividendsCheckYourAnswersModel.fromSession().fold {
                Redirect(redirectLocation(taxYear, None))
                  .addingToSession(SessionValues.DIVIDENDS_CYA ->
                    DividendsCheckYourAnswersModel().copy(ukDividends = Some(true), ukDividendsAmount = Some(bigDecimal)).asJsonString)
              } {
                cyaModel =>
                  Redirect(redirectLocation(taxYear, Some(cyaModel.copy(ukDividends = Some(true), ukDividendsAmount = Some(bigDecimal)))))
                    .addingToSession(SessionValues.DIVIDENDS_CYA ->
                      cyaModel.copy(ukDividends = Some(true), ukDividendsAmount = Some(bigDecimal)).asJsonString)
              }
          }
        )

  }

  private[dividends] def redirectLocation(taxYear: Int, cyaData: Option[DividendsCheckYourAnswersModel])(
    implicit priorSub: Option[DividendsPriorSubmission]
  ): Call = {
    if (
      priorSub.flatMap(_.ukDividends.map(_ => true)).getOrElse(false) ||
        cyaData.exists(_.isFinished)
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
