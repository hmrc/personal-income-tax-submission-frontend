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

package controllers.charity

import common.SessionValues
import helpers.PlaySessionCookieBaker
import models.charity.GiftAidCYAModel
import models.charity.prior.{GiftAidPaymentsModel, GiftAidSubmissionModel}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{GiftAidDatabaseHelper, IntegrationTest, ViewHelpers}

class GiftAidOverseasNameControllerISpec extends IntegrationTest with ViewHelpers with GiftAidDatabaseHelper {


  object IndividualExpected {
    val expectedTitle: String = "Name of overseas charity you used Gift Aid to donate to"
    val expectedH1: String = "Name of overseas charity you used Gift Aid to donate to"
    val expectedError: String = "Enter the name of the overseas charity you used Gift Aid to donate to"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  object AgentExpected {
    val expectedTitle: String = "Name of overseas charity your client used Gift Aid to donate to"
    val expectedH1: String = "Name of overseas charity your client used Gift Aid to donate to"
    val expectedError: String = "Enter the name of the overseas charity your client used Gift Aid to donate to"
    val expectedErrorTitle = s"Error: $expectedTitle"
  }

  val expectedCaption: String = "Donations to charity for 6 April 2021 to 5 April 2022"
  val expectedInputName: String = "name"
  val expectedButtonText: String = "Continue"
  val expectedInputHintText: String = "You can add more than one charity."
  val expectedCharLimitError: String = "The name of the overseas charity must be 75 characters or fewer"
  val expectedInvalidCharError: String = "Name of overseas charity must only include numbers 0-9, letters a " +
    "to z, hyphens, spaces, apostrophes, commas, full stops, round brackets and the special characters, &, /, @, £, *"
  val expectedDuplicateError: String = "You cannot add 2 charities with the same name"

  val captionSelector: String = ".govuk-caption-l"
  val inputFieldSelector: String = "#name"
  val buttonSelector: String = ".govuk-button"
  val inputHintTextSelector: String = "#main-content > div > div > form > div > label > p"

  val charLimit: String = "ukHzoBYHkKGGk2V5iuYgS137gN7EB7LRw3uDjvujYg00ZtHwo3sokyOOCEoAK9vuPiP374QKOelo"
  val testModel: GiftAidSubmissionModel = GiftAidSubmissionModel(Some(GiftAidPaymentsModel(None, Some(List("JaneDoe")), None, None, None, None)),None)

  val requiredSessionCyaModel: GiftAidCYAModel = GiftAidCYAModel(overseasDonationsViaGiftAidAmount = Some(BigDecimal(1)))
  val requiredSessionData: Option[GiftAidCYAModel] = Some(requiredSessionCyaModel)

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val taxYear: Int = 2022

  val overseasCharityNameUrl: String = s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/name-of-overseas-charity"

