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

package views.interest


import forms.ChangeAccountAmountForm
import models.User
import models.interest.InterestAccountModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.mvc.AnyContent
import utils.ViewTest
import views.html.interest.ChangeAccountAmountView

class ChangeAccountAmountViewSpec extends ViewTest {

  def changeAccountAmountAgentForm(taxType: String): Form[BigDecimal] = {
    val agentUser: User[AnyContent] = user.copy(arn = Some("XARN1234567890"))
    ChangeAccountAmountForm.changeAccountAmountForm(taxType)(agentUser)
  }

  def changeAccountAmountIndividualForm(taxType: String): Form[BigDecimal] = {
    val individualUser: User[AnyContent] = user
    ChangeAccountAmountForm.changeAccountAmountForm(taxType)(individualUser)
  }


  lazy val changeAccountAmountView: ChangeAccountAmountView = app.injector.instanceOf[ChangeAccountAmountView]

  val priorAmountValue = 5000
  val taxYear = 2020
  val taxYearMinusOne: Int = taxYear - 1

  val captionSelector = ".govuk-caption-l"
  val continueButtonSelector = "#continue"
  val continueFormSelector = "#main-content > div > div > form"
  val errorSummarySelector = ".govuk-error-summary"
  val errorSummaryTitleSelector = ".govuk-error-summary__title"
  val errorSummaryTextSelector = ".govuk-error-summary__body"
  val newAmountInputSelector = "#amount"
  val amountInputName = "amount"
  val youToldUsSelector = "#main-content > div > div > form > div > div > label > p"

  val expectedUntaxedTitleIndividual = "How much untaxed UK interest did you get?"
  val expectedUntaxedTitleAgent = "How much untaxed UK interest did your client get?"
  val expectedUntaxedErrorTitleIndividual = s"Error: $expectedUntaxedTitleIndividual"
  val expectedUntaxedErrorTitleAgent = s"Error: $expectedUntaxedTitleAgent"
  val expectedTaxedTitleIndividual = "How much taxed UK interest did you get?"
  val expectedTaxedTitleAgent = "How much taxed UK interest did your client get?"
  val expectedTaxedErrorTitleIndividual = s"Error: $expectedTaxedTitleIndividual"
  val expectedTaxedErrorTitleAgent = s"Error: $expectedTaxedTitleAgent"
  val expectedCaption = s"Interest for 6 April $taxYearMinusOne to 5 April $taxYear"
  val expectedUntaxedH1Individual = "Monzo: how much untaxed UK interest did you get?"
  val expectedUntaxedH1Agent = "Monzo: how much untaxed UK interest did your client get?"
  val expectedTaxedH1Individual = "Monzo: how much taxed UK interest did you get?"
  val expectedTaxedH1Agent = "Monzo: how much taxed UK interest did your client get?"
  val expectedHintText = "For example, £600 or £193.54"
  val continueText = "Continue"
  val continueLink = "/test-url"
  val differentAmountText = "A different amount"

  def youToldUsUntaxedIndividual(amount: String): String =
    s"You told us you got £$amount untaxed UK interest. Tell us if this has changed."

  def youToldUsUntaxedAgent(amount: String): String =
    s"You told us your client got £$amount untaxed UK interest. Tell us if this has changed."

  def youToldUsTaxedIndividual(amount: String): String =
    s"You told us you got £$amount taxed UK interest. Tell us if this has changed."

  def youToldUsTaxedAgent(amount: String): String =
    s"You told us your client got £$amount taxed UK interest. Tell us if this has changed."

  val account: InterestAccountModel = InterestAccountModel(Some("qwerty"), "Monzo", priorAmountValue)

  val TAXED = "taxed"
  val UNTAXED = "untaxed"

