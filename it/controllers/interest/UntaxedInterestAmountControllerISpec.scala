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
import forms.interest.UntaxedInterestAmountForm
import helpers.PlaySessionCookieBaker
import models.interest.{InterestAccountModel, InterestCYAModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, OK, UNAUTHORIZED}
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{IntegrationTest, ViewHelpers}
import java.util.UUID

import forms.interest.UntaxedInterestAmountForm

class UntaxedInterestAmountControllerISpec extends IntegrationTest with ViewHelpers {

  val amount: BigDecimal = 25
  val accountName: String = "HSBC"
  val taxYear: Int = 2022
  lazy val id: String = UUID.randomUUID().toString

  object Selectors {
    val accountName = "#main-content > div > div > form > div:nth-child(3) > label"
    val interestEarned = "#main-content > div > div > form > div:nth-child(4) > label"
    val accountNameInput = "#untaxedAccountName"
    val amountInput = "#untaxedAmount"

    val errorSummary = "#error-summary-title"
    val firstError = ".govuk-error-summary__body > ul > li:nth-child(1) > a"
    val secondError = ".govuk-error-summary__body > ul > li:nth-child(2) > a"
    val errorMessage = "#value-error"
  }

  object Content {
    val heading = "UK untaxed interest account details"
    val caption = "Interest for 6 April 2021 to 5 April 2022"
    val accountName = "What would you like to call this account?"
    val interestEarned = "Amount of interest earned"
    val hint = "For example, £600 or £193.54"
    val button = "Continue"

    val noNameEntryError = "Enter an account name"
    val invalidCharEntry: String = "Name of account with untaxed UK interest must only include numbers 0-9, " +
      "letters a to z, hyphens, spaces, apostrophes, commas, full stops, round brackets, and the special characters &, /, @, £, *"
    val nameTooLongError = "The name of the account must be 32 characters or fewer"
    val duplicateNameError = "You cannot add 2 accounts with the same name"

    val noAmountEntryError = "Enter the amount of untaxed interest earned"
    val invalidNumericError = "Enter an amount using numbers 0 to 9"
    val tooMuchMoneyError = "Enter an amount less than £100,000,000,000"
    val incorrectFormatError = "Enter the amount in the correct format"

    val errorTitle = s"Error: $heading"

    val changeAmountPageTitle = "How much untaxed UK interest did you get?"
  }

  object WelshContent {
    val heading = "UK untaxed interest account details"
    val caption = "Interest for 6 April 2021 to 5 April 2022"
    val accountName = "What would you like to call this account?"
    val interestEarned = "Amount of interest earned"
    val hint = "For example, £600 or £193.54"
    val button = "Continue"

    val noNameEntryError = "Enter an account name"
    val invalidCharEntry: String = "Name of account with untaxed UK interest must only include numbers 0-9, " +
      "letters a to z, hyphens, spaces, apostrophes, commas, full stops, round brackets, and the special characters &, /, @, £, *"
    val nameTooLongError = "The name of the account must be 32 characters or fewer"
    val duplicateNameError = "You cannot add 2 accounts with the same name"

    val noAmountEntryError = "Enter the amount of untaxed interest earned"
    val invalidNumericError = "Enter an amount using numbers 0 to 9"
    val tooMuchMoneyError = "Enter an amount less than £100,000,000,000"
    val incorrectFormatError = "Enter the amount in the correct format"

    val errorTitle = s"Error: $heading"
  }



  def untaxedInterestAmountUrl(newId: String): String = s"$startUrl/$taxYear/interest/untaxed-uk-interest-details/$newId"

