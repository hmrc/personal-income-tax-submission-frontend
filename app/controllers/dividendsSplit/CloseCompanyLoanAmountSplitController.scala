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

package controllers.dividendsSplit

import config._
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import forms.AmountForm
import models.dividends.StockDividendsCheckYourAnswersModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.StockDividendsSessionServiceProvider
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.dividends.CloseCompanyLoanAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CloseCompanyLoanAmountSplitController @Inject()(implicit val cc: MessagesControllerComponents,
                                                      authAction: AuthorisedAction,
                                                      view: CloseCompanyLoanAmountView,
                                                      errorHandler: ErrorHandler,
                                                      session: StockDividendsSessionServiceProvider,
                                                      implicit val appConfig: AppConfig,
                                                      ec: ExecutionContext) extends FrontendController(cc) with I18nSupport {

  val journeyKey: JourneyKey = if (appConfig.isJourneyAvailable(STOCK_DIVIDENDS)) STOCK_DIVIDENDS else DIVIDENDS

  def agentOrIndividual(implicit isAgent: Boolean): String = if (isAgent) "agent" else "individual"

  def form(implicit isAgent: Boolean, taxYear: Int): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "dividends.close-company-loan-amount.error.empty." + agentOrIndividual,
    wrongFormatKey = "dividends.close-company-loan-amount.invalidFormat",
    emptyFieldArguments = Seq(taxYear.toString)
  )

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, journeyKey).async { implicit user =>
    session.getSessionData(taxYear).flatMap {
      case Left(_) => Future.successful(errorHandler.internalServerError())
      case Right(sessionData) =>
        if (sessionData.isDefined) {
          val valueCheck: Option[BigDecimal] = sessionData.flatMap(_.stockDividends.flatMap(_.closeCompanyLoansWrittenOffAmount))

          valueCheck match {
            case None => Future.successful(Ok(view(form(user.isAgent, taxYear), taxYear)))
            case Some(value) => Future.successful(Ok(view(form(user.isAgent, taxYear).fill(value), taxYear)))
          }
        } else {
          session.createSessionData(StockDividendsCheckYourAnswersModel(), taxYear)(errorHandler.internalServerError()) {
            Ok(view(form(user.isAgent, taxYear), taxYear))
          }
        }
    }
  }


  def submit(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, journeyKey).async { implicit user =>
    form(user.isAgent, taxYear).bindFromRequest().fold(
      {
        formWithErrors => Future.successful(BadRequest(view(formWithErrors, taxYear)))
      },
      {
        bigDecimal =>
          session.getSessionData(taxYear).flatMap {
            case Left(_) => Future.successful(errorHandler.internalServerError())
            case Right(sessionData) =>
              val dividendsCya = sessionData.flatMap(_.stockDividends).getOrElse(StockDividendsCheckYourAnswersModel())
                .copy(closeCompanyLoansWrittenOffAmount = Some(bigDecimal))
              session.updateSessionData(dividendsCya, taxYear)(errorHandler.internalServerError())(
                Redirect(controllers.dividendsSplit.routes.CheckCloseCompanyLoanAmountController.show(taxYear))
              )
          }
      })
  }
}
