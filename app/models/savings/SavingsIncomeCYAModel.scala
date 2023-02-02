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

package models.savings

import config.AppConfig
import play.api.libs.json.{Json, OFormat}
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.crypto.EncryptedValue

import scala.concurrent.Future

case class SavingsIncomeCYAModel(
                             gateway: Option[Boolean] = None,
                             grossAmount: Option[BigDecimal] = None,
                             taxTakenOff: Option[Boolean] = None,
                             taxTakenOffAmount: Option[BigDecimal] = None
                           ){
  def fixGatewayData: SavingsIncomeCYAModel = {
    gateway.fold(this){ gatewayValue =>
      if (gatewayValue) this else SavingsIncomeCYAModel(Some(false), None, None, None)
    }
  }
  def fixTaxTakenOffData: SavingsIncomeCYAModel = {
    if (taxTakenOff.contains(false)) this.copy(taxTakenOffAmount = None) else this
  }

  def isFinished: Boolean = {
    (((taxTakenOff.contains(false) && taxTakenOffAmount.isEmpty) ||
    (taxTakenOff.contains(true) && taxTakenOffAmount.isDefined)) &&
    (gateway.contains(true) && grossAmount.isDefined)) ||
      (gateway.contains(false) && taxTakenOff.contains(false) && taxTakenOffAmount.isEmpty && grossAmount.isEmpty)
  }

  def getNextInJourney(taxYear: Int)(implicit appConfig: AppConfig): Result = {this match {
    case SavingsIncomeCYAModel(None, _, _, _) => Redirect(controllers.savings.routes.SavingsGatewayController.show(taxYear))
    case SavingsIncomeCYAModel(Some(value), None, _, _) => Redirect(controllers.savings.routes.SavingsInterestAmountController.show(taxYear))
    case SavingsIncomeCYAModel(Some(_), Some(_), None, _) => Redirect(controllers.savings.routes.TaxTakenFromInterestController.show(taxYear))
    case SavingsIncomeCYAModel(Some(_), Some(_), Some(true), None) => Redirect(controllers.savings.routes.TaxTakenOffInterestController.show(taxYear))
    case _ => Redirect(appConfig.incomeTaxSubmissionOverviewUrl(taxYear))
  }
  }

}

object SavingsIncomeCYAModel{

  implicit val formats: OFormat[SavingsIncomeCYAModel] = Json.format[SavingsIncomeCYAModel]
}

case class EncryptedSavingsIncomeCYAModel(
                                      gateway: Option[EncryptedValue] = None,
                                      grossAmount: Option[EncryptedValue] = None,
                                      taxTakenOff: Option[EncryptedValue] = None,
                                      taxTakenOffAmount: Option[EncryptedValue] = None
                                    )
object EncryptedSavingsIncomeCYAModel{
  implicit lazy val encryptedValueOFormat: OFormat[EncryptedValue] = Json.format[EncryptedValue]
  implicit val formats: OFormat[EncryptedSavingsIncomeCYAModel] = Json.format[EncryptedSavingsIncomeCYAModel]
}
