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

package models.dividends

import play.api.libs.json._
import play.api.mvc.{PathBindable, Result}


sealed abstract class Journey(name: String) {
  override def toString: String = name
  def sectionCompletedRedirect(taxYear: Int): Result = ??? //TODO: Add redirect results to each case object e.g. StockDividends
}

object Journey {
  val values: Seq[Journey] = Seq(StockDividends)


  def withName(journey: String): Either[String, Journey] = {
    val namesToValuesMap = values.map(v => v.toString -> v).toMap
    namesToValuesMap.get(journey) match {
      case Some(journeyName) => Right(journeyName)
      case None              => Left(s"Invalid journey name: $journey")
    }
  }

  implicit val format: Format[Journey] = new Format[Journey] {
    override def writes(journey: Journey): JsValue = JsString(journey.toString)

    override def reads(json: JsValue): JsResult[Journey] = json match {
      case JsString(name) =>
        withName(name) match {
          case Right(journey) => JsSuccess(journey)
          case Left(error)    => JsError(error)
        }
      case _ => JsError("String value expected")
    }
  }

  implicit def pathBindable(implicit strBinder: PathBindable[String]): PathBindable[Journey] = new PathBindable[Journey] {

    override def bind(key: String, value: String): Either[String, Journey] =
      strBinder.bind(key, value).flatMap(withName)

    override def unbind(key: String, journeyName: Journey): String =
      strBinder.unbind(key, journeyName.toString)
  }

  case object StockDividends extends Journey("pensions-summary")

}
