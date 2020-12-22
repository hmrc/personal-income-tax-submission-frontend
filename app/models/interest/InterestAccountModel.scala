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

import common.InterestTaxTypes
import play.api.libs.json._

case class InterestAccountModel(
                                 id: Option[String],
                                 accountName: String,
                                 amount: BigDecimal,
                                 uniqueSessionId: Option[String] = None,
                                 priorType: Option[String] = None
                               ) {

  def getPrimaryId(): Option[String] = {
    if(id.nonEmpty) id else uniqueSessionId
  }

}

object InterestAccountModel {
  implicit val reads: Reads[InterestAccountModel] = Json.reads[InterestAccountModel]
  implicit val writes: Writes[InterestAccountModel] = Writes[InterestAccountModel] { model =>
    JsObject(Json.obj(
      "id" -> model.id,
      "accountName" -> model.accountName,
      "amount" -> model.amount,
      "uniqueSessionId" -> model.uniqueSessionId
    ).fields.filterNot(_._2 == JsNull))
  }

  private val missingTaxValuesError: JsonValidationError = new JsonValidationError(Seq("No tax values are present."))
  private val bothTaxValuesEntered: JsonValidationError = new JsonValidationError(Seq("Both tax values are present."))

  private[interest] def checkOneExist(untaxed: Option[BigDecimal], taxed: Option[BigDecimal]): Boolean = {
    untaxed.nonEmpty || taxed.nonEmpty
  }

  private[interest] def checkOnlyOneExist(untaxed: Option[BigDecimal], taxed: Option[BigDecimal]): Boolean = {
    untaxed.isEmpty || taxed.isEmpty
  }

  val priorSubmissionReads: Reads[InterestAccountModel] = for {
    accountName <- (__ \ "accountName").read[String]
    uniqueId <- (__ \ "incomeSourceId").read[String]
    untaxedAmount <- (__ \ "untaxedUkInterest").readNullable[BigDecimal]
    taxedAmount <- (__ \ "taxedUkInterest").readNullable[BigDecimal]
      .filter(missingTaxValuesError)(value => checkOneExist(untaxedAmount, value))
      .filter(bothTaxValuesEntered)(value => checkOnlyOneExist(untaxedAmount, value))
  } yield {
    val amount = untaxedAmount.getOrElse(taxedAmount.get)
    val priorType = if (untaxedAmount.nonEmpty) InterestTaxTypes.UNTAXED else InterestTaxTypes.TAXED

    InterestAccountModel(Some(uniqueId), accountName, amount, None, Some(priorType))
  }
}
