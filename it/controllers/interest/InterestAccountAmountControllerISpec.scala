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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.{BAD_REQUEST, OK}
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, InterestDatabaseHelper, ViewHelpers}

class InterestAccountAmountControllerISpec extends IntegrationTest with ViewHelpers with InterestDatabaseHelper {

  object Selectors {
    val captionSelector = "#main-content > div > div > form > div > div > label > h1 > span"
    val hintTextSelector = "#amount-hint"
    val poundPrefixSelector = "#main-content > div > div > form > div > div > div.govuk-input__wrapper > div"
    val amountInputSelector = "#amount"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
  }

  val taxYear: Int = 2022
  val UNTAXED: String = "untaxed"
  val TAXED: String = "taxed"

  val newAmountInput = "#amount"
  val amountInputName = "amount"
  val emptyAmountInput = ""
  val expectedErrorLink = "#amount"

  def viewUrl(taxType: String): String = s"$appUrl/$taxYear/interest/how-much-$taxType-uk-interest"
  def viewUrlNoPrefix(taxType: String): String = s"/income-through-software/return/personal-income/$taxYear/interest/how-much-$taxType-uk-interest"
  def continueLink(taxType:String): String = s"/income-through-software/return/personal-income/$taxYear/interest/accounts-with-$taxType-uk-interest"


  trait SpecificExpectedResults {
    def expectedTitle(taxType: String): String
    def expectedHeading(taxType: String): String
    def expectedErrorTitle(taxType: String): String
    def expectedErrorNoEntry(taxType: String): String
    def expectedErrorInvalid(taxType: String): String
    def expectedErrorOverMax(taxType: String): String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedHintText: String
    val poundPrefixText: String
    val continueText: String

  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    def expectedTitle(taxType: String): String = s"How much $taxType UK interest did you get?"
    def expectedHeading(taxType: String): String = s"How much $taxType UK interest did you get from Halifax?"
    def expectedErrorTitle(taxType: String): String = s"Error: ${expectedTitle(taxType)}"
    def expectedErrorNoEntry(taxType: String): String = s"Enter the amount of $taxType UK interest you got"
    def expectedErrorInvalid(taxType: String): String = s"Enter the amount of $taxType UK interest in the correct format"
    def expectedErrorOverMax(taxType: String): String = s"The amount of $taxType UK interest must be less than £100,000,000,000"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    def expectedTitle(taxType: String): String = s"How much $taxType UK interest did you get?"
    def expectedHeading(taxType: String): String = s"How much $taxType UK interest did you get from Halifax?"
    def expectedErrorTitle(taxType: String): String = s"Error: ${expectedTitle(taxType)}"
    def expectedErrorNoEntry(taxType: String): String = s"Enter the amount of $taxType UK interest you got"
    def expectedErrorInvalid(taxType: String): String = s"Enter the amount of $taxType UK interest in the correct format"
    def expectedErrorOverMax(taxType: String): String = s"The amount of $taxType UK interest must be less than £100,000,000,000"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    def expectedTitle(taxType: String): String = s"How much $taxType UK interest did your client get?"
    def expectedHeading(taxType: String): String = s"How much $taxType UK interest did your client get from Halifax?"
    def expectedErrorTitle(taxType: String): String = s"Error: ${expectedTitle(taxType)}"
    def expectedErrorNoEntry(taxType: String): String = s"Enter the amount of $taxType UK interest your client got"
    def expectedErrorInvalid(taxType: String): String = s"Enter the amount of $taxType UK interest in the correct format"
    def expectedErrorOverMax(taxType: String): String = s"The amount of $taxType UK interest must be less than £100,000,000,000"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    def expectedTitle(taxType: String): String = s"How much $taxType UK interest did your client get?"
    def expectedHeading(taxType: String): String = s"How much $taxType UK interest did your client get from Halifax?"
    def expectedErrorTitle(taxType: String): String = s"Error: ${expectedTitle(taxType)}"
    def expectedErrorNoEntry(taxType: String): String = s"Enter the amount of $taxType UK interest your client got"
    def expectedErrorInvalid(taxType: String): String = s"Enter the amount of $taxType UK interest in the correct format"
    def expectedErrorOverMax(taxType: String): String = s"The amount of $taxType UK interest must be less than £100,000,000,000"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption = "Interest for 6 April 2021 to 5 April 2022"
    val expectedHintText = "For example, £600 or £193.54"
    val poundPrefixText = "£"
    val continueText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption = "Interest for 6 April 2021 to 5 April 2022"
    val expectedHintText = "For example, £600 or £193.54"
    val poundPrefixText = "£"
    val continueText = "Continue"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }


