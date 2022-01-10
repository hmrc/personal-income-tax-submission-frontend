/*
 * Copyright 2022 HM Revenue & Customs
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

package controllers.charity

import config.AppConfig
import models.User
import models.charity.GiftAidCYAModel
import models.charity.prior.GiftAidSubmissionModel
import play.api.mvc.{AnyContent, Result}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendController

trait CharityJourney extends FrontendController {

  def handleRedirect(taxYear: Int, cya: GiftAidCYAModel, prior: Option[GiftAidSubmissionModel], fromShow: Boolean = false)
                    (implicit user: User[AnyContent]): Result

  def redirectToOverview(taxYear: Int)(implicit appConfig: AppConfig): Result =
    Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))

  def redirectToCya(taxYear: Int): Result = Redirect(controllers.charity.routes.GiftAidCYAController.show(taxYear))

  def determineResult(showPage: Result, redirectedResult: Result, fromShow: Boolean): Result = {
    if(fromShow){
      showPage
    } else {
      redirectedResult
    }
  }
}
