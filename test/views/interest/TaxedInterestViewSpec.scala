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
import views.html.interest.TaxedInterestView

class TaxedInterestViewSpec extends ViewTest {

  lazy val yesNoFormIndividual: Form[Boolean] = YesNoForm.yesNoForm("Select yes if you received taxed interest from the UK")
  lazy val yesNoFormAgent: Form[Boolean] = YesNoForm.yesNoForm("Select yes if your client received taxed interest from the UK")

  lazy val taxedInterestView: TaxedInterestView = app.injector.instanceOf[TaxedInterestView]
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

  val expectedIndividualTitle = "Did you receive any taxed interest from the UK?"
  val expectedIndividualErrorTitle = s"Error: $expectedIndividualTitle"
  val expectedIndividualH1 = "Did you receive any taxed interest from the UK?"
  val expectedAgentTitle = "Did your client receive any taxed interest from the UK?"
  val expectedAgentErrorTitle = s"Error: $expectedAgentTitle"
  val expectedAgentH1 = "Did your client receive any taxed interest from the UK?"
  val expectedCaption = "Interest for 6 April 2019 to 5 April 2020"
  val forExampleText = "For example, interest from:"
  val trustFundsText = "trust funds"
  val companyBondsText = "company bonds"
  val lifeAnnuityText = "life annuity payments"
  val doNotIncludeText = "Do not include interest received from gilts. You’ll be able to add interest earned from gilts at a later date."
  val yesText = "Yes"
  val noText = "No"
  val continueText = "Continue"

  "Taxed interest view" should {

    "Correctly render as an individual" when {
      "There are no form errors " which {
        lazy val view = taxedInterestView(
          yesNoFormIndividual, taxYear)(user, implicitly, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(expectedIndividualTitle)
        h1Check(expectedIndividualH1)
        textOnPageCheck(expectedCaption, captionSelector)

        s"have text on the screen of '$forExampleText'" in {
          document.select(forExampleSelector).get(0).text() shouldBe forExampleText
        }

        s"three bullet points on the screen" which {
          s"has a first bullet point of '$trustFundsText'" in {
            document.select(bulletPointSelector).get(0).text() shouldBe trustFundsText
          }

          s"has a second bullet point of '$companyBondsText'" in {
            document.select(bulletPointSelector).get(1).text() shouldBe companyBondsText
          }

          s"has a third bullet point of '$lifeAnnuityText'" in {
            document.select(bulletPointSelector).get(2).text() shouldBe lifeAnnuityText
          }
        }

        s"have text on the screen of '$doNotIncludeText'" in {
          document.select(doNotIncludeSelector).text() should include (doNotIncludeText)
        }

//        Think about something for a radio button
        textOnPageCheck(yesText, yesSelector)
//        Think about something for a radio button
        textOnPageCheck(noText, noSelector)

        buttonCheck(continueText, continueSelector)
      }
    }
    "correctly render with errors as an individual" when {
      "no value is passed in" which {
        lazy val view = taxedInterestView(
          yesNoFormIndividual.bind(Map("value" -> "")),
          taxYear
        )(user, implicitly, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedErrorText = "Select yes if you received taxed interest from the UK"

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

        lazy val view = taxedInterestView(
          yesNoFormAgent, taxYear)(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(expectedAgentTitle)
        h1Check(expectedAgentH1)
        textOnPageCheck(expectedCaption, captionSelector)

        s"have text on the screen of '$forExampleText'" in {
          document.select(forExampleSelector).get(0).text() shouldBe forExampleText
        }

        s"three bullet points on the screen" which {
          s"has a first bullet point of '$trustFundsText'" in {
            document.select(bulletPointSelector).get(0).text() shouldBe trustFundsText
          }

          s"has a second bullet point of '$companyBondsText'" in {
            document.select(bulletPointSelector).get(1).text() shouldBe companyBondsText
          }

          s"has a third bullet point of '$lifeAnnuityText'" in {
            document.select(bulletPointSelector).get(2).text() shouldBe lifeAnnuityText
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
        lazy val view = taxedInterestView(
          yesNoFormAgent.bind(Map("value" -> "")),
          taxYear
        )(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)

        implicit lazy val document: Document = Jsoup.parse(view.body)

        val expectedErrorText = "Select yes if your client received taxed interest from the UK"

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
