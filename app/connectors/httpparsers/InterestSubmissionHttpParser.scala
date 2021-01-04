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

package connectors.httpparsers

import models.httpResponses.ErrorResponse
import org.slf4j
import play.api.Logger
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

object InterestSubmissionHttpParser {
  private val logger: slf4j.Logger = Logger.logger

  type InterestSubmissionsResponse = Either[ErrorResponse, Int]

  implicit object InterestSubmissionResponseReads extends HttpReads[InterestSubmissionsResponse] {
    override def read(method: String, url: String, response: HttpResponse): InterestSubmissionsResponse = {
      response.status match  {
        case `NO_CONTENT` => Right(NO_CONTENT)
        case `BAD_REQUEST` =>
          logger.info("[InterestSubmissionHttpParser][read] Bad request received from DES during interest submission.")
          Left(ErrorResponse(BAD_REQUEST, "Bad request received from DES."))
        case `INTERNAL_SERVER_ERROR` =>
          logger.info("[InterestSubmissionHttpParser][read] Internal server error received from DES during interest submission.")
          Left(ErrorResponse(INTERNAL_SERVER_ERROR, "Internal server error returned from DES."))
        case unexpectedStatus =>
          logger.info(s"[InterestSubmissionHttpParser][read] Unexpected status($unexpectedStatus) received from DES during interest submission.")
          Left(ErrorResponse(unexpectedStatus, "Unexpected status returned from DES."))
      }
    }
  }

}
