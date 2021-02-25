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

package views

import config.AppConfig
import org.jsoup.Jsoup
import org.jsoup.nodes.{Document, Element}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest
import views.html.authErrorPages.AgentAuthErrorPageView
import utils.ViewTest

class AgentAuthErrorPageViewSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with ViewTest {

  object Selectors {

    val pageHeading = "#main-content > div > div > header > h1"
    val p1 = "#main-content > div > div > p"
    val p2 = "#main-content > div > div > p:nth-child(3)"
    val link = "#client_auth_link"
  }

  val agentAuthErrorPageView: AgentAuthErrorPageView = app.injector.instanceOf[AgentAuthErrorPageView]

  implicit lazy val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest("", "")
  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val messages: Messages = messagesApi.preferred(fakeRequest)
  implicit lazy val mockConfig: AppConfig = app.injector.instanceOf[AppConfig]
  lazy val pText1 = "You cannot view this client’s information." +
    " Your client needs to authorise you as their agent (open in a new tab) before you can sign into this service."
  lazy val pText2 = "Try another client’s details"
  lazy val linkText = "authorise you as their agent (open in a new tab)"
  lazy val href = "https://www.gov.uk/guidance/client-authorisation-an-overview"

  def element(cssSelector: String)(implicit document: Document): Element = {
    val elements = document.select(cssSelector)

    if(elements.size == 0) {
      fail(s"No element exists with the selector '$cssSelector'")
    }

    document.select(cssSelector).first()
  }
  def elementText(selector: String)(implicit document: Document): String = {
    element(selector).text()
  }

  "AgentAuthErrorPageView " should {
    lazy val view = agentAuthErrorPageView()
    lazy implicit val document: Document = Jsoup.parse(view.body)

    "have a page heading of" in {
      elementText(Selectors.pageHeading) shouldBe "There’s a problem"
    }

    "have text of " in {
      elementText(Selectors.p1) shouldBe pText1
      elementText(Selectors.p2) shouldBe pText2
    }
    linkCheck(linkText, Selectors.link, href)
  }
}
