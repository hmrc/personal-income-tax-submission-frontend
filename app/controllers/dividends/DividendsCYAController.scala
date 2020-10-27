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

package controllers.dividends

import common.SessionValues
import config.AppConfig
import controllers.predicates.AuthorisedAction
import javax.inject.Inject
import models.{DividendsCheckYourAnswersModel, User}
import play.api.Logger
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.dividends.DividendsCYAView

class DividendsCYAController @Inject()(
                                        mcc: MessagesControllerComponents,
                                        dividendsCyaView: DividendsCYAView,
                                        authorisedAction: AuthorisedAction
                                      )
                                      (
                                        implicit appConfig: AppConfig
                                      ) extends FrontendController(mcc) with I18nSupport {

  lazy val logger = Logger(this.getClass.getName)

  def show(): Action[AnyContent] = authorisedAction { implicit user =>
    val cyaData: Option[DividendsCheckYourAnswersModel] = getCyaData()

    val cyaDataTemp = Some(DividendsCheckYourAnswersModel(
      ukDividends = true,
      Some(5),
      otherDividends = true,
      Some(10)
    ))

    cyaDataTemp.fold {
      logger.debug("[DividendsCYAController][show] Check your answers data missing.")
      Redirect(appConfig.incomeTaxSubmissionOverviewUrl)
    }{
      model => Ok(dividendsCyaView(model))
    }
  }

  def submit(): Action[AnyContent] = authorisedAction { implicit user =>
    val cyaData: Option[DividendsCheckYourAnswersModel] = getCyaData() //TODO ready to be used for the submission call
    Redirect(appConfig.incomeTaxSubmissionOverviewUrl)
  }

  private[controllers] def getCyaData()(implicit user: User[_]): Option[DividendsCheckYourAnswersModel] = {
    user.session.get(SessionValues.DIVIDENDS_CYA).flatMap { stringValue =>
      Json.parse(stringValue).asOpt[DividendsCheckYourAnswersModel]
    }
  }

}
