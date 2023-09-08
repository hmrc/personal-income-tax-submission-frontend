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

import models.dividends._
import play.api.libs.json.Json
import utils.UnitTest

class CreateOrAmendStockDividendsAuditDetailSpec extends UnitTest {

  val body: StockDividendsCheckYourAnswersModel = StockDividendsCheckYourAnswersModel(
    None,
    ukDividends = Some(true),
    ukDividendsAmount = Some(123.45),
    otherUkDividends = Some(true),
    otherUkDividendsAmount = Some(123.45),
    stockDividends = Some(true),
    stockDividendsAmount = Some(123.45),
    redeemableShares = Some(true),
    redeemableSharesAmount = Some(123.45),
    closeCompanyLoansWrittenOff = Some(true),
    closeCompanyLoansWrittenOffAmount = Some(123.45)
  )

  val prior: DividendsPriorSubmission = DividendsPriorSubmission(
    Some(856.23),
    Some(741.12)
  )

  val stockPrior: StockDividendsPriorSubmission = StockDividendsPriorSubmission(
    submittedOn = Some("2020-06-17T10:53:38Z"),
    foreignDividend = Some(Seq(ForeignDividendModel("BES", Some(2323.56), Some(5435.56), Some(4564.67), Some(true), 4564.67))),
    dividendIncomeReceivedWhilstAbroad = Some(Seq(ForeignDividendModel("CHN", Some(5664.67), Some(5657.56),
      Some(4644.56), Some(true), 4654.56))),
    stockDividend = Some(StockDividendModel(Some("Stock Dividend Customer Reference"), 2525.89)),
    redeemableShares = Some(StockDividendModel(Some("Redeemable Shares Customer Reference"), 3535.56)),
    bonusIssuesOfSecurities = Some(StockDividendModel(Some("Bonus Issues Of Securities Customer Reference"), 5633.67)),
    closeCompanyLoansWrittenOff = Some(StockDividendModel(Some("Close Company Loans WrittenOff Customer Reference"), 6743.23))
  )

  private val nino = "AA123456A"
  private val mtditid = "1234567890"
  private val userType = "Individual"
  private val taxYear = 2020


  "writes" when {
    "passed an audit detail model with success tax calculation field" should {
      "produce valid json" in {
        val json = Json.obj(
          "body" -> Json.obj(
            "ukDividends" -> true,
            "ukDividendsAmount" -> 123.45,
            "otherUkDividends" -> true,
            "otherUkDividendsAmount" -> 123.45,
            "stockDividends" -> true,
            "stockDividendsAmount" -> 123.45,
            "redeemableShares" -> true,
            "redeemableSharesAmount" -> 123.45,
            "closeCompanyLoansWrittenOff" -> true,
            "closeCompanyLoansWrittenOffAmount" -> 123.45
          ),
          "prior" -> Json.obj(
            "ukDividends" -> 856.23,
            "otherUkDividends" -> 741.12
          ),
          "stockDividendsPrior" -> Json.obj(
            "submittedOn" -> "2020-06-17T10:53:38Z",
            "foreignDividend" -> Json.arr(Json.obj(
              "countryCode" -> "BES",
              "amountBeforeTax" -> 2323.56,
              "taxTakenOff" -> 5435.56,
              "specialWithholdingTax" -> 4564.67,
              "foreignTaxCreditRelief" -> true,
              "taxableAmount" -> 4564.67
            )),
            "dividendIncomeReceivedWhilstAbroad" -> Json.arr(Json.obj(
              "countryCode" -> "CHN",
              "amountBeforeTax" -> 5664.67,
              "taxTakenOff" -> 5657.56,
              "specialWithholdingTax" -> 4644.56,
              "foreignTaxCreditRelief" -> true,
              "taxableAmount" -> 4654.56
            )),
            "stockDividend" -> Json.obj(
              "customerReference" -> "Stock Dividend Customer Reference",
              "grossAmount" -> 2525.89
            ),
            "redeemableShares" -> Json.obj(
              "customerReference" -> "Redeemable Shares Customer Reference",
              "grossAmount" -> 3535.56
            ),
            "bonusIssuesOfSecurities" -> Json.obj(
              "customerReference" -> "Bonus Issues Of Securities Customer Reference",
              "grossAmount" -> 5633.67
            ),
            "closeCompanyLoansWrittenOff" -> Json.obj(
              "customerReference" -> "Close Company Loans WrittenOff Customer Reference",
              "grossAmount" -> 6743.23
            )
          ),
          "isUpdate" -> true,
          "nino" -> "AA123456A",
          "mtditid" -> "1234567890",
          "userType" -> "Individual",
          "taxYear" -> 2020
        )

        val model = CreateOrAmendStockDividendsAuditDetail(Some(body), Some(prior), Some(stockPrior), true, nino, mtditid, userType, taxYear)
        Json.toJson(model) shouldBe json
      }
    }
  }
}

