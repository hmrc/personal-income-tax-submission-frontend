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
import common.SessionValues.GIFT_AID_PRIOR_SUB
import forms.YesNoForm
import helpers.PlaySessionCookieBaker
import models.charity.GiftAidCYAModel
import models.charity.prior.{GiftAidPaymentsModel, GiftAidSubmissionModel}
import models.priorDataModels.IncomeSourcesModel
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.http.Status._
import play.api.libs.json.Json
import play.api.libs.ws.{WSClient, WSResponse}
import utils.{GiftAidDatabaseHelper, IntegrationTest, ViewHelpers}

class GiftAidLastTaxYearControllerISpec extends IntegrationTest with ViewHelpers with GiftAidDatabaseHelper {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  lazy val controller: GiftAidLastTaxYearController = app.injector.instanceOf[GiftAidLastTaxYearController]
  val taxYear: Int = 2022

  object IndividualExpected {
    val expectedTitle: String = "Do you want to add any of your donations to the last tax year?"
    val expectedH1: String = "Do you want to add any of your donations to the last tax year?"
    val expectedError: String = "Select yes to add any of your donations to the last tax year"
    val expectedContent1: String = "You told us you donated £150 to charity by using Gift Aid. You can add some of this donation" +
      " to the 6 April 2020 to 5 April 2021 tax year."
    val expectedContent2: String = "You might want to do this if you paid higher rate tax last year but will not this year."
  }

  object AgentExpected {
    val expectedTitle: String = "Do you want to add any of your client’s donations to the last tax year?"
    val expectedH1: String = "Do you want to add any of your client’s donations to the last tax year?"
    val expectedError: String = "Select yes to add any of your client’s donations to the last tax year"
    val expectedContent1: String = "You told us your client donated £150 to charity by using Gift Aid. You can add some of this donation" +
      " to the 6 April 2020 to 5 April 2021 tax year."
    val expectedContent2: String = "You might want to do this if your client paid higher rate tax last year but will not this year."
  }

  val expectedCaption: String = "Donations to charity for 6 April 2021 to 5 April 2022"
  val yesText = "Yes"
  val noText = "No"
  val expectedContinue = "Continue"

  val continueSelector = "#continue"
  val captionSelector: String = ".govuk-caption-l"
  val contentSelector1: String = "#main-content > div > div > form > div > fieldset > legend > div > p:nth-child(1)"
  val contentSelector2: String = "#main-content > div > div > form > div > fieldset > legend > div > p:nth-child(2)"
  val errorSummaryHref = "#value"

  val url = s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/add-charity-donations-to-last-tax-year"
  
  val testModel: GiftAidSubmissionModel =
    GiftAidSubmissionModel(Some(GiftAidPaymentsModel(None, Some(List("JaneDoe")), None, Some(150.00), None, None)),None)

