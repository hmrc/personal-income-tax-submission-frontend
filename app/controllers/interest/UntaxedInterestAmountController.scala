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

import java.util.UUID
import java.util.UUID.randomUUID

import common.InterestTaxTypes
import common.InterestTaxTypes.UNTAXED
import config.{AppConfig, ErrorHandler, INTEREST}
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import controllers.predicates.{AuthorisedAction, QuestionsJourneyValidator}
import forms.interest.UntaxedInterestAmountForm
import javax.inject.Inject
import models.interest.{InterestAccountModel, InterestCYAModel, UntaxedInterestModel}
import models.question.QuestionsJourney
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import services.InterestSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.interest.UntaxedInterestAmountView

import scala.concurrent.{ExecutionContext, Future}

class UntaxedInterestAmountController @Inject()(
                                                 implicit val mcc: MessagesControllerComponents,
                                                 authAction: AuthorisedAction,
                                                 untaxedInterestAmountView: UntaxedInterestAmountView,
                                                 interestSessionService: InterestSessionService,
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
        UntaxedInterestAmountForm.untaxedInterestAmountForm(user.isAgent, disallowedDuplicateNames(cya,id))

      Future(questionsJourneyValidator.validate(routes.UntaxedInterestAmountController.show(taxYear, id), cya, taxYear) {

        if (idMatchesPreviouslySubmittedAccount) {
          Redirect(controllers.interest.routes.ChangeAccountAmountController.show(taxYear, UNTAXED, id))
        } else if (sessionIdIsUUID(id)) {

          val account: Option[InterestAccountModel] = cya.flatMap(_.accounts.flatMap(_.find(_.uniqueSessionId.contains(id))))

          val accountName: Option[String] = account.map(_.accountName)
          val accountAmount: Option[BigDecimal] = account.flatMap(_.untaxedAmount)

          val model: Option[UntaxedInterestModel] = (accountName, accountAmount) match {
            case (Some(name), Some(amount)) => Some(UntaxedInterestModel(name, amount))
            case _ => None
          }

          Ok(untaxedInterestAmountView(form = model.fold(untaxedInterestAmountForm)(untaxedInterestAmountForm.fill),
            taxYear = taxYear, postAction = routes.UntaxedInterestAmountController.submit(taxYear, id), isAgent = user.isAgent
          ))
        } else {
          Redirect(routes.UntaxedInterestAmountController.show(taxYear, randomUUID().toString))
        }
      })
    }
  }

  def disallowedDuplicateNames(optionalCyaData: Option[InterestCYAModel], id: String): Seq[String] = {
    optionalCyaData.flatMap(_.accounts.map { accounts =>
      accounts.filter(_.hasUntaxed).filterNot(_.getPrimaryId().contains(id))
    }).getOrElse(Seq()).map(_.accountName)
  }

  def submit(taxYear: Int, id: String): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, INTEREST)).async { implicit user =>

    interestSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>

      def untaxedInterestAmountForm: Form[UntaxedInterestModel] = {
        UntaxedInterestAmountForm.untaxedInterestAmountForm(user.isAgent, disallowedDuplicateNames(cya,id))
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

          val accountsAbleToReuse: Seq[InterestAccountModel] = {
            cya.flatMap(_.accounts.map(_.filter(!_.hasUntaxed))).getOrElse(Seq()) ++
            prior.map(_.submissions.filter(!_.hasUntaxed)).getOrElse(Seq())
          }

          cya match {
            case Some(cyaData) =>
              val accounts = cyaData.accounts.getOrElse(Seq.empty[InterestAccountModel])
              val existingAccountWithName: Option[InterestAccountModel] = accountsAbleToReuse.find(_.accountName == completeForm.untaxedAccountName)
              val newAccountList = createNewAccountsList(completeForm, existingAccountWithName, accounts, id)
              val updatedCyaModel = cyaData.copy(accounts = Some(newAccountList))

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

  def createNewAccountsList(completeForm: UntaxedInterestModel,
                            existingAccountWithName: Option[InterestAccountModel],
                            accounts: Seq[InterestAccountModel],
                            id: String): Seq[InterestAccountModel] = {
    def createNewAccount(overrideId: Option[String] = None): InterestAccountModel = {
      InterestAccountModel(None, completeForm.untaxedAccountName, Some(completeForm.untaxedAmount), None, Some(overrideId.getOrElse(id)))
    }

    if(existingAccountWithName.isDefined){
      val updatedAccount: InterestAccountModel = existingAccountWithName.get.copy(untaxedAmount = Some(completeForm.untaxedAmount))
      val existingAccount: Option[InterestAccountModel] = accounts.find(_.getPrimaryId().exists(_ == id)).map(_.copy(untaxedAmount = None))
      val existingAccountNeedsRemoving: Boolean = existingAccount.exists(account => !account.hasTaxed)

      val accountsExcludingImpactedAccounts: Seq[InterestAccountModel]  = {
        accounts.filterNot(account => account.accountName == completeForm.untaxedAccountName || account.getPrimaryId().contains(id))
      }

      if(existingAccountNeedsRemoving){
        accountsExcludingImpactedAccounts :+ updatedAccount
      } else {
        accountsExcludingImpactedAccounts ++ Seq(Some(updatedAccount), existingAccount).flatten
      }
    } else {

      val existingAccount: Option[InterestAccountModel] = accounts.find(_.getPrimaryId().exists(_ == id))
      val accountAlreadyExistsWithTaxedAmountAndNameChanged = existingAccount.exists{
        account => account.hasTaxed && (account.accountName != completeForm.untaxedAccountName)
      }

      //if the name has been updated only update the name for the untaxed account and keep the existing taxed account as is
      if(accountAlreadyExistsWithTaxedAmountAndNameChanged){
        val removedAmountFromExistingAccount: InterestAccountModel = existingAccount.get.copy(untaxedAmount = None)
        val newAccount: InterestAccountModel = createNewAccount(Some(UUID.randomUUID().toString))

        accounts.filterNot(_.getPrimaryId().contains(id)) ++ Seq(newAccount, removedAmountFromExistingAccount)
      } else {
        val newAccount = accounts.find(_.getPrimaryId().exists(_ == id)).map(_.copy(
          accountName = completeForm.untaxedAccountName, untaxedAmount = Some(completeForm.untaxedAmount)
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
