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

package services

import config.ErrorHandler
import connectors.{IncomeSourceConnector, IncomeTaxUserDataConnector}
import repositories.GiftAidUserDataRepository
import utils.IntegrationTest


class GiftAidSessionServiceISpec extends IntegrationTest{

  val giftAidUserDataRepository: GiftAidUserDataRepository = app.injector.instanceOf[GiftAidUserDataRepository]
  val incomeTaxUserDataConnector: IncomeTaxUserDataConnector = app.injector.instanceOf[IncomeTaxUserDataConnector]
  val incomeSourceConnector: IncomeSourceConnector = app.injector.instanceOf[IncomeSourceConnector]

  val giftAidSessionServiceInvalidEncryption: GiftAidSessionService = appWithInvalidEncryptionKey.injector.instanceOf[GiftAidSessionService]
  val giftAidSessionService: GiftAidSessionService = new GiftAidSessionService(giftAidUserDataRepository, incomeTaxUserDataConnector, incomeSourceConnector)

  "create" should{
    "return false when failing to decrypt the model" in {
      val result = await(giftAidSessionServiceInvalidEncryption.createSessionData(completeGiftAidCYAModel, year)(false)(true))
      result shouldBe false
    }
    "return true when succesful and false when adding a duplicate" in {
      await(giftAidUserDataRepository.collection.drop().toFuture())
      await(giftAidUserDataRepository.ensureIndexes)
      val initialResult = await(giftAidSessionService.createSessionData(completeGiftAidCYAModel, year)(false)(true))
      val duplicateResult = await(giftAidSessionService.createSessionData(completeGiftAidCYAModel, year)(false)(true))
      initialResult shouldBe true
      duplicateResult shouldBe false
    }
  }

  "update" should{
    "return false when failing to decrypt the model" in {
      val result = await(giftAidSessionServiceInvalidEncryption.updateSessionData(completeGiftAidCYAModel, year)(false)(true))
      result shouldBe false
    }
  }

}
