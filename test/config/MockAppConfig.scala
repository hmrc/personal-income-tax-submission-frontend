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

import org.scalamock.scalatest.MockFactory
import play.api.i18n.Lang
import play.api.mvc.{Call, RequestHeader}

import scala.concurrent.duration.Duration

class MockAppConfig extends AppConfig with MockFactory {
  override val signInBaseUrl: String = "/signInBase"
  override val signInContinueUrl: String = "/continue"
  override val signInUrl: String = "/signIn"
  override val dividendsBaseUrl: String = "/dividends"
  override val interestBaseUrl: String = "/interest"
  override val giftAidBaseUrl: String = "/giftAid"
  override val incomeTaxSubmissionBEBaseUrl: String = "/incomeTaxSubmissionBaseUrl"

  override def defaultTaxYear: Int = 2022

  override def incomeTaxSubmissionBaseUrl: String = "/incomeTaxSubmissionBaseUrl"

  override def incomeTaxSubmissionOverviewUrl(taxYear: Int): String = "/overview"

  def commonTaskListUrl(taxYear: Int): String = "/tasklist"

  override def incomeTaxSubmissionStartUrl(taxYear: Int): String = "/start"

  override def incomeTaxSubmissionIvRedirect: String = "/update-and-submit-income-tax-return/iv-uplift"

  override def viewAndChangeEnterUtrUrl: String = "/report-quarterly/income-and-expenses/view/agents/client-utr"

  override def contactFormServiceIdentifier(implicit isAgent: Boolean): String = "contactFormServiceIdentifier"

  override def feedbackSurveyUrl(implicit isAgent: Boolean): String = "/feedbackUrl"

  override def betaFeedbackUrl(implicit request: RequestHeader, isAgent: Boolean): String = "betaFeedbackUrl"

  override def contactUrl(implicit isAgent: Boolean): String = "/contact-frontend/contact"

  override val signOutUrl: String = "/sign-out-url"
  override val timeoutDialogTimeout: Int = 900
  override val timeoutDialogCountdown: Int = 120

  override def taxYearErrorFeature: Boolean = true

  override def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

  override def routeToSwitchLanguage: String => Call =
    (lang: String) => controllers.routes.LanguageSwitchController.switchToLanguage(lang)

  override val welshToggleEnabled: Boolean = true
  override val useEncryption: Boolean = true

  override def isJourneyAvailable(journeyKey: JourneyFeatureSwitchKeys): Boolean = true

  override def taxYearSwitchResetsSession: Boolean = true

  override val encryptionKey: String = "1234556"

  override def mongoTTL: Long = Duration("15").toMinutes
  override def replaceIndexes: Boolean = false

  override val tailoringEnabled: Boolean = false
  override val interestTailoringEnabled: Boolean = false
  override val dividendsTailoringEnabled: Boolean = false
  override val charityTailoringEnabled: Boolean = false
  override val interestSavingsEnabled: Boolean = false

  override def excludeJourneyUrl(taxYear: Int): String = "/exclude"

  override def miniJourneyEnabled: Boolean = false

  override def sectionCompletedQuestionEnabled: Boolean = false

}

class MockAppConfigEncyrptionOff extends MockAppConfig {
  override val useEncryption: Boolean = false
}
