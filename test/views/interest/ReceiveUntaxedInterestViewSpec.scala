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
import models.formatHelpers.YesNoModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import utils.ViewTest
import views.html.interest.ReceiveUntaxedInterestView

class ReceiveUntaxedInterestViewSpec extends ViewTest {


  lazy val yesNoForm: Form[YesNoModel] = YesNoForm.yesNoForm("Select yes if untaxed interest " +
    "was received from companies in the uk")

  lazy val receivedUntaxedInterestView: ReceiveUntaxedInterestView = app.injector.instanceOf[ReceiveUntaxedInterestView]

  val h1Selector = "h1"
  val captionSelector = ".govuk-caption-l"
  val yesOptionSelector = "#yes_no_yes"
  val noOptionSelector = "#yes_no_no"
  val continueButtonSelector = "#continue"

  val errorSummarySelector = ".govuk-error-summary"
  val errorSummaryTitle = ".govuk-error-summary__title"
  val errorSummaryText = ".govuk-error-summary__body"

  val taxYear = 2020

  "ReceivedUntaxedInterestView" should {

    "correctly render with no errors as an individual" when {

      "there are no form errors" which {


        lazy val view = receivedUntaxedInterestView("Did you receive any untaxed interest from the UK?",
          yesNoForm, taxYear)(user,implicitly,mockAppConfig)

        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedTitle = "Did you receive any untaxed interest from the UK?"
        val expectedH1 = "Did you receive any untaxed interest from the UK?"
        val expectedCaption = "Interest"

        "contains the correct title" in {
          document.title shouldBe expectedTitle
        }

        "contains the correct h1" in {
          elementText(h1Selector) shouldBe expectedH1
        }

        "contains the correct caption" in {
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

    "correctly render with errors as an individual"  when {

      "there are no form errors" which {

        lazy val view = receivedUntaxedInterestView("Did you receive any untaxed interest from the UK?",
          yesNoForm.copy(
          errors = Seq(FormError("yes_no", "Select yes if untaxed interest was received from companies in the UK"))),
          2020)(user,implicitly,mockAppConfig)

        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedTitle = "Did you receive any untaxed interest from the UK?"
        val expectedH1 = "Did you receive any untaxed interest from the UK?"
        val expectedCaption = "Interest"

        val expectedErrorTitle = "There is a problem"
        val expectedErrorText = "Select yes if untaxed interest was received from companies in the UK"

        "contains the correct title" in {
          document.title shouldBe expectedTitle
        }

        "contains the correct h1" in {
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

        "contains an error title" in {
          elementText(errorSummaryTitle) shouldBe expectedErrorTitle
        }

        "contains an error message" in {
          elementText(errorSummaryText) shouldBe expectedErrorText
        }
      }
    }

    "correctly renders with no errors as an agent" when {

      "there are no form errors" which {

        lazy val view = receivedUntaxedInterestView("Did your client receive any untaxed interest from the UK?",
          yesNoForm,taxYear)(user.copy(arn = Some("XARN1234567")),implicitly,mockAppConfig)

        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedTitle = "Did your client receive any untaxed interest from the UK?"
        val expectedH1 = "Did your client receive any untaxed interest from the UK?"
        val expectedCaption = "Interest"

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

        lazy val view = receivedUntaxedInterestView(
          "Did your client receive any untaxed interest from the UK?",
          yesNoForm.copy(
            errors = Seq(FormError("yes_no", "Select yes if untaxed interest was received from companies in the UK"))),
          taxYear)(user.copy(arn = Some("XARN1234567")),implicitly,mockAppConfig)

        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedTitle = "Did your client receive any untaxed interest from the UK?"
        val expectedH1 = "Did your client receive any untaxed interest from the UK?"
        val expectedCaption = "Interest"

        val expectedErrorTitle = "There is a problem"
        val expectedErrorText = "Select yes if untaxed interest was received from companies in the UK"

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
