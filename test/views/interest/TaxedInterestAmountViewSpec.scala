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

import forms.interest.TaxedInterestAmountForm
import models.interest.TaxedInterestModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.twirl.api.HtmlFormat
import utils.ViewTest
import views.html.interest.TaxedInterestAmountView

class TaxedInterestAmountViewSpec extends ViewTest{

  def agentOrIndividual(implicit isAgent: Boolean): String = if (isAgent) "agent" else "individual"

  def taxedInterestForm(implicit isAgent: Boolean): Form[TaxedInterestModel] = TaxedInterestAmountForm.taxedInterestAmountForm(
    emptyAmountKey = "interest.taxed-uk-interest-amount.error.empty." + agentOrIndividual,
    invalidNumericKey = "interest.taxed-uk-interest-amount.error.invalid-numeric",
    maxAmountInvalidKey = "interest.taxed-uk-interest-amount.error.max-amount"
  )
  lazy val taxedInterestView: TaxedInterestAmountView = app.injector.instanceOf[TaxedInterestAmountView]

  val captionSelector = ".govuk-caption-l"
  val continueButtonSelector = "#continue"
  val continueButtonFormSelector = "#main-content > div > div > form"
  val whatWouldYouCallSelector = "#main-content > div > div > form > div:nth-child(2) > label > div"
  val eachAccountNameSelector = "#main-content > div > div > form > div > label > p"
  val accountNameInputSelector = "input#taxedAccountName"
  val amountInterestSelector = "#main-content > div > div > form > div:nth-child(3) > label > div"
  val poundPrefixSelector = ".govuk-input__prefix"
  val interestEarnedInputSelector = "input#taxedAmount"
  val accountNameHintTextSelector = "#taxedAccountName-hint"
  val amountHintTextSelector = "#taxedAmount-hint"

  val taxYear = 2020
  val taxYearMinusOne: Int = taxYear -1
  val id = "id"

  val titleText = "Add an account with taxed UK interest"
  val errorTitleText = s"Error: $titleText"
  val h1Text = "Add an account with taxed UK interest"
  val captionText = s"Interest for 6 April $taxYearMinusOne to 5 April $taxYear"
  val accountNameHintText = "For example, ‘HSBC savings account’."
  val amountHintText = "For example, £600 or £193.54"
  val whatWouldYouCallText = "What do you want to name this account?"
  val eachAccountNameText = "Give each account a different name."
  val amountInterestText = "Amount of taxed UK interest"
  val poundPrefixText = "£"
  val continueButtonText = "Continue"
  val taxedAccountNameInput = "taxedAccountName"
  val taxedAmountInput = "taxedAmount"

  def newView(form: Form[TaxedInterestModel]): HtmlFormat.Appendable = taxedInterestView(
    form,
    taxYear,
    controllers.interest.routes.TaxedInterestAmountController.submit(taxYear,id),
    isAgent = true
  )(user, messages, mockAppConfig)

