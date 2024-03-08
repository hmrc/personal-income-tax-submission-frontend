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

package test.views.templates

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import play.api.test.FakeRequest
import play.twirl.api.HtmlFormat
import test.utils.{IntegrationTest, ViewHelpers}
import views.html.templates.InternalServerErrorTemplate

class InternalServerErrorTemplateISpec extends IntegrationTest with ViewHelpers {

  object Selectors {

    val h1Selector = "#main-content > div > div > header > h1"
    val p1Selector = "#main-content > div > div > div.govuk-body > p:nth-child(1)"
    val p2Selector = "#main-content > div > div > div.govuk-body > p:nth-child(2)"
    val bulletPoint1 = "#main-content > div > div > ul > li:nth-child(1)"
    val bulletPointLinkSelector1 = "#govuk-income-tax-link"
    val bulletPoint2 = "#main-content > div > div > ul > li:nth-child(2)"
    val bulletPointLinkSelector2 = "#govuk-self-assessment-link"

  }
  object EnglishText {
    val h1Expected = "Sorry, there is a problem with the service"
    val p1Expected = "Try again later."
    val p2Expected = "You can also:"
    val bulletPoint1Expected = "go to the Income Tax home page (opens in new tab) for more information"
    val bulletPoint1Link = "https://www.gov.uk/income-tax"
    val bulletPoint1LinkText = "Income Tax home page (opens in new tab)"
    val bulletPoint2Expected = "use Self Assessment: general enquiries (opens in new tab) to speak to someone about your income tax."
    val bulletPoint2Link = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment"
    val bulletPoint2LinkText = "Self Assessment: general enquiries (opens in new tab)"
  }

  object WelshText {
    val h1Expected = "Mae’n ddrwg gennym – mae problem gyda’r gwasanaeth"
    val p1Expected = "Rhowch gynnig arall arni yn nes ymlaen."
    val p2Expected = "Gallwch hefyd wneud y canlynol:"
    val bulletPoint1Expected = "ewch i’r Tudalen hafan Treth Incwm (yn agor tab newydd) am ragor o wybodaeth"
    val bulletPoint1Link = "https://www.gov.uk/income-tax"
    val bulletPoint1LinkText = "Tudalen hafan Treth Incwm (yn agor tab newydd)"
    val bulletPoint2Expected = "defnyddiwch Hunanasesiad: ymholiadau cyffredinol (yn agor tab newydd) i siarad â rhywun am eich Treth Incwm."
    val bulletPoint2Link = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment"
    val bulletPoint2LinkText = "Hunanasesiad: ymholiadau cyffredinol (yn agor tab newydd)"
  }

  lazy val internalServerErrorTemplate: InternalServerErrorTemplate = app.injector.instanceOf[InternalServerErrorTemplate]

  "UnauthorisedTemplate in English" should {

    "render the page correctly" which {

      lazy val view: HtmlFormat.Appendable = internalServerErrorTemplate()(FakeRequest(), messages, appConfig)
      implicit def document: () => Document = () => Jsoup.parse(view.body)

      titleCheck(EnglishText.h1Expected, false)
      welshToggleCheck("English")
      h1Check(EnglishText.h1Expected, "xl")
      textOnPageCheck(EnglishText.p1Expected, Selectors.p1Selector)
      textOnPageCheck(EnglishText.p2Expected, Selectors.p2Selector)

      textOnPageCheck(EnglishText.bulletPoint1Expected,Selectors.bulletPoint1)
      linkCheck(EnglishText.bulletPoint1LinkText, Selectors.bulletPointLinkSelector1, EnglishText.bulletPoint1Link)

      textOnPageCheck(EnglishText.bulletPoint2Expected,Selectors.bulletPoint2)
      linkCheck(EnglishText.bulletPoint2LinkText, Selectors.bulletPointLinkSelector2, EnglishText.bulletPoint2Link)

    }
  }

  "UnauthorisedTemplate in Welsh" should {

    "render the page correctly" which {

      lazy val view: HtmlFormat.Appendable = internalServerErrorTemplate()(FakeRequest(), welshMessages, appConfig)
      implicit def document: () => Document = () => Jsoup.parse(view.body)

      titleCheck(WelshText.h1Expected, true)
      welshToggleCheck("Welsh")
      h1Check(WelshText.h1Expected, "xl")
      textOnPageCheck(WelshText.p1Expected, Selectors.p1Selector)
      textOnPageCheck(WelshText.p2Expected, Selectors.p2Selector)

      textOnPageCheck(WelshText.bulletPoint1Expected,Selectors.bulletPoint1)
      linkCheck(WelshText.bulletPoint1LinkText, Selectors.bulletPointLinkSelector1, WelshText.bulletPoint1Link)

      textOnPageCheck(WelshText.bulletPoint2Expected,Selectors.bulletPoint2)
      linkCheck(WelshText.bulletPoint2LinkText, Selectors.bulletPointLinkSelector2, WelshText.bulletPoint2Link)

    }
  }
}
