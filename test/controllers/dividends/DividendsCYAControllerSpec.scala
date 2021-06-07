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

package controllers.dividends

import config.{ErrorHandler, MockAuditService}
import models.dividends.DividendsResponseModel
import services.DividendsSubmissionService
import utils.UnitTestWithApp
import views.html.dividends.DividendsCYAView
import views.html.templates.{InternalServerErrorTemplate, ServiceUnavailableTemplate}

class DividendsCYAControllerSpec extends UnitTestWithApp with MockAuditService {

  val service: DividendsSubmissionService = mock[DividendsSubmissionService]
  val errorHandler: ErrorHandler = mock[ErrorHandler]
  val serviceUnavailableTemplate: ServiceUnavailableTemplate = app.injector.instanceOf[ServiceUnavailableTemplate]
  val unauthorisedTemplate: InternalServerErrorTemplate = app.injector.instanceOf[InternalServerErrorTemplate]

  lazy val controller = new DividendsCYAController(
    app.injector.instanceOf[DividendsCYAView],
    service,
    mockDividendsSessionService,
    mockAuditService,
    errorHandler
  )(
    mockAppConfig,
    authorisedAction,
    mockMessagesControllerComponents
  )

  val taxYear: Int = mockAppConfig.defaultTaxYear
  val taxYear2020 = 2020
  val firstAmount = 10
  val secondAmount = 20
  val successResponseCode = 204
  val internalServerErrorResponse = 500
  val serviceUnavailableResponse = 503
  val individualAffinityGroup: String = "Individual"

  lazy val internalServerErrorModel: DividendsResponseModel = DividendsResponseModel(internalServerErrorResponse)

  ".priorityOrderOrNone" should {

    val amount1: BigDecimal = 120
    val amount2: BigDecimal = 140

    val priorityValue: Option[BigDecimal] = Some(amount1)
    val otherValue: Option[BigDecimal] = Some(amount2)

    "return the priority value" when {

      "the priority value is provided on it's own" in {
        controller.priorityOrderOrNone(priorityValue, None, yesNoResult = true) shouldBe Some(amount1)
      }

      "the priority value is provided along side the other value" in {
        controller.priorityOrderOrNone(priorityValue, otherValue, yesNoResult = true) shouldBe Some(amount1)
      }

    }

    "return the other value" when {

      "no priority value is provided" in {
        controller.priorityOrderOrNone(None, otherValue, yesNoResult = true) shouldBe Some(amount2)
      }

    }

    "return None" when {

      "no priority or other value is provided" in {
        controller.priorityOrderOrNone(None, None, yesNoResult = true) shouldBe None
      }

      "yesNoResult is false regardless of provided values" in {
        controller.priorityOrderOrNone(priorityValue, otherValue, yesNoResult = false) shouldBe None
      }

    }

  }

}
