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

import forms.UkDividendsAmountForm
import models.{DividendsPriorSubmission, User}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.api.mvc.AnyContent
import utils.ViewTest
import views.html.dividends.UkDividendsAmountView

class UkDividendsAmountViewSpec extends ViewTest {

  val priorAmount = 20

  def ukDividendsAmountAgentForm: Form[BigDecimal] = {
    val agentUser: User[AnyContent] = user.copy(arn = Some("XARN1234567890"))
    UkDividendsAmountForm.ukDividendsAmountForm(agentUser)
  }

  def ukDividendsAmountIndividualForm: Form[BigDecimal] = {
    val individualUser: User[AnyContent] = user
    UkDividendsAmountForm.ukDividendsAmountForm(individualUser)
  }

  lazy val ukDividendsAmountView: UkDividendsAmountView = app.injector.instanceOf[UkDividendsAmountView]

  val taxYear: Int = 2020
  val taxYearMinusOne: Int = taxYear -1

  val poundPrefixSelector = ".govuk-input__prefix"
  val captionSelector = ".govuk-caption-l"
  val inputSelector = ".govuk-input"
  val continueButtonSelector = "#continue"
  val continueButtonFormSelector = "#main-content > div > div > form"
  val youToldUsSelector = "#main-content > div > div > form > div > label > p"

  val expectedH1 = "How much did you get in dividends from UK-based companies?"
  val expectedTitle = "How much did you get in dividends from UK-based companies?"
  val expectedTitleAgent = "How much did your client get in dividends from UK-based companies?"
  val expectedH1Agent = "How much did your client get in dividends from UK-based companies?"
  val expectedErrorTitle = s"Error: $expectedTitle"
  val expectedErrorTitleAgent = s"Error: $expectedTitleAgent"
  val expectedCaption = s"Dividends for 6 April $taxYearMinusOne to 5 April $taxYear"
  val poundPrefixText = "£"
  val continueText = "Continue"
  val continueLink = "/test-url"

  val newAmountInput = "#amount"
  val amountInputName = "amount"

  def youToldUsPriorTextIndividual(amount: String): String =
    s"You told us you got £$amount in dividends from UK-based companies this year. Tell us if this has changed."

  def youToldUsPriorTextAgent(amount: String): String =
    s"You told us your client got £$amount in dividends from UK-based companies this year. Tell us if this has changed."

