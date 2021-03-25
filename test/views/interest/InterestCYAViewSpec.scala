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
import models.interest.{InterestAccountModel, InterestCYAModel, InterestPriorSubmission}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import utils.ViewTest
import views.html.interest.InterestCYAView

class InterestCYAViewSpec extends ViewTest {

  lazy val view: InterestCYAView = app.injector.instanceOf[InterestCYAView]

  val taxYear = 2020
  val taxYearMinusOne: Int = taxYear -1

  val question2 = 2
  val question4 = 4

  val account1 = 1
  val account2 = 2

  val titleSelector = "title"
  val h1Selector = "h1"
  val captionSelector = ".govuk-caption-l"
  val submitButton = ".govuk-button"
  val submitButtonForm = "#main-content > div > div > form"

  val questionSelector: Int => String = questionNumber => s".govuk-summary-list__row:nth-child($questionNumber) > .govuk-summary-list__key"
  val questionAccountSelector: (Int, Int, Int) => String = (questionNumber, accountNumber,account) =>
    s"#question-$questionNumber-account-$account:nth-child($accountNumber)"
  val questionChangeLinkSelector: Int => String = questionNumber => s"#main-content > div > div > dl > div:nth-child($questionNumber) " +
    s"> dd.govuk-summary-list__actions > a"
  val questionTextSelector: Int => String = question => s"#main-content > div > div > dl > div:nth-child($question) > dt"

  val yesNoQuestionAnswer: Int => String = questionNumber => s"#main-content > div > div > dl > div:nth-child($questionNumber) > dd.govuk-summary-list__value"

  val h1Expected = "Check your answers"
  val titleExpected = "Check your answers"
  val captionExpected = s"Interest for 6 April $taxYearMinusOne to 5 April $taxYear"

  val changeLinkExpected = "Change"

  val questionUntaxedInterestExpected = "Untaxed UK Interest"
  val questionUntaxedInterestDetailsExpected = "Details for the untaxed UK interest"
  val questionTaxedInterestExpected = "Taxed UK Interest"
  val question4TaxedInterestDetailExpected = "Details for the taxed UK interest"

  val changeUntaxedInterestIndividualHiddenText = "whether you got untaxed interest from UK accounts."
  val changeUntaxedInterestAgentHiddenText = "whether your client got untaxed interest from UK accounts."
  val changeUntaxedDetailsHiddenText = "or add a new UK account with untaxed interest."
  val changeTaxedInterestIndividualHiddenText = "whether you got taxed interest from UK accounts."
  val changeTaxedInterestAgentHiddenText = "whether your client got taxed interest from UK accounts."
  val changeTaxedDetailsHiddenText = "or add a new UK account with taxed interest."

  val untaxedInterestAccount1ExpectedTest = "UntaxedBank1 : £100"
  val taxedInterestAccount1ExpectedTest = "TaxedBank1 : £200"
  val taxedInterestAccount2ExpectedTest = "TaxedBank2 : £400"

  val changeUntaxedInterestHref = s"/income-through-software/return/personal-income/$taxYear/interest/untaxed-uk-interest"
  val changeUntaxedInterestAmountHref = s"/income-through-software/return/personal-income/$taxYear/interest/untaxed-uk-interest-account-summary"
  val changeTaxedInterestHref = s"/income-through-software/return/personal-income/$taxYear/interest/taxed-uk-interest"
  val changeTaxedInterestAmountHref = s"/income-through-software/return/personal-income/$taxYear/interest/taxed-uk-interest-account-summary"

  val submitText = "Save and continue"
  val submitLink = s"/income-through-software/return/personal-income/$taxYear/interest/check-your-answers"

  val Yes = "Yes"
  val No = "No"

