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

import forms.UntaxedInterestAmountForm
import forms.UntaxedInterestAmountForm._
import models.UntaxedInterestModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.data.{Form, FormError}
import utils.ViewTest
import views.html.interest.UntaxedInterestAmountView



class UntaxedInterestAmountViewSpec extends ViewTest {

  lazy val untaxedInterestForm: Form[UntaxedInterestModel] = UntaxedInterestAmountForm.untaxedInterestAmountForm()
  lazy val untaxedInterestView: UntaxedInterestAmountView = app.injector.instanceOf[UntaxedInterestAmountView]

  val h1Selector = "h1"
  val captionSelector = ".govuk-caption-l"
  val continueButtonSelector = "#continue"
  val errorSummarySelector = ".govuk-error-summary"
  val errorSummaryTitleSelector = ".govuk-error-summary__title"
  val errorSummaryTextSelector = ".govuk-error-summary__body"

  val expectedH1 = "UK untaxed interest account details"
  val expectedTitle = "UK untaxed interest account details"
  val expectedErrorTitle= s"Error: $expectedTitle"
  val expectedCaption = "Interest for 06 April 2019 to 05 April 2020"
  val expectedHintText = "For example, £600 or £193.54"

  val expectedErrorSummaryTitle = "There is a problem"
  val expectedErrorSummaryText = "Enter the amount of untaxed interest earned"

  val taxYear = 2020
  val id = "id"

  "UntaxedInterestAmountView" should {

    "Correctly render" when {
      "There are no form errors" which {

        lazy val view = untaxedInterestView(
          untaxedInterestForm,
          taxYear,
          controllers.interest.routes.UntaxedInterestAmountController.submit(taxYear,id)
        )(user,implicitly,mockAppConfig)

        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(expectedH1)
        captionCheck(expectedCaption)
        hintTextCheck(expectedHintText)
        h1Check(expectedH1, h1Selector)
      }

      "There are form errors" which {

        lazy val view = untaxedInterestView(
          untaxedInterestForm.copy(errors = Seq(FormError(untaxedAmount,
          "interest.untaxed-uk-interest-amount.error.empty"))),
          taxYear,
          controllers.interest.routes.UntaxedInterestAmountController.submit(taxYear,id)
        )(user, implicitly, mockAppConfig)

        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(expectedH1, error = true)
        captionCheck(expectedCaption)
        hintTextCheck(expectedHintText)
        buttonCheck("Continue", continueButtonSelector, "")

        "contains a continue button" in {
          elementExist(continueButtonSelector) shouldBe true
        }

        "contains an error" in {
          elementExist(errorSummarySelector) shouldBe true
        }

        "contain an error title" in {
          elementText(errorSummaryTitleSelector) shouldBe expectedErrorSummaryTitle
        }

        "contains an error message" in {
          elementText(errorSummaryTextSelector) shouldBe expectedErrorSummaryText
        }

      }
    }
  }

}
