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

package services

import connectors.DividendsSubmissionConnector
import connectors.httpParsers.DividendsSubmissionHttpParser.DividendsSubmissionsResponse

import javax.inject.Inject
import models.{DividendsCheckYourAnswersModel, DividendsResponseModel, DividendsSubmissionModel}
import play.api.Logger
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.http.Status.NO_CONTENT

class DividendsSubmissionService @Inject()(dividendsSubmissionConnector: DividendsSubmissionConnector){

  def submitDividends(body: Option[DividendsCheckYourAnswersModel], nino: String, mtditid: String, taxYear: Int)
                     (implicit hc: HeaderCarrier): Future[DividendsSubmissionsResponse] = {

    lazy val logger: Logger = Logger(this.getClass.getName)

    val nonOptBody: DividendsCheckYourAnswersModel = body.getOrElse(DividendsCheckYourAnswersModel(Some(false), None, Some(false), None))

    nonOptBody match {
      case DividendsCheckYourAnswersModel(Some(false), _, Some(false), _) =>
        logger.info("[DividendsSubmissionService][submitDividends] User has entered No & No to both dividends questions. " +
          "Not submitting data to DES.")
        Future(Right(DividendsResponseModel(NO_CONTENT)))
      case _ =>
        val newBody = new DividendsSubmissionModel(nonOptBody.ukDividendsAmount, nonOptBody.otherUkDividendsAmount)
        dividendsSubmissionConnector.submitDividends(newBody, nino, mtditid, taxYear)
    }
  }

}
