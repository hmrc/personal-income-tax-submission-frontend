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

package controllers.interest

import forms.YesNoForm
import models.interest.{InterestAccountModel, InterestCYAModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, InterestDatabaseHelper, ViewHelpers}

class TaxedInterestControllerISpec extends IntegrationTest with InterestDatabaseHelper with ViewHelpers {
  val taxYear = 2022
  val taxYearMinusOne: Int = taxYear - 1

  object Selectors {
    val captionSelector = ".govuk-caption-l"
    val valueHref = "#value"
    val forExampleSelector = "#main-content > div > div > form > div > fieldset > legend > p:nth-child(2)"
    val bulletPointSelector1 = "#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child(1)"
    val bulletPointSelector2 = "#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child(2)"
    val bulletPointSelector3 = "#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child(3)"
    val doNotIncludeSelector = "#main-content > div > div > form > div > fieldset > legend > p:nth-child(4)"
    val continueSelector = "#continue"
    val continueFormSelector = "#main-content > div > div > form"
  }

  import Selectors._

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedH1: String
    val doNotIncludeText: String
    val expectedErrorText: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val forExampleText: String
    val trustFundsText: String
    val companyBondsText: String
    val lifeAnnuityText: String
    val yesText: String
    val noText: String
    val continueText: String
    val continueLink: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = s"Interest for 6 April $taxYearMinusOne to 5 April $taxYear"
    val forExampleText = "This could be interest from:"
    val trustFundsText = "trust funds"
    val companyBondsText = "company bonds"
    val lifeAnnuityText = "life annuity payments"

    val yesText = "Yes"
    val noText = "No"
    val continueText = "Continue"
    val continueLink = s"/income-through-software/return/personal-income/$taxYear/interest/taxed-uk-interest"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = s"Interest for 6 April $taxYearMinusOne to 5 April $taxYear"
    val forExampleText = "This could be interest from:"
    val trustFundsText = "trust funds"
    val companyBondsText = "company bonds"
    val lifeAnnuityText = "life annuity payments"

