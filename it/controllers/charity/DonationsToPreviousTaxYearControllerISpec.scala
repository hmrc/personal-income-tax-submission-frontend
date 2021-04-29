
package controllers.charity

import common.SessionValues
import forms.YesNoForm
import helpers.PlaySessionCookieBaker
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.http.HeaderNames
import play.api.libs.ws.{WSClient, WSResponse}
import utils.IntegrationTest
import play.api.http.Status.{BAD_REQUEST, OK}

class DonationsToPreviousTaxYearControllerISpec extends IntegrationTest {

  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  lazy val controller: DonationsToPreviousTaxYearController = app.injector.instanceOf[DonationsToPreviousTaxYearController]

  val taxYear: Int = 2022

  object Selectors {

    val heading = "h1"
    val caption = ".govuk-caption-l"
    val errorSummaryNoSelection = ".govuk-error-summary__body > ul > li > a"
    val yesRadioButton = ".govuk-radios__item:nth-child(1) > label"
    val noRadioButton = ".govuk-radios__item:nth-child(2) > label"
    val paragraph1HintText = "#main-content > div > div > form > div > fieldset > legend > div > p:nth-child(1)"
    val paragraph2HintText = "#main-content > div > div > form > div > fieldset > legend > div > p:nth-child(2)"

  }

  object Content {

    val expectedTitle = "Do you want to add any donations made after 5 April 2022 to this tax year? - Update and submit an Income Tax Return - GOV.UK"
    val expectedErrorTitle = "Error: Do you want to add any donations made after 5 April 2022 to this tax year? - Update and submit an Income Tax Return - GOV.UK"
    val expectedHeading = "Do you want to add any donations made after 5 April 2022 to this tax year?"
    val expectedCaption = "Donations to charity for 6 April 2021 to 5 April 2022"
    val expectedParagraph1Individual = "If you made donations after 5 April 2022, you can add them to the 6 April 2021 to 5 April 2022 tax year."
    val expectedParagraph1Agent = "If your client made donations after 5 April 2022, you can add them to the 6 April 2021 to 5 April 2022 tax year."
    val expectedParagraph2Individual = "You might want to do this if you want tax relief sooner."
    val expectedParagraph2Agent = "You might want to do this if your client wants tax relief sooner."

  }

  "as an individual" when {

    ".show" should {

      "return an action" which {
        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donations-after-5-april-xxxx").get())
        }
        lazy val document: Document = Jsoup.parse(result.body)
        "has an OK(200) response" in {

          result.status shouldBe OK
        }

        "display the page content" in {

          document.title() shouldBe Content.expectedTitle
          document.select(Selectors.heading).text() shouldBe Content.expectedHeading
          document.select(Selectors.caption).text() shouldBe Content.expectedCaption
          document.select(Selectors.paragraph1HintText).text() shouldBe Content.expectedParagraph1Individual
          document.select(Selectors.paragraph2HintText).text() shouldBe Content.expectedParagraph2Individual

        }
      }
    }

    ".submit" should {

      "return an OK (200) response" in {

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donations-after-5-april-xxxx")
            .post(Map(YesNoForm.yesNo -> YesNoForm.yes)))
        }

        result.status shouldBe OK

      }

      "when there is an incorrect input" should {

        lazy val result: WSResponse = {
          authoriseIndividual()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donations-after-5-april-xxxx")
            .post(Map[String, String]()))
        }

        lazy val document: Document = Jsoup.parse(result.body)

        "return a BadRequest (400) response" in {

          result.status shouldBe BAD_REQUEST
        }

        "have the correct page content" in {

          document.title() shouldBe Content.expectedErrorTitle
        }
      }
    }
  }

  "as an agent" when {

    ".show" should {

      "return an action" which {

        lazy val result: WSResponse = {
          lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
            SessionValues.CLIENT_MTDITID -> "1234567890",
            SessionValues.CLIENT_NINO -> "AA123456A"
          ))

          authoriseAgent()
          await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donations-after-5-april-xxxx")
            .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie)
            .get())
        }

        lazy val document: Document = Jsoup.parse(result.body)

        "has an OK (200) response" in {

          result.status shouldBe OK
        }

        "displays the page content" in {

          document.title() shouldBe Content.expectedTitle
          document.select(Selectors.heading).text() shouldBe Content.expectedHeading
          document.select(Selectors.caption).text() shouldBe Content.expectedCaption
          document.select(Selectors.paragraph1HintText).text() shouldBe Content.expectedParagraph1Agent
          document.select(Selectors.paragraph2HintText).text() shouldBe Content.expectedParagraph2Agent
        }
      }
    }

    ".submit" when {

      "there is correct form data" should {

        "return a response" which {

          lazy val result: WSResponse = {

            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"))

            authoriseAgent()
            await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donations-after-5-april-xxxx")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map(YesNoForm.yesNo -> YesNoForm.yes)))
          }


          "has an Ok (200) response" in {

            result.status shouldBe OK
          }
        }
      }

      "there is no form data" should {

        "return a response" which {

          lazy val result: WSResponse = {

            lazy val sessionCookie: String = PlaySessionCookieBaker.bakeSessionCookie(Map[String, String](
              SessionValues.CLIENT_MTDITID -> "1234567890",
              SessionValues.CLIENT_NINO -> "AA123456A"))

            authoriseAgent()
            await(wsClient.url(s"http://localhost:$port/income-through-software/return/personal-income/$taxYear/charity/donations-after-5-april-xxxx")
              .withHttpHeaders(HeaderNames.COOKIE -> sessionCookie, "Csrf-Token" -> "nocheck")
              .post(Map[String, String]()))
          }

          lazy val document: Document = Jsoup.parse(result.body)

          "has a BadRequest (400) response" in {

            result.status shouldBe BAD_REQUEST
          }

          "has the error page content" in {

            document.title() shouldBe Content.expectedErrorTitle
          }

        }
      }
    }
  }

}
