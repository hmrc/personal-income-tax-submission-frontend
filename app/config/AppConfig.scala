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

import play.api.mvc.RequestHeader
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.{Inject, Singleton}

@Singleton
class AppConfig @Inject()(servicesConfig: ServicesConfig) {
  private lazy val signInBaseUrl: String = servicesConfig.getString(ConfigKeys.signInUrl)

  private lazy val signInContinueBaseUrl: String = servicesConfig.getString(ConfigKeys.signInContinueBaseUrl)
  lazy val signInContinueUrl: String = SafeRedirectUrl(signInContinueBaseUrl).encodedUrl //TODO add redirect to overview page
  private lazy val signInOrigin = servicesConfig.getString("appName")
  lazy val signInUrl: String = s"$signInBaseUrl?continue=$signInContinueUrl&origin=$signInOrigin"
  lazy val dividendsBaseUrl: String = servicesConfig.baseUrl("income-tax-dividends") + "/income-tax-dividends"
  lazy val interestBaseUrl: String = servicesConfig.baseUrl("income-tax-interest") + "/income-tax-interest"

  lazy val defaultTaxYear: Int = servicesConfig.getInt(ConfigKeys.defaultTaxYear)

  def incomeTaxSubmissionOverviewUrl(taxYear: Int): String = servicesConfig.getString(ConfigKeys.incomeTaxSubmissionFrontend) +
    servicesConfig.getString("microservice.services.income-tax-submission-frontend.context") + "/" + taxYear +
    servicesConfig.getString("microservice.services.income-tax-submission-frontend.overview")
  def incomeTaxSubmissionStartUrl(taxYear: Int): String = servicesConfig.getString(ConfigKeys.incomeTaxSubmissionFrontend) +
    servicesConfig.getString("microservice.services.income-tax-submission-frontend.context") + "/" + taxYear +
    "/start"

  lazy private val appUrl: String = servicesConfig.getString("microservice.url")
  lazy private val contactFrontEndUrl = {
    val contactUrl = servicesConfig.baseUrl("contact-frontend")
    servicesConfig.getConfString("contact-frontend.baseUrl", contactUrl)
  }

  lazy private val contactFormServiceIdentifier = "update-and-submit-income-tax-return"

  private def requestUri(implicit request: RequestHeader): String = SafeRedirectUrl(appUrl + request.uri).encodedUrl

  lazy val feedbackUrl: String = s"$contactFrontEndUrl/contact/beta-feedback?service=$contactFormServiceIdentifier"

  def feedbackUrlWithCallbackUrl(implicit request: RequestHeader): String = s"$feedbackUrl&backUrl=$requestUri"

  lazy val contactUrl = s"$contactFrontEndUrl/contact/contact-hmrc?service=$contactFormServiceIdentifier"

  private lazy val basGatewayUrl = {
    val basGatewayUrl = servicesConfig.baseUrl("bas-gateway")
    servicesConfig.getConfString("bas-gateway.relativeUrl", basGatewayUrl)
  }

  lazy val signOutUrl: String = s"$basGatewayUrl/bas-gateway/sign-out-without-state"

//  TODO - Do we need this?
//  lazy val signOutUrlWithState: String = s"$basGatewayUrl/bas-gateway/sign-out-with-state"


}
