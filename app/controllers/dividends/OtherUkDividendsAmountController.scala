/*
 * Copyright 2023 HM Revenue & Customs
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

import config.{AppConfig, DIVIDENDS, ErrorHandler}
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import controllers.predicates.{AuthorisedAction, QuestionsJourneyValidator}
import forms.AmountForm
import models.User
import models.dividends.DividendsCheckYourAnswersModel
import models.question.QuestionsJourney
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import services.DividendsSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.dividends.OtherUkDividendsAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OtherUkDividendsAmountController @Inject()(
                                                  implicit val cc: MessagesControllerComponents,
                                                  authAction: AuthorisedAction,
                                                  otherDividendsAmountView: OtherUkDividendsAmountView,
                                                  questionHelper: QuestionsJourneyValidator,
                                                  dividendsSessionService: DividendsSessionService,
                                                  errorHandler: ErrorHandler,
                                                  implicit val appConfig: AppConfig,
                                                  ec: ExecutionContext
                                           ) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def agentOrIndividual(implicit isAgent: Boolean): String = if (isAgent) "agent" else "individual"

  def form(implicit isAgent: Boolean, taxYear: Int): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "dividends.other-dividends-amount.error.empty." + agentOrIndividual,
    wrongFormatKey = "dividends.common.error.invalidFormat." + agentOrIndividual,
    exceedsMaxAmountKey = "dividends.other-dividends-amount.error.amountMaxLimit",
    emptyFieldArguments = Seq(taxYear.toString)
  )

  def view(
            formInput: Form[BigDecimal],
            taxYear: Int,
            preAmount: Option[BigDecimal] = None
          )(implicit user: User[AnyContent]): Html = {

    otherDividendsAmountView(
      form = preAmount.fold(formInput)(formInput.fill),
      taxYear = taxYear,
      postAction = controllers.dividends.routes.OtherUkDividendsAmountController.submit(taxYear),
      preAmount = preAmount
    )

  }

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, DIVIDENDS).async { implicit user =>
    implicit val questionsJourney: QuestionsJourney[DividendsCheckYourAnswersModel] = DividendsCheckYourAnswersModel.journey(taxYear)



    dividendsSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>
      Future.successful(questionHelper.validate(controllers.dividends.routes.OtherUkDividendsAmountController.show(taxYear), cya, taxYear) {
        val cyaOtherDividendAmount: Option[BigDecimal] = cya.flatMap(_.otherUkDividendsAmount)

        val amountForm = cyaOtherDividendAmount match {
          case Some(cyaAmount) => form(user.isAgent, taxYear).fill(cyaAmount)
          case _ => form(user.isAgent, taxYear)
        }

        cya match {
          case Some(cya) => Ok(view(amountForm, taxYear = taxYear, preAmount = cya.otherUkDividendsAmount))
          case _ => Ok(view(form(user.isAgent, taxYear), taxYear = taxYear))
        }
      })
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, DIVIDENDS)).async { implicit user =>
      dividendsSessionService.getAndHandle(taxYear)(errorHandler.futureInternalServerError()) { (cya, prior) =>
      val previousAmount: Option[BigDecimal] = cya.flatMap(_.otherUkDividendsAmount)

      form(user.isAgent, taxYear).bindFromRequest().fold(
        {
          formWithErrors => Future.successful(Future.successful(BadRequest(view(formWithErrors, taxYear = taxYear,
            preAmount = previousAmount))))
        },
        {
          bigDecimal =>
            cya.fold {
              Future.successful(Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))))
            } {
              cyaModel =>
                dividendsSessionService.updateSessionData(cyaModel.copy(otherUkDividendsAmount = Some(bigDecimal)), taxYear)(
                  Future.successful(InternalServerError(errorHandler.internalServerErrorTemplate))
                )(
                  Future.successful(Redirect(controllers.dividends.routes.DividendsCYAController.show(taxYear)))
                )
            }
        }
      )
    }.flatten
  }

}
