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
import controllers.dividends.routes.OtherUkDividendsAmountController
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import controllers.predicates.{AuthorisedAction, QuestionsJourneyValidator}
import forms.OtherDividendsAmountForm.otherDividendsAmountForm
import models.question.QuestionsJourney
import models.{DividendsCheckYourAnswersModel, DividendsPriorSubmission, User}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.dividends.OtherUkDividendsAmountView

import javax.inject.Inject

class OtherUkDividendsAmountController @Inject()(
                                                  implicit val cc: MessagesControllerComponents,
                                                  authAction: AuthorisedAction,
                                                  otherDividendsAmountView: OtherUkDividendsAmountView,
                                                  questionHelper: QuestionsJourneyValidator,
                                                  implicit val appConfig: AppConfig
                                           ) extends FrontendController(cc) with I18nSupport with SessionHelper {

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
      postAction = OtherUkDividendsAmountController.submit(taxYear),
      preAmount = preAmount
    )

  }

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, DIVIDENDS).apply { implicit user =>
    implicit val questionsJourney: QuestionsJourney[DividendsCheckYourAnswersModel] = DividendsCheckYourAnswersModel.journey(taxYear)

    val priorSubmission = getModelFromSession[DividendsPriorSubmission](SessionValues.DIVIDENDS_PRIOR_SUB)
    val cyaModel = getModelFromSession[DividendsCheckYourAnswersModel](SessionValues.DIVIDENDS_CYA)

    questionHelper.validate(OtherUkDividendsAmountController.show(taxYear), cyaModel, taxYear) {
      val priorOtherDividendAmount: Option[BigDecimal] = priorSubmission.flatMap(_.otherUkDividends)
      val cyaOtherDividendAmount: Option[BigDecimal] = cyaModel.flatMap(_.otherUkDividendsAmount)

      val amountForm = (priorOtherDividendAmount, cyaOtherDividendAmount) match {
        case (priorAmountOpt, Some(cyaAmount)) if !priorAmountOpt.contains(cyaAmount) => otherDividendsAmountForm.fill(cyaAmount)
        case (None, Some(cyaAmount)) => otherDividendsAmountForm.fill(cyaAmount)
        case _ => otherDividendsAmountForm
      }

      (priorSubmission, cyaModel) match {
        case (Some(submission: DividendsPriorSubmission), _) => Ok(view(amountForm, Some(submission), taxYear, cyaOtherDividendAmount))
        case (None, Some(cya)) => Ok(view(amountForm, taxYear = taxYear, preAmount = cya.otherUkDividendsAmount))
        case _ => Ok(view(otherDividendsAmountForm, taxYear = taxYear))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, DIVIDENDS)) { implicit user =>

    implicit val priorSubmissionSessionData: Option[DividendsPriorSubmission] =
      getModelFromSession[DividendsPriorSubmission](SessionValues.DIVIDENDS_PRIOR_SUB)

    val previousAmount: Option[BigDecimal] =
      getModelFromSession[DividendsCheckYourAnswersModel](SessionValues.DIVIDENDS_CYA).flatMap(_.otherUkDividendsAmount)

    otherDividendsAmountForm.bindFromRequest().fold(
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

}
