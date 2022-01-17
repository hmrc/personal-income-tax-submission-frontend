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

package models.priorDataModels

import models.charity.prior.GiftAidSubmissionModel
import models.dividends.DividendsPriorSubmission
import play.api.libs.json.{Json, OFormat}

case class IncomeSourcesModel(
                               dividends: Option[DividendsPriorSubmission] = None,
                               interest: Option[Seq[InterestModel]] = None,
                               giftAid: Option[GiftAidSubmissionModel] = None
                             )

object IncomeSourcesModel {
  implicit val formats: OFormat[IncomeSourcesModel] = Json.format[IncomeSourcesModel]
}
