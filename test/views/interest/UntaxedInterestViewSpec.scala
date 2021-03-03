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
import play.api.data.{Form, FormError}
import utils.ViewTest
import views.html.interest.UntaxedInterestView

class UntaxedInterestViewSpec extends ViewTest {

  lazy val yesNoForm: Form[Boolean] = YesNoForm.yesNoForm("Select yes if you received untaxed interest from the UK")

  lazy val untaxedInterestView: UntaxedInterestView = app.injector.instanceOf[UntaxedInterestView]
  val taxYear = 2020

  val captionSelector = ".govuk-caption-l"
  val yesOptionSelector = "#value"
  val noOptionSelector = "#value-no"
  val forExampleSelector = "#value-hint > p"
  val bulletPointSelector = "#value-hint > ul > li"
  val doNotIncludeSelector = "#value-hint"
  val yesSelector = "#main-content > div > div > form > div > fieldset > div.govuk-radios.govuk-radios--inline > div:nth-child(1) > label"
  val noSelector = "#main-content > div > div > form > div > fieldset > div.govuk-radios.govuk-radios--inline > div:nth-child(2) > label"
  val continueSelector = "#continue"

  val expectedIndividualTitle = "Did you receive any untaxed interest from the UK?"
  val expectedIndividualErrorTitle = s"Error: $expectedIndividualTitle"
  val expectedIndividualH1 = "Did you receive any untaxed interest from the UK?"
  val expectedAgentTitle = "Did your client receive any untaxed interest from the UK?"
  val expectedAgentErrorTitle = s"Error: $expectedAgentTitle"
  val expectedAgentH1 = "Did your client receive any untaxed interest from the UK?"
  val expectedCaption = "Interest for 06 April 2019 to 05 April 2020"
  val forExampleText = "For example, interest from:"
  val banksAndBuildingsText = "banks and building societies"
  val savingsAndCreditText = "savings and credit union accounts"
  val peerToPeerText = "peer-to-peer lending"
  val doNotIncludeText: String = "This does not include any interest earned from an Individual Savings Account (ISA) or gilts. " +
    "You’ll be able to add interest earned from gilts at a later date."
  val yesText = "Yes"
  val noText = "No"
  val continueText = "Continue"

  "Untaxed interest view" should {

    "Correctly render as an individual" when {
      "There are no form errors " which {
        lazy val view = untaxedInterestView(
          yesNoForm, taxYear)(user, implicitly, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(expectedIndividualTitle)
        h1Check(expectedIndividualH1)
        textOnPageCheck(expectedCaption, captionSelector)

        s"have text on the screen of '$forExampleText'" in {
          document.select(forExampleSelector).get(0).text() shouldBe forExampleText
        }

        s"three bullet points on the screen" which {
          s"has a first bullet point of '$banksAndBuildingsText'" in {
            document.select(bulletPointSelector).get(0).text() shouldBe banksAndBuildingsText
          }

          s"has a second bullet point of '$savingsAndCreditText'" in {
            document.select(bulletPointSelector).get(1).text() shouldBe savingsAndCreditText
          }

          s"has a third bullet point of '$peerToPeerText'" in {
            document.select(bulletPointSelector).get(2).text() shouldBe peerToPeerText
          }
        }

        s"have text on the screen of '$doNotIncludeText'" in {
          document.select(doNotIncludeSelector).text() should include (doNotIncludeText)
        }

//      TODO: Think about something for a radio button
        textOnPageCheck(yesText, yesSelector)
//      TODO: Think about something for a radio button
        textOnPageCheck(noText, noSelector)

        buttonCheck(continueText, continueSelector)
      }
    }
    "correctly render with errors as an individual" when {
      "no value is passed in" which {
        lazy val view = untaxedInterestView(
          yesNoForm.bind(Map("value" -> "")),
          taxYear
        )(user, implicitly, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedErrorText = "Select yes if you received untaxed interest from the UK"

        titleCheck(expectedIndividualErrorTitle)
        h1Check(expectedIndividualH1)
        textOnPageCheck(expectedCaption, captionSelector)
        errorSummaryCheck(expectedErrorText, "#value")
        errorAboveElementCheck(expectedErrorText)
        buttonCheck(continueText, continueSelector)
      }
    }
    "correctly render with no errors as an agent" when {
      "there are no form errors" which {

        lazy val view = untaxedInterestView(
          yesNoForm, taxYear)(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(expectedAgentTitle)
        h1Check(expectedAgentH1)
        textOnPageCheck(expectedCaption, captionSelector)

        s"have text on the screen of '$forExampleText'" in {
          document.select(forExampleSelector).get(0).text() shouldBe forExampleText
        }

        s"three bullet points on the screen" which {
          s"has a first bullet point of '$banksAndBuildingsText'" in {
            document.select(bulletPointSelector).get(0).text() shouldBe banksAndBuildingsText
          }

          s"has a second bullet point of '$savingsAndCreditText'" in {
            document.select(bulletPointSelector).get(1).text() shouldBe savingsAndCreditText
          }

          s"has a third bullet point of '$peerToPeerText'" in {
            document.select(bulletPointSelector).get(2).text() shouldBe peerToPeerText
          }
        }

        s"have text on the screen of '$doNotIncludeText'" in {
          document.select(doNotIncludeSelector).text() should include (doNotIncludeText)
        }

//        TODO: Think about something for a radio button
        textOnPageCheck(yesText, yesSelector)
//        TODO: Think about something for a radio button
        textOnPageCheck(noText, noSelector)

        buttonCheck(continueText, continueSelector)
      }
    }
    "correctly render with errors as an agent" when {
      "there is a form error" which {
        lazy val view = untaxedInterestView(
          yesNoForm.bind(Map("value" -> "")),
          taxYear
        )(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)

        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedErrorText = "Select yes if you received untaxed interest from the UK"

        titleCheck(expectedAgentErrorTitle)
        h1Check(expectedAgentH1)
        textOnPageCheck(expectedCaption, captionSelector)
        errorSummaryCheck(expectedErrorText, "#value")
        errorAboveElementCheck(expectedErrorText)
        buttonCheck(continueText, continueSelector)
      }
    }
  }
}
