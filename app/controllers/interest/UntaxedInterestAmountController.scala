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

import java.util.UUID.randomUUID
import common.InterestTaxTypes
import common.InterestTaxTypes.UNTAXED
import config.{AppConfig, ErrorHandler, INTEREST}
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import controllers.predicates.{AuthorisedAction, QuestionsJourneyValidator}
import forms.interest.UntaxedInterestAmountForm

import javax.inject.Inject
import models.interest.{InterestAccountModel, InterestCYAModel, AccountAmountModel, UntaxedInterestModel}
import models.question.QuestionsJourney
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.{InterestSessionService, UntaxedInterestAmountService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.interest.UntaxedInterestAmountView

import scala.concurrent.{ExecutionContext, Future}

class UntaxedInterestAmountController @Inject()(
                                                 implicit val mcc: MessagesControllerComponents,
                                                 authAction: AuthorisedAction,
                                                 untaxedInterestAmountView: UntaxedInterestAmountView,
                                                 interestSessionService: InterestSessionService,
                                                 untaxedInterestAmountService: UntaxedInterestAmountService,
                                                 errorHandler: ErrorHandler,
                                                 implicit val appConfig: AppConfig,
                                                 questionsJourneyValidator: QuestionsJourneyValidator,
                                                 ec: ExecutionContext
                                               ) extends FrontendController(mcc) with I18nSupport with SessionHelper with Logging {

  def show(taxYear: Int, id: String): Action[AnyContent] = commonPredicates(taxYear, INTEREST).async { implicit user =>
    interestSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>

      implicit val journey: QuestionsJourney[InterestCYAModel] = InterestCYAModel.interestJourney(taxYear, Some(id))

      val idMatchesPreviouslySubmittedAccount: Boolean = prior.exists(_.submissions.exists(_.id.contains(id)))

      def untaxedInterestAmountForm: Form[UntaxedInterestModel] =
        UntaxedInterestAmountForm.untaxedInterestAmountForm(user.isAgent, InterestCYAModel.disallowedDuplicateNames(cya, id, UNTAXED))

      Future(questionsJourneyValidator.validate(routes.UntaxedInterestAmountController.show(taxYear, id), cya, taxYear) {

        if (idMatchesPreviouslySubmittedAccount) {
          Redirect(controllers.interest.routes.ChangeAccountAmountController.show(taxYear, UNTAXED, id))
        } else if (sessionIdIsUUID(id)) {

          val model: Option[UntaxedInterestModel] = AccountAmountModel(cya, id, UNTAXED).map(taxModel => UntaxedInterestModel(taxModel.accountName, taxModel.accountAmount))

          Ok(untaxedInterestAmountView(form = model.fold(untaxedInterestAmountForm)(untaxedInterestAmountForm.fill),
            taxYear = taxYear, postAction = routes.UntaxedInterestAmountController.submit(taxYear, id), isAgent = user.isAgent
          ))
        } else {
          Redirect(routes.UntaxedInterestAmountController.show(taxYear, randomUUID().toString))
        }
      })
    }
  }

  def submit(taxYear: Int, id: String): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, INTEREST)).async { implicit user =>

    interestSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>

      def untaxedInterestAmountForm: Form[UntaxedInterestModel] = {
        UntaxedInterestAmountForm.untaxedInterestAmountForm(user.isAgent, InterestCYAModel.disallowedDuplicateNames(cya, id, UNTAXED))
      }

      untaxedInterestAmountForm.bindFromRequest().fold({
        formWithErrors =>
          Future.successful(BadRequest(untaxedInterestAmountView(
            form = formWithErrors,
            taxYear = taxYear,
            postAction = routes.UntaxedInterestAmountController.submit(taxYear, id),
            isAgent = user.isAgent
          )))
      }, {
        completeForm =>

          val untaxedAccounts = cya.map(_.untaxedAccounts).getOrElse(Seq())
          val taxedAccounts = cya.map(_.taxedAccounts).getOrElse(Seq())

          val accountsAbleToReuse: Seq[InterestAccountModel] = {
            (untaxedAccounts ++ taxedAccounts) ++
            prior.map(_.submissions.filter(!_.hasUntaxed)).getOrElse(Seq()).map(account => InterestAccountModel(account.id, account.accountName, account.taxedAmount, account.uniqueSessionId))
          }

          cya match {
            case Some(cyaData) =>
              val existingAccountWithName: Option[InterestAccountModel] = accountsAbleToReuse.find(_.accountName == completeForm.untaxedAccountName)
              val newAccountList = untaxedInterestAmountService.createNewAccountsList(completeForm, existingAccountWithName, cyaData.untaxedAccounts, id)
              val updatedCyaModel = cyaData.copy(untaxedAccounts = newAccountList)

              interestSessionService.updateSessionData(updatedCyaModel, taxYear)(InternalServerError(errorHandler.internalServerErrorTemplate))(
                Redirect(controllers.interest.routes.AccountsController.show(taxYear, InterestTaxTypes.UNTAXED))
              )
            case _ =>
              logger.info("[UntaxedInterestController][submit] No CYA data in session. Redirecting to overview page.")
              Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          }
      })
    }
  }
}
