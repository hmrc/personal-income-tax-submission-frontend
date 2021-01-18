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

import common.InterestTaxTypes._
import common.SessionValues
import config.AppConfig
import controllers.predicates.AuthorisedAction
import forms.YesNoForm
import javax.inject.Inject
import models.formatHelpers.YesNoModel
import models.interest.{InterestAccountModel, InterestCYAModel, InterestPriorSubmission}
import play.api.Logger
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.interest.RemoveAccountView

import scala.concurrent.Future

class RemoveAccountController @Inject()(
                                    mcc: MessagesControllerComponents,
                                    view: RemoveAccountView,
                                    authorisedAction: AuthorisedAction
                                  )(
                                    implicit appConfig: AppConfig
                                  ) extends FrontendController(mcc) with I18nSupport {

  private val logger = Logger.logger
  val yesNoForm: Form[YesNoModel] = YesNoForm.yesNoForm("interest.remove-account.errors.noRadioSelected")

  implicit def resultToFutureResult: Result => Future[Result] = baseResult => Future.successful(baseResult)

  def show(taxYear: Int, taxType: String, accountId: String): Action[AnyContent] = authorisedAction.async { implicit user =>
    def overviewRedirect = Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))

    val optionalCyaData: Option[InterestCYAModel] = user.session.get(SessionValues.INTEREST_CYA).flatMap(Json.parse(_).asOpt[InterestCYAModel])
    val priorSubmissionData = user.session.get(SessionValues.INTEREST_PRIOR_SUB).flatMap(Json.parse(_).asOpt[InterestPriorSubmission])

    val isPriorSubmission: Boolean =
      priorSubmissionData.exists(_.submissions.getOrElse(Seq.empty[InterestAccountModel]).map(_.id.contains(accountId)).foldRight(false)(_ || _))

    if (isPriorSubmission) {
      priorAccountExistsRedirect(taxType, taxYear)
    } else {
      optionalCyaData match {
        case Some(cyaData) =>
          getTaxAccounts(taxType, cyaData) match {
            case Some(taxAccounts) if taxAccounts.nonEmpty =>
              useAccount(taxAccounts, accountId, taxType, taxYear) { account =>
                Ok(view(yesNoForm, taxYear, taxType, account))
              }
            case _ => missingAccountsRedirect(taxType, taxYear)
          }
        case _ =>
          logger.info("[RemoveAccountController][show] No CYA data in session. Redirecting to the overview page.")
          overviewRedirect

      }
    }
  }

  def submit(taxYear: Int, taxType: String, accountId: String): Action[AnyContent] = authorisedAction.async { implicit user =>
    val optionalCyaData: Option[InterestCYAModel] = user.session.get(SessionValues.INTEREST_CYA).flatMap(Json.parse(_).asOpt[InterestCYAModel])
    yesNoForm.bindFromRequest().fold(
      {
        formWithErrors =>
          optionalCyaData match {
            case Some(cyaData) =>
              getTaxAccounts(taxType, cyaData) match {
                case Some(taxAccounts) if taxAccounts.nonEmpty =>
                  useAccount(taxAccounts, accountId, taxType, taxYear) { account =>
                    BadRequest(view(formWithErrors, taxYear, taxType, account))
                  }
                case _ => missingAccountsRedirect(taxType, taxYear)
              }
            case _ => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
          }
      },
      {
        yesNoModel =>
          optionalCyaData match {
            case Some(cyaData) =>
              getTaxAccounts(taxType, cyaData) match {
                case Some(taxAccounts) if taxAccounts.nonEmpty =>
                  if (yesNoModel.asBoolean) {
                    val updatedAccounts = taxAccounts.filterNot(account => account.id.getOrElse(account.uniqueSessionId.getOrElse("")) == accountId)
                    if(taxType == UNTAXED) {
                      val updatedCyaData = cyaData.copy(
                        untaxedUkInterest = Some(updatedAccounts.nonEmpty),
                        untaxedUkAccounts = Some(updatedAccounts))
                      if(updatedAccounts.nonEmpty){
                        Redirect(controllers.interest.routes.AccountsController.show(taxYear, taxType)).addingToSession(SessionValues.INTEREST_CYA -> updatedCyaData.asJsonString)
                      } else {
                        Redirect(controllers.interest.routes.TaxedInterestController.show(taxYear)).addingToSession(SessionValues.INTEREST_CYA -> updatedCyaData.asJsonString)
                      }
                    } else {
                      val updatedCyaData = cyaData.copy(
                        taxedUkInterest = Some(updatedAccounts.nonEmpty),
                        taxedUkAccounts = Some(updatedAccounts))
                      if(updatedAccounts.nonEmpty){
                        Redirect(controllers.interest.routes.AccountsController.show(taxYear, taxType)).addingToSession(SessionValues.INTEREST_CYA -> updatedCyaData.asJsonString)
                      } else {
                        Redirect(controllers.interest.routes.InterestCYAController.show(taxYear)).addingToSession(SessionValues.INTEREST_CYA -> updatedCyaData.asJsonString)
                      }
                    }
                  } else {
                    Redirect(controllers.interest.routes.AccountsController.show(taxYear, taxType)).withSession(user.session)
                  }
                case _ => missingAccountsRedirect(taxType, taxYear)
              }
            case _ =>
              logger.info("[RemoveAccountController][submit] No CYA data in session. Redirecting to the overview page.")
              Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
          }
      }
    )
  }

  private[interest] def getTaxAccounts(taxType: String, cyaData: InterestCYAModel): Option[Seq[InterestAccountModel]] = {
    taxType match {
      case `TAXED` => cyaData.taxedUkAccounts
      case `UNTAXED` => cyaData.untaxedUkAccounts
    }
  }

  private[interest] def missingAccountsRedirect(taxType: String, taxYear: Int): Result = {
    logger.info(s"[RemoveAccountController][missingAccountsRedirect] No accounts for tax type '$taxType'.")

    taxType match {
      case `TAXED` => Redirect(controllers.interest.routes.TaxedInterestController.show(taxYear))
      case `UNTAXED` => Redirect(controllers.interest.routes.UntaxedInterestController.show(taxYear))
    }
  }

  private[interest] def priorAccountExistsRedirect(taxType: String, taxYear: Int): Result = {
    logger.info(s"[RemoveAccountController][priorAccountExistsRedirect] Account deletion of prior account attempted.")
    Redirect(controllers.interest.routes.AccountsController.show(taxYear, taxType))
  }



  def useAccount(accounts: Seq[InterestAccountModel], identifier: String, taxType: String, taxYear: Int)(action: InterestAccountModel => Result): Result ={
    accounts.find(account => account.id.getOrElse(account.uniqueSessionId.getOrElse("")) == identifier) match {
      case Some(account) => action(account)
      case _ => missingAccountsRedirect(taxType, taxYear)
    }
  }

}
