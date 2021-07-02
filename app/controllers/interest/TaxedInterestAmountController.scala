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

import java.util.UUID
import java.util.UUID.randomUUID

import common.InterestTaxTypes
import common.InterestTaxTypes.TAXED
import config.{AppConfig, ErrorHandler, INTEREST}
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import controllers.predicates.{AuthorisedAction, QuestionsJourneyValidator}
import forms.interest.TaxedInterestAmountForm
import javax.inject.Inject
import models.interest.{InterestAccountModel, InterestCYAModel, TaxedInterestModel, UntaxedInterestModel}
import models.question.QuestionsJourney
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.InterestSessionService
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
                                               errorHandler: ErrorHandler,
                                               implicit val mcc: MessagesControllerComponents,
                                               questionsJourneyValidator: QuestionsJourneyValidator
                                             ) extends FrontendController(mcc) with SessionHelper with I18nSupport with Logging {

  implicit val executionContext: ExecutionContext = mcc.executionContext

  def show(taxYear: Int, id: String): Action[AnyContent] = commonPredicates(taxYear, INTEREST).async { implicit user =>
    interestSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>

      implicit val journey: QuestionsJourney[InterestCYAModel] = InterestCYAModel.interestJourney(taxYear, Some(id))

      val idMatchesPreviouslySubmittedAccount: Boolean = prior.exists(_.submissions.exists(_.exists(_.id.contains(id))))

      def taxedInterestAmountForm: Form[TaxedInterestModel] =
        TaxedInterestAmountForm.taxedInterestAmountForm(user.isAgent, disallowedDuplicateNames(cya,id))

      Future(questionsJourneyValidator.validate(controllers.interest.routes.TaxedInterestAmountController.show(taxYear, id), cya, taxYear) {

        if (idMatchesPreviouslySubmittedAccount) {
          Redirect(controllers.interest.routes.ChangeAccountAmountController.show(taxYear, TAXED, id))
        } else if (sessionIdIsUUID(id)) {

          val account: Option[InterestAccountModel] = cya.flatMap(_.accounts.flatMap(_.find(_.uniqueSessionId.contains(id))))

          val accountName: Option[String] = account.map(_.accountName)
          val accountAmount: Option[BigDecimal] = account.flatMap(_.taxedAmount)

          val model: Option[TaxedInterestModel] = (accountName, accountAmount) match {
            case (Some(name), Some(amount)) => Some(TaxedInterestModel(name, amount))
            case _ => None
          }

          Ok(taxedInterestAmountView(form = model.fold(taxedInterestAmountForm)(taxedInterestAmountForm.fill),
            taxYear, controllers.interest.routes.TaxedInterestAmountController.submit(taxYear, id), isAgent = user.isAgent
          ))
        } else {
          Redirect(controllers.interest.routes.TaxedInterestAmountController.show(taxYear, randomUUID().toString))
        }
      })
    }
  }

  def disallowedDuplicateNames(optionalCyaData: Option[InterestCYAModel], id: String): Seq[String] = {
    optionalCyaData.flatMap(_.accounts.map { accounts =>
      accounts.filter(_.hasTaxed).filterNot(_.getPrimaryId().contains(id))
    }).getOrElse(Seq()).map(_.accountName)
  }

  def submit(taxYear: Int, id: String): Action[AnyContent] = (authorisedAction andThen journeyFilterAction(taxYear, INTEREST)).async { implicit user =>
    interestSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>

      def taxedInterestAmountForm: Form[TaxedInterestModel] = {
        TaxedInterestAmountForm.taxedInterestAmountForm(user.isAgent, disallowedDuplicateNames(cya,id))
      }

      taxedInterestAmountForm.bindFromRequest().fold({
        formWithErrors =>
          Future.successful(BadRequest(taxedInterestAmountView(form = formWithErrors, taxYear = taxYear,
            postAction = controllers.interest.routes.TaxedInterestAmountController.submit(taxYear, id), isAgent = user.isAgent)))
      }, {
        completeForm =>

          val accountsAbleToReuse: Seq[InterestAccountModel] = {
            cya.flatMap(_.accounts.map(_.filter(!_.hasTaxed))).getOrElse(Seq()) ++
            prior.flatMap(_.submissions.map(_.filter(!_.hasTaxed))).getOrElse(Seq())
          }

          cya match {
            case Some(cyaData) =>

              val accounts = cyaData.accounts.getOrElse(Seq.empty[InterestAccountModel])
              val existingAccountWithName: Option[InterestAccountModel] = accountsAbleToReuse.find(_.accountName == completeForm.taxedAccountName)
              val newAccountList = createNewAccountsList(completeForm, existingAccountWithName, accounts, id)
              val updatedCyaModel = cyaData.copy(accounts = Some(newAccountList))

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

  def createNewAccountsList(completeForm: TaxedInterestModel,
                            existingAccountWithName: Option[InterestAccountModel],
                            accounts: Seq[InterestAccountModel],
                            id: String): Seq[InterestAccountModel] = {

    def createNewAccount(overrideId: Option[String] = None): InterestAccountModel = {
      InterestAccountModel(None, completeForm.taxedAccountName, None, Some(completeForm.taxedAmount), Some(overrideId.getOrElse(id)))
    }

    if(existingAccountWithName.isDefined){
      // update existing account
      // remove account with id if empty
      val updatedAccount: InterestAccountModel = existingAccountWithName.get.copy(taxedAmount = Some(completeForm.taxedAmount))
      val existingAccount: Option[InterestAccountModel] = accounts.find(_.getPrimaryId().exists(_ == id)).map(_.copy(taxedAmount = None))
      val existingAccountNeedsRemoving: Boolean = existingAccount.exists(account => !account.hasUntaxed)

      val accountsExcludingImpactedAccounts: Seq[InterestAccountModel]  = {
        accounts.filterNot(account => account.accountName == completeForm.taxedAccountName || account.getPrimaryId().contains(id))
      }

      if(existingAccountNeedsRemoving){
       accountsExcludingImpactedAccounts :+ updatedAccount
      } else {
        accountsExcludingImpactedAccounts ++ Seq(Some(updatedAccount), existingAccount).flatten
      }
    } else {

      val existingAccount: Option[InterestAccountModel] = accounts.find(_.getPrimaryId().exists(_ == id))
      val accountAlreadyExistsWithUntaxedAmountAndNameChanged = existingAccount.exists{
        account => account.hasTaxed && (account.accountName != completeForm.taxedAccountName)
      }

      //if the name has been updated only update the name for the taxed account and keep the existing untaxed account as is
      if(accountAlreadyExistsWithUntaxedAmountAndNameChanged){
        val removedAmountFromExistingAccount: InterestAccountModel = existingAccount.get.copy(taxedAmount = None)
        val newAccount: InterestAccountModel = createNewAccount(Some(UUID.randomUUID().toString))

        accounts.filterNot(_.getPrimaryId().contains(id)) ++ Seq(newAccount, removedAmountFromExistingAccount)
      } else {
        val newAccount = accounts.find(_.getPrimaryId().exists(_ == id)).map(_.copy(
          accountName = completeForm.taxedAccountName, taxedAmount = Some(completeForm.taxedAmount)
        )).getOrElse(createNewAccount())

        if (newAccount.getPrimaryId().nonEmpty && accounts.exists(_.getPrimaryId() == newAccount.getPrimaryId())) {
          accounts.map(account => if (account.getPrimaryId() == newAccount.getPrimaryId()) newAccount else account)
        } else {
          accounts :+ newAccount
        }
      }
    }
  }
}
