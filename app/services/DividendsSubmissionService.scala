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

package services

import connectors.DividendsSubmissionConnector
import connectors.httpparsers.DividendsSubmissionHttpParser.DividendsSubmissionsResponse
import javax.inject.Inject
import models.{DividendsCheckYourAnswersModel, DividendsSubmissionModel}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class DividendsSubmissionService @Inject()(dividendsSubmissionConnector: DividendsSubmissionConnector){

  def submitDividends(body: Option[DividendsCheckYourAnswersModel], nino: String, mtditd: String, taxYear: Int)
                     (implicit hc: HeaderCarrier): Future[DividendsSubmissionsResponse] = {
    val nonOptBody = body.getOrElse(DividendsCheckYourAnswersModel(Some(false), None, Some(false), None))
    val newBody = new DividendsSubmissionModel(nonOptBody.ukDividendsAmount, nonOptBody.otherUkDividendsAmount)
    dividendsSubmissionConnector.submitDividends(newBody, nino, mtditd, taxYear)
  }

}
