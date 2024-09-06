/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.dividendsBase

import com.google.inject.Inject
import config.AppConfig
import controllers.dividends.OtherUkDividendsAmountController
import controllers.dividendsSplit.OtherUkDividendsAmountSplitController
import play.api.mvc.{Action, AnyContent}

class OtherUkDividendsAmountBaseController @Inject()(linearController: OtherUkDividendsAmountController,
                                                     splitController: OtherUkDividendsAmountSplitController,
                                                     appConfig: AppConfig) {

  def show(taxYear: Int): Action[AnyContent] = {
    if (appConfig.isSplitDividends) {
      splitController.show(taxYear)
    } else {
      linearController.show(taxYear)
    }
  }

  def submit(taxYear: Int): Action[AnyContent] = {
    if (appConfig.isSplitDividends) {
      splitController.submit(taxYear)
    } else {
      linearController.submit(taxYear)
    }
  }
}
