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

import models.dividends.{DividendsCheckYourAnswersModel, DividendsPriorSubmission}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import utils.{DividendsDatabaseHelper, IntegrationTest, ViewHelpers}


class DividendsCYAControllerISpec extends IntegrationTest with ViewHelpers with DividendsDatabaseHelper {

  val taxYear = 2022
  val ukDividends: BigDecimal = 10
  val otherDividends: BigDecimal = 10.50
  val dividendsCheckYourAnswersUrl = s"$appUrl/$taxYear/dividends/check-income-from-dividends"

  val changeUkDividendsHref = "/income-through-software/return/personal-income/2022/dividends/dividends-from-uk-companies"
  val changeUkDividendsAmountHref = "/income-through-software/return/personal-income/2022/dividends/how-much-dividends-from-uk-companies"
  val changeOtherDividendsHref = "/income-through-software/return/personal-income/2022/dividends/dividends-from-uk-trusts-or-open-ended-investment-companies"
  val changeOtherDividendsAmountHref: String = "/income-through-software/return/personal-income/2022/dividends" +
    "/how-much-dividends-from-uk-trusts-and-open-ended-investment-companies"


  lazy val dividendsCyaModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(
    Some(true), Some(ukDividends),
    Some(true), Some(otherDividends)
  )
  lazy val dividendsNoModel: DividendsCheckYourAnswersModel = DividendsCheckYourAnswersModel(Some(false), None, Some(false))

  lazy val priorData: IncomeSourcesModel = IncomeSourcesModel(
    dividends = Some(DividendsPriorSubmission(
      Some(ukDividends),
      Some(otherDividends)
    ))
  )


  object Selectors {
    def cyaTitle(i: Int): String = s"#main-content > div > div > dl > div:nth-child($i) > dt"

    def cyaValue(i: Int): String = s"#main-content > div > div > dl > div:nth-child($i) > dd.govuk-summary-list__value"

    def cyaChangeLink(i: Int): String = s"#main-content > div > div > dl > div:nth-child($i) > dd.govuk-summary-list__actions > a"

    val captionSelector = "#main-content > div > div > h1 > span"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
  }

  trait SpecificExpectedResults {
    val titleExpected: String
    val h1Expected: String
    val expectedErrorTitle: String

    val changeUkDividendsHiddenText: String
    val changeUkDividendsAmountHiddenText: String
    val changeOtherDividendsHiddenText: String
    val changeOtherDividendsAmountHiddenText: String
  }

  trait CommonExpectedResults {
    val captionExpected: String
    val yesNoExpectedAnswer: Boolean => String
    val ukDividendsAmount: String
    val otherDividendsAmount: String
    val continueButtonText: String
    val continueButtonLink: String
    val changeLinkExpected: String
    val UkDividendsText: String
    val ukDividendsAmountText: String
    val otherDividendsText: String
    val otherDividendsAmountText: String
  }


  object IndividualExpectedEnglish extends SpecificExpectedResults {
    val titleExpected = "Check your income from dividends"
    val h1Expected = "Check your income from dividends"
    val expectedErrorTitle = s"Error: $titleExpected"

    val changeUkDividendsHiddenText = "Change if you got dividends from UK-based companies"
    val changeUkDividendsAmountHiddenText = "Change how much you got from UK-based companies"
    val changeOtherDividendsHiddenText = "Change if you got dividends from trusts or open-ended investment companies based in the UK"
    val changeOtherDividendsAmountHiddenText = "Change how much you got in dividends from trusts or open-ended investment companies based in the UK"
  }

  object AgentExpectedEnglish extends SpecificExpectedResults {
    val titleExpected = "Check your client’s income from dividends"
    val h1Expected = "Check your client’s income from dividends"
    val expectedErrorTitle = s"Error: $titleExpected"

    val changeUkDividendsHiddenText = "Change if your client got dividends from UK-based companies"
    val changeUkDividendsAmountHiddenText = "Change how much your client got from UK-based companies"
    val changeOtherDividendsHiddenText = "Change if your client got dividends from trusts or open-ended investment companies based in the UK"
    val changeOtherDividendsAmountHiddenText = "Change how much your client got in dividends from trusts or open-ended investment companies based in the UK"
  }

