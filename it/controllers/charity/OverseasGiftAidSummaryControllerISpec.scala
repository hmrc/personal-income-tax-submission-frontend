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
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{GiftAidDatabaseHelper, IntegrationTest, ViewHelpers}

class OverseasGiftAidSummaryControllerISpec extends IntegrationTest with ViewHelpers with GiftAidDatabaseHelper {

  object Selectors {
    val heading = "h1"
    val caption = ".govuk-caption-l"
    val charity1 = ".hmrc-add-to-a-list__contents:nth-child(1) > .hmrc-add-to-a-list__identifier"
    val charity2 = ".hmrc-add-to-a-list__contents:nth-child(2) > .hmrc-add-to-a-list__identifier"
    val change1 = ".hmrc-add-to-a-list__contents:nth-child(1) > .hmrc-add-to-a-list__change > a > span:nth-child(1)"
    val change1hidden = ".hmrc-add-to-a-list__contents:nth-child(1) > .hmrc-add-to-a-list__change > a > span:nth-child(2)"
    val change2 = ".hmrc-add-to-a-list__contents:nth-child(2) > .hmrc-add-to-a-list__change > a > span:nth-child(1)"
    val change2hidden = ".hmrc-add-to-a-list__contents:nth-child(2) > .hmrc-add-to-a-list__change > a > span:nth-child(2)"
    val remove1 = ".hmrc-add-to-a-list__contents:nth-child(1) > .hmrc-add-to-a-list__remove > a > span:nth-child(1)"
    val remove1hidden = ".hmrc-add-to-a-list__contents:nth-child(1) > .hmrc-add-to-a-list__remove > a > span:nth-child(2)"
    val remove2 = ".hmrc-add-to-a-list__contents:nth-child(2) > .hmrc-add-to-a-list__remove > a > span:nth-child(1)"
    val remove2hidden = ".hmrc-add-to-a-list__contents:nth-child(2) > .hmrc-add-to-a-list__remove > a > span:nth-child(2)"
    val question = ".govuk-fieldset__legend"
    val hint = "#value-hint"
    val yesRadio = ".govuk-radios__item:nth-child(1) > label"
    val noRadio = ".govuk-radios__item:nth-child(2) > label"
    val errorSummary = "#error-summary-title"
    val noSelectionError = ".govuk-error-summary__body > ul > li > a"
    val errorLink = "#value"
  }

  object Content {
    val heading = "Overseas charities you used Gift Aid to donate to"
    val headingAgent = "Overseas charities your client used Gift Aid to donate to"
    val caption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val charity1 = "Sponsor a pikachu"
    val charity2 = "Dudes in need"
    val question = "Do you need to add another overseas charity?"
    val hint = "You must tell us about all the overseas charities you donated to."
    val hintAgent = "You must tell us about all the overseas charities your client donated to."
    val change = "Change"
    val remove = "Remove"
    val hiddenChange1 = s"Change details you’ve entered for $charity1"
    val hiddenRemove1 = s"Remove $charity1"
    val hiddenChange2 = s"Change details you’ve entered for $charity2"
    val hiddenRemove2 = s"Remove $charity2"
    val yes = "Yes"
    val no = "No"
    val errorSummary = "There is a problem"
    val noSelectionError = "Select yes if you need to add another overseas charity"
    val button = "Continue"
  }

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  val taxYear: Int = 2022

