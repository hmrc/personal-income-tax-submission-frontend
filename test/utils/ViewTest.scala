/*
 * Copyright 2020 HM Revenue & Customs
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

package utils

import models.User
import org.jsoup.nodes.{Document, Element}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.i18n.{Messages, MessagesApi}
import play.api.mvc.{AnyContent, Call, MessagesControllerComponents}
import play.api.test.{FakeRequest, Helpers}

trait ViewTest extends UnitTest with GuiceOneAppPerSuite {

  class TestWithAuth(isAgent: Boolean = false) {
    if(isAgent) mockAuthAsAgent() else mockAuth()
  }

  val testBackUrl = "/test-back-url"
  val testCall: Call = Call("POST", "/test-url")
  implicit lazy val user: User[AnyContent] = new User[AnyContent]("1234567890", None)(FakeRequest())
  implicit lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  implicit lazy val messages: Messages = messagesApi.preferred(FakeRequest())
  implicit lazy val mockMessagesControllerComponents: MessagesControllerComponents = Helpers.stubMessagesControllerComponents()

  type IntString = Int => String

  val questionTextSelector: IntString = question => s"#main-content > div > div > dl > div:nth-child($question) > dt"
  val questionAnswerSelector: IntString = question => s"#main-content > div > div > dl > div:nth-child($question) > " +
    s"dd.govuk-summary-list__value"
  val questionChangeLinkSelector: IntString = question => s"#main-content > div > div > dl > div:nth-child($question) > " +
    s"dd.govuk-summary-list__actions > a"

  def elementText(selector: String)(implicit document: Document): String = {
    document.select(selector).text()
  }

  def element(selector: String)(implicit document: Document): Element = {
    val elements = document.select(selector)

    if(elements.size() == 0) {
      fail(s"No elements exist with the selector '$selector'")
    }

    elements.first()
  }

  def elementExist(selector: String)(implicit document: Document): Boolean = {
    !document.select(selector).isEmpty
  }

}
