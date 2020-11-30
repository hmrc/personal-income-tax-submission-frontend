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

package models.interest

import play.api.libs.json.{Json, OFormat}

case class InterestCYAModel(
                             untaxedUkInterest: Option[Boolean],
                             untaxedUkAccounts: Option[Seq[InterestAccountModel]],
                             taxedUkInterest: Option[Boolean],
                             taxedUkAccounts: Option[Seq[InterestAccountModel]]
                           ) {

  def asJsonString: String = Json.toJson(this).toString()

  def isFinished: Boolean = {

    val ukDividendsFinished: Boolean = untaxedUkInterest.exists {
      case true => untaxedUkAccounts.getOrElse(Seq.empty).nonEmpty
      case false => true
    }

    val otherUkDividendsFinished: Boolean = taxedUkInterest.exists {
      case true => taxedUkAccounts.getOrElse(Seq.empty).nonEmpty
      case false => true
    }

    ukDividendsFinished && otherUkDividendsFinished
  }

}

object InterestCYAModel {
  implicit val formats: OFormat[InterestCYAModel] = Json.format[InterestCYAModel]
}
