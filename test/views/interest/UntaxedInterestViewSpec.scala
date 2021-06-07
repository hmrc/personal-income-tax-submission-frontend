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

import forms.YesNoForm

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import utils.ViewTest
import views.html.interest.UntaxedInterestView

class UntaxedInterestViewSpec extends ViewTest {

  lazy val yesNoFormIndividual: Form[Boolean] = YesNoForm.yesNoForm("Select yes if you got untaxed UK interest")
  lazy val yesNoFormAgent: Form[Boolean] = YesNoForm.yesNoForm("Select yes if your client got untaxed UK interest")

  lazy val untaxedInterestView: UntaxedInterestView = app.injector.instanceOf[UntaxedInterestView]
  val taxYear = 2020
  val taxYearMinusOne: Int = taxYear -1

  val captionSelector = ".govuk-caption-l"
  val forExampleSelector = "#main-content > div > div > form > div > fieldset > legend > p:nth-child(2)"
  val bulletPointSelector1 = "#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child(1)"
  val bulletPointSelector2 = "#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child(2)"
  val bulletPointSelector3 = "#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child(3)"
  val doNotIncludeSelector = "#main-content > div > div > form > div > fieldset > legend > p:nth-child(4)"
  val continueSelector = "#continue"
  val continueFormSelector = "#main-content > div > div > form"

  val expectedIndividualTitle = "Did you get untaxed interest from the UK?"
  val expectedIndividualErrorTitle = s"Error: $expectedIndividualTitle"
  val expectedIndividualH1 = "Did you get untaxed interest from the UK?"
  val expectedAgentTitle = "Did your client get untaxed interest from the UK?"
  val expectedAgentErrorTitle = s"Error: $expectedAgentTitle"
  val expectedAgentH1 = "Did your client get untaxed interest from the UK?"
  val expectedCaption = s"Interest for 6 April $taxYearMinusOne to 5 April $taxYear"
  val forExampleText = "This could be interest from:"
  val banksAndBuildingsText = "banks and building societies"
  val savingsAndCreditText = "savings and credit union accounts"
  val peerToPeerText = "peer-to-peer lending"
  val doNotIncludeTextIndividual = "Do not include interest you got from an Individual Savings Account (ISA) or gilts."
  val doNotIncludeTextAgent = "Do not include interest your client got from an Individual Savings Account (ISA) or gilts."
  val yesText = "Yes"
  val noText = "No"
  val continueText = "Continue"
  val continueLink = s"/income-through-software/return/personal-income/$taxYear/interest/untaxed-uk-interest"
  val errorSummaryHref = "#value"

