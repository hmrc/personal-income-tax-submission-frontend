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

import models.charity.GiftAidCYAModel
import models.charity.prior.{GiftAidPaymentsModel, GiftAidSubmissionModel}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status._
import utils.CharityITHelper

class GiftAidAppendNextYearTaxAmountControllerSpec extends CharityITHelper {

  val urlWithSameYears = "/income-through-software/return/personal-income/2022/charity/amount-after-5-april-2022-added-to-this-tax-year"
  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  def url: String = url(year, year)

  def url(taxYear: Int, someTaxYear: Int): String =
    s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/amount-after-5-april-$someTaxYear-added-to-this-tax-year"

  trait SpecificExpectedResults {
    val heading: String
    val priorP1: String
    val cyaP1: String
    val tooLongError: String
    val emptyFieldError: String
    val incorrectFormatError: String
  }

  trait CommonExpectedResults {
    val hintText: String
    val expectedCaption: String
    val inputName: String
    val button: String
    val error: String
  }

  object Selectors {
    val titleSelector = "title"
    val inputField = ".govuk-input"
    val errorHref = "#amount"
    val p1Selector = "#main-content > div > div > form > div > label > p"
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val hintText: String = "For example, £600 or £193.54"
    val expectedCaption: String = "Donations to charity for 6 April 2021 to 5 April 2022"
    val inputName: String = "amount"
    val button: String = "Continue"
    val error = "Error: "
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val hintText: String = "Er enghraifft, £600 neu £193.54"
    val expectedCaption: String = "Rhoddion i elusennau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022"
    val inputName: String = "amount"
    val button: String = "Yn eich blaen"
    val error = "Gwall: "
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val heading: String = "How much of the donations you made after 5 April 2022 do you want to add to this tax year?"
    val priorP1: String = "You told us you want to add £444 of the donations you made after 5 April 2022 to this tax year. Tell us if this has changed."
    val cyaP1: String = "You told us you want to add £50 of the donations you made after 5 April 2022 to this tax year. Tell us if this has changed."
    val tooLongError: String = "The amount of your donation made after 5 April 2022 you add to the last tax year must be less than £100,000,000,000"
    val emptyFieldError: String = "Enter the amount of your donation made after 5 April 2022 you want to add to this tax year"
    val incorrectFormatError: String = "Enter the amount you want to add to this tax year in the correct format"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val heading: String = "How much of the donations your client made after 5 April 2022 do you want to add to this tax year?"
    val priorP1: String = "You told us you want to add £444 of the donations your client made after 5 April 2022 to this tax year. Tell us if this has changed."
    val cyaP1: String = "You told us you want to add £50 of the donations your client made after 5 April 2022 to this tax year. Tell us if this has changed."
    val tooLongError: String = "The amount of your client’s donation made after 5 April 2022 you add to the last tax year must be less than £100,000,000,000"
    val emptyFieldError: String = "Enter the amount of your client’s donation made after 5 April 2022 you want to add to this tax year"
    val incorrectFormatError: String = "Enter the amount you want to add to this tax year in the correct format"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val heading: String = "Faint o’r rhoddion a wnaethoch ar ôl 5 Ebrill 2022 ydych am eu hychwanegu at y flwyddyn dreth hon?"
    val priorP1: String = "You told us you want to add £444 of the donations you made after 5 April 2022 to this tax year. Tell us if this has changed."
    val cyaP1: String = "You told us you want to add £50 of the donations you made after 5 April 2022 to this tax year. Tell us if this has changed."
    val tooLongError: String = "Mae’n rhaid i swm eich rhodd a wnaed ar ôl 5 Ebrill 2022 a ychwanegwch at y flwyddyn dreth ddiwethaf fod yn llai na £100,000,000,000"
    val emptyFieldError: String = "Nodwch swm eich rhodd a wnaed ar ôl 5 Ebrill 2022 rydych am ei ychwanegu at y flwyddyn dreth hon"
    val incorrectFormatError: String = "Nodwch y swm rydych am ei ychwanegu at y flwyddyn dreth hon yn y fformat cywir"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val heading: String = "Faint o’r rhoddion a wnaeth eich cleient ar ôl 5 Ebrill 2022 ydych am eu hychwanegu at y flwyddyn dreth hon?"
    val priorP1: String = "You told us you want to add £444 of the donations your client made after 5 April 2022 to this tax year. Tell us if this has changed."
    val cyaP1: String = "You told us you want to add £50 of the donations your client made after 5 April 2022 to this tax year. Tell us if this has changed."
    val tooLongError: String = "Mae’n rhaid i swm rhodd eich cleient a wnaed ar ôl 5 Ebrill 2022 a ychwanegwch at y flwyddyn dreth ddiwethaf fod yn llai na £100,000,000,000"
    val emptyFieldError: String = "Nodwch swm rhodd eich cleient a wnaed ar ôl 5 Ebrill 2022 rydych am ei ychwanegu at y flwyddyn dreth hon"
    val incorrectFormatError: String = "Nodwch y swm rydych am ei ychwanegu at y flwyddyn dreth hon yn y fformat cywir"
  }

