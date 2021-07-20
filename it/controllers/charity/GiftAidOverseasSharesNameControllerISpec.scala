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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{GiftAidDatabaseHelper, IntegrationTest}

class GiftAidOverseasSharesNameControllerISpec extends IntegrationTest with GiftAidDatabaseHelper {


  object IndividualExpected {
    val expectedTitle: String = "Name of overseas charity you donated shares, securities, land or property to"
    val expectedH1: String = "Name of overseas charity you donated shares, securities, land or property to"
    val expectedError: String = "Enter the name of the overseas charity you donated shares, securities, land or property to"
    val expectedErrorTitle = s"Error: $expectedTitle"

    val expectedTitleCy: String = "Name of overseas charity you donated shares, securities, land or property to"
    val expectedH1Cy: String = "Name of overseas charity you donated shares, securities, land or property to"
    val expectedErrorCy: String = "Enter the name of the overseas charity you donated shares, securities, land or property to"
    val expectedErrorTitleCy = s"Error: $expectedTitleCy"
  }

  object AgentExpected {
    val expectedTitle: String = "Name of overseas charity your client donated shares, securities, land or property to"
    val expectedH1: String = "Name of overseas charity your client donated shares, securities, land or property to"
    val expectedError: String = "Enter the name of the overseas charity your client donated shares, securities, land or property to"
    val expectedErrorTitle = s"Error: $expectedTitle"

    val expectedTitleCy: String = "Name of overseas charity your client donated shares, securities, land or property to"
    val expectedH1Cy: String = "Name of overseas charity your client donated shares, securities, land or property to"
    val expectedErrorCy: String = "Enter the name of the overseas charity your client donated shares, securities, land or property to"
    val expectedErrorTitleCy = s"Error: $expectedTitleCy"
  }

  val expectedCaption: String = "Donations to charity for 6 April 2021 to 5 April 2022"
  val expectedCaptionCy: String = "Donations to charity for 6 April 2021 to 5 April 2022"
  val expectedInputName: String = "name"
  val expectedButtonText: String = "Continue"
  val expectedButtonTextCy: String = "Continue"
  val expectedInputHintText: String = "You can add more than one charity."
  val expectedInputHintTextCy: String = "You can add more than one charity."
  val expectedCharLimitError: String = "The name of the overseas charity must be 75 characters or fewer"
  val expectedCharLimitErrorCy: String = "The name of the overseas charity must be 75 characters or fewer"
  val expectedInvalidCharError: String = "Name of overseas charity must only include numbers 0-9, letters a " +
    "to z, hyphens, spaces, apostrophes, commas, full stops, round brackets and the special characters, &, /, @, £, *"
  val expectedInvalidCharErrorCy: String = "Name of overseas charity must only include numbers 0-9, letters a " +
    "to z, hyphens, spaces, apostrophes, commas, full stops, round brackets and the special characters, &, /, @, £, *"
  val expectedDuplicateError: String = "You cannot add 2 charities with the same name"
  val expectedDuplicateErrorCy: String = "You cannot add 2 charities with the same name"

  val captionSelector: String = ".govuk-caption-l"
  val inputFieldSelector: String = "#name"
  val buttonSelector: String = ".govuk-button"
  val inputHintTextSelector: String = "#main-content > div > div > form > div > label > p"
  val errorSelector: String = "#main-content > div > div > div.govuk-error-summary > div > ul > li > a"

  val serviceName = "Update and submit an Income Tax Return"
  val serviceNameCy = "Update and submit an Income Tax Return"
  val govUkExtension = "GOV.UK"

  val charLimit: String = "ukHzoBYHkKGGk2V5iuYgS137gN7EB7LRw3uDjvujYg00ZtHwo3sokyOOCEoAK9vuPiP374QKOelo"
  val testModel: GiftAidCYAModel =
    GiftAidCYAModel(overseasDonatedSharesSecuritiesLandOrPropertyAmount = Some(100.00), overseasDonatedSharesSecuritiesLandOrPropertyCharityNames = Some(List("JaneDoe")))

