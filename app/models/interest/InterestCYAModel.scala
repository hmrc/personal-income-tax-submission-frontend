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
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsNull, JsObject, JsPath, Json, OFormat, Reads, Writes}
import play.api.mvc.Call
import utils.EncryptedValue

case class InterestCYAModel(untaxedUkInterest: Option[Boolean] = None,
                            taxedUkInterest: Option[Boolean] = None,
                            accounts: Seq[InterestAccountModel] = Seq.empty) {

  def asJsonString: String = Json.toJson(this).toString()

  def isFinished: Boolean = {

    val untaxedInterestFinished: Boolean = untaxedUkInterest.exists {
      case true => accounts.exists(_.untaxedAmount.isDefined)
      case false => true
    }

    val taxedInterestFinished: Boolean = taxedUkInterest.exists {
      case true => accounts.exists(_.taxedAmount.isDefined)
      case false => true
    }

    untaxedInterestFinished && taxedInterestFinished
  }

}

object InterestCYAModel {
  implicit val reads: Reads[InterestCYAModel] = (
    (JsPath \ "untaxedUkInterest").readNullable[Boolean] and
      (JsPath \ "taxedUkInterest").readNullable[Boolean] and
      (JsPath \ "accounts").readNullable[Seq[InterestAccountModel]].map(_.getOrElse(Seq()))
    ) (InterestCYAModel.apply _)

  implicit val writes: Writes[InterestCYAModel] = (model: InterestCYAModel) => {
    JsObject(Json.obj(
      "untaxedUkInterest" -> model.untaxedUkInterest,
      "taxedUkInterest" -> model.taxedUkInterest,
      "accounts" -> {
        if (model.accounts.isEmpty) JsNull else model.accounts
      }
    ).fields.filterNot(_._2 == JsNull))
  }

  def interestJourney(taxYear: Int, idOpt: Option[String]): QuestionsJourney[InterestCYAModel] = new QuestionsJourney[InterestCYAModel] {
    override val firstPage: Call = controllers.interest.routes.UntaxedInterestController.show(taxYear)

    override def questions(model: InterestCYAModel): Set[Question] = {
      val questionsUsingId = idOpt.map { id =>
        Set(
          WithDependency(Option(model.accounts.filter(_.untaxedAmount.isDefined)).filterNot(_.isEmpty), model.untaxedUkInterest,
            controllers.interest.routes.UntaxedInterestAmountController.show(taxYear, id), controllers.interest.routes.UntaxedInterestController.show(taxYear)),
          WithDependency(Option(model.accounts.filter(_.taxedAmount.isDefined)).filterNot(_.isEmpty), model.taxedUkInterest,
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
                                     accounts: Seq[EncryptedInterestAccountModel] = Seq.empty)

object EncryptedInterestCYAModel {

  implicit val formats: OFormat[EncryptedInterestCYAModel] = Json.format[EncryptedInterestCYAModel]

}

