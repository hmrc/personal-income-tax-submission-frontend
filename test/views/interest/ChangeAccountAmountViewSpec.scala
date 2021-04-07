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

import forms.PriorOrNewAmountForm
import models.formatHelpers.PriorOrNewAmountModel
import models.interest.InterestAccountModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import utils.ViewTest
import views.html.interest.ChangeAccountAmountView

class ChangeAccountAmountViewSpec extends ViewTest {
  lazy val priorOrNewAmountForm: Form[PriorOrNewAmountModel] = PriorOrNewAmountForm.priorOrNewAmountForm(5000.00)
  lazy val changeAccountAmountView: ChangeAccountAmountView = app.injector.instanceOf[ChangeAccountAmountView]

  val priorAmountValue = 5000
  val taxYear = 2020
  val taxYearMinusOne: Int = taxYear -1

  val h1Selector = "h1"
  val captionSelector = ".govuk-caption-l"
  val inputSelector = ".govuk-input"
  val continueButtonSelector = "#continue"
  val continueFormSelector = "#main-content > div > div > form"
  val errorSummarySelector = ".govuk-error-summary"
  val errorSummaryTitleSelector = ".govuk-error-summary__title"
  val errorSummaryTextSelector = ".govuk-error-summary__body"
  val newAmountInputSelector = "#amount"
  val amountInputName = "amount"
  val youToldUsSelector = "#main-content > div > div > form > div > div > label"

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

