
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
