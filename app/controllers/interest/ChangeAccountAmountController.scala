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

import common.{InterestTaxTypes, SessionValues}
import config.AppConfig
import controllers.predicates.AuthorisedAction
import forms.PriorOrNewAmountForm
import javax.inject.Inject
import models.formatHelpers.PriorOrNewAmountModel
import models.interest.{InterestAccountModel, InterestCYAModel, InterestPriorSubmission}
import models.User
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.libs.json.{Json, Reads}
import play.api.mvc._
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.interest.ChangeAccountAmountView

class ChangeAccountAmountController @Inject()(
                                             cc: MessagesControllerComponents,
                                             authAction: AuthorisedAction,
                                             changeAccountAmountView: ChangeAccountAmountView,
                                             implicit val appConfig: AppConfig
                                           ) extends FrontendController(cc) with I18nSupport {

  def view(
            formInput: Form[PriorOrNewAmountModel],
            priorSubmission: InterestAccountModel,
            taxYear: Int,
            taxType: String,
            accountId: String,
            preAmount: Option[BigDecimal] = None
          )(implicit user: User[AnyContent]): Html = {

    changeAccountAmountView(
      form = formInput,
      postAction = controllers.interest.routes.ChangeAccountAmountController.submit(taxYear, taxType, accountId),
      taxYear = taxYear,
      taxType = taxType,
      account = priorSubmission,
      preAmount = preAmount
    )
  }

  def show(taxYear: Int, taxType: String, accountId: String): Action[AnyContent] = authAction { implicit user =>
    val interestPriorSubmissionSession = getSessionData[InterestPriorSubmission](SessionValues.INTEREST_PRIOR_SUB)
    val checkYourAnswerSession = getSessionData[InterestCYAModel](SessionValues.INTEREST_CYA)

    val preAmount = extractPreAmount(taxType, checkYourAnswerSession, accountId)

    val singleAccount: Option[InterestAccountModel] = interestPriorSubmissionSession.flatMap { unwrappedPrior =>
      unwrappedPrior.submissions.flatMap { unwrappedAccounts =>
        unwrappedAccounts.find { account =>
          account.id.contains(accountId)
        }
      }
    }

    (singleAccount, checkYourAnswerSession) match {
      case (None, Some(_)) => Redirect(controllers.interest.routes.AccountsController.show(taxYear, taxType))
      case (Some(accountModel), Some(_)) => Ok(view(PriorOrNewAmountForm.priorOrNewAmountForm(accountModel.amount), accountModel, taxYear, taxType, accountId, preAmount))
      case _ => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }
  }

  def submit(taxYear: Int, taxType: String, accountId: String): Action[AnyContent] = authAction { implicit user =>
    val interestPriorSubmissionSession = getSessionData[InterestPriorSubmission](SessionValues.INTEREST_PRIOR_SUB)
    val checkYourAnswerSession = getSessionData[InterestCYAModel](SessionValues.INTEREST_CYA)

    val preAmount = extractPreAmount(taxType, checkYourAnswerSession, accountId)

    val singleAccount: Option[InterestAccountModel] = interestPriorSubmissionSession.flatMap { unwrappedPrior =>
      unwrappedPrior.submissions.flatMap { unwrappedAccounts =>
        unwrappedAccounts.find { account =>
          account.id.contains(accountId)
        }
      }
    }
    checkYourAnswerSession match {
      case Some(cyaData) =>
        singleAccount match {
          case Some(account) => PriorOrNewAmountForm.priorOrNewAmountForm (account.amount).bindFromRequest().fold ( {
            formWithErrors => BadRequest(view(formWithErrors, account, taxYear, taxType, accountId, preAmount))
          }, {
            formModel =>
              import PriorOrNewAmountModel._
              formModel.whichAmount match {
                case `prior` =>
                  val updatedAccounts = updateAccounts(taxType, cyaData, accountId, account.amount)
                  val updatedCYA = replaceAccounts(taxType, cyaData, updatedAccounts)
                  Redirect(controllers.interest.routes.AccountsController.show(taxYear, taxType)).addingToSession(
                    SessionValues.INTEREST_CYA -> updatedCYA.asJsonString
                  )
                case `other` =>
                  val updatedAccounts = updateAccounts(taxType, cyaData, accountId, formModel.amount.get)
                  val updatedCYA = replaceAccounts(taxType, cyaData, updatedAccounts)
                  Redirect(controllers.interest.routes.AccountsController.show(taxYear, taxType)).addingToSession(
                    SessionValues.INTEREST_CYA -> updatedCYA.asJsonString
                  )
              }
          }
          )
          case _ => Redirect(controllers.interest.routes.AccountsController.show(taxYear, taxType))
        }
      case _ => Redirect(controllers.interest.routes.AccountsController.show(taxYear, taxType))
    }
  }

  private[interest] def replaceAccounts(taxType: String, cyaData: InterestCYAModel,  accounts: Option[Seq[InterestAccountModel]]): InterestCYAModel = taxType match {
    case InterestTaxTypes.UNTAXED => cyaData.copy(untaxedUkAccounts = accounts, untaxedUkInterest = Some(true))
    case InterestTaxTypes.TAXED => cyaData.copy(taxedUkAccounts = accounts, taxedUkInterest = Some(true))
  }

  private[interest] def extractPreAmount(taxType: String, checkYourAnswerSession: Option[InterestCYAModel], accountId: String): Option[BigDecimal] = taxType match {
    case InterestTaxTypes.UNTAXED => checkYourAnswerSession.flatMap { unwrappedCya =>
      unwrappedCya.untaxedUkAccounts.flatMap { unwrappedAccounts =>
        unwrappedAccounts.find { account =>
          account.id.contains(accountId) || account.uniqueSessionId.contains(accountId)
        }.map(_.amount)
      }
    }
    case InterestTaxTypes.TAXED => checkYourAnswerSession.flatMap { unwrappedCya =>
      unwrappedCya.taxedUkAccounts.flatMap { unwrappedAccounts =>
        unwrappedAccounts.find { account =>
          account.id.contains(accountId) || account.uniqueSessionId.contains(accountId)
        }.map(_.amount)
      }
    }
  }

  private[interest] def updateAccounts(taxType: String, cya: InterestCYAModel, accountId: String, newAmount: BigDecimal):Option[Seq[InterestAccountModel]] = (taxType) match {
    case InterestTaxTypes.UNTAXED =>
      cya.untaxedUkAccounts.map { unwrappedAccounts =>
        unwrappedAccounts.map { account =>
          if (account.id.contains(accountId) || account.uniqueSessionId.contains(accountId)) {
            account.copy(amount = newAmount)
          } else {
            account
          }
        }
      }

    case InterestTaxTypes.TAXED =>
      cya.taxedUkAccounts.map { unwrappedAccounts =>
        unwrappedAccounts.map { account =>
          if(account.id.contains(accountId) || account.uniqueSessionId.contains(accountId)){
            account.copy(amount = newAmount)
          } else {
            account
          }
        }
      }

  }

  private[interest] def getSessionData[T](key: String)(implicit user: User[_], reads: Reads[T]): Option[T] = {
    user.session.get(key).flatMap { stringValue =>
      Json.parse(stringValue).asOpt[T]
    }
  }

}
