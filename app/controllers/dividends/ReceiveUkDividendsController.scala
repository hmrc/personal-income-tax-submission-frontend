/*
 * Copyright 2022 HM Revenue & Customs
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
import forms.YesNoForm
import models.User
import models.dividends.DividendsCheckYourAnswersModel
import models.question.QuestionsJourney
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.DividendsSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.dividends.ReceiveUkDividendsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReceiveUkDividendsController @Inject()(
                                              implicit mcc: MessagesControllerComponents,
                                              authAction: AuthorisedAction,
                                              receiveUkDividendsView: ReceiveUkDividendsView,
                                              questionHelper: QuestionsJourneyValidator,
                                              errorHandler: ErrorHandler,
                                              appConfig: AppConfig,
                                              dividendsSessionService: DividendsSessionService,
                                              ec: ExecutionContext
                                            ) extends FrontendController(mcc) with I18nSupport with SessionHelper {

  def yesNoForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(s"dividends.uk-dividends.errors.noChoice.${if (isAgent) "agent" else "individual"}")

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, DIVIDENDS).async { implicit user =>
    implicit val questionsJourney: QuestionsJourney[DividendsCheckYourAnswersModel] = DividendsCheckYourAnswersModel.journey(taxYear)

    dividendsSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>
      prior match {
        case Some(prior) if prior.ukDividends.nonEmpty => Redirect(controllers.dividends.routes.DividendsCYAController.show(taxYear))
        case _ =>
          questionHelper.validate(routes.ReceiveUkDividendsController.show(taxYear), cya, taxYear) {
            Ok(receiveUkDividendsView(cya.flatMap(_.ukDividends).fold(yesNoForm(user.isAgent))(yesNoForm(user.isAgent).fill), taxYear))
          }
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, DIVIDENDS)).async { implicit user =>
    yesNoForm(user.isAgent).bindFromRequest().fold(
      {
        formWithErrors =>
          Future.successful(BadRequest(
            receiveUkDividendsView(formWithErrors, taxYear)
          ))
      },
      {
        yesNoModel =>
          dividendsSessionService.getSessionData(taxYear).map {
            case Left(_) => Future.successful(errorHandler.internalServerError())
            case Right(cya) =>
            if (yesNoModel) {
              val update = cya.nonEmpty
              val cyaModel = {
                cya.flatMap(_.dividends).getOrElse(DividendsCheckYourAnswersModel()).copy(ukDividends = Some(true))
              }

              if(update) {
                updateAndRedirect(cyaModel, taxYear, routes.UkDividendsAmountController.show(taxYear))
              } else {
                newAndRedirect(cyaModel, taxYear, routes.UkDividendsAmountController.show(taxYear))
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
                  newAndRedirect(DividendsCheckYourAnswersModel(Some(false)), taxYear, routes.ReceiveOtherUkDividendsController.show(taxYear))
              }
            }
      }.flatten
      }
    )
  }

  private def newAndRedirect(cyaModel: DividendsCheckYourAnswersModel, taxYear: Int, redirectCall: Call)(implicit user: User[_]): Future[Result] = {
    dividendsSessionService.createSessionData(cyaModel, taxYear)(
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
