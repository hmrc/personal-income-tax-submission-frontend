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

import forms.TaxedInterestAmountForm
import models.TaxedInterestModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import utils.ViewTest
import views.html.interest.TaxedInterestAmountView
import forms.TaxedInterestAmountForm._

class TaxedInterestAmountViewSpec extends ViewTest{

  lazy val taxedInterestForm: Form[TaxedInterestModel] = TaxedInterestAmountForm.taxedInterestAmountForm()
  lazy val taxedInterestView: TaxedInterestAmountView = app.injector.instanceOf[TaxedInterestAmountView]

  val h1Selector = "h1"
  val captionSelector = ".govuk-caption-l"
  val continueButtonSelector = "#continue"

  val errorSummarySelector = ".govuk-error-summary"
  val errorSummaryTitle = ".govuk-error-summary__title"
  val errorSummaryText = ".govuk-error-summary__body"

  val taxYear = 2020

  "Taxed interest amount view " should {

    "Correctly render" when {
      "there are no form errors " which {
        lazy val view = taxedInterestView(
          taxedInterestForm,
          2020,
          controllers.interest.routes.TaxedInterestAmountController.submit(taxYear),
          Some(mockAppConfig.signInUrl)
        )(user, implicitly, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)
        val expectedTitle = "UK Interest - Register your income tax return with HMRC - Gov.UK"
        val expectedCaption = "Interest for 06 April 2019 to 05 April 2020"
        val expectedH1 = "UK taxed interest account details"

        "has the correct h1" in {
          elementText(h1Selector) shouldBe expectedH1
        }

        "Contains the correct title" in {
          document.title shouldBe expectedTitle
        }
        "Contains the correct caption" in {
          elementText(captionSelector) shouldBe expectedCaption
        }
        "contains a continue button" in {
          elementExist(continueButtonSelector) shouldBe true
        }
      }
      "there are form errors " which {
        lazy val view = taxedInterestView(
          taxedInterestForm.copy(errors = Seq(FormError(taxedAmount, "interest.taxed-uk-interest-amount.error.empty"))),
          taxYear,
          controllers.interest.routes.TaxedInterestAmountController.submit(taxYear),
          Some(mockAppConfig.signInUrl)
        )(user, implicitly, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)
        val expectedTitle = "UK Interest - Register your income tax return with HMRC - Gov.UK"
        val expectedCaption = "Interest for 06 April 2019 to 05 April 2020"
        val expectedErrorTitle = "There is a problem"
        val expectedErrorText = "Enter the amount of taxed interest earned"
        val expectedH1 = "UK taxed interest account details"

        "has the correct h1" in {
          elementText(h1Selector) shouldBe expectedH1
        }

        "contains the correct title" in {
          document.title shouldBe expectedTitle
        }
        "contains the correct header caption" in {
          elementText(captionSelector) shouldBe expectedCaption
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
