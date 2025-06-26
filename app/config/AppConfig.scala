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

import com.google.inject.ImplementedBy
import play.api.Configuration
import play.api.i18n.Lang
import play.api.mvc.{Call, RequestHeader}
import uk.gov.hmrc.play.bootstrap.binders.RedirectUrl.idFunctor
import uk.gov.hmrc.play.bootstrap.binders.{AbsoluteWithHostnameFromAllowlist, OnlyRelative, RedirectUrl}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import javax.inject.Inject
import scala.concurrent.duration.Duration


class FrontendAppConfig @Inject()(servicesConfig: ServicesConfig, configuration: Configuration) extends AppConfig {
  lazy val signInBaseUrl: String = servicesConfig.getString(ConfigKeys.signInUrl)

  private val allowedHosts: Seq[String] = configuration.get[Seq[String]]("microservice.allowedRedirects")
  private val redirectPolicy = OnlyRelative | AbsoluteWithHostnameFromAllowlist(allowedHosts:_*)

  private val signInContinueBaseUrl: String = servicesConfig.getString(ConfigKeys.signInContinueUrl)
  val signInContinueUrl: String = RedirectUrl(signInContinueBaseUrl).get(redirectPolicy).encodedUrl //TODO add redirect to overview page
  private val signInOrigin: String = servicesConfig.getString("appName")

  val signInUrl: String = s"$signInBaseUrl?continue=$signInContinueUrl&origin=$signInOrigin"
  val dividendsBaseUrl: String = s"${servicesConfig.getString(ConfigKeys.incomeTaxDividendsUrl)}/income-tax-dividends"
  val interestBaseUrl: String = s"${servicesConfig.getString(ConfigKeys.incomeTaxInterestUrl)}/income-tax-interest"
  val giftAidBaseUrl: String = s"${servicesConfig.getString(ConfigKeys.incomeTaxGiftAidUrl)}/income-tax-gift-aid"
  val incomeTaxSubmissionBEBaseUrl: String = s"${servicesConfig.getString(ConfigKeys.incomeTaxSubmissionUrl)}/income-tax-submission-service"
  val vcSessionServiceBaseUrl: String = servicesConfig.baseUrl("income-tax-session-data")

  def defaultTaxYear: Int = servicesConfig.getInt(ConfigKeys.defaultTaxYear)

  def incomeTaxSubmissionBaseUrl: String = servicesConfig.getString(ConfigKeys.incomeTaxSubmissionFrontendUrl) +
    servicesConfig.getString("microservice.services.income-tax-submission-frontend.context")

  def incomeTaxSubmissionOverviewUrl(taxYear: Int): String = incomeTaxSubmissionBaseUrl + "/" + taxYear +
    servicesConfig.getString("microservice.services.income-tax-submission-frontend.overview")

  def commonTaskListUrl(taxYear: Int): String = incomeTaxSubmissionBaseUrl + "/" + taxYear + "/tasklist"

  def incomeTaxSubmissionStartUrl(taxYear: Int): String = s"$incomeTaxSubmissionBaseUrl/$taxYear/start"

  def incomeTaxSubmissionIvRedirect: String = incomeTaxSubmissionBaseUrl +
    servicesConfig.getString("microservice.services.income-tax-submission-frontend.iv-redirect")

  private val vcBaseUrl: String = servicesConfig.getString(ConfigKeys.viewAndChangeUrl)

  def viewAndChangeEnterUtrUrl: String = s"$vcBaseUrl/report-quarterly/income-and-expenses/view/agents/client-utr"
  def viewAndChangeAgentsUrl: String = s"$vcBaseUrl/report-quarterly/income-and-expenses/view/agents"

  private val appUrl: String = servicesConfig.getString("microservice.url")

  private val contactFrontEndUrl: String = servicesConfig.getString(ConfigKeys.contactFrontendUrl)

  private val contactFormServiceIndividual: String = "update-and-submit-income-tax-return"
  private val contactFormServiceAgent: String = "update-and-submit-income-tax-return-agent"

  def contactFormServiceIdentifier(implicit isAgent: Boolean): String = if (isAgent) contactFormServiceAgent else contactFormServiceIndividual

  private def requestUri(implicit request: RequestHeader): String = RedirectUrl(appUrl + request.uri).get(redirectPolicy).encodedUrl

  private val feedbackFrontendUrl: String = servicesConfig.getString(ConfigKeys.feedbackFrontendUrl)

  def feedbackSurveyUrl(implicit isAgent: Boolean): String = s"$feedbackFrontendUrl/feedback/$contactFormServiceIdentifier"

  def betaFeedbackUrl(implicit request: RequestHeader, isAgent: Boolean): String =
    s"$contactFrontEndUrl/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=$requestUri"

  def contactUrl(implicit isAgent: Boolean): String = s"$contactFrontEndUrl/contact/contact-hmrc?service=$contactFormServiceIdentifier"

  private val basGatewayUrl: String = servicesConfig.getString(ConfigKeys.basGatewayFrontendUrl)

