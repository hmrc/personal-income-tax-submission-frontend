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

import java.util.UUID
import forms.interest.TaxedInterestAmountForm
import models.interest.{InterestAccountModel, InterestCYAModel}
import models.priorDataModels.{IncomeSourcesModel, InterestModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER, UNAUTHORIZED}
import play.api.libs.ws.WSResponse
import test.utils.{IntegrationTest, InterestDatabaseHelper, ViewHelpers}

class TaxedInterestAmountControllerISpec extends IntegrationTest with ViewHelpers with InterestDatabaseHelper {

  val amount: BigDecimal = 25
  val firstAccountName: String = "HSBC"
  val secondAccountName: String = "Santander"
  lazy val id: String = UUID.randomUUID().toString

  val charLimit: String = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Cras suscipit turpis sed blandit" +
    " lobortis. Vestibulum dignissim nulla quis luctus placerat. Quisque commodo eros tristique nibh scelerisque, sit" +
    " amet aliquet odio laoreet. Sed finibus dapibus lorem sit amet elementum. Nunc euismod arcu augue, tincidunt" +
    " elementum elit vulputate et. Nunc imperdiet est magna, non vestibulum tortor vehicula eu. Nulla a est sed nibh" +
    " lacinia maximus. Nullam facilisis nunc vel sapien facilisis tincidunt. Sed odio."

