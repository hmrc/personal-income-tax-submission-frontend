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

  lazy val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm("Select yes if dividends were received trusts or investment companies")

  lazy val receiveOtherDividendsView: ReceiveOtherUkDividendsView = app.injector.instanceOf[ReceiveOtherUkDividendsView]

  val captionSelector = ".govuk-caption-l"
  val thisIncludesAuthSelector = "#value-hint > p:nth-child(1)"
  val doNotIncludeSelector = "#value-hint > p:nth-child(2)"
  val yesSelector = "#main-content > div > div > form > div > fieldset > div.govuk-radios.govuk-radios--inline > div:nth-child(1) > label"
  val noSelector = "#main-content > div > div > form > div > fieldset > div.govuk-radios.govuk-radios--inline > div:nth-child(2) > label"
  val continueSelector = "#continue"
  val whatAreInvestmentSelector = "#main-content > div > div > details > summary > span"
  val investmentTrustSelector = "#main-content > div > div > details > div > p:nth-child(1)"
  val unitTrustsSelector = "#main-content > div > div > details > div > p:nth-child(2)"
  val equalisationPaymentsSelector = "#main-content > div > div > details > div > p:nth-child(3)"
  val continueButtonSelector = "#continue"

  val expectedIndividualH1 = "Did you receive any dividends from trusts or open ended investment companies?"
  val expectedIndividualTitle = "Did you receive any dividends from trusts or open ended investment companies?"
  val expectedIndividualErrorTitle = s"Error: $expectedIndividualTitle"
  val expectedAgentH1 = "Did your client receive any dividends from trusts or open ended investment companies?"
  val expectedAgentTitle = "Did your client receive any dividends from trusts or open ended investment companies?"
  val expectedAgentErrorTitle = s"Error: $expectedAgentTitle"
  val expectedCaption = "Dividends for 06 April 2019 to 05 April 2020"
  val thisIncludesAuthText: String = "This includes authorised unit trusts or investment funds. If your dividend is automatically " +
    "re-invested, you must still include it."
  val doNotIncludeText = "Do not include any amounts shown as 'equalisation' on your dividend voucher."
  val yesText = "Yes"
  val noText = "No"
  val continueText = "Continue"
  val whatAreInvestmentText = "What are investment trusts and companies?"
  val investmentTrustText:String = "Investment trusts are public companies that aim to make money through buying and selling shares " +
    "or assets in other companies. They let you join a group of other investors with a fund manager to get the best possible return for your money."
  val unitTrustsText:String = "Unit trusts and open ended investment companies are the most popular investment funds. Open ended investment " +
    "companies are like unit trusts except that they’re run like a company. They create and cancel shares rather than units when investors join or leave."
  val equalisationPaymentsText:String = "Equalisation payments are what an investor receives after making an investment part way" +
    " through a distribution period. They’re made up of the income generated before the investment and included in the price" +
    " paid for each unit. They are not considered income as it is a return of part of the investor’s capital."

  val taxYear = 2020

  "ReceivedDividendsView" should {

    "correctly render for an individual" when {

      "there are no form errors" which {

        lazy val view = receiveOtherDividendsView(
          yesNoForm, taxYear)(user, implicitly, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(expectedIndividualTitle)
        h1Check(expectedIndividualH1)
        textOnPageCheck(expectedCaption, captionSelector)
        textOnPageCheck(thisIncludesAuthText, thisIncludesAuthSelector)
        textOnPageCheck(doNotIncludeText, doNotIncludeSelector)
        //        TODO: Think of something for the radio buttons
        textOnPageCheck(yesText, yesSelector)
        //        TODO: Think of something for the radio buttons
        textOnPageCheck(noText, noSelector)
        buttonCheck(continueText, continueButtonSelector)
        textOnPageCheck(whatAreInvestmentText, whatAreInvestmentSelector)
        textOnPageCheck(investmentTrustText, investmentTrustSelector)
        textOnPageCheck(unitTrustsText, unitTrustsSelector)
        textOnPageCheck(equalisationPaymentsText, equalisationPaymentsSelector)
      }

      "there are form errors" when {

        "a form with an empty value field is passed in" which {

          lazy val view = receiveOtherDividendsView(
            yesNoForm.bind(Map("value" -> "")),
            taxYear
          )(user, implicitly, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          val expectedErrorText = "Select yes if dividends were received trusts or investment companies"

          titleCheck(expectedIndividualErrorTitle)
          h1Check(expectedIndividualH1)
          textOnPageCheck(expectedCaption, captionSelector)
          errorSummaryCheck(expectedErrorText, "#value")
          textOnPageCheck(thisIncludesAuthText, thisIncludesAuthSelector)
          textOnPageCheck(doNotIncludeText, doNotIncludeSelector)
          //        TODO: Think of something for the radio buttons
          textOnPageCheck(yesText, yesSelector)
          //        TODO: Think of something for the radio buttons
          textOnPageCheck(noText, noSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueText, continueButtonSelector)
          textOnPageCheck(whatAreInvestmentText, whatAreInvestmentSelector)
          textOnPageCheck(investmentTrustText, investmentTrustSelector)
          textOnPageCheck(unitTrustsText, unitTrustsSelector)
          textOnPageCheck(equalisationPaymentsText, equalisationPaymentsSelector)
        }
      }
    }

    "correctly render for an agent" when {

      "there are no form errors" which {

        lazy val view = receiveOtherDividendsView(
          yesNoForm, taxYear)(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(expectedAgentTitle)
        h1Check(expectedAgentH1)
        textOnPageCheck(expectedCaption, captionSelector)
        textOnPageCheck(thisIncludesAuthText, thisIncludesAuthSelector)
        textOnPageCheck(doNotIncludeText, doNotIncludeSelector)
        //        TODO: Think of something for the radio buttons
        textOnPageCheck(yesText, yesSelector)
        //        TODO: Think of something for the radio buttons
        textOnPageCheck(noText, noSelector)
        buttonCheck(continueText, continueButtonSelector)
        textOnPageCheck(whatAreInvestmentText, whatAreInvestmentSelector)
        textOnPageCheck(investmentTrustText, investmentTrustSelector)
        textOnPageCheck(unitTrustsText, unitTrustsSelector)
        textOnPageCheck(equalisationPaymentsText, equalisationPaymentsSelector)
      }

      "there are form errors" when {

        "a form with an empty value field is passed in" which {

          lazy val view = receiveOtherDividendsView(
            yesNoForm.bind(Map("value" -> "")),
            taxYear
          )(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)

          implicit lazy val document: Document = Jsoup.parse(view.body)

          val expectedErrorText = "Select yes if dividends were received trusts or investment companies"

          titleCheck(expectedAgentErrorTitle)
          h1Check(expectedAgentH1)
          textOnPageCheck(expectedCaption, captionSelector)
          errorSummaryCheck(expectedErrorText, "#value")
          textOnPageCheck(thisIncludesAuthText, thisIncludesAuthSelector)
          textOnPageCheck(doNotIncludeText, doNotIncludeSelector)
          //        TODO: Think of something for the radio buttons
          textOnPageCheck(yesText, yesSelector)
          //        TODO: Think of something for the radio buttons
          textOnPageCheck(noText, noSelector)
          errorAboveElementCheck(expectedErrorText)
          buttonCheck(continueText, continueButtonSelector)
          textOnPageCheck(whatAreInvestmentText, whatAreInvestmentSelector)
          textOnPageCheck(investmentTrustText, investmentTrustSelector)
          textOnPageCheck(unitTrustsText, unitTrustsSelector)
          textOnPageCheck(equalisationPaymentsText, equalisationPaymentsSelector)
        }
      }
    }
  }
}
