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

import config._
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import controllers.predicates.{AuthorisedAction, QuestionsJourneyValidator}
import forms.AmountForm
import models.User
import models.dividends.{DividendsCheckYourAnswersModel, DividendsPriorSubmission, StockDividendsCheckYourAnswersModel}
import models.question.QuestionsJourney
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import services.{DividendsSessionService, StockDividendsSessionServiceProvider}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.dividends.UkDividendsAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UkDividendsAmountController @Inject()(implicit val cc: MessagesControllerComponents,
                                            authAction: AuthorisedAction,
                                            ukDividendsAmountView: UkDividendsAmountView,
                                            questionHelper: QuestionsJourneyValidator,
                                            dividendsSessionService: DividendsSessionService,
                                            stockDividendsSessionService: StockDividendsSessionServiceProvider,
                                            errorHandler: ErrorHandler,
                                            implicit val appConfig: AppConfig,
                                            ec: ExecutionContext) extends FrontendController(cc) with I18nSupport with SessionHelper {

  private val journeyKey: JourneyKey = if (appConfig.isJourneyAvailable(STOCK_DIVIDENDS)) STOCK_DIVIDENDS else DIVIDENDS
  private val isStockDividends: Boolean = appConfig.isJourneyAvailable(STOCK_DIVIDENDS)

  def agentOrIndividual(implicit isAgent: Boolean): String = if (isAgent) "agent" else "individual"

  def form(implicit isAgent: Boolean, taxYear: Int): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "dividends.uk-dividends-amount.error.empty." + agentOrIndividual,
    wrongFormatKey = "dividends.common.error.invalidFormat." + agentOrIndividual,
    exceedsMaxAmountKey = "dividends.uk-dividends-amount.error.amountMaxLimit",
    emptyFieldArguments = Seq(taxYear.toString)
  )

  def view(
            formInput: Form[BigDecimal],
            taxYear: Int,
            preAmount: Option[BigDecimal] = None
          )(implicit user: User[AnyContent]): Html = {

    ukDividendsAmountView(
      form = preAmount.fold(formInput)(formInput.fill),
      taxYear = taxYear,
      postAction = controllers.dividends.routes.UkDividendsAmountController.submit(taxYear),
      preAmount = preAmount
    )

  }

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, journeyKey).async { implicit user =>
    if (!isStockDividends) {
      implicit val questionsJourney: QuestionsJourney[DividendsCheckYourAnswersModel] = DividendsCheckYourAnswersModel.journey(taxYear)

      dividendsSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (optionalCya, _) =>
        Future {
          questionHelper.validate(controllers.dividends.routes.UkDividendsAmountController.show(taxYear), optionalCya, taxYear) {
            val cyaUkDividendAmount: Option[BigDecimal] = optionalCya.flatMap(_.ukDividendsAmount)

            val amountForm = cyaUkDividendAmount match {
              case Some(cyaAmount) => form(user.isAgent, taxYear).fill(cyaAmount)
              case _ => form(user.isAgent, taxYear)
            }

            optionalCya match {
              case Some(cya) => Ok(view(amountForm, taxYear = taxYear, preAmount = cya.ukDividendsAmount))
              case _ => Ok(view(form(user.isAgent, taxYear), taxYear = taxYear))
            }
          }
        }
      }
    } else {
      showStockDividends(taxYear, form(user.isAgent, taxYear))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, journeyKey)).async { implicit user =>

    dividendsSessionService.getAndHandle(taxYear)(errorHandler.futureInternalServerError()) { (optionalCya, optionalPrior) =>
      Future(form(user.isAgent, taxYear).bindFromRequest().fold(
        {
          formWithErrors =>
            Future.successful(BadRequest(view(
              formWithErrors, taxYear = taxYear, preAmount = optionalCya.flatMap(_.ukDividendsAmount)
            )))
        },
        {
          bigDecimal =>
            if (!isStockDividends) {
              optionalCya.fold {
                Future(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
              } {
                cyaModel =>
                  dividendsSessionService.updateSessionData(cyaModel.copy(ukDividends = Some(true), ukDividendsAmount = Some(bigDecimal)), taxYear)(
                    InternalServerError(errorHandler.internalServerErrorTemplate)
                  )(
                    Redirect(redirectLocation(taxYear, Some(cyaModel.copy(ukDividends = Some(true), ukDividendsAmount = Some(bigDecimal))))(optionalPrior))
                  )
              }
            } else {
              submitStockDividends(taxYear, bigDecimal)
            }
        }
      ))
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

  private def showStockDividends(taxYear: Int, form: Form[BigDecimal])(implicit user: User[AnyContent]): Future[Result] = {
    stockDividendsSessionService.getSessionData(taxYear).map {
      case Left(_) => errorHandler.internalServerError()
      case Right(sessionData) =>
        val valueCheck: Option[BigDecimal] = sessionData.flatMap(_.stockDividends.flatMap(_.ukDividendsAmount))

        valueCheck match {
          case Some(value) => Ok(view(form, taxYear = taxYear, preAmount = Some(value)))
          case _ => Ok(view(form, taxYear = taxYear))
        }
    }
  }

  private def submitStockDividends(taxYear: Int, bigDecimal: BigDecimal)(implicit user: User[AnyContent]) = {
    stockDividendsSessionService.getSessionData(taxYear).flatMap {
      case Left(_) => Future.successful(errorHandler.internalServerError())
      case Right(sessionData) =>
        val dividendsCya = sessionData.flatMap(_.stockDividends).getOrElse(StockDividendsCheckYourAnswersModel())
          .copy(ukDividendsAmount = Some(bigDecimal))
        stockDividendsSessionService.updateSessionData(dividendsCya, taxYear)(errorHandler.internalServerError())(
          if (dividendsCya.isFinished) {
            Redirect(controllers.dividends.routes.DividendsSummaryController.show(taxYear))
          }
          else {
            Redirect(controllers.dividends.routes.ReceiveOtherUkDividendsController.show(taxYear))
          }
        )
    }
  }

}
