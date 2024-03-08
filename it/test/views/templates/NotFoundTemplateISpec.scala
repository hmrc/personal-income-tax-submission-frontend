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
import views.html.templates.NotFoundTemplate

class NotFoundTemplateISpec extends IntegrationTest with ViewHelpers {

  object Selectors {

    val h1Selector = "#main-content > div > div > header > h1"
    val p1Selector = "#main-content > div > div > div.govuk-body > p:nth-child(1)"
    val p2Selector = "#main-content > div > div > div.govuk-body > p:nth-child(2)"
    val p3Selector = "#main-content > div > div > div.govuk-body > p:nth-child(3)"
    val linkSelector = "#govuk-self-assessment-link"
  }
  object EnglishText {
    val h1Expected = "Page not found"
    val p1Expected = "If you typed the web address, check it is correct."
    val p2Expected = "If you used ‘copy and paste’ to enter the web address, check you copied the full address."
    val p3Expected: String = "If the web address is correct or you selected a link or button, you can use Self Assessment: " +
      "general enquiries (opens in new tab) to speak to someone about your income tax."
    val p3ExpectedLink = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment"
    val p3ExpectedLinkText = "Self Assessment: general enquiries (opens in new tab)"
  }
  object WelshText {
    val h1Expected = "Heb ddod o hyd i’r dudalen"
    val p1Expected = "Os gwnaethoch deipio’r cyfeiriad gwe, gwiriwch ei fod yn gywir."
    val p2Expected = "Os gwnaethoch ddefnyddio ‘copïo a gludo’ i nodi’r cyfeiriad gwe, gwiriwch eich bod wedi copïo’r cyfeiriad llawn."
    val p3Expected: String = "Os yw’r cyfeiriad gwe yn gywir neu os ydych wedi dewis cysylltiad neu fotwm, gallwch ddefnyddio " +
      "Hunanasesiad: ymholiadau cyffredinol (yn agor tab newydd) i siarad â rhywun am eich Treth Incwm."
    val p3ExpectedLink = "https://www.gov.uk/government/organisations/hm-revenue-customs/contact/self-assessment"
    val p3ExpectedLinkText = "Hunanasesiad: ymholiadau cyffredinol (yn agor tab newydd)"
  }

  val notFoundTemplate: NotFoundTemplate = app.injector.instanceOf[NotFoundTemplate]

  "NotFoundTemplate in English" should {

    "render the page correctly" which {

      lazy val view: HtmlFormat.Appendable = notFoundTemplate()(FakeRequest(), messages, appConfig)
      implicit def document: () => Document = () => Jsoup.parse(view.body)

      titleCheck(EnglishText.h1Expected, false)
      welshToggleCheck("English")
      h1Check(EnglishText.h1Expected, "xl")

      textOnPageCheck(EnglishText.p1Expected,Selectors.p1Selector)
      textOnPageCheck(EnglishText.p2Expected,Selectors.p2Selector)
      textOnPageCheck(EnglishText.p3Expected,Selectors.p3Selector)
      linkCheck(EnglishText.p3ExpectedLinkText, Selectors.linkSelector, EnglishText.p3ExpectedLink)

    }
  }

  "NotFoundTemplate in Welsh" should {

    "render the page correctly" which {

      lazy val view: HtmlFormat.Appendable = notFoundTemplate()(FakeRequest(), welshMessages, appConfig)
      implicit def document: () => Document = () => Jsoup.parse(view.body)

      titleCheck(WelshText.h1Expected, true)
      welshToggleCheck("Welsh")
      h1Check(WelshText.h1Expected, "xl")

      textOnPageCheck(WelshText.p1Expected,Selectors.p1Selector)
      textOnPageCheck(WelshText.p2Expected,Selectors.p2Selector)
      textOnPageCheck(WelshText.p3Expected,Selectors.p3Selector)
      linkCheck(WelshText.p3ExpectedLinkText, Selectors.linkSelector, WelshText.p3ExpectedLink)

    }
  }

}
