///*
// * Copyright 2023 HM Revenue & Customs
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package connectors.httpParsers.stockdividends
//
//import play.api.http.Status._
//import uk.gov.hmrc.http.{HttpReads, HttpResponse}
//import utils.PagerDutyHelper.pagerDutyLog
//
//object UpdateGainsSessionHttpParser extends Parser {
//  type UpdateGainsSessionResponse = Either[ApiError, Int]
//
//  override val parserName: String = "UpdateGainsSessionHttpParser"
//  override val service: String = "income-tax-additional-information"
//
//  implicit object UpdateGainsSessionResponseResponseReads extends HttpReads[UpdateGainsSessionResponse] {
//    override def read(method: String, url: String, response: HttpResponse): UpdateGainsSessionResponse = response.status match {
//      case NO_CONTENT => Right(NO_CONTENT)
//      case BAD_REQUEST | FORBIDDEN | NOT_FOUND | UNPROCESSABLE_ENTITY =>
//        pagerDutyLog(FOURXX_RESPONSE_FROM_IF, logMessage(response).get)
//        handleError(response, BAD_REQUEST)
//      case INTERNAL_SERVER_ERROR =>
//        pagerDutyLog(INTERNAL_SERVER_ERROR_FROM_IF, logMessage(response).get)
//        handleError(response, INTERNAL_SERVER_ERROR)
//      case SERVICE_UNAVAILABLE =>
//        pagerDutyLog(SERVICE_UNAVAILABLE_FROM_IF, logMessage(response).get)
//        handleError(response, INTERNAL_SERVER_ERROR)
//      case _ =>
//        pagerDutyLog(UNEXPECTED_RESPONSE_FROM_IF, logMessage(response).get)
//        handleError(response, INTERNAL_SERVER_ERROR)
//    }
//  }
//}
