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

package models.dividends

import config.AppConfig
import models.question.Question.{WithDependency, WithoutDependency}
import models.question.{Question, QuestionsJourney}
import play.api.libs.json.{Format, Json, OFormat}
import play.api.mvc.Call
import uk.gov.hmrc.crypto.EncryptedValue

case class DividendsCheckYourAnswersModel(
                                           gateway: Option[Boolean] = None,
                                           ukDividends: Option[Boolean] = None,
                                           ukDividendsAmount: Option[BigDecimal] = None,
                                           otherUkDividends: Option[Boolean] = None,
                                           otherUkDividendsAmount: Option[BigDecimal] = None
                                         ) {

  def isFinished(implicit appConfig: AppConfig): Boolean = {

    val ukDividendsFinished: Boolean = ukDividends.exists {
      case true => ukDividendsAmount.isDefined
      case false => true
    }

    val otherUkDividendsFinished: Boolean = otherUkDividends.exists {
      case true => otherUkDividendsAmount.isDefined
      case false => true
    }

    if(appConfig.dividendsTailoringEnabled) {
      gateway.contains(false) || (gateway.contains(true) && ukDividendsFinished && otherUkDividendsFinished)
    } else {
      ukDividendsFinished && otherUkDividendsFinished
    }
  }

  def hasNoValues: Boolean =
    this.ukDividendsAmount.isEmpty && this.otherUkDividendsAmount.isEmpty

}

object DividendsCheckYourAnswersModel {
  private val receiveUkDividendsControllerRoute = controllers.dividends.routes.ReceiveUkDividendsController
  private val ukDividendsAmountControllerRoute = controllers.dividends.routes.UkDividendsAmountController
  private val receiveOtherUkDividendsControllerRoute = controllers.dividends.routes.ReceiveOtherUkDividendsController
  private val otherUkDividendsAmountControllerRoute = controllers.dividends.routes.OtherUkDividendsAmountController
  private val gatewayControllerRoute = controllers.dividends.routes.DividendsGatewayController
  
  implicit val formats: OFormat[DividendsCheckYourAnswersModel] = Json.format[DividendsCheckYourAnswersModel]

  private[dividends] def priorityOrderOrNone(priority: Option[BigDecimal], other: Option[BigDecimal], yesNoResult: Boolean): Option[BigDecimal] = {
    if (yesNoResult) {
      (priority, other) match {
        case (Some(priorityValue), _) => Some(priorityValue)
        case (None, Some(otherValue)) => Some(otherValue)
        case _ => None
      }
    } else {
      None
    }
  }

  def getCyaModel(cya: Option[DividendsCheckYourAnswersModel], prior: Option[DividendsPriorSubmission]): Option[DividendsCheckYourAnswersModel] = {
    (cya, prior) match {
      case (Some(cyaData), Some(priorData)) =>
        val ukDividendsExist = cyaData.ukDividends.getOrElse(priorData.ukDividends.nonEmpty)
        val otherDividendsExist = cyaData.otherUkDividends.getOrElse(priorData.otherUkDividends.nonEmpty)

        val ukDividendsValue: Option[BigDecimal] = priorityOrderOrNone(cyaData.ukDividendsAmount, priorData.ukDividends, ukDividendsExist)
        val otherDividendsValue: Option[BigDecimal] = priorityOrderOrNone(cyaData.otherUkDividendsAmount, priorData.otherUkDividends, otherDividendsExist)

        Some(DividendsCheckYourAnswersModel(
          cyaData.gateway,
          Some(ukDividendsExist),
          ukDividendsValue,
          Some(otherDividendsExist),
          otherDividendsValue
        ))
      case (Some(cyaData), _) => Some(cyaData)
      case (None, Some(priorData)) =>
        Some(DividendsCheckYourAnswersModel(
          Some(true),
          Some(priorData.ukDividends.nonEmpty),
          priorData.ukDividends,
          Some(priorData.otherUkDividends.nonEmpty),
          priorData.otherUkDividends
        ))
      case _ => None


    }
  }

  def journey(taxYear: Int)
             (implicit appConfig: AppConfig): QuestionsJourney[DividendsCheckYourAnswersModel] = new QuestionsJourney[DividendsCheckYourAnswersModel] {
    
    override def firstPage: Call = if(appConfig.dividendsTailoringEnabled) gatewayControllerRoute.show(taxYear) else receiveUkDividendsControllerRoute.show(taxYear)
    
    val gatewayQuestion: DividendsCheckYourAnswersModel => Option[Question] = model => if(appConfig.dividendsTailoringEnabled) {
      Some(WithoutDependency(model.gateway, gatewayControllerRoute.show(taxYear)))
    } else {
      None
    }
    val receiveUkDividendsQuestion: DividendsCheckYourAnswersModel => Option[Question] = model => if(appConfig.dividendsTailoringEnabled) {
      Some(WithDependency(
        model.ukDividends,
        model.gateway, receiveUkDividendsControllerRoute.show(taxYear),
        gatewayControllerRoute.show(taxYear)
      ))
    } else {
      Some(WithoutDependency(model.ukDividends, receiveUkDividendsControllerRoute.show(taxYear)))
    }
    
    override def questions(model: DividendsCheckYourAnswersModel): Set[Question] = Set(
      gatewayQuestion(model),
      receiveUkDividendsQuestion(model),
      Some(WithDependency(
        model.ukDividendsAmount, model.ukDividends, ukDividendsAmountControllerRoute.show(taxYear),
        receiveUkDividendsControllerRoute.show(taxYear)
      )),
      Some(WithoutDependency(model.otherUkDividends, receiveOtherUkDividendsControllerRoute.show(taxYear))),
      Some(WithDependency(model.otherUkDividendsAmount, model.otherUkDividends,
        otherUkDividendsAmountControllerRoute.show(taxYear), receiveOtherUkDividendsControllerRoute.show(taxYear)))
    ).flatten
  }
}

case class EncryptedDividendsCheckYourAnswersModel(
                                                    gateway: Option[EncryptedValue] = None,
                                                    ukDividends: Option[EncryptedValue] = None,
                                                    ukDividendsAmount: Option[EncryptedValue] = None,
                                                    otherUkDividends: Option[EncryptedValue] = None,
                                                    otherUkDividendsAmount: Option[EncryptedValue] = None
                                                  )

object EncryptedDividendsCheckYourAnswersModel {
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]

  implicit val formats: Format[EncryptedDividendsCheckYourAnswersModel] = Json.format[EncryptedDividendsCheckYourAnswersModel]

}
