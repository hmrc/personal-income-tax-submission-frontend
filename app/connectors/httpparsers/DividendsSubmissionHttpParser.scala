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

import models.DividendsResponseModel
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import play.api.http.Status._

object DividendsSubmissionHttpParser {
  type DividendsSubmissionsResponse = Either[DividendsSubmissionException, DividendsResponseModel]

  implicit object DividendsSubmissionResponseReads extends HttpReads[DividendsSubmissionsResponse] {
    override def read(method: String, url: String, response: HttpResponse): DividendsSubmissionsResponse = {
      response.status match  {
        case NO_CONTENT => Right(DividendsResponseModel(NO_CONTENT))
        case BAD_REQUEST => Left(BadRequestDividendsSubmissionException)
        case INTERNAL_SERVER_ERROR => Left(InternalServerErrorDividendsSubmissionException)
        case _ => Left(UnexpectedDividendsError)
      }
    }
  }

  sealed trait DividendsSubmissionException
  object BadRequestDividendsSubmissionException extends DividendsSubmissionException
  object InternalServerErrorDividendsSubmissionException extends DividendsSubmissionException
  object UnexpectedDividendsError extends DividendsSubmissionException


}
