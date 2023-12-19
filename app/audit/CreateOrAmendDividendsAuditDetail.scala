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

package audit

import models.dividends.{DividendsCheckYourAnswersModel, DividendsPriorAuditModel, DividendsPriorSubmission, StockDividendsCheckYourAnswersModel, StockDividendsPriorSubmission}
import play.api.libs.json.{Json, OWrites}

case class CreateOrAmendDividendsAuditDetail(gateway: Option[Boolean] = None,
                                             ukDividends: Option[Boolean] = None,
                                             ukDividendsAmount: Option[BigDecimal] = None,
                                             otherUkDividends: Option[Boolean] = None,
                                             otherUkDividendsAmount: Option[BigDecimal] = None,
                                             stockDividends: Option[Boolean] = None,
                                             stockDividendsAmount: Option[BigDecimal] = None,
                                             redeemableShares: Option[Boolean] = None,
                                             redeemableSharesAmount: Option[BigDecimal] = None,
                                             closeCompanyLoansWrittenOff: Option[Boolean] = None,
                                             closeCompanyLoansWrittenOffAmount: Option[BigDecimal] = None,
                                             prior: Option[DividendsPriorAuditModel],
                                             isUpdate: Boolean,
                                             nino: String,
                                             mtditid: String,
                                             userType: String,
                                             taxYear: Int)
object CreateOrAmendDividendsAuditDetail {

  def createFromCyaData(cyaData: DividendsCheckYourAnswersModel,
                        priorDividends: Option[DividendsPriorSubmission],
                        priorStockDividends: Option[StockDividendsPriorSubmission],
                        isUpdate: Boolean,
                        nino: String,
                        mtditid: String,
                        userType: String,
                        taxYear: Int): CreateOrAmendDividendsAuditDetail = {
    CreateOrAmendDividendsAuditDetail(
      gateway = cyaData.gateway,
      ukDividends = cyaData.ukDividends,
      ukDividendsAmount = cyaData.ukDividendsAmount,
      otherUkDividends = cyaData.otherUkDividends,
      otherUkDividendsAmount = cyaData.otherUkDividendsAmount,
      prior = if (DividendsPriorAuditModel.createFromPrior(priorDividends, priorStockDividends).isEmpty()) { None
      } else {Some(DividendsPriorAuditModel.createFromPrior(priorDividends, priorStockDividends))},
      isUpdate = isUpdate,
      nino = nino,
      mtditid = mtditid,
      userType = userType,
      taxYear = taxYear
    )
  }
  def createFromStockCyaData(cyaData: StockDividendsCheckYourAnswersModel,
                        priorDividends: Option[DividendsPriorSubmission],
                        priorStockDividends: Option[StockDividendsPriorSubmission],
                        isUpdate: Boolean,
                        nino: String,
                        mtditid: String,
                        userType: String,
                        taxYear: Int): CreateOrAmendDividendsAuditDetail = {
    CreateOrAmendDividendsAuditDetail(
      gateway = cyaData.gateway,
      ukDividends = cyaData.ukDividends,
      ukDividendsAmount = cyaData.ukDividendsAmount,
      otherUkDividends = cyaData.otherUkDividends,
      otherUkDividendsAmount = cyaData.otherUkDividendsAmount,
      stockDividends = cyaData.stockDividends,
      stockDividendsAmount = cyaData.stockDividendsAmount,
      redeemableShares = cyaData.redeemableShares,
      redeemableSharesAmount = cyaData.redeemableSharesAmount,
      closeCompanyLoansWrittenOff = cyaData.closeCompanyLoansWrittenOff,
      closeCompanyLoansWrittenOffAmount = cyaData.closeCompanyLoansWrittenOffAmount,
      prior = if (DividendsPriorAuditModel.createFromPrior(priorDividends, priorStockDividends).isEmpty()) { None
      } else {Some(DividendsPriorAuditModel.createFromPrior(priorDividends, priorStockDividends))},
      isUpdate = isUpdate,
      nino = nino,
      mtditid = mtditid,
      userType = userType,
      taxYear = taxYear
    )
  }

  implicit def writes: OWrites[CreateOrAmendDividendsAuditDetail] = Json.writes[CreateOrAmendDividendsAuditDetail]
}
