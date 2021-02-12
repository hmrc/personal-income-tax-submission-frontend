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

import org.scalamock.scalatest.MockFactory
import play.api.mvc.RequestHeader
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

class MockAppConfig extends MockFactory {

  val config: AppConfig = new AppConfig(mock[ServicesConfig]) {
    override lazy val signInContinueUrl: String = "/continue"
    override lazy val signInUrl: String = "/signIn"
    override lazy val dividendsBaseUrl: String = "/dividends"
    override lazy val interestBaseUrl: String = "/interest"

    override lazy val defaultTaxYear: Int = 2020

    override def incomeTaxSubmissionOverviewUrl(taxYear: Int): String = "/overview"
    override def incomeTaxSubmissionStartUrl(taxYear: Int): String = "/start"

    override def feedbackUrl(implicit request: RequestHeader): String = "feedbackUrl"
  }
}

