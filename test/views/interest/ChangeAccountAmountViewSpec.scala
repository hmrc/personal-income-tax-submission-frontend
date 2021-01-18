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
  lazy val priorOrNewAmountForm: Form[PriorOrNewAmountModel] = PriorOrNewAmountForm.priorOrNewAmountForm(5000)
  lazy val changeAccountAmountView: ChangeAccountAmountView = app.injector.instanceOf[ChangeAccountAmountView]

  val h1Selector = "h1"
  val captionSelector = ".govuk-caption-l"
  val inputSelector = ".govuk-input"
  val continueButtonSelector = "#continue"

  val errorSummarySelector = ".govuk-error-summary"
  val errorSummaryTitle = ".govuk-error-summary__title"
  val errorSummaryText = ".govuk-error-summary__body"

  val expectedUntaxedH1 = "Monzo untaxed interest earned"
  val expectedTaxedH1 = "Monzo taxed interest earned"

  val expectedUntaxedTitle = "Untaxed interest earned - Register your income tax return with HMRC - Gov.UK"
  val expectedTaxedTitle = "Taxed interest earned - Register your income tax return with HMRC - Gov.UK"

  val expectedCaption = "Interest for 06 April 2019 to 05 April 2020"

  val expectedErrorTitle = "There is a problem"
  val expectedErrorText = "Select £5000 or enter a different amount"

  val priorAmountRadio = "#prior-amount"
  val priorAmountRadioText = "#main-content > div > div > form > div > fieldset > div > div:nth-child(1) > label"
  val newAmountRadio = "#other-amount"
  val newAmountInput = "#other-amount-input"

  val account = InterestAccountModel(Some("qwerty"), "Monzo", 5000)

  val taxYear = 2020

  val TAXED = "taxed"
  val UNTAXED = "untaxed"


  "ChangeAccountAmountView" when {

    "passed a prior or new form" should {

      "correctly render with no errors as an individual with an untaxed account" when {

        "there are no form errors" which {

          lazy val view = changeAccountAmountView(
            priorOrNewAmountForm,
            testCall,
            taxYear,
            UNTAXED,
            account
          )(user, implicitly, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          "contains the correct h1" in {
            elementText(h1Selector) shouldBe expectedUntaxedH1
          }

          "contains the correct title" in {
            document.title shouldBe expectedUntaxedTitle
          }

          "contains the correct header caption" in {
            elementText(captionSelector) shouldBe expectedCaption
          }

          "contains a prior amount radio button" in {
            elementExist(priorAmountRadio) shouldBe true
          }

          "prior amount radio button contains amount returned in prior amount model" in {
            elementText(priorAmountRadioText) shouldBe "£5000"
          }

          "contains a new amount radio button" in {
            elementExist(newAmountRadio) shouldBe true
          }

          "new amount radio button is already selected" in {
            element(newAmountRadio).attributes().hasKey("checked") shouldBe true
          }

          "contains a new amount input field" in {
            elementExist(newAmountInput) shouldBe true
          }

          "contains a continue button" in {
            elementExist(continueButtonSelector) shouldBe true
          }

        }

      }

      "correctly render with errors as an individual with an untaxed account" when {

        "there are form errors" which {

          lazy val view = changeAccountAmountView(
            priorOrNewAmountForm.withError("amount","Select £5000 or enter a different amount"),
            testCall,
            taxYear,
            UNTAXED,
            account
          )(user, implicitly, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          "contains the correct h1" in {
            elementText(h1Selector) shouldBe expectedUntaxedH1
          }

          "contain the correct h1" in {
            elementText(h1Selector) shouldBe expectedUntaxedH1
          }

          "contains the correct header caption" in {
            elementText(captionSelector) shouldBe expectedCaption
          }

          "contains an input box" in {
            elementExist(inputSelector) shouldBe true
          }

          "contains a continue button" in {
            elementExist(continueButtonSelector) shouldBe true
          }

          "contains an error" in {
            elementExist(errorSummarySelector) shouldBe true
          }

          "contain an error title" in {
            elementText(errorSummaryTitle) shouldBe expectedErrorTitle
          }

          "contains an error message" in {
            elementText(errorSummaryText) shouldBe expectedErrorText
          }

        }

      }

      "correctly render with no errors as an individual with a taxed account" when {

        "there are no form errors" which {

          lazy val view = changeAccountAmountView(
            priorOrNewAmountForm,
            testCall,
            taxYear,
            TAXED,
            account
          )(user, implicitly, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          "contains the correct h1" in {
            elementText(h1Selector) shouldBe expectedTaxedH1
          }

          "contains the correct title" in {
            document.title shouldBe expectedTaxedTitle
          }

          "contains the correct header caption" in {
            elementText(captionSelector) shouldBe expectedCaption
          }

          "contains a prior amount radio button" in {
            elementExist(priorAmountRadio) shouldBe true
          }

          "prior amount radio button contains amount returned in prior amount model" in {
            elementText(priorAmountRadioText) shouldBe "£5000"
          }

          "contains a new amount radio button" in {
            elementExist(newAmountRadio) shouldBe true
          }

          "new amount radio button is already selected" in {
            element(newAmountRadio).attributes().hasKey("checked") shouldBe true
          }

          "contains a new amount input field" in {
            elementExist(newAmountInput) shouldBe true
          }

          "contains a continue button" in {
            elementExist(continueButtonSelector) shouldBe true
          }

        }

      }

      "correctly render with errors as an individual with a taxed account" when {

        "there are form errors" which {

          lazy val view = changeAccountAmountView(
            priorOrNewAmountForm.withError("amount","Select £5000 or enter a different amount"),
            testCall,
            taxYear,
            TAXED,
            account
          )(user, implicitly, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          "contains the correct h1" in {
            elementText(h1Selector) shouldBe expectedTaxedH1
          }

          "contain the correct h1" in {
            elementText(h1Selector) shouldBe expectedTaxedH1
          }

          "contains the correct header caption" in {
            elementText(captionSelector) shouldBe expectedCaption
          }

          "contains an input box" in {
            elementExist(inputSelector) shouldBe true
          }

          "contains a continue button" in {
            elementExist(continueButtonSelector) shouldBe true
          }

          "contains an error" in {
            elementExist(errorSummarySelector) shouldBe true
          }

          "contain an error title" in {
            elementText(errorSummaryTitle) shouldBe expectedErrorTitle
          }

          "contains an error message" in {
            elementText(errorSummaryText) shouldBe expectedErrorText
          }

        }

      }

    }

  }

}
