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
import helpers.PlaySessionCookieBaker
import models.APIErrorBodyModel
import models.charity.GiftAidCYAModel
import models.charity.prior.{GiftAidPaymentsModel, GiftAidSubmissionModel, GiftsModel}
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.libs.ws.WSClient
import utils.{IntegrationTest, ViewHelpers}
import play.api.http.Status.{BAD_REQUEST, NO_CONTENT, OK}
import play.api.libs.json.Json

class GiftAidCYAControllerISpec extends IntegrationTest with ViewHelpers {

  val taxYear = 2022

  val amount: String = "£100"
  val yes: String = "Yes"
  val no: String = "No"

  val url: String = s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/check-donations-to-charity"

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]

  val cyaDataMax: GiftAidCYAModel = GiftAidCYAModel(
    Some(true), Some(100.00),
    Some(true), Some(100.00),
    Some(true), Some(100.00), Some(Seq("Belgium Trust", "American Trust")),
    Some(true), Some(100.00),
    Some(true), Some(100.00),
    Some(true), Some(true), Some(100.00), Some(true), Some(100.00),
    Some(true), Some(100.00), Some(Seq("Belgium Trust", "American Trust"))
  )

  val cyaDataMin: GiftAidCYAModel = GiftAidCYAModel(
    Some(false), None,
    Some(false), None,
    Some(false), None, None,
    Some(false), None,
    Some(false), None,
    Some(false), None, None, None, None,
    Some(false), None, None
  )

  val cyaDataIncomplete: GiftAidCYAModel = GiftAidCYAModel(
    Some(false), None
  )

  val priorDataMax: GiftAidSubmissionModel = GiftAidSubmissionModel(
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

  object ExpectedValuesEnglish {

    object Individual {
      val title = "Check your donations to charity"

      val overseasDonationNames = "Overseas charities you donated to"

      val overseasSharesSecurityLandPropertyNames = "Overseas charities you donated shares, securities, land or property to"
    }

    object Agent {
      val title = "Check your client’s donations to charity"

      val overseasDonationNames = "Overseas charities your client donated to"

      val overseasSharesSecurityLandPropertyNames = "Overseas charities your client donated shares, securities, land or property to"
    }

    object Common {
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
    }

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

  "GET in English" should {

    "as an individual" should {

      "return a full CYA view" when {

        "the CYA model is full" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.GIFT_AID_CYA -> cyaDataMax.asJsonString
          ))

          lazy val result = {
            authoriseIndividual()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies).get())
          }

          implicit lazy val document: () => Document = () => Jsoup.parse(result.body)

          "has an OK (200) status" in {
            result.status shouldBe OK
          }

          titleCheck(ExpectedValuesEnglish.Individual.title)
          h1Check(ExpectedValuesEnglish.Individual.title + " " + ExpectedValuesEnglish.Common.caption)
          captionCheck(ExpectedValuesEnglish.Common.caption)

          //noinspection ScalaStyle
          {
            cyaRowCheck(ExpectedValuesEnglish.Common.donationViaGiftAid, yes, "#", 1)
            cyaRowCheck(ExpectedValuesEnglish.Common.donationViaGiftAidAmount, amount, "#", 2)

            cyaRowCheck(ExpectedValuesEnglish.Common.oneOffDonation, yes, "#", 3)
            cyaRowCheck(ExpectedValuesEnglish.Common.oneOffDonationAmount, amount, "#", 4)

            cyaRowCheck(ExpectedValuesEnglish.Common.overseasDonation, yes, "#", 5)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasDonationAmount, amount, "#", 6)
            cyaRowCheck(ExpectedValuesEnglish.Individual.overseasDonationNames, ExpectedValuesEnglish.Common.overseasDonationNamesValue, "#", 7)

            cyaRowCheck(ExpectedValuesEnglish.Common.lastYear, yes, "#", 8)
            cyaRowCheck(ExpectedValuesEnglish.Common.lastYearAmount, amount, "#", 9)

            cyaRowCheck(ExpectedValuesEnglish.Common.thisYear, yes, "#", 10)
            cyaRowCheck(ExpectedValuesEnglish.Common.thisYearAmount, amount, "#", 11)

            cyaRowCheck(ExpectedValuesEnglish.Common.sharesSecuritiesLandProperty, yes, "#", 12)
            cyaRowCheck(ExpectedValuesEnglish.Common.sharesSecurities, yes, "#", 13)
            cyaRowCheck(ExpectedValuesEnglish.Common.sharesSecuritiesAmount, amount, "#", 14)
            cyaRowCheck(ExpectedValuesEnglish.Common.landProperty, yes, "#", 15)
            cyaRowCheck(ExpectedValuesEnglish.Common.landPropertyAmount, amount, "#", 16)

            cyaRowCheck(ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandProperty, yes, "#", 17)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandPropertyAmount, amount, "#", 18)
            cyaRowCheck(ExpectedValuesEnglish.Individual.overseasSharesSecurityLandPropertyNames, ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandPropertyNamesValue, "#", 19)
          }

          buttonCheck(ExpectedValuesEnglish.Common.saveAndContinue)

          welshToggleCheck(ENGLISH)

        }

      }

      "return an almost empty CYA view" which {

        "has only the donated land, shares, securities and properties yes/no hidden" when {

          "land or properties is the only value" which {
              lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
                SessionValues.GIFT_AID_PRIOR_SUB -> Json.toJson(GiftAidSubmissionModel(None,
                  Some(GiftsModel(
                    landAndBuildings = Some(1000.74)
                  ))
                )).toString()
              ))

              lazy val result = {
                authoriseIndividual()
                await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies).get())
              }

              implicit lazy val document: () => Document = () => Jsoup.parse(result.body)

              "has an OK (200) status" in {
                result.status shouldBe OK
              }

              titleCheck(ExpectedValuesEnglish.Individual.title)
              h1Check(ExpectedValuesEnglish.Individual.title + " " + ExpectedValuesEnglish.Common.caption)
              captionCheck(ExpectedValuesEnglish.Common.caption)

              //noinspection ScalaStyle
              {
                cyaRowCheck(ExpectedValuesEnglish.Common.donationViaGiftAid, no, "#", 1)
                cyaRowCheck(ExpectedValuesEnglish.Common.oneOffDonation, no, "#", 2)
                cyaRowCheck(ExpectedValuesEnglish.Common.overseasDonation, no, "#", 3)
                cyaRowCheck(ExpectedValuesEnglish.Common.lastYear, no, "#", 4)
                cyaRowCheck(ExpectedValuesEnglish.Common.thisYear, no, "#", 5)

                cyaRowCheck(ExpectedValuesEnglish.Common.sharesSecurities, no, "#", 6)
                cyaRowCheck(ExpectedValuesEnglish.Common.landPropertyAmount, "£1000.74", "#", 7)

                cyaRowCheck(ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandProperty, no, "#", 8)
              }

              buttonCheck(ExpectedValuesEnglish.Common.saveAndContinue)

              welshToggleCheck(ENGLISH)
          }

          "shares or securities is the only value" which {
              lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
                SessionValues.GIFT_AID_PRIOR_SUB -> Json.toJson(GiftAidSubmissionModel(None,
                  Some(GiftsModel(
                    sharesOrSecurities = Some(1000.74)
                  ))
                )).toString()
              ))

              lazy val result = {
                authoriseIndividual()
                await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies).get())
              }

              implicit lazy val document: () => Document = () => Jsoup.parse(result.body)

              "has an OK (200) status" in {
                result.status shouldBe OK
              }

              titleCheck(ExpectedValuesEnglish.Individual.title)
              h1Check(ExpectedValuesEnglish.Individual.title + " " + ExpectedValuesEnglish.Common.caption)
              captionCheck(ExpectedValuesEnglish.Common.caption)

              //noinspection ScalaStyle
              {
                cyaRowCheck(ExpectedValuesEnglish.Common.donationViaGiftAid, no, "#", 1)
                cyaRowCheck(ExpectedValuesEnglish.Common.oneOffDonation, no, "#", 2)
                cyaRowCheck(ExpectedValuesEnglish.Common.overseasDonation, no, "#", 3)
                cyaRowCheck(ExpectedValuesEnglish.Common.lastYear, no, "#", 4)
                cyaRowCheck(ExpectedValuesEnglish.Common.thisYear, no, "#", 5)

                cyaRowCheck(ExpectedValuesEnglish.Common.sharesSecuritiesAmount, "£1000.74", "#", 6)
                cyaRowCheck(ExpectedValuesEnglish.Common.landProperty, no, "#", 7)

                cyaRowCheck(ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandProperty, no, "#", 8)
              }

              buttonCheck(ExpectedValuesEnglish.Common.saveAndContinue)

              welshToggleCheck(ENGLISH)
          }

        }

      }

      "return a cya page with all the yes/no questions hidden" when {

        "there is no CYA model, but there is a full prior data model" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.GIFT_AID_PRIOR_SUB -> Json.prettyPrint(Json.toJson(priorDataMax))
          ))

          lazy val result = {
            authoriseIndividual()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies).get())
          }

          implicit lazy val document: () => Document = () => Jsoup.parse(result.body)

          "has an OK (200) status" in {
            result.status shouldBe OK
          }

          titleCheck(ExpectedValuesEnglish.Individual.title)
          h1Check(ExpectedValuesEnglish.Individual.title + " " + ExpectedValuesEnglish.Common.caption)
          captionCheck(ExpectedValuesEnglish.Common.caption)

          //noinspection ScalaStyle
          {
            cyaRowCheck(ExpectedValuesEnglish.Common.donationViaGiftAidAmount, amount, "#", 1)
            cyaRowCheck(ExpectedValuesEnglish.Common.oneOffDonationAmount, amount, "#", 2)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasDonationAmount, amount, "#", 3)
            cyaRowCheck(ExpectedValuesEnglish.Individual.overseasDonationNames, ExpectedValuesEnglish.Common.priorDonationNames, "#", 4)
            cyaRowCheck(ExpectedValuesEnglish.Common.lastYearAmount, amount, "#", 5)
            cyaRowCheck(ExpectedValuesEnglish.Common.thisYearAmount, amount, "#", 6)
            cyaRowCheck(ExpectedValuesEnglish.Common.sharesSecuritiesAmount, amount, "#", 7)
            cyaRowCheck(ExpectedValuesEnglish.Common.landPropertyAmount, amount, "#", 8)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandPropertyAmount, amount, "#", 9)
            cyaRowCheck(ExpectedValuesEnglish.Individual.overseasSharesSecurityLandPropertyNames, ExpectedValuesEnglish.Common.priorSharesSecuritiesLandPropertyNames, "#", 10)
          }

          buttonCheck(ExpectedValuesEnglish.Common.saveAndContinue)

          welshToggleCheck(ENGLISH)
        }

        "there is a full CYA model, and there is a full prior data model" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.GIFT_AID_CYA -> cyaDataMax.asJsonString,
            SessionValues.GIFT_AID_PRIOR_SUB -> Json.prettyPrint(Json.toJson(priorDataMax))
          ))

          lazy val result = {
            authoriseIndividual()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies).get())
          }

          implicit lazy val document: () => Document = () => Jsoup.parse(result.body)

          "has an OK (200) status" in {
            result.status shouldBe OK
          }

          titleCheck(ExpectedValuesEnglish.Individual.title)
          h1Check(ExpectedValuesEnglish.Individual.title + " " + ExpectedValuesEnglish.Common.caption)
          captionCheck(ExpectedValuesEnglish.Common.caption)

          //noinspection ScalaStyle
          {
            cyaRowCheck(ExpectedValuesEnglish.Common.donationViaGiftAidAmount, amount, "#", 1)
            cyaRowCheck(ExpectedValuesEnglish.Common.oneOffDonationAmount, amount, "#", 2)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasDonationAmount, amount, "#", 3)
            cyaRowCheck(ExpectedValuesEnglish.Individual.overseasDonationNames, ExpectedValuesEnglish.Common.overseasDonationNamesValue, "#", 4)
            cyaRowCheck(ExpectedValuesEnglish.Common.lastYearAmount, amount, "#", 5)
            cyaRowCheck(ExpectedValuesEnglish.Common.thisYearAmount, amount, "#", 6)
            cyaRowCheck(ExpectedValuesEnglish.Common.sharesSecuritiesAmount, amount, "#", 7)
            cyaRowCheck(ExpectedValuesEnglish.Common.landPropertyAmount, amount, "#", 8)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandPropertyAmount, amount, "#", 9)
            cyaRowCheck(ExpectedValuesEnglish.Individual.overseasSharesSecurityLandPropertyNames, ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandPropertyNamesValue, "#", 10)
          }

          buttonCheck(ExpectedValuesEnglish.Common.saveAndContinue)

          welshToggleCheck(ENGLISH)
        }

      }

      "return a minimal CYA view" when {

        "the CYA model contains all false values" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.GIFT_AID_CYA -> cyaDataMin.asJsonString
          ))

          lazy val result = {
            authoriseIndividual()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies).get())
          }

          implicit lazy val document: () => Document = () => Jsoup.parse(result.body)

          "has an OK (200) status" in {
            result.status shouldBe OK
          }

          titleCheck(ExpectedValuesEnglish.Individual.title)
          h1Check(ExpectedValuesEnglish.Individual.title + " " + ExpectedValuesEnglish.Common.caption)
          captionCheck(ExpectedValuesEnglish.Common.caption)

          //noinspection ScalaStyle
          {
            cyaRowCheck(ExpectedValuesEnglish.Common.donationViaGiftAid, no, "#", 1)
            cyaRowCheck(ExpectedValuesEnglish.Common.oneOffDonation, no, "#", 2)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasDonation, no, "#", 3)
            cyaRowCheck(ExpectedValuesEnglish.Common.lastYear, no, "#", 4)
            cyaRowCheck(ExpectedValuesEnglish.Common.thisYear, no, "#", 5)
            cyaRowCheck(ExpectedValuesEnglish.Common.sharesSecuritiesLandProperty, no, "#", 6)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandProperty, no, "#", 7)
          }

          buttonCheck(ExpectedValuesEnglish.Common.saveAndContinue)

          welshToggleCheck(ENGLISH)

        }

      }

      "redirect to the overview page" when {

        "there is incomplete CYA data" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.GIFT_AID_CYA -> cyaDataIncomplete.asJsonString
          ))

          lazy val result = {
            authoriseIndividual()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies).get())
          }

          "redirects to the correct url" in {
            result
            verifyGet("/income-through-software/return/2022/view")
          }
        }

        "there is no CYA and no PRIOR data" which {
          lazy val result = {
            authoriseIndividual()
            await(wsClient.url(url).get())
          }

          "redirects to the correct url" in {
            result
            verifyGet("/income-through-software/return/2022/view")
          }
        }

      }
    }

    "as an agent" should {

      "return a full CYA view" when {

        "the CYA model is full" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.GIFT_AID_CYA -> cyaDataMax.asJsonString,
            SessionValues.CLIENT_NINO -> nino,
            SessionValues.CLIENT_MTDITID -> mtditid
          ))

          lazy val result = {
            authoriseAgent()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies).get())
          }

          implicit lazy val document: () => Document = () => Jsoup.parse(result.body)

          "has an OK (200) status" in {
            result.status shouldBe OK
          }

          titleCheck(ExpectedValuesEnglish.Agent.title)
          h1Check(ExpectedValuesEnglish.Agent.title + " " + ExpectedValuesEnglish.Common.caption)
          captionCheck(ExpectedValuesEnglish.Common.caption)

          //noinspection ScalaStyle
          {
            cyaRowCheck(ExpectedValuesEnglish.Common.donationViaGiftAid, yes, "#", 1)
            cyaRowCheck(ExpectedValuesEnglish.Common.donationViaGiftAidAmount, amount, "#", 2)

            cyaRowCheck(ExpectedValuesEnglish.Common.oneOffDonation, yes, "#", 3)
            cyaRowCheck(ExpectedValuesEnglish.Common.oneOffDonationAmount, amount, "#", 4)

            cyaRowCheck(ExpectedValuesEnglish.Common.overseasDonation, yes, "#", 5)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasDonationAmount, amount, "#", 6)
            cyaRowCheck(ExpectedValuesEnglish.Agent.overseasDonationNames, ExpectedValuesEnglish.Common.overseasDonationNamesValue, "#", 7)

            cyaRowCheck(ExpectedValuesEnglish.Common.lastYear, yes, "#", 8)
            cyaRowCheck(ExpectedValuesEnglish.Common.lastYearAmount, amount, "#", 9)

            cyaRowCheck(ExpectedValuesEnglish.Common.thisYear, yes, "#", 10)
            cyaRowCheck(ExpectedValuesEnglish.Common.thisYearAmount, amount, "#", 11)

            cyaRowCheck(ExpectedValuesEnglish.Common.sharesSecuritiesLandProperty, yes, "#", 12)
            cyaRowCheck(ExpectedValuesEnglish.Common.sharesSecurities, yes, "#", 13)
            cyaRowCheck(ExpectedValuesEnglish.Common.sharesSecuritiesAmount, amount, "#", 14)
            cyaRowCheck(ExpectedValuesEnglish.Common.landProperty, yes, "#", 15)
            cyaRowCheck(ExpectedValuesEnglish.Common.landPropertyAmount, amount, "#", 16)

            cyaRowCheck(ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandProperty, yes, "#", 17)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandPropertyAmount, amount, "#", 18)
            cyaRowCheck(ExpectedValuesEnglish.Agent.overseasSharesSecurityLandPropertyNames, ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandPropertyNamesValue, "#", 19)
          }

          buttonCheck(ExpectedValuesEnglish.Common.saveAndContinue)

          welshToggleCheck(ENGLISH)

        }

      }

      "return a cya page with all the yes/no questions hidden" when {

        "there is no CYA model, but there is a full prior data model" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.GIFT_AID_PRIOR_SUB -> Json.prettyPrint(Json.toJson(priorDataMax)),
            SessionValues.CLIENT_NINO -> nino,
            SessionValues.CLIENT_MTDITID -> mtditid
          ))

          lazy val result = {
            authoriseAgent()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies).get())
          }

          implicit lazy val document: () => Document = () => Jsoup.parse(result.body)

          "has an OK (200) status" in {
            result.status shouldBe OK
          }

          titleCheck(ExpectedValuesEnglish.Agent.title)
          h1Check(ExpectedValuesEnglish.Agent.title + " " + ExpectedValuesEnglish.Common.caption)
          captionCheck(ExpectedValuesEnglish.Common.caption)

          //noinspection ScalaStyle
          {
            cyaRowCheck(ExpectedValuesEnglish.Common.donationViaGiftAidAmount, amount, "#", 1)
            cyaRowCheck(ExpectedValuesEnglish.Common.oneOffDonationAmount, amount, "#", 2)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasDonationAmount, amount, "#", 3)
            cyaRowCheck(ExpectedValuesEnglish.Agent.overseasDonationNames, ExpectedValuesEnglish.Common.priorDonationNames, "#", 4)
            cyaRowCheck(ExpectedValuesEnglish.Common.lastYearAmount, amount, "#", 5)
            cyaRowCheck(ExpectedValuesEnglish.Common.thisYearAmount, amount, "#", 6)
            cyaRowCheck(ExpectedValuesEnglish.Common.sharesSecuritiesAmount, amount, "#", 7)
            cyaRowCheck(ExpectedValuesEnglish.Common.landPropertyAmount, amount, "#", 8)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandPropertyAmount, amount, "#", 9)
            cyaRowCheck(ExpectedValuesEnglish.Agent.overseasSharesSecurityLandPropertyNames, ExpectedValuesEnglish.Common.priorSharesSecuritiesLandPropertyNames, "#", 10)
          }

          buttonCheck(ExpectedValuesEnglish.Common.saveAndContinue)

          welshToggleCheck(ENGLISH)
        }

        "there is a full CYA model, and there is a full prior data model" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.GIFT_AID_CYA -> cyaDataMax.asJsonString,
            SessionValues.GIFT_AID_PRIOR_SUB -> Json.prettyPrint(Json.toJson(priorDataMax)),
            SessionValues.CLIENT_NINO -> nino,
            SessionValues.CLIENT_MTDITID -> mtditid
          ))

          lazy val result = {
            authoriseAgent()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies).get())
          }

          implicit lazy val document: () => Document = () => Jsoup.parse(result.body)

          "has an OK (200) status" in {
            result.status shouldBe OK
          }

          titleCheck(ExpectedValuesEnglish.Agent.title)
          h1Check(ExpectedValuesEnglish.Agent.title + " " + ExpectedValuesEnglish.Common.caption)
          captionCheck(ExpectedValuesEnglish.Common.caption)

          //noinspection ScalaStyle
          {
            cyaRowCheck(ExpectedValuesEnglish.Common.donationViaGiftAidAmount, amount, "#", 1)
            cyaRowCheck(ExpectedValuesEnglish.Common.oneOffDonationAmount, amount, "#", 2)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasDonationAmount, amount, "#", 3)
            cyaRowCheck(ExpectedValuesEnglish.Agent.overseasDonationNames, ExpectedValuesEnglish.Common.overseasDonationNamesValue, "#", 4)
            cyaRowCheck(ExpectedValuesEnglish.Common.lastYearAmount, amount, "#", 5)
            cyaRowCheck(ExpectedValuesEnglish.Common.thisYearAmount, amount, "#", 6)
            cyaRowCheck(ExpectedValuesEnglish.Common.sharesSecuritiesAmount, amount, "#", 7)
            cyaRowCheck(ExpectedValuesEnglish.Common.landPropertyAmount, amount, "#", 8)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandPropertyAmount, amount, "#", 9)
            cyaRowCheck(ExpectedValuesEnglish.Agent.overseasSharesSecurityLandPropertyNames, ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandPropertyNamesValue, "#", 10)
          }

          buttonCheck(ExpectedValuesEnglish.Common.saveAndContinue)

          welshToggleCheck(ENGLISH)
        }

      }

      "return a minimal CYA view" when {

        "the CYA model contains all false values" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.GIFT_AID_CYA -> cyaDataMin.asJsonString,
            SessionValues.CLIENT_NINO -> nino,
            SessionValues.CLIENT_MTDITID -> mtditid
          ))

          lazy val result = {
            authoriseAgent()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies).get())
          }

          implicit lazy val document: () => Document = () => Jsoup.parse(result.body)

          "has an OK (200) status" in {
            result.status shouldBe OK
          }

          titleCheck(ExpectedValuesEnglish.Agent.title)
          h1Check(ExpectedValuesEnglish.Agent.title + " " + ExpectedValuesEnglish.Common.caption)
          captionCheck(ExpectedValuesEnglish.Common.caption)

          //noinspection ScalaStyle
          {
            cyaRowCheck(ExpectedValuesEnglish.Common.donationViaGiftAid, no, "#", 1)
            cyaRowCheck(ExpectedValuesEnglish.Common.oneOffDonation, no, "#", 2)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasDonation, no, "#", 3)
            cyaRowCheck(ExpectedValuesEnglish.Common.lastYear, no, "#", 4)
            cyaRowCheck(ExpectedValuesEnglish.Common.thisYear, no, "#", 5)
            cyaRowCheck(ExpectedValuesEnglish.Common.sharesSecuritiesLandProperty, no, "#", 6)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandProperty, no, "#", 7)
          }

          buttonCheck(ExpectedValuesEnglish.Common.saveAndContinue)

          welshToggleCheck(ENGLISH)

        }

      }

      "redirect to the overview page" when {

        "there is incomplete CYA data" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.GIFT_AID_CYA -> cyaDataIncomplete.asJsonString,
            SessionValues.CLIENT_NINO -> nino,
            SessionValues.CLIENT_MTDITID -> mtditid
          ))

          lazy val result = {
            authoriseAgent()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies).get())
          }

          "redirects to the correct url" in {
            result
            verifyGet("/income-through-software/return/2022/view")
          }
        }

        "there is no CYA and no PRIOR data" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.CLIENT_NINO -> nino,
            SessionValues.CLIENT_MTDITID -> mtditid
          ))

          lazy val result = {
            authoriseAgent()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies).get())
          }

          "redirects to the correct url" in {
            result
            verifyGet("/income-through-software/return/2022/view")
          }
        }

      }
    }

  }

  "GET in Welsh" should {

    "as an individual" should {

      "return a full CYA view" when {

        "the CYA model is full" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.GIFT_AID_CYA -> cyaDataMax.asJsonString
          ))

          lazy val result = {
            authoriseIndividual()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, HeaderNames.ACCEPT_LANGUAGE -> "cy").get())
          }

          implicit lazy val document: () => Document = () => Jsoup.parse(result.body)

          "has an OK (200) status" in {
            result.status shouldBe OK
          }

          titleCheck(ExpectedValuesEnglish.Individual.title)
          h1Check(ExpectedValuesEnglish.Individual.title + " " + ExpectedValuesEnglish.Common.caption)
          captionCheck(ExpectedValuesEnglish.Common.caption)

          //noinspection ScalaStyle
          {
            cyaRowCheck(ExpectedValuesEnglish.Common.donationViaGiftAid, yes, "#", 1)
            cyaRowCheck(ExpectedValuesEnglish.Common.donationViaGiftAidAmount, amount, "#", 2)

            cyaRowCheck(ExpectedValuesEnglish.Common.oneOffDonation, yes, "#", 3)
            cyaRowCheck(ExpectedValuesEnglish.Common.oneOffDonationAmount, amount, "#", 4)

            cyaRowCheck(ExpectedValuesEnglish.Common.overseasDonation, yes, "#", 5)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasDonationAmount, amount, "#", 6)
            cyaRowCheck(ExpectedValuesEnglish.Individual.overseasDonationNames, ExpectedValuesEnglish.Common.overseasDonationNamesValue, "#", 7)

            cyaRowCheck(ExpectedValuesEnglish.Common.lastYear, yes, "#", 8)
            cyaRowCheck(ExpectedValuesEnglish.Common.lastYearAmount, amount, "#", 9)

            cyaRowCheck(ExpectedValuesEnglish.Common.thisYear, yes, "#", 10)
            cyaRowCheck(ExpectedValuesEnglish.Common.thisYearAmount, amount, "#", 11)

            cyaRowCheck(ExpectedValuesEnglish.Common.sharesSecuritiesLandProperty, yes, "#", 12)
            cyaRowCheck(ExpectedValuesEnglish.Common.sharesSecurities, yes, "#", 13)
            cyaRowCheck(ExpectedValuesEnglish.Common.sharesSecuritiesAmount, amount, "#", 14)
            cyaRowCheck(ExpectedValuesEnglish.Common.landProperty, yes, "#", 15)
            cyaRowCheck(ExpectedValuesEnglish.Common.landPropertyAmount, amount, "#", 16)

            cyaRowCheck(ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandProperty, yes, "#", 17)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandPropertyAmount, amount, "#", 18)
            cyaRowCheck(ExpectedValuesEnglish.Individual.overseasSharesSecurityLandPropertyNames, ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandPropertyNamesValue, "#", 19)
          }

          buttonCheck(ExpectedValuesEnglish.Common.saveAndContinue)

          welshToggleCheck(WELSH)

        }

      }

      "return a cya page with all the yes/no questions hidden" when {

        "there is no CYA model, but there is a full prior data model" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.GIFT_AID_PRIOR_SUB -> Json.prettyPrint(Json.toJson(priorDataMax))
          ))

          lazy val result = {
            authoriseIndividual()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, HeaderNames.ACCEPT_LANGUAGE -> "cy").get())
          }

          implicit lazy val document: () => Document = () => Jsoup.parse(result.body)

          "has an OK (200) status" in {
            result.status shouldBe OK
          }

          titleCheck(ExpectedValuesEnglish.Individual.title)
          h1Check(ExpectedValuesEnglish.Individual.title + " " + ExpectedValuesEnglish.Common.caption)
          captionCheck(ExpectedValuesEnglish.Common.caption)

          //noinspection ScalaStyle
          {
            cyaRowCheck(ExpectedValuesEnglish.Common.donationViaGiftAidAmount, amount, "#", 1)
            cyaRowCheck(ExpectedValuesEnglish.Common.oneOffDonationAmount, amount, "#", 2)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasDonationAmount, amount, "#", 3)
            cyaRowCheck(ExpectedValuesEnglish.Individual.overseasDonationNames, ExpectedValuesEnglish.Common.priorDonationNames, "#", 4)
            cyaRowCheck(ExpectedValuesEnglish.Common.lastYearAmount, amount, "#", 5)
            cyaRowCheck(ExpectedValuesEnglish.Common.thisYearAmount, amount, "#", 6)
            cyaRowCheck(ExpectedValuesEnglish.Common.sharesSecuritiesAmount, amount, "#", 7)
            cyaRowCheck(ExpectedValuesEnglish.Common.landPropertyAmount, amount, "#", 8)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandPropertyAmount, amount, "#", 9)
            cyaRowCheck(ExpectedValuesEnglish.Individual.overseasSharesSecurityLandPropertyNames, ExpectedValuesEnglish.Common.priorSharesSecuritiesLandPropertyNames, "#", 10)
          }

          buttonCheck(ExpectedValuesEnglish.Common.saveAndContinue)

          welshToggleCheck(WELSH)
        }

        "there is a full CYA model, and there is a full prior data model" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.GIFT_AID_CYA -> cyaDataMax.asJsonString,
            SessionValues.GIFT_AID_PRIOR_SUB -> Json.prettyPrint(Json.toJson(priorDataMax))
          ))

          lazy val result = {
            authoriseIndividual()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, HeaderNames.ACCEPT_LANGUAGE -> "cy").get())
          }

          implicit lazy val document: () => Document = () => Jsoup.parse(result.body)

          "has an OK (200) status" in {
            result.status shouldBe OK
          }

          titleCheck(ExpectedValuesEnglish.Individual.title)
          h1Check(ExpectedValuesEnglish.Individual.title + " " + ExpectedValuesEnglish.Common.caption)
          captionCheck(ExpectedValuesEnglish.Common.caption)

          //noinspection ScalaStyle
          {
            cyaRowCheck(ExpectedValuesEnglish.Common.donationViaGiftAidAmount, amount, "#", 1)
            cyaRowCheck(ExpectedValuesEnglish.Common.oneOffDonationAmount, amount, "#", 2)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasDonationAmount, amount, "#", 3)
            cyaRowCheck(ExpectedValuesEnglish.Individual.overseasDonationNames, ExpectedValuesEnglish.Common.overseasDonationNamesValue, "#", 4)
            cyaRowCheck(ExpectedValuesEnglish.Common.lastYearAmount, amount, "#", 5)
            cyaRowCheck(ExpectedValuesEnglish.Common.thisYearAmount, amount, "#", 6)
            cyaRowCheck(ExpectedValuesEnglish.Common.sharesSecuritiesAmount, amount, "#", 7)
            cyaRowCheck(ExpectedValuesEnglish.Common.landPropertyAmount, amount, "#", 8)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandPropertyAmount, amount, "#", 9)
            cyaRowCheck(ExpectedValuesEnglish.Individual.overseasSharesSecurityLandPropertyNames, ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandPropertyNamesValue, "#", 10)
          }

          buttonCheck(ExpectedValuesEnglish.Common.saveAndContinue)

          welshToggleCheck(WELSH)
        }

      }

      "return a minimal CYA view" when {

        "the CYA model contains all false values" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.GIFT_AID_CYA -> cyaDataMin.asJsonString
          ))

          lazy val result = {
            authoriseIndividual()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, HeaderNames.ACCEPT_LANGUAGE -> "cy").get())
          }

          implicit lazy val document: () => Document = () => Jsoup.parse(result.body)

          "has an OK (200) status" in {
            result.status shouldBe OK
          }

          titleCheck(ExpectedValuesEnglish.Individual.title)
          h1Check(ExpectedValuesEnglish.Individual.title + " " + ExpectedValuesEnglish.Common.caption)
          captionCheck(ExpectedValuesEnglish.Common.caption)

          //noinspection ScalaStyle
          {
            cyaRowCheck(ExpectedValuesEnglish.Common.donationViaGiftAid, no, "#", 1)
            cyaRowCheck(ExpectedValuesEnglish.Common.oneOffDonation, no, "#", 2)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasDonation, no, "#", 3)
            cyaRowCheck(ExpectedValuesEnglish.Common.lastYear, no, "#", 4)
            cyaRowCheck(ExpectedValuesEnglish.Common.thisYear, no, "#", 5)
            cyaRowCheck(ExpectedValuesEnglish.Common.sharesSecuritiesLandProperty, no, "#", 6)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandProperty, no, "#", 7)
          }

          buttonCheck(ExpectedValuesEnglish.Common.saveAndContinue)

          welshToggleCheck(WELSH)

        }

      }

      "redirect to the overview page" when {

        "there is incomplete CYA data" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.GIFT_AID_CYA -> cyaDataIncomplete.asJsonString
          ))

          lazy val result = {
            authoriseIndividual()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, HeaderNames.ACCEPT_LANGUAGE -> "cy").get())
          }

          "redirects to the correct url" in {
            result
            verifyGet("/income-through-software/return/2022/view")
          }
        }

        "there is no CYA and no PRIOR data" which {
          lazy val result = {
            authoriseIndividual()
            await(wsClient.url(url).get())
          }

          "redirects to the correct url" in {
            result
            verifyGet("/income-through-software/return/2022/view")
          }
        }

      }
    }

    "as an agent" should {

      "return a full CYA view" when {

        "the CYA model is full" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.GIFT_AID_CYA -> cyaDataMax.asJsonString,
            SessionValues.CLIENT_NINO -> nino,
            SessionValues.CLIENT_MTDITID -> mtditid
          ))

          lazy val result = {
            authoriseAgent()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, HeaderNames.ACCEPT_LANGUAGE -> "cy").get())
          }

          implicit lazy val document: () => Document = () => Jsoup.parse(result.body)

          "has an OK (200) status" in {
            result.status shouldBe OK
          }

          titleCheck(ExpectedValuesEnglish.Agent.title)
          h1Check(ExpectedValuesEnglish.Agent.title + " " + ExpectedValuesEnglish.Common.caption)
          captionCheck(ExpectedValuesEnglish.Common.caption)

          //noinspection ScalaStyle
          {
            cyaRowCheck(ExpectedValuesEnglish.Common.donationViaGiftAid, yes, "#", 1)
            cyaRowCheck(ExpectedValuesEnglish.Common.donationViaGiftAidAmount, amount, "#", 2)

            cyaRowCheck(ExpectedValuesEnglish.Common.oneOffDonation, yes, "#", 3)
            cyaRowCheck(ExpectedValuesEnglish.Common.oneOffDonationAmount, amount, "#", 4)

            cyaRowCheck(ExpectedValuesEnglish.Common.overseasDonation, yes, "#", 5)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasDonationAmount, amount, "#", 6)
            cyaRowCheck(ExpectedValuesEnglish.Agent.overseasDonationNames, ExpectedValuesEnglish.Common.overseasDonationNamesValue, "#", 7)

            cyaRowCheck(ExpectedValuesEnglish.Common.lastYear, yes, "#", 8)
            cyaRowCheck(ExpectedValuesEnglish.Common.lastYearAmount, amount, "#", 9)

            cyaRowCheck(ExpectedValuesEnglish.Common.thisYear, yes, "#", 10)
            cyaRowCheck(ExpectedValuesEnglish.Common.thisYearAmount, amount, "#", 11)

            cyaRowCheck(ExpectedValuesEnglish.Common.sharesSecuritiesLandProperty, yes, "#", 12)
            cyaRowCheck(ExpectedValuesEnglish.Common.sharesSecurities, yes, "#", 13)
            cyaRowCheck(ExpectedValuesEnglish.Common.sharesSecuritiesAmount, amount, "#", 14)
            cyaRowCheck(ExpectedValuesEnglish.Common.landProperty, yes, "#", 15)
            cyaRowCheck(ExpectedValuesEnglish.Common.landPropertyAmount, amount, "#", 16)

            cyaRowCheck(ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandProperty, yes, "#", 17)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandPropertyAmount, amount, "#", 18)
            cyaRowCheck(ExpectedValuesEnglish.Agent.overseasSharesSecurityLandPropertyNames, ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandPropertyNamesValue, "#", 19)
          }

          buttonCheck(ExpectedValuesEnglish.Common.saveAndContinue)

          welshToggleCheck(WELSH)

        }

      }

      "return a cya page with all the yes/no questions hidden" when {

        "there is no CYA model, but there is a full prior data model" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.GIFT_AID_PRIOR_SUB -> Json.prettyPrint(Json.toJson(priorDataMax)),
            SessionValues.CLIENT_NINO -> nino,
            SessionValues.CLIENT_MTDITID -> mtditid
          ))

          lazy val result = {
            authoriseAgent()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, HeaderNames.ACCEPT_LANGUAGE -> "cy").get())
          }

          implicit lazy val document: () => Document = () => Jsoup.parse(result.body)

          "has an OK (200) status" in {
            result.status shouldBe OK
          }

          titleCheck(ExpectedValuesEnglish.Agent.title)
          h1Check(ExpectedValuesEnglish.Agent.title + " " + ExpectedValuesEnglish.Common.caption)
          captionCheck(ExpectedValuesEnglish.Common.caption)

          //noinspection ScalaStyle
          {
            cyaRowCheck(ExpectedValuesEnglish.Common.donationViaGiftAidAmount, amount, "#", 1)
            cyaRowCheck(ExpectedValuesEnglish.Common.oneOffDonationAmount, amount, "#", 2)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasDonationAmount, amount, "#", 3)
            cyaRowCheck(ExpectedValuesEnglish.Agent.overseasDonationNames, ExpectedValuesEnglish.Common.priorDonationNames, "#", 4)
            cyaRowCheck(ExpectedValuesEnglish.Common.lastYearAmount, amount, "#", 5)
            cyaRowCheck(ExpectedValuesEnglish.Common.thisYearAmount, amount, "#", 6)
            cyaRowCheck(ExpectedValuesEnglish.Common.sharesSecuritiesAmount, amount, "#", 7)
            cyaRowCheck(ExpectedValuesEnglish.Common.landPropertyAmount, amount, "#", 8)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandPropertyAmount, amount, "#", 9)
            cyaRowCheck(ExpectedValuesEnglish.Agent.overseasSharesSecurityLandPropertyNames, ExpectedValuesEnglish.Common.priorSharesSecuritiesLandPropertyNames, "#", 10)
          }

          buttonCheck(ExpectedValuesEnglish.Common.saveAndContinue)

          welshToggleCheck(WELSH)
        }

        "there is a full CYA model, and there is a full prior data model" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.GIFT_AID_CYA -> cyaDataMax.asJsonString,
            SessionValues.GIFT_AID_PRIOR_SUB -> Json.prettyPrint(Json.toJson(priorDataMax)),
            SessionValues.CLIENT_NINO -> nino,
            SessionValues.CLIENT_MTDITID -> mtditid
          ))

          lazy val result = {
            authoriseAgent()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, HeaderNames.ACCEPT_LANGUAGE -> "cy").get())
          }

          implicit lazy val document: () => Document = () => Jsoup.parse(result.body)

          "has an OK (200) status" in {
            result.status shouldBe OK
          }

          titleCheck(ExpectedValuesEnglish.Agent.title)
          h1Check(ExpectedValuesEnglish.Agent.title + " " + ExpectedValuesEnglish.Common.caption)
          captionCheck(ExpectedValuesEnglish.Common.caption)

          //noinspection ScalaStyle
          {
            cyaRowCheck(ExpectedValuesEnglish.Common.donationViaGiftAidAmount, amount, "#", 1)
            cyaRowCheck(ExpectedValuesEnglish.Common.oneOffDonationAmount, amount, "#", 2)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasDonationAmount, amount, "#", 3)
            cyaRowCheck(ExpectedValuesEnglish.Agent.overseasDonationNames, ExpectedValuesEnglish.Common.overseasDonationNamesValue, "#", 4)
            cyaRowCheck(ExpectedValuesEnglish.Common.lastYearAmount, amount, "#", 5)
            cyaRowCheck(ExpectedValuesEnglish.Common.thisYearAmount, amount, "#", 6)
            cyaRowCheck(ExpectedValuesEnglish.Common.sharesSecuritiesAmount, amount, "#", 7)
            cyaRowCheck(ExpectedValuesEnglish.Common.landPropertyAmount, amount, "#", 8)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandPropertyAmount, amount, "#", 9)
            cyaRowCheck(ExpectedValuesEnglish.Agent.overseasSharesSecurityLandPropertyNames, ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandPropertyNamesValue, "#", 10)
          }

          buttonCheck(ExpectedValuesEnglish.Common.saveAndContinue)

          welshToggleCheck(WELSH)
        }

      }

      "return a minimal CYA view" when {

        "the CYA model contains all false values" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.GIFT_AID_CYA -> cyaDataMin.asJsonString,
            SessionValues.CLIENT_NINO -> nino,
            SessionValues.CLIENT_MTDITID -> mtditid
          ))

          lazy val result = {
            authoriseAgent()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, HeaderNames.ACCEPT_LANGUAGE -> "cy").get())
          }

          implicit lazy val document: () => Document = () => Jsoup.parse(result.body)

          "has an OK (200) status" in {
            result.status shouldBe OK
          }

          titleCheck(ExpectedValuesEnglish.Agent.title)
          h1Check(ExpectedValuesEnglish.Agent.title + " " + ExpectedValuesEnglish.Common.caption)
          captionCheck(ExpectedValuesEnglish.Common.caption)

          //noinspection ScalaStyle
          {
            cyaRowCheck(ExpectedValuesEnglish.Common.donationViaGiftAid, no, "#", 1)
            cyaRowCheck(ExpectedValuesEnglish.Common.oneOffDonation, no, "#", 2)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasDonation, no, "#", 3)
            cyaRowCheck(ExpectedValuesEnglish.Common.lastYear, no, "#", 4)
            cyaRowCheck(ExpectedValuesEnglish.Common.thisYear, no, "#", 5)
            cyaRowCheck(ExpectedValuesEnglish.Common.sharesSecuritiesLandProperty, no, "#", 6)
            cyaRowCheck(ExpectedValuesEnglish.Common.overseasSharesSecuritiesLandProperty, no, "#", 7)
          }

          buttonCheck(ExpectedValuesEnglish.Common.saveAndContinue)

          welshToggleCheck(WELSH)

        }

      }

      "redirect to the overview page" when {

        "there is incomplete CYA data" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.GIFT_AID_CYA -> cyaDataIncomplete.asJsonString,
            SessionValues.CLIENT_NINO -> nino,
            SessionValues.CLIENT_MTDITID -> mtditid
          ))

          lazy val result = {
            authoriseAgent()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, HeaderNames.ACCEPT_LANGUAGE -> "cy").get())
          }

          "redirects to the correct url" in {
            result
            verifyGet("/income-through-software/return/2022/view")
          }
        }

        "there is no CYA and no PRIOR data" which {
          lazy val playSessionCookies = PlaySessionCookieBaker.bakeSessionCookie(Map(
            SessionValues.CLIENT_NINO -> nino,
            SessionValues.CLIENT_MTDITID -> mtditid
          ))

          lazy val result = {
            authoriseAgent()
            await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookies, HeaderNames.ACCEPT_LANGUAGE -> "cy").get())
          }

          "redirects to the correct url" in {
            result
            verifyGet("/income-through-software/return/2022/view")
          }
        }

      }
    }

  }

  "POST" should {

    "redirect to the overview page" when {

      "there is no CYA data available" which {
        lazy val result = {
          wireMockServer.resetAll()
          authoriseIndividual()
          stubGet(s"/income-through-software/return/$taxYear/view", OK, "<title>Overview Page</title>")
          await(wsClient.url(url).withHttpHeaders("Csrf-Token" -> "nocheck").post(Map[String, String]()))
        }

        "page contains expected content" in {
          result.body shouldBe "<title>Overview Page</title>"
        }
      }

      "the request goes through successfully" which {
        lazy val playSessionCookie = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.GIFT_AID_CYA -> cyaDataMax.asJsonString
        ))

        lazy val result = {
          wireMockServer.resetAll()
          authoriseIndividual()
          stubPost(s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=$taxYear", NO_CONTENT, "{}")
          stubGet(s"/income-through-software/return/$taxYear/view", OK, "<title>Overview Page</title>")
          await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookie, "Csrf-Token" -> "nocheck").post(Map[String, String]()))
        }

        "redirects to the correct url" in {
          result.body shouldBe "<title>Overview Page</title>"
        }

      }

    }

    "redirect to an error page" when {

      "an error is returned from DES" which {
        lazy val playSessionCookie = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
          SessionValues.GIFT_AID_CYA -> cyaDataMax.asJsonString
        ))

        lazy val result = {
          wireMockServer.resetAll()
          authoriseIndividual()
          stubPut(
            s"/income-tax-gift-aid/income-tax/nino/$nino/sources\\?taxYear=$taxYear",
            BAD_REQUEST,
            Json.toJson(APIErrorBodyModel("BAD_REQUEST", "Oh hey look, literally any error.")).toString()
          )
          await(wsClient.url(url).withHttpHeaders(HeaderNames.COOKIE -> playSessionCookie, "Csrf-Token" -> "nocheck").post(Map[String, String]()))
        }

        implicit val document: () => Document = () => Jsoup.parse(result.body)

        titleCheck("Sorry, there is a problem with the service")
      }

    }

  }

}
