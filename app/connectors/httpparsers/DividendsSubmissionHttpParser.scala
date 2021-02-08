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

import models.{DesErrorBodyModel, DesErrorModel, DividendsResponseModel}
import play.api.http.Status._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
import utils.PagerDutyHelper.PagerDutyKeys._
import utils.PagerDutyHelper.pagerDutyLog

object DividendsSubmissionHttpParser {
  type DividendsSubmissionsResponse = Either[DesErrorModel, DividendsResponseModel]

  implicit object DividendsSubmissionResponseReads extends HttpReads[DividendsSubmissionsResponse] {
    override def read(method: String, url: String, response: HttpResponse): DividendsSubmissionsResponse = {
      response.status match  {
        case NO_CONTENT => Right(DividendsResponseModel(NO_CONTENT))
        case BAD_REQUEST | FORBIDDEN | CONFLICT =>
          pagerDutyLog(FOURXX_RESPONSE_FROM_API, logMessage(response))
          handleDESError(response)
        case SERVICE_UNAVAILABLE =>
          pagerDutyLog(SERVICE_UNAVAILABLE_FROM_API, logMessage(response))
          handleDESError(response)
        case INTERNAL_SERVER_ERROR =>
          pagerDutyLog(INTERNAL_SERVER_ERROR_FROM_API, logMessage(response))
          handleDESError(response)
        case _ =>
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_API, logMessage(response))
          handleDESError(response, Some(INTERNAL_SERVER_ERROR))
      }
    }
  }


  private def logMessage(response:HttpResponse): Option[String] ={
    Some(s"[DividendsSubmissionHttpParser][read] Received ${response.status} from DES. Body:${response.body}")
  }

  private def handleDESError(response: HttpResponse, statusOverride: Option[Int] = None): DividendsSubmissionsResponse = {
    val status = statusOverride.getOrElse(response.status)

    try {
      response.json.validate[DesErrorBodyModel].fold[DividendsSubmissionsResponse](
        jsonErrors => {
          pagerDutyLog(UNEXPECTED_RESPONSE_FROM_API, Some(s"[DividendsSubmissionHttpParser][read] Unexpected Json from DES."))
          Left(DesErrorModel(status, DesErrorBodyModel.parsingError))
        },
        parsedError => Left(DesErrorModel(status, parsedError))
      )
    } catch {
      case _: Exception => Left(DesErrorModel(status, DesErrorBodyModel.parsingError))
    }
  }

}
