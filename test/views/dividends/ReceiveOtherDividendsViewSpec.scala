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
import views.html.dividends.ReceiveOtherUkDividendsView

class ReceiveOtherDividendsViewSpec extends ViewTest {

  lazy val yesNoFormIndividual: Form[Boolean] = YesNoForm.yesNoForm("Select yes if you got dividends from UK-based trusts or open-ended investment companies")
  lazy val yesNoFormAgent: Form[Boolean] = YesNoForm.yesNoForm("Select yes if your " +
    "client got dividends from UK-based trusts or open-ended investment companies")

  lazy val receiveOtherDividendsView: ReceiveOtherUkDividendsView = app.injector.instanceOf[ReceiveOtherUkDividendsView]

  val taxYear = 2020
  val taxYearMinusOne: Int = taxYear -1

  val captionSelector = ".govuk-caption-l"
  val continueSelector = "#continue"
  val continueButtonFormSelector = "#main-content > div > div > form"
  val youMustAlsoSelector = "#main-content > div > div > form > div > fieldset > legend > div:nth-child(2) > p"
  val authorisedBulletSelector = "#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child(1)"
  val investmentBulletSelector = "#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child(2)"
  val yourDividendsBulletSelector = "#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child(3)"
  val youDoNotNeedSelector = "#main-content > div > div > form > div > fieldset > legend > div:nth-child(4) > p"
  val whatAreInvestmentSelector = "#main-content > div > div > form > details:nth-child(2) > summary > span"
  val investmentTrustSelector = "#main-content > div > div > form > details:nth-child(2) > div > p:nth-child(1)"
  val unitTrustsSelector = "#main-content > div > div > form > details:nth-child(2) > div > p:nth-child(2)"
  val openEndedSelector = "#main-content > div > div > form > details:nth-child(2) > div > p:nth-child(3)"
  val whatAreEqualisationSelector = "#main-content > div > div > form > details:nth-child(3) > summary > span"
  val equalisationPaymentsSelector = "#main-content > div > div > form > details:nth-child(3) > div > p"
  val continueButtonSelector = "#continue"
  val expectedErrorHref = "#value"

  val expectedIndividualH1 = "Did you get dividends from UK-based trusts or open-ended investment companies?"
  val expectedIndividualTitle = "Did you get dividends from UK-based trusts or open-ended investment companies?"
  val expectedIndividualErrorTitle = s"Error: $expectedIndividualTitle"
  val expectedAgentH1 = "Did your client get dividends from UK-based trusts or open-ended investment companies?"
  val expectedAgentTitle = "Did your client get dividends from UK-based trusts or open-ended investment companies?"
  val expectedAgentErrorTitle = s"Error: $expectedAgentTitle"
  val expectedCaption = s"Dividends for 6 April $taxYearMinusOne to 5 April $taxYear"
  val youMustAlsoText = "You must also tell us about:"
  val authorisedBulletText = "authorised unit trusts"
  val investmentBulletText = "investment trusts"
  val yourDividendsBulletText = "your dividends that were automatically reinvested"
  val youDoNotNeedAgentText = "You do not need to tell us about amounts shown as 'equalisation' on your client’s dividend voucher."
  val youDoNotNeedIndividualText = "You do not need to tell us about amounts shown as 'equalisation' on your dividend voucher."
  val whatAreInvestmentText = "What are investment trusts, unit trusts and open-ended investment companies?"
  val investmentTrustText = "Investment trusts make money through buying and selling shares or assets in other companies."
  val unitTrustsText: String = "Unit trusts make money by buying and selling bonds or shares on the stock market. The fund is split " +
    "into units which an investor buys. A fund manager creates and cancels units when investors join and leave the trust."
  val openEndedText = "Open-ended investment companies are like unit trusts but create and cancel shares, rather than units, when investors join or leave."
  val whatAreEqualisationText = "What are equalisation payments?"
  val equalisationPaymentsText: String = "Equalisation payments are given to investors to make sure they’re charged fairly based " +
    "on the performance of the trust. Equalisation payments are not counted as income because they’re a return of part of an investment."
  val yesText = "Yes"
  val noText = "No"
  val continueText = "Continue"
  val continueLink = s"/income-through-software/return/personal-income/$taxYear/dividends/dividends-from-uk-trusts-or-open-ended-investment-companies"

