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

import common.{InterestTaxTypes, PageLocations, SessionValues}
import config.AppConfig
import controllers.predicates.AuthorisedAction
import forms.UntaxedInterestAmountForm
import javax.inject.Inject
import models.UntaxedInterestModel
import models.interest.{InterestAccountModel, InterestCYAModel}
import play.api.Logger
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.InterestSessionHelper
import views.html.interest.UntaxedInterestAmountView

class UntaxedInterestAmountController @Inject()(
                                                 mcc: MessagesControllerComponents,
                                                 authAction: AuthorisedAction,
                                                 untaxedInterestAmountView: UntaxedInterestAmountView,
                                                 implicit val appConfig: AppConfig
                                               ) extends FrontendController(mcc) with I18nSupport with InterestSessionHelper {

  val untaxedInterestAmountForm: Form[UntaxedInterestModel] = UntaxedInterestAmountForm.untaxedInterestAmountForm()

  private[interest] def backLink(taxYear: Int)(implicit request: Request[_]): Option[String] = {
    import PageLocations.Interest._

    if (getModelFromSession[InterestCYAModel](SessionValues.INTEREST_CYA).exists(_.isFinished)) {
      Some(UntaxedAccountsView(taxYear))
    } else {
      getFromSession(SessionValues.PAGE_BACK_UNTAXED_AMOUNT) match {
        case location@Some(_) => location
        case None => Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }
  }

  def show(taxYear: Int, modify: Option[String] = None): Action[AnyContent] = authAction { implicit user =>
    val optionalCyaData = getModelFromSession[InterestCYAModel](SessionValues.INTEREST_CYA)

    val preName: Option[String] = modify.flatMap { identifier =>
      optionalCyaData.flatMap(_.untaxedUkAccounts.flatMap(_.find { account =>
        account.id.getOrElse(account.uniqueSessionId.getOrElse("")) == identifier
      }).map(_.accountName))
    }

    val preAmount: Option[BigDecimal] = modify.flatMap { identifier =>
      optionalCyaData.flatMap(_.untaxedUkAccounts.flatMap(_.find { account =>
        account.id.getOrElse(account.uniqueSessionId.getOrElse("")) == identifier
      }).map(_.amount))
    }

    Ok(untaxedInterestAmountView(
      form = untaxedInterestAmountForm,
      backLink = backLink(taxYear),
      taxYear = taxYear,
      postAction = controllers.interest.routes.UntaxedInterestAmountController.submit(taxYear, modify),
      preName = preName,
      preAmount = preAmount
    ))
  }

  def submit(taxYear: Int, modify: Option[String] = None): Action[AnyContent] = authAction { implicit user =>
    untaxedInterestAmountForm.bindFromRequest().fold({
      formWithErrors =>
        BadRequest(untaxedInterestAmountView(
          form = formWithErrors,
          backLink = backLink(taxYear),
          taxYear = taxYear,
          postAction = controllers.interest.routes.UntaxedInterestAmountController.submit(taxYear)
        ))
    },
      {
        completeForm =>
          val newAmount: BigDecimal = BigDecimal(completeForm.untaxedAmount)

          def createNewAccount: InterestAccountModel =
            InterestAccountModel(None, completeForm.untaxedAccountName, newAmount, Some(java.util.UUID.randomUUID().toString))

          val optionalCyaData = getModelFromSession[InterestCYAModel](SessionValues.INTEREST_CYA)

          optionalCyaData match {
            case Some(cyaData) =>
              val accounts = cyaData.untaxedUkAccounts.getOrElse(Seq.empty[InterestAccountModel])

              val newAccount = modify match {
                case Some(identifier) => accounts.find(_.getPrimaryId().exists(_ == identifier)).map(_.copy(
                  accountName = completeForm.untaxedAccountName, amount = newAmount
                )).getOrElse(createNewAccount)
                case _ => createNewAccount
              }

              val newAccountList = if(newAccount.getPrimaryId().nonEmpty && accounts.exists(_.getPrimaryId() == newAccount.getPrimaryId())) {
                accounts.map ( account => if(account.getPrimaryId() == newAccount.getPrimaryId()) newAccount else account)
              } else {
                accounts :+ newAccount
              }

              val updatedCyaModel = cyaData.copy(untaxedUkAccounts = Some(newAccountList))

              Redirect(controllers.interest.routes.AccountsController.show(taxYear, InterestTaxTypes.UNTAXED))
                .addingToSession(SessionValues.INTEREST_CYA -> updatedCyaModel.asJsonString)
                .updateAccountsOverviewRedirect(
                  PageLocations.Interest.UntaxedAmountsView(taxYear, newAccount.getPrimaryId()), InterestTaxTypes.UNTAXED
                )
            case _ =>
              Logger.logger.info("[UntaxedInterestController][submit] No CYA data in session. Redirecting to overview page.")
              Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
          }
      })

  }

}