    "passed a prior or new form" should {

      "correctly render for an individual with an untaxed account" when {

        "there are no form errors" which {

          lazy val view = changeAccountAmountView(
            priorOrNewAmountForm.fill(PriorOrNewAmountModel("other",None)),
            testCall,
            taxYear,
            UNTAXED,
            account
          )(user, messages, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedUntaxedTitleIndividual)
          welshToggleCheck("English")
          h1Check(expectedUntaxedH1Individual)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "")),
              testCall,
              taxYear,
              UNTAXED,
              account
            )(user, messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount in the correct format"

            titleCheck(expectedUntaxedErrorTitleIndividual)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Individual)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "abc")),
              testCall,
              taxYear,
              UNTAXED,
              account
            )(user, messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter an amount using numbers 0 to 9"

            titleCheck(expectedUntaxedErrorTitleIndividual)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Individual)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "200,000,000,000")),
              testCall,
              taxYear,
              UNTAXED,
              account
            )(user, messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter an amount less than £100,000,000,000"

            titleCheck(expectedUntaxedErrorTitleIndividual)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Individual)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "100.00.00")),
              testCall,
              taxYear,
              UNTAXED,
              account
            )(user, messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount in the correct format"

            titleCheck(expectedUntaxedErrorTitleIndividual)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Individual)
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
            priorOrNewAmountForm.fill(PriorOrNewAmountModel("other",None)),
            testCall,
            taxYear,
            TAXED,
            account
          )(user, messages, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTaxedTitleIndividual)
          welshToggleCheck("English")
          h1Check(expectedTaxedH1Individual)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "")),
              testCall,
              taxYear,
              TAXED,
              account
            )(user, messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount in the correct format"

            titleCheck(expectedTaxedErrorTitleIndividual)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Individual)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "abc")),
              testCall,
              taxYear,
              TAXED,
              account
            )(user, messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter an amount using numbers 0 to 9"

            titleCheck(expectedTaxedErrorTitleIndividual)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Individual)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "200,000,000,000")),
              testCall,
              taxYear,
              TAXED,
              account
            )(user, messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter an amount less than £100,000,000,000"

            titleCheck(expectedTaxedErrorTitleIndividual)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Individual)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "100.00.00")),
              testCall,
              taxYear,
              TAXED,
              account
            )(user, messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount in the correct format"

            titleCheck(expectedTaxedErrorTitleIndividual)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Individual)
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
            priorOrNewAmountForm.fill(PriorOrNewAmountModel("other",None)),
            testCall,
            taxYear,
            UNTAXED,
            account
          )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedUntaxedTitleAgent)
          welshToggleCheck("English")
          h1Check(expectedUntaxedH1Agent)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "")),
              testCall,
              taxYear,
              UNTAXED,
              account
            )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount in the correct format"

            titleCheck(expectedUntaxedErrorTitleAgent)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Agent)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "abc")),
              testCall,
              taxYear,
              UNTAXED,
              account
            )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter an amount using numbers 0 to 9"

            titleCheck(expectedUntaxedErrorTitleAgent)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Agent)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "200,000,000,000")),
              testCall,
              taxYear,
              UNTAXED,
              account
            )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter an amount less than £100,000,000,000"

            titleCheck(expectedUntaxedErrorTitleAgent)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Agent)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "100.00.00")),
              testCall,
              taxYear,
              UNTAXED,
              account
            )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount in the correct format"

            titleCheck(expectedUntaxedErrorTitleAgent)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Agent)
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
            priorOrNewAmountForm.fill(PriorOrNewAmountModel("other",None)),
            testCall,
            taxYear,
            TAXED,
            account
          )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTaxedTitleAgent)
          welshToggleCheck("English")
          h1Check(expectedTaxedH1Agent)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "")),
              testCall,
              taxYear,
              TAXED,
              account
            )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount in the correct format"

            titleCheck(expectedTaxedErrorTitleAgent)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Agent)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "abc")),
              testCall,
              taxYear,
              TAXED,
              account
            )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter an amount using numbers 0 to 9"

            titleCheck(expectedTaxedErrorTitleAgent)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Agent)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "200,000,000,000")),
              testCall,
              taxYear,
              TAXED,
              account
            )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter an amount less than £100,000,000,000"

            titleCheck(expectedTaxedErrorTitleAgent)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Agent)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "100.00.00")),
              testCall,
              taxYear,
              TAXED,
              account
            )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount in the correct format"

            titleCheck(expectedTaxedErrorTitleAgent)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Agent)
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

    "passed a prior or new form" should {

      "correctly render for an individual with an untaxed account" when {

        "there are no form errors" which {

          lazy val view = changeAccountAmountView(
            priorOrNewAmountForm.fill(PriorOrNewAmountModel("other",None)),
            testCall,
            taxYear,
            UNTAXED,
            account
          )(user, welshMessages, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedUntaxedTitleIndividual)
          welshToggleCheck("Welsh")
          h1Check(expectedUntaxedH1Individual)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "")),
              testCall,
              taxYear,
              UNTAXED,
              account
            )(user, welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount in the correct format"

            titleCheck(expectedUntaxedErrorTitleIndividual)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Individual)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "abc")),
              testCall,
              taxYear,
              UNTAXED,
              account
            )(user, welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter an amount using numbers 0 to 9"

            titleCheck(expectedUntaxedErrorTitleIndividual)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Individual)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "200,000,000,000")),
              testCall,
              taxYear,
              UNTAXED,
              account
            )(user, welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter an amount less than £100,000,000,000"

            titleCheck(expectedUntaxedErrorTitleIndividual)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Individual)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "100.00.00")),
              testCall,
              taxYear,
              UNTAXED,
              account
            )(user, welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount in the correct format"

            titleCheck(expectedUntaxedErrorTitleIndividual)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Individual)
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
            priorOrNewAmountForm.fill(PriorOrNewAmountModel("other",None)),
            testCall,
            taxYear,
            TAXED,
            account
          )(user, welshMessages, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTaxedTitleIndividual)
          welshToggleCheck("Welsh")
          h1Check(expectedTaxedH1Individual)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "")),
              testCall,
              taxYear,
              TAXED,
              account
            )(user, welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount in the correct format"

            titleCheck(expectedTaxedErrorTitleIndividual)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Individual)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "abc")),
              testCall,
              taxYear,
              TAXED,
              account
            )(user, welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter an amount using numbers 0 to 9"

            titleCheck(expectedTaxedErrorTitleIndividual)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Individual)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "200,000,000,000")),
              testCall,
              taxYear,
              TAXED,
              account
            )(user, welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter an amount less than £100,000,000,000"

            titleCheck(expectedTaxedErrorTitleIndividual)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Individual)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "100.00.00")),
              testCall,
              taxYear,
              TAXED,
              account
            )(user, welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount in the correct format"

            titleCheck(expectedTaxedErrorTitleIndividual)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Individual)
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
            priorOrNewAmountForm.fill(PriorOrNewAmountModel("other",None)),
            testCall,
            taxYear,
            UNTAXED,
            account
          )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedUntaxedTitleAgent)
          welshToggleCheck("Welsh")
          h1Check(expectedUntaxedH1Agent)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "")),
              testCall,
              taxYear,
              UNTAXED,
              account
            )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount in the correct format"

            titleCheck(expectedUntaxedErrorTitleAgent)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Agent)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "abc")),
              testCall,
              taxYear,
              UNTAXED,
              account
            )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter an amount using numbers 0 to 9"

            titleCheck(expectedUntaxedErrorTitleAgent)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Agent)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "200,000,000,000")),
              testCall,
              taxYear,
              UNTAXED,
              account
            )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter an amount less than £100,000,000,000"

            titleCheck(expectedUntaxedErrorTitleAgent)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Agent)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "100.00.00")),
              testCall,
              taxYear,
              UNTAXED,
              account
            )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount in the correct format"

            titleCheck(expectedUntaxedErrorTitleAgent)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedUntaxedH1Agent)
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
            priorOrNewAmountForm.fill(PriorOrNewAmountModel("other",None)),
            testCall,
            taxYear,
            TAXED,
            account
          )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTaxedTitleAgent)
          welshToggleCheck("Welsh")
          h1Check(expectedTaxedH1Agent)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "")),
              testCall,
              taxYear,
              TAXED,
              account
            )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount in the correct format"

            titleCheck(expectedTaxedErrorTitleAgent)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Agent)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "abc")),
              testCall,
              taxYear,
              TAXED,
              account
            )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter an amount using numbers 0 to 9"

            titleCheck(expectedTaxedErrorTitleAgent)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Agent)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "200,000,000,000")),
              testCall,
              taxYear,
              TAXED,
              account
            )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter an amount less than £100,000,000,000"

            titleCheck(expectedTaxedErrorTitleAgent)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Agent)
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
              priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "100.00.00")),
              testCall,
              taxYear,
              TAXED,
              account
            )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = "Enter the amount in the correct format"

            titleCheck(expectedTaxedErrorTitleAgent)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInputSelector)
            h1Check(expectedTaxedH1Agent)
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