  val testModelEmpty: GiftAidCYAModel =
    GiftAidCYAModel(overseasDonatedSharesSecuritiesLandOrPropertyAmount = Some(100.00))

  val testModelFalse: GiftAidCYAModel =
    GiftAidCYAModel(overseasDonatedSharesSecuritiesLandOrProperty = Some(true))


  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val taxYear: Int = 2022
  val fullNino = "AA000003A"

  "as an individual" when {
    import IndividualExpected._

    ".show" should {

      "returns an action with english content" which {

        lazy val result: WSResponse = {
          dropGiftAidDB()
          emptyUserDataStub()
          insertCyaData(Some(testModel))
          authoriseIndividual()
          await(wsClient.url(
            s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/" +
              s"charity/name-of-overseas-charities-donated-shares-securities-land-or-property-to"
          )
            .withHttpHeaders(xSessionId, csrfContent)
            .get())
        }
        lazy val document: Document = Jsoup.parse(result.body)

        "has an OK(200) status with the correct content" in {
          result.status shouldBe OK
          document.title() shouldBe s"$expectedTitle - $serviceName - $govUkExtension"
          document.select(".govuk-heading-l").text() shouldBe expectedH1 + " " + expectedCaption
          document.select(captionSelector).text() shouldBe expectedCaption
          document.select(inputHintTextSelector).text() shouldBe expectedInputHintText
          document.select(inputFieldSelector).attr("name")
          document.select(buttonSelector).text() shouldBe expectedButtonText
          document.select(buttonSelector).attr("class") should include("govuk-button")
        }

      }
      "returns an action without previousNames" which {

        lazy val result: WSResponse = {
          dropGiftAidDB()
          emptyUserDataStub()
          insertCyaData(Some(testModelEmpty))
          authoriseIndividual()
          await(wsClient.url(
            s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/" +
              s"charity/name-of-overseas-charities-donated-shares-securities-land-or-property-to"
          )
            .withHttpHeaders(xSessionId, csrfContent)
            .get())
        }
        lazy val document: Document = Jsoup.parse(result.body)

        "has an OK(200) status with the correct content" in {
          result.status shouldBe OK
        }

      }

      "returns an action with welsh content" which {
        lazy val result: WSResponse = {
          dropGiftAidDB()
          emptyUserDataStub()
          insertCyaData(Some(testModel))
          authoriseIndividual()
          await(wsClient.url(
            s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/" +
              s"charity/name-of-overseas-charities-donated-shares-securities-land-or-property-to"
          )
            .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy", xSessionId, csrfContent)
            .get())
        }
        lazy val document: Document = Jsoup.parse(result.body)

        "has an OK(200) status with the correct content" in {
          result.status shouldBe OK
          document.title() shouldBe s"$expectedTitleCy - $serviceNameCy - $govUkExtension"
          document.select(".govuk-heading-l").text() shouldBe expectedH1Cy + " " + expectedCaptionCy
          document.select(captionSelector).text() shouldBe expectedCaptionCy
          document.select(inputHintTextSelector).text() shouldBe expectedInputHintTextCy
          document.select(inputFieldSelector).attr("name")
          document.select(buttonSelector).text() shouldBe expectedButtonTextCy
          document.select(buttonSelector).attr("class") should include("govuk-button")
        }
      }
      "return the overview page when there is no data" which {
        lazy val result: WSResponse = {
          dropGiftAidDB()
          emptyUserDataStub()
          authoriseIndividual()
          await(wsClient
            .url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/" +
              s"charity/name-of-overseas-charities-donated-shares-securities-land-or-property-to"
            )
            .withHttpHeaders(xSessionId, csrfContent)
            .withFollowRedirects(false)
            .get())
        }

        "has a status of SEE_OTHER(303)" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the overview page" in {
          result.headers("Location").head shouldBe overviewUrl
        }
      }
      "return the OverseasSharesSecuritiesLandPropertyAmount page when there is no overseasDonatedSharesSecuritiesLandOrPropertyAmount" which {
        lazy val result: WSResponse = {
          dropGiftAidDB()
          emptyUserDataStub()
          insertCyaData(Some(testModelFalse))
          authoriseIndividual()
          await(wsClient
            .url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/" +
              s"charity/name-of-overseas-charities-donated-shares-securities-land-or-property-to"
            )
            .withHttpHeaders(xSessionId, csrfContent)
            .withFollowRedirects(false)
            .get())
        }

