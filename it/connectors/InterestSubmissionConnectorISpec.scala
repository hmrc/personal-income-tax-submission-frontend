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

import models.interest.InterestSubmissionModel
import models.{APIErrorBodyModel, APIErrorModel, APIErrorsBodyModel}
import play.api.libs.json.Json
import play.api.test.Helpers._
import utils.IntegrationTest

class InterestSubmissionConnectorISpec extends IntegrationTest {

  lazy val connector: InterestSubmissionConnector = app.injector.instanceOf[InterestSubmissionConnector]

  lazy val body: Seq[InterestSubmissionModel] = Seq(
    InterestSubmissionModel(Some("id"), "name", Some(999.99), None),
    InterestSubmissionModel(Some("ano'id"), "ano'name", None, Some(999.99))
  )

  lazy val nino = "A123456A"
  lazy val taxYear = 2020
  lazy val mtditid = "1234567890"

  ".submit" should {

    "return a successful response" when {

      "one is retrieved from the endpoint" in {
        stubPost(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=$mtditid", NO_CONTENT, "{}")

        val result = await(connector.submit(body, nino, taxYear, mtditid))

        result shouldBe Right(NO_CONTENT)
      }

    }

    "return an bad request response" when {

      "non json is returned" in {
        stubPost(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=$mtditid", BAD_REQUEST, "")

        val result = await(connector.submit(body, nino, taxYear, mtditid))

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
        stubPost(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=$mtditid", BAD_REQUEST, responseBody.toString())

        val result = await(connector.submit(body, nino, taxYear, mtditid))

        result shouldBe Left(expectedResult)
      }

      "one is retrieved from the endpoint" in {
        stubPost(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=$mtditid", BAD_REQUEST, "{}")

        val result = await(connector.submit(body, nino, taxYear, mtditid))

        result shouldBe Left(APIErrorModel(BAD_REQUEST, APIErrorBodyModel.parsingError))
      }

    }

    "return an internal server error response" when {

      "one is retrieved from the endpoint" in {

        val responseBody = Json.obj(
          "code" -> "INTERNAL_SERVER_ERROR",
          "reason" -> "there has been an error downstream"
        )

        stubPost(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=$mtditid", INTERNAL_SERVER_ERROR, responseBody.toString())

        val result = await(connector.submit(body, nino, taxYear, mtditid))

        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "there has been an error downstream")))
      }
    }

    "return a service unavailable response" when {

      "one is received from the endpoint" in {

        val responseBody = Json.obj(
          "code" -> "SERVICE_UNAVAILABLE",
          "reason" -> "the service is currently unavailable"
        )

        stubPost(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=$mtditid", SERVICE_UNAVAILABLE, responseBody.toString())

        val result = await(connector.submit(body, nino, taxYear, mtditid))

        result shouldBe Left(APIErrorModel(SERVICE_UNAVAILABLE, APIErrorBodyModel("SERVICE_UNAVAILABLE", "the service is currently unavailable")))
      }
    }

    "return an unexpected status error response" when {

      val responseBody = Json.obj(
        "code" -> "INTERNAL_SERVER_ERROR",
        "reason" -> "Unexpected status returned from DES"
      )

      "the response is not being handled explicitly when returned from the endpoint" in {
        stubPost(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=$mtditid", CREATED, responseBody.toString())

        val result = await(connector.submit(body, nino, taxYear, mtditid))

        result shouldBe Left(APIErrorModel(INTERNAL_SERVER_ERROR, APIErrorBodyModel("INTERNAL_SERVER_ERROR", "Unexpected status returned from DES")))
      }

    }

  }

}
