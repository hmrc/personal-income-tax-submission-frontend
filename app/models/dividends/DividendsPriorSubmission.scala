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

package models.dividends

import common.SessionValues
import models.User
import play.api.libs.json.{Json, OFormat}

case class DividendsPriorSubmission(
                                     ukDividends: Option[BigDecimal] = None,
                                     otherUkDividends: Option[BigDecimal] = None
                                   ) {
  def asJsonString: String = Json.toJson(this).toString()
}

object DividendsPriorSubmission {
  implicit val formats: OFormat[DividendsPriorSubmission] = Json.format[DividendsPriorSubmission]

  def fromSession()(implicit user: User[_]): Option[DividendsPriorSubmission] = {
    user.session.get(SessionValues.DIVIDENDS_PRIOR_SUB).flatMap(Json.parse(_).asOpt[DividendsPriorSubmission])
  }
}
