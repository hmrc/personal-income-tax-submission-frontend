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
import views.html.dividends.ReceiveOtherUkDividendsView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ReceiveOtherUkDividendsController @Inject()(
                                                   implicit val cc: MessagesControllerComponents,
                                                   authAction: AuthorisedAction,
                                                   receiveOtherDividendsView: ReceiveOtherUkDividendsView,
                                                   questionHelper: QuestionsJourneyValidator,
                                                   dividendsSessionService: DividendsSessionService,
                                                   errorHandler: ErrorHandler,
                                                   implicit val appConfig: AppConfig,
                                                   ec: ExecutionContext
                                                 ) extends FrontendController(cc) with I18nSupport with SessionHelper {

  def yesNoForm(isAgent: Boolean): Form[Boolean] = YesNoForm.yesNoForm(s"dividends.other-dividends.errors.noChoice.${if (isAgent) "agent" else "individual"}")

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, DIVIDENDS).async { implicit user =>
    dividendsSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>
      Future(
      prior match {
        case Some(prior) if prior.otherUkDividends.nonEmpty => Redirect(controllers.dividends.routes.DividendsCYAController.show(taxYear))
        case _ =>
          implicit val questionsJourney: QuestionsJourney[DividendsCheckYourAnswersModel] = DividendsCheckYourAnswersModel.journey(taxYear)

          questionHelper.validate[DividendsCheckYourAnswersModel](controllers.dividends.routes.ReceiveOtherUkDividendsController.show(taxYear), cya, taxYear) {
            Ok(receiveOtherDividendsView(cya.flatMap(_.otherUkDividends).fold(yesNoForm(user.isAgent))(yesNoForm(user.isAgent).fill), taxYear))
          }
      }
      )
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, DIVIDENDS)).async { implicit user =>
      yesNoForm(user.isAgent).bindFromRequest().fold(
        {
          formWithErrors =>
            Future.successful(BadRequest(
              receiveOtherDividendsView(formWithErrors, taxYear)
            ))
        }, {
          yesNoModel =>
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
                      controllers.dividends.routes.OtherUkDividendsAmountController.show(taxYear)
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
      )

  }


  private def updateAndRedirect(cyaModel: DividendsCheckYourAnswersModel, taxYear: Int, redirectCall: Call)(implicit user: User[_]): Future[Result] = {
    dividendsSessionService.updateSessionData(cyaModel, taxYear)(
      InternalServerError(errorHandler.internalServerErrorTemplate)
    )(
      Redirect(redirectCall)
    )
  }

}
