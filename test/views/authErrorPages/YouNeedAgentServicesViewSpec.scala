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

package views.authErrorPages

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import utils.ViewTest
import views.html.authErrorPages.YouNeedAgentServicesView

class YouNeedAgentServicesViewSpec extends AnyWordSpec with Matchers with GuiceOneAppPerSuite with ViewTest{

  val p1Selector = "#main-content > div > div > p"
  val createAnAgentLinkSelector = "#create_agent_services_link"

  lazy val h1Expected = "You cannot view this page"
  lazy val youNeedText = "You need to"
  lazy val createAnAgentText = "create an agent services account"
  lazy val beforeYouCanText = "before you can view this page."
  lazy val createAnAgentLink = "https://www.gov.uk/guidance/get-an-hmrc-agent-services-account"

  val youNeedAgentServicesView: YouNeedAgentServicesView = app.injector.instanceOf[YouNeedAgentServicesView]
  lazy implicit val document: Document = Jsoup.parse(youNeedAgentServicesView().body)

  "YouNeedAgentServicesView " should {
    "Correctly render" which {
      titleCheck(h1Expected)
      h1Check(h1Expected, "xl")

      textOnPageCheck(s"$youNeedText $createAnAgentText $beforeYouCanText", p1Selector)
      linkCheck(createAnAgentText, createAnAgentLinkSelector, createAnAgentLink)
    }
  }
}
