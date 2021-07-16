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
import forms.YesNoForm
import helpers.PlaySessionCookieBaker
import models.charity.GiftAidCYAModel
import models.charity.prior.{GiftAidPaymentsModel, GiftAidSubmissionModel}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{GiftAidDatabaseHelper, IntegrationTest, ViewHelpers}
import play.api.http.Status._

class RemoveOverseasCharityControllerGiftAidISpec extends IntegrationTest with ViewHelpers with GiftAidDatabaseHelper {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  object Selectors {

    val heading = "h1"
    val caption = ".govuk-caption-l"
    val content = "#main-content > div > div > form > div > fieldset > legend > p"
    val errorSummaryNoSelection = ".govuk-error-summary__body > ul > li > a"
    val yesRadioButton = ".govuk-radios__item:nth-child(1) > label"
    val noRadioButton = ".govuk-radios__item:nth-child(2) > label"

  }

  object Content {

    val charityName = "TestCharity"
    val expectedTitle = s"Are you sure you want to remove $charityName?"
    val expectedErrorTitle = "Select yes to remove this overseas charity"
    val expectedH1 = s"Are you sure you want to remove $charityName?"
    val expectedContent = "This will remove all overseas charities."
    val expectedCaption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val noSelectionError = "Select yes to remove this overseas charity"
    val yesText = "Yes"
    val noText = "No"
    val button = "Continue"
    val errorHref = "#value"

    val charityNameCy = "TestCharity"
    val expectedTitleCy = s"Are you sure you want to remove $charityName?"
    val expectedErrorTitleCy = "Select yes to remove this overseas charity"
    val expectedH1Cy = s"Are you sure you want to remove $charityName?"
    val expectedContentCy = "This will remove all overseas charities."
    val expectedCaptionCy = "Donations to charity for 6 April 2021 to 5 April 2022"
    val noSelectionErrorCy = "Select yes to remove this overseas charity"
    val yesTextCy = "Yes"
    val noTextCy = "No"
    val buttonCy = "Continue"
    val errorHrefCy = "#value"
  }
  val taxYear: Int = 2022
  val removeOverseasCharityUrl =
    s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/remove-overseas-charity-gift-aid?charityName=TestCharity"

  val yesNoFormNo: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
  val yesNoFormYes: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
  val yesNoFormEmpty: Map[String, String] = Map(YesNoForm.yesNo -> "")

