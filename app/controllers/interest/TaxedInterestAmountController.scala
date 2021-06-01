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

import common.InterestTaxTypes.TAXED
import common.{InterestTaxTypes, SessionValues}
import config.{AppConfig, INTEREST}
import controllers.predicates.{AuthorisedAction, QuestionsJourneyValidator}
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import models.question.QuestionsJourney
import models.interest.{InterestAccountModel, InterestCYAModel, TaxedInterestModel}
import play.api.Logger
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.InterestSessionHelper
import views.html.interest.TaxedInterestAmountView
import java.util.UUID.randomUUID
import forms.interest.TaxedInterestAmountForm
import javax.inject.Inject

import scala.concurrent.ExecutionContext

class TaxedInterestAmountController @Inject()(
                                               taxedInterestAmountView: TaxedInterestAmountView
                                             )(
                                               implicit appConfig: AppConfig,
                                               authorisedAction: AuthorisedAction,
                                               implicit val mcc: MessagesControllerComponents,
                                               questionsJourneyValidator: QuestionsJourneyValidator
                                             ) extends FrontendController(mcc) with InterestSessionHelper with I18nSupport {

  implicit val executionContext: ExecutionContext = mcc.executionContext

  def show(taxYear: Int, id: String): Action[AnyContent] = commonPredicates(taxYear, INTEREST).apply { implicit user =>

    val optionalCyaData = getModelFromSession[InterestCYAModel](SessionValues.INTEREST_CYA)

    val idMatchesPreviouslySubmittedAccount: Boolean = optionalCyaData.flatMap(_.taxedUkAccounts.map(_.exists(_.id.contains(id)))).getOrElse(false)

    val previousNames: Seq[String] = optionalCyaData.flatMap(_.taxedUkAccounts.map(_.map(_.accountName))).getOrElse(Seq.empty[String])

    implicit val journey: QuestionsJourney[InterestCYAModel] = InterestCYAModel.interestJourney(taxYear, Some(id))

    lazy val taxedInterestAmountForm: Form[TaxedInterestModel] = TaxedInterestAmountForm.taxedInterestAmountForm(previousNames)

    questionsJourneyValidator.validate(controllers.interest.routes.TaxedInterestAmountController.show(taxYear, id), optionalCyaData, taxYear) {

      if (idMatchesPreviouslySubmittedAccount) {
        Redirect(controllers.interest.routes.ChangeAccountAmountController.show(taxYear, TAXED, id))

      } else if (sessionIdIsUUID(id)) {

        val account: Option[InterestAccountModel] = optionalCyaData.flatMap(_.taxedUkAccounts.flatMap(_.find { account =>
          account.uniqueSessionId.getOrElse("") == id
        }))

        val accountName = account.map(_.accountName)
        val accountAmount = account.map(_.amount)

        val model: Option[TaxedInterestModel] = (accountName, accountAmount) match {
          case (Some(name), Some(amount)) => Some(TaxedInterestModel(name, amount))
          case _ => None
        }

        Ok(taxedInterestAmountView(
          form = model.fold(taxedInterestAmountForm)(taxedInterestAmountForm.fill),
          taxYear,
          controllers.interest.routes.TaxedInterestAmountController.submit(taxYear, id),
          isAgent = user.isAgent
        ))
      } else {
        Redirect(controllers.interest.routes.TaxedInterestAmountController.show(taxYear, randomUUID().toString))
      }
    }
  }

  def submit(taxYear: Int, id: String): Action[AnyContent] = (authorisedAction andThen journeyFilterAction(taxYear, INTEREST)) { implicit user =>

    val optionalCyaData = getModelFromSession[InterestCYAModel](SessionValues.INTEREST_CYA)

    val previousNames: Seq[String] = optionalCyaData.flatMap(_.taxedUkAccounts.map(_.map(_.accountName))).getOrElse(Seq.empty[String])

    lazy val taxedInterestAmountForm: Form[TaxedInterestModel] = TaxedInterestAmountForm.taxedInterestAmountForm(previousNames)

    taxedInterestAmountForm.bindFromRequest().fold({
      formWithErrors =>
        BadRequest(taxedInterestAmountView(
          form = formWithErrors,
          taxYear = taxYear,
          postAction = controllers.interest.routes.TaxedInterestAmountController.submit(taxYear, id),
          isAgent = user.isAgent
        ))
    }, {
      completeForm =>
        val newAmount: BigDecimal = completeForm.taxedAmount

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
