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

package services

import models.mongo.{EncryptedDividendsUserDataModel, EncryptedGiftAidUserDataModel, EncryptedInterestUserDataModel}
import utils.{IntegrationTest, SecureGCMCipher}

class EncryptionServiceISpec extends IntegrationTest{

  val service: EncryptionService = app.injector.instanceOf[EncryptionService]
  val encryption: SecureGCMCipher = app.injector.instanceOf[SecureGCMCipher]

  "encryptDividendsUserData" should {

    val data = CompleteDividendsUserData

    "encrypt all the user data apart from the look up ids and timestamp" in {
      val result = service.encryptDividendsUserData(data)
      result shouldBe EncryptedDividendsUserDataModel(
        sessionId = data.sessionId,
        mtdItId = data.mtdItId,
        nino = data.nino,
        taxYear = data.taxYear,
        dividends = result.dividends,
        lastUpdated = data.lastUpdated
      )
    }

    "encrypt the data and decrypt it back to the initial model" in {
      val encryptResult = service.encryptDividendsUserData(data)
      val decryptResult = service.decryptDividendsUserData(encryptResult)

      decryptResult shouldBe data
    }
  }

  "encryptInterestUserData" should {

    val data = CompletedInterestsUserData

    "encrypt all the user data apart from the look up ids and timestamp" in {
      val result = service.encryptInterestUserData(data)
      result shouldBe EncryptedInterestUserDataModel(
        sessionId = data.sessionId,
        mtdItId = data.mtdItId,
        nino = data.nino,
        taxYear = data.taxYear,
        interest = result.interest,
        lastUpdated = data.lastUpdated
      )
    }

    "encrypt the data and decrypt it back to the initial model" in {
      val encryptResult = service.encryptInterestUserData(data)
      val decryptResult = service.decryptInterestUserData(encryptResult)

      decryptResult shouldBe data
    }
  }

  "encryptGiftAidUserData" should {

    val data = CompletedGiftAidUserData

    "encrypt all the user data apart from the look up ids and timestamp" in {
      val result = service.encryptGiftAidUserData(data)
      result shouldBe EncryptedGiftAidUserDataModel(
        sessionId = data.sessionId,
        mtdItId = data.mtdItId,
        nino = data.nino,
        taxYear = data.taxYear,
        giftAid = result.giftAid,
        lastUpdated = data.lastUpdated
      )
    }

    "encrypt the data and decrypt it back to the initial model" in {
      val encryptResult = service.encryptGiftAidUserData(data)
      val decryptResult = service.decryptGiftAidUserData(encryptResult)

      decryptResult shouldBe data
    }
  }

}
