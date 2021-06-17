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

package controllers.charity

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, ViewHelpers}

class OverseasSharesSecuritiesLandPropertyAmountControllerSpec extends IntegrationTest with ViewHelpers {

  val taxYear = 2022

  object Selectors {
    val titleSelector = "title"
    val inputField = ".govuk-input"
    val inputLabel = ".govuk-label > div"
    val errorHref = "#amount"
  }

  def url: String = s"$appUrl/$taxYear/charity/value-of-shares-securities-land-or-property-to-overseas-charities"

  trait SpecificExpectedResults {
    val tooLong: String
    val emptyField: String
    val incorrectFormat: String
  }

  trait CommonExpectedResults {
    val heading: String
    val hintText: String
    val caption: String
    val button: String
    val inputName: String
    val inputLabel: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val heading: String = "What is the value of qualifying shares, securities, land or property donated to overseas charities?"
    val hintText: String = "For example, £600 or £193.54"
    val caption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val button = "Continue"
    val inputName = "amount"
    val inputLabel = "Total value, in pounds"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val heading: String = "What is the value of qualifying shares, securities, land or property donated to overseas charities?"
    val hintText: String = "For example, £600 or £193.54"
    val caption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val button = "Continue"
    val inputName = "amount"
    val inputLabel = "Total value, in pounds"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val tooLong = "The value of your shares, securities, land or property must be less than £100,000,000,000"
    val emptyField = "Enter the value of shares, securities, land or property you donated to overseas charities"
    val incorrectFormat = "Enter the value of shares, securities, land or property you donated to overseas charities in the correct format"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val tooLong = "The value of your client’s shares, securities, land or property must be less than £100,000,000,000"
    val emptyField = "Enter the value of shares, securities, land or property your client donated to overseas charities"
    val incorrectFormat = "Enter the value of shares, securities, land or property your client donated to overseas charities in the correct format"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val tooLong = "The value of your shares, securities, land or property must be less than £100,000,000,000"
    val emptyField = "Enter the value of shares, securities, land or property you donated to overseas charities"
    val incorrectFormat = "Enter the value of shares, securities, land or property you donated to overseas charities in the correct format"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val tooLong = "The value of your client’s shares, securities, land or property must be less than £100,000,000,000"
    val emptyField = "Enter the value of shares, securities, land or property your client donated to overseas charities"
    val incorrectFormat = "Enter the value of shares, securities, land or property your client donated to overseas charities in the correct format"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  ".show" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the page with correct content" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(heading)
          h1Check(heading + " " + caption)
          inputFieldCheck(inputName, Selectors.inputField)
          textOnPageCheck(inputLabel, Selectors.inputLabel)
          hintTextCheck(hintText)
          captionCheck(caption)
          buttonCheck(button)
          noErrorsCheck()
          welshToggleCheck(user.isWelsh)
        }
      }
    }
  }

  ".submit" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an OK" in {
          lazy val form: Map[String, Seq[String]] = Map("amount" -> Seq("1234"))

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(url, body = form, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          result.status shouldBe OK
        }

        "return an error" when {

          "the submitted data is empty" which {
            lazy val form: Map[String, Seq[String]] = Map("amount" -> Seq(""))

            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(url, body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import user.commonExpectedResults._

            titleCheck(errorPrefix + heading)
            h1Check(heading + " " + caption)
            inputFieldCheck(inputName, Selectors.inputField)
            textOnPageCheck(inputLabel, Selectors.inputLabel)
            hintTextCheck(hintText)
            captionCheck(caption)
            buttonCheck(button)
            errorSummaryCheck(user.specificExpectedResults.get.emptyField, Selectors.errorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.emptyField)
            welshToggleCheck(user.isWelsh)
          }

          "the submitted data is too long" which {
            lazy val form: Map[String, Seq[String]] = Map("amount" -> Seq("999999999999999999999999999999999999999999999999"))

            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(url, body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import user.commonExpectedResults._

            titleCheck(errorPrefix + heading)
            h1Check(heading + " " + caption)
            inputFieldCheck(inputName, Selectors.inputField)
            textOnPageCheck(inputLabel, Selectors.inputLabel)
            hintTextCheck(hintText)
            captionCheck(caption)
            buttonCheck(button)
            errorSummaryCheck(user.specificExpectedResults.get.tooLong, Selectors.errorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.tooLong)
            welshToggleCheck(user.isWelsh)
          }

          "the submitted data is in the incorrect format" which {
            lazy val form: Map[String, Seq[String]] = Map("amount" -> Seq(":@~{}<>?"))

            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(url, body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import user.commonExpectedResults._

            titleCheck(errorPrefix + heading)
            h1Check(heading + " " + caption)
            inputFieldCheck(inputName, Selectors.inputField)
            textOnPageCheck(inputLabel, Selectors.inputLabel)
            hintTextCheck(hintText)
            captionCheck(caption)
            buttonCheck(button)
            errorSummaryCheck(user.specificExpectedResults.get.incorrectFormat, Selectors.errorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.incorrectFormat)
            welshToggleCheck(user.isWelsh)
          }
        }
      }
    }
  }
}
