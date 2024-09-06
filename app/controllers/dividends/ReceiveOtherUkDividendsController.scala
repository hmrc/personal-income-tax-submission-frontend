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

import config.{AppConfig, DIVIDENDS, ErrorHandler, JourneyKey, STOCK_DIVIDENDS}
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import controllers.predicates.{AuthorisedAction, QuestionsJourneyValidator}
import forms.YesNoForm
import models.User
import models.dividends.{DividendsCheckYourAnswersModel, StockDividendsCheckYourAnswersModel}
import models.question.QuestionsJourney
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{DividendsSessionService, StockDividendsSessionServiceProvider}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.dividends.ReceiveOtherUkDividendsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReceiveOtherUkDividendsController @Inject()(
                                                   implicit val cc: MessagesControllerComponents,
                                                   authAction: AuthorisedAction,
                                                   receiveOtherDividendsView: ReceiveOtherUkDividendsView,
                                                   questionHelper: QuestionsJourneyValidator,
                                                   dividendsSessionService: DividendsSessionService,
                                                   stockDividendsSessionServiceProvider: StockDividendsSessionServiceProvider,
                                                   errorHandler: ErrorHandler,
                                                   implicit val appConfig: AppConfig,
                                                   ec: ExecutionContext
                                                 ) extends FrontendController(cc) with I18nSupport with SessionHelper {

  val journeyKey: JourneyKey = if (appConfig.isJourneyAvailable(STOCK_DIVIDENDS)) STOCK_DIVIDENDS else DIVIDENDS
  private val isStockDividends = appConfig.isJourneyAvailable(STOCK_DIVIDENDS)

  def yesNoForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(s"dividends.other-dividends.errors.noChoice.${if (isAgent) "agent" else "individual"}")

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, journeyKey).async { implicit user =>
    if (!isStockDividends) {
      dividendsSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>
        Future(
          prior match {
            case Some(prior) if prior.otherUkDividends.nonEmpty => Redirect(controllers.dividends.routes.DividendsCYAController.show(taxYear))
            case _ =>
              implicit val questionsJourney: QuestionsJourney[DividendsCheckYourAnswersModel] = DividendsCheckYourAnswersModel.journey(taxYear)

              questionHelper
                .validate[DividendsCheckYourAnswersModel](controllers.dividends.routes.ReceiveOtherUkDividendsController.show(taxYear), cya, taxYear) {
                  Ok(receiveOtherDividendsView(cya.flatMap(_.otherUkDividends).fold(yesNoForm(user.isAgent))(yesNoForm(user.isAgent).fill), taxYear))
                }
          }
        )
      }
    } else {
      stockDividendsSessionServiceProvider.getAndHandle(taxYear)(errorHandler.futureInternalServerError()) { (cya, prior) =>
        (cya, prior) match {
          case (Some(cyaData), _) => if (cyaData.otherUkDividends.nonEmpty) {
            Future.successful(Ok(
             receiveOtherDividendsView(yesNoForm(user.isAgent).fill(cyaData.otherUkDividends.contains(true)), taxYear)
            ))
          } else {
            Future.successful(Ok(receiveOtherDividendsView(yesNoForm(user.isAgent), taxYear)))
          }
          case (_, Some(priorData)) if priorData.otherUkDividendsAmount.nonEmpty =>
            Future.successful(Redirect(controllers.dividends.routes.DividendsSummaryController.show(taxYear)))
          case _ => Future(Ok(receiveOtherDividendsView(
              cya.flatMap(_.otherUkDividends).fold(yesNoForm(user.isAgent))(yesNoForm(user.isAgent).fill), taxYear
            )))
        }
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, journeyKey)).async { implicit user =>
    yesNoForm(user.isAgent).bindFromRequest().fold(
      {
        formWithErrors =>
          Future.successful(BadRequest(
            receiveOtherDividendsView(formWithErrors, taxYear)
          ))
      }, {
        yesNoModel =>
          if (!isStockDividends) {
            submitDividends(yesNoModel, taxYear)
          } else {
            submitStockDividends(yesNoModel, taxYear)
          }
      }
    )

  }

  private def updateAndRedirect(cyaModel: DividendsCheckYourAnswersModel, taxYear: Int, redirectCall: Call)(implicit user: User[_]): Future[Result] = {
    dividendsSessionService.updateSessionData(cyaModel, taxYear)(
      InternalServerError(errorHandler.internalServerErrorTemplate)
    )(
      Redirect(redirectCall)
    )
  }

  private def submitDividends(yesNoModel: Boolean, taxYear: Int)(implicit user: User[_]): Future[Result] = {
    dividendsSessionService.getSessionData(taxYear).map {
      case Left(_) => Future.successful(errorHandler.internalServerError())
      case Right(cyaData) => cyaData.flatMap(_.dividends)
        .fold {
          Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
        } {
          cyaModel =>
            if (yesNoModel) {
              updateAndRedirect(
                cyaModel.copy(otherUkDividends = Some(true)),
                taxYear,
                controllers.dividendsBase.routes.OtherUkDividendsAmountBaseController.show(taxYear)
              )
            } else {
              updateAndRedirect(
                cyaModel.copy(otherUkDividends = Some(false), otherUkDividendsAmount = None),
                taxYear,
                controllers.dividends.routes.DividendsCYAController.show(taxYear)
              )
            }
        }
    }.flatten
  }

  private def submitStockDividends(yesNoModel: Boolean, taxYear: Int)(implicit user: User[_]): Future[Result] = {
    stockDividendsSessionServiceProvider.getSessionData(taxYear).flatMap {
      case Left(_) => Future.successful(errorHandler.internalServerError())
      case Right(sessionData) =>
        val dividendsCya = if (yesNoModel) {
          sessionData.flatMap(_.stockDividends).getOrElse(StockDividendsCheckYourAnswersModel()).copy(otherUkDividends = Some(yesNoModel))
        } else {
          sessionData.flatMap(_.stockDividends).getOrElse(StockDividendsCheckYourAnswersModel())
            .copy(otherUkDividends = Some(yesNoModel), otherUkDividendsAmount = None)
        }
        val needsCreating = sessionData.forall(_.stockDividends.isEmpty)
        stockDividendsSessionServiceProvider.createOrUpdateSessionData(dividendsCya, taxYear, needsCreating)(errorHandler.internalServerError())(
          if (dividendsCya.isFinished) {
            Redirect(controllers.dividends.routes.DividendsSummaryController.show(taxYear))
          } else {
            if (yesNoModel) {
              Redirect(controllers.dividendsBase.routes.OtherUkDividendsAmountBaseController.show(taxYear))
            } else {
              Redirect(controllers.dividends.routes.StockDividendStatusController.show(taxYear))
            }
          }
        )
    }
  }

}
