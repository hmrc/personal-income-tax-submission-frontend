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
import models.UntaxedInterestModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.twirl.api.HtmlFormat
import utils.ViewTest
import views.html.interest.UntaxedInterestAmountView

class UntaxedInterestAmountViewSpec extends ViewTest{

  lazy val untaxedInterestForm: Form[UntaxedInterestModel] = UntaxedInterestAmountForm.untaxedInterestAmountForm()
  lazy val untaxedInterestView: UntaxedInterestAmountView = app.injector.instanceOf[UntaxedInterestAmountView]

  val captionSelector = ".govuk-caption-l"
  val continueButtonSelector = "#continue"
  val whatWouldYouCallSelector = "#main-content > div > div > form > div:nth-child(2) > label"
  val accountNameInputSelector = "input#untaxedAccountName"
  val amountInterestSelector = "#main-content > div > div > form > div:nth-child(3) > label"
  val poundPrefixSelector = ".govuk-input__prefix"
  val interestEarnedInputSelector = "input#untaxedAmount"

  val taxYear = 2020
  val taxYearMinusOne: Int = taxYear -1
  val id = "id"

  val titleText = "UK untaxed interest account details"
  val errorTitleText = s"Error: $titleText"
  val h1Text = "UK untaxed interest account details"
  val captionText = s"Interest for 06 April $taxYearMinusOne to 05 April $taxYear"
  val whatWouldYouCallText = "What would you like to call this account?"
  val amountInterestText = "Amount of interest earned"
  val poundPrefixText = "£"
  val continueButtonText = "Continue"

  def newView(form: Form[UntaxedInterestModel]): HtmlFormat.Appendable = untaxedInterestView(
    form,
    taxYear,
    controllers.interest.routes.UntaxedInterestAmountController.submit(taxYear,id)
  )(user, implicitly, mockAppConfig)

  "Untaxed interest amount view " should {

    "Correctly render" when {

      "there are no form errors" which {
        implicit lazy val document: Document = Jsoup.parse(newView(untaxedInterestForm).body)

        titleCheck(titleText)
        textOnPageCheck(captionText, captionSelector)
        h1Check(h1Text)
        textOnPageCheck(whatWouldYouCallText, whatWouldYouCallSelector)
        inputFieldCheck("untaxedAccountName", accountNameInputSelector)
        textOnPageCheck(amountInterestText, amountInterestSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldCheck("untaxedAmount", interestEarnedInputSelector)
        buttonCheck(continueButtonText, continueButtonSelector)
      }

      "there are form errors " which {

        "when passed a form without an empty untaxedAmount value" which {

          lazy val errorForm = untaxedInterestForm.bind(Map("untaxedAmount" -> "", "untaxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newView(errorForm).body)
          val expectedErrorText = "Enter the amount of untaxed interest earned"
          val errorSummaryHref = "#untaxedAmount"

          titleCheck(errorTitleText)
          h1Check(h1Text)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
        }

        "when passed a form without an empty untaxedAccountName value" which {
          lazy val errorForm = untaxedInterestForm.bind(Map("untaxedAmount" -> "100.00", "untaxedAccountName" -> ""))
          implicit lazy val document: Document = Jsoup.parse(newView(errorForm).body)
          val expectedErrorText = "Enter an account name"
          val errorSummaryHref = "#untaxedAccountName"

          titleCheck(errorTitleText)
          h1Check(h1Text)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
        }

        "when passed a form with a non monetary untaxedAmount value" which {
          lazy val errorForm = untaxedInterestForm.bind(Map("untaxedAmount" -> "abc", "untaxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newView(errorForm).body)
          val expectedErrorText = "Enter an amount using numbers 0 to 9"
          val errorSummaryHref = "#untaxedAmount"

          titleCheck(errorTitleText)
          h1Check(h1Text)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
        }

        "when passed a form with a untaxedAmount value over £100,000,000,000" which {
          lazy val errorForm = untaxedInterestForm.bind(Map("untaxedAmount" -> "£200,000,000,000", "untaxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newView(errorForm).body)
          val expectedErrorText = "Enter an amount less than £100,000,000,000"
          val errorSummaryHref = "#untaxedAmount"

          titleCheck(errorTitleText)
          h1Check(h1Text)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
        }

        "when passed a form with invalid currency untaxedAmount value" which {

          lazy val errorForm = untaxedInterestForm.bind(Map("untaxedAmount" -> "100.00.00.00", "untaxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newView(errorForm).body)
          val expectedErrorText = "Enter the amount in the correct format"
          val errorSummaryHref = "#untaxedAmount"

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
