/*
 * Copyright 2020 HM Revenue & Customs
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

import forms.OtherDividendsAmountForm
import models.CurrencyAmountModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import utils.ViewTest
import views.html.dividends.OtherDividendsAmountView

class OtherDividendsAmountViewSpec extends ViewTest {

  lazy val otherDividendsAmountForm: Form[CurrencyAmountModel] = OtherDividendsAmountForm.otherDividendsAmountForm()

  lazy val otherDividendsAmountView: OtherDividendsAmountView = app.injector.instanceOf[OtherDividendsAmountView]

  val h1Selector = "h1"
  val captionSelector = ".govuk-caption-l"
  val inputSelector = "#amount"
  val continueButtonSelector = "#continue"

  val errorSummarySelector = ".govuk-error-summary"
  val errorSummaryTitle = ".govuk-error-summary__title"
  val errorSummaryText = ".govuk-error-summary__body"

  val expectedH1 = "What is the amount of dividends from authorised unit trusts, open-ended investment companies or investment trusts?"
  val expectedCaption = "Dividends"

  val expectedErrorTitle = "There is a problem"
  val expectedErrorText = "Enter the amount of dividends received from trusts or investment companies"

  "OtherDividendsAmountView" should {

    "correctly render with no errors as an individual" when {

      "there are no form errors" which {

        lazy val view = otherDividendsAmountView(otherDividendsAmountForm, testCall, testBackUrl)(user, implicitly, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

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

      "there are no form errors" which {

        lazy val view = otherDividendsAmountView(
          otherDividendsAmountForm.copy(errors = Seq(FormError("amount", "Enter the amount of dividends received from trusts or investment companies"))),
          testCall,
          testBackUrl
        )(user, implicitly, mockAppConfig)

        implicit lazy val document: Document = Jsoup.parse(view.body)

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
          otherDividendsAmountForm,
          testCall,
          testBackUrl
        )(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)

        implicit lazy val document: Document = Jsoup.parse(view.body)

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
          otherDividendsAmountForm.copy(
            errors = Seq(FormError("amount", "Enter the amount of dividends received from trusts or investment companies"))),
            testCall,
            testBackUrl
        )(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)

        implicit lazy val document: Document = Jsoup.parse(view.body)

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

