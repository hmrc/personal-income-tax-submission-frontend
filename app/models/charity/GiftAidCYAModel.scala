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

case class GiftAidCYAModel(
                            donationsViaGiftAid: Option[Boolean] = None,
                            donationsViaGiftAidAmount: Option[BigDecimal] = None,
                            oneOffDonationsViaGiftAid: Option[Boolean] = None,
                            oneOffDonationsViaGiftAidAmount: Option[BigDecimal] = None,
                            overseasDonationsViaGiftAid: Option[Boolean] = None,
                            overseasDonationsViaGiftAidAmount: Option[BigDecimal] = None,
                            overseasCharityNames: Option[Seq[String]] = Some(Seq.empty[String]),
                            addDonationToLastYear: Option[Boolean] = None,
                            addDonationToLastYearAmount: Option[BigDecimal] = None,
                            addDonationToThisYear: Option[Boolean] = None,
                            addDonationToThisYearAmount: Option[BigDecimal] = None,
                            donatedSharesSecuritiesLandOrProperty: Option[Boolean] = None,
                            donatedSharesOrSecurities: Option[Boolean] = None,
                            donatedSharesOrSecuritiesAmount: Option[BigDecimal] = None,
                            donatedLandOrProperty: Option[Boolean] = None,
                            donatedLandOrPropertyAmount: Option[BigDecimal] = None,
                            overseasDonatedSharesSecuritiesLandOrProperty: Option[Boolean] = None,
                            overseasDonatedSharesSecuritiesLandOrPropertyAmount: Option[BigDecimal] = None,
                            overseasDonatedSharesSecuritiesLandOrPropertyCharityNames: Option[Seq[String]] = Some(Seq.empty[String])
                          ) {

  private def falseOrTrueAndAmountPopulated(boolField: Option[Boolean], amountField: Option[BigDecimal]) = {
    boolField.forall(value => !value || (value && amountField.nonEmpty))
  }

  def asJsonString: String = {
    Json.prettyPrint(Json.toJson(this))
  }

  //noinspection ScalaStyle
  def isFinished: Boolean = {
    val b_allRequiredYesNoFilledIn = donationsViaGiftAid.nonEmpty && oneOffDonationsViaGiftAid.nonEmpty && overseasDonationsViaGiftAid.nonEmpty &&
      addDonationToLastYear.nonEmpty && addDonationToThisYear.nonEmpty && donatedSharesSecuritiesLandOrProperty.nonEmpty && overseasDonatedSharesSecuritiesLandOrProperty.nonEmpty

    val b_donationsViaGiftAid = falseOrTrueAndAmountPopulated(donationsViaGiftAid, donationsViaGiftAidAmount)
    val b_oneOffDonationsViaGiftAid = falseOrTrueAndAmountPopulated(oneOffDonationsViaGiftAid, oneOffDonationsViaGiftAidAmount)
    val b_overseasDonationsViaGiftAid = overseasDonationsViaGiftAid.forall(value => !value || (value && overseasDonationsViaGiftAidAmount.nonEmpty && overseasCharityNames.nonEmpty))
    val b_addDonationToLastYear = falseOrTrueAndAmountPopulated(addDonationToLastYear, addDonationToLastYearAmount)
    val b_addDonationToThisYear = falseOrTrueAndAmountPopulated(addDonationToThisYear, addDonationToThisYearAmount)

    val b_donatedSharesSecuritiesLandOrProperty: Boolean = donatedSharesSecuritiesLandOrProperty.contains(true)
    val b_donatedSharesOrSecurities = donatedSharesOrSecurities.forall(value => !value || (value && donatedSharesOrSecuritiesAmount.nonEmpty && b_donatedSharesSecuritiesLandOrProperty))
    val b_donatedLandOrProperty = donatedLandOrProperty.forall(value => !value || (value && donatedLandOrPropertyAmount.nonEmpty && b_donatedSharesSecuritiesLandOrProperty))
    val b_overseasDonatedSharesSecurityLandOrProperty = overseasDonatedSharesSecuritiesLandOrProperty.forall( value =>
      !value || (value && overseasDonatedSharesSecuritiesLandOrPropertyAmount.nonEmpty && overseasDonatedSharesSecuritiesLandOrPropertyCharityNames.nonEmpty)
    )

    val b_requiredNameFieldsExist = overseasDonationsViaGiftAid.forall(value => !value || (value && overseasCharityNames.exists(_.nonEmpty))) &&
      overseasDonatedSharesSecuritiesLandOrProperty.forall(value => !value || (value && overseasDonatedSharesSecuritiesLandOrPropertyCharityNames.exists(_.nonEmpty)))

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

}

object GiftAidCYAModel {
  implicit val formats: OFormat[GiftAidCYAModel] = Json.format[GiftAidCYAModel]
}