  "ChangeAccountAmountView in English" when {

    "passed a change account amount form" should {

      "correctly render for an individual with an untaxed account" when {

        "there are no form errors" which {

          lazy val view = changeAccountAmountView(
            changeAccountAmountIndividualForm(UNTAXED).fill(priorAmountValue),
            testCall,
            taxYear,
            UNTAXED,
            account,
            Some(priorAmountValue)
          )(user, messages, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedUntaxedTitleIndividual)
          welshToggleCheck("English")
          h1Check(expectedUntaxedH1Individual + " " + expectedCaption)
          textOnPageCheck(youToldUsUntaxedIndividual(priorAmountValue.toString), youToldUsSelector)
          textOnPageCheck(expectedCaption, captionSelector)
          hintTextCheck(expectedHintText)
          inputFieldCheck(amountInputName, newAmountInputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueFormSelector)

        }

        "there are form errors" when {


          "an empty value for amount is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountIndividualForm(UNTAXED).bind(Map("amount" -> "")),
              testCall,
              taxYear,
              UNTAXED,
              account,
              Some(priorAmountValue)
            )(user, messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount of untaxed UK interest you got"

            titleCheck(expectedUntaxedErrorTitleIndividual)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Individual + " " + expectedCaption)
            textOnPageCheck(youToldUsUntaxedIndividual(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

          "an invalid value for amount is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountIndividualForm(UNTAXED).bind(Map("amount" -> "abc")),
              testCall,
              taxYear,
              UNTAXED,
              account,
              Some(priorAmountValue)
            )(user, messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter an amount using numbers 0 to 9"

            titleCheck(expectedUntaxedErrorTitleIndividual)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Individual + " " + expectedCaption)
            textOnPageCheck(youToldUsUntaxedIndividual(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

          "a value greater than 100,000,000,000 is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountIndividualForm(UNTAXED).bind(Map("amount" -> "200,000,000,000")),
              testCall,
              taxYear,
              UNTAXED,
              account,
              Some(priorAmountValue)
            )(user, messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "The amount of untaxed UK interest must be less than £99,999,999,999.99"

            titleCheck(expectedUntaxedErrorTitleIndividual)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Individual + " " + expectedCaption)
            textOnPageCheck(youToldUsUntaxedIndividual(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

          "an invalid format value for amount is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountIndividualForm(UNTAXED).bind(Map("amount" -> "100.00.00")),
              testCall,
              taxYear,
              UNTAXED,
              account,
              Some(priorAmountValue)
            )(user, messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount of untaxed UK interest in the correct format"

            titleCheck(expectedUntaxedErrorTitleIndividual)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Individual + " " + expectedCaption)
            textOnPageCheck(youToldUsUntaxedIndividual(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

        }
      }

      "correctly render for an individual with a taxed account" when {

        "there are no form errors" which {

          lazy val view = changeAccountAmountView(
            changeAccountAmountIndividualForm(TAXED).fill(priorAmountValue),
            testCall,
            taxYear,
            TAXED,
            account,
            Some(priorAmountValue)
          )(user, messages, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTaxedTitleIndividual)
          welshToggleCheck("English")
          h1Check(expectedTaxedH1Individual + " " + expectedCaption)
          textOnPageCheck(youToldUsTaxedIndividual(priorAmountValue.toString), youToldUsSelector)
          textOnPageCheck(expectedCaption, captionSelector)
          hintTextCheck(expectedHintText)
          inputFieldCheck(amountInputName, newAmountInputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueFormSelector)
        }

        "there are form errors" when {

          "an empty value for amount is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountIndividualForm(TAXED).bind(Map("amount" -> "")),
              testCall,
              taxYear,
              TAXED,
              account,
              Some(priorAmountValue)
            )(user, messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount of taxed UK interest you got"

            titleCheck(expectedTaxedErrorTitleIndividual)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Individual + " " + expectedCaption)
            textOnPageCheck(youToldUsTaxedIndividual(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

          "an invalid value for amount is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountIndividualForm(TAXED).bind(Map("amount" -> "abc")),
              testCall,
              taxYear,
              TAXED,
              account,
              Some(priorAmountValue)
            )(user, messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter an amount using numbers 0 to 9"

            titleCheck(expectedTaxedErrorTitleIndividual)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Individual + " " + expectedCaption)
            textOnPageCheck(youToldUsTaxedIndividual(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

          "a value greater than 100,000,000,000 is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountIndividualForm(TAXED).bind(Map("amount" -> "200,000,000,000")),
              testCall,
              taxYear,
              TAXED,
              account,
              Some(priorAmountValue)
            )(user, messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "The amount of taxed UK interest must be less than £99,999,999,999.99"

            titleCheck(expectedTaxedErrorTitleIndividual)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Individual + " " + expectedCaption)
            textOnPageCheck(youToldUsTaxedIndividual(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

          "an invalid format value for amount is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountIndividualForm(TAXED).bind(Map("amount" -> "100.00.00")),
              testCall,
              taxYear,
              TAXED,
              account,
              Some(priorAmountValue)
            )(user, messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount of taxed UK interest in the correct format"

            titleCheck(expectedTaxedErrorTitleIndividual)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Individual + " " + expectedCaption)
            textOnPageCheck(youToldUsTaxedIndividual(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

        }
      }

      "correctly render for an agent with an untaxed account" when {

        "there are no form errors" which {

          lazy val view = changeAccountAmountView(
            changeAccountAmountAgentForm(UNTAXED).fill(priorAmountValue),
            testCall,
            taxYear,
            UNTAXED,
            account,
            Some(priorAmountValue)
          )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedUntaxedTitleAgent)
          welshToggleCheck("English")
          h1Check(expectedUntaxedH1Agent + " " + expectedCaption)
          textOnPageCheck(youToldUsUntaxedAgent(priorAmountValue.toString), youToldUsSelector)
          textOnPageCheck(expectedCaption, captionSelector)
          hintTextCheck(expectedHintText)
          inputFieldCheck(amountInputName, newAmountInputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueFormSelector)

        }

        "there are form errors" when {


          "an empty value for amount is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountAgentForm(UNTAXED).bind(Map("amount" -> "")),
              testCall,
              taxYear,
              UNTAXED,
              account,
              Some(priorAmountValue)
            )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount of untaxed UK interest your client got"

            titleCheck(expectedUntaxedErrorTitleAgent)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Agent + " " + expectedCaption)
            textOnPageCheck(youToldUsUntaxedAgent(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

          "an invalid value for amount is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountAgentForm(UNTAXED).bind(Map("amount" -> "abc")),
              testCall,
              taxYear,
              UNTAXED,
              account,
              Some(priorAmountValue)
            )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter an amount using numbers 0 to 9"

            titleCheck(expectedUntaxedErrorTitleAgent)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Agent + " " + expectedCaption)
            textOnPageCheck(youToldUsUntaxedAgent(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

          "a value greater than 100,000,000,000 is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountAgentForm(UNTAXED).bind(Map("amount" -> "200,000,000,000")),
              testCall,
              taxYear,
              UNTAXED,
              account,
              Some(priorAmountValue)
            )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "The amount of untaxed UK interest must be less than £99,999,999,999.99"

            titleCheck(expectedUntaxedErrorTitleAgent)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Agent + " " + expectedCaption)
            textOnPageCheck(youToldUsUntaxedAgent(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

          "an invalid format value for amount is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountAgentForm(UNTAXED).bind(Map("amount" -> "100.00.00")),
              testCall,
              taxYear,
              UNTAXED,
              account,
              Some(priorAmountValue)
            )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount of untaxed UK interest in the correct format"

            titleCheck(expectedUntaxedErrorTitleAgent)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Agent + " " + expectedCaption)
            textOnPageCheck(youToldUsUntaxedAgent(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

        }
      }

      "correctly render for an agent with a taxed account" when {

        "there are no form errors" which {

          lazy val view = changeAccountAmountView(
            changeAccountAmountAgentForm(TAXED).fill(priorAmountValue),
            testCall,
            taxYear,
            TAXED,
            account,
            Some(priorAmountValue)
          )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTaxedTitleAgent)
          welshToggleCheck("English")
          h1Check(expectedTaxedH1Agent + " " + expectedCaption)
          textOnPageCheck(youToldUsTaxedAgent(priorAmountValue.toString), youToldUsSelector)
          textOnPageCheck(expectedCaption, captionSelector)
          hintTextCheck(expectedHintText)
          inputFieldCheck(amountInputName, newAmountInputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueFormSelector)
        }

        "there are form errors" when {

          "an empty value for amount is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountAgentForm(TAXED).bind(Map("amount" -> "")),
              testCall,
              taxYear,
              TAXED,
              account,
              Some(priorAmountValue)
            )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount of taxed UK interest your client got"

            titleCheck(expectedTaxedErrorTitleAgent)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Agent + " " + expectedCaption)
            textOnPageCheck(youToldUsTaxedAgent(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

          "an invalid value for amount is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountAgentForm(TAXED).bind(Map("amount" -> "abc")),
              testCall,
              taxYear,
              TAXED,
              account,
              Some(priorAmountValue)
            )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter an amount using numbers 0 to 9"

            titleCheck(expectedTaxedErrorTitleAgent)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Agent + " " + expectedCaption)
            textOnPageCheck(youToldUsTaxedAgent(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

          "a value greater than 100,000,000,000 is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountAgentForm(TAXED).bind(Map("amount" -> "200,000,000,000")),
              testCall,
              taxYear,
              TAXED,
              account,
              Some(priorAmountValue)
            )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "The amount of taxed UK interest must be less than £99,999,999,999.99"

            titleCheck(expectedTaxedErrorTitleAgent)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Agent + " " + expectedCaption)
            textOnPageCheck(youToldUsTaxedAgent(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

          "an invalid format value for amount is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountAgentForm(TAXED).bind(Map("amount" -> "100.00.00")),
              testCall,
              taxYear,
              TAXED,
              account,
              Some(priorAmountValue)
            )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount of taxed UK interest in the correct format"

            titleCheck(expectedTaxedErrorTitleAgent)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Agent + " " + expectedCaption)
            textOnPageCheck(youToldUsTaxedAgent(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

        }
      }

    }
  }

  "ChangeAccountAmountView in Welsh" when {

    "passed a change account amount form" should {

      "correctly render for an individual with an untaxed account" when {

        "there are no form errors" which {

          lazy val view = changeAccountAmountView(
            changeAccountAmountIndividualForm(UNTAXED).fill(priorAmountValue),
            testCall,
            taxYear,
            UNTAXED,
            account,
            Some(priorAmountValue)
          )(user, welshMessages, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedUntaxedTitleIndividual)
          welshToggleCheck("Welsh")
          h1Check(expectedUntaxedH1Individual + " " + expectedCaption)
          textOnPageCheck(youToldUsUntaxedIndividual(priorAmountValue.toString), youToldUsSelector)
          textOnPageCheck(expectedCaption, captionSelector)
          hintTextCheck(expectedHintText)
          inputFieldCheck(amountInputName, newAmountInputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueFormSelector)

        }

        "there are form errors" when {


          "an empty value for amount is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountIndividualForm(UNTAXED).bind(Map("amount" -> "")),
              testCall,
              taxYear,
              UNTAXED,
              account,
              Some(priorAmountValue)
            )(user, welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount of untaxed UK interest you got"

            titleCheck(expectedUntaxedErrorTitleIndividual)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Individual + " " + expectedCaption)
            textOnPageCheck(youToldUsUntaxedIndividual(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

          "an invalid value for amount is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountIndividualForm(UNTAXED).bind(Map("amount" -> "abc")),
              testCall,
              taxYear,
              UNTAXED,
              account,
              Some(priorAmountValue)
            )(user, welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter an amount using numbers 0 to 9"

            titleCheck(expectedUntaxedErrorTitleIndividual)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Individual + " " + expectedCaption)
            textOnPageCheck(youToldUsUntaxedIndividual(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

          "a value greater than 100,000,000,000 is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountIndividualForm(UNTAXED).bind(Map("amount" -> "200,000,000,000")),
              testCall,
              taxYear,
              UNTAXED,
              account,
              Some(priorAmountValue)
            )(user, welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "The amount of untaxed UK interest must be less than £99,999,999,999.99"

            titleCheck(expectedUntaxedErrorTitleIndividual)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Individual + " " + expectedCaption)
            textOnPageCheck(youToldUsUntaxedIndividual(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

          "an invalid format value for amount is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountIndividualForm(UNTAXED).bind(Map("amount" -> "100.00.00")),
              testCall,
              taxYear,
              UNTAXED,
              account,
              Some(priorAmountValue)
            )(user, welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount of untaxed UK interest in the correct format"

            titleCheck(expectedUntaxedErrorTitleIndividual)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Individual + " " + expectedCaption)
            textOnPageCheck(youToldUsUntaxedIndividual(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

        }
      }

      "correctly render for an individual with a taxed account" when {

        "there are no form errors" which {

          lazy val view = changeAccountAmountView(
            changeAccountAmountIndividualForm(TAXED).fill(priorAmountValue),
            testCall,
            taxYear,
            TAXED,
            account,
            Some(priorAmountValue)
          )(user, welshMessages, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTaxedTitleIndividual)
          welshToggleCheck("Welsh")
          h1Check(expectedTaxedH1Individual + " " + expectedCaption)
          textOnPageCheck(youToldUsTaxedIndividual(priorAmountValue.toString), youToldUsSelector)
          textOnPageCheck(expectedCaption, captionSelector)
          hintTextCheck(expectedHintText)
          inputFieldCheck(amountInputName, newAmountInputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueFormSelector)
        }

        "there are form errors" when {

          "an empty value for amount is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountIndividualForm(TAXED).bind(Map("amount" -> "")),
              testCall,
              taxYear,
              TAXED,
              account,
              Some(priorAmountValue)
            )(user, welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount of taxed UK interest you got"

            titleCheck(expectedTaxedErrorTitleIndividual)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Individual + " " + expectedCaption)
            textOnPageCheck(youToldUsTaxedIndividual(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

          "an invalid value for amount is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountIndividualForm(TAXED).bind(Map("amount" -> "abc")),
              testCall,
              taxYear,
              TAXED,
              account,
              Some(priorAmountValue)
            )(user, welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter an amount using numbers 0 to 9"

            titleCheck(expectedTaxedErrorTitleIndividual)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Individual + " " + expectedCaption)
            textOnPageCheck(youToldUsTaxedIndividual(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

          "a value greater than 100,000,000,000 is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountIndividualForm(TAXED).bind(Map("amount" -> "200,000,000,000")),
              testCall,
              taxYear,
              TAXED,
              account,
              Some(priorAmountValue)
            )(user, welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "The amount of taxed UK interest must be less than £99,999,999,999.99"

            titleCheck(expectedTaxedErrorTitleIndividual)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Individual + " " + expectedCaption)
            textOnPageCheck(youToldUsTaxedIndividual(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

          "an invalid format value for amount is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountIndividualForm(TAXED).bind(Map("amount" -> "100.00.00")),
              testCall,
              taxYear,
              TAXED,
              account,
              Some(priorAmountValue)
            )(user, welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount of taxed UK interest in the correct format"

            titleCheck(expectedTaxedErrorTitleIndividual)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Individual + " " + expectedCaption)
            textOnPageCheck(youToldUsTaxedIndividual(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

        }
      }

      "correctly render for an agent with an untaxed account" when {

        "there are no form errors" which {

          lazy val view = changeAccountAmountView(
            changeAccountAmountAgentForm(UNTAXED).fill(priorAmountValue),
            testCall,
            taxYear,
            UNTAXED,
            account,
            Some(priorAmountValue)
          )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedUntaxedTitleAgent)
          welshToggleCheck("Welsh")
          h1Check(expectedUntaxedH1Agent + " " + expectedCaption)
          textOnPageCheck(youToldUsUntaxedAgent(priorAmountValue.toString), youToldUsSelector)
          textOnPageCheck(expectedCaption, captionSelector)
          hintTextCheck(expectedHintText)
          inputFieldCheck(amountInputName, newAmountInputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueFormSelector)

        }

        "there are form errors" when {


          "an empty value for amount is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountAgentForm(UNTAXED).bind(Map("amount" -> "")),
              testCall,
              taxYear,
              UNTAXED,
              account,
              Some(priorAmountValue)
            )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount of untaxed UK interest your client got"

            titleCheck(expectedUntaxedErrorTitleAgent)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Agent + " " + expectedCaption)
            textOnPageCheck(youToldUsUntaxedAgent(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

          "an invalid value for amount is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountAgentForm(UNTAXED).bind(Map("amount" -> "abc")),
              testCall,
              taxYear,
              UNTAXED,
              account,
              Some(priorAmountValue)
            )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter an amount using numbers 0 to 9"

            titleCheck(expectedUntaxedErrorTitleAgent)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Agent + " " + expectedCaption)
            textOnPageCheck(youToldUsUntaxedAgent(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

          "a value greater than 100,000,000,000 is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountAgentForm(UNTAXED).bind(Map("amount" -> "200,000,000,000")),
              testCall,
              taxYear,
              UNTAXED,
              account,
              Some(priorAmountValue)
            )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "The amount of untaxed UK interest must be less than £99,999,999,999.99"

            titleCheck(expectedUntaxedErrorTitleAgent)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Agent + " " + expectedCaption)
            textOnPageCheck(youToldUsUntaxedAgent(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

          "an invalid format value for amount is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountAgentForm(UNTAXED).bind(Map("amount" -> "100.00.00")),
              testCall,
              taxYear,
              UNTAXED,
              account,
              Some(priorAmountValue)
            )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount of untaxed UK interest in the correct format"

            titleCheck(expectedUntaxedErrorTitleAgent)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Agent + " " + expectedCaption)
            textOnPageCheck(youToldUsUntaxedAgent(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

        }
      }

      "correctly render for an agent with a taxed account" when {

        "there are no form errors" which {

          lazy val view = changeAccountAmountView(
            changeAccountAmountAgentForm(TAXED).fill(priorAmountValue),
            testCall,
            taxYear,
            TAXED,
            account,
            Some(priorAmountValue)
          )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTaxedTitleAgent)
          welshToggleCheck("Welsh")
          h1Check(expectedTaxedH1Agent + " " + expectedCaption)
          textOnPageCheck(youToldUsTaxedAgent(priorAmountValue.toString), youToldUsSelector)
          textOnPageCheck(expectedCaption, captionSelector)
          hintTextCheck(expectedHintText)
          inputFieldCheck(amountInputName, newAmountInputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueFormSelector)
        }

        "there are form errors" when {

          "an empty value for amount is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountAgentForm(TAXED).bind(Map("amount" -> "")),
              testCall,
              taxYear,
              TAXED,
              account,
              Some(priorAmountValue)
            )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount of taxed UK interest your client got"

            titleCheck(expectedTaxedErrorTitleAgent)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Agent + " " + expectedCaption)
            textOnPageCheck(youToldUsTaxedAgent(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

          "an invalid value for amount is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountAgentForm(TAXED).bind(Map("amount" -> "abc")),
              testCall,
              taxYear,
              TAXED,
              account,
              Some(priorAmountValue)
            )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter an amount using numbers 0 to 9"

            titleCheck(expectedTaxedErrorTitleAgent)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Agent + " " + expectedCaption)
            textOnPageCheck(youToldUsTaxedAgent(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

          "a value greater than 100,000,000,000 is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountAgentForm(TAXED).bind(Map("amount" -> "200,000,000,000")),
              testCall,
              taxYear,
              TAXED,
              account,
              Some(priorAmountValue)
            )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "The amount of taxed UK interest must be less than £99,999,999,999.99"

            titleCheck(expectedTaxedErrorTitleAgent)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Agent + " " + expectedCaption)
            textOnPageCheck(youToldUsTaxedAgent(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

          "an invalid format value for amount is passed in" which {
            lazy val view = changeAccountAmountView(
              changeAccountAmountAgentForm(TAXED).bind(Map("whichAmount" -> "other", "amount" -> "100.00.00")),
              testCall,
              taxYear,
              TAXED,
              account,
              Some(priorAmountValue)
            )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount of taxed UK interest in the correct format"

            titleCheck(expectedTaxedErrorTitleAgent)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Agent + " " + expectedCaption)
            textOnPageCheck(youToldUsTaxedAgent(priorAmountValue.toString), youToldUsSelector)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            inputFieldCheck(amountInputName, newAmountInputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

        }
      }

    }
  }


}
