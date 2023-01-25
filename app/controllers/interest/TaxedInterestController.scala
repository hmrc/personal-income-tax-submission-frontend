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

package controllers.interest

import config.{AppConfig, ErrorHandler, INTEREST}
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import controllers.predicates.{AuthorisedAction, QuestionsJourneyValidator}
import forms.YesNoForm
import models.User
import models.interest.InterestCYAModel
import models.question.QuestionsJourney
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.InterestSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.interest.TaxedInterestView

import common.InterestTaxTypes.TAXED
import javax.inject.Inject
import utils.ClearingNewCYAAccountsHelper.clearNewEmptyAccounts

import scala.concurrent.{ExecutionContext, Future}

class TaxedInterestController @Inject()(
                                         taxedInterestView: TaxedInterestView
                                       )(implicit appConfig: AppConfig,
                                         authorisedAction: AuthorisedAction,
                                         interestSessionService: InterestSessionService,
                                         errorHandler: ErrorHandler,
                                         implicit val mcc: MessagesControllerComponents,
                                         questionsJourneyValidator: QuestionsJourneyValidator,
                                         ec: ExecutionContext
                                       ) extends FrontendController(mcc) with SessionHelper with I18nSupport with Logging {

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, INTEREST).async { implicit user: User[AnyContent] =>

    implicit val journey: QuestionsJourney[InterestCYAModel] = InterestCYAModel.interestJourney(taxYear, None)

    interestSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>
      prior match {
        case Some(prior) if prior.hasTaxed => Future(Redirect(controllers.interest.routes.InterestCYAController.show(taxYear)))
        case _ =>
          Future(questionsJourneyValidator.validate(routes.TaxedInterestController.show(taxYear), cya, taxYear) {
            val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm(
              missingInputError = s"interest.taxed-uk-interest.errors.noRadioSelected.${if (user.isAgent) "agent" else "individual"}"
            )
            Ok(taxedInterestView(cya.flatMap(_.taxedUkInterest).fold(yesNoForm)(yesNoForm.fill), taxYear))
          })
      }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = (authorisedAction andThen journeyFilterAction(taxYear, INTEREST)).async { implicit user =>

    interestSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>
      prior match {
        case Some(prior) if prior.hasTaxed => Future(Redirect(controllers.interest.routes.InterestCYAController.show(taxYear)))
        case _ =>
          val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm(
            s"interest.taxed-uk-interest.errors.noRadioSelected.${if (user.isAgent) "agent" else "individual"}")

          cya match {
            case Some(cyaData) =>
              yesNoForm.bindFromRequest().fold(
                {
                  formWithErrors => Future.successful(BadRequest(taxedInterestView(form = formWithErrors, taxYear = taxYear)))
                },
                {
                  yesNoModel =>
                    val updatedCya = cyaData.copy(taxedUkInterest = Some(yesNoModel), accounts = if (yesNoModel) {
                      cyaData.accounts
                    } else {
                      clearNewEmptyAccounts(cyaData, false)
                    })

                    if (yesNoModel) {
                      interestSessionService.updateSessionData(updatedCya, taxYear)(errorHandler.internalServerError())(
                        Redirect(controllers.interest.routes.ChooseAccountController.show(taxYear, TAXED)))
                    } else {
                      interestSessionService.updateSessionData(updatedCya, taxYear)(errorHandler.internalServerError())(
                        Redirect(controllers.interest.routes.InterestCYAController.show(taxYear)))
                    }
                })
            case _ =>
              logger.info("[TaxedInterestController][submit] No CYA data in session. Redirecting to overview page.")
              Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          }
      }
    }
  }
}
