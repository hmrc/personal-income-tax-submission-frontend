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
import connectors.httpParsers.GiftAidSubmissionHttpParser.GiftAidSubmissionsResponse
import models.{APIErrorBodyModel, APIErrorModel}
import play.api.http.Status._
import models.giftAid.{GiftAidPaymentsModel, GiftAidSubmissionModel, GiftsModel}
import play.api.libs.json.Json
import utils.IntegrationTest

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class GiftAidSubmissionConnectorSpec extends IntegrationTest {

  lazy val connector: GiftAidSubmissionConnector = app.injector.instanceOf[GiftAidSubmissionConnector]

  val currentTaxYear = 2022
  val nextTaxYear = 2023
  val previousTaxYear = 2021
  val nonUkCharitiesAmount = 5
  val landAndBuildingsAmount = 10
  val sharesOrSecuritiesAmount = 10
  val investmentsNonUkCharitiesAmount = 10

  val validGiftAidPaymentsModel: GiftAidPaymentsModel = GiftAidPaymentsModel(
    nonUkCharitiesCharityNames = List("non uk charity name", "non uk charity name 2"),
    currentYear = currentTaxYear,
    currentYearTreatedAsPreviousYear = previousTaxYear,
    nextYearTreatedAsCurrentYear = nextTaxYear,
    nonUkCharities = nonUkCharitiesAmount
  )

  val validGiftsModel: GiftsModel = GiftsModel(
    investmentsNonUkCharitiesCharityNames = List("charity name"),
    landAndBuildings = landAndBuildingsAmount,
    sharesOrSecurities = sharesOrSecuritiesAmount,
    investmentsNonUkCharities = investmentsNonUkCharitiesAmount
  )

  val validGiftAidModel: GiftAidSubmissionModel = GiftAidSubmissionModel(
    validGiftAidPaymentsModel,
    validGiftsModel
  )

  val taxYear = 2022

  val expectedHeaders = Seq(new HttpHeader("mtditid", mtditid))

  "GiftAidSubmissionConnectorSpec" should {
    "Return a success result" when {
      "Gift Aid returns a 204" in {
        stubPut(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=$taxYear", NO_CONTENT, "{}", expectedHeaders)
        val result: GiftAidSubmissionsResponse = Await.result(connector.submitGiftAid(validGiftAidModel, nino, taxYear), Duration.Inf)
        result shouldBe Right(NO_CONTENT)
      }

      "Gift Aid returns a 400" in {
        stubPut(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=$taxYear", BAD_REQUEST, "{}", expectedHeaders)
        val result = Await.result(connector.submitGiftAid(validGiftAidModel, nino, taxYear), Duration.Inf)
        result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API")))
      }

      "Gift Aid returns an error parsing from API 500 response" in {
        stubPut(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=$taxYear", INTERNAL_SERVER_ERROR, "{}", expectedHeaders)
        val result = Await.result(connector.submitGiftAid(validGiftAidModel, nino, taxYear), Duration.Inf)
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("PARSING_ERROR", "Error parsing response from API")))
      }

      "Gift Aid returns an unexpected status error 500 response" in {

        val responseBody = Json.obj(
          "code" -> "INTERNAL_SERVER_ERROR",
          "reason" -> "Unexpected status returned from DES"
        )

        stubPut(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=$taxYear", CREATED, responseBody.toString(), expectedHeaders)
        val result = await(connector.submitGiftAid(validGiftAidModel, nino, taxYear))
        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Unexpected status returned from DES")))
      }

      "Gift Aid returns a 503" in {

        val responseBody = Json.obj(
          "code" -> "SERVICE_UNAVAILABLE",
          "reason" -> "the service is currently unavailable"
        )

        stubPut(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=$taxYear", SERVICE_UNAVAILABLE, responseBody.toString(), expectedHeaders)
        val result = Await.result(connector.submitGiftAid(validGiftAidModel, nino, taxYear), Duration.Inf)
        result shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("SERVICE_UNAVAILABLE", "the service is currently unavailable")))
      }

    }
  }
}
