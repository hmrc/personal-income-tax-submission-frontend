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

import models.{ApiErrorBodyModel, ApiErrorModel}
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.PagerDutyHelper.pagerDutyLog

object InterestSubmissionHttpParser {

  type InterestSubmissionsResponse = Either[ApiErrorModel, Int]

  implicit object InterestSubmissionResponseReads extends HttpReads[InterestSubmissionsResponse] {
    override def read(method: String, url: String, response: HttpResponse): InterestSubmissionsResponse = {
      response.status match  {
        case NO_CONTENT => Right(NO_CONTENT)
        case BAD_REQUEST | FORBIDDEN | CONFLICT =>
          pagerDutyLog(FOURXX_RESPONSE_FROM_API, logMessage(response))
          handleApiError(response)
        case INTERNAL_SERVER_ERROR =>
          pagerDutyLog(INTERNAL_SERVER_ERROR_FROM_API, logMessage(response))
          handleApiError(response)
        case SERVICE_UNAVAILABLE =>
          pagerDutyLog(SERVICE_UNAVAILABLE_FROM_API, logMessage(response))
          handleApiError(response)
        case _ =>
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_API, logMessage(response))
          handleApiError(response, Some(INTERNAL_SERVER_ERROR))
      }
    }
  }
  private def logMessage(response:HttpResponse): Option[String] ={
    Some(s"[InterestSubmissionHttpParser][read] Received ${response.status} from API. Body:${response.body}")
  }

  private def handleApiError(response: HttpResponse, statusOverride: Option[Int] = None): InterestSubmissionsResponse = {
    val status = statusOverride.getOrElse(response.status)

    try {
      response.json.validate[ApiErrorBodyModel].fold[InterestSubmissionsResponse](
        jsonErrors => {
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_API, Some(s"[InterestSubmissionHttpParser][read] Unexpected Json from API."))
          Left(ApiErrorModel(status, ApiErrorBodyModel.parsingError))
        },
        parsedError => Left(ApiErrorModel(status, parsedError))
      )
    } catch {
      case _: Exception => Left(ApiErrorModel(status, ApiErrorBodyModel.parsingError))
    }
  }


}
