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

package models

import common.SessionValues
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Call

case class DividendsCheckYourAnswersModel(
                                           ukDividends: Option[Boolean] = None,
                                           ukDividendsAmount: Option[BigDecimal] = None,
                                           otherUkDividends: Option[Boolean] = None,
                                           otherUkDividendsAmount: Option[BigDecimal] = None
                                    ) {

  def asJsonString: String = Json.toJson(this).toString()

  def isFinished: Boolean = {

    val ukDividendsFinished: Boolean = ukDividends.exists{
      case true => ukDividendsAmount.isDefined
      case false => true
    }

    val otherUkDividendsFinished: Boolean = otherUkDividends.exists{
      case true => otherUkDividendsAmount.isDefined
      case false => true
    }

    ukDividendsFinished && otherUkDividendsFinished
  }

}

object DividendsCheckYourAnswersModel {
  implicit val formats: OFormat[DividendsCheckYourAnswersModel] = Json.format[DividendsCheckYourAnswersModel]

  def fromSession()(implicit user: User[_]): Option[DividendsCheckYourAnswersModel] = {
    user.session.get(SessionValues.DIVIDENDS_CYA).flatMap{ stringValue =>
      Json.parse(stringValue).asOpt[DividendsCheckYourAnswersModel]
    }

  }

}
