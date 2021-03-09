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

  val titleSelector = "h1"
  val captionSelector = ".govuk-caption-l"

  val h1Expected = "Check your answers"
  val titleExpected = s"$h1Expected - $serviceName - $govUkExtension"
  val captionExpected = "Dividends for 06 April 2019 to 05 April 2020"

  val changeLinkExpected = "Change"
  val yesNoExpectedAnswer: Boolean => String = isYes => if(isYes) "Yes" else "No"

  val question1TextExpected = "Dividends from UK companies?"
  val question2TextExpected = "Amount of dividends from UK companies"
  val question3TextExpected = "Dividends from unit trusts or investment companies?"
  val question4TextExpected = "Amount of dividends from unit trusts or investment companies"

  val changeUkDividendsIndividualHiddenText = "if you got dividends from UK-based companies."
  val changeUkDividendsAmountIndividualHiddenText = "how much you got from UK-based companies."
  val changeOtherDividendsIndividualHiddenText = "if you got dividends from trusts or investment companies based in the UK."
  val changeOtherDividendsAmountIndividualHiddenText = "how much you got in dividends from trusts or investment companies based in the UK."
  val changeUkDividendsAgentHiddenText = "if your client got dividends from UK-based companies."
  val changeUkDividendsAmountAgentHiddenText = "how much your client got from UK-based companies."
  val changeOtherDividendsAgentHiddenText = "if your client got dividends from trusts or investment companies based in the UK."
  val changeOtherDividendsAmountAgentHiddenText = "how much your client got in dividends from trusts or investment companies based in the UK."

  "DividendsCYAView" should {

    def dividendsCyaView: DividendsCYAView = app.injector.instanceOf[DividendsCYAView]
    
    "Render correctly for an individual" when {

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
              elementText(questionChangeLinkSelector(1)) shouldBe s"$changeLinkExpected $changeUkDividendsIndividualHiddenText"
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
              elementText(questionChangeLinkSelector(2)) shouldBe s"$changeLinkExpected $changeUkDividendsAmountIndividualHiddenText"
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
              elementText(questionChangeLinkSelector(3)) shouldBe s"$changeLinkExpected $changeOtherDividendsIndividualHiddenText"
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
              elementText(questionChangeLinkSelector(4)) shouldBe s"$changeLinkExpected $changeOtherDividendsAmountIndividualHiddenText"
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
              elementText(questionChangeLinkSelector(1)) shouldBe s"$changeLinkExpected $changeUkDividendsAmountIndividualHiddenText"
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
              elementText(questionChangeLinkSelector(2)) shouldBe s"$changeLinkExpected $changeOtherDividendsAmountIndividualHiddenText"
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
              elementText(questionChangeLinkSelector(1)) shouldBe s"$changeLinkExpected $changeUkDividendsIndividualHiddenText"
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
              elementText(questionChangeLinkSelector(2)) shouldBe s"$changeLinkExpected $changeOtherDividendsIndividualHiddenText"
            }

          }

        }

      }
    }

    "Render correctly for an agent" when {

      "render with all fields showing" when {

        "all boolean answers are yes and amount answers are filled in" which {

          val cyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(
            ukDividends = Some(true),
            Some(5),
            otherUkDividends = Some(true),
            Some(10)
          )
          lazy val view = dividendsCyaView(cyaModel, taxYear = 2020)(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)
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
              elementText(questionChangeLinkSelector(1)) shouldBe s"$changeLinkExpected $changeUkDividendsAgentHiddenText"
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
              elementText(questionChangeLinkSelector(2)) shouldBe s"$changeLinkExpected $changeUkDividendsAmountAgentHiddenText"
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
              elementText(questionChangeLinkSelector(3)) shouldBe s"$changeLinkExpected $changeOtherDividendsAgentHiddenText"
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
              elementText(questionChangeLinkSelector(4)) shouldBe s"$changeLinkExpected $changeOtherDividendsAmountAgentHiddenText"
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

          lazy val view = dividendsCyaView(cyaModel, priorSubmission, 2020)(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)
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
              elementText(questionChangeLinkSelector(1)) shouldBe s"$changeLinkExpected $changeUkDividendsAmountAgentHiddenText"
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
              elementText(questionChangeLinkSelector(2)) shouldBe s"$changeLinkExpected $changeOtherDividendsAmountAgentHiddenText"
            }

          }

        }

      }

      "render with the amount fields hidden" when {

        "all boolean answers are now and amount answers are not filled in" which {

          val cyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel()
          lazy val view = dividendsCyaView(cyaModel, taxYear = 2020)(user.copy(arn = Some("XARN1234567")), implicitly, mockAppConfig)
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
              elementText(questionChangeLinkSelector(1)) shouldBe s"$changeLinkExpected $changeUkDividendsAgentHiddenText"
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
              elementText(questionChangeLinkSelector(2)) shouldBe s"$changeLinkExpected $changeOtherDividendsAgentHiddenText"
            }

          }

        }

      }
    }

  }

}
