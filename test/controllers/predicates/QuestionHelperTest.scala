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

package controllers.predicates

import controllers.Assets.{Ok, Redirect}
import controllers.dividends.routes.{OtherUkDividendsAmountController, ReceiveOtherUkDividendsController,
  ReceiveUkDividendsController, UkDividendsAmountController}
import models.DividendsCheckYourAnswersModel
import play.api.mvc.Result
import play.api.test.DefaultAwaitTimeout
import utils.UnitTest

class QuestionHelperTest extends UnitTest with DefaultAwaitTimeout {

  "validateQuestion()" should {

    val taxYear = 2022
    val receivedDividendsPage = ReceiveUkDividendsController.show(taxYear)
    val dividendsAmountPage = UkDividendsAmountController.show(taxYear)
    val receivedOtherDividendsPage = ReceiveOtherUkDividendsController.show(taxYear)
    val otherDividendsAmountPage = OtherUkDividendsAmountController.show(taxYear)

    val expectedContent = "Some content"
    def block: Result = Ok(expectedContent)

    "validate for DividendsCheckYourAnswersModel" should {

      "return Ok status when viewing the first page of a questions journey (receivedDividendsPage) and cya data is not present" in {
        val result: Result = QuestionHelper.validateQuestion(receivedDividendsPage, Option.empty[DividendsCheckYourAnswersModel], taxYear)(block)

        result shouldBe Ok(expectedContent)
      }

      "Redirect to the first page of the questions journey (receivedDividendsPage) when viewing dividendsAmountPage and cya data is not present" in {

        val result: Result = QuestionHelper.validateQuestion(dividendsAmountPage, Option.empty[DividendsCheckYourAnswersModel], taxYear)(block)

        result shouldEqual Redirect(receivedDividendsPage)
      }

      "return Ok status when viewing receivedDividendsPage and 'no' is answer to receivedDividendsPage question" in {
        val model = DividendsCheckYourAnswersModel(Some(false), None)

        val result: Result = QuestionHelper.validateQuestion(receivedDividendsPage, Some(model), taxYear)(block)

        result shouldBe Ok(expectedContent)
      }

      "return Ok status when viewing receivedOtherDividendsPage and 'yes' is answer to receivedOtherDividendsPage question" in {
        val model = DividendsCheckYourAnswersModel(Some(false), None, Some(true), None)

        val result = QuestionHelper.validateQuestion(receivedOtherDividendsPage, Some(model), taxYear)(block)

        result shouldBe Ok(expectedContent)
      }

      "Redirect to receivedDividendsPage when viewing dividendsAmountPage and 'no' is answer to receivedDividendsPage question" in {
        val model = DividendsCheckYourAnswersModel(Some(false), None)

        val result = QuestionHelper.validateQuestion(dividendsAmountPage, Some(model), taxYear)(block)

        result shouldEqual Redirect(receivedDividendsPage)
      }

      "Redirect to receivedDividendsPage when viewing dividendsAmountPage when user has not answered receivedDividendsPage question" in {
        val model = DividendsCheckYourAnswersModel(None, None)

        val result = QuestionHelper.validateQuestion(dividendsAmountPage, Some(model), taxYear)(block)

        result shouldEqual Redirect(receivedDividendsPage)
      }

      "return Ok status when viewing receivedOtherDividendsPage and otherDividend question is unanswered" in {
        val model = DividendsCheckYourAnswersModel(Some(false), Some(BigDecimal(100)), None, None)

        val result = QuestionHelper.validateQuestion(receivedOtherDividendsPage, Some(model), taxYear)(block)

        result shouldBe Ok(expectedContent)
      }

      "Redirect to receivedOtherDividendsPage when viewing otherDividendsAmountPage and user has not answered receivedOtherDividendsPage question" in {
        val model = DividendsCheckYourAnswersModel(None, None, None, Some(BigDecimal(100)))

        val result = QuestionHelper.validateQuestion(otherDividendsAmountPage, Some(model), taxYear)(block)

        result shouldEqual Redirect(receivedOtherDividendsPage)
      }

      "Redirect to receivedOtherDividendsPage when viewing otherDividendsAmountPage and user said 'No' to receivedOtherDividendsPage question" in {
        val model = DividendsCheckYourAnswersModel(Some(false), None, Some(false), None)

        val result = QuestionHelper.validateQuestion(otherDividendsAmountPage, Some(model), taxYear)(block)

        result shouldEqual Redirect(receivedOtherDividendsPage)
      }
    }
  }

}