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

import common.SessionValues._
import config.AppConfig
import models.User
import play.api.Logger
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, MessagesControllerComponents, Result}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class TaxYearAction @Inject()(taxYear: Int)(
  appConfig: AppConfig,
  val mcc: MessagesControllerComponents
) extends ActionRefiner[User, User] with I18nSupport {

  implicit val executionContext: ExecutionContext = mcc.executionContext
  lazy val logger: Logger = Logger.apply(this.getClass)
  implicit val config: AppConfig = appConfig
  implicit val messagesApi: MessagesApi = mcc.messagesApi

  override def refine[A](request: User[A]): Future[Either[Result, User[A]]] = {
    implicit val implicitRequest: User[A] = request

    Future.successful(
      if (!appConfig.taxYearErrorFeature || taxYear == appConfig.defaultTaxYear) {
        val sameTaxYear = request.session.get(TAX_YEAR).exists(_.toInt == taxYear)

        if (sameTaxYear || !appConfig.taxYearSwitchResetsSession) {
          Right(request)
        } else {
          logger.info("[TaxYearAction][refine] Tax year provided is different than that in session. Redirecting to overview.")
          Left(
            Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
              .addingToSession(TAX_YEAR -> taxYear.toString)
          )
        }
      } else {
        logger.info(s"Invalid tax year, adding default tax year to session")
        Left(Redirect(controllers.routes.TaxYearErrorController.show)
          .addingToSession(TAX_YEAR -> config.defaultTaxYear.toString))
      }
    )
  }

}

object TaxYearAction {
  def taxYearAction(taxYear: Int)(
    implicit appConfig: AppConfig, messages: MessagesControllerComponents
  ): TaxYearAction = new TaxYearAction(taxYear)(appConfig, messages)
}