  lazy val agentSessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
    SessionValues.CLIENT_MTDITID -> "1234567890",
    SessionValues.CLIENT_NINO -> "AA123456A"
  ))

  def getResult(cyaData: Option[GiftAidCYAModel],
                priorData: Option[IncomeSourcesModel],
                isAgent: Boolean,
                welsh: Boolean = false): WSResponse = {

    if(priorData.isDefined) userDataStub(priorData.get, nino, taxYear) else emptyUserDataStub()

    dropGiftAidDB()
    insertCyaData(cyaData)

    val langHeader: (String, String) = if(welsh) HeaderNames.ACCEPT_LANGUAGE -> "cy" else HeaderNames.ACCEPT_LANGUAGE -> "en"

    if(isAgent){
      authoriseAgent()
      await(wsClient.url(overseasCharityNameUrl).withHttpHeaders(
        HeaderNames.COOKIE -> agentSessionCookie, langHeader, xSessionId, csrfContent)
        .withFollowRedirects(false).get
      )
    } else {
      authoriseIndividual()
      await(wsClient.url(overseasCharityNameUrl).withHttpHeaders(langHeader, xSessionId, csrfContent)
        .withFollowRedirects(false).get()
      )
    }

  }

  def postResult(cyaData: Option[GiftAidCYAModel],
                 priorData: Option[IncomeSourcesModel],
                 isAgent: Boolean,
                 input: Map[String, String],
                 welsh: Boolean = false): WSResponse = {

    if(priorData.isDefined) userDataStub(priorData.get, nino, taxYear) else emptyUserDataStub()

    dropGiftAidDB()
    insertCyaData(cyaData)

    val langHeader: (String, String) = if(welsh) HeaderNames.ACCEPT_LANGUAGE -> "cy" else HeaderNames.ACCEPT_LANGUAGE -> "en"

    if(isAgent) {
      authoriseAgent()
      await(wsClient.url(overseasCharityNameUrl).withHttpHeaders(
        HeaderNames.COOKIE -> agentSessionCookie, langHeader, xSessionId, csrfContent)
        .withFollowRedirects(false).post(input)
      )
    } else {
      authoriseIndividual()
      await(wsClient.url(overseasCharityNameUrl).withHttpHeaders(
        langHeader, xSessionId, csrfContent)
        .withFollowRedirects(false).post(input)
      )
    }
  }

  "as an individual" when {
    import IndividualExpected._

    s"Calling GET $overseasCharityNameUrl" when {

      "there is no cya data" should {

        lazy val result: WSResponse = getResult(None, None, isAgent = false)

        "redirect the user to the overview page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${appConfig.incomeTaxSubmissionOverviewUrl(taxYear)}"
        }
      }

      "there is cya data, but 'overseasDonationsViaGiftAidAmount' has not been stored" should {

        lazy val result: WSResponse = getResult(Some(GiftAidCYAModel(overseasDonationsViaGiftAid = Some(true))), None, isAgent = false)

        "redirect the user to the overseas donation amount page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOverseasAmountController.show(taxYear)}"
        }
      }

      "'overseasDonationsViaGiftAidAmount' exists - with the correct english content" should {
        lazy val result: WSResponse = getResult(requiredSessionData, None, isAgent = false)

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an OK(200) status" in {
          result.status shouldBe OK
        }
        titleCheck(expectedTitle)
        h1Check(expectedH1 + " " + expectedCaption)
        welshToggleCheck("English")
        textOnPageCheck(expectedCaption, captionSelector)
        textOnPageCheck(expectedInputHintText, inputHintTextSelector)
        inputFieldCheck(expectedInputName, inputFieldSelector)
        buttonCheck(expectedButtonText, buttonSelector)
      }
    }

    s"Calling POST $overseasCharityNameUrl" should {

      "there is no cya data stored" should {

        lazy val result: WSResponse = postResult(None, None, isAgent = false, Map("amount" -> "123000.42"))

        "redirect the user to the overview page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${appConfig.incomeTaxSubmissionOverviewUrl(taxYear)}"
        }
      }

      "there is data stored" when {

        "the user has not entered a name" which {
          lazy val result: WSResponse = postResult(requiredSessionData, None, isAgent = false, Map("name" -> ""))

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          errorSummaryCheck(expectedError, inputFieldSelector)
          errorAboveElementCheck(expectedError)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }

        "the user enters a name with an invalid character" which {
          lazy val result: WSResponse = postResult(requiredSessionData, None, isAgent = false, Map("name" -> "juamal|"))

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          errorSummaryCheck(expectedInvalidCharError, inputFieldSelector)
          errorAboveElementCheck(expectedInvalidCharError)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }

        "the user enters a name that exceeds the character limit" which {
          lazy val result: WSResponse = postResult(requiredSessionData, None, isAgent = false, Map("name" -> charLimit))

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          errorSummaryCheck(expectedCharLimitError, inputFieldSelector)
          errorAboveElementCheck(expectedCharLimitError)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }

        "the user enters a duplicate name" which {
          lazy val result: WSResponse =
            postResult(
              Some(requiredSessionCyaModel.copy(overseasCharityNames = Some(Seq("Dudes In Need")))),
              None,
              isAgent = false,
              Map("name" -> "Dudes In Need"))

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          errorSummaryCheck(expectedDuplicateError, inputFieldSelector)
          errorAboveElementCheck(expectedDuplicateError)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }

        "the user enters a valid name" when {
          lazy val result: WSResponse = postResult(requiredSessionData, None, isAgent = false, Map("name" -> "Dudes In Need"))

          "redirect the user to the 'overseas charity summary' page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.OverseasGiftAidSummaryController.show(taxYear)}"
          }
        }
      }
    }
  }

}