  "the user is an individual" when {
    import IndividualExpected._

    "calling the GET/ endpoint" should {

      "show the donations to previous tax year page" when {

        "the user has indicated they did not donate to an overseas charity" which {
          val cyaData = GiftAidCYAModel(
            donationsViaGiftAid = Some(true),
            donationsViaGiftAidAmount = Some(150.00),
            overseasDonationsViaGiftAid = Some(false)
          )

          lazy val result: WSResponse = {
            dropGiftAidDB()

            emptyUserDataStub()
            insertCyaData(Some(cyaData))

            authoriseIndividual()
            await(wsClient.url(url).withHttpHeaders(xSessionId, csrfContent).withFollowRedirects(false).get())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle)
          welshToggleCheck("English")
          h1Check(expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(expectedContent1, contentSelector1)
          textOnPageCheck(expectedContent2, contentSelector2)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(expectedContinue, continueSelector)
        }

        "the user has indicated they did donate to an overseas charity, and there are charity names in session" which {
          val cyaData = GiftAidCYAModel(
            donationsViaGiftAid = Some(true),
            donationsViaGiftAidAmount = Some(150.00),
            overseasDonationsViaGiftAid = Some(true),
            overseasDonationsViaGiftAidAmount = Some(1000.00),
            overseasCharityNames = Some(Seq("Skyrim Guard Knee Support", "Dodogama Preservation Fund"))
          )

          lazy val result: WSResponse = {
            dropGiftAidDB()

            emptyUserDataStub()
            insertCyaData(Some(cyaData))

            authoriseIndividual()
            await(wsClient.url(url).withHttpHeaders(xSessionId, csrfContent).withFollowRedirects(false).get())
          }

          implicit def document: () => Document = () => Jsoup.parse(result.body)

          titleCheck(expectedTitle)
          welshToggleCheck("English")
          h1Check(expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(expectedContent1, contentSelector1)
          textOnPageCheck(expectedContent2, contentSelector2)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(expectedContinue, continueSelector)
        }

      }

      "redirect to the overview page" when {

        "there is no session data" which {
          lazy val result = {
            dropGiftAidDB()

            emptyUserDataStub()
            insertCyaData(None)

            authoriseIndividual()
            await(wsClient.url(url).withHttpHeaders(xSessionId, csrfContent).withFollowRedirects(false).get())
          }

          "has a status of SEE_OTHER(303)" in {
            result.status shouldBe SEE_OTHER
          }

          "redirects to the overview page" in {
            result.headers("Location").head shouldBe overviewUrl
          }
        }
      }

      "redirect to the Name of overseas charity page" when {

        "the user has indicated they donated to overseas charities, but have no charity names" which {
          val cyaData = GiftAidCYAModel(
            donationsViaGiftAid = Some(true),
            donationsViaGiftAidAmount = Some(150.00),
            overseasDonationsViaGiftAid = Some(true),
            overseasDonationsViaGiftAidAmount = Some(1000.23)
          )

          lazy val result = {
            dropGiftAidDB()

            emptyUserDataStub()
            insertCyaData(Some(cyaData))

            authoriseIndividual()
            await(wsClient.url(url).withHttpHeaders(xSessionId, csrfContent).withFollowRedirects(false).get())
          }

          "has a status of SEE_OTHER(303)" in {
            result.status shouldBe SEE_OTHER
          }

          "redirects to the correct page" in {
            result.headers("Location").head shouldBe "/todo"
          }
        }

      }
    
      "redirect to the add donations made next year to this year  yes/no page" when {
        
        "there is CYA data, but the donationsViaGiftAidAmount prior field is not filled in" which {
          lazy val result = {
            dropGiftAidDB()
            
            emptyUserDataStub()
            insertCyaData(Some(GiftAidCYAModel(
              overseasDonationsViaGiftAid = Some(false),
              donationsViaGiftAid = Some(true)
            )))
            
            authoriseIndividual()
            await(
              wsClient.url(url)
                .withHttpHeaders(xSessionId, csrfContent)
                .withFollowRedirects(false)
                .get()
            )
          }
          
          "has a status of SEE_OTHER (303)" in {
            result.status shouldBe SEE_OTHER
          }
          
          "has the correct redirect URL" in {
            result.headers("Location").head shouldBe controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear).url
          }
        }
        
      }
    }

    "calling the POST/ endpoint" should {


      "redirect to the last tax year amount page" when {

        "the user answers yes" when {

          "there is valid session data available" which {
            lazy val result = {
              dropGiftAidDB()

              emptyUserDataStub()
              insertCyaData(Some(GiftAidCYAModel()))

              authoriseIndividual()
              await(
                wsClient.url(url)
                  .withHttpHeaders(xSessionId, csrfContent)
                  .withFollowRedirects(false)
                  .post(Map[String, String](YesNoForm.yesNo -> YesNoForm.yes))
              )
            }

            "has a status of SEE_OTHER" in {
              result.status shouldBe SEE_OTHER
            }

            "has the correct URL" in {
              result.headers("Location").head shouldBe controllers.charity.routes.LastTaxYearAmountController.show(taxYear).url
            }

            "updated the addDonationsToLastTaxYear field to true" in {
              await(giftAidDatabase.find(taxYear)).get.giftAid.get.addDonationToLastYear.get shouldBe true
            }

          }

        }

      }

      "redirect to the add tax to this tax year page" when {

        "the user answers no" when {

          "there is valid session data available" which {
            lazy val result = {
              dropGiftAidDB()

              emptyUserDataStub()
              insertCyaData(Some(GiftAidCYAModel(
                addDonationToLastYear = Some(true),
                addDonationToLastYearAmount = Some(1000.00)
              )))

              authoriseIndividual()
              await(
                wsClient.url(url)
                  .withHttpHeaders(xSessionId, csrfContent)
                  .withFollowRedirects(false)
                  .post(Map[String, String](YesNoForm.yesNo -> YesNoForm.no))
              )
            }

            lazy val databaseResult: GiftAidCYAModel = await(giftAidDatabase.find(taxYear)).get.giftAid.get

            "has a status of SEE_OTHER" in {
              result.status shouldBe SEE_OTHER
            }

            "has the correct URL" in {
              result.headers("Location").head shouldBe controllers.charity.routes.DonationsToPreviousTaxYearController.show(taxYear, taxYear).url
            }

            "updated the addDonationsToLastTaxYear field to false" in {
              databaseResult.addDonationToLastYear.get shouldBe false
            }

            "cleared the addDonatiosnToLastTaxYearAmount field" in {
              databaseResult.addDonationToLastYearAmount shouldBe None
            }
          }
        }
      }

      "redirect to the overview page" when {

        "the user selects a valid option (Yes or No) but has no CYA session data" which {
          lazy val result = {
            dropGiftAidDB()

            emptyUserDataStub()
            insertCyaData(None)

            authoriseIndividual()
            await(
              wsClient.url(url)
                .withHttpHeaders(xSessionId, csrfContent)
                .withFollowRedirects(false)
                .post(Map[String, String](YesNoForm.yesNo -> YesNoForm.yes))
            )
          }

          "has a status of SEE_OTHER" in {
            result.status shouldBe SEE_OTHER
          }

          "has the overview page as the redirect link" in {
            result.headers("Location").head shouldBe overviewUrl
          }
        }

        "the user has invalid form data, has session data, but no prior data" which {
          lazy val result = {
            dropGiftAidDB()

            emptyUserDataStub()
            insertCyaData(Some(GiftAidCYAModel()))

            authoriseIndividual()
            await(
              wsClient.url(url)
                .withHttpHeaders(xSessionId, csrfContent)
                .withFollowRedirects(false)
                .post(Map[String, String]())
            )
          }

          "has a status of SEE_OTHER" in {
            result.status shouldBe SEE_OTHER
          }

          "has the overview page as the redirect link" in {
            result.headers("Location").head shouldBe overviewUrl
          }

        }

      }

      "display the page with errors" when {

        "the user submits faulty form data when they have session and prior data" which {
          lazy val result = {
            dropGiftAidDB()

            userDataStub(IncomeSourcesModel(
              giftAid = Some(GiftAidSubmissionModel(
                giftAidPayments = Some(GiftAidPaymentsModel(
                  currentYearTreatedAsPreviousYear = Some(150.00)
                ))
              ))
            ), nino, taxYear)
            insertCyaData(Some(GiftAidCYAModel()))

            authoriseIndividual()
            await(
              wsClient.url(url)
                .withHttpHeaders(xSessionId, csrfContent)
                .withFollowRedirects(false)
                .post(Map[String, String]())
            )
          }

          "has a status of bad request" in {
            result.status shouldBe BAD_REQUEST
          }

          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck("Error: " + expectedTitle)
          welshToggleCheck("English")
          h1Check(expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(expectedContent1, contentSelector1)
          textOnPageCheck(expectedContent2, contentSelector2)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(expectedContinue, continueSelector)

          errorSummaryCheck(expectedError, errorSummaryHref)
          errorAboveElementCheck(expectedError)
        }

      }
      
    }

  }
  
  "as an agent" when {
  import AgentExpected._
    ".show" should {

      "returns an action" which {
        lazy val result: WSResponse = {
          val cyaData = GiftAidCYAModel(
            donationsViaGiftAid = Some(true),
            donationsViaGiftAidAmount = Some(150.00),
            overseasDonationsViaGiftAid = Some(false)
          )
          
          dropGiftAidDB()
          
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))
          
          userDataStub(IncomeSourcesModel(
            giftAid = Some(testModel)
          ), nino, taxYear)
          insertCyaData(Some(cyaData))

          authoriseAgent()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/add-charity-donations-to-last-tax-year")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
            .get())
        }
        implicit def document: () => Document = () => Jsoup.parse(result.body)

        "has an OK(200) status" in {
          result.status shouldBe OK
        }
        titleCheck(expectedTitle)
        welshToggleCheck("English")
        h1Check(expectedH1 + " " + expectedCaption)
        textOnPageCheck(expectedCaption, captionSelector)
        textOnPageCheck(expectedContent1, contentSelector1)
        textOnPageCheck(expectedContent2, contentSelector2)
        radioButtonCheck(yesText, 1)
        radioButtonCheck(noText, 2)
        buttonCheck(expectedContinue, continueSelector)
        
        noErrorsCheck()
      }
      
      "returns an redirect" which {
        lazy val result: WSResponse = {
          dropGiftAidDB()
          
          emptyUserDataStub()
          insertCyaData(None)
          
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A",
          ))

          authoriseAgent()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/add-charity-donations-to-last-tax-year")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, xSessionId, csrfContent)
            .get())
        }
        
        "has an NOT_FOUND status" in {
          result.status shouldBe NOT_FOUND
          result.uri.toString shouldBe overviewUrl
        }
        
      }
    }

    ".submit" should {

      "display the page with errors" when {
        
        "the user submits faulty form data when they have session and prior data" which {
          lazy val result = {
            dropGiftAidDB()

            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"
            ))
            
            userDataStub(IncomeSourcesModel(
              giftAid = Some(GiftAidSubmissionModel(
                giftAidPayments = Some(GiftAidPaymentsModel(
                  currentYearTreatedAsPreviousYear = Some(150.00)
                ))
              ))
            ), nino, taxYear)
            insertCyaData(Some(GiftAidCYAModel()))
            
            authoriseAgent()
            await(
              wsClient.url(url)
                .withHttpHeaders(xSessionId, csrfContent, HeaderNames.COOKIE -> sessionCookie)
                .withFollowRedirects(false)
                .post(Map[String, String]())
            )
          }
          
          "has a status of bad request" in {
            result.status shouldBe BAD_REQUEST
          }
          
          implicit val document: () => Document = () => Jsoup.parse(result.body)

          titleCheck("Error: " + expectedTitle)
          welshToggleCheck("English")
          h1Check(expectedH1 + " " + expectedCaption)
          textOnPageCheck(expectedCaption, captionSelector)
          textOnPageCheck(expectedContent1, contentSelector1)
          textOnPageCheck(expectedContent2, contentSelector2)
          radioButtonCheck(yesText, 1)
          radioButtonCheck(noText, 2)
          buttonCheck(expectedContinue, continueSelector)
          
          errorSummaryCheck(expectedError, errorSummaryHref)
          errorAboveElementCheck(expectedError)
        }
        
      }

    }

  }

}