  "ReceivedDividendsView in English" should {

    "correctly render for an individual" when {

      "there are no form errors" which {

        lazy val view = receiveOtherDividendsView(
          yesNoFormIndividual, taxYear)(user, messages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(expectedIndividualTitle)
        welshToggleCheck("English")
        h1Check(expectedIndividualH1)
        textOnPageCheck(expectedCaption, captionSelector)
        textOnPageCheck(youMustAlsoText, youMustAlsoSelector)
        textOnPageCheck(authorisedBulletText, authorisedBulletSelector)
        textOnPageCheck(investmentBulletText, investmentBulletSelector)
        textOnPageCheck(yourDividendsBulletText, yourDividendsBulletSelector)
        textOnPageCheck(youDoNotNeedIndividualText, youDoNotNeedSelector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueButtonSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
        textOnPageCheck(whatAreInvestmentText, whatAreInvestmentSelector)
        textOnPageCheck(investmentTrustText, investmentTrustSelector)
        textOnPageCheck(unitTrustsText, unitTrustsSelector)
        textOnPageCheck(openEndedText, openEndedSelector)
        textOnPageCheck(whatAreEqualisationText, whatAreEqualisationSelector)
        textOnPageCheck(equalisationPaymentsText, equalisationPaymentsSelector)
      }

      "there are form errors" when {

        "a form with an empty value field is passed in" which {

          lazy val view = receiveOtherDividendsView(
            yesNoFormIndividual.bind(Map("value" -> "")),
            taxYear
          )(user, messages, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          val expectedErrorText = "Select yes if you got dividends from UK-based trusts or open-ended investment companies"

          titleCheck(expectedIndividualErrorTitle)
          welshToggleCheck("English")
          h1Check(expectedIndividualH1)
          textOnPageCheck(expectedCaption, captionSelector)
          errorSummaryCheck(expectedErrorText, expectedErrorHref)
          textOnPageCheck(youMustAlsoText, youMustAlsoSelector)
          textOnPageCheck(authorisedBulletText, authorisedBulletSelector)
          textOnPageCheck(investmentBulletText, investmentBulletSelector)
          textOnPageCheck(yourDividendsBulletText, yourDividendsBulletSelector)
          textOnPageCheck(youDoNotNeedIndividualText, youDoNotNeedSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          textOnPageCheck(whatAreInvestmentText, whatAreInvestmentSelector)
          textOnPageCheck(investmentTrustText, investmentTrustSelector)
          textOnPageCheck(unitTrustsText, unitTrustsSelector)
          textOnPageCheck(openEndedText, openEndedSelector)
          textOnPageCheck(whatAreEqualisationText, whatAreEqualisationSelector)
          textOnPageCheck(equalisationPaymentsText, equalisationPaymentsSelector)
        }
      }
    }

    "correctly render for an agent" when {

      "there are no form errors" which {

        lazy val view = receiveOtherDividendsView(
          yesNoFormAgent, taxYear)(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(expectedAgentTitle)
        welshToggleCheck("English")
        h1Check(expectedAgentH1)
        textOnPageCheck(expectedCaption, captionSelector)
        textOnPageCheck(youMustAlsoText, youMustAlsoSelector)
        textOnPageCheck(authorisedBulletText, authorisedBulletSelector)
        textOnPageCheck(investmentBulletText, investmentBulletSelector)
        textOnPageCheck(yourDividendsBulletText, yourDividendsBulletSelector)
        textOnPageCheck(youDoNotNeedAgentText, youDoNotNeedSelector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueButtonSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
        textOnPageCheck(whatAreInvestmentText, whatAreInvestmentSelector)
        textOnPageCheck(investmentTrustText, investmentTrustSelector)
        textOnPageCheck(unitTrustsText, unitTrustsSelector)
        textOnPageCheck(openEndedText, openEndedSelector)
        textOnPageCheck(whatAreEqualisationText, whatAreEqualisationSelector)
        textOnPageCheck(equalisationPaymentsText, equalisationPaymentsSelector)
      }

      "there are form errors" when {

        "a form with an empty value field is passed in" which {

          lazy val view = receiveOtherDividendsView(
            yesNoFormAgent.bind(Map("value" -> "")),
            taxYear
          )(user.copy(arn = Some("XARN1234567")), messages, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          val expectedErrorText = "Select yes if your client got dividends from UK-based trusts or open-ended investment companies"

          titleCheck(expectedAgentErrorTitle)
          welshToggleCheck("English")
          h1Check(expectedAgentH1)
          textOnPageCheck(expectedCaption, captionSelector)
          errorSummaryCheck(expectedErrorText, expectedErrorHref)
          textOnPageCheck(youMustAlsoText, youMustAlsoSelector)
          textOnPageCheck(authorisedBulletText, authorisedBulletSelector)
          textOnPageCheck(investmentBulletText, investmentBulletSelector)
          textOnPageCheck(yourDividendsBulletText, yourDividendsBulletSelector)
          textOnPageCheck(youDoNotNeedAgentText, youDoNotNeedSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          textOnPageCheck(whatAreInvestmentText, whatAreInvestmentSelector)
          textOnPageCheck(investmentTrustText, investmentTrustSelector)
          textOnPageCheck(unitTrustsText, unitTrustsSelector)
          textOnPageCheck(openEndedText, openEndedSelector)
          textOnPageCheck(whatAreEqualisationText, whatAreEqualisationSelector)
          textOnPageCheck(equalisationPaymentsText, equalisationPaymentsSelector)
        }
      }
    }
  }

  "ReceivedDividendsView in Welsh" should {

    "correctly render for an individual" when {

      "there are no form errors" which {

        lazy val view = receiveOtherDividendsView(
          yesNoFormIndividual, taxYear)(user, welshMessages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(expectedIndividualTitle)
        welshToggleCheck("Welsh")
        h1Check(expectedIndividualH1)
        textOnPageCheck(expectedCaption, captionSelector)
        textOnPageCheck(youMustAlsoText, youMustAlsoSelector)
        textOnPageCheck(authorisedBulletText, authorisedBulletSelector)
        textOnPageCheck(investmentBulletText, investmentBulletSelector)
        textOnPageCheck(yourDividendsBulletText, yourDividendsBulletSelector)
        textOnPageCheck(youDoNotNeedIndividualText, youDoNotNeedSelector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueButtonSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
        textOnPageCheck(whatAreInvestmentText, whatAreInvestmentSelector)
        textOnPageCheck(investmentTrustText, investmentTrustSelector)
        textOnPageCheck(unitTrustsText, unitTrustsSelector)
        textOnPageCheck(openEndedText, openEndedSelector)
        textOnPageCheck(whatAreEqualisationText, whatAreEqualisationSelector)
        textOnPageCheck(equalisationPaymentsText, equalisationPaymentsSelector)
      }

      "there are form errors" when {

        "a form with an empty value field is passed in" which {

          lazy val view = receiveOtherDividendsView(
            yesNoFormIndividual.bind(Map("value" -> "")),
            taxYear
          )(user, welshMessages, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          val expectedErrorText = "Select yes if you got dividends from UK-based trusts or open-ended investment companies"

          titleCheck(expectedIndividualErrorTitle)
          welshToggleCheck("Welsh")
          h1Check(expectedIndividualH1)
          textOnPageCheck(expectedCaption, captionSelector)
          errorSummaryCheck(expectedErrorText, expectedErrorHref)
          textOnPageCheck(youMustAlsoText, youMustAlsoSelector)
          textOnPageCheck(authorisedBulletText, authorisedBulletSelector)
          textOnPageCheck(investmentBulletText, investmentBulletSelector)
          textOnPageCheck(yourDividendsBulletText, yourDividendsBulletSelector)
          textOnPageCheck(youDoNotNeedIndividualText, youDoNotNeedSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          textOnPageCheck(whatAreInvestmentText, whatAreInvestmentSelector)
          textOnPageCheck(investmentTrustText, investmentTrustSelector)
          textOnPageCheck(unitTrustsText, unitTrustsSelector)
          textOnPageCheck(openEndedText, openEndedSelector)
          textOnPageCheck(whatAreEqualisationText, whatAreEqualisationSelector)
          textOnPageCheck(equalisationPaymentsText, equalisationPaymentsSelector)
        }
      }
    }

    "correctly render for an agent" when {

      "there are no form errors" which {

        lazy val view = receiveOtherDividendsView(
          yesNoFormAgent, taxYear)(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(expectedAgentTitle)
        welshToggleCheck("Welsh")
        h1Check(expectedAgentH1)
        textOnPageCheck(expectedCaption, captionSelector)
        textOnPageCheck(youMustAlsoText, youMustAlsoSelector)
        textOnPageCheck(authorisedBulletText, authorisedBulletSelector)
        textOnPageCheck(investmentBulletText, investmentBulletSelector)
        textOnPageCheck(yourDividendsBulletText, yourDividendsBulletSelector)
        textOnPageCheck(youDoNotNeedAgentText, youDoNotNeedSelector)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(continueText, continueButtonSelector)
        formPostLinkCheck(continueLink, continueButtonFormSelector)
        textOnPageCheck(whatAreInvestmentText, whatAreInvestmentSelector)
        textOnPageCheck(investmentTrustText, investmentTrustSelector)
        textOnPageCheck(unitTrustsText, unitTrustsSelector)
        textOnPageCheck(openEndedText, openEndedSelector)
        textOnPageCheck(whatAreEqualisationText, whatAreEqualisationSelector)
        textOnPageCheck(equalisationPaymentsText, equalisationPaymentsSelector)
      }

      "there are form errors" when {

        "a form with an empty value field is passed in" which {

          lazy val view = receiveOtherDividendsView(
            yesNoFormAgent.bind(Map("value" -> "")),
            taxYear
          )(user.copy(arn = Some("XARN1234567")), welshMessages, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          val expectedErrorText = "Select yes if your client got dividends from UK-based trusts or open-ended investment companies"

          titleCheck(expectedAgentErrorTitle)
          welshToggleCheck("Welsh")
          h1Check(expectedAgentH1)
          textOnPageCheck(expectedCaption, captionSelector)
          errorSummaryCheck(expectedErrorText, expectedErrorHref)
          textOnPageCheck(youMustAlsoText, youMustAlsoSelector)
          textOnPageCheck(authorisedBulletText, authorisedBulletSelector)
          textOnPageCheck(investmentBulletText, investmentBulletSelector)
          textOnPageCheck(yourDividendsBulletText, yourDividendsBulletSelector)
          textOnPageCheck(youDoNotNeedAgentText, youDoNotNeedSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)
          textOnPageCheck(whatAreInvestmentText, whatAreInvestmentSelector)
          textOnPageCheck(investmentTrustText, investmentTrustSelector)
          textOnPageCheck(unitTrustsText, unitTrustsSelector)
          textOnPageCheck(openEndedText, openEndedSelector)
          textOnPageCheck(whatAreEqualisationText, whatAreEqualisationSelector)
          textOnPageCheck(equalisationPaymentsText, equalisationPaymentsSelector)
        }
      }
    }
  }
}
