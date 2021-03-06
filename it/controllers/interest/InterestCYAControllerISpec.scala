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

import com.github.tomakehurst.wiremock.http.HttpHeader
import models.interest.{InterestAccountModel, InterestCYAModel}
import models.priorDataModels.{IncomeSourcesModel, InterestModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import utils.{IntegrationTest, InterestDatabaseHelper, ViewHelpers}

class InterestCYAControllerISpec extends IntegrationTest with InterestDatabaseHelper with ViewHelpers {

  val taxYear: Int = 2022
  val taxYearMinusOne: Int = taxYear - 1
  val amount: BigDecimal = 25

  val question2 = 2
  val question4 = 4

  val account1 = 1
  val account2 = 2

  object Selectors {
    val titleSelector = "title"
    val h1Selector = "h1"
    val captionSelector = ".govuk-caption-l"
    val submitButton = ".govuk-button"
    val submitButtonForm = "#main-content > div > div > form"

    val questionSelector: Int => String = questionNumber => s".govuk-summary-list__row:nth-child($questionNumber) > .govuk-summary-list__key"
    val questionAccountSelector: (Int, Int, Int) => String = (questionNumber, accountNumber,account) =>
      s"#question-$questionNumber-account-$account:nth-child($accountNumber)"

    val questionChangeLinkSelector: Int => String = questionNumber => s"#main-content > div > div > dl > div:nth-child($questionNumber) " +
      s"> dd.govuk-summary-list__actions > a"
    val questionTextSelector: Int => String = question => s"#main-content > div > div > dl > div:nth-child($question) > dt"
    val yesNoQuestionAnswer: Int => String = questionNumber => s"#main-content > div > div > dl > div:nth-child($questionNumber) > dd.govuk-summary-list__value"
  }

  import Selectors._

  // Agent or individual
  trait SpecificExpectedResults {
    val h1Expected: String
    val titleExpected: String

    val changeUntaxedInterestHiddenText: String
    val changeUntaxedDetailsHiddenText: String
    val changeTaxedInterestHiddenText: String
    val changeTaxedDetailsHiddenText: String
  }

  // Generic content
  trait CommonExpectedResults {
    val captionExpected: String
    val changeLinkExpected: String
    val questionUntaxedInterestExpected: String
    val questionUntaxedInterestDetailsExpected: String
    val questionTaxedInterestExpected: String
    val question4TaxedInterestDetailExpected: String
    val untaxedInterestAccount1ExpectedTest: String
    val taxedInterestAccount1ExpectedTest: String
    val taxedInterestAccount2ExpectedTest: String
    val changeUntaxedInterestHref: String
    val changeUntaxedInterestAmountHref: String
    val changeTaxedInterestHref: String
    val changeTaxedInterestAmountHref: String
    val submitText: String
    val submitLink: String
    val Yes: String
    val No: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val captionExpected = s"Interest for 6 April $taxYearMinusOne to 5 April $taxYear"
    val changeLinkExpected = "Change"
    val questionUntaxedInterestExpected = "Untaxed UK Interest"
    val questionUntaxedInterestDetailsExpected = "Untaxed UK interest accounts"
    val questionTaxedInterestExpected = "Taxed UK Interest"
    val question4TaxedInterestDetailExpected = "Taxed UK interest accounts"
    val untaxedInterestAccount1ExpectedTest = "UntaxedBank1 : £100"
    val taxedInterestAccount1ExpectedTest = "TaxedBank1 : £200"
    val taxedInterestAccount2ExpectedTest = "TaxedBank2 : £400"
    val changeUntaxedInterestHref = s"/income-through-software/return/personal-income/$taxYear/interest/untaxed-uk-interest"
    val changeUntaxedInterestAmountHref = s"/income-through-software/return/personal-income/$taxYear/interest/accounts-with-untaxed-uk-interest"
    val changeTaxedInterestHref = s"/income-through-software/return/personal-income/$taxYear/interest/taxed-uk-interest"
    val changeTaxedInterestAmountHref = s"/income-through-software/return/personal-income/$taxYear/interest/accounts-with-taxed-uk-interest"
    val submitText = "Save and continue"
    val submitLink = s"/income-through-software/return/personal-income/$taxYear/interest/check-interest"
    val Yes = "Yes"
    val No = "No"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val captionExpected = s"Interest for 6 April $taxYearMinusOne to 5 April $taxYear"
    val changeLinkExpected = "Change"
    val questionUntaxedInterestExpected = "Untaxed UK Interest"
    val questionUntaxedInterestDetailsExpected = "Untaxed UK interest accounts"
    val questionTaxedInterestExpected = "Taxed UK Interest"
    val question4TaxedInterestDetailExpected = "Taxed UK interest accounts"
    val untaxedInterestAccount1ExpectedTest = "UntaxedBank1 : £100"
    val taxedInterestAccount1ExpectedTest = "TaxedBank1 : £200"
    val taxedInterestAccount2ExpectedTest = "TaxedBank2 : £400"
    val changeUntaxedInterestHref = s"/income-through-software/return/personal-income/$taxYear/interest/untaxed-uk-interest"
    val changeUntaxedInterestAmountHref = s"/income-through-software/return/personal-income/$taxYear/interest/accounts-with-untaxed-uk-interest"
    val changeTaxedInterestHref = s"/income-through-software/return/personal-income/$taxYear/interest/taxed-uk-interest"
    val changeTaxedInterestAmountHref = s"/income-through-software/return/personal-income/$taxYear/interest/accounts-with-taxed-uk-interest"
    val submitText = "Save and continue"
    val submitLink = s"/income-through-software/return/personal-income/$taxYear/interest/check-interest"
    val Yes = "Yes"
    val No = "No"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val h1Expected = "Check your UK interest"
    val titleExpected = "Check your UK interest"

    val changeUntaxedInterestHiddenText = "if you got untaxed UK interest"
    val changeUntaxedDetailsHiddenText = "the details of your account with untaxed UK interest"
    val changeTaxedInterestHiddenText = "if you got taxed UK interest"
    val changeTaxedDetailsHiddenText = "the details of your account with taxed UK interest"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val h1Expected = "Check your client’s UK interest"
    val titleExpected= "Check your client’s UK interest"

    val changeUntaxedInterestHiddenText = "if your client got untaxed UK interest"
    val changeUntaxedDetailsHiddenText = "the details of your client’s account with untaxed UK interest"
    val changeTaxedInterestHiddenText = "if your client got taxed UK interest"
    val changeTaxedDetailsHiddenText = "the details of your client’s account with taxed UK interest"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val h1Expected = "Check your UK interest"
    val titleExpected = "Check your UK interest"

    val changeUntaxedInterestHiddenText = "if you got untaxed UK interest"
    val changeUntaxedDetailsHiddenText = "the details of your account with untaxed UK interest"
    val changeTaxedInterestHiddenText = "if you got taxed UK interest"
    val changeTaxedDetailsHiddenText = "the details of your account with taxed UK interest"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val h1Expected = "Check your client’s UK interest"
    val titleExpected = "Check your client’s UK interest"

    val changeUntaxedInterestHiddenText = "if your client got untaxed UK interest"
    val changeUntaxedDetailsHiddenText = "the details of your client’s account with untaxed UK interest"
    val changeTaxedInterestHiddenText = "if your client got taxed UK interest"
    val changeTaxedDetailsHiddenText = "the details of your client’s account with taxed UK interest"
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

      s"attempt to return the InterestCYA page - ${welshTest(us.isWelsh)} - ${agentTest(us.isAgent)}" which {

        "has the untaxedUkInterest & taxedUkInterest question answered but with no accounts" which {
          val cyaModel = InterestCYAModel(
            untaxedUkInterest = Some(false),
            taxedUkInterest = Some(true)
          )

          lazy val result = {
            dropInterestDB()
            emptyUserDataStub()
            insertCyaData(Some(cyaModel))
            authoriseAgentOrIndividual(us.isAgent)
            urlGet(s"$appUrl/$taxYear/interest/check-interest", us.isWelsh, follow = false,  playSessionCookie(us.isAgent))
          }

          s"then redirects and has an SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("Location").get shouldBe
              "/income-through-software/return/personal-income/2022/interest/which-account-did-you-get-taxed-interest-from"
          }
        }

        "create a CYA model from prior data if no cya exists" which {

          lazy val result = {
            dropInterestDB()
            userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel("accountName", "id", Some(amount), Some(amount))))), nino, taxYear)
            authoriseAgentOrIndividual(us.isAgent)
            urlGet(s"$appUrl/$taxYear/interest/check-interest", us.isWelsh, follow = false,  playSessionCookie(us.isAgent))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(specific.titleExpected)
          welshToggleCheck(us.isWelsh)
          h1Check(specific.h1Expected + " " + captionExpected)
          textOnPageCheck(captionExpected, captionSelector)

          buttonCheck(submitText, submitButton)
          formPostLinkCheck(submitLink, submitButtonForm)

          "has an area for untaxed accounts" which {
            textOnPageCheck(questionUntaxedInterestDetailsExpected, questionSelector(1))
            textOnPageCheck("accountName : £25", questionAccountSelector(2, 1, 1))
            linkCheck(s"$changeLinkExpected ${specific.changeUntaxedDetailsHiddenText}", questionChangeLinkSelector(1), changeUntaxedInterestAmountHref)
          }

          "has an area for taxed accounts" which {
            textOnPageCheck(question4TaxedInterestDetailExpected, questionSelector(2))
            textOnPageCheck("accountName : £25", questionAccountSelector(4, 1, 1))
            linkCheck(s"$changeLinkExpected ${specific.changeTaxedDetailsHiddenText}", questionChangeLinkSelector(2), changeTaxedInterestAmountHref)
          }
        }

        "only has the untaxedUkInterest question answered with no" which {
          val cyaModel = InterestCYAModel(
            untaxedUkInterest = Some(false)
          )

          lazy val result = {
            dropInterestDB()
            emptyUserDataStub()
            insertCyaData(Some(cyaModel))
            authoriseAgentOrIndividual(us.isAgent)
            urlGet(s"$appUrl/$taxYear/interest/check-interest", us.isWelsh, follow = false,  playSessionCookie(us.isAgent))
          }

          s"then redirects and has an SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("Location").get shouldBe
              "/income-through-software/return/personal-income/2022/interest/taxed-uk-interest"
          }
        }

        "only has the untaxedUkInterest questions answered" which {
          val cyaModel = InterestCYAModel(
            untaxedUkInterest = Some(true),
            accounts = Some(Seq(
              InterestAccountModel(Some("id"), "UntaxedBank1", Some(100.00))
            ))
          )

          lazy val result = {
            dropInterestDB()
            emptyUserDataStub()
            insertCyaData(Some(cyaModel))
            authoriseAgentOrIndividual(us.isAgent)
            urlGet(s"$appUrl/$taxYear/interest/check-interest", us.isWelsh, follow = false,  playSessionCookie(us.isAgent))
          }

          s"then redirects and has an SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("Location").get shouldBe
              "/income-through-software/return/personal-income/2022/interest/taxed-uk-interest"
          }
        }

        "only has the untaxedUkInterest question answered" which {
          val cyaModel = InterestCYAModel(
            untaxedUkInterest = Some(true)
          )

          lazy val result = {
            dropInterestDB()
            emptyUserDataStub()
            insertCyaData(Some(cyaModel))
            authoriseAgentOrIndividual(us.isAgent)
            urlGet(s"$appUrl/$taxYear/interest/check-interest", us.isWelsh, follow = false,  playSessionCookie(us.isAgent))
          }

          s"then redirects and has an SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("Location").get shouldBe
              "/income-through-software/return/personal-income/2022/interest/which-account-did-you-get-untaxed-interest-from"
          }
        }

        "renders a page with all the fields" which {
          val cyaModel = InterestCYAModel(
            untaxedUkInterest = Some(true),
            taxedUkInterest = Some(true),
            accounts = Some(Seq(
              InterestAccountModel(Some("id"), "UntaxedBank1", Some(100.00)),
              InterestAccountModel(Some("id2"), "TaxedBank1", None, Some(200.00)),
              InterestAccountModel(Some("id3"), "TaxedBank2", None, Some(400.00))
            ))
          )

          lazy val result = {
            dropInterestDB()
            emptyUserDataStub()
            insertCyaData(Some(cyaModel))

            authoriseAgentOrIndividual(us.isAgent)
            urlGet(s"$appUrl/$taxYear/interest/check-interest", us.isWelsh, follow = false,  playSessionCookie(us.isAgent))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(specific.titleExpected)
          welshToggleCheck(us.isWelsh)
          h1Check(specific.h1Expected + " " + captionExpected)
          textOnPageCheck(captionExpected, captionSelector)

          buttonCheck(submitText, submitButton)
          formPostLinkCheck(submitLink, submitButtonForm)

          "has an area for question 1" which {
            textOnPageCheck(questionUntaxedInterestExpected, questionSelector(1))
            textOnPageCheck(Yes, yesNoQuestionAnswer(1))
            linkCheck(s"$changeLinkExpected ${specific.changeUntaxedInterestHiddenText}", questionChangeLinkSelector(1), changeUntaxedInterestHref)
          }

          "has an area for question 2" which {
            textOnPageCheck(questionUntaxedInterestDetailsExpected, questionSelector(2))
            textOnPageCheck(untaxedInterestAccount1ExpectedTest, questionAccountSelector(question2, account1, 1))
            linkCheck(s"$changeLinkExpected ${specific.changeUntaxedDetailsHiddenText}", questionChangeLinkSelector(2), changeUntaxedInterestAmountHref)
          }

          "has an area for question 3" which {
            textOnPageCheck(questionTaxedInterestExpected, questionSelector(3))
            textOnPageCheck(Yes, yesNoQuestionAnswer(3))
            linkCheck(s"$changeLinkExpected ${specific.changeTaxedInterestHiddenText}", questionChangeLinkSelector(3), changeTaxedInterestHref)
          }

          "has an area for question 4" which {
            textOnPageCheck(question4TaxedInterestDetailExpected, questionSelector(question4))
            textOnPageCheck(taxedInterestAccount1ExpectedTest, questionAccountSelector(question4, account1, 1))
            textOnPageCheck(taxedInterestAccount2ExpectedTest, questionAccountSelector(question4, account2, 2))
            linkCheck(s"$changeLinkExpected ${specific.changeTaxedDetailsHiddenText}", questionChangeLinkSelector(question4), changeTaxedInterestAmountHref)
          }

        }

        "renders a page with only the yes/no questions" when {

          "the user has selected no to receiving taxed and untaxed interest" which {
            val cyaModel = InterestCYAModel(
              untaxedUkInterest = Some(false),
              taxedUkInterest = Some(false)
            )

            lazy val result = {
              dropInterestDB()
              emptyUserDataStub()
              insertCyaData(Some(cyaModel))

              authoriseAgentOrIndividual(us.isAgent)
              urlGet(s"$appUrl/$taxYear/interest/check-interest", us.isWelsh, follow = true,  playSessionCookie(us.isAgent))
            }

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(specific.titleExpected)
            welshToggleCheck(us.isWelsh)
            h1Check(specific.h1Expected + " " + captionExpected)
            textOnPageCheck(captionExpected, captionSelector)

            buttonCheck(submitText, submitButton)
            formPostLinkCheck(submitLink, submitButtonForm)

            "has an area for question 1" which {
              textOnPageCheck(questionUntaxedInterestExpected, questionTextSelector(1))
              textOnPageCheck(No, yesNoQuestionAnswer(1))
              linkCheck(s"$changeLinkExpected ${specific.changeUntaxedInterestHiddenText}", questionChangeLinkSelector(1), changeUntaxedInterestHref)
            }

            "has an area for question 2" which {
              textOnPageCheck(questionTaxedInterestExpected, questionTextSelector(2))
              textOnPageCheck(No, yesNoQuestionAnswer(2))
              linkCheck(s"$changeLinkExpected ${specific.changeTaxedInterestHiddenText}", questionChangeLinkSelector(2), changeTaxedInterestHref)
            }

            "there is no question 3" in {
              elementExist(questionSelector(3)) shouldBe false
            }

            "there is no question 4" in {
              elementExist(questionSelector(question4)) shouldBe false
            }
          }

          "the user has both tax types prior" which {
            val interestModel = Some(Seq(
              InterestModel("TSB", "qwerty", None, Some(100.00)),
              InterestModel("TSB", "azerty", Some(100.00), None)
            ))

            val cyaModel = InterestCYAModel(
              untaxedUkInterest = Some(true),
              taxedUkInterest = Some(true),
              accounts = Some(Seq(
                InterestAccountModel(Some("qwerty"), "TSB", Some(100.00)),
                InterestAccountModel(Some("azerty"), "TSB Account", None, Some(100.00))))
            )

            lazy val result = {
              dropInterestDB()
              emptyUserDataStub()

              userDataStub(IncomeSourcesModel(interest = interestModel), nino, taxYear)
              insertCyaData(Some(cyaModel))

              authoriseAgentOrIndividual(us.isAgent)
              urlGet(s"$appUrl/$taxYear/interest/check-interest", us.isWelsh, follow = true,  playSessionCookie(us.isAgent))
            }

            s"has an OK($OK) status" in {
              result.status shouldBe OK
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(specific.titleExpected)
            welshToggleCheck(us.isWelsh)
            h1Check(specific.h1Expected + " " + captionExpected)
            textOnPageCheck(captionExpected, captionSelector)

            buttonCheck(submitText, submitButton)
            formPostLinkCheck(submitLink, submitButtonForm)

            "has an area for question 1" which {
              textOnPageCheck(questionUntaxedInterestDetailsExpected, questionTextSelector(1))
              textOnPageCheck("TSB : £100", yesNoQuestionAnswer(1))
              linkCheck(s"$changeLinkExpected ${specific.changeUntaxedDetailsHiddenText}", questionChangeLinkSelector(1), changeUntaxedInterestAmountHref)
            }

            "has an area for question 2" which {
              textOnPageCheck(question4TaxedInterestDetailExpected, questionTextSelector(2))
              textOnPageCheck("TSB Account : £100", yesNoQuestionAnswer(2))
              linkCheck(s"$changeLinkExpected ${specific.changeTaxedDetailsHiddenText}", questionChangeLinkSelector(2), changeTaxedInterestAmountHref)
            }

            "there is no question 3" in {
              elementExist(questionSelector(3)) shouldBe false
            }

            "there is no question 4" in {
              elementExist(questionSelector(question4)) shouldBe false
            }
          }
        }

        "there is no CYA data in session" which {
          lazy val result = {
            dropInterestDB()
            emptyUserDataStub()
            insertCyaData(None, taxYear, Some("AA123456A"), None)
            authoriseAgentOrIndividual(us.isAgent)

            urlGet(s"$appUrl/$taxYear/interest/check-interest", us.isWelsh, follow = false,  playSessionCookie(us.isAgent))
          }

          s"has an SEE_OTHER($SEE_OTHER) status" in {
            result.status shouldBe SEE_OTHER
            result.header("Location").get shouldBe "http://localhost:11111/income-through-software/return/2022/view"
          }
        }

        "the authorization fails" which {
          lazy val result = {
            unauthorisedAgentOrIndividual(us.isAgent)

            urlGet(s"$appUrl/$taxYear/interest/check-interest", us.isWelsh, follow = true,  playSessionCookie(us.isAgent))
          }

          s"has an Unauthorised($UNAUTHORIZED) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }

      }
    }
  }


  ".submit" should {

    userScenarios.foreach { us =>

      val expectedHeaders = Seq(new HttpHeader("mtditid", mtditid))

      s"attempt to return the InterestCYA page - ${welshTest(us.isWelsh)} - ${agentTest(us.isAgent)}" which {

        s"has an OK($OK) status" in {

          lazy val result = {
            dropInterestDB()
            emptyUserDataStub()
            insertCyaData(Some(InterestCYAModel(
              Some(false), Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", None, Some(amount))))
            )), taxYear, Some(mtditid), None)
            authoriseIndividual()
            stubGet(s"/income-through-software/return/$taxYear/view", OK, "")
            stubPost(s"/income-tax-interest/income-tax/nino/AA123456A/sources\\?taxYear=$taxYear", NO_CONTENT, "", expectedHeaders)

            urlPost(s"$appUrl/$taxYear/interest/check-interest", "{}", us.isWelsh, follow = true, playSessionCookie(us.isAgent))
          }

          result.status shouldBe OK
        }

        s"returns the internal server error page when no CYA" in {

          lazy val result = {
            dropInterestDB()
            emptyUserDataStub()
            insertCyaData(None)
            authoriseIndividual()
            stubPost(s"/income-tax-interest/income-tax/nino/AA123456A/sources\\?taxYear=$taxYear", NO_CONTENT, "", expectedHeaders)
            urlPost(s"$appUrl/$taxYear/interest/check-interest", "{}", us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          result.status shouldBe INTERNAL_SERVER_ERROR
        }

        s"redirect when there is no session" in {
          lazy val result = {
            dropInterestDB()
            emptyUserDataStub()
            insertCyaData(Some(InterestCYAModel(
              Some(false),
              Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", None, Some(amount))))
            )))
            authoriseIndividual(None)
            urlPost(s"$appUrl/$taxYear/interest/check-interest", "{}", us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          result.status shouldBe SEE_OTHER
        }

        "the authorization fails" in {
          lazy val result = {
            dropInterestDB()
            emptyUserDataStub()
            insertCyaData(Some(InterestCYAModel(
              Some(false),
              Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", None, Some(amount))))
            )))
            authoriseIndividualUnauthorized()
            urlPost(s"$appUrl/$taxYear/interest/check-interest", "{}", us.isWelsh, follow = true, playSessionCookie(us.isAgent))
          }

          result.status shouldBe UNAUTHORIZED

        }
      }
    }
  }
}