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

package audit

import models.savings.SecuritiesModel
import play.api.libs.json.{Json, OWrites}

case class CreateOrAmendSavingsAuditDetail(
                                            gateway: Option[Boolean] = None,
                                            grossAmount: Option[BigDecimal] = None,
                                            taxTakenOff: Option[Boolean] = None,
                                            taxTakenOffAmount: Option[BigDecimal] = None,
                                            prior: Option[SecuritiesModel],
                                            isUpdate: Boolean,
                                            nino: String,
                                            mtditid: String,
                                            userType: String,
                                            taxYear: Int)

object CreateOrAmendSavingsAuditDetail {
  implicit def writes: OWrites[CreateOrAmendSavingsAuditDetail] = Json.writes[CreateOrAmendSavingsAuditDetail]
}
