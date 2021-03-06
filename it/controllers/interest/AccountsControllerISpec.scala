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

import common.InterestTaxTypes.{TAXED, UNTAXED}
import forms.YesNoForm
import models.interest.{InterestAccountModel, InterestCYAModel}
import models.mongo.InterestUserDataModel
import models.priorDataModels.{IncomeSourcesModel, InterestModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, InterestDatabaseHelper, ViewHelpers}

class AccountsControllerISpec extends IntegrationTest with InterestDatabaseHelper with ViewHelpers {

  val yesNoFormNo: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
  val yesNoFormYes: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
  val yesNoFormEmpty: Map[String, String] = Map(YesNoForm.yesNo -> "")

  val taxYear: Int = 2022
  val taxYearMinusOne: Int = taxYear - 1
  val amount: BigDecimal = 25

  val untaxedUrl = s"$appUrl/$taxYear/interest/accounts-with-untaxed-uk-interest"
  val taxedUrl = s"$appUrl/$taxYear/interest/accounts-with-taxed-uk-interest"

  object Selectors {
    val accountRow: Int => String = rowNumber => s".govuk-form-group > ul > li:nth-child($rowNumber)"
    val accountRowName: Int => String = rowNumber => accountRow(rowNumber) + " > span:nth-child(1)"
    val accountRowChange: Int => String = rowNumber => accountRow(rowNumber) + " > span:nth-child(2) > a"
    val accountRowChangeHidden: Int => String = rowNumber => accountRow(rowNumber) + " > span:nth-child(2) > a > span.govuk-visually-hidden"
    val accountRowRemove: Int => String = rowNumber => accountRow(rowNumber) + " > span:nth-child(3) > a"
    val accountRowRemoveHidden: Int => String = rowNumber => accountRow(rowNumber) + " > span:nth-child(3) > a > span.govuk-visually-hidden"

    val accountRowChangePriorSubmission: Int => String = rowNumber => accountRow(rowNumber) + " > span:nth-child(3) > a"
    val accountRowChangePriorSubmissionHidden: Int => String = rowNumber => accountRow(rowNumber) + " > span:nth-child(3) > a > span.govuk-visually-hidden"

    val captionSelector = ".govuk-caption-l"
    val continueSelector = "#continue"
    val continueFormSelector = "#main-content > div > div > form"
    val doYouNeedSelector = "#main-content > div > div > form > div > fieldset > legend"
    val youMustTellSelector = "#value-hint"
  }

  import Selectors._

  trait SpecificExpectedResults {
    val youMustTellTextUntaxed: String
    val youMustTellTextTaxed: String
  }