    val yesText = "Yes"
    val noText = "No"
    val continueText = "Continue"
    val continueLink = s"/income-through-software/return/personal-income/$taxYear/interest/taxed-uk-interest"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle = "Did you get taxed interest from the UK?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedH1 = "Did you get taxed interest from the UK?"
    val doNotIncludeText = "Do not include interest you got from an Individual Savings Account (ISA) or gilts."
    val expectedErrorText: String = "Select yes if you got taxed UK interest"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle = "Did your client get taxed interest from the UK?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedH1 = "Did your client get taxed interest from the UK?"
    val doNotIncludeText = "Do not include interest your client got from an Individual Savings Account (ISA) or gilts."
    val expectedErrorText: String = "Select yes if your client got taxed UK interest"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle = "Did you get taxed interest from the UK?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedH1 = "Did you get taxed interest from the UK?"
    val doNotIncludeText = "Do not include interest you got from an Individual Savings Account (ISA) or gilts."
    val expectedErrorText: String = "Select yes if you got taxed UK interest"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle = "Did your client get taxed interest from the UK?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedH1 = "Did your client get taxed interest from the UK?"
    val doNotIncludeText = "Do not include interest your client got from an Individual Savings Account (ISA) or gilts."
    val expectedErrorText: String = "Select yes if your client got taxed UK interest"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(
      UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
    )
  }

  ".show" when {

    val url = s"$appUrl/$taxYear/interest/taxed-uk-interest"

    userScenarios.foreach { us =>

      import us.commonExpectedResults._

      val specific = us.specificExpectedResults.get

      s"user is ${agentTest(us.isAgent)} and request is ${welshTest(us.isWelsh)}" should {

        "return OK and correctly render the page" when {
          "there is cyaData in session" which {

            val interestCYA = InterestCYAModel(
              Some(false), None,
              Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25.00)))
            )

            lazy val result: WSResponse = {
              dropInterestDB()
              emptyUserDataStub()
              insertCyaData(Some(interestCYA))

              authoriseAgentOrIndividual(us.isAgent)
              urlGet(url, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
            }

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(specific.expectedTitle)
            welshToggleCheck(us.isWelsh)
            h1Check(specific.expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)

            textOnPageCheck(forExampleText, forExampleSelector)
            textOnPageCheck(trustFundsText, bulletPointSelector1)
            textOnPageCheck(companyBondsText, bulletPointSelector2)
            textOnPageCheck(lifeAnnuityText, bulletPointSelector3)

            textOnPageCheck(specific.doNotIncludeText, doNotIncludeSelector)

            radioButtonCheck(yesText, 1)
            radioButtonCheck(noText, 2)

            buttonCheck(continueText, continueSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }
        }

        "return SEE_OTHER which redirects to overview page" when {
          "there is no cyaData in session" which {
            lazy val result: WSResponse = {
              dropInterestDB()
              emptyUserDataStub()

              authoriseAgentOrIndividual(us.isAgent)
              urlGet(url, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
            }

            s"has an SEE_OTHER($SEE_OTHER) status" in {
              result.status shouldBe SEE_OTHER
              result.header("Location") shouldBe Some("http://localhost:11111/income-through-software/return/2022/view")
            }
          }
        }

        "return UNAUTHORIZED when auth call fails" which {
          lazy val result: WSResponse = {
            dropInterestDB()
            emptyUserDataStub()
            insertCyaData(None)

            unauthorisedAgentOrIndividual(us.isAgent)
            urlGet(url, us.isWelsh, follow = true, playSessionCookie(us.isAgent))
          }

          "has an UNAUTHORIZED(401) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }
      }
    }
  }

  ".submit" when {
    val yesNoFormYes: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
    val yesNoFormNo: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
    val yesNoFormEmpty: Map[String, String] = Map(YesNoForm.yesNo -> "")

    val url = s"$appUrl/$taxYear/interest/taxed-uk-interest"

    userScenarios.foreach { us =>

      import us.commonExpectedResults._

      val specific = us.specificExpectedResults.get

      s"user is ${agentTest(us.isAgent)} and request is ${welshTest(us.isWelsh)}" should {

        "return SEE_OTHER which redirects to TaxedInterestAmount page" when {
          "there is CYA data in session and answer to yes/no is YES" which {
            lazy val result: WSResponse = {
              val interestCYA = InterestCYAModel(
                Some(false), None,
                Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25.00)))
              )

              dropInterestDB()
              insertCyaData(Some(interestCYA))

              authoriseAgentOrIndividual(us.isAgent)
              urlPost(url, yesNoFormYes, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
            }

            s"returns an SEE_OTHER($SEE_OTHER) status" in {
              result.status shouldBe SEE_OTHER
              result.header("Location").get.contains("/income-through-software/return/personal-income/2022/interest/add-taxed-uk-interest-account/") shouldBe true
            }
          }
        }

        "return SEE_OTHER which redirects to InterestCya page" when {
          "there is CYA data in session and answer to yes/no is NO" which {
            lazy val result: WSResponse = {
              val interestCYA = InterestCYAModel(
                Some(false), None,
                Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25.00)))
              )

              dropInterestDB()
              insertCyaData(Some(interestCYA))

              authoriseAgentOrIndividual(us.isAgent)
              urlPost(url, yesNoFormNo, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
            }

            s"returns an SEE_OTHER($SEE_OTHER) status" in {
              result.status shouldBe SEE_OTHER
              result.header("Location") shouldBe Some("/income-through-software/return/personal-income/2022/interest/check-interest")
            }
          }
        }

        "return BAD_REQUEST and render correct errors" when {
          "the yes/no radio button has not been selected" which {
            val interestCYA = InterestCYAModel(
              Some(false), None,
              Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25.00)))
            )

            lazy val result: WSResponse = {
              dropInterestDB()
              emptyUserDataStub()
              insertCyaData(Some(interestCYA))

              authoriseAgentOrIndividual(us.isAgent)
              urlPost(url, yesNoFormEmpty, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
            }

            s"has an BAD_REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(specific.expectedErrorTitle)
            welshToggleCheck(us.isWelsh)
            h1Check(specific.expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            errorSummaryCheck(specific.expectedErrorText, valueHref)
            errorAboveElementCheck(specific.expectedErrorText)
            buttonCheck(continueText, continueSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }
        }

        "return SEE_OTHER which redirects to overview page" when {
          "there is no cyaData in session" which {
            lazy val result: WSResponse = {
              dropInterestDB()
              emptyUserDataStub()

              authoriseAgentOrIndividual(us.isAgent)
              urlPost(url, yesNoFormYes, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
            }

            s"has an SEE_OTHER($SEE_OTHER) status" in {
              result.status shouldBe SEE_OTHER
              result.header("Location") shouldBe Some("http://localhost:11111/income-through-software/return/2022/view")
            }
          }
        }

        "return UNAUTHORIZED when auth call fails" which {
          lazy val result: WSResponse = {
            dropInterestDB()
            emptyUserDataStub()
            insertCyaData(None)

            unauthorisedAgentOrIndividual(us.isAgent)
            urlPost(url, yesNoFormYes, us.isWelsh, follow = true, playSessionCookie(us.isAgent))
          }

          "has an UNAUTHORIZED(401) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }
      }
    }
  }

}