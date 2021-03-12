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
import common.{SessionValues, UUID}
import config.AppConfig
import controllers.predicates.AuthorisedAction
import forms.YesNoForm
import models.interest.{InterestAccountModel, InterestCYAModel}
import play.api.Logger
import play.api.data.Form
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc._
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import utils.InterestSessionHelper
import views.html.interest.InterestAccountsView

import javax.inject.Inject
import scala.concurrent.Future

class AccountsController @Inject()(
                                    mcc: MessagesControllerComponents,
                                    view: InterestAccountsView,
                                    authorisedAction: AuthorisedAction,
                                    uuid: UUID
                                  )(
                                    implicit appConfig: AppConfig
                                  ) extends FrontendController(mcc) with I18nSupport with InterestSessionHelper {

  private val logger = Logger.logger

  implicit def resultToFutureResult: Result => Future[Result] = baseResult => Future.successful(baseResult)

  private def yesNoForm(taxType: String): Form[Boolean] = {
    taxType match {
      case `TAXED` => YesNoForm.yesNoForm ("interest.taxed-uk-interest.errors.noRadioSelected.individual")
      case `UNTAXED` => YesNoForm.yesNoForm ("interest.untaxed-uk-interest.errors.noRadioSelected.individual")
    }
  }

  def show(taxYear: Int, taxType: String): Action[AnyContent] = authorisedAction.async { implicit user =>
    def overviewRedirect = Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))

    val optionalCyaData: Option[InterestCYAModel] = user.session.get(SessionValues.INTEREST_CYA).flatMap(Json.parse(_).asOpt[InterestCYAModel])

    optionalCyaData match {
      case Some(cyaData) =>
        getTaxAccounts(taxType, cyaData) match {
          case Some(taxAccounts) if taxAccounts.nonEmpty => Ok(view(yesNoForm(taxType), taxYear, taxAccounts, taxType))
          case _ => missingAccountsRedirect(taxType, taxYear)
        }
      case _ =>
        logger.info("[AccountsController][show] No CYA data in session. Redirecting to the overview page.")
        overviewRedirect
    }
  }

  def submit(taxYear: Int, taxType: String): Action[AnyContent] = authorisedAction.async { implicit user =>
    val optionalCyaData: Option[InterestCYAModel] = user.session.get(SessionValues.INTEREST_CYA).flatMap(Json.parse(_).asOpt[InterestCYAModel])

    def checkInterestDataIsPresent: Either[Result, InterestCYAModel] = optionalCyaData.toRight{
      logger.info("[AccountsController][submit] No CYA data in session. Redirecting to the overview page.")
      Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }

    def checkTaxAccounts(cyaData: InterestCYAModel) = {
      getTaxAccounts(taxType, cyaData).collect{
        case taxAccounts: Seq[InterestAccountModel] if taxAccounts.nonEmpty => Right(taxAccounts)
      }.getOrElse(Left(missingAccountsRedirect(taxType, taxYear)))
    }

    def checkForm(taxAccounts: Seq[InterestAccountModel]) = {
      yesNoForm(taxType).bindFromRequest().fold(
        { formWithErrors => Left(BadRequest(view(formWithErrors, taxYear, taxAccounts, taxType))) },
        { yesNoModel => Right(yesNoModel) }
      )
    }

    def addAnotherAccount(yesNoModel: Boolean, cyaData: InterestCYAModel) = {
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

    response.merge
  }

  private[interest] def getTaxAccounts(taxType: String, cyaData: InterestCYAModel): Option[Seq[InterestAccountModel]] = {
    taxType match {
      case `TAXED` => cyaData.taxedUkAccounts
      case `UNTAXED` => cyaData.untaxedUkAccounts
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
