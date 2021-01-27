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

class MockAppConfig extends AppConfig {
  override val signInContinueUrl: String = "/continue"
  override val signInUrl: String = "/signIn"
  override val dividendsBaseUrl: String = "/dividends"
  override val interestBaseUrl: String = "/interest"

  override val defaultTaxYear: Int = 2020

  override def incomeTaxSubmissionOverviewUrl(taxYear: Int): String = "/overview"
  override def incomeTaxSubmissionStartUrl(taxYear: Int): String = "/start"




}

