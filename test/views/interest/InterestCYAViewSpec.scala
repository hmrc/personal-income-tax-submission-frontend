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

import common.InterestTaxTypes
import common.InterestTaxTypes._
import models.interest.{InterestAccountModel, InterestCYAModel, InterestPriorSubmission}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import utils.ViewTest
import views.html.interest.InterestCYAView

class InterestCYAViewSpec extends ViewTest {

  lazy val view: InterestCYAView = app.injector.instanceOf[InterestCYAView]

  val taxYear = 2020

  val question2 = 2
  val question4 = 4

  val account1 = 1
  val account2 = 2

  object Selectors {
    val titleSelector = "title"
    val h1Selector = "h1"
    val captionSelector = ".govuk-caption-l"
    val submitButton = ".govuk-button"

    val questionSelector: Int => String = questionNumber => s".govuk-summary-list__row:nth-child($questionNumber) > .govuk-summary-list__key"

    val questionAccountSelector: (Int, Int) => String = (questionNumber, accountNumber) => s"#question${questionNumber}account:nth-child($accountNumber)"

    val questionChangeLinkSelector: Int => String = questionNumber => s"#main-content > div > div > dl > div:nth-child($questionNumber) " +
      s"> dd.govuk-summary-list__actions > a"

    val yesNoQuestionAnswer: Int => String = questionNumber => s"#main-content > div > div > dl > div:nth-child($questionNumber) > dd.govuk-summary-list__value"
  }

  object ExpectedResult {
    val titleExpected = "Check your answers - Register your income tax return with HMRC - Gov.UK"
    val h1Expected = "Check your answers"
    val captionExpected = "Interest for 06 April 2019 to 05 April 2020"

    val changeLinkExpected = "Change"

    val questionUntaxedInterestExpected = "Untaxed UK Interest?"
    val questionUntaxedInterestDetailsExpected = "Details for the untaxed UK interest?"
    val questionTaxedInterestExpected = "Taxed UK Interest?"
    val question4TaxedInterestDetailExpected = "Details for the taxed UK interest?"

    val untaxedInterestAccount1ExpectedTest = "UntaxedBank1 : £100.00"
    val taxedInterestAccount1ExpectedTest = "TaxedBank1 : £200.00"
    val taxedInterestAccount2ExpectedTest = "TaxedBank2 : £400.00"

    val submitText = "Save and continue"

    val Yes = "Yes"
    val No = "No"
  }

