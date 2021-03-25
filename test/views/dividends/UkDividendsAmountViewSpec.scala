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

package views.dividends

import forms.{PriorOrNewAmountForm, UkDividendsAmountForm}
import models.DividendsPriorSubmission
import models.formatHelpers.PriorOrNewAmountModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import utils.ViewTest
import views.html.dividends.UkDividendsAmountView

class UkDividendsAmountViewSpec extends ViewTest {

  val priorAmount = 20

  lazy val ukDividendsAmountForm: Form[BigDecimal] = UkDividendsAmountForm.ukDividendsAmountForm()
  lazy val priorOrNewAmountForm: Form[PriorOrNewAmountModel] = PriorOrNewAmountForm.priorOrNewAmountForm(priorAmount)

  lazy val ukDividendsAmountView: UkDividendsAmountView = app.injector.instanceOf[UkDividendsAmountView]

  val taxYear: Int = 2020
  val taxYearMinusOne: Int = taxYear -1

  val poundPrefixSelector = ".govuk-input__prefix"
  val captionSelector = ".govuk-caption-l"
  val inputSelector = ".govuk-input"
  val continueButtonSelector = "#continue"
  val continueButtonFormSelector = "#main-content > div > div > form"
  val enterAmountSelector = "#conditional-otherAmount > div > label"
  val priorAmountSelector = "#main-content > div > div > form > div > div > fieldset > div > div:nth-child(1) > label"

  val expectedH1 = "What is the total amount of dividends earned from companies in the UK?"
  val expectedTitle = "What is the total amount of dividends earned from companies in the UK?"
  val expectedErrorTitle = s"Error: $expectedTitle"
  val expectedCaption = s"Dividends for 6 April $taxYearMinusOne to 5 April $taxYear"
  val poundPrefixText = "£"
  val differentAmountText = "A different amount"
  val enterAmountText = "Enter amount"
  val continueText = "Continue"
  val continueLink = "/test-url"

  val priorAmountRadio = "#whichAmount"
  val priorAmountRadioText = "#main-content > div > div > form > div > div > fieldset > div > div:nth-child(1) > label"
  val newAmountRadio = "#otherAmount"
  val newAmountInput = "#amount"
  val amountInputName = "amount"

