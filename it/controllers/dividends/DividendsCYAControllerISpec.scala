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
import models.dividends.{DividendsCheckYourAnswersModel, DividendsSubmissionModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, ViewHelpers}


class DividendsCYAControllerISpec extends IntegrationTest with ViewHelpers{


  val connector: DividendsSubmissionConnector = app.injector.instanceOf[DividendsSubmissionConnector]

  val taxYear = 2022

  val ukDividends: BigDecimal = 10
  val otherDividends: BigDecimal = 10.50
  val dividendsCheckYourAnswersUrl = s"$startUrl/$taxYear/dividends/check-income-from-dividends"

  val changeUkDividendsHref = "/income-through-software/return/personal-income/2022/dividends/dividends-from-uk-companies"
  val changeUkDividendsAmountHref = "/income-through-software/return/personal-income/2022/dividends/how-much-dividends-from-uk-companies"
  val changeOtherDividendsHref = "/income-through-software/return/personal-income/2022/dividends/dividends-from-uk-trusts-or-open-ended-investment-companies"
  val changeOtherDividendsAmountHref = "/income-through-software/return/personal-income/2022/dividends/how-much-dividends-from-uk-trusts-and-open-ended-investment-companies"


  lazy val dividendsBody: DividendsSubmissionModel = DividendsSubmissionModel(
    Some(ukDividends),
    Some(otherDividends)
  )

  lazy val dividendsModel:DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(Some(true),
    Some(ukDividends), Some(true), Some(otherDividends))

  object Selectors {
    def cyaTitle(i: Int): String = s"#main-content > div > div > dl > div:nth-child($i) > dt"
    def cyaValue(i: Int): String = s"#main-content > div > div > dl > div:nth-child($i) > dd.govuk-summary-list__value"
    def cyaChangeLink(i: Int): String = s"#main-content > div > div > dl > div:nth-child($i) > dd.govuk-summary-list__actions > a"
    val captionSelector = "#main-content > div > div > h1 > span"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
  }

  object ExpectedResults {

    object IndividualExpected {
      val expectedTitle = "Check your income from dividends"
      val expectedH1 = "Check your income from dividends"
      val expectedErrorTitle = s"Error: $expectedTitle"
      val UkDividendsTitle = "Dividends from UK-based companies"
      val ukDividendsAmountTitle = "Value of dividends from UK-based companies"
      val otherDividendsTitle = "Dividends from UK-based unit trusts or open-ended investment companies"
      val otherDividendsAmountTitle = "Value of dividends from UK-based unit trusts or open-ended investment companies"
    }

    object AllExpected {
      val expectedCaption = s"Dividends for 6 April ${taxYear - 1} to 5 April $taxYear"
      val yesNoExpectedAnswer: Boolean => String = isYes => if (isYes) "Yes" else "No"
      val ukDividendsAmount = "£10"
      val otherDividendsAmount = "£10.50"
      val continueButtonText = "Save and continue"
      val continueButtonLink = "/income-through-software/return/personal-income/2022/dividends/check-income-from-dividends"
      val changeLinkExpected = "Change"

      val changeUkDividendsIndividualHiddenText = "if you got dividends from UK-based companies."
      val changeUkDividendsAmountIndividualHiddenText = "how much you got from UK-based companies."
      val changeOtherDividendsIndividualHiddenText = "if you got dividends from trusts or open-ended investment companies based in the UK."
      val changeOtherDividendsAmountIndividualHiddenText = "how much you got in dividends from trusts or open-ended investment companies based in the UK."
      val changeUkDividendsAgentHiddenText = "if your client got dividends from UK-based companies."
      val changeUkDividendsAmountAgentHiddenText = "how much your client got from UK-based companies."
      val changeOtherDividendsAgentHiddenText = "if your client got dividends from trusts or open-ended investment companies based in the UK."
      val changeOtherDividendsAmountAgentHiddenText = "how much your client got in dividends from trusts or open-ended investment companies based in the UK."
    }
  }


