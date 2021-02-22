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

import forms.YesNoForm

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import utils.ViewTest
import views.html.dividends.ReceiveUkDividendsView

class ReceiveUkDividendsViewSpec extends ViewTest {

  lazy val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm("Select yes if dividends were received from the UK")

  lazy val receiveUkDividendsView: ReceiveUkDividendsView = app.injector.instanceOf[ReceiveUkDividendsView]

  val h1Selector = "h1"
  val captionSelector = ".govuk-caption-l"
  val yesOptionSelector = "#value"
  val noOptionSelector = "#value-no"
  val continueButtonSelector = "#continue"

  val errorSummarySelector = ".govuk-error-summary"
  val errorSummaryTitle = ".govuk-error-summary__title"
  val errorSummaryText = ".govuk-error-summary__body"

  val taxYear = 2020
  val expectedIndividualH1 = "Did you receive any dividends from companies in the UK?"
  val expectedIndividualTitle = s"$expectedIndividualH1 - $serviceName - $govUkExtension"
  val expectedAgentH1 = "Did your client receive any dividends from companies in the UK?"
  val expectedAgentTitle = s"$expectedAgentH1 - $serviceName - $govUkExtension"
  val expectedCaption = "Dividends for 06 April 2019 to 05 April 2020"

  "ReceivedDividendsView" should {

    "correctly render with no errors as an individual" when {

      "there are no form errors" which {

        lazy val view = receiveUkDividendsView(
          yesNoForm, taxYear)(user, implicitly, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        "contains the correct title" in {
          document.title shouldBe expectedIndividualTitle
        }

        "contain the correct h1" in {
          elementText(h1Selector) shouldBe expectedIndividualH1
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

    "correctly render with errors as an individual" when {

      "there are no form errors" which {

        lazy val view = receiveUkDividendsView(
          yesNoForm.copy(
            errors = Seq(FormError("yes_no", "Select yes if dividends were received from the UK"))),
          taxYear
        )(user, implicitly, mockAppConfig)

        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedErrorTitle = "There is a problem"
        val expectedErrorText = "Select yes if dividends were received from the UK"

        "contains the correct title" in {
          document.title shouldBe expectedIndividualTitle
        }

        "contain the correct h1" in {
          elementText(h1Selector) shouldBe expectedIndividualH1
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

        lazy val view = receiveUkDividendsView(
          yesNoForm, taxYear)(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        "contains the correct title" in {
          document.title shouldBe expectedAgentTitle
        }

        "contain the correct h1" in {
          elementText(h1Selector) shouldBe expectedAgentH1
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

        lazy val view = receiveUkDividendsView(
          yesNoForm.copy(
            errors = Seq(FormError("yes_no", "Select yes if dividends were received from the UK"))),
          taxYear
        )(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)

        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedErrorTitle = "There is a problem"
        val expectedErrorText = "Select yes if dividends were received from the UK"

        "contains the correct title" in {
          document.title shouldBe expectedAgentTitle
        }

        "contain the correct h1" in {
          elementText(h1Selector) shouldBe expectedAgentH1
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
