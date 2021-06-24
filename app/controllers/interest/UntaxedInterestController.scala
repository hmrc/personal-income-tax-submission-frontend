/*
 * Copyright 2021 HM Revenue & Customs
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
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import services.InterestSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.interest.UntaxedInterestView

import java.util.UUID.randomUUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class UntaxedInterestController @Inject()(
                                           untaxedInterestView: UntaxedInterestView)(
                                           implicit val appConfig: AppConfig,
                                           authAction: AuthorisedAction,
                                           questionHelper: QuestionsJourneyValidator,
                                           interestSessionService: InterestSessionService,
                                           errorHandler: ErrorHandler,
                                           implicit val mcc: MessagesControllerComponents
                                         )
  extends FrontendController(mcc) with I18nSupport with SessionHelper {

  implicit val executionContext: ExecutionContext = mcc.executionContext
  val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm("interest.untaxed-uk-interest.errors.noRadioSelected")

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, INTEREST).async { implicit user =>
    interestSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>
      prior match {
        case Some(prior) if prior.hasUntaxed => Future(Redirect(controllers.interest.routes.InterestCYAController.show(taxYear)))
        case _ =>
          implicit val questionsJourney: QuestionsJourney[InterestCYAModel] = InterestCYAModel.interestJourney(taxYear, None)

          Future(questionHelper.validate(routes.UntaxedInterestController.show(taxYear), cya, taxYear) {
            val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm(
              missingInputError = s"interest.untaxed-uk-interest.errors.noRadioSelected.${if (user.isAgent) "agent" else "individual"}"
            )
            Ok(untaxedInterestView(cya.flatMap(_.untaxedUkInterest).fold(yesNoForm)(yesNoForm.fill), taxYear))
          })
      }

    }
  }

  private[interest] def createOrUpdateSessionData(cyaModel: InterestCYAModel, taxYear: Int, newData: Boolean)
                                                 (block: Result)
                                                 (implicit user: User[_]): Future[Result] = {

    if(newData) {
      interestSessionService.createSessionData(cyaModel, taxYear)(
        errorHandler.internalServerError()
      )(
        block
      )
    } else {
      interestSessionService.updateSessionData(cyaModel, taxYear)(
        errorHandler.internalServerError()
      )(
        block
      )
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authAction.async { implicit user =>

    interestSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>
      prior match {
        case Some(prior) if prior.hasUntaxed => Future(Redirect(controllers.interest.routes.InterestCYAController.show(taxYear)))
        case _ =>
          val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm(
            s"interest.untaxed-uk-interest.errors.noRadioSelected.${if (user.isAgent) "agent" else "individual"}")

          yesNoForm.bindFromRequest().fold(
            {
              formWithErrors => Future(BadRequest(untaxedInterestView(formWithErrors, taxYear)))
            },
            {
              yesNoModel =>
                val baseCya = cya.getOrElse(InterestCYAModel(None, None, None))
                val updatedCya = baseCya.copy(untaxedUkInterest = Some(yesNoModel), accounts = if (yesNoModel) {
                  baseCya.accounts
                } else {
                  baseCya.accounts.map(accounts => accounts.map(interestAccountModel => interestAccountModel.copy(untaxedAmount = None)))
                })

                (yesNoModel, updatedCya.isFinished) match {
                  case (true, false) =>
                    createOrUpdateSessionData(updatedCya, taxYear, cya.isEmpty)(
                      Redirect(controllers.interest.routes.UntaxedInterestAmountController.show(taxYear, id = randomUUID().toString))
                    )
                  case (false, false) =>
                    createOrUpdateSessionData(updatedCya, taxYear, cya.isEmpty)(
                      Redirect(controllers.interest.routes.TaxedInterestController.show(taxYear))
                    )
                  case (_, true) =>
                    interestSessionService.updateSessionData(updatedCya, taxYear)(errorHandler.internalServerError())(
                      Redirect(controllers.interest.routes.InterestCYAController.show(taxYear))
                    )
                }
            }
          )
      }
    }
  }
}
