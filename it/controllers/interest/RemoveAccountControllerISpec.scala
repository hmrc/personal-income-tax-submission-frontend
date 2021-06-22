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

import forms.YesNoForm
import models.interest.{InterestAccountModel, InterestCYAModel}
import play.api.http.Status.{BAD_REQUEST, OK, UNAUTHORIZED, SEE_OTHER}
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{IntegrationTest, InterestDatabaseHelper}

class RemoveAccountControllerISpec extends IntegrationTest with InterestDatabaseHelper {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  val taxYear: Int = 2022
  val amount: BigDecimal = 25

  ".show untaxed" should {

    s"returns an action" when {

      "there is CYA data in session" which {
        lazy val result = {
          dropInterestDB()
          emptyUserDataStub()
          insertCyaData(Some(InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
            Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", amount)))
          )))
          authoriseIndividual()
          await(wsClient.url(s"$startUrl/$taxYear/interest/remove-untaxed-interest-account?accountId=UntaxedId")
            .withHttpHeaders(xSessionId, csrfContent)
            .get())
        }

        s"has an OK($OK) status" in {
          result.status shouldBe OK
        }

      }

      "there is no CYA data in session" which {
        lazy val result = {
          dropInterestDB()
          emptyUserDataStub()
          insertCyaData(None)
          authoriseIndividual()
          stubGet(s"/income-through-software/return/$taxYear/view", OK, "<title>Overview Page</title>")
          await(wsClient.url(s"$startUrl/$taxYear/interest/remove-untaxed-interest-account?accountId=UntaxedId")
            .withHttpHeaders(xSessionId, csrfContent)
            .withFollowRedirects(false)
            .get())
        }

        s"has a SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the correct URL" in {
          result.headers("Location").head shouldBe "http://localhost:11111/income-through-software/return/2022/view"

        }

        "the authorization fails" which {
          lazy val result = {
            authoriseIndividualUnauthorized()
            await(wsClient.url(s"$startUrl/$taxYear/interest/remove-untaxed-interest-account?accountId=UntaxedId")
              .withHttpHeaders(xSessionId, csrfContent)
              .get())
          }

          s"has an Unauthorised($UNAUTHORIZED) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }

      }

    }
  }

  ".show taxed" should {

    s"returns an action" when {

      "there is CYA data in session" which {
        lazy val result = {
          dropInterestDB()
          emptyUserDataStub()
          insertCyaData(Some(InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
            Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", amount)))
          )))
          authoriseIndividual()
          await(wsClient.url(s"$startUrl/$taxYear/interest/remove-taxed-interest-account?accountId=TaxedId")
            .withHttpHeaders(xSessionId, csrfContent)
            .get())
        }

        s"has an OK($OK) status" in {
          result.status shouldBe OK
        }

      }

      "there is no CYA data in session" which {
        lazy val result = {
          dropInterestDB()
          emptyUserDataStub()
          insertCyaData(None)
          authoriseIndividual()
          stubGet(s"/income-through-software/return/$taxYear/view", OK, "<title>Overview Page</title>")
          await(wsClient.url(s"$startUrl/$taxYear/interest/remove-taxed-interest-account?accountId=TaxedId")
            .withHttpHeaders(xSessionId, csrfContent)
            .withFollowRedirects(false)
            .get())
        }

        s"has a SEE_OTHER($SEE_OTHER) status" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the correct URL" in {
          result.headers("Location").head shouldBe "http://localhost:11111/income-through-software/return/2022/view"

        }

        "the authorization fails" which {
          lazy val result = {
            authoriseIndividualUnauthorized()
            await(wsClient.url(s"$startUrl/$taxYear/interest/remove-taxed-interest-account?accountId=TaxedId")
              .withHttpHeaders(xSessionId, csrfContent)
              .get())
          }

          s"has an Unauthorised($UNAUTHORIZED) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }

      }

    }

    ".submit untaxed" should {

      s"return an OK($OK) status" when {

        "there is no CYA data in session" in {
          lazy val result: WSResponse = {
            dropInterestDB()
            emptyUserDataStub()
            insertCyaData(None)
            authoriseIndividual()
            await(
              wsClient.url(s"$startUrl/$taxYear/interest/remove-untaxed-interest-account?accountId=UntaxedId")
                .withHttpHeaders(xSessionId, csrfContent)
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
            )
          }

          result.status shouldBe OK
        }

      }

      s"return a BAD_REQUEST($BAD_REQUEST) status" when {

        "when the user clicks continue without selecting a remove option" in {
          lazy val result: WSResponse = {
            dropInterestDB()
            emptyUserDataStub()
            insertCyaData(Some(InterestCYAModel(
              Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
              Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", amount)))
            )))
            authoriseIndividual()
            await(wsClient.url(s"$startUrl/$taxYear/interest/remove-untaxed-interest-account?accountId=UntaxedId")
              .withHttpHeaders(xSessionId, csrfContent)
              .post(Map[String, String]()))
          }

          result.status shouldBe BAD_REQUEST
        }
      }

      "returns an action when auth call fails" which {
        lazy val result: WSResponse = {
          dropInterestDB()
          emptyUserDataStub()
          insertCyaData(Some(InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
            Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", amount)))
          )))
          authoriseIndividualUnauthorized()
          await(wsClient.url(s"$startUrl/$taxYear/interest/remove-untaxed-interest-account?accountId=UntaxedId")
            .withHttpHeaders(xSessionId, csrfContent)
            .post(Map(YesNoForm.yesNo -> YesNoForm.yes)))
        }

        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

    ".submit taxed" should {

      s"return an OK($OK) status" when {

        "there is no CYA data in session" in {
          lazy val result: WSResponse = {
            authoriseIndividual()
            await(
              wsClient.url(s"$startUrl/$taxYear/interest/remove-taxed-interest-account?accountId=TaxedId")
                .withHttpHeaders(xSessionId, csrfContent)
                .post(Map(YesNoForm.yesNo -> YesNoForm.yes))
            )
          }

          result.status shouldBe OK
        }

      }

      s"return a BAD_REQUEST($BAD_REQUEST) status" which {

        "when the user clicks continue without selecting a remove option" in {
          lazy val result: WSResponse = {
            dropInterestDB()
            emptyUserDataStub()
            insertCyaData(Some(InterestCYAModel(
              Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
              Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", amount)))
            )))
            authoriseIndividual()
            await(wsClient.url(s"$startUrl/$taxYear/interest/remove-taxed-interest-account?accountId=TaxedId")
              .withHttpHeaders(xSessionId, csrfContent)
              .post(Map[String, String]()))
          }

          result.status shouldBe BAD_REQUEST
        }
      }

      "returns an action when auth call fails" which {
        lazy val result: WSResponse = {
          dropInterestDB()
          emptyUserDataStub()
          insertCyaData(Some(InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
            Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", amount)))
          )))
          authoriseIndividualUnauthorized()
          await(wsClient.url(s"$startUrl/$taxYear/interest/remove-taxed-interest-account?accountId=TaxedId")
            .withHttpHeaders(xSessionId, csrfContent)
            .post(Map(YesNoForm.yesNo -> YesNoForm.yes)))
        }

        "has an UNAUTHORIZED(401) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

  }

}