  s"Calling GET /interest/untaxed-uk-interest-details/$id" when {

    "the user is authorised" when {

      "the user is a non-agent" should {

        lazy val interestCYA = InterestCYAModel(
          Some(true), Some(Seq(
            InterestAccountModel(Some("differentId"), accountName, amount),
            InterestAccountModel(None, accountName, amount, Some(id))
          )),
          Some(false), None
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(untaxedInterestAmountUrl(id)).withHttpHeaders(
            HeaderNames.COOKIE -> sessionCookie
          ).get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return the page" which {
          titleCheck(Content.heading)
          welshToggleCheck("English")
          h1Check(s"${Content.heading} ${Content.caption}")
          captionCheck(Content.caption)
          textOnPageCheck(Content.accountName, Selectors.accountName)
          inputFieldCheck(UntaxedInterestAmountForm.untaxedAccountName, Selectors.accountNameInput)
          textOnPageCheck(Content.interestEarned, Selectors.interestEarned)
          hintTextCheck(Content.hint)
          inputFieldCheck(UntaxedInterestAmountForm.untaxedAmount, Selectors.amountInput)
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
          Some(true), Some(Seq(
            InterestAccountModel(Some("differentId"), accountName, amount),
            InterestAccountModel(None, accountName, amount, Some(id))
          )),
          Some(false), None
        )

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA)),
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A")
        )

