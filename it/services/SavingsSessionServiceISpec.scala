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

import connectors.{IncomeSourceConnector, IncomeTaxUserDataConnector}
import repositories.SavingsUserDataRepository
import utils.IntegrationTest


class SavingsSessionServiceISpec extends IntegrationTest{

  val savingsUserDataRepository: SavingsUserDataRepository = app.injector.instanceOf[SavingsUserDataRepository]
  val incomeTaxUserDataConnector: IncomeTaxUserDataConnector = app.injector.instanceOf[IncomeTaxUserDataConnector]
  val incomeSourceConnector: IncomeSourceConnector = app.injector.instanceOf[IncomeSourceConnector]

  val savingsSessionServiceInvalidEncryption: SavingsSessionService = appWithInvalidEncryptionKey.injector.instanceOf[SavingsSessionService]
  val savingsSessionService: SavingsSessionService = new SavingsSessionService(savingsUserDataRepository, incomeTaxUserDataConnector, incomeSourceConnector)

  "create" should{
    "return false when failing to decrypt the model" in {
      val result = await(savingsSessionServiceInvalidEncryption.createSessionData(completeSavingsCYAModel, taxYear)(false)(true))
      result shouldBe false
    }
    "return true when successful and false when adding a duplicate" in {
      await(savingsUserDataRepository.collection.drop().toFuture())
      await(savingsUserDataRepository.ensureIndexes())
      val initialResult = await(savingsSessionService.createSessionData(completeSavingsCYAModel, taxYear)(false)(true))
      val duplicateResult = await(savingsSessionService.createSessionData(completeSavingsCYAModel, taxYear)(false)(true))
      initialResult shouldBe true
      duplicateResult shouldBe false
    }
  }

  "update" should{
    "return false when failing to decrypt the model" in {
      val result = await(savingsSessionServiceInvalidEncryption.updateSessionData(completeSavingsCYAModel, taxYear)(false)(true))
      result shouldBe false
    }
  }

}
