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
import play.api.data.Form
import utils.ViewTest
import views.html.dividends.ReceiveUkDividendsView

class ReceiveUkDividendsViewSpec extends ViewTest {

  lazy val yesNoFormIndividual: Form[Boolean] = YesNoForm.yesNoForm("Select yes if you got dividends from UK-based companies")
  lazy val yesNoFormAgent: Form[Boolean] = YesNoForm.yesNoForm("Select yes if your client got dividends from UK-based companies")

  lazy val receiveUkDividendsView: ReceiveUkDividendsView = app.injector.instanceOf[ReceiveUkDividendsView]

  val taxYear = 2020
  val taxYearMinusOne: Int = taxYear -1
  val expectedIndividualH1 = "Did you get dividends from UK-based companies?"
  val expectedIndividualTitle = "Did you get dividends from UK-based companies?"
  val expectedIndividualErrorTitle = s"Error: $expectedIndividualTitle"
  val expectedAgentH1 = "Did your client get dividends from UK-based companies?"
  val expectedAgentTitle = "Did your client get dividends from UK-based companies?"
  val expectedAgentErrorTitle = s"Error: $expectedAgentTitle"
  val captionText = s"Dividends for 6 April $taxYearMinusOne to 5 April $taxYear"
  val yourDividendsTextIndividual = "Your dividend voucher will tell you the shares you have in the company and the amount of the dividend you got."
  val yourDividendsTextAgent = "Your client’s dividend voucher will tell you the shares they have in the company and the amount of the dividend they got."
  val yesText = "Yes"
  val noText = "No"
  val continueText = "Continue"
  val continueLink = s"/income-through-software/return/personal-income/$taxYear/dividends/dividends-from-uk-companies"

  val captionSelector = ".govuk-caption-l"
  val yourDividendsSelector = "#main-content > div > div > form > div > fieldset > legend > p"
  val continueSelector = "#continue"
  val continueButtonFormSelector = "#main-content > div > div > form"

  "ReceivedUKDividendsView in English" should {

    "correctly render for an individual" when {

      "there are no form errors" which {

        lazy val view = receiveUkDividendsView(
          yesNoFormIndividual, taxYear)(user, messages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(expectedIndividualTitle)
        welshToggleCheck("English")
        h1Check(expectedIndividualH1)
        textOnPageCheck(captionText, captionSelector)
        textOnPageCheck(yourDividendsTextIndividual, yourDividendsSelector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }

      "there is a form error due to no radio button selected" which {

        lazy val view = receiveUkDividendsView(
          yesNoFormIndividual.bind(Map("value" -> "")), taxYear)(user, messages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedErrorText = "Select yes if you got dividends from UK-based companies"
        val errorSummaryHref = "#value"

        titleCheck(expectedIndividualErrorTitle)
        welshToggleCheck("English")
        h1Check(expectedIndividualH1)
        textOnPageCheck(captionText, captionSelector)
        errorSummaryCheck(expectedErrorText, errorSummaryHref)
        textOnPageCheck(yourDividendsTextIndividual, yourDividendsSelector)
        errorAboveElementCheck(expectedErrorText)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }
    }

    "correctly render for an agent" when {

      "there are no form errors" which {

        lazy val view = receiveUkDividendsView(
          yesNoFormAgent, taxYear)(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(expectedAgentTitle)
        welshToggleCheck("English")
        h1Check(expectedAgentH1)
        textOnPageCheck(captionText, captionSelector)
        textOnPageCheck(yourDividendsTextAgent, yourDividendsSelector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }

      "there is a form error due to no radio button selected" which {

        lazy val view = receiveUkDividendsView(
          yesNoFormAgent.bind(Map("value" -> "")), taxYear)(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedErrorText = "Select yes if your client got dividends from UK-based companies"
        val errorSummaryHref = "#value"

        titleCheck(expectedAgentErrorTitle)
        welshToggleCheck("English")
        h1Check(expectedAgentH1)
        textOnPageCheck(captionText, captionSelector)
        errorSummaryCheck(expectedErrorText, errorSummaryHref)
        textOnPageCheck(yourDividendsTextAgent, yourDividendsSelector)
        errorAboveElementCheck(expectedErrorText)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }
    }
  }

  "ReceivedUKDividendsView in Welsh" should {

    "correctly render for an individual" when {

      "there are no form errors" which {

        lazy val view = receiveUkDividendsView(
          yesNoFormIndividual, taxYear)(user, welshMessages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(expectedIndividualTitle)
        welshToggleCheck("Welsh")
        h1Check(expectedIndividualH1)
        textOnPageCheck(captionText, captionSelector)
        textOnPageCheck(yourDividendsTextIndividual, yourDividendsSelector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }

      "there is a form error due to no radio button selected" which {

        lazy val view = receiveUkDividendsView(
          yesNoFormIndividual.bind(Map("value" -> "")), taxYear)(user, welshMessages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedErrorText = "Select yes if you got dividends from UK-based companies"
        val errorSummaryHref = "#value"

        titleCheck(expectedIndividualErrorTitle)
        welshToggleCheck("Welsh")
        h1Check(expectedIndividualH1)
        textOnPageCheck(captionText, captionSelector)
        errorSummaryCheck(expectedErrorText, errorSummaryHref)
        textOnPageCheck(yourDividendsTextIndividual, yourDividendsSelector)
        errorAboveElementCheck(expectedErrorText)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }
    }

    "correctly render for an agent" when {

      "there are no form errors" which {

        lazy val view = receiveUkDividendsView(
          yesNoFormAgent, taxYear)(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(expectedAgentTitle)
        welshToggleCheck("Welsh")
        h1Check(expectedAgentH1)
        textOnPageCheck(captionText, captionSelector)
        textOnPageCheck(yourDividendsTextAgent, yourDividendsSelector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }

      "there is a form error due to no radio button selected" which {

        lazy val view = receiveUkDividendsView(
          yesNoFormAgent.bind(Map("value" -> "")), taxYear)(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedErrorText = "Select yes if your client got dividends from UK-based companies"
        val errorSummaryHref = "#value"

        titleCheck(expectedAgentErrorTitle)
        welshToggleCheck("Welsh")
        h1Check(expectedAgentH1)
        textOnPageCheck(captionText, captionSelector)
        errorSummaryCheck(expectedErrorText, errorSummaryHref)
        textOnPageCheck(yourDividendsTextAgent, yourDividendsSelector)
        errorAboveElementCheck(expectedErrorText)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }
    }
  }
}
