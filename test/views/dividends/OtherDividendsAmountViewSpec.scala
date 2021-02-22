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

import forms.{OtherDividendsAmountForm, PriorOrNewAmountForm}
import models.formatHelpers.PriorOrNewAmountModel
import models.DividendsPriorSubmission
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import utils.ViewTest
import views.html.dividends.OtherUkDividendsAmountView

class OtherDividendsAmountViewSpec extends ViewTest {

  lazy val otherDividendsAmountForm: Form[BigDecimal] = OtherDividendsAmountForm.otherDividendsAmountForm()
  lazy val priorOrNewAmountForm: Form[PriorOrNewAmountModel] = PriorOrNewAmountForm.priorOrNewAmountForm(20)

  lazy val otherDividendsAmountView: OtherUkDividendsAmountView = app.injector.instanceOf[OtherUkDividendsAmountView]

  val taxYear: Int = 2020

  val h1Selector = "h1"
  val captionSelector = ".govuk-caption-l"
  val inputSelector = ".govuk-input"
  val continueButtonSelector = "#continue"

  val errorSummarySelector = ".govuk-error-summary"
  val errorSummaryTitle = ".govuk-error-summary__title"
  val errorSummaryText = ".govuk-error-summary__body"

  val expectedH1 = "What is the total amount of dividends from trusts or open ended investment companies?"
  val expectedTitle = s"$expectedH1 - $serviceName - $govUkExtension"
  val expectedCaption = "Dividends for 06 April 2019 to 05 April 2020"

  val expectedErrorTitle = "There is a problem"
  val expectedErrorText = "Enter the amount of dividends received from trusts or investment companies"

  val priorAmountRadio = "#whichAmount"
  val priorAmountRadioText = "#main-content > div > div > form > div > div > fieldset > div > div:nth-child(1) > label"
  val newAmountRadio = "#otherAmount"
  val newAmountInput = "#amount"

  "OtherDividendsAmountView" when {

    "passed an amount currency form" should {

      "correctly render with no errors as an individual" when {

        "there are no form errors" which {

          lazy val view = otherDividendsAmountView(Right(otherDividendsAmountForm), None, taxYear, testCall)(user, implicitly, mockAppConfig)
          implicit lazy val document: Document = Jsoup.parse(view.body)

          "contain the correct title" in {
            document.title shouldBe expectedTitle
          }

          "contain the correct h1" in {
            elementText(h1Selector) shouldBe expectedH1
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

        }

      }

      "correctly render with errors as an individual" when {

        "there are form errors" which {

          lazy val view = otherDividendsAmountView(
            Right(otherDividendsAmountForm.copy(errors = Seq(FormError("amount", "Enter the amount of dividends received from trusts or investment companies")))),
            None,
            taxYear,
            testCall
          )(user, implicitly, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          "contain the correct title" in {
            document.title shouldBe expectedTitle
          }

          "contain the correct h1" in {
            elementText(h1Selector) shouldBe expectedH1
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

      "correctly render with no errors as an agent" when {

        "there are no form errors" which {

          lazy val view = otherDividendsAmountView(
            Right(otherDividendsAmountForm), None, taxYear, testCall
          )(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          "contain the correct title" in {
            document.title shouldBe expectedTitle
          }

          "contain the correct h1" in {
            elementText(h1Selector) shouldBe expectedH1
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

        }

      }

      "correctly render with errors as an agent" when {

        "there is a form error" which {

          lazy val view = otherDividendsAmountView(
            Right(otherDividendsAmountForm.copy(
              errors = Seq(FormError("amount", "Enter the amount of dividends received from trusts or investment companies")))),
            None,
            taxYear,
            testCall
          )(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          "contain the correct title" in {
            document.title shouldBe expectedTitle
          }

          "contain the correct h1" in {
            elementText(h1Selector) shouldBe expectedH1
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

    "passed a prior or new form" should {

      "correctly render with no errors as an individual" when {

        "there are no form errors" which {

          lazy val view = otherDividendsAmountView(
            Left(priorOrNewAmountForm.fill(PriorOrNewAmountModel("other",None))),
            Some(DividendsPriorSubmission(None, Some(40))),
            taxYear,
            testCall
          )(user, implicitly, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          "contain the correct title" in {
            document.title shouldBe expectedTitle
          }

          "contain the correct h1" in {
            elementText(h1Selector) shouldBe expectedH1
          }

          "contains the correct header caption" in {
            elementText(captionSelector) shouldBe expectedCaption
          }

          "contains a prior amount radio button" in {
            elementExist(priorAmountRadio) shouldBe true
          }

          "prior amount radio button contains amount returned in prior amount model" in {
            elementText(priorAmountRadioText) shouldBe "£40"
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

      "correctly render with errors as an individual" when {

        "there are form errors" which {

          lazy val view = otherDividendsAmountView(
            Left(priorOrNewAmountForm.withError("amount", "Enter the amount of dividends received from trusts or investment companies")),
            None,
            taxYear,
            testCall
          )(user, implicitly, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          "contain the correct title" in {
            document.title shouldBe expectedTitle
          }

          "contain the correct h1" in {
            elementText(h1Selector) shouldBe expectedH1
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

      "correctly render with no errors as an agent" when {

        "there are no form errors" which {

          lazy val view = otherDividendsAmountView(
            Left(priorOrNewAmountForm), None, taxYear, testCall
          )(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          "contain the correct title" in {
            document.title shouldBe expectedTitle
          }

          "contain the correct h1" in {
            elementText(h1Selector) shouldBe expectedH1
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

        }

      }

      "correctly render with errors as an agent" when {

        "there is a form error" which {

          lazy val view = otherDividendsAmountView(
            Left(priorOrNewAmountForm.withError("amount", "Enter the amount of dividends received from trusts or investment companies")),
            None,
            taxYear,
            testCall,
          )(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          "contain the correct title" in {
            document.title shouldBe expectedTitle
          }

          "contain the correct h1" in {
            elementText(h1Selector) shouldBe expectedH1
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
