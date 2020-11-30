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
import models.User
import models.interest.{InterestAccountModel, InterestCYAModel}
import play.api.i18n.I18nSupport
import play.api.libs.json.{Json, Reads}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController
import views.html.interest.InterestCYAView

import scala.concurrent.Future

class InterestCYAController @Inject()(
                                       mcc: MessagesControllerComponents,
                                       authorisedAction: AuthorisedAction,
                                       interestCyaView: InterestCYAView
                                     )
                                     (
                                       implicit appConfig: AppConfig
                                     ) extends FrontendController(mcc) with I18nSupport {

  def show(taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit user =>
    val cyaModel = getSessionData[InterestCYAModel](SessionValues.INTEREST_CYA)

    //TODO Remove on wireup - Use this if reviewing and you want to see the page work
//    val cyaModel = Some(InterestCYAModel(
//      Some(true),
//      Some(Seq(InterestAccountModel("", "WOO", 12345))),
//      Some(false),
//      None
//    ))

    cyaModel match {
      case Some(cyaData) => Future.successful(Ok(interestCyaView(cyaData, taxYear)))
      case _ => Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = authorisedAction.async { implicit user =>
    //TODO Submission logic lives here
    Future.successful(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear)))
  }

  private[interest] def getSessionData[T](key: String)(implicit user: User[_], reads: Reads[T]): Option[T] = {
    user.session.get(key).flatMap { stringValue =>
      Json.parse(stringValue).asOpt[T]
    }
  }

}
