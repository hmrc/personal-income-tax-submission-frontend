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

import common.SessionValues
import forms.interest.TaxedInterestAmountForm
import helpers.PlaySessionCookieBaker
import models.interest.{InterestAccountModel, InterestCYAModel}
import models.priorDataModels.{IncomeSourcesModel, InterestModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, UNAUTHORIZED, SEE_OTHER}
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{IntegrationTest, InterestDatabaseHelper, ViewHelpers}

import java.util.UUID

class TaxedInterestAmountControllerISpec extends IntegrationTest with ViewHelpers with InterestDatabaseHelper {

  object Selectors {
    val accountName = "#main-content > div > div > form > div:nth-child(3) > label > div"
    val interestEarned = "#main-content > div > div > form > div:nth-child(4) > label > div"
    val accountNameInput = "#taxedAccountName"
    val eachAccount = "#main-content > div > div > form > div:nth-child(3) > label > p"
    val amountInput = "#taxedAmount"

    val errorSummary = "#error-summary-title"
    val firstError = ".govuk-error-summary__body > ul > li:nth-child(1) > a"
    val secondError = ".govuk-error-summary__body > ul > li:nth-child(2) > a"
    val errorMessage = "#value-error"
  }

  object Content {
    val heading = "Add an account with taxed UK interest"
    val caption = "Interest for 6 April 2021 to 5 April 2022"
    val accountName = "What do you want to name this account?"
    val eachAccount = "Give each account a different name."
    val interestEarned = "Amount of taxed UK interest"
    val hint = "For example, ‘HSBC savings account’. " + "For example, £600 or £193.54"
    val button = "Continue"

    val noNameEntryError = "Enter a name for this account"
    val invalidCharEntry: String = "Name of account with taxed UK interest must only include numbers 0-9, " +
      "letters a to z, hyphens, spaces, apostrophes, commas, full stops, round brackets, and the special characters &, /, @, £, *"
    val nameTooLongError = "The name of the account must be 32 characters or fewer"
    val duplicateNameError = "You cannot add 2 accounts with the same name"

    val noAmountEntryErrorIndividual = "Enter the amount of taxed UK interest you got"
    val noAmountEntryErrorAgent = "Enter the amount of taxed UK interest your client got"
    val tooMuchMoneyError = "The amount of taxed UK interest must be less than £100,000,000,000"
    val incorrectFormatError = "Enter the amount of taxed UK interest in the correct format"

    val errorTitle = s"Error: $heading"

    val changeAmountPageTitle = "How much taxed UK interest did you get?"
  }

  object WelshContent {
    val heading = "Add an account with taxed UK interest"
    val caption = "Interest for 6 April 2021 to 5 April 2022"
    val accountName = "What do you want to name this account?"
    val interestEarned = "Amount of taxed UK interest"
    val hint = "For example, ‘HSBC savings account’. " + "For example, £600 or £193.54"
    val button = "Continue"

    val noNameEntryError = "Enter a name for this account"
    val invalidCharEntry: String = "Name of account with taxed UK interest must only include numbers 0-9, " +
      "letters a to z, hyphens, spaces, apostrophes, commas, full stops, round brackets, and the special characters &, /, @, £, *"
    val nameTooLongError = "The name of the account must be 32 characters or fewer"
    val duplicateNameError = "You cannot add 2 accounts with the same name"

    val noAmountEntryErrorIndividual = "Enter the amount of taxed UK interest you got"
    val noAmountEntryErrorAgent = "Enter the amount of taxed UK interest your client got"
    val tooMuchMoneyError = "The amount of taxed UK interest must be less than £100,000,000,000"
    val incorrectFormatError = "Enter the amount of taxed UK interest in the correct format"

    val errorTitle = s"Error: $heading"
  }

  val taxYear: Int = 2022

  val amount: BigDecimal = 25
  val accountName: String = "HSBC"

  lazy val id: String = UUID.randomUUID().toString

  def url(newId: String): String = s"$appUrl/$taxYear/interest/add-taxed-uk-interest-account/$newId"

  val charLimit: String = "ukHzoBYHkKGGk2V5iuYgS137gN7EB7LRw3uDjvujYg00ZtHwo3sokyOOCEoAK9vuPiP374QKOelo"

