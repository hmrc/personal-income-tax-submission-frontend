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
import forms.YesNoForm
import helpers.PlaySessionCookieBaker
import models.dividends.DividendsCheckYourAnswersModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{DividendsDatabaseHelper, IntegrationTest, ViewHelpers}

class ReceiveUkDividendsControllerISpec extends IntegrationTest with ViewHelpers with DividendsDatabaseHelper {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  lazy val controller: ReceiveUkDividendsController = app.injector.instanceOf[ReceiveUkDividendsController]

  val taxYear: Int = 2022
  val amount: BigDecimal = 500
  val receiveUkDividendsUrl = s"$startUrl/$taxYear/dividends/dividends-from-uk-companies"

  "as an individual" when {

    ".show" should {

      "returns the uk dividends page when there is no priorSubmission data and no cyaData in session" which {
        lazy val result: WSResponse = {
          dropDividendsDB()

          emptyUserDataStub()
          insertCyaData(None)

          authoriseIndividual()
          await(wsClient.url(receiveUkDividendsUrl).withHttpHeaders(xSessionId, csrfContent).get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck("Did you get dividends from UK-based companies?")
        h1Check("Did you get dividends from UK-based companies? Dividends for 6 April 2021 to 5 April 2022")

      }
      "returns an action when cyaData is in session" which {
        lazy val result: WSResponse = {
          dropDividendsDB()

          emptyUserDataStub()
          insertCyaData(Some(
            DividendsCheckYourAnswersModel()
          ))

          authoriseIndividual()
          await(wsClient.url(receiveUkDividendsUrl)
            .withHttpHeaders(xSessionId, csrfContent).get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck("Did you get dividends from UK-based companies?")
        h1Check("Did you get dividends from UK-based companies? Dividends for 6 April 2021 to 5 April 2022")
      }

      "returns an action when auth call fails" which {
        lazy val result: WSResponse = {
          authoriseIndividualUnauthorized()
          await(wsClient.url(receiveUkDividendsUrl)
            .withHttpHeaders(xSessionId, "Csrf-Token" -> "nocheck").get())
        }
        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

    ".submit" should {

      s"return a SEE_OTHER(303) status" when {

        "there is no CYA data in session" in {
          lazy val result: WSResponse = {
            dropDividendsDB()

            insertCyaData(None)

            authoriseIndividual()
            await(
              wsClient.url(receiveUkDividendsUrl)
                .withFollowRedirects(false)
                .withHttpHeaders(xSessionId, csrfContent)
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
            )
          }

          result.status shouldBe SEE_OTHER
          result.header(HeaderNames.LOCATION) shouldBe Some(routes.UkDividendsAmountController.show(taxYear).url)
        }

        "there is form data and answer to question is 'No'" in {
          lazy val result: WSResponse = {
            dropDividendsDB()

            emptyUserDataStub()

            authoriseIndividual()
            await(
              wsClient.url(receiveUkDividendsUrl)
                .withFollowRedirects(false)
                .withHttpHeaders(xSessionId, csrfContent)
                .post(Map(YesNoForm.yesNo -> YesNoForm.no))
            )
          }

          result.status shouldBe SEE_OTHER
          result.header(HeaderNames.LOCATION) shouldBe Some(routes.ReceiveOtherUkDividendsController.show(taxYear).url)
        }

        "there is form data and answer to question is 'No' and the cyaModel is completed" in {
          lazy val result: WSResponse = {
            dropDividendsDB()

            emptyUserDataStub()
            insertCyaData(Some(
              DividendsCheckYourAnswersModel(ukDividends = Some(false), ukDividendsAmount = None, Some(false), None)
            ))

            authoriseIndividual()
            await(
              wsClient.url(receiveUkDividendsUrl)
                .withFollowRedirects(false)
                .withHttpHeaders(xSessionId, csrfContent)
                .post(Map(YesNoForm.yesNo -> YesNoForm.no))
            )
          }

          result.status shouldBe SEE_OTHER
          result.header(HeaderNames.LOCATION) shouldBe Some(routes.DividendsCYAController.show(taxYear).url)
        }

      }

      s"return a BAD_REQUEST($BAD_REQUEST) status" in {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(receiveUkDividendsUrl)
            .withHttpHeaders(xSessionId, csrfContent)
            .post(Map[String, String]()))
        }

        result.status shouldBe BAD_REQUEST
      }

      "returns an action when auth call fails" which {
        lazy val result: WSResponse = {
          authoriseIndividualUnauthorized()
          await(wsClient.url(receiveUkDividendsUrl)
            .withHttpHeaders(xSessionId, csrfContent)
            .post(Map[String, String]()))
        }
        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

  }

  "as an agent" when {

    ".show" should {

      "returns an action" which {
        lazy val result: WSResponse = {
          dropDividendsDB()

          emptyUserDataStub()
          insertCyaData(None)

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(receiveUkDividendsUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
            .get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }
      }

      "returns an action when auth call fails" which {
        lazy val result: WSResponse = {
          authoriseAgentUnauthorized()
          await(wsClient.url(receiveUkDividendsUrl)
            .withHttpHeaders(xSessionId, csrfContent).get())
        }
        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }
    }

    ".submit" should {

      s"return Redirect($SEE_OTHER) status" when {

        "there is form data and answer to question is 'YES'" in {
          lazy val result: WSResponse = {
            dropDividendsDB()

            emptyUserDataStub()
            insertCyaData(None)

            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"
            ))

            authoriseAgent()
            await(
              wsClient.url(receiveUkDividendsUrl)
                .withFollowRedirects(false)
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
            )
          }

          result.status shouldBe SEE_OTHER
          result.header(HeaderNames.LOCATION) shouldBe Some(routes.UkDividendsAmountController.show(taxYear).url)
        }
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status" when {

        "there is no form data" in {
          lazy val result: WSResponse = {
            dropDividendsDB()

            emptyUserDataStub()
            insertCyaData(None)

            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"
            ))

            authoriseAgent()
            await(wsClient.url(receiveUkDividendsUrl)
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
              .post(Map[String, String]()))
          }

          result.status shouldBe BAD_REQUEST
        }
      }

      "returns an action when auth call fails" when {
        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgentUnauthorized()
          await(wsClient.url(receiveUkDividendsUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post(Map[String, String]()))
        }

        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

  }

}
