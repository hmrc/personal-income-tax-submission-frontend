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

package connectors

import config.AppConfig
import connectors.httpParsers.InterestSubmissionHttpParser.{InterestSubmissionResponseReads, InterestSubmissionsResponse}
import javax.inject.Inject
import models.interest.InterestSubmissionModel
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.{ExecutionContext, Future}

class InterestSubmissionConnector @Inject()(
                                             httpClient: HttpClient,
                                             appConfig: AppConfig
                                           ) {

  def submit(body: Seq[InterestSubmissionModel], nino: String, taxYear: Int, mtditid: String)
            (implicit hc: HeaderCarrier, ec: ExecutionContext): Future[InterestSubmissionsResponse] = {
    val url: String = appConfig.interestBaseUrl + s"/income-tax/nino/$nino/sources?taxYear=$taxYear&mtditid=$mtditid"
    httpClient.POST[Seq[InterestSubmissionModel], InterestSubmissionsResponse](url, body)
  }

}
