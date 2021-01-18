/*
 * Copyright 2021 HM Revenue & Customs
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

import common.InterestTaxTypes
import models.interest.{InterestAccountModel, InterestCYAModel, InterestPriorSubmission}
import play.api.libs.json.Json
import utils.UnitTest

class CreateOrAmendInterestAuditDetailSpec extends UnitTest {

  val body = InterestCYAModel(
    Some(true), Some(Seq(InterestAccountModel(Some("azerty"), "Account 1", 100.01))),
    Some(true), Some(Seq(InterestAccountModel(Some("qwerty"), "Account 2", 9001.01)))
  )

  val prior = InterestPriorSubmission(
    hasUntaxed = true,
    hasTaxed = true,
    Some(Seq(
      InterestAccountModel(
        Some("UntaxedId1"),
        "Untaxed Account",
        100.01,
        priorType = Some(InterestTaxTypes.UNTAXED)
      ),
      InterestAccountModel(
        Some("TaxedId1"),
        "Taxed Account",
        9001.01,
        priorType = Some(InterestTaxTypes.TAXED)
      )
    ))
  )
  private val nino = "AA123456A"
  private val mtditid = "1234567890"
  private val taxYear = 2020


  "writes" when {
    "passed an audit detail model with success tax calculation field" should {
      "produce valid json" in {
        val json = Json.parse(
          s"""{
             |	"body": {
             |		"untaxedUkInterest": true,
             |		"untaxedUkAccounts": [{
             |			"id": "azerty",
             |			"accountName": "Account 1",
             |			"amount": 100.01
             |		}],
             |		"taxedUkInterest": true,
             |		"taxedUkAccounts": [{
             |			"id": "qwerty",
             |			"accountName": "Account 2",
             |			"amount": 9001.01
             |		}]
             |	},
             |	"prior": {
             |		"submissions": [{
             |			"id": "UntaxedId1",
             |			"accountName": "Untaxed Account",
             |			"amount": 100.01
             |		}, {
             |			"id": "TaxedId1",
             |			"accountName": "Taxed Account",
             |			"amount": 9001.01
             |		}]
             |	},
             |	"nino": "AA123456A",
             |	"mtditid": "1234567890",
             |	"taxYear": 2020
             |}""".stripMargin)

          val model = CreateOrAmendInterestAuditDetail(Some(body), Some(prior), nino, mtditid, taxYear)
        Json.toJson(model) shouldBe json
        }
      }
    }
  }

