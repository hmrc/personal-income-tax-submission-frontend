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

package models.charity

import models.charity.GiftAidCYAModel.resetDonatedSharesSecuritiesLandOrProperty
import play.api.libs.json.{JsObject, Json}
import utils.UnitTest

class GiftAidCYAModelSpec extends UnitTest {

  val modelMax: GiftAidCYAModel = GiftAidCYAModel(
    Some(true), Some(100.00),
    Some(true), Some(100.00),
    Some(true), Some(100.00), Some(Seq("Belgian Trust", "American Trust")),
    Some(true), Some(100.00),
    Some(true), Some(100.00),
    Some(true), Some(true), Some(100.00), Some(true), Some(100.00),
    Some(true), Some(100.00), Some(Seq("Belgian Trust", "American Trust"))
  )

  val modelMin: GiftAidCYAModel = GiftAidCYAModel()

  val jsonMax: JsObject = Json.obj(
    "donationsViaGiftAid" -> true,
    "donationsViaGiftAidAmount" -> 100,
    "oneOffDonationsViaGiftAid" -> true,
    "oneOffDonationsViaGiftAidAmount" -> 100,
    "overseasDonationsViaGiftAid" -> true,
    "overseasDonationsViaGiftAidAmount" -> 100,
    "overseasCharityNames" -> Json.arr("Belgian Trust", "American Trust"),
    "addDonationToLastYear" -> true,
    "addDonationToLastYearAmount" -> 100,
    "addDonationToThisYear" -> true,
    "addDonationToThisYearAmount" -> 100,
    "donatedSharesSecuritiesLandOrProperty" -> true,
    "donatedSharesOrSecurities" -> true,
    "donatedSharesOrSecuritiesAmount" -> 100,
    "donatedLandOrProperty" -> true,
    "donatedLandOrPropertyAmount" -> 100,
    "overseasDonatedSharesSecuritiesLandOrProperty" -> true,
    "overseasDonatedSharesSecuritiesLandOrPropertyAmount" -> 100,
    "overseasDonatedSharesSecuritiesLandOrPropertyCharityNames" -> Json.arr("Belgian Trust", "American Trust")
  )

  val jsonMin: JsObject = Json.obj(
    "overseasCharityNames" -> Json.arr(),
    "overseasDonatedSharesSecuritiesLandOrPropertyCharityNames" -> Json.arr(),
  )
  val anyBoolean: Boolean = false

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
      "the model is full filled in" in {
        modelMax.isFinished shouldBe true
      }

      "all required values in the model are false" in {
        GiftAidCYAModel(
          donationsViaGiftAid = Some(false),
          addDonationToThisYear = Some(false),
          donatedSharesSecuritiesLandOrProperty = Some(false),
        ).isFinished shouldBe true
      }

      "only donations via gift aid is true" in {
        GiftAidCYAModel(
          donationsViaGiftAid = Some(true), donationsViaGiftAidAmount = Some(100.00),
          oneOffDonationsViaGiftAid = Some(false),
          overseasDonationsViaGiftAid = Some(false),
          addDonationToLastYear = Some(false),
          addDonationToThisYear = Some(false),
          donatedSharesSecuritiesLandOrProperty = Some(false)
        ).isFinished shouldBe true
      }

      "only add donations to this year is true" in {
        GiftAidCYAModel(
          donationsViaGiftAid = Some(false),
          addDonationToThisYear = Some(true),
          addDonationToThisYearAmount = Some(100.00),
          donatedSharesSecuritiesLandOrProperty = Some(false)
        ).isFinished shouldBe true
      }

