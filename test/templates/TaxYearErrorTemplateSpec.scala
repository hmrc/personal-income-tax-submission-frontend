
package templates

import config.AppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.twirl.api.HtmlFormat
import utils.ViewTest
import views.html.templates.TaxYearErrorTemplate

class TaxYearErrorTemplateSpec extends ViewTest {

  object Selectors {

    val h1Selector = "#main-content > div > div > header > h1"
    val p1Selector = "#main-content > div > div > div.govuk-body > p:nth-child(1)"
    val p2Selector = "#main-content > div > div > div.govuk-body > p:nth-child(2)"
    val p3Selector = "#main-content > div > div > div.govuk-body > p:nth-child(3)"
  }

  val h1Expected = "Page not found"
  val p1Expected = "You can only enter information for the 2021 to 2022 tax year."
  val p2Expected = "Check that you’ve entered the correct web address."
  val p3Expected: String = "If the web address is correct or you selected a link or button, you can use Self Assessment: " +
    "general enquiries (opens in new tab) to speak to someone about your income tax."
  val p3ExpectedLink = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment"
  val expectedTitle = s"$h1Expected - $serviceName - $govUkExtension"

  val taxYearErrorTemplate: TaxYearErrorTemplate = app.injector.instanceOf[TaxYearErrorTemplate]
  val appConfig: AppConfig = mockAppConfig

  lazy val view: HtmlFormat.Appendable = taxYearErrorTemplate()(fakeRequest, messages, mockAppConfig)
  implicit lazy val document: Document = Jsoup.parse(view.body)

  "TaxYearErrorTemplate" should {

    "render the page correctly" which {

      "has the correct page title" in {

        document.title() shouldBe expectedTitle
      }

      "has the correct heading" in {

        elementText(Selectors.h1Selector) shouldBe h1Expected
      }

      "has the correct p1" in {

        elementText(Selectors.p1Selector) shouldBe p1Expected
      }

      "has the correct p2" in {

        elementText(Selectors.p2Selector) shouldBe p2Expected
      }

      "has the correct p3" in {

        elementText(Selectors.p3Selector) shouldBe p3Expected
      }

      "has the correct link in 3rd paragraph" in {

        document.select(s"""[id=govuk-self-assessment-link]""").attr("href") shouldBe p3ExpectedLink
      }
    }
  }

}
