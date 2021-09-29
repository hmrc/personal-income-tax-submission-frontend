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

import forms.YesNoForm
import models.charity.GiftAidCYAModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import utils.CharityITHelper

class GiftAidSharesSecuritiesLandPropertyConfirmationControllerISpec extends CharityITHelper {

  def url: String = s"$appUrl/$year/charity/remove-shares-securities-land-and-property"

  object Selectors {
    val captionSelector = ".govuk-caption-l"
    val extraContentSelector = "#main-content > div > div > form > div > fieldset > legend > div > p"
    val continueSelector = "#continue"
    val errorSummaryHref = "#value"
  }

  trait SpecificExpectedResults {
    val expectedH1: String
    val expectedTitle: String
    val expectedError: String
  }

  trait CommonExpectedResults {
    val captionText: String
    val yesText: String
    val noText: String
    val continueText: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val captionText = s"Donations to charity for 6 April ${year - 1} to 5 April $year"
    val yesText = "Yes"
    val noText = "No"
    val continueText = "Continue"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val captionText = s"Rhoddion i elusennau ar gyfer 6 Ebrill ${year - 1} i 5 Ebrill $year"
    val yesText = "Iawn"
    val noText = "Na"
    val continueText = "Yn eich blaen"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedH1 = "Are you sure you did not donate qualifying shares, securities, land, or property to charity?"
    val expectedTitle = "Are you sure you did not donate qualifying shares, securities, land, or property to charity?"
    val expectedError: String = "Select yes to remove all shares, securities, land and property donated to charity"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedH1 = "Are you sure your client did not donate qualifying shares, securities, land, or property to charity?"
    val expectedTitle = "Are you sure your client did not donate qualifying shares, securities, land, or property to charity?"
    val expectedError: String = "Select yes to remove all shares, securities, land and property donated to charity"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedH1 = "A ydych yn siŵr na wnaethoch roi cyfranddaliadau, gwarantau, tir nac eiddo i elusen?"
    val expectedTitle = "A ydych yn siŵr na wnaethoch roi cyfranddaliadau, gwarantau, tir nac eiddo i elusen?"
    val expectedError: String = "Dewiswch ‘Iawn’ i dynnu pob cyfranddaliad, gwarant, tir ac eiddo a roddwyd i elusen"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedH1 = "A ydych yn siŵr na wnaeth eich cleient roi cyfranddaliadau, gwarantau, tir nac eiddo i elusen?"
    val expectedTitle = "A ydych yn siŵr na wnaeth eich cleient roi cyfranddaliadau, gwarantau, tir nac eiddo i elusen?"
    val expectedError: String = "Dewiswch ‘Iawn’ i dynnu pob cyfranddaliad, gwarant, tir ac eiddo a roddwyd i elusen"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  val requiredSessionModel: GiftAidCYAModel =
    GiftAidCYAModel(
      donatedSharesSecuritiesLandOrProperty = Some(true),
      donatedSharesOrSecurities = Some(true),
      donatedLandOrProperty = Some(false)
  )
  val requiredSessionData: Option[GiftAidCYAModel] = Some(requiredSessionModel)


  ".show" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the page with correct content" which {
          lazy val result = getResult(url, requiredSessionData, None, user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          welshToggleCheck(user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1 + " " + captionText)
          textOnPageCheck(captionText, captionSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(continueText, continueSelector)
          noErrorsCheck()
        }

      }
    }
    "there is no cya data" should {
      lazy val result = getResult(url, None, None)

      "redirect to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe overviewUrl
      }
    }

    "there is cya data but no sharesSecurities or landOrProperty" should {
      lazy val result = getResult(url, Some(GiftAidCYAModel()), None)

      "redirect to the giftAid cya page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe
          s"${controllers.charity.routes.GiftAidCYAController.show(year).url}"
      }
    }

  }

  ".submit" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "no radio button has been selected" should {
          lazy val result = postResult(url, requiredSessionData, None, Map(YesNoForm.yesNo -> ""), user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          titleCheck(errorPrefix(user.isWelsh) + user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          welshToggleCheck(user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1 + " " + captionText)
          textOnPageCheck(captionText, captionSelector)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(continueText, continueSelector)
          errorSummaryCheck(user.specificExpectedResults.get.expectedError, errorSummaryHref, user.isWelsh)
          errorAboveElementCheck(user.specificExpectedResults.get.expectedError)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }
      }
    }

    "the user submits 'yes'" should {
      lazy val result = postResult(url, requiredSessionData, None, Map(YesNoForm.yesNo -> YesNoForm.yes))

      "redirect to the 'shares, securities amount' page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(year)}"
      }

      "update the cya data" in {
        findGiftAidDb shouldBe Some(requiredSessionModel.copy(
          donatedSharesSecuritiesLandOrProperty = Some(false),
          donatedSharesOrSecurities = None,
          donatedSharesOrSecuritiesAmount = None,
          donatedLandOrProperty = None,
          donatedLandOrPropertyAmount = None,
          overseasDonatedSharesSecuritiesLandOrProperty = None,
          overseasDonatedSharesSecuritiesLandOrPropertyAmount = None,
          overseasDonatedSharesSecuritiesLandOrPropertyCharityNames = Seq.empty
        ))
      }
    }
    "the user submits 'no' and landOrProperty is empty" should {
      lazy val result = postResult(url, Some(requiredSessionModel.copy(donatedLandOrProperty = None)),
        None, Map(YesNoForm.yesNo -> YesNoForm.no))

      "redirect to the 'shares, securities amount' page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidQualifyingSharesSecuritiesController.show(year)}"
      }

      "update the cya data" in {
        findGiftAidDb shouldBe Some(requiredSessionModel.copy(
          donatedSharesSecuritiesLandOrProperty = Some(true),
          donatedSharesOrSecurities = None,
          donatedSharesOrSecuritiesAmount = None,
          donatedLandOrProperty = None,
          donatedLandOrPropertyAmount = None,
          overseasDonatedSharesSecuritiesLandOrProperty = None,
          overseasDonatedSharesSecuritiesLandOrPropertyAmount = None,
          overseasDonatedSharesSecuritiesLandOrPropertyCharityNames = Seq.empty
        ))
      }
    }
    "the user submits 'no' and landOrProperty is not empty" should {
      lazy val result = postResult(url, Some(requiredSessionModel),
        None, Map(YesNoForm.yesNo -> YesNoForm.no))

      "redirect to the 'shares, securities amount' page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(year)}"
      }

      "update the cya data" in {
        findGiftAidDb shouldBe Some(requiredSessionModel)
      }
    }
    "the user submits with no cyaData" should {
      lazy val result = postResult(url, None,
        None, Map(YesNoForm.yesNo -> YesNoForm.no))

      "redirect to the 'shares, securities amount' page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe overviewUrl
      }

    }

  }
}