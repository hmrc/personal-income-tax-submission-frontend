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

import common.InterestTaxTypes.UNTAXED
import models.interest.{InterestAccountModel, InterestAccountSourceModel, InterestCYAModel, InterestPriorSubmission}

import javax.inject.Inject

class ChooseAccountService @Inject() () {
  def accountsIgnoringAmounts(accounts: Seq[InterestAccountModel]): Set[InterestAccountModel] = {
    accounts.map(_.copy(amount = None)).toSet
  }

  def getPreviousAccounts(cya: Option[InterestCYAModel], prior: Option[InterestPriorSubmission], taxType: String): Set[InterestAccountModel] = {

    val priorAccounts: Seq[InterestAccountSourceModel] = prior.map(_.submissions).getOrElse(Seq())

    if (taxType.equals(UNTAXED)) {

      val accountsInSession: Seq[InterestAccountModel] = cya.map(_.untaxedAccounts).getOrElse(Seq()) ++ cya.map(_.taxedAccounts).getOrElse(Seq())

      val taxedAccounts = cya.map(_.taxedAccounts).getOrElse(Seq())
      val untaxedAccounts = cya.map(_.untaxedAccounts).getOrElse(Seq())

      val diffedAccounts = accountsInSession.filterNot(x => untaxedAccounts.exists(y => x.accountName == y.accountName))

      val inSessionAccountsToDisplay = diffedAccounts

      val inSessionIdsToExclude: Seq[String] = accountsInSession.filter(_.amount.isDefined).flatMap(_.id).filter(untaxedAccounts.map(x => x.id).contains)

      val priorAccountsToDisplay: Seq[InterestAccountSourceModel] = priorAccounts.filter(!_.hasUntaxed).filterNot(_.id.exists(inSessionIdsToExclude.contains))

      val returning = accountsIgnoringAmounts(inSessionAccountsToDisplay ++ priorAccountsToDisplay.map(x => InterestAccountModel(x.id, x.accountName, x.taxedAmount, x.uniqueSessionId)))

      returning

    } else {
      val accountsInSession: Seq[InterestAccountModel] = cya.map(_.untaxedAccounts).getOrElse(Seq()) ++ cya.map(_.taxedAccounts).getOrElse(Seq())

      val taxedAccounts = cya.map(_.taxedAccounts).getOrElse(Seq())
      val untaxedAccounts = cya.map(_.untaxedAccounts).getOrElse(Seq())

      val diffedAccounts = accountsInSession.filterNot(x => taxedAccounts.exists(y => x.accountName == y.accountName))

      val inSessionAccountsToDisplay = diffedAccounts

      val inSessionIdsToExclude: Seq[String] = accountsInSession.filter(_.amount.isDefined).flatMap(_.id).filter(taxedAccounts.map(x => x.id).contains)

      val priorAccountsToDisplay: Seq[InterestAccountSourceModel] = priorAccounts.filter(!_.hasTaxed).filterNot(_.id.exists(inSessionIdsToExclude.contains))

      val returning = accountsIgnoringAmounts(inSessionAccountsToDisplay ++ priorAccountsToDisplay.map(x => InterestAccountModel(x.id, x.accountName, x.untaxedAmount, x.uniqueSessionId)))

      returning
    }
  }
}
