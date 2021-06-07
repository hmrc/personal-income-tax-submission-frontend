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

import forms.interest.UntaxedInterestAmountForm
import models.interest.UntaxedInterestModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import play.twirl.api.HtmlFormat
import utils.ViewTest
import views.html.interest.UntaxedInterestAmountView

class UntaxedInterestAmountViewSpec extends ViewTest{

  def agentOrIndividual(implicit isAgent: Boolean): String = if (isAgent) "agent" else "individual"

  def untaxedInterestForm(implicit isAgent: Boolean): Form[UntaxedInterestModel] = UntaxedInterestAmountForm.untaxedInterestAmountForm(
    emptyAmountKey = "interest.untaxed-uk-interest-amount.error.empty." + agentOrIndividual,
    invalidNumericKey = "interest.untaxed-uk-interest-amount.error.invalid-numeric",
    maxAmountInvalidKey = "interest.untaxed-uk-interest-amount.error.max-amount"
  )
  lazy val untaxedInterestView: UntaxedInterestAmountView = app.injector.instanceOf[UntaxedInterestAmountView]

  val captionSelector = ".govuk-caption-l"
  val continueButtonSelector = "#continue"
  val continueButtonFormSelector = "#main-content > div > div > form"
  val whatWouldYouCallSelector = "#main-content > div > div > form > div:nth-child(2) > label > div"
  val eachAccountNameSelector = "#main-content > div > div > form > div > label > p"
  val accountNameInputSelector = "input#untaxedAccountName"
  val amountInterestSelector = "#main-content > div > div > form > div:nth-child(3) > label > div"
  val poundPrefixSelector = ".govuk-input__prefix"
  val interestEarnedInputSelector = "input#untaxedAmount"
  val accountNameHintTextSelector = "#untaxedAccountName-hint"
  val amountHintTextSelector = "#untaxedAmount-hint"

  val taxYear = 2020
  val taxYearMinusOne: Int = taxYear -1
  val id = "id"

  val titleText = "Add an account with untaxed UK interest"
  val errorTitleText = s"Error: $titleText"
  val h1Text = "Add an account with untaxed UK interest"
  val captionText = s"Interest for 6 April $taxYearMinusOne to 5 April $taxYear"
  val accountNameHintText = "For example, ‘HSBC savings account’."
  val amountHintText = "For example, £600 or £193.54"
  val whatWouldYouCallText = "What do you want to name this account?"
  val eachAccountNameText = "Give each account a different name."
  val amountInterestText = "Amount of untaxed UK interest"
  val poundPrefixText = "£"
  val continueButtonText = "Continue"
  val untaxedAccountNameInput = "untaxedAccountName"
  val untaxedAmountInput = "untaxedAmount"

  def newView(form: Form[UntaxedInterestModel]): HtmlFormat.Appendable = untaxedInterestView(
    form,
    taxYear,
    controllers.interest.routes.UntaxedInterestAmountController.submit(taxYear,id),
    isAgent = false
  )(user, messages, mockAppConfig)

