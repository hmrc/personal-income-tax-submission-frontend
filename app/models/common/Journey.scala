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

package models.common

import enumeratum._
import play.api.mvc.PathBindable

sealed abstract class Journey(name: String, val subJourneys: List[SubJourney]) extends EnumEntry {
  override def toString: String = name
}

object Journey extends Enum[Journey] with utils.PlayJsonEnum[Journey] {
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

  case object CharitableDonations extends Journey("charitableDonations",
    List(
      SubJourney.DonationsUsingGiftAid,
      SubJourney.GiftsOfLandOrProperty,
      SubJourney.GiftsOfShares,
      SubJourney.GiftsToOverseas
    )
  )
  case object UKInterest extends Journey("ukInterest",
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

object SubJourney extends Enum[SubJourney] with utils.PlayJsonEnum[SubJourney] {
  val values: IndexedSeq[SubJourney] = findValues

  // Charitable Donations
  case object DonationsUsingGiftAid extends SubJourney("donationsUsingGiftAid")

  case object GiftsOfLandOrProperty extends SubJourney("giftsOfLandOrProperty")

  case object GiftsOfShares extends SubJourney("giftsOfShares")

  case object GiftsToOverseas extends SubJourney("giftsToOverseasCharities")

  // UK interest
  case object BanksAndBuilding extends SubJourney("banksAndBuildingTitle")

  case object TrustFundBond extends SubJourney("trustFundBondTitle")

  case object GiltEdged extends SubJourney("giltEdgedTitle")

  // UK dividends
  case object CashDividends extends SubJourney("cashDividendsTitle")

  case object StockDividends extends SubJourney("stockDividendsTitle")

  case object DividendsFromUnitTrusts extends SubJourney("dividendsFromUnitTrustsTitle")

  case object FreeRedeemableShares extends SubJourney("freeRedeemableSharesTitle")

  case object CloseCompanyLoans extends SubJourney("closeCompanyLoansTitle")
}
