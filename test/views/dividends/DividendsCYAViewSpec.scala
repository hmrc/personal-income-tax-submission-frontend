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
  val titleSelector = "h1"
  val captionSelector = ".govuk-caption-l"

  val titleExpected = "Check your answers - Register your income tax return with HMRC - Gov.UK"
  val h1Expected = "Check your answers"
  val captionExpected = "Dividends for 06 April 2019 to 05 April 2020"

  val changeLinkExpected = "Change"
  val yesNoExpectedAnswer: Boolean => String = isYes => if(isYes) "Yes" else "No"

  val question1TextExpected = "Dividends from UK companies?"
  val question2TextExpected = "Amount of dividends from UK companies"
  val question3TextExpected = "Dividends from unit trusts or investment companies?"
  val question4TextExpected = "Amount of dividends from unit trusts or investment companies"

  "DividendsCYAView" should {

    def dividendsCyaView: DividendsCYAView = app.injector.instanceOf[DividendsCYAView]

    "render with all fields showing" when {

      "all boolean answers are yes and amount answers are filled in" which {

        val cyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(
          ukDividends = Some(true),
          Some(5),
          otherUkDividends = Some(true),
          Some(10)
        )
        lazy val view = dividendsCyaView(cyaModel, taxYear = 2020)(user, implicitly, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        "contains the correct title" in {
          document.title() shouldBe titleExpected
        }

        "contains the correct heading" in {
          elementText(titleSelector) shouldBe h1Expected
        }

        "contains the correct caption" in {
          elementText(captionSelector) shouldBe captionExpected
        }

        "contains question 1" which {

          "has the correct question text" in {
            elementText(questionTextSelector(1)) shouldBe question1TextExpected
          }

          "has the correct answer" in {
            elementText(questionAnswerSelector(1)) shouldBe yesNoExpectedAnswer(true)
          }

          "contains the change link" in {
            elementText(questionChangeLinkSelector(1)) shouldBe changeLinkExpected
          }

        }

        "contains question 2" which {

          "has the correct question text" in {
            elementText(questionTextSelector(2)) shouldBe question2TextExpected
          }

          "has the correct answer" in {
            elementText(questionAnswerSelector(2)) shouldBe "£5"
          }

          "contains the change link" in {
            elementText(questionChangeLinkSelector(2)) shouldBe changeLinkExpected
          }

        }

        "contains question 3" which {

          "has the correct question text" in {
            elementText(questionTextSelector(3)) shouldBe question3TextExpected
          }

          "has the correct answer" in {
            elementText(questionAnswerSelector(3)) shouldBe yesNoExpectedAnswer(true)
          }

          "contains the change link" in {
            elementText(questionChangeLinkSelector(3)) shouldBe changeLinkExpected
          }

        }

        "contains question 4" which {

          "has the correct question text" in {
            elementText(questionTextSelector(4)) shouldBe question4TextExpected
          }

          "has the correct answer" in {
            elementText(questionAnswerSelector(4)) shouldBe "£10"
          }

          "contains the change link" in {
            elementText(questionChangeLinkSelector(4)) shouldBe changeLinkExpected
          }

        }

      }

    }

    "render with the yesNo fields hidden" when {

      "prior values are available" which {

        val cyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(
          ukDividends = Some(true),
          Some(10),
          otherUkDividends = Some(true),
          Some(20)
        )

        val priorSubmission: DividendsPriorSubmission = DividendsPriorSubmission(
          Some(10),
          Some(20)
        )

        lazy val view = dividendsCyaView(cyaModel, priorSubmission, 2020)(user, implicitly, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        "contains the correct title" in {
          document.title() shouldBe titleExpected
        }

        "contains the correct heading" in {
          elementText(titleSelector) shouldBe h1Expected
        }

        "contains the correct caption" in {
          elementText(captionSelector) shouldBe captionExpected
        }

        "contains question 1" which {

          "has the correct question text" in {
            elementText(questionTextSelector(1)) shouldBe question2TextExpected
          }

          "has the correct answer" in {
            elementText(questionAnswerSelector(1)) shouldBe "£10"
          }

          "contains the change link" in {
            elementText(questionChangeLinkSelector(1)) shouldBe changeLinkExpected
          }

        }

        "contains question 2" which {

          "has the correct question text" in {
            elementText(questionTextSelector(2)) shouldBe question4TextExpected
          }

          "has the correct answer" in {
            elementText(questionAnswerSelector(2)) shouldBe "£20"
          }

          "contains the change link" in {
            elementText(questionChangeLinkSelector(2)) shouldBe changeLinkExpected
          }

        }

      }

    }

    "render with the amount fields hidden" when {

      "all boolean answers are now and amount answers are not filled in" which {

        val cyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel()
        lazy val view = dividendsCyaView(cyaModel, taxYear = 2020)(user, implicitly, mockAppConfig)
        implicit lazy val document: Document = Jsoup.parse(view.body)

        "contains the correct title" in {
          document.title() shouldBe titleExpected
        }

        "contains the correct heading" in {
          elementText(titleSelector) shouldBe h1Expected
        }

        "contains the correct caption" in {
          elementText(captionSelector) shouldBe captionExpected
        }

        "contains question 1" which {

          "has the correct question text" in {
            elementText(questionTextSelector(1)) shouldBe question1TextExpected
          }

          "has the correct answer" in {
            elementText(questionAnswerSelector(1)) shouldBe yesNoExpectedAnswer(false)
          }

          "contains the change link" in {
            elementText(questionChangeLinkSelector(1)) shouldBe changeLinkExpected
          }

        }

        "contains question 3" which {

          "has the correct question text" in {
            elementText(questionTextSelector(2)) shouldBe question3TextExpected
          }

          "has the correct answer" in {
            elementText(questionAnswerSelector(2)) shouldBe yesNoExpectedAnswer(false)
          }

          "contains the change link" in {
            elementText(questionChangeLinkSelector(2)) shouldBe changeLinkExpected
          }

        }

      }

    }

  }

}