  trait CommonExpectedResults {
    val changeUntaxedHref: String
    val changePriorUntaxedHref: String
    val changeTaxedHref: String
    val changePriorTaxedHref: String
    val removeUntaxedHref: String
    val removeTaxedHref: String
    val untaxedH1: String
    val taxedH1: String
    val untaxedTitle: String
    val taxedTitle: String
    val captionText: String
    val changeText: String
    val removeText: String
    val removeAccountHiddenText: String => String
    val changeAccountHiddenText: String => String
    val addAnotherAccountText: String
    val continueText: String
    val errorTitleText: String => String
    val doYouNeedText: String
    val yesText: String
    val noText: String
    val expectedErrorText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val changeUntaxedHref = s"/income-through-software/return/personal-income/$taxYear/interest/add-untaxed-uk-interest-account/qwerty"
    val changePriorUntaxedHref = s"/income-through-software/return/personal-income/$taxYear/interest/change-untaxed-uk-interest?accountId=azerty"
    val changeTaxedHref = s"/income-through-software/return/personal-income/$taxYear/interest/add-taxed-uk-interest-account/qwerty"
    val changePriorTaxedHref = s"/income-through-software/return/personal-income/$taxYear/interest/change-taxed-uk-interest?accountId=azerty"
    val removeUntaxedHref = s"/income-through-software/return/personal-income/$taxYear/interest/remove-untaxed-interest-account?accountId=qwerty"
    val removeTaxedHref = s"/income-through-software/return/personal-income/$taxYear/interest/remove-taxed-interest-account?accountId=qwerty"
    val untaxedH1 = "Accounts with untaxed UK interest"
    val taxedH1 = "Accounts with taxed UK interest"
    val untaxedTitle = "Accounts with untaxed UK interest"
    val taxedTitle = "Accounts with taxed UK interest"
    val captionText = s"Interest for 6 April $taxYearMinusOne to 5 April $taxYear"
    val changeText = "Change"
    val removeText = "Remove"
    val addAnotherAccountText = "Add another account"
    val continueText = "Continue"
    val errorTitleText: String => String = (titleText: String) => s"Error: $titleText"
    val doYouNeedText = "Do you need to add another account?"
    val yesText = "Yes"
    val noText = "No"
    val removeAccountHiddenText = (account: String) => s"$removeText $account account"
    val changeAccountHiddenText = (account: String) => s"$changeText $account account details"
    val expectedErrorText = "Select yes to add another account"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val changeUntaxedHref = s"/income-through-software/return/personal-income/$taxYear/interest/add-untaxed-uk-interest-account/qwerty"
    val changePriorUntaxedHref = s"/income-through-software/return/personal-income/$taxYear/interest/change-untaxed-uk-interest?accountId=azerty"
    val changeTaxedHref = s"/income-through-software/return/personal-income/$taxYear/interest/add-taxed-uk-interest-account/qwerty"
    val changePriorTaxedHref = s"/income-through-software/return/personal-income/$taxYear/interest/change-taxed-uk-interest?accountId=azerty"
    val removeUntaxedHref = s"/income-through-software/return/personal-income/$taxYear/interest/remove-untaxed-interest-account?accountId=qwerty"
    val removeTaxedHref = s"/income-through-software/return/personal-income/$taxYear/interest/remove-taxed-interest-account?accountId=qwerty"
    val untaxedH1 = "Accounts with untaxed UK interest"
    val taxedH1 = "Accounts with taxed UK interest"
    val untaxedTitle = "Accounts with untaxed UK interest"
    val taxedTitle = "Accounts with taxed UK interest"
    val captionText = s"Interest for 6 April $taxYearMinusOne to 5 April $taxYear"
    val changeText = "Change"
    val removeText = "Remove"
    val addAnotherAccountText = "Add another account"
    val continueText = "Continue"
    val errorTitleText: String => String = (titleText: String) => s"Error: $titleText"
    val doYouNeedText = "Do you need to add another account?"
    val yesText = "Yes"
    val noText = "No"
    val removeAccountHiddenText = (account: String) => s"$removeText $account account"
    val changeAccountHiddenText = (account: String) => s"$changeText $account account details"
    val expectedErrorText = "Select yes to add another account"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val youMustTellTextUntaxed = "You must tell us about all your accounts with untaxed UK interest."
    val youMustTellTextTaxed = "You must tell us about all your accounts with taxed UK interest."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val youMustTellTextUntaxed = "You must tell us about all your client’s accounts with untaxed UK interest."
    val youMustTellTextTaxed = "You must tell us about all your client’s accounts with taxed UK interest."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val youMustTellTextUntaxed = "You must tell us about all your accounts with untaxed UK interest."
    val youMustTellTextTaxed = "You must tell us about all your accounts with taxed UK interest."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val youMustTellTextUntaxed = "You must tell us about all your client’s accounts with untaxed UK interest."
    val youMustTellTextTaxed = "You must tell us about all your client’s accounts with taxed UK interest."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(
      UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
    )
  }

