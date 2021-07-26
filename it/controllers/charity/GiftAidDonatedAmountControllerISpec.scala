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
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{GiftAidDatabaseHelper, IntegrationTest, ViewHelpers}

class GiftAidDonatedAmountControllerISpec extends IntegrationTest with ViewHelpers with GiftAidDatabaseHelper {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val taxYear: Int = 2022
  val gitAidDonatedAmountUrl = s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/amount-donated-using-gift-aid"

  object IndividualExpected {
    val expectedTitle = "How much did you donate to charity by using Gift Aid?"
    val expectedH1 = "How much did you donate to charity by using Gift Aid?"
    val expectedParagraph = "Do not include the Gift Aid that was added to your donation."
    val expectedErrorEmpty = "Enter the amount you donated to charity by using Gift Aid"
    val expectedErrorOverMax = "The amount you donated to charity must be less than £100,000,000,000"
    val expectedErrorBadFormat = "Enter the amount you donated to charity in the correct format"
    val expectedErrorTitle = s"Error: $expectedTitle"

    val expectedTitleCy = "How much did you donate to charity by using Gift Aid?"
    val expectedH1Cy = "How much did you donate to charity by using Gift Aid?"
    val expectedParagraphCy = "Do not include the Gift Aid that was added to your donation."
    val expectedErrorEmptyCy = "Enter the amount you donated to charity by using Gift Aid"
    val expectedErrorOverMaxCy = "The amount you donated to charity must be less than £100,000,000,000"
    val expectedErrorBadFormatCy = "Enter the amount you donated to charity in the correct format"
    val expectedErrorTitleCy = s"Error: $expectedTitle"
  }

  object AgentExpected {
    val expectedTitle = "How much did your client donate to charity by using Gift Aid?"
    val expectedH1 = "How much did your client donate to charity by using Gift Aid?"
    val expectedParagraph = "Do not include the Gift Aid that was added to your client’s donation."
    val expectedErrorEmpty = "Enter the amount your client donated to charity by using Gift Aid"
    val expectedErrorOverMax = "The amount your client donated to charity must be less than £100,000,000,000"
    val expectedErrorBadFormat = "Enter the amount your client donated to charity in the correct format"
    val expectedErrorTitle = s"Error: $expectedTitle"

    val expectedTitleCy = "How much did your client donate to charity by using Gift Aid?"
    val expectedH1Cy = "How much did your client donate to charity by using Gift Aid?"
    val expectedParagraphCy = "Do not include the Gift Aid that was added to your client’s donation."
    val expectedErrorEmptyCy = "Enter the amount your client donated to charity by using Gift Aid"
    val expectedErrorOverMaxCy = "The amount your client donated to charity must be less than £100,000,000,000"
    val expectedErrorBadFormatCy = "Enter the amount your client donated to charity in the correct format"
    val expectedErrorTitleCy = s"Error: $expectedTitle"
  }

  val expectedCaption = "Donations to charity for 6 April 2021 to 5 April 2022"
  val expectedInputName = "amount"
  val expectedButtonText = "Continue"
  val expectedInputLabelText = "Total amount for the year"
  val expectedInputHintText = "For example, £600 or £193.54"

  val expectedCaptionCy = "Donations to charity for 6 April 2021 to 5 April 2022"
  val expectedInputNameCy = "amount"
  val expectedButtonTextCy = "Continue"
  val expectedInputLabelTextCy = "Total amount for the year"
  val expectedInputHintTextCy = "For example, £600 or £193.54"

  val expectedErrorLink = "#amount"
  val captionSelector = ".govuk-caption-l"
  val paragraphSelector = "#main-content > div > div > form > div > label > p"
  val inputFieldSelector = "#amount"
  val buttonSelector = ".govuk-button"
  val inputLabelSelector = "#main-content > div > div > form > div > label > div"
  val inputHintTextSelector = ".govuk-hint"

