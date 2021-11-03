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

package models.charity

import play.api.libs.json.{Json, OFormat}
import utils.EncryptedValue

case class GiftAidCYAModel(
                            donationsViaGiftAid: Option[Boolean] = None,
                            donationsViaGiftAidAmount: Option[BigDecimal] = None,
                            oneOffDonationsViaGiftAid: Option[Boolean] = None,
                            oneOffDonationsViaGiftAidAmount: Option[BigDecimal] = None,
                            overseasDonationsViaGiftAid: Option[Boolean] = None,
                            overseasDonationsViaGiftAidAmount: Option[BigDecimal] = None,
                            overseasCharityNames: Seq[CharityNameModel] = Seq.empty,
                            addDonationToLastYear: Option[Boolean] = None,
                            addDonationToLastYearAmount: Option[BigDecimal] = None,
                            addDonationToThisYear: Option[Boolean] = None,
                            addDonationToThisYearAmount: Option[BigDecimal] = None,
                            donatedSharesOrSecurities: Option[Boolean] = None,
                            donatedSharesOrSecuritiesAmount: Option[BigDecimal] = None,
                            donatedLandOrProperty: Option[Boolean] = None,
                            donatedLandOrPropertyAmount: Option[BigDecimal] = None,
                            overseasDonatedSharesSecuritiesLandOrProperty: Option[Boolean] = None,
                            overseasDonatedSharesSecuritiesLandOrPropertyAmount: Option[BigDecimal] = None,
                            overseasDonatedSharesSecuritiesLandOrPropertyCharityNames: Seq[CharityNameModel] = Seq.empty
                          ) {

  private def falseOrTrueAndAmountPopulated(boolField: Option[Boolean], amountField: Option[BigDecimal]) = {
    boolField.forall(value => !value || (value && amountField.nonEmpty))
  }

  def asJsonString: String = {
    Json.prettyPrint(Json.toJson(this))
  }

  //noinspection ScalaStyle
  def isFinished: Boolean = {
    val b_allRequiredYesNoFilledIn = hasAllRequiredAnswers

    val b_donationsViaGiftAid = falseOrTrueAndAmountPopulated(donationsViaGiftAid, donationsViaGiftAidAmount)
    val b_oneOffDonationsViaGiftAid = falseOrTrueAndAmountPopulated(oneOffDonationsViaGiftAid, oneOffDonationsViaGiftAidAmount)
    val b_overseasDonationsViaGiftAid = overseasDonationsViaGiftAid.forall(value => !value || (value && overseasDonationsViaGiftAidAmount.nonEmpty && overseasCharityNames.nonEmpty))
    val b_addDonationToLastYear = falseOrTrueAndAmountPopulated(addDonationToLastYear, addDonationToLastYearAmount)
    val b_addDonationToThisYear = falseOrTrueAndAmountPopulated(addDonationToThisYear, addDonationToThisYearAmount)
    val b_donatedSharesOrSecurities = donatedSharesOrSecurities.forall(value => !value || (value && donatedSharesOrSecuritiesAmount.nonEmpty))
    val b_donatedLandOrProperty = donatedLandOrProperty.forall(value => !value || (value && donatedLandOrPropertyAmount.nonEmpty))
    val b_overseasDonatedSharesSecurityLandOrProperty = overseasDonatedSharesSecuritiesLandOrProperty.forall(value =>
      !value || (value && overseasDonatedSharesSecuritiesLandOrPropertyAmount.nonEmpty && overseasDonatedSharesSecuritiesLandOrPropertyCharityNames.nonEmpty)
    )

    val b_requiredNameFieldsExist = overseasDonationsViaGiftAid.forall(value => !value || (value && overseasCharityNames.nonEmpty)) &&
      overseasDonatedSharesSecuritiesLandOrProperty.forall(value => !value || (value && overseasDonatedSharesSecuritiesLandOrPropertyCharityNames.nonEmpty))

    Seq(
      b_allRequiredYesNoFilledIn,
      b_donationsViaGiftAid,
      b_oneOffDonationsViaGiftAid,
      b_overseasDonationsViaGiftAid,
      b_addDonationToLastYear,
      b_addDonationToThisYear,
      b_donatedSharesOrSecurities,
      b_donatedLandOrProperty,
      b_overseasDonatedSharesSecurityLandOrProperty,
      b_requiredNameFieldsExist
    ).forall(_ == true)
  }

  def hasAllRequiredAnswers: Boolean = addDonationToThisYear.nonEmpty &&
    donationsViaGiftAidCompleted &&
    donatedSharesOrSecuritiesCompleted

  def donatedSharesOrSecuritiesCompleted: Boolean = {
      ((donatedSharesOrSecurities.contains(true) || donatedLandOrProperty.contains(true)) && overseasDonatedSharesSecuritiesLandOrProperty.nonEmpty) ||
        (donatedSharesOrSecurities.contains(false) && donatedLandOrProperty.contains(false) && overseasDonatedSharesSecuritiesLandOrProperty.isEmpty)
  }

  def donationsViaGiftAidCompleted: Boolean = {
    (donationsViaGiftAid.contains(true) && oneOffDonationsViaGiftAid.nonEmpty && overseasDonationsViaGiftAid.nonEmpty && addDonationToLastYear.nonEmpty) ||
      (donationsViaGiftAid.contains(false) && oneOffDonationsViaGiftAid.isEmpty && overseasDonationsViaGiftAid.isEmpty && addDonationToLastYear.isEmpty)
  }
}

