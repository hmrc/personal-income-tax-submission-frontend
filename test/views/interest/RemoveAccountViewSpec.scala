///*
// * Copyright 2021 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package views.interest
//
//import forms.YesNoForm
//
//import models.interest.InterestAccountModel
//import org.jsoup.Jsoup
//import org.jsoup.nodes.Document
//import play.api.data.Form
//import utils.ViewTest
//import views.html.interest.RemoveAccountView
//
//// TODO to be removed as part of moving view tests to IT tests
//
//class RemoveAccountViewSpec extends ViewTest {
//
//  lazy val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm("Select yes to remove this account")
//
//  lazy val removeAccountView: RemoveAccountView = app.injector.instanceOf[RemoveAccountView]
//
//  val taxYear = 2020
//  val taxYearMinusOne: Int = taxYear -1
//
//  val TAXED = "taxed"
//  val UNTAXED = "untaxed"
//
//  val account: InterestAccountModel = InterestAccountModel(Some("qwerty"), "Monzo", 9001.00)
//
//  val captionSelector = ".govuk-caption-l"
//  val thisWillTextSelector = "#value-hint > div > p"
//  val yesOptionSelector = "#main-content > div > div > form > div > fieldset > div.govuk-radios.govuk-radios--inline > div:nth-child(1) > label"
//  val noOptionSelector = "#main-content > div > div > form > div > fieldset > div.govuk-radios.govuk-radios--inline > div:nth-child(2) > label"
//  val continueButtonSelector = "#continue"
//  val continueButtonFormSelector = "#main-content > div > div > form"
//  val errorSummaryHref = "#value"
//
//  val errorSummarySelector = ".govuk-error-summary"
//  val errorSummaryTitleSelector = ".govuk-error-summary__title"
//  val errorSummaryTextSelector = ".govuk-error-summary__body"
//
//  val expectedTitle = "Are you sure you want to remove this account?"
//  val expectedErrorTitle = s"Error: $expectedTitle"
//  val expectedH1 = "Are you sure you want to remove Monzo?"
//  val expectedCaption = s"Interest for 6 April $taxYearMinusOne to 5 April $taxYear"
//  val thisWillTextUntaxed = "This will remove all untaxed UK interest."
//  val thisWillTextTaxed = "This will remove all taxed UK interest."
//  val yesText = "Yes"
//  val noText = "No"
//  val continueText = "Continue"
//
//  val expectedErrorText = "Select yes to remove this account"
//
//  "Remove Account view in English" should {
//
//    "Correctly render for an untaxed account" when {
//      "the user is an individual" when {
//        "There are no form errors " when {
//          "The account is not the last account" which {
//            lazy val view = removeAccountView(yesNoForm, taxYear, UNTAXED, account, isLastAccount = false)(user, messages, mockAppConfig)
//            implicit lazy val document: Document = Jsoup.parse(view.body)
//
//            titleCheck(expectedTitle)
//            welshToggleCheck("English")
//            textOnPageCheck(expectedCaption, captionSelector)
//            h1Check(expectedH1 + " " + expectedCaption)
//            radioButtonCheck(yesText, 1)
//            radioButtonCheck(noText, 2)
//            buttonCheck(continueText, continueButtonSelector)
//            formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, UNTAXED, account.id.get).url, continueButtonFormSelector)
//          }
//
//          "The last account is being removed" which {
//            lazy val view = removeAccountView(yesNoForm, taxYear, UNTAXED, account,
//              isLastAccount = true)(user, messages, mockAppConfig)
//
//            implicit lazy val document: Document = Jsoup.parse(view.body)
//
//            titleCheck(expectedTitle)
//            welshToggleCheck("English")
//            textOnPageCheck(expectedCaption, captionSelector)
//            h1Check(expectedH1 + " " + expectedCaption)
//            textOnPageCheck(thisWillTextUntaxed, thisWillTextSelector)
//            radioButtonCheck(yesText, 1)
//            radioButtonCheck(noText, 2)
//            buttonCheck(continueText, continueButtonSelector)
//            formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, UNTAXED, account.id.get).url, continueButtonFormSelector)
//          }
//
//        }
//
//        "There are form errors " when {
//          "no value is passed to the form" which {
//            lazy val view = removeAccountView(yesNoForm.bind(Map("value" -> "")), taxYear, UNTAXED, account,
//              isLastAccount = false)(user, messages, mockAppConfig)
//
//            implicit lazy val document: Document = Jsoup.parse(view.body)
//
//            titleCheck(expectedErrorTitle)
//            welshToggleCheck("English")
//            errorSummaryCheck(expectedErrorText, errorSummaryHref)
//            textOnPageCheck(expectedCaption, captionSelector)
//            h1Check(expectedH1 + " " + expectedCaption)
//            errorAboveElementCheck(expectedErrorText)
//            radioButtonCheck(yesText, 1)
//            radioButtonCheck(noText, 2)
//            buttonCheck(continueText, continueButtonSelector)
//            formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, UNTAXED, account.id.get).url, continueButtonFormSelector)
//          }
//        }
//      }
//
//      "the user is as an agent" when {
//        "There are no form errors " when {
//          "The account is not the last account" which {
//            lazy val view = removeAccountView(yesNoForm, taxYear, UNTAXED, account,
//              isLastAccount = false)(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)
//            implicit lazy val document: Document = Jsoup.parse(view.body)
//
//            titleCheck(expectedTitle)
//            welshToggleCheck("English")
//            textOnPageCheck(expectedCaption, captionSelector)
//            h1Check(expectedH1 + " " + expectedCaption)
//            radioButtonCheck(yesText, 1)
//            radioButtonCheck(noText, 2)
//            buttonCheck(continueText, continueButtonSelector)
//            formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, UNTAXED, account.id.get).url, continueButtonFormSelector)
//          }
//
//          "The last account is being removed" which {
//            lazy val view = removeAccountView(yesNoForm, taxYear, UNTAXED, account,
//              isLastAccount = true)(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)
//
//            implicit lazy val document: Document = Jsoup.parse(view.body)
//
//            titleCheck(expectedTitle)
//            welshToggleCheck("English")
//            textOnPageCheck(expectedCaption, captionSelector)
//            h1Check(expectedH1 + " " + expectedCaption)
//            textOnPageCheck(thisWillTextUntaxed, thisWillTextSelector)
//            radioButtonCheck(yesText, 1)
//            radioButtonCheck(noText, 2)
//            buttonCheck(continueText, continueButtonSelector)
//            formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, UNTAXED, account.id.get).url, continueButtonFormSelector)
//          }
//
//        }
//
//        "There are form errors " when {
//          "no value is passed to the form" which {
//            lazy val view = removeAccountView(yesNoForm.bind(Map("value" -> "")), taxYear, UNTAXED,
//              account, isLastAccount = false)(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)
//            implicit lazy val document: Document = Jsoup.parse(view.body)
//
//            titleCheck(expectedErrorTitle)
//            welshToggleCheck("English")
//            errorSummaryCheck(expectedErrorText, errorSummaryHref)
//            textOnPageCheck(expectedCaption, captionSelector)
//            h1Check(expectedH1 + " " + expectedCaption)
//            errorAboveElementCheck(expectedErrorText)
//            radioButtonCheck(yesText, 1)
//            radioButtonCheck(noText, 2)
//            buttonCheck(continueText, continueButtonSelector)
//            formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, UNTAXED, account.id.get).url, continueButtonFormSelector)
//          }
//        }
//      }
//    }
//
//    "Correctly render for an taxed account" when {
//      "the user is an individual" when {
//        "There are no form errors " when {
//          "The account is not the last account" which {
//            lazy val view = removeAccountView(yesNoForm, taxYear, TAXED, account, isLastAccount = false)(user, messages, mockAppConfig)
//            implicit lazy val document: Document = Jsoup.parse(view.body)
//
//            titleCheck(expectedTitle)
//            welshToggleCheck("English")
//            textOnPageCheck(expectedCaption, captionSelector)
//            h1Check(expectedH1 + " " + expectedCaption)
//            radioButtonCheck(yesText, 1)
//            radioButtonCheck(noText, 2)
//            buttonCheck(continueText, continueButtonSelector)
//            formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, TAXED, account.id.get).url, continueButtonFormSelector)
//          }
//
//          "The last account is being removed" which {
//            lazy val view = removeAccountView(yesNoForm, taxYear, TAXED, account,
//              isLastAccount = true)(user, messages, mockAppConfig)
//
//            implicit lazy val document: Document = Jsoup.parse(view.body)
//
//            titleCheck(expectedTitle)
//            welshToggleCheck("English")
//            textOnPageCheck(expectedCaption, captionSelector)
//            h1Check(expectedH1 + " " + expectedCaption)
//            textOnPageCheck(thisWillTextTaxed, thisWillTextSelector)
//            radioButtonCheck(yesText, 1)
//            radioButtonCheck(noText, 2)
//            buttonCheck(continueText, continueButtonSelector)
//            formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, TAXED, account.id.get).url, continueButtonFormSelector)
//          }
//
//        }
//
//        "There are form errors " when {
//          "no value is passed to the form" which {
//            lazy val view = removeAccountView(yesNoForm.bind(Map("value" -> "")), taxYear, TAXED, account,
//              isLastAccount = false)(user, messages, mockAppConfig)
//
//            implicit lazy val document: Document = Jsoup.parse(view.body)
//
//            titleCheck(expectedErrorTitle)
//            welshToggleCheck("English")
//            errorSummaryCheck(expectedErrorText, errorSummaryHref)
//            textOnPageCheck(expectedCaption, captionSelector)
//            h1Check(expectedH1 + " " + expectedCaption)
//            errorAboveElementCheck(expectedErrorText)
//            radioButtonCheck(yesText, 1)
//            radioButtonCheck(noText, 2)
//            buttonCheck(continueText, continueButtonSelector)
//            formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, TAXED, account.id.get).url, continueButtonFormSelector)
//          }
//        }
//      }
//
//      "the user is as an agent" when {
//        "There are no form errors " when {
//          "The account is not the last account" which {
//            lazy val view = removeAccountView(yesNoForm, taxYear, TAXED, account,
//              isLastAccount = false)(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)
//            implicit lazy val document: Document = Jsoup.parse(view.body)
//
//            titleCheck(expectedTitle)
//            welshToggleCheck("English")
//            textOnPageCheck(expectedCaption, captionSelector)
//            h1Check(expectedH1 + " " + expectedCaption)
//            radioButtonCheck(yesText, 1)
//            radioButtonCheck(noText, 2)
//            buttonCheck(continueText, continueButtonSelector)
//            formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, TAXED, account.id.get).url, continueButtonFormSelector)
//          }
//
//          "The last account is being removed" which {
//            lazy val view = removeAccountView(yesNoForm, taxYear, TAXED, account,
//              isLastAccount = true)(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)
//
//            implicit lazy val document: Document = Jsoup.parse(view.body)
//
//            titleCheck(expectedTitle)
//            welshToggleCheck("English")
//            textOnPageCheck(expectedCaption, captionSelector)
//            h1Check(expectedH1 + " " + expectedCaption)
//            textOnPageCheck(thisWillTextTaxed, thisWillTextSelector)
//            radioButtonCheck(yesText, 1)
//            radioButtonCheck(noText, 2)
//            buttonCheck(continueText, continueButtonSelector)
//            formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, TAXED, account.id.get).url, continueButtonFormSelector)
//          }
//
//        }
//
//        "There are form errors " when {
//          "no value is passed to the form" which {
//            lazy val view = removeAccountView(yesNoForm.bind(Map("value" -> "")), taxYear, TAXED,
//              account, isLastAccount = false)(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)
//            implicit lazy val document: Document = Jsoup.parse(view.body)
//
//            titleCheck(expectedErrorTitle)
//            welshToggleCheck("English")
//            errorSummaryCheck(expectedErrorText, errorSummaryHref)
//            textOnPageCheck(expectedCaption, captionSelector)
//            h1Check(expectedH1 + " " + expectedCaption)
//            errorAboveElementCheck(expectedErrorText)
//            radioButtonCheck(yesText, 1)
//            radioButtonCheck(noText, 2)
//            buttonCheck(continueText, continueButtonSelector)
//            formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, TAXED, account.id.get).url, continueButtonFormSelector)
//          }
//        }
//      }
//    }
//  }
//
//  "Remove Account view in Welsh" should {
//
//    "Correctly render for an untaxed account" when {
//      "the user is an individual" when {
//        "There are no form errors " when {
//          "The account is not the last account" which {
//            lazy val view = removeAccountView(yesNoForm, taxYear, UNTAXED, account, isLastAccount = false)(user, welshMessages, mockAppConfig)
//            implicit lazy val document: Document = Jsoup.parse(view.body)
//
//            titleCheck(expectedTitle)
//            welshToggleCheck("Welsh")
//            textOnPageCheck(expectedCaption, captionSelector)
//            h1Check(expectedH1 + " " + expectedCaption)
//            radioButtonCheck(yesText, 1)
//            radioButtonCheck(noText, 2)
//            buttonCheck(continueText, continueButtonSelector)
//            formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, UNTAXED, account.id.get).url, continueButtonFormSelector)
//          }
//
//          "The last account is being removed" which {
//            lazy val view = removeAccountView(yesNoForm, taxYear, UNTAXED, account,
//              isLastAccount = true)(user, welshMessages, mockAppConfig)
//
//            implicit lazy val document: Document = Jsoup.parse(view.body)
//
//            titleCheck(expectedTitle)
//            welshToggleCheck("Welsh")
//            textOnPageCheck(expectedCaption, captionSelector)
//            h1Check(expectedH1 + " " + expectedCaption)
//            textOnPageCheck(thisWillTextUntaxed, thisWillTextSelector)
//            radioButtonCheck(yesText, 1)
//            radioButtonCheck(noText, 2)
//            buttonCheck(continueText, continueButtonSelector)
//            formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, UNTAXED, account.id.get).url, continueButtonFormSelector)
//          }
//
//        }
//
//        "There are form errors " when {
//          "no value is passed to the form" which {
//            lazy val view = removeAccountView(yesNoForm.bind(Map("value" -> "")), taxYear, UNTAXED, account,
//              isLastAccount = false)(user, welshMessages, mockAppConfig)
//
//            implicit lazy val document: Document = Jsoup.parse(view.body)
//
//            titleCheck(expectedErrorTitle)
//            welshToggleCheck("Welsh")
//            errorSummaryCheck(expectedErrorText, errorSummaryHref)
//            textOnPageCheck(expectedCaption, captionSelector)
//            h1Check(expectedH1 + " " + expectedCaption)
//            errorAboveElementCheck(expectedErrorText)
//            radioButtonCheck(yesText, 1)
//            radioButtonCheck(noText, 2)
//            buttonCheck(continueText, continueButtonSelector)
//            formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, UNTAXED, account.id.get).url, continueButtonFormSelector)
//          }
//        }
//      }
//
//      "the user is as an agent" when {
//        "There are no form errors " when {
//          "The account is not the last account" which {
//            lazy val view = removeAccountView(yesNoForm, taxYear, UNTAXED, account,
//              isLastAccount = false)(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)
//            implicit lazy val document: Document = Jsoup.parse(view.body)
//
//            titleCheck(expectedTitle)
//            welshToggleCheck("Welsh")
//            textOnPageCheck(expectedCaption, captionSelector)
//            h1Check(expectedH1 + " " + expectedCaption)
//            radioButtonCheck(yesText, 1)
//            radioButtonCheck(noText, 2)
//            buttonCheck(continueText, continueButtonSelector)
//            formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, UNTAXED, account.id.get).url, continueButtonFormSelector)
//          }
//
//          "The last account is being removed" which {
//            lazy val view = removeAccountView(yesNoForm, taxYear, UNTAXED, account,
//              isLastAccount = true)(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)
//
//            implicit lazy val document: Document = Jsoup.parse(view.body)
//
//            titleCheck(expectedTitle)
//            welshToggleCheck("Welsh")
//            textOnPageCheck(expectedCaption, captionSelector)
//            h1Check(expectedH1 + " " + expectedCaption)
//            textOnPageCheck(thisWillTextUntaxed, thisWillTextSelector)
//            radioButtonCheck(yesText, 1)
//            radioButtonCheck(noText, 2)
//            buttonCheck(continueText, continueButtonSelector)
//            formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, UNTAXED, account.id.get).url, continueButtonFormSelector)
//          }
//
//        }
//
//        "There are form errors " when {
//          "no value is passed to the form" which {
//            lazy val view = removeAccountView(yesNoForm.bind(Map("value" -> "")), taxYear, UNTAXED,
//              account, isLastAccount = false)(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)
//            implicit lazy val document: Document = Jsoup.parse(view.body)
//
//            titleCheck(expectedErrorTitle)
//            welshToggleCheck("Welsh")
//            errorSummaryCheck(expectedErrorText, errorSummaryHref)
//            textOnPageCheck(expectedCaption, captionSelector)
//            h1Check(expectedH1 + " " + expectedCaption)
//            errorAboveElementCheck(expectedErrorText)
//            radioButtonCheck(yesText, 1)
//            radioButtonCheck(noText, 2)
//            buttonCheck(continueText, continueButtonSelector)
//            formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, UNTAXED, account.id.get).url, continueButtonFormSelector)
//          }
//        }
//      }
//    }
//
//    "Correctly render for an taxed account" when {
//      "the user is an individual" when {
//        "There are no form errors " when {
//          "The account is not the last account" which {
//            lazy val view = removeAccountView(yesNoForm, taxYear, TAXED, account, isLastAccount = false)(user, welshMessages, mockAppConfig)
//            implicit lazy val document: Document = Jsoup.parse(view.body)
//
//            titleCheck(expectedTitle)
//            welshToggleCheck("Welsh")
//            textOnPageCheck(expectedCaption, captionSelector)
//            h1Check(expectedH1 + " " + expectedCaption)
//            radioButtonCheck(yesText, 1)
//            radioButtonCheck(noText, 2)
//            buttonCheck(continueText, continueButtonSelector)
//            formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, TAXED, account.id.get).url, continueButtonFormSelector)
//          }
//
//          "The last account is being removed" which {
//            lazy val view = removeAccountView(yesNoForm, taxYear, TAXED, account,
//              isLastAccount = true)(user, welshMessages, mockAppConfig)
//
//            implicit lazy val document: Document = Jsoup.parse(view.body)
//
//            titleCheck(expectedTitle)
//            welshToggleCheck("Welsh")
//            textOnPageCheck(expectedCaption, captionSelector)
//            h1Check(expectedH1 + " " + expectedCaption)
//            textOnPageCheck(thisWillTextTaxed, thisWillTextSelector)
//            radioButtonCheck(yesText, 1)
//            radioButtonCheck(noText, 2)
//            buttonCheck(continueText, continueButtonSelector)
//            formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, TAXED, account.id.get).url, continueButtonFormSelector)
//          }
//
//        }
//
//        "There are form errors " when {
//          "no value is passed to the form" which {
//            lazy val view = removeAccountView(yesNoForm.bind(Map("value" -> "")), taxYear, TAXED, account,
//              isLastAccount = false)(user, welshMessages, mockAppConfig)
//
//            implicit lazy val document: Document = Jsoup.parse(view.body)
//
//            titleCheck(expectedErrorTitle)
//            welshToggleCheck("Welsh")
//            errorSummaryCheck(expectedErrorText, errorSummaryHref)
//            textOnPageCheck(expectedCaption, captionSelector)
//            h1Check(expectedH1 + " " + expectedCaption)
//            errorAboveElementCheck(expectedErrorText)
//            radioButtonCheck(yesText, 1)
//            radioButtonCheck(noText, 2)
//            buttonCheck(continueText, continueButtonSelector)
//            formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, TAXED, account.id.get).url, continueButtonFormSelector)
//          }
//        }
//      }
//
//      "the user is as an agent" when {
//        "There are no form errors " when {
//          "The account is not the last account" which {
//            lazy val view = removeAccountView(yesNoForm, taxYear, TAXED, account,
//              isLastAccount = false)(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)
//            implicit lazy val document: Document = Jsoup.parse(view.body)
//
//            titleCheck(expectedTitle)
//            welshToggleCheck("Welsh")
//            textOnPageCheck(expectedCaption, captionSelector)
//            h1Check(expectedH1 + " " + expectedCaption)
//            radioButtonCheck(yesText, 1)
//            radioButtonCheck(noText, 2)
//            buttonCheck(continueText, continueButtonSelector)
//            formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, TAXED, account.id.get).url, continueButtonFormSelector)
//          }
//
//          "The last account is being removed" which {
//            lazy val view = removeAccountView(yesNoForm, taxYear, TAXED, account,
//              isLastAccount = true)(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)
//
//            implicit lazy val document: Document = Jsoup.parse(view.body)
//
//            titleCheck(expectedTitle)
//            welshToggleCheck("Welsh")
//            textOnPageCheck(expectedCaption, captionSelector)
//            h1Check(expectedH1 + " " + expectedCaption)
//            textOnPageCheck(thisWillTextTaxed, thisWillTextSelector)
//            radioButtonCheck(yesText, 1)
//            radioButtonCheck(noText, 2)
//            buttonCheck(continueText, continueButtonSelector)
//            formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, TAXED, account.id.get).url, continueButtonFormSelector)
//          }
//
//        }
//
//        "There are form errors " when {
//          "no value is passed to the form" which {
//            lazy val view = removeAccountView(yesNoForm.bind(Map("value" -> "")), taxYear, TAXED,
//              account, isLastAccount = false)(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)
//            implicit lazy val document: Document = Jsoup.parse(view.body)
//
//            titleCheck(expectedErrorTitle)
//            welshToggleCheck("Welsh")
//            errorSummaryCheck(expectedErrorText, errorSummaryHref)
//            textOnPageCheck(expectedCaption, captionSelector)
//            h1Check(expectedH1 + " " + expectedCaption)
//            errorAboveElementCheck(expectedErrorText)
//            radioButtonCheck(yesText, 1)
//            radioButtonCheck(noText, 2)
//            buttonCheck(continueText, continueButtonSelector)
//            formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, TAXED, account.id.get).url, continueButtonFormSelector)
//          }
//        }
//      }
//    }
//  }
//}
