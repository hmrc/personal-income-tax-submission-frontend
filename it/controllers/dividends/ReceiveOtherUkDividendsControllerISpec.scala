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
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, ViewHelpers}

class ReceiveOtherUkDividendsControllerISpec extends IntegrationTest with ViewHelpers {


  val taxYear: Int = 2022
  val amount: BigDecimal = 500
  val receivedOtherDividendsUrl = s"${appUrl(port)}/$taxYear/dividends/dividends-from-uk-trusts-or-open-ended-investment-companies"

  val cyaModel = DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount),
    otherUkDividends = Some(true), otherUkDividendsAmount = None)


  object Selectors {
    val youMustAlsoSelector = "#main-content > div > div > form > div > fieldset > legend > div:nth-child(2) > p"

    def listContentSelector(i: Int): String = s"#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child($i)"

    val youDoNotNeedSelector = "#main-content > div > div > form > div > fieldset > legend > div:nth-child(4) > p"

    def investmentTitleSelector(i: Int = 3): String = s"#main-content > div > div > form > details:nth-child($i) > summary > span"

    def investmentsContentSelector(i: Int = 3)(j: Int): String = s"#main-content > div > div > form > details:nth-child($i) > div > p:nth-child($j)"

    def equalisationTitleSelector(i: Int = 4): String = s"#main-content > div > div > form > details:nth-child($i) > summary > span"

    def equalisationContentSelector(i: Int = 4): String = s"#main-content > div > div > form > details:nth-child($i) > div > p"

    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#value"

  }

  object ExpectedResults {

    object EnglishLang {
      object IndividualExpected {
        val expectedH1 = "Did you get dividends from UK-based trusts or open-ended investment companies?"
        val expectedTitle = "Did you get dividends from UK-based trusts or open-ended investment companies?"
        val expectedErrorTitle = s"Error: $expectedTitle"
        val youDoNotNeedText = "You do not need to tell us about amounts shown as 'equalisation' on your dividend voucher."
        val expectedErrorText = "Select yes if you got dividends from UK-based trusts or open-ended investment companies"
      }

      object AgentExpected {
        val expectedH1 = "Did your client get dividends from UK-based trusts or open-ended investment companies?"
        val expectedTitle = "Did your client get dividends from UK-based trusts or open-ended investment companies?"
        val expectedErrorTitle = s"Error: $expectedTitle"
        val youDoNotNeedText = "You do not need to tell us about amounts shown as 'equalisation' on your client’s dividend voucher."
        val expectedErrorText = "Select yes if your client got dividends from UK-based trusts or open-ended investment companies"

      }

      object AllExpected {
        val captionExpected = s"Dividends for 6 April ${taxYear - 1} to 5 April $taxYear"
        val youMustAlsoText = "You must also tell us about:"
        val authorisedBulletText = "authorised unit trusts"
        val investmentBulletText = "investment trusts"
        val yourDividendsBulletText = "your dividends that were automatically reinvested"
        val whatAreInvestmentText = "What are investment trusts, unit trusts and open-ended investment companies?"
        val investmentTrustText = "Investment trusts make money through buying and selling shares or assets in other companies."
        val unitTrustsText: String = "Unit trusts make money by buying and selling bonds or shares on the stock market. The fund is split " +
          "into units which an investor buys. A fund manager creates and cancels units when investors join and leave the trust."
        val openEndedText = "Open-ended investment companies are like unit trusts but create and cancel shares, rather than units, when investors join or leave."
        val whatAreEqualisationText = "What are equalisation payments?"
        val equalisationPaymentsText: String = "Equalisation payments are given to investors to make sure they’re charged fairly based " +
          "on the performance of the trust. Equalisation payments are not counted as income because they’re a return of part of an investment."
        val yesNo: Boolean => String = isYes => if (isYes) "Yes" else "No"
        val continueButtonText = "Continue"
        val continueLink = s"/income-through-software/return/personal-income/$taxYear/dividends/dividends-from-uk-trusts-or-open-ended-investment-companies"

      }
    }

    object WelshLang {

      object IndividualExpected {
        val expectedH1 = "Did you get dividends from UK-based trusts or open-ended investment companies?"
        val expectedTitle = "Did you get dividends from UK-based trusts or open-ended investment companies?"
        val expectedErrorTitle = s"Error: $expectedTitle"
        val youDoNotNeedText = "You do not need to tell us about amounts shown as 'equalisation' on your dividend voucher."
        val expectedErrorText = "Select yes if you got dividends from UK-based trusts or open-ended investment companies"
      }

      object AgentExpected {
        val expectedH1 = "Did your client get dividends from UK-based trusts or open-ended investment companies?"
        val expectedTitle = "Did your client get dividends from UK-based trusts or open-ended investment companies?"
        val expectedErrorTitle = s"Error: $expectedTitle"
        val youDoNotNeedText = "You do not need to tell us about amounts shown as 'equalisation' on your client’s dividend voucher."
        val expectedErrorText = "Select yes if your client got dividends from UK-based trusts or open-ended investment companies"

      }
      object AllExpected {
        val expectedErrorText = "Select yes if your client got dividends from UK-based trusts or open-ended investment companies"
        val captionExpected = s"Dividends for 6 April ${taxYear - 1} to 5 April $taxYear"
        val youMustAlsoText = "You must also tell us about:"
        val authorisedBulletText = "authorised unit trusts"
        val investmentBulletText = "investment trusts"
        val yourDividendsBulletText = "your dividends that were automatically reinvested"
        val whatAreInvestmentText = "What are investment trusts, unit trusts and open-ended investment companies?"
        val investmentTrustText = "Investment trusts make money through buying and selling shares or assets in other companies."
        val unitTrustsText: String = "Unit trusts make money by buying and selling bonds or shares on the stock market. The fund is split " +
          "into units which an investor buys. A fund manager creates and cancels units when investors join and leave the trust."
        val openEndedText = "Open-ended investment companies are like unit trusts but create and cancel shares, rather than units, when investors join or leave."
        val whatAreEqualisationText = "What are equalisation payments?"
        val equalisationPaymentsText: String = "Equalisation payments are given to investors to make sure they’re charged fairly based " +
          "on the performance of the trust. Equalisation payments are not counted as income because they’re a return of part of an investment."
        val yesNo: Boolean => String = isYes => if (isYes) "Yes" else "No"
        val continueButtonText = "Continue"
        val continueLink = s"/income-through-software/return/personal-income/$taxYear/dividends/dividends-from-uk-trusts-or-open-ended-investment-companies"

      }
    }
  }


  ".show" should {

    import Selectors._

    "in English" should {

      import ExpectedResults.EnglishLang._
      import ExpectedResults.EnglishLang.AllExpected._

      "as an Individual" when {

        "render correct content when if first question in dividends journey has been answered in session" which {

          lazy val result: WSResponse = {
            authoriseIndividual()
            urlGet(receivedOtherDividendsUrl, headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(cyaModel)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(IndividualExpected.expectedTitle)
          h1Check(IndividualExpected.expectedH1 + " " + captionExpected)
          textOnPageCheck(youMustAlsoText, youMustAlsoSelector)
          textOnPageCheck(authorisedBulletText, listContentSelector(1))
          textOnPageCheck(investmentBulletText, listContentSelector(2))
          textOnPageCheck(yourDividendsBulletText, listContentSelector(3))
          textOnPageCheck(IndividualExpected.youDoNotNeedText, youDoNotNeedSelector)
          radioButtonCheck(yesNo(true), 1)
          radioButtonCheck(yesNo(false), 2)
          textOnPageCheck(whatAreInvestmentText, investmentTitleSelector())
          textOnPageCheck(investmentTrustText, investmentsContentSelector()(1))
          textOnPageCheck(unitTrustsText, investmentsContentSelector()(2))
          textOnPageCheck(openEndedText, investmentsContentSelector()(3))
          textOnPageCheck(whatAreEqualisationText, equalisationTitleSelector())
          textOnPageCheck(equalisationPaymentsText, equalisationContentSelector())

          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)

          welshToggleCheck(ENGLISH)
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
        "redirects user dividendCya page when there is prior submission data but no cyaData in session" which {
          val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.DIVIDENDS_PRIOR_SUB -> DividendsPriorSubmission(ukDividends = None, Some(100)).asJsonString
          ))

          lazy val result: WSResponse = {
            authoriseIndividual()
            stubGet(s"/income-through-software/return/$taxYear/view", OK, "overview page content")
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

      }

      "as an Agent" when {

        "render correct content when if first question in dividends journey has been answered in session" which {

          lazy val result: WSResponse = {
            authoriseAgent()
            urlGet(receivedOtherDividendsUrl, headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(cyaModel)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(AgentExpected.expectedTitle)
          h1Check(AgentExpected.expectedH1 + " " + captionExpected)
          textOnPageCheck(youMustAlsoText, youMustAlsoSelector)
          textOnPageCheck(authorisedBulletText, listContentSelector(1))
          textOnPageCheck(investmentBulletText, listContentSelector(2))
          textOnPageCheck(yourDividendsBulletText, listContentSelector(3))
          textOnPageCheck(AgentExpected.youDoNotNeedText, youDoNotNeedSelector)
          radioButtonCheck(yesNo(true), 1)
          radioButtonCheck(yesNo(false), 2)
          textOnPageCheck(whatAreInvestmentText, investmentTitleSelector())
          textOnPageCheck(investmentTrustText, investmentsContentSelector()(1))
          textOnPageCheck(unitTrustsText, investmentsContentSelector()(2))
          textOnPageCheck(openEndedText, investmentsContentSelector()(3))
          textOnPageCheck(whatAreEqualisationText, equalisationTitleSelector())
          textOnPageCheck(equalisationPaymentsText, equalisationContentSelector())

          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)

          welshToggleCheck(ENGLISH)
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
    }

    "in Welsh" should {

      import ExpectedResults.WelshLang._
      import ExpectedResults.WelshLang.AllExpected._

      "as an Individual" when {

        "render correct content when if first question in dividends journey has been answered in session" which {

          lazy val result: WSResponse = {
            authoriseIndividual()
            urlGet(receivedOtherDividendsUrl, welsh = true, headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(cyaModel)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(IndividualExpected.expectedTitle)
          h1Check(IndividualExpected.expectedH1 + " " + captionExpected)
          textOnPageCheck(youMustAlsoText, youMustAlsoSelector)
          textOnPageCheck(authorisedBulletText, listContentSelector(1))
          textOnPageCheck(investmentBulletText, listContentSelector(2))
          textOnPageCheck(yourDividendsBulletText, listContentSelector(3))
          textOnPageCheck(IndividualExpected.youDoNotNeedText, youDoNotNeedSelector)
          radioButtonCheck(yesNo(true), 1)
          radioButtonCheck(yesNo(false), 2)
          textOnPageCheck(whatAreInvestmentText, investmentTitleSelector())
          textOnPageCheck(investmentTrustText, investmentsContentSelector()(1))
          textOnPageCheck(unitTrustsText, investmentsContentSelector()(2))
          textOnPageCheck(openEndedText, investmentsContentSelector()(3))
          textOnPageCheck(whatAreEqualisationText, equalisationTitleSelector())
          textOnPageCheck(equalisationPaymentsText, equalisationContentSelector())

          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)

          welshToggleCheck(WELSH)
        }
      }

      "as an Agent" when {

        "render correct content when if first question in dividends journey has been answered in session" which {

          lazy val result: WSResponse = {
            authoriseAgent()
            urlGet(receivedOtherDividendsUrl, welsh = true, headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(cyaModel)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(AgentExpected.expectedTitle)
          h1Check(AgentExpected.expectedH1 + " " + captionExpected)
          textOnPageCheck(youMustAlsoText, youMustAlsoSelector)
          textOnPageCheck(authorisedBulletText, listContentSelector(1))
          textOnPageCheck(investmentBulletText, listContentSelector(2))
          textOnPageCheck(yourDividendsBulletText, listContentSelector(3))
          textOnPageCheck(AgentExpected.youDoNotNeedText, youDoNotNeedSelector)
          radioButtonCheck(yesNo(true), 1)
          radioButtonCheck(yesNo(false), 2)
          textOnPageCheck(whatAreInvestmentText, investmentTitleSelector())
          textOnPageCheck(investmentTrustText, investmentsContentSelector()(1))
          textOnPageCheck(unitTrustsText, investmentsContentSelector()(2))
          textOnPageCheck(openEndedText, investmentsContentSelector()(3))
          textOnPageCheck(whatAreEqualisationText, equalisationTitleSelector())
          textOnPageCheck(equalisationPaymentsText, equalisationContentSelector())

          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueLink, continueButtonFormSelector)

          welshToggleCheck(WELSH)
        }
      }
    }


  }

  ".submit" when {

    "as an individual" should {

      s"return a Redirect($SEE_OTHER) to other uk dividends amount page when form is valid an answer is yes" should {
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

      s"return a Redirect($SEE_OTHER) to dividend cya page when form is valid and answer is no" in {
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

      "when form is invalid - no radio button clicked. Show page with Error text" should {

        import Selectors._

        "in English" when {

          import ExpectedResults.EnglishLang._
          import ExpectedResults.EnglishLang.AllExpected._


          lazy val result: WSResponse = {
            authoriseIndividual()
            urlPost(receivedOtherDividendsUrl, postRequest = Map())
          }

          "has an BAD_REQUEST(400) status" in {
            result.status shouldBe BAD_REQUEST
          }


          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(IndividualExpected.expectedErrorTitle)
          h1Check(IndividualExpected.expectedH1 + " " + captionExpected)
          errorSummaryCheck(IndividualExpected.expectedErrorText, expectedErrorHref)
          textOnPageCheck(youMustAlsoText, youMustAlsoSelector)
          textOnPageCheck(authorisedBulletText, listContentSelector(1))
          textOnPageCheck(investmentBulletText, listContentSelector(2))
          textOnPageCheck(yourDividendsBulletText, listContentSelector(3))
          textOnPageCheck(IndividualExpected.youDoNotNeedText, youDoNotNeedSelector)
          radioButtonCheck(yesNo(true), 1)
          radioButtonCheck(yesNo(false), 2)
          textOnPageCheck(whatAreInvestmentText, investmentTitleSelector(2))
          textOnPageCheck(investmentTrustText, investmentsContentSelector(2)(1))
          textOnPageCheck(unitTrustsText, investmentsContentSelector(2)(2))
          textOnPageCheck(openEndedText, investmentsContentSelector(2)(3))
          textOnPageCheck(whatAreEqualisationText, equalisationTitleSelector(3))
          textOnPageCheck(equalisationPaymentsText, equalisationContentSelector(3))

          welshToggleCheck(ENGLISH)
        }

        "in Welsh" when {
          import ExpectedResults.WelshLang._
          import ExpectedResults.WelshLang.AllExpected._


          lazy val result: WSResponse = {
            authoriseIndividual()
            urlPost(receivedOtherDividendsUrl, welsh=true, postRequest = Map())
          }

          "has an BAD_REQUEST(400) status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(IndividualExpected.expectedErrorTitle)
          h1Check(IndividualExpected.expectedH1 + " " + captionExpected)
          errorSummaryCheck(IndividualExpected.expectedErrorText, expectedErrorHref)
          textOnPageCheck(youMustAlsoText, youMustAlsoSelector)
          textOnPageCheck(authorisedBulletText, listContentSelector(1))
          textOnPageCheck(investmentBulletText, listContentSelector(2))
          textOnPageCheck(yourDividendsBulletText, listContentSelector(3))
          textOnPageCheck(IndividualExpected.youDoNotNeedText, youDoNotNeedSelector)
          radioButtonCheck(yesNo(true), 1)
          radioButtonCheck(yesNo(false), 2)
          textOnPageCheck(whatAreInvestmentText, investmentTitleSelector(2))
          textOnPageCheck(investmentTrustText, investmentsContentSelector(2)(1))
          textOnPageCheck(unitTrustsText, investmentsContentSelector(2)(2))
          textOnPageCheck(openEndedText, investmentsContentSelector(2)(3))
          textOnPageCheck(whatAreEqualisationText, equalisationTitleSelector(3))
          textOnPageCheck(equalisationPaymentsText, equalisationContentSelector(3))

        welshToggleCheck(WELSH)
        }
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

    "as an agent" should {

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

        "when form is invalid - no radio button clicked. Show page with Error text" should {

          import Selectors._

          "in English" when {

            import ExpectedResults.EnglishLang._
            import ExpectedResults.EnglishLang.AllExpected._


            lazy val result: WSResponse = {
              lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
                SessionValues.CLIENT_MTDITID -> "1234567890",
                SessionValues.CLIENT_NINO -> "AA123456A",
              ))

              authoriseAgent()
              await(wsClient.url(receivedOtherDividendsUrl)
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
                .post(Map[String, String]()))
            }

            "has an BAD_REQUEST(400) status" in {
              result.status shouldBe BAD_REQUEST
            }


            implicit val document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(AgentExpected.expectedErrorTitle)
            welshToggleCheck("English")
            h1Check(AgentExpected.expectedH1 + " " + captionExpected)
            errorSummaryCheck(AgentExpected.expectedErrorText, expectedErrorHref)
            textOnPageCheck(youMustAlsoText, youMustAlsoSelector)
            textOnPageCheck(authorisedBulletText, listContentSelector(1))
            textOnPageCheck(investmentBulletText, listContentSelector(2))
            textOnPageCheck(yourDividendsBulletText, listContentSelector(3))
            textOnPageCheck(AgentExpected.youDoNotNeedText, youDoNotNeedSelector)
            radioButtonCheck(yesNo(true), 1)
            radioButtonCheck(yesNo(false), 2)
            textOnPageCheck(whatAreInvestmentText, investmentTitleSelector(2))
            textOnPageCheck(investmentTrustText, investmentsContentSelector(2)(1))
            textOnPageCheck(unitTrustsText, investmentsContentSelector(2)(2))
            textOnPageCheck(openEndedText, investmentsContentSelector(2)(3))
            textOnPageCheck(whatAreEqualisationText, equalisationTitleSelector(3))
            textOnPageCheck(equalisationPaymentsText, equalisationContentSelector(3))
          }

          "in Welsh" when {
            import ExpectedResults.WelshLang._
            import ExpectedResults.WelshLang.AllExpected._


            lazy val result: WSResponse = {
              lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
                SessionValues.CLIENT_MTDITID -> "1234567890",
                SessionValues.CLIENT_NINO -> "AA123456A",

              ))

              authoriseAgent()
              await(wsClient.url(receivedOtherDividendsUrl)
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", "Csrf-Token" -> "nocheck")
                .post(Map[String, String]()))
            }

            "has an BAD_REQUEST(400) status" in {
              result.status shouldBe BAD_REQUEST
            }


            implicit val document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(AgentExpected.expectedErrorTitle)
            h1Check(AgentExpected.expectedH1 + " " + captionExpected)
            errorSummaryCheck(AgentExpected.expectedErrorText, expectedErrorHref)
            textOnPageCheck(youMustAlsoText, youMustAlsoSelector)
            textOnPageCheck(authorisedBulletText, listContentSelector(1))
            textOnPageCheck(investmentBulletText, listContentSelector(2))
            textOnPageCheck(yourDividendsBulletText, listContentSelector(3))
            textOnPageCheck(AgentExpected.youDoNotNeedText, youDoNotNeedSelector)
            radioButtonCheck(yesNo(true), 1)
            radioButtonCheck(yesNo(false), 2)
            textOnPageCheck(whatAreInvestmentText, investmentTitleSelector(2))
            textOnPageCheck(investmentTrustText, investmentsContentSelector(2)(1))
            textOnPageCheck(unitTrustsText, investmentsContentSelector(2)(2))
            textOnPageCheck(openEndedText, investmentsContentSelector(2)(3))
            textOnPageCheck(whatAreEqualisationText, equalisationTitleSelector(3))
            textOnPageCheck(equalisationPaymentsText, equalisationContentSelector(3))

            welshToggleCheck(WELSH)

          }
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