  val amount: Int = 2000

  val requiredSessionModel: GiftAidCYAModel = GiftAidCYAModel(addDonationToThisYear = Some(true))
  val requiredSessionData: Option[GiftAidCYAModel] = Some(requiredSessionModel)

  val requiredSessionModelPrefill: GiftAidCYAModel = GiftAidCYAModel(
    addDonationToThisYear = Some(true),
    addDonationToThisYearAmount = Some(amount)
  )

  val requiredSessionDataPrefill: Option[GiftAidCYAModel] = Some(requiredSessionModelPrefill)

  val requiredPriorData = Some(IncomeSourcesModel(None, None, Some(priorDataMax)))

  val validForm: Map[String, String] = Map("amount" -> "1234")

  ".show" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "redirect to a correct URL when years don't match up" which {
          lazy val result = getResult(url(year, year + 1), requiredSessionData, None, user.isAgent, user.isWelsh)

          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").headOption shouldBe Some(urlWithSameYears)
          }
        }

        "render the page with correct content" which {
          lazy val result = getResult(url, requiredSessionData, None, user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.heading, user.isWelsh)
          h1Check(user.specificExpectedResults.get.heading + " " + expectedCaption)
          inputFieldCheck(inputName, Selectors.inputField)
          hintTextCheck(hintText)
          captionCheck(expectedCaption)
          buttonCheck(button)
          welshToggleCheck(user.isWelsh)
        }

        "render the page with correct content with prefilled CYA data" which {
          lazy val result = getResult(url, requiredSessionDataPrefill, None, user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.heading)
          h1Check(user.specificExpectedResults.get.heading + " " + expectedCaption)
          inputFieldCheck(inputName, Selectors.inputField)
          hintTextCheck(hintText)
          captionCheck(expectedCaption)
          buttonCheck(button)
          welshToggleCheck(user.isWelsh)
        }

        "display the correct prior amount when returning after submission" which {
          lazy val result = getResult(url, requiredSessionData, requiredPriorData, user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          inputFieldCheck(inputName, Selectors.inputField)
          inputFieldValueCheck("", Selectors.inputField)
          textOnPageCheck(user.specificExpectedResults.get.priorP1, Selectors.p1Selector)
        }

        "display the correct cya amount when returning before resubmitting" which {
          lazy val result = getResult(url, Some(completeGiftAidCYAModel), requiredPriorData, user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import user.commonExpectedResults._

          "has an OK status" in {
            result.status shouldBe OK
          }

          inputFieldCheck(inputName, Selectors.inputField)
          inputFieldValueCheck("50", Selectors.inputField)
          textOnPageCheck(user.specificExpectedResults.get.cyaP1, Selectors.p1Selector)
        }
      }
    }

    "there is no cya session data" should {

      "redirect to the overview page" which {

        lazy val result = getResult(url, None, None)

        "has a status of SEE_OTHER(303)" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the overview page" in {
          result.headers("Location").head shouldBe overviewUrl
        }
      }
    }

    "the addDonationToThisYear field is empty" should {

      "redirect to the donations to previous tax year controller" which {
        val cyaData = GiftAidCYAModel(
          donationsViaGiftAid = Some(true),
          addDonationToLastYear = Some(false)
        )

        lazy val result = getResult(url, Some(cyaData), None)

        "has a status of SEE_OTHER (303)" in {
          result.status shouldBe SEE_OTHER
        }

        "has the correct redirect URL" in {
          result.headers("Location").head shouldBe controllers.charity.routes.DonationsToPreviousTaxYearController.show(year, year).url
        }
      }

      "there is prior data for nextTaxYearTreatedAsCurrent" should {

        "display the GiftAidAppendNextYearTaxAmount page when the 'Change' link is clicked on the CYA page " which {

          val priorData = IncomeSourcesModel(None, None,
            giftAid = Some(GiftAidSubmissionModel(Some(GiftAidPaymentsModel(nextYearTreatedAsCurrentYear = Some(1000.56)))))
          )

          lazy val result = getResult(url, requiredSessionData, Some(priorData))

          "has an 200 OK status" in {
            result.status shouldBe OK
          }

        }
      }
    }

    "addDonationToThisYear is false" should {

      "redirect to the did you donate shares, securities, land or property page" which {

        lazy val result = getResult(url, Some(GiftAidCYAModel(addDonationToThisYear = Some(false))), None)

        "has a status of SEE_OTHER (303)" in {
          result.status shouldBe SEE_OTHER
        }

        "has a redirect location of the shares, securities, land or property yes/no page" in {
          result.headers("Location").head shouldBe controllers.charity.routes.GiftAidSharesSecuritiesLandPropertyDonationController.show(year).url
        }
      }
    }
  }

  ".submit" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "redirect to a correct URL when years don't match up" which {
          lazy val result = postResult(url(year, year + 1), requiredSessionData, None, validForm, user.isAgent, user.isWelsh)

          "has an SEE_OTHER status" in {
            result.status shouldBe SEE_OTHER
            result.headers("Location").headOption shouldBe Some(urlWithSameYears)
          }
        }

        "return an error" when {

          "the submitted data is empty" which {
            lazy val form: Map[String, String] = Map("amount" -> "")

            lazy val result = postResult(url, requiredSessionData, None, form, user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import user.commonExpectedResults._
            titleCheck(error + user.specificExpectedResults.get.heading, user.isWelsh)
            h1Check(user.specificExpectedResults.get.heading + " " + expectedCaption)
            inputFieldCheck(inputName, Selectors.inputField)
            hintTextCheck(hintText)
            captionCheck(expectedCaption)
            buttonCheck(button)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.emptyFieldError, Selectors.errorHref, user.isWelsh)
            errorAboveElementCheck(user.specificExpectedResults.get.emptyFieldError)
          }

          "the submitted data is too long" which {
            lazy val form: Map[String, String] = Map("amount" -> "999999999999999999999999999999999999999999999999")

            lazy val result = postResult(url, requiredSessionData, None, form, user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import user.commonExpectedResults._
            titleCheck(error + user.specificExpectedResults.get.heading, user.isWelsh)
            h1Check(user.specificExpectedResults.get.heading + " " + expectedCaption)
            inputFieldCheck(inputName, Selectors.inputField)
            hintTextCheck(hintText)
            captionCheck(expectedCaption)
            buttonCheck(button)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.tooLongError, Selectors.errorHref, user.isWelsh)
            errorAboveElementCheck(user.specificExpectedResults.get.tooLongError)
          }

          "the submitted data is in the incorrect format" which {
            lazy val form: Map[String, String] = Map("amount" -> ":@~{}<>?")

            lazy val result = postResult(url, requiredSessionData, None, form, user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import user.commonExpectedResults._
            titleCheck(error + user.specificExpectedResults.get.heading, user.isWelsh)
            h1Check(user.specificExpectedResults.get.heading + " " + expectedCaption)
            inputFieldCheck(inputName, Selectors.inputField)
            hintTextCheck(hintText)
            captionCheck(expectedCaption)
            buttonCheck(button)
            welshToggleCheck(user.isWelsh)

            errorSummaryCheck(user.specificExpectedResults.get.incorrectFormatError, Selectors.errorHref, user.isWelsh)
            errorAboveElementCheck(user.specificExpectedResults.get.incorrectFormatError)
          }
        }
      }
    }

    "submission is successful and completes the CYA model" should {

      "redirect to the CYA page" which {
        lazy val result = postResult(url, Some(completeGiftAidCYAModel), None, validForm)

        "has a status of SEE_OTHER(303)" in {
          result.status shouldBe SEE_OTHER
        }

        "redirects to the check your answers page" in {
          result.headers("Location").head shouldBe controllers.charity.routes.GiftAidCYAController.show(year).url
        }

        "addDonationToThisYearAmount should be 1234" in {
          findGiftAidDb.get.addDonationToThisYearAmount shouldBe Some(1234)
        }
      }
    }

    "submission is successful, but does not complete the CYA model" should {

      "redirect to the donation of SSLP page" which {
        lazy val result = postResult(url, requiredSessionData, None, validForm)

        "has a status of SEE_OTHER (303)" in {
          result.status shouldBe SEE_OTHER
        }

        "has a redirect location of the shares, securities, land or property yes/no page" in {
          result.headers("Location").head shouldBe controllers.charity.routes.GiftAidSharesSecuritiesLandPropertyDonationController.show(year).url
        }

        "addDonationToThisYearAmount should be 1234" in {
          findGiftAidDb.get.addDonationToThisYearAmount shouldBe Some(1234)
        }
      }
    }

    "there is no session data" should {

      "redirect to the overview page" which {

        lazy val result = postResult(url, None, None, validForm)

        "has a status of SEE_OTHER (303)" in {
          result.status shouldBe SEE_OTHER
        }

        "has the redirect url to the overview page" in {
          result.headers("Location").head shouldBe overviewUrl
        }
      }
    }
  }
}

