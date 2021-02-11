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

package config

import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.UnitTest

class AppConfigSpec extends UnitTest {

  private val rootServices = "microservice.services"
  private val mockServicesConfig: ServicesConfig = mock[ServicesConfig]
  private val appUrl = "http://localhost:9308"
  (mockServicesConfig.getString _).expects(s"$rootServices.contact-frontend.protocol").returns("http")
  (mockServicesConfig.getString _).expects(s"$rootServices.contact-frontend.host").returns("contact-frontend")
  (mockServicesConfig.getString _).expects(s"$rootServices.contact-frontend.port").returns("9250")
  (mockServicesConfig.getString _).expects("microservice.url").returns(appUrl)

  "AppConfig" should {

    "return correct feedbackUrl" in {
      val appConfig = new AppConfig(mockServicesConfig)

      val expectedBackUrl = SafeRedirectUrl(appUrl + fakeRequest.uri).encodedUrl
      val expectedServiceIdentifier = "update-and-submit-income-tax-return"

      val expectedFeedbackUrl =
        s"http://contact-frontend:9250/contact/beta-feedback?service=$expectedServiceIdentifier&backUrl=$expectedBackUrl"

      appConfig.feedbackUrl(fakeRequest) shouldBe expectedFeedbackUrl
    }
  }
}