  "UkDividendsAmountView in English" should {

    "Render successfully without prior data" when {

      "correctly render for an individual" when {

        "there are no form errors" which {

          lazy val view = ukDividendsAmountView(ukDividendsAmountIndividualForm, None, taxYear, testCall)(user, messages, mockAppConfig)
          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTitle)
          welshToggleCheck("English")
          h1Check( expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
        }

        "there are form errors" when {

          "an empty value is passed in" which {

            lazy val view = ukDividendsAmountView(
              ukDividendsAmountIndividualForm.bind(Map("amount" -> "")),
              None,
              taxYear,
              testCall
            )(user, messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter how much you got in dividends from UK-based companies"

            titleCheck(expectedErrorTitle)
            welshToggleCheck("English")
            h1Check( expectedH1 + " " + expectedCaption)
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
              ukDividendsAmountIndividualForm.bind(Map("amount" -> "abc")),
              None,
              taxYear,
              testCall
            )(user, messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter how much you got in dividends in the correct format"

            titleCheck(expectedErrorTitle)
            welshToggleCheck("English")
            h1Check( expectedH1 + " " + expectedCaption)
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
              ukDividendsAmountIndividualForm.bind(Map("amount" -> "200,000,000,000")),
              None,
              taxYear,
              testCall
            )(user, messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "The amount of dividends from UK-based companies must be less than £100,000,000,000"

            titleCheck(expectedErrorTitle)
            welshToggleCheck("English")
            h1Check( expectedH1 + " " + expectedCaption)
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
              ukDividendsAmountIndividualForm.bind(Map("amount" -> "10.00.00.00")),
              None,
              taxYear,
              testCall
            )(user, messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter how much you got in dividends in the correct format"

            titleCheck(expectedErrorTitle)
            welshToggleCheck("English")
            h1Check( expectedH1 + " " + expectedCaption)
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

          lazy val view = ukDividendsAmountView(ukDividendsAmountAgentForm, None, taxYear,
            testCall)(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)
          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTitleAgent)
          welshToggleCheck("English")
          h1Check( expectedH1Agent + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
        }

        "there are form errors" when {

          "an empty value is passed in" which {

            lazy val view = ukDividendsAmountView(
              ukDividendsAmountAgentForm.bind(Map("amount" -> "")),
              None,
              taxYear,
              testCall
            )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter how much your client got in dividends from UK-based companies"

            titleCheck(expectedErrorTitleAgent)
            welshToggleCheck("English")
            h1Check( expectedH1Agent + " " + expectedCaption)
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
              ukDividendsAmountAgentForm.bind(Map("amount" -> "abc")),
              None,
              taxYear,
              testCall
            )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter how much your client got in dividends in the correct format"

            titleCheck(expectedErrorTitleAgent)
            welshToggleCheck("English")
            h1Check( expectedH1Agent + " " + expectedCaption)
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
              ukDividendsAmountAgentForm.bind(Map("amount" -> "200,000,000,000")),
              None,
              taxYear,
              testCall
            )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "The amount of dividends from UK-based companies must be less than £100,000,000,000"

            titleCheck(expectedErrorTitleAgent)
            welshToggleCheck("English")
            h1Check( expectedH1Agent + " " + expectedCaption)
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
              ukDividendsAmountAgentForm.bind(Map("amount" -> "10.00.00.00")),
              None,
              taxYear,
              testCall
            )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter how much your client got in dividends in the correct format"

            titleCheck(expectedErrorTitleAgent)
            welshToggleCheck("English")
            h1Check( expectedH1Agent + " " + expectedCaption)
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
                      ukDividendsAmountIndividualForm,
                      Some(DividendsPriorSubmission(Some(priorAmount))),
                      taxYear,
                      testCall
                    )(user, messages, mockAppConfig)
          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTitle)
          welshToggleCheck("English")
          h1Check( expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(youToldUsPriorTextIndividual(priorAmount.toString), youToldUsSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
        }

        "there are form errors" when {

          "an empty value is passed in" which {
            lazy val view = ukDividendsAmountView(
              ukDividendsAmountIndividualForm.bind(Map("amount" -> "")),
              Some(DividendsPriorSubmission(Some(priorAmount))),
              taxYear,
              testCall
            )(user, messages, mockAppConfig)
            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter how much you got in dividends from UK-based companies"

            titleCheck(expectedErrorTitle)
            welshToggleCheck("English")
            h1Check( expectedH1 + " " + expectedCaption)
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
              ukDividendsAmountIndividualForm.bind(Map("amount" -> "abc")),
              Some(DividendsPriorSubmission(Some(priorAmount))),
              taxYear,
              testCall
            )(user, messages, mockAppConfig)
            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter how much you got in dividends in the correct format"

            titleCheck(expectedErrorTitle)
            welshToggleCheck("English")
            h1Check( expectedH1 + " " + expectedCaption)
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
              ukDividendsAmountIndividualForm.bind(Map("amount" -> "200,000,000,000")),
              Some(DividendsPriorSubmission(Some(priorAmount))),
              taxYear,
              testCall
            )(user, messages, mockAppConfig)
            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "The amount of dividends from UK-based companies must be less than £100,000,000,000"

            titleCheck(expectedErrorTitle)
            welshToggleCheck("English")
            h1Check( expectedH1 + " " + expectedCaption)
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
              ukDividendsAmountIndividualForm.bind(Map("amount" -> "100.000.00.00")),
              Some(DividendsPriorSubmission(Some(priorAmount))),
              taxYear,
              testCall
            )(user, messages, mockAppConfig)
            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter how much you got in dividends in the correct format"

            titleCheck(expectedErrorTitle)
            welshToggleCheck("English")
            h1Check( expectedH1 + " " + expectedCaption)
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

      "correctly render for an agent" when {

        "there are no form errors" which {

          lazy val view = ukDividendsAmountView(
            ukDividendsAmountAgentForm,
            Some(DividendsPriorSubmission(Some(priorAmount))),
            taxYear,
            testCall
          )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)
          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTitleAgent)
          welshToggleCheck("English")
          h1Check( expectedH1Agent + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          textOnPageCheck(youToldUsPriorTextAgent(priorAmount.toString), youToldUsSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
        }

        "there are form errors" when {

          "an empty value is passed in" which {
            lazy val view = ukDividendsAmountView(
              ukDividendsAmountAgentForm.bind(Map("amount" -> "")),
              Some(DividendsPriorSubmission(Some(priorAmount))),
              taxYear,
              testCall
            )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)
            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter how much your client got in dividends from UK-based companies"

            titleCheck(expectedErrorTitleAgent)
            welshToggleCheck("English")
            h1Check( expectedH1Agent + " " + expectedCaption)
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
              ukDividendsAmountAgentForm.bind(Map("amount" -> "abc")),
              Some(DividendsPriorSubmission(Some(priorAmount))),
              taxYear,
              testCall
            )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)
            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter how much your client got in dividends in the correct format"

            titleCheck(expectedErrorTitleAgent)
            welshToggleCheck("English")
            h1Check( expectedH1Agent + " " + expectedCaption)
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
              ukDividendsAmountAgentForm.bind(Map("amount" -> "200,000,000,000")),
              Some(DividendsPriorSubmission(Some(priorAmount))),
              taxYear,
              testCall
            )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)
            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "The amount of dividends from UK-based companies must be less than £100,000,000,000"

