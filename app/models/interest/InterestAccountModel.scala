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

package models.interest

import play.api.libs.json._
import uk.gov.hmrc.crypto.EncryptedValue

case class InterestAccountModel(
  id: Option[String],
  accountName: String,
  untaxedAmount: Option[BigDecimal] = None,
  taxedAmount: Option[BigDecimal] = None,
  uniqueSessionId: Option[String] = None
) {
  def getPrimaryId(): Option[String] = if (id.nonEmpty) id else uniqueSessionId

  def hasTaxed: Boolean = taxedAmount.isDefined

  def hasUntaxed: Boolean = untaxedAmount.isDefined

  val hasNonZeroData: Boolean =
    !hasTaxed || taxedAmount.exists(_ != 0) ||
    !hasUntaxed || untaxedAmount.exists(_ != 0)
}

object InterestAccountModel {
  implicit val reads: Reads[InterestAccountModel] = Json.reads[InterestAccountModel]
  implicit val writes: Writes[InterestAccountModel] = Writes[InterestAccountModel] { model =>
    JsObject(Json.obj(
      "id" -> model.id,
      "accountName" -> model.accountName,
      "untaxedAmount" -> model.untaxedAmount,
      "taxedAmount" -> model.taxedAmount,
      "uniqueSessionId" -> model.uniqueSessionId
    ).fields.filterNot(_._2 == JsNull))
  }

  val priorSubmissionReads: Reads[InterestAccountModel] = for {
    accountName <- (__ \ "accountName").read[String]
    uniqueId <- (__ \ "incomeSourceId").read[String]
    untaxedAmount <- (__ \ "untaxedUkInterest").readNullable[BigDecimal]
    taxedAmount <- (__ \ "taxedUkInterest").readNullable[BigDecimal]
  } yield {
    InterestAccountModel(Some(uniqueId), accountName, untaxedAmount, taxedAmount, None)
  }
}

case class EncryptedInterestAccountModel(id: Option[EncryptedValue],
                                accountName: EncryptedValue,
                                untaxedAmount: Option[EncryptedValue] = None,
                                taxedAmount: Option[EncryptedValue] = None,
                                uniqueSessionId: Option[EncryptedValue] = None)

object EncryptedInterestAccountModel {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]
  implicit val formats: Format[EncryptedInterestAccountModel] = Json.format[EncryptedInterestAccountModel]
}
