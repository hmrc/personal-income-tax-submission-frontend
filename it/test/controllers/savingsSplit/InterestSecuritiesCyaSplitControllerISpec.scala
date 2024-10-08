/*
 * Copyright 2024 HM Revenue & Customs
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

package controllers.savingsSplit

import models.priorDataModels.IncomeSourcesModel
import models.savings.{SavingsIncomeCYAModel, SavingsIncomeDataModel, SecuritiesModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper
import play.api.http.HeaderNames
import play.api.http.Status.{NO_CONTENT, OK, SEE_OTHER, UNAUTHORIZED}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import test.utils.{IntegrationTest, SavingsDatabaseHelper, ViewHelpers}

class InterestSecuritiesCyaSplitControllerISpec extends IntegrationTest with SavingsDatabaseHelper with ViewHelpers {

  val relativeUrl: String =  controllers.savingsBase.routes.InterestSecuritiesCyaBaseController.show(taxYear).url

  val monetaryValue: BigDecimal = 100.00

  object Selectors {
    val titleSelector = "title"
    val h1Selector = "h1"
    val captionSelector = ".govuk-caption-l"
    val submitButton = ".govuk-button"
    val submitButtonForm = "#main-content > div > div > form"

    val questionChangeLinkSelector: Int => String = questionNumber => s"#main-content > div > div > dl > div:nth-child($questionNumber) " +
      s"> dd.govuk-summary-list__actions > a"
    val questionTextSelector: Int => String = question => s"#main-content > div > div > dl > div:nth-child($question) > dt"
    val yesNoQuestionAnswer: Int => String = questionNumber => s"#main-content > div > div > dl > div:nth-child($questionNumber) > dd.govuk-summary-list__value"
  }

  import Selectors._

  // Agent or individual
  trait SpecificExpectedResults {
    val titleExpected: String
  }

  // Generic content
  trait CommonExpectedResults {
    val captionExpected: String

    val changeLinkExpected: String

    val questionInterestFromGiltEdgedOrAccruedIncomeSecuritiesExpected: String
    val questionAmountOfInterestExpected: String
    val questionTaxTakenOffExpected: String
    val questionAmountOfTaxTakenOffExpected: String

    val changeInterestFromGiltEdgedOrAccruedIncomeSecuritiesHref: String
    val changeAmountOfInterestHref: String
    val changeTaxTakenOffHref: String
    val changeAmountOfTaxTakenOffHref: String

    val changeInterestFromGiltEdgedOrAccruedIncomeSecuritiesHiddenText: String
    val changeAmountOfInterestHiddenText: String
    val changeTaxTakenOffHiddenText: String
    val changeAmountOfTaxTakenOffHiddenText: String

    val submitText: String
    val submitLink: String
    val Yes: String
    val No: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val captionExpected = s"Interest from gilt-edged or accrued income securities for 6 April $taxYearEOY to 5 April $taxYear"

    val changeLinkExpected = "Change"

    val questionInterestFromGiltEdgedOrAccruedIncomeSecuritiesExpected = "Interest from gilt-edged or accrued income securities"
    val questionAmountOfInterestExpected = "Amount of interest"
    val questionTaxTakenOffExpected = "Tax taken off"
    val questionAmountOfTaxTakenOffExpected = "Amount of tax taken off"

    val changeInterestFromGiltEdgedOrAccruedIncomeSecuritiesHref = s"/update-and-submit-income-tax-return/$taxYear/view"
    val changeAmountOfInterestHref = s"/update-and-submit-income-tax-return/$taxYear/view"
    val changeTaxTakenOffHref = s"/update-and-submit-income-tax-return/$taxYear/view"
    val changeAmountOfTaxTakenOffHref = s"/update-and-submit-income-tax-return/$taxYear/view"

    val changeInterestFromGiltEdgedOrAccruedIncomeSecuritiesHiddenText = "Change interest from gilt-edged or accrued income securities"
    val changeAmountOfInterestHiddenText = "Change amount of interest"
    val changeTaxTakenOffHiddenText = "Change tax taken off"
    val changeAmountOfTaxTakenOffHiddenText = "Change amount of tax taken off"

    val submitText = "Save and continue"
    val submitLink = s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/check-interest-from-securities"
    val Yes = "Yes"
    val No = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val captionExpected = s"Llog o warantau gilt neu warantau incwm cronedig ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"

    val changeLinkExpected = "Newid"

    val questionInterestFromGiltEdgedOrAccruedIncomeSecuritiesExpected = "Llog o warantau gilt neu warantau incwm cronedig"
    val questionAmountOfInterestExpected = "Swm y llog"
    val questionTaxTakenOffExpected = "Treth a ddidynnwyd"
    val questionAmountOfTaxTakenOffExpected = "Swm y dreth a ddidynnwyd"

    val changeInterestFromGiltEdgedOrAccruedIncomeSecuritiesHref = s"/update-and-submit-income-tax-return/$taxYear/view"
    val changeAmountOfInterestHref = s"/update-and-submit-income-tax-return/$taxYear/view"
    val changeTaxTakenOffHref = s"/update-and-submit-income-tax-return/$taxYear/view"
    val changeAmountOfTaxTakenOffHref = s"/update-and-submit-income-tax-return/$taxYear/view"

    val changeInterestFromGiltEdgedOrAccruedIncomeSecuritiesHiddenText = "Newid llog o warantau gilt neu warantau incwm cronedig"
    val changeAmountOfInterestHiddenText = "Newid swm y llog"
    val changeTaxTakenOffHiddenText = "Newid treth a ddidynnwyd"
    val changeAmountOfTaxTakenOffHiddenText = "Newid swm y dreth a ddidynnwyd"

    val submitText = "Cadw ac yn eich blaen"
    val submitLink = s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/check-interest-from-securities"
    val Yes = "Iawn"
    val No = "Na"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val titleExpected = "Check your interest from gilt-edged or accrued income securities"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val titleExpected = "Check your client's interest from gilt-edged or accrued income securities"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val titleExpected = "Gwiriwch eich llog o warantau gilt neu warantau incwm cronedig"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val titleExpected = "Gwiriwch log eich cleient o warantau gilt neu warantau incwm cronedig"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  ".show" should {

    userScenarios.foreach { us =>

      import us.commonExpectedResults._

      val specific = us.specificExpectedResults.get

      s"attempt to return the InterestSecuritiesCYA page - ${welshTest(us.isWelsh)} - ${agentTest(us.isAgent)}" which {

        "renders a page with all the fields only cyadata" which {
          lazy val headers = playSessionCookie(us.isAgent) ++ (if (us.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
          lazy val request = FakeRequest("GET", relativeUrl).withHeaders(headers: _*)

          lazy val result = {
            dropSavingsDB()
            emptyUserDataStub()
            insertSavingsCyaData(cyaDataComplete)
            authoriseAgentOrIndividual(us.isAgent)

            route(app, request, "{}").get
          }

          s"has an OK($OK) status" in {
            status(result) shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(contentAsString(result))

          titleCheck(specific.titleExpected, us.isWelsh)
          welshToggleCheck(us.isWelsh)
          h1Check(specific.titleExpected + " " + captionExpected)
          textOnPageCheck(captionExpected, captionSelector)

          buttonCheck(submitText, submitButton)
          formPostLinkCheck(submitLink, submitButtonForm)

          "has an area for section 1" which {
            textOnPageCheck(questionInterestFromGiltEdgedOrAccruedIncomeSecuritiesExpected, Selectors.questionTextSelector(1))
            textOnPageCheck(Yes, Selectors.yesNoQuestionAnswer(1))
            linkCheck(s"$changeLinkExpected ${us.commonExpectedResults.changeInterestFromGiltEdgedOrAccruedIncomeSecuritiesHiddenText}",
              Selectors.questionChangeLinkSelector(1), controllers.savings.routes.SavingsGatewayController.show(taxYear).url)
          }

          "has an area for section 2" which {
            textOnPageCheck(questionAmountOfInterestExpected, Selectors.questionTextSelector(2))
            textOnPageCheck("£100", Selectors.yesNoQuestionAnswer(2))
            linkCheck(s"$changeLinkExpected ${us.commonExpectedResults.changeAmountOfInterestHiddenText}",
              Selectors.questionChangeLinkSelector(2), controllers.savingsBase.routes.SavingsInterestAmountBaseController.show(taxYear).url)
          }

          "has an area for section 3" which {
            textOnPageCheck(questionTaxTakenOffExpected, Selectors.questionTextSelector(3))
            textOnPageCheck(Yes, Selectors.yesNoQuestionAnswer(3))
            linkCheck(s"$changeLinkExpected ${us.commonExpectedResults.changeTaxTakenOffHiddenText}",
              Selectors.questionChangeLinkSelector(3), controllers.savings.routes.TaxTakenFromInterestController.show(taxYear).url)
          }

          "has an area for section 4" which {
            textOnPageCheck(questionAmountOfTaxTakenOffExpected, Selectors.questionTextSelector(4))
            textOnPageCheck("£50", Selectors.yesNoQuestionAnswer(4))
            linkCheck(s"$changeLinkExpected ${us.commonExpectedResults.changeAmountOfTaxTakenOffHiddenText}",
              Selectors.questionChangeLinkSelector(4), controllers.savings.routes.TaxTakenOffInterestController.show(taxYear).url)
          }
        }

        "renders a page with all the fields from priorData only" which {
          lazy val headers = playSessionCookie(us.isAgent) ++ (if (us.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
          lazy val request = FakeRequest("GET", relativeUrl).withHeaders(headers: _*)

          lazy val result = {
            dropSavingsDB()
            emptyUserDataStub()
            userDataStub(IncomeSourcesModel(interestSavings = Some(SavingsIncomeDataModel(None,
              Some(SecuritiesModel(Some(monetaryValue), monetaryValue, Some(monetaryValue))),None))), nino, taxYear)
            authoriseAgentOrIndividual(us.isAgent)

            route(app, request, "{}").get
          }

          s"has an OK($OK) status" in {
            status(result) shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(contentAsString(result))

          titleCheck(specific.titleExpected, us.isWelsh)
          welshToggleCheck(us.isWelsh)
          h1Check(specific.titleExpected + " " + captionExpected)
          textOnPageCheck(captionExpected, captionSelector)

          buttonCheck(submitText, submitButton)
          formPostLinkCheck(submitLink, submitButtonForm)

          "has an area for section 1" which {
            textOnPageCheck(questionInterestFromGiltEdgedOrAccruedIncomeSecuritiesExpected, Selectors.questionTextSelector(1))
            textOnPageCheck(Yes, Selectors.yesNoQuestionAnswer(1))
            linkCheck(s"$changeLinkExpected ${us.commonExpectedResults.changeInterestFromGiltEdgedOrAccruedIncomeSecuritiesHiddenText}",
              Selectors.questionChangeLinkSelector(1), controllers.savings.routes.SavingsGatewayController.show(taxYear).url)
          }

          "has an area for section 2" which {
            textOnPageCheck(questionAmountOfInterestExpected, Selectors.questionTextSelector(2))
            textOnPageCheck("£100", Selectors.yesNoQuestionAnswer(2))
            linkCheck(s"$changeLinkExpected ${us.commonExpectedResults.changeAmountOfInterestHiddenText}",
              Selectors.questionChangeLinkSelector(2), controllers.savingsBase.routes.SavingsInterestAmountBaseController.show(taxYear).url)
          }

          "has an area for section 3" which {
            textOnPageCheck(questionTaxTakenOffExpected, Selectors.questionTextSelector(3))
            textOnPageCheck(Yes, Selectors.yesNoQuestionAnswer(3))
            linkCheck(s"$changeLinkExpected ${us.commonExpectedResults.changeTaxTakenOffHiddenText}",
              Selectors.questionChangeLinkSelector(3), controllers.savings.routes.TaxTakenFromInterestController.show(taxYear).url)
          }

          "has an area for section 4" which {
            textOnPageCheck(questionAmountOfTaxTakenOffExpected, Selectors.questionTextSelector(4))
            textOnPageCheck("£100", Selectors.yesNoQuestionAnswer(4))
            linkCheck(s"$changeLinkExpected ${us.commonExpectedResults.changeAmountOfTaxTakenOffHiddenText}",
              Selectors.questionChangeLinkSelector(4), controllers.savings.routes.TaxTakenOffInterestController.show(taxYear).url)
          }
        }

        "renders a page with all the fields from both cya and priorData" which {
          lazy val headers = playSessionCookie(us.isAgent) ++ (if (us.isWelsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
          lazy val request = FakeRequest("GET", relativeUrl).withHeaders(headers: _*)

          lazy val result = {
            dropSavingsDB()
            emptyUserDataStub()
            insertSavingsCyaData(cyaDataComplete)
            userDataStub(IncomeSourcesModel(interestSavings = Some(SavingsIncomeDataModel(None,
              Some(SecuritiesModel(Some(monetaryValue), monetaryValue, Some(monetaryValue))),None))), nino, taxYear)
            authoriseAgentOrIndividual(us.isAgent)

            route(app, request, "{}").get
          }

          s"has an OK($OK) status" in {
            status(result) shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(contentAsString(result))

          titleCheck(specific.titleExpected, us.isWelsh)
          welshToggleCheck(us.isWelsh)
          h1Check(specific.titleExpected + " " + captionExpected)
          textOnPageCheck(captionExpected, captionSelector)

          buttonCheck(submitText, submitButton)
          formPostLinkCheck(submitLink, submitButtonForm)

          "has an area for section 1" which {
            textOnPageCheck(questionInterestFromGiltEdgedOrAccruedIncomeSecuritiesExpected, Selectors.questionTextSelector(1))
            textOnPageCheck(Yes, Selectors.yesNoQuestionAnswer(1))
            linkCheck(s"$changeLinkExpected ${us.commonExpectedResults.changeInterestFromGiltEdgedOrAccruedIncomeSecuritiesHiddenText}",
              Selectors.questionChangeLinkSelector(1), controllers.savings.routes.SavingsGatewayController.show(taxYear).url)
          }

          "has an area for section 2" which {
            textOnPageCheck(questionAmountOfInterestExpected, Selectors.questionTextSelector(2))
            textOnPageCheck("£100", Selectors.yesNoQuestionAnswer(2))
            linkCheck(s"$changeLinkExpected ${us.commonExpectedResults.changeAmountOfInterestHiddenText}",
              Selectors.questionChangeLinkSelector(2), controllers.savingsBase.routes.SavingsInterestAmountBaseController.show(taxYear).url)
          }

          "has an area for section 3" which {
            textOnPageCheck(questionTaxTakenOffExpected, Selectors.questionTextSelector(3))
            textOnPageCheck(Yes, Selectors.yesNoQuestionAnswer(3))
            linkCheck(s"$changeLinkExpected ${us.commonExpectedResults.changeTaxTakenOffHiddenText}",
              Selectors.questionChangeLinkSelector(3), controllers.savings.routes.TaxTakenFromInterestController.show(taxYear).url)
          }

          "has an area for section 4" which {
            textOnPageCheck(questionAmountOfTaxTakenOffExpected, Selectors.questionTextSelector(4))
            textOnPageCheck("£50", Selectors.yesNoQuestionAnswer(4))
            linkCheck(s"$changeLinkExpected ${us.commonExpectedResults.changeAmountOfTaxTakenOffHiddenText}",
              Selectors.questionChangeLinkSelector(4), controllers.savings.routes.TaxTakenOffInterestController.show(taxYear).url)
          }
        }

        "the authorization fails" which {
          lazy val result = {
            unauthorisedAgentOrIndividual(us.isAgent)
            urlGet(s"$appUrl/$taxYear/interest/check-interest-from-securities", us.isWelsh, follow = true,  playSessionCookie(us.isAgent))
          }

          s"has an Unauthorised($UNAUTHORIZED) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }

        "redirect to Savings Interest Amount page" which {
          lazy val result = {
            dropSavingsDB()
            emptyUserDataStub()
            insertSavingsCyaData(Some(SavingsIncomeCYAModel(Some(true), None)))
            userDataStub(IncomeSourcesModel(interestSavings = Some(SavingsIncomeDataModel(None, None, None))), nino, taxYear)
            authoriseAgentOrIndividual(us.isAgent)

            urlGet(s"$appUrl/$taxYear/interest/check-interest-from-securities", us.isWelsh, follow = false,  playSessionCookie(us.isAgent))
          }

          s"has a SEE_OTHER($SEE_OTHER) status" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe controllers.savingsBase.routes.SavingsInterestAmountBaseController.show(taxYear).url
          }
        }

        "redirect to Tax Taken From Interest page" which {
          lazy val result = {
            dropSavingsDB()
            emptyUserDataStub()
            insertSavingsCyaData(Some(SavingsIncomeCYAModel(Some(true), Some(monetaryValue))))
            userDataStub(IncomeSourcesModel(interestSavings = Some(SavingsIncomeDataModel(None, None, None))), nino, taxYear)

            authoriseAgentOrIndividual(us.isAgent)
            urlGet(s"$appUrl/$taxYear/interest/check-interest-from-securities", us.isWelsh, follow = false,  playSessionCookie(us.isAgent))
          }

          s"has a SEE_OTHER($SEE_OTHER) status" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe controllers.savings.routes.TaxTakenFromInterestController.show(taxYear).url
          }
        }

        "redirect to Tax Taken Off Interest page" which {
          lazy val result = {
            dropSavingsDB()
            emptyUserDataStub()
            insertSavingsCyaData(Some(SavingsIncomeCYAModel(Some(true), Some(monetaryValue), Some(true))))
            userDataStub(IncomeSourcesModel(interestSavings = Some(SavingsIncomeDataModel(None, None, None))), nino, taxYear)

            authoriseAgentOrIndividual(us.isAgent)
            urlGet(s"$appUrl/$taxYear/interest/check-interest-from-securities", us.isWelsh, follow = false,  playSessionCookie(us.isAgent))
          }

          s"has a SEE_OTHER($SEE_OTHER) status" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe controllers.savings.routes.TaxTakenOffInterestController.show(taxYear).url
          }
        }

      }

    }

    "render the page without gateway row when 'miniJourneyEnabled' is true" in {
      val application = buildApplication(miniJourneyEnabled = true)

      running(application) {
        authoriseIndividual(Some(nino))
        dropSavingsDB()
        emptyUserDataStub()
        insertSavingsCyaData(cyaDataComplete)

        val headers: Seq[(String, String)] = playSessionCookie() ++ Seq("Csrf-Token" -> "nocheck")
        val request = FakeRequest(GET, relativeUrl).withHeaders(headers: _*)
        val result = route(application, request).value

        status(result) mustEqual OK
        contentAsString(result) contains "<dt class=\"govuk-summary-list__key govuk-!-width-one-half\">\n        " +
          "Interest from gilt-edged or accrued income securities\n      </dt>" shouldBe false
      }
    }
  }

  ".submit" should {
    s"attempt to return the Task List page" in {
      val application = buildApplication(miniJourneyEnabled = true)

      running(application) {
        authoriseIndividual()
        dropSavingsDB()
        emptyUserDataStub()
        insertSavingsCyaData(cyaDataComplete)

        stubPut(s"/income-tax/nino/$nino/sources/session\\?taxYear=$taxYear", NO_CONTENT, "")
        stubPut(s"/income-tax-interest/income-tax/nino/AA123456A/savings\\?taxYear=$taxYear", NO_CONTENT, "")

        val headers: Seq[(String, String)] = playSessionCookie() ++ Seq("Csrf-Token" -> "nocheck")
        val request = FakeRequest(POST, relativeUrl).withHeaders(headers: _*)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(s"${appConfig.incomeTaxSubmissionBaseUrl}/$taxYear/tasklist")
      }
    }

    s"attempt to return the Task List page when prior data exists and is different" in {
      val application = buildApplication(miniJourneyEnabled = true)

      running(application) {
        authoriseIndividual()
        dropSavingsDB()
        emptyUserDataStub()
        insertSavingsCyaData(Some(SavingsIncomeCYAModel(Some(true), Some(monetaryValue), Some(true), Some(monetaryValue))))
        userDataStub(IncomeSourcesModel(interestSavings = Some(SavingsIncomeDataModel(None,
          Some(SecuritiesModel(Some(monetaryValue), monetaryValue, Some(monetaryValue))), None))), nino, taxYear)

        stubPut(s"/income-tax-interest/income-tax/nino/AA123456A/savings\\?taxYear=$taxYear", NO_CONTENT, "")
        val headers: Seq[(String, String)] = playSessionCookie() ++ Seq("Csrf-Token" -> "nocheck")

        val request = FakeRequest(POST, relativeUrl).withHeaders(headers: _*)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(s"${appConfig.incomeTaxSubmissionBaseUrl}/$taxYear/tasklist")
      }
    }

    s"attempt to return to the Task List page when prior data exists has a status of $SEE_OTHER" in {
      val application = buildApplication(miniJourneyEnabled = true)

      running(application) {
        authoriseIndividual()
        dropSavingsDB()
        emptyUserDataStub()
        insertSavingsCyaData(Some(SavingsIncomeCYAModel(Some(true), Some(monetaryValue), Some(true), Some(monetaryValue))))
        userDataStub(IncomeSourcesModel(interestSavings = Some(SavingsIncomeDataModel(None,
          Some(SecuritiesModel(Some(monetaryValue), monetaryValue, Some(monetaryValue))), None))), nino, taxYear)

        stubPut(s"/income-tax-interest/income-tax/nino/AA123456A/savings\\?taxYear=$taxYear", NO_CONTENT, "")

        val headers: Seq[(String, String)] = playSessionCookie() ++ Seq("Csrf-Token" -> "nocheck")
        val request = FakeRequest(POST, relativeUrl).withHeaders(headers: _*)
        val result = route(application, request).value

        status(result) mustEqual SEE_OTHER
        redirectLocation(result) mustBe Some(s"${appConfig.incomeTaxSubmissionBaseUrl}/$taxYear/tasklist")
      }
    }

    "the authorization fails" in {
      lazy val result = {
        dropSavingsDB()
        emptyUserDataStub()
        authoriseIndividualUnauthorized()
        urlPost(s"$appUrl/$taxYear/interest/check-interest-from-securities", "{}", welsh = false, follow = true, playSessionCookie())
      }

      result.status shouldBe UNAUTHORIZED
    }

  }

}
