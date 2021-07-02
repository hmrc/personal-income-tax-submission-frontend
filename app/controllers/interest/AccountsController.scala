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
import common.UUID
import config.{AppConfig, ErrorHandler, INTEREST}
import controllers.predicates.AuthorisedAction
import controllers.predicates.CommonPredicates.commonPredicates
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import forms.YesNoForm
import models.interest.{InterestAccountModel, InterestCYAModel}
import play.api.Logging
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.mvc._
import services.InterestSessionService
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.SessionHelper
import views.html.interest.InterestAccountsView
import javax.inject.Inject

import scala.concurrent.{ExecutionContext, Future}

class AccountsController @Inject()(view: InterestAccountsView,
                                   uuid: UUID,
                                   interestSessionService: InterestSessionService)
                                  (implicit appConfig: AppConfig,
                                   val mcc: MessagesControllerComponents,
                                   val authorisedAction: AuthorisedAction,
                                   ec: ExecutionContext,
                                   errorHandler: ErrorHandler) extends FrontendController(mcc) with I18nSupport with SessionHelper with Logging {

  implicit def resultToFutureResult: Result => Future[Result] = baseResult => Future.successful(baseResult)

  val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm("interest.accounts.error.noRadioSelected")

  def show(taxYear: Int, taxType: String): Action[AnyContent] = commonPredicates(taxYear, INTEREST).async { implicit user =>
    def overviewRedirect: Result = Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))

    interestSessionService.getAndHandle(taxYear)(errorHandler.futureInternalServerError()) { (cya, prior) =>
      (cya, prior) match {
        case (Some(cyaData), priorSubmission) =>
          Future(getTaxAccounts(taxType, cyaData) match {
            case Some(taxAccounts) if taxAccounts.nonEmpty =>
              Ok(view(yesNoForm, taxYear, taxAccounts, taxType, isAgent = user.isAgent, priorSubmission))
            case _ => missingAccountsRedirect(taxType, taxYear)
          })
        case _ =>
          logger.info("[AccountsController][show] No CYA data in session. Redirecting to the overview page.")
          Future(overviewRedirect)
      }
    }.flatten
  }

  def submit(taxYear: Int, taxType: String): Action[AnyContent] = (authorisedAction andThen journeyFilterAction(taxYear, INTEREST)).async { implicit user =>

    interestSessionService.getAndHandle(taxYear)(errorHandler.futureInternalServerError()) { (cya, prior) =>

      def checkInterestDataIsPresent: Either[Result, InterestCYAModel] = cya.toRight {
        logger.info("[AccountsController][submit] No CYA data in session. Redirecting to the overview page.")
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }

      def checkTaxAccounts(cyaData: InterestCYAModel): Either[Result, Seq[InterestAccountModel]] = {
        getTaxAccounts(taxType, cyaData).collect {
          case taxAccounts: Seq[InterestAccountModel] if taxAccounts.nonEmpty => Right(taxAccounts)
        }.getOrElse(Left(missingAccountsRedirect(taxType, taxYear)))
      }

      def checkForm(taxAccounts: Seq[InterestAccountModel]): Either[Result, Boolean] = {
        yesNoForm.bindFromRequest().fold(
          { formWithErrors => Left(BadRequest(view(formWithErrors, taxYear, taxAccounts, taxType, isAgent = user.isAgent, prior))) },
          { yesNoModel => Right(yesNoModel) }
        )
      }

      def addAnotherAccount(yesNoModel: Boolean, cyaData: InterestCYAModel): Right[Nothing, Result] = {
        if (yesNoModel) {
          Right(Redirect(getRedirectUrl(taxType, taxYear)))
        } else {
          if (cyaData.isFinished) {
            Right(Redirect(controllers.interest.routes.InterestCYAController.show(taxYear)))
          } else {
            Right(Redirect(controllers.interest.routes.TaxedInterestController.show(taxYear)))
          }
        }
      }

      val response: Either[Result, Result] = for {
        cyaData <- checkInterestDataIsPresent
        taxAccounts <- checkTaxAccounts(cyaData)
        yesNoModel <- checkForm(taxAccounts)
        result <- addAnotherAccount(yesNoModel, cyaData)
      } yield result

      Future(response.merge)
    }.flatten
  }

  private[interest] def getTaxAccounts(taxType: String, cyaData: InterestCYAModel): Option[Seq[InterestAccountModel]] = {
    taxType match {
      case `TAXED` => cyaData.accounts.map(_.filter(_.hasTaxed))
      case `UNTAXED` => cyaData.accounts.map(_.filter(_.hasUntaxed))
    }
  }

  private[interest] def missingAccountsRedirect(taxType: String, taxYear: Int): Result = {
    logger.info(s"[AccountsController][missingAccountsRedirect] No accounts for tax type '$taxType'.")

    taxType match {
      case `TAXED` => Redirect(controllers.interest.routes.TaxedInterestController.show(taxYear))
      case `UNTAXED` => Redirect(controllers.interest.routes.UntaxedInterestController.show(taxYear))
    }
  }

  private[interest] def getRedirectUrl(taxType: String, taxYear: Int): Call = {
    taxType match {
      case `TAXED` => controllers.interest.routes.TaxedInterestAmountController.show(taxYear, uuid.randomUUID)
      case `UNTAXED` => controllers.interest.routes.UntaxedInterestAmountController.show(taxYear, uuid.randomUUID)
    }
  }
}
