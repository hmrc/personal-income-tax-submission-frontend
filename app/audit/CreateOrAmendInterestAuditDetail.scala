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

import models.interest.{InterestAccountModel, InterestCYAModel, InterestPriorSubmission}
import play.api.libs.json.{Json, OWrites, Writes}
import utils.JsonUtils.jsonObjNoNulls

case class CreateOrAmendInterestAuditDetail(body: Option[InterestCYAModel], prior: Option[InterestPriorSubmission], isUpdate: Boolean, nino: String, mtditid: String, userType: String, taxYear: Int)

object CreateOrAmendInterestAuditDetail {
  implicit val writes: OWrites[CreateOrAmendInterestAuditDetail] = OWrites[CreateOrAmendInterestAuditDetail] { model =>
    (if (model.body.nonEmpty) {
      Json.obj("body" -> jsonObjNoNulls("gateway" -> model.body.get.gateway).++(Json.obj("untaxedUkInterest" -> model.body.get.untaxedUkInterest, "taxedUkInterest" -> model.body.get.taxedUkInterest, "accounts" -> (if (model.body.get.accounts.nonEmpty) Json.toJson(model.body.get.accounts)(Writes.iterableWrites2[InterestAccountModel, Seq[InterestAccountModel]](implicitly, InterestAccountModel.priorSubmissionWrites)) else Json.obj()))))
    } else {
      Json.obj()
    }).++(if (model.prior.nonEmpty) {
      Json.obj("prior" -> Json.obj("submissions" -> Json.toJson(model.prior.get.submissions)(Writes.iterableWrites2[InterestAccountModel, Seq[InterestAccountModel]](implicitly, InterestAccountModel.priorSubmissionWrites))))
    } else {
      Json.obj()
    }).++(Json.obj("isUpdate" -> model.isUpdate, "nino" -> model.nino, "mtditid" -> model.mtditid, "userType" -> model.userType, "taxYear" -> model.taxYear))
  }
}