  userScenarios.foreach { us =>

    import us.commonExpectedResults._

    val specific = us.specificExpectedResults.get

    s"Calling GET - UNTAXED - ${welshTest(us.isWelsh)} - ${agentTest(us.isAgent)}" when {

      s"UNTAXED - there is cya session data " should {

        "render 1 row when there is a single untaxed account passed in that is not a prior submission" which {
          lazy val result: WSResponse = {
            dropInterestDB()
            emptyUserDataStub()
            await(interestDatabase.create(InterestUserDataModel(
              sessionId, mtditid, nino, taxYear,
              Some(InterestCYAModel(
                Some(true), Some(false), Some(Seq(InterestAccountModel(None, "Bank of UK", untaxedAmount = Some(9001.00), None, Some("qwerty"))))
              ))
            )))

            authoriseAgentOrIndividual(us.isAgent)
            urlGet(untaxedUrl, us.isWelsh, follow = true, playSessionCookie(us.isAgent))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(untaxedTitle)
          welshToggleCheck(us.isWelsh)
          textOnPageCheck(captionText, captionSelector)
          h1Check(untaxedH1 + " " + captionText)
          textOnPageCheck("Bank of UK", accountRowName(1))

          "has a link for changing the account" which {
            "has the correct text" in {
              element(accountRowChange(1)).child(0).text shouldBe changeText
            }

            "has the correct hidden text" in {
              element(accountRowChangeHidden(1)).text shouldBe changeAccountHiddenText("Bank of UK")
            }

            "has the correct link" in {
              element(accountRowChange(1)).attr("href") shouldBe changeUntaxedHref
            }
          }

          "has a link for removing the account" which {
            s"has the text $removeText" in {
              element(accountRowRemove(1)).child(0).text() shouldBe removeText
            }
            s"has the correct hidden text" in {
              element(accountRowRemoveHidden(1)).text shouldBe removeAccountHiddenText("Bank of UK")
            }
            s"has the correct link" in {
              element(accountRowRemove(1)).attr("href") shouldBe removeUntaxedHref
            }
          }

          textOnPageCheck(doYouNeedText, doYouNeedSelector)
          textOnPageCheck(specific.youMustTellTextUntaxed, youMustTellSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(controllers.interest.routes.AccountsController.submit(taxYear, UNTAXED).url, continueFormSelector)
        }

        "render 2 rows when there are two accounts passed in, one new account and one prior" which {
          lazy val result: WSResponse = {
            dropInterestDB()
            userDataStub(IncomeSourcesModel(None,Some(Seq(InterestModel("Bank of EU","azerty",None,Some(1234.56))))) ,nino, taxYear)
            await(interestDatabase.create(InterestUserDataModel(
              sessionId, mtditid, nino, taxYear,
              Some(InterestCYAModel(
                Some(true), Some(false), Some(Seq(
                  InterestAccountModel(None, "Bank of UK", Some(9000.01), None, Some("qwerty")),
                  InterestAccountModel(Some("azerty"), "Bank of EU",  Some(1234.56), None)
                ))
              ))
            )))

            authoriseAgentOrIndividual(us.isAgent)
            urlGet(untaxedUrl, us.isWelsh, follow = true, playSessionCookie(us.isAgent))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(untaxedTitle)
          welshToggleCheck(us.isWelsh)
          h1Check(untaxedH1 + " " + captionText)
          textOnPageCheck(captionText, captionSelector)

          "have an area for the first row" which {
            "has a link for changing the account" which {
              "has the correct text" in {
                element(accountRowChange(1)).child(0).text shouldBe changeText
              }
              "has the correct hidden text" in {
                element(accountRowChangeHidden(1)).text shouldBe changeAccountHiddenText("Bank of UK")
              }
              "has the correct link" in {
                element(accountRowChange(1)).attr("href") shouldBe changeUntaxedHref
              }
            }

            "has a link for removing the account" which {
              s"has the text $removeText" in {
                element(accountRowRemove(1)).child(0).text() shouldBe removeText
              }
              s"has the correct hidden text" in {
                element(accountRowRemoveHidden(1)).text shouldBe removeAccountHiddenText("Bank of UK")
              }
              s"has the correct link" in {
                element(accountRowRemove(1)).attr("href") shouldBe removeUntaxedHref
              }
            }
          }

          "have an area for the second row" which {
            "has a link for changing the account" which {
              "has the correct text" in {
                element(accountRowChangePriorSubmission(2)).child(0).text shouldBe changeText
              }
              "has the correct hidden text" in {
                element(accountRowChangePriorSubmissionHidden(2)).text shouldBe changeAccountHiddenText("Bank of EU")
              }
              "has the correct link" in {
                element(accountRowChangePriorSubmission(2)).attr("href") shouldBe changePriorUntaxedHref
              }
            }
          }
          textOnPageCheck(doYouNeedText, doYouNeedSelector)
          textOnPageCheck(specific.youMustTellTextUntaxed, youMustTellSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(controllers.interest.routes.AccountsController.submit(taxYear, UNTAXED).url, continueFormSelector)
        }
      }

      "redirect to the overview page" when {

        s"there is no cya data in session - ${welshTest(us.isWelsh)} - ${agentTest(us.isAgent)}" in {
          dropInterestDB()

          await(interestDatabase.create(InterestUserDataModel(
            sessionId, mtditid, nino, taxYear, None
          )))

          val result: WSResponse = {
            emptyUserDataStub()
            authoriseAgentOrIndividual(us.isAgent)
            urlGet(untaxedUrl, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          result.status shouldBe SEE_OTHER
          result.header("Location") shouldBe Some("http://localhost:11111/income-through-software/return/2022/view")
        }
      }

      "redirect to the untaxed interest page" when {

        "the cya data in session does not contain an untaxed interest account" in {
          dropInterestDB()

          await(interestDatabase.create(InterestUserDataModel(
            sessionId, mtditid, nino, taxYear,
            Some(InterestCYAModel(
              Some(true), Some(false)
            ))
          )))

          val result: WSResponse = {
            emptyUserDataStub()
            authoriseAgentOrIndividual(us.isAgent)
            urlGet(untaxedUrl, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          result.status shouldBe SEE_OTHER
          result.header("Location") shouldBe Some("/income-through-software/return/personal-income/2022/interest/untaxed-uk-interest")
        }
      }
    }

    s"Calling GET - TAXED - ${welshTest(us.isWelsh)} - ${agentTest(us.isAgent)}" when {

      s"TAXED - there is cya session data " should {

        "render 1 row when there is a single taxed account passed in that is not a prior submission" which {
          lazy val result: WSResponse = {
            dropInterestDB()
            await(interestDatabase.create(InterestUserDataModel(
              sessionId, mtditid, nino, taxYear,
              Some(InterestCYAModel(
                Some(false), Some(true), Some(Seq(InterestAccountModel(None, "Bank of UK", None, Some(9001.00), Some("qwerty"))))
              ))
            )))
            emptyUserDataStub()
            authoriseAgentOrIndividual(us.isAgent)
            urlGet(taxedUrl, us.isWelsh, follow = true, playSessionCookie(us.isAgent))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(taxedTitle)
          welshToggleCheck(us.isWelsh)
          textOnPageCheck(captionText, captionSelector)
          h1Check(taxedH1 + " " + captionText)
          textOnPageCheck("Bank of UK", accountRowName(1))

          "has a link for changing the account" which {
            "has the correct text" in {
              element(accountRowChange(1)).child(0).text shouldBe changeText
            }

            "has the correct hidden text" in {
              element(accountRowChangeHidden(1)).text shouldBe changeAccountHiddenText("Bank of UK")
            }

            "has the correct link" in {
              element(accountRowChange(1)).attr("href") shouldBe changeTaxedHref
            }
          }

          "has a link for removing the account" which {
            s"has the text $removeText" in {
              element(accountRowRemove(1)).child(0).text() shouldBe removeText
            }
            s"has the correct hidden text" in {
              element(accountRowRemoveHidden(1)).text shouldBe removeAccountHiddenText("Bank of UK")
            }
            s"has the correct link" in {
              element(accountRowRemove(1)).attr("href") shouldBe removeTaxedHref
            }
          }

          textOnPageCheck(doYouNeedText, doYouNeedSelector)
          textOnPageCheck(specific.youMustTellTextTaxed, youMustTellSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(controllers.interest.routes.AccountsController.submit(taxYear, TAXED).url, continueFormSelector)
        }

        "render 2 rows when there are two accounts passed in, one new account and one prior" which {
          lazy val result: WSResponse = {
            dropInterestDB()
            userDataStub(IncomeSourcesModel(None,Some(Seq(InterestModel("Bank of EU","azerty",Some(1234.56),None)))) ,nino, taxYear)
            await(interestDatabase.create(InterestUserDataModel(
              sessionId, mtditid, nino, taxYear,
              Some(InterestCYAModel(
                Some(false), Some(true), Some(Seq(
                  InterestAccountModel(None, "Bank of UK", None, Some(9000.01), Some("qwerty")),
                  InterestAccountModel(Some("azerty"), "Bank of EU", None, Some(1234.56))
                ))
              ))
            )))

            authoriseAgentOrIndividual(us.isAgent)
            urlGet(taxedUrl, us.isWelsh, follow = true, playSessionCookie(us.isAgent))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(taxedTitle)
          welshToggleCheck(us.isWelsh)
          h1Check(taxedH1 + " " + captionText)
          textOnPageCheck(captionText, captionSelector)

          "have an area for the first row" which {
            "has a link for changing the account" which {
              "has the correct text" in {
                element(accountRowChange(1)).child(0).text shouldBe changeText
              }
              "has the correct hidden text" in {
                element(accountRowChangeHidden(1)).text shouldBe changeAccountHiddenText("Bank of UK")
              }
              "has the correct link" in {
                element(accountRowChange(1)).attr("href") shouldBe changeTaxedHref
              }
            }

            "has a link for removing the account" which {
              s"has the text $removeText" in {
                element(accountRowRemove(1)).child(0).text() shouldBe removeText
              }
              s"has the correct hidden text" in {
                element(accountRowRemoveHidden(1)).text shouldBe removeAccountHiddenText("Bank of UK")
              }
              s"has the correct link" in {
                element(accountRowRemove(1)).attr("href") shouldBe removeTaxedHref
              }
            }
          }

          "have an area for the second row" which {
            "has a link for changing the account" which {
              "has the correct text" in {
                element(accountRowChangePriorSubmission(2)).child(0).text shouldBe changeText
              }
              "has the correct hidden text" in {
                element(accountRowChangePriorSubmissionHidden(2)).text shouldBe changeAccountHiddenText("Bank of EU")
              }
              "has the correct link" in {
                element(accountRowChangePriorSubmission(2)).attr("href") shouldBe changePriorTaxedHref
              }
            }
          }
          textOnPageCheck(doYouNeedText, doYouNeedSelector)
          textOnPageCheck(specific.youMustTellTextTaxed, youMustTellSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(controllers.interest.routes.AccountsController.submit(taxYear, TAXED).url, continueFormSelector)
        }

      }

      "redirect to the overview page" when {

        s"there is no cya data in session - ${welshTest(us.isWelsh)} - ${agentTest(us.isAgent)}" in {
          dropInterestDB()

          await(interestDatabase.create(InterestUserDataModel(
            sessionId, mtditid, nino, taxYear, None
          )))

          val result: WSResponse = {
            emptyUserDataStub()
            authoriseAgentOrIndividual(us.isAgent)
            urlGet(taxedUrl, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          result.status shouldBe SEE_OTHER
          result.header("Location") shouldBe Some("http://localhost:11111/income-through-software/return/2022/view")
        }
      }

      "redirect to the taxed interest page" when {

        "the cya data in session does not contain an taxed interest account" in {
          dropInterestDB()

          await(interestDatabase.create(InterestUserDataModel(
            sessionId, mtditid, nino, taxYear,
            Some(InterestCYAModel(
              Some(false),Some(true)
            ))
          )))

          val result: WSResponse = {
            emptyUserDataStub()
            authoriseAgentOrIndividual(us.isAgent)
            urlGet(taxedUrl, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          result.status shouldBe SEE_OTHER
          result.header("Location") shouldBe Some("/income-through-software/return/personal-income/2022/interest/taxed-uk-interest")
        }
      }

    }

    s"Calling POST - UNTAXED - ${welshTest(us.isWelsh)} - ${agentTest(us.isAgent)}" when {

      "the user has selected 'no' to adding an extra account" should {

        "redirect to the interest cya page when cya data is complete" in {
          dropInterestDB()

          await(interestDatabase.create(InterestUserDataModel(
            sessionId, mtditid, nino, taxYear,
            Some(InterestCYAModel(
              Some(true), Some(false),Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", Some(amount), None))),
            ))
          )))

          val result: WSResponse = {
            emptyUserDataStub()
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(untaxedUrl, yesNoFormNo, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          result.status shouldBe SEE_OTHER
          result.headers("Location").head.contains("/income-through-software/return/personal-income/2022/interest/check-interest") shouldBe true
        }

        "redirect to the  page when accounts are missing" in {
          dropInterestDB()

          await(interestDatabase.create(InterestUserDataModel(
            sessionId, mtditid, nino, taxYear,
            Some(InterestCYAModel(
              Some(true), Some(false),None
            ))
          )))

          val result: WSResponse = {
            emptyUserDataStub()
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(untaxedUrl, yesNoFormNo, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe "/income-through-software/return/personal-income/2022/interest/untaxed-uk-interest"
        }

        "redirect to the overview page" when {

          s"there is no cya data in session - ${welshTest(us.isWelsh)} - ${agentTest(us.isAgent)}" in {
            dropInterestDB()

            await(interestDatabase.create(InterestUserDataModel(
              sessionId, mtditid, nino, taxYear, None
            )))

            val result: WSResponse = {
              emptyUserDataStub()
              authoriseAgentOrIndividual(us.isAgent)
              urlPost(untaxedUrl, yesNoFormNo, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
            }

            result.status shouldBe SEE_OTHER
            result.header("Location") shouldBe Some("http://localhost:11111/income-through-software/return/2022/view")
          }
        }

        "redirect to the taxed interest page when cya taxed data is not yet complete" in {
          dropInterestDB()

          await(interestDatabase.create(InterestUserDataModel(
            sessionId, mtditid, nino, taxYear,
            Some(InterestCYAModel(
              Some(true), None, Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", Some(amount), None)))
            ))
          )))

          emptyUserDataStub()

          val result: WSResponse = {
            emptyUserDataStub()
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(untaxedUrl, yesNoFormNo, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          result.status shouldBe SEE_OTHER
          result.headers("Location").head.contains("/income-through-software/return/personal-income/2022/interest/taxed-uk-interest") shouldBe true
        }
      }

      "the user has selected to add an extra account" should {

        "redirect to the untaxed interest amount page" in {
          val result: WSResponse = {
            dropInterestDB()
            emptyUserDataStub()
            insertCyaData(Some(InterestCYAModel(
              Some(true),  Some(false), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", Some(amount)))),
            )), overrideNino = Some(nino))

            authoriseAgentOrIndividual(us.isAgent)
            urlPost(untaxedUrl, yesNoFormYes, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          result.status shouldBe SEE_OTHER
          result.headers("Location").head should include(
            "/income-through-software/return/personal-income/2022/interest/which-account-did-you-get-untaxed-interest-from")
        }
      }

      "the user has not selected whether or not to add an extra account" should {

        "return a BAD_REQUEST with correct errors displayed" which {
          lazy val result: WSResponse = {
            dropInterestDB()
            emptyUserDataStub()
            await(interestDatabase.create(InterestUserDataModel(
              sessionId, mtditid, nino, taxYear,
              Some(InterestCYAModel(
                Some(true), Some(false), Some(Seq(InterestAccountModel(None, "Untaxed Account", Some(9001.00), None, Some("qwerty"))))
              ))
            )))

            authoriseAgentOrIndividual(us.isAgent)
            urlPost(untaxedUrl, yesNoFormEmpty, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          s"has an BAD_REQUEST($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          welshToggleCheck(us.isWelsh)
          titleCheck(errorTitleText(untaxedTitle))
          h1Check(untaxedH1 + " " + captionText)
          textOnPageCheck(captionText, captionSelector)
          textOnPageCheck("Untaxed Account", accountRowName(1))

          errorSummaryCheck(expectedErrorText, "#value")
          errorAboveElementCheck(expectedErrorText)

          "has a link for changing the account" which {
            "has the correct text" in {
              element(accountRowChange(1)).child(0).text shouldBe changeText
            }
            "has the correct hidden text" in {
              element(accountRowChangeHidden(1)).text shouldBe changeAccountHiddenText("Untaxed Account")
            }
            "has the correct link" in {
              element(accountRowChange(1)).attr("href") shouldBe changeUntaxedHref
            }
          }

          "has a link for removing the account" which {
            s"has the text $removeText" in {
              element(accountRowRemove(1)).child(0).text() shouldBe removeText
            }
            s"has the correct hidden text" in {
              element(accountRowRemoveHidden(1)).text shouldBe removeAccountHiddenText("Untaxed Account")
            }
            s"has the correct" in {
              element(accountRowRemove(1)).attr("href") shouldBe removeUntaxedHref
            }
          }

          textOnPageCheck(doYouNeedText, doYouNeedSelector)
          textOnPageCheck(specific.youMustTellTextUntaxed, youMustTellSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(controllers.interest.routes.AccountsController.submit(taxYear, UNTAXED).url, continueFormSelector)
        }
      }
    }

    s"Calling POST TAXED - ${welshTest(us.isWelsh)} - ${agentTest(us.isAgent)}" when {

      "the user has selected 'no' to adding an extra account" when {
        "cya data is complete" should {

          "redirect to the interest cya page" in {
            val result: WSResponse = {
              dropInterestDB()
              emptyUserDataStub()
              await(interestDatabase.create(InterestUserDataModel(
                sessionId, mtditid, nino, taxYear,
                Some(InterestCYAModel(
                  Some(false), Some(true),
                  Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", None, Some(amount))))
                ))
              )))
              emptyUserDataStub()

              authoriseAgentOrIndividual(us.isAgent)
              urlPost(taxedUrl, yesNoFormNo, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
            }

            result.status shouldBe SEE_OTHER
            result.headers("Location").head.contains("/income-through-software/return/personal-income/2022/interest/check-interest") shouldBe true
          }
        }
      }

      "redirect to the  page when accounts are missing" in {
        dropInterestDB()

        await(interestDatabase.create(InterestUserDataModel(
          sessionId, mtditid, nino, taxYear,
          Some(InterestCYAModel(
            Some(true), Some(false),None
          ))
        )))

        val result: WSResponse = {
          emptyUserDataStub()
          authoriseAgentOrIndividual(us.isAgent)
          urlPost(taxedUrl, yesNoFormNo, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
        }

        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe "/income-through-software/return/personal-income/2022/interest/taxed-uk-interest"
      }

      "redirect to the overview page" when {

        s"there is no cya data in session - ${welshTest(us.isWelsh)} - ${agentTest(us.isAgent)}" in {
          dropInterestDB()

          await(interestDatabase.create(InterestUserDataModel(
            sessionId, mtditid, nino, taxYear, None
          )))

          val result: WSResponse = {
            emptyUserDataStub()
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(taxedUrl, yesNoFormNo, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          result.status shouldBe SEE_OTHER
          result.header("Location") shouldBe Some("http://localhost:11111/income-through-software/return/2022/view")
        }
      }

      "the user has selected to add an extra account" should {
        "redirect to the untaxed interest amount page" in {
          val result: WSResponse = {
            dropInterestDB()
            emptyUserDataStub()
            insertCyaData(Some(InterestCYAModel(
              Some(false),
              Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", None, Some(amount))))
            )), overrideNino = Some(nino))

            authoriseAgentOrIndividual(us.isAgent)
            urlPost(taxedUrl, yesNoFormYes, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          result.status shouldBe SEE_OTHER
          result.headers("Location").head should include(
            "/income-through-software/return/personal-income/2022/interest/which-account-did-you-get-taxed-interest-from")
        }
      }

      "the user has not selected whether or not to add an extra account" should {

        "return a BAD_REQUEST with correct errors displayed" which {
          lazy val result: WSResponse = {
            dropInterestDB()
            emptyUserDataStub()
            await(interestDatabase.create(InterestUserDataModel(
              sessionId, mtditid, nino, taxYear,
              Some(InterestCYAModel(
                Some(false),
                Some(true), Some(Seq(InterestAccountModel(None, "Taxed Account", None, Some(amount), Some("qwerty"))))
              ))
            )))
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(taxedUrl, yesNoFormEmpty, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          s"has an BAD_REQUEST($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          welshToggleCheck(us.isWelsh)
          titleCheck(errorTitleText(taxedTitle))
          h1Check(taxedH1 + " " + captionText)
          textOnPageCheck(captionText, captionSelector)
          textOnPageCheck("Taxed Account", accountRowName(1))

          errorSummaryCheck(expectedErrorText, "#value")
          errorAboveElementCheck(expectedErrorText)

          "has a link for changing the account" which {
            "has the correct text" in {
              element(accountRowChange(1)).child(0).text shouldBe changeText
            }
            "has the correct hidden text" in {
              element(accountRowChangeHidden(1)).text shouldBe changeAccountHiddenText("Taxed Account")
            }
            "has the correct link" in {
              element(accountRowChange(1)).attr("href") shouldBe changeTaxedHref
            }
          }

          "has a link for removing the account" which {
            s"has the text $removeText" in {
              element(accountRowRemove(1)).child(0).text() shouldBe removeText
            }
            s"has the correct hidden text" in {
              element(accountRowRemoveHidden(1)).text shouldBe removeAccountHiddenText("Taxed Account")
            }
            s"has the correct" in {
              element(accountRowRemove(1)).attr("href") shouldBe removeTaxedHref
            }
          }

          textOnPageCheck(doYouNeedText, doYouNeedSelector)
          textOnPageCheck(specific.youMustTellTextTaxed, youMustTellSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(continueText, continueSelector)
          formPostLinkCheck(controllers.interest.routes.AccountsController.submit(taxYear, TAXED).url, continueFormSelector)
        }
      }
    }
  }
}