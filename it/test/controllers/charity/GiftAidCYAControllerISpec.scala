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

package test.controllers.charity

import models.APIErrorBodyModel
import models.charity.prior.{GiftAidPaymentsModel, GiftAidSubmissionModel, GiftsModel}
import models.charity.{CharityNameModel, GiftAidCYAModel}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT, OK, SEE_OTHER}
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout, route}
import test.utils.CharityITHelper

import scala.concurrent.Future

class GiftAidCYAControllerISpec extends CharityITHelper {

  val amount: String = "£100"

  val url: String = s"$appUrl/$taxYear/charity/check-donations-to-charity"

  val relativeUrl: String = s"/update-and-submit-income-tax-return/personal-income/$taxYear/charity/check-donations-to-charity"

  val cyaDataMax: GiftAidCYAModel = GiftAidCYAModel(
    Some(true),
    Some(true), Some(100.00),
    Some(true), Some(100.00),
    Some(true), Some(100.00), Seq(CharityNameModel("Belgium Trust"), CharityNameModel("American Trust")),
    Some(true), Some(100.00),
    Some(true), Some(100.00),
    Some(true), Some(100.00), Some(true), Some(100.00),
    Some(true), Some(100.00), Seq(CharityNameModel("Belgium Trust"), CharityNameModel("American Trust"))
  )

  val cyaDataMin: GiftAidCYAModel = GiftAidCYAModel(
    gateway = Some(false),
    donationsViaGiftAid =  Some(false),
    overseasCharityNames = Seq.empty,
    addDonationToThisYear = Some(false),
    donatedSharesOrSecurities = Some(false),
    donatedLandOrProperty = Some(false),
    overseasDonatedSharesSecuritiesLandOrPropertyCharityNames= Seq.empty
  )

  val cyaDataIncomplete: GiftAidCYAModel = GiftAidCYAModel(
    Some(true), Some(false), None
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
    val madeDonationsToCharityHidden: String
    val donationViaGiftAidHidden: String
    val oneOffDonationHidden: String
    val overseasDonationHidden: String
    val lastYearHidden: String
    val lastYearAmountHidden: String
    val thisYearAmountHidden: String
    val sharesSecuritiesHidden: String
    val landPropertyHidden: String
    val landPropertyAmountHidden: String
    val overseasSharesSecuritiesLandPropertyHidden: String
    val overseasSharesSecurityLandPropertyNamesHidden: String
  }

  trait CommonExpectedResults {
    val yes: String
    val no: String
    val caption: String
    val madeDonationsToCharity: String
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
    val donationViaGiftAidAmountHidden: String
    val oneOffDonationAmountHidden: String
    val overseasDonationAmountHidden: String
    val overseasDonationNamesHidden: String
    val thisYearHidden: String
    val sharesSecuritiesAmountHidden: String
    val overseasSharesSecuritiesLandPropertyAmountHidden: String
  }

  object CommonExpectedEN extends CommonExpectedResults {
    val yes: String = "Yes"
    val no: String = "No"
    val caption = s"Donations to charity for 6 April $taxYearEOY to 5 April $taxYear"
    val madeDonationsToCharity = "Made donations to charity"
    val donationViaGiftAid = "Donation to charity using Gift Aid"
    val donationViaGiftAidAmount = "Amount donated to charity using Gift Aid"
    val oneOffDonation = "One-off donations to charity using Gift Aid"
    val oneOffDonationAmount = "Amount of one-off donations to charity using Gift Aid"
    val overseasDonation = "Donations to overseas charities using Gift Aid"
    val overseasDonationAmount = "Amount donated to overseas charities using Gift Aid"
    val overseasDonationNamesValue = "Belgium Trust American Trust"
    val lastYear = "Adding donations to last tax year"
    val lastYearAmount = "Amount added to last tax year"
    val thisYear = s"Donation after 5 April $taxYear added to this tax year"
    val thisYearAmount = s"Amount of donation after 5 April $taxYear added to this tax year"
    val sharesSecurities = "Donation of shares or securities"
    val sharesSecuritiesAmount = "Value of shares or securities"
    val landProperty = "Donation of land or property"
    val landPropertyAmount = "Value of land or property"
    val overseasSharesSecuritiesLandProperty = "Donation of shares, securities, land or property to overseas charities"
    val overseasSharesSecuritiesLandPropertyAmount = "Value of shares, securities, land or property to overseas charities"
    val overseasSharesSecuritiesLandPropertyNamesValue = "Belgium Trust American Trust"
    val donationViaGiftAidAmountHidden = "Change the amount donated to charity by using Gift Aid"
    val oneOffDonationAmountHidden = "Change the amount donated to charities as one-off payments"
    val overseasDonationAmountHidden = "Change the amount donated to overseas charities"
    val overseasDonationNamesHidden = "Change the names of overseas charities you used Gift Aid to donate to"
    val thisYearHidden = s"Change if you want to add donations made after 5 April $taxYear to this tax year"
    val sharesSecuritiesAmountHidden = "Change the value of the shares or securities donated to charity"
    val overseasSharesSecuritiesLandPropertyAmountHidden = "Change the value of shares, securities, land or properties donated to overseas charities"
    val priorDonationNames = "Jello Corporation"
    val priorSharesSecuritiesLandPropertyNames = "Simbas College Fund"
    val saveAndContinue = "Save and continue"
    val error = "Sorry, there is a problem with the service"
  }

