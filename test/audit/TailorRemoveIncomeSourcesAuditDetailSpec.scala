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

import config.{DIVIDENDS, INTEREST}
import play.api.libs.json.Json
import utils.UnitTest

class TailorRemoveIncomeSourcesAuditDetailSpec extends UnitTest {

  private val IncomeSources = Seq(DIVIDENDS.stringify, INTEREST.stringify)

  private val nino = "AA000010A"
  private val mtditid = "1234567890"
  private val userType = "individual"
  private val taxYear = 2022

  "writes" when {
    IncomeSources.foreach { incomeSource =>
      s"passed an audit detail model with sources removed body field containing '$incomeSource'" should {
        "produce valid json" in {
          val json = Json.obj(
            "nino" -> nino,
            "mtditid" -> mtditid,
            "userType" -> userType,
            "taxYear" -> taxYear,
            "body" -> Json.obj(
              "SourcesRemoved" -> Seq(incomeSource)
            ),
          )

          val model = TailorRemoveIncomeSourcesAuditDetail(nino, mtditid, userType, taxYear, TailorRemoveIncomeSourcesBody(Seq(incomeSource)))
          Json.toJson(model) shouldBe json
        }
      }
    }
  }
}
