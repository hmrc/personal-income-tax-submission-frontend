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

import models.interest.{InterestAccountModel, UntaxedInterestModel}

import java.util.UUID
import javax.inject.Inject

class UntaxedInterestAmountService @Inject() () {
  def createNewAccountsList(completeForm: UntaxedInterestModel,
                            existingAccountWithName: Option[InterestAccountModel],
                            accounts: Seq[InterestAccountModel],
                            id: String): Seq[InterestAccountModel] = {
    def createNewAccount(overrideId: Option[String] = None): InterestAccountModel = {
      InterestAccountModel(None, completeForm.untaxedAccountName, Some(completeForm.untaxedAmount), Some(overrideId.getOrElse(id)))
    }

    if(existingAccountWithName.isDefined){
      val updatedAccount: InterestAccountModel = existingAccountWithName.get.copy(amount = Some(completeForm.untaxedAmount))
      val existingAccount: Option[InterestAccountModel] = accounts.find(_.getPrimaryId().exists(_ == id)).map(_.copy(amount = None))

      val existingAccountNeedsRemoving: Boolean = existingAccount.exists(account => account.amount.isEmpty)

      val accountsExcludingImpactedAccounts: Seq[InterestAccountModel]  = {
        accounts.filterNot(account => account.accountName == completeForm.untaxedAccountName || account.getPrimaryId().contains(id))
      }

      if (existingAccountNeedsRemoving) {
        updatedAccount +: accountsExcludingImpactedAccounts
      } else {
        Seq(Some(updatedAccount), existingAccount).flatten ++ accountsExcludingImpactedAccounts
      }
    } else {

      val existingAccount: Option[InterestAccountModel] = accounts.find(_.getPrimaryId().exists(_ == id))
      val accountAlreadyExistsWithTaxedAmountAndNameChanged = existingAccount.exists{
        account => account.amount.isDefined && (account.accountName != completeForm.untaxedAccountName)
      }

      //if the name has been updated only update the name for the untaxed account and keep the existing taxed account as is
      if(accountAlreadyExistsWithTaxedAmountAndNameChanged){
        val removedAmountFromExistingAccount: InterestAccountModel = existingAccount.get.copy(amount = None)
        val newAccount: InterestAccountModel = createNewAccount(Some(UUID.randomUUID().toString))

        accounts.filterNot(_.getPrimaryId().contains(id)) ++ Seq(newAccount, removedAmountFromExistingAccount)
      } else {
        val newAccount = accounts.find(_.getPrimaryId().exists(_ == id)).map(_.copy(
          accountName = completeForm.untaxedAccountName, amount = Some(completeForm.untaxedAmount)
        )).getOrElse(createNewAccount())

        if (newAccount.getPrimaryId().nonEmpty && accounts.exists(_.getPrimaryId() == newAccount.getPrimaryId())) {
          val updatedAccounts = accounts.filter(account => account.getPrimaryId() == newAccount.getPrimaryId())

          val otherAccounts = accounts.filterNot(account => account.getPrimaryId() == newAccount.getPrimaryId())

          updatedAccounts ++ otherAccounts

        } else {
          accounts :+ newAccount
        }
      }
    }
  }
}