  ".show" should {

    import ExpectedResults.AllExpected._
    import ExpectedResults.IndividualExpected._
    import Selectors._

    s"as an Individual" when {

      " renders CYA page with correct content when there is data in session" which {

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.DIVIDENDS_CYA -> Json.prettyPrint(Json.toJson(dividendsModel))))

        lazy val result = {
          authoriseIndividual()
          urlGet(dividendsCheckYourAnswersUrl, headers = Seq(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck"))
        }

        s"has an OK($OK) status" in {
          result.status shouldBe OK
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)


        titleCheck(expectedTitle)
        h1Check(expectedH1 + " " + expectedCaption)
        textOnPageCheck(expectedCaption, Selectors.captionSelector)
        "has an area for section 1" which {
          textOnPageCheck(UkDividendsTitle, Selectors.cyaTitle(1))
          textOnPageCheck(yesNoExpectedAnswer(true), Selectors.cyaValue(1))
          linkCheck(s"$changeLinkExpected $changeUkDividendsIndividualHiddenText", cyaChangeLink(1), changeUkDividendsHref)
        }
        "has an area for section 2" which {
          textOnPageCheck(ukDividendsAmountTitle, cyaTitle(2))
          textOnPageCheck(ukDividendsAmount, cyaValue(2))
          linkCheck(s"$changeLinkExpected $changeUkDividendsAmountIndividualHiddenText", cyaChangeLink(2), changeUkDividendsAmountHref)
        }
        "has an area for section 3" which {
          textOnPageCheck(otherDividendsTitle, cyaTitle(3))
          textOnPageCheck(yesNoExpectedAnswer(true), cyaValue(3))
          linkCheck(s"$changeLinkExpected $changeOtherDividendsIndividualHiddenText", cyaChangeLink(3), changeOtherDividendsHref)
        }
        "has an area for section 4" which {
          textOnPageCheck(otherDividendsAmountTitle, cyaTitle(4))
          textOnPageCheck(otherDividendsAmount, cyaValue(4))
          linkCheck(s"$changeLinkExpected $changeOtherDividendsAmountIndividualHiddenText", cyaChangeLink(4), changeOtherDividendsAmountHref)
        }

        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(continueButtonLink, continueButtonFormSelector)

        welshToggleCheck(ENGLISH)

      }

      "renders CYA page without yesNo Content when there is a prior submission" which {

      lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
        SessionValues.DIVIDENDS_PRIOR_SUB -> Json.prettyPrint(Json.toJson(dividendsBody))))

      lazy val result = {
        authoriseIndividual()
        urlGet(dividendsCheckYourAnswersUrl, headers = Seq(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck"))
      }

      s"has an OK($OK) status" in {
        result.status shouldBe OK
      }

      implicit val document: () => Document = () => Jsoup.parse(result.body)

      titleCheck(expectedTitle)
      h1Check(expectedH1 + " " + expectedCaption)
      textOnPageCheck(expectedCaption, Selectors.captionSelector)
      "has an area for section 1" which {
        textOnPageCheck(ukDividendsAmountTitle, cyaTitle(1))
        textOnPageCheck(ukDividendsAmount, cyaValue(1))
        linkCheck(s"$changeLinkExpected $changeUkDividendsAmountIndividualHiddenText", cyaChangeLink(1), changeUkDividendsAmountHref)
      }
      "has an area for section 2" which {
        textOnPageCheck(otherDividendsAmountTitle, cyaTitle(2))
        textOnPageCheck(otherDividendsAmount, cyaValue(2))
        linkCheck(s"$changeLinkExpected $changeOtherDividendsAmountIndividualHiddenText", cyaChangeLink(2), changeOtherDividendsAmountHref)
      }

      buttonCheck(continueButtonText, continueButtonSelector)
      formPostLinkCheck(continueButtonLink, continueButtonFormSelector)

      welshToggleCheck(ENGLISH)

    }


      "renders CYA page without amount when boolean answers are false" which {

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.DIVIDENDS_CYA -> Json.prettyPrint(Json.toJson(DividendsCheckYourAnswersModel(
            Some(false),None, Some(false), Some(otherDividends))))))

        lazy val result = {
          authoriseIndividual()
          urlGet(dividendsCheckYourAnswersUrl, headers = Seq(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck"))
        }

        s"has an OK($OK) status" in {
          result.status shouldBe OK
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedTitle)
        h1Check(expectedH1 + " " + expectedCaption)
        textOnPageCheck(expectedCaption, Selectors.captionSelector)
        "has an area for section 1" which {
          textOnPageCheck(UkDividendsTitle, cyaTitle(1))
          textOnPageCheck(yesNoExpectedAnswer(false), cyaValue(1))
          linkCheck(s"$changeLinkExpected $changeUkDividendsIndividualHiddenText", cyaChangeLink(1), changeUkDividendsHref)
        }
        "has an area for section 2" which {
          textOnPageCheck(otherDividendsTitle, cyaTitle(2))
          textOnPageCheck(yesNoExpectedAnswer(false), cyaValue(2))
          linkCheck(s"$changeLinkExpected $changeOtherDividendsIndividualHiddenText", cyaChangeLink(2), changeOtherDividendsHref)
        }

        buttonCheck(continueButtonText, continueButtonSelector)
        formPostLinkCheck(continueButtonLink, continueButtonFormSelector)

        welshToggleCheck(ENGLISH)

      }


    "redirects user to overview page there is no CYA data in session" which {
        lazy val result = {
          authoriseIndividual()
          stubGet(s"/income-through-software/return/$taxYear/view", OK, "<title>Overview Page</title>")
          urlGet(dividendsCheckYourAnswersUrl)
        }

        s"has an OK($OK) status" in {
          result.status shouldBe OK
        }

      }

      "the authorization fails" which {
        lazy val result = {
          authoriseIndividualUnauthorized()
          stubGet(s"/income-through-software/return/$taxYear/view", OK, "<title>Overview Page</title>")


          await(wsClient.url(dividendsCheckYourAnswersUrl)
            .get())
        }

        s"has an OK($UNAUTHORIZED) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }

    }

  }

  ".submit" should {

    "return an action" which {

      s"has an OK(200) status" in {
        authoriseIndividual()
        stubPut(s"/income-tax-dividends/income-tax/nino/AA123456A/sources\\?taxYear=$taxYear", NO_CONTENT, "{}")
        stubGet("/income-through-software/return/2022/view", OK, "{}")

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.DIVIDENDS_CYA -> Json.prettyPrint(Json.toJson(DividendsCheckYourAnswersModel(
            ukDividends = Some(true),
            Some(ukDividends),
            otherUkDividends = Some(true),
            Some(otherDividends)
          ))),
        ))

        val result: WSResponse = await(wsClient.url(dividendsCheckYourAnswersUrl)
          .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
          .post("{}"))

        result.status shouldBe OK
      }

      s"handle no nino is in the enrolments" in {
        lazy val result = {
          authoriseIndividual(false)
          stubGet(s"/income-through-software/return/$taxYear/view", OK, "")
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.DIVIDENDS_CYA -> Json.prettyPrint(Json.toJson(DividendsCheckYourAnswersModel(
              ukDividends = Some(true),
              Some(ukDividends),
              otherUkDividends = Some(true),
              Some(otherDividends)
            ))),
          ))

          await(wsClient.url(dividendsCheckYourAnswersUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post("{}"))
        }

        result.status shouldBe NOT_FOUND
      }

