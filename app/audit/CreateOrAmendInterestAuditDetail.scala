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

package audit

import models.interest.{InterestAccountSourceModel, InterestCYAModel, InterestPriorSubmission, InterestSubmissionModel}
import play.api.libs.json.{JsNull, JsObject, Json, OWrites, Writes}
import utils.JsonUtils.jsonObjNoNulls

case class CreateOrAmendInterestAuditDetail(body: Option[InterestCYAModel],
                                            prior: Option[InterestPriorSubmission],
                                            isUpdate: Boolean,
                                            nino: String,
                                            mtditid: String,
                                            userType: String,
                                            taxYear: Int)

object CreateOrAmendInterestAuditDetail {
  implicit def writes: Writes[CreateOrAmendInterestAuditDetail] = (audit: CreateOrAmendInterestAuditDetail) => {
    val body = audit.body

    jsonObjNoNulls(
      "body" -> {
        if (body.isEmpty) {
          JsNull
        } else {
          val cyaData = body.get
          jsonObjNoNulls(
            "gateway" -> cyaData.gateway,
            "untaxedUkInterest" -> body.get.untaxedUkInterest,
            "taxedUkInterest" -> body.get.taxedUkInterest,
            "accounts" -> {
              val taxedAccounts = cyaData.taxedAccounts
              val untaxedAccounts = cyaData.untaxedAccounts

              val m1: Map[String, Option[BigDecimal]] = untaxedAccounts.foldLeft(Map.empty[String, Option[BigDecimal]]) {
                case (key, value) =>
                  key + (value.accountName -> value.amount)
              }
              val m2: Map[String, Option[BigDecimal]] = taxedAccounts.foldLeft(Map.empty[String, Option[BigDecimal]]) {
                case (key, value) =>
                  key + (value.accountName -> value.amount)
              }

              val accounts: Seq[InterestAccountSourceModel] = (m1.keySet ++ m2.keySet).foldLeft(Map.empty[String, InterestAccountSourceModel]) {
                case (m: Map[String, InterestAccountSourceModel], key: String) =>
                  val untaxedAccountId = untaxedAccounts.find(_.accountName == key).flatMap(_.id)
                  val taxedAccountId = taxedAccounts.find(_.accountName == key).flatMap(_.id)

                  m + (key -> InterestAccountSourceModel(
                    if (untaxedAccountId.isDefined) untaxedAccountId else taxedAccountId,
                    key,
                    m1.get(key).flatten,
                    m2.get(key).flatten))
              }.values.to[Seq]

              if (accounts.isEmpty) JsNull else accounts
            }
          )
        }
      }
    ).++(Json.obj(
      "prior" -> audit.prior,
      "isUpdate" -> audit.isUpdate,
      "nino" -> audit.nino,
      "mtditid" -> audit.mtditid,
      "userType" -> audit.userType,
      "taxYear" -> audit.taxYear
    ))
  }

}
