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

import models.APIErrorBodyModel
import models.charity.prior.{GiftAidPaymentsModel, GiftAidSubmissionModel, GiftsModel}
import models.charity.{CharityNameModel, GiftAidCYAModel}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import utils.CharityITHelper

class GiftAidCYAControllerISpec extends CharityITHelper {

  val taxYear = 2022

  val amount: String = "£100"

  val url: String = s"$appUrl/$taxYear/charity/check-donations-to-charity"

  val cyaDataMax: GiftAidCYAModel = GiftAidCYAModel(
    Some(true), Some(100.00),
    Some(true), Some(100.00),
    Some(true), Some(100.00), Seq(CharityNameModel("Belgium Trust"), CharityNameModel("American Trust")),
    Some(true), Some(100.00),
    Some(true), Some(100.00),
    Some(true), Some(true), Some(100.00), Some(true), Some(100.00),
    Some(true), Some(100.00), Seq(CharityNameModel("Belgium Trust"), CharityNameModel("American Trust"))
  )

  val cyaDataMin: GiftAidCYAModel = GiftAidCYAModel(
    Some(false), None,
    Some(false), None,
    Some(false), None, Seq.empty,
    Some(false), None,
    Some(false), None,
    Some(false), None, None, None, None,
    Some(false), None, Seq.empty
  )

  val cyaDataIncomplete: GiftAidCYAModel = GiftAidCYAModel(
    Some(false), None
  )

  override val priorDataMax: GiftAidSubmissionModel = GiftAidSubmissionModel(
    Some(GiftAidPaymentsModel(
      Some(100.00),
      Some(List("Jello Corporation")),
      Some(100.00),
      Some(100.00),
      Some(100.00),
      Some(100.00)
    )),
    Some(GiftsModel(
      Some(100.00),
      Some(List("Simbas College Fund")),
      Some(100.00),
      Some(100.00)
    ))
  )

  val unchangedPriorData: GiftAidSubmissionModel = GiftAidSubmissionModel(
    Some(GiftAidPaymentsModel(
      Some(100.00),
      Some(List("Belgium Trust", "American Trust")),
      Some(100.00),
      Some(100.00),
      Some(100.00),
      Some(100.00)
    )),
    Some(GiftsModel(
      Some(100.00),
      Some(List("Belgium Trust", "American Trust")),
      Some(100.00),
      Some(100.00)
    ))
  )

  val priorDataMin: GiftAidSubmissionModel = GiftAidSubmissionModel(
    Some(GiftAidPaymentsModel(
      Some(100.00),
      None,
      Some(100.00),
      Some(100.00),
      Some(100.00),
      Some(100.00)
    )),
    Some(GiftsModel(
      Some(100.00),
      None,
      Some(100.00),
      Some(100.00)
    ))
  )

  trait SpecificExpectedResults {
    val title: String
    val overseasDonationNames: String
    val overseasSharesSecurityLandPropertyNames: String
  }

