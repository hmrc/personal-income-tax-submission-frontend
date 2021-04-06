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

package models.giftAid

import play.api.libs.json.{JsObject, Json}
import utils.UnitTest

class GiftAidSubmissionModelSpec extends UnitTest {

  val validGiftAidPaymentsModel: GiftAidPaymentsModel = GiftAidPaymentsModel(
    nonUkCharitiesCharityNames = List("non uk charity name","non uk charity name 2"),
    currentYear = 2022,
    oneOffCurrentYear = 2021,
    currentYearTreatedAsPreviousYear = 2021,
    nextYearTreatedAsCurrentYear = 2023,
    nonUkCharities = 5
  )

  val validGiftsModel: GiftsModel = GiftsModel(
    investmentsNonUkCharitiesCharityNames = List("charity name"),
    landAndBuildings = 10,
    sharesOrSecurities = 10,
    investmentsNonUkCharities = 10
  )

  val validGiftAidModel: GiftAidSubmissionModel = GiftAidSubmissionModel(
    validGiftAidPaymentsModel,
    validGiftsModel
  )

  val validJson: JsObject = Json.obj(
    "giftAidPayments" -> Json.obj(
      "nonUkCharitiesCharityNames" -> Json.arr(
        "non uk charity name",
        "non uk charity name 2"
      ),
      "currentYear" -> 2022,
      "oneOffCurrentYear" -> 2021,
      "currentYearTreatedAsPreviousYear" -> 2021,
      "nextYearTreatedAsCurrentYear" -> 2023,
      "nonUkCharities" -> 5
    ),
    "gifts" -> Json.obj(
      "investmentsNonUkCharitiesCharityNames" -> Json.arr(
        "charity name"
      ),
      "landAndBuildings" -> 10,
      "sharesOrSecurities" -> 10,
      "investmentsNonUkCharities" -> 10
    )
  )

  "GiftAidSubmission" should {

    "parse from json" in {
      validJson.as[GiftAidSubmissionModel] shouldBe validGiftAidModel
    }

    "parse to json" in {
      Json.toJson(validGiftAidModel) shouldBe validJson
    }

  }

}