  object CommonExpectedCY extends CommonExpectedResults {
    val yes: String = "Iawn"
    val no: String = "Na"
    val caption = s"Rhoddion i elusennau ar gyfer 6 Ebrill $taxYearEOY i 5 Ebrill $taxYear"
    val madeDonationsToCharity = "Gwnaeth gyfraniadau at elusennau"
    val donationViaGiftAid = "Rhoddion i elusen drwy ddefnyddio Rhodd Cymorth"
    val donationViaGiftAidAmount = "Swm a roddwyd i elusen drwy ddefnyddio Rhodd Cymorth"
    val oneOffDonation = "Rhoddion untro i elusen drwy ddefnyddio Rhodd Cymorth"
    val oneOffDonationAmount = "Swm y rhoddion untro i elusen drwy ddefnyddio Rhodd Cymorth"
    val overseasDonation = "Rhoddion i elusennau tramor drwy ddefnyddio Rhodd Cymorth"
    val overseasDonationAmount = "Swm y rhoddion i elusennau tramor drwy ddefnyddio Rhodd Cymorth"
    val overseasDonationNamesValue = "Belgium Trust American Trust"
    val lastYear = "Ychwanegu rhoddion at y flwyddyn dreth ddiwethaf"
    val lastYearAmount = "Y swm a ychwanegwyd at y flwyddyn dreth ddiwethaf"
    val thisYear = s"Ychwanegwyd rhodd ar ôl 5 Ebrill $taxYear at y flwyddyn dreth hon"
    val thisYearAmount = s"Swm y rhodd ar ôl 5 Ebrill $taxYear wedi’i ychwanegu at y flwyddyn dreth hon"
    val sharesSecurities = "Rhoi cyfranddaliadau neu warantau"
    val sharesSecuritiesAmount = "Swm y cyfranddaliadau neu warantau"
    val landProperty = "Rhoi tir neu eiddo"
    val landPropertyAmount = "Swm y tir neu eiddo"
    val overseasSharesSecuritiesLandProperty = "Rhoi cyfranddaliadau, gwarantau, tir neu eiddo i elusennau tramor"
    val overseasSharesSecuritiesLandPropertyAmount = "Swm y cyfranddaliadau, gwarantau, tir neu eiddo i elusennau tramor"
    val overseasSharesSecuritiesLandPropertyNamesValue = "Belgium Trust American Trust"
    val donationViaGiftAidAmountHidden = "Newidiwch y swm a roddir i elusen drwy ddefnyddio Rhodd Cymorth"
    val oneOffDonationAmountHidden = "Newidiwch y swm a roddir i elusennau fel taliadau untro"
    val overseasDonationAmountHidden = "Newidiwch y swm a roddir i elusennau tramor"
    val overseasDonationNamesHidden = "Newidiwch enwau elusennau tramor y gwnaethoch ddefnyddio Rhodd Cymorth i roi rhodd iddynt"
    val thisYearHidden = s"Newidiwch os ydych am ychwanegu rhoddion a wnaed ar ôl 5 Ebrill $taxYear at y flwyddyn dreth hon"
    val sharesSecuritiesAmountHidden = "Newidiwch werth y cyfranddaliadau neu’r gwarantau a roddir i elusen"
    val overseasSharesSecuritiesLandPropertyAmountHidden = "Newidiwch werth cyfranddaliadau, gwarantau, tir neu eiddo a roddir i elusennau tramor"
    val priorDonationNames = "Jello Corporation"
    val priorSharesSecuritiesLandPropertyNames = "Simbas College Fund"
    val saveAndContinue = "Cadw ac yn eich blaen"
    val error = "Mae’n ddrwg gennym – mae problem gyda’r gwasanaeth"
  }

  object ExpectedIndividualEN extends SpecificExpectedResults {
    val title = "Check your donations to charity"
    val overseasDonationNames = "Overseas charities you donated to"
    val overseasSharesSecurityLandPropertyNames = "Overseas charities you donated shares, securities, land or property to"
    val madeDonationsToCharityHidden = "Change donations to charity"
    val donationViaGiftAidHidden = "Change if you made a donation to charity by using Gift Aid"
    val oneOffDonationHidden = "Change if you made one-off donations to charity"
    val overseasDonationHidden = "Change if you made a donation to an overseas charity by using Gift Aid"
    val lastYearHidden = "Change if you want to add some of your donations to the last tax year"
    val lastYearAmountHidden = "Change the amount of your donations you want to add to the last tax year"
    val thisYearAmountHidden = s"Change the amount of your donations made after 5 April $taxYear you want to add to this tax year"
    val sharesSecuritiesHidden = "Change if you donated shares or securities to charity"
    val landPropertyHidden = "Change if you donated land or property to charity"
    val landPropertyAmountHidden = "Change the value of the land or property you donated"
    val overseasSharesSecuritiesLandPropertyHidden = "Change if you donated shares, securities, land or property to overseas charities"
    val overseasSharesSecurityLandPropertyNamesHidden = "Change the names of overseas charities you donated shares, securities, land or property to"
  }