  trait CommonExpectedResults {
    val yes: String
    val no: String
    val caption: String
    val donationViaGiftAid: String
    val donationViaGiftAidAmount: String
    val oneOffDonation: String
    val oneOffDonationAmount: String
    val overseasDonation: String
    val overseasDonationAmount: String
    val overseasDonationNamesValue: String
    val lastYear: String
    val lastYearAmount: String
    val thisYear: String
    val thisYearAmount: String
    val sharesSecuritiesLandProperty: String
    val sharesSecurities: String
    val sharesSecuritiesAmount: String
    val landProperty: String
    val landPropertyAmount: String
    val overseasSharesSecuritiesLandProperty: String
    val overseasSharesSecuritiesLandPropertyAmount: String
    val overseasSharesSecuritiesLandPropertyNamesValue: String
    val priorDonationNames: String
    val priorSharesSecuritiesLandPropertyNames: String
    val saveAndContinue: String
    val error: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val yes: String = "Yes"
    val no: String = "No"
    val caption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val donationViaGiftAid = "Donation to charity using Gift Aid"
    val donationViaGiftAidAmount = "Amount donated to charity using Gift Aid"
    val oneOffDonation = "One-off donations to charity using Gift Aid"
    val oneOffDonationAmount = "Amount of one-off donations to charity using Gift Aid"
    val overseasDonation = "Donations to overseas charities using Gift Aid"
    val overseasDonationAmount = "Amount donated to overseas charities using Gift Aid"
    val overseasDonationNamesValue = "Belgium Trust American Trust"
    val lastYear = "Adding donations to last tax year"
    val lastYearAmount = "Amount added to last tax year"
    val thisYear = "Donation after 5 April 2022 added to this tax year"
    val thisYearAmount = "Amount of donation after 5 April 2022 added to this tax year"
    val sharesSecuritiesLandProperty = "Donation of shares, securities, land or property"
    val sharesSecurities = "Donation of shares or securities"
    val sharesSecuritiesAmount = "Value of shares or securities"
    val landProperty = "Donation of land or property"
    val landPropertyAmount = "Value of land or property"
    val overseasSharesSecuritiesLandProperty = "Donation of shares, securities, land or property to overseas charities"
    val overseasSharesSecuritiesLandPropertyAmount = "Value of shares, securities, land or property to overseas charities"
    val overseasSharesSecuritiesLandPropertyNamesValue = "Belgium Trust American Trust"
    val priorDonationNames = "Jello Corporation"
    val priorSharesSecuritiesLandPropertyNames = "Simbas College Fund"
    val saveAndContinue = "Save and continue"
    val error = "Sorry, there is a problem with the service"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val yes: String = "Iawn"
    val no: String = "Na"
    val caption = "Rhoddion i elusennau ar gyfer 6 Ebrill 2021 i 5 Ebrill 2022"
    val donationViaGiftAid = "Rhoddion i elusen drwy ddefnyddio Rhodd Cymorth"
    val donationViaGiftAidAmount = "Swm a roddwyd i elusen drwy ddefnyddio Rhodd Cymorth"
    val oneOffDonation = "Rhoddion untro i elusen drwy ddefnyddio Rhodd Cymorth"
    val oneOffDonationAmount = "Swm y rhoddion untro i elusen drwy ddefnyddio Rhodd Cymorth"
    val overseasDonation = "Rhoddion i elusennau tramor drwy ddefnyddio Rhodd Cymorth"
    val overseasDonationAmount = "Swm y rhoddion i elusennau tramor drwy ddefnyddio Rhodd Cymorth"
    val overseasDonationNamesValue = "Belgium Trust American Trust"
    val lastYear = "Ychwanegu rhoddion at y flwyddyn dreth ddiwethaf"
    val lastYearAmount = "Y swm a ychwanegwyd at y flwyddyn dreth ddiwethaf"
    val thisYear = "Ychwanegwyd rhodd ar ôl 5 Ebrill 2022 at y flwyddyn dreth hon"
    val thisYearAmount = "Swm y rhodd ar ôl 5 Ebrill 2022 wedi’i ychwanegu at y flwyddyn dreth hon"
    val sharesSecuritiesLandProperty = "Rhoi cyfranddaliadau, gwarantau, tir neu eiddo"
    val sharesSecurities = "Rhoi cyfranddaliadau neu warantau"
    val sharesSecuritiesAmount = "Swm y cyfranddaliadau neu warantau"
    val landProperty = "Rhoi tir neu eiddo"
    val landPropertyAmount = "Swm y tir neu eiddo"
    val overseasSharesSecuritiesLandProperty = "Rhoi cyfranddaliadau, gwarantau, tir neu eiddo i elusennau tramor"
    val overseasSharesSecuritiesLandPropertyAmount = "Swm y cyfranddaliadau, gwarantau, tir neu eiddo i elusennau tramor"
    val overseasSharesSecuritiesLandPropertyNamesValue = "Belgium Trust American Trust"
    val priorDonationNames = "Jello Corporation"
    val priorSharesSecuritiesLandPropertyNames = "Simbas College Fund"
    val saveAndContinue = "Cadw ac yn eich blaen"
    val error = "Mae’n ddrwg gennym – mae problem gyda’r gwasanaeth"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val title = "Check your donations to charity"
    val overseasDonationNames = "Overseas charities you donated to"
    val overseasSharesSecurityLandPropertyNames = "Overseas charities you donated shares, securities, land or property to"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val title = "Check your client’s donations to charity"
    val overseasDonationNames = "Overseas charities your client donated to"
    val overseasSharesSecurityLandPropertyNames = "Overseas charities your client donated shares, securities, land or property to"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val title = "Gwiriwch eich rhoddion i elusen"
    val overseasDonationNames = "Elusennau tramor a roesoch rhodd iddynt"
    val overseasSharesSecurityLandPropertyNames = "Elusen dramor y gwnaethoch roi cyfranddaliadau, gwarantau, tir neu eiddo iddi"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val title = "Gwiriwch roddion eich cleient i elusen"
    val overseasDonationNames = "Elusennau tramor a roddodd eich cleient rhodd iddynt"
    val overseasSharesSecurityLandPropertyNames = "Elusen dramor y gwnaeth eich cleient roi cyfranddaliadau, gwarantau, tir neu eiddo iddi"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  def cyaRowCheck(expectedText: String, expectedValue: String, changeLinkHref: String, rowNumber: Int)(implicit document: () => Document): Unit = {
    val keySelector = s"#main-content > div > div > dl > div:nth-child($rowNumber) > dt"
    val valueSelector = s"#main-content > div > div > dl > div:nth-child($rowNumber) > dd.govuk-summary-list__value"
    val changeLinkSelector = s"#main-content > div > div > dl > div:nth-child($rowNumber) > dd.govuk-summary-list__actions > a"

    s"row number $rowNumber is correct" which {

      s"has the correct row name of '$expectedText'" in {
        document().select(keySelector).text() shouldBe expectedText
      }

      s"has the correct row value of '$expectedValue'" in {
        document().select(valueSelector).text() shouldBe expectedValue
      }

      s"the change link should go to '$changeLinkHref''" in {
        document().select(changeLinkSelector).attr("href") shouldBe changeLinkHref
      }

    }
  }

