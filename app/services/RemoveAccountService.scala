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

import common.InterestTaxTypes.TAXED
import models.User
import models.interest.{InterestAccountModel, InterestCYAModel, InterestPriorSubmission}

import javax.inject.Inject

class RemoveAccountService @Inject() () {
  def accountLookup(account : InterestAccountModel, accountId: String): Boolean = {
    account.id.getOrElse(account.uniqueSessionId.getOrElse("")) == accountId
  }

  def calculateTaxedUpdate(cyaData: InterestCYAModel,
                                             accounts: Seq[InterestAccountModel],
                                             accountId: String)
                                            (implicit user: User[_]): (InterestCYAModel, Seq[InterestAccountModel]) = {

    val accountToUpdate: Option[InterestAccountModel] = accounts.find(account => accountLookup(account, accountId))
    val accountsWithoutCurrentAccount: Seq[InterestAccountModel] = accounts.filterNot(account => accountLookup(account, accountId))

    val updatedAccounts = if(accountToUpdate.exists(_.hasUntaxed)){
      accountsWithoutCurrentAccount ++ Seq(accountToUpdate.map(_.copy(taxedAmount = None))).flatten
    } else {
      accountsWithoutCurrentAccount
    }

    val updatedCyaData = cyaData.copy(
      taxedUkInterest = Some(updatedAccounts.exists(_.hasTaxed)),
      accounts = updatedAccounts
    )

    (updatedCyaData, updatedAccounts)
  }

  def calculateUntaxedUpdate(cyaData: InterestCYAModel,
                                               accounts: Seq[InterestAccountModel],
                                               accountId: String)
                                              (implicit user: User[_]): (InterestCYAModel, Seq[InterestAccountModel]) = {

    val accountToUpdate: Option[InterestAccountModel] = accounts.find(account => accountLookup(account, accountId))
    val accountsWithoutCurrentAccount: Seq[InterestAccountModel] = accounts.filterNot(account => accountLookup(account, accountId))

    val updatedAccounts = if(accountToUpdate.exists(_.hasTaxed)){
      accountsWithoutCurrentAccount ++ Seq(accountToUpdate.map(_.copy(untaxedAmount = None))).flatten
    } else {
      accountsWithoutCurrentAccount
    }

    val updatedCyaData = cyaData.copy(
      untaxedUkInterest = Some(updatedAccounts.exists(_.hasUntaxed)),
      accounts = updatedAccounts
    )

    (updatedCyaData, updatedAccounts)
  }

  def isLastAccount(taxType: String, priorSubmission: Option[InterestPriorSubmission], taxAccounts: Seq[InterestAccountModel]): Boolean = {
    lazy val blankPriorSub = InterestPriorSubmission(hasTaxed = false, hasUntaxed = false)
    taxType match {
      case TAXED =>
        if (priorSubmission.getOrElse(blankPriorSub).hasTaxed) {
          false
        } else {
          taxAccounts.length == 1
        }
      case _ =>
        if (priorSubmission.getOrElse(blankPriorSub).hasUntaxed) {
          false
        } else {
          taxAccounts.length == 1
        }
    }
  }
}
