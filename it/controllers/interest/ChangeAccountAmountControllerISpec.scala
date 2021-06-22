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
import helpers.PlaySessionCookieBaker
import models.interest.{InterestAccountModel, InterestCYAModel}
import models.priorDataModels.{IncomeSourcesModel, InterestModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.WSClient
import utils.{IntegrationTest, InterestDatabaseHelper, ViewHelpers}

import java.util.UUID

class ChangeAccountAmountControllerISpec extends IntegrationTest with ViewHelpers with InterestDatabaseHelper{

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  val taxYear: Int = 2022

  val amountSelector = "#amount"

  val amount: BigDecimal = 25
  val differentAmount: BigDecimal = 30
  val accountName: String = "HSBC"

  val overviewPageTitle = "Your Income Tax Return"
  val untaxedAccountPageTitle = "Accounts with untaxed UK interest"
  val taxedAccountPageTitle = "Accounts with taxed UK interest"
  val untaxedChangeAmountPageTitle = "How much untaxed UK interest did you get?"
  val taxedChangeAmountPageTitle = "How much taxed UK interest did you get?"

  val taxYearMinusOne: Int = taxYear - 1

  val captionSelector = ".govuk-caption-l"
  val continueButtonSelector = "#continue"
  val continueFormSelector = "#main-content > div > div > form"
  val errorSummarySelector = ".govuk-error-summary"
  val errorSummaryTitleSelector = ".govuk-error-summary__title"
  val errorSummaryTextSelector = ".govuk-error-summary__body"
  val newAmountInputSelector = "#amount"
  val amountInputName = "amount"
  val youToldUsSelector = "#main-content > div > div > form > div > div > label > p"

  object IndividualExpected {
    val expectedUntaxedTitle = "How much untaxed UK interest did you get?"
    val expectedUntaxedErrorTitle = s"Error: $expectedUntaxedTitle"
    val expectedTaxedTitle = "How much taxed UK interest did you get?"
    val expectedTaxedErrorTitle = s"Error: $expectedTaxedTitle"
    def expectedErrorEmpty(taxType: String): String = s"Enter the amount of $taxType UK interest you got"
    def expectedErrorOverMax(taxType: String): String = s"The amount of $taxType UK interest must be less than £100,000,000,000"
    def expectedErrorInvalid(taxType: String): String = s"Enter the amount of $taxType UK interest in the correct format"
    val expectedUntaxedH1 = "HSBC: how much untaxed UK interest did you get?"
    val expectedTaxedH1 = "HSBC: how much taxed UK interest did you get?"
    val youToldUsUntaxed = s"You told us you got £$amount untaxed UK interest. Tell us if this has changed."
    val youToldUsTaxed = s"You told us you got £$amount taxed UK interest. Tell us if this has changed."

    val expectedUntaxedTitleCy = "How much untaxed UK interest did you get?"
    val expectedUntaxedErrorTitleCy = s"Error: $expectedUntaxedTitle"
    val expectedTaxedTitleCy = "How much taxed UK interest did you get?"
    val expectedTaxedErrorTitleCy = s"Error: $expectedTaxedTitle"
    def expectedErrorEmptyCy(taxType: String): String = s"Enter the amount of $taxType UK interest you got"
    def expectedErrorOverMaxCy(taxType: String): String = s"The amount of $taxType UK interest must be less than £100,000,000,000"
    def expectedErrorInvalidCy(taxType: String): String = s"Enter the amount of $taxType UK interest in the correct format"
    val expectedUntaxedH1Cy = "HSBC: how much untaxed UK interest did you get?"
    val expectedTaxedH1Cy = "HSBC: how much taxed UK interest did you get?"
    val youToldUsUntaxedCy = s"You told us you got £$amount untaxed UK interest. Tell us if this has changed."
    val youToldUsTaxedCy = s"You told us you got £$amount taxed UK interest. Tell us if this has changed."
  }


  object AgentExpected {
    val expectedUntaxedTitle = "How much untaxed UK interest did your client get?"
    val expectedUntaxedErrorTitle = s"Error: $expectedUntaxedTitle"
    val expectedTaxedTitle = "How much taxed UK interest did your client get?"
    val expectedTaxedErrorTitle = s"Error: $expectedTaxedTitle"
    def expectedErrorEmpty(taxType: String): String = s"Enter the amount of $taxType UK interest your client got"
    def expectedErrorOverMax(taxType: String): String = s"The amount of $taxType UK interest must be less than £100,000,000,000"
    def expectedErrorInvalid(taxType: String): String = s"Enter the amount of $taxType UK interest in the correct format"
    val expectedUntaxedH1 = "HSBC: how much untaxed UK interest did your client get?"
    val expectedTaxedH1 = "HSBC: how much taxed UK interest did your client get?"
    val youToldUsUntaxed = s"You told us your client got £$amount untaxed UK interest. Tell us if this has changed."
    val youToldUsTaxed = s"You told us your client got £$amount taxed UK interest. Tell us if this has changed."

    val expectedUntaxedTitleCy = "How much untaxed UK interest did your client get?"
    val expectedUntaxedErrorTitleCy = s"Error: $expectedUntaxedTitle"
    val expectedTaxedTitleCy = "How much taxed UK interest did your client get?"
    val expectedTaxedErrorTitleCy = s"Error: $expectedTaxedTitle"
    def expectedErrorEmptyCy(taxType: String): String = s"Enter the amount of $taxType UK interest your client got"
    def expectedErrorOverMaxCy(taxType: String): String = s"The amount of $taxType UK interest must be less than £100,000,000,000"
    def expectedErrorInvalidCy(taxType: String): String = s"Enter the amount of $taxType UK interest in the correct format"
    val expectedUntaxedH1Cy = "HSBC: how much untaxed UK interest did your client get?"
    val expectedTaxedH1Cy = "HSBC: how much taxed UK interest did your client get?"
    val youToldUsUntaxedCy = s"You told us your client got £$amount untaxed UK interest. Tell us if this has changed."
    val youToldUsTaxedCy = s"You told us your client got £$amount taxed UK interest. Tell us if this has changed."
  }

  val expectedCaption = s"Interest for 6 April $taxYearMinusOne to 5 April $taxYear"
  val expectedHintText = "For example, £600 or £193.54"
  val continueText = "Continue"

  val expectedCaptionCy = s"Interest for 6 April $taxYearMinusOne to 5 April $taxYear"
  val expectedHintTextCy = "For example, £600 or £193.54"
  val continueTextCy = "Continue"

  def url(newId: String, accountType: String): String = s"$startUrl/$taxYear/interest/change-$accountType-uk-interest?accountId=$newId"

  def urlNoPreFix(newId: String, accountType: String): String =
    s"/income-through-software/return/personal-income/$taxYear/interest/change-$accountType-uk-interest?accountId=$newId"

  lazy val id: String = UUID.randomUUID().toString

  val untaxedInterestCyaModel: InterestCYAModel = InterestCYAModel(
    Some(true), Some(Seq(InterestAccountModel(Some(id), accountName, amount))),
    Some(false), None
  )

  val taxedInterestCyaModel: InterestCYAModel = InterestCYAModel(
    Some(false), None,
    Some(true), Some(Seq(InterestAccountModel(Some(id), accountName, amount)))
  )

  val taxedCyaSubmitModel: InterestCYAModel = InterestCYAModel(
    Some(false), None,
    Some(true), Some(Seq(InterestAccountModel(Some("TaxedId"), "Taxed Account", amount)))
  )

  val untaxedCyaSubmitModel: InterestCYAModel = InterestCYAModel(
    Some(true), Some(Seq(InterestAccountModel(Some("UntaxedId"), "Untaxed Account", amount))),
    Some(false), None
  )

  "calling /GET" when {

    "untaxed" should {

      "render the untaxed change amount page without pre-populated amount box and Individual content" which {
        import IndividualExpected._

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        lazy val result = {
          dropInterestDB()

          authoriseIndividual()
          insertCyaData(Some(untaxedInterestCyaModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, id, None, Some(amount))))), nino, taxYear)
          await(wsClient.url(url(id, "untaxed"))
            .withHttpHeaders(
              xSessionId,
              csrfContent,
              HeaderNames.COOKIE -> sessionCookie
            ).get()
          )
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedUntaxedTitle)
        welshToggleCheck("English")
        h1Check(expectedUntaxedH1 + " " + expectedCaption)
        textOnPageCheck(youToldUsUntaxed, youToldUsSelector)
        textOnPageCheck(expectedCaption, captionSelector)
        hintTextCheck(expectedHintText)
        inputFieldCheck(amountInputName, newAmountInputSelector)
        buttonCheck(continueText, continueButtonSelector)
        formPostLinkCheck(urlNoPreFix(id, "untaxed"), continueFormSelector)
        inputFieldValueCheck("", amountSelector)
      }

      "render the untaxed change amount page without pre-populated amount box and agent content" which {
        import AgentExpected._

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A"
        ))

        lazy val result = {
          dropInterestDB()

          authoriseAgent()
          insertCyaData(Some(untaxedInterestCyaModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, id, None, Some(amount))))), nino, taxYear)
          await(wsClient.url(url(id, "untaxed"))
            .withHttpHeaders(
              xSessionId,
              csrfContent,
              HeaderNames.COOKIE -> sessionCookie
            ).get()
          )
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedUntaxedTitle)
        welshToggleCheck("English")
        h1Check(expectedUntaxedH1 + " " + expectedCaption)
        textOnPageCheck(youToldUsUntaxed, youToldUsSelector)
        textOnPageCheck(expectedCaption, captionSelector)
        hintTextCheck(expectedHintText)
        inputFieldCheck(amountInputName, newAmountInputSelector)
        buttonCheck(continueText, continueButtonSelector)
        formPostLinkCheck(urlNoPreFix(id, "untaxed"), continueFormSelector)
        inputFieldValueCheck("", amountSelector)
      }

      "render the untaxed change amount page without pre-populated amount box and Individual content- Welsh" which {
        import IndividualExpected._

        lazy val result = {
          dropInterestDB()

          authoriseIndividual()
          insertCyaData(Some(untaxedInterestCyaModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, id, None, Some(amount))))), nino, taxYear)
          await(wsClient.url(url(id, "untaxed"))
            .withHttpHeaders(
              xSessionId,
              csrfContent,
              HeaderNames.ACCEPT_LANGUAGE -> "cy"
            ).get()
          )
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedUntaxedTitleCy)
        welshToggleCheck("Welsh")
        h1Check(expectedUntaxedH1Cy + " " + expectedCaptionCy)
        textOnPageCheck(youToldUsUntaxedCy, youToldUsSelector)
        textOnPageCheck(expectedCaptionCy, captionSelector)
        hintTextCheck(expectedHintTextCy)
        inputFieldCheck(amountInputName, newAmountInputSelector)
        buttonCheck(continueTextCy, continueButtonSelector)
        formPostLinkCheck(urlNoPreFix(id, "untaxed"), continueFormSelector)
        inputFieldValueCheck("", amountSelector)
      }

      "render the untaxed change amount page without pre-populated amount box and agent content - Welsh" which {
        import AgentExpected._

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A",
        ))

        lazy val result = {
          dropInterestDB()

          authoriseAgent()
          insertCyaData(Some(untaxedInterestCyaModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, id, None, Some(amount))))), nino, taxYear)
          await(wsClient.url(url(id, "untaxed"))
            .withHttpHeaders(
              xSessionId,
              csrfContent,
              HeaderNames.COOKIE -> sessionCookie,
              HeaderNames.ACCEPT_LANGUAGE -> "cy"
            ).get()
          )
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedUntaxedTitleCy)
        welshToggleCheck("Welsh")
        h1Check(expectedUntaxedH1Cy + " " + expectedCaptionCy)
        textOnPageCheck(youToldUsUntaxedCy, youToldUsSelector)
        textOnPageCheck(expectedCaptionCy, captionSelector)
        hintTextCheck(expectedHintTextCy)
        inputFieldCheck(amountInputName, newAmountInputSelector)
        buttonCheck(continueTextCy, continueButtonSelector)
        formPostLinkCheck(urlNoPreFix(id, "untaxed"), continueFormSelector)
        inputFieldValueCheck("", amountSelector)
      }

      "render the untaxed change amount page with pre-populated amount box" which {

        val interestCYA = InterestCYAModel(
          Some(true), Some(Seq(InterestAccountModel(Some(id), accountName, differentAmount))),
          Some(false), None
        )

        lazy val result = {
          dropInterestDB()

          authoriseIndividual()
          insertCyaData(Some(interestCYA))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, id, None, Some(amount))))), nino, taxYear)
          await(wsClient.url(url(id, "untaxed"))
            .withHttpHeaders(
              xSessionId,
              csrfContent
            ).get()
          )
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(untaxedChangeAmountPageTitle)
        inputFieldValueCheck(differentAmount.toString(), amountSelector)
      }

      "redirect to the overview page" when {

        "there is no prior or cya data" in {

          val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> taxYear.toString
          ))

          lazy val result = {
            dropInterestDB()

            authoriseIndividual()
            insertCyaData(None)
            userDataStub(IncomeSourcesModel(interest = Some(Seq())), nino, taxYear)
            stubGet("/income-through-software/return/2022/view", SEE_OTHER, "overview")
            await(wsClient.url(url(id, "untaxed"))
              .withHttpHeaders(
                xSessionId,
                csrfContent,
                HeaderNames.COOKIE -> playSessionCookie
              )
              .withFollowRedirects(false)
              .get()
            )
          }

          wireMockServer.resetAll()

          result.status shouldBe SEE_OTHER
          result.headers("Location").head.contains("/income-through-software/return/2022/view") shouldBe true
        }

      }

      "redirect to untaxed account summary page" when {

        "there is cya data but no prior data" which {

          lazy val result = {
            dropInterestDB()

            authoriseIndividual()
            insertCyaData(Some(untaxedInterestCyaModel))
            userDataStub(IncomeSourcesModel(interest = Some(Seq())), nino, taxYear)
            await(wsClient.url(url(id, "untaxed"))
              .withHttpHeaders(
                xSessionId,
                csrfContent,
              ).get()
            )
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(untaxedAccountPageTitle)
        }
      }
    }

    "taxed" should {

      "render the taxed change amount page without pre-populated amount box and Individual content" which {
        import IndividualExpected._

        val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.TAX_YEAR -> taxYear.toString
        ))

        lazy val result = {
          dropInterestDB()

          authoriseIndividual()
          insertCyaData(Some(taxedInterestCyaModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, id, Some(amount), None)))), nino, taxYear)
          await(wsClient.url(url(id, "taxed"))
            .withHttpHeaders(
              xSessionId,
              csrfContent,
              HeaderNames.COOKIE -> sessionCookie
            ).get()
          )
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedTaxedTitle)
        welshToggleCheck("English")
        h1Check(expectedTaxedH1 + " " + expectedCaption)
        textOnPageCheck(youToldUsTaxed, youToldUsSelector)
        textOnPageCheck(expectedCaption, captionSelector)
        hintTextCheck(expectedHintText)
        inputFieldCheck(amountInputName, newAmountInputSelector)
        buttonCheck(continueText, continueButtonSelector)
        formPostLinkCheck(urlNoPreFix(id, "taxed"), continueFormSelector)
        inputFieldValueCheck("", amountSelector)

      }

      "render the taxed change amount page without pre-populated amount box and agent content" which {
        import AgentExpected._

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A"
        ))

        lazy val result = {
          dropInterestDB()

          authoriseAgent()
          insertCyaData(Some(taxedInterestCyaModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, id, Some(amount), None)))), nino, taxYear)
          await(wsClient.url(url(id, "taxed"))
            .withHttpHeaders(
              xSessionId,
              csrfContent,
              HeaderNames.COOKIE -> sessionCookie
            ).get()
          )
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedTaxedTitle)
        welshToggleCheck("English")
        h1Check(expectedTaxedH1 + " " + expectedCaption)
        textOnPageCheck(youToldUsTaxed, youToldUsSelector)
        textOnPageCheck(expectedCaption, captionSelector)
        hintTextCheck(expectedHintText)
        inputFieldCheck(amountInputName, newAmountInputSelector)
        buttonCheck(continueText, continueButtonSelector)
        formPostLinkCheck(urlNoPreFix(id, "taxed"), continueFormSelector)
        inputFieldValueCheck("", amountSelector)

      }

      "render the taxed change amount page without pre-populated amount box and Individual content - Welsh" which {
        import IndividualExpected._

        lazy val result = {
          dropInterestDB()

          authoriseIndividual()
          insertCyaData(Some(taxedInterestCyaModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, id, Some(amount), None)))), nino, taxYear)
          await(wsClient.url(url(id, "taxed"))
            .withHttpHeaders(
              xSessionId,
              csrfContent,
              HeaderNames.ACCEPT_LANGUAGE -> "cy"
            ).get()
          )
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedTaxedTitleCy)
        welshToggleCheck("Welsh")
        h1Check(expectedTaxedH1Cy + " " + expectedCaptionCy)
        textOnPageCheck(youToldUsTaxedCy, youToldUsSelector)
        textOnPageCheck(expectedCaptionCy, captionSelector)
        hintTextCheck(expectedHintTextCy)
        inputFieldCheck(amountInputName, newAmountInputSelector)
        buttonCheck(continueTextCy, continueButtonSelector)
        formPostLinkCheck(urlNoPreFix(id, "taxed"), continueFormSelector)
        inputFieldValueCheck("", amountSelector)

      }

      "render the taxed change amount page without pre-populated amount box and agent content - Welsh" which {
        import AgentExpected._

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A"
        ))

        lazy val result = {
          dropInterestDB()

          authoriseAgent()
          insertCyaData(Some(taxedInterestCyaModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, id, None, Some(amount))))), nino, taxYear)
          await(wsClient.url(url(id, "taxed"))
            .withHttpHeaders(
              xSessionId,
              csrfContent,
              HeaderNames.COOKIE -> sessionCookie,
              HeaderNames.ACCEPT_LANGUAGE -> "cy"
            ).get()
          )
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(expectedTaxedTitleCy)
        welshToggleCheck("Welsh")
        h1Check(expectedTaxedH1Cy + " " + expectedCaptionCy)
        textOnPageCheck(youToldUsTaxedCy, youToldUsSelector)
        textOnPageCheck(expectedCaptionCy, captionSelector)
        hintTextCheck(expectedHintTextCy)
        inputFieldCheck(amountInputName, newAmountInputSelector)
        buttonCheck(continueTextCy, continueButtonSelector)
        formPostLinkCheck(urlNoPreFix(id, "taxed"), continueFormSelector)
        inputFieldValueCheck("", amountSelector)

      }

      "render the taxed change amount page with pre-populated amount box" which {

        lazy val interestCYA = InterestCYAModel(
          Some(false), None,
          Some(true), Some(Seq(InterestAccountModel(Some(id), accountName, differentAmount)))
        )

        lazy val result = {
          dropInterestDB()

          authoriseIndividual()
          insertCyaData(Some(interestCYA))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, id, Some(amount), None)))), nino, taxYear)
          await(wsClient.url(url(id, "taxed"))
            .withHttpHeaders(
              xSessionId,
              csrfContent
            ).get()
          )
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck(taxedChangeAmountPageTitle)
        inputFieldValueCheck(differentAmount.toString(), amountSelector)

      }

      "redirect to the overview page" when {

        "there is no prior or cya data" in {

          val playSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.TAX_YEAR -> taxYear.toString
          ))

          lazy val result = {
            dropInterestDB()

            authoriseIndividual()
            insertCyaData(None)
            userDataStub(IncomeSourcesModel(interest = Some(Seq())), nino, taxYear)
            stubGet("/income-through-software/return/2022/view", SEE_OTHER, "overview")
            await(wsClient.url(url(id, "taxed"))
              .withHttpHeaders(
                xSessionId,
                csrfContent,
                HeaderNames.COOKIE -> playSessionCookie
              )
              .withFollowRedirects(false)
              .get()
            )
          }

          wireMockServer.resetAll()

          result.status shouldBe SEE_OTHER
          result.headers("Location").head.contains("/income-through-software/return/2022/view") shouldBe true
        }

      }

      "redirect to taxed account summary page" when {

        "there is cya data but no prior data" which {

          lazy val result = {
            dropInterestDB()

            authoriseIndividual()
            insertCyaData(Some(taxedInterestCyaModel))
            userDataStub(IncomeSourcesModel(interest = Some(Seq())), nino, taxYear)
            await(wsClient.url(url(id, "taxed"))
              .withHttpHeaders(
                xSessionId,
                csrfContent,
              ).get()
            )
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(taxedAccountPageTitle)

        }

      }

    }

  }

  ".submit taxed" should {

    "return an action and the correct errors" when {

      "there is both CYA data and prior in session and input is empty as an Individual" which {
        import IndividualExpected._

        lazy val result = {
          dropInterestDB()

          authoriseIndividual()
          insertCyaData(Some(taxedCyaSubmitModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, "TaxedId", Some(amount), None)))), nino, taxYear)
          await(wsClient.url(url("TaxedId", "taxed"))
            .withHttpHeaders(
              xSessionId,
              csrfContent
            ).post(Map("amount" -> ""))
          )
        }
        s"has an BAD_REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        errorSummaryCheck(expectedErrorEmpty("taxed"), newAmountInputSelector)
        errorAboveElementCheck(expectedErrorEmpty("taxed"))
      }

      "there is both CYA data and prior in session and input is empty as an agent" which {
        import AgentExpected._

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A"
        ))

        lazy val result = {
          dropInterestDB()

          authoriseAgent()
          insertCyaData(Some(taxedCyaSubmitModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, "TaxedId", Some(amount), None)))), nino, taxYear)
          await(wsClient.url(url("TaxedId", "taxed"))
            .withHttpHeaders(
              xSessionId,
              csrfContent,
              HeaderNames.COOKIE -> sessionCookie
            ).post(Map("amount" -> ""))
          )
        }
        s"has an BAD_REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        errorSummaryCheck(expectedErrorEmpty("taxed"), newAmountInputSelector)
        errorAboveElementCheck(expectedErrorEmpty("taxed"))
      }

      "there is both CYA data and prior in session and input is invalid as an Individual" which {
        import IndividualExpected._

        lazy val result = {
          dropInterestDB()

          authoriseIndividual()
          insertCyaData(Some(taxedCyaSubmitModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, "TaxedId", Some(amount), None)))), nino, taxYear)
          await(wsClient.url(url("TaxedId", "taxed"))
            .withHttpHeaders(
              xSessionId,
              csrfContent)
            .post(Map("amount" -> "|")))
        }
        s"has an BAD_REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        errorSummaryCheck(expectedErrorInvalid("taxed"), newAmountInputSelector)
        errorAboveElementCheck(expectedErrorInvalid("taxed"))
      }

      "there is both CYA data and prior in session and input is invalid as an agent" which {
        import AgentExpected._

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A"
        ))

        lazy val result = {
          dropInterestDB()

          authoriseAgent()
          insertCyaData(Some(taxedCyaSubmitModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, "TaxedId", Some(amount), None)))), nino, taxYear)
          await(wsClient.url(url("TaxedId", "taxed"))
            .withHttpHeaders(
              xSessionId,
              csrfContent,
              HeaderNames.COOKIE -> sessionCookie
            ).post(Map("amount" -> "|"))
          )
        }
        s"has an BAD_REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        errorSummaryCheck(expectedErrorInvalid("taxed"), newAmountInputSelector)
        errorAboveElementCheck(expectedErrorInvalid("taxed"))
      }

      "there is both CYA data and prior in session and input is OverMax as an Individual" which {
        import IndividualExpected._

        lazy val result = {
          dropInterestDB()
          authoriseIndividual()
          insertCyaData(Some(taxedCyaSubmitModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, "TaxedId", Some(amount), None)))), nino, taxYear)
          await(wsClient.url(url("TaxedId", "taxed"))
            .withHttpHeaders(
              xSessionId,
              csrfContent
            ).post(Map("amount" -> "99999999999999999999999999999999999999999"))
          )
        }
        s"has an BAD_REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        errorSummaryCheck(expectedErrorOverMax("taxed"), newAmountInputSelector)
        errorAboveElementCheck(expectedErrorOverMax("taxed"))
      }

      "there is both CYA data and prior in session and input is OverMax as an agent" which {
        import AgentExpected._

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A",
        ))

        lazy val result = {
          dropInterestDB()
          authoriseAgent()
          insertCyaData(Some(taxedCyaSubmitModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, "TaxedId", Some(amount), None)))), nino, taxYear)
          await(wsClient.url(url("TaxedId", "taxed"))
            .withHttpHeaders(
              HeaderNames.COOKIE -> sessionCookie,
              xSessionId,
              csrfContent
            ).post(Map("amount" -> "999999999999999999999999999999999999"))
          )
        }
        s"has an BAD_REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        errorSummaryCheck(expectedErrorOverMax("taxed"), newAmountInputSelector)
        errorAboveElementCheck(expectedErrorOverMax("taxed"))
      }

      "there is both CYA data and prior in session and input is empty as an Individual - Welsh" which {
        import IndividualExpected._

        lazy val result = {
          dropInterestDB()
          authoriseIndividual()
          insertCyaData(Some(taxedCyaSubmitModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, "TaxedId", Some(amount), None)))), nino, taxYear)
          await(wsClient.url(url("TaxedId", "taxed"))
            .withHttpHeaders(
              HeaderNames.ACCEPT_LANGUAGE -> "cy",
              xSessionId,
              csrfContent
            ).post(Map("amount" -> ""))
          )
        }

        s"has an BAD_REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)
        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorEmptyCy("taxed"), newAmountInputSelector)
        errorAboveElementCheck(expectedErrorEmptyCy("taxed"))
      }

      "there is both CYA data and prior in session and input is empty as an agent - Welsh" which {
        import AgentExpected._

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A"
        ))

        lazy val result = {
          dropInterestDB()
          authoriseAgent()
          insertCyaData(Some(taxedCyaSubmitModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, "TaxedId", Some(amount), None)))), nino, taxYear)
          await(wsClient.url(url("TaxedId", "taxed"))
            .withHttpHeaders(
              HeaderNames.COOKIE -> sessionCookie,
              HeaderNames.ACCEPT_LANGUAGE -> "cy",
              xSessionId,
              csrfContent
            ).post(Map("amount" -> ""))
          )
        }

        s"has an BAD_REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorEmptyCy("taxed"), newAmountInputSelector)
        errorAboveElementCheck(expectedErrorEmptyCy("taxed"))
      }

      "there is both CYA data and prior in session and input is invalid as an Individual - Welsh" which {
        import IndividualExpected._

        lazy val result = {
          dropInterestDB()
          authoriseIndividual()
          insertCyaData(Some(taxedCyaSubmitModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, "TaxedId", Some(amount), None)))), nino, taxYear)
          await(wsClient.url(url("TaxedId", "taxed"))
            .withHttpHeaders(
              HeaderNames.ACCEPT_LANGUAGE -> "cy",
              xSessionId,
              csrfContent
            ).post(Map("amount" -> "|"))
          )
        }

        s"has an BAD_REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorInvalidCy("taxed"), newAmountInputSelector)
        errorAboveElementCheck(expectedErrorInvalidCy("taxed"))
      }

      "there is both CYA data and prior in session and input is invalid as an agent - Welsh" which {
        import AgentExpected._

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A"
        ))

        lazy val result = {
          dropInterestDB()
          authoriseAgent()
          insertCyaData(Some(taxedCyaSubmitModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, "TaxedId", Some(amount), None)))), nino, taxYear)
          await(wsClient.url(url("TaxedId", "taxed"))
            .withHttpHeaders(
              HeaderNames.COOKIE -> sessionCookie,
              HeaderNames.ACCEPT_LANGUAGE -> "cy",
              xSessionId,
              csrfContent
            ).post(Map("amount" -> "|"))
          )
        }

        s"has an BAD_REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorInvalidCy("taxed"), newAmountInputSelector)
        errorAboveElementCheck(expectedErrorInvalidCy("taxed"))
      }

      "there is both CYA data and prior in session and input is OverMax as an Individual - Welsh" which {
        import IndividualExpected._

        lazy val result = {
          dropInterestDB()
          authoriseIndividual()
          insertCyaData(Some(taxedCyaSubmitModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, "TaxedId", Some(amount), None)))), nino, taxYear)
          await(wsClient.url(url("TaxedId", "taxed"))
            .withHttpHeaders(
              HeaderNames.ACCEPT_LANGUAGE -> "cy",
              xSessionId,
              csrfContent
            ).post(Map("amount" -> "99999999999999999999999999999999999999999"))
          )
        }

        s"has an BAD_REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorOverMaxCy("taxed"), newAmountInputSelector)
        errorAboveElementCheck(expectedErrorOverMaxCy("taxed"))
      }

      "there is both CYA data and prior in session and input is OverMax as an agent - Welsh" which {
        import AgentExpected._

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A",
        ))

        lazy val result = {
          dropInterestDB()
          authoriseAgent()
          insertCyaData(Some(taxedCyaSubmitModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, "TaxedId", Some(amount), None)))), nino, taxYear)
          await(wsClient.url(url("TaxedId", "taxed"))
            .withHttpHeaders(
              HeaderNames.COOKIE -> sessionCookie,
              HeaderNames.ACCEPT_LANGUAGE -> "cy",
              xSessionId,
              csrfContent
            ).post(Map("amount" -> "99999999999999999999999999999999999999999"))
          )
        }

        s"has an BAD_REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorOverMaxCy("taxed"), newAmountInputSelector)
        errorAboveElementCheck(expectedErrorOverMaxCy("taxed"))
      }

      "there is CYA data in session" which {

        lazy val result = {
          dropInterestDB()
          authoriseIndividual()
          insertCyaData(Some(taxedCyaSubmitModel))
          userDataStub(IncomeSourcesModel(interest = None), nino, taxYear)
          await(wsClient.url(url("TaxedId", "taxed"))
            .withHttpHeaders(
              xSessionId,
              csrfContent
            ).post("{}")
          )
        }

        s"has an OK($OK) status" in {
          result.status shouldBe OK
        }
      }

      "there is no CYA data in session" which {

        lazy val result = {
          dropInterestDB()

          emptyUserDataStub()
          insertCyaData(None)

          authoriseIndividual()
          await(wsClient.url(url("TaxedId", "taxed"))
            .withHttpHeaders(
              xSessionId,
              csrfContent
            ).withFollowRedirects(false).post("{}")
          )
        }

        "redirects to the taxed accounts summary page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe "/income-through-software/return/personal-income/2022/interest/accounts-with-taxed-uk-interest"
        }
      }

      "the authorization fails" which {

        lazy val result = {
          dropInterestDB()
          authoriseIndividualUnauthorized()
          await(wsClient.url(url("TaxedId", "taxed"))
            .withHttpHeaders(
              xSessionId,
              csrfContent
            ).post("{}")
          )
        }
        s"has an UNAUTHORIZED($UNAUTHORIZED) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }
    }
  }

  ".submit untaxed" should {

    "return an action" when {

      "there is CYA data in session" which {

        lazy val result = {
          dropInterestDB()
          authoriseIndividual()
          insertCyaData(Some(untaxedCyaSubmitModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, "UntaxedId", None, Some(amount))))), nino, taxYear)
          await(wsClient.url(url("UntaxedId", "untaxed"))
            .withHttpHeaders(
              xSessionId,
              csrfContent
            ).post(Map("amount" -> "250"))
          )
        }

        s"has an OK($OK) status" in {
          result.status shouldBe OK
        }
      }

      "there is CYA and Prior data in session and input is empty as an Individual" which {
        import IndividualExpected._

        lazy val result = {
          dropInterestDB()

          authoriseIndividual()
          insertCyaData(Some(untaxedCyaSubmitModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, "UntaxedId", None, Some(amount))))), nino, taxYear)
          await(wsClient.url(url("UntaxedId", "untaxed"))
            .withHttpHeaders(
              xSessionId,
              csrfContent
            ).post(Map("amount" -> ""))
          )
        }

        s"has an BAD_REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        errorSummaryCheck(expectedErrorEmpty("untaxed"), newAmountInputSelector)
        errorAboveElementCheck(expectedErrorEmpty("untaxed"))
      }

      "there is CYA and Prior data in session and input is empty as an agent" which {
        import AgentExpected._

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A"
        ))
        lazy val result = {
          dropInterestDB()

          authoriseAgent()
          insertCyaData(Some(untaxedCyaSubmitModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, "UntaxedId", None, Some(amount))))), nino, taxYear)
          await(wsClient.url(url("UntaxedId", "untaxed"))
            .withHttpHeaders(
              HeaderNames.COOKIE -> sessionCookie,
              xSessionId,
              csrfContent
            ).post(Map("amount" -> ""))
          )
        }

        s"has an BAD_REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        errorSummaryCheck(expectedErrorEmpty("untaxed"), newAmountInputSelector)
        errorAboveElementCheck(expectedErrorEmpty("untaxed"))
      }

      "there is CYA and Prior data in session and input is invalid as an Individual" which {
        import IndividualExpected._

        lazy val result = {
          dropInterestDB()

          authoriseIndividual()
          insertCyaData(Some(untaxedCyaSubmitModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, "UntaxedId", None, Some(amount))))), nino, taxYear)
          await(wsClient.url(url("UntaxedId", "untaxed"))
            .withHttpHeaders(
              xSessionId,
              csrfContent
            ).post(Map("amount" -> "|"))
          )
        }
        s"has an BAD_REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        errorSummaryCheck(expectedErrorInvalid("untaxed"), newAmountInputSelector)
        errorAboveElementCheck(expectedErrorInvalid("untaxed"))
      }

      "there is CYA and Prior data in session and input is invalid as an agent" which {
        import AgentExpected._

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A"
        ))

        lazy val result = {
          dropInterestDB()

          authoriseAgent()
          insertCyaData(Some(untaxedCyaSubmitModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, "UntaxedId", None, Some(amount))))), nino, taxYear)
          await(wsClient.url(url("UntaxedId", "untaxed"))
            .withHttpHeaders(
              HeaderNames.COOKIE -> sessionCookie,
              xSessionId,
              csrfContent
            ).post(Map("amount" -> "|"))
          )
        }
        s"has an BAD_REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        errorSummaryCheck(expectedErrorInvalid("untaxed"), newAmountInputSelector)
        errorAboveElementCheck(expectedErrorInvalid("untaxed"))
      }

      "there is CYA and Prior data in session and input is OverMax as an Individual" which {
        import IndividualExpected._

        lazy val result = {
          dropInterestDB()

          authoriseIndividual()
          insertCyaData(Some(untaxedCyaSubmitModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, "UntaxedId", None, Some(amount))))), nino, taxYear)
          await(wsClient.url(url("UntaxedId", "untaxed"))
            .withHttpHeaders(
              xSessionId,
              csrfContent
            ).post(Map("amount" -> "9999999999999999999999999999"))
          )
        }
        s"has an BAD_REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        errorSummaryCheck(expectedErrorOverMax("untaxed"), newAmountInputSelector)
        errorAboveElementCheck(expectedErrorOverMax("untaxed"))
      }

      "there is CYA and Prior data in session and input is OverMax as an agent" which {
        import AgentExpected._

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A"
        ))

        lazy val result = {
          dropInterestDB()

          authoriseAgent()
          insertCyaData(Some(untaxedCyaSubmitModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, "UntaxedId", None, Some(amount))))), nino, taxYear)
          await(wsClient.url(url("UntaxedId", "untaxed"))
            .withHttpHeaders(
              HeaderNames.COOKIE -> sessionCookie,
              xSessionId,
              csrfContent
            ).post(Map("amount" -> "99999999999999999999999999999999"))
          )
        }

        s"has an BAD_REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        errorSummaryCheck(expectedErrorOverMax("untaxed"), newAmountInputSelector)
        errorAboveElementCheck(expectedErrorOverMax("untaxed"))
      }

      "there is CYA and Prior data in session and input is empty as an Individual - Welsh" which {
        import IndividualExpected._

        lazy val result = {
          dropInterestDB()

          authoriseIndividual()
          insertCyaData(Some(untaxedCyaSubmitModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, "UntaxedId", None, Some(amount))))), nino, taxYear)
          await(wsClient.url(url("UntaxedId", "untaxed"))
            .withHttpHeaders(
              HeaderNames.ACCEPT_LANGUAGE -> "cy",
              xSessionId,
              csrfContent
            ).post(Map("amount" -> ""))
          )
        }
        s"has an BAD_REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorEmptyCy("untaxed"), newAmountInputSelector)
        errorAboveElementCheck(expectedErrorEmptyCy("untaxed"))
      }

      "there is CYA and Prior data in session and input is empty as an agent - Welsh" which {
        import AgentExpected._

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A"
        ))
        lazy val result = {
          dropInterestDB()

          authoriseAgent()
          insertCyaData(Some(untaxedCyaSubmitModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, "UntaxedId", None, Some(amount))))), nino, taxYear)
          await(wsClient.url(url("UntaxedId", "untaxed"))
            .withHttpHeaders(
              HeaderNames.ACCEPT_LANGUAGE -> "cy",
              HeaderNames.COOKIE -> sessionCookie,
              xSessionId,
              csrfContent
            ).post(Map("amount" -> ""))
          )
        }
        s"has an BAD_REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorEmptyCy("untaxed"), newAmountInputSelector)
        errorAboveElementCheck(expectedErrorEmptyCy("untaxed"))
      }

      "there is CYA and Prior data in session and input is invalid as an Individual - Welsh" which {
        import IndividualExpected._

        lazy val result = {
          dropInterestDB()

          authoriseIndividual()
          insertCyaData(Some(untaxedCyaSubmitModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, "UntaxedId", None, Some(amount))))), nino, taxYear)
          await(wsClient.url(url("UntaxedId", "untaxed"))
            .withHttpHeaders(
              HeaderNames.ACCEPT_LANGUAGE -> "cy",
              xSessionId,
              csrfContent
            ).post(Map("amount" -> "|"))
          )
        }
        s"has an BAD_REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorInvalidCy("untaxed"), newAmountInputSelector)
        errorAboveElementCheck(expectedErrorInvalidCy("untaxed"))
      }

      "there is CYA and Prior data in session and input is invalid as an agent - Welsh" which {
        import AgentExpected._

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A"
        ))

        lazy val result = {
          dropInterestDB()

          authoriseAgent()
          insertCyaData(Some(untaxedCyaSubmitModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, "UntaxedId", None, Some(amount))))), nino, taxYear)
          await(wsClient.url(url("UntaxedId", "untaxed"))
            .withHttpHeaders(
              HeaderNames.ACCEPT_LANGUAGE -> "cy",
              HeaderNames.COOKIE -> sessionCookie,
              xSessionId,
              csrfContent
            ).post(Map("amount" -> "|"))
          )
        }
        s"has an BAD_REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorInvalidCy("untaxed"), newAmountInputSelector)
        errorAboveElementCheck(expectedErrorInvalidCy("untaxed"))
      }

      "there is CYA and Prior data in session and input is OverMax as an Individual - Welsh" which {
        import IndividualExpected._

        lazy val result = {
          dropInterestDB()

          authoriseIndividual()
          insertCyaData(Some(untaxedCyaSubmitModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, "UntaxedId", None, Some(amount))))), nino, taxYear)
          await(wsClient.url(url("UntaxedId", "untaxed"))
            .withHttpHeaders(
              HeaderNames.ACCEPT_LANGUAGE -> "cy",
              xSessionId,
              csrfContent
            ).post(Map("amount" -> "9999999999999999999999999999"))
          )
        }
        s"has an BAD_REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorOverMaxCy("untaxed"), newAmountInputSelector)
        errorAboveElementCheck(expectedErrorOverMaxCy("untaxed"))
      }

      "there is CYA and Prior data in session and input is OverMax as an agent - Welsh" which {
        import AgentExpected._

        lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map(
          SessionValues.CLIENT_MTDITID -> "1234567890",
          SessionValues.CLIENT_NINO -> "AA123456A"
        ))

        lazy val result = {
          dropInterestDB()

          authoriseAgent()
          insertCyaData(Some(untaxedCyaSubmitModel))
          userDataStub(IncomeSourcesModel(interest = Some(Seq(InterestModel(accountName, "UntaxedId", None, Some(amount))))), nino, taxYear)
          await(wsClient.url(url("UntaxedId", "untaxed"))
            .withHttpHeaders(
              HeaderNames.ACCEPT_LANGUAGE -> "cy",
              HeaderNames.COOKIE -> sessionCookie,
              xSessionId,
              csrfContent
            ).post(Map("amount" -> "99999999999999999999999999999999"))
          )
        }
        s"has an BAD_REQUEST($BAD_REQUEST) status" in {
          result.status shouldBe BAD_REQUEST
        }
        implicit val document: () => Document = () => Jsoup.parse(result.body)

        welshToggleCheck("Welsh")
        errorSummaryCheck(expectedErrorOverMaxCy("untaxed"), newAmountInputSelector)
        errorAboveElementCheck(expectedErrorOverMaxCy("untaxed"))
      }

      "there is no CYA data in session" which {
        lazy val result = {
          dropInterestDB()

          emptyUserDataStub()
          insertCyaData(None)

          authoriseIndividual()
          await(wsClient.url(url("UntaxedId", "untaxed"))
            .withHttpHeaders(
              xSessionId,
              csrfContent
            ).withFollowRedirects(false).post("{}")
          )
        }
        s"redirects to the untaxed accounts summary page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe "/income-through-software/return/personal-income/2022/interest/accounts-with-untaxed-uk-interest"
        }
      }

      "the authorization fails" which {

        lazy val result = {
          dropInterestDB()
          authoriseIndividualUnauthorized()
          await(wsClient.url(url("UntaxedId", "untaxed"))
            .withHttpHeaders(
              xSessionId,
              csrfContent
            ).post("{}")
          )
        }
        s"has an UNAUTHORIZED($UNAUTHORIZED) status" in {
          result.status shouldBe UNAUTHORIZED
        }
      }
    }

  }

}
