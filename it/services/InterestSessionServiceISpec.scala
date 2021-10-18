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

package services

import connectors.IncomeTaxUserDataConnector
import repositories.InterestUserDataRepository
import utils.IntegrationTest


class InterestSessionServiceISpec extends IntegrationTest{

  val interestUserDataRepository: InterestUserDataRepository = app.injector.instanceOf[InterestUserDataRepository]
  val incomeTaxUserDataConnector: IncomeTaxUserDataConnector = app.injector.instanceOf[IncomeTaxUserDataConnector]

  val interestSessionServiceInvalidEncryption: InterestSessionService = appWithInvalidEncryptionKey.injector.instanceOf[InterestSessionService]
  val interestSessionService: InterestSessionService = new InterestSessionService(interestUserDataRepository, incomeTaxUserDataConnector)

  "update" should{
    "return false when failing to decrypt the model" in {
      val result = await(interestSessionServiceInvalidEncryption.updateSessionData(completeInterestCYAModel, year)(false)(true))
      result shouldBe false
    }
  }


}