object GiftAidCYAModel {
  implicit val formats: OFormat[GiftAidCYAModel] = Json.format[GiftAidCYAModel]

  def resetDonatedSharesSecuritiesLandOrProperty(cyaData: GiftAidCYAModel): GiftAidCYAModel = {
    cyaData.copy(
      donatedSharesOrSecurities = Some(false),
      donatedSharesOrSecuritiesAmount = None,
      donatedLandOrProperty = Some(false),
      donatedLandOrPropertyAmount = None,
      overseasDonatedSharesSecuritiesLandOrProperty = None,
      overseasDonatedSharesSecuritiesLandOrPropertyAmount = None,
      overseasDonatedSharesSecuritiesLandOrPropertyCharityNames = Seq.empty
    )
  }
}

case class EncryptedGiftAidCYAModel(
                                     donationsViaGiftAid: Option[EncryptedValue] = None,
                                     donationsViaGiftAidAmount: Option[EncryptedValue] = None,
                                     oneOffDonationsViaGiftAid: Option[EncryptedValue] = None,
                                     oneOffDonationsViaGiftAidAmount: Option[EncryptedValue] = None,
                                     overseasDonationsViaGiftAid: Option[EncryptedValue] = None,
                                     overseasDonationsViaGiftAidAmount: Option[EncryptedValue] = None,
                                     overseasCharityNames: Seq[EncryptedCharityNameModel] = Seq.empty,
                                     addDonationToLastYear: Option[EncryptedValue] = None,
                                     addDonationToLastYearAmount: Option[EncryptedValue] = None,
                                     addDonationToThisYear: Option[EncryptedValue] = None,
                                     addDonationToThisYearAmount: Option[EncryptedValue] = None,
                                     donatedSharesOrSecurities: Option[EncryptedValue] = None,
                                     donatedSharesOrSecuritiesAmount: Option[EncryptedValue] = None,
                                     donatedLandOrProperty: Option[EncryptedValue] = None,
                                     donatedLandOrPropertyAmount: Option[EncryptedValue] = None,
                                     overseasDonatedSharesSecuritiesLandOrProperty: Option[EncryptedValue] = None,
                                     overseasDonatedSharesSecuritiesLandOrPropertyAmount: Option[EncryptedValue] = None,
                                     overseasDonatedSharesSecuritiesLandOrPropertyCharityNames: Seq[EncryptedCharityNameModel] = Seq.empty
                          )

object EncryptedGiftAidCYAModel {
  implicit val formats: OFormat[EncryptedGiftAidCYAModel] = Json.format[EncryptedGiftAidCYAModel]
}
