/*
 * Copyright 2022 HM Revenue & Customs
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

import common.InterestTaxTypes.{TAXED, UNTAXED}
import forms.YesNoForm
import models.interest.{InterestAccountModel, InterestCYAModel}
import models.priorDataModels.{IncomeSourcesModel, InterestModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER, UNAUTHORIZED}
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, InterestDatabaseHelper, ViewHelpers}

class RemoveAccountControllerISpec extends IntegrationTest with InterestDatabaseHelper with ViewHelpers {

  val taxYear: Int = 2022
  val taxYearMinusOne: Int = taxYear - 1
  val amount: BigDecimal = 25

  object Selectors {
    val captionSelector = ".govuk-caption-l"
    val thisWillTextSelector = "#value-hint > div > p"
    val yesOptionSelector = "#main-content > div > div > form > div > fieldset > div.govuk-radios.govuk-radios--inline > div:nth-child(1) > label"
    val noOptionSelector = "#main-content > div > div > form > div > fieldset > div.govuk-radios.govuk-radios--inline > div:nth-child(2) > label"
    val continueButtonSelector = "#continue"
    val continueButtonFormSelector = "#main-content > div > div > form"
    val errorSummaryHref = "#value"

    val errorSummarySelector = ".govuk-error-summary"
    val errorSummaryTitleSelector = ".govuk-error-summary__title"
    val errorSummaryTextSelector = ".govuk-error-summary__body"
  }

  import Selectors._

  trait SpecificExpectedResults {
  }

  trait CommonExpectedResults {
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedH1: String
    val expectedCaption: String
    val thisWillTextUntaxed: String
    val thisWillTextTaxed: String
    val yesText: String
    val noText: String
    val continueText: String

    val expectedErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedTitle = "Are you sure you want to remove this account?"
    val expectedErrorTitle = s"Error: $expectedTitle"
    val expectedH1 = "Are you sure you want to remove Monzo?"
    val expectedCaption = s"Interest for 6 April $taxYearMinusOne to 5 April $taxYear"
    val thisWillTextUntaxed = "This will remove all untaxed UK interest."
    val thisWillTextTaxed = "This will remove all taxed UK interest."
    val yesText = "Yes"
    val noText = "No"
    val continueText = "Continue"

    val expectedErrorText = "Select yes to remove this account"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedTitle = "A ydych yn siŵr eich bod am ddileu’r cyfrif hwn?"
    val expectedErrorTitle = s"Gwall: $expectedTitle"
    val expectedH1 = "A ydych yn siŵr eich bod am dynnu Monzo?"
    val expectedCaption = s"Llog ar gyfer 6 Ebrill $taxYearMinusOne i 5 Ebrill $taxYear"
    val thisWillTextUntaxed = "Bydd hyn yn dileu holl log y DU sydd heb ei drethu."
    val thisWillTextTaxed = "Bydd hyn yn dileu holl log y DU a drethwyd."
    val yesText = "Iawn"
    val noText = "Na"
    val continueText = "Yn eich blaen"
    val expectedErrorText = "Dewiswch ‘Iawn’ i dynnu’r cyfrif hwn"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults
  object ExpectedAgentEN extends SpecificExpectedResults
  object ExpectedIndividualCY extends SpecificExpectedResults
  object ExpectedAgentCY extends SpecificExpectedResults

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(
      UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
    )
  }

  val untaxedInterestAccount = InterestAccountModel(Some("UntaxedId"), "Monzo", Some(9001.00))
  val untaxedInterestAccount2 = InterestAccountModel(Some("UntaxedId2"), "Starling", Some(9001.00))

  val taxedInterestAccount = InterestAccountModel(Some("TaxedId"), "Monzo", None, Some(9001.00))
  val taxedInterestAccount2 = InterestAccountModel(Some("TaxedId2"), "Starling", None, Some(9001.00))

  ".show" when {

    val untaxedInterestAccount = InterestAccountModel(Some("UntaxedId"), "Monzo", Some(9001.00))
    val untaxedInterestAccount2 = InterestAccountModel(Some("UntaxedId2"), "Starling", Some(9001.00))

    val taxedInterestAccount = InterestAccountModel(Some("TaxedId"), "Monzo", None, Some(9001.00))
    val taxedInterestAccount2 = InterestAccountModel(Some("TaxedId2"), "Starling", None, Some(9001.00))

    userScenarios.foreach { us =>

      import us.commonExpectedResults._

      s"user is ${agentTest(us.isAgent)} and request is ${welshTest(us.isWelsh)} - UNTAXED" should {

        s"return 200 and correctly render for an UNTAXED account" when {
          s"the user is ${agentTest(us.isAgent)} and request is ${welshTest(us.isWelsh)}" when {
            "There are no form errors " when {
              "The account is not the last account" which {
                lazy val result: WSResponse = {
                  dropInterestDB()
                  emptyUserDataStub()
                  insertCyaData(Some(InterestCYAModel(
                    Some(true), Some(false),Some(Seq(untaxedInterestAccount, untaxedInterestAccount2)),
                  )))
                  authoriseAgentOrIndividual(us.isAgent)
                  urlGet(s"$appUrl/$taxYear/interest/remove-untaxed-interest-account?accountId=UntaxedId", us.isWelsh, follow = false, playSessionCookie(us.isAgent))
                }

                s"has an OK($OK) status" in {
                  result.status shouldBe OK
                }

                implicit def document: () => Document = () => Jsoup.parse(result.body)

                titleCheck(expectedTitle, us.isWelsh)
                welshToggleCheck(us.isWelsh)
                textOnPageCheck(expectedCaption, captionSelector)
                h1Check(expectedH1 + " " + expectedCaption)
                radioButtonCheck(yesText, 1)
                radioButtonCheck(noText, 2)
                buttonCheck(continueText, continueButtonSelector)
                formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, UNTAXED, untaxedInterestAccount.id.get).url, continueButtonFormSelector)
              }

              "The account is not the last account when they have prior data for untaxed" which {
                lazy val result: WSResponse = {
                  dropInterestDB()
                  userDataStub(IncomeSourcesModel(None, Some(Seq(InterestModel("firstAccountName", "Id", Some(123), Some(123)))), None), nino, taxYear)
                  insertCyaData(Some(InterestCYAModel(
                    Some(true), Some(true),Some(Seq(untaxedInterestAccount, taxedInterestAccount)),
                  )))
                  authoriseAgentOrIndividual(us.isAgent)
                  urlGet(s"$appUrl/$taxYear/interest/remove-untaxed-interest-account?accountId=UntaxedId", us.isWelsh, follow = false, playSessionCookie(us.isAgent))
                }

                s"has an OK($OK) status" in {
                  result.status shouldBe OK
                }

                implicit def document: () => Document = () => Jsoup.parse(result.body)

                titleCheck(expectedTitle, us.isWelsh)
                welshToggleCheck(us.isWelsh)
                textOnPageCheck(expectedCaption, captionSelector)
                h1Check(expectedH1 + " " + expectedCaption)
                radioButtonCheck(yesText, 1)
                radioButtonCheck(noText, 2)
                buttonCheck(continueText, continueButtonSelector)
                formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, UNTAXED, untaxedInterestAccount.id.get).url, continueButtonFormSelector)
              }

              "The account is not the last account when they have prior data for taxed" which {
                lazy val result: WSResponse = {
                  dropInterestDB()
                  userDataStub(IncomeSourcesModel(None, Some(Seq(InterestModel("firstAccountName", "Id", Some(123), Some(123)))), None), nino, taxYear)
                  insertCyaData(Some(InterestCYAModel(
                    Some(true), Some(true),Some(Seq(untaxedInterestAccount, taxedInterestAccount)),
                  )))
                  authoriseAgentOrIndividual(us.isAgent)
                  urlGet(s"$appUrl/$taxYear/interest/remove-taxed-interest-account?accountId=TaxedId", us.isWelsh, follow = false, playSessionCookie(us.isAgent))
                }

                s"has an OK($OK) status" in {
                  result.status shouldBe OK
                }

                implicit def document: () => Document = () => Jsoup.parse(result.body)

                titleCheck(expectedTitle, us.isWelsh)
                welshToggleCheck(us.isWelsh)
                textOnPageCheck(expectedCaption, captionSelector)
                h1Check(expectedH1 + " " + expectedCaption)
                radioButtonCheck(yesText, 1)
                radioButtonCheck(noText, 2)
                buttonCheck(continueText, continueButtonSelector)
                formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, TAXED, taxedInterestAccount.id.get).url, continueButtonFormSelector)
              }

              "The last account is being removed" which {
                lazy val result: WSResponse = {
                  dropInterestDB()
                  emptyUserDataStub()
                  insertCyaData(Some(InterestCYAModel(
                    Some(true), Some(false),Some(Seq(untaxedInterestAccount))
                  )))
                  authoriseAgentOrIndividual(us.isAgent)
                  urlGet(s"$appUrl/$taxYear/interest/remove-untaxed-interest-account?accountId=UntaxedId", us.isWelsh, follow = false, playSessionCookie(us.isAgent))
                }

                s"has an OK($OK) status" in {
                  result.status shouldBe OK
                }

                implicit def document: () => Document = () => Jsoup.parse(result.body)

                titleCheck(expectedTitle, us.isWelsh)
                welshToggleCheck(us.isWelsh)
                textOnPageCheck(expectedCaption, captionSelector)
                h1Check(expectedH1 + " " + expectedCaption)
                textOnPageCheck(thisWillTextUntaxed, thisWillTextSelector)
                radioButtonCheck(yesText, 1)
                radioButtonCheck(noText, 2)
                buttonCheck(continueText, continueButtonSelector)
                formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, UNTAXED, untaxedInterestAccount.id.get).url, continueButtonFormSelector)
              }

            }
          }
        }

        "return 303 with valid redirect url" when {
          "there is no CYA data in session" which {
            lazy val result = {
              dropInterestDB()
              emptyUserDataStub()
              insertCyaData(None)
              authoriseAgentOrIndividual(us.isAgent)
              urlGet(s"$appUrl/$taxYear/interest/remove-untaxed-interest-account?accountId=UntaxedId", us.isWelsh, follow = false, playSessionCookie(us.isAgent))
            }

            s"has a SEE_OTHER($SEE_OTHER) status" in {
              result.status shouldBe SEE_OTHER
            }

            "redirects to the correct URL" in {
              result.headers("Location").head shouldBe "http://localhost:11111/update-and-submit-income-tax-return/2022/view"
            }
          }

          "there is no valid CYA account data in session for untaxed" which {
            lazy val result = {
              dropInterestDB()
              emptyUserDataStub()
              insertCyaData(Some(InterestCYAModel(
                Some(true), Some(false),Some(Seq(taxedInterestAccount))
              )))
              authoriseAgentOrIndividual(us.isAgent)
              urlGet(s"$appUrl/$taxYear/interest/remove-untaxed-interest-account?accountId=UntaxedId", us.isWelsh, follow = false, playSessionCookie(us.isAgent))
            }

            s"has a SEE_OTHER($SEE_OTHER) status" in {
              result.status shouldBe SEE_OTHER
            }

            "redirects to the correct URL" in {
              result.headers("Location").head shouldBe "/update-and-submit-income-tax-return/personal-income/2022/interest/untaxed-uk-interest"
            }
          }

          "there is CYA account data in session for untaxed but not for the id" which {
            lazy val result = {
              dropInterestDB()
              emptyUserDataStub()
              insertCyaData(Some(InterestCYAModel(
                Some(true), Some(false),Some(Seq(untaxedInterestAccount))
              )))
              authoriseAgentOrIndividual(us.isAgent)
              urlGet(s"$appUrl/$taxYear/interest/remove-untaxed-interest-account?accountId=UntaxedId2", us.isWelsh, follow = false, playSessionCookie(us.isAgent))
            }

            s"has a SEE_OTHER($SEE_OTHER) status" in {
              result.status shouldBe SEE_OTHER
            }

            "redirects to the correct URL" in {
              result.headers("Location").head shouldBe "/update-and-submit-income-tax-return/personal-income/2022/interest/untaxed-uk-interest"
            }
          }
          "there is no valid CYA account data in session for taxed" which {
            lazy val result = {
              dropInterestDB()
              emptyUserDataStub()
              insertCyaData(Some(InterestCYAModel(
                Some(false), Some(true),Some(Seq(untaxedInterestAccount))
              )))
              authoriseAgentOrIndividual(us.isAgent)
              urlGet(s"$appUrl/$taxYear/interest/remove-taxed-interest-account?accountId=UntaxedId", us.isWelsh, follow = false, playSessionCookie(us.isAgent))
            }

            s"has a SEE_OTHER($SEE_OTHER) status" in {
              result.status shouldBe SEE_OTHER
            }

            "redirects to the correct URL" in {
              result.headers("Location").head shouldBe "/update-and-submit-income-tax-return/personal-income/2022/interest/taxed-uk-interest"
            }
          }

          "there is prior untaxed data for id" which {
            lazy val result = {
              dropInterestDB()
              userDataStub(IncomeSourcesModel(None, Some(Seq(InterestModel("firstAccountName", "UntaxedId", Some(123), Some(123)))), None), nino, taxYear)
              insertCyaData(None)
              authoriseAgentOrIndividual(us.isAgent)
              urlGet(s"$appUrl/$taxYear/interest/remove-untaxed-interest-account?accountId=UntaxedId", us.isWelsh, follow = false, playSessionCookie(us.isAgent))
            }

            s"has a SEE_OTHER($SEE_OTHER) status" in {
              result.status shouldBe SEE_OTHER
            }

            "redirects to the correct URL" in {
              result.headers("Location").head shouldBe "/update-and-submit-income-tax-return/personal-income/2022/interest/accounts-with-untaxed-uk-interest"
            }
          }

          "there is prior taxed data for id" which {
            lazy val result = {
              dropInterestDB()
              userDataStub(IncomeSourcesModel(None, Some(Seq(InterestModel("firstAccountName", "TaxedId", Some(123), Some(123)))), None), nino, taxYear)
              insertCyaData(None)
              authoriseAgentOrIndividual(us.isAgent)
              urlGet(s"$appUrl/$taxYear/interest/remove-taxed-interest-account?accountId=TaxedId", us.isWelsh, follow = false, playSessionCookie(us.isAgent))
            }

            s"has a SEE_OTHER($SEE_OTHER) status" in {
              result.status shouldBe SEE_OTHER
            }

            "redirects to the correct URL" in {
              result.headers("Location").head shouldBe "/update-and-submit-income-tax-return/personal-income/2022/interest/accounts-with-taxed-uk-interest"
            }
          }
        }

        "return 401" when {
          "the authorization fails" which {
            lazy val result = {
              unauthorisedAgentOrIndividual(us.isAgent)
              urlGet(s"$appUrl/$taxYear/interest/remove-untaxed-interest-account?accountId=UntaxedId", us.isWelsh, follow = true, playSessionCookie(us.isAgent))
            }

            s"has an Unauthorised($UNAUTHORIZED) status" in {
              result.status shouldBe UNAUTHORIZED
            }
          }
        }
      }

      s"user is ${agentTest(us.isAgent)} and request is ${welshTest(us.isWelsh)} - TAXED" should {

        s"return 200 and correctly render for an TAXED account" when {
          s"the user is ${agentTest(us.isAgent)} and request is ${welshTest(us.isWelsh)}" when {
            "There are no form errors " when {
              "The account is not the last account" which {
                lazy val result: WSResponse = {
                  dropInterestDB()
                  emptyUserDataStub()
                  insertCyaData(Some(InterestCYAModel(
                    Some(false), Some(true), Some(Seq(taxedInterestAccount, taxedInterestAccount2)),
                  )))
                  authoriseAgentOrIndividual(us.isAgent)
                  urlGet(s"$appUrl/$taxYear/interest/remove-taxed-interest-account?accountId=TaxedId", us.isWelsh, follow = false, playSessionCookie(us.isAgent))
                }

                s"has an OK($OK) status" in {
                  result.status shouldBe OK
                }

                implicit def document: () => Document = () => Jsoup.parse(result.body)

                titleCheck(expectedTitle, us.isWelsh)
                welshToggleCheck(us.isWelsh)
                textOnPageCheck(expectedCaption, captionSelector)
                h1Check(expectedH1 + " " + expectedCaption)
                radioButtonCheck(yesText, 1)
                radioButtonCheck(noText, 2)
                buttonCheck(continueText, continueButtonSelector)
                formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, TAXED, taxedInterestAccount.id.get).url, continueButtonFormSelector)
              }

              "The last account is being removed" which {
                lazy val result: WSResponse = {
                  dropInterestDB()
                  emptyUserDataStub()
                  insertCyaData(Some(InterestCYAModel(
                    Some(false), Some(true), Some(Seq(taxedInterestAccount)),
                  )))
                  authoriseAgentOrIndividual(us.isAgent)
                  urlGet(s"$appUrl/$taxYear/interest/remove-taxed-interest-account?accountId=TaxedId", us.isWelsh, follow = false, playSessionCookie(us.isAgent))
                }

                s"has an OK($OK) status" in {
                  result.status shouldBe OK
                }

                implicit def document: () => Document = () => Jsoup.parse(result.body)

                titleCheck(expectedTitle, us.isWelsh)
                welshToggleCheck(us.isWelsh)
                textOnPageCheck(expectedCaption, captionSelector)
                h1Check(expectedH1 + " " + expectedCaption)
                textOnPageCheck(thisWillTextTaxed, thisWillTextSelector)
                radioButtonCheck(yesText, 1)
                radioButtonCheck(noText, 2)
                buttonCheck(continueText, continueButtonSelector)
                formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, TAXED, taxedInterestAccount.id.get).url, continueButtonFormSelector)
              }

            }
          }

        }

        "return 303 with valid redirect url" when {
          "there is no CYA data in session" which {
            lazy val result = {
              dropInterestDB()
              emptyUserDataStub()
              insertCyaData(None)
              authoriseAgentOrIndividual(us.isAgent)
              urlGet(s"$appUrl/$taxYear/interest/remove-taxed-interest-account?accountId=UntaxedId", us.isWelsh, follow = false, playSessionCookie(us.isAgent))
            }

            s"has a SEE_OTHER($SEE_OTHER) status" in {
              result.status shouldBe SEE_OTHER
            }

            "redirects to the correct URL" in {
              result.headers("Location").head shouldBe "http://localhost:11111/update-and-submit-income-tax-return/2022/view"
            }
          }
        }

        "return 401" when {
          "the authorization fails" which {
            lazy val result = {
              unauthorisedAgentOrIndividual(us.isAgent)
              urlGet(s"$appUrl/$taxYear/interest/remove-taxed-interest-account?accountId=UntaxedId", us.isWelsh, follow = true, playSessionCookie(us.isAgent))
            }

            s"has an Unauthorised($UNAUTHORIZED) status" in {
              result.status shouldBe UNAUTHORIZED
            }
          }
        }
      }
    }
  }

  ".submit" when {

    val yesNoFormYes: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
    val yesNoFormNo: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
    val yesNoFormEmpty: Map[String, String] = Map(YesNoForm.yesNo -> "")

    userScenarios.foreach { us =>

      import us.commonExpectedResults._

      s"user is ${agentTest(us.isAgent)} and request is ${welshTest(us.isWelsh)} - UNTAXED" should {

        "remove the account" when {
          "there is CYA data in session and they have selected yes for untaxed" in {
            lazy val result: WSResponse = {
              dropInterestDB()
              emptyUserDataStub()
              insertCyaData(Some(InterestCYAModel(
                Some(true), Some(true), Some(Seq(untaxedInterestAccount,taxedInterestAccount)),
              )))
              authoriseAgentOrIndividual(us.isAgent)

              urlPost(
                s"$appUrl/$taxYear/interest/remove-untaxed-interest-account?accountId=UntaxedId",
                yesNoFormYes,
                us.isWelsh,
                follow = false,
                playSessionCookie(us.isAgent)
              )
            }

            result.status shouldBe SEE_OTHER
            result.header("Location") shouldBe Some("/update-and-submit-income-tax-return/personal-income/2022/interest/check-interest")
          }
          "there is CYA data in session and they have selected yes for taxed" in {
            lazy val result: WSResponse = {
              dropInterestDB()
              emptyUserDataStub()
              insertCyaData(Some(InterestCYAModel(
                Some(true), Some(true), Some(Seq(untaxedInterestAccount,taxedInterestAccount)),
              )))
              authoriseAgentOrIndividual(us.isAgent)

              urlPost(
                s"$appUrl/$taxYear/interest/remove-taxed-interest-account?accountId=TaxedId",
                yesNoFormYes,
                us.isWelsh,
                follow = false,
                playSessionCookie(us.isAgent)
              )
            }

            result.status shouldBe SEE_OTHER
            result.header("Location") shouldBe Some("/update-and-submit-income-tax-return/personal-income/2022/interest/check-interest")
          }
          "there is CYA data in session and they have selected yes for taxed when account has both amounts" in {
            lazy val result: WSResponse = {
              dropInterestDB()
              emptyUserDataStub()
              insertCyaData(Some(InterestCYAModel(
                Some(true), Some(true), Some(Seq(untaxedInterestAccount.copy(taxedAmount = Some(55)),taxedInterestAccount.copy(untaxedAmount = Some(55)))),
              )))
              authoriseAgentOrIndividual(us.isAgent)

              urlPost(
                s"$appUrl/$taxYear/interest/remove-taxed-interest-account?accountId=TaxedId",
                yesNoFormYes,
                us.isWelsh,
                follow = false,
                playSessionCookie(us.isAgent)
              )
            }

            result.status shouldBe SEE_OTHER
            result.header("Location") shouldBe Some("/update-and-submit-income-tax-return/personal-income/2022/interest/accounts-with-taxed-uk-interest")
          }
          "there is CYA data in session and they have selected yes for untaxed when account has both amounts" in {
            lazy val result: WSResponse = {
              dropInterestDB()
              emptyUserDataStub()
              insertCyaData(Some(InterestCYAModel(
                Some(true), Some(true), Some(Seq(untaxedInterestAccount.copy(taxedAmount = Some(55)),taxedInterestAccount.copy(untaxedAmount = Some(55)))),
              )))
              authoriseAgentOrIndividual(us.isAgent)

              urlPost(
                s"$appUrl/$taxYear/interest/remove-untaxed-interest-account?accountId=UntaxedId",
                yesNoFormYes,
                us.isWelsh,
                follow = false,
                playSessionCookie(us.isAgent)
              )
            }

            result.status shouldBe SEE_OTHER
            result.header("Location") shouldBe Some("/update-and-submit-income-tax-return/personal-income/2022/interest/accounts-with-untaxed-uk-interest")
          }
        }

        "redirect" when {
          "there is CYA data in session and they have selected no" in {
            lazy val result: WSResponse = {
              dropInterestDB()
              emptyUserDataStub()
              insertCyaData(Some(InterestCYAModel(
                Some(true), Some(true), Some(Seq(untaxedInterestAccount,taxedInterestAccount)),
              )))
              authoriseAgentOrIndividual(us.isAgent)

              urlPost(
                s"$appUrl/$taxYear/interest/remove-untaxed-interest-account?accountId=UntaxedId",
                yesNoFormNo,
                us.isWelsh,
                follow = false,
                playSessionCookie(us.isAgent)
              )
            }

            result.status shouldBe SEE_OTHER
            result.header("Location") shouldBe Some("/update-and-submit-income-tax-return/personal-income/2022/interest/accounts-with-untaxed-uk-interest")
          }
          "there is no valid CYA data in session and they have selected yes" in {
            lazy val result: WSResponse = {
              dropInterestDB()
              emptyUserDataStub()
              insertCyaData(Some(InterestCYAModel(
                Some(true), Some(true), Some(Seq(taxedInterestAccount)),
              )))
              authoriseAgentOrIndividual(us.isAgent)

              urlPost(
                s"$appUrl/$taxYear/interest/remove-untaxed-interest-account?accountId=UntaxedId",
                yesNoFormYes,
                us.isWelsh,
                follow = false,
                playSessionCookie(us.isAgent)
              )
            }

            result.status shouldBe SEE_OTHER
            result.header("Location") shouldBe Some("/update-and-submit-income-tax-return/personal-income/2022/interest/untaxed-uk-interest")
          }
        }

        "return SEE_OTHER which redirects to overview page" when {
          "there is no CYA data in session" in {
            lazy val result: WSResponse = {
              dropInterestDB()
              emptyUserDataStub()
              insertCyaData(None)
              authoriseAgentOrIndividual(us.isAgent)

              urlPost(
                s"$appUrl/$taxYear/interest/remove-untaxed-interest-account?accountId=UntaxedId",
                yesNoFormYes,
                us.isWelsh,
                follow = false,
                playSessionCookie(us.isAgent)
              )
            }

            result.status shouldBe SEE_OTHER
            result.header("Location") shouldBe Some("http://localhost:11111/update-and-submit-income-tax-return/2022/view")
          }
        }

        "return a redirect" when {
          "there is prior data for id" which {
            lazy val result = {
              dropInterestDB()
              userDataStub(IncomeSourcesModel(None, Some(Seq(InterestModel("firstAccountName", "UntaxedId", Some(123), Some(123)))), None), nino, taxYear)
              insertCyaData(None)
              authoriseAgentOrIndividual(us.isAgent)
              urlPost(
                s"$appUrl/$taxYear/interest/remove-untaxed-interest-account?accountId=UntaxedId",
                yesNoFormYes,
                us.isWelsh,
                follow = false,
                playSessionCookie(us.isAgent)
              )
            }

            s"has a SEE_OTHER($SEE_OTHER) status" in {
              result.status shouldBe SEE_OTHER
            }

            "redirects to the correct URL" in {
              result.headers("Location").head shouldBe "/update-and-submit-income-tax-return/personal-income/2022/interest/accounts-with-untaxed-uk-interest"
            }
          }
        }

        "return BAD_REQUEST" when {
          "There are form errors when no value is passed to the form " when {
            lazy val result = {
              dropInterestDB()
              emptyUserDataStub()
              insertCyaData(Some(InterestCYAModel(
                Some(true),
                Some(true), Some(Seq(taxedInterestAccount, untaxedInterestAccount))
              )))
              authoriseAgentOrIndividual(us.isAgent)

              urlPost(
                s"$appUrl/$taxYear/interest/remove-untaxed-interest-account?accountId=UntaxedId",
                yesNoFormEmpty,
                us.isWelsh,
                follow = false,
                playSessionCookie(us.isAgent)
              )
            }

            s"has an BAD_REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(expectedErrorTitle, us.isWelsh)
            welshToggleCheck(us.isWelsh)
            errorSummaryCheck(expectedErrorText, errorSummaryHref, us.isWelsh)
            textOnPageCheck(expectedCaption, captionSelector)
            h1Check(expectedH1 + " " + expectedCaption)
            errorAboveElementCheck(expectedErrorText)
            radioButtonCheck(yesText, 1)
            radioButtonCheck(noText, 2)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, UNTAXED, untaxedInterestAccount.id.get).url, continueButtonFormSelector)
          }
        }

        "return UNAUTHORIZED" when {
          "auth call fails" which {
            lazy val result: WSResponse = {
              dropInterestDB()
              emptyUserDataStub()

              unauthorisedAgentOrIndividual(us.isAgent)

              urlPost(
                s"$appUrl/$taxYear/interest/remove-untaxed-interest-account?accountId=TaxedId",
                yesNoFormYes,
                us.isWelsh,
                follow = true,
                playSessionCookie(us.isAgent)
              )
            }

            "has an UNAUTHORIZED(401) status" in {
              result.status shouldBe UNAUTHORIZED
            }
          }
        }

      }

      s"user is ${agentTest(us.isAgent)} and request is ${welshTest(us.isWelsh)} - TAXED" should {

        "return SEE_OTHER which redirects to overview page" when {
          "there is no CYA data in session" in {
            lazy val result: WSResponse = {
              dropInterestDB()
              emptyUserDataStub()
              insertCyaData(None)
              authoriseAgentOrIndividual(us.isAgent)

              urlPost(
                s"$appUrl/$taxYear/interest/remove-taxed-interest-account?accountId=TaxedId",
                yesNoFormYes,
                us.isWelsh,
                follow = false,
                playSessionCookie(us.isAgent)
              )
            }

            result.status shouldBe SEE_OTHER
            result.header("Location") shouldBe Some("http://localhost:11111/update-and-submit-income-tax-return/2022/view")
          }
        }

        "return BAD_REQUEST" when {
          "There are form errors when no value is passed to the form " when {
            lazy val result = {
              dropInterestDB()
              emptyUserDataStub()
              insertCyaData(Some(InterestCYAModel(
                Some(true),
                Some(true), Some(Seq(untaxedInterestAccount, taxedInterestAccount))
              )))
              authoriseAgentOrIndividual(us.isAgent)

              urlPost(
                s"$appUrl/$taxYear/interest/remove-taxed-interest-account?accountId=TaxedId",
                yesNoFormEmpty,
                us.isWelsh,
                follow = false,
                playSessionCookie(us.isAgent)
              )
            }

            s"has an BAD_REQUEST($BAD_REQUEST) status" in {
              result.status shouldBe BAD_REQUEST
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(expectedErrorTitle, us.isWelsh)
            welshToggleCheck(us.isWelsh)
            errorSummaryCheck(expectedErrorText, errorSummaryHref, us.isWelsh)
            textOnPageCheck(expectedCaption, captionSelector)
            h1Check(expectedH1 + " " + expectedCaption)
            errorAboveElementCheck(expectedErrorText)
            radioButtonCheck(yesText, 1)
            radioButtonCheck(noText, 2)
            buttonCheck(continueText, continueButtonSelector)
            formPostLinkCheck(controllers.interest.routes.RemoveAccountController.submit(taxYear, TAXED, taxedInterestAccount.id.get).url, continueButtonFormSelector)
          }
        }

        "return UNAUTHORIZED" when {
          "auth call fails" which {
            lazy val result: WSResponse = {
              dropInterestDB()
              emptyUserDataStub()

              unauthorisedAgentOrIndividual(us.isAgent)

              urlPost(
                s"$appUrl/$taxYear/interest/remove-taxed-interest-account?accountId=TaxedId",
                yesNoFormYes,
                us.isWelsh,
                follow = true,
                playSessionCookie(us.isAgent)
              )
            }

            "has an UNAUTHORIZED(401) status" in {
              result.status shouldBe UNAUTHORIZED
            }
          }
        }
      }
    }
  }
}