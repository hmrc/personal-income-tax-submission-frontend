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
import views.html.charity.GiftAidOneOffView

class GiftAidOneOffViewSpec extends ViewTest {

  lazy val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm("Select yes if you made a one-off donation to charity")
  lazy val yesNoFormAgent: Form[Boolean] = YesNoForm.yesNoForm("Select yes if your client made a one-off donation to charity")
  lazy val gitAidOneOffView: GiftAidOneOffView = app.injector.instanceOf[GiftAidOneOffView]

  val taxYear = 2020
  val giftAidDonations = 100
  val taxYearMinusOne: Int = taxYear -1
  val expectedIndividualH1 = "Did you make one-off donations?"
  val expectedIndividualTitle = "Did you make one-off donations?"
  val expectedIndividualPara1= s"You told us you used Gift Aid to donate £$giftAidDonations to charity. Tell us if any of this was made as one-off payments."
  val expectedIndividualPara2 = "One-off donations are payments you did not repeat."
  val expectedIndividualErrorTitle = s"Error: $expectedIndividualTitle"
  val expectedAgentH1 = "Did your client make one-off donations?"
  val expectedAgentTitle = "Did your client make one-off donations?"
  val expectedAgentPara1 = s"You told us your client used Gift Aid to donate £$giftAidDonations to charity. Tell us if any of this was made as one-off payments."
  val expectedAgentPara2 = "One-off donations are payments your client did not repeat."
  val expectedAgentErrorTitle = s"Error: $expectedAgentTitle"
  val captionText = s"Donations to charity for 6 April $taxYearMinusOne to 5 April $taxYear"
  val yesText = "Yes"
  val noText = "No"
  val continueText = "Continue"
  val continueLink = s"/income-through-software/return/personal-income/$taxYear/charity/oneoff-charity-donations"

  val captionSelector = "#main-content > div > div > form > div > fieldset > legend > header > p"
  val p1Selector = "#main-content > div > div > form > div > fieldset > legend > div > p:nth-child(1)"
  val p2Selector = "#main-content > div > div > form > div > fieldset > legend > div > p:nth-child(2)"
  val continueSelector = "#continue"
  val continueButtonFormSelector = "#main-content > div > div > form"



  "GiftAidOneOffView in English" should {

    "correctly render for an individual" when {

      "there are no form errors" which {

        lazy val view = gitAidOneOffView(
          yesNoForm, taxYear,giftAidDonations)(user, messages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(expectedIndividualTitle)
        welshToggleCheck("English")
        h1Check(expectedIndividualH1)
        textOnPageCheck(captionText, captionSelector)
        textOnPageCheck(expectedIndividualPara1, p1Selector)
        textOnPageCheck(expectedIndividualPara2, p2Selector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }

      "there is a form error due to no radio button selected" which {

        lazy val view = gitAidOneOffView(
          yesNoForm.bind(Map("value" -> "")), taxYear, giftAidDonations)(user, messages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedErrorText = "Select yes if you made a one-off donation to charity"
        val errorSummaryHref = "#value"

        titleCheck(expectedIndividualErrorTitle)
        welshToggleCheck("English")
        h1Check(expectedIndividualH1)
        textOnPageCheck(captionText, captionSelector)
        textOnPageCheck(expectedIndividualPara1, p1Selector)
        textOnPageCheck(expectedIndividualPara2, p2Selector)
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

        lazy val view = gitAidOneOffView(
          yesNoFormAgent, taxYear, giftAidDonations)(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(expectedAgentTitle)
        welshToggleCheck("English")
        h1Check(expectedAgentH1)
        textOnPageCheck(captionText, captionSelector)
        textOnPageCheck(expectedAgentPara1, p1Selector)
        textOnPageCheck(expectedAgentPara2, p2Selector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }

      "there is a form error due to no radio button selected" which {

        lazy val view = gitAidOneOffView(
          yesNoFormAgent.bind(Map("value" -> "")), taxYear, giftAidDonations)(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedErrorText = "Select yes if your client made a one-off donation to charity"
        val errorSummaryHref = "#value"

        titleCheck(expectedAgentErrorTitle)
        welshToggleCheck("English")
        h1Check(expectedAgentH1)
        textOnPageCheck(captionText, captionSelector)
        textOnPageCheck(expectedAgentPara1, p1Selector)
        textOnPageCheck(expectedAgentPara2, p2Selector)
        errorSummaryCheck(expectedErrorText, errorSummaryHref)
        errorAboveElementCheck(expectedErrorText)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }
    }
  }

  "GiftAidOneOffView in Welsh" should {

    "correctly render for an individual" when {

      "there are no form errors" which {

        lazy val view = gitAidOneOffView(
          yesNoForm, taxYear, giftAidDonations)(user, welshMessages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(expectedIndividualTitle)
        welshToggleCheck("Welsh")
        h1Check(expectedIndividualH1)
        textOnPageCheck(captionText, captionSelector)
        textOnPageCheck(expectedIndividualPara1, p1Selector)
        textOnPageCheck(expectedIndividualPara2, p2Selector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }

      "there is a form error due to no radio button selected" which {

        lazy val view = gitAidOneOffView(
          yesNoForm.bind(Map("value" -> "")), taxYear, giftAidDonations)(user, welshMessages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedErrorText = "Select yes if you made a one-off donation to charity"
        val errorSummaryHref = "#value"

        titleCheck(expectedIndividualErrorTitle)
        welshToggleCheck("Welsh")
        h1Check(expectedIndividualH1)
        textOnPageCheck(captionText, captionSelector)
        textOnPageCheck(expectedIndividualPara1, p1Selector)
        textOnPageCheck(expectedIndividualPara2, p2Selector)
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

        lazy val view = gitAidOneOffView(
          yesNoFormAgent, taxYear, giftAidDonations)(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(expectedAgentTitle)
        welshToggleCheck("Welsh")
        h1Check(expectedAgentH1)
        textOnPageCheck(captionText, captionSelector)
        textOnPageCheck(expectedAgentPara1, p1Selector)
        textOnPageCheck(expectedAgentPara2, p2Selector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
      }

      "there is a form error due to no radio button selected" which {

        lazy val view = gitAidOneOffView(
          yesNoFormAgent.bind(Map("value" -> "")), taxYear, giftAidDonations)(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedErrorText = "Select yes if your client made a one-off donation to charity"
        val errorSummaryHref = "#value"

        titleCheck(expectedAgentErrorTitle)
        welshToggleCheck("Welsh")
        h1Check(expectedAgentH1)
        textOnPageCheck(captionText, captionSelector)
        textOnPageCheck(expectedAgentPara1, p1Selector)
        textOnPageCheck(expectedAgentPara2, p2Selector)
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