  def response(
                cya: Option[GiftAidCYAModel] = None,
                prior: Option[GiftAidSubmissionModel] = None
              ): WSResponse = {
    val priorModel = IncomeSourcesModel(giftAid = prior)

    dropGiftAidDB()

    userDataStub(priorModel, nino, taxYear)
    insertCyaData(cya)

    authoriseIndividual()
    await(wsClient.url(url).withFollowRedirects(false).withHttpHeaders(xSessionId, csrfContent).get())
  }

  ".show" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the page with full CYA model" which {

          lazy val result = getResult(url, Some(cyaDataMax), None, user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          import user.commonExpectedResults._

          "has an OK (200) status" in {
            result.status shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.title, user.isWelsh)
          h1Check(user.specificExpectedResults.get.title + " " + caption)
          captionCheck(caption)

          //noinspection ScalaStyle
          {
            cyaRowCheck(donationViaGiftAid, yes, controllers.charity.routes.GiftAidDonationsController.show(year).url, 1)
            cyaRowCheck(donationViaGiftAidAmount, amount, controllers.charity.routes.GiftAidDonatedAmountController.show(year).url, 2)

            cyaRowCheck(oneOffDonation, yes, controllers.charity.routes.GiftAidOneOffController.show(year).url, 3)
            cyaRowCheck(oneOffDonationAmount, amount, controllers.charity.routes.GiftAidOneOffAmountController.show(year).url, 4)

            cyaRowCheck(overseasDonation, yes, controllers.charity.routes.OverseasGiftAidDonationsController.show(year).url, 5)
            cyaRowCheck(overseasDonationAmount, amount, controllers.charity.routes.GiftAidOverseasAmountController.show(year).url, 6)
            cyaRowCheck(user.specificExpectedResults.get.overseasDonationNames, overseasDonationNamesValue, controllers.charity.routes.OverseasGiftAidSummaryController.show(year).url, 7)

            cyaRowCheck(lastYear, yes, controllers.charity.routes.GiftAidLastTaxYearController.show(year).url, 8)
            cyaRowCheck(lastYearAmount, amount, controllers.charity.routes.LastTaxYearAmountController.show(year).url, 9)

            cyaRowCheck(thisYear, yes, controllers.charity.routes.DonationsToPreviousTaxYearController.show(year, year).url, 10)
            cyaRowCheck(thisYearAmount, amount, controllers.charity.routes.GiftAidAppendNextYearTaxAmountController.show(year, year).url, 11)

            cyaRowCheck(sharesSecuritiesLandProperty, yes, controllers.charity.routes.GiftAidSharesSecuritiesLandPropertyDonationController.show(year).url, 12)
            cyaRowCheck(sharesSecurities, yes, controllers.charity.routes.GiftAidQualifyingSharesSecuritiesController.show(year).url, 13)
            cyaRowCheck(sharesSecuritiesAmount, amount, controllers.charity.routes.GiftAidTotalShareSecurityAmountController.show(year).url, 14)
            cyaRowCheck(landProperty, yes, controllers.charity.routes.GiftAidDonateLandOrPropertyController.show(year).url, 15)
            cyaRowCheck(landPropertyAmount, amount, controllers.charity.routes.GiftAidLandOrPropertyAmountController.show(year).url, 16)

            cyaRowCheck(overseasSharesSecuritiesLandProperty, yes, controllers.charity.routes.GiftAidSharesSecuritiesLandPropertyOverseasController.show(year).url, 17)
            cyaRowCheck(overseasSharesSecuritiesLandPropertyAmount, amount, controllers.charity.routes.OverseasSharesSecuritiesLandPropertyAmountController.show(year).url, 18)
            cyaRowCheck(user.specificExpectedResults.get.overseasSharesSecurityLandPropertyNames, overseasSharesSecuritiesLandPropertyNamesValue, controllers.charity.routes.OverseasSharesLandSummaryController.show(year).url, 19)
          }

          buttonCheck(saveAndContinue)
          welshToggleCheck(user.isWelsh)
        }