  val yesNoFormNo: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.no)
  val yesNoFormYes: Map[String, String] = Map(YesNoForm.yesNo -> YesNoForm.yes)
  val yesNoFormEmpty: Map[String, String] = Map(YesNoForm.yesNo -> "")

  val overseasGiftAidSummaryUrl = s"$startUrl/$taxYear/charity/overseas-charities-donated-to"

  val requiredSessionData: Option[GiftAidCYAModel] = Some(GiftAidCYAModel(overseasCharityNames = Some(Seq(Content.charity1, Content.charity2))))

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
      await(wsClient.url(overseasGiftAidSummaryUrl).withHttpHeaders(
        HeaderNames.COOKIE -> agentSessionCookie, langHeader, xSessionId, csrfContent)
        .withFollowRedirects(false).get
      )
    } else {
      authoriseIndividual()
      await(wsClient.url(overseasGiftAidSummaryUrl).withHttpHeaders(langHeader, xSessionId, csrfContent)
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
      await(wsClient.url(overseasGiftAidSummaryUrl).withHttpHeaders(
        HeaderNames.COOKIE -> agentSessionCookie, langHeader, xSessionId, csrfContent)
        .withFollowRedirects(false).post(input)
      )
    } else {
      authoriseIndividual()
      await(wsClient.url(overseasGiftAidSummaryUrl).withHttpHeaders(
        langHeader, xSessionId, csrfContent)
        .withFollowRedirects(false).post(input)
      )
    }
  }

  "As an individual" when {

    s"Calling GET $overseasGiftAidSummaryUrl" when {

      "there is no cya data" should {

        lazy val result: WSResponse = getResult(None, None, isAgent = false)

        "redirect the user to the overview page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${appConfig.incomeTaxSubmissionOverviewUrl(taxYear)}"
        }
      }

      "there is cya data, but 'overseasCharityNames' has not been stored" should {

        lazy val result: WSResponse = getResult(Some(GiftAidCYAModel(overseasDonationsViaGiftAidAmount = Some(50))), None, isAgent = false)

        "redirect the user to the overseas charity name page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOverseasNameController.show(taxYear, None)}"
        }
      }

      "'overseasCharityNames' exists" should {
        lazy val result: WSResponse = getResult(requiredSessionData, None, isAgent = false)

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        titleCheck(Content.heading)
        h1Check(Content.heading + " " + Content.caption)
        welshToggleCheck("English")
        textOnPageCheck(Content.caption, Selectors.caption)
        taskListCheck(Seq((Content.charity1, Content.hiddenChange1, Content.hiddenRemove1), (Content.charity2, Content.hiddenChange2, Content.hiddenRemove2)))
        textOnPageCheck(Content.question, Selectors.question)
        hintTextCheck(Content.hint)
        radioButtonCheck(Content.yes, 1)
        radioButtonCheck(Content.no, 2)
        buttonCheck(Content.button)
      }
    }

    s"Calling POST $overseasGiftAidSummaryUrl" when {

      "there is no cya data stored" should {

        lazy val result: WSResponse = postResult(None, None, isAgent = false, Map("amount" -> "123000.42"))

        "redirect the user to the overview page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${appConfig.incomeTaxSubmissionOverviewUrl(taxYear)}"
        }
      }

      "there is cya data stored" when {

        "the user has selected 'Yes'" should {
          lazy val result = postResult(requiredSessionData, None, isAgent = false, yesNoFormYes)

          "redirect the user to the overseas charity name page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOverseasNameController.show(taxYear, None)}"
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
            h1Check(Content.heading + " " + Content.caption)
            welshToggleCheck("English")
            textOnPageCheck(Content.caption, Selectors.caption)
            taskListCheck(Seq(
              (Content.charity1, Content.hiddenChange1, Content.hiddenRemove1),
              (Content.charity2, Content.hiddenChange2, Content.hiddenRemove2)))
            textOnPageCheck(Content.question, Selectors.question)
            hintTextCheck(Content.hint)
            radioButtonCheck(Content.yes, 1)
            radioButtonCheck(Content.no, 2)
            buttonCheck(Content.button)

            errorSummaryCheck(Content.noSelectionError, Selectors.errorLink)
            errorAboveElementCheck(Content.noSelectionError)
          }

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }
      }
    }
  }

  "As an agent" when {

    s"Calling GET $overseasGiftAidSummaryUrl" when {

      "there is no cya data" should {

        lazy val result: WSResponse = getResult(None, None, isAgent = true)

        "redirect the user to the overview page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${appConfig.incomeTaxSubmissionOverviewUrl(taxYear)}"
        }
      }

      "there is cya data, but 'overseasCharityNames' has not been stored" should {

        lazy val result: WSResponse =getResult(Some(GiftAidCYAModel(overseasDonationsViaGiftAidAmount = Some(50))), None, isAgent = true)

        "redirect the user to the overseas charity name page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOverseasNameController.show(taxYear, None)}"
        }
      }

      "'overseasCharityNames' exists" should {
        lazy val result: WSResponse = getResult(requiredSessionData, None, isAgent = true)

        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an OK(200) status" in {
          result.status shouldBe OK
        }

        titleCheck(Content.headingAgent)
        h1Check(Content.headingAgent + " " + Content.caption)
        welshToggleCheck("English")
        textOnPageCheck(Content.caption, Selectors.caption)
        taskListCheck(Seq(
          (Content.charity1, Content.hiddenChange1, Content.hiddenRemove1),
          (Content.charity2, Content.hiddenChange2, Content.hiddenRemove2)))
        textOnPageCheck(Content.question, Selectors.question)
        hintTextCheck(Content.hintAgent)
        radioButtonCheck(Content.yes, 1)
        radioButtonCheck(Content.no, 2)
        buttonCheck(Content.button)
      }
    }

    s"Calling POST $overseasGiftAidSummaryUrl" when {

      "there is no cya data stored" should {

        lazy val result: WSResponse = postResult(None, None, isAgent = true, Map("amount" -> "123000.42"))

        "redirect the user to the overview page" in {
          result.status shouldBe SEE_OTHER
          result.headers("Location").head shouldBe s"${appConfig.incomeTaxSubmissionOverviewUrl(taxYear)}"
        }
      }

      "there is cya data stored" when {

        "the user has selected 'Yes'" should {
          lazy val result = postResult(requiredSessionData, None, isAgent = true, yesNoFormYes)

          "redirect the user to the overseas charity name page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOverseasNameController.show(taxYear, None)}"
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
            h1Check(Content.headingAgent + " " + Content.caption)
            welshToggleCheck("English")
            textOnPageCheck(Content.caption, Selectors.caption)
            taskListCheck(Seq(
              (Content.charity1, Content.hiddenChange1, Content.hiddenRemove1),
              (Content.charity2, Content.hiddenChange2, Content.hiddenRemove2)))
            textOnPageCheck(Content.question, Selectors.question)
            hintTextCheck(Content.hintAgent)
            radioButtonCheck(Content.yes, 1)
            radioButtonCheck(Content.no, 2)
            buttonCheck(Content.button)

            errorSummaryCheck(Content.noSelectionError, Selectors.errorLink)
            errorAboveElementCheck(Content.noSelectionError)
          }

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }
      }
    }
  }
}
