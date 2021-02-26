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

package utils

import uk.gov.hmrc.auth.core.authorise.Predicate
import uk.gov.hmrc.auth.core.retrieve.Retrieval
import uk.gov.hmrc.auth.core.{AuthConnector, AuthenticateHeaderParser, ConfidenceLevel}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class MockAuthConnector(stubbedRetrievalResult: Future[_], acceptedConfidenceLevels: Seq[ConfidenceLevel]) extends AuthConnector {
  def authorise[A](predicate: Predicate, retrieval: Retrieval[A])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[A] = {

    val confidenceLevelValue = (predicate.toJson \ "confidenceLevel").asOpt[Int].getOrElse(
      (predicate.toJson \\ "confidenceLevel").headOption.map(_.as[Int]).getOrElse(0)
    )

    if(acceptedConfidenceLevels.contains(ConfidenceLevel.fromInt(confidenceLevelValue).get)) {
      stubbedRetrievalResult.map(_.asInstanceOf[A])
    } else {
      Future.failed(AuthenticateHeaderParser.parse(Map()))
    }

  }
}
