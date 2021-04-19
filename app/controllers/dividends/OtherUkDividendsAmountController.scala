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
import config.{AppConfig, DIVIDENDS}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import forms.OtherDividendsAmountForm
import models.{DividendsCheckYourAnswersModel, DividendsPriorSubmission, User}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.libs.json.{Json, Reads}
import play.api.mvc._
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.dividends.OtherUkDividendsAmountView

import javax.inject.Inject

class OtherUkDividendsAmountController @Inject()(
                                             implicit val cc: MessagesControllerComponents,
                                             authAction: AuthorisedAction,
                                             otherDividendsAmountView: OtherUkDividendsAmountView,
                                             implicit val appConfig: AppConfig
                                           ) extends FrontendController(cc) with I18nSupport {



  def view(
            formInput: Form[BigDecimal],
            priorSubmission: Option[DividendsPriorSubmission] = None,
            taxYear: Int,
            preAmount: Option[BigDecimal] = None
          )(implicit user: User[AnyContent]): Html = {

    otherDividendsAmountView(
      form = formInput,
      priorSubmission = priorSubmission,
      taxYear = taxYear,
      postAction = controllers.dividends.routes.OtherUkDividendsAmountController.submit(taxYear),
      preAmount = preAmount
    )

  }

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, DIVIDENDS).apply { implicit user =>
    val dividendsPriorSubmissionSession: Option[DividendsPriorSubmission] = getSessionData[DividendsPriorSubmission](SessionValues.DIVIDENDS_PRIOR_SUB)
    val checkYourAnswerSession: Option[DividendsCheckYourAnswersModel] = getSessionData[DividendsCheckYourAnswersModel](SessionValues.DIVIDENDS_CYA)

    val previousAmount: Option[BigDecimal] = checkYourAnswerSession.flatMap(_.otherUkDividendsAmount)

    (dividendsPriorSubmissionSession, checkYourAnswerSession) match {
      case (Some(submission@DividendsPriorSubmission(_, Some(prior))), _) =>

        if(previousAmount.contains(prior)) {
          Ok(view(OtherDividendsAmountForm.otherDividendsAmountForm(), Some(submission), taxYear, previousAmount))
        }
        else {
          Ok(view(OtherDividendsAmountForm.otherDividendsAmountForm().fill(previousAmount.getOrElse(prior)), Some(submission), taxYear, previousAmount))
        }

      case (None, Some(cya)) =>
        Ok(view(cya.otherUkDividendsAmount.fold(
          OtherDividendsAmountForm.otherDividendsAmountForm()
        )(amount => OtherDividendsAmountForm.otherDividendsAmountForm().fill(amount)),
          taxYear = taxYear, preAmount = cya.otherUkDividendsAmount))
      case _ =>
        Ok(view(OtherDividendsAmountForm.otherDividendsAmountForm(), taxYear = taxYear))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, DIVIDENDS)) { implicit user =>

    implicit val priorSubmissionSessionData: Option[DividendsPriorSubmission] =
      getSessionData[DividendsPriorSubmission](SessionValues.DIVIDENDS_PRIOR_SUB)

    val previousAmount: Option[BigDecimal] = getSessionData[DividendsCheckYourAnswersModel](SessionValues.DIVIDENDS_CYA)
      .flatMap(_.otherUkDividendsAmount)

    OtherDividendsAmountForm.otherDividendsAmountForm().bindFromRequest().fold(
      {
        formWithErrors => BadRequest(view(formWithErrors, taxYear = taxYear,
          priorSubmission = priorSubmissionSessionData, preAmount = previousAmount))
      },
      {
        bigDecimal =>
          DividendsCheckYourAnswersModel.fromSession().fold {
            Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
          } {
            cyaModel =>
              Redirect(controllers.dividends.routes.DividendsCYAController.show(taxYear))
                .addingToSession(SessionValues.DIVIDENDS_CYA -> cyaModel.copy(otherUkDividendsAmount = Some(bigDecimal)).asJsonString)
          }
      }
    )
  }

  private[dividends] def getSessionData[T](key: String)(implicit user: User[_], reads: Reads[T]): Option[T] = {
    user.session.get(key).flatMap { stringValue =>
      Json.parse(stringValue).asOpt[T]
    }
  }

}
