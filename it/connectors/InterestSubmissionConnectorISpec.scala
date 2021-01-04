/*
 * Copyright 2020 HM Revenue & Customs
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

import models.httpResponses.ErrorResponse
import models.interest.InterestSubmissionModel
import utils.IntegrationTest
import play.api.test.Helpers.{BAD_REQUEST, NO_CONTENT, INTERNAL_SERVER_ERROR, CREATED}

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

      "one is retrieved from the endpoint" in {
        stubPost(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=$mtditid", BAD_REQUEST, "{}")

        val result = await(connector.submit(body, nino, taxYear, mtditid))

        result shouldBe Left(ErrorResponse(BAD_REQUEST, "Bad request received from DES."))
      }

    }

    "return an internal server error response" when {

      "one is retrieved from the endpoint" in {
        stubPost(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=$mtditid", INTERNAL_SERVER_ERROR, "{}")

        val result = await(connector.submit(body, nino, taxYear, mtditid))

        result shouldBe Left(ErrorResponse(INTERNAL_SERVER_ERROR, "Internal server error returned from DES."))
      }

    }

    "return an unexpected status error response" when {

      "a response the is not being handled explicitly is returned from the endpoint" in {
        stubPost(s"/income-tax-interest/income-tax/nino/$nino/sources\\?taxYear=$taxYear&mtditid=$mtditid", CREATED, "{}")

        val result = await(connector.submit(body, nino, taxYear, mtditid))

        result shouldBe Left(ErrorResponse(CREATED, "Unexpected status returned from DES."))
      }

    }

  }

}