        "has a status of SEE_OTHER(303)" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the OverseasSharesSecuritiesLandPropertyAmountController page" in {
          result.headers("Location").head shouldBe s"${controllers.charity.routes.OverseasSharesSecuritiesLandPropertyAmountController.show(taxYear)}"
        }
      }
    }

    ".submit" should {

      s"return an OK($OK) status when there are previous names" in {
        lazy val result: WSResponse = {
          dropGiftAidDB()
          emptyUserDataStub()
          insertCyaData(Some(testModel))
          authoriseIndividual()
          await(
            wsClient.url(
              s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/" +
                s"charity/name-of-overseas-charities-donated-shares-securities-land-or-property-to"
            )
              .withHttpHeaders(xSessionId, csrfContent)
              .post(Map("name" -> "adam"))
          )
        }

        result.status shouldBe OK
      }
      s"return an OK($OK) status when there are no previous names" in {
        lazy val result: WSResponse = {
          dropGiftAidDB()
          emptyUserDataStub()
          insertCyaData(Some(testModelEmpty))
          authoriseIndividual()
          await(
            wsClient.url(
              s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/" +
                s"charity/name-of-overseas-charities-donated-shares-securities-land-or-property-to"
            )
              .withHttpHeaders(xSessionId, csrfContent)
              .post(Map("name" -> "adam"))
          )
        }

        result.status shouldBe OK
      }

      s"return a Redirect to the overview page when there is no data" in {
        lazy val result: WSResponse = {
          dropGiftAidDB()
          emptyUserDataStub()
          authoriseIndividual()
          await(
            wsClient.url(
              s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/" +
                s"charity/name-of-overseas-charities-donated-shares-securities-land-or-property-to"
            )
              .withHttpHeaders(xSessionId, csrfContent)
              .withFollowRedirects(false)
              .post(Map("name" -> "adam"))
          )
        }

        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe overviewUrl
      }
      s"return a Redirect to the overview page when there is empty data" in {
        lazy val result: WSResponse = {
          dropGiftAidDB()
          emptyUserDataStub()
          insertCyaData(None)
          authoriseIndividual()
          await(
            wsClient.url(
              s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/" +
                s"charity/name-of-overseas-charities-donated-shares-securities-land-or-property-to"
            )
              .withHttpHeaders(xSessionId, csrfContent)
              .withFollowRedirects(false)
              .post(Map("name" -> "adam"))
          )
        }

        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe overviewUrl
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with an empty error in english" in {
        lazy val result: WSResponse = {
          dropGiftAidDB()
          emptyUserDataStub()
          insertCyaData(Some(testModel))
          authoriseIndividual()
          await(wsClient.url(
            s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/" +
              s"charity/name-of-overseas-charities-donated-shares-securities-land-or-property-to"
          )
            .withHttpHeaders(xSessionId, csrfContent)
            .post(Map[String, String]()))
        }
        lazy val document: Document = Jsoup.parse(result.body)

        result.status shouldBe BAD_REQUEST
        document.select(errorSelector).text() shouldBe expectedError
        document.title() shouldBe s"$expectedErrorTitle - $serviceName - $govUkExtension"
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with an invalid Character error in english" in {
        lazy val result: WSResponse = {
          dropGiftAidDB()
          emptyUserDataStub()
          insertCyaData(Some(testModel))
          authoriseIndividual()
          await(
            wsClient.url(
              s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/" +
                s"charity/name-of-overseas-charities-donated-shares-securities-land-or-property-to"
            )
              .withHttpHeaders(xSessionId, csrfContent)
              .post(Map("name" -> "ad|am"))
          )
        }
        lazy val document: Document = Jsoup.parse(result.body)

        result.status shouldBe BAD_REQUEST
        document.select(errorSelector).text() shouldBe expectedInvalidCharError
        document.title() shouldBe s"$expectedErrorTitle - $serviceName - $govUkExtension"
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with a character limit error in english" in {
        lazy val result: WSResponse = {
          dropGiftAidDB()
          emptyUserDataStub()
          insertCyaData(Some(testModel))
          authoriseIndividual()
          await(
            wsClient.url(
              s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/" +
                s"charity/name-of-overseas-charities-donated-shares-securities-land-or-property-to"
            )
              .withHttpHeaders(xSessionId, csrfContent)
              .post(Map("name" -> charLimit))
          )
        }
        lazy val document: Document = Jsoup.parse(result.body)

        result.status shouldBe BAD_REQUEST
        document.select(errorSelector).text() shouldBe expectedCharLimitError
        document.title() shouldBe s"$expectedErrorTitle - $serviceName - $govUkExtension"
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with an duplicate name error in english" in {

        lazy val result: WSResponse = {
          dropGiftAidDB()
          emptyUserDataStub()
          insertCyaData(Some(testModel))
          authoriseIndividual()
          await(
            wsClient.url(
              s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/" +
                s"charity/name-of-overseas-charities-donated-shares-securities-land-or-property-to"
            )
              .withHttpHeaders(xSessionId, csrfContent)
              .post(Map("name" -> "JaneDoe"))
          )
        }
        lazy val document: Document = Jsoup.parse(result.body)

        result.status shouldBe BAD_REQUEST
        document.select(errorSelector).text() shouldBe expectedDuplicateError
        document.title() shouldBe s"$expectedErrorTitle - $serviceName - $govUkExtension"
      }

    }

    s"return a BAD_REQUEST($BAD_REQUEST) status with an empty error in welsh" in {
      lazy val result: WSResponse = {
        dropGiftAidDB()
        emptyUserDataStub()
        insertCyaData(Some(testModel))
        authoriseIndividual()
        await(wsClient.url(
          s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/" +
            s"charity/name-of-overseas-charities-donated-shares-securities-land-or-property-to"
        )
          .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy", xSessionId, csrfContent)
          .post(Map[String, String]()))
      }
      lazy val document: Document = Jsoup.parse(result.body)

      result.status shouldBe BAD_REQUEST
      document.select(errorSelector).text() shouldBe expectedErrorCy
      document.title() shouldBe s"$expectedErrorTitleCy - $serviceNameCy - $govUkExtension"
    }

    s"return a BAD_REQUEST($BAD_REQUEST) status with an invalid Character error in welsh" in {
      lazy val result: WSResponse = {
        dropGiftAidDB()
        emptyUserDataStub()
        insertCyaData(Some(testModel))
        authoriseIndividual()
        await(
          wsClient.url(
            s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/" +
              s"charity/name-of-overseas-charities-donated-shares-securities-land-or-property-to"
          )
            .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy", xSessionId, csrfContent)
            .post(Map("name" -> "ad|am"))
        )
      }
      lazy val document: Document = Jsoup.parse(result.body)

      result.status shouldBe BAD_REQUEST
      document.select(errorSelector).text() shouldBe expectedInvalidCharErrorCy
      document.title() shouldBe s"$expectedErrorTitleCy - $serviceNameCy - $govUkExtension"
    }

    s"return a BAD_REQUEST($BAD_REQUEST) status with a character limit error in welsh" in {
      lazy val result: WSResponse = {
        dropGiftAidDB()
        emptyUserDataStub()
        insertCyaData(Some(testModel))
        authoriseIndividual()
        await(
          wsClient.url(
            s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/" +
              s"charity/name-of-overseas-charities-donated-shares-securities-land-or-property-to"
          )
            .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy", xSessionId, csrfContent)
            .post(Map("name" -> charLimit))
        )
      }
      lazy val document: Document = Jsoup.parse(result.body)

      result.status shouldBe BAD_REQUEST
      document.select(errorSelector).text() shouldBe expectedCharLimitErrorCy
      document.title() shouldBe s"$expectedErrorTitleCy - $serviceNameCy - $govUkExtension"
    }

    s"return a BAD_REQUEST($BAD_REQUEST) status with an duplicate name error in welsh" in {

      lazy val result: WSResponse = {
        dropGiftAidDB()
        emptyUserDataStub()
        insertCyaData(Some(testModel))
        authoriseIndividual()
        await(
          wsClient.url(
            s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/" +
              s"charity/name-of-overseas-charities-donated-shares-securities-land-or-property-to"
          )
            .withHttpHeaders(HeaderNames.ACCEPT_LANGUAGE -> "cy", xSessionId, csrfContent)
            .post(Map("name" -> "JaneDoe"))
        )
      }
      lazy val document: Document = Jsoup.parse(result.body)

      result.status shouldBe BAD_REQUEST
      document.select(errorSelector).text() shouldBe expectedDuplicateErrorCy
      document.title() shouldBe s"$expectedErrorTitleCy - $serviceNameCy - $govUkExtension"
    }
  }

  "as an agent" when {
    import AgentExpected._

    ".show" should {

      "returns an action with english content" which {
        lazy val result: WSResponse = {
          dropGiftAidDB()
          emptyUserDataStub()
          insertCyaData(Some(testModel))
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(
            s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/" +
              s"charity/name-of-overseas-charities-donated-shares-securities-land-or-property-to"
          )
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
            .get())
        }
        lazy val document: Document = Jsoup.parse(result.body)

        "has an OK(200) status with the correct content" in {
          result.status shouldBe OK
          document.title() shouldBe s"$expectedTitle - $serviceName - $govUkExtension"
          document.select(".govuk-heading-l").text() shouldBe expectedH1 + " " + expectedCaption
          document.select(captionSelector).text() shouldBe expectedCaption
          document.select(inputHintTextSelector).text() shouldBe expectedInputHintText
          document.select(inputFieldSelector).attr("name")
          document.select(buttonSelector).text() shouldBe expectedButtonText
          document.select(buttonSelector).attr("class") should include("govuk-button")
        }
      }

      "returns an action with welsh content" which {
        lazy val result: WSResponse = {
          dropGiftAidDB()
          emptyUserDataStub()
          insertCyaData(Some(testModel))
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(
            s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/" +
              s"charity/name-of-overseas-charities-donated-shares-securities-land-or-property-to"
          )
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", xSessionId, csrfContent)
            .get())
        }
        lazy val document: Document = Jsoup.parse(result.body)

        "has an OK(200) status with the correct content" in {
          result.status shouldBe OK
          document.title() shouldBe s"$expectedTitleCy - $serviceNameCy - $govUkExtension"
          document.select(".govuk-heading-l").text() shouldBe expectedH1Cy + " " + expectedCaptionCy
          document.select(captionSelector).text() shouldBe expectedCaptionCy
          document.select(inputHintTextSelector).text() shouldBe expectedInputHintTextCy
          document.select(inputFieldSelector).attr("name")
          document.select(buttonSelector).text() shouldBe expectedButtonTextCy
          document.select(buttonSelector).attr("class") should include("govuk-button")
        }
      }
    }

    ".submit" should {

      s"return an OK($OK) status" when {

        "there is form data" in {
          lazy val result: WSResponse = {
            dropGiftAidDB()
            emptyUserDataStub()
            insertCyaData(Some(testModel))
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"))

            authoriseAgent()
            await(
              wsClient.url(
                s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/" +
                  s"charity/name-of-overseas-charities-donated-shares-securities-land-or-property-to"
              )
                .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
                .post(Map("name" -> "adam"))
            )
          }

          result.status shouldBe OK
        }
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with an empty error in english" when {

        "there is no form data" in {
          lazy val result: WSResponse = {
            dropGiftAidDB()
            emptyUserDataStub()
            insertCyaData(Some(testModel))
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"
            ))

            authoriseAgent()
            await(wsClient.url(
              s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/" +
                s"charity/name-of-overseas-charities-donated-shares-securities-land-or-property-to"
            )
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
              .post(Map[String, String]()))
          }
          lazy val document: Document = Jsoup.parse(result.body)

          result.status shouldBe BAD_REQUEST
          document.select(errorSelector).text() shouldBe expectedError
          document.title() shouldBe s"$expectedErrorTitle - $serviceName - $govUkExtension"
        }
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with an empty error in welsh" when {

        "there is no form data" in {
          lazy val result: WSResponse = {
            dropGiftAidDB()
            emptyUserDataStub()
            insertCyaData(Some(testModel))
            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"
            ))

            authoriseAgent()
            await(wsClient.url(
              s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/" +
                s"charity/name-of-overseas-charities-donated-shares-securities-land-or-property-to"
            )
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", xSessionId, csrfContent)
              .post(Map[String, String]()))
          }
          lazy val document: Document = Jsoup.parse(result.body)

          result.status shouldBe BAD_REQUEST
          document.select(errorSelector).text() shouldBe expectedErrorCy
          document.title() shouldBe s"$expectedErrorTitleCy - $serviceNameCy - $govUkExtension"
        }
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with an invalid Character error in welsh" in {
        lazy val result: WSResponse = {
          dropGiftAidDB()
          emptyUserDataStub()
          insertCyaData(Some(testModel))
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(
            wsClient.url(
              s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/" +
                s"charity/name-of-overseas-charities-donated-shares-securities-land-or-property-to"
            )
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", xSessionId, csrfContent)
              .post(Map("name" -> "ad|am"))
          )
        }
        lazy val document: Document = Jsoup.parse(result.body)

        result.status shouldBe BAD_REQUEST
        document.select(errorSelector).text() shouldBe expectedInvalidCharErrorCy
        document.title() shouldBe s"$expectedErrorTitleCy - $serviceNameCy - $govUkExtension"
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with a character limit error in welsh" in {
        lazy val result: WSResponse = {
          dropGiftAidDB()
          emptyUserDataStub()
          insertCyaData(Some(testModel))
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(
            wsClient.url(
              s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/" +
                s"charity/name-of-overseas-charities-donated-shares-securities-land-or-property-to"
            )
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", xSessionId, csrfContent)
              .post(Map("name" -> charLimit))
          )
        }
        lazy val document: Document = Jsoup.parse(result.body)

        result.status shouldBe BAD_REQUEST
        document.select(errorSelector).text() shouldBe expectedCharLimitErrorCy
        document.title() shouldBe s"$expectedErrorTitleCy - $serviceNameCy - $govUkExtension"
      }

      s"return a BAD_REQUEST($BAD_REQUEST) status with an duplicate name error in welsh" in {

        lazy val result: WSResponse = {
          dropGiftAidDB()
          emptyUserDataStub()
          insertCyaData(Some(testModel))
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(
            wsClient.url(
              s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/" +
                s"charity/name-of-overseas-charities-donated-shares-securities-land-or-property-to"
            )
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, HeaderNames.ACCEPT_LANGUAGE -> "cy", xSessionId, csrfContent)
              .post(Map("name" -> "JaneDoe"))
          )
        }
        lazy val document: Document = Jsoup.parse(result.body)

        result.status shouldBe BAD_REQUEST
        document.select(errorSelector).text() shouldBe expectedDuplicateErrorCy
        document.title() shouldBe s"$expectedErrorTitleCy - $serviceNameCy - $govUkExtension"
      }

    }

  }

}
