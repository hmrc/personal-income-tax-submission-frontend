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

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

trait AppConfig {
  val signInContinueUrl: String
  val signInUrl: String
  val dividendsBaseUrl: String
  val interestBaseUrl: String

  def incomeTaxSubmissionOverviewUrl(taxYear: Int): String
}

@Singleton
class FrontendAppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) extends AppConfig {
  private val signInBaseUrl: String = config.get[String](ConfigKeys.signInUrl)
  private val signInContinueBaseUrl: String = config.get[String](ConfigKeys.signInContinueBaseUrl)
  override val signInContinueUrl: String = SafeRedirectUrl(signInContinueBaseUrl).encodedUrl //TODO add redirect to overview page
  private val signInOrigin = servicesConfig.getString("appName")
  override val signInUrl: String = s"$signInBaseUrl?continue=$signInContinueUrl&origin=$signInOrigin"
  override val dividendsBaseUrl: String = servicesConfig.baseUrl("income-tax-dividends") + "/income-tax-dividends"
  override val interestBaseUrl: String = servicesConfig.baseUrl("income-tax-interest") + "/income-tax-interest"
  def incomeTaxSubmissionOverviewUrl(taxYear: Int): String = servicesConfig.getString(ConfigKeys.incomeTaxSubmissionFrontend) +
    servicesConfig.getString("microservice.services.income-tax-submission-frontend.context") + "/" + taxYear +
    servicesConfig.getString("microservice.services.income-tax-submission-frontend.overview")

}
