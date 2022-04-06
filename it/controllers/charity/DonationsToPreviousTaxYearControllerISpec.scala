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

import forms.YesNoForm
import models.charity.GiftAidCYAModel
import models.charity.prior.{GiftAidPaymentsModel, GiftAidSubmissionModel}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.{BAD_REQUEST, OK, SEE_OTHER}
import play.api.libs.ws.WSResponse
import utils.{CharityITHelper, GiftAidDatabaseHelper}

import scala.concurrent.Future
import scala.util.Try

class DonationsToPreviousTaxYearControllerISpec extends CharityITHelper with GiftAidDatabaseHelper {

  lazy val controller: DonationsToPreviousTaxYearController = app.injector.instanceOf[DonationsToPreviousTaxYearController]

  def url(overrideYear: Int = taxYear): String = s"$appUrl/$taxYear/charity/donations-after-5-april-$overrideYear"
  val urlWithSameYears = s"/update-and-submit-income-tax-return/personal-income/$taxYear/charity/donations-after-5-april-$taxYear"

  object Selectors {
    val paragraph1HintText = "#p1"
    val paragraph2HintText = "#p2"
    val errorHref = "#value"
  }

  trait SpecificExpectedResults {
    val errorText: String
    val expectedParagraph1: String
    val expectedParagraph2: String
  }

  trait CommonExpectedResults {
    val expectedHeading: String
    val expectedCaption: String
    val yesText: String
    val noText: String
    val button: String
    val error: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val expectedHeading = s"Do you want to add any donations made after 5 April $taxYear to this tax year?"
    val expectedCaption = s"Donations to charity for 6 April $taxYearEOY to 5 April $taxYear"
    val yesText = "Yes"
    val noText = "No"
    val button = "Continue"
    val error = "Error: "
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val expectedHeading = s"A ydych am ychwanegu unrhyw roddion a wnaed ar ôl 5 Ebrill $taxYear i’r flwyddyn dreth hon?"
    val expectedCaption = s"Rhoddion i elusennau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val yesText = "Iawn"
    val noText = "Na"
    val button = "Yn eich blaen"
    val error = "Gwall: "
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val errorText = s"Select yes to add any of your donations made after 5 April $taxYear to this tax year"
    val expectedParagraph1: String = s"If you made donations after 5 April $taxYear, you can add them to the 6 April $taxYearEOY to 5 April $taxYear tax year."
    val expectedParagraph2: String = "You might want to do this if you want tax relief sooner."
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val errorText = s"Select yes to add any of your client’s donations made after 5 April $taxYear to this tax year"
    val expectedParagraph1: String = s"If your client made donations after 5 April $taxYear, you can add them to the 6 April $taxYearEOY to 5 April $taxYear tax year."
    val expectedParagraph2: String = "You might want to do this if your client wants tax relief sooner."
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val errorText = s"Dewiswch ‘Iawn’ os ydych am ychwanegu unrhyw roddion a wnaed ar ôl 5 Ebrill $taxYear i’r flwyddyn dreth hon"
    val expectedParagraph1: String = s"Os gwnaethoch roddion ar ôl 5 Ebrill $taxYear, gallwch eu hychwanegu at flwyddyn dreth 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear."
    val expectedParagraph2: String = "Mae’n bosibl y byddwch am wneud hyn os ydych am gael gostyngiad treth yn gynt."
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val errorText = s"Dewiswch ‘Iawn’ os ydych am ychwanegu unrhyw roddion a wnaed gan eich cleient ar ôl 5 Ebrill $taxYear i’r flwyddyn dreth hon"
    val expectedParagraph1: String = s"Os gwnaeth eich cleient roddion ar ôl 5 Ebrill $taxYear, gallwch eu hychwanegu at flwyddyn dreth 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear."
    val expectedParagraph2: String = "Mae’n bosibl am wneud hyn os yw’ch cleient am gael gostyngiad treth yn gynt."
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true,  CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  val validAmount = 50

  val requiredSessionModel: GiftAidCYAModel = GiftAidCYAModel(
    donationsViaGiftAid = Some(true),
    addDonationToLastYear = Some(false)
  )
  val requiredSessionData: Some[GiftAidCYAModel] = Some(requiredSessionModel)

  val requiredSessionModelPrefill: GiftAidCYAModel = GiftAidCYAModel(
    donationsViaGiftAid = Some(true),
    addDonationToLastYear = Some(false),
    addDonationToThisYear = Some(false)
  )

