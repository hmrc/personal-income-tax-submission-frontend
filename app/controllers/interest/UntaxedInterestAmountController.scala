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

import common.InterestTaxTypes.UNTAXED
import common.{InterestTaxTypes, SessionValues}
import config.{AppConfig, INTEREST}
import controllers.interest.routes.UntaxedInterestAmountController
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import controllers.predicates.{AuthorisedAction, QuestionsJourneyValidator}
import forms.interest.UntaxedInterestAmountForm
import models.interest.{InterestAccountModel, InterestCYAModel, UntaxedInterestModel}
import models.question.QuestionsJourney
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.InterestSessionHelper
import views.html.interest.UntaxedInterestAmountView

import java.util.UUID.randomUUID
import javax.inject.Inject

class UntaxedInterestAmountController @Inject()(
                                                 implicit val mcc: MessagesControllerComponents,
                                                 authAction: AuthorisedAction,
                                                 untaxedInterestAmountView: UntaxedInterestAmountView,
                                                 implicit val appConfig: AppConfig,
                                                 questionsJourneyValidator: QuestionsJourneyValidator
                                               ) extends FrontendController(mcc) with I18nSupport with InterestSessionHelper with Logging {

  def show(taxYear: Int, id: String): Action[AnyContent] = commonPredicates(taxYear, INTEREST).apply { implicit user =>

    val optionalCyaData = getModelFromSession[InterestCYAModel](SessionValues.INTEREST_CYA)

    val idMatchesPreviouslySubmittedAccount: Boolean = optionalCyaData.flatMap(_.untaxedUkAccounts.map(_.exists(_.id.contains(id)))).getOrElse(false)

    val previousNames: Seq[String] = optionalCyaData.flatMap(_.untaxedUkAccounts.map(_.map(_.accountName))).getOrElse(Seq.empty[String])

    implicit val journey: QuestionsJourney[InterestCYAModel] = InterestCYAModel.interestJourney(taxYear, Some(id))

    def untaxedInterestAmountForm(implicit isAgent: Boolean, previousNames: Seq[String]): Form[UntaxedInterestModel] =
      UntaxedInterestAmountForm.untaxedInterestAmountForm(
        isAgent,
        previousNames
    )

    questionsJourneyValidator.validate(UntaxedInterestAmountController.show(taxYear, id), optionalCyaData, taxYear) {
      if (idMatchesPreviouslySubmittedAccount) {
        Redirect(controllers.interest.routes.ChangeAccountAmountController.show(taxYear, UNTAXED, id))

    } else if (sessionIdIsUUID(id)) {

      val account: Option[InterestAccountModel] = optionalCyaData.flatMap(_.untaxedUkAccounts.flatMap(_.find { account =>
        account.uniqueSessionId.getOrElse("") == id
      }))

        val accountName = account.map(_.accountName)
        val accountAmount = account.map(_.amount)

        val model: Option[UntaxedInterestModel] = (accountName, accountAmount) match {
          case (Some(name), Some(amount)) => Some(UntaxedInterestModel(name, amount))
          case _ => None
        }

        Ok(untaxedInterestAmountView(
          form = model.fold(untaxedInterestAmountForm(user.isAgent, previousNames))(untaxedInterestAmountForm(user.isAgent, previousNames).fill),
          taxYear = taxYear,
          postAction = UntaxedInterestAmountController.submit(taxYear, id),
          isAgent = user.isAgent
        ))
      }
      else
      {
        Redirect(UntaxedInterestAmountController.show(taxYear, randomUUID().toString))
      }
    }
  }

  def submit(taxYear: Int, id: String): Action[AnyContent] = (authAction andThen journeyFilterAction(taxYear, INTEREST)) { implicit user =>

    val optionalCyaData = getModelFromSession[InterestCYAModel](SessionValues.INTEREST_CYA)

    val previousNames: Seq[String] = optionalCyaData.flatMap(_.untaxedUkAccounts.map(_.map(_.accountName))).getOrElse(Seq.empty[String])

    def untaxedInterestAmountForm(implicit isAgent: Boolean): Form[UntaxedInterestModel] = UntaxedInterestAmountForm.untaxedInterestAmountForm(
      isAgent,
      previousNames
    )

    untaxedInterestAmountForm(user.isAgent).bindFromRequest().fold({
      formWithErrors =>
        BadRequest(untaxedInterestAmountView(
          form = formWithErrors,
          taxYear = taxYear,
          postAction = UntaxedInterestAmountController.submit(taxYear, id),
          isAgent = user.isAgent
        ))
    }, {
      completeForm =>
        val newAmount: BigDecimal = completeForm.untaxedAmount

        def createNewAccount: InterestAccountModel = InterestAccountModel(None, completeForm.untaxedAccountName, newAmount, Some(id))

        optionalCyaData match {
          case Some(cyaData) =>
            val accounts = cyaData.untaxedUkAccounts.getOrElse(Seq.empty[InterestAccountModel])
            val newAccount = accounts.find(_.getPrimaryId().exists(_ == id)).map(_.copy(
              accountName = completeForm.untaxedAccountName, amount = newAmount
            )).getOrElse(createNewAccount)
            val newAccountList = if (newAccount.getPrimaryId().nonEmpty && accounts.exists(_.getPrimaryId() == newAccount.getPrimaryId())) {
              accounts.map(account => if (account.getPrimaryId() == newAccount.getPrimaryId()) newAccount else account)
            } else {
              accounts :+ newAccount
            }
            val updatedCyaModel = cyaData.copy(untaxedUkAccounts = Some(newAccountList))
            Redirect(controllers.interest.routes.AccountsController.show(taxYear, InterestTaxTypes.UNTAXED))
              .addingToSession(SessionValues.INTEREST_CYA -> updatedCyaModel.asJsonString)
          case _ =>
            logger.info("[UntaxedInterestController][submit] No CYA data in session. Redirecting to overview page.")
            Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
        }
    })
  }
}
