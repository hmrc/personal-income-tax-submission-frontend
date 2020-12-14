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

package utils

import common.{InterestTaxTypes, SessionValues}
import models.User
import play.api.mvc.Result

trait InterestSessionHelper extends SessionHelper {

  implicit class BetterResult(result: Result) {

    def updateUntaxedAmountRedirect(url: String)(implicit user: User[_]): Result = {
      result.addingToSession(SessionValues.PAGE_BACK_UNTAXED_AMOUNT -> url)
    }

    def updateTaxedAmountRedirect(url: String)(implicit user: User[_]): Result = {
      result.addingToSession(SessionValues.PAGE_BACK_TAXED_AMOUNT -> url)
    }

    def updateAccountsOverviewRedirect(url: String, taxType: String)(implicit user: User[_]): Result = {
      taxType match {
        case InterestTaxTypes.UNTAXED =>
          result.addingToSession(SessionValues.PAGE_BACK_UNTAXED_ACCOUNTS -> url)
        case _ =>
          result.addingToSession(SessionValues.PAGE_BACK_TAXED_ACCOUNTS -> url)
      }
    }

    def updateCyaRedirect(url: String)(implicit user: User[_]): Result = {
      result.addingToSession(SessionValues.PAGE_BACK_CYA -> url)
    }

  }

}
