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
import controllers.Assets.BAD_REQUEST
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

class UntaxedInterestAmountControllerISpec extends IntegrationTest with ViewHelpers {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val taxYear: Int = 2022
  val taxYearMinusOne: Int = taxYear - 1

  object IndividualExpected {
    val expectedTitle: String = "UK untaxed interest account details"
    val changeAmountPageTitle = "How much untaxed UK interest did you get?"
    val expectedH1: String = "UK untaxed interest account details"
    val expectedWhatWouldYouCallText = "What would you like to call this account?"
    val expectedAmountInterestText = "Amount of interest earned"

    val expectedNameEmptyError: String = "Enter an account name"
    val expectedNameCharLimitError: String = "The name of the account must be 32 characters or fewer"
    val expectedNameInvalidCharError: String = "Name of account must only include numbers 0-9, letters a to z, hyphens," +
      " spaces, apostrophes, commas, full stops, round brackets, and the special characters &, /, @, £, *"
    val expectedNameDuplicateError: String = "You cannot add 2 accounts with the same name"

    val expectedAmountEmptyError: String = "Enter the amount of untaxed interest earned"
    val expectedAmountMaxValueError: String = "Enter an amount less than £100,000,000,000"
    val expectedAmountInvalidCharError: String = "Enter an amount using numbers 0 to 9"

    val expectedErrorTitle: String = s"Error: $expectedTitle"

    val expectedTitleCy: String = "UK untaxed interest account details"
    val changeAmountPageTitleCy: String = "How much untaxed UK interest did you get?"
    val expectedH1Cy: String = "UK untaxed interest account details"
    val expectedWhatWouldYouCallTextCy = "What would you like to call this account?"
    val expectedAmountInterestTextCy = "Amount of interest earned"

    val expectedNameEmptyErrorCy: String = "Enter an account name"
    val expectedNameCharLimitErrorCy: String = "The name of the account must be 32 characters or fewer"
    val expectedNameInvalidCharErrorCy: String = "Name of account must only include numbers 0-9, letters a to z, hyphens," +
      " spaces, apostrophes, commas, full stops, round brackets, and the special characters &, /, @, £, *"
    val expectedNameDuplicateErrorCy: String = "You cannot add 2 accounts with the same name"

    val expectedAmountEmptyErrorCy: String = "Enter the amount of untaxed interest earned"
    val expectedAmountMaxValueErrorCy: String = "Enter an amount less than £100,000,000,000"
    val expectedAmountInvalidCharErrorCy: String = "Enter an amount using numbers 0 to 9"

    val expectedErrorTitleCy: String = s"Error: $expectedTitle"
  }

  object AgentExpected {
    val expectedTitle: String = "UK untaxed interest account details"
    val changeAmountPageTitle: String = "How much untaxed UK interest did you get?"
    val expectedH1: String = "UK untaxed interest account details"
    val expectedWhatWouldYouCallText = "What would you like to call this account?"
    val expectedAmountInterestText = "Amount of interest earned"

    val expectedNameEmptyError: String = "Enter an account name"
    val expectedNameCharLimitError: String = "The name of the account must be 32 characters or fewer"
    val expectedNameInvalidCharError: String = "Name of account must only include numbers 0-9, letters a to z, hyphens," +
      " spaces, apostrophes, commas, full stops, round brackets, and the special characters &, /, @, £, *"
    val expectedNameDuplicateError: String = "You cannot add 2 accounts with the same name"

    val expectedAmountEmptyError: String = "Enter the amount of untaxed interest earned"
    val expectedAmountMaxValueError: String = "Enter an amount less than £100,000,000,000"
    val expectedAmountInvalidCharError: String = "Enter an amount using numbers 0 to 9"

    val expectedErrorTitle: String = s"Error: $expectedTitle"

