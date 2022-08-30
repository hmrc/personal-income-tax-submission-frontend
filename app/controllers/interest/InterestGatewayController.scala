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

package controllers.interest

import config.{AppConfig, ErrorHandler, INTEREST}
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.{AuthorisedAction, QuestionsJourneyValidator}
import forms.YesNoForm
import models.User
import models.interest.InterestCYAModel
import models.question.QuestionsJourney
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.{ExcludeJourneyService, InterestSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.interest.InterestGatewayView
import controllers.predicates.JourneyFilterAction.journeyFilterAction

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class InterestGatewayController @Inject()(
                                           interestSessionService: InterestSessionService,
                                           tailoringGatewayView: InterestGatewayView,
                                           questionsJourneyValidator: QuestionsJourneyValidator,
                                           errorHandler: ErrorHandler,
                                           excludeJourneyService: ExcludeJourneyService
                                         )(
                                           implicit appConfig: AppConfig,
                                           authAction: AuthorisedAction,
                                           ec: ExecutionContext,
                                           mcc: MessagesControllerComponents
                                         ) extends FrontendController(mcc) with I18nSupport with SessionHelper with Logging {

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, INTEREST).async { implicit user =>

    if (appConfig.interestTailoringEnabled) {

      implicit val journey: QuestionsJourney[InterestCYAModel] = InterestCYAModel.interestJourney(taxYear, None)

      interestSessionService.getSessionData(taxYear).map {
        case Left(_) => errorHandler.internalServerError()
        case Right(sessionData) =>
          questionsJourneyValidator.validate(controllers.interest.routes.InterestGatewayController.show(taxYear),
            sessionData.flatMap(_.interest), taxYear) {

            val gatewayCheck: Option[Boolean] = sessionData.flatMap(_.interest.flatMap(_.gateway))

            val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm(
              missingInputError = s"interest.tailorGateway.noRadioSelected.${if (user.isAgent) "agent" else "individual"}"
            )

            gatewayCheck match {
              case Some(checkedValue) => Ok(tailoringGatewayView(yesNoForm.fill(checkedValue), taxYear))
              case None => Ok(tailoringGatewayView(yesNoForm, taxYear))
            }
          }
      }
    } else {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }

  //noinspection ScalaStyle
  def submit(taxYear: Int): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, INTEREST)).async { implicit user =>

    if (appConfig.interestTailoringEnabled) {

      val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm(
        missingInputError = s"interest.tailorGateway.noRadioSelected.${if (user.isAgent) "agent" else "individual"}"
      )
      yesNoForm.bindFromRequest().fold({
        formWithErrors => Future.successful(BadRequest(tailoringGatewayView(formWithErrors, taxYear)))
      }, {
        yesNoValue =>
          interestSessionService.getSessionData(taxYear).flatMap {
            case Left(_) => Future.successful(errorHandler.internalServerError())
            case Right(sessionData) =>

              val interestCya = sessionData.flatMap(_.interest).getOrElse(InterestCYAModel()).copy(gateway = Some(yesNoValue))
              val updated: Boolean = sessionData.nonEmpty

              createOrUpdateInterestData(interestCya, taxYear, updated)(
                if (interestCya.isFinished) {
                  if(!appConfig.interestTailoringEnabled || (appConfig.interestTailoringEnabled && sessionData.isEmpty)) {
                    Redirect(controllers.interest.routes.InterestCYAController.show(taxYear))
                  } else {
                    val untaxedAccountsHasNonZeroData: Boolean = {
                      interestCya.untaxedAccounts.foldLeft(false) { (check, model) =>
                        if(check) {
                          check
                        } else {
                          model.amount.exists(_ != 0)
                        }
                      }
                    }

                    val taxedAccountsHasNonZeroData: Boolean = {
                      interestCya.taxedAccounts.foldLeft(false) { (check, model) =>
                        if (check) {
                          check
                        } else {
                          model.amount.exists(_ != 0)
                        }
                      }
                    }

                    if(!yesNoValue && untaxedAccountsHasNonZeroData && taxedAccountsHasNonZeroData) {
                      Redirect(controllers.routes.ZeroingWarningController.show(taxYear, INTEREST.stringify))
                    } else {
                      Redirect(controllers.interest.routes.InterestCYAController.show(taxYear))
                    }
                  }
                } else {
                  Redirect(controllers.interest.routes.UntaxedInterestController.show(taxYear))
                }
              )
          }
      })

    } else {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }

  def createOrUpdateInterestData(interestCYAModel: InterestCYAModel, taxYear: Int, isUpdate: Boolean)
                                (redirect: Result)(implicit user: User[_]): Future[Result] = {

    if (isUpdate) {
      interestSessionService.updateSessionData(interestCYAModel, taxYear)(errorHandler.internalServerError)(redirect)
    } else {
      interestSessionService.updateSessionData(interestCYAModel, taxYear, true)(errorHandler.internalServerError)(redirect)
    }
  }

}