  object Selectors {
    val accountName: String = "#main-content > div > div > form > div:first-of-type > label > .govuk-label"
    val interestEarned: String = "#main-content > div > div > form > div:last-of-type > label"
    val accountNameInput: String = "#taxedAccountName"
    val eachAccount = "#p1"
    val amountInput: String = "#taxedAmount"

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
    val heading: String = "Add an account with taxed UK interest"
    val caption: String = s"Interest for 6 April $taxYearEOY to 5 April $taxYear"
    val accountName: String = "What do you want to name this account?"
    val eachAccount = "Give each account a different name."
    val interestEarned: String = "Amount of taxed UK interest"
    val hint: String = "For example, ‘HSBC savings account’. " + "For example, £193.52"
    val button: String = "Continue"
    val noNameEntryError: String = "Enter a name for this account"
    val invalidCharEntry: String = "Name of account with taxed UK interest must only include numbers 0-9, " +
      "letters a to z, hyphens, spaces, apostrophes, commas, full stops and ampersands"
    val nameTooLongError: String = "The name of the account must be 32 characters or fewer"
    val duplicateNameError: String = "You cannot add 2 accounts with the same name"
    val tooMuchMoneyError = "The amount of taxed UK interest must be less than £100,000,000,000"
    val incorrectFormatError = "Enter the amount of taxed UK interest in the correct format"
    val errorTitle: String = s"Error: $heading"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val heading: String = "Ychwanegwch gyfrif gyda llog y DU a drethwyd"
    val caption: String = s"Llog ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val accountName: String = "Beth yw’r enw rydych am roi i’r cyfrif hwn?"
    val eachAccount = "Rhowch enw gwahanol i bob cyfrif."
    val interestEarned: String = "Swm y llog y DU a drethwyd"
    val hint: String = "Er enghraifft, ‘cyfrif cynilo HSBC’. " + "Er enghraifft, £193.52"
    val button: String = "Yn eich blaen"
    val noNameEntryError: String = "Nodwch enw ar gyfer y cyfrif hwn"
    val invalidCharEntry: String = "Rhaid i enw’r cyfrif, sydd â llog y DU a drethwyd, gynnwys y rhifau 0-9, " +
      "llythrennau a i z, cysylltnodau, bylchau, collnodau, atalnodau, atalnodau llawn ac ampersands yn unig"
    val nameTooLongError: String = "Mae’n rhaid i enw’r cyfrif fod yn 32 o gymeriadau neu lai"
    val duplicateNameError: String = "Ni allwch ychwanegu 2 gyfrif gyda’r un enw"
    val tooMuchMoneyError = "Mae’n rhaid i swm y llog y DU a drethwyd fod yn llai na £100,000,000,000"
    val incorrectFormatError = "Nodwch swm y llog y DU a drethwyd yn y fformat cywir"
    val errorTitle: String = s"Gwall: $heading"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val noAmountEntryError = "Enter the amount of taxed UK interest you got"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val noAmountEntryError = "Enter the amount of taxed UK interest your client got"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val noAmountEntryError = "Nodwch swm y llog y DU a drethwyd a gawsoch"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val noAmountEntryError = "Nodwch swm y llog y DU a drethwyd a gafodd eich cleient"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(
      UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY))
    )
  }

  def url(newId: String): String = s"$appUrl/$taxYear/interest/add-taxed-uk-interest-account/$newId"

  ".show" when {

    userScenarios.foreach { us =>

      import us.commonExpectedResults._

      s"user is ${agentTest(us.isAgent)} and request is ${welshTest(us.isWelsh)}" should {

        "return OK and correctly render the page" when {
          lazy val result = {
            dropInterestDB()
            emptyUserDataStub()
            insertInterestCyaData(Some(InterestCYAModel(
              None, Some(false), Some(true), Seq(
                InterestAccountModel(Some("differentId"), firstAccountName, taxedAmount = Some(amount)),
                InterestAccountModel(None, secondAccountName, taxedAmount = Some(amount), uniqueSessionId = Some(id))
              ))
            ))
            authoriseAgentOrIndividual(us.isAgent)
            urlGet(url(id), us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          s"has an OK($OK) status" in {
            result.status shouldBe OK
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(heading, us.isWelsh)
          welshToggleCheck(us.isWelsh)
          h1Check(s"$heading $caption")
          captionCheck(caption)
          textOnPageCheck(accountName, Selectors.accountName)
          textOnPageCheck(eachAccount, Selectors.eachAccount)
          inputFieldCheck(TaxedInterestAmountForm.taxedAccountName, Selectors.accountNameInput)
          textOnPageCheck(interestEarned, Selectors.interestEarned)
          hintTextCheck(hint)
          inputFieldCheck(TaxedInterestAmountForm.taxedAmount, Selectors.amountInput)
          buttonCheck(button)

          elementExtinct(Selectors.errorSummary)
          elementExtinct(Selectors.firstError)
          elementExtinct(Selectors.errorMessage)
        }

        "redirect when the id is not a UUID" which {
          lazy val result = {
            dropInterestDB()
            emptyUserDataStub()
            insertInterestCyaData(Some(InterestCYAModel(None, Some(false), Some(true))))
            authoriseAgentOrIndividual(us.isAgent)
            urlGet(url("id"), us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          s"has an SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("Location").get should include(
              s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/add-taxed-uk-interest-account/")
          }
        }

        "redirect to the change amount page" when {

          "the id matches a prior submission" which {
            lazy val result = {
              dropInterestDB()
              userDataStub(IncomeSourcesModel(None, Some(Seq(InterestModel(firstAccountName, id, None, Some(amount)))), None), nino, taxYear)
              insertInterestCyaData(Some(InterestCYAModel(
                None, Some(false), Some(true),
                Seq(InterestAccountModel(Some(id), firstAccountName, taxedAmount = Some(amount), uniqueSessionId= Some(id)))
              )), taxYear, None, Some(nino))
              authoriseAgentOrIndividual(us.isAgent)
              urlGet(url(id), us.isWelsh, follow = false, playSessionCookie(us.isAgent))
            }

            s"has an SEE OTHER status" in {
              result.status shouldBe SEE_OTHER
              result.header("Location").get should include(
                s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/change-taxed-uk-interest?accountId=")
            }
          }
        }

        "redirect to the overview page" when {

          "there is no cya data in session" which {

            lazy val result = {
              dropInterestDB()
              emptyUserDataStub()
              insertInterestCyaData(None)
              authoriseAgentOrIndividual(us.isAgent)
              urlGet(url(id), us.isWelsh, follow = false, playSessionCookie(us.isAgent))
            }

            s"has an SEE OTHER status" in {
              result.status shouldBe SEE_OTHER
              result.header("Location").get should include(
                s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view")
            }
          }
        }

        "handle when the user is unauthorized" should {

          lazy val result = {
            dropInterestDB()
            emptyUserDataStub()
            insertInterestCyaData(None)
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
          insertInterestCyaData(Some(InterestCYAModel(
            None, Some(false), Some(true), Seq(
              InterestAccountModel(Some("differentId"), firstAccountName, taxedAmount =  Some(amount)),
              InterestAccountModel(None, secondAccountName, taxedAmount = Some(amount), uniqueSessionId = Some(id))
            ))
          ))
          authoriseAgentOrIndividual(us.isAgent)
          urlPost(url(id), formMap, us.isWelsh, follow = false, playSessionCookie(us.isAgent))
        }

        "an account name and amount are entered correctly" should {

          lazy val result = response(Map(TaxedInterestAmountForm.taxedAmount -> "67.66",
            TaxedInterestAmountForm.taxedAccountName -> "Halifax"))

          "return a SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("Location").get should include(
              s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/accounts-with-taxed-uk-interest")
          }
        }

        "an existing account is updated" should {
          lazy val result = response(Map(
            TaxedInterestAmountForm.taxedAmount -> "50",
            TaxedInterestAmountForm.taxedAccountName -> secondAccountName
          ))

          "return a 200(Ok) status" in {
            result.status shouldBe SEE_OTHER
            result.header("Location").get should include(
              s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/accounts-with-taxed-uk-interest")
          }
        }

        "the fields are empty" should {

          lazy val result = response(Map(TaxedInterestAmountForm.taxedAmount -> "",
            TaxedInterestAmountForm.taxedAccountName -> ""))

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"return a 400(BadRequest) status" in {
            result.status shouldBe BAD_REQUEST
          }

          multipleErrorCheck(
            List(
              (noNameEntryError, Selectors.accountNameInput),
              (specific.noAmountEntryError, Selectors.amountInput)
            ), us.isWelsh
          )
        }

        "invalid characters are entered into the fields" should {
          lazy val result = response(Map(TaxedInterestAmountForm.taxedAmount -> "money",
            TaxedInterestAmountForm.taxedAccountName -> "$uper"))

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"return a 400(BadRequest) status" in {
            result.status shouldBe BAD_REQUEST
          }

          multipleErrorCheck(
            List(
              (invalidCharEntry, Selectors.accountNameInput),
              (incorrectFormatError, Selectors.amountInput)
            ), us.isWelsh
          )
        }

        "round brackets are entered into the account name field" should {
          lazy val result = response(Map(TaxedInterestAmountForm.taxedAmount -> "money",
            TaxedInterestAmountForm.taxedAccountName -> "Account (taxed)"))

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"return a 400(BadRequest) status" in {
            result.status shouldBe BAD_REQUEST
          }

          multipleErrorCheck(
            List(
              (invalidCharEntry, Selectors.accountNameInput),
              (incorrectFormatError, Selectors.amountInput)
            ), us.isWelsh
          )
        }

        "at sign is entered into the account name field" should {
          lazy val result = response(Map(TaxedInterestAmountForm.taxedAmount -> "money",
            TaxedInterestAmountForm.taxedAccountName -> "Account @"))

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"return a 400(BadRequest) status" in {
            result.status shouldBe BAD_REQUEST
          }

          multipleErrorCheck(
            List(
              (invalidCharEntry, Selectors.accountNameInput),
              (incorrectFormatError, Selectors.amountInput)
            ), us.isWelsh
          )
        }

        "pound symbol is entered into the account name field" should {
          lazy val result = response(Map(TaxedInterestAmountForm.taxedAmount -> "money",
            TaxedInterestAmountForm.taxedAccountName -> "Account £"))

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"return a 400(BadRequest) status" in {
            result.status shouldBe BAD_REQUEST
          }

          multipleErrorCheck(
            List(
              (invalidCharEntry, Selectors.accountNameInput),
              (incorrectFormatError, Selectors.amountInput)
            ), us.isWelsh
          )
        }

        "asterisk symbol is entered into the account name field" should {
          lazy val result = response(Map(TaxedInterestAmountForm.taxedAmount -> "money",
            TaxedInterestAmountForm.taxedAccountName -> "Account *"))

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"return a 400(BadRequest) status" in {
            result.status shouldBe BAD_REQUEST
          }

          multipleErrorCheck(
            List(
              (invalidCharEntry, Selectors.accountNameInput),
              (incorrectFormatError, Selectors.amountInput)
            ), us.isWelsh
          )
        }

        "the account name is too long and the amount is too great" should {
          lazy val result = response(Map(TaxedInterestAmountForm.taxedAmount -> "100000000000",
            TaxedInterestAmountForm.taxedAccountName -> charLimit))

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"return a 400(BadRequest) status" in {
            result.status shouldBe BAD_REQUEST
          }

          multipleErrorCheck(
            List(
              (nameTooLongError, Selectors.accountNameInput),
              (tooMuchMoneyError, Selectors.amountInput)
            ), us.isWelsh
          )
        }

        "an amount is entered with incorrect format" should {
          lazy val result = response(Map(
            TaxedInterestAmountForm.taxedAmount -> "500.8.75",
            TaxedInterestAmountForm.taxedAccountName -> "Sensible account"
          ))

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"return a 400(BadRequest) status" in {
            result.status shouldBe BAD_REQUEST
          }

          inputFieldValueCheck("Sensible account", Selectors.accountNameInput)
          inputFieldValueCheck("500.8.75", Selectors.amountInput)
          titleCheck(errorTitle, us.isWelsh)
          errorSummaryCheck(incorrectFormatError, Selectors.amountInput, us.isWelsh)
          errorAboveElementCheck(incorrectFormatError)
        }

        "a duplicate account name is entered" should {

          lazy val result: WSResponse = {
            dropInterestDB()
            emptyUserDataStub()
            insertInterestCyaData(Some(InterestCYAModel(
              None, Some(false), Some(true), Seq(
                InterestAccountModel(Some("differentId"), firstAccountName, taxedAmount = Some(amount)),
                InterestAccountModel(None, secondAccountName, taxedAmount = Some(amount), uniqueSessionId = Some(id))
              )
            )))
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(url("1234567890-09876543210"), Map(
              TaxedInterestAmountForm.taxedAmount -> "12,344.98",
              TaxedInterestAmountForm.taxedAccountName -> secondAccountName
            ), us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"return a 400(BadRequest) status" in {
            result.status shouldBe BAD_REQUEST
          }

          inputFieldValueCheck(secondAccountName, Selectors.accountNameInput)
          inputFieldValueCheck("12,344.98", Selectors.amountInput)
          titleCheck(errorTitle, us.isWelsh)

          errorSummaryCheck(duplicateNameError, Selectors.accountNameInput, us.isWelsh)
          errorAboveElementCheck(duplicateNameError)
        }

        "return a redirect when no cya" should {

          lazy val result: WSResponse = {
            dropInterestDB()
            userDataStub(IncomeSourcesModel(None, Some(Seq(InterestModel(firstAccountName, id, None, None))), None), nino, taxYear)
            insertInterestCyaData(None)
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(url(id), Map(
              TaxedInterestAmountForm.taxedAmount -> "12,344.98",
              TaxedInterestAmountForm.taxedAccountName -> firstAccountName
            ), us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          s"return a SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("Location").get should include(
              s"http://localhost:11111/update-and-submit-income-tax-return/$taxYear/view")
          }
        }

        "an account is reused and amount is updated" should {

          lazy val result: WSResponse = {
            dropInterestDB()
            userDataStub(IncomeSourcesModel(None, Some(Seq(InterestModel(firstAccountName, id, None, None))), None), nino, taxYear)
            insertInterestCyaData(Some(InterestCYAModel(None, None, Some(true), Seq(
              InterestAccountModel(None,"name",taxedAmount = Some(1234),uniqueSessionId = Some("1234567890"))
            ))))
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(url("1234567890"), Map(
              TaxedInterestAmountForm.taxedAmount -> "12,344.98",
              TaxedInterestAmountForm.taxedAccountName -> firstAccountName
            ), us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          s"return a SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("Location").get should include(
              s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/accounts-with-taxed-uk-interest")
          }
        }

        "a new account is added and old account removes the taxed amount" should {

          lazy val result: WSResponse = {
            dropInterestDB()
            emptyUserDataStub()
            insertInterestCyaData(Some(InterestCYAModel(None, None, Some(true), Seq(
              InterestAccountModel(None,"name",Some(1234),Some(1234),uniqueSessionId = Some("1234567890"))
            ))))
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(url("1234567890"), Map(
              TaxedInterestAmountForm.taxedAmount -> "12,344.98",
              TaxedInterestAmountForm.taxedAccountName -> firstAccountName
            ), us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          s"return a SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("Location").get should include(
              s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/accounts-with-taxed-uk-interest")

            val data = findInterestDb
            data.head shouldBe InterestCYAModel(None, None, Some(true), Seq(
              InterestAccountModel(None,firstAccountName,taxedAmount = Some(12344.98),uniqueSessionId = data.head.accounts.head.uniqueSessionId),
              InterestAccountModel(None,"name",Some(1234),None,uniqueSessionId = Some("1234567890"))
            ))
          }
        }

        "a new account is added" should {

          lazy val result: WSResponse = {
            dropInterestDB()
            emptyUserDataStub()
            insertInterestCyaData(Some(InterestCYAModel(None, None, Some(true))))
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(url(id), Map(
              TaxedInterestAmountForm.taxedAmount -> "12,344.98",
              TaxedInterestAmountForm.taxedAccountName -> firstAccountName
            ), us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          s"return a SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("Location").get should include(
              s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/accounts-with-taxed-uk-interest")

            val data = findInterestDb
            data.head shouldBe InterestCYAModel(None, None,Some(true),List(
              InterestAccountModel(None,firstAccountName,taxedAmount = Some(12344.98),uniqueSessionId = data.head.accounts.head.uniqueSessionId)
            ))
          }
        }

        "an account is reused and amount is updated even if the id is different" should {

          lazy val result: WSResponse = {
            dropInterestDB()
            userDataStub(IncomeSourcesModel(None, Some(Seq(InterestModel(firstAccountName, id, None, None))), None), nino, taxYear)
            insertInterestCyaData(Some(InterestCYAModel(None, None, Some(true))))
            authoriseAgentOrIndividual(us.isAgent)
            urlPost(url("id"), Map(
              TaxedInterestAmountForm.taxedAmount -> "12,344.98",
              TaxedInterestAmountForm.taxedAccountName -> firstAccountName
            ), us.isWelsh, follow = false, playSessionCookie(us.isAgent))
          }

          s"return a SEE OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.header("Location").get should include(
              s"/update-and-submit-income-tax-return/personal-income/$taxYear/interest/accounts-with-taxed-uk-interest")
          }
        }
      }
    }
  }
}