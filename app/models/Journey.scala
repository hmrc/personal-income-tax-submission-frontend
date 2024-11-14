/*
 * Copyright 2024 HM Revenue & Customs
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

package models

import play.api.mvc.PathBindable
import enumeratum._

sealed abstract class Journey(name: String) extends EnumEntry {
  override def toString: String = name
}

object Journey extends Enum[Journey] with PlayJsonEnum[Journey] {

  implicit def pathBindable(implicit strBinder: PathBindable[String]): PathBindable[Journey] = new PathBindable[Journey] {

    override def bind(key: String, value: String): Either[String, Journey] =
      strBinder.bind(key, value).flatMap { stringValue =>
        Journey.withNameOption(stringValue) match {
          case Some(journeyName) => Right(journeyName)
          case None => Left(s"$stringValue Invalid journey name")
        }
      }

    override def unbind(key: String, journey: Journey): String =
      strBinder.unbind(key, journey.entryName)
  }

  val values: IndexedSeq[Journey] = findValues

  // Charitable Donations
  case object GiftAid extends Journey("gift-aid")

  case object DonationsUsingGiftAid extends Journey("donations-using-gift-aid")

  case object GiftsOfLandOrProperty extends Journey("gifts-of-land-or-property")

  case object GiftsOfShares extends Journey("gifts-of-shares")

  case object GiftsToOverseas extends Journey("gifts-to-overseas-charities")

  // UK interest
  case object UkInterest extends Journey("uk-interest")

  case object BanksAndBuilding extends Journey("banks-and-building")

  case object TrustFundBond extends Journey("trust-fund-bond")

  case object GiltEdged extends Journey("gilt-edged")

  // UK dividends

  case object CashDividends extends Journey("cash-dividends")

  case object StockDividends extends Journey("stock-dividends")

  case object DividendsFromUnitTrusts extends Journey("dividends-from-unit-trusts")

  case object FreeRedeemableShares extends Journey("free-redeemable-shares")

  case object CloseCompanyLoans extends Journey("close-company-loans")
}
