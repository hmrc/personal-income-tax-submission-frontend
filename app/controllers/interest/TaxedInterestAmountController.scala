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

import java.util.UUID.randomUUID

import common.InterestTaxTypes.TAXED
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

  def show(taxYear: Int, id: String): Action[AnyContent] = authorisedAction { implicit user =>

    val optionalCyaData = getModelFromSession[InterestCYAModel](SessionValues.INTEREST_CYA)

    val idMatchesPreviouslySubmittedAccount: Boolean = optionalCyaData.flatMap(_.taxedUkAccounts.map(_.exists(_.id.contains(id)))).getOrElse(false)

    if (idMatchesPreviouslySubmittedAccount) {
      Redirect(controllers.interest.routes.ChangeAccountAmountController.show(taxYear, TAXED, id))

    } else if (sessionIdIsUUID(id)) {

      val account: Option[InterestAccountModel] = optionalCyaData.flatMap(_.taxedUkAccounts.flatMap(_.find { account =>
        account.uniqueSessionId.getOrElse("") == id
      }))

      val accountName = account.map(_.accountName)
      val accountAmount = account.map(_.amount)

      val model: Option[TaxedInterestModel] = (accountName, accountAmount) match {
        case (Some(name), Some(amount)) => Some(TaxedInterestModel(name, amount.toString()))
        case _ => None
      }

      Ok(taxedInterestAmountView(
        form = model.fold(taxedInterestAmountForm)(taxedInterestAmountForm.fill),
        taxYear,
        controllers.interest.routes.TaxedInterestAmountController.submit(taxYear, id)
      ))
    } else {
      Redirect(controllers.interest.routes.TaxedInterestAmountController.show(taxYear, randomUUID().toString))
    }
  }

  def submit(taxYear: Int, id: String): Action[AnyContent] = authorisedAction { implicit user =>
    taxedInterestAmountForm.bindFromRequest().fold({
      formWithErrors =>
        BadRequest(taxedInterestAmountView(
          form = formWithErrors,
          taxYear = taxYear,
          postAction = controllers.interest.routes.TaxedInterestAmountController.submit(taxYear, id)
        ))
    }, {
      completeForm =>
        val newAmount: BigDecimal = BigDecimal(completeForm.taxedAmount)

        def createNewAccount: InterestAccountModel = InterestAccountModel(None, completeForm.taxedAccountName, newAmount, Some(id))

        val optionalCyaData = getModelFromSession[InterestCYAModel](SessionValues.INTEREST_CYA)

        optionalCyaData match {
          case Some(cyaData) =>
            val accounts = cyaData.taxedUkAccounts.getOrElse(Seq.empty[InterestAccountModel])

            val newAccount = accounts.find(_.getPrimaryId().exists(_ == id)).map(_.copy(
              accountName = completeForm.taxedAccountName, amount = newAmount
            )).getOrElse(createNewAccount)

            val newAccountList = if (newAccount.getPrimaryId().nonEmpty && accounts.exists(_.getPrimaryId() == newAccount.getPrimaryId())) {
              accounts.map(account => if (account.getPrimaryId() == newAccount.getPrimaryId()) newAccount else account)
            } else {
              accounts :+ newAccount
            }

            val updatedCyaModel = cyaData.copy(taxedUkAccounts = Some(newAccountList))

            Redirect(controllers.interest.routes.AccountsController.show(taxYear, InterestTaxTypes.TAXED))
              .addingToSession(SessionValues.INTEREST_CYA -> updatedCyaModel.asJsonString)
          case _ =>
            Logger.logger.info("[TaxedInterestController][submit] No CYA data in session. Redirecting to overview page.")
            Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        }
    })
  }

}
