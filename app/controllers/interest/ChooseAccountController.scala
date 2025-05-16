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

import common.InterestTaxTypes.{TAXED, UNTAXED}
import common.SessionValues
import config.{AppConfig, ErrorHandler, INTEREST}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import forms.AccountList
import models.interest.{InterestAccountModel, InterestCYAModel}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.{ChooseAccountService, InterestSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.interest.ChooseAccountView

import java.util.UUID.randomUUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChooseAccountController @Inject()(
                                         chooseAccountView: ChooseAccountView,
                                         interestSessionService: InterestSessionService,
                                         chooseAccountService: ChooseAccountService,
                                         errorHandler: ErrorHandler
                                       )(
                                         implicit appConfig: AppConfig,
                                         authorisedAction: AuthorisedAction,
                                         val mcc: MessagesControllerComponents,
                                         ec: ExecutionContext
                                       ) extends FrontendController(mcc) with I18nSupport with Logging {

  private[interest] def form(isAgent: Boolean, taxType: String): Form[String] = {
    val user = if (isAgent) "agent" else "individual"
    if(taxType == TAXED) {
      AccountList.accountListForm("interest.chooseAccount.error.noRadioSelected.taxed." + user,
        Seq(TAXED))
    } else {
      AccountList.accountListForm("interest.chooseAccount.error.noRadioSelected.untaxed." + user,
        Seq(UNTAXED))
    }
  }

  def show(taxYear: Int, taxType: String): Action[AnyContent] = commonPredicates(taxYear, INTEREST).async { implicit user =>
    interestSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>

      checkHittingPageIsValid(taxYear, taxType, cya) {

        val previousAccounts: Set[InterestAccountModel] = chooseAccountService.getPreviousAccounts(cya, prior, taxType)

        val accountForm: Form[String] = form(user.isAgent, taxType)

        if (previousAccounts.isEmpty) {
          redirectToRelevantAmountPage(taxYear, taxType)
        } else {
          Future.successful(Ok(chooseAccountView(accountForm, taxYear, previousAccounts.toSeq, taxType)))
        }
      }
    }

  }

  def submit(taxYear: Int, taxType: String): Action[AnyContent] = (authorisedAction andThen journeyFilterAction(taxYear, INTEREST)).async { implicit user =>
    interestSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>

      checkHittingPageIsValid(taxYear, taxType, cya) {

        val previousAccounts: Set[InterestAccountModel] = chooseAccountService.getPreviousAccounts(cya, prior, taxType)

        form(user.isAgent, taxType).bindFromRequest().fold(
          {
            formWithErrors =>
              Future.successful(BadRequest(chooseAccountView(
                form = formWithErrors,
                taxYear = taxYear,
                previousAccounts.toSeq,
                taxType
              )))
          },
          {
            accountId =>
              if (accountId.equals(SessionValues.ADD_A_NEW_ACCOUNT)) {
                redirectToRelevantAmountPage(taxYear, taxType)
              } else {
                Future(Redirect(controllers.interest.routes.ChangeAccountAmountController.show(taxYear, taxType, accountId)))
              }
          }
        )
      }
    }
  }

  private def redirectToRelevantAmountPage(taxYear: Int, taxType: String, idOverride: Option[String] = None): Future[Result] = {
    if (taxType.equals(UNTAXED)) {
      Future.successful(Redirect(controllers.interest.routes.UntaxedInterestAmountController.show(taxYear, id = idOverride.getOrElse(randomUUID().toString))))
    } else {
      Future.successful(Redirect(controllers.interest.routes.TaxedInterestAmountController.show(taxYear, id = idOverride.getOrElse(randomUUID().toString))))
    }
  }

  private def checkHittingPageIsValid(taxYear: Int, taxType: String, interestCYAModel: Option[InterestCYAModel])(block: => Future[Result]): Future[Result] = {
    if (interestCYAModel.isDefined) {
      if (taxType.equals(UNTAXED) && !interestCYAModel.flatMap(_.untaxedUkInterest).getOrElse(false)) {
        Future.successful(Redirect(controllers.interest.routes.UntaxedInterestController.show(taxYear)))
      } else if (taxType.equals(TAXED) && !interestCYAModel.flatMap(_.taxedUkInterest).getOrElse(false)) {
        Future.successful(Redirect(controllers.interest.routes.TaxedInterestController.show(taxYear)))
      } else {
        block
      }
    } else {
      Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      //Redirect to overview page?
    }
  }
}

