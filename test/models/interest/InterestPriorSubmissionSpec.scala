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

package models.interest

import common.{InterestTaxTypes, SessionValues}
import models.User
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.mvc.AnyContent
import utils.UnitTest

class InterestPriorSubmissionSpec extends UnitTest {

  val validJsonRead: JsArray = Json.arr(
    Json.obj(
      "accountName" -> "TSB Account",
      "incomeSourceId" -> "qwerty",
      "taxedUkInterest" -> 500
    ),
    Json.obj(
      "accountName" -> "Lloyds Savings",
      "incomeSourceId" -> "azerty",
      "untaxedUkInterest" -> 3000
    )
  )

  val validJsonReadUntaxed: JsArray = Json.arr(
    Json.obj(
      "accountName" -> "Lloyds Savings",
      "incomeSourceId" -> "azerty",
      "untaxedUkInterest" -> 3000
    )
  )

  val validJsonReadTaxed: JsArray = Json.arr(
    Json.obj(
      "accountName" -> "TSB Account",
      "incomeSourceId" -> "qwerty",
      "taxedUkInterest" -> 500
    )
  )

  val validJsonWrite: JsObject = Json.obj(
    "submissions" -> Json.arr(
      Json.obj(
        "id" -> "qwerty",
        "accountName" -> "TSB Account",
        "amount" -> 500
      ),
      Json.obj(
        "id" -> "azerty",
        "accountName" -> "Lloyds Savings",
        "amount" -> 3000
      )
    )
  )

  val validModel = InterestPriorSubmission(
    hasUntaxed = true,
    hasTaxed = true,
    Some(Seq(
      InterestAccountModel(
        Some("qwerty"),
        "TSB Account",
        500.00,
        priorType = Some(InterestTaxTypes.TAXED)
      ),
      InterestAccountModel(
        Some("azerty"),
        "Lloyds Savings",
        3000.00,
        priorType = Some(InterestTaxTypes.UNTAXED)
      )
    ))
  )

  val validModelUntaxed = InterestPriorSubmission(
    hasUntaxed = true,
    hasTaxed = false,
    Some(Seq(
      InterestAccountModel(
        Some("azerty"),
        "Lloyds Savings",
        3000.00,
        priorType = Some(InterestTaxTypes.UNTAXED)
      )
    ))
  )

  val validModelTaxed = InterestPriorSubmission(
    hasUntaxed = false,
    hasTaxed = true,
    Some(Seq(
      InterestAccountModel(
        Some("qwerty"),
        "TSB Account",
        500.00,
        priorType = Some(InterestTaxTypes.TAXED)
      )
    ))
  )

  "should correctly parse from json when both tax types are present" in {
    validJsonRead.as[InterestPriorSubmission] shouldBe validModel
  }

  "should correctly parse from json when only untaxed accounts are present" in {
    validJsonReadUntaxed.as[InterestPriorSubmission] shouldBe validModelUntaxed
  }

  "should correctly parse from json when taxed accounts are present" in {
    validJsonReadTaxed.as[InterestPriorSubmission] shouldBe validModelTaxed
  }

  "should correctly parse from json when no accounts are present" in {
    Json.arr().as[InterestPriorSubmission] shouldBe InterestPriorSubmission(hasUntaxed = false, hasTaxed = false, None)
  }

  "should correctly parse to json" in {
    Json.toJson(validModel) shouldBe validJsonWrite
  }

  ".asJsonString" should {

    "convert the model into a string of json format" when {

      "there are submissions" in {
        InterestPriorSubmission(hasUntaxed = true, hasTaxed = false, Some(Seq(
          InterestAccountModel(
            Some("someId"),
            "An account",
            100.00,
            priorType = Some(InterestTaxTypes.UNTAXED)
          )
        ))).asJsonString shouldBe Json.stringify(Json.obj("submissions" -> Json.arr(Json.obj(
          "id" -> "someId",
          "accountName" -> "An account",
          "amount" -> 100.00
        ))))
      }

    }

    "return an empty string" when {

      "there are no submissions" in {
        InterestPriorSubmission(hasUntaxed = false, hasTaxed = false, None).asJsonString shouldBe Json.stringify(Json.obj())
      }

    }

  }

  ".fromSession" should {

    "return an InterestPriorSubmission" when {

      "there is prior data in session" in {
        val user = User[AnyContent]("1234567890", None, "AA123456A", "Individual", sessionId)(fakeRequest.withSession(
          SessionValues.INTEREST_PRIOR_SUB -> Json.stringify(Json.arr(
            Json.obj(
              "accountName" -> "Account1",
              "incomeSourceId" -> "1234567890",
              "untaxedUkInterest" -> 100.00
            )
          ))
        ))

        InterestPriorSubmission.fromSession()(user) shouldBe Some(InterestPriorSubmission(
          hasUntaxed = true, hasTaxed = false,
          Some(Seq(InterestAccountModel(
            Some("1234567890"), "Account1", 100.00, priorType = Some(InterestTaxTypes.UNTAXED)
          )))
        ))
      }

    }

    "return a None" when {

      "there is no prior data in session" in {
        val user: User[AnyContent] = User[AnyContent]("1234567890", None, "AA123456A", "Individual", sessionId)(fakeRequest)

        InterestPriorSubmission.fromSession()(user) shouldBe None
      }

    }

  }

}
