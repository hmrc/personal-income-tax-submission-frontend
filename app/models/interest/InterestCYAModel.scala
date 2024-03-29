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

package models.interest

import common.InterestTaxTypes
import config.AppConfig
import models.question.Question.{WithDependency, WithoutDependency}
import models.question.{Question, QuestionsJourney}
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsNull, JsObject, JsPath, Json, OFormat, Reads, Writes}
import play.api.mvc.Call
import uk.gov.hmrc.crypto.EncryptedValue
case class InterestCYAModel(gateway: Option[Boolean] = None,
                            untaxedUkInterest: Option[Boolean] = None,
                            taxedUkInterest: Option[Boolean] = None,
                            accounts: Seq[InterestAccountModel] = Seq.empty) {

  def asJsonString: String = Json.toJson(this).toString()

  def isFinished (implicit appConfig: AppConfig): Boolean = {

    val untaxedInterestFinished: Boolean = untaxedUkInterest.exists {
      case true => accounts.exists(_.untaxedAmount.isDefined)
      case false => true
    }

    val taxedInterestFinished: Boolean = taxedUkInterest.exists {
      case true => accounts.exists(_.taxedAmount.isDefined)
      case false => true
    }

    if (appConfig.interestTailoringEnabled) {
      gateway.contains(false) || (gateway.contains(true) && untaxedInterestFinished && taxedInterestFinished)
    } else {
      untaxedInterestFinished && taxedInterestFinished
    }
  }

}

object InterestCYAModel {
  implicit val reads: Reads[InterestCYAModel] = (
    (JsPath \ "gateway").readNullable[Boolean] and
    (JsPath \ "untaxedUkInterest").readNullable[Boolean] and
      (JsPath \ "taxedUkInterest").readNullable[Boolean] and
      (JsPath \ "accounts").readNullable[Seq[InterestAccountModel]].map(_.getOrElse(Seq()))
    ) (InterestCYAModel.apply _)

  implicit val writes: Writes[InterestCYAModel] = (model: InterestCYAModel) => {
    JsObject(Json.obj(
      "gateway" -> model.gateway,
      "untaxedUkInterest" -> model.untaxedUkInterest,
      "taxedUkInterest" -> model.taxedUkInterest,
      "accounts" -> {
        if (model.accounts.isEmpty) JsNull else model.accounts
      }
    ).fields.filterNot(_._2 == JsNull))
  }

  def interestJourney(taxYear: Int, idOpt: Option[String])
                     (implicit appConfig: AppConfig): QuestionsJourney[InterestCYAModel] = new QuestionsJourney[InterestCYAModel] {
    override val firstPage: Call =
      if (appConfig.interestTailoringEnabled) {
        controllers.interest.routes.InterestGatewayController.show(taxYear)
      } else {
        controllers.interest.routes.UntaxedInterestController.show(taxYear)
      }

    val gatewayQuestion: InterestCYAModel => Option[Question] = model =>
      if (appConfig.interestTailoringEnabled) {
        Some(WithoutDependency(model.gateway, controllers.interest.routes.InterestGatewayController.show(taxYear)))
      } else {
        None
      }

    val untaxedUkInterestQuestion: InterestCYAModel => Option[Question] = model =>

      if (appConfig.interestTailoringEnabled) {
        Some(WithDependency(model.untaxedUkInterest, model.gateway, controllers.interest.routes.UntaxedInterestController.show(taxYear),
          controllers.interest.routes.InterestGatewayController.show(taxYear)))
      } else {
        Some(WithoutDependency(model.untaxedUkInterest, controllers.interest.routes.UntaxedInterestController.show(taxYear)))
      }

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
        gatewayQuestion(model),
        untaxedUkInterestQuestion(model),
        Some(WithoutDependency(model.taxedUkInterest, controllers.interest.routes.TaxedInterestController.show(taxYear)))
      ).flatten ++ questionsUsingId
    }
  }

  def disallowedDuplicateNames(optionalCyaData: Option[InterestCYAModel], id: String, taxType: String): Seq[String] = {
    optionalCyaData.map {
      _.accounts
        .filter(if (taxType == InterestTaxTypes.TAXED) _.hasTaxed else _.hasUntaxed)
        .filterNot(_.getPrimaryId().contains(id))
    }.getOrElse(Seq()).map(_.accountName)
  }

  def getCyaModel(cya: Option[InterestCYAModel], prior: Option[InterestPriorSubmission]): Option[InterestCYAModel] = {
    (cya, prior) match {
      case (None, Some(priorData)) =>
        Some(InterestCYAModel(
          Some(priorData.hasTaxed || priorData.hasUntaxed),
          Some(priorData.hasUntaxed),
          Some(priorData.hasTaxed),
          priorData.submissions.filter(x => x.hasTaxed || x.hasUntaxed)
        ))
      case (Some(cyaData), _) => Some(cyaData)
      case _ => None
    }
  }
}

case class EncryptedInterestCYAModel(gateway: Option[EncryptedValue] = None,
                                     untaxedUkInterest: Option[EncryptedValue] = None,
                                     taxedUkInterest: Option[EncryptedValue] = None,
                                     accounts: Seq[EncryptedInterestAccountModel] = Seq.empty)

object EncryptedInterestCYAModel {

  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

  implicit val reads: Reads[EncryptedInterestCYAModel] = (
    (JsPath \ "gateway").readNullable[EncryptedValue] and
    (JsPath \ "untaxedUkInterest").readNullable[EncryptedValue] and
      (JsPath \ "taxedUkInterest").readNullable[EncryptedValue] and
      (JsPath \ "accounts").readNullable[Seq[EncryptedInterestAccountModel]].map(_.getOrElse(Seq()))
    ) (EncryptedInterestCYAModel.apply _)

  implicit val writes: Writes[EncryptedInterestCYAModel] = (model: EncryptedInterestCYAModel) => {
    JsObject(Json.obj(
      "gateway" -> model.gateway,
      "untaxedUkInterest" -> model.untaxedUkInterest,
      "taxedUkInterest" -> model.taxedUkInterest,
      "accounts" -> {
        if (model.accounts.isEmpty) JsNull else model.accounts
      }
    ).fields.filterNot(_._2 == JsNull))
  }

}

