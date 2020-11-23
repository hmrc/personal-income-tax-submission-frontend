package connectors

import models.DividendsSubmissionModel
import uk.gov.hmrc.http.HeaderCarrier
import utils.IntegrationTest
import play.api.http.Status._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class DividendsSubmissionConnectorSpec extends IntegrationTest{

  lazy val connector = app.injector.instanceOf[DividendsSubmissionConnector]
  val body =  DividendsSubmissionModel(
    Some(10),
    Some(10)
  )
  val nino = "nino"
  val mtditid = "mtditid"
  val taxYear = 2020
  implicit val hc =  HeaderCarrier()

  "DividendsSubmissionConnectorSpec" should {
    "Return a success result" when {
      "Dividends returns a 204" in {
        stubPut(s"/income-tax-dividends/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=$mtditid", NO_CONTENT, "{}")
        val result = Await.result(connector.submitDividends(body, nino, mtditid, taxYear), Duration.Inf)
        result.isRight shouldBe true
      }
      "Dividends returns a 400" in {
        stubPut(s"/income-tax-dividends/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=$mtditid", BAD_REQUEST, "{}")
        val result = Await.result(connector.submitDividends(body, nino, mtditid, taxYear), Duration.Inf)
        result.isLeft shouldBe true
      }
      "Dividends returns a 500" in {
        stubPut(s"/income-tax-dividends/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=$mtditid", INTERNAL_SERVER_ERROR, "{}")
        val result = Await.result(connector.submitDividends(body, nino, mtditid, taxYear), Duration.Inf)
        result.isLeft shouldBe true
      }
    }
  }

}
