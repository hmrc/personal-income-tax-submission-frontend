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

package test.services

import models.dividends.DividendsCheckYourAnswersModel
import services.DividendsSessionService
import test.utils.IntegrationTest

class DividendsSessionServiceISpec extends IntegrationTest {
  val dividendsSessionServiceInvalidEncryption: DividendsSessionService = appWithInvalidEncryptionKey.injector.instanceOf[DividendsSessionService]
  dividendsSessionService.createSessionData(DividendsCheckYourAnswersModel(), taxYear)(false)(true)

  "create" should {
    "return false when failing to decrypt the model" in {
      val result = await(dividendsSessionServiceInvalidEncryption.createSessionData(completeDividendsCYAModel, taxYear)(false)(true))
      result shouldBe false
    }
    "return true when successful and false when adding a duplicate" in {
      await(dividendsUserDataRepository.collection.drop().toFuture())
      await(dividendsUserDataRepository.ensureIndexes())
      val initialResult = await(dividendsSessionService.createSessionData(completeDividendsCYAModel, taxYear)(false)(true))
      val duplicateResult = await(dividendsSessionService.createSessionData(completeDividendsCYAModel, taxYear)(false)(true))
      initialResult shouldBe true
      duplicateResult shouldBe false
    }
  }

  "update" should {
    "return false when failing to decrypt the model" in {
      val result = await(dividendsSessionServiceInvalidEncryption.updateSessionData(completeDividendsCYAModel, taxYear)(false)(true))
      result shouldBe false
    }
  }

}
