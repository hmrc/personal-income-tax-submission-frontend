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

import common.InterestTaxTypes.TAXED
import config.{AppConfig, ErrorHandler, INTEREST}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import forms.AmountForm
import models.User
import models.interest.InterestAccountModel
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import play.twirl.api.Html
import services.{ChangeAccountAmountService, InterestSessionService}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.interest.ChangeAccountAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class ChangeAccountAmountController @Inject()(
                                               implicit val cc: MessagesControllerComponents,
                                               authAction: AuthorisedAction,
                                               changeAccountAmountView: ChangeAccountAmountView,
                                               interestSessionService: InterestSessionService,
                                               changeAccountAmountService: ChangeAccountAmountService,
                                               errorHandler: ErrorHandler,
                                               implicit val appConfig: AppConfig,
                                               ec: ExecutionContext
                                             ) extends FrontendController(cc) with I18nSupport {

  def agentOrIndividual(implicit isAgent: Boolean): String = if (isAgent) "agent" else "individual"

  def changeAmountForm(implicit isAgent: Boolean, taxType: String): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "interest.changeAccountAmount.required." + agentOrIndividual,
    wrongFormatKey = "interest.changeAccountAmount.format",
    exceedsMaxAmountKey = "interest.changeAccountAmount.amountMaxLimit",
    emptyFieldArguments = Seq(if (taxType.equals(TAXED)) "taxed" else "untaxed")
  )

  def view(formInput: Form[BigDecimal],
           priorSubmission: InterestAccountModel,
           taxYear: Int,
           taxType: String,
           accountId: String,
           preAmount: Option[BigDecimal] = None)(implicit user: User[AnyContent]): Html = {

    changeAccountAmountView(
      form = preAmount.fold(formInput)(formInput.fill),
      postAction = controllers.interest.routes.ChangeAccountAmountController.submit(taxYear, taxType, accountId),
      taxYear = taxYear,
      taxType = taxType,
      account = priorSubmission,
      preAmount = preAmount)
  }

  def show(taxYear: Int, taxType: String, accountId: String): Action[AnyContent] = commonPredicates(taxYear, INTEREST).async { implicit user =>
    interestSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>
      val singleAccount: Option[InterestAccountModel] = changeAccountAmountService.getSingleAccount(accountId, prior, cya)

      (singleAccount, cya) match {
        case (None, Some(_)) => Future(Redirect(controllers.interest.routes.AccountsController.show(taxYear, taxType)))
        case (Some(accountModel), Some(cya)) =>

          val previousCYAAmount: Option[BigDecimal] = changeAccountAmountService.extractPreAmount(taxType, cya, accountId)

          val priorValue: Option[BigDecimal] = {
            if(previousCYAAmount.isDefined){
              previousCYAAmount
            } else if (changeAccountAmountService.priorAmount(accountModel, taxType).isDefined){
              changeAccountAmountService.priorAmount(accountModel, taxType)
            } else {
              None
            }
          }

          val form: Form[BigDecimal] = {
            if (previousCYAAmount == changeAccountAmountService.priorAmount(accountModel, taxType)) {
              changeAmountForm(user.isAgent, taxType)
            } else {
              priorValue.fold(changeAmountForm(user.isAgent, taxType))(changeAmountForm(user.isAgent, taxType).fill(_))
            }
          }

          Future(Ok(view(form, accountModel, taxYear, taxType, accountId, priorValue)))

        case _ => Future(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
      }
    }
  }

  def submit(taxYear: Int, taxType: String, accountId: String): Action[AnyContent] = authAction.async { implicit user =>
    interestSessionService.getAndHandle(taxYear)(errorHandler.internalServerError()) { (cya, prior) =>
      val singleAccount: Option[InterestAccountModel] = changeAccountAmountService.getSingleAccount(accountId, prior, cya)

      cya match {
        case Some(cyaData) =>
          singleAccount match {
            case Some(account) =>

              val previousAmount = changeAccountAmountService.extractPreAmount(taxType, cyaData, accountId)

              changeAmountForm(user.isAgent, taxType).bindFromRequest().fold(
                formWithErrors => {
                  Future(BadRequest(view(formWithErrors, account, taxYear, taxType, accountId, previousAmount)))
                },
                formAmount => {
                  val updatedAccounts = changeAccountAmountService.updateAccounts(taxType, cyaData, prior, accountId, formAmount)
                  val updatedCYA = changeAccountAmountService.replaceAccounts(taxType, cyaData, updatedAccounts)

                  interestSessionService.updateSessionData(updatedCYA, taxYear)(errorHandler.futureInternalServerError())(
                    Redirect(controllers.interest.routes.AccountsController.show(taxYear, taxType))
                  )
                }
              )
            case _ => Future(Redirect(controllers.interest.routes.AccountsController.show(taxYear, taxType)))
          }
        case _ => Future(Redirect(controllers.interest.routes.AccountsController.show(taxYear, taxType)))
      }
    }
  }
}
