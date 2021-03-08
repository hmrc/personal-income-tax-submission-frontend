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

import forms.YesNoForm

import models.interest.InterestAccountModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import utils.ViewTest
import views.html.interest.RemoveAccountView

class RemoveAccountViewSpec extends ViewTest {

  lazy val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm("Select yes to remove this account")

  lazy val removeAccountView: RemoveAccountView = app.injector.instanceOf[RemoveAccountView]

  val taxYear = 2020

  val TAXED = "taxed"
  val UNTAXED = "untaxed"

  val account: InterestAccountModel = InterestAccountModel(Some("qwerty"), "Monzo", 9001.00)

  val captionSelector = ".govuk-caption-l"
  val thisWillTextSelector = "#value-hint > div > p"
  val yesOptionSelector = "#main-content > div > div > form > div > fieldset > div.govuk-radios.govuk-radios--inline > div:nth-child(1) > label"
  val noOptionSelector = "#main-content > div > div > form > div > fieldset > div.govuk-radios.govuk-radios--inline > div:nth-child(2) > label"
  val continueButtonSelector = "#continue"

  val errorSummarySelector = ".govuk-error-summary"
  val errorSummaryTitleSelector = ".govuk-error-summary__title"
  val errorSummaryTextSelector = ".govuk-error-summary__body"

  val expectedTitle = "Are you sure you want to remove this account?"
  val expectedErrorTitle = s"Error: $expectedTitle"
  val expectedH1 = "Are you sure you want to remove Monzo?"
  val expectedCaption = "Interest for 6 April 2019 to 5 April 2020"
  val thisWillTextUntaxed = "This will remove all untaxed UK interest."
  val thisWillTextTaxed = "This will remove all taxed UK interest."
  val yesText = "Yes"
  val noText = "No"
  val continueText = "Continue"

  val expectedErrorText = "Select yes to remove this account"