    val expectedTitleCy: String = "UK untaxed interest account details"
    val changeAmountPageTitleCy: String = "How much untaxed UK interest did you get?"
    val expectedH1Cy: String = "UK untaxed interest account details"
    val expectedWhatWouldYouCallTextCy = "What would you like to call this account?"
    val expectedAmountInterestTextCy = "Amount of interest earned"

    val expectedNameEmptyErrorCy: String = "Enter an account name"
    val expectedNameCharLimitErrorCy: String = "The name of the account must be 32 characters or fewer"
    val expectedNameInvalidCharErrorCy: String = "Name of account must only include numbers 0-9, letters a to z, hyphens," +
      " spaces, apostrophes, commas, full stops, round brackets, and the special characters &, /, @, £, *"
    val expectedNameDuplicateErrorCy: String = "You cannot add 2 accounts with the same name"

    val expectedAmountEmptyErrorCy: String = "Enter the amount of untaxed interest earned"
    val expectedAmountMaxValueErrorCy: String = "Enter an amount less than £100,000,000,000"
    val expectedAmountInvalidCharErrorCy: String = "Enter an amount using numbers 0 to 9"

    val expectedErrorTitleCy: String = s"Error: $expectedTitle"
  }

  val expectedCaption = s"Interest for 6 April $taxYearMinusOne to 5 April $taxYear"
  val expectedButtonText = "Continue"
  val expectedInputLabelText = "Total amount for the year"
  val expectedInputHintText = "For example, £600 or £193.54"

  val expectedCaptionCy = s"Interest for 6 April $taxYearMinusOne to 5 April $taxYear"
  val expectedButtonTextCy = "Continue"
  val expectedInputLabelTextCy = "Total amount for the year"
  val expectedInputHintTextCy = "For example, £600 or £193.54"

  val buttonSelector = ".govuk-button"
  val inputHintTextSelector = ".govuk-hint"
  val captionSelector = ".govuk-caption-l"

  val whatWouldYouCallSelector = "#main-content > div > div > form > div:nth-child(3) > label"
  val amountInterestSelector = "#main-content > div > div > form > div:nth-child(4) > label"

  val accountNameSelector = "#untaxedAccountName"
  val amountSelector = "#untaxedAmount"

  val amount: BigDecimal = 25
  val accountName: String = "HSBC"

  lazy val id: String = UUID.randomUUID().toString

  val emptyPreviousNames = Seq("")

  def url(newId: String): String = s"$startUrl/$taxYear/interest/untaxed-uk-interest-details/$newId"

  val charLimit: String = "ukHzoBYHkKGGk2V5iuYgS137gN7EB7LRw3uDjvujYg00ZtHwo3sokyOOCEoAK9vuPiP374QKOelo"

  "calling /GET" should {

    import IndividualExpected._

    "render the untaxed interest amount page" when {

      "the id is unique and is also a UUID" which {

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
          await(wsClient.url(url(id)).withHttpHeaders(HeaderNames.COOKIE -> sessionCookie).get())
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedTitle)
        inputFieldValueCheck(accountName, accountNameSelector)
        inputFieldValueCheck(amount.toString(), amountSelector)

      }

      "the id is not a UUID" which {

        lazy val interestCYA = InterestCYAModel(Some(true), None, Some(false), None)

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
        ))

        lazy val result = {
          authoriseIndividual()
          await(wsClient.url(url(id)).withHttpHeaders(HeaderNames.COOKIE -> sessionCookie).get())
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedTitle)

