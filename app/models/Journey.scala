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


sealed abstract class Journey(name: String, val subJourneys: List[SubJourney]) extends EnumEntry {
  override def toString: String = name
}

object Journey extends Enum[Journey] with PlayJsonEnum[Journey] {
  val values: IndexedSeq[Journey] = findValues

  implicit def pathBindable(implicit strBinder: PathBindable[String]): PathBindable[Journey] = new PathBindable[Journey] {

    override def bind(key: String, value: String): Either[String, Journey] =
      strBinder.bind(key, value).flatMap { stringValue =>
        Journey.withNameOption(stringValue) match {
          case Some(journeyName) => Right(journeyName)
          case None              => Left(s"$stringValue Invalid journey name")
        }
      }

    override def unbind(key: String, journeyName: Journey): String =
      strBinder.unbind(key, journeyName.entryName)
  }

  case object CharitableDonations extends Journey("charitable_donations",
    List(
      SubJourney.DonationsUsingGiftAid,
      SubJourney.GiftsOfLandOrProperty,
      SubJourney.GiftsOfShares,
      SubJourney.GiftsToOverseas
    )
  )
  case object UKInterest extends Journey("uk_interest",
    List(
      SubJourney.BanksAndBuilding,
      SubJourney.TrustFundBond,
      SubJourney.GiltEdged
    )
  )
  case object Dividends extends Journey("dividends",
    List(
      SubJourney.CashDividends,
      SubJourney.StockDividends,
      SubJourney.DividendsFromUnitTrusts,
      SubJourney.FreeRedeemableShares,
      SubJourney.CloseCompanyLoans
    )
  )
}

sealed abstract class SubJourney(name: String) extends EnumEntry {
  override def toString: String = name
}

object SubJourney extends Enum[SubJourney] with PlayJsonEnum[SubJourney] {

  implicit def pathBindable(implicit strBinder: PathBindable[String]): PathBindable[SubJourney] = new PathBindable[SubJourney] {

    override def bind(key: String, value: String): Either[String, SubJourney] =
      strBinder.bind(key, value).flatMap { stringValue =>
        SubJourney.withNameOption(stringValue) match {
          case Some(subJourneyName) => Right(subJourneyName)
          case None => Left(s"$stringValue Invalid subjourney name")
        }
      }

    override def unbind(key: String, subJourney: SubJourney): String =
      strBinder.unbind(key, subJourney.entryName)
  }

  val values: IndexedSeq[SubJourney] = findValues

  // Charitable Donations
  case object DonationsUsingGiftAid extends SubJourney("donations-using-gift-aid")

  case object GiftsOfLandOrProperty extends SubJourney("gifts_of_land_or_property")

  case object GiftsOfShares extends SubJourney("gifts_of_shares")

  case object GiftsToOverseas extends SubJourney("gifts_to_overseas_charities")

  // UK interest
  case object BanksAndBuilding extends SubJourney("banks_and_building")

  case object TrustFundBond extends SubJourney("trust_fund_bond")

  case object GiltEdged extends SubJourney("gilt_edged")

  // UK dividends
  case object CashDividends extends SubJourney("cash_dividends")

  case object StockDividends extends SubJourney("stock_dividends")

  case object DividendsFromUnitTrusts extends SubJourney("dividends_from_unit_trusts")

  case object FreeRedeemableShares extends SubJourney("free_redeemable_shares")

  case object CloseCompanyLoans extends SubJourney("close_company_loans")
}
