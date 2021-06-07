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

import common.InterestTaxTypes._
import common.SessionValues
import config.{AppConfig, ErrorHandler, INTEREST}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import forms.YesNoForm
import models.User
import models.interest.{InterestAccountModel, InterestCYAModel, InterestPriorSubmission}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc._
import services.InterestSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.interest.RemoveAccountView

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RemoveAccountController @Inject()(
                                         view: RemoveAccountView,
                                         interestSessionService: InterestSessionService,
                                         errorHandler: ErrorHandler
                                       )(
                                         implicit appConfig: AppConfig,
                                         val mcc: MessagesControllerComponents,
                                         authorisedAction: AuthorisedAction,
                                         ec: ExecutionContext
                                       ) extends FrontendController(mcc) with I18nSupport with Logging{


  val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm("interest.remove-account.errors.noRadioSelected")

  implicit def resultToFutureResult: Result => Future[Result] = baseResult => Future.successful(baseResult)

  def show(taxYear: Int, taxType: String, accountId: String): Action[AnyContent] = commonPredicates(taxYear, INTEREST).async { implicit user =>
    def overviewRedirect: Result = Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))

    interestSessionService.getSessionData(taxYear).map { cya =>
      val optionalCyaData: Option[InterestCYAModel] = cya.flatMap(_.interest)
      val priorSubmissionData = user.session.get(SessionValues.INTEREST_PRIOR_SUB).flatMap(Json.parse(_).asOpt[InterestPriorSubmission])

      val isPriorSubmission: Boolean =
        priorSubmissionData.exists(_.submissions.getOrElse(Seq.empty[InterestAccountModel]).map(_.id.contains(accountId)).foldRight(false)(_ || _))

      if (isPriorSubmission) {
        priorAccountExistsRedirect(taxType, taxYear)
      } else {
        optionalCyaData match {
          case Some(cyaData) =>
            getTaxAccounts(taxType, cyaData) match {
              case Some(taxAccounts) if taxAccounts.nonEmpty =>
                useAccount(taxAccounts, accountId, taxType, taxYear) { account =>
                  Ok(view(yesNoForm, taxYear, taxType, account, isLastAccount(taxType, priorSubmissionData, taxAccounts)))
                }
              case _ => missingAccountsRedirect(taxType, taxYear)
            }
          case _ =>
            logger.info("[RemoveAccountController][show] No CYA data in session. Redirecting to the overview page.")
            overviewRedirect

        }
      }
    }
  }

  def submit(taxYear: Int, taxType: String, accountId: String): Action[AnyContent] =
    (authorisedAction andThen journeyFilterAction(taxYear, INTEREST)).async { implicit user =>
      interestSessionService.getAndHandle(taxYear)(errorHandler.futureInternalServerError()) { (cya, prior) =>
        yesNoForm.bindFromRequest().fold(
          formWithErrors =>
            Future.successful(cya match {
              case Some(cyaData) =>
                getTaxAccounts(taxType, cyaData) match {
                  case Some(taxAccounts) if taxAccounts.nonEmpty =>
                    useAccount(taxAccounts, accountId, taxType, taxYear)(account =>
                      BadRequest(view(formWithErrors, taxYear, taxType, account, isLastAccount(taxType, prior, taxAccounts))))
                  case _ => missingAccountsRedirect(taxType, taxYear)
                }
              case _ => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
            }),
          yesNoModel =>
            cya match {
              case Some(cyaData) => removeAccount(taxYear, taxType, accountId, yesNoModel, cyaData, prior)
              case _ =>
                logger.info("[RemoveAccountController][submit] No CYA data in session. Redirecting to the overview page.")
                Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
            }
        )
      }.flatten
    }

  def updateAndRedirect(cyaData: InterestCYAModel, taxYear: Int)(redirect: Result)(implicit user: User[_]): Future[Result] = {
    interestSessionService.updateSessionData(cyaData, taxYear)(errorHandler.internalServerError())(
      redirect
    )
  }

  private[interest] def removeAccount(
                                       taxYear: Int,
                                       taxType: String,
                                       accountId: String,
                                       yesNoModel: Boolean,
                                       cyaData: InterestCYAModel,
                                       prior: Option[InterestPriorSubmission]
                                     )(implicit user: User[_]): Future[Result] = {

    getTaxAccounts(taxType, cyaData) match {
      case Some(taxAccounts) if taxAccounts.nonEmpty =>
        if (yesNoModel) {
          val updatedAccounts = taxAccounts.filterNot(account => account.id.getOrElse(account.uniqueSessionId.getOrElse("")) == accountId)
          if (taxType == UNTAXED) {
            handleUntaxedUpdate(taxYear, taxType, cyaData, prior, updatedAccounts)
          } else {
            handleTaxedUpdate(taxYear, taxType, cyaData, updatedAccounts)
          }
        } else {
          Redirect(controllers.interest.routes.AccountsController.show(taxYear, taxType)).withSession(user.session)
        }
      case _ => missingAccountsRedirect(taxType, taxYear)
    }
  }

  private[interest] def handleTaxedUpdate(taxYear: Int, taxType: String, cyaData: InterestCYAModel, updatedAccounts: Seq[InterestAccountModel])
                                         (implicit user: User[_]): Future[Result] = {
    val updatedCyaData = cyaData.copy(
      taxedUkInterest = Some(updatedAccounts.nonEmpty),
      taxedUkAccounts = Some(updatedAccounts)
    )

    if (updatedAccounts.nonEmpty) {
      updateAndRedirect(updatedCyaData, taxYear)(Redirect(controllers.interest.routes.AccountsController.show(taxYear, taxType)))
    } else {
      updateAndRedirect(updatedCyaData, taxYear)(Redirect(controllers.interest.routes.InterestCYAController.show(taxYear)))
    }
  }

  private[interest] def handleUntaxedUpdate(
                                             taxYear: Int,
                                             taxType: String,
                                             cyaData: InterestCYAModel,
                                             priorData: Option[InterestPriorSubmission],
                                             updatedAccounts: Seq[InterestAccountModel]
                                           )
                                           (implicit user: User[_]): Future[Result] = {

    val updatedCyaData = cyaData.copy(
      untaxedUkInterest = Some(updatedAccounts.nonEmpty),
      untaxedUkAccounts = Some(updatedAccounts)
    )

    val priorTaxedExist: Boolean = priorData.exists(_.hasTaxed)

    if (updatedAccounts.nonEmpty) {
      updateAndRedirect(updatedCyaData, taxYear)(Redirect(controllers.interest.routes.AccountsController.show(taxYear, taxType)))
    } else if (priorTaxedExist) {
      updateAndRedirect(updatedCyaData, taxYear)(Redirect(controllers.interest.routes.InterestCYAController.show(taxYear)))
    } else {
      updateAndRedirect(updatedCyaData, taxYear)(Redirect(controllers.interest.routes.TaxedInterestController.show(taxYear)))
    }
  }

  private[interest] def getTaxAccounts(taxType: String, cyaData: InterestCYAModel): Option[Seq[InterestAccountModel]] = {
    taxType match {
      case `TAXED` => cyaData.taxedUkAccounts
      case _ => cyaData.untaxedUkAccounts
    }
  }

  private[interest] def missingAccountsRedirect(taxType: String, taxYear: Int): Result = {
    logger.info(s"[RemoveAccountController][missingAccountsRedirect] No accounts for tax type '$taxType'.")

    taxType match {
      case `TAXED` => Redirect(controllers.interest.routes.TaxedInterestController.show(taxYear))
      case _ => Redirect(controllers.interest.routes.UntaxedInterestController.show(taxYear))
    }
  }

  private[interest] def priorAccountExistsRedirect(taxType: String, taxYear: Int): Result = {
    logger.info(s"[RemoveAccountController][priorAccountExistsRedirect] Account deletion of prior account attempted.")
    Redirect(controllers.interest.routes.AccountsController.show(taxYear, taxType))
  }


  def useAccount(accounts: Seq[InterestAccountModel], identifier: String, taxType: String, taxYear: Int)(action: InterestAccountModel => Result): Result = {
    accounts.find(account => account.id.getOrElse(account.uniqueSessionId.getOrElse("")) == identifier) match {
      case Some(account) => action(account)
      case _ => missingAccountsRedirect(taxType, taxYear)
    }
  }

  private[interest] def isLastAccount(taxType: String, priorSubmission: Option[InterestPriorSubmission], taxAccounts: Seq[InterestAccountModel]): Boolean = {
    lazy val blankPriorSub = InterestPriorSubmission(hasTaxed = false, hasUntaxed = false, submissions = None)
    taxType match {
      case TAXED =>
        if (priorSubmission.getOrElse(blankPriorSub).hasTaxed) {
          false
        } else {
          taxAccounts.length == 1
        }
      case _ =>
        if (priorSubmission.getOrElse(blankPriorSub).hasUntaxed) {
          false
        } else {
          taxAccounts.length == 1
        }
    }
  }

}
