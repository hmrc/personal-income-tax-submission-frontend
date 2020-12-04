/*
 * Copyright 2020 HM Revenue & Customs
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

import common.{InterestTaxTypes, SessionValues}
import config.AppConfig
import controllers.predicates.AuthorisedAction
import forms.TaxedInterestAmountForm
import javax.inject.Inject
import models.TaxedInterestModel
import models.interest.{InterestAccountModel, InterestCYAModel}
import play.api.Logger
import play.api.data.Form
import play.api.i18n.{Lang, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.interest.TaxedInterestAmountView

import scala.concurrent.ExecutionContext

class TaxedInterestAmountController @Inject()(mcc: MessagesControllerComponents,
                                              authorisedAction: AuthorisedAction,
                                              taxedInterestAmountView: TaxedInterestAmountView
                                             )(
                                               implicit appConfig: AppConfig) extends FrontendController(mcc) with SessionHelper {

  implicit val executionContext: ExecutionContext = mcc.executionContext
  implicit val messages: Messages = mcc.messagesApi.preferred(Seq(Lang("en")))
  val taxedInterestAmountForm: Form[TaxedInterestModel] = TaxedInterestAmountForm.taxedInterestAmountForm()

  def show(taxYear: Int, modify: Option[String]): Action[AnyContent] = authorisedAction { implicit user =>

    val optionalCyaData = getModelFromSession[InterestCYAModel](SessionValues.INTEREST_CYA)

    val preName: Option[String] = modify.flatMap { identifier =>
      optionalCyaData.flatMap(_.taxedUkAccounts.flatMap(_.find{ account =>
        account.id.getOrElse(account.uniqueSessionId.getOrElse("")) == identifier
      }).map(_.accountName))
    }

    val preAmount: Option[BigDecimal] = modify.flatMap { identifier =>
      optionalCyaData.flatMap(_.taxedUkAccounts.flatMap(_.find{ account =>
        account.id.getOrElse(account.uniqueSessionId.getOrElse("")) == identifier
      }).map(_.amount))
    }

    Ok(taxedInterestAmountView(
      TaxedInterestAmountForm.taxedInterestAmountForm(),
      taxYear,
      controllers.interest.routes.TaxedInterestAmountController.submit(taxYear, modify),
      appConfig.signInUrl,
      preName,
      preAmount
    ))
  }

  def submit(taxYear: Int, modify: Option[String]): Action[AnyContent] = authorisedAction { implicit user =>
    taxedInterestAmountForm.bindFromRequest().fold({
      formWithErrors =>
        BadRequest(taxedInterestAmountView(formWithErrors, taxYear, controllers.interest.routes.TaxedInterestAmountController.submit(taxYear, modify),
          appConfig.signInUrl))
    }, {
      completeForm =>
        val newAmount: BigDecimal = BigDecimal(completeForm.amount)
        def createNewAccount: InterestAccountModel = {
          InterestAccountModel(None, completeForm.friendlyName, newAmount, Some(java.util.UUID.randomUUID().toString))
        }
        val optionalCyaData = getModelFromSession[InterestCYAModel](SessionValues.INTEREST_CYA)

        optionalCyaData match {
          case Some(cyaData) =>
            val accounts = cyaData.taxedUkAccounts.getOrElse(Seq.empty[InterestAccountModel])
            val newAccountList: Seq[InterestAccountModel] = modify match {
              case Some(identifier) =>
                accounts.map { account =>
                  if(account.id.getOrElse(account.uniqueSessionId.getOrElse("")) == identifier) {
                    account.copy(accountName = completeForm.friendlyName, amount = newAmount)
                  } else {
                    account
                  }
                }
              case _ =>
                accounts :+ createNewAccount
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