  object ExpectedAgentEN extends SpecificExpectedResults {
    val title = "Check your client’s donations to charity"
    val overseasDonationNames = "Overseas charities your client donated to"
    val overseasSharesSecurityLandPropertyNames = "Overseas charities your client donated shares, securities, land or property to"
    val madeDonationsToCharityHidden = "Change donations to charity"
    val donationViaGiftAidHidden = "Change if your client made a donation to charity by using Gift Aid"
    val oneOffDonationHidden = "Change if your client made one-off donations to charity"
    val overseasDonationHidden = "Change if your client made a donation to an overseas charity by using Gift Aid"
    val lastYearHidden = "Change if you want to add some of your client’s donations to the last tax year"
    val lastYearAmountHidden = "Change the amount of your client’s donations you want to add to the last tax year"
    val thisYearAmountHidden = s"Change the amount of your client’s donations made after 5 April $taxYear you want to add to this tax year"
    val sharesSecuritiesHidden = "Change if your client donated shares or securities to charity"
    val landPropertyHidden = "Change if your client donated land or property to charity"
    val landPropertyAmountHidden = "Change the value of the land or property your client donated"
    val overseasSharesSecuritiesLandPropertyHidden = "Change if your client donated shares, securities, land or property to overseas charities"
    val overseasSharesSecurityLandPropertyNamesHidden = "Change the names of overseas charities your client donated shares, securities, land or property to"
  }

  object ExpectedIndividualCY extends SpecificExpectedResults {
    val title = "Gwiriwch eich rhoddion i elusen"
    val overseasDonationNames = "Elusennau tramor a roesoch rhodd iddynt"
    val overseasSharesSecurityLandPropertyNames = "Elusen dramor y gwnaethoch roi cyfranddaliadau, gwarantau, tir neu eiddo iddi"
    val madeDonationsToCharityHidden = "Newid cyfraniadau at elusennau"
    val donationViaGiftAidHidden = "Newidiwch os gwnaethoch rhoi rodd i elusen drwy ddefnyddio Rhodd Cymorth"
    val oneOffDonationHidden = "Newidiwch os gwnaethoch rhoi roddion untro i elusen"
    val overseasDonationHidden = "Newidiwch os gwnaethoch rhoi rodd i elusen o dramor drwy ddefnyddio Rhodd Cymorth"
    val lastYearHidden = "Newidiwch os ydych am ychwanegu rhai o’ch rhoddion at y flwyddyn dreth ddiwethaf"
    val lastYearAmountHidden = "Newidiwch swm eich rhoddion rydych am eu hychwanegu at y flwyddyn dreth ddiwethaf"
    val thisYearAmountHidden = s"Newidiwch swm eich rhoddion a wnaed ar ôl 5 Ebrill $taxYear rydych am eu hychwanegu at y flwyddyn dreth hon"
    val sharesSecuritiesHidden = "Newidiwch os gwnaethoch roi cyfranddaliadau neu warantau i elusen"
    val landPropertyHidden = "Newidiwch os gwnaethoch roi tir neu eiddo i elusen"
    val landPropertyAmountHidden = "Newidiwch werth y tir neu’r eiddo a roddoch"
    val overseasSharesSecuritiesLandPropertyHidden = "Newidiwch os gwnaethoch roi cyfranddaliadau, gwarantau, tir neu eiddo i elusennau tramor"
    val overseasSharesSecurityLandPropertyNamesHidden = "Newidiwch enwau elusennau tramor y gwnaethoch roi cyfranddaliadau, gwarantau, tir neu eiddo iddynt"
  }

  object ExpectedAgentCY extends SpecificExpectedResults {
    val title = "Gwiriwch roddion eich cleient i elusen"
    val overseasDonationNames = "Elusennau tramor a roddodd eich cleient rhodd iddynt"
    val overseasSharesSecurityLandPropertyNames = "Elusen dramor y gwnaeth eich cleient roi cyfranddaliadau, gwarantau, tir neu eiddo iddi"
    val madeDonationsToCharityHidden = "Newid cyfraniadau at elusennau"
    val donationViaGiftAidHidden = "Newidiwch os gwnaeth eich cleient rhoi rhodd i elusen drwy ddefnyddio Rhodd Cymorth"
    val oneOffDonationHidden = "Newidiwch os gwnaeth eich cleient rhoi roddion untro i elusen"
    val overseasDonationHidden = "Newidiwch os gwnaeth eich cleient rhoi rhodd i elusen o dramor drwy ddefnyddio Rhodd Cymorth"
    val lastYearHidden = "Newidiwch os ydych am ychwanegu rhai o roddion eich cleient i’r flwyddyn dreth ddiwethaf"
    val lastYearAmountHidden = "Newidiwch swm rhoddion eich cleient rydych am eu hychwanegu at y flwyddyn dreth ddiwethaf"
    val thisYearAmountHidden = s"Newidiwch swm rhoddion eich cleient a wnaed ar ôl 5 Ebrill $taxYear rydych am eu hychwanegu at y flwyddyn dreth hon"
    val sharesSecuritiesHidden = "Newidiwch os gwnaeth eich cleient rhoi cyfranddaliadau neu warantau i elusen"
    val landPropertyHidden = "Newidiwch os gwnaeth eich cleient rhoi tir neu eiddo i elusen"
    val landPropertyAmountHidden = "Newidiwch werth y tir neu’r eiddo a roddodd eich cleient"
    val overseasSharesSecuritiesLandPropertyHidden = "Newidiwch os yw’ch cleient yn rhoi cyfranddaliadau, gwarantau, tir neu eiddo i elusennau tramor"
    val overseasSharesSecurityLandPropertyNamesHidden = "Newidiwch enwau elusennau tramor y rhoddodd eich cleient gyfranddaliadau, gwarantau, tir neu eiddo iddynt"
  }

