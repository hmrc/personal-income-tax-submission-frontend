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

package models.interest

import play.api.libs.json._

case class InterestAccountSourceModel(id: Option[String],
                                accountName: String,
                                untaxedAmount: Option[BigDecimal] = None,
                                taxedAmount: Option[BigDecimal] = None,
                                uniqueSessionId: Option[String] = None) {

  def getPrimaryId(): Option[String] = {
    if(id.nonEmpty) id else uniqueSessionId
  }
  def hasTaxed: Boolean = taxedAmount.isDefined
  def hasUntaxed: Boolean = untaxedAmount.isDefined
}

object InterestAccountSourceModel {
  implicit val reads: Reads[InterestAccountSourceModel] = Json.reads[InterestAccountSourceModel]
  implicit val writes: Writes[InterestAccountSourceModel] = Writes[InterestAccountSourceModel] { model =>
    JsObject(Json.obj(
      "id" -> model.id,
      "accountName" -> model.accountName,
      "untaxedAmount" -> model.untaxedAmount,
      "taxedAmount" -> model.taxedAmount,
      "uniqueSessionId" -> model.uniqueSessionId
    ).fields.filterNot(_._2 == JsNull))
  }
}
