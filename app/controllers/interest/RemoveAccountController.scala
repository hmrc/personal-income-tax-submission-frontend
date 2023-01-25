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

import common.InterestTaxTypes._
import config.{AppConfig, ErrorHandler, INTEREST}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import forms.YesNoForm
import models.User
import models.interest.{InterestAccountModel, InterestCYAModel, InterestPriorSubmission}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{InterestSessionService, RemoveAccountService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SeqConversions.SeqInterestAccountModel
import views.html.interest.RemoveAccountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveAccountController @Inject()(
                                         view: RemoveAccountView,
                                         interestSessionService: InterestSessionService,
                                         removeAccountService: RemoveAccountService,
                                         errorHandler: ErrorHandler
                                       )(
                                         implicit appConfig: AppConfig,
                                         val mcc: MessagesControllerComponents,
                                         authorisedAction: AuthorisedAction,
                                         ec: ExecutionContext
                                       ) extends FrontendController(mcc) with I18nSupport with Logging{


  val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm("interest.remove-account.errors.noRadioSelected")

  implicit def resultToFutureResult: Result => Future[Result] = baseResult => Future.successful(baseResult)

  def show(taxYear: Int, taxType: String, accountId: String): Action[AnyContent] = commonPredicates(taxYear, INTEREST).async { implicit user =>
    interestSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>
      if (InterestPriorSubmission.foundPriorSubmission(prior,accountId).isDefined && InterestPriorSubmission.isPriorSubmissionWithAmountsThatCantBeRemoved(prior,accountId, taxType)) {
        priorAccountExistsRedirect(taxType, taxYear)
      } else {
        renderPage(cya,taxType,accountId,taxYear,prior)
      }
    }
  }

  def renderPage(cya: Option[InterestCYAModel], taxType: String, accountId: String, taxYear: Int,
                 prior: Option[InterestPriorSubmission], errorForm: Option[Form[Boolean]] = None)(implicit user: User[_]): Result = {
    cya match {
      case Some(cyaData) =>
        cyaData.accounts match {
          case taxAccounts if taxAccounts.filterByTaxType(taxType).nonEmpty =>
            val accounts = taxAccounts.filterByTaxType(taxType)
            useAccount(accounts, accountId, taxType, taxYear) { account =>
              errorForm.fold(Ok(view(yesNoForm, taxYear, taxType, account, removeAccountService.isLastAccount(taxType, prior, accounts)))){
                formWithErrors => BadRequest(view(formWithErrors, taxYear, taxType, account, removeAccountService.isLastAccount(taxType, prior, accounts)))
              }
            }
          case _ => missingAccountsRedirect(taxType, taxYear)
        }
      case _ => overviewRedirect(taxYear)
    }
  }

  def useAccount(accounts: Seq[InterestAccountModel], identifier: String, taxType: String, taxYear: Int)(action: InterestAccountModel => Result): Result = {
    accounts.find(account => removeAccountService.accountLookup(account, identifier)) match {
      case Some(account) => action(account)
      case _ => missingAccountsRedirect(taxType, taxYear)
    }
  }

  def submit(taxYear: Int, taxType: String, accountId: String): Action[AnyContent] =
    (authorisedAction andThen journeyFilterAction(taxYear, INTEREST)).async { implicit user =>
      interestSessionService.getAndHandle(taxYear)(errorHandler.futureInternalServerError()) { (cya, prior) =>

        if (InterestPriorSubmission.foundPriorSubmission(prior,accountId).isDefined && InterestPriorSubmission.isPriorSubmissionWithAmountsThatCantBeRemoved(prior,accountId, taxType)) {
          Future(priorAccountExistsRedirect(taxType, taxYear))
        } else {
          yesNoForm.bindFromRequest().fold(
            formWithErrors =>
              Future.successful(renderPage(cya,taxType,accountId,taxYear,prior,Some(formWithErrors))),
            yesNoModel =>
              cya match {
                case Some(cyaData) => Future(removeAccount(taxYear, taxType, accountId, yesNoModel, cyaData))
                case _ => Future(overviewRedirect(taxYear))
              }
          )
        }
      }.flatten
    }

  def updateAndRedirect(cyaData: InterestCYAModel, taxYear: Int)(redirect: Result)(implicit user: User[_]): Future[Result] = {
    interestSessionService.updateSessionData(cyaData, taxYear)(errorHandler.internalServerError())(redirect)
  }

  private[interest] def removeAccount(taxYear: Int,
                                      taxType: String,
                                      accountId: String,
                                      yesNoModel: Boolean,
                                      cyaData: InterestCYAModel
                                     )(implicit user: User[_]): Future[Result] = {
    cyaData.accounts match {
      case taxAccounts if taxAccounts.filterByTaxType(taxType).nonEmpty =>
        if (yesNoModel) {
          if (taxType == UNTAXED) {
            val (updatedCyaData, updatedAccounts) = removeAccountService.calculateUntaxedUpdate(cyaData, taxAccounts, accountId)

            if (updatedAccounts.exists(_.hasUntaxed)) {
              updateAndRedirect(updatedCyaData, taxYear)(Redirect(controllers.interest.routes.AccountsController.show(taxYear, taxType)))
            } else {
              updateAndRedirect(updatedCyaData, taxYear)(Redirect(controllers.interest.routes.InterestCYAController.show(taxYear)))
            }
          } else {
            val (updatedCyaData, updatedAccounts) = removeAccountService.calculateTaxedUpdate(cyaData, taxAccounts, accountId)

            if (updatedAccounts.exists(_.hasTaxed)) {
              updateAndRedirect(updatedCyaData, taxYear)(Redirect(controllers.interest.routes.AccountsController.show(taxYear, taxType)))
            } else {
              updateAndRedirect(updatedCyaData, taxYear)(Redirect(controllers.interest.routes.InterestCYAController.show(taxYear)))
            }
          }
        } else {
          Redirect(controllers.interest.routes.AccountsController.show(taxYear, taxType)).withSession(user.session)
        }
      case _ => missingAccountsRedirect(taxType, taxYear)
    }
  }

  def overviewRedirect(taxYear: Int): Result = {
    logger.info("[RemoveAccountController][submit] No CYA data in session. Redirecting to the overview page.")
    Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
  }

  private[interest] def missingAccountsRedirect(taxType: String, taxYear: Int): Result = {
    logger.info(s"[RemoveAccountController][missingAccountsRedirect] No accounts for tax type '$taxType'.")

    taxType match {
      case `TAXED` => Redirect(controllers.interest.routes.TaxedInterestController.show(taxYear))
      case _ => Redirect(controllers.interest.routes.UntaxedInterestController.show(taxYear))
    }
  }

  private[interest] def priorAccountExistsRedirect(taxType: String, taxYear: Int): Result = {
    logger.info(s"[RemoveAccountController][priorAccountExistsRedirect] Account deletion of prior account attempted.")
    Redirect(controllers.interest.routes.AccountsController.show(taxYear, taxType))
  }
}
