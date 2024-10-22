/*
 * Copyright 2024 HM Revenue & Customs
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
import models.User
import models.dividends.StockDividendsCheckYourAnswersModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import play.twirl.api.Html
import services.StockDividendsSessionServiceProvider
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.dividends.UkDividendsAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class UkDividendsAmountSplitController @Inject()(implicit val cc: MessagesControllerComponents,
                                                 authAction: AuthorisedAction,
                                                 dividendsAmountView: UkDividendsAmountView,
                                                 errorHandler: ErrorHandler,
                                                 session: StockDividendsSessionServiceProvider,
                                                 implicit val appConfig: AppConfig,
                                                 ec: ExecutionContext) extends FrontendController(cc) with I18nSupport {

  val journeyKey: JourneyFeatureSwitchKeys = if (appConfig.isJourneyAvailable(STOCK_DIVIDENDS)) STOCK_DIVIDENDS else DIVIDENDS

  def agentOrIndividual(implicit isAgent: Boolean): String = if (isAgent) "agent" else "individual"

  def form(implicit isAgent: Boolean, taxYear: Int): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "dividends.dividends-amount.error.empty." + agentOrIndividual,
    wrongFormatKey = "dividends.common.error.invalidFormat." + agentOrIndividual,
    exceedsMaxAmountKey = "dividends.dividends-amount.error.amountMaxLimit." + agentOrIndividual,
    emptyFieldArguments = Seq(taxYear.toString)
  )

  def view(formInput: Form[BigDecimal], taxYear: Int, preAmount: Option[BigDecimal] = None)(implicit user: User[AnyContent]): Html = {
    dividendsAmountView(
      form = formInput,
      taxYear = taxYear,
      postAction = controllers.dividendsBase.routes.UkDividendsAmountBaseController.submit(taxYear),
      preAmount = preAmount
    )
  }

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, journeyKey).async { implicit user =>
    session.getSessionData(taxYear).flatMap {
      case Left(_) => Future.successful(errorHandler.internalServerError())
      case Right(sessionData) =>
        if (sessionData.isDefined) {
          val optionalAmount: Option[BigDecimal] = sessionData.flatMap(_.stockDividends.flatMap(_.ukDividendsAmount))

          optionalAmount match {
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
                .copy(ukDividendsAmount = Some(bigDecimal))
              session.updateSessionData(dividendsCya, taxYear)(errorHandler.internalServerError())(
                Redirect(controllers.dividendsSplit.routes.CheckUkDividendsAmountController.show(taxYear))
              )
          }
      }
    )
  }
}