  "calling /GET" when {

    userScenarios.foreach {
    us =>
      import Selectors._
      import us.specificExpectedResults._
      import us.commonExpectedResults._

      s"language is ${welshTest (us.isWelsh)} and request is from an ${agentTest (us.isAgent)}" should {

        s"render the $UNTAXED interest account amount page with the correct account name and amount field empty" which {

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual (us.isAgent)
            dropInterestDB()
            emptyUserDataStub ()
            urlGet(viewUrl(UNTAXED), us.isWelsh, headers = playSessionCookie (us.isAgent) )
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse (result.body)

          titleCheck (get.expectedTitle (UNTAXED) )
          h1Check (get.expectedHeading (UNTAXED) + " " + expectedCaption)
          textOnPageCheck (expectedCaption, captionSelector)
          hintTextCheck (expectedHintText)
          textOnPageCheck (poundPrefixText, poundPrefixSelector)
          inputFieldCheck (amountInputName, amountInputSelector)
          inputFieldValueCheck(emptyAmountInput,newAmountInput)
          buttonCheck (continueText, continueButtonSelector)
          formPostLinkCheck (viewUrlNoPrefix(UNTAXED), continueButtonFormSelector)

          welshToggleCheck (us.isWelsh)
        }

        s"render the $TAXED interest account amount page with the correct account name and amount field empty" which {

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual (us.isAgent)
            dropInterestDB()
            emptyUserDataStub ()
            urlGet(viewUrl(TAXED), us.isWelsh, headers = playSessionCookie (us.isAgent) )
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse (result.body)

          titleCheck (get.expectedTitle (TAXED) )
          h1Check (get.expectedHeading (TAXED) + " " + expectedCaption)
          textOnPageCheck (expectedCaption, captionSelector)
          hintTextCheck (expectedHintText)
          textOnPageCheck (poundPrefixText, poundPrefixSelector)
          inputFieldCheck (amountInputName, amountInputSelector)
          inputFieldValueCheck(emptyAmountInput,newAmountInput)
          buttonCheck (continueText, continueButtonSelector)
          formPostLinkCheck (viewUrlNoPrefix(TAXED), continueButtonFormSelector)

          welshToggleCheck (us.isWelsh)
        }

      }
    }
  }


  "call /POST" when {

    userScenarios.foreach { us =>

      import us.specificExpectedResults._

      s"language is ${welshTest(us.isWelsh)} and request is from an ${agentTest(us.isAgent)}" should {

        s"return a BAD_REQUEST($BAD_REQUEST) status with an Empty Error for $UNTAXED" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(viewUrl(UNTAXED), welsh = us.isWelsh, headers = playSessionCookie(us.isAgent), body = Map[String, String]())
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)
          errorSummaryCheck(get.expectedErrorNoEntry(UNTAXED), expectedErrorLink)
          errorAboveElementCheck(get.expectedErrorNoEntry(UNTAXED))
        }
        s"return a BAD_REQUEST($BAD_REQUEST) status with an Invalid Error for $UNTAXED" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(viewUrl(UNTAXED), welsh = us.isWelsh, headers = playSessionCookie(us.isAgent), body = Map("amount" -> "|"))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)
          errorSummaryCheck(get.expectedErrorInvalid(UNTAXED), expectedErrorLink)
          errorAboveElementCheck(get.expectedErrorInvalid(UNTAXED))
        }
        s"return a BAD_REQUEST($BAD_REQUEST) status with an OverMax Error for $UNTAXED" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(viewUrl(UNTAXED), welsh = us.isWelsh, headers = playSessionCookie(us.isAgent),
              body = Map("amount" -> "9999999999999999999999999999"))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)
          errorSummaryCheck(get.expectedErrorOverMax(UNTAXED), expectedErrorLink)
          errorAboveElementCheck(get.expectedErrorOverMax(UNTAXED))
        }



        s"return a BAD_REQUEST($BAD_REQUEST) status with an Empty Error for $TAXED" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(viewUrl(TAXED), welsh = us.isWelsh, headers = playSessionCookie(us.isAgent), body = Map[String, String]())
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)
          errorSummaryCheck(get.expectedErrorNoEntry(TAXED), expectedErrorLink)
          errorAboveElementCheck(get.expectedErrorNoEntry(TAXED))
        }
        s"return a BAD_REQUEST($BAD_REQUEST) status with an Invalid Error for $TAXED" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(viewUrl(TAXED), welsh = us.isWelsh, headers = playSessionCookie(us.isAgent), body = Map("amount" -> "|"))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)
          errorSummaryCheck(get.expectedErrorInvalid(TAXED), expectedErrorLink)
          errorAboveElementCheck(get.expectedErrorInvalid(TAXED))
        }
        s"return a BAD_REQUEST($BAD_REQUEST) status with an OverMax Error for $TAXED" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(viewUrl(TAXED), welsh = us.isWelsh, headers = playSessionCookie(us.isAgent),
              body = Map("amount" -> "9999999999999999999999999999"))
          }

          "has the correct status" in {
            result.status shouldBe BAD_REQUEST
          }
          implicit val document: () => Document = () => Jsoup.parse(result.body)
          errorSummaryCheck(get.expectedErrorOverMax(TAXED), expectedErrorLink)
          errorAboveElementCheck(get.expectedErrorOverMax(TAXED))
        }
      }
    }

  }

}