  val userScenarios: Seq[UserScenario[CommonExpectedResults, SpecificExpectedResults]] = {
    Seq(UserScenario(isWelsh = false, isAgent = false, CommonExpectedEN, Some(ExpectedIndividualEN)),
      UserScenario(isWelsh = false, isAgent = true, CommonExpectedEN, Some(ExpectedAgentEN)),
      UserScenario(isWelsh = true, isAgent = false, CommonExpectedCY, Some(ExpectedIndividualCY)),
      UserScenario(isWelsh = true, isAgent = true, CommonExpectedCY, Some(ExpectedAgentCY)))
  }

  def getResultAsFuture(pageUrl: String,
                         cyaData: Option[GiftAidCYAModel],
                         priorData: Option[IncomeSourcesModel],
                         isAgent: Boolean = false,
                         welsh: Boolean = false): Future[Result] = {

    wireMockServer.resetAll()

    if(priorData.isDefined) userDataStub(priorData.get, nino, taxYear) else emptyUserDataStub()

    dropGiftAidDB()
    if (cyaData.isDefined) insertGiftAidCyaData(cyaData)

    authoriseAgentOrIndividual(isAgent)

    val headers = playSessionCookie(isAgent) ++ (if (welsh) Seq(HeaderNames.ACCEPT_LANGUAGE -> "cy") else Seq())
    val request = FakeRequest("GET", pageUrl).withHeaders(headers: _*)

    route(appWithTailoring, request, "{}").get
  }