  "InterestCYAView" should {

    "Render correctly for an individual" when {

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

          val render = view(cyaModel, taxYear)(user, messages, mockAppConfig).body
          implicit val document: Document = Jsoup.parse(render)

          titleCheck(titleExpected)
          h1Check(h1Expected)
          textOnPageCheck(captionExpected, captionSelector)

          buttonCheck(submitText, submitButton)
          formPostLinkCheck(submitLink, submitButtonForm)

          "has an area for question 1" which {
            textOnPageCheck(questionUntaxedInterestExpected, questionSelector(1))
            textOnPageCheck(Yes, yesNoQuestionAnswer(1))
            linkCheck(s"$changeLinkExpected $changeUntaxedInterestIndividualHiddenText", questionChangeLinkSelector(1), changeUntaxedInterestHref)
          }

          "has an area for question 2" which {
            textOnPageCheck(questionUntaxedInterestDetailsExpected, questionSelector(2))
            textOnPageCheck(untaxedInterestAccount1ExpectedTest, questionAccountSelector(question2, account1, 1))
            linkCheck(s"$changeLinkExpected $changeUntaxedDetailsHiddenText", questionChangeLinkSelector(2), changeUntaxedInterestAmountHref)
          }

          "has an area for question 3" which {
            textOnPageCheck(questionTaxedInterestExpected, questionSelector(3))
            textOnPageCheck(Yes, yesNoQuestionAnswer(3))
            linkCheck(s"$changeLinkExpected $changeTaxedInterestIndividualHiddenText", questionChangeLinkSelector(3), changeTaxedInterestHref)
          }

          "has an area for question 4" which {
            textOnPageCheck(question4TaxedInterestDetailExpected, questionSelector(question4))
            textOnPageCheck(taxedInterestAccount1ExpectedTest, questionAccountSelector(question4, account1, 1))
            textOnPageCheck(taxedInterestAccount2ExpectedTest, questionAccountSelector(question4, account2, 2))
            linkCheck(s"$changeLinkExpected $changeTaxedDetailsHiddenText", questionChangeLinkSelector(question4), changeTaxedInterestAmountHref)
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

          val render = view(cyaModel, taxYear)(user, messages, mockAppConfig).body
          implicit val document: Document = Jsoup.parse(render)

          titleCheck(titleExpected)
          h1Check(h1Expected)
          textOnPageCheck(captionExpected, captionSelector)

          buttonCheck(submitText, submitButton)
          formPostLinkCheck(submitLink, submitButtonForm)

          "has an area for question 1" which {
            textOnPageCheck(questionUntaxedInterestExpected, questionTextSelector(1))
            textOnPageCheck(No, yesNoQuestionAnswer(1))
            linkCheck(s"$changeLinkExpected $changeUntaxedInterestIndividualHiddenText", questionChangeLinkSelector(1), changeUntaxedInterestHref)
          }

          "has an area for question 2" which {
            textOnPageCheck(questionTaxedInterestExpected, questionTextSelector(2))
            textOnPageCheck(No, yesNoQuestionAnswer(2))
            linkCheck(s"$changeLinkExpected $changeTaxedInterestIndividualHiddenText", questionChangeLinkSelector(2), changeTaxedInterestHref)
          }

          "there is no question 3" in {
            elementExist(questionSelector(3)) shouldBe false
          }

          "there is no question 4" in {
            elementExist(questionSelector(question4)) shouldBe false
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

          val render = view(cyaModel, taxYear, Some(priorSubmission))(user, messages, mockAppConfig).body
          implicit val document: Document = Jsoup.parse(render)

          titleCheck(titleExpected)
          h1Check(h1Expected)
          textOnPageCheck(captionExpected, captionSelector)

          buttonCheck(submitText, submitButton)
          formPostLinkCheck(submitLink, submitButtonForm)

          "has an area for question 1" which {
            textOnPageCheck(questionUntaxedInterestDetailsExpected, questionTextSelector(1))
            textOnPageCheck("TSB : £100", yesNoQuestionAnswer(1))
            linkCheck(s"$changeLinkExpected $changeUntaxedDetailsHiddenText", questionChangeLinkSelector(1), changeUntaxedInterestAmountHref)
          }

          "has an area for question 2" which {
            textOnPageCheck(question4TaxedInterestDetailExpected, questionTextSelector(2))
            textOnPageCheck("TSB Account : £100", yesNoQuestionAnswer(2))
            linkCheck(s"$changeLinkExpected $changeTaxedDetailsHiddenText", questionChangeLinkSelector(2), changeTaxedInterestAmountHref)
          }

          "there is no question 3" in {
            elementExist(questionSelector(3)) shouldBe false
          }

          "there is no question 4" in {
            elementExist(questionSelector(question4)) shouldBe false
          }
        }
      }
    }
  }
}
