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

import common.SessionValues
import config.{AppConfig, INTEREST}
import controllers.interest.routes.TaxedInterestController
import controllers.predicates.{AuthorisedAction, QuestionsJourneyValidator}
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import forms.YesNoForm
import models.User
import models.interest.{InterestCYAModel, InterestPriorSubmission}
import models.question.QuestionsJourney
import play.api.Logger
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.InterestSessionHelper
import views.html.interest.TaxedInterestView

import java.util.UUID.randomUUID
import javax.inject.Inject
import scala.concurrent.ExecutionContext


class TaxedInterestController @Inject()(
                                         taxedInterestView: TaxedInterestView
                                       )(implicit appConfig: AppConfig,
                                         authorisedAction: AuthorisedAction,
                                         implicit val mcc: MessagesControllerComponents,
                                         questionsJourneyValidator: QuestionsJourneyValidator
                                        ) extends FrontendController(mcc) with InterestSessionHelper with I18nSupport {

  implicit val executionContext: ExecutionContext = mcc.executionContext

  def show(taxYear: Int): Action[AnyContent] = commonPredicates(taxYear, INTEREST).apply { implicit user: User[AnyContent] =>

    implicit val journey: QuestionsJourney[InterestCYAModel] = InterestCYAModel.interestJourney(taxYear, None)

    InterestPriorSubmission.fromSession() match {
      case Some(prior) if prior.hasTaxed => Redirect(controllers.interest.routes.InterestCYAController.show(taxYear))
      case _ =>
        val cyaData: Option[InterestCYAModel] = getModelFromSession[InterestCYAModel](SessionValues.INTEREST_CYA)
        questionsJourneyValidator.validate(TaxedInterestController.show(taxYear), cyaData, taxYear) {
          val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm(s"interest.taxed-uk-interest.errors.noRadioSelected.${if(user.isAgent) "agent" else "individual"}")
          Ok(taxedInterestView(cyaData.flatMap(_.taxedUkInterest).fold(yesNoForm)(yesNoForm.fill), taxYear))
        }
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = (authorisedAction andThen journeyFilterAction(taxYear, INTEREST)) { implicit user =>
    val optionalCyaData: Option[InterestCYAModel] = getModelFromSession[InterestCYAModel](SessionValues.INTEREST_CYA)
    val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm(s"interest.taxed-uk-interest.errors.noRadioSelected.${if(user.isAgent) "agent" else "individual"}")

    yesNoForm.bindFromRequest().fold(
      {
        formWithErrors =>
          BadRequest(taxedInterestView(
            form = formWithErrors,
            taxYear = taxYear
          ))
      },
      {
        yesNoModel =>
          optionalCyaData match {
            case Some(cyaData) =>
              val updatedCya = cyaData.copy(taxedUkInterest = Some(yesNoModel), taxedUkAccounts = if (yesNoModel) {
                cyaData.taxedUkAccounts
              } else {
                None
              })

              if (yesNoModel) {
                Redirect(controllers.interest.routes.TaxedInterestAmountController.show(taxYear, id = randomUUID().toString))
                  .addingToSession(SessionValues.INTEREST_CYA -> updatedCya.asJsonString)
              } else {
                Redirect(controllers.interest.routes.InterestCYAController.show(taxYear))
                  .addingToSession(SessionValues.INTEREST_CYA -> updatedCya.asJsonString)
              }
            case _ =>
              Logger.logger.info("[TaxedInterestController][submit] No CYA data in session. Redirecting to overview page.")
              Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
          }
      }
    )
  }

}
