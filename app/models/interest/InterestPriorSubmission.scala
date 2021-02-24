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

package models.interest

import common.{InterestTaxTypes, SessionValues}
import models.User
import play.api.libs.json._

case class InterestPriorSubmission(hasUntaxed: Boolean, hasTaxed: Boolean, submissions: Option[Seq[InterestAccountModel]]) {
  def asJsonString: String = Json.toJson(this).toString()
}

object InterestPriorSubmission {

  implicit val reads: Reads[InterestPriorSubmission] = for {
    interestAccounts <- __.readNullable[JsArray]
  } yield {
    interestAccounts.map(_.value.flatMap(_.asOpt[InterestAccountModel](InterestAccountModel.priorSubmissionReads))) match {
      case Some(accounts) if accounts.nonEmpty =>
        InterestPriorSubmission(
          hasUntaxed = accounts.exists(_.priorType.contains(InterestTaxTypes.UNTAXED)),
          hasTaxed = accounts.exists(_.priorType.contains(InterestTaxTypes.TAXED)),
          Some(accounts)
        )
      case _ => InterestPriorSubmission(hasUntaxed = false, hasTaxed = false, None)
    }
  }

  implicit val writes: OWrites[InterestPriorSubmission] = OWrites[InterestPriorSubmission] { model =>
    if(model.submissions.nonEmpty) {
      Json.obj("submissions" -> Json.toJson(model.submissions))
    } else {
      Json.obj()
    }
  }

  def fromSession()(implicit user: User[_]): Option[InterestPriorSubmission] = {
    user.session.get(SessionValues.INTEREST_PRIOR_SUB).flatMap(Json.parse(_).asOpt[InterestPriorSubmission])
  }

}
