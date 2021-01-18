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

import forms.UntaxedInterestAmountForm
import forms.UntaxedInterestAmountForm._
import models.UntaxedInterestModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import utils.ViewTest
import views.html.interest.UntaxedInterestAmountView



class UntaxedInterestAmountViewSpec extends ViewTest {

  lazy val untaxedInterestForm: Form[UntaxedInterestModel] = UntaxedInterestAmountForm.untaxedInterestAmountForm()
  lazy val untaxedInterestView: UntaxedInterestAmountView = app.injector.instanceOf[UntaxedInterestAmountView]

  val h1Selector = "h1"
  val captionSelector = ".govuk-caption-l"
  val continueButtonSelector = "#continue"
  val errorSummarySelector = ".govuk-error-summary"
  val errorSummaryTitle = ".govuk-error-summary__title"
  val errorSummaryText = ".govuk-error-summary__body"

  val taxYear = 2020

  "UntaxedInterestAmountView" should {

    "Correctly render" when {
      "There are no form errors" which {

        lazy val view = untaxedInterestView(
          untaxedInterestForm,
          Some(mockAppConfig.signInUrl),
          taxYear,
          controllers.interest.routes.UntaxedInterestAmountController.submit(taxYear)
        )(user,implicitly,mockAppConfig)

        implicit lazy val document: Document = Jsoup.parse(view.body)
        val expectedTitle = "UK Interest - Register your income tax return with HMRC - Gov.UK"
        val expectedCaption = "Interest for 06 April 2019 to 05 April 2020"
        val expectedH1 = "UK untaxed interest account details"

        "contains the correct title" in {
          document.title shouldBe expectedTitle
        }

        "contains the correct caption" in {
          elementText(captionSelector) shouldBe expectedCaption
        }

        "contains the correct h1" in {
          elementText(h1Selector) shouldBe expectedH1
        }
      }

      "There are form errors" which {

        lazy val view = untaxedInterestView(
          untaxedInterestForm.copy(errors = Seq(FormError(untaxedAmount,
          "interest.untaxed-uk-interest-amount.error.empty"))),
          Some(mockAppConfig.signInUrl),
          taxYear,
          controllers.interest.routes.UntaxedInterestAmountController.submit(taxYear)
        )(user, implicitly, mockAppConfig)

        implicit lazy val document: Document = Jsoup.parse(view.body)
        val expectedTitle = "UK Interest - Register your income tax return with HMRC - Gov.UK"
        val expectedCaption = "Interest for 06 April 2019 to 05 April 2020"
        val expectedErrorTitle = "There is a problem"
        val expectedErrorText = "Enter the amount of untaxed interest earned"

        "contains the document title" in {
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
