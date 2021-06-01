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

package connectors

import com.github.tomakehurst.wiremock.http.HttpHeader
import config.AppConfig
import models.interest.InterestSubmissionModel
import models.{APIErrorBodyModel, APIErrorModel, APIErrorsBodyModel}
import play.api.libs.json.Json
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, HttpClient, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import utils.IntegrationTest

class InterestSubmissionConnectorISpec extends IntegrationTest {

  lazy val connector: InterestSubmissionConnector = app.injector.instanceOf[InterestSubmissionConnector]

  lazy val httpClient: HttpClient = app.injector.instanceOf[HttpClient]

  def appConfig(host: String): AppConfig = new AppConfig(app.injector.instanceOf[ServicesConfig]) {
    override lazy val dividendsBaseUrl: String = s"http://$host:$wiremockPort/income-tax-interest"
  }

  lazy val body: Seq[InterestSubmissionModel] = Seq(
    InterestSubmissionModel(Some("id"), "name", Some(999.99), None),
    InterestSubmissionModel(Some("ano'id"), "ano'name", None, Some(999.99))
  )

  val taxYear = 2020

  val expectedHeaders = Seq(new HttpHeader("mtditid", mtditid))

  ".submit" should {

    "include internal headers" when {
      val headersSentToDividends = Seq(new HttpHeader(HeaderNames.xSessionId, "sessionIdValue"), new HttpHeader("mtditid", mtditid))

      val internalHost = "localhost"
      val externalHost = "127.0.0.1"

      "the host for Interest is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue"))).withExtraHeaders("mtditid"->mtditid)
        val connector = new InterestSubmissionConnector(httpClient, appConfig(internalHost))

        stubPost(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear", NO_CONTENT, "{}",
          headersSentToDividends)

        val result = await(connector.submit(body, nino, taxYear)(hc, ec))

        result shouldBe Right(NO_CONTENT)
      }
      "the host for Interest is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue"))).withExtraHeaders("mtditid"->mtditid)

        val connector = new InterestSubmissionConnector(httpClient, appConfig(externalHost))

        stubPost(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear", NO_CONTENT, "{}",
          headersSentToDividends)

        val result = await(connector.submit(body, nino, taxYear)(hc, ec))

        result shouldBe Right(NO_CONTENT)
      }
    }

    "return a successful response" when {

      "one is retrieved from the endpoint" in {
        stubPost(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear", NO_CONTENT, "{}", expectedHeaders)

        val result = await(connector.submit(body, nino, taxYear))

        result shouldBe Right(NO_CONTENT)
      }

    }

    "return an bad request response" when {

      "non json is returned" in {
        stubPost(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear", BAD_REQUEST, "", expectedHeaders)

        val result = await(connector.submit(body, nino, taxYear))

        result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel.parsingError))
      }

      "API Returns multiple errors" in {
        val expectedResult = APIErrorModel(BAD_REQUEST, APIErrorsBodyModel(Seq(
          APIErrorBodyModel("INVALID_IDTYPE","ID is invalid"),
          APIErrorBodyModel("INVALID_IDTYPE_2","ID 2 is invalid"))))

        val responseBody = Json.obj(
          "failures" -> Json.arr(
            Json.obj("code" -> "INVALID_IDTYPE",
              "reason" -> "ID is invalid"),
            Json.obj("code" -> "INVALID_IDTYPE_2",
              "reason" -> "ID 2 is invalid")
          )
        )
        stubPost(
          s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
          BAD_REQUEST,
          responseBody.toString(),
          expectedHeaders
        )

        val result = await(connector.submit(body, nino, taxYear))

        result shouldBe Left(expectedResult)
      }

      "one is retrieved from the endpoint" in {
        stubPost(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear", BAD_REQUEST, "{}", expectedHeaders)

        val result = await(connector.submit(body, nino, taxYear))

        result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel.parsingError))
      }

    }

    "return an internal server error response" when {

      "one is retrieved from the endpoint" in {

        val responseBody = Json.obj(
          "code" -> "INTERNAL_SERVER_ERROR",
          "reason" -> "there has been an error downstream"
        )

        stubPost(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear", INTERNAL_SERVER_ERROR, responseBody.toString(), expectedHeaders)

        val result = await(connector.submit(body, nino, taxYear))

        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "there has been an error downstream")))
      }
    }

    "return a service unavailable response" when {

      "one is received from the endpoint" in {

        val responseBody = Json.obj(
          "code" -> "SERVICE_UNAVAILABLE",
          "reason" -> "the service is currently unavailable"
        )

        stubPost(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear", SERVICE_UNAVAILABLE, responseBody.toString(), expectedHeaders)

        val result = await(connector.submit(body, nino, taxYear))

        result shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("SERVICE_UNAVAILABLE", "the service is currently unavailable")))
      }
    }

    "return an unexpected status error response" when {

      val responseBody = Json.obj(
        "code" -> "INTERNAL_SERVER_ERROR",
        "reason" -> "Unexpected status returned from DES"
      )

      "the response is not being handled explicitly when returned from the endpoint" in {
        stubPost(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear", CREATED, responseBody.toString(), expectedHeaders)

        val result = await(connector.submit(body, nino, taxYear))

        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Unexpected status returned from DES")))
      }

    }

  }

}
