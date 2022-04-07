/*
 * Copyright 2022 HM Revenue & Customs
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

package connectors

import com.github.tomakehurst.wiremock.http.HttpHeader
import config.{AppConfig, FrontendAppConfig}
import models.dividends.{DecodedDividendsSubmissionPayload, DividendsCheckYourAnswersModel, DividendsPriorSubmission}
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.libs.json.{JsString, Writes}
import play.mvc.Http.Status._
import uk.gov.hmrc.http.{HeaderCarrier, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.IntegrationTest

class NrsConnectorSpec extends IntegrationTest {

  lazy val connector: NrsConnector = app.injector.instanceOf[NrsConnector]

  def appConfig(host: String): AppConfig = new FrontendAppConfig(app.injector.instanceOf[ServicesConfig]) {
    override lazy val nrsProxyBaseUrl: String = s"http://$host:$wiremockPort/income-tax-nrs-proxy"
  }

  implicit val headerCarrierWithSession: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId(sessionId)))
  implicit val writesObject: Writes[DecodedDividendsSubmissionPayload] = (o: DecodedDividendsSubmissionPayload) => JsString(o.toString)

  val ukDividends: BigDecimal = 10
  val otherDividends: BigDecimal = 10.50

  val dividendsCyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(
    Some(true), Some(ukDividends),
    Some(true), Some(otherDividends)
  )

  val priorData: DividendsPriorSubmission = DividendsPriorSubmission(
    Some(ukDividends),
    Some(otherDividends)
  )

  val decodedModel: DecodedDividendsSubmissionPayload = DecodedDividendsSubmissionPayload(Some(dividendsCyaModel), Some(priorData))
  val expectedHeaders = Seq(new HttpHeader("mtditid", mtditid))

  val url: String = s"/income-tax-nrs-proxy/$nino/itsa-personal-income-submission"

  ".NrsConnector" should {

    "return an Ok response when successful" in {

      stubPost(url, OK, "{}")
      val result = await(connector.postNrsConnector(nino, decodedModel))

      result shouldBe Right()
    }

    "return an InternalServerError" in {

      val expectedResult = APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Internal Server Error"))

      stubPost(url, INTERNAL_SERVER_ERROR, expectedResult.toJson.toString())
      val result = await(connector.postNrsConnector(nino, decodedModel))

      result shouldBe Left(expectedResult)
    }

    "return a NotFound error" in {

      val expectedResult = APIErrorModel(NOT_FOUND, APIErrorBodyModel("NOT_FOUND", "NRS returning not found error"))

      stubPost(url, NOT_FOUND, expectedResult.toJson.toString())
      val result = await(connector.postNrsConnector(nino, decodedModel))

      result shouldBe Left(expectedResult)
    }

    "return a ParsingError when an unexpected error has occurred" in {

      val expectedResult = APIErrorModel(CONFLICT, APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API"))

      stubPost(url, CONFLICT, expectedResult.toJson.toString())
      val result = await(connector.postNrsConnector(nino, decodedModel))

      result shouldBe Left(expectedResult)
    }

  }
}
