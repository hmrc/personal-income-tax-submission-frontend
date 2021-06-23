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
import models.mongo.InterestUserDataModel
import play.api.http.Status._
import play.api.libs.ws.{WSClient, WSResponse}
import play.mvc.Http.HeaderNames
import utils.{IntegrationTest, InterestDatabaseHelper}

class AccountsControllerISpec extends IntegrationTest with InterestDatabaseHelper {

  val yesNoFormNo: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
  val yesNoFormYes: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
  val yesNoFormEmpty: Map[String, String] = Map(YesNoForm.yesNo -> "")

  val taxYear: Int = 2022
  val amount: BigDecimal = 25
  val someNino = "AB654321C"

  val untaxedUrl = s"$appUrl/$taxYear/interest/accounts-with-untaxed-uk-interest"
  val taxedUrl = s"$appUrl/$taxYear/interest/accounts-with-taxed-uk-interest"

  s"Calling GET $untaxedUrl" should {

    s"return an OK($OK)" when {

      "there is cya session data" in {
        dropInterestDB()

        await(interestDatabase.create(InterestUserDataModel(
          sessionId, mtditid, someNino, taxYear,
          Some(InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
            Some(false), None
          ))
        )))

        val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        val result: WSResponse = {
          authoriseIndividual(Some(someNino))
          await(wsClient.url(untaxedUrl)
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

      "there is no cya data in session" in {
        dropInterestDB()

        await(interestDatabase.create(InterestUserDataModel(
          sessionId, mtditid, someNino, taxYear, None
        )))

        val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        val result: WSResponse = {
          authoriseIndividual(Some(someNino))
          stubGet("/income-through-software/return/2022/view", SEE_OTHER, "overview")
          await(wsClient.url(untaxedUrl)
            .withHttpHeaders(
              xSessionId,
              "mtditid" -> mtditid,
              csrfContent,
              HeaderNames.COOKIE -> playSessionCookie
            )
            .withFollowRedirects(false)
            .get())
        }
        resetWiremock()
        result.status shouldBe SEE_OTHER
        result.headers("Location").head.contains("/income-through-software/return/2022/view") shouldBe true
      }
    }

    "redirect to the untaxed interest page" when {

      "the cya data in session does not contain an untaxed interest account" in {
        dropInterestDB()

        await(interestDatabase.create(InterestUserDataModel(
          sessionId, mtditid, someNino, taxYear,
          Some(InterestCYAModel(
            Some(true), None,
            Some(false), None
          ))
        )))

        val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        val result: WSResponse = {
          authoriseIndividual(Some(someNino))
          await(wsClient.url(untaxedUrl)
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
        result.headers("Location").head.contains("/income-through-software/return/personal-income/2022/interest/untaxed-uk-interest") shouldBe true
      }
    }
  }

  s"Calling GET $taxedUrl" should {

    s"return an OK($OK)" when {

      "there is cya session data" in {
        dropInterestDB()

        await(interestDatabase.create(InterestUserDataModel(
          sessionId, mtditid, someNino, taxYear,
          Some(InterestCYAModel(
            Some(false), None,
            Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", amount)))
          ))
        )))

        val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        val result: WSResponse = {
          authoriseIndividual(Some(someNino))
          await(wsClient.url(taxedUrl)
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

      "there is no cya data in session" in {
        dropInterestDB()

        await(interestDatabase.create(InterestUserDataModel(
          sessionId, mtditid, someNino, taxYear, None
        )))

        val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        val result: WSResponse = {
          authoriseIndividual(Some(someNino))
          stubGet("/income-through-software/return/2022/view", SEE_OTHER, "overview")
          await(wsClient.url(taxedUrl)
            .withHttpHeaders(
              xSessionId,
              "mtditid" -> mtditid,
              csrfContent,
              HeaderNames.COOKIE -> playSessionCookie
            )
            .withFollowRedirects(false)
            .get())
        }
        resetWiremock()
        result.status shouldBe SEE_OTHER
        result.headers("Location").head.contains("/income-through-software/return/2022/view") shouldBe true
      }
    }

    "redirect to the taxed interest page" when {

      "the cya data in session does not contain a taxed interest account" in {
        dropInterestDB()

        await(interestDatabase.create(InterestUserDataModel(
          sessionId, mtditid, someNino, taxYear,
          Some(InterestCYAModel(
            Some(false), None,
            Some(true), None
          ))
        )))

        val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        val result: WSResponse = {
          authoriseIndividual(Some(someNino))
          await(wsClient.url(taxedUrl)
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
        result.headers("Location").head.contains("/income-through-software/return/personal-income/2022/interest/taxed-uk-interest") shouldBe true
      }
    }
  }

  s"Calling POST $untaxedUrl" when {

    "the user has selected 'no' to adding an extra account" when {

      "cya data is complete" should {

        "redirect to the interest cya page" in {
          dropInterestDB()

          await(interestDatabase.create(InterestUserDataModel(
            sessionId, mtditid, someNino, taxYear,
            Some(InterestCYAModel(
              Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
              Some(false), None
            ))
          )))

          val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> taxYear.toString
          ))

          val result: WSResponse = {
            authoriseIndividual(Some(someNino))
            await(wsClient.url(untaxedUrl)
              .withHttpHeaders(
                xSessionId,
                "mtditid" -> mtditid,
                csrfContent,
                HeaderNames.COOKIE -> playSessionCookie
              )
              .withFollowRedirects(false)
              .post(yesNoFormNo))
          }

          result.status shouldBe SEE_OTHER
          result.headers("Location").head.contains("/income-through-software/return/personal-income/2022/interest/check-interest") shouldBe true
        }
      }

      "cya taxed data is not yet complete" should {

        "redirect to the taxed interest page" in {
          dropInterestDB()

          await(interestDatabase.create(InterestUserDataModel(
            sessionId, mtditid, someNino, taxYear,
            Some(InterestCYAModel(
              Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
              None, None
            ))
          )))

          emptyUserDataStub()

          val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> taxYear.toString
          ))

          val result: WSResponse = {
            authoriseIndividual(Some(someNino))
            await(wsClient.url(untaxedUrl)
              .withHttpHeaders(
                xSessionId,
                "mtditid" -> mtditid,
                csrfContent,
                HeaderNames.COOKIE -> playSessionCookie
              )
              .withFollowRedirects(false)
              .post(yesNoFormNo))
          }

          result.status shouldBe SEE_OTHER
          result.headers("Location").head.contains("/income-through-software/return/personal-income/2022/interest/taxed-uk-interest") shouldBe true
        }
      }
    }

    "the user has selected to add an extra account" should {

      "redirect to the untaxed interest amount page" in {
        val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        val result: WSResponse = {
          dropInterestDB()

          emptyUserDataStub()
          insertCyaData(Some(InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
            Some(false), None
          )), overrideNino = Some(someNino))

          authoriseIndividual(Some(someNino))
          await(wsClient.url(untaxedUrl)
            .withHttpHeaders(
              xSessionId,
              "mtditid" -> mtditid,
              csrfContent,
              HeaderNames.COOKIE -> playSessionCookie
            )
            .withFollowRedirects(false)
            .post(yesNoFormYes))
        }

        result.status shouldBe SEE_OTHER
        result.headers("Location").head should include("/income-through-software/return/personal-income/2022/interest/add-untaxed-uk-interest-account")
      }
    }

    "the user has not selected whether or not to add an extra account" should {

      "return a BAD_REQUEST" in {

        dropInterestDB()

        await(interestDatabase.create(InterestUserDataModel(
          sessionId, mtditid, someNino, taxYear,
          Some(InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
            Some(false), None
          ))
        )))

        val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        val result: WSResponse = {
          authoriseIndividual(Some(someNino))
          await(wsClient.url(untaxedUrl)
            .withHttpHeaders(
              xSessionId,
              "mtditid" -> mtditid,
              csrfContent,
              HeaderNames.COOKIE -> playSessionCookie
            )
            .withFollowRedirects(false)
            .post(yesNoFormEmpty))
        }

        result.status shouldBe BAD_REQUEST
      }
    }
  }

  s"Calling POST $taxedUrl" when {

    "the user has selected 'no' to adding an extra account" when {

      "cya data is complete" should {

        "redirect to the interest cya page" in {
          val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> taxYear.toString
          ))

          val result: WSResponse = {
            dropInterestDB()

            await(interestDatabase.create(InterestUserDataModel(
              sessionId, mtditid, someNino, taxYear,
              Some(InterestCYAModel(
                Some(false), None,
                Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", amount)))
              ))
            )))
            emptyUserDataStub()

            authoriseIndividual(Some(someNino))
            await(wsClient.url(taxedUrl)
              .withHttpHeaders(
                xSessionId,
                "mtditid" -> mtditid,
                csrfContent,
                HeaderNames.COOKIE -> playSessionCookie
              )
              .withFollowRedirects(false)
              .post(yesNoFormNo))
          }

          result.status shouldBe SEE_OTHER
          result.headers("Location").head.contains("/income-through-software/return/personal-income/2022/interest/check-interest") shouldBe true
        }
      }
    }

    "the user has selected to add an extra account" should {

      "redirect to the untaxed interest amount page" in {
        val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        val result: WSResponse = {
          dropInterestDB()

          emptyUserDataStub()
          insertCyaData(Some(InterestCYAModel(
            Some(false), None,
            Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", amount)))
          )), overrideNino = Some(someNino))

          authoriseIndividual(Some(someNino))
          await(wsClient.url(taxedUrl)
            .withHttpHeaders(
              xSessionId,
              "mtditid" -> mtditid,
              csrfContent,
              HeaderNames.COOKIE -> playSessionCookie
            )
            .withFollowRedirects(false)
            .post(yesNoFormYes))
        }

        result.status shouldBe SEE_OTHER
        result.headers("Location").head should include("/income-through-software/return/personal-income/2022/interest/add-taxed-uk-interest-account")
      }
    }

    "the user has not selected whether or not to add an extra account" should {

      "return a BAD_REQUEST" in {

        dropInterestDB()

        await(interestDatabase.create(InterestUserDataModel(
          sessionId, mtditid, someNino, taxYear,
          Some(InterestCYAModel(
            Some(false), None,
            Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", amount)))
          ))
        )))

        val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        val result: WSResponse = {
          authoriseIndividual(Some(someNino))
          await(wsClient.url(taxedUrl)
            .withHttpHeaders(
              xSessionId,
              "mtditid" -> mtditid,
              csrfContent,
              HeaderNames.COOKIE -> playSessionCookie
            )
            .withFollowRedirects(false)
            .post(yesNoFormEmpty))
        }

        result.status shouldBe BAD_REQUEST
      }
    }
  }
}