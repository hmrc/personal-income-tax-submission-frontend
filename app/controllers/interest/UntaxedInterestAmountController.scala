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
    interestSessionService.getSessionData(taxYear).map { cya =>

      implicit val journey: QuestionsJourney[InterestCYAModel] = InterestCYAModel.interestJourney(taxYear, Some(id))

      val optionalCyaData = cya.flatMap(_.interest)
      val idMatchesPreviouslySubmittedAccount: Boolean = optionalCyaData.flatMap(_.accounts.map(_.exists(_.id.contains(id)))).getOrElse(false)

      def untaxedInterestAmountForm: Form[UntaxedInterestModel] =
        UntaxedInterestAmountForm.untaxedInterestAmountForm(user.isAgent, disallowedDuplicateNames(optionalCyaData,id))

      questionsJourneyValidator.validate(routes.UntaxedInterestAmountController.show(taxYear, id), optionalCyaData, taxYear) {

        if (idMatchesPreviouslySubmittedAccount) {
          Redirect(controllers.interest.routes.ChangeAccountAmountController.show(taxYear, UNTAXED, id))
        } else if (sessionIdIsUUID(id)) {

          val account: Option[InterestAccountModel] = optionalCyaData.flatMap(_.accounts.flatMap(_.find { account =>
            account.uniqueSessionId.getOrElse("") == id
          }))

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
      }
    }
  }

  def disallowedDuplicateNames(optionalCyaData: Option[InterestCYAModel], id: String): Seq[String] = {
    optionalCyaData.flatMap(_.accounts.map { accounts =>
      accounts.filter(_.hasUntaxed).filterNot(_.getPrimaryId().contains(id))
    }).getOrElse(Seq()).map(_.accountName)
  }

  def submit(taxYear: Int, id: String): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, INTEREST)).async { implicit user =>

    interestSessionService.getSessionData(taxYear).map { cya =>
      val optionalCyaData = cya.flatMap(_.interest)

      def untaxedInterestAmountForm: Form[UntaxedInterestModel] = {
        UntaxedInterestAmountForm.untaxedInterestAmountForm(user.isAgent, disallowedDuplicateNames(optionalCyaData,id))
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
            optionalCyaData.flatMap(_.accounts.map(accounts => accounts.filter(!_.hasUntaxed))).getOrElse(Seq())
          }

          cya.flatMap(_.interest) match {
            case Some(cyaData) =>
              val accounts = cyaData.accounts.getOrElse(Seq.empty[InterestAccountModel])
              val accountToReuse: Option[InterestAccountModel] = accountsAbleToReuse.find(_.accountName == completeForm.untaxedAccountName)
              val newAccountList = createNewAccountsList(completeForm, accountToReuse, accounts, id)
              val updatedCyaModel = cyaData.copy(accounts = Some(newAccountList))

              interestSessionService.updateSessionData(updatedCyaModel, taxYear)(InternalServerError(errorHandler.internalServerErrorTemplate))(
                Redirect(controllers.interest.routes.AccountsController.show(taxYear, InterestTaxTypes.UNTAXED))
              )
            case _ =>
              logger.info("[UntaxedInterestController][submit] No CYA data in session. Redirecting to overview page.")
              Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          }
      })
    }.flatten
  }

  def createNewAccountsList(completeForm: UntaxedInterestModel,
                            accountToReuse: Option[InterestAccountModel],
                            accounts: Seq[InterestAccountModel],
                            id: String): Seq[InterestAccountModel] = {

    def createNewAccount(overrideId: Option[String] = None): InterestAccountModel = {
      InterestAccountModel(None, completeForm.untaxedAccountName, Some(completeForm.untaxedAmount), None, Some(overrideId.getOrElse(id)))
    }

    if(accountToReuse.isDefined){
      // update existing account
      // remove account with id if now empty
      val updatedAccount: InterestAccountModel = accountToReuse.get.copy(untaxedAmount = Some(completeForm.untaxedAmount))
      val existingAccountNeedsRemoving: Boolean = {
        accounts.find(_.getPrimaryId().exists(_ == id)).exists(account => !account.hasTaxed)
      }

      if(existingAccountNeedsRemoving){
        accounts.filterNot(account => account.accountName == completeForm.untaxedAccountName || account.getPrimaryId().contains(id)) :+ updatedAccount
      } else {
        accounts.filterNot(_.accountName == completeForm.untaxedAccountName) :+ updatedAccount
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