        lazy val result: WSResponse = {
          authoriseAgent()
          await(wsClient.url(untaxedInterestAmountUrl(id))
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie)
            .get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return the page" which {
          titleCheck(Content.heading)
          welshToggleCheck("English")
          h1Check(s"${Content.heading} ${Content.caption}")
          captionCheck(Content.caption)
          textOnPageCheck(Content.accountName, Selectors.accountName)
          inputFieldCheck(UntaxedInterestAmountForm.untaxedAccountName, Selectors.accountNameInput)
          textOnPageCheck(Content.interestEarned, Selectors.interestEarned)
          hintTextCheck(Content.hint)
          inputFieldCheck(UntaxedInterestAmountForm.untaxedAmount, Selectors.amountInput)
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
          Some(true), Some(Seq(
            InterestAccountModel(Some("differentId"), accountName, amount),
            InterestAccountModel(None, accountName, amount, Some(id))
          )),
          Some(false), None
        )
        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(untaxedInterestAmountUrl(id)).withHttpHeaders(
            HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy"
          ).get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "return the page" which {
          titleCheck(WelshContent.heading)
          welshToggleCheck("Welsh")
          h1Check(s"${WelshContent.heading} ${WelshContent.caption}")
          captionCheck(WelshContent.caption)
          textOnPageCheck(WelshContent.accountName, Selectors.accountName)
          inputFieldCheck(UntaxedInterestAmountForm.untaxedAccountName, Selectors.accountNameInput)
          textOnPageCheck(WelshContent.interestEarned, Selectors.interestEarned)
          hintTextCheck(WelshContent.hint)
          inputFieldCheck(UntaxedInterestAmountForm.untaxedAmount, Selectors.amountInput)
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

        lazy val interestCYA = InterestCYAModel(Some(true), None, Some(false), None)

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(untaxedInterestAmountUrl(id)).withHttpHeaders(HeaderNames.COOKIE -> sessionCookie).get())
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
            Some(true),
            Some(Seq(
              InterestAccountModel(Some(id), accountName, amount))),
            Some(false),
            None
          )

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA)),
            SessionValues.INTEREST_PRIOR_SUB -> Json.arr(
              Json.obj(
                "accountName" -> accountName,
                "incomeSourceId" -> id,
                "untaxedUkInterest" -> amount
              )
            ).toString()
          ))

          lazy val result = {
            authoriseIndividual()
            await(wsClient.url(untaxedInterestAmountUrl(id)).withHttpHeaders(HeaderNames.COOKIE -> sessionCookie).get())
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(Content.changeAmountPageTitle)

        }
      }

      "redirect to the overview page" when {

        "there is no cya data in session" in {
          {
            authoriseIndividual()
            await(wsClient.url(untaxedInterestAmountUrl(id)).get())
          }

          stubGet("/income-through-software/return/2022/view", 303, "")
          verifyGet("/income-through-software/return/2022/view")
          wireMockServer.resetAll()

        }
      }
    }

    "the user is unauthorized" should {

      lazy val result = {
        authoriseIndividualUnauthorized()
        await(wsClient.url(untaxedInterestAmountUrl(id)).get())
      }

      s"return an Unauthorised($UNAUTHORIZED) status" in {
        result.status shouldBe UNAUTHORIZED
      }
    }
  }

  s"Calling POST /interest/untaxed-uk-interest-details/$id" when {

    "the user is authorised" when {

      lazy val interestCYA = InterestCYAModel(
        Some(true), Some(Seq(
          InterestAccountModel(Some("differentId"), accountName, amount),
          InterestAccountModel(None, accountName, amount, Some(id))
        )),
        Some(false), None
      )
      lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
        SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
      ))

      def response(formMap: Map[String, String]): WSResponse = {
        authoriseIndividual()
        await(wsClient.url(untaxedInterestAmountUrl(id))
          .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
          .post(formMap))
      }

      "an account name and amount are entered correctly" should {

        lazy val result = response(Map(UntaxedInterestAmountForm.untaxedAmount -> "67.66",
          UntaxedInterestAmountForm.untaxedAccountName -> "Santander"))

        "return a 200(Ok) status" in {
          result.status shouldBe OK
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
            (Content.noNameEntryError, Selectors.accountNameInput),
            (Content.noAmountEntryError, Selectors.amountInput)
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
            (Content.invalidCharEntry, Selectors.accountNameInput),
            (Content.invalidNumericError, Selectors.amountInput)
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
            (Content.nameTooLongError, Selectors.accountNameInput),
            (Content.tooMuchMoneyError, Selectors.amountInput)
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
        titleCheck(Content.errorTitle)

        errorSummaryCheck(Content.incorrectFormatError, Selectors.amountInput)
        errorAboveElementCheck(Content.incorrectFormatError)
      }

      "a duplicate account name is entered" should {
        lazy val result = response(Map(
          UntaxedInterestAmountForm.untaxedAmount -> "12344.98",
          UntaxedInterestAmountForm.untaxedAccountName -> accountName
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

    "the user has Welsh toggled" when {

      lazy val interestCYA = InterestCYAModel(
        Some(true), Some(Seq(
          InterestAccountModel(Some("differentId"), accountName, amount),
          InterestAccountModel(None, accountName, amount, Some(id))
        )),
        Some(false), None
      )
      lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
        SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
      ))

      def response(formMap: Map[String, String]): WSResponse = {
        authoriseIndividual()
        await(wsClient.url(untaxedInterestAmountUrl(id))
          .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck", HeaderNames.ACCEPT_LANGUAGE -> "cy")
          .post(formMap))
      }

      "an account name and amount are entered correctly" should {

        lazy val result = response(Map(UntaxedInterestAmountForm.untaxedAmount -> "67.66",
          UntaxedInterestAmountForm.untaxedAccountName -> "Santander"))

        "return a 200(Ok) status" in {
          result.status shouldBe OK
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
            (WelshContent.noNameEntryError, Selectors.accountNameInput),
            (WelshContent.noAmountEntryError, Selectors.amountInput)
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
            (WelshContent.invalidCharEntry, Selectors.accountNameInput),
            (WelshContent.invalidNumericError, Selectors.amountInput)
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
            (WelshContent.nameTooLongError, Selectors.accountNameInput),
            (WelshContent.tooMuchMoneyError, Selectors.amountInput)
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
        titleCheck(WelshContent.errorTitle)

        errorSummaryCheck(WelshContent.incorrectFormatError, Selectors.amountInput)
        errorAboveElementCheck(WelshContent.incorrectFormatError)
      }

      "a duplicate account name is entered" should {
        lazy val result = response(Map(
          UntaxedInterestAmountForm.untaxedAmount -> "12344.98",
          UntaxedInterestAmountForm.untaxedAccountName -> accountName
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