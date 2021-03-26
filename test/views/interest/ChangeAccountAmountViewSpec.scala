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
  val priorAmountRadioText = "#main-content > div > div > form > div > div > fieldset > div > div:nth-child(1) > label"
  val newAmountRadio = "#otherAmount"
  val newAmountInput = "#amount"
  val amountInputName = "amount"

  val expectedUntaxedTitle = "Untaxed interest earned"
  val expectedUntaxedErrorTitle = s"Error: $expectedUntaxedTitle"
  val expectedTaxedTitle = "Taxed interest earned"
  val expectedTaxedErrorTitle = s"Error: $expectedTaxedTitle"
  val expectedCaption = s"Interest for 6 April $taxYearMinusOne to 5 April $taxYear"
  val expectedUntaxedH1 = "Monzo untaxed interest earned"
  val expectedTaxedH1 = "Monzo taxed interest earned"
  val expectedHintText = "For example, £600 or £193.54"
  val continueText = "Continue"
  val continueLink = "/test-url"
  val differentAmountText = "A different amount"

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

          titleCheck(expectedUntaxedTitle)
          welshToggleCheck("English")
          h1Check(expectedUntaxedH1)
          textOnPageCheck(expectedCaption, captionSelector)
          radioButtonCheck(s"£$priorAmountValue", 1)
          radioButtonCheck(differentAmountText, 2)

          "new amount radio button is already selected" in {
            element(newAmountRadio).attributes().hasKey("checked") shouldBe true
          }

          hintTextCheck(expectedHintText)
          textOnPageCheck(s"£$priorAmountValue", priorAmountRadioText)
          inputFieldCheck(amountInputName, newAmountInput)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueFormSelector)

        }

        "there are form errors" when {

          "neither radio button is chosen" which {
            lazy val view = changeAccountAmountView(
              priorOrNewAmountForm.bind(Map("whichAmount" -> "", "amount" -> "")),
              testCall,
              taxYear,
              UNTAXED,
              account
            )(user, messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = s"Select £5,000 or enter a different amount"

            titleCheck(expectedUntaxedErrorTitle)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, "#whichAmount")
            h1Check(expectedUntaxedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            radioButtonCheck(s"£$priorAmountValue", 1)
            radioButtonCheck(differentAmountText, 2)

            "new amount radio button is not selected" in {
              element(newAmountRadio).attributes().hasKey("checked") shouldBe false
            }

            inputFieldCheck(amountInputName, newAmountInput)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

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

            titleCheck(expectedUntaxedErrorTitle)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInput)
            h1Check(expectedUntaxedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            radioButtonCheck(s"£$priorAmountValue", 1)
            radioButtonCheck(differentAmountText, 2)

            "new amount radio button is already selected" in {
              element(newAmountRadio).attributes().hasKey("checked") shouldBe true
            }

            inputFieldCheck(amountInputName, newAmountInput)
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

            titleCheck(expectedUntaxedErrorTitle)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInput)
            h1Check(expectedUntaxedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            radioButtonCheck(s"£$priorAmountValue", 1)
            radioButtonCheck(differentAmountText, 2)

            "new amount radio button is already selected" in {
              element(newAmountRadio).attributes().hasKey("checked") shouldBe true
            }

            inputFieldCheck(amountInputName, newAmountInput)
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

            titleCheck(expectedUntaxedErrorTitle)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInput)
            h1Check(expectedUntaxedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            radioButtonCheck(s"£$priorAmountValue", 1)
            radioButtonCheck(differentAmountText, 2)

            "new amount radio button is already selected" in {
              element(newAmountRadio).attributes().hasKey("checked") shouldBe true
            }

            inputFieldCheck(amountInputName, newAmountInput)
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

            titleCheck(expectedUntaxedErrorTitle)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInput)
            h1Check(expectedUntaxedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            radioButtonCheck(s"£$priorAmountValue", 1)
            radioButtonCheck(differentAmountText, 2)

            "new amount radio button is already selected" in {
              element(newAmountRadio).attributes().hasKey("checked") shouldBe true
            }

            inputFieldCheck(amountInputName, newAmountInput)
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

          titleCheck(expectedTaxedTitle)
          welshToggleCheck("English")
          h1Check(expectedTaxedH1)
          textOnPageCheck(expectedCaption, captionSelector)
          hintTextCheck(expectedHintText)
          radioButtonCheck(s"£$priorAmountValue", 1)
          radioButtonCheck(differentAmountText, 2)

          "new amount radio button is already selected" in {
            element(newAmountRadio).attributes().hasKey("checked") shouldBe true
          }

          inputFieldCheck(amountInputName, newAmountInput)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueFormSelector)
        }

        "there are form errors" when {

          "neither radio button is chosen" which {
            lazy val view = changeAccountAmountView(
              priorOrNewAmountForm.bind(Map("whichAmount" -> "", "amount" -> "")),
              testCall,
              taxYear,
              TAXED,
              account
            )(user, messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = s"Select £5,000 or enter a different amount"

            titleCheck(expectedTaxedErrorTitle)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, "#whichAmount")
            h1Check(expectedTaxedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            radioButtonCheck(s"£$priorAmountValue", 1)
            radioButtonCheck(differentAmountText, 2)

            "new amount radio button is already selected" in {
              element(newAmountRadio).attributes().hasKey("checked") shouldBe false
            }

            inputFieldCheck(amountInputName, newAmountInput)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

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

            titleCheck(expectedTaxedErrorTitle)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInput)
            h1Check(expectedTaxedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            radioButtonCheck(s"£$priorAmountValue", 1)
            radioButtonCheck(differentAmountText, 2)

            "new amount radio button is already selected" in {
              element(newAmountRadio).attributes().hasKey("checked") shouldBe true
            }

            inputFieldCheck(amountInputName, newAmountInput)
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

            titleCheck(expectedTaxedErrorTitle)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInput)
            h1Check(expectedTaxedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            radioButtonCheck(s"£$priorAmountValue", 1)
            radioButtonCheck(differentAmountText, 2)

            "new amount radio button is already selected" in {
              element(newAmountRadio).attributes().hasKey("checked") shouldBe true
            }

            inputFieldCheck(amountInputName, newAmountInput)
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

            titleCheck(expectedTaxedErrorTitle)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInput)
            h1Check(expectedTaxedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            radioButtonCheck(s"£$priorAmountValue", 1)
            radioButtonCheck(differentAmountText, 2)

            "new amount radio button is already selected" in {
              element(newAmountRadio).attributes().hasKey("checked") shouldBe true
            }

            inputFieldCheck(amountInputName, newAmountInput)
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

            titleCheck(expectedTaxedErrorTitle)
            welshToggleCheck("English")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInput)
            h1Check(expectedTaxedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            radioButtonCheck(s"£$priorAmountValue", 1)
            radioButtonCheck(differentAmountText, 2)

            "new amount radio button is already selected" in {
              element(newAmountRadio).attributes().hasKey("checked") shouldBe true
            }

            inputFieldCheck(amountInputName, newAmountInput)
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

          titleCheck(expectedUntaxedTitle)
          welshToggleCheck("Welsh")
          h1Check(expectedUntaxedH1)
          textOnPageCheck(expectedCaption, captionSelector)
          radioButtonCheck(s"£$priorAmountValue", 1)
          radioButtonCheck(differentAmountText, 2)

          "new amount radio button is already selected" in {
            element(newAmountRadio).attributes().hasKey("checked") shouldBe true
          }

          hintTextCheck(expectedHintText)
          textOnPageCheck(s"£$priorAmountValue", priorAmountRadioText)
          inputFieldCheck(amountInputName, newAmountInput)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueFormSelector)

        }

        "there are form errors" when {

          "neither radio button is chosen" which {
            lazy val view = changeAccountAmountView(
              priorOrNewAmountForm.bind(Map("whichAmount" -> "", "amount" -> "")),
              testCall,
              taxYear,
              UNTAXED,
              account
            )(user, welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = s"Select £5,000 or enter a different amount"

            titleCheck(expectedUntaxedErrorTitle)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, "#whichAmount")
            h1Check(expectedUntaxedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            radioButtonCheck(s"£$priorAmountValue", 1)
            radioButtonCheck(differentAmountText, 2)

            "new amount radio button is not selected" in {
              element(newAmountRadio).attributes().hasKey("checked") shouldBe false
            }

            inputFieldCheck(amountInputName, newAmountInput)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

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

            titleCheck(expectedUntaxedErrorTitle)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInput)
            h1Check(expectedUntaxedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            radioButtonCheck(s"£$priorAmountValue", 1)
            radioButtonCheck(differentAmountText, 2)

            "new amount radio button is already selected" in {
              element(newAmountRadio).attributes().hasKey("checked") shouldBe true
            }

            inputFieldCheck(amountInputName, newAmountInput)
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

            titleCheck(expectedUntaxedErrorTitle)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInput)
            h1Check(expectedUntaxedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            radioButtonCheck(s"£$priorAmountValue", 1)
            radioButtonCheck(differentAmountText, 2)

            "new amount radio button is already selected" in {
              element(newAmountRadio).attributes().hasKey("checked") shouldBe true
            }

            inputFieldCheck(amountInputName, newAmountInput)
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

            titleCheck(expectedUntaxedErrorTitle)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInput)
            h1Check(expectedUntaxedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            radioButtonCheck(s"£$priorAmountValue", 1)
            radioButtonCheck(differentAmountText, 2)

            "new amount radio button is already selected" in {
              element(newAmountRadio).attributes().hasKey("checked") shouldBe true
            }

            inputFieldCheck(amountInputName, newAmountInput)
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

            titleCheck(expectedUntaxedErrorTitle)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInput)
            h1Check(expectedUntaxedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            radioButtonCheck(s"£$priorAmountValue", 1)
            radioButtonCheck(differentAmountText, 2)

            "new amount radio button is already selected" in {
              element(newAmountRadio).attributes().hasKey("checked") shouldBe true
            }

            inputFieldCheck(amountInputName, newAmountInput)
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

          titleCheck(expectedTaxedTitle)
          welshToggleCheck("Welsh")
          h1Check(expectedTaxedH1)
          textOnPageCheck(expectedCaption, captionSelector)
          hintTextCheck(expectedHintText)
          radioButtonCheck(s"£$priorAmountValue", 1)
          radioButtonCheck(differentAmountText, 2)

          "new amount radio button is already selected" in {
            element(newAmountRadio).attributes().hasKey("checked") shouldBe true
          }

          inputFieldCheck(amountInputName, newAmountInput)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueFormSelector)
        }

        "there are form errors" when {

          "neither radio button is chosen" which {
            lazy val view = changeAccountAmountView(
              priorOrNewAmountForm.bind(Map("whichAmount" -> "", "amount" -> "")),
              testCall,
              taxYear,
              TAXED,
              account
            )(user, welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorSummaryText = s"Select £5,000 or enter a different amount"

            titleCheck(expectedTaxedErrorTitle)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, "#whichAmount")
            h1Check(expectedTaxedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            radioButtonCheck(s"£$priorAmountValue", 1)
            radioButtonCheck(differentAmountText, 2)

            "new amount radio button is already selected" in {
              element(newAmountRadio).attributes().hasKey("checked") shouldBe false
            }

            inputFieldCheck(amountInputName, newAmountInput)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

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

            titleCheck(expectedTaxedErrorTitle)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInput)
            h1Check(expectedTaxedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            radioButtonCheck(s"£$priorAmountValue", 1)
            radioButtonCheck(differentAmountText, 2)

            "new amount radio button is already selected" in {
              element(newAmountRadio).attributes().hasKey("checked") shouldBe true
            }

            inputFieldCheck(amountInputName, newAmountInput)
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

            titleCheck(expectedTaxedErrorTitle)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInput)
            h1Check(expectedTaxedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            radioButtonCheck(s"£$priorAmountValue", 1)
            radioButtonCheck(differentAmountText, 2)

            "new amount radio button is already selected" in {
              element(newAmountRadio).attributes().hasKey("checked") shouldBe true
            }

            inputFieldCheck(amountInputName, newAmountInput)
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

            titleCheck(expectedTaxedErrorTitle)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInput)
            h1Check(expectedTaxedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            radioButtonCheck(s"£$priorAmountValue", 1)
            radioButtonCheck(differentAmountText, 2)

            "new amount radio button is already selected" in {
              element(newAmountRadio).attributes().hasKey("checked") shouldBe true
            }

            inputFieldCheck(amountInputName, newAmountInput)
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

            titleCheck(expectedTaxedErrorTitle)
            welshToggleCheck("Welsh")
            errorSummaryCheck(expectedErrorSummaryText, newAmountInput)
            h1Check(expectedTaxedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            hintTextCheck(expectedHintText)
            errorAboveElementCheck(expectedErrorSummaryText)
            radioButtonCheck(s"£$priorAmountValue", 1)
            radioButtonCheck(differentAmountText, 2)

            "new amount radio button is already selected" in {
              element(newAmountRadio).attributes().hasKey("checked") shouldBe true
            }

            inputFieldCheck(amountInputName, newAmountInput)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueFormSelector)
          }

        }
      }

    }
  }
}
