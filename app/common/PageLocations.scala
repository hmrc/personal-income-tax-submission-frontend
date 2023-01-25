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

package common

object PageLocations {

  object Interest {
    val TaxedAccountsView: Int => String = taxYear => controllers.interest.routes.AccountsController.show(taxYear, InterestTaxTypes.TAXED).url
    val TaxedAmountsView: (Int, String) => String = (taxYear, id) =>
      controllers.interest.routes.TaxedInterestAmountController.show(taxYear, id).url
    val TaxedView: Int => String = taxYear => controllers.interest.routes.TaxedInterestController.show(taxYear).url

    val UntaxedAccountsView: Int => String = taxYear => controllers.interest.routes.AccountsController.show(taxYear, InterestTaxTypes.UNTAXED).url
    val UntaxedAmountsView: (Int, String) => String = (taxYear, id) =>
      controllers.interest.routes.UntaxedInterestAmountController.show(taxYear, id).url
    val UntaxedView: Int => String = taxYear => controllers.interest.routes.UntaxedInterestController.show(taxYear).url

    val cya: Int => String = taxYear => controllers.interest.routes.InterestCYAController.show(taxYear).url
  }

}
