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

import common.InterestTaxTypes.UNTAXED
import models.interest.{InterestAccountModel, InterestCYAModel, InterestPriorSubmission}

import javax.inject.Inject

class ChooseAccountService @Inject() () {
  def accountsIgnoringAmounts(accounts: Seq[InterestAccountModel]): Set[InterestAccountModel] = {
    accounts.map(_.copy(untaxedAmount = None, taxedAmount = None)).toSet
  }

  def getPreviousAccounts(cya: Option[InterestCYAModel], prior: Option[InterestPriorSubmission], taxType: String): Set[InterestAccountModel] = {

    val accountsInSession: Seq[InterestAccountModel] = cya.map(_.accounts).getOrElse(Seq())
    val priorAccounts: Seq[InterestAccountModel] = prior.map(_.submissions).getOrElse(Seq())

    if (taxType.equals(UNTAXED)) {

      val inSessionAccountsToDisplay = accountsInSession.filter(!_.hasUntaxed)
      val inSessionIdsToExclude: Seq[String] = accountsInSession.filter(_.hasUntaxed).flatMap(_.id)

      val priorAccountsToDisplay: Seq[InterestAccountModel] = priorAccounts.filter(!_.hasUntaxed).filterNot(_.id.exists(inSessionIdsToExclude.contains))

      accountsIgnoringAmounts(inSessionAccountsToDisplay ++ priorAccountsToDisplay)

    } else {
      val inSessionAccountsToDisplay = accountsInSession.filter(!_.hasTaxed)
      val inSessionIdsToExclude: Seq[String] = accountsInSession.filter(_.hasTaxed).flatMap(_.id)

      val priorAccountsToDisplay: Seq[InterestAccountModel] = priorAccounts.filter(!_.hasTaxed).filterNot(_.id.exists(inSessionIdsToExclude.contains))

      accountsIgnoringAmounts(inSessionAccountsToDisplay ++ priorAccountsToDisplay)
    }
  }
}
