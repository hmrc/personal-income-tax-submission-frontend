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

import models.{DividendsCheckYourAnswersModel, DividendsPriorSubmission}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import utils.ViewTest
import views.html.dividends.DividendsCYAView

class DividendsCYAViewSpec extends ViewTest {

  type IntString = Int => String

  val questionTextSelector: IntString = question => s"#main-content > div > div > dl > div:nth-child($question) > dt"
  val questionAnswerSelector: IntString = question => s"#main-content > div > div > dl > div:nth-child($question) > " +
    s"dd.govuk-summary-list__value"
  val questionChangeLinkSelector: IntString = question => s"#main-content > div > div > dl > div:nth-child($question) > " +
    s"dd.govuk-summary-list__actions > a"
  val continueButtonSelector = "#continue"

  val question4 = 4
  val fivePoundAmount = 5
  val tenPoundAmount = 10
  val taxYear = 2020
  val taxYearMinusOne = taxYear - 1

  val captionSelector = ".govuk-caption-l"

  val h1Expected = "Check your answers"
  val titleExpected = "Check your answers"
  val captionExpected = s"Dividends for 06 April $taxYearMinusOne to 05 April $taxYear"
  val continueButtonText = "Save and continue"

  val changeLinkExpected = "Change"
  val yesNoExpectedAnswer: Boolean => String = isYes => if(isYes) "Yes" else "No"

  val ukDividendsHeader = "Dividends from UK companies?"
  val ukDividendsAmount = "Amount of dividends from UK companies"
  val otherDividendsHeader = "Dividends from unit trusts or investment companies?"
  val otherDividendsAmount = "Amount of dividends from unit trusts or investment companies"

  val changeUkDividendsHref = "/income-through-software/return/personal-income/2020/dividends/uk-dividends"
  val changeUkDividendsAmountHref = "/income-through-software/return/personal-income/2020/dividends/uk-dividends-amount"
  val changeOtherDividendsHref = "/income-through-software/return/personal-income/2020/dividends/other-dividends"
  val changeOtherDividendsAmountHref = "/income-through-software/return/personal-income/2020/dividends/other-dividends-amount"

  "DividendsCYAView" should {

    def dividendsCyaView: DividendsCYAView = app.injector.instanceOf[DividendsCYAView]

    "render with all fields showing" when {

      "all boolean answers are yes and amount answers are filled in" which {

        val cyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(
          ukDividends = Some(true),
          Some(fivePoundAmount),
          otherUkDividends = Some(true),
          Some(tenPoundAmount)
        )
        lazy val view = dividendsCyaView(cyaModel, taxYear = taxYear)(user, implicitly, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(titleExpected)
        h1Check(h1Expected)
        textOnPageCheck(captionExpected, captionSelector)

        "has an area for question 1" which {
          textOnPageCheck(ukDividendsHeader, questionTextSelector(1))
          textOnPageCheck(yesNoExpectedAnswer(true), questionAnswerSelector(1))
          linkCheck(changeLinkExpected, questionChangeLinkSelector(1), changeUkDividendsHref)
        }

        "has an area for question 2" which {
          textOnPageCheck(ukDividendsAmount, questionTextSelector(2))
          textOnPageCheck(s"£$fivePoundAmount", questionAnswerSelector(2))
          linkCheck(changeLinkExpected, questionChangeLinkSelector(2), changeUkDividendsAmountHref)
        }

        "has an area for question 3" which {
          textOnPageCheck(otherDividendsHeader, questionTextSelector(3))
          textOnPageCheck(yesNoExpectedAnswer(true), questionAnswerSelector(3))
          linkCheck(changeLinkExpected, questionChangeLinkSelector(3), changeOtherDividendsHref)
        }

        "has an area for question 4" which {
          textOnPageCheck(otherDividendsAmount, questionTextSelector(question4))
          textOnPageCheck(s"£$tenPoundAmount", questionAnswerSelector(question4))
          linkCheck(changeLinkExpected, questionChangeLinkSelector(question4),changeOtherDividendsAmountHref)
        }

        s"have a $continueButtonText button" which {
          s"has the text '$continueButtonText'" in {
            document.select(continueButtonSelector).text() shouldBe continueButtonText
          }
          s"has a class of govuk-button" in {
            document.select(continueButtonSelector).attr("class") should include ("govuk-button")
          }
        }
      }
    }

    "render with the yesNo fields hidden" when {

      "prior values are available" which {

        val cyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(
          ukDividends = Some(true),
          Some(fivePoundAmount),
          otherUkDividends = Some(true),
          Some(tenPoundAmount)
        )

        val priorSubmission: DividendsPriorSubmission = DividendsPriorSubmission(
          Some(tenPoundAmount),
          Some(fivePoundAmount)
        )

        lazy val view = dividendsCyaView(cyaModel, priorSubmission, taxYear)(user, implicitly, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(titleExpected)
        h1Check(h1Expected)
        textOnPageCheck(captionExpected, captionSelector)

        "has an area for question 1" which {
          textOnPageCheck(ukDividendsAmount, questionTextSelector(1))
          textOnPageCheck(s"£$fivePoundAmount", questionAnswerSelector(1))
          linkCheck(changeLinkExpected, questionChangeLinkSelector(1), changeUkDividendsAmountHref)
        }

        "has an area for question 2" which {
          textOnPageCheck(otherDividendsAmount, questionTextSelector(2))
          textOnPageCheck(s"£$tenPoundAmount", questionAnswerSelector(2))
          linkCheck(changeLinkExpected, questionChangeLinkSelector(2), changeOtherDividendsAmountHref)
        }

        s"have a $continueButtonText button" which {
          s"has the text '$continueButtonText'" in {
            document.select(continueButtonSelector).text() shouldBe continueButtonText
          }
          s"has a class of govuk-button" in {
            document.select(continueButtonSelector).attr("class") should include ("govuk-button")
          }
        }
      }
    }

    "render with the amount fields hidden" when {

      "all boolean answers are no and the amount answers are not filled in" which {

        val cyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel()
        lazy val view = dividendsCyaView(cyaModel, taxYear = taxYear)(user, implicitly, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        titleCheck(titleExpected)
        h1Check(h1Expected)
        textOnPageCheck(captionExpected, captionSelector)

        "has an area for question 1" which {
          textOnPageCheck(ukDividendsHeader, questionTextSelector(1))
          textOnPageCheck(yesNoExpectedAnswer(false), questionAnswerSelector(1))
          linkCheck(changeLinkExpected, questionChangeLinkSelector(1), changeUkDividendsHref)
        }

        "has an area for question 2" which {
          textOnPageCheck(otherDividendsHeader, questionTextSelector(2))
          textOnPageCheck(yesNoExpectedAnswer(false), questionAnswerSelector(2))
          linkCheck(changeLinkExpected, questionChangeLinkSelector(2), changeOtherDividendsHref)
        }

        s"have a $continueButtonText button" which {
          s"has the text '$continueButtonText'" in {
            document.select(continueButtonSelector).text() shouldBe continueButtonText
          }
          s"has a class of govuk-button" in {
            document.select(continueButtonSelector).attr("class") should include ("govuk-button")
          }
        }
      }
    }
  }
}
