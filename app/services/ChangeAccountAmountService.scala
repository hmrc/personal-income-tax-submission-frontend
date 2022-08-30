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

import common.InterestTaxTypes
import common.InterestTaxTypes.TAXED
import models.interest.{InterestAccountModel, InterestAccountSourceModel, InterestCYAModel, InterestPriorSubmission}

import javax.inject.Inject

class ChangeAccountAmountService @Inject() () {
  def getSingleAccount(accountId: String, prior: Option[InterestPriorSubmission], cya: Option[InterestCYAModel]): Option[InterestAccountModel] = {

    val priorAccount: Option[InterestAccountSourceModel] = prior.flatMap(_.submissions.find(_.id.contains(accountId)))

    val accounts = cya.map(_.taxedAccounts).getOrElse(Seq()) ++ cya.map(_.untaxedAccounts).getOrElse(Seq())

    val cyaAccount: Option[InterestAccountModel] = accounts.find(_.uniqueSessionId.contains(accountId))

    (priorAccount, cyaAccount) match {
      case (_@Some(accountModel: InterestAccountSourceModel), _) =>
        Some(InterestAccountModel(accountModel.id, accountModel.accountName, if (accountModel.untaxedAmount.isDefined) accountModel.untaxedAmount else accountModel.taxedAmount, accountModel.uniqueSessionId))
      case (_, account@Some(_)) =>
        account
      case _ =>
        None
    }
  }

  def replaceAccounts(taxType: String, cyaData: InterestCYAModel,
                                        accounts: Seq[InterestAccountModel]): InterestCYAModel = taxType match {
    case InterestTaxTypes.UNTAXED => cyaData.copy(untaxedAccounts = accounts, untaxedUkInterest = Some(true))
    case InterestTaxTypes.TAXED => cyaData.copy(taxedAccounts = accounts, taxedUkInterest = Some(true))
  }

  def extractPreAmount(taxType: String, cya: InterestCYAModel,
                                         accountId: String): Option[BigDecimal] = taxType match {
    case InterestTaxTypes.UNTAXED =>
      cya.untaxedAccounts.find(_.getPrimaryId().contains(accountId)).flatMap(_.amount)
    case InterestTaxTypes.TAXED =>
      cya.taxedAccounts.find(_.getPrimaryId().contains(accountId)).flatMap(_.amount)
  }

  def updateAccounts(taxType: String, cya: InterestCYAModel, prior: Option[InterestPriorSubmission],  accountId: String,
                                       newAmount: BigDecimal): Seq[InterestAccountModel] = {

    val otherAccounts = if (taxType == TAXED) cya.taxedAccounts.filterNot(_.getPrimaryId().contains(accountId)) else cya.untaxedAccounts.filterNot(_.getPrimaryId().contains(accountId))

    val accounts = if (taxType == TAXED) cya.taxedAccounts else cya.untaxedAccounts

    val accountInCYA: Option[InterestAccountModel] = accounts.find(_.getPrimaryId().contains(accountId))
    val accountInPrior: Option[InterestAccountSourceModel] = prior.flatMap(_.submissions.find(_.getPrimaryId().contains(accountId)))

    taxType match {
      case InterestTaxTypes.UNTAXED =>
        if (accountInCYA.isDefined) {
          val newAccounts = accountInCYA.get.copy(amount = Some(newAmount)) +: otherAccounts
          newAccounts
        } else if (accountInPrior.isDefined) {
          val priorAccount = accountInPrior.get.copy(untaxedAmount = Some(newAmount))
          val newAccounts = InterestAccountModel(priorAccount.id, priorAccount.accountName, priorAccount.untaxedAmount, priorAccount.uniqueSessionId) +: otherAccounts
          newAccounts
        } else {
          val existingAccount = cya.taxedAccounts.find(_.getPrimaryId().contains(accountId))

          if (existingAccount.isDefined) {
            otherAccounts :+ existingAccount.get.copy(amount = Some(newAmount))
          } else {
            otherAccounts
          }
        }

      case InterestTaxTypes.TAXED =>
        if (accountInCYA.isDefined) {
          val newAccounts = accountInCYA.get.copy(amount = Some(newAmount)) +: otherAccounts
          newAccounts
        } else if (accountInPrior.isDefined) {
          val priorAccount = accountInPrior.get.copy(taxedAmount = Some(newAmount))
          val newAccounts = InterestAccountModel(priorAccount.id, priorAccount.accountName, priorAccount.taxedAmount, priorAccount.uniqueSessionId) +: otherAccounts
          newAccounts
        } else {
          val existingAccount = cya.untaxedAccounts.find(_.getPrimaryId().contains(accountId))

          if (existingAccount.isDefined) {
            otherAccounts :+ existingAccount.get.copy(amount = Some(newAmount))
          } else {
            otherAccounts
          }
        }
    }
  }
}
