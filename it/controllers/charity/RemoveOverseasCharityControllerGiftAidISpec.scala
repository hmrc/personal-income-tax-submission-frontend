/*
 * Copyright 2023 HM Revenue & Customs
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


import forms.YesNoForm
import models.charity.{CharityNameModel, GiftAidCYAModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import utils.CharityITHelper

class RemoveOverseasCharityControllerGiftAidISpec extends CharityITHelper {

  object Selectors {
    val heading = "h1"
    val caption = ".govuk-caption-l"
    val content = "#p1"
    val errorSummaryNoSelection = ".govuk-error-summary__body > ul > li > a"
    val yesRadioButton = ".govuk-radios__item:nth-child(1) > label"
    val noRadioButton = ".govuk-radios__item:nth-child(2) > label"
    val errorHref = "#value"
  }

  private val charityNameModel = CharityNameModel("TestCharity")

  def url: String = s"$appUrl/$taxYear/charity/remove-overseas-charity-gift-aid/${charityNameModel.id}"

  trait CommonExpectedResults {
    val expectedTitle: String
    val expectedErrorTitle: String
    val expectedH1: String
    val expectedContent: String
    val expectedCaption: String
    val noSelectionError: String
    val yesText: String
    val noText: String
    val button: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedTitle = s"Are you sure you want to remove ${charityNameModel.name}?"
    val expectedErrorTitle = "Select yes to remove this overseas charity"
    val expectedH1 = s"Are you sure you want to remove ${charityNameModel.name}?"
    val expectedContent = "This will remove all overseas charities."
    val expectedCaption = s"Donations to charity for 6 April $taxYearEOY to 5 April $taxYear"
    val noSelectionError = "Select yes to remove this overseas charity"
    val yesText = "Yes"
    val noText = "No"
    val button = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedTitle = s"A ydych yn siŵr eich bod am dynnu ${charityNameModel.name}?"
    val expectedErrorTitle = "Dewiswch ‘Iawn’ i dynnu’r elusen o dramor hon"
    val expectedH1 = s"A ydych yn siŵr eich bod am dynnu ${charityNameModel.name}?"
    val expectedContent = "Bydd hyn yn tynnu pob elusen o dramor."
    val expectedCaption = s"Rhoddion i elusennau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val noSelectionError = "Dewiswch ‘Iawn’ i dynnu’r elusen o dramor hon"
    val yesText = "Iawn"
    val noText = "Na"
    val button = "Yn eich blaen"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, CommonExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, None),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, None),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, None),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, None))
  }

  val requiredSessionModel: GiftAidCYAModel = GiftAidCYAModel(overseasCharityNames = Seq(charityNameModel))
  val requiredSessionData: Option[GiftAidCYAModel] = Some(requiredSessionModel)

  ".show" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" when {

        "there are multiple charities in session" should {

          "render the page with correct content" which {
            val multipleCharities = GiftAidCYAModel(overseasCharityNames = Seq(charityNameModel, CharityNameModel("secondCharity")))
            lazy val result = getResult(url, Some(multipleCharities), None, user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import user.commonExpectedResults._

            "has an OK status" in {
              result.status shouldBe OK
            }

            titleCheck(expectedTitle, user.isWelsh)
            h1Check(expectedH1 + " " + expectedCaption)
            elementExtinct(Selectors.content)
            radioButtonCheck(yesText, 1)
            radioButtonCheck(noText, 2)
            captionCheck(expectedCaption)
            buttonCheck(button)
            noErrorsCheck()
            welshToggleCheck(user.isWelsh)
          }
        }

        "there is a single charity in session" should {

          "render the page with correct content" which {
            lazy val result = getResult(url, requiredSessionData, None, user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import user.commonExpectedResults._

            "has an OK status" in {
              result.status shouldBe OK
            }

            titleCheck(expectedTitle, user.isWelsh)
            h1Check(expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedContent, Selectors.content)
            radioButtonCheck(yesText, 1)
            radioButtonCheck(noText, 2)
            captionCheck(expectedCaption)
            buttonCheck(button)
            noErrorsCheck()
            welshToggleCheck(user.isWelsh)
          }
        }

        "there is no cya data stored" should {

          lazy val result = getResult(url, None, None)

          "redirect the user to the overview page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe overviewUrl
          }
        }

        "'overseasCharityNames' is empty in cya data" should {

          lazy val result = getResult(url, Some(GiftAidCYAModel(overseasDonationsViaGiftAidAmount = Some(50))), None)

          "redirect the user to the 'overseas charity name' page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOverseasNameController.show(taxYear, None)}"
          }
        }

        "'overseasCharityNames' is nonEmpty, but the given charity is not there" should {

          lazy val result = getResult(url, Some(GiftAidCYAModel(overseasCharityNames = Seq(CharityNameModel("Dudes In Need")))), None)

          "redirect the user to the 'overseas charity summary' page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.OverseasGiftAidSummaryController.show(taxYear)}"
          }
        }
      }
    }
  }

  ".submit" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an error" when {

          "the submitted data is empty" which {
            lazy val result = postResult(url, requiredSessionData, None, Map(YesNoForm.yesNo -> ""), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import user.commonExpectedResults._

            titleCheck(errorPrefix(user.isWelsh) + expectedTitle, user.isWelsh)
            h1Check(expectedH1 + " " + expectedCaption)
            radioButtonCheck(yesText, 1)
            radioButtonCheck(noText, 2)
            captionCheck(expectedCaption)
            buttonCheck(button)
            welshToggleCheck(user.isWelsh)
            errorSummaryCheck(expectedErrorTitle, Selectors.errorHref, user.isWelsh)
            errorAboveElementCheck(expectedErrorTitle)
          }
        }

        "there is no cya data" should {
          lazy val result = postResult(url, None, None, Map(YesNoForm.yesNo -> YesNoForm.yes))

          "redirect the user to the overview page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${appConfig.incomeTaxSubmissionOverviewUrl(taxYear)}"
          }
        }

        "the user has selected 'Yes' and is removing the last charity" should {
          lazy val result = postResult(url, requiredSessionData, None, Map(YesNoForm.yesNo -> YesNoForm.yes))

          "redirect the user to the 'Add donations to last tax year' page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidLastTaxYearController.show(taxYear)}"
          }
          "update the cya data" in {
            findGiftAidDb shouldBe Some(requiredSessionModel.copy(
              overseasDonationsViaGiftAid = Some(false),
              overseasDonationsViaGiftAidAmount = None,
              overseasCharityNames = Seq.empty))
          }
        }

        "the user has selected 'Yes' and is not removing the last charity" should {
          val multipleCharities = GiftAidCYAModel(overseasCharityNames = Seq(charityNameModel, CharityNameModel("secondCharity")))
          lazy val result = postResult(url, Some(multipleCharities), None, Map(YesNoForm.yesNo -> YesNoForm.yes))

          "redirect the user to the 'overseas charity summary' page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.OverseasGiftAidSummaryController.show(taxYear)}"
          }
          "update the cya data" in {
            val giftAidCYAModel = findGiftAidDb.get
            val id = giftAidCYAModel.overseasCharityNames.head.id

            giftAidCYAModel shouldBe requiredSessionModel.copy(overseasCharityNames = Seq(CharityNameModel(id, "secondCharity")))
          }
        }

        "the user has selected 'No'" should {
          lazy val result = postResult(url, requiredSessionData, None, Map(YesNoForm.yesNo -> YesNoForm.no))

          "redirect the user to the 'overseas charity summary' page" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").head shouldBe s"${controllers.charity.routes.OverseasGiftAidSummaryController.show(taxYear)}"
          }
        }
      }
    }
  }
}
