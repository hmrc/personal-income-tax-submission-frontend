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
import models.interest.{InterestAccountModel, InterestCYAModel, InterestPriorSubmission}

import javax.inject.Inject

class ChangeAccountAmountService @Inject() () {
  def priorAmount(account: InterestAccountModel, taxType: String): Option[BigDecimal] ={
    taxType match {
      case InterestTaxTypes.UNTAXED => account.untaxedAmount
      case InterestTaxTypes.TAXED => account.taxedAmount
    }
  }

  def getSingleAccount(accountId: String, prior: Option[InterestPriorSubmission], cya: Option[InterestCYAModel]): Option[InterestAccountModel] = {

    val priorAccount: Option[InterestAccountModel] = prior.flatMap(_.submissions.find(_.id.contains(accountId)))
    val cyaAccount: Option[InterestAccountModel] = cya.flatMap(_.accounts.find(_.uniqueSessionId.contains(accountId)))

    (priorAccount, cyaAccount) match {
      case (account@Some(_), _) => account
      case (_, account@Some(_)) => account
      case _ => None
    }
  }

  def replaceAccounts(taxType: String, cyaData: InterestCYAModel,
                                        accounts: Seq[InterestAccountModel]): InterestCYAModel = taxType match {
    case InterestTaxTypes.UNTAXED => cyaData.copy(accounts = accounts, untaxedUkInterest = Some(true))
    case InterestTaxTypes.TAXED => cyaData.copy(accounts = accounts, taxedUkInterest = Some(true))
  }

  def extractPreAmount(taxType: String, cya: InterestCYAModel,
                                         accountId: String): Option[BigDecimal] = taxType match {
    case InterestTaxTypes.UNTAXED =>
      cya.accounts.find(_.getPrimaryId().contains(accountId)).flatMap(_.untaxedAmount)
    case InterestTaxTypes.TAXED =>
      cya.accounts.find(_.getPrimaryId().contains(accountId)).flatMap(_.taxedAmount)
  }

  def updateAccounts(taxType: String, cya: InterestCYAModel, prior: Option[InterestPriorSubmission],  accountId: String,
                                       newAmount: BigDecimal): Seq[InterestAccountModel] = {

    val otherAccounts = cya.accounts.filterNot(_.getPrimaryId().contains(accountId))
    val accountInCYA: Option[InterestAccountModel] = cya.accounts.find(_.getPrimaryId().contains(accountId))
    val accountInPrior: Option[InterestAccountModel] = prior.flatMap(_.submissions.find(_.getPrimaryId().contains(accountId)))

    taxType match {
      case InterestTaxTypes.UNTAXED =>
        if(accountInCYA.isDefined){
          otherAccounts :+ accountInCYA.get.copy(untaxedAmount = Some(newAmount))
        } else if(accountInPrior.isDefined){
          otherAccounts :+ accountInPrior.get.copy(untaxedAmount = Some(newAmount))
        } else {
          otherAccounts
        }

      case InterestTaxTypes.TAXED =>
        if(accountInCYA.isDefined){
          otherAccounts :+ accountInCYA.get.copy(taxedAmount = Some(newAmount))
        } else if(accountInPrior.isDefined){
          otherAccounts :+ accountInPrior.get.copy(taxedAmount = Some(newAmount))
        } else {
          otherAccounts
        }
    }
  }
}