        "return an almost empty CYA view" which {

          "has only the donated land, shares, securities and properties yes/no hidden" when {

            "land or properties is the only value" which {

              val priorData = IncomeSourcesModel(giftAid = Some(GiftAidSubmissionModel(None,
                Some(GiftsModel(
                  landAndBuildings = Some(1000.74)
                ))
              )))

              lazy val result = getResult(url, None, Some(priorData), user.isAgent, user.isWelsh)

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              import user.commonExpectedResults._

              "has an OK (200) status" in {
                result.status shouldBe OK
              }

              titleCheck(user.specificExpectedResults.get.title, user.isWelsh)
              h1Check(user.specificExpectedResults.get.title + " " + caption)
              captionCheck(caption)

              //noinspection ScalaStyle
              {
                cyaRowCheck(donationViaGiftAid, user.commonExpectedResults.no, controllers.charity.routes.GiftAidDonationsController.show(year).url, 1)
                cyaRowCheck(thisYear, user.commonExpectedResults.no, controllers.charity.routes.DonationsToPreviousTaxYearController.show(year, year).url, 2)
                cyaRowCheck(sharesSecurities, user.commonExpectedResults.no, controllers.charity.routes.GiftAidQualifyingSharesSecuritiesController.show(year).url, 3)
                cyaRowCheck(landPropertyAmount, "£1000.74", controllers.charity.routes.GiftAidLandOrPropertyAmountController.show(year).url, 4)
              }

              buttonCheck(saveAndContinue)
              welshToggleCheck(user.isWelsh)
            }

            "shares or securities is the only value" which {

              val priorData = IncomeSourcesModel(giftAid = Some(GiftAidSubmissionModel(None,
                Some(GiftsModel(
                  sharesOrSecurities = Some(1000.74)
                ))
              )))

              lazy val result = getResult(url, None, Some(priorData), user.isAgent, user.isWelsh)

              implicit def document: () => Document = () => Jsoup.parse(result.body)

              import user.commonExpectedResults._

              "has an OK (200) status" in {
                result.status shouldBe OK
              }

              titleCheck(user.specificExpectedResults.get.title, user.isWelsh)
              h1Check(user.specificExpectedResults.get.title + " " + caption)
              captionCheck(caption)

              //noinspection ScalaStyle
              {
                cyaRowCheck(donationViaGiftAid, user.commonExpectedResults.no, controllers.charity.routes.GiftAidDonationsController.show(year).url, 1)
                cyaRowCheck(thisYear, user.commonExpectedResults.no, controllers.charity.routes.DonationsToPreviousTaxYearController.show(year, year).url, 2)
                cyaRowCheck(sharesSecuritiesAmount, "£1000.74", controllers.charity.routes.GiftAidTotalShareSecurityAmountController.show(year).url, 3)
                cyaRowCheck(landProperty, user.commonExpectedResults.no, controllers.charity.routes.GiftAidDonateLandOrPropertyController.show(year).url, 4)
                cyaRowCheck(overseasSharesSecuritiesLandProperty, user.commonExpectedResults.no, controllers.charity.routes.GiftAidSharesSecuritiesLandPropertyOverseasController.show(year).url, 5)
              }

              buttonCheck(saveAndContinue)
              welshToggleCheck(user.isWelsh)
            }

          }

        }

