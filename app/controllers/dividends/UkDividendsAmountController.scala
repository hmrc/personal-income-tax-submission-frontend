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

import config.{AppConfig, DIVIDENDS, ErrorHandler}
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import controllers.predicates.{AuthorisedAction, QuestionsJourneyValidator}
import forms.AmountForm
import models.User
import models.dividends.{DividendsCheckYourAnswersModel, DividendsPriorSubmission}
import models.question.QuestionsJourney
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import services.DividendsSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.dividends.UkDividendsAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UkDividendsAmountController @Inject()(
                                             implicit val cc: MessagesControllerComponents,
                                             authAction: AuthorisedAction,
                                             ukDividendsAmountView: UkDividendsAmountView,
                                             questionHelper: QuestionsJourneyValidator,
                                             dividendsSessionService: DividendsSessionService,
                                             errorHandler: ErrorHandler,
                                             implicit val appConfig: AppConfig,
                                             ec: ExecutionContext
                                           ) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def agentOrIndividual(implicit isAgent: Boolean): String = if (isAgent) "agent" else "individual"

  def form(implicit isAgent: Boolean, taxYear: Int): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "dividends.uk-dividends-amount.error.empty." + agentOrIndividual,
    wrongFormatKey = "dividends.common.error.invalidFormat." + agentOrIndividual,
    exceedsMaxAmountKey = "dividends.uk-dividends-amount.error.amountMaxLimit",
    emptyFieldArguments = Seq(taxYear.toString)
  )

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

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, DIVIDENDS).async { implicit user =>
    implicit val questionsJourney: QuestionsJourney[DividendsCheckYourAnswersModel] = DividendsCheckYourAnswersModel.journey(taxYear)

    dividendsSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (optionalCya, optionalPrior) =>

      questionHelper.validate(controllers.dividends.routes.UkDividendsAmountController.show(taxYear), optionalCya, taxYear) {
        val priorUkDividendAmount: Option[BigDecimal] = optionalPrior.flatMap(_.ukDividends)
        val cyaUkDividendAmount: Option[BigDecimal] = optionalCya.flatMap(_.ukDividendsAmount)

        val amountForm = (priorUkDividendAmount, cyaUkDividendAmount) match {
          case (priorAmountOpt, Some(cyaAmount)) if !priorAmountOpt.contains(cyaAmount) => form(user.isAgent, taxYear).fill(cyaAmount)
          case (None, Some(cyaAmount)) => form(user.isAgent, taxYear).fill(cyaAmount)
          case _ => form(user.isAgent, taxYear)
        }

        (optionalPrior, optionalCya) match {
          case (Some(submission: DividendsPriorSubmission), _) => Ok(view(amountForm, Some(submission), taxYear, cyaUkDividendAmount))
          case (None, Some(cya)) => Ok(view(amountForm, taxYear = taxYear, preAmount = cya.ukDividendsAmount))
          case _ => Ok(view(form(user.isAgent, taxYear), taxYear = taxYear))
        }
      }

    }
  }

  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, DIVIDENDS)).async { implicit user =>
    dividendsSessionService.getAndHandle(taxYear)(errorHandler.futureInternalServerError()) { (optionalCya, optionalPrior) =>
      form(user.isAgent, taxYear).bindFromRequest().fold(
        {
          formWithErrors => Future.successful(BadRequest(view(
            formWithErrors, priorSubmission = optionalPrior, taxYear = taxYear, preAmount = optionalCya.flatMap(_.ukDividendsAmount)
          )))
        },
        {
          bigDecimal =>
            optionalCya.fold {
              dividendsSessionService.createSessionData(
                DividendsCheckYourAnswersModel().copy(ukDividends = Some(true), ukDividendsAmount = Some(bigDecimal)), taxYear
              )(
                InternalServerError(errorHandler.internalServerErrorTemplate)
              )(
                Redirect(redirectLocation(taxYear, None)(optionalPrior))
              )
            } {
              cyaModel =>
                dividendsSessionService.updateSessionData(cyaModel.copy(ukDividends = Some(true), ukDividendsAmount = Some(bigDecimal)), taxYear)(
                  InternalServerError(errorHandler.internalServerErrorTemplate)
                )(
                  Redirect(redirectLocation(taxYear, Some(cyaModel.copy(ukDividends = Some(true), ukDividendsAmount = Some(bigDecimal))))(optionalPrior))
                )
            }
        }
      )
    }.flatten
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
