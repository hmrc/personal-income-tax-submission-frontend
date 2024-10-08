/*
 * Copyright 2023 HM Revenue & Customs
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

package test.controllers.interest

import common.SessionValues
import forms.AccountList
import models.interest.{InterestAccountModel, InterestCYAModel}
import models.priorDataModels.{IncomeSourcesModel, InterestModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import test.utils.{IntegrationTest, InterestDatabaseHelper, ViewHelpers}

class ChooseAccountControllerISpec extends IntegrationTest with ViewHelpers with InterestDatabaseHelper {

  val UNTAXED: String = "untaxed"
  val TAXED: String = "taxed"

  def chooseAccountUrl(taxType: String): String = s"$appUrl/$taxYear/interest/which-account-did-you-get-$taxType-interest-from"

  object Selectors {
    val radioDivider = "#main-content > div > div > form > div > fieldset > div > div.govuk-radios__divider"
    val continueSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
  }

  trait SpecificExpectedResults {
    def expectedTitleTaxed: String
    def expectedTitleUntaxed: String

    def expectedH1Taxed: String
    def expectedH1Untaxed: String

    def expectedErrorTitleTaxed: String
    def expectedErrorTitleUntaxed: String

    def expectedErrorTextTaxed: String
    def expectedErrorTextUntaxed: String
  }

  trait CommonExpectedResults {
    val natwestAccount: String
    val halifaxAccount: String
    val santanderAccount: String
    val nationwideAccount: String
    val barclayAccount: String
    val addAccountText: String
    val captionExpected: String
    val or: String
    val continueText: String

    def continueLink(taxType: String): String
  }

  object IndividualExpectedEnglish extends SpecificExpectedResults {
    def expectedTitleTaxed: String = "Which account did you get taxed UK interest from?"
    def expectedTitleUntaxed: String = "Which account did you get untaxed UK interest from?"


    def expectedH1Taxed: String = "Which account did you get taxed UK interest from?"
    def expectedH1Untaxed: String = "Which account did you get untaxed UK interest from?"

    def expectedErrorTitleTaxed: String = s"Error: ${expectedTitleTaxed}"
    def expectedErrorTitleUntaxed: String = s"Error: ${expectedTitleUntaxed}"

    def expectedErrorTextTaxed: String = "Select the account you got taxed UK interest from"
    def expectedErrorTextUntaxed: String = "Select the account you got untaxed UK interest from"
  }

  object AgentExpectedEnglish extends SpecificExpectedResults {
    def expectedTitleTaxed: String = "Which account did your client get taxed UK interest from?"
    def expectedTitleUntaxed: String = "Which account did your client get untaxed UK interest from?"

    def expectedH1Taxed: String = "Which account did your client get taxed UK interest from?"
    def expectedH1Untaxed: String = "Which account did your client get untaxed UK interest from?"

    def expectedErrorTitleTaxed: String = s"Error: ${expectedTitleTaxed}"
    def expectedErrorTitleUntaxed: String = s"Error: ${expectedTitleUntaxed}"

    def expectedErrorTextTaxed: String = "Select the account your client got taxed UK interest from"
    def expectedErrorTextUntaxed: String = "Select the account your client got untaxed UK interest from"
  }

  object AllExpectedEnglish extends CommonExpectedResults {
    val natwestAccount = "Natwest"
    val halifaxAccount = "Halifax"
    val santanderAccount = "Santander"
    val nationwideAccount = "Nationwide"
    val barclayAccount = "Barclays"
    val addAccountText = "Add a new account"
    val captionExpected = s"Interest for 6 April $taxYearEOY to 5 April $taxYear"
    val or = "or"
    val continueText = "Continue"

    def continueLink(taxType: String): String =
      s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/which-account-did-you-get-$taxType-interest-from"
  }

  object IndividualExpectedWelsh extends SpecificExpectedResults {
    def expectedTitleTaxed: String = "O ba gyfrif a gawsoch log y DU a drethwyd?"
    def expectedTitleUntaxed: String = "O ba gyfrif a gawsoch log y DU sydd heb ei drethu?"

    def expectedH1Taxed: String = "O ba gyfrif a gawsoch log y DU a drethwyd?"
    def expectedH1Untaxed: String = "O ba gyfrif a gawsoch log y DU sydd heb ei drethu?"

    def expectedErrorTitleTaxed: String = s"Error: ${expectedTitleTaxed}"
    def expectedErrorTitleUntaxed: String = s"Error: ${expectedTitleUntaxed}"

    def expectedErrorTextTaxed: String = "Dewiswch y cyfrif a gawsoch log y DU a drethwyd"
    def expectedErrorTextUntaxed: String = "Dewiswch y cyfrif a gawsoch log y DU sydd heb ei drethu"
  }

  object AgentExpectedWelsh extends SpecificExpectedResults {
    def expectedTitleTaxed: String = "O ba gyfrif a gafodd eich cleient log y DU a drethwyd?"
    def expectedTitleUntaxed: String = "O ba gyfrif a gafodd eich cleient log y DU sydd heb ei drethu?"

    def expectedH1Taxed: String = "O ba gyfrif a gafodd eich cleient log y DU a drethwyd?"
    def expectedH1Untaxed: String = "O ba gyfrif a gafodd eich cleient log y DU sydd heb ei drethu?"

    def expectedErrorTitleTaxed: String = s"Error: ${expectedTitleTaxed}"
    def expectedErrorTitleUntaxed: String = s"Error: ${expectedTitleUntaxed}"

    def expectedErrorTextTaxed: String = "Dewiswch y cyfrif a gafodd eich cleient log y DU a drethwyd"
    def expectedErrorTextUntaxed: String = "Dewiswch y cyfrif a gafodd eich cleient log y DU sydd heb ei drethu"
  }

  object AllExpectedWelsh extends CommonExpectedResults {
    val natwestAccount = "Natwest"
    val halifaxAccount = "Halifax"
    val santanderAccount = "Santander"
    val nationwideAccount = "Nationwide"
    val barclayAccount = "Barclays"
    val addAccountText = "Ychwanegwch gyfrif newydd"
    val captionExpected = s"Llog ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val or = "neu"
    val continueText = "Yn eich blaen"

    def continueLink(taxType: String): String =
      s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/which-account-did-you-get-$taxType-interest-from"
  }

  val errorSummaryHref = "#value"

  val sessionId1 = "session-id-1"

  val amount = 1000
  val accounts = Seq(InterestModel("Natwest", "1", taxedUkInterest = Some(amount), untaxedUkInterest = None),
    InterestModel("Halifax", "2", taxedUkInterest = None, untaxedUkInterest = Some(amount)))

  val accounts2 = Seq(InterestAccountModel(None, "Santander", untaxedAmount = Some(amount), taxedAmount = None, Some(sessionId1)),
    InterestAccountModel(None, "Nationwide", untaxedAmount = None, taxedAmount = Some(amount)),
    InterestAccountModel(None, "Barclays", untaxedAmount = Some(amount), taxedAmount = Some(amount)))

  val accounts3 = Seq(InterestModel("Natwest", "1", taxedUkInterest = Some(amount), untaxedUkInterest = Some(amount)),
    InterestModel("Halifax", "2", taxedUkInterest = Some(amount), untaxedUkInterest = None))

  val accounts4 = Seq(InterestModel("Natwest", "1", taxedUkInterest = Some(amount), untaxedUkInterest = Some(amount)),
    InterestModel("Halifax", "2", taxedUkInterest = None, untaxedUkInterest = Some(amount)))


  val userScenarios =
    Seq(UserScenario(isWelsh = false, isAgent = false, AllExpectedEnglish, Some(IndividualExpectedEnglish)),
      UserScenario(isWelsh = false, isAgent = true, AllExpectedEnglish, Some(AgentExpectedEnglish)),
      UserScenario(isWelsh = true, isAgent = false, AllExpectedWelsh, Some(IndividualExpectedWelsh)),
      UserScenario(isWelsh = true, isAgent = true, AllExpectedWelsh, Some(AgentExpectedWelsh)))

  ".show" when {
    userScenarios.foreach { us =>

      s"language is ${welshTest(us.isWelsh)} and request is from an ${agentTest(us.isAgent)}" should {

        import Selectors._
        import us.commonExpectedResults._
        import us.specificExpectedResults._

        s"render $TAXED interest page with all accounts without a predefined Taxed amount." which {

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            dropInterestDB()
            emptyUserDataStub()
            insertInterestCyaData(Some(InterestCYAModel(taxedUkInterest = Some(true))))
            userDataStub(IncomeSourcesModel(interest = Some(accounts)), nino, taxYear)
            urlGet(chooseAccountUrl(TAXED), us.isWelsh, headers = playSessionCookie(us.isAgent))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(get.expectedTitleTaxed, us.isWelsh)
          h1Check(get.expectedH1Taxed + " " + captionExpected)
          radioButtonCheck(halifaxAccount, 1)
          radioButtonCheck(addAccountText, 2)
          textOnPageCheck(or, radioDivider)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink(TAXED), continueButtonFormSelector)

          s"should not display the following values" in {
            document().body().toString.contains(natwestAccount) shouldBe false
          }

          welshToggleCheck(us.isWelsh)
        }

        s"render $TAXED interest page with accounts from prior and cya data without a predefined Taxed amount." which {

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            dropInterestDB()
            emptyUserDataStub()
            userDataStub(IncomeSourcesModel(interest = Some(accounts)), nino, taxYear)
            insertInterestCyaData(Some(InterestCYAModel(accounts = accounts2, taxedUkInterest = Some(true))))
            urlGet(chooseAccountUrl(TAXED), us.isWelsh, headers = playSessionCookie(us.isAgent))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(get.expectedTitleTaxed, us.isWelsh)
          h1Check(get.expectedH1Taxed + " " + captionExpected)
          radioButtonCheck(santanderAccount, 1)
          radioButtonCheck(halifaxAccount, 2)
          radioButtonCheck(addAccountText, 3)
          textOnPageCheck(or, radioDivider)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink(TAXED), continueButtonFormSelector)

          s"should not display the following values" in {
            document().body().toString.contains(nationwideAccount) shouldBe false
            document().body().toString.contains(natwestAccount) shouldBe false
            document().body().toString.contains(barclayAccount) shouldBe false
          }

          welshToggleCheck(us.isWelsh)
        }

        s"render $UNTAXED interest page with all accounts without a predefined Untaxed amount." which {

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            dropInterestDB()
            emptyUserDataStub()
            insertInterestCyaData(Some(InterestCYAModel(untaxedUkInterest = Some(true))))
            userDataStub(IncomeSourcesModel(interest = Some(accounts)), nino, taxYear)
            urlGet(chooseAccountUrl(UNTAXED), us.isWelsh, headers = playSessionCookie(us.isAgent))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(get.expectedTitleUntaxed, us.isWelsh)
          h1Check(get.expectedH1Untaxed + " " + captionExpected)
          radioButtonCheck(natwestAccount, 1)
          radioButtonCheck(addAccountText, 2)
          textOnPageCheck(or, radioDivider)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink(UNTAXED), continueButtonFormSelector)

          s"should not display the following values" in {
            document().body().toString.contains(halifaxAccount) shouldBe false
          }

          welshToggleCheck(us.isWelsh)

        }

        s"render $UNTAXED interest page with accounts from prior and cya data without a predefined untaxed amount." which {

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(us.isAgent)
            dropInterestDB()
            emptyUserDataStub()
            userDataStub(IncomeSourcesModel(interest = Some(accounts)), nino, taxYear)
            insertInterestCyaData(Some(InterestCYAModel(accounts = accounts2, untaxedUkInterest = Some(true))))
            urlGet(chooseAccountUrl(UNTAXED), us.isWelsh, headers = playSessionCookie(us.isAgent))
          }

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(get.expectedTitleUntaxed, us.isWelsh)
          h1Check(get.expectedH1Untaxed + " " + captionExpected)
          radioButtonCheck(nationwideAccount, 1)
          radioButtonCheck(natwestAccount, 2)
          radioButtonCheck(addAccountText, 3)
          textOnPageCheck(or, radioDivider)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(continueLink(UNTAXED), continueButtonFormSelector)

          s"should not display the following values" in {
            document().body().toString.contains(santanderAccount) shouldBe false
            document().body().toString.contains(halifaxAccount) shouldBe false
            document().body().toString.contains(barclayAccount) shouldBe false
          }

          welshToggleCheck(us.isWelsh)
        }
      }
    }
  }


  ".show" should {

    s"redirect from $TAXED  when there are previous accounts but they all have an $TAXED amount defined" when {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropInterestDB()
        emptyUserDataStub()
        insertInterestCyaData(Some(InterestCYAModel(taxedUkInterest = Some(true))))
        userDataStub(IncomeSourcesModel(interest = Some(accounts3)), nino, taxYear)
        urlGet(chooseAccountUrl(TAXED), follow = false, headers = playSessionCookie())
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
      }

    }
    s"redirect from $UNTAXED  when there are previous accounts but they all have an $UNTAXED amount defined" when {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropInterestDB()
        emptyUserDataStub()
        insertInterestCyaData(Some(InterestCYAModel(untaxedUkInterest = Some(true))))
        userDataStub(IncomeSourcesModel(interest = Some(accounts4)), nino, taxYear)
        urlGet(chooseAccountUrl(UNTAXED), follow = false, headers = playSessionCookie())
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
      }

    }
    s"redirect to from $TAXED page when there are no previous accounts" when {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropInterestDB()
        emptyUserDataStub()
        insertInterestCyaData(Some(InterestCYAModel(taxedUkInterest = Some(true))))
        urlGet(chooseAccountUrl(TAXED), follow = false, headers = playSessionCookie())
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
      }
    }

    s"redirect to from $UNTAXED page when there are no previous accounts" when {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropInterestDB()
        emptyUserDataStub()
        insertInterestCyaData(Some(InterestCYAModel(untaxedUkInterest = Some(true))))
        urlGet(chooseAccountUrl(UNTAXED), follow = false, headers = playSessionCookie())
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
      }
    }

    s"redirect from $TAXED page to Did you get untaxed interest page? when the is the preceding question has been answered no" when {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropInterestDB()
        emptyUserDataStub()
        insertInterestCyaData(Some(InterestCYAModel(untaxedUkInterest = Some(true), taxedUkInterest = Some(false))))
        userDataStub(IncomeSourcesModel(interest = Some(accounts3)), nino, taxYear)
        urlGet(chooseAccountUrl(TAXED), follow = false, headers = playSessionCookie())
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
      }
    }

    s"redirect from $UNTAXED page to Did you get untaxed interest page? when the is the preceding question has been answered no" when {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropInterestDB()
        emptyUserDataStub()
        insertInterestCyaData(Some(InterestCYAModel(untaxedUkInterest = Some(false), taxedUkInterest = Some(true))))
        userDataStub(IncomeSourcesModel(interest = Some(accounts4)), nino, taxYear)
        urlGet(chooseAccountUrl(UNTAXED), follow = false, headers = playSessionCookie())
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
      }
    }

    s"redirect from $TAXED page to Did you get untaxed interest page? when the is the preceding question hasn't been answered" when {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropInterestDB()
        emptyUserDataStub()
        insertInterestCyaData(Some(InterestCYAModel(untaxedUkInterest = Some(true), taxedUkInterest = None)))
        userDataStub(IncomeSourcesModel(interest = Some(accounts3)), nino, taxYear)
        urlGet(chooseAccountUrl(TAXED), follow = false, headers = playSessionCookie())
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
      }
    }

    s"redirect from $UNTAXED page to Did you get untaxed interest page? when the is the preceding question hasn't been answered" when {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropInterestDB()
        emptyUserDataStub()
        insertInterestCyaData(Some(InterestCYAModel(untaxedUkInterest = None, taxedUkInterest = Some(true))))
        userDataStub(IncomeSourcesModel(interest = Some(accounts4)), nino, taxYear)
        urlGet(chooseAccountUrl(UNTAXED), follow = false, headers = playSessionCookie())
      }

      "has an SEE_OTHER(303) status" in {
        result.status shouldBe SEE_OTHER
      }
    }

    s"redirect from $TAXED page to overview page when there is no cya data" when {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropInterestDB()
        emptyUserDataStub()
        userDataStub(IncomeSourcesModel(interest = Some(accounts3)), nino, taxYear)
        stubGet(s"/update-and-submit-income-tax-return/$taxYear/view", OK, "overview page content")
        urlGet(chooseAccountUrl(TAXED), headers = playSessionCookie())
      }

      "has an OK(200) status" in {
        result.status shouldBe OK
        result.body shouldBe "overview page content"
      }
    }


    s"redirect from $UNTAXED page to overview page when there is no cya dat" when {
      lazy val result: WSResponse = {
        authoriseIndividual()
        dropInterestDB()
        emptyUserDataStub()
        userDataStub(IncomeSourcesModel(interest = Some(accounts4)), nino, taxYear)
        stubGet(s"/update-and-submit-income-tax-return/$taxYear/view", OK, "overview page content")
        urlGet(chooseAccountUrl(UNTAXED), headers = playSessionCookie())
      }

      "has an OK(200) status" in {
        result.status shouldBe OK
        result.body shouldBe "overview page content"
      }
    }


    s"returns an action when auth call fails for $TAXED page" which {
      lazy val result: WSResponse = {
        authoriseIndividualUnauthorized()
        dropInterestDB()
        emptyUserDataStub()
        urlGet(chooseAccountUrl(TAXED), headers = playSessionCookie())
      }
      "has an UNAUTHORIZED(401) status" in {
        result.status shouldBe UNAUTHORIZED
      }
    }

    s"returns an action when auth call fails for $UNTAXED page" which {
      lazy val result: WSResponse = {
        authoriseIndividualUnauthorized()
        dropInterestDB()
        emptyUserDataStub()
        urlGet(chooseAccountUrl(UNTAXED), headers = playSessionCookie())
      }
      "has an UNAUTHORIZED(401) status" in {
        result.status shouldBe UNAUTHORIZED
      }
    }
  }


    ".submit" should {

      userScenarios.foreach { us =>

        s"language is ${welshTest(us.isWelsh)} and request is from an ${agentTest(us.isAgent)}" should {

          import Selectors._
          import us.commonExpectedResults._
          import us.specificExpectedResults._

          s"render a $TAXED page with errors when no radio button is selected" when {

            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(us.isAgent)
              dropInterestDB()
              emptyUserDataStub()
              userDataStub(IncomeSourcesModel(interest = Some(accounts)), nino, taxYear)
              insertInterestCyaData(Some(InterestCYAModel(accounts = accounts2, taxedUkInterest = Some(true))))
              urlPost(chooseAccountUrl(TAXED), body = Map[String, String](), us.isWelsh, headers = playSessionCookie(us.isAgent))
            }

            "has an BAD_REQUEST(400) status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit val document: () => Document = () => Jsoup.parse(result.body)

            h1Check(get.expectedH1Taxed + " " + captionExpected)
            radioButtonCheck(santanderAccount, 1)
            radioButtonCheck(halifaxAccount, 2)
            radioButtonCheck(addAccountText, 3)
            textOnPageCheck(or, radioDivider)
            buttonCheck(continueText, continueSelector)
            formPostLinkCheck(continueLink(TAXED), continueButtonFormSelector)

            errorSummaryCheck(get.expectedErrorTextTaxed, errorSummaryHref, us.isWelsh)

            welshToggleCheck(us.isWelsh)
          }

          s"render a $UNTAXED page with errors when no radio button is selected" when {

            lazy val result: WSResponse = {
              authoriseAgentOrIndividual(us.isAgent)
              dropInterestDB()
              emptyUserDataStub()
              userDataStub(IncomeSourcesModel(interest = Some(accounts)), nino, taxYear)
              insertInterestCyaData(Some(InterestCYAModel(accounts = accounts2, untaxedUkInterest = Some(true))))
              urlPost(chooseAccountUrl(UNTAXED), body = Map[String, String](), us.isWelsh, headers = playSessionCookie(us.isAgent))
            }

            "has an BAD_REQUEST(400) status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit val document: () => Document = () => Jsoup.parse(result.body)

            h1Check(get.expectedH1Untaxed + " " + captionExpected)
            radioButtonCheck(nationwideAccount, 1)
            radioButtonCheck(natwestAccount, 2)
            radioButtonCheck(addAccountText, 3)
            textOnPageCheck(or, radioDivider)
            buttonCheck(continueText, continueSelector)
            formPostLinkCheck(continueLink(UNTAXED), continueButtonFormSelector)

            errorSummaryCheck(get.expectedErrorTextUntaxed, errorSummaryHref, us.isWelsh)

            welshToggleCheck(us.isWelsh)
          }
        }
      }
    }

    ".submit" should {

      s"redirect user to Taxed Interest Amount page when Add another account radio is clicked on $TAXED choose account page" when {

        lazy val result: WSResponse = {
          authoriseIndividual()
          dropInterestDB()
          emptyUserDataStub()
          userDataStub(IncomeSourcesModel(interest = Some(accounts)), nino, taxYear)
          insertInterestCyaData(Some(InterestCYAModel(accounts = accounts2, taxedUkInterest = Some(true))))
          urlPost(chooseAccountUrl(TAXED), follow = false, body =
            Map(AccountList.accountName -> SessionValues.ADD_A_NEW_ACCOUNT), headers = playSessionCookie())
        }

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header(HeaderNames.LOCATION).head should
            include(s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/add-taxed-uk-interest-account")
        }
      }

      s"redirect user to Untaxed Interest Amount page when Add another account radio is clicked on $UNTAXED choose account page" when {

        lazy val result: WSResponse = {
          authoriseIndividual()
          dropInterestDB()
          emptyUserDataStub()
          userDataStub(IncomeSourcesModel(interest = Some(accounts)), nino, taxYear)
          insertInterestCyaData(Some(InterestCYAModel(accounts = accounts2, untaxedUkInterest = Some(true))))
          urlPost(chooseAccountUrl(UNTAXED), follow = false, body =
            Map(AccountList.accountName -> SessionValues.ADD_A_NEW_ACCOUNT), headers = playSessionCookie())
        }

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header(HeaderNames.LOCATION).head should
            include(s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/add-untaxed-uk-interest-account")
        }
      }

      s"redirect user to how-much-taxed-uk-interest did you get from[account Name] page when specific account is selected on $TAXED choose account page" when {

        lazy val result: WSResponse = {
          authoriseIndividual()
          dropInterestDB()
          emptyUserDataStub()
          userDataStub(IncomeSourcesModel(interest = Some(accounts)), nino, taxYear)
          insertInterestCyaData(Some(InterestCYAModel(accounts = accounts2, taxedUkInterest = Some(true))))
          urlPost(chooseAccountUrl(TAXED), follow = false, body =
            Map(AccountList.accountName -> sessionId1), headers = playSessionCookie())
        }

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header(HeaderNames.LOCATION).head should
            include(s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/change-taxed-uk-interest?accountId=session-id-1")
        }
      }

      s"redirect user to how-much-untaxed-uk-interest did you get from[account Name]" +
        s"page when specific account is selected on $UNTAXED choose account page" when {

        lazy val result: WSResponse = {
          authoriseIndividual()
          dropInterestDB()
          emptyUserDataStub()
          userDataStub(IncomeSourcesModel(interest = Some(accounts)), nino, taxYear)
          insertInterestCyaData(Some(InterestCYAModel(accounts = accounts2, untaxedUkInterest = Some(true))))
          urlPost(chooseAccountUrl(UNTAXED), follow = false, body =
            Map(AccountList.accountName -> "1"), headers = playSessionCookie())
        }

        "has a SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
          result.header(HeaderNames.LOCATION).head should
            include(s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/change-untaxed-uk-interest?accountId=1")
        }
      }


      s"redirect from $TAXED page to Did you get untaxed interest page? when the is the preceding question has been answered no" when {
        lazy val result: WSResponse = {
          authoriseIndividual()
          dropInterestDB()
          emptyUserDataStub()
          insertInterestCyaData(Some(InterestCYAModel(untaxedUkInterest = Some(true), taxedUkInterest = Some(false))))
          userDataStub(IncomeSourcesModel(interest = Some(accounts3)), nino, taxYear)
          urlGet(chooseAccountUrl(TAXED), follow = false, headers = playSessionCookie())
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
        }
      }

      s"redirect from $UNTAXED page to Did you get untaxed interest page? when the is the preceding question has been answered no" when {
        lazy val result: WSResponse = {
          authoriseIndividual()
          dropInterestDB()
          emptyUserDataStub()
          insertInterestCyaData(Some(InterestCYAModel(untaxedUkInterest = Some(false), taxedUkInterest = Some(true))))
          userDataStub(IncomeSourcesModel(interest = Some(accounts4)), nino, taxYear)
          urlGet(chooseAccountUrl(UNTAXED), follow = false, headers = playSessionCookie())
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
        }
      }

      s"redirect from $TAXED page to Did you get untaxed interest page? when the is the preceding question hasn't been answered" when {
        lazy val result: WSResponse = {
          authoriseIndividual()
          dropInterestDB()
          emptyUserDataStub()
          insertInterestCyaData(Some(InterestCYAModel(untaxedUkInterest = Some(true), taxedUkInterest = None)))
          userDataStub(IncomeSourcesModel(interest = Some(accounts3)), nino, taxYear)
          urlGet(chooseAccountUrl(TAXED), follow = false, headers = playSessionCookie())
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
        }
      }

      s"redirect from $UNTAXED page to Did you get untaxed interest page? when the is the preceding question hasn't been answered" when {
        lazy val result: WSResponse = {
          authoriseIndividual()
          dropInterestDB()
          emptyUserDataStub()
          insertInterestCyaData(Some(InterestCYAModel(untaxedUkInterest = None, taxedUkInterest = Some(true))))
          userDataStub(IncomeSourcesModel(interest = Some(accounts4)), nino, taxYear)
          urlGet(chooseAccountUrl(UNTAXED), follow = false, headers = playSessionCookie())
        }

        "has an SEE_OTHER(303) status" in {
          result.status shouldBe SEE_OTHER
        }
      }

      s"redirect from $TAXED page to overview page when there is no cya data" when {
        lazy val result: WSResponse = {
          authoriseIndividual()
          dropInterestDB()
          emptyUserDataStub()
          userDataStub(IncomeSourcesModel(interest = Some(accounts3)), nino, taxYear)
          stubGet(s"/update-and-submit-income-tax-return/$taxYear/view", OK, "overview page content")
          urlGet(chooseAccountUrl(TAXED), headers = playSessionCookie())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
          result.body shouldBe "overview page content"
        }
      }


      s"redirect from $UNTAXED page to overview page when there is no cya dat" when {
        lazy val result: WSResponse = {
          authoriseIndividual()
          dropInterestDB()
          emptyUserDataStub()
          userDataStub(IncomeSourcesModel(interest = Some(accounts4)), nino, taxYear)
          stubGet(s"/update-and-submit-income-tax-return/$taxYear/view", OK, "overview page content")
          urlGet(chooseAccountUrl(UNTAXED), headers = playSessionCookie())
        }

        "has an OK(200) status" in {
          result.status shouldBe OK
          result.body shouldBe "overview page content"
        }
      }

    }
  }

