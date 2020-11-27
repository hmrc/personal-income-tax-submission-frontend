/*
 * Copyright 2020 HM Revenue & Customs
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

import common.SessionValues
import config.AppConfig
import controllers.predicates.AuthorisedAction
import javax.inject.Inject
import models.interest.{InterestAccountModel, InterestCYAModel}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.interest.InterestAccountsView

import scala.concurrent.Future

class AccountsController @Inject()(
                                    mcc: MessagesControllerComponents,
                                    view: InterestAccountsView,
                                    authorisedAction: AuthorisedAction,
                                  )(
                                    implicit appConfig: AppConfig
                                  ) extends FrontendController(mcc) with I18nSupport {

  private val TAXED: String = "taxed"
  private val UNTAXED: String = "untaxed"

  private val logger = Logger.logger

  implicit def resultToFutureResult: Result => Future[Result] = baseResult => Future.successful(baseResult)

  //TODO Remove on wire up
  //Replace optionalCyaData in show and submit to view these locally.
  val tempCyaData = Some(InterestCYAModel(
    Some(true), Some(Seq(InterestAccountModel("qwerty", "Untaxed 1", 100))),
    Some(true), Some(Seq(InterestAccountModel("azerty", "Taxed 1", 100)))
  ))

  def show(taxYear: Int, taxType: String): Action[AnyContent] = authorisedAction.async { implicit user =>
    def overviewRedirect = Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    val optionalCyaData: Option[InterestCYAModel] = user.session.get(SessionValues.INTEREST_CYA).flatMap(Json.parse(_).asOpt[InterestCYAModel])

    optionalCyaData match {
      case Some(cyaData) =>
        getTaxAccounts(taxType, cyaData) match {
          case Some(taxAccounts) if taxAccounts.nonEmpty => Ok(view(taxYear, taxAccounts, taxType))
          case _ => missingAccountsRedirect(taxType, taxYear)
        }
      case _ =>
        logger.info("[AccountsController][show] No CYA data in session. Redirecting to the overview page.")
        overviewRedirect
    }
  }

  def submit(taxYear: Int, taxType: String): Action[AnyContent] = authorisedAction.async { implicit user =>
    val optionalCyaData: Option[InterestCYAModel] = user.session.get(SessionValues.INTEREST_CYA).flatMap(Json.parse(_).asOpt[InterestCYAModel])

    optionalCyaData match {
      case Some(cyaData) =>
        getTaxAccounts(taxType, cyaData) match {
          case Some(taxAccounts) if taxAccounts.nonEmpty =>
            if(cyaData.isFinished) {
              Redirect(controllers.interest.routes.InterestCYAController.show(taxYear))
            } else {
              Redirect(controllers.interest.routes.TaxedInterestController.show(taxYear))
            }
          case _ => missingAccountsRedirect(taxType, taxYear)
        }
      case _ =>
        logger.info("[AccountsController][submit] No CYA data in session. Redirecting to the overview page.")
        Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
    }
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

}
