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
import models.dividends.{DividendsCheckYourAnswersModel, DividendsPriorSubmission}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{DividendsDatabaseHelper, IntegrationTest, ViewHelpers}

class ReceiveOtherUkDividendsControllerISpec extends IntegrationTest with ViewHelpers with DividendsDatabaseHelper {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  val taxYear: Int = 2022
  val amount: BigDecimal = 500
  val receivedOtherDividendsUrl = s"$startUrl/$taxYear/dividends/dividends-from-uk-trusts-or-open-ended-investment-companies"

  "as an individual" when {

    ".show" should {

      "redirects user to overview page when there is no cyaData in session" which {
        lazy val result: WSResponse = {
          dropDividendsDB()

          emptyUserDataStub()
          insertCyaData(None)

          authoriseIndividual()
          stubGet(s"/income-through-software/return/$taxYear/view", SEE_OTHER, "overview page content")
          await(wsClient.url(receivedOtherDividendsUrl).withHttpHeaders(xSessionId, csrfContent).withFollowRedirects(false).get())
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
        }
      }

      "redirects user dividendCya page when there is prior submission data but no cyaData in session" which {
        lazy val result: WSResponse = {
          dropDividendsDB()

          userDataStub(IncomeSourcesModel(Some(
            DividendsPriorSubmission(Some(300.43), Some(983.43))
          )), nino, taxYear)


          authoriseIndividual()

          await(wsClient.url(receivedOtherDividendsUrl)
            .withHttpHeaders(xSessionId, csrfContent).get()
          )
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck("Check your income from dividends")
        h1Check("Check your income from dividends Dividends for 6 April 2021 to 5 April 2022")
      }

      "returns an action when only cyaData is in session" which {
        lazy val result: WSResponse = {
          dropDividendsDB()

          emptyUserDataStub()
          insertCyaData(Some(DividendsCheckYourAnswersModel(
            ukDividends = Some(true), ukDividendsAmount = Some(amount),
            otherUkDividends = Some(true), otherUkDividendsAmount = None
          )))

          authoriseIndividual()
          await(wsClient.url(receivedOtherDividendsUrl)
            .withHttpHeaders(xSessionId, csrfContent).get()
          )
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck("Did you get dividends from UK-based trusts or open-ended investment companies?")
        h1Check("Did you get dividends from UK-based trusts or open-ended investment companies? Dividends for 6 April 2021 to 5 April 2022")
      }

      "returns an action when auth call fails" which {
        lazy val result: WSResponse = {
          authoriseIndividualUnauthorized()
          await(wsClient.url(receivedOtherDividendsUrl).get())
        }
        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

    ".submit" when {

      "called with the 'YES' on form" should {
        lazy val result: WSResponse = {
          dropDividendsDB()

          emptyUserDataStub()
          insertCyaData(Some(DividendsCheckYourAnswersModel(
            ukDividends = Some(true), ukDividendsAmount = Some(amount)
          )))

          authoriseIndividual()
          await(
            wsClient.url(receivedOtherDividendsUrl)
              .withFollowRedirects(false)
              .withHttpHeaders(xSessionId, csrfContent)
              .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
          )
        }

        s"return a Redirect($SEE_OTHER) to the other uk dividend amount page" in {
          result.status shouldBe SEE_OTHER
          result.header(HeaderNames.LOCATION) shouldBe Some(routes.OtherUkDividendsAmountController.show(taxYear).url)
        }
      }

      "called with a no" should {

        s"return a Redirect($SEE_OTHER) to dividend cya page" in {
          lazy val result: WSResponse = {
            dropDividendsDB()

            emptyUserDataStub()
            insertCyaData(Some(DividendsCheckYourAnswersModel(
              ukDividends = Some(true), ukDividendsAmount = Some(amount)
            )))

            authoriseIndividual()
            await(
              wsClient.url(receivedOtherDividendsUrl)
                .withFollowRedirects(false)
                .withHttpHeaders(xSessionId, csrfContent)
                .post(Map(YesNoForm.yesNo -> YesNoForm.no))
            )
          }

          result.status shouldBe SEE_OTHER
          result.header(HeaderNames.LOCATION) shouldBe Some(routes.DividendsCYAController.show(taxYear).url)
        }

      }

      s"return a BAD_REQUEST($BAD_REQUEST) status" when {

        "the user submits the form with no data" in {
          lazy val result: WSResponse = {
            dropDividendsDB()

            emptyUserDataStub()
            insertCyaData(Some(DividendsCheckYourAnswersModel(
              ukDividends = Some(true), ukDividendsAmount = Some(amount)
            )))

            authoriseIndividual()
            await(
              wsClient
                .url(receivedOtherDividendsUrl)
                .withHttpHeaders(xSessionId, csrfContent)
                .post(Map[String, String]())
            )
          }

          result.status shouldBe BAD_REQUEST
        }
      }

      "returns an action when auth call fails" which {
        lazy val result: WSResponse = {
          authoriseIndividualUnauthorized()
          await(wsClient.url(receivedOtherDividendsUrl).post(Map[String, String]()))
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
          insertCyaData(Some(DividendsCheckYourAnswersModel(
            ukDividends = Some(true), ukDividendsAmount = Some(amount)
          )))

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(
            wsClient.url(receivedOtherDividendsUrl)
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
              .get()
          )
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }
      }

      "returns an action when auth call fails" which {
        lazy val result: WSResponse = {
          authoriseAgentUnauthorized()
          await(wsClient.url(receivedOtherDividendsUrl).get())
        }
        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }
    }

    ".submit" should {

      s"return an OK($OK) status" when {

        "there is form data" in {
          lazy val result: WSResponse = {
            dropDividendsDB()

            emptyUserDataStub()
            insertCyaData(Some(DividendsCheckYourAnswersModel(
              ukDividends = Some(true), ukDividendsAmount = Some(amount)
            )))

            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"
            ))

            authoriseAgent()
            await(
              wsClient.url(receivedOtherDividendsUrl)
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
            )
          }

          result.status shouldBe OK
        }
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status" when {

        "there is no form data" in {
          lazy val result: WSResponse = {
            dropDividendsDB()

            emptyUserDataStub()
            insertCyaData(Some(DividendsCheckYourAnswersModel(
              ukDividends = Some(true), ukDividendsAmount = Some(amount)
            )))

            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"
            ))

            authoriseAgent()
            await(wsClient.url(receivedOtherDividendsUrl)
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
          await(wsClient.url(receivedOtherDividendsUrl)
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
