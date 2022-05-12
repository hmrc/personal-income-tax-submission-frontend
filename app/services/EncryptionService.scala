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

package services

import config.AppConfig
import models.charity.{CharityNameModel, EncryptedCharityNameModel, EncryptedGiftAidCYAModel, GiftAidCYAModel}
import models.dividends.{DividendsCheckYourAnswersModel, EncryptedDividendsCheckYourAnswersModel}
import models.interest.{EncryptedInterestAccountModel, EncryptedInterestCYAModel, InterestAccountModel, InterestCYAModel}
import models.mongo._
import utils.SecureGCMCipher

import javax.inject.Inject

class EncryptionService @Inject()(encryptionService: SecureGCMCipher, appConfig: AppConfig) {

  // DIVIDENDS
  def encryptDividendsUserData(dividendsUserDataModel: DividendsUserDataModel): EncryptedDividendsUserDataModel ={
    implicit val textAndKey: TextAndKey = TextAndKey(dividendsUserDataModel.mtdItId,appConfig.encryptionKey)

    EncryptedDividendsUserDataModel(
      sessionId = dividendsUserDataModel.sessionId,
      mtdItId = dividendsUserDataModel.mtdItId,
      nino = dividendsUserDataModel.nino,
      taxYear = dividendsUserDataModel.taxYear,
      dividends = dividendsUserDataModel.dividends.map(encryptDividendsCheckYourAnswersModel),
      lastUpdated = dividendsUserDataModel.lastUpdated
    )
  }
  def decryptDividendsUserData(encryptedDividendsUserDataModel: EncryptedDividendsUserDataModel): DividendsUserDataModel ={
    implicit val textAndKey: TextAndKey = TextAndKey(encryptedDividendsUserDataModel.mtdItId,appConfig.encryptionKey)

    DividendsUserDataModel(
      sessionId = encryptedDividendsUserDataModel.sessionId,
      mtdItId = encryptedDividendsUserDataModel.mtdItId,
      nino = encryptedDividendsUserDataModel.nino,
      taxYear = encryptedDividendsUserDataModel.taxYear,
      dividends = encryptedDividendsUserDataModel.dividends.map(decryptDividendsCheckYourAnswersModel),
      lastUpdated = encryptedDividendsUserDataModel.lastUpdated
    )
  }

  private def encryptDividendsCheckYourAnswersModel(dividends: DividendsCheckYourAnswersModel)
                                                   (implicit textAndKey: TextAndKey): EncryptedDividendsCheckYourAnswersModel ={
    EncryptedDividendsCheckYourAnswersModel(
      dividends.gateway.map(x => encryptionService.encrypt[Boolean](x)),
      dividends.ukDividends.map(x => encryptionService.encrypt[Boolean](x)),
      dividends.ukDividendsAmount.map(x => encryptionService.encrypt[BigDecimal](x)),
      dividends.otherUkDividends.map(x => encryptionService.encrypt[Boolean](x)),
      dividends.otherUkDividendsAmount.map(x => encryptionService.encrypt[BigDecimal](x))
    )
  }

