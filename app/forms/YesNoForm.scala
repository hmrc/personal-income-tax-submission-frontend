/*
 * Copyright 2020 HM Revenue & Customs
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

package forms

import models.formatHelpers.YesNoModel
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formatter

object YesNoForm {

  val yesNo = "yes_no"
  val yes = "yes"
  val no = "no"

  def formatter(missingInputError: String): Formatter[YesNoModel] = new Formatter[YesNoModel] {
    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], YesNoModel] = {
      data.get(key) match {
        case Some(`yes`) => Right(YesNoModel(yes))
        case Some(`no`) => Right(YesNoModel(no))
        case _ => Left(Seq(FormError(key, missingInputError)))
      }
    }

    override def unbind(key: String, value: YesNoModel): Map[String, String] = {
      Map(
        key -> value.yesNoValue
      )
    }
  }

  def yesNoForm(missingInputError: String): Form[YesNoModel] = Form(
    single(
      yesNo -> of(formatter(missingInputError))
    )
  )

}