        "return a cya page with all the yes/no questions hidden" when {

          "there is no CYA model, but there is a full prior data model" which {

            lazy val result = getResult(url, None, Some(IncomeSourcesModel(giftAid = Some(priorDataMax))), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import user.commonExpectedResults._

            "has an OK (200) status" in {
              result.status shouldBe OK
            }

            titleCheck(user.specificExpectedResults.get.title, user.isWelsh)
            h1Check(user.specificExpectedResults.get.title + " " + caption)
            captionCheck(caption)

            //noinspection ScalaStyle
            {
              cyaRowCheck(donationViaGiftAidAmount, amount, controllers.charity.routes.GiftAidDonatedAmountController.show(year).url, 1)
              cyaRowCheck(oneOffDonationAmount, amount, controllers.charity.routes.GiftAidOneOffAmountController.show(year).url, 2)
              cyaRowCheck(overseasDonationAmount, amount, controllers.charity.routes.GiftAidOverseasAmountController.show(year).url, 3)
              cyaRowCheck(user.specificExpectedResults.get.overseasDonationNames, priorDonationNames, controllers.charity.routes.OverseasGiftAidSummaryController.show(year).url, 4)
              cyaRowCheck(lastYearAmount, amount, controllers.charity.routes.LastTaxYearAmountController.show(year).url, 5)
              cyaRowCheck(thisYearAmount, amount, controllers.charity.routes.GiftAidAppendNextYearTaxAmountController.show(year, year).url, 6)
              cyaRowCheck(sharesSecuritiesAmount, amount, controllers.charity.routes.GiftAidTotalShareSecurityAmountController.show(year).url, 7)
              cyaRowCheck(landPropertyAmount, amount, controllers.charity.routes.GiftAidLandOrPropertyAmountController.show(year).url, 8)
              cyaRowCheck(overseasSharesSecuritiesLandPropertyAmount, amount, controllers.charity.routes.OverseasSharesSecuritiesLandPropertyAmountController.show(year).url, 9)
              cyaRowCheck(user.specificExpectedResults.get.overseasSharesSecurityLandPropertyNames, priorSharesSecuritiesLandPropertyNames, controllers.charity.routes.OverseasSharesLandSummaryController.show(year).url, 10)
            }

            buttonCheck(saveAndContinue)
            welshToggleCheck(user.isWelsh)
          }

          "there is a full CYA model, and there is a full prior data model" which {

            lazy val result =
              getResult(url, Some(cyaDataMax), Some(IncomeSourcesModel(giftAid = Some(priorDataMax))), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import user.commonExpectedResults._

            "has an OK (200) status" in {
              result.status shouldBe OK
            }

            titleCheck(user.specificExpectedResults.get.title, user.isWelsh)
            h1Check(user.specificExpectedResults.get.title + " " + caption)
            captionCheck(caption)

            //noinspection ScalaStyle
            {
              cyaRowCheck(donationViaGiftAidAmount, amount, controllers.charity.routes.GiftAidDonatedAmountController.show(year).url, 1)
              cyaRowCheck(oneOffDonationAmount, amount, controllers.charity.routes.GiftAidOneOffAmountController.show(year).url, 2)
              cyaRowCheck(overseasDonationAmount, amount, controllers.charity.routes.GiftAidOverseasAmountController.show(year).url, 3)
              cyaRowCheck(user.specificExpectedResults.get.overseasDonationNames, overseasDonationNamesValue, controllers.charity.routes.OverseasGiftAidSummaryController.show(year).url, 4)
              cyaRowCheck(lastYearAmount, amount, controllers.charity.routes.LastTaxYearAmountController.show(year).url, 5)
              cyaRowCheck(thisYearAmount, amount, controllers.charity.routes.GiftAidAppendNextYearTaxAmountController.show(year, year).url, 6)
              cyaRowCheck(sharesSecuritiesAmount, amount, controllers.charity.routes.GiftAidTotalShareSecurityAmountController.show(year).url, 7)
              cyaRowCheck(landPropertyAmount, amount, controllers.charity.routes.GiftAidLandOrPropertyAmountController.show(year).url, 8)
              cyaRowCheck(overseasSharesSecuritiesLandPropertyAmount, amount, controllers.charity.routes.OverseasSharesSecuritiesLandPropertyAmountController.show(year).url, 9)
              cyaRowCheck(user.specificExpectedResults.get.overseasSharesSecurityLandPropertyNames, overseasSharesSecuritiesLandPropertyNamesValue, controllers.charity.routes.OverseasSharesLandSummaryController.show(year).url, 10)
            }

            buttonCheck(saveAndContinue)
            welshToggleCheck(user.isWelsh)
          }

        }

        "return a minimal CYA view" when {

          "the CYA model contains all false values" which {

            lazy val result = getResult(url, Some(cyaDataMin), None, user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            import user.commonExpectedResults._

            "has an OK (200) status" in {
              result.status shouldBe OK
            }

            titleCheck(user.specificExpectedResults.get.title, user.isWelsh)
            h1Check(user.specificExpectedResults.get.title + " " + caption)
            captionCheck(caption)

            //noinspection ScalaStyle
            {
              cyaRowCheck(donationViaGiftAid, user.commonExpectedResults.no, controllers.charity.routes.GiftAidDonationsController.show(year).url, 1)
              cyaRowCheck(thisYear, user.commonExpectedResults.no, controllers.charity.routes.DonationsToPreviousTaxYearController.show(year, year).url, 2)
              cyaRowCheck(sharesSecuritiesLandProperty, user.commonExpectedResults.no, controllers.charity.routes.GiftAidSharesSecuritiesLandPropertyDonationController.show(year).url, 3)
            }

            buttonCheck(saveAndContinue)
            welshToggleCheck(user.isWelsh)

          }

        }

        "redirect to the overview page" when {

          "there is incomplete CYA data" which {

            lazy val result: WSResponse = {

              dropGiftAidDB()
              insertCyaData(Some(cyaDataIncomplete))
              userDataStub(IncomeSourcesModel(), nino, taxYear)

              authoriseAgentOrIndividual(user.isAgent)
              urlGet(url, welsh = user.isWelsh, headers = playSessionCookie(user.isAgent))
            }

            "redirects to the correct url" in {
              result
              verifyGet("/income-through-software/return/2022/view")
            }
          }

          "there is no CYA and no PRIOR data" which {
            lazy val result = {

              dropGiftAidDB()
              insertCyaData(None)
              userDataStub(IncomeSourcesModel(), nino, taxYear)

              authoriseAgentOrIndividual(user.isAgent)
              urlGet(url, welsh = user.isWelsh, headers = playSessionCookie(user.isAgent))
            }

            "redirects to the correct url" in {
              result
              verifyGet("/income-through-software/return/2022/view")
            }
          }
        }
      }
    }
  }

