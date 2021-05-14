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
import controllers.dividends.routes.UkDividendsAmountController
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import controllers.predicates.{AuthorisedAction, QuestionsJourneyValidator}
import forms.UkDividendsAmountForm.ukDividendsAmountForm
import models.question.QuestionsJourney
import models.{DividendsCheckYourAnswersModel, DividendsPriorSubmission, User}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.dividends.UkDividendsAmountView

import javax.inject.Inject

class UkDividendsAmountController @Inject()(
                                             implicit val cc: MessagesControllerComponents,
                                             authAction: AuthorisedAction,
                                             ukDividendsAmountView: UkDividendsAmountView,
                                             questionHelper: QuestionsJourneyValidator,
                                             implicit val appConfig: AppConfig
                                           ) extends FrontendController(cc) with I18nSupport with SessionHelper {

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
      postAction = UkDividendsAmountController.submit(taxYear),
      preAmount = preAmount
    )

  }

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, DIVIDENDS).apply { implicit user =>
    implicit val questionsJourney: QuestionsJourney[DividendsCheckYourAnswersModel] = DividendsCheckYourAnswersModel.journey(taxYear)
    val priorSubmission: Option[DividendsPriorSubmission] = getModelFromSession[DividendsPriorSubmission](SessionValues.DIVIDENDS_PRIOR_SUB)
    val cyaModel: Option[DividendsCheckYourAnswersModel] = getModelFromSession[DividendsCheckYourAnswersModel](SessionValues.DIVIDENDS_CYA)

    questionHelper.validate(UkDividendsAmountController.show(taxYear), cyaModel, taxYear) {
      val priorUkDividendAmount: Option[BigDecimal] = priorSubmission.flatMap(_.ukDividends)
      val cyaUkDividendAmount: Option[BigDecimal] = cyaModel.flatMap(_.ukDividendsAmount)

      val amountForm = (priorUkDividendAmount, cyaUkDividendAmount) match {
        case (priorAmountOpt, Some(cyaAmount)) if !priorAmountOpt.contains(cyaAmount) => ukDividendsAmountForm.fill(cyaAmount)
        case (None, Some(cyaAmount)) => ukDividendsAmountForm.fill(cyaAmount)
        case _ => ukDividendsAmountForm
      }

      (priorSubmission, cyaModel) match {
        case (Some(submission: DividendsPriorSubmission), _) => Ok(view(amountForm, Some(submission), taxYear, cyaUkDividendAmount))
        case (None, Some(cya)) => Ok(view(amountForm, taxYear = taxYear, preAmount = cya.ukDividendsAmount))
        case _ => Ok(view(ukDividendsAmountForm, taxYear = taxYear))
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, DIVIDENDS)) { implicit user =>

    implicit val priorSubmissionSessionData: Option[DividendsPriorSubmission] = {
      getModelFromSession[DividendsPriorSubmission](SessionValues.DIVIDENDS_PRIOR_SUB)
    }
    val previousAmount: Option[BigDecimal] =
      getModelFromSession[DividendsCheckYourAnswersModel](SessionValues.DIVIDENDS_CYA).flatMap(_.ukDividendsAmount)

    ukDividendsAmountForm.bindFromRequest().fold(
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

}