  object AllExpectedEnglish extends CommonExpectedResults {
    val captionExpected = s"Dividends for 6 April ${taxYear - 1} to 5 April $taxYear"
    val yesNoExpectedAnswer: Boolean => String = isYes => if (isYes) "Yes" else "No"
    val ukDividendsAmount = "£10"
    val otherDividendsAmount = "£10.50"
    val continueButtonText = "Save and continue"
    val continueButtonLink = "/income-through-software/return/personal-income/2022/dividends/check-income-from-dividends"
    val changeLinkExpected = "Change"
    val UkDividendsText = "Dividends from UK-based companies"
    val ukDividendsAmountText = "Value of dividends from UK-based companies"
    val otherDividendsText = "Dividends from UK-based unit trusts or open-ended investment companies"
    val otherDividendsAmountText = "Value of dividends from UK-based unit trusts or open-ended investment companies"
  }

  object IndividualExpectedWelsh extends SpecificExpectedResults {
    val titleExpected = "Gwiriwch eich incwm o ddifidendau"
    val h1Expected = "Gwiriwch eich incwm o ddifidendau"
    val expectedErrorTitle = s"Error: $titleExpected"

    val changeUkDividendsHiddenText = "Newidiwch os cawsoch ddifidendau gan gwmnïau yn y DU"
    val changeUkDividendsAmountHiddenText = "Newidiwch faint a gawsoch gan gwmnïau yn y DU"
    val changeOtherDividendsHiddenText = "Newidiwch os cawsoch ddifidendau gan ymddiriedolaethau neu gwmnïau buddsoddi penagored yn y DU"
    val changeOtherDividendsAmountHiddenText = "Newidiwch faint a gawsoch mewn difidendau gan ymddiriedolaethau neu gwmnïau buddsoddi penagored yn y DU"
  }

  object AgentExpectedWelsh extends SpecificExpectedResults {
    val titleExpected = "Gwiriwch incwm eich cleient o ddifidendau"
    val h1Expected = "Gwiriwch incwm eich cleient o ddifidendau"
    val expectedErrorTitle = s"Error: $titleExpected"

    val changeUkDividendsHiddenText = "Newidiwch os cafodd eich cleient ddifidendau gan gwmnïau yn y DU"
    val changeUkDividendsAmountHiddenText = "Newidiwch faint gafodd eich cleient gan gwmnïau yn y DU"
    val changeOtherDividendsHiddenText = "Newidiwch os cafodd eich cleient ddifidendau gan ymddiriedolaethau neu gwmnïau buddsoddi penagored yn y DU"
    val changeOtherDividendsAmountHiddenText = "Newidiwch faint a gafodd eich cleient mewn difidendau gan ymddiriedolaethau neu gwmnïau buddsoddi penagored yn y DU"
  }

  object AllExpectedWelsh extends CommonExpectedResults {
    val captionExpected = s"Difidendau ar gyfer 6 Ebrill ${taxYear - 1} i 5 Ebrill $taxYear"
    val yesNoExpectedAnswer: Boolean => String = isYes => if (isYes) "Iawn" else "Na"
    val ukDividendsAmount = "£10"
    val otherDividendsAmount = "£10.50"
    val continueButtonText = "Cadw ac yn eich blaen"
    val continueButtonLink = "/income-through-software/return/personal-income/2022/dividends/check-income-from-dividends"
    val changeLinkExpected = "Newid"
    val UkDividendsText = "Difidendau o gwmnïau yn y DU"
    val ukDividendsAmountText = "Swm difidendau o gwmnïau yn y DU"
    val otherDividendsText = "Difidendau gan ymddiriedolaethau unedol yn y DU neu gwmnïau buddsoddi penagored"
    val otherDividendsAmountText = "Swm difidendau gan ymddiriedolaethau unedol yn y DU neu gwmnïau buddsoddi penagored"
  }


  val userScenarios =
    Seq(UserScenario(isWelsh = false, isAgent = false, AllExpectedEnglish, Some(IndividualExpectedEnglish)),
      UserScenario(isWelsh = false, isAgent = true,AllExpectedEnglish, Some(AgentExpectedEnglish)),
      UserScenario(isWelsh = true, isAgent = false, AllExpectedWelsh, Some(IndividualExpectedWelsh)),
      UserScenario(isWelsh = true, isAgent = true,  AllExpectedWelsh, Some(AgentExpectedWelsh)))

