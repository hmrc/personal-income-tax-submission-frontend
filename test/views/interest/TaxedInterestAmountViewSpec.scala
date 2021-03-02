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
import play.api.data.Form
import play.twirl.api.HtmlFormat
import utils.ViewTest
import views.html.interest.TaxedInterestAmountView

class TaxedInterestAmountViewSpec extends ViewTest{

  lazy val taxedInterestForm: Form[TaxedInterestModel] = TaxedInterestAmountForm.taxedInterestAmountForm()
  lazy val taxedInterestView: TaxedInterestAmountView = app.injector.instanceOf[TaxedInterestAmountView]

  val captionSelector = ".govuk-caption-l"
  val continueButtonSelector = "#continue"
  val whatWouldYouCallSelector = "#main-content > div > div > form > div:nth-child(2) > label"
  val accountNameInputSelector = "input#taxedAccountName"
  val amountInterestSelector = "#main-content > div > div > form > div:nth-child(3) > label"
  val poundPrefixSelector = ".govuk-input__prefix"
  val interestEarnedInputSelector = "input#taxedAmount"

  val taxYear = 2020
  val taxYearMinusOne: Int = taxYear -1
  val id = "id"

  val titleText = "UK taxed interest account details"
  val errorTitleText = s"$titleText"
  val h1Text = "UK taxed interest account details"
  val captionText = s"Interest for 06 April $taxYearMinusOne to 05 April $taxYear"
  val whatWouldYouCallText = "What would you like to call this account?"
  val amountInterestText = "Amount of interest earned"
  val poundPrefixText = "£"
  val continueButtonText = "Continue"

  def newView(form: Form[TaxedInterestModel]): HtmlFormat.Appendable = taxedInterestView(
    form,
    taxYear,
    controllers.interest.routes.TaxedInterestAmountController.submit(taxYear,id)
  )(user, implicitly, mockAppConfig)

  "Taxed interest amount view " should {

    "Correctly render" when {

      "there are no form errors" which {
        implicit lazy val document: Document = Jsoup.parse(newView(taxedInterestForm).body)

        titleCheck(titleText)
        textOnPageCheck(captionText, captionSelector)
        h1Check(h1Text)
        textOnPageCheck(whatWouldYouCallText, whatWouldYouCallSelector)
        inputFieldCheck("taxedAccountName", accountNameInputSelector)
        textOnPageCheck(amountInterestText, amountInterestSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldCheck("taxedAmount", interestEarnedInputSelector)
        buttonCheck(continueButtonText, continueButtonSelector)
      }

      "there are form errors " which {

        "when passed a form without an empty taxedAmount value" which {

          lazy val errorForm = taxedInterestForm.bind(Map("taxedAmount" -> "", "taxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newView(errorForm).body)
          val expectedErrorText = "Enter the amount of taxed interest earned"
          val errorSummaryHref = "#taxedAmount"

          titleCheck(errorTitleText)
          h1Check(h1Text)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
        }

        "when passed a form without an empty taxedAccountName value" which {
          lazy val errorForm = taxedInterestForm.bind(Map("taxedAmount" -> "100.00", "taxedAccountName" -> ""))
          implicit lazy val document: Document = Jsoup.parse(newView(errorForm).body)
          val expectedErrorText = "Enter an account name"
          val errorSummaryHref = "#taxedAccountName"

          titleCheck(errorTitleText)
          h1Check(h1Text)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
        }

        "when passed a form with a non monetary taxedAmount value" which {
          lazy val errorForm = taxedInterestForm.bind(Map("taxedAmount" -> "abc", "taxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newView(errorForm).body)
          val expectedErrorText = "Enter an amount using numbers 0 to 9"
          val errorSummaryHref = "#taxedAmount"

          titleCheck(errorTitleText)
          h1Check(h1Text)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
        }

        "when passed a form with a taxedAmount value over £100,000,000,000" which {
          lazy val errorForm = taxedInterestForm.bind(Map("taxedAmount" -> "£200,000,000,000", "taxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newView(errorForm).body)
          val expectedErrorText = "Enter an amount less than £100,000,000,000"
          val errorSummaryHref = "#taxedAmount"

          titleCheck(errorTitleText)
          h1Check(h1Text)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
        }

        "when passed a form with invalid currency taxedAmount value" which {

          lazy val errorForm = taxedInterestForm.bind(Map("taxedAmount" -> "100.00.00.00", "taxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newView(errorForm).body)
          val expectedErrorText = "Enter an amount in pounds and pence"
          val errorSummaryHref = "#taxedAmount"

          titleCheck(errorTitleText)
          h1Check(h1Text)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
        }
      }
    }
  }
}
