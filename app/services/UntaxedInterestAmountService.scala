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

import models.interest.{InterestAccountModel, UntaxedInterestModel}

import java.util.UUID
import javax.inject.Inject

class UntaxedInterestAmountService @Inject() () {
  def createNewAccountsList(completeForm: UntaxedInterestModel,
                            existingAccountWithName: Option[InterestAccountModel],
                            accounts: Seq[InterestAccountModel],
                            id: String): Seq[InterestAccountModel] = {
    def createNewAccount(overrideId: Option[String] = None): InterestAccountModel = {
      InterestAccountModel(None, completeForm.untaxedAccountName, Some(completeForm.untaxedAmount), None, Some(overrideId.getOrElse(id)))
    }

    if(existingAccountWithName.isDefined){
      val updatedAccount: InterestAccountModel = existingAccountWithName.get.copy(untaxedAmount = Some(completeForm.untaxedAmount))
      val existingAccount: Option[InterestAccountModel] = accounts.find(_.getPrimaryId().exists(_ == id)).map(_.copy(untaxedAmount = None))

      val existingAccountNeedsRemoving: Boolean = existingAccount.exists(account => !account.hasTaxed)

      val accountsExcludingImpactedAccounts: Seq[InterestAccountModel]  = {
        accounts.filterNot(account => account.accountName == completeForm.untaxedAccountName || account.getPrimaryId().contains(id))
      }

      if(existingAccountNeedsRemoving){
        accountsExcludingImpactedAccounts :+ updatedAccount
      } else {
        accountsExcludingImpactedAccounts ++ Seq(Some(updatedAccount), existingAccount).flatten
      }
    } else {

      val existingAccount: Option[InterestAccountModel] = accounts.find(_.getPrimaryId().exists(_ == id))
      val accountAlreadyExistsWithTaxedAmountAndNameChanged = existingAccount.exists{
        account => account.hasTaxed && (account.accountName != completeForm.untaxedAccountName)
      }

      //if the name has been updated only update the name for the untaxed account and keep the existing taxed account as is
      if(accountAlreadyExistsWithTaxedAmountAndNameChanged){
        val removedAmountFromExistingAccount: InterestAccountModel = existingAccount.get.copy(untaxedAmount = None)
        val newAccount: InterestAccountModel = createNewAccount(Some(UUID.randomUUID().toString))

        accounts.filterNot(_.getPrimaryId().contains(id)) ++ Seq(newAccount, removedAmountFromExistingAccount)
      } else {
        val newAccount = accounts.find(_.getPrimaryId().exists(_ == id)).map(_.copy(
          accountName = completeForm.untaxedAccountName, untaxedAmount = Some(completeForm.untaxedAmount)
        )).getOrElse(createNewAccount())

        if (newAccount.getPrimaryId().nonEmpty && accounts.exists(_.getPrimaryId() == newAccount.getPrimaryId())) {
          accounts.map(account => if (account.getPrimaryId() == newAccount.getPrimaryId()) newAccount else account)
        } else {
          accounts :+ newAccount
        }
      }
    }
  }
}