  val requiredSessionDataPrefill: Some[GiftAidCYAModel] = Some(requiredSessionModelPrefill)

  ".show" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "redirect to a correct URL when years don't match up" which {
          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url(taxYear + 1), welsh = user.isWelsh, follow = false, headers =  playSessionCookie(user.isAgent))
          }

          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").headOption shouldBe Some(urlWithSameYears)
          }
        }

        "render the page with correct content" which {
          lazy val result: WSResponse = getResult(url(), requiredSessionData, None, user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedHeading, user.isWelsh)
          h1Check(expectedHeading + " " + expectedCaption)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph1, Selectors.paragraph1HintText)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph2, Selectors.paragraph2HintText)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          captionCheck(expectedCaption)
          buttonCheck(button)
          welshToggleCheck(user.isWelsh)
        }

        "render the page with correct content and prefilled CYA data" which {
          lazy val result: WSResponse = getResult(url(), requiredSessionDataPrefill, None, user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(expectedHeading, user.isWelsh)
          h1Check(expectedHeading + " " + expectedCaption)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph1, Selectors.paragraph1HintText)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph2, Selectors.paragraph2HintText)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          radioButtonHasChecked(noText, 2)
          captionCheck(expectedCaption)
          buttonCheck(button)
          welshToggleCheck(user.isWelsh)
        }
      }
    }

    "there is prior data for nextYearTreatedAsCurrentYear" should {

      "redirect to the cya page" which {
        val priorData = IncomeSourcesModel(
          giftAid = Some(GiftAidSubmissionModel(Some(GiftAidPaymentsModel(nextYearTreatedAsCurrentYear = Some(1000.00)))))
        )

        lazy val result = getResult(url(), requiredSessionData, Some(priorData))

        "has a status of SEE_OTHER(303)" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the overview page" in {
          result.headers("Location").head shouldBe s"${controllers.charity.routes.GiftAidCYAController.show(taxYear)}"
        }
      }
    }

    "the is no session data" should {

      "redirect to the overview page" which {

        lazy val result = getResult(url(), None, None)

        "has a status of SEE_OTHER(303)" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the overview page" in {
          result.headers("Location").head shouldBe overviewUrl
        }
      }
    }

    "the user has indicated they attribute donations to last year, but have not entered an amount" should {

      "redirect to the how much donations to be added to this tax year from next page" which {
        val cyaData = GiftAidCYAModel(
          donationsViaGiftAid = Some(true),
          donationsViaGiftAidAmount = Some(validAmount),
          addDonationToLastYear = Some(true)
        )
        lazy val result = getResult(url(), Some(cyaData), None)

        "has a status of SEE_OTHER(303)" in {
          result.status shouldBe SEE_OTHER
        }

        "have the correct redirect url" in {
          result.headers("Location").head shouldBe controllers.charity.routes.LastTaxYearAmountController.show(taxYear).url
        }
      }
    }

    "the user has donated to gift aid, but 'addToLastTaxYear' is missing" should {

      "redirect to the donations to be attributed to last tax year page" which {
        val cyaData = GiftAidCYAModel(
          donationsViaGiftAid = Some(true),
          donationsViaGiftAidAmount = Some(validAmount),
          overseasDonationsViaGiftAid = Some(false)
        )
        lazy val result = getResult(url(), Some(cyaData), None)

        "has a status of SEE_OTHER(303)" in {
          result.status shouldBe SEE_OTHER
        }

        "have the correct redirect url" in {
          result.headers("Location").head shouldBe controllers.charity.routes.GiftAidLastTaxYearController.show(taxYear).url
        }
      }
    }

    "the cya model is missing the donationsViaGiftAid field" should {

      "redirect to 'did you donate via gift aid'" which {

        lazy val result = getResult(url(), Some(GiftAidCYAModel()), None)

        "has a status of SEE_OTHER(303)" in {
          result.status shouldBe SEE_OTHER
        }

        "have the correct redirect url" in {
          result.headers("Location").head shouldBe controllers.charity.routes.GiftAidDonationsController.show(taxYear).url
        }
      }
    }
  }

  ".submit" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "redirect to a correct URL when years don't match up" which {
          lazy val form: Map[String, Seq[String]] = Map(YesNoForm.yesNo -> Seq(YesNoForm.yes))

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(url(taxYear + 1), body = form, follow = false, welsh = user.isWelsh, headers =  playSessionCookie(user.isAgent))
          }

          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").headOption shouldBe Some(urlWithSameYears)
          }
        }

        "return a SEE_OTHER" in {
          lazy val form: Map[String, Seq[String]] = Map(YesNoForm.yesNo -> Seq(YesNoForm.yes))

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(url(taxYear), body = form, follow = false, welsh = user.isWelsh, headers =  playSessionCookie(user.isAgent))
          }

          result.status shouldBe SEE_OTHER
        }

        "no radio button has been selected" should {

          lazy val form: Map[String, Seq[String]] = Map(YesNoForm.yesNo -> Seq(""))

          lazy val result: WSResponse = {
            authoriseAgentOrIndividual(user.isAgent)
            urlPost(url(taxYear), body = form, follow = false, welsh = user.isWelsh, headers =  playSessionCookie(user.isAgent))
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import user.commonExpectedResults._

          titleCheck(error + expectedHeading, user.isWelsh)
          h1Check(expectedHeading + " " + expectedCaption)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph1, Selectors.paragraph1HintText)
          textOnPageCheck(user.specificExpectedResults.get.expectedParagraph2, Selectors.paragraph2HintText)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          captionCheck(expectedCaption)
          buttonCheck(button)
          errorSummaryCheck(user.specificExpectedResults.get.errorText, Selectors.errorHref, user.isWelsh)
          errorAboveElementCheck(user.specificExpectedResults.get.errorText)
          welshToggleCheck(user.isWelsh)

          "return a BAD_REQUEST" in {
            result.status shouldBe BAD_REQUEST
          }
        }
      }
    }

    "the user has selected 'yes'" should {

      "redirect to the appendNextYear amount page" which {

        lazy val result = postResult(url(), requiredSessionData, None, Map(YesNoForm.yesNo -> YesNoForm.yes))

        "has a status of SEE_OTHER(303)" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the donation to previous year amount page" in {
          result.headers("Location").head shouldBe controllers.charity.routes.GiftAidAppendNextYearTaxAmountController.show(taxYear, taxYear).url
        }

        "addDonationToThisYear should be true" in {
          findGiftAidDb.get.addDonationToThisYear shouldBe Some(true)
        }
      }
    }

    "the user has selected 'no'" should {

      "redirect to the cya page when this completes the cya model" which {

        lazy val result =
          postResult(
            url(),
            Some(completeGiftAidCYAModel),
            None,
            Map(YesNoForm.yesNo -> YesNoForm.no)
          )

        "has a status of SEE_OTHER(303)" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the check your answers page" in {
          result.headers("Location").head shouldBe controllers.charity.routes.GiftAidCYAController.show(taxYear).url
        }

        "addDonationToLastYear should be false" in {
          findGiftAidDb.get.addDonationToThisYear shouldBe Some(false)
        }

        "addDonationToLastYearAmount should have been cleared" in {
          findGiftAidDb.get.addDonationToThisYearAmount shouldBe None
        }
      }

      "redirect to the donation of shares or securities page when this does not complete the cya model" which {

        lazy val result =
          postResult(
            url(),
            Some(requiredSessionModel),
            None,
            Map(YesNoForm.yesNo -> YesNoForm.no)
          )

        "has a status of SEE_OTHER(303)" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the donation of qualifying shares or securities page" in {
          result.headers("Location").head shouldBe
            controllers.charity.routes.GiftAidQualifyingSharesSecuritiesController.show(taxYear).url
        }

        "addDonationToLastYear should be false" in {
          findGiftAidDb.get.addDonationToThisYear shouldBe Some(false)
        }
      }
    }

    "there is prior data for nextYearTreatedAsCurrentYear" should {

      "redirect to the overview page" which {
        val priorData = IncomeSourcesModel(
          giftAid = Some(GiftAidSubmissionModel(Some(GiftAidPaymentsModel(nextYearTreatedAsCurrentYear = Some(1000.00)))))
        )
        lazy val result = postResult(url(), requiredSessionData, Some(priorData), Map(YesNoForm.yesNo -> YesNoForm.yes))

        "has a status of SEE_OTHER(303)" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the cya page" in {
          result.headers("Location").head shouldBe cyaUrl(taxYear)
        }
      }
    }

    "there is no cya data" should {

      "redirect to the overview page" which {

        lazy val result = postResult(url(), None, None, Map(YesNoForm.yesNo -> YesNoForm.yes))

        "has a status of SEE_OTHER(303)" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the overview page" in {
          result.headers("Location").head shouldBe overviewUrl
        }
      }
    }
  }
}