  "UkDividendsAmountView" should {

    "Render successfully without prior data" when {

      "correctly render for an individual" when {

        "there are no form errors" which {

          lazy val view = ukDividendsAmountView(Right(ukDividendsAmountForm), None, taxYear, testCall)(user, implicitly, mockAppConfig)
          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTitle)
          h1Check(expectedH1)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
        }

        "there are form errors" when {

          "an empty value is passed in" which {

            lazy val view = ukDividendsAmountView(
              Right(ukDividendsAmountForm.bind(Map("amount" -> ""))),
              None,
              taxYear,
              testCall
            )(user, implicitly, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter the amount of dividends earned from the UK"

            titleCheck(expectedErrorTitle)
            h1Check(expectedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)

          }

          "a non numeric value is passed in" which {

            lazy val view = ukDividendsAmountView(
              Right(ukDividendsAmountForm.bind(Map("amount" -> "abc"))),
              None,
              taxYear,
              testCall
            )(user, implicitly, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter an amount using numbers 0 to 9"

            titleCheck(expectedErrorTitle)
            h1Check(expectedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)

          }

          "a value bigger than £100,000,000,000 is passed in" which {

            lazy val view = ukDividendsAmountView(
              Right(ukDividendsAmountForm.bind(Map("amount" -> "200,000,000,000"))),
              None,
              taxYear,
              testCall
            )(user, implicitly, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter an amount less than £100,000,000,000"

            titleCheck(expectedErrorTitle)
            h1Check(expectedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)

          }

          "an invalid format value is passed in" which {

            lazy val view = ukDividendsAmountView(
              Right(ukDividendsAmountForm.bind(Map("amount" -> "10.00.00.00"))),
              None,
              taxYear,
              testCall
            )(user, implicitly, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter the amount in the correct format"

            titleCheck(expectedErrorTitle)
            h1Check(expectedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
          }
        }
      }

      "correctly render for an agent" when {

        "there are no form errors" which {

          lazy val view = ukDividendsAmountView(Right(ukDividendsAmountForm), None, taxYear,
            testCall)(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)
          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTitle)
          h1Check(expectedH1)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
        }

        "there are form errors" when {

          "an empty value is passed in" which {

            lazy val view = ukDividendsAmountView(
              Right(ukDividendsAmountForm.bind(Map("amount" -> ""))),
              None,
              taxYear,
              testCall
            )(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter the amount of dividends earned from the UK"

            titleCheck(expectedErrorTitle)
            h1Check(expectedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)

          }

          "a non numeric value is passed in" which {

            lazy val view = ukDividendsAmountView(
              Right(ukDividendsAmountForm.bind(Map("amount" -> "abc"))),
              None,
              taxYear,
              testCall
            )(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter an amount using numbers 0 to 9"

            titleCheck(expectedErrorTitle)
            h1Check(expectedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)

          }

          "a value bigger than £100,000,000,000 is passed in" which {

            lazy val view = ukDividendsAmountView(
              Right(ukDividendsAmountForm.bind(Map("amount" -> "200,000,000,000"))),
              None,
              taxYear,
              testCall
            )(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter an amount less than £100,000,000,000"

            titleCheck(expectedErrorTitle)
            h1Check(expectedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)

          }

          "an invalid format value is passed in" which {

            lazy val view = ukDividendsAmountView(
              Right(ukDividendsAmountForm.bind(Map("amount" -> "10.00.00.00"))),
              None,
              taxYear,
              testCall
            )(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter the amount in the correct format"

            titleCheck(expectedErrorTitle)
            h1Check(expectedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
          }
        }
      }

    }

    "Render successfully with prior data" when {

      "correctly render for an individual" when {

        "there are no form errors" which {

          lazy val view = ukDividendsAmountView(
                      Left(priorOrNewAmountForm.fill(PriorOrNewAmountModel("other",None))),
                      Some(DividendsPriorSubmission(Some(priorAmount))),
                      taxYear,
                      testCall
                    )(user, implicitly, mockAppConfig)
          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTitle)
          h1Check(expectedH1)
          textOnPageCheck(expectedCaption, captionSelector)
          radioButtonCheck(s"£$priorAmount", 1)
          radioButtonCheck(differentAmountText, 2)
          textOnPageCheck(enterAmountText, enterAmountSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
        }

        "there are form errors" when {

          "an empty value is passed in" which {
            lazy val view = ukDividendsAmountView(
              Left(priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> ""))),
              Some(DividendsPriorSubmission(Some(priorAmount))),
              taxYear,
              testCall
            )(user, implicitly, mockAppConfig)
            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter the amount in the correct format"

            titleCheck(expectedErrorTitle)
            h1Check(expectedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            radioButtonCheck(s"£$priorAmount", 1)
            radioButtonCheck(differentAmountText, 2)
            textOnPageCheck(enterAmountText, enterAmountSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
          }

          "a non numeric value is passed in" which {
            lazy val view = ukDividendsAmountView(
              Left(priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "abc"))),
              Some(DividendsPriorSubmission(Some(priorAmount))),
              taxYear,
              testCall
            )(user, implicitly, mockAppConfig)
            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter an amount using numbers 0 to 9"

            titleCheck(expectedErrorTitle)
            h1Check(expectedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            radioButtonCheck(s"£$priorAmount", 1)
            radioButtonCheck(differentAmountText, 2)
            textOnPageCheck(enterAmountText, enterAmountSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
          }

          "a value bigger than £100,000,000,000 is passed in" which {
            lazy val view = ukDividendsAmountView(
              Left(priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "200,000,000,000"))),
              Some(DividendsPriorSubmission(Some(priorAmount))),
              taxYear,
              testCall
            )(user, implicitly, mockAppConfig)
            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter an amount less than £100,000,000,000"

            titleCheck(expectedErrorTitle)
            h1Check(expectedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            radioButtonCheck(s"£$priorAmount", 1)
            radioButtonCheck(differentAmountText, 2)
            textOnPageCheck(enterAmountText, enterAmountSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
          }

          "an invalid format value is passed in" which {
            lazy val view = ukDividendsAmountView(
              Left(priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "100.000.00.00"))),
              Some(DividendsPriorSubmission(Some(priorAmount))),
              taxYear,
              testCall
            )(user, implicitly, mockAppConfig)
            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter the amount in the correct format"

            titleCheck(expectedErrorTitle)
            h1Check(expectedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            radioButtonCheck(s"£$priorAmount", 1)
            radioButtonCheck(differentAmountText, 2)
            textOnPageCheck(enterAmountText, enterAmountSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
          }
        }
      }
    }

    "Render successfully with prior data" when {

      "correctly render for an agent" when {

        "there are no form errors" which {

          lazy val view = ukDividendsAmountView(
            Left(priorOrNewAmountForm.fill(PriorOrNewAmountModel("other",None))),
            Some(DividendsPriorSubmission(Some(priorAmount))),
            taxYear,
            testCall
          )(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)
          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTitle)
          h1Check(expectedH1)
          textOnPageCheck(expectedCaption, captionSelector)
          radioButtonCheck(s"£$priorAmount", 1)
          radioButtonCheck(differentAmountText, 2)
          textOnPageCheck(enterAmountText, enterAmountSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
        }

        "there are form errors" when {

          "an empty value is passed in" which {
            lazy val view = ukDividendsAmountView(
              Left(priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> ""))),
              Some(DividendsPriorSubmission(Some(priorAmount))),
              taxYear,
              testCall
            )(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)
            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter the amount in the correct format"

            titleCheck(expectedErrorTitle)
            h1Check(expectedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            radioButtonCheck(s"£$priorAmount", 1)
            radioButtonCheck(differentAmountText, 2)
            textOnPageCheck(enterAmountText, enterAmountSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
          }

          "a non numeric value is passed in" which {
            lazy val view = ukDividendsAmountView(
              Left(priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "abc"))),
              Some(DividendsPriorSubmission(Some(priorAmount))),
              taxYear,
              testCall
            )(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)
            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter an amount using numbers 0 to 9"

            titleCheck(expectedErrorTitle)
            h1Check(expectedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            radioButtonCheck(s"£$priorAmount", 1)
            radioButtonCheck(differentAmountText, 2)
            textOnPageCheck(enterAmountText, enterAmountSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
          }

          "a value bigger than £100,000,000,000 is passed in" which {
            lazy val view = ukDividendsAmountView(
              Left(priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "200,000,000,000"))),
              Some(DividendsPriorSubmission(Some(priorAmount))),
              taxYear,
              testCall
            )(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)
            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter an amount less than £100,000,000,000"

            titleCheck(expectedErrorTitle)
            h1Check(expectedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            radioButtonCheck(s"£$priorAmount", 1)
            radioButtonCheck(differentAmountText, 2)
            textOnPageCheck(enterAmountText, enterAmountSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
          }

          "an invalid format value is passed in" which {
            lazy val view = ukDividendsAmountView(
              Left(priorOrNewAmountForm.bind(Map("whichAmount" -> "other", "amount" -> "100.000.00.00"))),
              Some(DividendsPriorSubmission(Some(priorAmount))),
              taxYear,
              testCall
            )(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)
            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter the amount in the correct format"

            titleCheck(expectedErrorTitle)
            h1Check(expectedH1)
            textOnPageCheck(expectedCaption, captionSelector)
            radioButtonCheck(s"£$priorAmount", 1)
            radioButtonCheck(differentAmountText, 2)
            textOnPageCheck(enterAmountText, enterAmountSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(continueLink, continueButtonFormSelector)
          }
        }
      }
    }
  }
}