  val requiredSessionData: Option[GiftAidCYAModel] = Some(GiftAidCYAModel(overseasCharityNames = Some(Seq(Content.charityName))))


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
      await(wsClient.url(removeOverseasCharityUrl).withHttpHeaders(
        HeaderNames.COOKIE -> agentSessionCookie, langHeader, xSessionId, csrfContent)
        .withFollowRedirects(false).get
      )
    } else {
      authoriseIndividual()
      await(wsClient.url(removeOverseasCharityUrl).withHttpHeaders(langHeader, xSessionId, csrfContent)
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
      await(wsClient.url(removeOverseasCharityUrl).withHttpHeaders(
        HeaderNames.COOKIE -> agentSessionCookie, langHeader, xSessionId, csrfContent)
        .withFollowRedirects(false).post(input)
      )
    } else {
      authoriseIndividual()
      await(wsClient.url(removeOverseasCharityUrl).withHttpHeaders(
        langHeader, xSessionId, csrfContent)
        .withFollowRedirects(false).post(input)
      )
    }
  }

  "As an individual" when {

    s"Calling GET $removeOverseasCharityUrl" when {

      "there is no cya data stored" should {

        lazy val result = getResult(None, None, isAgent = false)

        "redirect the user to the overview page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${appConfig.incomeTaxSubmissionOverviewUrl(taxYear)}"
        }
      }

      "there is stored cya data" when {

        "'overseasCharityNames' is empty in cya data" should {

          lazy val result = getResult(Some(GiftAidCYAModel(overseasDonationsViaGiftAidAmount = Some(50))), None, isAgent = false)

          "redirect the user to the 'overseas charity name' page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOverseasNameController.show(taxYear)}"
          }
        }

        "'overseasCharityNames' is nonEmpty and the given charity is the only one" which {
          lazy val result = getResult(requiredSessionData, None, isAgent = false)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          titleCheck(Content.expectedTitle)
          h1Check(Content.expectedH1 + " " + Content.expectedCaption)
          captionCheck(Content.expectedCaption)
          textOnPageCheck(Content.expectedContent, Selectors.content)
          radioButtonCheck(Content.yesText, 1)
          radioButtonCheck(Content.noText, 2)

          buttonCheck(Content.button)
          welshToggleCheck(ENGLISH)
        }

        "'overseasCharityNames' is nonEmpty and the given charity is not the only one" which {
          lazy val result = getResult(Some(GiftAidCYAModel(overseasCharityNames = Some(Seq("Other", Content.charityName)))), None, isAgent = false)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "has an OK(200) status" in {
            result.status shouldBe OK
          }

          titleCheck(Content.expectedTitle)
          h1Check(Content.expectedH1 + " " + Content.expectedCaption)
          captionCheck(Content.expectedCaption)
          elementExtinct(Selectors.content)
          radioButtonCheck(Content.yesText, 1)
          radioButtonCheck(Content.noText, 2)

          buttonCheck(Content.button)
          welshToggleCheck(ENGLISH)
        }

        "'overseasCharityNames' is nonEmpty, but the given charity is not there" should {

          lazy val result = getResult(Some(GiftAidCYAModel(overseasCharityNames = Some(Seq("Dudes In Need")))), None, isAgent = false)

          "redirect the user to the 'overseas charity summary' page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.OverseasGiftAidSummaryController.show(taxYear)}"
          }
        }
      }
    }

    s"Calling POST $removeOverseasCharityUrl" when {

      "there is no cya data" should {
        lazy val result = postResult(None, None, isAgent = false, yesNoFormYes)

        "redirect the user to the overview page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${appConfig.incomeTaxSubmissionOverviewUrl(taxYear)}"
        }
      }

      "there is cya data stored" when {

        "the user has selected 'Yes' and is removing the last charity" should {
          lazy val result = postResult(requiredSessionData, None, isAgent = false, yesNoFormYes)

          "redirect the user to the 'Add donations to last tax year' page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidLastTaxYearController.show(taxYear)}"
          }
        }

        "the user has selected 'Yes' and is not removing the last charity" should {
          lazy val result = postResult(Some(GiftAidCYAModel(
            overseasCharityNames = Some(Seq("Other", Content.charityName)))),
            None,
            isAgent = false,
            yesNoFormYes
          )
          "redirect the user to the 'overseas charity summary' page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.OverseasGiftAidSummaryController.show(taxYear)}"
          }
        }

        "the user has selected 'No'" should {
          lazy val result = postResult(requiredSessionData, None, isAgent = false, yesNoFormNo)

          "redirect the user to the 'overseas charity summary' page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.OverseasGiftAidSummaryController.show(taxYear)}"
          }
        }

        "the user has not selected an option" should {
          lazy val result = postResult(requiredSessionData, None, isAgent = false, yesNoFormEmpty)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "return the page" which {
            titleCheck("Error: " + Content.expectedTitle)
            h1Check(Content.expectedH1 + " " + Content.expectedCaption)
            captionCheck(Content.expectedCaption)
            textOnPageCheck(Content.expectedContent, Selectors.content)
            radioButtonCheck(Content.yesText, 1)
            radioButtonCheck(Content.noText, 2)

            buttonCheck(Content.button)
            welshToggleCheck(ENGLISH)

            errorSummaryCheck(Content.expectedErrorTitle, Content.errorHref)
            errorAboveElementCheck(Content.expectedErrorTitle)
          }

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }
      }
    }
  }
}

