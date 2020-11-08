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

import models.formatHelpers.PriorOrNewAmountModel
import play.api.data.Forms._
import play.api.data.format.Formatter
import play.api.data.{Form, FormError}
import play.api.i18n.Messages

object PriorOrNewAmountForm {

  val amountTypeField = "whichAmount"
  val otherAmountInputField = "amount"

  val priorAmount = "prior"
  val otherAmount = "other"

  def stringFormatter(
                       currentAmount: BigDecimal,
                       radioErrorLocation: String
                     )(implicit messages: Messages): Formatter[String] = new Formatter[String] {

    val priorAmountId = "prior-amount"

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
      data.get(key) match {
        case Some(stringValue) => Right(stringValue)
        case _ => Left(Seq(FormError(priorAmountId, messages(s"$radioErrorLocation.error.noRadioSelected", currentAmount))))
      }
    }

    override def unbind(key: String, value: String): Map[String, String] = {
      Map(key -> value)
    }
  }

  def otherAmountFormatter(
                            currentAmount: BigDecimal,
                            radioErrorLocation: String
                          )(implicit messages: Messages): Formatter[Option[BigDecimal]] = new Formatter[Option[BigDecimal]] {

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Option[BigDecimal]] = {
      val amountTypeSelect = data.get(amountTypeField)
      val potentialAmount = data.get(otherAmountInputField)

      val priorAmountId = "prior-amount"

      (amountTypeSelect, potentialAmount) match {
        case (Some(`priorAmount`), _) =>
          Right(Some(currentAmount))
        case (Some(`otherAmount`), Some(someAmount)) if !isANumber(someAmount) =>
          Left(Seq(FormError(priorAmountId, messages("dividends.error.invalid_number"))))
        case (Some(`otherAmount`), Some(someAmount)) if isANumber(someAmount) =>
          Right(Some(BigDecimal(someAmount)))
        case _ =>
          Left(Seq(FormError(priorAmountId, messages(s"$radioErrorLocation.error.noRadioSelected", currentAmount))))
      }
    }

    override def unbind(key: String, value: Option[BigDecimal]): Map[String, String] = {
      value match {
        case Some(unwrappedAmount) => Map(key -> unwrappedAmount.toString())
        case _ => Map[String, String]()
      }
    }
  }

  def priorOrNewAmountForm(currentAmount: BigDecimal, radioErrorLocation: String)(implicit messages: Messages): Form[PriorOrNewAmountModel] = Form(
    mapping(
      amountTypeField -> of(stringFormatter(currentAmount, radioErrorLocation)),
      otherAmountInputField -> of(otherAmountFormatter(currentAmount, radioErrorLocation))
    )(PriorOrNewAmountModel.apply)(PriorOrNewAmountModel.unapply)
  )

  def isANumber(input: String): Boolean = {
    val regex: String = "\\d+"
    input.matches(regex)
  }

}