  ".show" when {


    userScenarios.foreach { us =>

      import Selectors._
      import us.commonExpectedResults._
      import us.specificExpectedResults._

      s"language is ${welshTest(us.isWelsh)} and request is from an ${agentTest(us.isAgent)}" should {


        " renders CYA page with correct content when there is data in session" which {

          lazy val result = {
            authoriseAgentOrIndividual(us.isAgent)
            dropDividendsDB()
            emptyUserDataStub()
            insertCyaData(Some(dividendsCyaModel))
            urlGet(dividendsCheckYourAnswersUrl, us.isWelsh, false, headers =
              playSessionCookie(us.isAgent))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)


          titleCheck(get.titleExpected, us.isWelsh)
          h1Check(get.h1Expected + " " + captionExpected)
          textOnPageCheck(captionExpected, Selectors.captionSelector)
          "has an area for section 1" which {
            textOnPageCheck(UkDividendsText, Selectors.cyaTitle(1))
            textOnPageCheck(yesNoExpectedAnswer(true), Selectors.cyaValue(1))
            linkCheck(s"${changeLinkExpected} ${get.changeUkDividendsHiddenText}"
              , cyaChangeLink(1), changeUkDividendsHref)
          }
          "has an area for section 2" which {
            textOnPageCheck(ukDividendsAmountText, cyaTitle(2))
            textOnPageCheck(ukDividendsAmount, cyaValue(2))
            linkCheck(s"${changeLinkExpected} ${get.changeUkDividendsAmountHiddenText}",
              cyaChangeLink(2), changeUkDividendsAmountHref)
          }
          "has an area for section 3" which {
            textOnPageCheck(otherDividendsText, cyaTitle(3))
            textOnPageCheck(yesNoExpectedAnswer(true), cyaValue(3))
            linkCheck(s"${changeLinkExpected} ${get.changeOtherDividendsHiddenText}",
              cyaChangeLink(3), changeOtherDividendsHref)
          }
          //noinspection ScalaStyle
          "has an area for section 4" which {
            textOnPageCheck(otherDividendsAmountText, cyaTitle(4))
            textOnPageCheck(otherDividendsAmount, cyaValue(4))
            linkCheck(s"${changeLinkExpected} ${get.changeOtherDividendsAmountHiddenText}",
              cyaChangeLink(4), changeOtherDividendsAmountHref)
          }

          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)

          welshToggleCheck(us.isWelsh)

        }


        "renders CYA page without yesNo Content when there is a prior submission" which {

          lazy val result = {
            authoriseAgentOrIndividual(us.isAgent)
            dropDividendsDB()
            emptyUserDataStub()
            userDataStub(priorData, nino, taxYear)
            urlGet(dividendsCheckYourAnswersUrl, us.isWelsh, headers =
              playSessionCookie(us.isAgent))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(get.titleExpected, us.isWelsh)
          h1Check(get.h1Expected + " " + captionExpected)
          textOnPageCheck(captionExpected, Selectors.captionSelector)
          "has an area for section 1" which {
            textOnPageCheck(ukDividendsAmountText, cyaTitle(1))
            textOnPageCheck(ukDividendsAmount, cyaValue(1))
            linkCheck(s"${changeLinkExpected} ${get.changeUkDividendsAmountHiddenText}",
              cyaChangeLink(1), changeUkDividendsAmountHref)
          }
          "has an area for section 2" which {
            textOnPageCheck(otherDividendsAmountText, cyaTitle(2))
            textOnPageCheck(otherDividendsAmount, cyaValue(2))
            linkCheck(s"${changeLinkExpected} ${get.changeOtherDividendsAmountHiddenText}",
              cyaChangeLink(2), changeOtherDividendsAmountHref)
          }

          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)

          welshToggleCheck(us.isWelsh)
        }

        "renders CYA page without amount when cyaModels boolean answers are false" which {

          lazy val result = {
            authoriseAgentOrIndividual(us.isAgent)
            dropDividendsDB()
            emptyUserDataStub()
            insertCyaData(Some(dividendsNoModel))
            urlGet(dividendsCheckYourAnswersUrl, us.isWelsh, headers =
              playSessionCookie(us.isAgent))
          }


          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(get.titleExpected, us.isWelsh)
          h1Check(get.h1Expected + " " + captionExpected)
          textOnPageCheck(captionExpected, Selectors.captionSelector)
          "has an area for section 1" which {
            textOnPageCheck(UkDividendsText, cyaTitle(1))
            textOnPageCheck(yesNoExpectedAnswer(false), cyaValue(1))
            linkCheck(s"${changeLinkExpected} ${get.changeUkDividendsHiddenText}",
              cyaChangeLink(1), changeUkDividendsHref)
          }
          "has an area for section 2" which {
            textOnPageCheck(otherDividendsText, cyaTitle(2))
            textOnPageCheck(yesNoExpectedAnswer(false), cyaValue(2))
            linkCheck(s"${changeLinkExpected} ${get.changeOtherDividendsHiddenText}",
              cyaChangeLink(2), changeOtherDividendsHref)
          }

          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)

          welshToggleCheck(us.isWelsh)
        }

        "renders CYA with new amounts if they have been updated in session compared to prior submission" which {

          val ukDividends1 = 100
          val otherDividends1 = 200

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            dropDividendsDB()
            emptyUserDataStub()
            userDataStub(priorData, nino, taxYear)
            insertCyaData(Some(DividendsCheckYourAnswersModel(
              Some(true), Some(ukDividends1),
              Some(true), Some(otherDividends1)
            )))
            urlGet(dividendsCheckYourAnswersUrl, us.isWelsh, headers =
              playSessionCookie(us.isAgent))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(get.titleExpected, us.isWelsh)
          h1Check(get.h1Expected + " " + captionExpected)
          textOnPageCheck(captionExpected, Selectors.captionSelector)
          "has an area for section 1" which {
            textOnPageCheck(ukDividendsAmountText, cyaTitle(1))
            textOnPageCheck("£100", cyaValue(1))
            linkCheck(s"${changeLinkExpected} ${get.changeUkDividendsAmountHiddenText}",
              cyaChangeLink(1), changeUkDividendsAmountHref)
          }
          "has an area for section 2" which {
            textOnPageCheck(otherDividendsAmountText, cyaTitle(2))
            textOnPageCheck("£200", cyaValue(2))
            linkCheck(s"${changeLinkExpected} ${get.changeOtherDividendsAmountHiddenText}",
              cyaChangeLink(2), changeOtherDividendsAmountHref)
          }

          buttonCheck(continueButtonText, continueButtonSelector)
          formPostLinkCheck(continueButtonLink, continueButtonFormSelector)

          welshToggleCheck(us.isWelsh)
        }
      }
    }
  }

  ".show" should {

    "redirect to the overview page" when {
      "there is no session data" in {

        val result: WSResponse = {
          authoriseIndividual()
          dropDividendsDB()
          emptyUserDataStub()
          stubGet("/income-through-software/return/2022/view", SEE_OTHER, "overview")
          urlGet(dividendsCheckYourAnswersUrl, follow = false, headers = playSessionCookie())
        }

        result.status shouldBe SEE_OTHER
      }

    }

    "redirect the user to the most relevant page in the user journey if CYA is part completed" should {

      "Uk dividends yesNo question has been answered" when {

        "redirect to How much Uk dividends page if the answer is Yes" which {

          lazy val result: WSResponse = {
            authoriseIndividual()
            dropDividendsDB()
            emptyUserDataStub()
            insertCyaData(Some(DividendsCheckYourAnswersModel(
              Some(true)
            )))

            urlGet(dividendsCheckYourAnswersUrl, follow = false, headers = playSessionCookie())
          }

          s"has a status of 303" in {
            result.status shouldBe SEE_OTHER
          }

          "has the correct title" in {
            result.headers("Location").head shouldBe "/income-through-software/return/personal-income/2022/dividends/how-much-dividends-from-uk-companies"
          }
        }

        "redirect the user to Did you receive other dividends page if the the answer is No" which {

          lazy val result: WSResponse = {
            authoriseIndividual()
            dropDividendsDB()
            emptyUserDataStub()
            insertCyaData(Some(DividendsCheckYourAnswersModel(
              Some(false)
            )))
            urlGet(dividendsCheckYourAnswersUrl, follow = false, headers = playSessionCookie())
          }

          s"has a status of 303" in {
            result.status shouldBe SEE_OTHER
          }

          "has the correct title" in {
            result.headers("Location").head shouldBe
              "/income-through-software/return/personal-income/2022/dividends/dividends-from-uk-trusts-or-open-ended-investment-companies"
          }
        }
      }

      "redirect the user to Did you receive other dividends page if Uk dividends amount has cya data" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          dropDividendsDB()
          emptyUserDataStub()
          insertCyaData(Some(DividendsCheckYourAnswersModel(
            Some(true), Some(1000.43)
          )))
          urlGet(dividendsCheckYourAnswersUrl, follow = false, headers = playSessionCookie())
        }

        s"has a status of 303" in {
          result.status shouldBe SEE_OTHER
        }

        "has the correct title" in {
          result.headers("Location").head shouldBe
            "/income-through-software/return/personal-income/2022/dividends/dividends-from-uk-trusts-or-open-ended-investment-companies"
        }
      }

      "redirect to How much did you receive in other dividends if Did you receive other dividends has been answered yes" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          dropDividendsDB()
          emptyUserDataStub()
          insertCyaData(Some(DividendsCheckYourAnswersModel(
            Some(true), Some(1000.43), Some(true)
          )))

          urlGet(dividendsCheckYourAnswersUrl, follow = false, headers = playSessionCookie())
        }

        s"has a status of 303" in {
          result.status shouldBe SEE_OTHER
        }

        "has the correct title" in {
          result.headers("Location").head shouldBe
            "/income-through-software/return/personal-income/2022/dividends/how-much-dividends-from-uk-trusts-and-open-ended-investment-companies"
        }
      }
    }
    "the authorization fails" which {
      lazy val result = {
        authoriseAgentUnauthorized()
        stubGet(s"/income-through-software/return/$taxYear/view", OK, "<title>Overview Page</title>")
        urlGet(dividendsCheckYourAnswersUrl)
      }

      s"has an Unauthorised($UNAUTHORIZED) status" in {
        result.status shouldBe UNAUTHORIZED
      }
    }
  }

  ".submit" should {

    "redirect to the overview page when there is valid session data" when {

      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()
        insertCyaData(
          Some(DividendsCheckYourAnswersModel(
            Some(true), Some(1000.43), Some(true), Some(9983.21)
          )))
        stubPut("/income-tax-dividends/income-tax/nino/AA123456A/sources\\?taxYear=2022", NO_CONTENT, "")
        urlPost(dividendsCheckYourAnswersUrl, follow = false, headers = playSessionCookie(), body = "")
      }
      s"has a status of 303" in {
        result.status shouldBe SEE_OTHER
      }

      "has the correct title" in {
        result.headers("Location").head shouldBe
          "http://localhost:11111/income-through-software/return/2022/view"
      }
    }

    "redirect to the 500 unauthorised error template page when there is a problem posting data" should {

      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()

        insertCyaData(
          Some(DividendsCheckYourAnswersModel(
            Some(true), Some(1000.43), Some(true), Some(9983.21)
          )))
        stubPut("/income-tax-dividends/income-tax/nino/AA123456A/sources\\?taxYear=2022", INTERNAL_SERVER_ERROR, "")
        urlPost(dividendsCheckYourAnswersUrl, follow = false, headers = playSessionCookie(), body = "")
      }

      "has a status of 500" in {
        result.status shouldBe INTERNAL_SERVER_ERROR
      }
    }

    "redirect to the 503 service unavailable page when the service is unavailable" which {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropDividendsDB()
        emptyUserDataStub()

        insertCyaData(
          Some(DividendsCheckYourAnswersModel(
            Some(true), Some(1000.43), Some(true), Some(9983.21)
          )))
        stubPut("/income-tax-dividends/income-tax/nino/AA123456A/sources\\?taxYear=2022", SERVICE_UNAVAILABLE, "")
        urlPost(dividendsCheckYourAnswersUrl, follow = false, headers = playSessionCookie(), body = "")
      }

      "has a status of 503" in {
        result.status shouldBe SERVICE_UNAVAILABLE
      }
    }
  }
}
