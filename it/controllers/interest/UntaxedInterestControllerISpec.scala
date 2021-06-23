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
import models.priorDataModels.IncomeSourcesModel
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{IntegrationTest, InterestDatabaseHelper}

import java.util.UUID

class UntaxedInterestControllerISpec extends IntegrationTest with InterestDatabaseHelper {

  val taxYear: Int = 2022
  val amount: BigDecimal = 25

  lazy val id: String = UUID.randomUUID().toString

  "as an individual" when {

    ".show" should {

      "returns an action when data is not in session" which {
        lazy val result: WSResponse = {
          dropInterestDB()

          emptyUserDataStub()
          insertCyaData(None)

          authoriseIndividual()
          await(
            wsClient.url(s"$appUrl/$taxYear/interest/untaxed-uk-interest")
              .withHttpHeaders(xSessionId, csrfContent)
              .get()
          )
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

      }
      "returns an action when data is in session" which {

        lazy val interestCYA = InterestCYAModel(
          Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
          Some(false), None
        )

        lazy val result: WSResponse = {
          dropInterestDB()

          emptyUserDataStub()
          insertCyaData(Some(interestCYA))

          authoriseIndividual()
          await(wsClient.url(s"$appUrl/$taxYear/interest/untaxed-uk-interest")
            .withHttpHeaders(xSessionId, csrfContent).get())
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
          await(wsClient.url(s"$appUrl/$taxYear/interest/untaxed-uk-interest")
            .withHttpHeaders(xSessionId, csrfContent).get())
        }
        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

    ".submit" should {

      s"redirect" when {

        "the answer is yes" when {

          "data is not in session" which {
            lazy val result: WSResponse = {
              dropInterestDB()

              emptyUserDataStub()

              authoriseIndividual()
              await(wsClient.url(s"$appUrl/$taxYear/interest/untaxed-uk-interest/")
                .withHttpHeaders(xSessionId, csrfContent)
                .withFollowRedirects(false)
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes)))
            }

            "has a SEE_OTHER(303) status" in {
              result.status shouldBe SEE_OTHER
            }

            "redirects to the untaxed interest amount page" in {
              result.headers("Location").head should include("/income-through-software/return/personal-income/2022/interest/add-untaxed-uk-interest-account/")
            }

          }

          "data is in session" which {
            lazy val result: WSResponse = {
              dropInterestDB()

              emptyUserDataStub()
              insertCyaData(Some(InterestCYAModel(Some(true))))

              authoriseIndividual()
              await(wsClient.url(s"$appUrl/$taxYear/interest/untaxed-uk-interest/")
                .withHttpHeaders(xSessionId, csrfContent)
                .withFollowRedirects(false)
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes)))
            }

            "has a SEE_OTHER(303) status" in {
              result.status shouldBe SEE_OTHER
            }

            "redirects to the untaxed interest amount page" in {
              result.headers("Location").head should include("/income-through-software/return/personal-income/2022/interest/add-untaxed-uk-interest-account/")
            }

          }

          "there is CYA model indicates it is finished" which {
            lazy val interestCYA = InterestCYAModel(
              Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
              Some(false), None
            )

            lazy val result: WSResponse = {
              dropInterestDB()

              emptyUserDataStub()
              insertCyaData(Some(interestCYA))

              authoriseIndividual()
              await(
                wsClient.url(s"$appUrl/$taxYear/interest/untaxed-uk-interest")
                  .withHttpHeaders(xSessionId, csrfContent)
                  .withFollowRedirects(false)
                  .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
              )
            }

            "has a SEE_OTHER(303) status" in {
              result.status shouldBe SEE_OTHER
            }

            "redirects to cya" in {
              result.headers("Location").head shouldBe "/income-through-software/return/personal-income/2022/interest/check-interest"
            }
          }

        }

        "the answer is no" when {

          "data is not in session" which {
            lazy val result: WSResponse = {
              dropInterestDB()

              emptyUserDataStub()

              authoriseIndividual()
              await(wsClient.url(s"$appUrl/$taxYear/interest/untaxed-uk-interest/")
                .withHttpHeaders(xSessionId, csrfContent)
                .withFollowRedirects(false)
                .post(Map(YesNoForm.yesNo -> YesNoForm.no)))
            }

            "has a SEE_OTHER(303) status" in {
              result.status shouldBe SEE_OTHER
            }

            "redirects to the receive tax interest page" in {
              result.headers("Location").head shouldBe "/income-through-software/return/personal-income/2022/interest/taxed-uk-interest"
            }

          }

          "data is in session" which {
            lazy val result: WSResponse = {
              dropInterestDB()

              emptyUserDataStub()
              insertCyaData(Some(InterestCYAModel(Some(true))))

              authoriseIndividual()
              await(wsClient.url(s"$appUrl/$taxYear/interest/untaxed-uk-interest/")
                .withHttpHeaders(xSessionId, csrfContent)
                .withFollowRedirects(false)
                .post(Map(YesNoForm.yesNo -> YesNoForm.no)))
            }

            "has a SEE_OTHER(303) status" in {
              result.status shouldBe SEE_OTHER
            }

            "redirects to the receive tax interest page" in {
              result.headers("Location").head shouldBe "/income-through-software/return/personal-income/2022/interest/taxed-uk-interest"
            }

          }

          "there is CYA model indicates it is finished" which {
            lazy val interestCYA = InterestCYAModel(
              Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
              Some(false), None
            )

            lazy val result: WSResponse = {
              dropInterestDB()

              emptyUserDataStub()
              insertCyaData(Some(interestCYA))

              authoriseIndividual()
              await(
                wsClient.url(s"$appUrl/$taxYear/interest/untaxed-uk-interest")
                  .withHttpHeaders(xSessionId, csrfContent)
                  .withFollowRedirects(false)
                  .post(Map(YesNoForm.yesNo -> YesNoForm.no))
              )
            }

            "has a SEE_OTHER(303) status" in {
              result.status shouldBe SEE_OTHER
            }

            "redirects to cya" in {
              result.headers("Location").head shouldBe "/income-through-software/return/personal-income/2022/interest/check-interest"
            }
          }

        }

      }

      s"return a BAD_REQUEST($BAD_REQUEST) status" in {
        lazy val result: WSResponse = {
          dropInterestDB()

          emptyUserDataStub()
          insertCyaData(None)

          authoriseIndividual()
          await(
            wsClient.url(s"$appUrl/$taxYear/interest/untaxed-uk-interest")
              .withHttpHeaders(xSessionId, csrfContent)
              .post(Map[String, String]())
          )
        }

        result.status shouldBe BAD_REQUEST
      }

      "returns an action when auth call fails" which {
        lazy val result: WSResponse = {
          dropInterestDB()

          emptyUserDataStub()
          insertCyaData(None)

          authoriseIndividualUnauthorized()
          await(
            wsClient.url(s"$appUrl/$taxYear/interest/untaxed-uk-interest")
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
          Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
          Some(false), None
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
          await(wsClient.url(s"$appUrl/$taxYear/interest/untaxed-uk-interest")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent).get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

      }

      "returns an action when auth call fails" which {
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A")
        )

        lazy val result: WSResponse = {
          dropInterestDB()

          emptyUserDataStub()
          insertCyaData(None)

          authoriseAgentUnauthorized()
          await(wsClient.url(s"$appUrl/$taxYear/interest/untaxed-uk-interest")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent).get())
        }
        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

    ".submit" should {

      s"redirect" when {

        "CYA data is not in session and the answer is yes" which {

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A")
          )

          lazy val result: WSResponse = {
            dropInterestDB()

            emptyUserDataStub()
            insertCyaData(None)

            authoriseAgent()
            await(wsClient.url(s"$appUrl/2020/interest/untaxed-uk-interest")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
              .withFollowRedirects(false)
              .post(Map(YesNoForm.yesNo -> YesNoForm.yes)))
          }

          s"has a SEE_OTHER(303) status" in {
            result.status shouldBe SEE_OTHER
          }

          "redirects to the untaxed amount page" in {
            result.headers("Location").head should include("/income-through-software/return/personal-income/2020/interest/add-untaxed-uk-interest-account/")
          }

        }

        "there is CYA data in session" in {
          lazy val interestCYA = InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
            Some(false), None
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
              wsClient.url(s"$appUrl/$taxYear/interest/untaxed-uk-interest")
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
            )
          }

          result.status shouldBe OK
        }

      }

      s"return a BAD_REQUEST($BAD_REQUEST) status" in {
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A")
        )

        lazy val result: WSResponse = {
          dropInterestDB()

          emptyUserDataStub()
          insertCyaData(None)

          authoriseAgent()
          await(
            wsClient.url(s"$appUrl/2020/interest/untaxed-uk-interest")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck", xSessionId)
              .post(Map[String, String]())
          )
        }

        result.status shouldBe BAD_REQUEST
      }

      "returns an action when auth call fails" which {
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A")
        )
        lazy val result: WSResponse = {
          dropInterestDB()

          authoriseAgentUnauthorized()
          await(
            wsClient.url(s"$appUrl/$taxYear/interest/untaxed-uk-interest")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
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
