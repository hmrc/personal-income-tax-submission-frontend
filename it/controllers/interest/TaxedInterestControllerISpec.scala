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

package controllers.interest

import common.SessionValues
import forms.YesNoForm
import helpers.PlaySessionCookieBaker
import models.interest.{InterestAccountModel, InterestCYAModel}
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{IntegrationTest, InterestDatabaseHelper}

class TaxedInterestControllerISpec extends IntegrationTest with InterestDatabaseHelper {

  "as an individual" when {

    ".show" should {

      "returns an action when data is not in session" which {
        lazy val result: WSResponse = {
          dropInterestDB()

          emptyUserDataStub()
          insertCyaData(None)

          authoriseIndividual()
          await(wsClient.url(s"$appUrl/2020/interest/taxed-uk-interest").withHttpHeaders(xSessionId, csrfContent).get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

      }
      "returns an action when data is in session" which {

        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25.00)))
        )

        lazy val result: WSResponse = {
          dropInterestDB()

          emptyUserDataStub()
          insertCyaData(Some(interestCYA))

          authoriseIndividual()
          await(
            wsClient
              .url(s"$appUrl/2020/interest/taxed-uk-interest")
              .withHttpHeaders(xSessionId, csrfContent)
              .get()
          )
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

      }

      "returns an action when auth call fails" which {
        lazy val result: WSResponse = {
          dropInterestDB()

          emptyUserDataStub()
          insertCyaData(None)

          authoriseIndividualUnauthorized()
          await(wsClient.url(s"$appUrl/2020/interest/taxed-uk-interest")
            .withHttpHeaders(xSessionId, csrfContent).get())
        }
        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

    ".submit" should {

      s"return an OK($OK) status" when {

        "returns an action when data is not in session" which {
          lazy val result: WSResponse = {
            dropInterestDB()

            emptyUserDataStub()
            insertCyaData(None)

            authoriseIndividual()
            await(
              wsClient.url(s"$appUrl/2020/interest/taxed-uk-interest")
                .withHttpHeaders(xSessionId, csrfContent)
                .withFollowRedirects(false)
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
            )
          }

          s"has an SEE_OTHER(303) status" in {
            result.status shouldBe SEE_OTHER
          }

          "redirects to the overview page" in {
            result.headers("Location").head shouldBe "http://localhost:11111/income-through-software/return/2020/view"
          }

        }

        "there is CYA data in session" which {
          lazy val result: WSResponse = {
            val interestCYA = InterestCYAModel(
              Some(false), None,
              Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25.00)))
            )

            dropInterestDB()

            insertCyaData(Some(interestCYA))

            authoriseIndividual()
            await(
              wsClient.url(s"$appUrl/2022/interest/taxed-uk-interest")
                .withHttpHeaders(xSessionId, csrfContent)
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
            )
          }

          "returns an OK(200)" in {
            result.status shouldBe OK
          }
        }

      }

      s"return a BAD_REQUEST($BAD_REQUEST) status" in {
        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25.00)))
        )

        lazy val result: WSResponse = {
          dropInterestDB()

          emptyUserDataStub()
          insertCyaData(Some(interestCYA))

          authoriseIndividual()
          await(wsClient.url(s"$appUrl/2022/interest/taxed-uk-interest").withHttpHeaders(xSessionId, csrfContent).post(Map[String, String]()))
        }

        result.status shouldBe BAD_REQUEST
      }

      "returns an action when auth call fails" which {
        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25)))
        )

        lazy val result: WSResponse = {
          dropInterestDB()

          emptyUserDataStub()
          insertCyaData(Some(interestCYA))

          authoriseIndividualUnauthorized()
          await(
            wsClient.url(s"$appUrl/2020/interest/taxed-uk-interest")
              .withHttpHeaders(xSessionId, csrfContent)
              .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
          )
        }

        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

  }

  "as an agent" when {

    ".show" should {

      "returns an action when data is in session" which {

        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25.00)))
        )

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A")
        )

        lazy val result: WSResponse = {
          dropInterestDB()

          emptyUserDataStub()
          insertCyaData(Some(interestCYA))

          authoriseAgent()
          await(wsClient.url(s"$appUrl/2020/interest/taxed-uk-interest")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent).get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

      }

      "returns an action when auth call fails" which {
        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25.00)))
        )

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A")
        )

        lazy val result: WSResponse = {
          dropInterestDB()

          emptyUserDataStub()
          insertCyaData(Some(interestCYA))

          authoriseAgentUnauthorized()
          await(wsClient.url(s"$appUrl/2020/interest/taxed-uk-interest")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent).get())
        }

        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

    ".submit" should {

      s"return an OK($OK) status" when {

        "returns an action when data is not in session" which {
          lazy val result: WSResponse = {
            dropInterestDB()

            emptyUserDataStub()
            insertCyaData(None)

            authoriseIndividual()
            await(wsClient.url(s"$appUrl/2020/interest/taxed-uk-interest")
              .withHttpHeaders(xSessionId, csrfContent).post(Map(YesNoForm.yesNo -> YesNoForm.yes)))
          }

          s"has an NOT_FOUND($NOT_FOUND) status" in {
            result.status shouldBe NOT_FOUND
          }

        }

        "there is CYA data in session" in {
          lazy val interestCYA = InterestCYAModel(
            Some(false), None,
            Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25.00)))
          )

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A")
          )

          lazy val result: WSResponse = {
            dropInterestDB()

            emptyUserDataStub()
            insertCyaData(Some(interestCYA))

            authoriseAgent()
            await(
              wsClient.url(s"$appUrl/2022/interest/taxed-uk-interest")
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
                .withFollowRedirects(false)
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
            )
          }

          result.status shouldBe SEE_OTHER
          result.headers("Location").head should include("/income-through-software/return/personal-income/2022/interest/add-taxed-uk-interest-account/")
        }

      }

      s"return a BAD_REQUEST($BAD_REQUEST) status" in {
        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25.00)))
        )

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A")
        )

        lazy val result: WSResponse = {
          dropInterestDB()

          emptyUserDataStub()
          insertCyaData(Some(interestCYA))

          authoriseAgent()
          await(
            wsClient.url(s"$appUrl/2022/interest/taxed-uk-interest")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
              .post(Map[String, String]())
          )
        }

        result.status shouldBe BAD_REQUEST
      }

      "returns an action when auth call fails" which {
        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", 25.00)))
        )

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A")
        )

        lazy val result: WSResponse = {
          dropInterestDB()

          emptyUserDataStub()
          insertCyaData(Some(interestCYA))

          authoriseAgentUnauthorized()
          await(
            wsClient.url(s"$appUrl/2020/interest/taxed-uk-interest")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
              .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
          )
        }

        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

  }
}