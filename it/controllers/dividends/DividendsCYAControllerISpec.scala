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

package controllers.dividends

import common.SessionValues
import connectors.DividendsSubmissionConnector
import helpers.PlaySessionCookieBaker
import models.dividends.{DividendsCheckYourAnswersModel, DividendsPriorSubmission, DividendsSubmissionModel}
import models.mongo.DividendsUserDataModel
import models.priorDataModels.IncomeSourcesModel
import play.api.http.Status._
import play.api.libs.ws.{WSClient, WSResponse}
import play.mvc.Http.HeaderNames
import utils.{DividendsDatabaseHelper, IntegrationTest}

class DividendsCYAControllerISpec extends IntegrationTest with DividendsDatabaseHelper {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val connector: DividendsSubmissionConnector = app.injector.instanceOf[DividendsSubmissionConnector]

  val taxYear = 2022

  val dividends: BigDecimal = 10
  val dividendsCheckYourAnswersUrl = s"$startUrl/$taxYear/dividends/check-income-from-dividends"

  lazy val dividendsBody: DividendsSubmissionModel = DividendsSubmissionModel(
    Some(dividends),
    Some(dividends)
  )

  val firstAmount = 10
  val secondAmount = 20
  val successResponseCode = 204
  val internalServerErrorResponse = 500
  val serviceUnavailableResponse = 503
  val individualAffinityGroup: String = "Individual"

  val fullDividendsNino = "AA000003A"

  val priorData: IncomeSourcesModel = IncomeSourcesModel(
    dividends = Some(DividendsPriorSubmission(
      Some(firstAmount),
      Some(firstAmount)
    ))
  )

  val priorDataEmpty: IncomeSourcesModel = IncomeSourcesModel()

  ".show" should {

    s"return an OK($OK)" when {

      "there is CYA session data and prior submission data" in {
        dropDividendsDB()

        await(dividendsDatabase.create(DividendsUserDataModel(
          sessionId, mtditid, fullDividendsNino, taxYear,
          Some(DividendsCheckYourAnswersModel(
            Some(true), Some(1000.09),
            Some(true), Some(1234.31)
          ))
        )))

        val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        val result: WSResponse = {
          authoriseIndividual(Some(fullDividendsNino))
          userDataStub(priorData, fullDividendsNino, taxYear)
          await(wsClient.url(dividendsCheckYourAnswersUrl)
            .withHttpHeaders(
              xSessionId,
              "mtditid" -> mtditid,
              csrfContent,
              HeaderNames.COOKIE -> playSessionCookie
            )
            .get())
        }

        result.status shouldBe OK
      }

      "there is CYA session data and no prior submission data" in {
        dropDividendsDB()

        await(dividendsDatabase.create(DividendsUserDataModel(
          sessionId, mtditid, fullDividendsNino, taxYear,
          Some(DividendsCheckYourAnswersModel(
            Some(true), Some(1000.09),
            Some(true), Some(1234.31)
          ))
        )))

        val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        val result: WSResponse = {
          authoriseIndividual(Some(fullDividendsNino))
          userDataStub(IncomeSourcesModel(), fullDividendsNino, taxYear)
          await(wsClient.url(dividendsCheckYourAnswersUrl)
            .withHttpHeaders(
              xSessionId,
              "mtditid" -> mtditid,
              csrfContent,
              HeaderNames.COOKIE -> playSessionCookie
            )
            .get())
        }

        result.status shouldBe OK
      }

      "there is prior submission data and no CYA session data" in {
        dropDividendsDB()

        val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        val result: WSResponse = {
          authoriseIndividual(Some("AA112233B"))
          userDataStub(priorData, "AA112233B", taxYear)
          await(wsClient.url(dividendsCheckYourAnswersUrl)
            .withHttpHeaders(
              xSessionId,
              "mtditid" -> mtditid,
              csrfContent,
              HeaderNames.COOKIE -> playSessionCookie
            )
            .get())
        }

        result.status shouldBe OK
      }

    }

    "redirect to the overview page" when {

      "there is no session data" in {
        dropDividendsDB()

        val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        val result: WSResponse = {
          authoriseIndividual(Some("AA119293B"))
          userDataStub(IncomeSourcesModel(), "AA119293B", taxYear)
          stubGet("/income-through-software/return/2022/view", SEE_OTHER, "overview")
          await(wsClient.url(dividendsCheckYourAnswersUrl)
            .withHttpHeaders(
              xSessionId,
              "mtditid" -> mtditid,
              csrfContent,
              HeaderNames.COOKIE -> playSessionCookie
            )
            .withFollowRedirects(false)
            .get())
        }

        result.status shouldBe SEE_OTHER
      }

    }

    "redirect the user to the most relevant page if journey has not been completed" when {

      "up to receive UK Dividends is filled in" when {

        "the answer is Yes" which {
          lazy val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> taxYear.toString
          ))

          lazy val result: WSResponse = {
            dropDividendsDB()

            await(dividendsDatabase.create(DividendsUserDataModel(
              sessionId, mtditid, fullDividendsNino, taxYear,
              Some(DividendsCheckYourAnswersModel(
                Some(true)
              ))
            )))

            authoriseIndividual(Some(fullDividendsNino))
            userDataStub(priorDataEmpty, fullDividendsNino, taxYear)
            await(wsClient.url(dividendsCheckYourAnswersUrl)
              .withHttpHeaders(
                xSessionId,
                "mtditid" -> mtditid,
                csrfContent,
                HeaderNames.COOKIE -> playSessionCookie
              )
              .withFollowRedirects(false)
              .get())
          }

          s"has a status of 303" in {
            result.status shouldBe SEE_OTHER
          }

          "has the correct title" in {
            result.headers("Location").head shouldBe "/income-through-software/return/personal-income/2022/dividends/how-much-dividends-from-uk-companies"
          }
        }

        "the answer is No" which {
          lazy val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> taxYear.toString
          ))

