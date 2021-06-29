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

import common.InterestTaxTypes.{TAXED, UNTAXED}
import config.{AppConfig, ErrorHandler, INTEREST}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import forms.AccountList
import models.interest.InterestAccountModel
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.InterestSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.interest.ChooseAccountView

import java.util.UUID.randomUUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChooseAccountController @Inject()(
                                         chooseAccountView: ChooseAccountView,
                                         interestSessionService: InterestSessionService,
                                         errorHandler: ErrorHandler
                                       )(
                                         implicit appConfig: AppConfig,
                                         authorisedAction: AuthorisedAction,
                                         val mcc: MessagesControllerComponents,
                                         ec: ExecutionContext
                                       ) extends FrontendController(mcc) with I18nSupport with SessionHelper with Logging {

  private[interest] def form(isAgent: Boolean, taxType: String): Form[String] = {
    val user = if (isAgent) "agent" else "individual"
    AccountList.accountListForm("interest.chooseAccount.error.noRadioSelected." + user,
      Seq(if (taxType.equals(TAXED)) "taxed" else "untaxed"))
  }

  //TODO Remove tax year from getAndHandle so all previous accounts are returned?
  def show(taxYear: Int, taxType: String): Action[AnyContent] = commonPredicates(taxYear, INTEREST).async { implicit user =>
    interestSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>

      val previousAccounts: Seq[InterestAccountModel] = getPreviousAccounts(cya.flatMap(_.accounts), taxType) ++
        getPreviousAccounts(prior.flatMap(_.submissions), taxType)

      val accountForm: Form[String] = form(user.isAgent, taxType)

      if (previousAccounts.isEmpty){
        redirectToRelevantAmountPage(taxYear, taxType)
      }
      else {
        Future.successful(Ok(chooseAccountView(accountForm, taxYear, previousAccounts, taxType)))
      }
    }

  }

  def submit(taxYear: Int, taxType: String): Action[AnyContent] = (authorisedAction andThen journeyFilterAction(taxYear, INTEREST)).async { implicit user =>
    interestSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>

      val previousAccounts: Seq[InterestAccountModel] =
        getPreviousAccounts(cya.flatMap(_.accounts), taxType) ++ getPreviousAccounts(prior.flatMap(_.submissions), taxType)

      form(user.isAgent, taxType).bindFromRequest().fold(
        {
          formWithErrors =>
            Future.successful(BadRequest(chooseAccountView(
              form = formWithErrors,
              taxYear = taxYear,
              previousAccounts,
              taxType
            )))
        },
        {
          accountName =>
            if (accountName.trim.equals("Add a new account")){
              redirectToRelevantAmountPage(taxYear, taxType)
            } else {
              //TODO should send name or id(what is this ID?)??? to new page created in SASS-984
              //Future.successful(OK(controllers.interest.routes.HowMuchPage(taxYear, id=
              Future.successful(Redirect(controllers.interest.routes.InterestCYAController.show(taxYear)))
            }
        }
      )
    }
  }

  private def getPreviousAccounts(interestAccountModel: Option[Seq[InterestAccountModel]], taxType: String): Seq[InterestAccountModel] = {
    if (taxType.equals(UNTAXED)) {
      interestAccountModel.map(_.filter(!_.untaxedAmount.isDefined)).getOrElse(Seq.empty)
    } else {
      interestAccountModel.map(_.filter(!_.taxedAmount.isDefined)).getOrElse(Seq.empty)
    }
  }

  private def redirectToRelevantAmountPage(taxYear:Int, taxType:String): Future[Result] ={
    if(taxType.equals(UNTAXED)){
      Future.successful(Redirect(controllers.interest.routes.UntaxedInterestAmountController.show(taxYear, id = randomUUID().toString)))
    }else{
      Future.successful(Redirect(controllers.interest.routes.TaxedInterestAmountController.show(taxYear, id = randomUUID().toString)))
    }
  }
}

