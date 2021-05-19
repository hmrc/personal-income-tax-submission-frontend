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

package models.charity.prior

import play.api.libs.json.{JsObject, Json}
import utils.UnitTest

class GiftAidSubmissionModelSpec extends UnitTest {

  val validGiftAidPaymentsModel: GiftAidPaymentsModel = GiftAidPaymentsModel(
    nonUkCharitiesCharityNames = Some(List("non uk charity name","non uk charity name 2")),
    currentYear = Some(1000.89),
    oneOffCurrentYear = Some(605.99),
    currentYearTreatedAsPreviousYear = Some(10.21),
    nextYearTreatedAsCurrentYear = Some(999.99),
    nonUkCharities = Some(55.55)
  )

  val validGiftsModel: GiftsModel = GiftsModel(
    investmentsNonUkCharitiesCharityNames = Some(List("charity name")),
    landAndBuildings = Some(10.21),
    sharesOrSecurities = Some(10.21),
    investmentsNonUkCharities = Some(10.21)
  )

  val validGiftAidModel: GiftAidSubmissionModel = GiftAidSubmissionModel(
    Some(validGiftAidPaymentsModel),
    Some(validGiftsModel)
  )

  val validJson: JsObject = Json.obj(
    "giftAidPayments" -> Json.obj(
      "nonUkCharitiesCharityNames" -> Json.arr(
        "non uk charity name",
        "non uk charity name 2"
      ),
      "currentYear" -> 1000.89,
      "oneOffCurrentYear" -> 605.99,
      "currentYearTreatedAsPreviousYear" -> 10.21,
      "nextYearTreatedAsCurrentYear" -> 999.99,
      "nonUkCharities" -> 55.55
    ),
    "gifts" -> Json.obj(
      "investmentsNonUkCharitiesCharityNames" -> Json.arr(
        "charity name"
      ),
      "landAndBuildings" -> 10.21,
      "sharesOrSecurities" -> 10.21,
      "investmentsNonUkCharities" -> 10.21
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
