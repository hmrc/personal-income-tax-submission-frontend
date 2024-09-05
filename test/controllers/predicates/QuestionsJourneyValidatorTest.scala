/*
 * Copyright 2023 HM Revenue & Customs
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


import models.dividends.DividendsCheckYourAnswersModel
import models.question.Question.{WithDependency, WithoutDependency}
import models.question.{Question, QuestionsJourney}
import play.api.mvc.Results.{Ok, Redirect}
import play.api.mvc.{Call, Result}
import play.api.test.DefaultAwaitTimeout
import utils.UnitTest

class QuestionsJourneyValidatorTest extends UnitTest with DefaultAwaitTimeout {

  val questionsJourneyValidator = new QuestionsJourneyValidator(mockAppConfig)

  private val receiveUkDividendsRoute = controllers.dividends.routes.ReceiveUkDividendsController
  private val ukDividendsAmountRoute = controllers.dividendsBase.routes.UkDividendsAmountBaseController
  private val receiveOtherDividendsRoute = controllers.dividends.routes.ReceiveOtherUkDividendsController
  private val otherDividendsAmountRoute = controllers.dividendsBase.routes.OtherUkDividendsAmountBaseController
  
  "validateQuestion()" when {

    val taxYear = 2022
    val receivedDividendsPage = receiveUkDividendsRoute.show(taxYear)
    val dividendsAmountPage = ukDividendsAmountRoute.show(taxYear)
    val receivedOtherDividendsPage = receiveOtherDividendsRoute.show(taxYear)
    val otherDividendsAmountPage = otherDividendsAmountRoute.show(taxYear)

    val expectedContent = "Some content"

    def block: Result = Ok(expectedContent)

    implicit val journey: QuestionsJourney[DividendsCheckYourAnswersModel] = new QuestionsJourney[DividendsCheckYourAnswersModel] {
      override def firstPage: Call = receivedDividendsPage

      override def questions(model: DividendsCheckYourAnswersModel): Set[Question] = Set(
        WithoutDependency(model.ukDividends, receivedDividendsPage),
        WithDependency(model.ukDividendsAmount, model.ukDividends, dividendsAmountPage, receivedDividendsPage),
        WithoutDependency(model.otherUkDividends, receivedOtherDividendsPage),
        WithDependency(model.otherUkDividendsAmount, model.otherUkDividends, otherDividendsAmountPage, receivedOtherDividendsPage)
      )
    }

    "CYA model is not defined" should {

      "allow viewing the provided currentPage if the currentPage is the firstPage of the journey" in {
        val result: Result = questionsJourneyValidator.validate(receivedDividendsPage, Option.empty[DividendsCheckYourAnswersModel], taxYear)(block)

        result shouldBe Ok(expectedContent)
      }

      "Redirect to the overview page if the currentPage is not the firstPage of the journey" in {

        val result: Result = questionsJourneyValidator.validate(dividendsAmountPage, Option.empty[DividendsCheckYourAnswersModel], taxYear)(block)

        result shouldEqual Redirect(mockAppConfig.incomeTaxSubmissionOverviewUrl(taxYear))
      }

    }

    "CYA model is defined" should {

      "allow viewing the provided currentPage is a WithoutDependency page in the journey" in {
        val model = DividendsCheckYourAnswersModel(ukDividends = Some(false), ukDividendsAmount = None)

        val result: Result = questionsJourneyValidator.validate(receivedDividendsPage, Some(model), taxYear)(block)

        result shouldBe Ok(expectedContent)
      }

      "allow viewing the provided currentPage if it is a WithoutDependency page in the journey" in {
        val model = DividendsCheckYourAnswersModel(ukDividends = Some(false), ukDividendsAmount = None)

        val result: Result = questionsJourneyValidator.validate(receivedDividendsPage, Some(model), taxYear)(block)

        result shouldBe Ok(expectedContent)
      }

      "allow viewing the provided currentPage if it is a WithDependency page and the dependency is Some(true) in the journey" in {
        val model = DividendsCheckYourAnswersModel(
          ukDividends = Some(true),
          ukDividendsAmount = None
        )

        val result: Result = questionsJourneyValidator.validate(dividendsAmountPage, Some(model), taxYear)(block)

        result shouldBe Ok(expectedContent)
      }

      "Redirect to the redirectPage of the WithDependency if dependency is None" in {
        val currentPage = dividendsAmountPage
        val model = DividendsCheckYourAnswersModel(ukDividends = None, ukDividendsAmount = None)
        val expectedRedirectPage = journey.questions(model).find(_.expectedPage == currentPage).get.asInstanceOf[WithDependency].redirectPage

        val result: Result = questionsJourneyValidator.validate(currentPage, Some(model), taxYear)(block)

        result shouldEqual Redirect(expectedRedirectPage)
      }

      "Redirect to the redirectPage of the WithDependency if dependency is Some(false)" in {
        val currentPage = dividendsAmountPage
        val model = DividendsCheckYourAnswersModel(ukDividends = Some(false), ukDividendsAmount = None)
        val expectedRedirectPage = journey.questions(model).find(_.expectedPage == currentPage).get.asInstanceOf[WithDependency].redirectPage

        val result: Result = questionsJourneyValidator.validate(currentPage, Some(model), taxYear)(block)

        result shouldEqual Redirect(expectedRedirectPage)
      }

    }

  }

}