  "Taxed interest amount view in English" should {

    "Correctly render" when {

      "there are no form errors" which {
        implicit lazy val document: Document = Jsoup.parse(newView(taxedInterestForm(user.isAgent)).body)

        titleCheck(titleText)
        welshToggleCheck("English")
        textOnPageCheck(captionText, captionSelector)
        h1Check(h1Text + " " + captionText)
        textOnPageCheck(whatWouldYouCallText, whatWouldYouCallSelector)
        textOnPageCheck(eachAccountNameText, eachAccountNameSelector)
        inputFieldCheck(taxedAccountNameInput, accountNameInputSelector)
        textOnPageCheck(amountInterestText, amountInterestSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        textOnPageCheck(accountNameHintText, accountNameHintTextSelector)
        textOnPageCheck(amountHintText, amountHintTextSelector)
        inputFieldCheck(taxedAmountInput, interestEarnedInputSelector)
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(controllers.interest.routes.TaxedInterestAmountController.submit(taxYear,id).url, continueButtonFormSelector)
      }

      "there are form errors " which {

        "when passed a form without an empty taxedAmount value as an individual" which {

          lazy val errorForm = taxedInterestForm(user.isAgent).bind(Map("taxedAmount" -> "", "taxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newView(errorForm).body)
          val expectedErrorText = "Enter the amount of taxed UK interest you got"
          val errorSummaryHref = "#taxedAmount"

          titleCheck(errorTitleText)
          welshToggleCheck("English")
          h1Check(h1Text + " " + captionText)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(controllers.interest.routes.TaxedInterestAmountController.submit(taxYear,id).url, continueButtonFormSelector)
        }

        "when passed a form without an empty taxedAccountName value" which {
          lazy val errorForm = taxedInterestForm(user.isAgent).bind(Map("taxedAmount" -> "100.00", "taxedAccountName" -> ""))
          implicit lazy val document: Document = Jsoup.parse(newView(errorForm).body)
          val expectedErrorText = "Enter a name for this account"
          val errorSummaryHref = "#taxedAccountName"

          titleCheck(errorTitleText)
          welshToggleCheck("English")
          h1Check(h1Text + " " + captionText)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(controllers.interest.routes.TaxedInterestAmountController.submit(taxYear,id).url, continueButtonFormSelector)
        }

        "when passed a form with a non monetary taxedAmount value" which {
          lazy val errorForm = taxedInterestForm(user.isAgent).bind(Map("taxedAmount" -> "abc", "taxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newView(errorForm).body)
          val expectedErrorText = "Enter the amount of taxed UK interest in the correct format"
          val errorSummaryHref = "#taxedAmount"

          titleCheck(errorTitleText)
          welshToggleCheck("English")
          h1Check(h1Text + " " + captionText)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(controllers.interest.routes.TaxedInterestAmountController.submit(taxYear,id).url, continueButtonFormSelector)
        }

        "when passed a form with a taxedAmount value over £100,000,000,000" which {
          lazy val errorForm = taxedInterestForm(user.isAgent).bind(Map("taxedAmount" -> "£200,000,000,000", "taxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newView(errorForm).body)
          val expectedErrorText = "The amount of taxed UK interest must be less than £100,000,000,000"
          val errorSummaryHref = "#taxedAmount"

          titleCheck(errorTitleText)
          welshToggleCheck("English")
          h1Check(h1Text + " " + captionText)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(controllers.interest.routes.TaxedInterestAmountController.submit(taxYear,id).url, continueButtonFormSelector)
        }

        "when passed a form with invalid currency taxedAmount value" which {

          lazy val errorForm = taxedInterestForm(user.isAgent).bind(Map("taxedAmount" -> "100.00.00.00", "taxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newView(errorForm).body)
          val expectedErrorText = "Enter the amount of taxed UK interest in the correct format"
          val errorSummaryHref = "#taxedAmount"

          titleCheck(errorTitleText)
          welshToggleCheck("English")
          h1Check(h1Text + " " + captionText)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(controllers.interest.routes.TaxedInterestAmountController.submit(taxYear,id).url, continueButtonFormSelector)
        }
      }
    }
  }

  def newViewWelsh(form: Form[TaxedInterestModel]): HtmlFormat.Appendable = taxedInterestView(
    form,
    taxYear,
    controllers.interest.routes.TaxedInterestAmountController.submit(taxYear,id),
    isAgent = true
  )(user, welshMessages, mockAppConfig)

  "Taxed interest amount view in Welsh" should {

    "Correctly render" when {

      "there are no form errors" which {
        implicit lazy val document: Document = Jsoup.parse(newViewWelsh(taxedInterestForm(user.isAgent)).body)

        titleCheck(titleText)
        welshToggleCheck("Welsh")
        textOnPageCheck(captionText, captionSelector)
        h1Check(h1Text + " " + captionText)
        textOnPageCheck(whatWouldYouCallText, whatWouldYouCallSelector)
        inputFieldCheck(taxedAccountNameInput, accountNameInputSelector)
        textOnPageCheck(amountInterestText, amountInterestSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldCheck(taxedAmountInput, interestEarnedInputSelector)
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(controllers.interest.routes.TaxedInterestAmountController.submit(taxYear,id).url, continueButtonFormSelector)
      }

      "there are form errors " which {

        "when passed a form without an empty taxedAmount value as an individual" which {

          lazy val errorForm = taxedInterestForm(user.isAgent).bind(Map("taxedAmount" -> "", "taxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newViewWelsh(errorForm).body)
          val expectedErrorText = "Enter the amount of taxed UK interest you got"
          val errorSummaryHref = "#taxedAmount"

          titleCheck(errorTitleText)
          welshToggleCheck("Welsh")
          h1Check(h1Text + " " + captionText)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(controllers.interest.routes.TaxedInterestAmountController.submit(taxYear,id).url, continueButtonFormSelector)
        }

        "when passed a form without an empty taxedAccountName value as an individual" which {
          lazy val errorForm = taxedInterestForm(user.isAgent).bind(Map("taxedAmount" -> "100.00", "taxedAccountName" -> ""))
          implicit lazy val document: Document = Jsoup.parse(newViewWelsh(errorForm).body)
          val expectedErrorText = "Enter a name for this account"
          val errorSummaryHref = "#taxedAccountName"

          titleCheck(errorTitleText)
          welshToggleCheck("Welsh")
          h1Check(h1Text + " " + captionText)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(controllers.interest.routes.TaxedInterestAmountController.submit(taxYear,id).url, continueButtonFormSelector)
        }

        "when passed a form with a non monetary taxedAmount value" which {
          lazy val errorForm = taxedInterestForm(user.isAgent).bind(Map("taxedAmount" -> "abc", "taxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newViewWelsh(errorForm).body)
          val expectedErrorText = "Enter the amount of taxed UK interest in the correct format"
          val errorSummaryHref = "#taxedAmount"

          titleCheck(errorTitleText)
          welshToggleCheck("Welsh")
          h1Check(h1Text + " " + captionText)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(controllers.interest.routes.TaxedInterestAmountController.submit(taxYear,id).url, continueButtonFormSelector)
        }

        "when passed a form with a taxedAmount value over £100,000,000,000 as an individual" which {
          lazy val errorForm = taxedInterestForm(user.isAgent).bind(Map("taxedAmount" -> "£200,000,000,000", "taxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newViewWelsh(errorForm).body)
          val expectedErrorText = "The amount of taxed UK interest must be less than £100,000,000,000"
          val errorSummaryHref = "#taxedAmount"

          titleCheck(errorTitleText)
          welshToggleCheck("Welsh")
          h1Check(h1Text + " " + captionText)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(controllers.interest.routes.TaxedInterestAmountController.submit(taxYear,id).url, continueButtonFormSelector)
        }

        "when passed a form with invalid currency taxedAmount value" which {

          lazy val errorForm = taxedInterestForm(user.isAgent).bind(Map("taxedAmount" -> "100.00.00.00", "taxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newViewWelsh(errorForm).body)
          val expectedErrorText = "Enter the amount of taxed UK interest in the correct format"
          val errorSummaryHref = "#taxedAmount"

          titleCheck(errorTitleText)
          welshToggleCheck("Welsh")
          h1Check(h1Text + " " + captionText)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(controllers.interest.routes.TaxedInterestAmountController.submit(taxYear,id).url, continueButtonFormSelector)
        }
      }
    }
  }
}