  def taxedInterestAmountUrl(newId: String): String = s"$appUrl/$taxYear/interest/add-taxed-uk-interest-account/$newId"
  s"Calling GET /interest/taxed-uk-interest-details/$id" when {

    "the user is authorised" when {

      "the user is a non-agent" should {
        lazy val interestCYA = InterestCYAModel(
          Some(false), None, Some(true), Some(Seq(
            InterestAccountModel(Some("differentId"), accountName, amount),
            InterestAccountModel(None, accountName, amount, Some(id))
          ))
        )

        lazy val result = {
          dropInterestDB()

          emptyUserDataStub()
          insertCyaData(Some(interestCYA))

          authoriseIndividual()
          await(
            wsClient.url(taxedInterestAmountUrl(id))
              .withHttpHeaders(xSessionId, csrfContent)
              .get()
          )
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return the page" which {
          titleCheck(Content.heading)
          welshToggleCheck("English")
          h1Check(s"${Content.heading} ${Content.caption}")
          captionCheck(Content.caption)
          textOnPageCheck(Content.accountName, Selectors.accountName)
          textOnPageCheck(Content.eachAccount, Selectors.eachAccount)
          inputFieldCheck(TaxedInterestAmountForm.taxedAccountName, Selectors.accountNameInput)
          textOnPageCheck(Content.interestEarned, Selectors.interestEarned)
          hintTextCheck(Content.hint)
          inputFieldCheck(TaxedInterestAmountForm.taxedAmount, Selectors.amountInput)
          buttonCheck(Content.button)

          elementExtinct(Selectors.errorSummary)
          elementExtinct(Selectors.firstError)
          elementExtinct(Selectors.errorMessage)
        }

        s"have an OK($OK) status" in {
          result.status shouldBe OK
        }
      }

      "the user is an agent" should {

        lazy val interestCYA = InterestCYAModel(
          Some(false), None, Some(true), Some(Seq(
            InterestAccountModel(Some("differentId"), accountName, amount),
            InterestAccountModel(None, accountName, amount, Some(id))
          ))
        )

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A")
        )

        lazy val result: WSResponse = {
          dropInterestDB()

          emptyUserDataStub()
          insertCyaData(Some(interestCYA))

          authoriseAgent()
          await(wsClient.url(taxedInterestAmountUrl(id))
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
            .get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return the page" which {
          titleCheck(Content.heading)
          welshToggleCheck("English")
          h1Check(s"${Content.heading} ${Content.caption}")
          captionCheck(Content.caption)
          textOnPageCheck(Content.accountName, Selectors.accountName)
          textOnPageCheck(Content.eachAccount, Selectors.eachAccount)
          inputFieldCheck(TaxedInterestAmountForm.taxedAccountName, Selectors.accountNameInput)
          textOnPageCheck(Content.interestEarned, Selectors.interestEarned)
          hintTextCheck(Content.hint)
          inputFieldCheck(TaxedInterestAmountForm.taxedAmount, Selectors.amountInput)
          buttonCheck(Content.button)

          elementExtinct(Selectors.errorSummary)
          elementExtinct(Selectors.firstError)
          elementExtinct(Selectors.errorMessage)
        }

        s"have an OK($OK) status" in {
          result.status shouldBe OK
        }
      }

      "the user has welsh selected" should {
        lazy val interestCYA = InterestCYAModel(
          Some(false), None, Some(true), Some(Seq(
            InterestAccountModel(Some("differentId"), accountName, amount),
            InterestAccountModel(None, accountName, amount, Some(id))
          ))
        )

        lazy val result = {
          dropInterestDB()

          emptyUserDataStub()
          insertCyaData(Some(interestCYA))

          authoriseIndividual()
          await(wsClient.url(taxedInterestAmountUrl(id)).withHttpHeaders(
            xSessionId, csrfContent, HeaderNames.ACCEPT_LANGUAGE -> "cy"
          ).get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return the page" which {
          titleCheck(WelshContent.heading)
          welshToggleCheck("Welsh")
          h1Check(s"${WelshContent.heading} ${WelshContent.caption}")
          captionCheck(WelshContent.caption)
          textOnPageCheck(WelshContent.accountName, Selectors.accountName)
          textOnPageCheck(Content.eachAccount, Selectors.eachAccount)
          inputFieldCheck(TaxedInterestAmountForm.taxedAccountName, Selectors.accountNameInput)
          textOnPageCheck(WelshContent.interestEarned, Selectors.interestEarned)
          hintTextCheck(WelshContent.hint)
          inputFieldCheck(TaxedInterestAmountForm.taxedAmount, Selectors.amountInput)
          buttonCheck(WelshContent.button)

          elementExtinct(Selectors.errorSummary)
          elementExtinct(Selectors.firstError)
          elementExtinct(Selectors.errorMessage)
        }

        s"have an OK($OK) status" in {
          result.status shouldBe OK
        }
      }

      "the id is not a UUID" which {
        lazy val interestCYA = InterestCYAModel(Some(false), None, Some(true), None)

        lazy val result = {
          dropInterestDB()

          emptyUserDataStub()
          insertCyaData(Some(interestCYA))

          authoriseIndividual()
          await(wsClient.url(taxedInterestAmountUrl(id)).withHttpHeaders(xSessionId, csrfContent).get())
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(Content.heading)

        "id in url is UUID" in {
          UUID.fromString(result.uri.toString.split("/").last)
        }
      }

      "redirect to the change amount page" when {

        "the id matches a prior submission" which {

          lazy val interestCYA = InterestCYAModel(
            Some(false),
            None,
            Some(true),
            Some(Seq(
              InterestAccountModel(Some(id), accountName, amount)))
          )

          lazy val priorData = Some(Seq(
            InterestModel(accountName, id, Some(amount), None)
          ))

          lazy val result = {
            dropInterestDB()

            userDataStub(IncomeSourcesModel(interest = priorData), nino, taxYear)
            insertCyaData(Some(interestCYA))

            authoriseIndividual()
            await(wsClient.url(taxedInterestAmountUrl(id)).withHttpHeaders(xSessionId, csrfContent).get())
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(Content.changeAmountPageTitle)
        }
      }

      "redirect to the overview page" when {

        "there is no cya data in session" which {
          lazy val result = {
            dropInterestDB()

            emptyUserDataStub()
            insertCyaData(None)

            authoriseIndividual()
            await(
              wsClient.url(taxedInterestAmountUrl(id))
                .withFollowRedirects(false)
                .withHttpHeaders(xSessionId, csrfContent)
                .get()
            )
          }

          "has a SEE_OTHER(303) status" in {
            result.status shouldBe SEE_OTHER
          }

          "redirects to the overview page" in {
            result.headers("Location").head shouldBe "http://localhost:11111/income-through-software/return/2022/view"
          }
        }
      }
    }

    "the user is unauthorized" should {

      lazy val result = {
        dropInterestDB()

        authoriseIndividualUnauthorized()
        await(wsClient.url(taxedInterestAmountUrl(id)).get())
      }

      s"return an Unauthorised($UNAUTHORIZED) status" in {
        result.status shouldBe UNAUTHORIZED
      }
    }
  }

  s"Calling POST /interest/add-taxed-uk-interest-account/$id" when {

    "the user is authorised" when {

      lazy val interestCYA = InterestCYAModel(
        Some(false), None, Some(true), Some(Seq(
          InterestAccountModel(Some("differentId"), accountName, amount),
          InterestAccountModel(None, accountName, amount, Some(id))
        ))
      )

      def response(formMap: Map[String, String]): WSResponse = {
        dropInterestDB()

        emptyUserDataStub()
        insertCyaData(Some(interestCYA))

        authoriseIndividual()
        await(wsClient.url(taxedInterestAmountUrl(id))
          .withHttpHeaders(xSessionId, csrfContent)
          .post(formMap))
      }

      "an account name and amount are entered correctly" should {

        lazy val result = {
          response(Map(
            TaxedInterestAmountForm.taxedAmount -> "67.66",
            TaxedInterestAmountForm.taxedAccountName -> "Santander"
          ))
        }

        "return a 200(Ok) status" in {
          result.status shouldBe OK
        }
      }

      "the fields are empty as an individual" should {

        lazy val result = response(Map(TaxedInterestAmountForm.taxedAmount -> "",
          TaxedInterestAmountForm.taxedAccountName -> ""))
        implicit def document: () => Document = () => Jsoup.parse(result.body)

        s"return a 400(BadRequest) status" in {
          result.status shouldBe BAD_REQUEST
        }

        multipleErrorCheck(
          List(
            (Content.noNameEntryError, Selectors.accountNameInput),
            (Content.noAmountEntryErrorIndividual, Selectors.amountInput)
          )
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
            (Content.invalidCharEntry, Selectors.accountNameInput),
            (Content.incorrectFormatError, Selectors.amountInput)
          )
        )
      }

      "the account name is too long and the amount is too great" should {
        lazy val result = response(Map(TaxedInterestAmountForm.taxedAmount -> "100000000000",
          TaxedInterestAmountForm.taxedAccountName -> "SuperAwesomeBigBusinessMoneyStash"))

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        s"return a 400(BadRequest) status" in {
          result.status shouldBe BAD_REQUEST
        }

        multipleErrorCheck(
          List(
            (Content.nameTooLongError, Selectors.accountNameInput),
            (Content.tooMuchMoneyError, Selectors.amountInput)
          )
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
        inputFieldValueCheck("", Selectors.amountInput)
        titleCheck(Content.errorTitle)

        errorSummaryCheck(Content.incorrectFormatError, Selectors.amountInput)
        errorAboveElementCheck(Content.incorrectFormatError)
      }

      "a duplicate account name is entered" should {
        lazy val result = response(Map(
          TaxedInterestAmountForm.taxedAmount -> "12344.98",
          TaxedInterestAmountForm.taxedAccountName -> accountName
        ))

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        s"return a 400(BadRequest) status" in {
          result.status shouldBe BAD_REQUEST
        }

        inputFieldValueCheck(accountName, Selectors.accountNameInput)
        inputFieldValueCheck("12344.98", Selectors.amountInput)
        titleCheck(Content.errorTitle)

        errorSummaryCheck(Content.duplicateNameError, Selectors.accountNameInput)
        errorAboveElementCheck(Content.duplicateNameError)
      }
    }

    "the user is authorised as an agent" when {

      lazy val interestCYA = InterestCYAModel(
        Some(false), None, Some(true), Some(Seq(
          InterestAccountModel(Some("differentId"), accountName, amount),
          InterestAccountModel(None, accountName, amount, Some(id))
        ))
      )
      lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
        SessionValues.CLIENT_MTDITID -> "1234567890",
        SessionValues.CLIENT_NINO -> "AA123456A"
      ))

      def response(formMap: Map[String, String]): WSResponse = {
        dropInterestDB()

        insertCyaData(Some(interestCYA))
        emptyUserDataStub()

        authoriseAgent()
        await(wsClient.url(taxedInterestAmountUrl(id))
          .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
          .post(formMap))
      }

      "the fields are empty as an agent" should {

        lazy val result = response(Map(TaxedInterestAmountForm.taxedAmount -> "",
          TaxedInterestAmountForm.taxedAccountName -> ""))

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        s"return a 400(BadRequest) status" in {
          result.status shouldBe BAD_REQUEST
        }

        multipleErrorCheck(
          List(
            (Content.noNameEntryError, Selectors.accountNameInput),
            (Content.noAmountEntryErrorAgent, Selectors.amountInput)
          )
        )
      }
    }

    "the user has Welsh toggled" when {

      lazy val interestCYA = InterestCYAModel(
        Some(false), None, Some(true), Some(Seq(
          InterestAccountModel(Some("differentId"), accountName, amount),
          InterestAccountModel(None, accountName, amount, Some(id))
        ))
      )

      def response(formMap: Map[String, String]): WSResponse = {
        dropInterestDB()

        emptyUserDataStub()
        insertCyaData(Some(interestCYA))

        authoriseIndividual()
        await(wsClient.url(taxedInterestAmountUrl(id))
          .withHttpHeaders(xSessionId, csrfContent, HeaderNames.ACCEPT_LANGUAGE -> "cy")
          .post(formMap))
      }

      "an account name and amount are entered correctly" should {

        lazy val result = response(Map(TaxedInterestAmountForm.taxedAmount -> "67.66",
          TaxedInterestAmountForm.taxedAccountName -> "Santander"))

        "return a 200(Ok) status" in {
          result.status shouldBe OK
        }
      }

      "the fields are empty" should {

        lazy val result = response(Map(
          TaxedInterestAmountForm.taxedAmount -> "",
          TaxedInterestAmountForm.taxedAccountName -> ""
        ))

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        s"return a 400(BadRequest) status" in {
          result.status shouldBe BAD_REQUEST
        }

        multipleErrorCheck(
          List(
            (WelshContent.noNameEntryError, Selectors.accountNameInput),
            (WelshContent.noAmountEntryErrorIndividual, Selectors.amountInput)
          )
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
            (WelshContent.invalidCharEntry, Selectors.accountNameInput),
            (WelshContent.incorrectFormatError, Selectors.amountInput)
          )
        )
      }

      "the account name is too long and the amount is too great" should {
        lazy val result = response(Map(TaxedInterestAmountForm.taxedAmount -> "100000000000",
          TaxedInterestAmountForm.taxedAccountName -> "SuperAwesomeBigBusinessMoneyStash"))

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        s"return a 400(BadRequest) status" in {
          result.status shouldBe BAD_REQUEST
        }

        multipleErrorCheck(
          List(
            (WelshContent.nameTooLongError, Selectors.accountNameInput),
            (WelshContent.tooMuchMoneyError, Selectors.amountInput)
          )
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
        inputFieldValueCheck("", Selectors.amountInput)
        titleCheck(WelshContent.errorTitle)

        errorSummaryCheck(WelshContent.incorrectFormatError, Selectors.amountInput)
        errorAboveElementCheck(WelshContent.incorrectFormatError)
      }

      "a duplicate account name is entered" should {
        lazy val result = response(Map(
          TaxedInterestAmountForm.taxedAmount -> "12344.98",
          TaxedInterestAmountForm.taxedAccountName -> accountName
        ))

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        s"return a 400(BadRequest) status" in {
          result.status shouldBe BAD_REQUEST
        }

        inputFieldValueCheck(accountName, Selectors.accountNameInput)
        inputFieldValueCheck("12344.98", Selectors.amountInput)
        titleCheck(WelshContent.errorTitle)

        errorSummaryCheck(WelshContent.duplicateNameError, Selectors.accountNameInput)
        errorAboveElementCheck(WelshContent.duplicateNameError)
      }
    }
  }
}