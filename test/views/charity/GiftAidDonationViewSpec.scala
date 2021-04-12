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

package views.charity

import forms.YesNoForm
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.Form
import utils.ViewTest
import views.html.charity.GiftAidDonationView
import views.html.dividends.ReceiveUkDividendsView

class GiftAidDonationViewSpec extends ViewTest {

  lazy val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm("Select yes if you used Gift Aid to donate to charity")
  lazy val yesNoFormAgent: Form[Boolean] = YesNoForm.yesNoForm("Select yes if your client used Gift Aid to donate to charity")
  lazy val gitAidDonationView: GiftAidDonationView = app.injector.instanceOf[GiftAidDonationView]

  val taxYear = 2020
  val taxYearMinusOne: Int = taxYear -1
  val expectedIndividualH1 = "Did you use Gift Aid to donate to charity?"
  val expectedIndividualTitle = "Did you use Gift Aid to donate to charity?"
  val expectedIndividualErrorTitle = s"Error: $expectedIndividualTitle"
  val expectedAgentH1 = "Did your client use Gift Aid to donate to charity?"
  val expectedAgentTitle = "Did your client use Gift Aid to donate to charity?"
  val expectedAgentErrorTitle = s"Error: $expectedAgentTitle"
  val captionText = s"Charitable giving for 6 April $taxYearMinusOne to 5 April $taxYear"
  val yesText = "Yes"
  val noText = "No"
  val continueText = "Continue"
  val continueLink = s"/income-through-software/return/personal-income/$taxYear/charity/uk-charity"

  val captionSelector = ".govuk-caption-l"
  val yourDividendsSelector = "#value-hint"
  val continueSelector = "#continue"
  val continueButtonFormSelector = "#main-content > div > div > form"

  "GiftAidDonationView in English" should {

    "correctly render for an individual" when {

      "there are no form errors" which {

        lazy val view = gitAidDonationView(
          yesNoForm, taxYear)(user, messages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(expectedIndividualTitle)
        welshToggleCheck("English")
        h1Check(expectedIndividualH1)
        textOnPageCheck(captionText, captionSelector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }

      "there is a form error due to no radio button selected" which {

        lazy val view = gitAidDonationView(
          yesNoForm.bind(Map("value" -> "")), taxYear)(user, messages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedErrorText = "Select yes if you used Gift Aid to donate to charity"
        val errorSummaryHref = "#value"

        titleCheck(expectedIndividualErrorTitle)
        welshToggleCheck("English")
        h1Check(expectedIndividualH1)
        textOnPageCheck(captionText, captionSelector)
        errorSummaryCheck(expectedErrorText, errorSummaryHref)
        errorAboveElementCheck(expectedErrorText)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }
    }

    "correctly render for an agent" when {

      "there are no form errors" which {

        lazy val view = gitAidDonationView(
          yesNoFormAgent, taxYear)(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(expectedAgentTitle)
        welshToggleCheck("English")
        h1Check(expectedAgentH1)
        textOnPageCheck(captionText, captionSelector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }

      "there is a form error due to no radio button selected" which {

        lazy val view = gitAidDonationView(
          yesNoFormAgent.bind(Map("value" -> "")), taxYear)(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedErrorText = "Select yes if your client used Gift Aid to donate to charity"
        val errorSummaryHref = "#value"

        titleCheck(expectedAgentErrorTitle)
        welshToggleCheck("English")
        h1Check(expectedAgentH1)
        textOnPageCheck(captionText, captionSelector)
        errorSummaryCheck(expectedErrorText, errorSummaryHref)
        errorAboveElementCheck(expectedErrorText)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }
    }
  }
}
