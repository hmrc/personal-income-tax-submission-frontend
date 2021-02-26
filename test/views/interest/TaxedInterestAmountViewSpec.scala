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
import forms.TaxedInterestAmountForm._
import models.TaxedInterestModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import play.twirl.api.HtmlFormat
import utils.ViewTest
import views.html.interest.TaxedInterestAmountView

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
  val id = "id"

  val expectedCaption = "Interest for 06 April 2019 to 05 April 2020"
  val expectedH1 = "UK taxed interest account details"
  val expectedTitle = s"$expectedH1 - $serviceName - $govUkExtension"
  val expectedErrorTitle = "There is a problem"

  def newView(form: Form[TaxedInterestModel]): HtmlFormat.Appendable = taxedInterestView(
    taxedInterestForm,
    taxYear,
    controllers.interest.routes.TaxedInterestAmountController.submit(taxYear,id)
  )(user, implicitly, mockAppConfig)

  "Taxed interest amount view " should {

    "Correctly render" when {

        implicit lazy val document: Document = Jsoup.parse(newView(taxedInterestForm).body)

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

        "when passed a form without a taxedAmount value" which {

          lazy val emptyTaxedAccountForm = taxedInterestForm.bind(Map("taxedAmount" -> "", "taxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newView(emptyTaxedAccountForm).body)
          val expectedErrorText = "Enter the amount of taxed interest earned"

          titleCheck("Error: " + expectedTitle)
          h1Check(expectedH1)
          errorSummaryCheck(expectedErrorText, "#value")
          textOnPageCheck(expectedCaption, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck("Continue", continueButtonSelector)
        }

        "when passed a form without a taxedAccountName value" which {

          lazy val emptyTaxedAccountNameForm = taxedInterestForm.bind(Map("taxedAmount" -> "100.00", "taxedAccountName" -> ""))
          implicit lazy val document: Document = Jsoup.parse(newView(emptyTaxedAccountNameForm).body)
          val expectedErrorText = "Enter an account name"

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

        "when passed a form with a non monetary taxedAmount value" which {

          lazy val emptyTaxedAccountForm = taxedInterestForm.bind(Map("taxedAmount" -> "abc", "taxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newView(emptyTaxedAccountForm).body)
          val expectedErrorText = "Enter an amount using numbers 0 to 9"

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

        "when passed a form with a taxedAmount value over £100,000,000,000" which {

          lazy val emptyTaxedAccountForm = taxedInterestForm.bind(Map("taxedAmount" -> "£200,000,000,000", "taxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newView(emptyTaxedAccountForm).body)
          val expectedErrorText = "Enter an amount less than £100,000,000,000"

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

        "when passed a form with invalid currency taxedAmount value" which {

          lazy val emptyTaxedAccountForm = taxedInterestForm.bind(Map("taxedAmount" -> "100.00.00.00", "taxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newView(emptyTaxedAccountForm).body)
          val expectedErrorText = "Enter an amount in pounds and pence"

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