  private def decryptDividendsCheckYourAnswersModel(dividends: EncryptedDividendsCheckYourAnswersModel)
                                                   (implicit textAndKey: TextAndKey): DividendsCheckYourAnswersModel ={
    DividendsCheckYourAnswersModel(
      dividends.gateway.map(x => encryptionService.decrypt[Boolean](x.value, x.nonce)),
      dividends.ukDividends.map(x => encryptionService.decrypt[Boolean](x.value, x.nonce)),
      dividends.ukDividendsAmount.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
      dividends.otherUkDividends.map(x => encryptionService.decrypt[Boolean](x.value,x.nonce)),
      dividends.otherUkDividendsAmount.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce))
    )
  }

  // GiftAid/Charity
  def encryptGiftAidUserData(giftAidUserDataModel: GiftAidUserDataModel): EncryptedGiftAidUserDataModel ={
    implicit val textAndKey: TextAndKey = TextAndKey(giftAidUserDataModel.mtdItId,appConfig.encryptionKey)

    EncryptedGiftAidUserDataModel(
      sessionId = giftAidUserDataModel.sessionId,
      mtdItId = giftAidUserDataModel.mtdItId,
      nino = giftAidUserDataModel.nino,
      taxYear = giftAidUserDataModel.taxYear,
      giftAid = giftAidUserDataModel.giftAid.map(encryptGiftAidCheckYourAnswersModel),
      lastUpdated = giftAidUserDataModel.lastUpdated
    )
  }

  def decryptGiftAidUserData(giftAidUserDataModel: EncryptedGiftAidUserDataModel): GiftAidUserDataModel ={
    implicit val textAndKey: TextAndKey = TextAndKey(giftAidUserDataModel.mtdItId, appConfig.encryptionKey)

    GiftAidUserDataModel(
      sessionId = giftAidUserDataModel.sessionId,
      mtdItId = giftAidUserDataModel.mtdItId,
      nino = giftAidUserDataModel.nino,
      taxYear = giftAidUserDataModel.taxYear,
      giftAid = giftAidUserDataModel.giftAid.map(decryptGiftAidCheckYourAnswersModel),
      lastUpdated = giftAidUserDataModel.lastUpdated
    )
  }

  private def encryptGiftAidCheckYourAnswersModel(giftAid: GiftAidCYAModel)
                                                   (implicit textAndKey: TextAndKey): EncryptedGiftAidCYAModel ={
    EncryptedGiftAidCYAModel(
      giftAid.donationsViaGiftAid.map(x => encryptionService.encrypt[Boolean](x)),
      giftAid.donationsViaGiftAidAmount.map(x => encryptionService.encrypt[BigDecimal](x)),
      giftAid.oneOffDonationsViaGiftAid.map(x => encryptionService.encrypt[Boolean](x)),
      giftAid.oneOffDonationsViaGiftAidAmount.map(x => encryptionService.encrypt[BigDecimal](x)),
      giftAid.overseasDonationsViaGiftAid.map(x => encryptionService.encrypt[Boolean](x)),
      giftAid.overseasDonationsViaGiftAidAmount.map(x => encryptionService.encrypt[BigDecimal](x)),
      giftAid.overseasCharityNames.map(encryptCharityNameModel),
      giftAid.addDonationToLastYear.map(x => encryptionService.encrypt[Boolean](x)),
      giftAid.addDonationToLastYearAmount.map(x => encryptionService.encrypt[BigDecimal](x)),
      giftAid.addDonationToThisYear.map(x => encryptionService.encrypt[Boolean](x)),
      giftAid.addDonationToThisYearAmount.map(x => encryptionService.encrypt[BigDecimal](x)),
      giftAid.donatedSharesOrSecurities.map(x => encryptionService.encrypt[Boolean](x)),
      giftAid.donatedSharesOrSecuritiesAmount.map(x => encryptionService.encrypt[BigDecimal](x)),
      giftAid.donatedLandOrProperty.map(x => encryptionService.encrypt[Boolean](x)),
      giftAid.donatedLandOrPropertyAmount.map(x => encryptionService.encrypt[BigDecimal](x)),
      giftAid.overseasDonatedSharesSecuritiesLandOrProperty.map(x => encryptionService.encrypt[Boolean](x)),
      giftAid.overseasDonatedSharesSecuritiesLandOrPropertyAmount.map(x => encryptionService.encrypt[BigDecimal](x)),
      giftAid.overseasDonatedSharesSecuritiesLandOrPropertyCharityNames.map(encryptCharityNameModel)
    )
  }

  private def decryptGiftAidCheckYourAnswersModel(giftAid: EncryptedGiftAidCYAModel)
                                                   (implicit textAndKey: TextAndKey): GiftAidCYAModel ={
    GiftAidCYAModel(
      giftAid.donationsViaGiftAid.map(x => encryptionService.decrypt[Boolean](x.value, x.nonce)),
      giftAid.donationsViaGiftAidAmount.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
      giftAid.oneOffDonationsViaGiftAid.map(x => encryptionService.decrypt[Boolean](x.value, x.nonce)),
      giftAid.oneOffDonationsViaGiftAidAmount.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
      giftAid.overseasDonationsViaGiftAid.map(x => encryptionService.decrypt[Boolean](x.value, x.nonce)),
      giftAid.overseasDonationsViaGiftAidAmount.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
      giftAid.overseasCharityNames.map(decryptCharityNameModel),
      giftAid.addDonationToLastYear.map(x => encryptionService.decrypt[Boolean](x.value, x.nonce)),
      giftAid.addDonationToLastYearAmount.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
      giftAid.addDonationToThisYear.map(x => encryptionService.decrypt[Boolean](x.value, x.nonce)),
      giftAid.addDonationToThisYearAmount.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
      giftAid.donatedSharesOrSecurities.map(x => encryptionService.decrypt[Boolean](x.value, x.nonce)),
      giftAid.donatedSharesOrSecuritiesAmount.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
      giftAid.donatedLandOrProperty.map(x => encryptionService.decrypt[Boolean](x.value, x.nonce)),
      giftAid.donatedLandOrPropertyAmount.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
      giftAid.overseasDonatedSharesSecuritiesLandOrProperty.map(x => encryptionService.decrypt[Boolean](x.value, x.nonce)),
      giftAid.overseasDonatedSharesSecuritiesLandOrPropertyAmount.map(x => encryptionService.decrypt[BigDecimal](x.value,x.nonce)),
      giftAid.overseasDonatedSharesSecuritiesLandOrPropertyCharityNames.map(decryptCharityNameModel)
    )
  }

  private def encryptCharityNameModel(charityNameModel: CharityNameModel)
                                                 (implicit textAndKey: TextAndKey): EncryptedCharityNameModel ={
    EncryptedCharityNameModel(
      encryptionService.encrypt[String](charityNameModel.name),
      encryptionService.encrypt[String](charityNameModel.id)
    )
  }
  private def decryptCharityNameModel(charityNameModel: EncryptedCharityNameModel)
                                                 (implicit textAndKey: TextAndKey): CharityNameModel ={
    CharityNameModel(
      encryptionService.decrypt[String](charityNameModel.name.value, charityNameModel.name.nonce),
      encryptionService.decrypt[String](charityNameModel.id.value, charityNameModel.id.nonce)
    )
  }

  // INTERESTS
  def encryptInterestUserData(interestUserDataModel: InterestUserDataModel): EncryptedInterestUserDataModel ={
    implicit val textAndKey: TextAndKey = TextAndKey(interestUserDataModel.mtdItId,appConfig.encryptionKey)

    EncryptedInterestUserDataModel(
      sessionId = interestUserDataModel.sessionId,
      mtdItId = interestUserDataModel.mtdItId,
      nino = interestUserDataModel.nino,
      taxYear = interestUserDataModel.taxYear,
      interest = interestUserDataModel.interest.map(encryptInterestCheckYourAnswersModel),
      lastUpdated = interestUserDataModel.lastUpdated
    )
  }
  def decryptInterestUserData(encryptedinterestUserDataModel: EncryptedInterestUserDataModel): InterestUserDataModel ={
    implicit val textAndKey: TextAndKey = TextAndKey(encryptedinterestUserDataModel.mtdItId,appConfig.encryptionKey)

    InterestUserDataModel(
      sessionId = encryptedinterestUserDataModel.sessionId,
      mtdItId = encryptedinterestUserDataModel.mtdItId,
      nino = encryptedinterestUserDataModel.nino,
      taxYear = encryptedinterestUserDataModel.taxYear,
      interest = encryptedinterestUserDataModel.interest.map(decryptInterestCheckYourAnswersModel),
      lastUpdated = encryptedinterestUserDataModel.lastUpdated
    )
  }

  private def encryptInterestCheckYourAnswersModel(interests: InterestCYAModel)
                                                   (implicit textAndKey: TextAndKey): EncryptedInterestCYAModel ={
    EncryptedInterestCYAModel(
      interests.gateway.map(x => encryptionService.encrypt[Boolean](x)),
      interests.untaxedUkInterest.map(x => encryptionService.encrypt[Boolean](x)),
      interests.taxedUkInterest.map(x => encryptionService.encrypt[Boolean](x)),
      interests.accounts.map(encryptInterestAccountModel)
    )
  }

  private def decryptInterestCheckYourAnswersModel(interests: EncryptedInterestCYAModel)
                                                   (implicit textAndKey: TextAndKey): InterestCYAModel ={
    InterestCYAModel(
      interests.gateway.map(x => encryptionService.decrypt[Boolean](x.value, x.nonce)),
      interests.untaxedUkInterest.map(x => encryptionService.decrypt[Boolean](x.value, x.nonce)),
      interests.taxedUkInterest.map(x => encryptionService.decrypt[Boolean](x.value, x.nonce)),
      interests.accounts.map(decryptInterestAccountModel)
    )
  }

  private def encryptInterestAccountModel(interestAccount: InterestAccountModel)
                                     (implicit textAndKey: TextAndKey): EncryptedInterestAccountModel ={
    EncryptedInterestAccountModel(
      interestAccount.id.map(x => encryptionService.encrypt[String](x)),
      encryptionService.encrypt[String](interestAccount.accountName),
      interestAccount.untaxedAmount.map(x => encryptionService.encrypt[BigDecimal](x)),
      interestAccount.taxedAmount.map(x => encryptionService.encrypt[BigDecimal](x)),
      interestAccount.uniqueSessionId.map(x => encryptionService.encrypt[String](x))
    )
  }
  private def decryptInterestAccountModel(interestAccount: EncryptedInterestAccountModel)
                                     (implicit textAndKey: TextAndKey): InterestAccountModel ={
    InterestAccountModel(
      interestAccount.id.map(x => encryptionService.decrypt[String](x.value, x.nonce)),
      encryptionService.decrypt[String](interestAccount.accountName.value, interestAccount.accountName.nonce),
      interestAccount.untaxedAmount.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
      interestAccount.taxedAmount.map(x => encryptionService.decrypt[BigDecimal](x.value, x.nonce)),
      interestAccount.uniqueSessionId.map(x => encryptionService.decrypt[String](x.value, x.nonce))
    )
  }

}