  ".submit" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "redirect to the overview page" when {

          "there is no CYA data available" which {

            val form = Map[String, String]()

            lazy val result = postResult(url, None, None, form, user.isAgent, user.isWelsh)

            "the status is SEE OTHER" in {
              result.status shouldBe SEE_OTHER
            }

            "redirects to the overview page" in {
              result.headers("Location").head shouldBe overviewUrl
            }
          }

          "the request goes through successfully" which {

            val form = Map[String, String]()

            lazy val result: WSResponse = {

              wireMockServer.resetAll()
              dropGiftAidDB()
              insertCyaData(Some(cyaDataMax))
              userDataStub(IncomeSourcesModel(), nino, taxYear)
              authoriseAgentOrIndividual(user.isAgent)
              stubPost(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=$taxYear", NO_CONTENT, "{}")
              urlPost(url, body = form, follow = false, welsh = user.isWelsh, headers = playSessionCookie(user.isAgent))
            }

            "the status is SEE OTHER" in {
              result.status shouldBe SEE_OTHER
            }

            "redirects to the overview page and submits to des" in {
              result.headers("Location").head shouldBe overviewUrl
            }
          }

          "the user makes no changes and does not submit downstream to des" which {

            val form = Map[String, String]()

            lazy val result: WSResponse = {

              wireMockServer.resetAll()
              dropGiftAidDB()
              insertCyaData(Some(cyaDataMax))
              userDataStub(IncomeSourcesModel(None, None, Some(unchangedPriorData)), nino, taxYear)
              authoriseAgentOrIndividual(user.isAgent)
              stubPost(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=$taxYear", NO_CONTENT, "{}")
              urlPost(url, body = form, follow = false, welsh = user.isWelsh, headers = playSessionCookie(user.isAgent))
            }

            "the status is SEE OTHER" in {
              result.status shouldBe SEE_OTHER
            }

            "redirects to the overview page and does not submit to des" in {
              result.headers("Location").head shouldBe overviewUrl
            }
          }

        }

        "redirect to an error page" when {
          "an error is returned from DES" which {

            val form = Map[String, String]()

            lazy val result: WSResponse = {
              wireMockServer.resetAll()

              dropGiftAidDB()
              insertCyaData(Some(cyaDataMax))
              userDataStub(IncomeSourcesModel(), nino, taxYear)

              authoriseAgentOrIndividual(user.isAgent)
              stubPut(
                s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
                BAD_REQUEST,
                Json.toJson(APIErrorBodyModel("BAD_REQUEST", "Oh hey look, literally any error.")).toString()
              )
              urlPost(url, body = form, welsh = user.isWelsh, headers = playSessionCookie(user.isAgent))
            }

            implicit def document: () => Document = () => Jsoup.parse(result.body)

            titleCheck(user.commonExpectedResults.error, user.isWelsh)
          }
        }
      }
    }
  }
}