  def cyaRowCheck(expectedText: String, expectedValue: String, changeLinkHref: String, changeLinkHiddenText: String, rowNumber: Int)(implicit document: () => Document): Unit = {
    val keySelector = s"#main-content > div > div > dl > div:nth-child($rowNumber) > dt"
    val valueSelector = s"#main-content > div > div > dl > div:nth-child($rowNumber) > dd.govuk-summary-list__value"
    val changeLinkSelector = s"#main-content > div > div > dl > div:nth-child($rowNumber) > dd.govuk-summary-list__actions > a"
    val cyaHiddenChangeLink = s"#main-content > div > div > dl > div:nth-child($rowNumber) > dd.govuk-summary-list__actions > a > span.govuk-visually-hidden"

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

      s"the change link should have hidden text '$changeLinkHiddenText''" in {
        document().select(cyaHiddenChangeLink).text() shouldBe changeLinkHiddenText
      }

    }
  }

  ".show" when {

    userScenarios.foreach { user =>
      s"language is ${welshTest(user.isWelsh)} and request is from an ${agentTest(user.isAgent)}" should {

        "render the page with full CYA model" which {

          lazy val result = getResultAsFuture(relativeUrl, Some(cyaDataMax), None, user.isAgent, user.isWelsh)

          implicit def document: () => Document = () => Jsoup.parse(contentAsString(result))

          import user.commonExpectedResults._

          "has an OK (200) status" in {
            status(result) shouldBe OK
          }

          titleCheck(user.specificExpectedResults.get.title, user.isWelsh)
          h1Check(user.specificExpectedResults.get.title + " " + caption)
          captionCheck(caption)

          //noinspection ScalaStyle
          {
            cyaRowCheck(madeDonationsToCharity, yes, controllers.charity.routes.GiftAidGatewayController.show(taxYear).url, user.specificExpectedResults.get.madeDonationsToCharityHidden, 1)

            cyaRowCheck(donationViaGiftAid, yes, controllers.charity.routes.GiftAidDonationsController.show(taxYear).url, user.specificExpectedResults.get.donationViaGiftAidHidden, 2)
            cyaRowCheck(donationViaGiftAidAmount, amount, controllers.charity.routes.GiftAidDonatedAmountController.show(taxYear).url, donationViaGiftAidAmountHidden, 3)

            cyaRowCheck(oneOffDonation, yes, controllers.charity.routes.GiftAidOneOffController.show(taxYear).url,user.specificExpectedResults.get.oneOffDonationHidden, 4)
            cyaRowCheck(oneOffDonationAmount, amount, controllers.charity.routes.GiftAidOneOffAmountController.show(taxYear).url, oneOffDonationAmountHidden, 5)

            cyaRowCheck(overseasDonation, yes, controllers.charity.routes.OverseasGiftAidDonationsController.show(taxYear).url, user.specificExpectedResults.get.overseasDonationHidden, 6)
            cyaRowCheck(overseasDonationAmount, amount, controllers.charity.routes.GiftAidOverseasAmountController.show(taxYear).url, overseasDonationAmountHidden, 7)
            cyaRowCheck(user.specificExpectedResults.get.overseasDonationNames, overseasDonationNamesValue, controllers.charity.routes.OverseasGiftAidSummaryController.show(taxYear).url, overseasDonationNamesHidden, 8)

            cyaRowCheck(lastYear, yes, controllers.charity.routes.GiftAidLastTaxYearController.show(taxYear).url, user.specificExpectedResults.get.lastYearHidden, 9)
            cyaRowCheck(lastYearAmount, amount, controllers.charity.routes.GiftAidLastTaxYearAmountController.show(taxYear).url, user.specificExpectedResults.get.lastYearAmountHidden, 10)

            cyaRowCheck(thisYear, yes, controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear).url, thisYearHidden, 11)
            cyaRowCheck(thisYearAmount, amount, controllers.charity.routes.GiftAidAppendNextYearTaxAmountController.show(taxYear, taxYear).url, user.specificExpectedResults.get.thisYearAmountHidden, 12)

            cyaRowCheck(sharesSecurities, yes, controllers.charity.routes.GiftAidQualifyingSharesSecuritiesController.show(taxYear).url, user.specificExpectedResults.get.sharesSecuritiesHidden, 13)
            cyaRowCheck(sharesSecuritiesAmount, amount, controllers.charity.routes.GiftAidTotalShareSecurityAmountController.show(taxYear).url, sharesSecuritiesAmountHidden,14)
            cyaRowCheck(landProperty, yes, controllers.charity.routes.GiftAidDonateLandOrPropertyController.show(taxYear).url, user.specificExpectedResults.get.landPropertyHidden, 15)
            cyaRowCheck(landPropertyAmount, amount, controllers.charity.routes.GiftAidLandOrPropertyAmountController.show(taxYear).url, user.specificExpectedResults.get.landPropertyAmountHidden, 16)

            cyaRowCheck(overseasSharesSecuritiesLandProperty, yes, controllers.charity.routes.GiftAidSharesSecuritiesLandPropertyOverseasController.show(taxYear).url, user.specificExpectedResults.get.overseasSharesSecuritiesLandPropertyHidden, 17)
            cyaRowCheck(overseasSharesSecuritiesLandPropertyAmount, amount, controllers.charity.routes.OverseasSharesSecuritiesLandPropertyAmountController.show(taxYear).url, overseasSharesSecuritiesLandPropertyAmountHidden, 18)
            cyaRowCheck(user.specificExpectedResults.get.overseasSharesSecurityLandPropertyNames, overseasSharesSecuritiesLandPropertyNamesValue, controllers.charity.routes.OverseasSharesLandSummaryController.show(taxYear).url, user.specificExpectedResults.get.overseasSharesSecurityLandPropertyNamesHidden, 19)
          }

          buttonCheck(saveAndContinue)
          welshToggleCheck(user.isWelsh)
        }

        "return an almost empty CYA view" which {

          "has only the donated land or property yes/no hidden" when {

            "land or properties is the only value" which {

              val priorData = IncomeSourcesModel(giftAid = Some(GiftAidSubmissionModel(None,
                Some(GiftsModel(
                  landAndBuildings = Some(1000.74)
                ))
              )))

              lazy val result = getResultAsFuture(relativeUrl, None, Some(priorData), user.isAgent, user.isWelsh)

              implicit def document: () => Document = () => Jsoup.parse(contentAsString(result))

              import user.commonExpectedResults._

              "has an OK (200) status" in {
                status(result) shouldBe OK
              }

              titleCheck(user.specificExpectedResults.get.title, user.isWelsh)
              h1Check(user.specificExpectedResults.get.title + " " + caption)
              captionCheck(caption)

              //noinspection ScalaStyle
              {
                cyaRowCheck(madeDonationsToCharity, user.commonExpectedResults.yes, controllers.charity.routes.GiftAidGatewayController.show(taxYear).url, user.specificExpectedResults.get.madeDonationsToCharityHidden, 1)
                cyaRowCheck(donationViaGiftAid, user.commonExpectedResults.no, controllers.charity.routes.GiftAidDonationsController.show(taxYear).url, user.specificExpectedResults.get.donationViaGiftAidHidden, 2)
                cyaRowCheck(thisYear, user.commonExpectedResults.no, controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear).url, thisYearHidden, 3)
                cyaRowCheck(sharesSecurities, user.commonExpectedResults.no, controllers.charity.routes.GiftAidQualifyingSharesSecuritiesController.show(taxYear).url, user.specificExpectedResults.get.sharesSecuritiesHidden, 4)
                cyaRowCheck(landPropertyAmount, "£1,000.74", controllers.charity.routes.GiftAidLandOrPropertyAmountController.show(taxYear).url, user.specificExpectedResults.get.landPropertyAmountHidden, 5)
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

              lazy val result = getResultAsFuture(relativeUrl, None, Some(priorData), user.isAgent, user.isWelsh)

              implicit def document: () => Document = () => Jsoup.parse(contentAsString(result))

              import user.commonExpectedResults._

              "has an OK (200) status" in {
                status(result) shouldBe OK
              }

              titleCheck(user.specificExpectedResults.get.title, user.isWelsh)
              h1Check(user.specificExpectedResults.get.title + " " + caption)
              captionCheck(caption)

              //noinspection ScalaStyle
              {
                cyaRowCheck(madeDonationsToCharity, user.commonExpectedResults.yes, controllers.charity.routes.GiftAidGatewayController.show(taxYear).url, user.specificExpectedResults.get.madeDonationsToCharityHidden, 1)
                cyaRowCheck(donationViaGiftAid, user.commonExpectedResults.no, controllers.charity.routes.GiftAidDonationsController.show(taxYear).url, user.specificExpectedResults.get.donationViaGiftAidHidden, 2)
                cyaRowCheck(thisYear, user.commonExpectedResults.no, controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear).url, thisYearHidden, 3)
                cyaRowCheck(sharesSecuritiesAmount, "£1,000.74", controllers.charity.routes.GiftAidTotalShareSecurityAmountController.show(taxYear).url,sharesSecuritiesAmountHidden, 4)
                cyaRowCheck(landProperty, user.commonExpectedResults.no, controllers.charity.routes.GiftAidDonateLandOrPropertyController.show(taxYear).url, user.specificExpectedResults.get.landPropertyHidden, 5)
                cyaRowCheck(overseasSharesSecuritiesLandProperty, user.commonExpectedResults.no, controllers.charity.routes.GiftAidSharesSecuritiesLandPropertyOverseasController.show(taxYear).url, user.specificExpectedResults.get.overseasSharesSecuritiesLandPropertyHidden, 6)
              }

              buttonCheck(saveAndContinue)
              welshToggleCheck(user.isWelsh)
            }

          }

          "has only the shares or securities yes/no hidden" when {

            "shares or securities is the only value" which {

              val priorData = IncomeSourcesModel(giftAid = Some(GiftAidSubmissionModel(None,
                Some(GiftsModel(
                  sharesOrSecurities = Some(740.10)
                ))
              )))

              lazy val result = getResultAsFuture(relativeUrl, None, Some(priorData), user.isAgent, user.isWelsh)

              implicit def document: () => Document = () => Jsoup.parse(contentAsString(result))

              import user.commonExpectedResults._

              "has an OK (200) status" in {
                status(result) shouldBe OK
              }

              titleCheck(user.specificExpectedResults.get.title, user.isWelsh)
              h1Check(user.specificExpectedResults.get.title + " " + caption)
              captionCheck(caption)

              //noinspection ScalaStyle
              {
                cyaRowCheck(madeDonationsToCharity, user.commonExpectedResults.yes, controllers.charity.routes.GiftAidGatewayController.show(taxYear).url, user.specificExpectedResults.get.madeDonationsToCharityHidden, 1)
                cyaRowCheck(donationViaGiftAid, user.commonExpectedResults.no, controllers.charity.routes.GiftAidDonationsController.show(taxYear).url, user.specificExpectedResults.get.donationViaGiftAidHidden, 2)
                cyaRowCheck(thisYear, user.commonExpectedResults.no, controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear).url, thisYearHidden, 3)
                cyaRowCheck(sharesSecuritiesAmount, "£740.10", controllers.charity.routes.GiftAidTotalShareSecurityAmountController.show(taxYear).url, sharesSecuritiesAmountHidden, 4)
                cyaRowCheck(landProperty, user.commonExpectedResults.no, controllers.charity.routes.GiftAidDonateLandOrPropertyController.show(taxYear).url, user.specificExpectedResults.get.landPropertyHidden, 5)
              }

              buttonCheck(saveAndContinue)
              welshToggleCheck(user.isWelsh)
            }

            "land or property is the only value" which {

              val priorData = IncomeSourcesModel(giftAid = Some(GiftAidSubmissionModel(None,
                Some(GiftsModel(
                  landAndBuildings = Some(740.10)
                ))
              )))

              lazy val result = getResultAsFuture(relativeUrl, None, Some(priorData), user.isAgent, user.isWelsh)

              implicit def document: () => Document = () => Jsoup.parse(contentAsString(result))

              import user.commonExpectedResults._

              "has an OK (200) status" in {
                status(result) shouldBe OK
              }

              titleCheck(user.specificExpectedResults.get.title, user.isWelsh)
              h1Check(user.specificExpectedResults.get.title + " " + caption)
              captionCheck(caption)

              //noinspection ScalaStyle
              {
                cyaRowCheck(madeDonationsToCharity, user.commonExpectedResults.yes, controllers.charity.routes.GiftAidGatewayController.show(taxYear).url, user.specificExpectedResults.get.madeDonationsToCharityHidden, 1)
                cyaRowCheck(donationViaGiftAid, user.commonExpectedResults.no, controllers.charity.routes.GiftAidDonationsController.show(taxYear).url, user.specificExpectedResults.get.donationViaGiftAidHidden, 2)
                cyaRowCheck(thisYear, user.commonExpectedResults.no, controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear).url, thisYearHidden, 3)
                cyaRowCheck(sharesSecurities, user.commonExpectedResults.no, controllers.charity.routes.GiftAidQualifyingSharesSecuritiesController.show(taxYear).url, user.specificExpectedResults.get.sharesSecuritiesHidden, 4)
                cyaRowCheck(landPropertyAmount, "£740.10", controllers.charity.routes.GiftAidLandOrPropertyAmountController.show(taxYear).url, user.specificExpectedResults.get.landPropertyAmountHidden, 5)
                cyaRowCheck(overseasSharesSecuritiesLandProperty, user.commonExpectedResults.no, controllers.charity.routes.GiftAidSharesSecuritiesLandPropertyOverseasController.show(taxYear).url, user.specificExpectedResults.get.overseasSharesSecuritiesLandPropertyHidden, 6)
              }

              buttonCheck(saveAndContinue)
              welshToggleCheck(user.isWelsh)
            }

          }

        }

        "return a cya page with all the yes/no questions hidden" when {

          "there is no CYA model, but there is a full prior data model" which {

            lazy val result = getResultAsFuture(relativeUrl, None, Some(IncomeSourcesModel(giftAid = Some(priorDataMax))), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(contentAsString(result))

            import user.commonExpectedResults._

            "has an OK (200) status" in {
              status(result) shouldBe OK
            }

            titleCheck(user.specificExpectedResults.get.title, user.isWelsh)
            h1Check(user.specificExpectedResults.get.title + " " + caption)
            captionCheck(caption)

            //noinspection ScalaStyle
            {
              cyaRowCheck(madeDonationsToCharity, user.commonExpectedResults.yes, controllers.charity.routes.GiftAidGatewayController.show(taxYear).url, user.specificExpectedResults.get.madeDonationsToCharityHidden, 1)
              cyaRowCheck(donationViaGiftAidAmount, amount, controllers.charity.routes.GiftAidDonatedAmountController.show(taxYear).url, donationViaGiftAidAmountHidden, 2)
              cyaRowCheck(oneOffDonationAmount, amount, controllers.charity.routes.GiftAidOneOffAmountController.show(taxYear).url, oneOffDonationAmountHidden, 3)
              cyaRowCheck(overseasDonationAmount, amount, controllers.charity.routes.GiftAidOverseasAmountController.show(taxYear).url, overseasDonationAmountHidden, 4)
              cyaRowCheck(user.specificExpectedResults.get.overseasDonationNames, priorDonationNames, controllers.charity.routes.OverseasGiftAidSummaryController.show(taxYear).url, overseasDonationNamesHidden, 5)
              cyaRowCheck(lastYearAmount, amount, controllers.charity.routes.GiftAidLastTaxYearAmountController.show(taxYear).url, user.specificExpectedResults.get.lastYearAmountHidden, 6)
              cyaRowCheck(thisYearAmount, amount, controllers.charity.routes.GiftAidAppendNextYearTaxAmountController.show(taxYear, taxYear).url, user.specificExpectedResults.get.thisYearAmountHidden, 7)
              cyaRowCheck(sharesSecuritiesAmount, amount, controllers.charity.routes.GiftAidTotalShareSecurityAmountController.show(taxYear).url, sharesSecuritiesAmountHidden, 8)
              cyaRowCheck(landPropertyAmount, amount, controllers.charity.routes.GiftAidLandOrPropertyAmountController.show(taxYear).url, user.specificExpectedResults.get.landPropertyAmountHidden, 9)
              cyaRowCheck(overseasSharesSecuritiesLandPropertyAmount, amount, controllers.charity.routes.OverseasSharesSecuritiesLandPropertyAmountController.show(taxYear).url, overseasSharesSecuritiesLandPropertyAmountHidden,10)
              cyaRowCheck(user.specificExpectedResults.get.overseasSharesSecurityLandPropertyNames, priorSharesSecuritiesLandPropertyNames, controllers.charity.routes.OverseasSharesLandSummaryController.show(taxYear).url, user.specificExpectedResults.get.overseasSharesSecurityLandPropertyNamesHidden, 11)
            }

            buttonCheck(saveAndContinue)
            welshToggleCheck(user.isWelsh)
          }

          "there is a full CYA model, and there is a full prior data model" which {

            lazy val result =
              getResultAsFuture(relativeUrl, Some(cyaDataMax), Some(IncomeSourcesModel(giftAid = Some(priorDataMax))), user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(contentAsString(result))

            import user.commonExpectedResults._

            "has an OK (200) status" in {
              status(result) shouldBe OK
            }

            titleCheck(user.specificExpectedResults.get.title, user.isWelsh)
            h1Check(user.specificExpectedResults.get.title + " " + caption)
            captionCheck(caption)

            //noinspection ScalaStyle
            {
              cyaRowCheck(madeDonationsToCharity, user.commonExpectedResults.yes, controllers.charity.routes.GiftAidGatewayController.show(taxYear).url, user.specificExpectedResults.get.madeDonationsToCharityHidden, 1)
              cyaRowCheck(donationViaGiftAidAmount, amount, controllers.charity.routes.GiftAidDonatedAmountController.show(taxYear).url, donationViaGiftAidAmountHidden, 2)
              cyaRowCheck(oneOffDonationAmount, amount, controllers.charity.routes.GiftAidOneOffAmountController.show(taxYear).url, oneOffDonationAmountHidden, 3)
              cyaRowCheck(overseasDonationAmount, amount, controllers.charity.routes.GiftAidOverseasAmountController.show(taxYear).url, overseasDonationAmountHidden, 4)
              cyaRowCheck(user.specificExpectedResults.get.overseasDonationNames, overseasDonationNamesValue, controllers.charity.routes.OverseasGiftAidSummaryController.show(taxYear).url, overseasDonationNamesHidden, 5)
              cyaRowCheck(lastYearAmount, amount, controllers.charity.routes.GiftAidLastTaxYearAmountController.show(taxYear).url, user.specificExpectedResults.get.lastYearAmountHidden, 6)
              cyaRowCheck(thisYearAmount, amount, controllers.charity.routes.GiftAidAppendNextYearTaxAmountController.show(taxYear, taxYear).url, user.specificExpectedResults.get.thisYearAmountHidden, 7)
              cyaRowCheck(sharesSecuritiesAmount, amount, controllers.charity.routes.GiftAidTotalShareSecurityAmountController.show(taxYear).url, sharesSecuritiesAmountHidden, 8)
              cyaRowCheck(landPropertyAmount, amount, controllers.charity.routes.GiftAidLandOrPropertyAmountController.show(taxYear).url, user.specificExpectedResults.get.landPropertyAmountHidden, 9)
              cyaRowCheck(overseasSharesSecuritiesLandPropertyAmount, amount, controllers.charity.routes.OverseasSharesSecuritiesLandPropertyAmountController.show(taxYear).url, overseasSharesSecuritiesLandPropertyAmountHidden, 10)
              cyaRowCheck(user.specificExpectedResults.get.overseasSharesSecurityLandPropertyNames, overseasSharesSecuritiesLandPropertyNamesValue, controllers.charity.routes.OverseasSharesLandSummaryController.show(taxYear).url, user.specificExpectedResults.get.overseasSharesSecurityLandPropertyNamesHidden, 11)
            }

            buttonCheck(saveAndContinue)
            welshToggleCheck(user.isWelsh)
          }

        }

        "return a minimal CYA view" when {

          "the CYA model contains all false values not gateway" which {

            lazy val result = getResultAsFuture(relativeUrl, Some(cyaDataMin.copy(gateway = Some(true))), None, user.isAgent, user.isWelsh)

            implicit def document: () => Document = () => Jsoup.parse(contentAsString(result))

            import user.commonExpectedResults._

            "has an OK (200) status" in {
              status(result) shouldBe OK
            }

            titleCheck(user.specificExpectedResults.get.title, user.isWelsh)
            h1Check(user.specificExpectedResults.get.title + " " + caption)
            captionCheck(caption)

            //noinspection ScalaStyle
            {
              cyaRowCheck(madeDonationsToCharity, user.commonExpectedResults.yes, controllers.charity.routes.GiftAidGatewayController.show(taxYear).url, user.specificExpectedResults.get.madeDonationsToCharityHidden, 1)
              cyaRowCheck(donationViaGiftAid, user.commonExpectedResults.no, controllers.charity.routes.GiftAidDonationsController.show(taxYear).url, user.specificExpectedResults.get.donationViaGiftAidHidden, 2)
              cyaRowCheck(thisYear, user.commonExpectedResults.no, controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear).url, thisYearHidden, 3)
              cyaRowCheck(sharesSecurities, user.commonExpectedResults.no, controllers.charity.routes.GiftAidQualifyingSharesSecuritiesController.show(taxYear).url, user.specificExpectedResults.get.sharesSecuritiesHidden, 4)
              cyaRowCheck(landProperty, user.commonExpectedResults.no, controllers.charity.routes.GiftAidDonateLandOrPropertyController.show(taxYear).url, user.specificExpectedResults.get.landPropertyHidden, 5)
            }

            buttonCheck(saveAndContinue)
            welshToggleCheck(user.isWelsh)

          }

        }

        "redirect to Gift Aid OneOff amount page when it is unfinished" which {

          lazy val result: WSResponse = {
            dropGiftAidDB()
            emptyUserDataStub(nino, taxYear)
            insertGiftAidCyaData(Some(cyaDataMax.copy(oneOffDonationsViaGiftAidAmount  = None)))

            authoriseAgentOrIndividual(user.isAgent)
            urlGet(url, welsh = user.isWelsh, headers = playSessionCookie(user.isAgent))
          }

          "redirects to the correct url" in {
            result
            verifyGet(s"/update-and-submit-income-tax-return/personal-income/$taxYear/charity/amount-donated-as-one-off")
          }
        }

        "redirect to the overview page" when {

          "there is no CYA and no PRIOR data" which {
            lazy val result = {

              dropGiftAidDB()
              insertGiftAidCyaData(None)
              userDataStub(IncomeSourcesModel(), nino, taxYear)

              authoriseAgentOrIndividual(user.isAgent)
              urlGet(url, welsh = user.isWelsh, headers = playSessionCookie(user.isAgent))
            }

            "redirects to the correct url" in {
              result
              verifyGet(s"/update-and-submit-income-tax-return/$taxYear/view")
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
              insertGiftAidCyaData(Some(cyaDataMax))
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
              insertGiftAidCyaData(Some(cyaDataMax))
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
              insertGiftAidCyaData(Some(cyaDataMax))
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
