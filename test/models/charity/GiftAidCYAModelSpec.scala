/*
 * Copyright 2022 HM Revenue & Customs
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

package models.charity

import common.UUID
import play.api.libs.json.{JsObject, Json}
import utils.UnitTest

class GiftAidCYAModelSpec extends UnitTest {

  private val belgianTrustId: String = UUID().randomUUID
  private val americanTrustId: String = UUID().randomUUID

  val modelMax: GiftAidCYAModel = GiftAidCYAModel(
    Some(true),
    Some(true), Some(100.00),
    Some(true), Some(100.00),
    Some(true), Some(100.00), Seq(CharityNameModel(belgianTrustId, "Belgian Trust"), CharityNameModel(americanTrustId, "American Trust")),
    Some(true), Some(100.00),
    Some(true), Some(100.00),
    Some(true), Some(100.00), Some(true), Some(100.00),
    Some(true), Some(100.00), Seq(CharityNameModel(belgianTrustId, "Belgian Trust"), CharityNameModel(americanTrustId, "American Trust"))
  )

  val modelMin: GiftAidCYAModel = GiftAidCYAModel()

  val jsonMax: JsObject = Json.obj(
    "gateway" -> true,
    "donationsViaGiftAid" -> true,
    "donationsViaGiftAidAmount" -> 100,
    "oneOffDonationsViaGiftAid" -> true,
    "oneOffDonationsViaGiftAidAmount" -> 100,
    "overseasDonationsViaGiftAid" -> true,
    "overseasDonationsViaGiftAidAmount" -> 100,
    "overseasCharityNames" -> Json.arr(
      Json.obj("id" -> belgianTrustId, "name" -> "Belgian Trust"),
      Json.obj("id" -> americanTrustId, "name" -> "American Trust")
    ),
    "addDonationToLastYear" -> true,
    "addDonationToLastYearAmount" -> 100,
    "addDonationToThisYear" -> true,
    "addDonationToThisYearAmount" -> 100,
    "donatedSharesOrSecurities" -> true,
    "donatedSharesOrSecuritiesAmount" -> 100,
    "donatedLandOrProperty" -> true,
    "donatedLandOrPropertyAmount" -> 100,
    "overseasDonatedSharesSecuritiesLandOrProperty" -> true,
    "overseasDonatedSharesSecuritiesLandOrPropertyAmount" -> 100,
    "overseasDonatedSharesSecuritiesLandOrPropertyCharityNames" -> Json.arr(
      Json.obj("id" -> belgianTrustId, "name" -> "Belgian Trust"),
      Json.obj("id" -> americanTrustId, "name" -> "American Trust")
    )
  )

  val jsonMin: JsObject = Json.obj(
    "overseasCharityNames" -> Json.arr(),
    "overseasDonatedSharesSecuritiesLandOrPropertyCharityNames" -> Json.arr(),
  )

  "GiftAidCYAModel" should {

    "correctly parse to Json" when {

      "the model is fully filled out" in {
        Json.toJson(modelMax) shouldBe jsonMax
      }

      "the model is empty" in {
        Json.toJson(modelMin) shouldBe jsonMin
      }

    }

    "correctly parse to a model" when {

      "the json contains all the data for the model" in {
        jsonMax.as[GiftAidCYAModel] shouldBe modelMax
      }

      "the json contains no data" in {
        jsonMin.as[GiftAidCYAModel] shouldBe modelMin
      }

    }

  }

  "isFinished" should {

    "return true" when {

      "the model is fully filled in" in {
        modelMax.isFinished shouldBe true
      }

      "all required values in the model are false" in {
        GiftAidCYAModel(
          gateway = Some(false),
          donationsViaGiftAid = Some(false),
          addDonationToThisYear = Some(false),
          donatedSharesOrSecurities = Some(false),
          donatedLandOrProperty = Some(false)
        ).isFinished shouldBe true
      }

      "only donations via gift aid is true" in {
        GiftAidCYAModel(
          gateway = Some(true),
          donationsViaGiftAid = Some(true), donationsViaGiftAidAmount = Some(100.00),
          oneOffDonationsViaGiftAid = Some(false),
          overseasDonationsViaGiftAid = Some(false),
          addDonationToLastYear = Some(false),
          addDonationToThisYear = Some(false),
          donatedSharesOrSecurities = Some(false),
          donatedLandOrProperty = Some(false)
        ).isFinished shouldBe true
      }

      "only add donations to this year is true" in {
        GiftAidCYAModel(
          gateway = Some(true),
          donationsViaGiftAid = Some(false),
          addDonationToThisYear = Some(true), addDonationToThisYearAmount = Some(100.00),
          donatedSharesOrSecurities = Some(false),
          donatedLandOrProperty = Some(false)
        ).isFinished shouldBe true
      }
      
      "the gateway question is set to false, and no other value is present" in {
        GiftAidCYAModel(Some(false)).isFinished shouldBe true
      }
    }

    "return false" when {

      "only donations via gift aid is true, but there is no amount value" in {
        GiftAidCYAModel(
          gateway = Some(true),
          donationsViaGiftAid = Some(true),
          oneOffDonationsViaGiftAid = Some(false),
          overseasDonationsViaGiftAid = Some(false),
          addDonationToLastYear = Some(false),
          addDonationToThisYear = Some(false),
          donatedSharesOrSecurities = Some(false),
          donatedLandOrProperty = Some(false),
          overseasDonatedSharesSecuritiesLandOrProperty = Some(false)
        ).isFinished shouldBe false
      }

      "only one off donations via gift aid is true, but there is no amount value" in {
        GiftAidCYAModel(
          gateway = Some(true),
          donationsViaGiftAid = Some(false),
          oneOffDonationsViaGiftAid = Some(true),
          overseasDonationsViaGiftAid = Some(false),
          addDonationToLastYear = Some(false),
          addDonationToThisYear = Some(false),
          donatedSharesOrSecurities = Some(false),
          donatedLandOrProperty = Some(false),
          overseasDonatedSharesSecuritiesLandOrProperty = Some(false)
        ).isFinished shouldBe false
      }

      "only overseas donations via gift aid is true" when {

        "there is no amount of name values" in {
          GiftAidCYAModel(
            gateway = Some(true),
            donationsViaGiftAid = Some(false),
            oneOffDonationsViaGiftAid = Some(false),
            overseasDonationsViaGiftAid = Some(true),
            addDonationToLastYear = Some(false),
            addDonationToThisYear = Some(false),
            donatedSharesOrSecurities = Some(false),
            donatedLandOrProperty = Some(false),
            overseasDonatedSharesSecuritiesLandOrProperty = Some(false)
          ).isFinished shouldBe false
        }

        "there is no amount value" in {
          GiftAidCYAModel(
            gateway = Some(true),
            donationsViaGiftAid = Some(false),
            oneOffDonationsViaGiftAid = Some(false),
            overseasDonationsViaGiftAid = Some(true),
            overseasCharityNames = Seq(CharityNameModel("Cyberpunk Performance Help Fund")),
            addDonationToLastYear = Some(false),
            addDonationToThisYear = Some(false),
            donatedSharesOrSecurities = Some(false),
            donatedLandOrProperty = Some(false),
            overseasDonatedSharesSecuritiesLandOrProperty = Some(false)
          ).isFinished shouldBe false
        }

        "there is no charity names" in {
          GiftAidCYAModel(
            gateway = Some(true),
            donationsViaGiftAid = Some(false),
            oneOffDonationsViaGiftAid = Some(false),
            overseasDonationsViaGiftAid = Some(true), overseasDonationsViaGiftAidAmount = Some(100.00),
            addDonationToLastYear = Some(false),
            addDonationToThisYear = Some(false),
            donatedSharesOrSecurities = Some(false),
            donatedLandOrProperty = Some(false),
            overseasDonatedSharesSecuritiesLandOrProperty = Some(false)
          ).isFinished shouldBe false
        }
      }

      "only add donations to last year is true, but there is no amount value" in {
        GiftAidCYAModel(
          gateway = Some(true),
          donationsViaGiftAid = Some(false),
          oneOffDonationsViaGiftAid = Some(false),
          overseasDonationsViaGiftAid = Some(false),
          addDonationToLastYear = Some(true),
          addDonationToThisYear = Some(false),
          donatedSharesOrSecurities = Some(false),
          donatedLandOrProperty = Some(false)
        ).isFinished shouldBe false
      }

      "only add donations to this year is true, but there is no amount value" in {
        GiftAidCYAModel(
          gateway = Some(true),
          donationsViaGiftAid = Some(false),
          addDonationToThisYear = Some(true),
          donatedSharesOrSecurities = Some(false),
          donatedLandOrProperty = Some(false)
        ).isFinished shouldBe false
      }

      "only donated shares or securities is true" when {

        "only donated shares or securities is true, but there is no amount value" in {
          GiftAidCYAModel(
            gateway = Some(true),
            donationsViaGiftAid = Some(false),
            donatedSharesOrSecurities = Some(true),
            donatedLandOrProperty = Some(false)
          ).isFinished shouldBe false
        }


      }

      "only donated land or property is true" when {

        "only donated land or property is true, but there is no amount value" in {
          GiftAidCYAModel(
            gateway = Some(true),
            donationsViaGiftAid = Some(false),
            donatedSharesOrSecurities = Some(false),
            donatedLandOrProperty = Some(true)
          ).isFinished shouldBe false
        }
      }
      
      "the gateway question is true, but no other field is filled in" in {
        GiftAidCYAModel(Some(true)).isFinished shouldBe false
      }

    }

  }

}
