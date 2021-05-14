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

package models.interest

import controllers.interest.routes.{TaxedInterestAmountController, TaxedInterestController, UntaxedInterestAmountController, UntaxedInterestController}
import models.question.Question.{WithDependency, WithoutDependency}
import models.question.{Question, QuestionsJourney}
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Call

case class InterestCYAModel(
                             untaxedUkInterest: Option[Boolean] = None,
                             untaxedUkAccounts: Option[Seq[InterestAccountModel]] = None,
                             taxedUkInterest: Option[Boolean] = None,
                             taxedUkAccounts: Option[Seq[InterestAccountModel]] = None
                           ) {

  def asJsonString: String = Json.toJson(this).toString()

  def isFinished: Boolean = {

    val untaxedInterestFinished: Boolean = untaxedUkInterest.exists {
      case true => untaxedUkAccounts.getOrElse(Seq.empty).nonEmpty
      case false => true
    }

    val taxedInterestFinished: Boolean = taxedUkInterest.exists {
      case true => taxedUkAccounts.getOrElse(Seq.empty).nonEmpty
      case false => true
    }

    untaxedInterestFinished && taxedInterestFinished
  }

}

object InterestCYAModel {
  implicit val formats: OFormat[InterestCYAModel] = Json.format[InterestCYAModel]

  def interestJourney(taxYear: Int, idOpt: Option[String]): QuestionsJourney[InterestCYAModel] = new QuestionsJourney[InterestCYAModel] {
    override val firstPage: Call = UntaxedInterestController.show(taxYear)

    override def questions(model: InterestCYAModel): Set[Question] = {
      val questionsUsingId = idOpt.map { id =>
        Set(
          WithDependency(model.untaxedUkAccounts, model.untaxedUkInterest,
            UntaxedInterestAmountController.show(taxYear, id), UntaxedInterestController.show(taxYear)),
          WithDependency(model.taxedUkAccounts, model.taxedUkInterest,
            TaxedInterestAmountController.show(taxYear, id), TaxedInterestController.show(taxYear))
        )
      }.getOrElse(Seq.empty[Question])

      Set(
        WithoutDependency(model.untaxedUkInterest, UntaxedInterestController.show(taxYear)),
        WithoutDependency(model.taxedUkInterest, TaxedInterestController.show(taxYear))
      ) ++ questionsUsingId
    }
  }

}

