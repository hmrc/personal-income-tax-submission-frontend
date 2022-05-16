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
import controllers.predicates.{AuthorisedAction, QuestionsJourneyValidator}
import forms.YesNoForm
import models.User
import models.dividends.DividendsCheckYourAnswersModel
import models.question.QuestionsJourney
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.DividendsSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.dividends.DividendsGatewayView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DividendsGatewayController @Inject()(
                                            view: DividendsGatewayView,
                                            session: DividendsSessionService,
                                            errorHandler: ErrorHandler,
                                            questionsJourneyValidator: QuestionsJourneyValidator
                                          )
                                          (
                                            implicit appConfig: AppConfig,
                                            mcc: MessagesControllerComponents,
                                            ec: ExecutionContext,
                                            authorisedAction: AuthorisedAction
                                          ) extends FrontendController(mcc) with I18nSupport {

  private[dividends] val form: Boolean => Form[Boolean] = isAgent => YesNoForm.yesNoForm("dividends.gateway.error." + (if (isAgent) "agent" else "individual"))

  private def internalError(implicit user: User[_]): Result = errorHandler.internalServerError()

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, DIVIDENDS).async { implicit user =>
    if(appConfig.tailoringEnabled) {
      implicit val questionsJourney: QuestionsJourney[DividendsCheckYourAnswersModel] = DividendsCheckYourAnswersModel.journey(taxYear)

      session.getSessionData(taxYear).map {
        case Left(_) => errorHandler.internalServerError()
        case Right(sessionData) =>

          questionsJourneyValidator.validate(routes.DividendsGatewayController.show(taxYear), sessionData.flatMap(_.dividends), taxYear) {
            val gatewayCheck: Option[Boolean] = sessionData.flatMap(_.dividends.flatMap(_.gateway))

            gatewayCheck match {
              case None => Ok(view(form(user.isAgent), taxYear))
              case Some(checkValue) => Ok(view(form(user.isAgent).fill(checkValue), taxYear))
            }
          }
      }
    } else {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }

  def createOrUpdateDividendsData(
                                   dividendsCyaModel: DividendsCheckYourAnswersModel,
                                   taxYear: Int,
                                   isUpdate: Boolean
                                 )(redirect: Result)(implicit user: User[_]): Future[Result] = {
    if (isUpdate) {
      session.updateSessionData(dividendsCyaModel, taxYear)(internalError)(redirect)
    } else {
      session.createSessionData(dividendsCyaModel, taxYear)(internalError)(redirect)
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, DIVIDENDS).async { implicit user =>
    if(appConfig.tailoringEnabled) {
      form(user.isAgent).bindFromRequest().fold(formWithErrors => {
        Future.successful(BadRequest(view(formWithErrors, taxYear)))
      }, {
        yesNoValue =>

          session.getSessionData(taxYear).flatMap {
            case Left(_) => Future.successful(internalError)
            case Right(sessionData) =>
              val update = sessionData.nonEmpty
              val dividendsCya = sessionData.flatMap(_.dividends).getOrElse(DividendsCheckYourAnswersModel()).copy(gateway = Some(yesNoValue))

              if (yesNoValue) {
                createOrUpdateDividendsData(dividendsCya, taxYear, update)(
                  if(dividendsCya.isFinished) {
                    Redirect(controllers.dividends.routes.DividendsCYAController.show(taxYear))
                  } else {
                    Redirect(controllers.dividends.routes.ReceiveUkDividendsController.show(taxYear))
                  }
                )
              } else {
                Future.successful(Redirect(controllers.dividends.routes.DividendsCYAController.show(taxYear))) //TODO Redirect to the 0ing warning page when ready
              }
          }

      })
    } else {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }

  }
}