          lazy val result: WSResponse = {
            dropDividendsDB()

            await(dividendsDatabase.create(DividendsUserDataModel(
              sessionId, mtditid, fullDividendsNino, taxYear,
              Some(DividendsCheckYourAnswersModel(
                Some(false)
              ))
            )))

            authoriseIndividual(Some(fullDividendsNino))
            userDataStub(priorDataEmpty, fullDividendsNino, taxYear)
            await(wsClient.url(dividendsCheckYourAnswersUrl)
              .withHttpHeaders(
                xSessionId,
                "mtditid" -> mtditid,
                csrfContent,
                HeaderNames.COOKIE -> playSessionCookie
              )
              .withFollowRedirects(false)
              .get())
          }

          s"has a status of 303" in {
            result.status shouldBe SEE_OTHER
          }

          "has the correct title" in {
            result.headers("Location").head shouldBe
              "/income-through-software/return/personal-income/2022/dividends/dividends-from-uk-trusts-or-open-ended-investment-companies"
          }
        }
      }

      "up to UK Dividends Amount is filled in" which {
        lazy val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        lazy val result: WSResponse = {
          dropDividendsDB()

          await(dividendsDatabase.create(DividendsUserDataModel(
            sessionId, mtditid, fullDividendsNino, taxYear,
            Some(DividendsCheckYourAnswersModel(
              Some(true), Some(1000.43)
            ))
          )))

          authoriseIndividual(Some(fullDividendsNino))
          userDataStub(priorDataEmpty, fullDividendsNino, taxYear)
          await(wsClient.url(dividendsCheckYourAnswersUrl)
            .withHttpHeaders(
              xSessionId,
              "mtditid" -> mtditid,
              csrfContent,
              HeaderNames.COOKIE -> playSessionCookie
            )
            .withFollowRedirects(false)
            .get())
        }

        s"has a status of 303" in {
          result.status shouldBe SEE_OTHER
        }

        "has the correct title" in {
          result.headers("Location").head shouldBe
            "/income-through-software/return/personal-income/2022/dividends/dividends-from-uk-trusts-or-open-ended-investment-companies"
        }
      }

      "up to Receive Other UK Dividends is filled in" which {
        lazy val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        lazy val result: WSResponse = {
          dropDividendsDB()

          await(dividendsDatabase.create(DividendsUserDataModel(
            sessionId, mtditid, fullDividendsNino, taxYear,
            Some(DividendsCheckYourAnswersModel(
              Some(true), Some(1000.43), Some(true)
            ))
          )))

          authoriseIndividual(Some(fullDividendsNino))
          userDataStub(priorDataEmpty, fullDividendsNino, taxYear)
          await(wsClient.url(dividendsCheckYourAnswersUrl)
            .withHttpHeaders(
              xSessionId,
              "mtditid" -> mtditid,
              csrfContent,
              HeaderNames.COOKIE -> playSessionCookie
            )
            .withFollowRedirects(false)
            .get())
        }

        s"has a status of 303" in {
          result.status shouldBe SEE_OTHER
        }

        "has the correct title" in {
          result.headers("Location").head shouldBe
            "/income-through-software/return/personal-income/2022/dividends/how-much-dividends-from-uk-trusts-and-open-ended-investment-companies"
        }

      }

    }

    "Redirect to the tax year error " when {

      "an invalid tax year has been added to the url" which {
        lazy val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        lazy val result: WSResponse = {
          dropDividendsDB()

          await(dividendsDatabase.create(DividendsUserDataModel(
            sessionId, mtditid, fullDividendsNino, taxYear,
            Some(DividendsCheckYourAnswersModel(
              Some(true), Some(1000.43), Some(true), Some(9983.21)
            ))
          )))

          authoriseIndividual(Some(fullDividendsNino))
          userDataStub(priorDataEmpty, fullDividendsNino, taxYear)
          await(wsClient.url(s"$startUrl/2004/dividends/check-income-from-dividends")
            .withHttpHeaders(
              xSessionId,
              "mtditid" -> mtditid,
              csrfContent,
              HeaderNames.COOKIE -> playSessionCookie
            )
            .withFollowRedirects(false)
            .get())
        }

        s"has a status of 303" in {
          result.status shouldBe SEE_OTHER
        }

        "has the correct title" in {
          result.headers("Location").head shouldBe
            "/income-through-software/return/personal-income/error/wrong-tax-year"
        }
      }
    }

  }

  ".submit" should {

    "redirect to the overview page" when {

      "there is session data " which {
        lazy val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        lazy val result: WSResponse = {
          dropDividendsDB()

          await(dividendsDatabase.create(DividendsUserDataModel(
            sessionId, mtditid, fullDividendsNino, taxYear,
            Some(DividendsCheckYourAnswersModel(
              Some(true), Some(1000.43), Some(true), Some(9983.21)
            ))
          )))

          authoriseIndividual(Some(fullDividendsNino))
          userDataStub(priorDataEmpty, fullDividendsNino, taxYear)
          stubPut("/income-tax-dividends/income-tax/nino/AA000003A/sources\\?taxYear=2022", NO_CONTENT, "")
          await(wsClient.url(dividendsCheckYourAnswersUrl)
            .withHttpHeaders(
              xSessionId,
              "mtditid" -> mtditid,
              csrfContent,
              HeaderNames.COOKIE -> playSessionCookie
            )
            .withFollowRedirects(false)
            .post(""))
        }

        s"has a status of 303" in {
          result.status shouldBe SEE_OTHER
        }

        "has the correct title" in {
          result.headers("Location").head shouldBe
            "http://localhost:11111/income-through-software/return/2022/view"
        }
      }
    }

    "there is an error posting downstream" should {

      "redirect to the 500 unauthorised error template page when there is a problem posting data" which {
        lazy val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        lazy val result: WSResponse = {
          dropDividendsDB()

          await(dividendsDatabase.create(DividendsUserDataModel(
            sessionId, mtditid, fullDividendsNino, taxYear,
            Some(DividendsCheckYourAnswersModel(
              Some(true), Some(1000.43), Some(true), Some(9983.21)
            ))
          )))

          authoriseIndividual(Some(fullDividendsNino))
          userDataStub(priorDataEmpty, fullDividendsNino, taxYear)
          stubPut("/income-tax-dividends/income-tax/nino/AA000003A/sources\\?taxYear=2022", INTERNAL_SERVER_ERROR, "")
          await(wsClient.url(dividendsCheckYourAnswersUrl)
            .withHttpHeaders(
              xSessionId,
              "mtditid" -> mtditid,
              csrfContent,
              HeaderNames.COOKIE -> playSessionCookie
            )
            .withFollowRedirects(false)
            .post(""))
        }

        s"has a status of 303" in {
          result.status shouldBe INTERNAL_SERVER_ERROR
        }
      }

      "redirect to the 503 service unavailable page when the service is unavailable" which {
        lazy val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        lazy val result: WSResponse = {
          dropDividendsDB()

          await(dividendsDatabase.create(DividendsUserDataModel(
            sessionId, mtditid, fullDividendsNino, taxYear,
            Some(DividendsCheckYourAnswersModel(
              Some(true), Some(1000.43), Some(true), Some(9983.21)
            ))
          )))

          authoriseIndividual(Some(fullDividendsNino))
          userDataStub(priorDataEmpty, fullDividendsNino, taxYear)
          stubPut("/income-tax-dividends/income-tax/nino/AA000003A/sources\\?taxYear=2022", SERVICE_UNAVAILABLE, "")
          await(wsClient.url(dividendsCheckYourAnswersUrl)
            .withHttpHeaders(
              xSessionId,
              "mtditid" -> mtditid,
              csrfContent,
              HeaderNames.COOKIE -> playSessionCookie
            )
            .withFollowRedirects(false)
            .post(""))
        }

        s"has a status of 303" in {
          result.status shouldBe SERVICE_UNAVAILABLE
        }
      }
    }

  }

}