  val signOutUrl: String = s"$basGatewayUrl/bas-gateway/sign-out-without-state"

  val timeoutDialogTimeout: Int = servicesConfig.getInt("timeoutDialogTimeout")
  val timeoutDialogCountdown: Int = servicesConfig.getInt("timeoutDialogCountdown")

  def taxYearErrorFeature: Boolean = servicesConfig.getBoolean("taxYearErrorFeatureSwitch")

  override def languageMap: Map[String, Lang] = Map(
    "english" -> Lang("en"),
    "cymraeg" -> Lang("cy")
  )

  override def routeToSwitchLanguage: String => Call =
    (lang: String) => controllers.routes.LanguageSwitchController.switchToLanguage(lang)

  val welshToggleEnabled: Boolean = servicesConfig.getBoolean("feature-switch.welshToggleEnabled")
  val useEncryption: Boolean = servicesConfig.getBoolean("useEncryption")

  val miniJourneyEnabled: Boolean = servicesConfig.getBoolean("feature-switch.journeys.miniJourneyEnabled")

  def isJourneyAvailable(journeyKey: JourneyFeatureSwitchKeys): Boolean = servicesConfig.getBoolean("feature-switch.journeys." + journeyKey.stringify)

  def taxYearSwitchResetsSession: Boolean = servicesConfig.getBoolean("taxYearChangeResetsSession")

  val encryptionKey: String = servicesConfig.getString("mongodb.encryption.key")
  def mongoTTL: Long = Duration(servicesConfig.getString("mongodb.timeToLive")).toMinutes.toInt
  def replaceIndexes: Boolean = servicesConfig.getBoolean("mongodb.replaceIndexes")

  def excludeJourneyUrl(taxYear: Int): String = incomeTaxSubmissionBaseUrl + "/" + taxYear +
    servicesConfig.getString("microservice.services.income-tax-submission-frontend.exclude")

  // feature switches
  val tailoringEnabled: Boolean = servicesConfig.getBoolean("feature-switch.tailoringEnabled")
  val backendSessionEnabled: Boolean = servicesConfig.getBoolean("feature-switch.backendSessionEnabled")
  val interestTailoringEnabled: Boolean = servicesConfig.getBoolean("feature-switch.tailoring.interest")
  val interestSavingsEnabled: Boolean = servicesConfig.getBoolean("feature-switch.journeys.savings")
  val dividendsTailoringEnabled: Boolean = servicesConfig.getBoolean("feature-switch.tailoring.dividends")
  val charityTailoringEnabled: Boolean = servicesConfig.getBoolean("feature-switch.tailoring.charity")
  val sectionCompletedQuestionEnabled: Boolean = servicesConfig.getBoolean("feature-switch.journeys.sectionCompletedQuestionEnabled")
  val sessionCookieServiceEnabled: Boolean = servicesConfig.getBoolean("feature-switch.journeys.sessionCookieServiceEnabled")
}

@ImplementedBy(classOf[FrontendAppConfig])
trait AppConfig {
  def signInBaseUrl: String

  def signInContinueUrl: String
  def signInUrl: String
  def dividendsBaseUrl: String
  def interestBaseUrl: String
  def giftAidBaseUrl: String
  def incomeTaxSubmissionBEBaseUrl: String
  def vcSessionServiceBaseUrl: String

  def defaultTaxYear: Int

  def incomeTaxSubmissionBaseUrl: String

  def incomeTaxSubmissionOverviewUrl(taxYear: Int): String

  def commonTaskListUrl(taxYear: Int) : String

  def incomeTaxSubmissionStartUrl(taxYear: Int): String

  def incomeTaxSubmissionIvRedirect: String

  def viewAndChangeEnterUtrUrl: String

  def viewAndChangeAgentsUrl: String

  def contactFormServiceIdentifier(implicit isAgent: Boolean): String

  def feedbackSurveyUrl(implicit isAgent: Boolean): String

  def betaFeedbackUrl(implicit request: RequestHeader, isAgent: Boolean): String

  def contactUrl(implicit isAgent: Boolean): String

  def signOutUrl: String

  def timeoutDialogTimeout: Int
  def timeoutDialogCountdown: Int

  def taxYearErrorFeature: Boolean

  def languageMap: Map[String, Lang]

  def routeToSwitchLanguage: String => Call

  def welshToggleEnabled: Boolean
  def useEncryption: Boolean

  def isJourneyAvailable(journeyKey: JourneyFeatureSwitchKeys): Boolean

  def taxYearSwitchResetsSession: Boolean

  def encryptionKey: String

  def mongoTTL: Long
  def replaceIndexes: Boolean

  def excludeJourneyUrl(taxYear: Int): String

  // feature switches
  def tailoringEnabled: Boolean
  def interestTailoringEnabled: Boolean
  def interestSavingsEnabled: Boolean
  def dividendsTailoringEnabled: Boolean
  def charityTailoringEnabled: Boolean
  def miniJourneyEnabled: Boolean
  def sectionCompletedQuestionEnabled: Boolean
  def sessionCookieServiceEnabled: Boolean

}
