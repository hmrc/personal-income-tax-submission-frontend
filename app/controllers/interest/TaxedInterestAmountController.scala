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
import common.InterestTaxTypes.TAXED
import config.{AppConfig, ErrorHandler, INTEREST}
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import controllers.predicates.{AuthorisedAction, QuestionsJourneyValidator}
import forms.interest.TaxedInterestAmountForm

import javax.inject.Inject
import models.interest.{InterestAccountModel, InterestCYAModel, TaxedInterestModel, AccountAmountModel}
import models.question.QuestionsJourney
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{InterestSessionService, TaxedInterestAmountService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.interest.TaxedInterestAmountView

import scala.concurrent.{ExecutionContext, Future}

class TaxedInterestAmountController @Inject()(
                                               taxedInterestAmountView: TaxedInterestAmountView
                                             )(
                                               implicit appConfig: AppConfig,
                                               authorisedAction: AuthorisedAction,
                                               interestSessionService: InterestSessionService,
                                               taxedInterestAmountService: TaxedInterestAmountService,
                                               errorHandler: ErrorHandler,
                                               implicit val mcc: MessagesControllerComponents,
                                               questionsJourneyValidator: QuestionsJourneyValidator
                                             ) extends FrontendController(mcc) with SessionHelper with I18nSupport with Logging {

  implicit val executionContext: ExecutionContext = mcc.executionContext

  def show(taxYear: Int, id: String): Action[AnyContent] = commonPredicates(taxYear, INTEREST).async { implicit user =>
    interestSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>

      implicit val journey: QuestionsJourney[InterestCYAModel] = InterestCYAModel.interestJourney(taxYear, Some(id))

      val idMatchesPreviouslySubmittedAccount: Boolean = prior.exists(_.submissions.exists(_.id.contains(id)))

      def taxedInterestAmountForm: Form[TaxedInterestModel] =
        TaxedInterestAmountForm.taxedInterestAmountForm(user.isAgent, InterestCYAModel.disallowedDuplicateNames(cya, id, InterestTaxTypes.TAXED))

      Future(questionsJourneyValidator.validate(controllers.interest.routes.TaxedInterestAmountController.show(taxYear, id), cya, taxYear) {

        if (idMatchesPreviouslySubmittedAccount) {
          Redirect(controllers.interest.routes.ChangeAccountAmountController.show(taxYear, TAXED, id))
        } else if (sessionIdIsUUID(id)) {

          val model: Option[TaxedInterestModel] = AccountAmountModel(cya, id, TAXED).map(taxModel => TaxedInterestModel(taxModel.accountName, taxModel.accountAmount))

          Ok(taxedInterestAmountView(form = model.fold(taxedInterestAmountForm)(taxedInterestAmountForm.fill),
            taxYear, controllers.interest.routes.TaxedInterestAmountController.submit(taxYear, id), isAgent = user.isAgent
          ))
        } else {
          Redirect(controllers.interest.routes.TaxedInterestAmountController.show(taxYear, randomUUID().toString))
        }
      })
    }
  }

  def submit(taxYear: Int, id: String): Action[AnyContent] = (authorisedAction andThen journeyFilterAction(taxYear, INTEREST)).async { implicit user =>
    interestSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>

      def taxedInterestAmountForm: Form[TaxedInterestModel] = {
        TaxedInterestAmountForm.taxedInterestAmountForm(user.isAgent, InterestCYAModel.disallowedDuplicateNames(cya, id, TAXED))
      }

      taxedInterestAmountForm.bindFromRequest().fold({
        formWithErrors =>
          Future.successful(BadRequest(taxedInterestAmountView(form = formWithErrors, taxYear = taxYear,
            postAction = controllers.interest.routes.TaxedInterestAmountController.submit(taxYear, id), isAgent = user.isAgent)))
      }, {
        completeForm =>

          val accountsAbleToReuse: Seq[InterestAccountModel] = {
            cya.map(_.accounts.filter(!_.hasTaxed)).getOrElse(Seq()) ++
            prior.map(_.submissions.filter(!_.hasTaxed)).getOrElse(Seq())
          }

          cya match {
            case Some(cyaData) =>

              val existingAccountWithName: Option[InterestAccountModel] = accountsAbleToReuse.find(_.accountName == completeForm.taxedAccountName)
              val newAccountList = taxedInterestAmountService.createNewAccountsList(completeForm, existingAccountWithName, cyaData.accounts, id)
              val updatedCyaModel = cyaData.copy(accounts = newAccountList)

              interestSessionService.updateSessionData(updatedCyaModel, taxYear)(errorHandler.internalServerError())(
                Redirect(controllers.interest.routes.AccountsController.show(taxYear, InterestTaxTypes.TAXED))
              )
            case _ =>
              logger.info("[TaxedInterestController][submit] No CYA data in session. Redirecting to overview page.")
              Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          }
      })
    }
  }
}
