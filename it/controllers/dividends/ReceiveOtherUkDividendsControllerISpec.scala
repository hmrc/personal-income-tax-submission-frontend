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
import controllers.dividends.routes.{DividendsCYAController, OtherUkDividendsAmountController}
import forms.YesNoForm
import helpers.PlaySessionCookieBaker
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import models.dividends.{DividendsCheckYourAnswersModel, DividendsPriorSubmission}
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{IntegrationTest, ViewHelpers}

class ReceiveOtherUkDividendsControllerISpec extends IntegrationTest with ViewHelpers {


  val taxYear: Int = 2022
  val amount: BigDecimal = 500
  val receivedOtherDividendsUrl = s"$startUrl/$taxYear/dividends/dividends-from-uk-trusts-or-open-ended-investment-companies"

  "as an individual" when {

    ".show" should {

      "redirects user to overview page when there is no cyaData in session" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          stubGet(s"/income-through-software/return/$taxYear/view", 200, "overview page content")
          await(wsClient.url(receivedOtherDividendsUrl).get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
          result.body shouldBe "overview page content"
        }
      }

      "redirects user dividendCya page when there is prior submission data but no cyaData in session" which {
        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.DIVIDENDS_PRIOR_SUB -> DividendsPriorSubmission(ukDividends = None, Some(100)).asJsonString
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          stubGet(s"/income-through-software/return/$taxYear/view", 200, "overview page content")
          await(wsClient.url(receivedOtherDividendsUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get()
          )
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        implicit val document = () => Jsoup.parse(result.body)

        titleCheck("Check your income from dividends")
        h1Check("Check your income from dividends Dividends for 6 April 2021 to 5 April 2022")
      }

      "returns an action when data only cyaData is in session" which {
        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.DIVIDENDS_CYA -> DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount),
            otherUkDividends = Some(true), otherUkDividendsAmount = None).asJsonString
        ))

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(receivedOtherDividendsUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck").get()
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
          authoriseIndividual()
          await(
            wsClient.url(receivedOtherDividendsUrl)
              .withFollowRedirects(false)
              .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
          )
        }

        s"return a Redirect($SEE_OTHER) to the other uk dividend amount page" in {
          result.status shouldBe SEE_OTHER
          result.header(HeaderNames.LOCATION) shouldBe Some(OtherUkDividendsAmountController.show(taxYear).url)
        }
      }

      s"return a Redirect($SEE_OTHER) to dividend cya page" in {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(
            wsClient.url(receivedOtherDividendsUrl)
              .withFollowRedirects(false)
              .post(Map(YesNoForm.yesNo -> YesNoForm.no))
          )
        }

        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION) shouldBe Some(DividendsCYAController.show(taxYear).url)
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status" in {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(receivedOtherDividendsUrl).post(Map[String, String]()))
        }

        result.status shouldBe BAD_REQUEST
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
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(receivedOtherDividendsUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie)
            .get())
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
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"
            ))

            authoriseAgent()
            await(
              wsClient.url(receivedOtherDividendsUrl)
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
            )
          }

          result.status shouldBe OK
        }
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status" when {

        "there is no form data" in {
          lazy val result: WSResponse = {
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"
            ))

            authoriseAgent()
            await(wsClient.url(receivedOtherDividendsUrl)
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
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
            .post(Map[String, String]()))        }
        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

  }

}