        "id in url is UUID" in {
          UUID.fromString(result.uri.toString.split("/").last)
        }

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
          await(wsClient.url(url(id)).withHttpHeaders(HeaderNames.COOKIE -> sessionCookie).get())
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(changeAmountPageTitle)

      }
    }

    "redirect to the overview page" when {

      "there is no cya data in session" in {
        {
          authoriseIndividual()
          await(wsClient.url(url(id)).get())
        }

        stubGet("/income-through-software/return/2022/view", 303, "")
        verifyGet("/income-through-software/return/2022/view")
        wireMockServer.resetAll()

      }
    }

  }

  "as an individual" when {
    import IndividualExpected._

    ".show" should {

      "returns an action with the correct english content" which {
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

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(url(id))
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie)
            .get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        s"has an OK($OK) status" in {
          result.status shouldBe OK
        }

        titleCheck(expectedTitle)
        h1Check(expectedH1 + " " + expectedCaption)
        welshToggleCheck("English")
        textOnPageCheck(expectedCaption, captionSelector)
        textOnPageCheck(expectedWhatWouldYouCallText, whatWouldYouCallSelector)
        textOnPageCheck(expectedAmountInterestText, amountInterestSelector)
        textOnPageCheck(expectedInputHintText, inputHintTextSelector)
        buttonCheck(expectedButtonText, buttonSelector)

      }

      "returns an action with the correct welsh content" which {
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

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(url(id))
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie,
              HeaderNames.ACCEPT_LANGUAGE -> "cy")
            .get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        s"has an OK($OK) status" in {
          result.status shouldBe OK
        }

        titleCheck(expectedTitleCy)
        h1Check(expectedH1Cy + " " + expectedCaptionCy)
        welshToggleCheck("Welsh")
        textOnPageCheck(expectedCaptionCy, captionSelector)
        textOnPageCheck(expectedWhatWouldYouCallTextCy, whatWouldYouCallSelector)
        textOnPageCheck(expectedAmountInterestTextCy, amountInterestSelector)
        textOnPageCheck(expectedInputHintTextCy, inputHintTextSelector)
        buttonCheck(expectedButtonTextCy, buttonSelector)

      }
    }

    ".submit" should {

      "with a language of english" should {

        s"return an OK($OK) status" in {
          lazy val interestCYA = InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
            Some(false), None
          )

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
          ))

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(
              wsClient.url(url(id))
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
                .post(Map("untaxedAccountName" -> "jennifer",
                  "untaxedAmount" -> "250"))
            )
          }

          result.status shouldBe OK
        }

        s"returns an empty name error" which {
          lazy val interestCYA = InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
            Some(false), None
          )

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
          ))

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(url(id))
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map("untaxedAccountName" -> "",
                "untaxedAmount" -> "12344.98")))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"returns a BAD_REQUEST($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          inputFieldValueCheck("", accountNameSelector)
          inputFieldValueCheck("12344.98", amountSelector)
          titleCheck(s"$expectedErrorTitle")

          errorSummaryCheck(expectedNameEmptyError, accountNameSelector)
          errorAboveElementCheck(expectedNameEmptyError)
        }

        s"returns a duplicate name error" which {
          lazy val interestCYA = InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Monzo", amount))),
            Some(false), None
          )


          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
          ))

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(
              wsClient.url(url(id))
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
                .post(Map("untaxedAccountName" -> "Monzo",
                  "untaxedAmount" -> "12344.98"))
            )
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"returns a BAD_REQUEST($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          inputFieldValueCheck("Monzo", accountNameSelector)
          inputFieldValueCheck("12344.98", amountSelector)
          titleCheck(s"$expectedErrorTitle")

          errorSummaryCheck(expectedNameDuplicateError, accountNameSelector)
          errorAboveElementCheck(expectedNameDuplicateError)
        }

        s"returns an empty amount error" which {
          lazy val interestCYA = InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
            Some(false), None
          )

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
          ))

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(url(id))
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map("untaxedAccountName" -> "Halifax",
                "untaxedAmount" -> "")))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"returns a BAD_REQUEST($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          inputFieldValueCheck("Halifax", accountNameSelector)
          inputFieldValueCheck("", amountSelector)
          titleCheck(s"$expectedErrorTitle")

          errorSummaryCheck(expectedAmountEmptyError, amountSelector)
          errorAboveElementCheck(expectedAmountEmptyError)
        }

        s"returns an over max value error" which {
          lazy val interestCYA = InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
            Some(false), None
          )

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
          ))

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(url(id))
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map("untaxedAccountName" -> "Halifax",
                "untaxedAmount" -> "9999999999999.01")))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"returns a BAD_REQUEST($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          inputFieldValueCheck("Halifax", accountNameSelector)
          inputFieldValueCheck("9999999999999.01", amountSelector)
          titleCheck(s"$expectedErrorTitle")

          errorSummaryCheck(expectedAmountMaxValueError, amountSelector)
          errorAboveElementCheck(expectedAmountMaxValueError)
        }

        s"returns an incorrect value format error" which {
          lazy val interestCYA = InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
            Some(false), None
          )

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
          ))

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(url(id))
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map("untaxedAccountName" -> "Halifax",
                "untaxedAmount" -> "One Hundred")))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"returns a BAD_REQUEST($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          inputFieldValueCheck("Halifax", accountNameSelector)
          inputFieldValueCheck("", amountSelector)
          titleCheck(s"$expectedErrorTitle")

          errorSummaryCheck(expectedAmountInvalidCharError, amountSelector)
          errorAboveElementCheck(expectedAmountInvalidCharError)
        }

        s"return an UNAUTHORIZED($UNAUTHORIZED) status" which {
          lazy val interestCYA = InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
            Some(false), None
          )

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
          ))

          lazy val result = {
            authoriseIndividualUnauthorized()
            await(wsClient.url(s"$startUrl/$taxYear/interest/untaxed-uk-interest-details/UntaxedId")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map[String, String]()))
          }

          s"has an BAD_REQUEST($UNAUTHORIZED) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }
      }

      "with a language of welsh" should {

        s"return an OK($OK) status" in {
          lazy val interestCYA = InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
            Some(false), None
          )

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
          ))

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(
              wsClient.url(url(id))
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie,
                  HeaderNames.ACCEPT_LANGUAGE -> "cy",
                  "Csrf-Token" -> "nocheck")
                .post(Map("untaxedAccountName" -> "jennifer",
                  "untaxedAmount" -> "250"))
            )
          }

          result.status shouldBe OK
        }

        s"returns an empty name error" which {
          lazy val interestCYA = InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
            Some(false), None
          )

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
          ))

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(url(id))
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie,
                HeaderNames.ACCEPT_LANGUAGE -> "cy",
                "Csrf-Token" -> "nocheck")
              .post(Map("untaxedAccountName" -> "",
                "untaxedAmount" -> "12344.98")))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"returns a BAD_REQUEST($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          inputFieldValueCheck("", accountNameSelector)
          inputFieldValueCheck("12344.98", amountSelector)
          titleCheck(s"$expectedErrorTitleCy")

          errorSummaryCheck(expectedNameEmptyErrorCy, accountNameSelector)
          errorAboveElementCheck(expectedNameEmptyErrorCy)
        }

        s"returns a duplicate name error" which {
          lazy val interestCYA = InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Monzo", amount))),
            Some(false), None
          )

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
          ))

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(
              wsClient.url(url(id))
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie,
                  HeaderNames.ACCEPT_LANGUAGE -> "cy",
                  "Csrf-Token" -> "nocheck")
                .post(Map("untaxedAccountName" -> "Monzo",
                  "untaxedAmount" -> "12344.98"))
            )
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"returns a BAD_REQUEST($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          inputFieldValueCheck("Monzo", accountNameSelector)
          inputFieldValueCheck("12344.98", amountSelector)
          titleCheck(s"$expectedErrorTitleCy")

          errorSummaryCheck(expectedNameDuplicateErrorCy, accountNameSelector)
          errorAboveElementCheck(expectedNameDuplicateErrorCy)
        }

        s"returns an empty amount error" which {
          lazy val interestCYA = InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
            Some(false), None
          )

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
          ))

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(url(id))
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map("untaxedAccountName" -> "Halifax",
                "untaxedAmount" -> "")))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"returns a BAD_REQUEST($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          inputFieldValueCheck("Halifax", accountNameSelector)
          inputFieldValueCheck("", amountSelector)
          titleCheck(s"$expectedErrorTitle")

          errorSummaryCheck(expectedAmountEmptyError, amountSelector)
          errorAboveElementCheck(expectedAmountEmptyError)
        }

        s"returns an over max value error" which {
          lazy val interestCYA = InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
            Some(false), None
          )

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
          ))

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(url(id))
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map("untaxedAccountName" -> "Halifax",
                "untaxedAmount" -> "9999999999999.01")))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"returns a BAD_REQUEST($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          inputFieldValueCheck("Halifax", accountNameSelector)
          inputFieldValueCheck("9999999999999.01", amountSelector)
          titleCheck(s"$expectedErrorTitle")

          errorSummaryCheck(expectedAmountMaxValueError, amountSelector)
          errorAboveElementCheck(expectedAmountMaxValueError)
        }

        s"returns an incorrect value format error" which {
          lazy val interestCYA = InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
            Some(false), None
          )

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
          ))

          lazy val result: WSResponse = {
            authoriseIndividual()
            await(wsClient.url(url(id))
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map("untaxedAccountName" -> "Halifax",
                "untaxedAmount" -> "One Hundred")))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"returns a BAD_REQUEST($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          inputFieldValueCheck("Halifax", accountNameSelector)
          inputFieldValueCheck("", amountSelector)
          titleCheck(s"$expectedErrorTitle")

          errorSummaryCheck(expectedAmountInvalidCharError, amountSelector)
          errorAboveElementCheck(expectedAmountInvalidCharError)
        }

        s"return an UNAUTHORIZED($UNAUTHORIZED) status" which {
          lazy val interestCYA = InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
            Some(false), None
          )

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA))
          ))

          lazy val result = {
            authoriseIndividualUnauthorized()
            await(wsClient.url(s"$startUrl/$taxYear/interest/untaxed-uk-interest-details/UntaxedId")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie,
                HeaderNames.ACCEPT_LANGUAGE -> "cy",
                "Csrf-Token" -> "nocheck")
              .post(Map[String, String]()))
          }

          s"has an BAD_REQUEST($UNAUTHORIZED) status" in {
            result.status shouldBe UNAUTHORIZED
          }

        }

      }

    }
  }

  "as an agent" when {

    import AgentExpected._

    ".show" should {

      "returns an action with the correct english content" which {
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
          await(wsClient.url(url(id))
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie)
            .get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        s"has an OK($OK) status" in {
          result.status shouldBe OK
        }

        titleCheck(expectedTitle)
        h1Check(expectedH1 + " " + expectedCaption)
        welshToggleCheck("English")
        textOnPageCheck(expectedCaption, captionSelector)
        textOnPageCheck(expectedWhatWouldYouCallText, whatWouldYouCallSelector)
        textOnPageCheck(expectedAmountInterestText, amountInterestSelector)
        textOnPageCheck(expectedInputHintText, inputHintTextSelector)
        buttonCheck(expectedButtonText, buttonSelector)

      }

      "returns an action with the correct welsh content" which {
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
          await(wsClient.url(url(id))
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie,
              HeaderNames.ACCEPT_LANGUAGE -> "cy")
            .get())
        }

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        s"has an OK($OK) status" in {
          result.status shouldBe OK
        }

        titleCheck(expectedTitleCy)
        h1Check(expectedH1Cy + " " + expectedCaptionCy)
        welshToggleCheck("Welsh")
        textOnPageCheck(expectedCaptionCy, captionSelector)
        textOnPageCheck(expectedWhatWouldYouCallTextCy, whatWouldYouCallSelector)
        textOnPageCheck(expectedAmountInterestTextCy, amountInterestSelector)
        textOnPageCheck(expectedInputHintTextCy, inputHintTextSelector)
        buttonCheck(expectedButtonTextCy, buttonSelector)

      }
    }

    ".submit" should {

      "with a language of english" should {

        s"return an OK($OK) status" in {
          lazy val interestCYA = InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
            Some(false), None
          )

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA)),
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A")
          )

          lazy val result: WSResponse = {
            authoriseAgent()
            await(
              wsClient.url(url(id))
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
                .post(Map("untaxedAccountName" -> "jennifer",
                  "untaxedAmount" -> "250"))
            )
          }

          result.status shouldBe OK
        }

        s"returns an empty amount error" which {
          lazy val interestCYA = InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
            Some(false), None
          )

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA)),
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A")
          )

          lazy val result: WSResponse = {
            authoriseAgent()
            await(wsClient.url(url(id))
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map("untaxedAccountName" -> "Halifax",
                "untaxedAmount" -> "")))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"returns a BAD_REQUEST($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          inputFieldValueCheck("Halifax", accountNameSelector)
          inputFieldValueCheck("", amountSelector)
          titleCheck(s"$expectedErrorTitle")

          errorSummaryCheck(expectedAmountEmptyError, amountSelector)
          errorAboveElementCheck(expectedAmountEmptyError)
        }

        s"return an UNAUTHORIZED($UNAUTHORIZED) status" which {
          lazy val interestCYA = InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
            Some(false), None
          )

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA)),
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A")
          )

          lazy val result = {
            authoriseAgentUnauthorized()
            await(wsClient.url(s"$startUrl/$taxYear/interest/untaxed-uk-interest-details/UntaxedId")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map[String, String]()))
          }

          s"has an BAD_REQUEST($UNAUTHORIZED) status" in {
            result.status shouldBe UNAUTHORIZED
          }
        }
      }

      "with a language of welsh" should {

        s"return an OK($OK) status" in {
          lazy val interestCYA = InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
            Some(false), None
          )

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA)),
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A")
          )

          lazy val result: WSResponse = {
            authoriseAgent()
            await(
              wsClient.url(url(id))
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie,
                  HeaderNames.ACCEPT_LANGUAGE -> "cy",
                  "Csrf-Token" -> "nocheck")
                .post(Map("untaxedAccountName" -> "jennifer",
                  "untaxedAmount" -> "250"))
            )
          }

          result.status shouldBe OK
        }

        s"returns an empty amount error" which {
          lazy val interestCYA = InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
            Some(false), None
          )

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA)),
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A")
          )

          lazy val result: WSResponse = {
            authoriseAgent()
            await(wsClient.url(url(id))
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie,
                HeaderNames.ACCEPT_LANGUAGE -> "cy",
                "Csrf-Token" -> "nocheck")
              .post(Map("untaxedAccountName" -> "Halifax",
                "untaxedAmount" -> "")))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          s"returns a BAD_REQUEST($BAD_REQUEST) status" in {
            result.status shouldBe BAD_REQUEST
          }

          inputFieldValueCheck("Halifax", accountNameSelector)
          inputFieldValueCheck("", amountSelector)
          titleCheck(s"$expectedErrorTitleCy")

          errorSummaryCheck(expectedAmountEmptyErrorCy, amountSelector)
          errorAboveElementCheck(expectedAmountEmptyErrorCy)
        }

        s"return an UNAUTHORIZED($UNAUTHORIZED) status" which {
          lazy val interestCYA = InterestCYAModel(
            Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
            Some(false), None
          )

          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.INTEREST_CYA -> Json.prettyPrint(Json.toJson(interestCYA)),
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A")
          )

          lazy val result = {
            authoriseAgentUnauthorized()
            await(wsClient.url(s"$startUrl/$taxYear/interest/untaxed-uk-interest-details/UntaxedId")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie,
                HeaderNames.ACCEPT_LANGUAGE -> "cy",
                "Csrf-Token" -> "nocheck")
              .post(Map[String, String]()))
          }

          s"has an BAD_REQUEST($UNAUTHORIZED) status" in {
            result.status shouldBe UNAUTHORIZED
          }

        }

      }

    }
  }
}
