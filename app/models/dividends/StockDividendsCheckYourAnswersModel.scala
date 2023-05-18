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

package models.dividends

import models.priorDataModels.StockDividendsPriorDataModel
import play.api.libs.json.{Format, Json, OFormat}
import uk.gov.hmrc.crypto.EncryptedValue

case class StockDividendsCheckYourAnswersModel(
                                                gateway: Option[Boolean] = None,
                                                ukDividends: Option[Boolean] = None,
                                                ukDividendsAmount: Option[BigDecimal] = None,
                                                otherUkDividends: Option[Boolean] = None,
                                                otherUkDividendsAmount: Option[BigDecimal] = None,
                                                stockDividends: Option[Boolean] = None,
                                                stockDividendsAmount: Option[BigDecimal] = None,
                                                redeemableShares: Option[Boolean] = None,
                                                redeemableSharesAmount: Option[BigDecimal] = None,
                                                closeCompanyLoansWrittenOff: Option[Boolean] = None,
                                                closeCompanyLoansWrittenOffAmount: Option[BigDecimal] = None
                                              ) {
  def toDividendsSubmissionModel: DividendsSubmissionModel = {
    DividendsSubmissionModel(
      ukDividends = this.ukDividendsAmount,
      otherUkDividends = this.otherUkDividendsAmount)
  }

  def isFinished: Boolean = {
    // TODO: uncomment when journey completed
    //    checkState(ukDividends, ukDividendsAmount) &&
    //      checkState(otherUkDividends, otherUkDividendsAmount) &&
    checkState(stockDividends, stockDividendsAmount) &&
      checkState(redeemableShares, redeemableSharesAmount) &&
      checkState(closeCompanyLoansWrittenOff, closeCompanyLoansWrittenOffAmount)
  }

  private def checkState(status: Option[Boolean], value: Option[BigDecimal]): Boolean = {
    (status.contains(true) && value.isDefined) || (status.contains(false) && value.isEmpty)
  }
}

object StockDividendsCheckYourAnswersModel {

  implicit val formats: OFormat[StockDividendsCheckYourAnswersModel] = Json.format[StockDividendsCheckYourAnswersModel]

  private[dividends] def priorityOrderOrNone(priority: Option[BigDecimal], other: Option[BigDecimal], yesNoResult: Boolean): Option[BigDecimal] = {
    if (yesNoResult) {
      (priority, other) match {
        case (Some(priorityValue), _) => Some(priorityValue)
        case (None, Some(otherValue)) => Some(otherValue)
        case _ => None
      }
    } else {
      None
    }
  }

  def getCyaModel(cya: Option[StockDividendsCheckYourAnswersModel], prior: Option[StockDividendsPriorDataModel])
  : Option[StockDividendsCheckYourAnswersModel] = {
    (cya, prior) match {
      case (Some(cyaData), Some(priorData)) =>
        val ukDividendsExist = cyaData.ukDividends.getOrElse(priorData.ukDividendsAmount.nonEmpty)
        val otherDividendsExist = cyaData.otherUkDividends.getOrElse(priorData.otherUkDividendsAmount.nonEmpty)
        val stockDividendsExist = cyaData.stockDividends.getOrElse(priorData.stockDividendsAmount.nonEmpty)
        val redeemableSharesExist = cyaData.redeemableShares.getOrElse(priorData.redeemableSharesAmount.nonEmpty)
        val closeCompanyLoansWrittenOffExist = cyaData.closeCompanyLoansWrittenOff.getOrElse(priorData.closeCompanyLoansWrittenOffAmount.nonEmpty)

        val ukDividendsValue: Option[BigDecimal] = priorityOrderOrNone(cyaData.ukDividendsAmount, priorData.ukDividendsAmount, ukDividendsExist)
        val otherDividendsValue: Option[BigDecimal] = priorityOrderOrNone(cyaData.otherUkDividendsAmount, priorData.otherUkDividendsAmount, otherDividendsExist)
        val stockDividendsValue: Option[BigDecimal] = priorityOrderOrNone(cyaData.stockDividendsAmount, priorData.stockDividendsAmount, stockDividendsExist)
        val redeemableSharesValue: Option[BigDecimal] =
          priorityOrderOrNone(cyaData.redeemableSharesAmount, priorData.redeemableSharesAmount, redeemableSharesExist)
        val closeCompanyLoansWrittenOffValue: Option[BigDecimal] =
          priorityOrderOrNone(cyaData.closeCompanyLoansWrittenOffAmount, priorData.closeCompanyLoansWrittenOffAmount, closeCompanyLoansWrittenOffExist)

        Some(StockDividendsCheckYourAnswersModel(
          cyaData.gateway,
          ukDividends = Some(ukDividendsExist),
          ukDividendsAmount = ukDividendsValue,
          otherUkDividends = Some(otherDividendsExist),
          otherUkDividendsAmount = otherDividendsValue,
          stockDividends = Some(stockDividendsExist),
          stockDividendsAmount = stockDividendsValue,
          redeemableShares = Some(redeemableSharesExist),
          redeemableSharesAmount = redeemableSharesValue,
          closeCompanyLoansWrittenOff = Some(closeCompanyLoansWrittenOffExist),
          closeCompanyLoansWrittenOffAmount = closeCompanyLoansWrittenOffValue
        ))
      case (Some(cyaData), _) => Some(cyaData)
      case (None, Some(priorData)) => Some(StockDividendsCheckYourAnswersModel(
        Some(true),
        ukDividends = Some(priorData.ukDividendsAmount.nonEmpty),
        ukDividendsAmount = priorData.ukDividendsAmount,
        otherUkDividends = Some(priorData.otherUkDividendsAmount.nonEmpty),
        otherUkDividendsAmount = priorData.otherUkDividendsAmount,
        stockDividends = Some(priorData.stockDividendsAmount.nonEmpty),
        stockDividendsAmount = priorData.stockDividendsAmount,
        redeemableShares = Some(priorData.redeemableSharesAmount.nonEmpty),
        redeemableSharesAmount = priorData.redeemableSharesAmount,
        closeCompanyLoansWrittenOff = Some(priorData.closeCompanyLoansWrittenOffAmount.nonEmpty),
        closeCompanyLoansWrittenOffAmount = priorData.closeCompanyLoansWrittenOffAmount
      ))
      case _ => None
    }
  }

}

case class EncryptedStockDividendsCheckYourAnswersModel(
                                                         gateway: Option[EncryptedValue] = None,
                                                         ukDividends: Option[EncryptedValue] = None,
                                                         ukDividendsAmount: Option[EncryptedValue] = None,
                                                         otherUkDividends: Option[EncryptedValue] = None,
                                                         otherUkDividendsAmount: Option[EncryptedValue] = None,
                                                         stockDividends: Option[EncryptedValue] = None,
                                                         stockDividendsAmount: Option[EncryptedValue] = None,
                                                         redeemableShares: Option[EncryptedValue] = None,
                                                         redeemableSharesAmount: Option[EncryptedValue] = None,
                                                         closeCompanyLoansWrittenOff: Option[EncryptedValue] = None,
                                                         closeCompanyLoansWrittenOffAmount: Option[EncryptedValue] = None
                                                       )

object EncryptedStockDividendsCheckYourAnswersModel {

  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]
  implicit val formats: Format[EncryptedStockDividendsCheckYourAnswersModel] = Json.format[EncryptedStockDividendsCheckYourAnswersModel]

}
