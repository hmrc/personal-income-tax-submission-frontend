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

import java.util.UUID

import forms.interest.UntaxedInterestAmountForm
import models.interest.{InterestAccountModel, InterestCYAModel}
import models.priorDataModels.{IncomeSourcesModel, InterestModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER, UNAUTHORIZED}
import play.api.libs.ws.WSResponse
import utils.{IntegrationTest, InterestDatabaseHelper, ViewHelpers}

class UntaxedInterestAmountControllerISpec extends IntegrationTest with ViewHelpers with InterestDatabaseHelper {

  val amount: BigDecimal = 25
  val firstAccountName: String = "HSBC"
  val secondAccountName: String = "Santander"
  val taxYear: Int = 2022
  lazy val id: String = UUID.randomUUID().toString

  object Selectors {
    val accountName: String = "#main-content > div > div > form > div:nth-child(3) > label > div"
    val interestEarned: String = "#main-content > div > div > form > div:nth-child(4) > label > div"
    val accountNameInput: String = "#untaxedAccountName"
    val eachAccount = "#main-content > div > div > form > div:nth-child(3) > label > p"
    val amountInput: String = "#untaxedAmount"

    val errorSummary: String = "#error-summary-title"
    val firstError: String = ".govuk-error-summary__body > ul > li:nth-child(1) > a"
    val secondError: String = ".govuk-error-summary__body > ul > li:nth-child(2) > a"
    val errorMessage: String = "#value-error"
  }

  trait SpecificExpectedResults {
    val noAmountEntryError: String
  }

  trait CommonExpectedResults {
    val heading: String
    val caption: String
    val accountName: String
    val eachAccount: String
    val interestEarned: String
    val hint: String
    val button: String
    val noNameEntryError: String
    val invalidCharEntry: String
    val nameTooLongError: String
    val duplicateNameError: String
    val tooMuchMoneyError: String
    val incorrectFormatError: String
    val errorTitle: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val heading: String = "Add an account with untaxed UK interest"
    val caption: String = "Interest for 6 April 2021 to 5 April 2022"
    val accountName: String = "What do you want to name this account?"
    val eachAccount = "Give each account a different name."
    val interestEarned: String = "Amount of untaxed UK interest"
    val hint: String = "For example, ‘HSBC savings account’. " + "For example, £600 or £193.54"
    val button: String = "Continue"
    val noNameEntryError: String = "Enter a name for this account"
    val invalidCharEntry: String = "Name of account with untaxed UK interest must only include numbers 0-9, " +
      "letters a to z, hyphens, spaces, apostrophes, commas, full stops, round brackets, and the special characters &, /, @, £, *"
    val nameTooLongError: String = "The name of the account must be 32 characters or fewer"
    val duplicateNameError: String = "You cannot add 2 accounts with the same name"
    val tooMuchMoneyError = "The amount of untaxed UK interest must be less than £100,000,000,000"
    val incorrectFormatError = "Enter the amount of untaxed UK interest in the correct format"
    val errorTitle: String = s"Error: $heading"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val heading: String = "Add an account with untaxed UK interest"
    val caption: String = "Interest for 6 April 2021 to 5 April 2022"
    val accountName: String = "What do you want to name this account?"
    val eachAccount = "Give each account a different name."
    val interestEarned: String = "Amount of untaxed UK interest"
    val hint: String = "For example, ‘HSBC savings account’. " + "For example, £600 or £193.54"
    val button: String = "Continue"
    val noNameEntryError: String = "Enter a name for this account"
    val invalidCharEntry: String = "Name of account with untaxed UK interest must only include numbers 0-9, " +
      "letters a to z, hyphens, spaces, apostrophes, commas, full stops, round brackets, and the special characters &, /, @, £, *"
    val nameTooLongError: String = "The name of the account must be 32 characters or fewer"
    val duplicateNameError: String = "You cannot add 2 accounts with the same name"
    val tooMuchMoneyError = "The amount of untaxed UK interest must be less than £100,000,000,000"
    val incorrectFormatError = "Enter the amount of untaxed UK interest in the correct format"
    val errorTitle: String = s"Error: $heading"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val noAmountEntryError = "Enter the amount of untaxed UK interest you got"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val noAmountEntryError = "Enter the amount of untaxed UK interest your client got"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val noAmountEntryError = "Enter the amount of untaxed UK interest you got"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val noAmountEntryError = "Enter the amount of untaxed UK interest your client got"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(
      UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
    )
  }

