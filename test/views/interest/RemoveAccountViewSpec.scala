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

package views.interest

import forms.YesNoForm
import play.api.data.{Form, FormError}
import models.formatHelpers.YesNoModel
import models.interest.InterestAccountModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import utils.ViewTest
import views.html.interest.RemoveAccountView

class RemoveAccountViewSpec extends ViewTest {

  lazy val yesNoForm: Form[YesNoModel] = YesNoForm.yesNoForm("Select yes to remove this account")

  lazy val removeAccountView: RemoveAccountView = app.injector.instanceOf[RemoveAccountView]

  val taxYear = 2020

  val TAXED = "taxed"
  val UNTAXED = "untaxed"

  val account = InterestAccountModel(Some("qwerty"), "Monzo", 9001.00)

  val h1Selector = "h1"
  val captionSelector = ".govuk-caption-l"
  val yesOptionSelector = "#yes_no_yes"
  val noOptionSelector = "#yes_no_no"
  val continueButtonSelector = "#continue"

  val errorSummarySelector = ".govuk-error-summary"
  val errorSummaryTitle = ".govuk-error-summary__title"
  val errorSummaryText = ".govuk-error-summary__body"

  "Remove Account view" should {

    "Correctly render as an individual" when {
      "There are no form errors " which {
        lazy val view = removeAccountView(yesNoForm, taxYear, UNTAXED, account)(user, implicitly, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)
        val expectedTitle = "Are you sure you want to remove this account? - Register your income tax return with HMRC - Gov.UK"
        val expectedH1 = "Are you sure you want to remove Monzo?"
        val expectedCaption = "Interest for 06 April 2019 to 05 April 2020"

        "Contains the correct title" in {
          document.title shouldBe expectedTitle
        }
        "Contains the correct h1" in {
          elementText(h1Selector) shouldBe expectedH1
        }
        "Contains the correct caption" in {
          elementText(captionSelector) shouldBe expectedCaption
        }
        "contains a yes option" in {
          elementExist(yesOptionSelector) shouldBe true
        }

        "contains a no option" in {
          elementExist(noOptionSelector) shouldBe true
        }

        "contains a continue button" in {
          elementExist(continueButtonSelector) shouldBe true
        }
      }
    }
    "correctly render with errors as an individual" when {
      "there are no form errors" which {
        lazy val view = removeAccountView(
          yesNoForm.copy(
            errors = Seq(FormError("yes_no", "Select yes to remove this account"))),
          taxYear, UNTAXED, account
        )(user, implicitly, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedTitle = "Are you sure you want to remove this account? - Register your income tax return with HMRC - Gov.UK"
        val expectedH1 = "Are you sure you want to remove Monzo?"
        val expectedCaption = "Interest for 06 April 2019 to 05 April 2020"
        val expectedErrorTitle = "There is a problem"
        val expectedErrorText = "Select yes to remove this account"

        "contains the correct title" in {
          document.title shouldBe expectedTitle
        }

        "contain the correct h1" in {
          elementText(h1Selector) shouldBe expectedH1
        }

        "contains the correct header caption" in {
          elementText(captionSelector) shouldBe expectedCaption
        }

        "contains a yes option" in {
          elementExist(yesOptionSelector) shouldBe true
        }

        "contains a no option" in {
          elementExist(noOptionSelector) shouldBe true
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

        lazy val view = removeAccountView(
          yesNoForm, taxYear, UNTAXED, account)(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedTitle = "Are you sure you want to remove this account? - Register your income tax return with HMRC - Gov.UK"
        val expectedH1 = "Are you sure you want to remove Monzo?"
        val expectedCaption = "Interest for 06 April 2019 to 05 April 2020"

        "contains the correct title" in {
          document.title shouldBe expectedTitle
        }

        "contain the correct h1" in {
          elementText(h1Selector) shouldBe expectedH1
        }

        "contains the correct header caption" in {
          elementText(captionSelector) shouldBe expectedCaption
        }

        "contains a yes option" in {
          elementExist(yesOptionSelector) shouldBe true
        }

        "contains a no option" in {
          elementExist(noOptionSelector) shouldBe true
        }

        "contains a continue button" in {
          elementExist(continueButtonSelector) shouldBe true
        }
      }
    }
    "correctly render with errors as an agent" when {
      "there is a form error" which {
        lazy val view = removeAccountView(yesNoForm.copy(
            errors = Seq(FormError("yes_no", "Select yes to remove this account"))),
          taxYear, UNTAXED, account
        )(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)

        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedTitle = "Are you sure you want to remove this account? - Register your income tax return with HMRC - Gov.UK"
        val expectedH1 = "Are you sure you want to remove Monzo?"
        val expectedCaption = "Interest for 06 April 2019 to 05 April 2020"

        val expectedErrorTitle = "There is a problem"
        val expectedErrorText = "Select yes to remove this account"
        "contains the correct title" in {
          document.title shouldBe expectedTitle
        }

        "contain the correct h1" in {
          elementText(h1Selector) shouldBe expectedH1
        }

        "contains the correct header caption" in {
          elementText(captionSelector) shouldBe expectedCaption
        }

        "contains a yes option" in {
          elementExist(yesOptionSelector) shouldBe true
        }

        "contains a no option" in {
          elementExist(noOptionSelector) shouldBe true
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