  "InterestCYAView" should {

    "render with all fields" when {

      "all fields are present" which {
        val cyaModel = InterestCYAModel(
          untaxedUkInterest = Some(true),
          untaxedUkAccounts = Some(Seq(InterestAccountModel(Some("id"), "UntaxedBank1", 100.00))),
          taxedUkInterest = Some(true),
          taxedUkAccounts = Some(Seq(
            InterestAccountModel(Some("id"), "TaxedBank1", 200.00),
            InterestAccountModel(Some("id"), "TaxedBank2", 400.00)
          ))
        )

        val render = view(cyaModel, taxYear)(fakeRequest, messages, mockAppConfig).body
        implicit val document: Document = Jsoup.parse(render)

        "has the correct title" in {
          assertTitle(ExpectedResult.titleExpected)
        }

        "has the correct h1" in {
          assertH1(ExpectedResult.h1Expected)
        }

        "has the correct caption" in {
          elementText(Selectors.captionSelector) shouldBe ExpectedResult.captionExpected
        }

        "the submit button" which {

          "exist" in {
            elementExist(Selectors.submitButton) shouldBe true
          }

          "has the correct text" in {
            elementText(Selectors.submitButton) shouldBe ExpectedResult.submitText
          }

        }

        "has the correct question 1 text" in {
          elementText(Selectors.questionSelector(1)) shouldBe ExpectedResult.questionUntaxedInterestExpected
        }

        "has the correct question 2 text" in {
          elementText(Selectors.questionSelector(2)) shouldBe ExpectedResult.questionUntaxedInterestDetailsExpected
        }

        "has the correct question 3 text" in {
          elementText(Selectors.questionSelector(3)) shouldBe ExpectedResult.questionTaxedInterestExpected
        }

        "has the correct question 4 text" in {
          //noinspection ScalaStyle
          elementText(Selectors.questionSelector(4)) shouldBe ExpectedResult.question4TaxedInterestDetailExpected
        }

        "question 1 answer should be Yes" in {
          elementText(Selectors.yesNoQuestionAnswer(1)) shouldBe ExpectedResult.Yes
        }

        "question 3 answer should be Yes" in {
          elementText(Selectors.yesNoQuestionAnswer(3)) shouldBe ExpectedResult.Yes
        }

        "has the correct question 2 account text" in {
          elementText(Selectors.questionAccountSelector(question2, account1)) shouldBe ExpectedResult.untaxedInterestAccount1ExpectedTest
        }

        "has the correct question 4 account 1 text" in {
          elementText(Selectors.questionAccountSelector(question4, account1)) shouldBe ExpectedResult.taxedInterestAccount1ExpectedTest
        }

        "has the correct question 4 account 2 text" in {
          elementText(Selectors.questionAccountSelector(question4, account2)) shouldBe ExpectedResult.taxedInterestAccount2ExpectedTest
        }

        "question 1 change link" should {

          "have the correct text" in {
            elementText(Selectors.questionChangeLinkSelector(1)) shouldBe ExpectedResult.changeLinkExpected
          }

          "have the correct link" in {
            element(Selectors.questionChangeLinkSelector(1)).attr("href") shouldBe controllers.interest.routes.UntaxedInterestController.show(taxYear).url
          }

        }

        "question 2 change link" should {

          "have the correct text" in {
            elementText(Selectors.questionChangeLinkSelector(2)) shouldBe ExpectedResult.changeLinkExpected
          }

          "have the correct link" in {
            element(Selectors.questionChangeLinkSelector(2)).attr("href") shouldBe controllers.interest.routes.AccountsController.show(taxYear, UNTAXED).url
          }

        }

        "question 3 change link" should {

          "have the correct text" in {
            elementText(Selectors.questionChangeLinkSelector(3)) shouldBe ExpectedResult.changeLinkExpected
          }

          "have the correct link" in {
            element(Selectors.questionChangeLinkSelector(3)).attr("href") shouldBe controllers.interest.routes.TaxedInterestController.show(taxYear).url
          }

        }

        "question 4 change link" should {

          "have the correct text" in {
            //noinspection ScalaStyle
            elementText(Selectors.questionChangeLinkSelector(4)) shouldBe ExpectedResult.changeLinkExpected
          }

          "have the correct link" in {
            //noinspection ScalaStyle
            element(Selectors.questionChangeLinkSelector(4)).attr("href") shouldBe controllers.interest.routes.AccountsController.show(taxYear, TAXED).url
          }

        }

      }

    }

    "renders only the yes/no questions" when {

      "the user has selected no to receiving taxed and untaxed interest" which {
        val cyaModel = InterestCYAModel(
          untaxedUkInterest = Some(false),
          untaxedUkAccounts = None,
          taxedUkInterest = Some(false),
          taxedUkAccounts = None
        )

        val render = view(cyaModel, taxYear)(fakeRequest, messages, mockAppConfig).body
        implicit val document: Document = Jsoup.parse(render)

        "has the correct title" in {
          assertTitle(ExpectedResult.titleExpected)
        }

        "has the correct h1" in {
          assertH1(ExpectedResult.h1Expected)
        }

        "has the correct caption" in {
          elementText(Selectors.captionSelector) shouldBe ExpectedResult.captionExpected
        }

        "the submit button" which {

          "exist" in {
            elementExist(Selectors.submitButton) shouldBe true
          }

          "has the correct text" in {
            elementText(Selectors.submitButton) shouldBe ExpectedResult.submitText
          }

        }

        "question 1 should be the untaxed interest question" in {
          elementText(questionTextSelector(1)) shouldBe ExpectedResult.questionUntaxedInterestExpected
        }

        "question 2 should be the taxed interest question" in {
          elementText(questionTextSelector(2)) shouldBe ExpectedResult.questionTaxedInterestExpected
        }

        "question 1 answer should be No" in {
          elementText(Selectors.yesNoQuestionAnswer(1)) shouldBe ExpectedResult.No
        }

        "question 2 answer should be No" in {
          elementText(Selectors.yesNoQuestionAnswer(2)) shouldBe ExpectedResult.No
        }

        "there is no question 3" in {
          elementExist(Selectors.questionSelector(3)) shouldBe false
        }

        "there is no question 4" in {
          //noinspection ScalaStyle
          elementExist(Selectors.questionSelector(4)) shouldBe false
        }

        "question 1 change link" should {

          "have the correct text" in {
            elementText(Selectors.questionChangeLinkSelector(1)) shouldBe ExpectedResult.changeLinkExpected
          }

          "have the correct link" in {
            element(Selectors.questionChangeLinkSelector(1)).attr("href") shouldBe controllers.interest.routes.UntaxedInterestController.show(taxYear).url
          }

        }

        "question 2 change link" should {

          "have the correct text" in {
            elementText(Selectors.questionChangeLinkSelector(2)) shouldBe ExpectedResult.changeLinkExpected
          }

          "have the correct link" in {
            element(Selectors.questionChangeLinkSelector(2)).attr("href") shouldBe controllers.interest.routes.TaxedInterestController.show(taxYear).url
          }

        }

      }

      "the user has both tax types prior" which {
        val priorSubmission = InterestPriorSubmission(
          hasUntaxed = true, hasTaxed = true,
          Some(Seq(
            InterestAccountModel(Some("qwerty"), "TSB", 100.00, priorType = Some(InterestTaxTypes.UNTAXED)),
            InterestAccountModel(Some("azerty"), "TSB", 100.00, priorType = Some(InterestTaxTypes.TAXED))
          ))
        )

        val cyaModel = InterestCYAModel(
          untaxedUkInterest = Some(true),
          untaxedUkAccounts = Some(Seq(InterestAccountModel(Some("qwerty"), "TSB", 100.00))),
          taxedUkInterest = Some(true),
          taxedUkAccounts = Some(Seq(InterestAccountModel(Some("azerty"), "TSB Account", 100.00)))
        )

        val render = view(cyaModel, taxYear, Some(priorSubmission))(fakeRequest, messages, mockAppConfig).body
        implicit val document: Document = Jsoup.parse(render)

        "has the correct title" in {
          assertTitle(ExpectedResult.titleExpected)
        }

        "has the correct h1" in {
          assertH1(ExpectedResult.h1Expected)
        }

        "has the correct caption" in {
          elementText(Selectors.captionSelector) shouldBe ExpectedResult.captionExpected
        }

        "the submit button" which {

          "exist" in {
            elementExist(Selectors.submitButton) shouldBe true
          }

          "has the correct text" in {
            elementText(Selectors.submitButton) shouldBe ExpectedResult.submitText
          }

        }

        "question 1 should be the untaxed interest accounts question" in {
          elementText(questionTextSelector(1)) shouldBe ExpectedResult.questionUntaxedInterestDetailsExpected
        }

        "question 2 should be the taxed interest question" in {
          elementText(questionTextSelector(2)) shouldBe ExpectedResult.question4TaxedInterestDetailExpected
        }

        "question 1 answer should be No" in {
          elementText(Selectors.yesNoQuestionAnswer(1)) shouldBe "TSB : £100.00"
        }

        "question 2 answer should be No" in {
          elementText(Selectors.yesNoQuestionAnswer(2)) shouldBe "TSB Account : £100.00"
        }

        "there is no question 3" in {
          elementExist(Selectors.questionSelector(3)) shouldBe false
        }

        "there is no question 4" in {
          //noinspection ScalaStyle
          elementExist(Selectors.questionSelector(4)) shouldBe false
        }

        "question 1 change link" should {

          "have the correct text" in {
            elementText(Selectors.questionChangeLinkSelector(1)) shouldBe ExpectedResult.changeLinkExpected
          }

          "have the correct link" in {
            element(Selectors.questionChangeLinkSelector(1)).attr("href") shouldBe
              controllers.interest.routes.AccountsController.show(taxYear, InterestTaxTypes.UNTAXED).url
          }

        }

        "question 2 change link" should {

          "have the correct text" in {
            elementText(Selectors.questionChangeLinkSelector(2)) shouldBe ExpectedResult.changeLinkExpected
          }

          "have the correct link" in {
            element(Selectors.questionChangeLinkSelector(2)).attr("href") shouldBe
              controllers.interest.routes.AccountsController.show(taxYear, InterestTaxTypes.TAXED).url
          }

        }

      }

    }

  }

}