  def url(newId: String): String = s"$appUrl/$taxYear/interest/add-untaxed-uk-interest-account/$newId"

  ".show" when {

    userScenarios.foreach { us =>

      import us.commonExpectedResults._

      val specific = us.specificExpectedResults.get

      s"user is ${agentTest(us.isAgent)} and request is ${welshTest(us.isWelsh)}" should {

        "return OK and correctly render the page" when {
          lazy val result = {
            dropInterestDB()
            emptyUserDataStub()
            insertCyaData(Some(InterestCYAModel(
              Some(true), Some(false), Some(Seq(
                InterestAccountModel(Some("differentId"), firstAccountName, Some(amount)),
                InterestAccountModel(None, secondAccountName, Some(amount), uniqueSessionId = Some(id))
              ))
            )))
            authoriseAgentOrIndividual(us.isAgent)
            urlGet(url(id), us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(heading)
          welshToggleCheck(us.isWelsh)
          h1Check(s"$heading $caption")
          captionCheck(caption)
          textOnPageCheck(accountName, Selectors.accountName)
          textOnPageCheck(eachAccount, Selectors.eachAccount)
          inputFieldCheck(UntaxedInterestAmountForm.untaxedAccountName, Selectors.accountNameInput)
          textOnPageCheck(interestEarned, Selectors.interestEarned)
          hintTextCheck(hint)
          inputFieldCheck(UntaxedInterestAmountForm.untaxedAmount, Selectors.amountInput)
          buttonCheck(button)

          elementExtinct(Selectors.errorSummary)
          elementExtinct(Selectors.firstError)
          elementExtinct(Selectors.errorMessage)
        }

        "redirect when the id is not a UUID" which {
          lazy val result = {
            dropInterestDB()
            emptyUserDataStub()
            insertCyaData(Some(InterestCYAModel(Some(true), Some(false), None)))
            authoriseAgentOrIndividual(us.isAgent)
            urlGet(url("id"), us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          s"has an SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("Location").get should include(
              "/income-through-software/return/personal-income/2022/interest/add-untaxed-uk-interest-account/")
          }
        }

        "redirect to the change amount page" when {

          "the id matches a prior submission" which {
            lazy val result = {
              dropInterestDB()
              userDataStub(IncomeSourcesModel(None, Some(Seq(InterestModel(firstAccountName, id, None, Some(amount)))), None), nino, taxYear)
              insertCyaData(Some(InterestCYAModel(
                Some(true), Some(false),
                Some(Seq(InterestAccountModel(Some(id), firstAccountName, Some(amount), None, Some(id))))
              )), taxYear, None, Some(nino))
              authoriseAgentOrIndividual(us.isAgent)
              urlGet(url(id), us.isWelsh, follow = false, playSessionCookie(us.isAgent))
            }

            s"has an SEE OTHER status" in {
              result.status shouldBe SEE_OTHER
              result.header("Location").get should include(
                "/income-through-software/return/personal-income/2022/interest/change-untaxed-uk-interest?accountId=")
            }
          }
        }

        "redirect to the overview page" when {

          "there is no cya data in session" which {

            lazy val result = {
              dropInterestDB()
              emptyUserDataStub()
              insertCyaData(None)
              authoriseAgentOrIndividual(us.isAgent)
              urlGet(url(id), us.isWelsh, follow = false, playSessionCookie(us.isAgent))
            }

            s"has an SEE OTHER status" in {
              result.status shouldBe SEE_OTHER
              result.header("Location").get should include(
                "http://localhost:11111/income-through-software/return/2022/view")
            }
          }
        }

        "handle when the user is unauthorized" should {

          lazy val result = {
            dropInterestDB()
            emptyUserDataStub()
            insertCyaData(None)
            unauthorisedAgentOrIndividual(us.isAgent)
            urlGet(url(id), us.isWelsh, follow = true, playSessionCookie(us.isAgent))
          }

          s"return an Unauthorised($UNAUTHORIZED) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }
      }
    }
  }

  ".submit" when {

    userScenarios.foreach { us =>

      import us.commonExpectedResults._

      val specific = us.specificExpectedResults.get

      s"user is ${agentTest(us.isAgent)} and request is ${welshTest(us.isWelsh)}" when {

        def response(formMap: Map[String, String]): WSResponse = {
          dropInterestDB()
          emptyUserDataStub()
          insertCyaData(Some(InterestCYAModel(
            Some(true), Some(false), Some(Seq(
              InterestAccountModel(Some("differentId"), firstAccountName, Some(amount)),
              InterestAccountModel(None, secondAccountName, Some(amount), uniqueSessionId = Some(id))
            ))
          )))
          authoriseAgentOrIndividual(us.isAgent)
          urlPost(url(id), formMap, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
        }

        "an account name and amount are entered correctly" should {

          lazy val result = response(Map(UntaxedInterestAmountForm.untaxedAmount -> "67.66",
            UntaxedInterestAmountForm.untaxedAccountName -> "Halifax"))

          "return a SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("Location").get should include(
              "/income-through-software/return/personal-income/2022/interest/accounts-with-untaxed-uk-interest")
          }
        }

        "an existing account is updated" should {
          lazy val result = response(Map(
            UntaxedInterestAmountForm.untaxedAmount -> "50",
            UntaxedInterestAmountForm.untaxedAccountName -> secondAccountName
          ))

          "return a 200(Ok) status" in {
            result.status shouldBe SEE_OTHER
            result.header("Location").get should include(
              "/income-through-software/return/personal-income/2022/interest/accounts-with-untaxed-uk-interest")
          }
        }

        "the fields are empty" should {

          lazy val result = response(Map(UntaxedInterestAmountForm.untaxedAmount -> "",
            UntaxedInterestAmountForm.untaxedAccountName -> ""))

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"return a 400(BadRequest) status" in {
            result.status shouldBe BAD_REQUEST
          }

          multipleErrorCheck(
            List(
              (noNameEntryError, Selectors.accountNameInput),
              (specific.noAmountEntryError, Selectors.amountInput)
            )
          )
        }

        "invalid characters are entered into the fields" should {
          lazy val result = response(Map(UntaxedInterestAmountForm.untaxedAmount -> "money",
            UntaxedInterestAmountForm.untaxedAccountName -> "$uper"))

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"return a 400(BadRequest) status" in {
            result.status shouldBe BAD_REQUEST
          }

          multipleErrorCheck(
            List(
              (invalidCharEntry, Selectors.accountNameInput),
              (incorrectFormatError, Selectors.amountInput)
            )
          )
        }

        "the account name is too long and the amount is too great" should {
          lazy val result = response(Map(UntaxedInterestAmountForm.untaxedAmount -> "100000000000",
            UntaxedInterestAmountForm.untaxedAccountName -> "SuperAwesomeBigBusinessMoneyStash"))

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"return a 400(BadRequest) status" in {
            result.status shouldBe BAD_REQUEST
          }

          multipleErrorCheck(
            List(
              (nameTooLongError, Selectors.accountNameInput),
              (tooMuchMoneyError, Selectors.amountInput)
            )
          )
        }

        "an amount is entered with incorrect format" should {
          lazy val result = response(Map(
            UntaxedInterestAmountForm.untaxedAmount -> "500.8.75",
            UntaxedInterestAmountForm.untaxedAccountName -> "Sensible account"
          ))

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"return a 400(BadRequest) status" in {
            result.status shouldBe BAD_REQUEST
          }

          inputFieldValueCheck("Sensible account", Selectors.accountNameInput)
          inputFieldValueCheck("", Selectors.amountInput)
          titleCheck(errorTitle)

          errorSummaryCheck(incorrectFormatError, Selectors.amountInput)
          errorAboveElementCheck(incorrectFormatError)
        }

        "a duplicate account name is entered" should {

          lazy val result: WSResponse = {
            dropInterestDB()
            emptyUserDataStub()
            insertCyaData(Some(InterestCYAModel(
              Some(true), Some(false), Some(Seq(
                InterestAccountModel(Some("differentId"), firstAccountName, Some(amount)),
                InterestAccountModel(None, secondAccountName, Some(amount), uniqueSessionId = Some(id))
              ))
            )))
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(url("1234567890-09876543210"), Map(
              UntaxedInterestAmountForm.untaxedAmount -> "12344.98",
              UntaxedInterestAmountForm.untaxedAccountName -> secondAccountName
            ), us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"return a 400(BadRequest) status" in {
            result.status shouldBe BAD_REQUEST
          }

          inputFieldValueCheck(secondAccountName, Selectors.accountNameInput)
          inputFieldValueCheck("12344.98", Selectors.amountInput)
          titleCheck(errorTitle)

          errorSummaryCheck(duplicateNameError, Selectors.accountNameInput)
          errorAboveElementCheck(duplicateNameError)
        }

        "return a redirect when no cya" should {

          lazy val result: WSResponse = {
            dropInterestDB()
            userDataStub(IncomeSourcesModel(None, Some(Seq(InterestModel(firstAccountName, id, None, None))), None), nino, taxYear)
            insertCyaData(None)
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(url(id), Map(
              UntaxedInterestAmountForm.untaxedAmount -> "12344.98",
              UntaxedInterestAmountForm.untaxedAccountName -> firstAccountName
            ), us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"return a SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("Location").get should include(
              "http://localhost:11111/income-through-software/return/2022/view")
          }
        }

        "an account is reused and amount is updated" should {

          lazy val result: WSResponse = {
            dropInterestDB()
            userDataStub(IncomeSourcesModel(None, Some(Seq(InterestModel(firstAccountName, id, None, None))), None), nino, taxYear)
            insertCyaData(Some(InterestCYAModel(Some(true),None,Some(Seq(
              InterestAccountModel(None,"name",Some(1234),uniqueSessionId = Some("1234567890"))
            )))))
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(url("1234567890"), Map(
              UntaxedInterestAmountForm.untaxedAmount -> "12344.98",
              UntaxedInterestAmountForm.untaxedAccountName -> firstAccountName
            ), us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"return a SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("Location").get should include(
              "/income-through-software/return/personal-income/2022/interest/accounts-with-untaxed-uk-interest")
          }
        }

        "a new account is added and old account removes the untaxed amount" should {

          lazy val result: WSResponse = {
            dropInterestDB()
            emptyUserDataStub()
            insertCyaData(Some(InterestCYAModel(Some(true),None,Some(Seq(
              InterestAccountModel(None,"name",Some(1234),Some(1234),uniqueSessionId = Some("1234567890"))
            )))))
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(url("1234567890"), Map(
              UntaxedInterestAmountForm.untaxedAmount -> "12344.98",
              UntaxedInterestAmountForm.untaxedAccountName -> firstAccountName
            ), us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"return a SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("Location").get should include(
              "/income-through-software/return/personal-income/2022/interest/accounts-with-untaxed-uk-interest")

            val data: Seq[InterestCYAModel] = await(interestDatabase.collection.find().toFuture()).flatMap(_.interest)
            data.head shouldBe InterestCYAModel(Some(true),None,Some(Seq(
              InterestAccountModel(None,firstAccountName,Some(12344.98),uniqueSessionId = data.head.accounts.get.head.uniqueSessionId),
              InterestAccountModel(None,"name",None,Some(1234),uniqueSessionId = Some("1234567890"))
            )))
          }
        }

        "a new account is added" should {

          lazy val result: WSResponse = {
            dropInterestDB()
            emptyUserDataStub()
            insertCyaData(Some(InterestCYAModel(Some(true),None)))
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(url(id), Map(
              UntaxedInterestAmountForm.untaxedAmount -> "12344.98",
              UntaxedInterestAmountForm.untaxedAccountName -> firstAccountName
            ), us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"return a SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("Location").get should include(
              "/income-through-software/return/personal-income/2022/interest/accounts-with-untaxed-uk-interest")

            val data: Seq[InterestCYAModel] = await(interestDatabase.collection.find().toFuture()).flatMap(_.interest)
            data.head shouldBe InterestCYAModel(Some(true),None,Some(Seq(
              InterestAccountModel(None,firstAccountName,Some(12344.98),uniqueSessionId = data.head.accounts.get.head.uniqueSessionId)
            )))
          }
        }

        "an account is reused and amount is updated even if the id is different" should {

          lazy val result: WSResponse = {
            dropInterestDB()
            userDataStub(IncomeSourcesModel(None, Some(Seq(InterestModel(firstAccountName, id, None, None))), None), nino, taxYear)
            insertCyaData(Some(InterestCYAModel(Some(true))))
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(url("id"), Map(
              UntaxedInterestAmountForm.untaxedAmount -> "12344.98",
              UntaxedInterestAmountForm.untaxedAccountName -> firstAccountName
            ), us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"return a SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("Location").get should include(
              "/income-through-software/return/personal-income/2022/interest/accounts-with-untaxed-uk-interest")
          }
        }
      }
    }
  }
}