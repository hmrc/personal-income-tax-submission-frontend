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
import play.api.http.Status._
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{GiftAidDatabaseHelper, IntegrationTest, ViewHelpers}

class OverseasGiftAidDonationControllerISpec extends IntegrationTest with ViewHelpers with GiftAidDatabaseHelper {

  object Content {
    val heading = "Did you use Gift Aid to donate to an overseas charity?"
    val headingAgent = "Did your client use Gift Aid to donate to an overseas charity?"
    val caption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val button = "Continue"
    val yesText = "Yes"
    val noText = "No"
    val errorText = "Select yes if you used Gift Aid to donate to an overseas charity"
    val errorTextAgent = "Select yes if your client used Gift Aid to donate to an overseas charity"
  }

  object Selectors {
    val errorSummary: String = "#error-summary-title"
    val firstError: String = ".govuk-error-summary__body > ul > li:nth-child(1) > a"
    val errorMessage: String = "#value-error"
    val errorHref = "#value"
  }

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  lazy val controller: GiftAidOneOffController = app.injector.instanceOf[GiftAidOneOffController]

  val yesNoFormNo: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
  val yesNoFormYes: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
  val yesNoFormEmpty: Map[String, String] = Map(YesNoForm.yesNo -> "")

  val taxYear: Int = 2022
  val requiredSessionData: Some[GiftAidCYAModel] =
    Some(GiftAidCYAModel(oneOffDonationsViaGiftAid = Some(true), oneOffDonationsViaGiftAidAmount = Some(BigDecimal("50"))))
  val overseasDonationsUrl = s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/overseas-charity-donations-using-gift-aid"

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
      await(wsClient.url(overseasDonationsUrl).withHttpHeaders(
        HeaderNames.COOKIE -> agentSessionCookie, langHeader, xSessionId, csrfContent)
        .withFollowRedirects(false).get
      )
    } else {
      authoriseIndividual()
      await(wsClient.url(overseasDonationsUrl).withHttpHeaders(langHeader, xSessionId, csrfContent)
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
      await(wsClient.url(overseasDonationsUrl).withHttpHeaders(
        HeaderNames.COOKIE -> agentSessionCookie, langHeader, xSessionId, csrfContent)
        .withFollowRedirects(false).post(input)
      )
    } else {
      authoriseIndividual()
      await(wsClient.url(overseasDonationsUrl).withHttpHeaders(
        langHeader, xSessionId, csrfContent)
        .withFollowRedirects(false).post(input)
      )
    }
  }

  "As an individual" when {

    s"Calling GET $overseasDonationsUrl" when {

      "there is no cya data stored" should {

        lazy val result = getResult(None, None, isAgent = false)

        "redirect the user to the overview page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${appConfig.incomeTaxSubmissionOverviewUrl(taxYear)}"
        }
      }

      "there is stored cya data" when {

        "there is prior overseas donations data" should {

          lazy val result: WSResponse =
            getResult(
              requiredSessionData,
              Some(IncomeSourcesModel(giftAid = Some(GiftAidSubmissionModel(Some(GiftAidPaymentsModel(nonUkCharities = Some(BigDecimal(125)))))))),
              isAgent = false)

          "redirect the user to the CYA page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(taxYear)}"
          }
        }

        "'one off donations' does not exist in cya data" should {

          lazy val result = getResult(Some(GiftAidCYAModel(donationsViaGiftAidAmount = Some(BigDecimal(50)))), None, isAgent = false)

          "redirect the user to the 'one off donations' page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOneOffController.show(taxYear)}"
          }
        }

        "'one off donations' is true, but 'one off amount' does not exist in cya data" should {

          lazy val result = getResult(Some(GiftAidCYAModel(oneOffDonationsViaGiftAid = Some(true))), None, isAgent = false)

          "redirect the user to the 'one off amount' page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOneOffAmountController.show(taxYear)}"
          }
        }

        "'one off donated amount' exists in cya data" should {

          lazy val result = getResult(requiredSessionData, None, isAgent = false)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "return the page" which {
            titleCheck(Content.heading)
            welshToggleCheck("English")
            h1Check(s"${Content.heading} ${Content.caption}")
            captionCheck(Content.caption)
            radioButtonCheck(Content.yesText, 1)
            radioButtonCheck(Content.noText, 2)
            buttonCheck(Content.button)

            elementExtinct(Selectors.errorSummary)
            elementExtinct(Selectors.firstError)
            elementExtinct(Selectors.errorMessage)
          }

          s"have an OK($OK) status" in {
            result.status shouldBe OK
          }
        }
      }
    }

    s"Calling POST $overseasDonationsUrl" when {

      "there is no cya data" should {
        lazy val result = postResult(None, None, isAgent = false, yesNoFormYes)

        "redirect the user to the overview page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${appConfig.incomeTaxSubmissionOverviewUrl(taxYear)}"
        }
      }

      "there is cya data stored" when {

        "the user has selected 'Yes'" should {
          lazy val result = postResult(requiredSessionData, None, isAgent = false, yesNoFormYes)

          "redirect the user to the 'Overseas donation amount' page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOverseasAmountController.show(taxYear)}"
          }
        }

        "the user has selected 'No'" should {
          lazy val result = postResult(requiredSessionData, None, isAgent = false, yesNoFormNo)

          "redirect the user to the 'Add donation to last tax year' page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidLastTaxYearController.show(taxYear)}"
          }
        }

        "the user has not selected an option" should {
          lazy val result = postResult(requiredSessionData, None, isAgent = false, yesNoFormEmpty)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "return the page" which {
            titleCheck("Error: " + Content.heading)
            welshToggleCheck("English")
            h1Check(s"${Content.heading} ${Content.caption}")
            captionCheck(Content.caption)
            radioButtonCheck(Content.yesText, 1)
            radioButtonCheck(Content.noText, 2)
            buttonCheck(Content.button)

            errorSummaryCheck(Content.errorText, Selectors.errorHref)
            errorAboveElementCheck(Content.errorText)
          }

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }
      }
    }
  }

  "As an agent" when {

    s"Calling GET $overseasDonationsUrl" when {

      "there is no cya data stored" should {

        lazy val result = getResult(None, None, isAgent = true)

        "redirect the user to the overview page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${appConfig.incomeTaxSubmissionOverviewUrl(taxYear)}"
        }
      }

      "there is stored cya data" when {

        "there is prior overseas donations data" should {

          lazy val result: WSResponse =
            getResult(
              requiredSessionData,
              Some(IncomeSourcesModel(giftAid = Some(GiftAidSubmissionModel(Some(GiftAidPaymentsModel(nonUkCharities = Some(BigDecimal(125)))))))),
              isAgent = true)

          "redirect the user to the CYA page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(taxYear)}"
          }
        }

        "'one off donations' does not exist in cya data" should {

          lazy val result = getResult(Some(GiftAidCYAModel(donationsViaGiftAidAmount = Some(BigDecimal(50)))), None, isAgent = true)

          "redirect the user to the 'one off donations' page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOneOffController.show(taxYear)}"
          }
        }

        "'one off donations' is true, but 'one off amount' does not exist in cya data" should {

          lazy val result = getResult(Some(GiftAidCYAModel(oneOffDonationsViaGiftAid = Some(true))), None, isAgent = true)

          "redirect the user to the 'one off amount' page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOneOffAmountController.show(taxYear)}"
          }
        }

        "'one off donated amount' exists in cya data" should {

          lazy val result = getResult(requiredSessionData, None, isAgent = true)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "return the page" which {
            titleCheck(Content.headingAgent)
            welshToggleCheck("English")
            h1Check(s"${Content.headingAgent} ${Content.caption}")
            captionCheck(Content.caption)
            radioButtonCheck(Content.yesText, 1)
            radioButtonCheck(Content.noText, 2)
            buttonCheck(Content.button)

            elementExtinct(Selectors.errorSummary)
            elementExtinct(Selectors.firstError)
            elementExtinct(Selectors.errorMessage)
          }

          s"have an OK($OK) status" in {
            result.status shouldBe OK
          }
        }
      }
    }

    s"Calling POST $overseasDonationsUrl" when {

      "there is no cya data" should {
        lazy val result = postResult(None, None, isAgent = true, yesNoFormYes)

        "redirect the user to the overview page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${appConfig.incomeTaxSubmissionOverviewUrl(taxYear)}"
        }
      }

      "there is cya data stored" when {

        "the user has selected 'Yes'" should {
          lazy val result = postResult(requiredSessionData, None, isAgent = true, yesNoFormYes)

          "redirect the user to the 'Overseas donation amount' page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOverseasAmountController.show(taxYear)}"
          }
        }

        "the user has selected 'No'" should {
          lazy val result = postResult(requiredSessionData, None, isAgent = true, yesNoFormNo)

          "redirect the user to the 'Add donation to last tax year' page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidLastTaxYearController.show(taxYear)}"
          }
        }

        "the user has not selected an option" should {
          lazy val result = postResult(requiredSessionData, None, isAgent = true, yesNoFormEmpty)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          "return the page" which {
            titleCheck("Error: " + Content.headingAgent)
            welshToggleCheck("English")
            h1Check(s"${Content.headingAgent} ${Content.caption}")
            captionCheck(Content.caption)
            radioButtonCheck(Content.yesText, 1)
            radioButtonCheck(Content.noText, 2)
            buttonCheck(Content.button)

            errorSummaryCheck(Content.errorTextAgent, Selectors.errorHref)
            errorAboveElementCheck(Content.errorTextAgent)
          }

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }
      }
    }
  }
}
