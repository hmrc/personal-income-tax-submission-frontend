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

package controllers.predicates

import common.SessionValues
import config.AppConfig
import play.api.Logger.logger
import play.api.mvc.{Request, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

import scala.concurrent.Future

trait TaxYearFilter {
  appConfig: FrontendController =>

  private[controllers] def taxYearFilterFuture(taxYear: Int)(f: => Future[Result])(implicit request: Request[_], appConfig: AppConfig): Future[Result] = {

    val config = appConfig.defaultTaxYear

    if (taxYear == config || !appConfig.taxYearErrorFeature) {

      f

    } else {

      logger.info(s"Invalid tax year, adding default tax year to session")
      Future.successful(Redirect(controllers.routes.TaxYearErrorController.show()).addingToSession(SessionValues.TAX_YEAR -> config.toString))
    }
  }

  private[controllers] def taxYearFilter(taxYear: Int)(f: => Result)(implicit request: Request[_], appConfig: AppConfig): Result = {

    val config = appConfig.defaultTaxYear

    if (taxYear == config || !appConfig.taxYearErrorFeature) {

      f

      } else {

        logger.info(s"Invalid tax year, adding default tax year to session")
        Redirect(controllers.routes.TaxYearErrorController.show()).addingToSession(SessionValues.TAX_YEAR -> config.toString)
      }
    }
}