  val requiredSessionModel: GiftAidCYAModel = GiftAidCYAModel(donationsViaGiftAid = Some(true))
  val requiredSessionData: Some[GiftAidCYAModel] = Some(requiredSessionModel)

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
      await(wsClient.url(gitAidDonatedAmountUrl).withHttpHeaders(
        HeaderNames.COOKIE -> agentSessionCookie, langHeader, xSessionId, csrfContent)
        .withFollowRedirects(false).get
      )
    } else {
      authoriseIndividual()
      await(wsClient.url(gitAidDonatedAmountUrl).withHttpHeaders(langHeader, xSessionId, csrfContent)
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
      await(wsClient.url(gitAidDonatedAmountUrl).withHttpHeaders(
        HeaderNames.COOKIE -> agentSessionCookie, langHeader, xSessionId, csrfContent)
        .withFollowRedirects(false).post(input)
      )
    } else {
      authoriseIndividual()
      await(wsClient.url(gitAidDonatedAmountUrl).withHttpHeaders(
        langHeader, xSessionId, csrfContent)
          .withFollowRedirects(false).post(input)
      )
    }
  }

  "as an individual" when {
    import IndividualExpected._
    s"Calling GET $gitAidDonatedAmountUrl" when {

      "there is no cya data" should {

        lazy val result: WSResponse = getResult(None, None, isAgent = false)

        "redirect the user to the overview page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${appConfig.incomeTaxSubmissionOverviewUrl(taxYear)}"
        }
      }

      "there is cya data, but 'donationsViaGiftAid' has not been stored" should {

        lazy val result: WSResponse = getResult(Some(GiftAidCYAModel()), None, isAgent = false)

        "redirect the user to the gift aid donation page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidDonationsController.show(taxYear)}"
        }
      }

      "'donationsViaGiftAid' exists and is false" which {
        lazy val result: WSResponse = getResult(Some(GiftAidCYAModel(donationsViaGiftAid = Some(false))), None, isAgent = false)

        "redirect the user to the gift aid donation page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidDonationsController.show(taxYear)}"
        }
      }

      "'donationsViaGiftAid' exists and is true - with the correct english content" which {
        lazy val result: WSResponse = getResult(requiredSessionData, None, isAgent = false)

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        titleCheck(expectedTitle)
        h1Check(expectedH1 + " " + expectedCaption)
        welshToggleCheck("English")
        textOnPageCheck(expectedCaption, captionSelector)
        textOnPageCheck(expectedParagraph, paragraphSelector)
        textOnPageCheck(expectedInputLabelText, inputLabelSelector)
        textOnPageCheck(expectedInputHintText, inputHintTextSelector)
        inputFieldCheck(expectedInputName, inputFieldSelector)
        buttonCheck(expectedButtonText, buttonSelector)

      }

      "'donationsViaGiftAid' exists and is true - with the correct welsh content" which {
        lazy val result: WSResponse = getResult(requiredSessionData, None, welsh = true, isAgent = false)

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        titleCheck(expectedTitleCy)
        h1Check(expectedH1Cy + " " + expectedCaptionCy)
        welshToggleCheck("Welsh")
        textOnPageCheck(expectedCaptionCy, captionSelector)
        textOnPageCheck(expectedParagraphCy, paragraphSelector)
        textOnPageCheck(expectedInputLabelTextCy, inputLabelSelector)
        textOnPageCheck(expectedInputHintTextCy, inputHintTextSelector)
        inputFieldCheck(expectedInputNameCy, inputFieldSelector)
        buttonCheck(expectedButtonTextCy, buttonSelector)

      }
    }

    s"Calling POST $gitAidDonatedAmountUrl" when {

      "there is no cya data stored" should {

        lazy val result: WSResponse = postResult(None, None, isAgent = false, Map("amount" -> "123000.42"))

        "redirect the user to the overview page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${appConfig.incomeTaxSubmissionOverviewUrl(taxYear)}"
        }
      }

      "there is cya data stored" when {

        "the user has not entered an amount" which {
          lazy val result: WSResponse = postResult(requiredSessionData, None, isAgent = false, Map("amount" -> ""))

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          errorSummaryCheck(expectedErrorEmpty, expectedErrorLink)
          errorAboveElementCheck(expectedErrorEmpty)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }

        "the user has entered an amount too large" which {
          lazy val result: WSResponse = postResult(requiredSessionData, None, isAgent = false, Map("amount" -> "999999999999"))

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          errorSummaryCheck(expectedErrorOverMax, expectedErrorLink)
          errorAboveElementCheck(expectedErrorOverMax)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }

        "the user has entered an invalid amount" which {
          lazy val result: WSResponse = postResult(requiredSessionData, None, isAgent = false, Map("amount" -> "|"))

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          errorSummaryCheck(expectedErrorBadFormat, expectedErrorLink)
          errorAboveElementCheck(expectedErrorBadFormat)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }

        "the user has not entered an amount - with welsh toggled" which {
          lazy val result: WSResponse = postResult(requiredSessionData, None, isAgent = false, Map("amount" -> ""), welsh = true)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          welshToggleCheck("Welsh")
          errorSummaryCheck(expectedErrorEmptyCy, expectedErrorLink)
          errorAboveElementCheck(expectedErrorEmptyCy)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }

        "the user has entered an amount too large - with welsh toggled" which {
          lazy val result: WSResponse = postResult(requiredSessionData, None, isAgent = false, Map("amount" -> "999999999999"), welsh = true)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          welshToggleCheck("Welsh")
          errorSummaryCheck(expectedErrorOverMaxCy, expectedErrorLink)
          errorAboveElementCheck(expectedErrorOverMaxCy)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }

        "the user has entered an invalid amount - with welsh toggled" which {
          lazy val result: WSResponse = postResult(requiredSessionData, None, isAgent = false, Map("amount" -> "|"), welsh = true)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          welshToggleCheck("Welsh")
          errorSummaryCheck(expectedErrorBadFormatCy, expectedErrorLink)
          errorAboveElementCheck(expectedErrorBadFormatCy)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }

        "the user has entered a valid amount" when {
          val validAmount = 123

          "the cya data is updated successfully" should {
            lazy val result: WSResponse = postResult(requiredSessionData, None, isAgent = false, Map("amount" -> s"$validAmount"))

            "redirect the user to the 'One off donations' page" in {
              result.status shouldBe SEE_OTHER
              result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOneOffController.show(taxYear)}"
            }

            "update the cya data" in {
              findGiftAidDb shouldBe Some(requiredSessionModel.copy(donationsViaGiftAidAmount = Some(validAmount)))
            }
          }
        }
      }
    }
  }

  "as an agent" when {
    import AgentExpected._
    s"Calling GET $gitAidDonatedAmountUrl" when {

      "there is no cya data" should {

        lazy val result: WSResponse = getResult(None, None, isAgent = true)

        "redirect the user to the overview page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${appConfig.incomeTaxSubmissionOverviewUrl(taxYear)}"
        }
      }

      "there is cya data, but 'donationsViaGiftAid' has not been stored" should {

        lazy val result: WSResponse = getResult(Some(GiftAidCYAModel()), None, isAgent = true)

        "redirect the user to the gift aid donation page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidDonationsController.show(taxYear)}"
        }
      }

      "'donationsViaGiftAid' exists and is false" which {
        lazy val result: WSResponse = getResult(Some(GiftAidCYAModel(donationsViaGiftAid = Some(false))), None, isAgent = true)

        "redirect the user to the gift aid donation page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidDonationsController.show(taxYear)}"
        }
      }

      "'donationsViaGiftAid' exists and is true - with the correct english content" which {
        lazy val result: WSResponse = getResult(requiredSessionData, None, isAgent = true)

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        titleCheck(expectedTitle)
        h1Check(expectedH1 + " " + expectedCaption)
        welshToggleCheck("English")
        textOnPageCheck(expectedCaption, captionSelector)
        textOnPageCheck(expectedParagraph, paragraphSelector)
        textOnPageCheck(expectedInputLabelText, inputLabelSelector)
        textOnPageCheck(expectedInputHintText, inputHintTextSelector)
        inputFieldCheck(expectedInputName, inputFieldSelector)
        buttonCheck(expectedButtonText, buttonSelector)

      }

      "'donationsViaGiftAid' exists and is true - with the correct welsh content" which {
        lazy val result: WSResponse = getResult(requiredSessionData, None, isAgent = true, welsh = true)

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        titleCheck(expectedTitleCy)
        h1Check(expectedH1Cy + " " + expectedCaptionCy)
        welshToggleCheck("Welsh")
        textOnPageCheck(expectedCaptionCy, captionSelector)
        textOnPageCheck(expectedParagraphCy, paragraphSelector)
        textOnPageCheck(expectedInputLabelTextCy, inputLabelSelector)
        textOnPageCheck(expectedInputHintTextCy, inputHintTextSelector)
        inputFieldCheck(expectedInputNameCy, inputFieldSelector)
        buttonCheck(expectedButtonTextCy, buttonSelector)

      }
    }

    s"Calling POST $gitAidDonatedAmountUrl" when {

      "there is no cya data stored" should {

        lazy val result: WSResponse = postResult(None, None, isAgent = true, Map("amount" -> "123000.42"))

        "redirect the user to the overview page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${appConfig.incomeTaxSubmissionOverviewUrl(taxYear)}"
        }
      }

      "there is cya data stored" when {

        "the user has not entered an amount" which {
          lazy val result: WSResponse = postResult(requiredSessionData, None, isAgent = true, Map("amount" -> ""))

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          errorSummaryCheck(expectedErrorEmpty, expectedErrorLink)
          errorAboveElementCheck(expectedErrorEmpty)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }

        "the user has entered an amount too large" which {
          lazy val result: WSResponse = postResult(requiredSessionData, None, isAgent = true, Map("amount" -> "999999999999"))

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          errorSummaryCheck(expectedErrorOverMax, expectedErrorLink)
          errorAboveElementCheck(expectedErrorOverMax)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }

        "the user has entered an invalid amount" which {
          lazy val result: WSResponse = postResult(requiredSessionData, None, isAgent = true, Map("amount" -> "|"))

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          errorSummaryCheck(expectedErrorBadFormat, expectedErrorLink)
          errorAboveElementCheck(expectedErrorBadFormat)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }

        "the user has not entered an amount - with welsh toggled" which {
          lazy val result: WSResponse = postResult(requiredSessionData, None, isAgent = true, Map("amount" -> ""), welsh = true)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          welshToggleCheck("Welsh")
          errorSummaryCheck(expectedErrorEmptyCy, expectedErrorLink)
          errorAboveElementCheck(expectedErrorEmptyCy)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }

        "the user has entered an amount too large - with welsh toggled" which {
          lazy val result: WSResponse = postResult(requiredSessionData, None, isAgent = true, Map("amount" -> "999999999999"), welsh = true)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          welshToggleCheck("Welsh")
          errorSummaryCheck(expectedErrorOverMaxCy, expectedErrorLink)
          errorAboveElementCheck(expectedErrorOverMaxCy)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }

        "the user has entered an invalid amount - with welsh toggled" which {
          lazy val result: WSResponse = postResult(requiredSessionData, None, isAgent = true, Map("amount" -> "|"), welsh = true)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          welshToggleCheck("Welsh")
          errorSummaryCheck(expectedErrorBadFormatCy, expectedErrorLink)
          errorAboveElementCheck(expectedErrorBadFormatCy)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }

        "the user has entered a valid amount" when {
          val validAmount = 123

          "this completes the CYA model" should {
            lazy val result: WSResponse =
              postResult(Some(completeGiftAidCYAModel.copy(donationsViaGiftAidAmount = None)), None, isAgent = true, Map("amount" -> s"$validAmount"))

            "redirect the user to the 'check your answers' page" in {
              result.status shouldBe SEE_OTHER
              result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(taxYear)}"
            }

            "update the cya data" in {
              findGiftAidDb shouldBe Some(completeGiftAidCYAModel.copy(donationsViaGiftAidAmount = Some(validAmount)))
            }
          }

          "this does not complete the CYA model" should {
            lazy val result: WSResponse = postResult(requiredSessionData, None, isAgent = true, Map("amount" -> s"$validAmount"))

            "redirect the user to the 'One off donations' page" in {
              result.status shouldBe SEE_OTHER
              result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOneOffController.show(taxYear)}"
            }

            "update the cya data" in {
              findGiftAidDb shouldBe Some(requiredSessionModel.copy(donationsViaGiftAidAmount = Some(validAmount)))
            }
          }
        }
      }
    }
  }
}
