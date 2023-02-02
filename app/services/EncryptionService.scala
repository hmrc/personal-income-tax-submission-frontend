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

package services

import models.charity.{CharityNameModel, EncryptedCharityNameModel, EncryptedGiftAidCYAModel, GiftAidCYAModel}
import models.dividends.{DividendsCheckYourAnswersModel, EncryptedDividendsCheckYourAnswersModel}
import models.interest.{EncryptedInterestAccountModel, EncryptedInterestCYAModel, InterestAccountModel, InterestCYAModel}
import models.mongo._
import utils.AesGcmAdCrypto
import utils.CypherSyntax.{DecryptableOps, EncryptableOps}
import models.savings.{EncryptedSavingsIncomeCYAModel, SavingsIncomeCYAModel}


import javax.inject.Inject

class EncryptionService @Inject()(implicit encryptionService: AesGcmAdCrypto) {

  // DIVIDENDS
  def encryptDividendsUserData(dividendsUserDataModel: DividendsUserDataModel): EncryptedDividendsUserDataModel ={
    implicit val associatedText: String = dividendsUserDataModel.mtdItId

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
    implicit val associatedText: String = encryptedDividendsUserDataModel.mtdItId

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
                                                   (implicit associatedText: String): EncryptedDividendsCheckYourAnswersModel ={
    EncryptedDividendsCheckYourAnswersModel(
      dividends.gateway.map(_.encrypted),
      dividends.ukDividends.map(_.encrypted),
      dividends.ukDividendsAmount.map(_.encrypted),
      dividends.otherUkDividends.map(_.encrypted),
      dividends.otherUkDividendsAmount.map(_.encrypted)
    )
  }

  private def decryptDividendsCheckYourAnswersModel(dividends: EncryptedDividendsCheckYourAnswersModel)
                                                   (implicit associatedText: String): DividendsCheckYourAnswersModel ={
    DividendsCheckYourAnswersModel(
      dividends.gateway.map(_.decrypted[Boolean]),
      dividends.ukDividends.map(_.decrypted[Boolean]),
      dividends.ukDividendsAmount.map(_.decrypted[BigDecimal]),
      dividends.otherUkDividends.map(_.decrypted[Boolean]),
      dividends.otherUkDividendsAmount.map(_.decrypted[BigDecimal])
    )
  }

  // GiftAid/Charity
  def encryptGiftAidUserData(giftAidUserDataModel: GiftAidUserDataModel): EncryptedGiftAidUserDataModel ={
    implicit val associatedText: String = giftAidUserDataModel.mtdItId

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
    implicit val associatedText: String = giftAidUserDataModel.mtdItId

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
                                                   (implicit associatedText: String): EncryptedGiftAidCYAModel ={
    EncryptedGiftAidCYAModel(
      giftAid.gateway.map(_.encrypted),
      giftAid.donationsViaGiftAid.map(_.encrypted),
      giftAid.donationsViaGiftAidAmount.map(_.encrypted),
      giftAid.oneOffDonationsViaGiftAid.map(_.encrypted),
      giftAid.oneOffDonationsViaGiftAidAmount.map(_.encrypted),
      giftAid.overseasDonationsViaGiftAid.map(_.encrypted),
      giftAid.overseasDonationsViaGiftAidAmount.map(_.encrypted),
      giftAid.overseasCharityNames.map(encryptCharityNameModel),
      giftAid.addDonationToLastYear.map(_.encrypted),
      giftAid.addDonationToLastYearAmount.map(_.encrypted),
      giftAid.addDonationToThisYear.map(_.encrypted),
      giftAid.addDonationToThisYearAmount.map(_.encrypted),
      giftAid.donatedSharesOrSecurities.map(_.encrypted),
      giftAid.donatedSharesOrSecuritiesAmount.map(_.encrypted),
      giftAid.donatedLandOrProperty.map(_.encrypted),
      giftAid.donatedLandOrPropertyAmount.map(_.encrypted),
      giftAid.overseasDonatedSharesSecuritiesLandOrProperty.map(_.encrypted),
      giftAid.overseasDonatedSharesSecuritiesLandOrPropertyAmount.map(_.encrypted),
      giftAid.overseasDonatedSharesSecuritiesLandOrPropertyCharityNames.map(encryptCharityNameModel)
    )
  }

  private def decryptGiftAidCheckYourAnswersModel(giftAid: EncryptedGiftAidCYAModel)
                                                   (implicit associatedText: String): GiftAidCYAModel ={
    GiftAidCYAModel(
      giftAid.gateway.map(_.decrypted[Boolean]),
      giftAid.donationsViaGiftAid.map(_.decrypted[Boolean]),
      giftAid.donationsViaGiftAidAmount.map(_.decrypted[BigDecimal]),
      giftAid.oneOffDonationsViaGiftAid.map(_.decrypted[Boolean]),
      giftAid.oneOffDonationsViaGiftAidAmount.map(_.decrypted[BigDecimal]),
      giftAid.overseasDonationsViaGiftAid.map(_.decrypted[Boolean]),
      giftAid.overseasDonationsViaGiftAidAmount.map(_.decrypted[BigDecimal]),
      giftAid.overseasCharityNames.map(decryptCharityNameModel),
      giftAid.addDonationToLastYear.map(_.decrypted[Boolean]),
      giftAid.addDonationToLastYearAmount.map(_.decrypted[BigDecimal]),
      giftAid.addDonationToThisYear.map(_.decrypted[Boolean]),
      giftAid.addDonationToThisYearAmount.map(_.decrypted[BigDecimal]),
      giftAid.donatedSharesOrSecurities.map(_.decrypted[Boolean]),
      giftAid.donatedSharesOrSecuritiesAmount.map(_.decrypted[BigDecimal]),
      giftAid.donatedLandOrProperty.map(_.decrypted[Boolean]),
      giftAid.donatedLandOrPropertyAmount.map(_.decrypted[BigDecimal]),
      giftAid.overseasDonatedSharesSecuritiesLandOrProperty.map(_.decrypted[Boolean]),
      giftAid.overseasDonatedSharesSecuritiesLandOrPropertyAmount.map(_.decrypted[BigDecimal]),
      giftAid.overseasDonatedSharesSecuritiesLandOrPropertyCharityNames.map(decryptCharityNameModel)
    )
  }

  private def encryptCharityNameModel(charityNameModel: CharityNameModel)
                                                 (implicit associatedText: String): EncryptedCharityNameModel ={
    EncryptedCharityNameModel(
      charityNameModel.name.encrypted,
      charityNameModel.id.encrypted
    )
  }
  private def decryptCharityNameModel(charityNameModel: EncryptedCharityNameModel)
                                                 (implicit associatedText: String): CharityNameModel ={
    CharityNameModel(
      charityNameModel.name.decrypted[String],
      charityNameModel.id.decrypted[String]
    )
  }

  // INTERESTS
  def encryptInterestUserData(interestUserDataModel: InterestUserDataModel): EncryptedInterestUserDataModel ={
    implicit val associatedText: String = interestUserDataModel.mtdItId

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
    implicit val associatedText: String = encryptedinterestUserDataModel.mtdItId

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
                                                   (implicit associatedText: String): EncryptedInterestCYAModel ={
    EncryptedInterestCYAModel(
      interests.gateway.map(_.encrypted),
      interests.untaxedUkInterest.map(_.encrypted),
      interests.taxedUkInterest.map(_.encrypted),
      interests.accounts.map(encryptInterestAccountModel)
    )
  }

  private def decryptInterestCheckYourAnswersModel(interests: EncryptedInterestCYAModel)
                                                   (implicit associatedText: String): InterestCYAModel ={
    InterestCYAModel(
      interests.gateway.map(_.decrypted[Boolean]),
      interests.untaxedUkInterest.map(_.decrypted[Boolean]),
      interests.taxedUkInterest.map(_.decrypted[Boolean]),
      interests.accounts.map(decryptInterestAccountModel)
    )
  }

  private def encryptInterestAccountModel(interestAccount: InterestAccountModel)
                                     (implicit associatedText: String): EncryptedInterestAccountModel ={
    EncryptedInterestAccountModel(
      interestAccount.id.map(_.encrypted),
      interestAccount.accountName.encrypted,
      interestAccount.untaxedAmount.map(_.encrypted),
      interestAccount.taxedAmount.map(_.encrypted),
      interestAccount.uniqueSessionId.map(_.encrypted)
    )
  }
  private def decryptInterestAccountModel(interestAccount: EncryptedInterestAccountModel)
                                     (implicit associatedText: String): InterestAccountModel ={
    InterestAccountModel(
      interestAccount.id.map(_.decrypted[String]),
      interestAccount.accountName.decrypted[String],
      interestAccount.untaxedAmount.map(_.decrypted[BigDecimal]),
      interestAccount.taxedAmount.map(_.decrypted[BigDecimal]),
      interestAccount.uniqueSessionId.map(_.decrypted[String])
    )
  }

  private def encryptSavingsIncomeCheckYourAnswersModel(savings: SavingsIncomeCYAModel)
                                                       (implicit associatedText: String): EncryptedSavingsIncomeCYAModel = {
    EncryptedSavingsIncomeCYAModel(
      savings.gateway.map(_.encrypted),
      savings.grossAmount.map(_.encrypted),
      savings.taxTakenOff.map(_.encrypted),
      savings.taxTakenOffAmount.map(_.encrypted)
    )
  }

  private def decryptSavingsIncomeCheckYourAnswersModel(savings: EncryptedSavingsIncomeCYAModel)
                                                       (implicit associatedText: String): SavingsIncomeCYAModel = {
    SavingsIncomeCYAModel(
      savings.gateway.map(_.decrypted[Boolean]),
      savings.grossAmount.map(_.decrypted[BigDecimal]),
      savings.taxTakenOff.map(_.decrypted[Boolean]),
      savings.taxTakenOffAmount.map(_.decrypted[BigDecimal])
    )
  }

  def encryptSavingsIncomeUserData(savingsIncomeUserDataModel: SavingsIncomeUserDataModel): EncryptedSavingsIncomeUserDataModel = {
    implicit val associatedText: String = savingsIncomeUserDataModel.mtdItId

    EncryptedSavingsIncomeUserDataModel(
      sessionId = savingsIncomeUserDataModel.sessionId,
      mtdItId = savingsIncomeUserDataModel.mtdItId,
      nino = savingsIncomeUserDataModel.nino,
      taxYear = savingsIncomeUserDataModel.taxYear,
      savingsIncome = savingsIncomeUserDataModel.savingsIncome.map(encryptSavingsIncomeCheckYourAnswersModel),
      lastUpdated = savingsIncomeUserDataModel.lastUpdated
    )
  }

  def decryptSavingsIncomeUserData(encryptedSavingsIncomeUserDataModel: EncryptedSavingsIncomeUserDataModel): SavingsIncomeUserDataModel = {
    implicit val associatedText: String = encryptedSavingsIncomeUserDataModel.mtdItId

    SavingsIncomeUserDataModel(
      sessionId = encryptedSavingsIncomeUserDataModel.sessionId,
      mtdItId = encryptedSavingsIncomeUserDataModel.mtdItId,
      nino = encryptedSavingsIncomeUserDataModel.nino,
      taxYear = encryptedSavingsIncomeUserDataModel.taxYear,
      savingsIncome = encryptedSavingsIncomeUserDataModel.savingsIncome.map(decryptSavingsIncomeCheckYourAnswersModel),
      lastUpdated = encryptedSavingsIncomeUserDataModel.lastUpdated
    )
  }

}
