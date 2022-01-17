/*
 * Copyright 2022 HM Revenue & Customs
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

import models.question.Question.{WithDependency, WithoutDependency}
import models.question.{Question, QuestionsJourney}
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Call
import utils.EncryptedValue

case class InterestCYAModel(untaxedUkInterest: Option[Boolean] = None,
                            taxedUkInterest: Option[Boolean] = None,
                            accounts: Option[Seq[InterestAccountModel]] = None) {

  def asJsonString: String = Json.toJson(this).toString()

  def isFinished: Boolean = {

    val untaxedInterestFinished: Boolean = untaxedUkInterest.exists {
      case true => accounts.getOrElse(Seq.empty).exists(_.untaxedAmount.isDefined)
      case false => true
    }

    val taxedInterestFinished: Boolean = taxedUkInterest.exists {
      case true => accounts.getOrElse(Seq.empty).exists(_.taxedAmount.isDefined)
      case false => true
    }

    untaxedInterestFinished && taxedInterestFinished
  }

}

object InterestCYAModel {
  implicit val formats: OFormat[InterestCYAModel] = Json.format[InterestCYAModel]

  def interestJourney(taxYear: Int, idOpt: Option[String]): QuestionsJourney[InterestCYAModel] = new QuestionsJourney[InterestCYAModel] {
    override val firstPage: Call = controllers.interest.routes.UntaxedInterestController.show(taxYear)

    override def questions(model: InterestCYAModel): Set[Question] = {
      val questionsUsingId = idOpt.map { id =>
        Set(
          WithDependency(model.accounts.map(_.filter(_.untaxedAmount.isDefined)), model.untaxedUkInterest,
            controllers.interest.routes.UntaxedInterestAmountController.show(taxYear, id), controllers.interest.routes.UntaxedInterestController.show(taxYear)),
          WithDependency(model.accounts.map(_.filter(_.taxedAmount.isDefined)), model.taxedUkInterest,
            controllers.interest.routes.TaxedInterestAmountController.show(taxYear, id), controllers.interest.routes.TaxedInterestController.show(taxYear))
        )
      }.getOrElse(Seq.empty[Question])

      Set(
        WithoutDependency(model.untaxedUkInterest, controllers.interest.routes.UntaxedInterestController.show(taxYear)),
        WithoutDependency(model.taxedUkInterest, controllers.interest.routes.TaxedInterestController.show(taxYear))
      ) ++ questionsUsingId
    }
  }

}

case class EncryptedInterestCYAModel(untaxedUkInterest: Option[EncryptedValue] = None,
                            taxedUkInterest: Option[EncryptedValue] = None,
                            accounts: Option[Seq[EncryptedInterestAccountModel]] = None)

object EncryptedInterestCYAModel {

  implicit val formats: OFormat[EncryptedInterestCYAModel] = Json.format[EncryptedInterestCYAModel]

}

