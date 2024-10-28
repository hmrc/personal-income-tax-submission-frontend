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
import views.html.dividends.ReceiveUkDividendsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReceiveUkDividendsController @Inject()(implicit mcc: MessagesControllerComponents,
                                             authAction: AuthorisedAction,
                                             receiveUkDividendsView: ReceiveUkDividendsView,
                                             questionHelper: QuestionsJourneyValidator,
                                             errorHandler: ErrorHandler,
                                             appConfig: AppConfig,
                                             dividendsSessionService: DividendsSessionService,
                                             stockDividendsSessionServiceProvider: StockDividendsSessionServiceProvider,
                                             ec: ExecutionContext) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  private val journeyKey: JourneyFeatureSwitchKeys = if (appConfig.isJourneyAvailable(STOCK_DIVIDENDS)) STOCK_DIVIDENDS else DIVIDENDS
  private val isStockDividends: Boolean = appConfig.isJourneyAvailable(STOCK_DIVIDENDS)

  def yesNoForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(s"dividends.uk-dividends.errors.noChoice.${if (isAgent) "agent" else "individual"}")

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, journeyKey).async { implicit user =>
    if (!isStockDividends) {
      implicit val questionsJourney: QuestionsJourney[DividendsCheckYourAnswersModel] = DividendsCheckYourAnswersModel.journey(taxYear)

      dividendsSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>
        Future(prior match {
          case Some(prior) if prior.ukDividends.nonEmpty => Redirect(controllers.dividends.routes.DividendsCYAController.show(taxYear))
          case _ =>
            questionHelper.validate(routes.ReceiveUkDividendsController.show(taxYear), cya, taxYear) {
              Ok(receiveUkDividendsView(cya.flatMap(_.ukDividends).fold(yesNoForm(user.isAgent))(yesNoForm(user.isAgent).fill), taxYear))
            }
        })
      }
    } else {
      stockDividendsSessionServiceProvider.getAndHandle(taxYear)(errorHandler.futureInternalServerError()) { (cya, prior) =>
        (cya, prior) match {
          case (Some(cyaData), _) => if (cyaData.ukDividends.nonEmpty) {
            Future.successful(Ok(
              receiveUkDividendsView(yesNoForm(user.isAgent).fill(cyaData.ukDividends.contains(true)), taxYear)
            ))
          } else {
            Future.successful(Ok(receiveUkDividendsView(yesNoForm(user.isAgent), taxYear)))
          }
          case (_, Some(priorData)) if priorData.ukDividendsAmount.nonEmpty =>
            Future.successful(Redirect(controllers.dividends.routes.DividendsSummaryController.show(taxYear)))
          case _ => Future(Ok(receiveUkDividendsView(
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
            receiveUkDividendsView(formWithErrors, taxYear)
          ))
      },
      {
        yesNoModel =>
          if (!isStockDividends) {
            submitDividends(yesNoModel, taxYear)
          } else {
            submitStockDividends(yesNoModel, taxYear)
          }
      }
    )
  }

  private def submitDividends(yesNoModel: Boolean, taxYear: Int)(implicit user: User[_]): Future[Result] = {
    dividendsSessionService.getSessionData(taxYear).map {
      case Left(_) => Future.successful(errorHandler.internalServerError())
      case Right(cya) =>
        val gateway = if (appConfig.dividendsTailoringEnabled) None else Some(true)

        if (yesNoModel) {
          val update = cya.nonEmpty
          val cyaModel = {
            cya.flatMap(_.dividends).getOrElse(DividendsCheckYourAnswersModel(gateway = gateway)).copy(ukDividends = Some(true))
          }

          if (update) {
            updateAndRedirect(cyaModel, taxYear, controllers.dividendsBase.routes.UkDividendsAmountBaseController.show(taxYear))
          } else {
            newAndRedirect(cyaModel, taxYear, controllers.dividendsBase.routes.UkDividendsAmountBaseController.show(taxYear))
          }
        } else {
          cya.flatMap(_.dividends) match {
            case Some(model) if model.isFinished =>
              updateAndRedirect(model.copy(ukDividends = Some(false), ukDividendsAmount = None), taxYear, routes.DividendsCYAController.show(taxYear))
            case Some(model) =>
              updateAndRedirect(
                model.copy(ukDividends = Some(false), ukDividendsAmount = None), taxYear, routes.ReceiveOtherUkDividendsController.show(taxYear)
              )
            case _ =>
              newAndRedirect(DividendsCheckYourAnswersModel(gateway, Some(false)), taxYear, routes.ReceiveOtherUkDividendsController.show(taxYear))
          }
        }
    }.flatten
  }

  private def submitStockDividends(yesNoModel: Boolean, taxYear: Int)(implicit user: User[_]): Future[Result] = {
    stockDividendsSessionServiceProvider.getSessionData(taxYear).flatMap {
      case Left(_) => Future.successful(errorHandler.internalServerError())
      case Right(sessionData) =>
        val dividendsCya = if (yesNoModel) {
          sessionData.flatMap(_.stockDividends).getOrElse(StockDividendsCheckYourAnswersModel()).copy(gateway = Some(true), ukDividends = Some(yesNoModel))
        } else {
          sessionData.flatMap(_.stockDividends).getOrElse(StockDividendsCheckYourAnswersModel())
            .copy(gateway = Some(true), ukDividends = Some(yesNoModel), ukDividendsAmount = None)
        }
        val needsCreating = sessionData.forall(_.stockDividends.isEmpty)
        stockDividendsSessionServiceProvider.createOrUpdateSessionData(dividendsCya, taxYear, needsCreating)(errorHandler.internalServerError())(
          if (dividendsCya.isFinished) {
            Redirect(controllers.dividends.routes.DividendsSummaryController.show(taxYear))
          } else {
            if (yesNoModel) {
              Redirect(controllers.dividendsBase.routes.UkDividendsAmountBaseController.show(taxYear))
            } else {
              Redirect(controllers.dividends.routes.ReceiveOtherUkDividendsController.show(taxYear))
            }
          }
        )
    }
  }

  private def newAndRedirect(cyaModel: DividendsCheckYourAnswersModel, taxYear: Int, redirectCall: Call)(implicit user: User[_]): Future[Result] = {
    dividendsSessionService.updateSessionData(cyaModel, taxYear, true)(
      InternalServerError(errorHandler.internalServerErrorTemplate)
    )(
      Redirect(redirectCall)
    )
  }

  private def updateAndRedirect(cyaModel: DividendsCheckYourAnswersModel, taxYear: Int, redirectCall: Call)(implicit user: User[_]): Future[Result] = {
    dividendsSessionService.updateSessionData(cyaModel, taxYear)({
      InternalServerError(errorHandler.internalServerErrorTemplate)
    })(
      Redirect(redirectCall)
    )
  }

}
