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

package models.dividends

import config.AppConfig
import models.question.Question.{WithDependency, WithoutDependency}
import models.question.{Question, QuestionsJourney}
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Call
import utils.EncryptedValue

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

    if(appConfig.tailoringEnabled) {
      gateway.contains(false) || (gateway.contains(true) && ukDividendsFinished && otherUkDividendsFinished)
    } else {
      ukDividendsFinished && otherUkDividendsFinished
    }
  }

}

object DividendsCheckYourAnswersModel {
  private val receiveUkDividendsControllerRoute = controllers.dividends.routes.ReceiveUkDividendsController
  private val ukDividendsAmountControllerRoute = controllers.dividends.routes.UkDividendsAmountController
  private val receiveOtherUkDividendsControllerRoute = controllers.dividends.routes.ReceiveOtherUkDividendsController
  private val otherUkDividendsAmountControllerRoute = controllers.dividends.routes.OtherUkDividendsAmountController
  private val gatewayControllerRoute = controllers.dividends.routes.DividendsGatewayController
  
  implicit val formats: OFormat[DividendsCheckYourAnswersModel] = Json.format[DividendsCheckYourAnswersModel]

  def journey(taxYear: Int)
             (implicit appConfig: AppConfig): QuestionsJourney[DividendsCheckYourAnswersModel] = new QuestionsJourney[DividendsCheckYourAnswersModel] {
    
    override def firstPage: Call = if(appConfig.tailoringEnabled) gatewayControllerRoute.show(taxYear) else receiveUkDividendsControllerRoute.show(taxYear)
    
    val gatewayQuestion: DividendsCheckYourAnswersModel => Option[Question] = model => if(appConfig.tailoringEnabled) {
      Some(WithoutDependency(model.gateway, gatewayControllerRoute.show(taxYear)))
    } else {
      None
    }
    val receiveUkDividendsQuestion: DividendsCheckYourAnswersModel => Option[Question] = model => if(appConfig.tailoringEnabled) {
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

  implicit val formats: OFormat[EncryptedDividendsCheckYourAnswersModel] = Json.format[EncryptedDividendsCheckYourAnswersModel]

}