      "the authorization fails" in {
        lazy val result = {
          authoriseIndividualUnauthorized()
          stubGet(s"/income-through-software/return/$taxYear/view", OK, "")
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.DIVIDENDS_CYA -> Json.prettyPrint(Json.toJson(DividendsCheckYourAnswersModel(ukDividends = Some(true),
              Some(ukDividends),
              otherUkDividends = Some(true),
              Some(otherDividends)))),
          ))
          await(wsClient.url(dividendsCheckYourAnswersUrl)
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
            .post("{}"))
        }

        result.status shouldBe UNAUTHORIZED
      }
    }

    "return a service is unavailable" when {

      "one is retrieved from the endpoint" in {
        authoriseIndividual()
        stubPut(s"/income-tax-dividends/income-tax/nino/AA123456A/sources\\?taxYear=$taxYear", SERVICE_UNAVAILABLE, "{}")

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.DIVIDENDS_CYA -> Json.prettyPrint(Json.toJson(DividendsCheckYourAnswersModel(
            ukDividends = Some(true),
            Some(ukDividends),
            otherUkDividends = Some(true),
            Some(otherDividends)
          ))),
        ))

        val result: WSResponse = await(wsClient.url(dividendsCheckYourAnswersUrl)
          .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
          .post("{}"))

        result.status shouldBe SERVICE_UNAVAILABLE
      }

    }

    "return an internal server error" when {

      "an unhandled response is returned from the income-tax backend" in {
        authoriseIndividual()
        stubPut(s"/income-tax-dividends/income-tax/nino/AA123456A/sources\\?taxYear=$taxYear", IM_A_TEAPOT, "{}")

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.DIVIDENDS_CYA -> Json.prettyPrint(Json.toJson(DividendsCheckYourAnswersModel(
            ukDividends = Some(true),
            Some(ukDividends),
            otherUkDividends = Some(true),
            Some(otherDividends)
          ))),
        ))

        val result: WSResponse = await(wsClient.url(dividendsCheckYourAnswersUrl)
          .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
          .post("{}"))

        result.status shouldBe INTERNAL_SERVER_ERROR
      }
    }
  }
}
