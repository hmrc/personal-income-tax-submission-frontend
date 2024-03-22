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

import connectors.{IncomeSourceConnector, IncomeTaxUserDataConnector}
import repositories.InterestUserDataRepository
import services.InterestSessionService
import test.utils.IntegrationTest


class InterestSessionServiceISpec extends IntegrationTest{

  val interestUserDataRepository: InterestUserDataRepository = app.injector.instanceOf[InterestUserDataRepository]
  val incomeTaxUserDataConnector: IncomeTaxUserDataConnector = app.injector.instanceOf[IncomeTaxUserDataConnector]
  val incomeSourceConnector: IncomeSourceConnector = app.injector.instanceOf[IncomeSourceConnector]

  val interestSessionServiceInvalidEncryption: InterestSessionService = appWithInvalidEncryptionKey.injector.instanceOf[InterestSessionService]
  val interestSessionService: InterestSessionService = new InterestSessionService(interestUserDataRepository, incomeTaxUserDataConnector, incomeSourceConnector)

  "update" should{
    "return false when failing to decrypt the model" in {
      val result = await(interestSessionServiceInvalidEncryption.updateSessionData(completeInterestCYAModel, taxYear)(false)(true))
      result shouldBe false
    }
  }


}
