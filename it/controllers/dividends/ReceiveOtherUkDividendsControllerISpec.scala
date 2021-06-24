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

import forms.YesNoForm
import models.dividends.{DividendsCheckYourAnswersModel, DividendsPriorSubmission}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import utils.{DividendsDatabaseHelper, IntegrationTest, ViewHelpers}

class ReceiveOtherUkDividendsControllerISpec extends IntegrationTest with ViewHelpers with DividendsDatabaseHelper {


  val taxYear: Int = 2022
  val amount: BigDecimal = 500
  val receivedOtherDividendsUrl = s"$appUrl/$taxYear/dividends/dividends-from-uk-trusts-or-open-ended-investment-companies"

  lazy val cyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(ukDividends = Some(true), ukDividendsAmount = Some(amount),
    otherUkDividends = Some(true), otherUkDividendsAmount = None)

  lazy val priorData: IncomeSourcesModel = IncomeSourcesModel(
    dividends = Some(DividendsPriorSubmission(
      ukDividends = None,
      otherUkDividends = Some(amount)
    ))
  )


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

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedErrorTitle: String
    val youDoNotNeedText: String
    val expectedErrorText: String
    val redirectTitle: String
    val redirectH1: String
  }

  trait CommonExpectedResults {
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


  object IndividualExpectedEnglish extends SpecificExpectedResults {
    val expectedH1 = "Did you get dividends from UK-based trusts or open-ended investment companies?"
    val expectedTitle = "Did you get dividends from UK-based trusts or open-ended investment companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val youDoNotNeedText = "You do not need to tell us about amounts shown as 'equalisation' on your dividend voucher."
    val expectedErrorText = "Select yes if you got dividends from UK-based trusts or open-ended investment companies"
    val redirectTitle = "Check your income from dividends"
    val redirectH1 = "Check your income from dividends Dividends for 6 April 2021 to 5 April 2022"
  }

  object AgentExpectedEnglish extends SpecificExpectedResults {
    val expectedH1 = "Did your client get dividends from UK-based trusts or open-ended investment companies?"
    val expectedTitle = "Did your client get dividends from UK-based trusts or open-ended investment companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val youDoNotNeedText = "You do not need to tell us about amounts shown as 'equalisation' on your client’s dividend voucher."
    val expectedErrorText = "Select yes if your client got dividends from UK-based trusts or open-ended investment companies"
    val redirectTitle = "Check your client’s income from dividends"
    val redirectH1 = "Check your client’s income from dividends Dividends for 6 April 2021 to 5 April 2022"
  }

  object AllExpectedEnglish extends CommonExpectedResults {
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

  object IndividualExpectedWelsh extends SpecificExpectedResults {
    val expectedH1 = "Did you get dividends from UK-based trusts or open-ended investment companies?"
    val expectedTitle = "Did you get dividends from UK-based trusts or open-ended investment companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val youDoNotNeedText = "You do not need to tell us about amounts shown as 'equalisation' on your dividend voucher."
    val expectedErrorText = "Select yes if you got dividends from UK-based trusts or open-ended investment companies"
    val redirectTitle = "Check your income from dividends"
    val redirectH1 = "Check your income from dividends Dividends for 6 April 2021 to 5 April 2022"
  }

  object AgentExpectedWelsh extends SpecificExpectedResults {
    val expectedH1 = "Did your client get dividends from UK-based trusts or open-ended investment companies?"
    val expectedTitle = "Did your client get dividends from UK-based trusts or open-ended investment companies?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val youDoNotNeedText = "You do not need to tell us about amounts shown as 'equalisation' on your client’s dividend voucher."
    val expectedErrorText = "Select yes if your client got dividends from UK-based trusts or open-ended investment companies"
    val redirectTitle = "Check your client’s income from dividends"
    val redirectH1 = "Check your client’s income from dividends Dividends for 6 April 2021 to 5 April 2022"
  }

  object AllExpectedWelsh extends CommonExpectedResults {
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
    Seq(UserScenario(isWelsh = false, isAgent = false,AllExpectedEnglish, Some(IndividualExpectedEnglish)),
      UserScenario(isWelsh = false, isAgent = true, AllExpectedEnglish, Some(AgentExpectedEnglish)),
      UserScenario(isWelsh = true, isAgent = false, AllExpectedWelsh, Some(IndividualExpectedWelsh)),
      UserScenario(isWelsh = true, isAgent = true, AllExpectedWelsh, Some(AgentExpectedWelsh)))

  ".show" when {
    userScenarios.foreach { us =>

      import Selectors._
      import us.specificExpectedResults._
      import us.commonExpectedResults._
      
      s"language is ${welshTest(us.isWelsh)} and request is from an ${agentTest(us.isAgent)}" should {

        "render correct content if first question in dividends journey has been answered in session" which {

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            dropDividendsDB()
            emptyUserDataStub()
            insertCyaData(Some(cyaModel))
            urlGet(receivedOtherDividendsUrl, us.isWelsh, headers = playSessionCookie(us.isAgent))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(get.expectedTitle)
          h1Check(get.expectedH1 + " " + captionExpected)
          textOnPageCheck(youMustAlsoText, youMustAlsoSelector)
          textOnPageCheck(authorisedBulletText, listContentSelector(1))
          textOnPageCheck(investmentBulletText, listContentSelector(2))
          textOnPageCheck(yourDividendsBulletText, listContentSelector(3))
          textOnPageCheck(get.youDoNotNeedText, youDoNotNeedSelector)
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

          welshToggleCheck(us.isWelsh)
        }
      }
    }
  }

  ".show" should {

    "redirects user to dividendsCya page when there is prior submission data but no cyaData in session" which {

      lazy val result: WSResponse = {
        authoriseIndividual()
        emptyUserDataStub()
        userDataStub(priorData, nino, taxYear)
        stubGet(s"/income-through-software/return/$taxYear/view", OK, "overview page content")
        urlGet(receivedOtherDividendsUrl, follow = false, headers = playSessionCookie())
      }

      s"has an $SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION) shouldBe
          Some("/income-through-software/return/personal-income/2022/dividends/check-income-from-dividends")
      }
    }

    "redirects user to overview page when there is no cyaData or prior data" which {

      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        stubGet(s"/income-through-software/return/$taxYear/view", SEE_OTHER, "overview page content")
        urlGet(receivedOtherDividendsUrl, follow = false, headers = playSessionCookie())
      }

      "has an OK(200) status" in {
        result.status shouldBe SEE_OTHER
      }
    }

    "returns an action when auth call fails" which {
      lazy val result: WSResponse = {
        authoriseIndividualUnauthorized()
        dropDividendsDB()
        emptyUserDataStub()
        urlGet(receivedOtherDividendsUrl, headers = playSessionCookie())
      }
      "has an UNAUTHORIZED(401) status" in {
        result.status shouldBe UNAUTHORIZED
      }
    }
  }

  ".submit" when {

    userScenarios.foreach { us =>

      import us.specificExpectedResults._
      import us.commonExpectedResults._

      s"language is ${welshTest(us.isWelsh)} and request is from an ${agentTest(us.isAgent)}" should {

        "when form is invalid - no radio button clicked. Show page with Error text" should {

          import Selectors._

          "in English" when {

            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(us.isAgent)
              urlPost(receivedOtherDividendsUrl, welsh=us.isWelsh, headers = playSessionCookie(us.isAgent), body = Map[String, String]())
            }

            "has an BAD_REQUEST(400) status" in {
              result.status shouldBe BAD_REQUEST
            }


            implicit val document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(get.expectedErrorTitle)
            h1Check(get.expectedH1 + " " + captionExpected)
            errorSummaryCheck(get.expectedErrorText, expectedErrorHref)
            textOnPageCheck(youMustAlsoText, youMustAlsoSelector)
            textOnPageCheck(authorisedBulletText, listContentSelector(1))
            textOnPageCheck(investmentBulletText, listContentSelector(2))
            textOnPageCheck(yourDividendsBulletText, listContentSelector(3))
            textOnPageCheck(get.youDoNotNeedText, youDoNotNeedSelector)
            radioButtonCheck(yesNo(true), 1)
            radioButtonCheck(yesNo(false), 2)
            textOnPageCheck(whatAreInvestmentText, investmentTitleSelector(2))
            textOnPageCheck(investmentTrustText, investmentsContentSelector(2)(1))
            textOnPageCheck(unitTrustsText, investmentsContentSelector(2)(2))
            textOnPageCheck(openEndedText, investmentsContentSelector(2)(3))
            textOnPageCheck(whatAreEqualisationText, equalisationTitleSelector(3))
            textOnPageCheck(equalisationPaymentsText, equalisationContentSelector(3))

            welshToggleCheck(us.isWelsh)
          }
        }
      }
    }
  }


  ".submit" should {

    "redirects User to overview page if no CYA data is in session" when {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        urlGet(receivedOtherDividendsUrl, follow = false, headers = playSessionCookie())
        urlPost(receivedOtherDividendsUrl, follow = false, headers = playSessionCookie(), body = Map(YesNoForm.yesNo -> YesNoForm.yes))
      }
      "has a SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
      }

      "have the correct redirect URL" in {
        result.headers(HeaderNames.LOCATION).head shouldBe "http://localhost:11111/income-through-software/return/2022/view"
      }
    }

    "return a Redirect to other uk dividends amount page when form is valid and answer is yes and cya model already has data. Update Mongo" should {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        insertCyaData(Some(DividendsCheckYourAnswersModel(
          ukDividends = Some(true), ukDividendsAmount = Some(amount)
        )))
        urlPost(receivedOtherDividendsUrl, follow = false, headers = playSessionCookie(), body = Map(YesNoForm.yesNo -> YesNoForm.yes)
        )
      }

      s"return a Redirect to the other uk dividend amount page" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION) shouldBe Some(routes.OtherUkDividendsAmountController.show(taxYear).url)
      }
    }

    "return a Redirect to dividend cya page when form is valid and answer is no. update Mongo" should {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        insertCyaData(Some(DividendsCheckYourAnswersModel(
          ukDividends = Some(true), ukDividendsAmount = Some(amount)
        )))
        urlPost(receivedOtherDividendsUrl, follow = false, headers = playSessionCookie(), body = Map(YesNoForm.yesNo -> YesNoForm.no))
      }

      s"return a Redirect($SEE_OTHER) to the other uk dividend amount page" in {
        result.status shouldBe SEE_OTHER
        result.header(HeaderNames.LOCATION) shouldBe Some(routes.DividendsCYAController.show(taxYear).url)
      }
    }

    "returns an action when auth call fails" which {
      lazy val result: WSResponse = {
        authoriseIndividualUnauthorized()
        dropDividendsDB()
        emptyUserDataStub()
        urlPost(receivedOtherDividendsUrl, headers = playSessionCookie(), body = Map(YesNoForm.yesNo -> YesNoForm.no))
      }
      "has an UNAUTHORIZED(401) status" in {
        result.status shouldBe UNAUTHORIZED
      }
    }
  }
}
