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
import models.dividends.{DividendsCheckYourAnswersModel, DividendsPriorSubmission}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, ViewHelpers}

class ReceiveOtherUkDividendsControllerISpec extends IntegrationTest with ViewHelpers {


  val taxYear: Int = 2022
  val amount: BigDecimal = 500
  val receivedOtherDividendsUrl = s"$startUrl/$taxYear/dividends/dividends-from-uk-trusts-or-open-ended-investment-companies"

  val cyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount),
    otherUkDividends = Some(true), otherUkDividendsAmount = None)


  object Selectors {
    val youMustAlsoSelector = "#main-content > div > div > form > div > fieldset > legend > div:nth-child(2) > p"

    def listContentSelector(i: Int): String = s"#main-content > div > div > form > div > fieldset > legend > ul > li:nth-child($i)"

    val youDoNotNeedSelector = "#main-content > div > div > form > div > fieldset > legend > div:nth-child(4) > p"

    def investmentTitleSelector(i: Int = 3): String = s"#main-content > div > div > form > details:nth-child($i) > summary > span"

    def investmentsContentSelector(i: Int = 3)(j: Int): String = s"#main-content > div > div > form > details:nth-child($i) > div > p:nth-child($j)"
    //noinspection ScalaStyle
    def equalisationTitleSelector(i: Int = 4): String = s"#main-content > div > div > form > details:nth-child($i) > summary > span"
    //noinspection ScalaStyle
    def equalisationContentSelector(i: Int = 4): String = s"#main-content > div > div > form > details:nth-child($i) > div > p"

    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val expectedErrorHref = "#value"

  }

  trait ExpectedResultsUserType {
    val expectedH1: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val youDoNotNeedText: String
    val expectedErrorText: String
    val redirectTitle:String
    val redirectH1:String
  }

  trait ExpectedResultsAllUsers {
    val captionExpected: String
    val yesNo: Boolean => String
    val continueButtonText: String
    val continueLink: String
    val youMustAlsoText: String
    val authorisedBulletText: String
    val investmentBulletText: String
    val yourDividendsBulletText: String
    val whatAreInvestmentText: String
    val investmentTrustText: String
    val unitTrustsText: String
    val openEndedText: String
    val whatAreEqualisationText: String
    val equalisationPaymentsText: String
  }


  object IndividualExpectedEnglish extends ExpectedResultsUserType {
    val expectedH1 = "Did you get dividends from UK-based trusts or open-ended investment companies?"
    val expectedTitle = "Did you get dividends from UK-based trusts or open-ended investment companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val youDoNotNeedText = "You do not need to tell us about amounts shown as 'equalisation' on your dividend voucher."
    val expectedErrorText = "Select yes if you got dividends from UK-based trusts or open-ended investment companies"
    val redirectTitle = "Check your income from dividends"
    val redirectH1 = "Check your income from dividends Dividends for 6 April 2021 to 5 April 2022"
  }

  object AgentExpectedEnglish extends ExpectedResultsUserType {
    val expectedH1 = "Did your client get dividends from UK-based trusts or open-ended investment companies?"
    val expectedTitle = "Did your client get dividends from UK-based trusts or open-ended investment companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val youDoNotNeedText = "You do not need to tell us about amounts shown as 'equalisation' on your client’s dividend voucher."
    val expectedErrorText = "Select yes if your client got dividends from UK-based trusts or open-ended investment companies"
    val redirectTitle = "Check your client’s income from dividends"
    val redirectH1 = "Check your client’s income from dividends Dividends for 6 April 2021 to 5 April 2022"
  }

  object AllExpectedEnglish extends ExpectedResultsAllUsers {
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

  object IndividualExpectedWelsh extends ExpectedResultsUserType {
    val expectedH1 = "Did you get dividends from UK-based trusts or open-ended investment companies?"
    val expectedTitle = "Did you get dividends from UK-based trusts or open-ended investment companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val youDoNotNeedText = "You do not need to tell us about amounts shown as 'equalisation' on your dividend voucher."
    val expectedErrorText = "Select yes if you got dividends from UK-based trusts or open-ended investment companies"
    val redirectTitle = "Check your income from dividends"
    val redirectH1 = "Check your income from dividends Dividends for 6 April 2021 to 5 April 2022"
  }

  object AgentExpectedWelsh extends ExpectedResultsUserType {
    val expectedH1 = "Did your client get dividends from UK-based trusts or open-ended investment companies?"
    val expectedTitle = "Did your client get dividends from UK-based trusts or open-ended investment companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val youDoNotNeedText = "You do not need to tell us about amounts shown as 'equalisation' on your client’s dividend voucher."
    val expectedErrorText = "Select yes if your client got dividends from UK-based trusts or open-ended investment companies"
    val redirectTitle = "Check your client’s income from dividends"
    val redirectH1 = "Check your client’s income from dividends Dividends for 6 April 2021 to 5 April 2022"
  }

  object AllExpectedWelsh extends ExpectedResultsAllUsers {
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

  val userScenarios =
    Seq(UserScenario(isWelsh = false, isAgent = false, Some(IndividualExpectedEnglish), AllExpectedEnglish),
      UserScenario(isWelsh = false, isAgent = true, Some(AgentExpectedEnglish), AllExpectedEnglish),
      UserScenario(isWelsh = true, isAgent = false, Some(IndividualExpectedWelsh), AllExpectedWelsh),
      UserScenario(isWelsh = true, isAgent = true, Some(AgentExpectedWelsh), AllExpectedWelsh))

  ".show" when {
    import Selectors._

    userScenarios.foreach { us =>
      s"language is ${printLang(us.isWelsh)} and request is from an ${printAgent(us.isAgent)}" should {

        "render correct content if first question in dividends journey has been answered in session" which {

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            urlGet(receivedOtherDividendsUrl, us.isWelsh, headers = playSessionCookies(taxYear, SessionValues.DIVIDENDS_CYA, Json.toJson(cyaModel)))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(us.expectedResultsUserType.get.expectedTitle)
          h1Check(us.expectedResultsUserType.get.expectedH1 + " " + us.expectedResultsAllUsers.captionExpected)
          textOnPageCheck(us.expectedResultsAllUsers.youMustAlsoText, youMustAlsoSelector)
          textOnPageCheck(us.expectedResultsAllUsers.authorisedBulletText, listContentSelector(1))
          textOnPageCheck(us.expectedResultsAllUsers.investmentBulletText, listContentSelector(2))
          textOnPageCheck(us.expectedResultsAllUsers.yourDividendsBulletText, listContentSelector(3))
          textOnPageCheck(us.expectedResultsUserType.get.youDoNotNeedText, youDoNotNeedSelector)
          radioButtonCheck(us.expectedResultsAllUsers.yesNo(true), 1)
          radioButtonCheck(us.expectedResultsAllUsers.yesNo(false), 2)
          textOnPageCheck(us.expectedResultsAllUsers.whatAreInvestmentText, investmentTitleSelector())
          textOnPageCheck(us.expectedResultsAllUsers.investmentTrustText, investmentsContentSelector()(1))
          textOnPageCheck(us.expectedResultsAllUsers.unitTrustsText, investmentsContentSelector()(2))
          textOnPageCheck(us.expectedResultsAllUsers.openEndedText, investmentsContentSelector()(3))
          textOnPageCheck(us.expectedResultsAllUsers.whatAreEqualisationText, equalisationTitleSelector())
          textOnPageCheck(us.expectedResultsAllUsers.equalisationPaymentsText, equalisationContentSelector())

          buttonCheck(us.expectedResultsAllUsers.continueButtonText, continueButtonSelector)
          formPostLinkCheck(us.expectedResultsAllUsers.continueLink, continueButtonFormSelector)

          welshToggleCheck(us.isWelsh)
        }

        "redirects user dividendCya page when there is prior submission data but no cyaData in session" which {

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            stubGet(s"/income-through-software/return/$taxYear/view", OK, "overview page content")
            urlGet(receivedOtherDividendsUrl, us.isWelsh, headers = playSessionCookies(
              taxYear, SessionValues.DIVIDENDS_PRIOR_SUB, Json.toJson(DividendsPriorSubmission(ukDividends = None, Some(amount)))))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(us.expectedResultsUserType.get.redirectTitle)
          h1Check(us.expectedResultsUserType.get.redirectH1)
        }

        "redirects user to overview page when there is no cyaData or prior data" which {

          lazy val result: WSResponse = {
            authoriseIndividual()
            stubGet(s"/income-through-software/return/$taxYear/view", OK, "overview page content")
            urlGet(receivedOtherDividendsUrl, us.isWelsh, headers = playSessionCookies(taxYear))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
            result.body shouldBe "overview page content"
          }
        }

        "returns an action when auth call fails" which {
          lazy val result: WSResponse = {
            if (us.isAgent) {
              authoriseAgentUnauthorized()
            }
            else {
              authoriseIndividualUnauthorized()
            }
            urlGet(receivedOtherDividendsUrl, us.isWelsh, headers = playSessionCookies(taxYear))
          }
          "has an UNAUTHORIZED(401) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }

      }

    }
  }

  ".submit" when {

    userScenarios.foreach { us =>
      s"language is ${printLang(us.isWelsh)} and request is from an ${printAgent(us.isAgent)}" should {

        "when form is invalid - no radio button clicked. Show page with Error text" should {

          import Selectors._

          "in English" when {

            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(us.isAgent)
              urlPost(receivedOtherDividendsUrl, us.isWelsh, headers = playSessionCookies(taxYear), postRequest = Map[String, String]())
            }

            "has an BAD_REQUEST(400) status" in {
              result.status shouldBe BAD_REQUEST
            }


            implicit val document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(us.expectedResultsUserType.get.expectedErrorTitle)
            h1Check(us.expectedResultsUserType.get.expectedH1 + " " + us.expectedResultsAllUsers.captionExpected)
            errorSummaryCheck(us.expectedResultsUserType.get.expectedErrorText, expectedErrorHref)
            textOnPageCheck(us.expectedResultsAllUsers.youMustAlsoText, youMustAlsoSelector)
            textOnPageCheck(us.expectedResultsAllUsers.authorisedBulletText, listContentSelector(1))
            textOnPageCheck(us.expectedResultsAllUsers.investmentBulletText, listContentSelector(2))
            textOnPageCheck(us.expectedResultsAllUsers.yourDividendsBulletText, listContentSelector(3))
            textOnPageCheck(us.expectedResultsUserType.get.youDoNotNeedText, youDoNotNeedSelector)
            radioButtonCheck(us.expectedResultsAllUsers.yesNo(true), 1)
            radioButtonCheck(us.expectedResultsAllUsers.yesNo(false), 2)
            textOnPageCheck(us.expectedResultsAllUsers.whatAreInvestmentText, investmentTitleSelector(2))
            textOnPageCheck(us.expectedResultsAllUsers.investmentTrustText, investmentsContentSelector(2)(1))
            textOnPageCheck(us.expectedResultsAllUsers.unitTrustsText, investmentsContentSelector(2)(2))
            textOnPageCheck(us.expectedResultsAllUsers.openEndedText, investmentsContentSelector(2)(3))
            textOnPageCheck(us.expectedResultsAllUsers.whatAreEqualisationText, equalisationTitleSelector(3))
            textOnPageCheck(us.expectedResultsAllUsers.equalisationPaymentsText, equalisationContentSelector(3))

            welshToggleCheck(us.isWelsh)
          }
        }
      }
    }
  }

    ".submit" should {
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
      }}
}