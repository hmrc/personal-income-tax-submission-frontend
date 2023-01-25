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

import models.charity.prior.{GiftAidPaymentsModel, GiftAidSubmissionModel, GiftsModel}
import play.api.libs.json.Json
import utils.UnitTest

class CreateOrAmendGiftAidAuditDetailSpec extends UnitTest {

  val payments: GiftAidPaymentsModel = GiftAidPaymentsModel(Some(856.23), Some(List("name")), Some(856.23), Some(856.23), Some(856.23), Some(856.23))
  val gifts: GiftsModel = GiftsModel(Some(856.23), Some(List("name")), Some(856.23), Some(856.23))


  val body: GiftAidSubmissionModel = GiftAidSubmissionModel(
    Some(payments),
    Some(gifts)
  )

  val prior: GiftAidSubmissionModel = GiftAidSubmissionModel(
    Some(payments),
    Some(gifts)
  )
  private val nino = "AA123456A"
  private val mtditid = "1234567890"
  private val userType = "Individual"
  private val taxYear = 2020


  "writes" when {
    "passed an audit detail model with success tax calculation field" should {
      "produce valid json" in {
        val json = Json.parse(
          s"""{
            "prior": {
              "giftAidPayments": {
                "nonUkCharities": 856.23,
                "nonUkCharitiesCharityNames": [
                  "name"
                ],
                "currentYear": 856.23,
                "currentYearTreatedAsPreviousYear": 856.23,
                "nextYearTreatedAsCurrentYear": 856.23,
                "oneOffCurrentYear": 856.23
              },
              "gifts": {
                "investmentsNonUkCharities": 856.23,
                "investmentsNonUkCharitiesCharityNames": [
                  "name"
                ],
                "sharesOrSecurities": 856.23,
                "landAndBuildings": 856.23
              }
            },
            "body": {
              "giftAidPayments": {
                "nonUkCharities": 856.23,
                "nonUkCharitiesCharityNames": [
                  "name"
                ],
                "currentYear": 856.23,
                "currentYearTreatedAsPreviousYear": 856.23,
                "nextYearTreatedAsCurrentYear": 856.23,
                "oneOffCurrentYear": 856.23
              },
              "gifts": {
                "investmentsNonUkCharities": 856.23,
                "investmentsNonUkCharitiesCharityNames": [
                  "name"
                ],
                "sharesOrSecurities": 856.23,
                "landAndBuildings": 856.23
              }
            },
            "isUpdate": true,
            "nino": "AA123456A",
            "mtditid": "1234567890",
            "userType": "Individual",
            "taxYear": 2020
          }"""
        )
        val model = CreateOrAmendGiftAidAuditDetail(Some(body), Some(prior), isUpdate = true, nino, mtditid, userType, taxYear)
        Json.toJson(model) shouldBe json
      }
    }
  }
}

