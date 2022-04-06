/*
 * Copyright 2022 HM Revenue & Customs
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

import common.UUID
import models.charity.prior.{GiftAidPaymentsModel, GiftAidSubmissionModel}
import models.charity.{CharityNameModel, GiftAidCYAModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import utils.CharityITHelper

class GiftAidOverseasNameControllerISpec extends CharityITHelper {

  val charLimit: String = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Cras suscipit turpis sed blandit" +
    " lobortis. Vestibulum dignissim nulla quis luctus placerat. Quisque commodo eros tristique nibh scelerisque, sit" +
    " amet aliquet odio laoreet. Sed finibus dapibus lorem sit amet elementum. Nunc euismod arcu augue, tincidunt" +
    " elementum elit vulputate et. Nunc imperdiet est magna, non vestibulum tortor vehicula eu. Nulla a est sed nibh" +
    " lacinia maximus. Nullam facilisis nunc vel sapien facilisis tincidunt. Sed odio."

  val testModel: GiftAidSubmissionModel = GiftAidSubmissionModel(Some(GiftAidPaymentsModel(None, Some(List("dupe")), None, None, None, None)), None)

  def url(changeCharityId: Option[String] = None): String =
    s"$appUrl/$taxYear/charity/name-of-overseas-charity${if (changeCharityId.nonEmpty) s"?changeCharityId=${changeCharityId.get}" else ""}"

  object Selectors {
    val captionSelector: String = ".govuk-caption-l"
    val inputFieldSelector: String = "#name"
    val buttonSelector: String = ".govuk-button"
    val inputHintTextSelector: String = "#p1"
    val errorSelector: String = "#main-content > div > div > div.govuk-error-summary > div > ul > li > a"
  }

  trait SpecificExpectedResults {
    val expectedTitle: String
    val expectedH1: String
    val expectedInputHintText: String
    val expectedError: String
  }

  trait CommonExpectedResults {
    val expectedCaption: String
    val expectedInputName: String
    val expectedButtonText: String
    val expectedInvalidError: String
    val expectedLengthError: String
    val expectedDuplicateError: String
    val serviceName: String
    val govUkExtension: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedCaption: String = s"Donations to charity for 6 April $taxYearEOY to 5 April $taxYear"
    val expectedInputName: String = "name"
    val expectedButtonText: String = "Continue"
    val serviceName = "Update and submit an Income Tax Return"
    val govUkExtension = "GOV.UK"
    val expectedInvalidError: String = "Name of overseas charity must only include numbers 0-9, letters a to z," +
      " hyphens, spaces, apostrophes, commas, full stops, round brackets and the special characters, &, /, @, £, *"
    val expectedLengthError: String = "The name of the overseas charity must be 75 characters or fewer"
    val expectedDuplicateError: String = "You cannot add 2 charities with the same name"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedCaption: String = s"Rhoddion i elusennau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val expectedInputName: String = "name"
    val expectedButtonText: String = "Yn eich blaen"
    val serviceName = "Diweddaru a chyflwyno Ffurflen Dreth Incwm"
    val govUkExtension = "GOV.UK"
    val expectedInvalidError: String = "Mae’n rhaid i enw’r elusen o dramor gynnwys rhifau 0-9, llythrennau a i z," +
      " cysylltnodau, bylchau, collnodau, comas, atalnodau llawn, cromfachau crwn, a’r cymeriadau arbennig &, /, @, £, * yn unig"
    val expectedLengthError: String = "Mae’n rhaid i enw’r elusen o dramor fod yn 75 o gymeriadau neu’n llai"
    val expectedDuplicateError: String = "Ni allwch ychwanegu 2 elusen gyda’r un enw"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val expectedTitle: String = "Name of overseas charity you used Gift Aid to donate to"
    val expectedH1: String = "Name of overseas charity you used Gift Aid to donate to"
    val expectedInputHintText: String = "If you donated to more than one charity, you can add them later."
    val expectedError: String = "Enter the name of the overseas charity you used Gift Aid to donate to"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val expectedTitle: String = "Name of overseas charity your client used Gift Aid to donate to"
    val expectedH1: String = "Name of overseas charity your client used Gift Aid to donate to"
    val expectedInputHintText: String = "If your client donated to more than one charity, you can add them later."
    val expectedError: String = "Enter the name of the overseas charity your client used Gift Aid to donate to"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val expectedTitle: String = "Enw’r elusen o dramor y gwnaethoch ddefnyddio Rhodd Cymorth i’w rhoi iddo"
    val expectedH1: String = "Enw’r elusen o dramor y gwnaethoch ddefnyddio Rhodd Cymorth i’w rhoi iddo"
    val expectedInputHintText: String = "Os gwnaethoch roi i fwy nag un elusen, gallwch eu hychwanegu’n ddiweddarach."
    val expectedError: String = "Nodwch enw’r elusen o dramor y gwnaethoch ddefnyddio Rhodd Cymorth i’w rhoi iddo"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val expectedTitle: String = "Enw’r elusen o dramor y gwnaeth eich cleient ddefnyddio Rhodd Cymorth i’w rhoi iddo"
    val expectedH1: String = "Enw’r elusen o dramor y gwnaeth eich cleient ddefnyddio Rhodd Cymorth i’w rhoi iddo"
    val expectedInputHintText: String = "Os gwnaeth eich cleient roi i fwy nag un elusen, gallwch eu hychwanegu’n ddiweddarach."
    val expectedError: String = "Nodwch enw’r elusen o dramor y gwnaeth eich cleient defnyddio Rhodd Cymorth i’w rhoi iddo"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  val requiredSessionModel: GiftAidCYAModel = GiftAidCYAModel(overseasDonationsViaGiftAidAmount = Some(BigDecimal(1)))
  val requiredSessionData: Option[GiftAidCYAModel] = Some(requiredSessionModel)

  val validAmount = 445.30

  ".show" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the page with correct content" which {
          lazy val result = getResult(url(), requiredSessionData, None, user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import Selectors._
          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.expectedTitle, user.isWelsh)
          h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(user.specificExpectedResults.get.expectedInputHintText, inputHintTextSelector)
          inputFieldCheck(expectedInputName, inputFieldSelector)
          buttonCheck(expectedButtonText, buttonSelector)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "there is no cya data" should {
      lazy val result = getResult(url(), None, None)

      "redirect the user to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe overviewUrl
      }
    }

    "there is cya data, but 'overseasDonationsViaGiftAidAmount' has not been stored" should {
      lazy val result =
        getResult(
          url(),
          Some(GiftAidCYAModel(overseasDonationsViaGiftAid = Some(true), donationsViaGiftAidAmount = Some(validAmount))),
          None
        )

      "redirect the user to the overseas donation amount page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidOverseasAmountController.show(taxYear)}"
      }
    }

    "there is cya data, and 'overseasDonationsViaGiftAidAmount' has been stored and charityNameId is present but not valid" should {
      val charityId = UUID().randomUUID
      lazy val result = getResult(
        url(Some(charityId)),
        Some(GiftAidCYAModel(overseasDonationsViaGiftAid = Some(true),
          overseasDonationsViaGiftAidAmount = Some(validAmount),
          overseasCharityNames = Seq.empty
        )),
        None
      )

      "redirect the user to the overseas gift aid summary page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.OverseasGiftAidSummaryController.show(taxYear)}"
      }
    }

    "there is cya data, and 'overseasDonationsViaGiftAidAmount' has been stored and charityNameId is present and is valid" should {
      val charityId = UUID().randomUUID
      lazy val result = getResult(
        url(Some(charityId)),
        Some(GiftAidCYAModel(overseasDonationsViaGiftAid = Some(true),
          overseasDonationsViaGiftAidAmount = Some(validAmount),
          overseasCharityNames = Seq(CharityNameModel(charityId, "some-charity"))
        )),
        None
      )

      implicit def document: () => Document = () => Jsoup.parse(result.body)

      "redirect the user to the overseas name page" in {
        result.status shouldBe OK
      }

      titleCheck(ExpectedIndividualEN.expectedTitle, isWelsh = false)
    }
  }

  ".submit" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "return an error" when {

          "the submitted data is empty" which {
            lazy val result = postResult(url(), requiredSessionData, None, Map("name" -> ""), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(errorPrefix(user.isWelsh) + user.specificExpectedResults.get.expectedTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedInputHintText, inputHintTextSelector)
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, buttonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.expectedError, Selectors.inputFieldSelector, user.isWelsh)
            errorAboveElementCheck(user.specificExpectedResults.get.expectedError)
          }

          "the submitted data is too long" which {
            lazy val result = postResult(url(), requiredSessionData, None, Map("name" -> charLimit), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(errorPrefix(user.isWelsh) + user.specificExpectedResults.get.expectedTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedInputHintText, inputHintTextSelector)
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, buttonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(expectedLengthError, Selectors.inputFieldSelector, user.isWelsh)
            errorAboveElementCheck(expectedLengthError)
          }

          "the submitted data is in the incorrect format" which {
            lazy val result = postResult(url(), requiredSessionData, None, Map("name" -> ":@~{}<>?"), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(errorPrefix(user.isWelsh) + user.specificExpectedResults.get.expectedTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedInputHintText, inputHintTextSelector)
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, buttonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(expectedInvalidError, Selectors.inputFieldSelector, user.isWelsh)
            errorAboveElementCheck(expectedInvalidError)
          }

          "the submitted data is a duplicate name" which {
            val cyaModel = requiredSessionModel.copy(overseasCharityNames = Seq(CharityNameModel("Dudes In Need")))
            lazy val result = postResult(url(), Some(cyaModel), None, Map("name" -> "Dudes In Need"), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import Selectors._
            import user.commonExpectedResults._

            titleCheck(errorPrefix(user.isWelsh) + user.specificExpectedResults.get.expectedTitle, user.isWelsh)
            h1Check(user.specificExpectedResults.get.expectedH1 + " " + expectedCaption)
            textOnPageCheck(expectedCaption, captionSelector)
            textOnPageCheck(user.specificExpectedResults.get.expectedInputHintText, inputHintTextSelector)
            inputFieldCheck(expectedInputName, inputFieldSelector)
            buttonCheck(expectedButtonText, buttonSelector)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(expectedDuplicateError, Selectors.inputFieldSelector, user.isWelsh)
            errorAboveElementCheck(expectedDuplicateError)
          }
        }
      }
    }

    "there is no cya data stored" should {

      lazy val result = postResult(url(), None, None, Map("amount" -> s"$validAmount"))

      "redirect the user to the overview page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${appConfig.incomeTaxSubmissionOverviewUrl(taxYear)}"
      }
    }

    "the user enters a valid name" when {
      lazy val result = postResult(url(), requiredSessionData, None, Map("name" -> "Dudes In Need"))

      "redirect the user to the 'overseas charity summary' page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.OverseasGiftAidSummaryController.show(taxYear)}"
      }

      "store the data" in {
        val giftAidCYAModel = findGiftAidDb.get
        val id = giftAidCYAModel.overseasCharityNames.head.id

        giftAidCYAModel shouldBe requiredSessionModel.copy(overseasCharityNames = Seq(CharityNameModel(id, "Dudes In Need")))
      }
    }

    "the user enters a valid name and is changing an existing name" when {
      val charityId = UUID().randomUUID
      lazy val result = postResult(url(Some(charityId)),
        Some(requiredSessionModel.copy(overseasCharityNames = Seq(CharityNameModel(charityId, "Dudes")))),
        None, Map("name" -> "Dudess"))

      "redirect the user to the 'overseas charity summary' page" in {
        result.status shouldBe SEE_OTHER
        result.headers("Location").head shouldBe s"${controllers.charity.routes.OverseasGiftAidSummaryController.show(taxYear)}"
      }

      "store the data" in {
        findGiftAidDb.get shouldBe requiredSessionModel.copy(overseasCharityNames = Seq(CharityNameModel(charityId, "Dudess")))
      }
    }
  }
}