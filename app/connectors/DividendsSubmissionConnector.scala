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

package connectors

import config.AppConfig
import connectors.httpParsers.DividendsSubmissionHttpParser._
import javax.inject.Inject
import models.dividends.DividendsSubmissionModel
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient}

import scala.concurrent.{ExecutionContext, Future}

class DividendsSubmissionConnector @Inject()(val http: HttpClient,  appConfig: AppConfig)
                                            (implicit ec: ExecutionContext) extends RawResponseReads {

  def submitDividends(body: DividendsSubmissionModel, nino: String, taxYear: Int)
                     (implicit hc: HeaderCarrier): Future[DividendsSubmissionsResponse] = {
      val dividendsSubmissionUrl: String = appConfig.dividendsBaseUrl + s"/income-tax/nino/$nino/sources?taxYear=$taxYear"
      http.PUT[DividendsSubmissionModel, DividendsSubmissionsResponse](dividendsSubmissionUrl, body)
  }
}
