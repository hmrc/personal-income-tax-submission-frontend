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

import common.InterestTaxTypes
import common.InterestTaxTypes.TAXED
import config.{AppConfig, ErrorHandler, INTEREST}
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import controllers.predicates.{AuthorisedAction, QuestionsJourneyValidator}
import forms.interest.TaxedInterestAmountForm
import javax.inject.Inject
import models.interest.{InterestAccountModel, InterestCYAModel, TaxedInterestModel}
import models.question.QuestionsJourney
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.InterestSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.interest.TaxedInterestAmountView

import scala.concurrent.{ExecutionContext, Future}

class TaxedInterestAmountController @Inject()(
                                               taxedInterestAmountView: TaxedInterestAmountView
                                             )(
                                               implicit appConfig: AppConfig,
                                               authorisedAction: AuthorisedAction,
                                               interestSessionService: InterestSessionService,
                                               errorHandler: ErrorHandler,
                                               implicit val mcc: MessagesControllerComponents,
                                               questionsJourneyValidator: QuestionsJourneyValidator
                                             ) extends FrontendController(mcc) with SessionHelper with I18nSupport with Logging {

  implicit val executionContext: ExecutionContext = mcc.executionContext

  def show(taxYear: Int, id: String): Action[AnyContent] = commonPredicates(taxYear, INTEREST).async { implicit user =>
    interestSessionService.getSessionData(taxYear).map { cya =>

      implicit val journey: QuestionsJourney[InterestCYAModel] = InterestCYAModel.interestJourney(taxYear, Some(id))

      val optionalCyaData = cya.flatMap(_.interest)
      val previousNames: Seq[String] = getPreviousNames(optionalCyaData)
      val idMatchesPreviouslySubmittedAccount: Boolean = optionalCyaData.flatMap(_.taxedUkAccounts.map(_.exists(_.id.contains(id)))).getOrElse(false)
      val sessionIDMatchesPreviouslySubmittedAccount: Boolean = optionalCyaData.flatMap(_.untaxedUkAccounts.map(_.exists(_.uniqueSessionId.contains(id)))).getOrElse(false)

      def taxedInterestAmountForm(implicit isAgent: Boolean, previousNames: Seq[String]): Form[TaxedInterestModel] =
        TaxedInterestAmountForm.taxedInterestAmountForm(
          isAgent,
          previousNames,
          sessionIDMatchesPreviouslySubmittedAccount
        )

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
            form = model.fold(taxedInterestAmountForm(user.isAgent, previousNames))(taxedInterestAmountForm(user.isAgent, previousNames).fill),
            taxYear,
            controllers.interest.routes.TaxedInterestAmountController.submit(taxYear, id),
            isAgent = user.isAgent
          ))
        } else {
          Redirect(controllers.interest.routes.TaxedInterestAmountController.show(taxYear, randomUUID().toString))
        }
      }
    }
  }

  def submit(taxYear: Int, id: String): Action[AnyContent] = (authorisedAction andThen journeyFilterAction(taxYear, INTEREST)).async { implicit user =>


    interestSessionService.getSessionData(taxYear).map { cya =>

      val optionalCyaData = cya.flatMap(_.interest)
      val previousNames: Seq[String] = getPreviousNames(optionalCyaData)
      val sessionIDMatchesPreviouslySubmittedAccount: Boolean = optionalCyaData.flatMap(_.taxedUkAccounts.map(_.exists(_.uniqueSessionId.contains(id)))).getOrElse(false)

      def taxedInterestAmountForm(implicit isAgent: Boolean): Form[TaxedInterestModel] = TaxedInterestAmountForm.taxedInterestAmountForm(
        isAgent,
        previousNames,
        sessionIDMatchesPreviouslySubmittedAccount
      )

      taxedInterestAmountForm(user.isAgent).bindFromRequest().fold({
        formWithErrors =>
          Future.successful(BadRequest(taxedInterestAmountView(
            form = formWithErrors,
            taxYear = taxYear,
            postAction = controllers.interest.routes.TaxedInterestAmountController.submit(taxYear, id),
            isAgent = user.isAgent
          )))
      }, {
        completeForm =>
          val newAmount: BigDecimal = completeForm.taxedAmount

          def createNewAccount: InterestAccountModel = InterestAccountModel(None, completeForm.taxedAccountName, newAmount, Some(id))

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

              interestSessionService.updateSessionData(updatedCyaModel, taxYear)(errorHandler.internalServerError())(
                Redirect(controllers.interest.routes.AccountsController.show(taxYear, InterestTaxTypes.TAXED))
              )
            case _ =>
              logger.info("[TaxedInterestController][submit] No CYA data in session. Redirecting to overview page.")
              Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
          }
      })
    }.flatten
  }

  private def getPreviousNames(optionalCyaData: Option[InterestCYAModel]) = {
    optionalCyaData.flatMap(_.taxedUkAccounts.map(_.map(_.accountName))).getOrElse(Seq.empty[String])
  }
}
