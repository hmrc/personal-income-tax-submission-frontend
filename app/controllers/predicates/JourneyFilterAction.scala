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

import config.{AppConfig, JourneyFeatureSwitchKeys}
import models.User
import play.api.mvc.Results.Redirect
import play.api.mvc.{ActionRefiner, Result}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class JourneyFilterAction @Inject()(journeyKey: JourneyFeatureSwitchKeys, taxYear: Int)(
                                     appConfig: AppConfig,
                                     implicit val executionContext: ExecutionContext
                                   ) extends ActionRefiner[User, User] {


  override def refine[A](request: User[A]): Future[Either[Result, User[A]]] = {
    if(appConfig.isJourneyAvailable(journeyKey)) {
      Future.successful(Right(request))
    } else {
      Future.successful(Left(Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))))
    }
  }
}

object JourneyFilterAction {
  def journeyFilterAction(taxYear: Int, journeyKey: JourneyFeatureSwitchKeys)(implicit appConfig: AppConfig, executionContext: ExecutionContext): JourneyFilterAction = {
    new JourneyFilterAction(journeyKey, taxYear)(appConfig, executionContext)
  }
}
