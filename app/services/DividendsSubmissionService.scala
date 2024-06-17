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

package services

import connectors.DividendsSubmissionConnector
import connectors.httpParsers.DividendsSubmissionHttpParser.DividendsSubmissionsResponse
import models.dividends.{DividendsCheckYourAnswersModel, DividendsSubmissionModel}
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class DividendsSubmissionService @Inject()(dividendsSubmissionConnector: DividendsSubmissionConnector)(
  implicit val ec: ExecutionContext
) {
  def submitDividends(dividendsCYA: DividendsCheckYourAnswersModel, nino: String, mtditid: String, taxYear: Int)
                     (implicit hc: HeaderCarrier): Future[DividendsSubmissionsResponse] = {

    lazy val logger: Logger = Logger(this.getClass.getName)

    val dividendsSubmission = new DividendsSubmissionModel(dividendsCYA.ukDividendsAmount, dividendsCYA.otherUkDividendsAmount)

    logger.info("[DividendsSubmissionService][submitDividends] User has updated CheckYourAnswers, submitting data to DES.")

    dividendsSubmissionConnector.submitDividends(dividendsSubmission, nino, taxYear)(hc.withExtraHeaders("mtditid" -> mtditid))
  }
}
