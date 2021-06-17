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

class GiftAidAppendNextYearTaxAmountControllerSpec extends IntegrationTest with ViewHelpers {

  val taxYear = 2022
  val urlWithSameYears = "/income-through-software/return/personal-income/2022/charity/amount-after-5-april-2022-added-to-this-tax-year"
  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  def url: String = url(taxYear, taxYear)

  def url(taxYear: Int, someTaxYear: Int): String =
    s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/amount-after-5-april-$someTaxYear-added-to-this-tax-year"

  trait SpecificExpectedResults {
    val heading: String
    val tooLongError: String
    val emptyFieldError: String
    val incorrectFormatError: String
  }

  trait CommonExpectedResults {
    val hintText: String
    val expectedCaption: String
    val inputName: String
    val button: String
    val error: String
  }

  object Selectors {
    val titleSelector = "title"
    val inputField = ".govuk-input"
    val errorHref = "#amount"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val hintText: String = "For example, £600 or £193.54"
    val expectedCaption: String = "Donations to charity for 6 April 2021 to 5 April 2022"
    val inputName: String = "amount"
    val button: String = "Continue"
    val error = "Error: "
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val hintText: String = "For example, £600 or £193.54"
    val expectedCaption: String = "Donations to charity for 6 April 2021 to 5 April 2022"
    val inputName: String = "amount"
    val button: String = "Continue"
    val error = "Error: "
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val heading: String = "How much of the donations you made after 5 April 2022 do you want to add to this tax year?"
    val tooLongError: String = "The amount of your donation made after 5 April 2022 you add to the last tax year must be less than £100,000,000,000"
    val emptyFieldError: String = "Enter the amount of your donation made after 5 April 2022 you want to add to this tax year"
    val incorrectFormatError: String = "Enter the amount you want to add to this tax year in the correct format"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val heading: String = "How much of the donations your client made after 5 April 2022 do you want to add to this tax year?"
    val tooLongError: String = "The amount of your client’s donation made after 5 April 2022 you add to the last tax year must be less than £100,000,000,000"
    val emptyFieldError: String = "Enter the amount of your client’s donation made after 5 April 2022 you want to add to this tax year"
    val incorrectFormatError: String = "Enter the amount you want to add to this tax year in the correct format"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val heading: String = "How much of the donations you made after 5 April 2022 do you want to add to this tax year?"
    val tooLongError: String = "The amount of your donation made after 5 April 2022 you add to the last tax year must be less than £100,000,000,000"
    val emptyFieldError: String = "Enter the amount of your donation made after 5 April 2022 you want to add to this tax year"
    val incorrectFormatError: String = "Enter the amount you want to add to this tax year in the correct format"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val heading: String = "How much of the donations your client made after 5 April 2022 do you want to add to this tax year?"
    val tooLongError: String = "The amount of your client’s donation made after 5 April 2022 you add to the last tax year must be less than £100,000,000,000"
    val emptyFieldError: String = "Enter the amount of your client’s donation made after 5 April 2022 you want to add to this tax year"
    val incorrectFormatError: String = "Enter the amount you want to add to this tax year in the correct format"
  }

  ".show" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "redirect to a correct URL when years don't match up" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url(taxYear, taxYear + 1), follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").headOption shouldBe Some(urlWithSameYears)
          }
        }

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

          titleCheck(user.specificExpectedResults.get.heading)
          h1Check(user.specificExpectedResults.get.heading + " " + expectedCaption)
          inputFieldCheck(inputName, Selectors.inputField)
          hintTextCheck(hintText)
          captionCheck(expectedCaption)
          buttonCheck(button)
          welshToggleCheck(user.isWelsh)
        }
      }
    }
  }

  ".submit" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "redirect to a correct URL when years don't match up" which {
          lazy val form: Map[String, Seq[String]] = Map("amount" -> Seq("1234"))

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(url(taxYear, taxYear + 1), body = form, follow = false, welsh = user.isWelsh,
              headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").headOption shouldBe Some(urlWithSameYears)
          }
        }

        "return an OK" in {
          lazy val form: Map[String, Seq[String]] = Map("amount" -> Seq("1234"))

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(url, body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
          }

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
            titleCheck(error + user.specificExpectedResults.get.heading)
            h1Check(user.specificExpectedResults.get.heading + " " + expectedCaption)
            inputFieldCheck(inputName, Selectors.inputField)
            hintTextCheck(hintText)
            captionCheck(expectedCaption)
            buttonCheck(button)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.emptyFieldError, Selectors.errorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.emptyFieldError)
          }

          "the submitted data is too long" which {
            lazy val form: Map[String, Seq[String]] = Map("amount" -> Seq("999999999999999999999999999999999999999999999999"))

            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(url, body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import user.commonExpectedResults._
            titleCheck(error + user.specificExpectedResults.get.heading)
            h1Check(user.specificExpectedResults.get.heading + " " + expectedCaption)
            inputFieldCheck(inputName, Selectors.inputField)
            hintTextCheck(hintText)
            captionCheck(expectedCaption)
            buttonCheck(button)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.tooLongError, Selectors.errorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.tooLongError)
          }

          "the submitted data is in the incorrect format" which {
            lazy val form: Map[String, Seq[String]] = Map("amount" -> Seq(":@~{}<>?"))

            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(user.isAgent)
              urlPost(url, body = form, follow = false, welsh = user.isWelsh, headers = Seq(HeaderNames.COOKIE -> playSessionCookies(taxYear)))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import user.commonExpectedResults._
            titleCheck(error + user.specificExpectedResults.get.heading)
            h1Check(user.specificExpectedResults.get.heading + " " + expectedCaption)
            inputFieldCheck(inputName, Selectors.inputField)
            hintTextCheck(hintText)
            captionCheck(expectedCaption)
            buttonCheck(button)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.incorrectFormatError, Selectors.errorHref)
            errorAboveElementCheck(user.specificExpectedResults.get.incorrectFormatError)
          }
        }
      }
    }
  }
}
