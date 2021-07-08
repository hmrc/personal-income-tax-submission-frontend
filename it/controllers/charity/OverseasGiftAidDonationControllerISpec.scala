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

package controllers.charity

import common.SessionValues
import forms.YesNoForm
import helpers.PlaySessionCookieBaker
import models.charity.GiftAidCYAModel
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{GiftAidDatabaseHelper, IntegrationTest}

class OverseasGiftAidDonationControllerISpec extends IntegrationTest with GiftAidDatabaseHelper {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  lazy val controller: OverseasGiftAidDonationsController = app.injector.instanceOf[OverseasGiftAidDonationsController]
  val taxYear: Int = 2022

  val requiredSessionData: Some[GiftAidCYAModel] = Some(GiftAidCYAModel(oneOffDonationsViaGiftAid = Some(false)))

    "as an individual" when {

      ".show" should {

        "returns an action" which {
          lazy val result: WSResponse = {
            dropGiftAidDB()

            emptyUserDataStub()
            insertCyaData(requiredSessionData)

            authoriseIndividual()
            await(
              wsClient
                .url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/overseas-charity-donations-using-gift-aid")
                .withHttpHeaders(xSessionId, csrfContent)
                .get()
            )
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

        }

      }

      ".submit" should {

        s"return an OK($OK) status" in {
          lazy val result: WSResponse = {
            dropGiftAidDB()

            emptyUserDataStub()
            insertCyaData(requiredSessionData)

            authoriseIndividual()
            await(
              wsClient
                .url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/overseas-charity-donations-using-gift-aid")
                .withHttpHeaders(xSessionId, csrfContent)
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
            )
          }

          result.status shouldBe OK
        }

        s"return a BAD_REQUEST($BAD_REQUEST) status" in {
          lazy val result: WSResponse = {
            dropGiftAidDB()

            emptyUserDataStub()
            insertCyaData(requiredSessionData)

            authoriseIndividual()
            await(
              wsClient
                .url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/overseas-charity-donations-using-gift-aid")
                .withHttpHeaders(xSessionId, csrfContent)
                .post(Map[String, String]())
            )
          }

          result.status shouldBe BAD_REQUEST
        }

      }

    }

  "as an agent" when {

    ".show" should {

      "returns an action" which {
        lazy val result: WSResponse = {
          dropGiftAidDB()

          emptyUserDataStub()
          insertCyaData(requiredSessionData)

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(
            s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/overseas-charity-donations-using-gift-aid")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
            .get())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
        }
      }
    }

    ".submit" should {

      s"return an OK($OK) status" when {

        "there is form data" in {
          lazy val result: WSResponse = {
            dropGiftAidDB()

            emptyUserDataStub()
            insertCyaData(requiredSessionData)

            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"))

            authoriseAgent()
            await(
              wsClient.url(
                s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/overseas-charity-donations-using-gift-aid")
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
            dropGiftAidDB()

            emptyUserDataStub()
            insertCyaData(requiredSessionData)

            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"
            ))

            authoriseAgent()
            await(wsClient.url(
              s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/overseas-charity-donations-using-gift-aid")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
              .post(Map[String, String]()))
          }

          result.status shouldBe BAD_REQUEST
        }
      }

    }

  }

}