  "Remove Account view" should {

    "Correctly render for untaxed account" when {

      "Correctly render as an individual" when {
        "There are no form errors " which {
          lazy val view = removeAccountView(yesNoForm, taxYear, UNTAXED, account, isLastAccount = false)(user, implicitly, mockAppConfig)
          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTitle)
          textOnPageCheck(expectedCaption, captionSelector)
          h1Check(expectedH1)
          //        TODO: Think of something for radio buttons
          textOnPageCheck(yesText, yesOptionSelector)
          textOnPageCheck(noText, noOptionSelector)
          buttonCheck(continueText, continueButtonSelector)
        }

        "There are form errors " when {
          "no value is passed to the form" which {
            lazy val view = removeAccountView(yesNoForm.bind(Map("value" -> "")), taxYear, UNTAXED, account,
              isLastAccount = false)(user, implicitly, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            titleCheck(expectedErrorTitle)
            errorSummaryCheck(expectedErrorText, "#value")
            textOnPageCheck(expectedCaption, captionSelector)
            h1Check(expectedH1)
            errorAboveElementCheck(expectedErrorText)
            //        TODO: Think of something for radio buttons
            textOnPageCheck(yesText, yesOptionSelector)
            textOnPageCheck(noText, noOptionSelector)
            buttonCheck(continueText, continueButtonSelector)
          }
        }

        "The last account is being removed" which {
          lazy val view = removeAccountView(yesNoForm, taxYear, UNTAXED, account,
            isLastAccount = true)(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTitle)
          textOnPageCheck(expectedCaption, captionSelector)
          h1Check(expectedH1)
          //        TODO: Think of something for radio buttons
          textOnPageCheck(thisWillTextUntaxed, thisWillTextSelector)
          textOnPageCheck(yesText, yesOptionSelector)
          textOnPageCheck(noText, noOptionSelector)
          buttonCheck(continueText, continueButtonSelector)
        }
      }

      "Correctly render as an agent" when {
        "There are no form errors " which {
          lazy val view = removeAccountView(yesNoForm, taxYear, UNTAXED, account,
            isLastAccount = false)(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTitle)
          textOnPageCheck(expectedCaption, captionSelector)
          h1Check(expectedH1)
          //        TODO: Think of something for radio buttons
          textOnPageCheck(yesText, yesOptionSelector)
          textOnPageCheck(noText, noOptionSelector)
          buttonCheck(continueText, continueButtonSelector)
        }

        "There are form errors " when {
          "no value is passed to the form" which {
            lazy val view = removeAccountView(yesNoForm.bind(Map("value" -> "")), taxYear, UNTAXED,
              account, isLastAccount = false)(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)
            implicit lazy val document: Document = Jsoup.parse(view.body)

            titleCheck(expectedErrorTitle)
            errorSummaryCheck(expectedErrorText, "#value")
            textOnPageCheck(expectedCaption, captionSelector)
            h1Check(expectedH1)
            errorAboveElementCheck(expectedErrorText)
            //        TODO: Think of something for radio buttons
            textOnPageCheck(yesText, yesOptionSelector)
            textOnPageCheck(noText, noOptionSelector)
            buttonCheck(continueText, continueButtonSelector)
          }
        }

        "The last account is being removed " which {
          lazy val view = removeAccountView(yesNoForm, taxYear, UNTAXED, account,
            isLastAccount = true)(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTitle)
          textOnPageCheck(expectedCaption, captionSelector)
          h1Check(expectedH1)
          //        TODO: Think of something for radio buttons
          textOnPageCheck(thisWillTextUntaxed, thisWillTextSelector)
          textOnPageCheck(yesText, yesOptionSelector)
          textOnPageCheck(noText, noOptionSelector)
          buttonCheck(continueText, continueButtonSelector)
        }
      }
    }


    "Correctly render for taxed account" when {

      "Correctly render as an individual" when {
        "There are no form errors " which {
          lazy val view = removeAccountView(yesNoForm, taxYear, TAXED, account, isLastAccount = false)(user, implicitly, mockAppConfig)
          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTitle)
          textOnPageCheck(expectedCaption, captionSelector)
          h1Check(expectedH1)
          //        TODO: Think of something for radio buttons
          textOnPageCheck(yesText, yesOptionSelector)
          textOnPageCheck(noText, noOptionSelector)
          buttonCheck(continueText, continueButtonSelector)
        }

        "There are form errors " when {
          "no value is passed to the form" which {
            lazy val view = removeAccountView(yesNoForm.bind(Map("value" -> "")), taxYear, TAXED, account,
              isLastAccount = false)(user, implicitly, mockAppConfig)

            implicit lazy val document: Document = Jsoup.parse(view.body)

            titleCheck(expectedErrorTitle)
            errorSummaryCheck(expectedErrorText, "#value")
            textOnPageCheck(expectedCaption, captionSelector)
            h1Check(expectedH1)
            errorAboveElementCheck(expectedErrorText)
            //        TODO: Think of something for radio buttons
            textOnPageCheck(yesText, yesOptionSelector)
            textOnPageCheck(noText, noOptionSelector)
            buttonCheck(continueText, continueButtonSelector)
          }
        }

        "The last account is being removed" which {
          lazy val view = removeAccountView(yesNoForm, taxYear, TAXED, account,
            isLastAccount = true)(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTitle)
          textOnPageCheck(expectedCaption, captionSelector)
          h1Check(expectedH1)
          //        TODO: Think of something for radio buttons
          textOnPageCheck(thisWillTextTaxed, thisWillTextSelector)
          textOnPageCheck(yesText, yesOptionSelector)
          textOnPageCheck(noText, noOptionSelector)
          buttonCheck(continueText, continueButtonSelector)
        }
      }

      "Correctly render as an agent" when {
        "There are no form errors " which {
          lazy val view = removeAccountView(yesNoForm, taxYear, TAXED, account,
            isLastAccount = false)(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTitle)
          textOnPageCheck(expectedCaption, captionSelector)
          h1Check(expectedH1)
          //        TODO: Think of something for radio buttons
          textOnPageCheck(yesText, yesOptionSelector)
          textOnPageCheck(noText, noOptionSelector)
          buttonCheck(continueText, continueButtonSelector)
        }

        "There are form errors " when {
          "no value is passed to the form" which {
            lazy val view = removeAccountView(yesNoForm.bind(Map("value" -> "")), taxYear, TAXED,
              account, isLastAccount = false)(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)
            implicit lazy val document: Document = Jsoup.parse(view.body)

            titleCheck(expectedErrorTitle)
            errorSummaryCheck(expectedErrorText, "#value")
            textOnPageCheck(expectedCaption, captionSelector)
            h1Check(expectedH1)
            errorAboveElementCheck(expectedErrorText)
            //        TODO: Think of something for radio buttons
            textOnPageCheck(yesText, yesOptionSelector)
            textOnPageCheck(noText, noOptionSelector)
            buttonCheck(continueText, continueButtonSelector)
          }
        }

        "The last account is being removed " which {
          lazy val view = removeAccountView(yesNoForm, taxYear, TAXED, account,
            isLastAccount = true)(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          titleCheck(expectedTitle)
          textOnPageCheck(expectedCaption, captionSelector)
          h1Check(expectedH1)
          //        TODO: Think of something for radio buttons
          textOnPageCheck(thisWillTextTaxed, thisWillTextSelector)
          textOnPageCheck(yesText, yesOptionSelector)
          textOnPageCheck(noText, noOptionSelector)
          buttonCheck(continueText, continueButtonSelector)
        }
      }
    }
  }
}