  "Untaxed interest amount view in English " should {

    "Correctly render" when {

      "there are no form errors" which {
        implicit lazy val document: Document = Jsoup.parse(newView(untaxedInterestForm(user.isAgent)).body)

        titleCheck(titleText)
        welshToggleCheck("English")
        textOnPageCheck(captionText, captionSelector)
        h1Check(h1Text + " " + captionText)
        textOnPageCheck(whatWouldYouCallText, whatWouldYouCallSelector)
        textOnPageCheck(eachAccountNameText, eachAccountNameSelector)
        inputFieldCheck(untaxedAccountNameInput, accountNameInputSelector)
        textOnPageCheck(amountInterestText, amountInterestSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        textOnPageCheck(accountNameHintText, accountNameHintTextSelector)
        textOnPageCheck(amountHintText, amountHintTextSelector)
        inputFieldCheck(untaxedAmountInput, interestEarnedInputSelector)
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(controllers.interest.routes.UntaxedInterestAmountController.submit(taxYear,id).url, continueButtonFormSelector)
      }

      "there are form errors " which {

        "when passed a form without an empty untaxedAmount value" which {

          lazy val errorForm = untaxedInterestForm(user.isAgent).bind(Map("untaxedAmount" -> "", "untaxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newView(errorForm).body)
          val expectedErrorText = "Enter the amount of untaxed UK interest you got"
          val errorSummaryHref = "#untaxedAmount"

          titleCheck(errorTitleText)
          welshToggleCheck("English")
          h1Check(h1Text + " " + captionText)
          println(errorSummaryCheck(expectedErrorText, errorSummaryHref))
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(controllers.interest.routes.UntaxedInterestAmountController.submit(taxYear,id).url, continueButtonFormSelector)
        }

        "when passed a form without an empty untaxedAccountName value" which {
          lazy val errorForm = untaxedInterestForm(user.isAgent).bind(Map("untaxedAmount" -> "100.00", "untaxedAccountName" -> ""))
          implicit lazy val document: Document = Jsoup.parse(newView(errorForm).body)
          val expectedErrorText = "Enter a name for this account"
          val errorSummaryHref = "#untaxedAccountName"

          titleCheck(errorTitleText)
          welshToggleCheck("English")
          h1Check(h1Text + " " + captionText)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(controllers.interest.routes.UntaxedInterestAmountController.submit(taxYear,id).url, continueButtonFormSelector)
        }

        "when passed a form with a non monetary untaxedAmount value" which {
          lazy val errorForm = untaxedInterestForm(user.isAgent).bind(Map("untaxedAmount" -> "abc", "untaxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newView(errorForm).body)
          val expectedErrorText = "Enter the amount of untaxed UK interest in the correct format"
          val errorSummaryHref = "#untaxedAmount"

          titleCheck(errorTitleText)
          welshToggleCheck("English")
          h1Check(h1Text + " " + captionText)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(controllers.interest.routes.UntaxedInterestAmountController.submit(taxYear,id).url, continueButtonFormSelector)
        }

        "when passed a form with a untaxedAmount value over £100,000,000,000" which {
          lazy val errorForm = untaxedInterestForm(user.isAgent).bind(Map("untaxedAmount" -> "£200,000,000,000", "untaxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newView(errorForm).body)
          val expectedErrorText = "The amount of untaxed UK interest must be less than £100,000,000,000"
          val errorSummaryHref = "#untaxedAmount"

          titleCheck(errorTitleText)
          welshToggleCheck("English")
          h1Check(h1Text + " " + captionText)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(controllers.interest.routes.UntaxedInterestAmountController.submit(taxYear,id).url, continueButtonFormSelector)
        }

        "when passed a form with invalid currency untaxedAmount value" which {

          lazy val errorForm = untaxedInterestForm(user.isAgent).bind(Map("untaxedAmount" -> "100.00.00.00", "untaxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newView(errorForm).body)
          val expectedErrorText = "Enter the amount of untaxed UK interest in the correct format"
          val errorSummaryHref = "#untaxedAmount"

          titleCheck(errorTitleText)
          welshToggleCheck("English")
          h1Check(h1Text + " " + captionText)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(controllers.interest.routes.UntaxedInterestAmountController.submit(taxYear,id).url, continueButtonFormSelector)
        }
      }
    }
  }

  def newViewWelsh(form: Form[UntaxedInterestModel]): HtmlFormat.Appendable = untaxedInterestView(
    form,
    taxYear,
    controllers.interest.routes.UntaxedInterestAmountController.submit(taxYear,id),
    isAgent = false
  )(user, welshMessages, mockAppConfig)

  "Untaxed interest amount view in Welsh " should {

    "Correctly render" when {

      "there are no form errors" which {
        implicit lazy val document: Document = Jsoup.parse(newViewWelsh(untaxedInterestForm(user.isAgent)).body)

        titleCheck(titleText)
        welshToggleCheck("Welsh")
        textOnPageCheck(captionText, captionSelector)
        h1Check(h1Text + " " + captionText)
        textOnPageCheck(whatWouldYouCallText, whatWouldYouCallSelector)
        inputFieldCheck(untaxedAccountNameInput, accountNameInputSelector)
        textOnPageCheck(amountInterestText, amountInterestSelector)
        textOnPageCheck(poundPrefixText, poundPrefixSelector)
        inputFieldCheck(untaxedAmountInput, interestEarnedInputSelector)
        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(controllers.interest.routes.UntaxedInterestAmountController.submit(taxYear,id).url, continueButtonFormSelector)
      }

      "there are form errors " which {

        "when passed a form without an empty untaxedAmount value" which {

          lazy val errorForm = untaxedInterestForm(user.isAgent).bind(Map("untaxedAmount" -> "", "untaxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newViewWelsh(errorForm).body)
          val expectedErrorText = "Enter the amount of untaxed UK interest you got"
          val errorSummaryHref = "#untaxedAmount"

          titleCheck(errorTitleText)
          welshToggleCheck("Welsh")
          h1Check(h1Text + " " + captionText)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(controllers.interest.routes.UntaxedInterestAmountController.submit(taxYear,id).url, continueButtonFormSelector)
        }

        "when passed a form without an empty untaxedAccountName value" which {
          lazy val errorForm = untaxedInterestForm(user.isAgent).bind(Map("untaxedAmount" -> "100.00", "untaxedAccountName" -> ""))
          implicit lazy val document: Document = Jsoup.parse(newViewWelsh(errorForm).body)
          val expectedErrorText = "Enter a name for this account"
          val errorSummaryHref = "#untaxedAccountName"

          titleCheck(errorTitleText)
          welshToggleCheck("Welsh")
          h1Check(h1Text + " " + captionText)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(controllers.interest.routes.UntaxedInterestAmountController.submit(taxYear,id).url, continueButtonFormSelector)
        }

        "when passed a form with a non monetary untaxedAmount value" which {
          lazy val errorForm = untaxedInterestForm(user.isAgent).bind(Map("untaxedAmount" -> "abc", "untaxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newViewWelsh(errorForm).body)
          val expectedErrorText = "Enter the amount of untaxed UK interest in the correct format"
          val errorSummaryHref = "#untaxedAmount"

          titleCheck(errorTitleText)
          welshToggleCheck("Welsh")
          h1Check(h1Text + " " + captionText)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(controllers.interest.routes.UntaxedInterestAmountController.submit(taxYear,id).url, continueButtonFormSelector)
        }

        "when passed a form with a untaxedAmount value over £100,000,000,000" which {
          lazy val errorForm = untaxedInterestForm(user.isAgent).bind(Map("untaxedAmount" -> "£200,000,000,000", "untaxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newViewWelsh(errorForm).body)
          val expectedErrorText = "The amount of untaxed UK interest must be less than £100,000,000,000"
          val errorSummaryHref = "#untaxedAmount"

          titleCheck(errorTitleText)
          welshToggleCheck("Welsh")
          h1Check(h1Text + " " + captionText)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(controllers.interest.routes.UntaxedInterestAmountController.submit(taxYear,id).url, continueButtonFormSelector)
        }

        "when passed a form with invalid currency untaxedAmount value" which {

          lazy val errorForm = untaxedInterestForm(user.isAgent).bind(Map("untaxedAmount" -> "100.00.00.00", "untaxedAccountName" -> "Account Name"))
          implicit lazy val document: Document = Jsoup.parse(newViewWelsh(errorForm).body)
          val expectedErrorText = "Enter the amount of untaxed UK interest in the correct format"
          val errorSummaryHref = "#untaxedAmount"

          titleCheck(errorTitleText)
          welshToggleCheck("Welsh")
          h1Check(h1Text + " " + captionText)
          errorSummaryCheck(expectedErrorText, errorSummaryHref)
          textOnPageCheck(captionText, captionSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(controllers.interest.routes.UntaxedInterestAmountController.submit(taxYear,id).url, continueButtonFormSelector)
        }
      }
    }
  }

}