      "only donated shares securities land or properties is true" when {
        "only donated shares or securities is true" in {
          GiftAidCYAModel(
            donationsViaGiftAid = Some(false),
            addDonationToThisYear = Some(false),
            donatedSharesSecuritiesLandOrProperty = Some(true),
            donatedSharesOrSecurities = Some(true),
            donatedSharesOrSecuritiesAmount = Some(100.00),
            donatedLandOrProperty = Some(false),
            overseasDonatedSharesSecuritiesLandOrProperty = Some(false)
          ).isFinished shouldBe true
        }

        "only donated land or properties is true" in {
          GiftAidCYAModel(
            donationsViaGiftAid = Some(false),
            addDonationToThisYear = Some(false),
            donatedSharesSecuritiesLandOrProperty = Some(true),
            donatedSharesOrSecurities = Some(false),
            donatedLandOrProperty = Some(true),
            donatedLandOrPropertyAmount = Some(100.00),
            overseasDonatedSharesSecuritiesLandOrProperty = Some(false)
          ).isFinished shouldBe true
        }
      }
    }

    "return false" when {
      "only donations via gift aid is true, but there is no amount value" in {
        GiftAidCYAModel(
          donationsViaGiftAid = Some(true),
          oneOffDonationsViaGiftAid = Some(false),
          overseasDonationsViaGiftAid = Some(false),
          addDonationToLastYear = Some(false),
          addDonationToThisYear = Some(false),
          donatedSharesSecuritiesLandOrProperty = Some(false),
          overseasDonatedSharesSecuritiesLandOrProperty = Some(false)
        ).isFinished shouldBe false
      }

      "only one off donations via gift aid is true, but there is no amount value" in {
        GiftAidCYAModel(
          donationsViaGiftAid = Some(false),
          oneOffDonationsViaGiftAid = Some(true),
          overseasDonationsViaGiftAid = Some(false),
          addDonationToLastYear = Some(false),
          addDonationToThisYear = Some(false),
          donatedSharesSecuritiesLandOrProperty = Some(false),
          overseasDonatedSharesSecuritiesLandOrProperty = Some(false)
        ).isFinished shouldBe false
      }

      "only overseas donations via gift aid is true" when {
        "there is no amount of name values" in {
          GiftAidCYAModel(
            donationsViaGiftAid = Some(false),
            oneOffDonationsViaGiftAid = Some(false),
            overseasDonationsViaGiftAid = Some(true),
            addDonationToLastYear = Some(false),
            addDonationToThisYear = Some(false),
            donatedSharesSecuritiesLandOrProperty = Some(false),
            overseasDonatedSharesSecuritiesLandOrProperty = Some(false)
          ).isFinished shouldBe false
        }

        "there is no amount value" in {
          GiftAidCYAModel(
            donationsViaGiftAid = Some(false),
            oneOffDonationsViaGiftAid = Some(false),
            overseasDonationsViaGiftAid = Some(true), overseasCharityNames = Some(Seq("Cyberpunk Performance Help Fund")),
            addDonationToLastYear = Some(false),
            addDonationToThisYear = Some(false),
            donatedSharesSecuritiesLandOrProperty = Some(false),
            overseasDonatedSharesSecuritiesLandOrProperty = Some(false)
          ).isFinished shouldBe false
        }

        "there is no charity names" in {
          GiftAidCYAModel(
            donationsViaGiftAid = Some(false),
            oneOffDonationsViaGiftAid = Some(false),
            overseasDonationsViaGiftAid = Some(true), overseasDonationsViaGiftAidAmount = Some(100.00),
            addDonationToLastYear = Some(false),
            addDonationToThisYear = Some(false),
            donatedSharesSecuritiesLandOrProperty = Some(false),
            overseasDonatedSharesSecuritiesLandOrProperty = Some(false)
          ).isFinished shouldBe false
        }
      }

      "only add donations to last year is true, but there is no amount value" in {
        GiftAidCYAModel(
          donationsViaGiftAid = Some(false),
          oneOffDonationsViaGiftAid = Some(false),
          overseasDonationsViaGiftAid = Some(false),
          addDonationToLastYear = Some(true),
          addDonationToThisYear = Some(false),
          donatedSharesSecuritiesLandOrProperty = Some(false),
          overseasDonatedSharesSecuritiesLandOrProperty = Some(false)
        ).isFinished shouldBe false
      }

      "only add donations to this year is true, but there is no amount value" in {
        GiftAidCYAModel(
          donationsViaGiftAid = Some(false),
          oneOffDonationsViaGiftAid = Some(false),
          overseasDonationsViaGiftAid = Some(false),
          addDonationToLastYear = Some(false),
          addDonationToThisYear = Some(true),
          donatedSharesSecuritiesLandOrProperty = Some(false),
          overseasDonatedSharesSecuritiesLandOrProperty = Some(false)
        ).isFinished shouldBe false
      }

      "only donated shares securities land or properties is true" when {
        "only donated shares or securities is true, but there is no amount value" in {
          GiftAidCYAModel(
            donationsViaGiftAid = Some(false),
            oneOffDonationsViaGiftAid = Some(false),
            overseasDonationsViaGiftAid = Some(false),
            addDonationToLastYear = Some(false),
            addDonationToThisYear = Some(false),
            donatedSharesSecuritiesLandOrProperty = Some(true), donatedSharesOrSecurities = Some(true),
            overseasDonatedSharesSecuritiesLandOrProperty = Some(false)
          ).isFinished shouldBe false
        }

        "only donated land or properties is true, but there is no amount value" in {
          GiftAidCYAModel(
            donationsViaGiftAid = Some(false),
            oneOffDonationsViaGiftAid = Some(false),
            overseasDonationsViaGiftAid = Some(false),
            addDonationToLastYear = Some(false),
            addDonationToThisYear = Some(false),
            donatedSharesSecuritiesLandOrProperty = Some(true), donatedLandOrProperty = Some(true),
            overseasDonatedSharesSecuritiesLandOrProperty = Some(false)
          ).isFinished shouldBe false
        }
      }

      "only overseas donated shares securities land or property is true" when {
        "there is no amount of charity name values" in {
          GiftAidCYAModel(
            donationsViaGiftAid = Some(false),
            oneOffDonationsViaGiftAid = Some(false),
            overseasDonationsViaGiftAid = Some(false),
            addDonationToLastYear = Some(false),
            addDonationToThisYear = Some(false),
            donatedSharesSecuritiesLandOrProperty = Some(false),
            overseasDonatedSharesSecuritiesLandOrProperty = Some(true)
          ).isFinished shouldBe false
        }

        "there is no amount value" in {
          GiftAidCYAModel(
            donationsViaGiftAid = Some(false),
            oneOffDonationsViaGiftAid = Some(false),
            overseasDonationsViaGiftAid = Some(false),
            addDonationToLastYear = Some(false),
            addDonationToThisYear = Some(false),
            donatedSharesSecuritiesLandOrProperty = Some(false),
            overseasDonatedSharesSecuritiesLandOrProperty = Some(true),
            overseasDonatedSharesSecuritiesLandOrPropertyCharityNames = Some(Seq("Whiterun Nezrim Removal Fund"))
          ).isFinished shouldBe false
        }

        "there is no charity names" in {
          GiftAidCYAModel(
            donationsViaGiftAid = Some(false),
            oneOffDonationsViaGiftAid = Some(false),
            overseasDonationsViaGiftAid = Some(false),
            addDonationToLastYear = Some(false),
            addDonationToThisYear = Some(false),
            donatedSharesSecuritiesLandOrProperty = Some(false),
            overseasDonatedSharesSecuritiesLandOrProperty = Some(true),
            overseasDonatedSharesSecuritiesLandOrPropertyAmount = Some(100.00)
          ).isFinished shouldBe false
        }
      }
    }
  }

  "hasAllRequiredAnswers" should {
    "return true" when {
      "maximal data is passed" in {
        GiftAidCYAModel(
          donationsViaGiftAid = Some(true),
          oneOffDonationsViaGiftAid = Some(true),
          overseasDonationsViaGiftAid = Some(true),
          addDonationToLastYear = Some(true),
          addDonationToThisYear = Some(true),
          donatedSharesSecuritiesLandOrProperty = Some(true),
          donatedSharesOrSecurities = Some(true),
          donatedLandOrProperty = Some(true),
          overseasDonatedSharesSecuritiesLandOrProperty = Some(true)
        ).hasAllRequiredAnswers shouldBe true
      }

      "minimal data is passed" in {
        GiftAidCYAModel(
          donationsViaGiftAid = Some(false),
          addDonationToThisYear = Some(false),
          donatedSharesSecuritiesLandOrProperty = Some(false)
        ).hasAllRequiredAnswers shouldBe true
      }
    }

    "return false" when {
      "addDonationToThisYear isEmpty" in {
        GiftAidCYAModel(
          donationsViaGiftAid = Some(true),
          oneOffDonationsViaGiftAid = Some(true),
          overseasDonationsViaGiftAid = Some(true),
          addDonationToLastYear = Some(true),
          addDonationToThisYear = None,
          donatedSharesSecuritiesLandOrProperty = Some(true),
          donatedSharesOrSecurities = Some(true),
          donatedLandOrProperty = Some(true),
          overseasDonatedSharesSecuritiesLandOrProperty = Some(true)
        ).hasAllRequiredAnswers shouldBe false
      }

      "donationsViaGiftAidCompleted is false" in {
        GiftAidCYAModel(
          donationsViaGiftAid = Some(true),
          overseasDonationsViaGiftAid = Some(true),
          addDonationToLastYear = Some(true),
          addDonationToThisYear = Some(true),
          donatedSharesSecuritiesLandOrProperty = Some(true),
          donatedSharesOrSecurities = Some(true),
          donatedLandOrProperty = Some(true),
          overseasDonatedSharesSecuritiesLandOrProperty = Some(true)
        ).hasAllRequiredAnswers shouldBe false
      }

      "donatedSharesSecuritiesLandOrPropertyCompleted is false" in {
        GiftAidCYAModel(
          donationsViaGiftAid = Some(true),
          oneOffDonationsViaGiftAid = Some(true),
          overseasDonationsViaGiftAid = Some(true),
          addDonationToLastYear = Some(true),
          addDonationToThisYear = Some(anyBoolean),
          donatedSharesSecuritiesLandOrProperty = Some(true),
          donatedSharesOrSecurities = None,
          donatedLandOrProperty = Some(true),
          overseasDonatedSharesSecuritiesLandOrProperty = Some(true)
        ).hasAllRequiredAnswers shouldBe false
      }

      "donatedSharesOrSecuritiesCompleted is false" in {
        GiftAidCYAModel(
          donationsViaGiftAid = Some(true),
          oneOffDonationsViaGiftAid = Some(true),
          overseasDonationsViaGiftAid = Some(true),
          addDonationToLastYear = Some(true),
          addDonationToThisYear = Some(anyBoolean),
          donatedSharesSecuritiesLandOrProperty = Some(true),
          donatedSharesOrSecurities = Some(true),
          donatedLandOrProperty = Some(true),
          overseasDonatedSharesSecuritiesLandOrProperty = None
        ).hasAllRequiredAnswers shouldBe false
      }
    }
  }

  "donatedSharesOrSecuritiesCompleted" should {
    "return true" when {
      "donatedSharesSecuritiesLandOrProperty is None" in {
        GiftAidCYAModel(
          donatedSharesSecuritiesLandOrProperty = None
        ).donatedSharesOrSecuritiesCompleted shouldBe true
      }

      "donatedSharesSecuritiesLandOrProperty is false" in {
        GiftAidCYAModel(
          donatedSharesSecuritiesLandOrProperty = Some(false)
        ).donatedSharesOrSecuritiesCompleted shouldBe true
      }

      "donatedSharesOrSecurities is true and overseasDonatedSharesSecuritiesLandOrProperty is nonEmpty" in {
        GiftAidCYAModel(
          donatedSharesSecuritiesLandOrProperty = Some(true),
          donatedSharesOrSecurities = Some(true),
          overseasDonatedSharesSecuritiesLandOrProperty = Some(anyBoolean)
        ).donatedSharesOrSecuritiesCompleted shouldBe true
      }

      "donatedLandOrProperty is true and overseasDonatedSharesSecuritiesLandOrProperty is nonEmpty" in {
        GiftAidCYAModel(
          donatedSharesSecuritiesLandOrProperty = Some(true),
          donatedLandOrProperty = Some(true),
          overseasDonatedSharesSecuritiesLandOrProperty = Some(anyBoolean)
        ).donatedSharesOrSecuritiesCompleted shouldBe true
      }

      "donatedSharesOrSecurities and donatedLandOrProperty are true and overseasDonatedSharesSecuritiesLandOrProperty is nonEmpty" in {
        GiftAidCYAModel(
          donatedSharesSecuritiesLandOrProperty = Some(true),
          donatedSharesOrSecurities = Some(true),
          donatedLandOrProperty = Some(true),
          overseasDonatedSharesSecuritiesLandOrProperty = Some(anyBoolean)
        ).donatedSharesOrSecuritiesCompleted shouldBe true
      }

      "donatedSharesOrSecurities and donatedLandOrProperty are false and overseasDonatedSharesSecuritiesLandOrProperty is empty" in {
        GiftAidCYAModel(
          donatedSharesSecuritiesLandOrProperty = Some(true),
          donatedSharesOrSecurities = Some(false),
          donatedLandOrProperty = Some(false),
          overseasDonatedSharesSecuritiesLandOrProperty = None
        ).donatedSharesOrSecuritiesCompleted shouldBe true
      }
    }

    "return false" when {
      "donatedSharesSecuritiesLandOrProperty is true and " should {
        "donatedSharesOrSecurities is true and overseasDonatedSharesSecuritiesLandOrProperty is empty" in {
          GiftAidCYAModel(
            donatedSharesSecuritiesLandOrProperty = Some(true),
            donatedSharesOrSecurities = Some(true),
            overseasDonatedSharesSecuritiesLandOrProperty = None
          ).donatedSharesOrSecuritiesCompleted shouldBe false
        }

        "donatedLandOrProperty is true and overseasDonatedSharesSecuritiesLandOrProperty is empty" in {
          GiftAidCYAModel(
            donatedSharesSecuritiesLandOrProperty = Some(true),
            donatedLandOrProperty = Some(true),
            overseasDonatedSharesSecuritiesLandOrProperty = None
          ).donatedSharesOrSecuritiesCompleted shouldBe false
        }

        "donatedSharesOrSecurities and donatedLandOrProperty are true and overseasDonatedSharesSecuritiesLandOrProperty is empty" in {
          GiftAidCYAModel(
            donatedSharesSecuritiesLandOrProperty = Some(true),
            donatedSharesOrSecurities = Some(true),
            donatedLandOrProperty = Some(true),
            overseasDonatedSharesSecuritiesLandOrProperty = None
          ).donatedSharesOrSecuritiesCompleted shouldBe false
        }

        "donatedSharesOrSecurities and donatedLandOrProperty are false and overseasDonatedSharesSecuritiesLandOrProperty is nonEmpty" in {
          GiftAidCYAModel(
            donatedSharesSecuritiesLandOrProperty = Some(true),
            donatedSharesOrSecurities = Some(false),
            donatedLandOrProperty = Some(false),
            overseasDonatedSharesSecuritiesLandOrProperty = Some(anyBoolean)
          ).donatedSharesOrSecuritiesCompleted shouldBe false
        }
      }
    }
  }

  "donatedSharesSecuritiesLandOrPropertyCompleted" should {
    "return true" when {
      "donatedSharesSecuritiesLandOrProperty is true && donatedSharesOrSecurities.nonEmpty && donatedLandOrProperty.nonEmpty" in {
        GiftAidCYAModel(
          donatedSharesSecuritiesLandOrProperty = Some(true),
          donatedSharesOrSecurities = Some(anyBoolean),
          donatedLandOrProperty = Some(anyBoolean)
        ).donatedSharesSecuritiesLandOrPropertyCompleted shouldBe true
      }

      "donatedSharesSecuritiesLandOrProperty is false and " +
        "(donatedSharesOrSecurities, donatedLandOrProperty, overseasDonatedSharesSecuritiesLandOrProperty are empty)" in {
        GiftAidCYAModel(
          donatedSharesSecuritiesLandOrProperty = Some(false),
          donatedSharesOrSecurities = None,
          donatedLandOrProperty = None,
          overseasDonatedSharesSecuritiesLandOrProperty = None
        ).donatedSharesSecuritiesLandOrPropertyCompleted shouldBe true
      }
    }

    "return false" when {
      "donatedSharesSecuritiesLandOrProperty is true && (donatedSharesOrSecurities.isEmpty or donatedLandOrProperty.isEmpty" in {
        GiftAidCYAModel(
          donatedSharesSecuritiesLandOrProperty = Some(true),
          donatedSharesOrSecurities = None,
          donatedLandOrProperty = Some(anyBoolean)
        ).donatedSharesSecuritiesLandOrPropertyCompleted shouldBe false
      }

      "donatedSharesSecuritiesLandOrProperty is false and " +
        "(donatedSharesOrSecurities or donatedLandOrProperty or overseasDonatedSharesSecuritiesLandOrProperty is nonEmpty)" in {
        GiftAidCYAModel(
          donatedSharesSecuritiesLandOrProperty = Some(false),
          donatedSharesOrSecurities = Some(anyBoolean),
          donatedLandOrProperty = None,
          overseasDonatedSharesSecuritiesLandOrProperty = None
        ).donatedSharesSecuritiesLandOrPropertyCompleted shouldBe false
      }
    }
  }

  "donationsViaGiftAidCompleted" should {
    "return true" when {
      "donationsViaGiftAid is true and oneOffDonationsViaGiftAid.nonEmpty and overseasDonationsViaGiftAid.nonEmpty and addDonationToLastYear.nonEmpty" in {
        GiftAidCYAModel(
          donationsViaGiftAid = Some(true),
          oneOffDonationsViaGiftAid = Some(anyBoolean),
          overseasDonationsViaGiftAid = Some(anyBoolean),
          addDonationToLastYear = Some(anyBoolean)
        ).donationsViaGiftAidCompleted shouldBe true
      }

      "donationsViaGiftAid is false and oneOffDonationsViaGiftAid.isEmpty and overseasDonationsViaGiftAid.isEmpty and addDonationToLastYear.isEmpty" in {
        GiftAidCYAModel(
          donationsViaGiftAid = Some(false),
          oneOffDonationsViaGiftAid = None,
          overseasDonationsViaGiftAid = None,
          addDonationToLastYear = None
        ).donationsViaGiftAidCompleted shouldBe true
      }
    }

    "return false" when {
      "donationsViaGiftAid is true and (oneOffDonationsViaGiftAid.isEmpty or overseasDonationsViaGiftAid.isEmpty or addDonationToLastYear.isEmpty)" in {
        GiftAidCYAModel(
          donationsViaGiftAid = Some(true),
          oneOffDonationsViaGiftAid = None,
          overseasDonationsViaGiftAid = Some(anyBoolean),
          addDonationToLastYear = Some(anyBoolean)
        ).donationsViaGiftAidCompleted shouldBe false
      }

      "donationsViaGiftAid is false and (oneOffDonationsViaGiftAid.nonEmpty or overseasDonationsViaGiftAid.nonEmpty or addDonationToLastYear.nonEmpty)" in {
        GiftAidCYAModel(
          donationsViaGiftAid = Some(false),
          oneOffDonationsViaGiftAid = Some(anyBoolean),
        ).donationsViaGiftAidCompleted shouldBe false
      }
    }
  }

  "resetDonatedSharesSecuritiesLandOrProperty" should {
    "reset model to correct state" in {
      val model = GiftAidCYAModel(
        donationsViaGiftAid = Some(true),
        oneOffDonationsViaGiftAid = Some(true),
        overseasDonationsViaGiftAid = Some(true),
        addDonationToLastYear = Some(true),
        addDonationToThisYear = Some(true),
        donatedSharesSecuritiesLandOrProperty = Some(true),
        donatedSharesOrSecurities = Some(true),
        donatedLandOrProperty = Some(true),
        overseasDonatedSharesSecuritiesLandOrProperty = Some(true)
      )

      resetDonatedSharesSecuritiesLandOrProperty(model) shouldBe model.copy(
        donatedSharesSecuritiesLandOrProperty = Some(false),
        donatedSharesOrSecurities = None,
        donatedSharesOrSecuritiesAmount = None,
        donatedLandOrProperty = None,
        donatedLandOrPropertyAmount = None,
        overseasDonatedSharesSecuritiesLandOrProperty = None,
        overseasDonatedSharesSecuritiesLandOrPropertyAmount = None,
        overseasDonatedSharesSecuritiesLandOrPropertyCharityNames = Some(Seq.empty[String])
      )
    }
  }
}
