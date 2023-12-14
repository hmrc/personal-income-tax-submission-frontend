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

import models.savings.{SavingsIncomeCYAModel, SecuritiesModel}
import play.api.libs.json.Json
import utils.UnitTest

class CreateOrAmendSavingsAuditDetailSpec extends UnitTest {

  val body =  SavingsIncomeCYAModel(
    None,
    Some(856.23),
    Some(true),
    Some(741.12)
  )

  val prior = SecuritiesModel(
    Some(856.23),
    741.12,
    Some(741.12)
  )
  private val nino = "AA123456A"
  private val mtditid = "1234567890"
  private val userType = "Individual"
  private val taxYear = 2020


  "writes" when {
    "passed an audit detail model with success tax calculation field" should {
      "produce valid json" in {
        val json = Json.obj(
            "grossAmount" -> 856.23,
            "taxTakenOff" -> true,
            "taxTakenOffAmount" -> 741.12,
          "prior" -> Json.obj(
            "taxTakenOff" -> 856.23,
            "grossAmount" -> 741.12,
            "netAmount" -> 741.12,
          ),
          "isUpdate" -> true,
          "nino" -> "AA123456A",
          "mtditid" -> "1234567890",
          "userType" -> "Individual",
          "taxYear" -> 2020
        )

          val model = CreateOrAmendSavingsAuditDetail(None, Some(856.23), Some(true), Some(741.12), Some(prior), true, nino, mtditid, userType, taxYear)
        Json.toJson(model) shouldBe json
        }
      }
    }
  }

