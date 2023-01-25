/*
 * Copyright 2023 HM Revenue & Customs
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

import config.{AppConfig, JourneyKey}
import controllers.predicates.JourneyFilterAction.journeyFilterAction
import controllers.predicates.TaxYearAction.taxYearAction
import models.User
import play.api.mvc.{ActionBuilder, AnyContent, MessagesControllerComponents}

import scala.concurrent.ExecutionContext

class CommonPredicates(taxYear: Int, journeyKey: JourneyKey) {
  def predicateList()
                   (implicit authorisedAction: AuthorisedAction,executionContext: ExecutionContext,  appConfig: AppConfig, mcc: MessagesControllerComponents): ActionBuilder[User, AnyContent] =
    authorisedAction andThen
      taxYearAction(taxYear) andThen
      journeyFilterAction(taxYear, journeyKey)
}

object CommonPredicates {
  def commonPredicates(taxYear: Int, journeyKey: JourneyKey)(
    implicit authAction: AuthorisedAction, executionContext: ExecutionContext, appConfig: AppConfig, mcc: MessagesControllerComponents
  ): ActionBuilder[User, AnyContent] = new CommonPredicates(taxYear, journeyKey).predicateList()
}