  "Untaxed interest view in English" should {

    "Correctly render as an individual" when {
      "There are no form errors " which {
        lazy val view = untaxedInterestView(
          yesNoFormIndividual, taxYear)(user, messages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(expectedIndividualTitle)
        welshToggleCheck("English")
        h1Check(expectedIndividualH1 + " " + expectedCaption)
        textOnPageCheck(expectedCaption, captionSelector)

        textOnPageCheck(forExampleText, forExampleSelector)
        textOnPageCheck(banksAndBuildingsText, bulletPointSelector1)
        textOnPageCheck(savingsAndCreditText, bulletPointSelector2)
        textOnPageCheck(peerToPeerText, bulletPointSelector3)

        textOnPageCheck(doNotIncludeTextIndividual, doNotIncludeSelector)

        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(continueLink, continueFormSelector)
      }
    }
    "correctly render with errors as an individual" when {
      "no value is passed in" which {
        lazy val view = untaxedInterestView(
          yesNoFormIndividual.bind(Map("value" -> "")),
          taxYear
        )(user, messages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedErrorText = "Select yes if you got untaxed UK interest"

        titleCheck(expectedIndividualErrorTitle)
        welshToggleCheck("English")
        h1Check(expectedIndividualH1 + " " + expectedCaption)
        textOnPageCheck(expectedCaption, captionSelector)
        errorSummaryCheck(expectedErrorText, errorSummaryHref)
        errorAboveElementCheck(expectedErrorText)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(continueLink, continueFormSelector)
      }
    }
    "correctly render with no errors as an agent" when {
      "there are no form errors" which {

        lazy val view = untaxedInterestView(
          yesNoFormAgent, taxYear)(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(expectedAgentTitle)
        welshToggleCheck("English")
        h1Check(expectedAgentH1 + " " + expectedCaption)
        textOnPageCheck(expectedCaption, captionSelector)

        textOnPageCheck(forExampleText, forExampleSelector)
        textOnPageCheck(banksAndBuildingsText, bulletPointSelector1)
        textOnPageCheck(savingsAndCreditText, bulletPointSelector2)
        textOnPageCheck(peerToPeerText, bulletPointSelector3)

        textOnPageCheck(doNotIncludeTextAgent, doNotIncludeSelector)

        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)

        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(continueLink, continueFormSelector)
      }
    }
    "correctly render with errors as an agent" when {
      "there is a form error" which {
        lazy val view = untaxedInterestView(
          yesNoFormAgent.bind(Map("value" -> "")),
          taxYear
        )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedErrorText = "Select yes if your client got untaxed UK interest"

        titleCheck(expectedAgentErrorTitle)
        welshToggleCheck("English")
        h1Check(expectedAgentH1 + " " + expectedCaption)
        textOnPageCheck(expectedCaption, captionSelector)
        errorSummaryCheck(expectedErrorText, errorSummaryHref)
        errorAboveElementCheck(expectedErrorText)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(continueLink, continueFormSelector)
      }
    }
  }

  "Untaxed interest view in Welsh" should {

    "Correctly render as an individual" when {
      "There are no form errors " which {
        lazy val view = untaxedInterestView(
          yesNoFormIndividual, taxYear)(user, welshMessages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(expectedIndividualTitle)
        welshToggleCheck("Welsh")
        h1Check(expectedIndividualH1 + " " + expectedCaption)
        textOnPageCheck(expectedCaption, captionSelector)

        textOnPageCheck(forExampleText, forExampleSelector)
        textOnPageCheck(banksAndBuildingsText, bulletPointSelector1)
        textOnPageCheck(savingsAndCreditText, bulletPointSelector2)
        textOnPageCheck(peerToPeerText, bulletPointSelector3)

        textOnPageCheck(doNotIncludeTextIndividual, doNotIncludeSelector)

        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(continueLink, continueFormSelector)
      }
    }
    "correctly render with errors as an individual" when {
      "no value is passed in" which {
        lazy val view = untaxedInterestView(
          yesNoFormIndividual.bind(Map("value" -> "")),
          taxYear
        )(user, welshMessages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedErrorText = "Select yes if you got untaxed UK interest"

        titleCheck(expectedIndividualErrorTitle)
        welshToggleCheck("Welsh")
        h1Check(expectedIndividualH1 + " " + expectedCaption)
        textOnPageCheck(expectedCaption, captionSelector)
        errorSummaryCheck(expectedErrorText, errorSummaryHref)
        errorAboveElementCheck(expectedErrorText)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(continueLink, continueFormSelector)
      }
    }
    "correctly render with no errors as an agent" when {
      "there are no form errors" which {

        lazy val view = untaxedInterestView(
          yesNoFormAgent, taxYear)(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(expectedAgentTitle)
        welshToggleCheck("Welsh")
        h1Check(expectedAgentH1 + " " + expectedCaption)
        textOnPageCheck(expectedCaption, captionSelector)

        textOnPageCheck(forExampleText, forExampleSelector)
        textOnPageCheck(banksAndBuildingsText, bulletPointSelector1)
        textOnPageCheck(savingsAndCreditText, bulletPointSelector2)
        textOnPageCheck(peerToPeerText, bulletPointSelector3)

        textOnPageCheck(doNotIncludeTextAgent, doNotIncludeSelector)

        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)

        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(continueLink, continueFormSelector)
      }
    }
    "correctly render with errors as an agent" when {
      "there is a form error" which {
        lazy val view = untaxedInterestView(
          yesNoFormAgent.bind(Map("value" -> "")),
          taxYear
        )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)

        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedErrorText = "Select yes if your client got untaxed UK interest"

        titleCheck(expectedAgentErrorTitle)
        welshToggleCheck("Welsh")
        h1Check(expectedAgentH1 + " " + expectedCaption)
        textOnPageCheck(expectedCaption, captionSelector)
        errorSummaryCheck(expectedErrorText, errorSummaryHref)
        errorAboveElementCheck(expectedErrorText)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(continueLink, continueFormSelector)
      }
    }
  }
}
