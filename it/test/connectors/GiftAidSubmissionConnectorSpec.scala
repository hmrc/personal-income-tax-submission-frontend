/*
 * Copyright 2023 HM Revenue & Customs
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

package test.connectors

import com.github.tomakehurst.wiremock.http.HttpHeader
import config.{AppConfig, FrontendAppConfig}
import connectors.GiftAidSubmissionConnector
import connectors.httpParsers.GiftAidSubmissionHttpParser.GiftAidSubmissionsResponse
import models.charity.prior.{GiftAidPaymentsModel, GiftAidSubmissionModel, GiftsModel}
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.Configuration
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.test.Helpers.NO_CONTENT
import test.utils.IntegrationTest
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, HeaderNames, SessionId}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class GiftAidSubmissionConnectorSpec extends IntegrationTest {

  lazy val connector: GiftAidSubmissionConnector = app.injector.instanceOf[GiftAidSubmissionConnector]

  lazy val httpClient: HttpClientV2 = app.injector.instanceOf[HttpClientV2]

  def appConfig(host: String): AppConfig = new FrontendAppConfig(
    app.injector.instanceOf[ServicesConfig],
    app.injector.instanceOf[Configuration]
  ) {
    override val giftAidBaseUrl: String = s"http://$host:$wiremockPort/income-tax-gift-aid"
  }


  val currentTaxYear: Option[BigDecimal] = Some(1000.89)
  val oneOffCurrentTaxYear: Option[BigDecimal] = Some(605.99)
  val nextTaxYear: Option[BigDecimal] = Some(999.99)
  val previousTaxYear: Option[BigDecimal] = Some(10.21)
  val nonUkCharitiesAmount: Option[BigDecimal] = Some(55.55)
  val landAndBuildingsAmount: Option[BigDecimal] = Some(10.21)
  val sharesOrSecuritiesAmount: Option[BigDecimal] = Some(10.21)
  val investmentsNonUkCharitiesAmount: Option[BigDecimal] = Some(10.21)

  val validGiftAidPaymentsModel: GiftAidPaymentsModel = GiftAidPaymentsModel(
    nonUkCharitiesCharityNames = Some(List("non uk charity name", "non uk charity name 2")),
    currentYear = currentTaxYear,
    oneOffCurrentYear = oneOffCurrentTaxYear,
    currentYearTreatedAsPreviousYear = previousTaxYear,
    nextYearTreatedAsCurrentYear = nextTaxYear,
    nonUkCharities = nonUkCharitiesAmount
  )

  val validGiftsModel: GiftsModel = GiftsModel(
    investmentsNonUkCharitiesCharityNames = Some(List("charity name")),
    landAndBuildings = landAndBuildingsAmount,
    sharesOrSecurities = sharesOrSecuritiesAmount,
    investmentsNonUkCharities = investmentsNonUkCharitiesAmount
  )

  val validGiftAidModel: GiftAidSubmissionModel = GiftAidSubmissionModel(
    Some(validGiftAidPaymentsModel),
    Some(validGiftsModel)
  )

  val expectedHeaders = Seq(new HttpHeader("mtditid", mtditid))

  "GiftAidSubmissionConnectorSpec" should {

    "include internal headers" when {
      val headersSentToGiftAid = Seq(new HttpHeader(HeaderNames.xSessionId, "sessionIdValue"), new HttpHeader("mtditid", mtditid))

      val internalHost = "localhost"
      val externalHost = "127.0.0.1"

      "the host for Gift Aid is 'Internal'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue"))).withExtraHeaders("mtditid"->mtditid)
        val connector = new GiftAidSubmissionConnector(httpClient, appConfig(internalHost))

        stubPost(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=$taxYear", NO_CONTENT, "{}",
          headersSentToGiftAid)

        val result: GiftAidSubmissionsResponse = Await.result(connector.submitGiftAid(validGiftAidModel, nino, taxYear)(hc), Duration.Inf)

        result shouldBe Right(NO_CONTENT)
      }
      "the host for Gift Aid is 'External'" in {
        implicit val hc: HeaderCarrier = HeaderCarrier(sessionId = Some(SessionId("sessionIdValue"))).withExtraHeaders("mtditid"->mtditid)

        val connector = new GiftAidSubmissionConnector(httpClient, appConfig(externalHost))

        stubPost(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=$taxYear", NO_CONTENT, "{}",
          headersSentToGiftAid)

        val result: GiftAidSubmissionsResponse = Await.result(connector.submitGiftAid(validGiftAidModel, nino, taxYear)(hc), Duration.Inf)

        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API")))
      }
    }
    "Return a success result" when {
      "Gift Aid returns a 204" in {
        stubPost(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=$taxYear", NO_CONTENT, "{}", expectedHeaders)
        val result: GiftAidSubmissionsResponse = Await.result(connector.submitGiftAid(validGiftAidModel, nino, taxYear), Duration.Inf)
        result shouldBe Right(NO_CONTENT)
      }

      "Gift Aid returns a 400" in {
        stubPost(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=$taxYear", BAD_REQUEST, "{}", expectedHeaders)
        val result = Await.result(connector.submitGiftAid(validGiftAidModel, nino, taxYear), Duration.Inf)
        result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API")))
      }

      "Gift Aid returns an error parsing from API 500 response" in {
        stubPost(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=$taxYear", INTERNAL_SERVER_ERROR, "{}", expectedHeaders)
        val result = Await.result(connector.submitGiftAid(validGiftAidModel, nino, taxYear), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API")))
      }

      "Gift Aid returns an unexpected status error 500 response" in {

        val responseBody = Json.obj(
          "code" -> "INTERNAL_SERVER_ERROR",
          "reason" -> "Unexpected status returned from DES"
        )

        stubPost(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=$taxYear", CREATED, responseBody.toString(), expectedHeaders)
        val result = await(connector.submitGiftAid(validGiftAidModel, nino, taxYear))
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Unexpected status returned from DES")))
      }

      "Gift Aid returns a 503" in {

        val responseBody = Json.obj(
          "code" -> "SERVICE_UNAVAILABLE",
          "reason" -> "the service is currently unavailable"
        )

        stubPost(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=$taxYear", SERVICE_UNAVAILABLE, responseBody.toString(), expectedHeaders)
        val result = Await.result(connector.submitGiftAid(validGiftAidModel, nino, taxYear), Duration.Inf)
        result shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("SERVICE_UNAVAILABLE", "the service is currently unavailable")))
      }

    }
  }
}
