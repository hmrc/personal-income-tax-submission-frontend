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
import forms.TaxedInterestAmountForm
import javax.inject.Inject
import models.TaxedInterestModel
import models.interest.{InterestAccountModel, InterestCYAModel}
import play.api.Logger
import play.api.data.Form
import play.api.i18n.{Lang, Messages}
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.InterestSessionHelper
import views.html.interest.TaxedInterestAmountView

import scala.concurrent.ExecutionContext

class TaxedInterestAmountController @Inject()(
                                               mcc: MessagesControllerComponents,
                                               authorisedAction: AuthorisedAction,
                                               taxedInterestAmountView: TaxedInterestAmountView
                                             )(
                                               implicit appConfig: AppConfig
                                             ) extends FrontendController(mcc) with InterestSessionHelper {

  implicit val executionContext: ExecutionContext = mcc.executionContext
  implicit val messages: Messages = mcc.messagesApi.preferred(Seq(Lang("en")))
  val taxedInterestAmountForm: Form[TaxedInterestModel] = TaxedInterestAmountForm.taxedInterestAmountForm()

  private[interest] def backLink(taxYear: Int)(implicit request: Request[_]): Option[String] = {
    import PageLocations.Interest._

    if (getModelFromSession[InterestCYAModel](SessionValues.INTEREST_CYA).exists(_.isFinished)) {
      Some(TaxedAccountsView(taxYear))
    } else {
      getFromSession(SessionValues.PAGE_BACK_TAXED_AMOUNT) match {
        case location@Some(_) => location
        case None => Some(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }
    }
  }

  def show(taxYear: Int, modify: Option[String]): Action[AnyContent] = authorisedAction { implicit user =>

    val optionalCyaData = getModelFromSession[InterestCYAModel](SessionValues.INTEREST_CYA)

    val preName: Option[String] = modify.flatMap { identifier =>
      optionalCyaData.flatMap(_.taxedUkAccounts.flatMap(_.find { account =>
        account.id.getOrElse(account.uniqueSessionId.getOrElse("")) == identifier
      }).map(_.accountName))
    }

    val preAmount: Option[BigDecimal] = modify.flatMap { identifier =>
      optionalCyaData.flatMap(_.taxedUkAccounts.flatMap(_.find { account =>
        account.id.getOrElse(account.uniqueSessionId.getOrElse("")) == identifier
      }).map(_.amount))
    }

    Ok(taxedInterestAmountView(
      TaxedInterestAmountForm.taxedInterestAmountForm(),
      taxYear,
      controllers.interest.routes.TaxedInterestAmountController.submit(taxYear, modify),
      backLink(taxYear),
      preName,
      preAmount
    ))
  }

  def submit(taxYear: Int, modify: Option[String]): Action[AnyContent] = authorisedAction { implicit user =>
    taxedInterestAmountForm.bindFromRequest().fold({
      formWithErrors =>
        BadRequest(taxedInterestAmountView(
          form = formWithErrors,
          taxYear = taxYear,
          postAction = controllers.interest.routes.TaxedInterestAmountController.submit(taxYear, modify),
          backLink = backLink(taxYear)
        ))
    }, {
      completeForm =>
        val newAmount: BigDecimal = BigDecimal(completeForm.taxedAmount)

        def createNewAccount: InterestAccountModel = {
          InterestAccountModel(None, completeForm.taxedAccountName, newAmount, Some(java.util.UUID.randomUUID().toString))
        }

        val optionalCyaData = getModelFromSession[InterestCYAModel](SessionValues.INTEREST_CYA)

        optionalCyaData match {
          case Some(cyaData) =>
            val accounts = cyaData.taxedUkAccounts.getOrElse(Seq.empty[InterestAccountModel])

            val newAccount = modify match {
              case Some(identifier) => accounts.find(_.getPrimaryId().exists(_ == identifier)).map(_.copy(
                accountName = completeForm.taxedAccountName, amount = newAmount
              )).getOrElse(createNewAccount)
              case _ => createNewAccount
            }

            val newAccountList = if(newAccount.getPrimaryId().nonEmpty && accounts.exists(_.getPrimaryId() == newAccount.getPrimaryId())) {
              accounts.map ( account => if(account.getPrimaryId() == newAccount.getPrimaryId()) newAccount else account)
            } else {
              accounts :+ newAccount
            }

            val updatedCyaModel = cyaData.copy(taxedUkAccounts = Some(newAccountList))

            Redirect(controllers.interest.routes.AccountsController.show(taxYear, InterestTaxTypes.TAXED))
              .addingToSession(SessionValues.INTEREST_CYA -> updatedCyaModel.asJsonString)
              .updateAccountsOverviewRedirect(
                PageLocations.Interest.TaxedAmountsView(taxYear, newAccount.getPrimaryId()), InterestTaxTypes.TAXED
              )
          case _ =>
            Logger.logger.info("[TaxedInterestController][submit] No CYA data in session. Redirecting to overview page.")
            Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        }
    })
  }

}
