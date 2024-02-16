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

package config

object ConfigKeys {
  val signInUrl = "microservice.services.sign-in.url"
  val signInContinueUrl = "microservice.services.sign-in.continueUrl"
  val contactFrontendUrl = "microservice.services.contact-frontend.url"
  val incomeTaxDividendsUrl = "microservice.services.income-tax-dividends.url"
  val incomeTaxInterestUrl = "microservice.services.income-tax-interest.url"
  val incomeTaxGiftAidUrl = "microservice.services.income-tax-gift-aid.url"
  val incomeTaxSubmissionUrl = "microservice.services.income-tax-submission.url"
  val incomeTaxSubmissionFrontendUrl = "microservice.services.income-tax-submission-frontend.url"
  val basGatewayFrontendUrl = "microservice.services.bas-gateway-frontend.url"
  val feedbackFrontendUrl = "microservice.services.feedback-frontend.url"
  val viewAndChangeUrl = "microservice.services.view-and-change.url"
  val defaultTaxYear = "defaultTaxYear"
}
