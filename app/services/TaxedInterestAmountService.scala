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

import models.interest.{InterestAccountModel, TaxedInterestModel}

import java.util.UUID
import javax.inject.Inject

class TaxedInterestAmountService @Inject() () {
  def createNewAccountsList(completeForm: TaxedInterestModel,
                            existingAccountWithName: Option[InterestAccountModel],
                            accounts: Seq[InterestAccountModel],
                            id: String): Seq[InterestAccountModel] = {

    def createNewAccount(overrideId: Option[String] = None): InterestAccountModel = {
      InterestAccountModel(None, completeForm.taxedAccountName, None, Some(completeForm.taxedAmount), Some(overrideId.getOrElse(id)))
    }

    if(existingAccountWithName.isDefined){
      val updatedAccount: InterestAccountModel = existingAccountWithName.get.copy(taxedAmount = Some(completeForm.taxedAmount))
      val existingAccount: Option[InterestAccountModel] = accounts.find(_.getPrimaryId().exists(_ == id)).map(_.copy(taxedAmount = None))
      val existingAccountNeedsRemoving: Boolean = existingAccount.exists(account => !account.hasUntaxed)

      val accountsExcludingImpactedAccounts: Seq[InterestAccountModel]  = {
        accounts.filterNot(account => account.accountName == completeForm.taxedAccountName || account.getPrimaryId().contains(id))
      }

      if(existingAccountNeedsRemoving){
        accountsExcludingImpactedAccounts :+ updatedAccount
      } else {
        accountsExcludingImpactedAccounts ++ Seq(Some(updatedAccount), existingAccount).flatten
      }
    } else {

      val existingAccount: Option[InterestAccountModel] = accounts.find(_.getPrimaryId().exists(_ == id))
      val accountAlreadyExistsWithUntaxedAmountAndNameChanged = existingAccount.exists{
        account => account.hasUntaxed && (account.accountName != completeForm.taxedAccountName)
      }

      //if the name has been updated only update the name for the taxed account and keep the existing untaxed account as is
      if(accountAlreadyExistsWithUntaxedAmountAndNameChanged){
        val removedAmountFromExistingAccount: InterestAccountModel = existingAccount.get.copy(taxedAmount = None)
        val newAccount: InterestAccountModel = createNewAccount(Some(UUID.randomUUID().toString))

        accounts.filterNot(_.getPrimaryId().contains(id)) ++ Seq(newAccount, removedAmountFromExistingAccount)
      } else {
        val newAccount = accounts.find(_.getPrimaryId().exists(_ == id)).map(_.copy(
          accountName = completeForm.taxedAccountName, taxedAmount = Some(completeForm.taxedAmount)
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
