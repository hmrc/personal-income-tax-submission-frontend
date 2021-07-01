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
import config.{AppConfig, ErrorHandler, INTEREST}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import forms.AmountForm
import models.User
import models.interest.{InterestAccountModel, InterestPriorSubmission}
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, Call, MessagesControllerComponents}
import play.api.mvc.Result
import play.twirl.api.Html
import services.InterestSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.helper.form
import views.html.interest.InterestAccountAmountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class InterestAccountAmountController @Inject()(
                                                 implicit val cc: MessagesControllerComponents,
                                                 authAction: AuthorisedAction,
                                                 interestAccountAmountView: InterestAccountAmountView,
                                                 interestSessionService: InterestSessionService,
                                                 errorHandler: ErrorHandler,
                                                 implicit val appConfig: AppConfig,
                                                 ec: ExecutionContext
                                               ) extends FrontendController(cc) with I18nSupport {

  def agentOrIndividual(implicit isAgent: Boolean): String = if (isAgent) "agent" else "individual"

  def interestAccountAmountForm(implicit isAgent: Boolean, taxType: String): Form[BigDecimal] = AmountForm.amountForm(
    emptyFieldKey = "interest.changeAccountAmount.required." + agentOrIndividual,
    wrongFormatKey = "interest.changeAccountAmount.format",
    exceedsMaxAmountKey = "interest.changeAccountAmount.amountMaxLimit",
    emptyFieldArguments = Seq(if (taxType.equals(TAXED)) "taxed" else "untaxed")
  )

  def view(
            formInput: Form[BigDecimal],
            taxYear: Int,
            taxType: String,
            accountName: InterestAccountModel
          )(implicit user: User[AnyContent]): Html = {

    interestAccountAmountView(
      form = formInput,
      taxYear = taxYear,
      taxType = taxType,
      account = accountName
    )
  }

val accountName = InterestAccountModel(None, "Halifax", 0, None, None)

//add id as a parameter
  def show(taxYear: Int, taxType: String): Action[AnyContent] = commonPredicates(taxYear, INTEREST).async { implicit user =>

    val form: Form[BigDecimal] = interestAccountAmountForm(user.isAgent, taxType)

    //check name using id or session id
    // use " account => account.id.contains(accountId) " (taken from ChangeAccountAmountController)
    //checks session data
    //redirect: if there's already a value assigned to the account (interest cya) - check id or session id - check for each tax type
    //pre-pop box with session data - cya session (for change link)

//    interestSessionService.getSessionData(taxYear).map { cya =>
//
//      def accountIdExists(accountId: String, interestAccounts: Option[InterestAccountModel]): Boolean = {
//        interestAccounts.flatMap(_.id.contains(accountId)) {
//          interestAccounts.map(_.accountName).get
//      }
//      val singleAccount: InterestAccountModel = accountIdExists(id, cya)
//
//      interestSessionService.getSessionData(taxYear).map { cya =>
//        val cyaData = cya.flatMap(_.interest)

        Future.successful(Ok(interestAccountAmountView(form, taxYear, taxType, accountName)))

//      }
//    }
  }



  def submit(taxYear: Int, taxType: String): Action[AnyContent] = authAction.async { implicit user =>

    val form: Form[BigDecimal] = interestAccountAmountForm(user.isAgent, taxType)

    form.bindFromRequest().fold({
      formWithErrors => {
      Future.successful(BadRequest(interestAccountAmountView(formWithErrors, taxYear, taxType, accountName)))
    }
  }, {
      bigDecimalAmount => {
        Future.successful(Ok(interestAccountAmountView(form,taxYear, taxType, accountName)))
      }
    })

  }

}