            titleCheck(expectedErrorTitleAgent)
            welshToggleCheck("English")
            h1Check( expectedH1Agent + " " + expectedCaption)
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
              ukDividendsAmountAgentForm.bind(Map("amount" -> "100.000.00.00")),
              Some(DividendsPriorSubmission(Some(priorAmount))),
              taxYear,
              testCall
            )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)
            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter how much your client got in dividends in the correct format"

            titleCheck(expectedErrorTitleAgent)
            welshToggleCheck("English")
            h1Check( expectedH1Agent + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)
          }
        }
      }
    }
  }

  "UkDividendsAmountView in Welsh" should {

    "Render successfully without prior data" when {

      "correctly render for an individual" when {

        "there are no form errors" which {

          lazy val view = ukDividendsAmountView(ukDividendsAmountIndividualForm, None, taxYear, testCall)(user, welshMessages, mockAppConfig)
          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTitle)
          welshToggleCheck("Welsh")
          h1Check( expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
        }

        "there are form errors" when {

          "an empty value is passed in" which {

            lazy val view = ukDividendsAmountView(
              ukDividendsAmountIndividualForm.bind(Map("amount" -> "")),
              None,
              taxYear,
              testCall
            )(user, welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter how much you got in dividends from UK-based companies"

            titleCheck(expectedErrorTitle)
            welshToggleCheck("Welsh")
            h1Check( expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)

          }

          "a non numeric value is passed in" which {

            lazy val view = ukDividendsAmountView(
              ukDividendsAmountIndividualForm.bind(Map("amount" -> "abc")),
              None,
              taxYear,
              testCall
            )(user, welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter how much you got in dividends in the correct format"

            titleCheck(expectedErrorTitle)
            welshToggleCheck("Welsh")
            h1Check( expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)

          }

          "a value bigger than £100,000,000,000 is passed in" which {

            lazy val view = ukDividendsAmountView(
              ukDividendsAmountIndividualForm.bind(Map("amount" -> "200,000,000,000")),
              None,
              taxYear,
              testCall
            )(user, welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "The amount of dividends from UK-based companies must be less than £100,000,000,000"

            titleCheck(expectedErrorTitle)
            welshToggleCheck("Welsh")
            h1Check( expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)

          }

          "an invalid format value is passed in" which {

            lazy val view = ukDividendsAmountView(
              ukDividendsAmountIndividualForm.bind(Map("amount" -> "10.00.00.00")),
              None,
              taxYear,
              testCall
            )(user, welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter how much you got in dividends in the correct format"

            titleCheck(expectedErrorTitle)
            welshToggleCheck("Welsh")
            h1Check( expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)
          }
        }
      }

      "correctly render for an agent" when {

        "there are no form errors" which {

          lazy val view = ukDividendsAmountView(ukDividendsAmountAgentForm, None, taxYear,
            testCall)(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)
          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTitleAgent)
          welshToggleCheck("Welsh")
          h1Check( expectedH1Agent + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
        }

        "there are form errors" when {

          "an empty value is passed in" which {

            lazy val view = ukDividendsAmountView(
              ukDividendsAmountAgentForm.bind(Map("amount" -> "")),
              None,
              taxYear,
              testCall
            )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter how much your client got in dividends from UK-based companies"

            titleCheck(expectedErrorTitleAgent)
            welshToggleCheck("Welsh")
            h1Check( expectedH1Agent + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)

          }

          "a non numeric value is passed in" which {

            lazy val view = ukDividendsAmountView(
              ukDividendsAmountAgentForm.bind(Map("amount" -> "abc")),
              None,
              taxYear,
              testCall
            )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter how much your client got in dividends in the correct format"

            titleCheck(expectedErrorTitleAgent)
            welshToggleCheck("Welsh")
            h1Check( expectedH1Agent + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)

          }

          "a value bigger than £100,000,000,000 is passed in" which {

            lazy val view = ukDividendsAmountView(
              ukDividendsAmountAgentForm.bind(Map("amount" -> "200,000,000,000")),
              None,
              taxYear,
              testCall
            )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "The amount of dividends from UK-based companies must be less than £100,000,000,000"

            titleCheck(expectedErrorTitleAgent)
            welshToggleCheck("Welsh")
            h1Check( expectedH1Agent + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)

          }

          "an invalid format value is passed in" which {

            lazy val view = ukDividendsAmountView(
              ukDividendsAmountAgentForm.bind(Map("amount" -> "10.00.00.00")),
              None,
              taxYear,
              testCall
            )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter how much your client got in dividends in the correct format"

            titleCheck(expectedErrorTitleAgent)
            welshToggleCheck("Welsh")
            h1Check( expectedH1Agent + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)
          }
        }
      }

    }

    "Render successfully with prior data" when {

      "correctly render for an individual" when {

        "there are no form errors" which {

          lazy val view = ukDividendsAmountView(
            ukDividendsAmountIndividualForm,
            Some(DividendsPriorSubmission(Some(priorAmount))),
            taxYear,
            testCall
          )(user, welshMessages, mockAppConfig)
          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTitle)
          welshToggleCheck("Welsh")
          h1Check( expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
        }

        "there are form errors" when {

          "an empty value is passed in" which {
            lazy val view = ukDividendsAmountView(
              ukDividendsAmountIndividualForm.bind(Map("amount" -> "")),
              Some(DividendsPriorSubmission(Some(priorAmount))),
              taxYear,
              testCall
            )(user, welshMessages, mockAppConfig)
            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter how much you got in dividends from UK-based companies"

            titleCheck(expectedErrorTitle)
            welshToggleCheck("Welsh")
            h1Check( expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)
          }

          "a non numeric value is passed in" which {
            lazy val view = ukDividendsAmountView(
              ukDividendsAmountIndividualForm.bind(Map("amount" -> "abc")),
              Some(DividendsPriorSubmission(Some(priorAmount))),
              taxYear,
              testCall
            )(user, welshMessages, mockAppConfig)
            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter how much you got in dividends in the correct format"

            titleCheck(expectedErrorTitle)
            welshToggleCheck("Welsh")
            h1Check( expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)
          }

          "a value bigger than £100,000,000,000 is passed in" which {
            lazy val view = ukDividendsAmountView(
              ukDividendsAmountIndividualForm.bind(Map("amount" -> "200,000,000,000")),
              Some(DividendsPriorSubmission(Some(priorAmount))),
              taxYear,
              testCall
            )(user, welshMessages, mockAppConfig)
            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "The amount of dividends from UK-based companies must be less than £100,000,000,000"

            titleCheck(expectedErrorTitle)
            welshToggleCheck("Welsh")
            h1Check( expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)
          }

          "an invalid format value is passed in" which {
            lazy val view = ukDividendsAmountView(
              ukDividendsAmountIndividualForm.bind(Map("amount" -> "100.000.00.00")),
              Some(DividendsPriorSubmission(Some(priorAmount))),
              taxYear,
              testCall
            )(user, welshMessages, mockAppConfig)
            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter how much you got in dividends in the correct format"

            titleCheck(expectedErrorTitle)
            welshToggleCheck("Welsh")
            h1Check( expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)
          }
        }
      }
    }

    "Render successfully with prior data" when {

      "correctly render for an agent" when {

        "there are no form errors" which {

          lazy val view = ukDividendsAmountView(
            ukDividendsAmountAgentForm,
            Some(DividendsPriorSubmission(Some(priorAmount))),
            taxYear,
            testCall
          )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)
          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTitleAgent)
          welshToggleCheck("Welsh")
          h1Check( expectedH1Agent + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(poundPrefixText, poundPrefixSelector)
          inputFieldCheck(amountInputName, inputSelector)
          buttonCheck(continueText, continueButtonSelector)
        }

        "there are form errors" when {

          "an empty value is passed in" which {
            lazy val view = ukDividendsAmountView(
              ukDividendsAmountAgentForm.bind(Map("amount" -> "")),
              Some(DividendsPriorSubmission(Some(priorAmount))),
              taxYear,
              testCall
            )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)
            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter how much your client got in dividends from UK-based companies"

            titleCheck(expectedErrorTitleAgent)
            welshToggleCheck("Welsh")
            h1Check( expectedH1Agent + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)
          }

          "a non numeric value is passed in" which {
            lazy val view = ukDividendsAmountView(
              ukDividendsAmountAgentForm.bind(Map("amount" -> "abc")),
              Some(DividendsPriorSubmission(Some(priorAmount))),
              taxYear,
              testCall
            )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)
            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter how much your client got in dividends in the correct format"

            titleCheck(expectedErrorTitleAgent)
            welshToggleCheck("Welsh")
            h1Check( expectedH1Agent + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)
          }

          "a value bigger than £100,000,000,000 is passed in" which {
            lazy val view = ukDividendsAmountView(
              ukDividendsAmountAgentForm.bind(Map("amount" -> "200,000,000,000")),
              Some(DividendsPriorSubmission(Some(priorAmount))),
              taxYear,
              testCall
            )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)
            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "The amount of dividends from UK-based companies must be less than £100,000,000,000"

            titleCheck(expectedErrorTitleAgent)
            welshToggleCheck("Welsh")
            h1Check( expectedH1Agent + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            errorSummaryCheck(expectedErrorText, newAmountInput)
            errorAboveElementCheck(expectedErrorText)
            textOnPageCheck(poundPrefixText, poundPrefixSelector)
            inputFieldCheck(amountInputName, inputSelector)
            buttonCheck(continueText, continueButtonSelector)
          }

          "an invalid format value is passed in" which {
            lazy val view = ukDividendsAmountView(
              ukDividendsAmountAgentForm.bind(Map("amount" -> "100.000.00.00")),
              Some(DividendsPriorSubmission(Some(priorAmount))),
              taxYear,
              testCall
            )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)
            implicit lazy val document: Document = Jsoup.parse(view.body)

            val expectedErrorText = "Enter how much your client got in dividends in the correct format"

            titleCheck(expectedErrorTitleAgent)
            welshToggleCheck("Welsh")
            h1Check( expectedH1Agent + " " + expectedCaption)
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
  }
}
