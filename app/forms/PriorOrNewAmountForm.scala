/*
 * Copyright 2021 HM Revenue & Customs
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

  val validationChecks: Seq[(String => Boolean, String)] = Seq[(String => Boolean, String)](
    (isANumber, "common.error.invalid_number"),
    (isValidCurrency, "common.error.invalid_currency"),
    (isTooBig, "common.error.amountMaxLimit")
  )

  def stringFormatter(
                       currentAmount: BigDecimal
                     )(implicit messages: Messages): Formatter[String] = new Formatter[String] {

    val priorAmountId = "whichAmount"

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], String] = {
      data.get(key) match {
        case Some(stringValue) => Right(stringValue)
        case _ => Left(Seq(FormError(priorAmountId, messages("common.error.priorOrNewAmount.noRadioSelected", currentAmount))))
      }
    }

    override def unbind(key: String, value: String): Map[String, String] = {
      Map(key -> value)
    }
  }

  def otherAmountFormatter(
                            currentAmount: BigDecimal
                          )(implicit messages: Messages): Formatter[Option[BigDecimal]] = new Formatter[Option[BigDecimal]] {

    override def bind(key: String, data: Map[String, String]): Either[Seq[FormError], Option[BigDecimal]] = {
      val amountTypeSelect = data.get(amountTypeField)
      val potentialAmount = data.get(otherAmountInputField)

      val priorAmountId = "whichAmount"

      (amountTypeSelect, potentialAmount) match {
        case (Some(`priorAmount`), _) =>
          Right(Some(currentAmount))
        case (Some(`otherAmount`), Some(amount)) =>
          runChecks(amount, validationChecks)
        case _ =>
          Left(Seq(FormError(priorAmountId, messages("common.error.priorOrNewAmount.noRadioSelected", currentAmount))))
      }
    }

    override def unbind(key: String, value: Option[BigDecimal]): Map[String, String] = {
      value match {
        case Some(unwrappedAmount) => Map(key -> unwrappedAmount.toString())
        case _ => Map[String, String]()
      }
    }
  }

  def runChecks(amount: String, checks: Seq[(String => Boolean, String)])
               (implicit messages: Messages): Either[Seq[FormError], Option[BigDecimal]] = {

    val otherAmountInput = "amount"

    val errors: Seq[String] = checks.flatMap { case (func, errorKey) =>
      if (!func(amount)) Some(errorKey) else None
    }

    if (errors.nonEmpty) {
      Left(Seq(FormError(otherAmountInput, messages(errors.head))))
    } else {
      Right(Some(BigDecimal(amount)))
    }
  }

  def priorOrNewAmountForm(currentAmount: BigDecimal)(implicit messages: Messages): Form[PriorOrNewAmountModel] = Form(
    mapping(
      amountTypeField -> of(stringFormatter(currentAmount)),
      otherAmountInputField -> of(otherAmountFormatter(currentAmount))
    )(PriorOrNewAmountModel.apply)(PriorOrNewAmountModel.unapply)
  )

  def isANumber(input: String): Boolean = {
    val regex: String = """^[0-9.]*$"""
    input.matches(regex)
  }

  def isValidCurrency(input: String): Boolean = {
    val regex: String = """\d+|\d*\.\d{1,2}"""
    input.matches(regex)
  }

  def isTooBig(input: String): Boolean = {
    val regex: String = """^([0-9]{1,11}$)|^([0-9]{1,11})\.\d{1,2}"""
    input.matches(regex)
  }

}
